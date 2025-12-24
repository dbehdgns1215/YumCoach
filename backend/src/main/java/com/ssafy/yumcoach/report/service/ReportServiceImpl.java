package com.ssafy.yumcoach.report.service;

import com.ssafy.yumcoach.ai.AiResult;
import com.ssafy.yumcoach.ai.OpenAiService;
import com.ssafy.yumcoach.ai.ReportAnalysisResult;
import com.ssafy.yumcoach.challenge.model.ChallengeDto;
import com.ssafy.yumcoach.challenge.model.service.ChallengeService;
import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.mapper.FoodMapper;
import com.ssafy.yumcoach.meal.model.MealItemDto;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import com.ssafy.yumcoach.meal.model.mapper.MealMapper;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportInsightDto;
import com.ssafy.yumcoach.report.model.ReportMealDto;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final MealMapper mealMapper;
    private final FoodMapper foodMapper;
    private final UserMapper userMapper;
    private final ChallengeService challengeService;
    private final OpenAiService openAiService;

    @Override
    public ReportDto createDailyReport(int userId, LocalDate date) {
        User user = userMapper.findById(userId);

        int limit = user != null && "ADMIN".equalsIgnoreCase(user.getRole()) ? 1000 :
                user != null && "ADVANCED".equalsIgnoreCase(user.getRole()) ? 2 : 1;

        var start = date.atStartOfDay();
        var end = date.plusDays(1).atStartOfDay();

        if (reportMapper.countGenerationLogsInPeriod(userId, "DAILY", start, end, "USER") >= limit) {
            throw new IllegalStateException("LIMIT_EXCEEDED");
        }

        ReportDto dto = new ReportDto();
        dto.setUserId(userId);
        dto.setDate(date);
        dto.setType("DAILY");
        dto.setStatus("PROGRESS");
        dto.setCreatedBy("USER");

        reportMapper.insertReport(dto);

        List<MealLogDto> logs = mealMapper.selectMealLogsByUserAndDateRange(userId, date, date);

        int cal = 0, pro = 0, carb = 0, fat = 0, cnt = 0;
        List<ReportMealDto> meals = new ArrayList<>();

        for (MealLogDto log : logs) {
            if (log.getItems() == null) continue;
            for (MealItemDto item : log.getItems()) {
                if (item.getMealCode() == null || item.getAmount() == null) continue;
                FoodDetailDto fd = foodMapper.selectFoodDetailById(item.getMealCode());
                if (fd == null || fd.getNutrition() == null) continue;

                double f = item.getAmount() / 100.0;
                int k = (int)(fd.getNutrition().getEnergyKcal() * f);
                int p = (int)(fd.getNutrition().getProteinG() * f);
                int c = (int)(fd.getNutrition().getCarbohydrateG() * f);
                int fa = (int)(fd.getNutrition().getFatG() * f);

                cal += k; pro += p; carb += c; fat += fa; cnt++;

                ReportMealDto rm = new ReportMealDto();
                rm.setReportId(dto.getId());
                rm.setCalories(k);
                rm.setProteinG(p);
                rm.setCarbG(c);
                rm.setFatG(fa);
                rm.setMealName(fd.getFood().getFoodName());

                reportMapper.insertReportMeal(rm);
                meals.add(rm);
            }
        }

        if (cnt == 0) throw new IllegalStateException("NO_MEALS");

        reportMapper.updateReportSummary(dto.getId(), cal, pro, carb, fat, cnt);

        // meals objects now have generated ids (mapper configured to useGeneratedKeys)
        dto.setMeals(meals);

        boolean analysisPassed = false;
        try { analyzeReport(dto.getId()); analysisPassed = true; } catch (Exception ignored) { log.warn("analyzeReport failed for reportId={}", dto.getId()); }

        // 기록: 생성 로그 남기기 (생성 횟수 집계에 사용)
        try {
            int inserted = reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER", analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), null);
            log.debug("insertGenerationLog returned {} for reportId={}", inserted, dto.getId());
        } catch (Exception ex) {
            log.error("insertGenerationLog failed for reportId={} error={}", dto.getId(), ex.toString());
        }

        // Refresh DTO from DB to populate DB-generated fields (createdAt/updatedAt etc.)
        ReportDto refreshed = reportMapper.selectReportById(dto.getId());
        if (refreshed != null) {
            // attach the meals we've built (they include generated ids)
            refreshed.setMeals(meals);
            try {
                var insights = reportMapper.selectReportInsights(dto.getId());
                refreshed.setInsights(insights);
                log.debug("selectReportInsights returned {} rows for reportId={}", insights == null ? 0 : insights.size(), dto.getId());
            } catch (Exception ex) {
                log.warn("failed to load insights for reportId={} error={}", dto.getId(), ex.toString());
            }
            return refreshed;
        }

        return dto;
    }

    @Override
    public ReportDto getDailyReport(int userId, LocalDate date) {
        ReportDto dto = reportMapper.selectReportByUserTypeDate(userId, "DAILY", date);
        if (dto == null) return null;
        try {
            var insights = reportMapper.selectReportInsights(dto.getId());
            dto.setInsights(insights);
        } catch (Exception ex) {
            log.warn("failed to load insights for getDailyReport reportId={} error={}", dto.getId(), ex.toString());
        }
        try {
            var meals = reportMapper.selectReportMeals(dto.getId());
            dto.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getDailyReport reportId={} error={}", dto.getId(), ex.toString());
        }
        return dto;
    }

    @Override
    public ReportDto createWeeklyReport(int userId, LocalDate from, LocalDate to) {

        ReportDto dto = new ReportDto();
        dto.setUserId(userId);
        dto.setFromDate(from);
        dto.setToDate(to);
        dto.setType("WEEKLY");
        dto.setStatus("PROGRESS");
        dto.setCreatedBy("USER");

        reportMapper.insertReport(dto);

        List<MealLogDto> logs = mealMapper.selectMealLogsByUserAndDateRange(userId, from, to);

        int cal = 0, pro = 0, carb = 0, fat = 0, cnt = 0;

        for (MealLogDto log : logs) {
            if (log.getItems() == null) continue;
            for (MealItemDto item : log.getItems()) {
                FoodDetailDto fd = foodMapper.selectFoodDetailById(item.getMealCode());
                if (fd == null || fd.getNutrition() == null) continue;

                double f = item.getAmount() / 100.0;
                cal += fd.getNutrition().getEnergyKcal() * f;
                pro += fd.getNutrition().getProteinG() * f;
                carb += fd.getNutrition().getCarbohydrateG() * f;
                fat += fd.getNutrition().getFatG() * f;
                cnt++;
            }
        }

        if (cnt == 0) throw new IllegalStateException("NO_MEALS");

        reportMapper.updateReportSummary(dto.getId(), cal, pro, carb, fat, cnt);

        // 분석 시도 및 생성 로그 남기기 (주간 생성도 동일하게 기록)
        boolean analysisPassed = false;
        try {
            analyzeReport(dto.getId());
            analysisPassed = true;
        } catch (Exception ignored) {
            log.warn("analyzeReport failed for weekly reportId={}", dto.getId());
        }

        try {
            int inserted = reportMapper.insertGenerationLog(userId, "WEEKLY", null, from, to, "USER", analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), null);
            log.debug("insertGenerationLog returned {} for weekly reportId={}", inserted, dto.getId());
        } catch (Exception ex) {
            log.error("insertGenerationLog failed for weekly reportId={} error={}", dto.getId(), ex.toString());
        }

        // Refresh DTO from DB and attach persisted insights (and empty meals list)
        ReportDto refreshed = reportMapper.selectReportById(dto.getId());
        if (refreshed != null) {
            refreshed.setMeals(new ArrayList<>());
            try {
                var insights = reportMapper.selectReportInsights(dto.getId());
                refreshed.setInsights(insights);
                log.debug("selectReportInsights returned {} rows for weekly reportId={}", insights == null ? 0 : insights.size(), dto.getId());
            } catch (Exception ex) {
                log.warn("failed to load insights for weekly reportId={} error={}", dto.getId(), ex.toString());
            }
            return refreshed;
        }

        return dto;
    }

    @Override
    public ReportDto getWeeklyReport(int userId, LocalDate fromDate, LocalDate toDate) {
        ReportDto dto = reportMapper.selectReportByUserAndRange(userId, "WEEKLY", fromDate, toDate);
        if (dto == null) return null;
        try {
            var insights = reportMapper.selectReportInsights(dto.getId());
            dto.setInsights(insights);
        } catch (Exception ex) {
            log.warn("failed to load insights for getWeeklyReport reportId={} error={}", dto.getId(), ex.toString());
        }
        try {
            var meals = reportMapper.selectReportMeals(dto.getId());
            dto.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getWeeklyReport reportId={} error={}", dto.getId(), ex.toString());
        }
        return dto;
    }

    @Override
    public ReportDto getReportById(int userId, int reportId) {
        ReportDto report = reportMapper.selectReportById(reportId);
        if (report == null || report.getUserId() != userId) return null;
        try {
            var insights = reportMapper.selectReportInsights(report.getId());
            report.setInsights(insights);
        } catch (Exception ex) {
            log.warn("failed to load insights for getReportById reportId={} error={}", report.getId(), ex.toString());
        }
        try {
            var meals = reportMapper.selectReportMeals(report.getId());
            report.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getReportById reportId={} error={}", report.getId(), ex.toString());
        }
        return report;
    }

    @Transactional
    public void analyzeReport(int reportId) throws Exception {
        ReportDto report = reportMapper.selectReportById(reportId);
        if (report == null) return;
        // Ensure meals and insights are loaded so AI receives full report context
        try {
            var meals = reportMapper.selectReportMeals(reportId);
            report.setMeals(meals);
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load meals for reportId={} error={}", reportId, ex.toString());
        }
        try {
            var existingInsights = reportMapper.selectReportInsights(reportId);
            report.setInsights(existingInsights);
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load insights for reportId={} error={}", reportId, ex.toString());
        }

        // Load active challenges for this user so AI gets challenge context
        try {
            if (report.getUserId() != null) {
                var activeCh = challengeService.getActiveChallenges(report.getUserId());
                report.setActiveChallenges(activeCh);
                log.debug("analyzeReport: attached {} activeChallenges for reportId={}", activeCh == null ? 0 : activeCh.size(), reportId);
            }
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load active challenges for reportId={} error={}", reportId, ex.toString());
        }

        AiResult ai = openAiService.analyze(report);
        // save raw AI response (for debugging / audit)
        reportMapper.updateReportAiResponse(reportId, ai.rawJson());

        if (ai.parsed() == null) {
            log.warn("analyzeReport: parsed AI result is null for reportId={}", reportId);
            return;
        }

        ReportAnalysisResult r = ai.parsed();
        // Log parsed fields to diagnose mapping issues
        log.debug("analyzeReport parsed: reportId={}, heroTitle={}, heroLine={}, score={}, coachMessagePresent={}, nextActionPresent={}, insightsCount={}",
                reportId,
                r.getHeroTitle(),
                r.getHeroLine(),
                r.getScore(),
                r.getCoachMessage() != null,
                r.getNextAction() != null,
                r.getInsights() == null ? 0 : r.getInsights().size()
        );

        // persist parsed fields (may be null individually)
        reportMapper.updateReportCoachMessage(reportId, r.getCoachMessage());
        reportMapper.updateReportNextAction(reportId, r.getNextAction());
        reportMapper.updateReportScore(reportId, r.getScore());
        reportMapper.updateReportHero(reportId, r.getHeroTitle(), r.getHeroLine());

        reportMapper.deleteInsightsByReportId(reportId);

        if (r.getInsights() != null) {
            int inserted = 0;
            for (ReportAnalysisResult.Insight i : r.getInsights()) {
                try {
                    log.debug("inserting insight for reportId={} kind={} title={}", reportId, i.getKind(), i.getTitle());
                    int res = reportMapper.insertReportInsight(reportId, i.getKind(), i.getTitle(), i.getBody());
                    log.debug("insertReportInsight returned {} for reportId={} kind={}", res, reportId, i.getKind());
                    if (res > 0) inserted++;
                } catch (Exception ex) {
                    log.error("failed to insert insight for reportId={} kind={} title={} error={}", reportId, i.getKind(), i.getTitle(), ex.toString());
                }
            }
            log.debug("analyzeReport: inserted {} insights for reportId={}", inserted, reportId);
            // Also persist coachMessage and nextAction as separate insight rows (so frontend can fallback to insights)
            int extraInserted = 0;
            try {
                if (r.getCoachMessage() != null && !r.getCoachMessage().isBlank()) {
                    int res = reportMapper.insertReportInsight(reportId, "coach", "코치 한마디", r.getCoachMessage());
                    log.debug("insertReportInsight (coach) returned {} for reportId={}", res, reportId);
                    if (res > 0) extraInserted++;
                }
            } catch (Exception ex) {
                log.error("failed to insert coach insight for reportId={} error={}", reportId, ex.toString());
            }
            try {
                if (r.getNextAction() != null && !r.getNextAction().isBlank()) {
                    int res = reportMapper.insertReportInsight(reportId, "action", "권장 행동", r.getNextAction());
                    log.debug("insertReportInsight (action) returned {} for reportId={}", res, reportId);
                    if (res > 0) extraInserted++;
                }
            } catch (Exception ex) {
                log.error("failed to insert action insight for reportId={} error={}", reportId, ex.toString());
            }
            if (extraInserted > 0) log.debug("analyzeReport: inserted {} extra insights (coach/action) for reportId={}", extraInserted, reportId);
        } else {
            log.debug("analyzeReport: no insights to insert for reportId={}", reportId);
        }
    }
}
