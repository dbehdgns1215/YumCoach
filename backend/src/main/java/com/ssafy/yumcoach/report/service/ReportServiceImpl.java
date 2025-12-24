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
            String details = analysisPassed ? "ANALYZED_WITH_AI" : "CREATED_NO_AI";
            int inserted = reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER", analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), details);
            log.debug("insertGenerationLog returned {} for reportId={} details={}", inserted, dto.getId(), details);
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

    public ReportDto createDailyReport(int userId, LocalDate date, String createdBy) {
        User user = userMapper.findById(userId);

        // 🔥 SCHEDULER로 만든 리포트는 제한 체크 안 함
        if (!"SCHEDULER".equals(createdBy)) {
            int limit = user != null && "ADMIN".equalsIgnoreCase(user.getRole()) ? 1000 :
                    user != null && "ADVANCED".equalsIgnoreCase(user.getRole()) ? 2 : 1;

            var start = date.atStartOfDay();
            var end = date.plusDays(1).atStartOfDay();

            if (reportMapper.countGenerationLogsInPeriod(userId, "DAILY", start, end, "USER") >= limit) {
                throw new IllegalStateException("LIMIT_EXCEEDED");
            }
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
            String details = analysisPassed ? "ANALYZED_WITH_AI" : "CREATED_NO_AI";
            int inserted = reportMapper.insertGenerationLog(userId, "WEEKLY", null, from, to, "USER", analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), details);
            log.debug("insertGenerationLog returned {} for weekly reportId={} details={}", inserted, dto.getId(), details);
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

        // meals 로드
        try {
            var meals = reportMapper.selectReportMeals(reportId);
            report.setMeals(meals);
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load meals for reportId={} error={}", reportId, ex.toString());
        }

        // 기존 insights 로드
        try {
            var existingInsights = reportMapper.selectReportInsights(reportId);
            report.setInsights(existingInsights);
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load insights for reportId={} error={}", reportId, ex.toString());
        }

        // 활성 챌린지 로드
        List<ChallengeDto> activeChallenges = null;
        try {
            if (report.getUserId() != null) {
                activeChallenges = challengeService.getActiveChallenges(report.getUserId());
                report.setActiveChallenges(activeChallenges);
                log.debug("analyzeReport: attached {} activeChallenges for reportId={}",
                        activeChallenges == null ? 0 : activeChallenges.size(), reportId);
            }
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load active challenges for reportId={} error={}", reportId, ex.toString());
        }

        // AI 분석 호출
        AiResult ai = openAiService.analyze(report);
        reportMapper.updateReportAiResponse(reportId, ai.rawJson());

        if (ai.parsed() == null) {
            log.warn("analyzeReport: parsed AI result is null for reportId={}", reportId);
            return;
        }

        ReportAnalysisResult r = ai.parsed();

        // 기본 필드 업데이트
        reportMapper.updateReportCoachMessage(reportId, r.getCoachMessage());
        reportMapper.updateReportNextAction(reportId, r.getNextAction());
        reportMapper.updateReportScore(reportId, r.getScore());
        reportMapper.updateReportHero(reportId, r.getHeroTitle(), r.getHeroLine());

        // Insights 저장
        reportMapper.deleteInsightsByReportId(reportId);
        if (r.getInsights() != null) {
            for (ReportAnalysisResult.Insight i : r.getInsights()) {
                try {
                    reportMapper.insertReportInsight(reportId, i.getKind(), i.getTitle(), i.getBody());
                } catch (Exception ex) {
                    log.error("failed to insert insight for reportId={} kind={} error={}",
                            reportId, i.getKind(), ex.toString());
                }
            }

            // coach/action도 insights로 저장
            if (r.getCoachMessage() != null && !r.getCoachMessage().isBlank()) {
                reportMapper.insertReportInsight(reportId, "coach", "코치 한마디", r.getCoachMessage());
            }
            if (r.getNextAction() != null && !r.getNextAction().isBlank()) {
                reportMapper.insertReportInsight(reportId, "action", "권장 행동", r.getNextAction());
            }
        }

        // ✨ AI 응답의 challengeProgress 처리
        processChallengeProgress(report, ai, activeChallenges);
    }
    /**
     * AI 응답에서 챌린지 진행도를 추출하여 DB에 반영
     */
    private void processChallengeProgress(ReportDto report, AiResult aiResult, List<ChallengeDto> activeChallenges) {
        try {
            if (aiResult.rawJson() == null || aiResult.rawJson().isBlank()) {
                log.debug("[ReportService] AI 응답이 없어 챌린지 업데이트 스킵");
                return;
            }

            if (activeChallenges == null || activeChallenges.isEmpty()) {
                log.debug("[ReportService] 활성 챌린지가 없어 챌린지 업데이트 스킵");
                return;
            }

            // AI 응답 파싱
            ObjectMapper mapper = new ObjectMapper();
            JsonNode aiResponse = mapper.readTree(aiResult.rawJson());
            JsonNode challengeProgressNode = aiResponse.get("challengeProgress");

            if (challengeProgressNode == null || !challengeProgressNode.isArray()) {
                log.debug("[ReportService] challengeProgress 없음 - AI가 생성하지 않았거나 파싱 실패");
                return;
            }

            // 리포트 날짜 결정
            LocalDate logDate = report.getDate() != null ? report.getDate() :
                    report.getToDate() != null ? report.getToDate() : LocalDate.now();

            // reportData 구성 (recordDailyLog에서 사용)
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("totalCalories", report.getTotalCalories());
            reportData.put("totalProtein", report.getProteinG());
            reportData.put("totalCarbs", report.getCarbG());
            reportData.put("totalFat", report.getFatG());
            reportData.put("mealCount", report.getMealCount());

            // 각 챌린지별로 dailyLog 기록
            for (JsonNode progress : challengeProgressNode) {
                try {
                    Long challengeId = progress.get("challengeId").asLong();

                    // 해당 챌린지가 실제로 활성화되어 있는지 확인
                    boolean isActiveChallenge = activeChallenges.stream()
                            .anyMatch(ch -> ch.getId().equals(challengeId));

                    if (!isActiveChallenge) {
                        log.warn("[ReportService] challengeId={} not in active challenges, skipping", challengeId);
                        continue;
                    }

                    // recordDailyLog 호출 (내부에서 달성 여부 재계산)
                    challengeService.recordDailyLog(challengeId, logDate, reportData);

                    log.info("[ReportService] 챌린지 업데이트 완료 - challengeId={}, date={}, " +
                                    "isAchieved={}, rate={}%",
                            challengeId,
                            logDate,
                            progress.get("isAchieved").asBoolean(),
                            progress.get("achievementRate").asDouble());

                } catch (Exception e) {
                    log.error("[ReportService] 챌린지 진행도 업데이트 실패 - challengeId from AI response", e);
                }
            }

        } catch (Exception e) {
            log.error("[ReportService] 챌린지 진행도 처리 전체 실패", e);
            // 실패해도 리포트 생성은 성공으로 처리
        }
    }
}
