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
import java.util.stream.Collectors;

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

        int used = getUsedCountForPeriod(userId, "DAILY", date, start, end);
        if (used >= limit) {
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

        int cal = 0, pro = 0, carb = 0, fat = 0;
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

                cal += k; pro += p; carb += c; fat += fa;

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

        if (meals.isEmpty()) throw new IllegalStateException("NO_MEALS");

        // 🔥 식사 횟수 계산: MealHistory의 고유 타입 개수
        int mealCount = calculateMealCount(userId, date);

        reportMapper.updateReportSummary(dto.getId(), cal, pro, carb, fat, mealCount);

        dto.setMeals(meals);

        boolean analysisPassed = false;
        try {
            analyzeReport(dto.getId());
            analysisPassed = true;
        } catch (Exception e) {
            log.warn("analyzeReport failed for reportId={}", dto.getId(), e);
        }

        // 기록: 생성 로그 남기기
        try {
            String details = analysisPassed ? "ANALYZED_WITH_AI" : "CREATED_NO_AI";
            reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER",
                    analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), details);
        } catch (Exception ex) {
            log.error("insertGenerationLog failed for reportId={}", dto.getId(), ex);
        }

        // Refresh DTO
        ReportDto refreshed = reportMapper.selectReportById(dto.getId());
        if (refreshed != null) {
            refreshed.setMeals(meals);
            try {
                var insights = reportMapper.selectReportInsights(dto.getId());
                refreshed.setInsights(insights);
            } catch (Exception ex) {
                log.warn("failed to load insights for reportId={}", dto.getId(), ex);
            }
            return refreshed;
        }

        return dto;
    }

    public ReportDto createDailyReport(int userId, LocalDate date, String createdBy) {
        User user = userMapper.findById(userId);

        if (!"SCHEDULER".equals(createdBy)) {
            int limit = user != null && "ADMIN".equalsIgnoreCase(user.getRole()) ? 1000 :
                    user != null && "ADVANCED".equalsIgnoreCase(user.getRole()) ? 2 : 1;

            var start = date.atStartOfDay();
            var end = date.plusDays(1).atStartOfDay();

            int used = getUsedCountForPeriod(userId, "DAILY", date, start, end);
            if (used >= limit) {
                throw new IllegalStateException("LIMIT_EXCEEDED");
            }
        }

        ReportDto dto = new ReportDto();
        dto.setUserId(userId);
        dto.setDate(date);
        dto.setType("DAILY");
        dto.setStatus("PROGRESS");
        dto.setCreatedBy(createdBy);

        reportMapper.insertReport(dto);

        List<MealLogDto> logs = mealMapper.selectMealLogsByUserAndDateRange(userId, date, date);

        int cal = 0, pro = 0, carb = 0, fat = 0;
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

                cal += k; pro += p; carb += c; fat += fa;

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

        if (meals.isEmpty()) throw new IllegalStateException("NO_MEALS");

        // 🔥 식사 횟수 계산
        int mealCount = calculateMealCount(userId, date);

        reportMapper.updateReportSummary(dto.getId(), cal, pro, carb, fat, mealCount);

        dto.setMeals(meals);

        boolean analysisPassed = false;
        try {
            analyzeReport(dto.getId());
            analysisPassed = true;
        } catch (Exception e) {
            log.warn("analyzeReport failed for reportId={}", dto.getId(), e);
        }

        try {
            reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, createdBy,
                    analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), null);
        } catch (Exception ex) {
            log.error("insertGenerationLog failed for reportId={}", dto.getId(), ex);
        }

        ReportDto refreshed = reportMapper.selectReportById(dto.getId());
        if (refreshed != null) {
            refreshed.setMeals(meals);
            try {
                var insights = reportMapper.selectReportInsights(dto.getId());
                refreshed.setInsights(insights);
            } catch (Exception ex) {
                log.warn("failed to load insights for reportId={}", dto.getId(), ex);
            }
            return refreshed;
        }

        return dto;
    }

    /**
     * 🔥 식사 횟수 계산: MealHistory에서 해당 날짜의 고유 타입 개수를 카운트
     */
    private int calculateMealCount(int userId, LocalDate date) {
        try {
            // MealMapper에 메서드 추가 필요: selectMealHistoryByUserAndDate
            List<String> mealTypes = mealMapper.selectMealTypesByUserAndDate(userId, date);

            if (mealTypes == null || mealTypes.isEmpty()) {
                log.debug("[ReportService] No meal history for userId={}, date={}", userId, date);
                return 0;
            }

            // 고유 타입만 카운트 (BREAKFAST, LUNCH, DINNER, SNACK 등)
            Set<String> uniqueTypes = new HashSet<>(mealTypes);

            log.debug("[ReportService] Meal count for userId={}, date={}: {} types ({})",
                    userId, date, uniqueTypes.size(), uniqueTypes);

            return uniqueTypes.size();

        } catch (Exception e) {
            log.error("[ReportService] Failed to calculate meal count for userId={}, date={}",
                    userId, date, e);
            return 0;
        }
    }

    @Override
    public ReportDto getDailyReport(int userId, LocalDate date) {
        ReportDto dto = reportMapper.selectReportByUserTypeDate(userId, "DAILY", date);
        if (dto == null) return null;
        try {
            var insights = reportMapper.selectReportInsights(dto.getId());
            dto.setInsights(insights);
        } catch (Exception ex) {
            log.warn("failed to load insights for getDailyReport reportId={}", dto.getId(), ex);
        }
        try {
            var meals = reportMapper.selectReportMeals(dto.getId());
            dto.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getDailyReport reportId={}", dto.getId(), ex);
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

        int cal = 0, pro = 0, carb = 0, fat = 0;

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
            }
        }

        if (cal == 0) throw new IllegalStateException("NO_MEALS");

        // 🔥 주간 식사 횟수: 기간 내 모든 날짜의 평균
        int totalMealCount = 0;
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;

        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            totalMealCount += calculateMealCount(userId, d);
        }

        int avgMealCount = daysBetween > 0 ? (int) Math.round((double) totalMealCount / daysBetween) : 0;

        reportMapper.updateReportSummary(dto.getId(), cal, pro, carb, fat, avgMealCount);

        boolean analysisPassed = false;
        try {
            analyzeReport(dto.getId());
            analysisPassed = true;
        } catch (Exception e) {
            log.warn("analyzeReport failed for weekly reportId={}", dto.getId(), e);
        }

        try {
            String details = analysisPassed ? "ANALYZED_WITH_AI" : "CREATED_NO_AI";
            reportMapper.insertGenerationLog(userId, "WEEKLY", null, from, to, "USER",
                    analysisPassed ? "CREATED_WITH_AI" : "CREATED", dto.getId(), details);
        } catch (Exception ex) {
            log.error("insertGenerationLog failed for weekly reportId={}", dto.getId(), ex);
        }

        ReportDto refreshed = reportMapper.selectReportById(dto.getId());
        if (refreshed != null) {
            refreshed.setMeals(new ArrayList<>());
            try {
                var insights = reportMapper.selectReportInsights(dto.getId());
                refreshed.setInsights(insights);
            } catch (Exception ex) {
                log.warn("failed to load insights for weekly reportId={}", dto.getId(), ex);
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
            log.warn("failed to load insights for getWeeklyReport reportId={}", dto.getId(), ex);
        }
        try {
            var meals = reportMapper.selectReportMeals(dto.getId());
            dto.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getWeeklyReport reportId={}", dto.getId(), ex);
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
            log.warn("failed to load insights for getReportById reportId={}", report.getId(), ex);
        }
        try {
            var meals = reportMapper.selectReportMeals(report.getId());
            report.setMeals(meals);
        } catch (Exception ex) {
            log.warn("failed to load meals for getReportById reportId={}", report.getId(), ex);
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
            log.warn("analyzeReport: failed to load meals for reportId={}", reportId, ex);
        }

        // 기존 insights 로드
        try {
            var existingInsights = reportMapper.selectReportInsights(reportId);
            report.setInsights(existingInsights);
        } catch (Exception ex) {
            log.warn("analyzeReport: failed to load insights for reportId={}", reportId, ex);
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
            log.warn("analyzeReport: failed to load active challenges for reportId={}", reportId, ex);
        }

        // 챌린지 진행도 업데이트
        updateChallengesFromReport(report, activeChallenges);

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
                    log.error("failed to insert insight for reportId={} kind={}", reportId, i.getKind(), ex);
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
    }

    /**
     * 우선적으로 `user_generation_count` 테이블의 값(daily/weekly)을 사용하여
     * 해당 기간의 사용량을 결정합니다. 레코드가 없거나 날짜가 맞지 않을 경우
     * 기존의 로그 카운트로 폴백합니다.
     */
    private int getUsedCountForPeriod(int userId, String type, LocalDate startDate, java.time.LocalDateTime start, java.time.LocalDateTime end) {
        try {
            Map<String, Object> ugc = reportMapper.selectUserGenerationCount(userId);
            if (ugc != null && !ugc.isEmpty()) {
                if ("WEEKLY".equalsIgnoreCase(type)) {
                    Object wf = ugc.get("weeklyFrom");
                    Object wu = ugc.get("weeklyUsed");
                    if (wf instanceof java.time.LocalDate && ((java.time.LocalDate) wf).equals(startDate)) {
                        return wu instanceof Integer ? (Integer) wu : 0;
                    }
                } else {
                    Object dd = ugc.get("dailyDate");
                    Object du = ugc.get("dailyUsed");
                    if (dd instanceof java.time.LocalDate && ((java.time.LocalDate) dd).equals(startDate)) {
                        return du instanceof Integer ? (Integer) du : 0;
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("getUsedCountForPeriod: failed to read user_generation_count, fallback to logs", ex);
        }

        try {
            return reportMapper.countGenerationLogsInPeriod(userId, type.toUpperCase(), start, end, "USER");
        } catch (Exception ex) {
            log.warn("countGenerationLogsInPeriod failed, returning 0", ex);
            return 0;
        }
    }

    /**
     * 리포트 데이터를 기반으로 챌린지 진행도 업데이트
     */
    private void updateChallengesFromReport(ReportDto report, List<ChallengeDto> activeChallenges) {
        try {
            if (activeChallenges == null || activeChallenges.isEmpty()) {
                log.debug("[ReportService] No active challenges to update");
                return;
            }

            // 리포트 날짜 결정
            LocalDate logDate = report.getDate() != null ? report.getDate() :
                    report.getToDate() != null ? report.getToDate() : LocalDate.now();

            // reportData 구성 (챌린지 분석용)
            Map<String, Object> reportData = new HashMap<>();
            reportData.put("totalCalories", report.getTotalCalories());
            reportData.put("totalProtein", report.getProteinG());
            reportData.put("totalCarb", report.getCarbG());
            reportData.put("totalFat", report.getFatG());
            reportData.put("mealCount", report.getMealCount());

            log.info("[ReportService] Updating {} challenges with reportData: {}",
                    activeChallenges.size(), reportData);

            // 각 챌린지별로 dailyLog 기록
            for (ChallengeDto challenge : activeChallenges) {
                try {
                    challengeService.recordDailyLog(challenge.getId(), logDate, reportData);

                    log.info("[ReportService] Challenge updated - id={}, date={}, type={}",
                            challenge.getId(), logDate, challenge.getGoalType());

                } catch (Exception e) {
                    log.error("[ReportService] Failed to update challenge id={}",
                            challenge.getId(), e);
                }
            }

        } catch (Exception e) {
            log.error("[ReportService] Failed to update challenges from report", e);
        }
    }
}