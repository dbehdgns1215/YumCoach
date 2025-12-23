package com.ssafy.yumcoach.report.service;

import com.ssafy.yumcoach.ai.OpenAiService;
import com.ssafy.yumcoach.report.model.ReportDto;
import com.ssafy.yumcoach.report.model.ReportMealDto;
import com.ssafy.yumcoach.report.model.mapper.ReportMapper;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import com.ssafy.yumcoach.meal.model.MealItemDto;
import com.ssafy.yumcoach.meal.model.mapper.MealMapper;
import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.mapper.FoodMapper;
import com.ssafy.yumcoach.user.model.User;
import com.ssafy.yumcoach.user.model.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.ssafy.yumcoach.report.model.ReportInsightDto;

/**
 * Report 관련 서비스 구현체
 *
 * 본 구현체는 다음 책임을 가집니다:
 * - 사용자/시스템에 의한 리포트 생성(일별/주간)
 * - 생성 제한 확인 및 `report_generation_log` 기록
 * - `meal` 및 `nutrition` 데이터를 조회하여 요약 영양소 집계
 * - `report` 및 `report_meal` 저장
 *
 * 주의:
 * - 현재 구현은 단순한 동기식 생성 방식입니다. 대용량 데이터/성능 튜닝이 필요하면 비동기 배치로 전환하세요.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportServiceImpl implements ReportService {

    private final ReportMapper reportMapper;
    private final MealMapper mealMapper;
    private final FoodMapper foodMapper;
    private final UserMapper userMapper;
    private final OpenAiService openAiService;

    /**
     * 사용자 요청으로 일별 리포트를 생성합니다.
     *
     * 처리 순서:
     * 1. 사용자 권한/롤에 따른 생성 한도 검사
     * 2. `report` 레코드 생성(status=PROGRESS)
     * 3. 해당 날짜의 `meal` 항목들을 조회하여 음식별 영양소를 합산
     * 4. `report_meal`에 요약을 저장
     * 5. `report_generation_log`에 결과 기록
     *
     * 한도 초과 시 `IllegalStateException("LIMIT_EXCEEDED")` 를 던집니다.
     */
    @Override
    public ReportDto createDailyReport(int userId, LocalDate date) {
        log.info("createDailyReport user={}, date={}", userId, date);
        User user = userMapper.findById(userId);
        int dailyLimit;
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            dailyLimit = 1000; // 관리자 예외: 매우 높은 한도
        } else if (user != null && "ADVANCED".equalsIgnoreCase(user.getRole())) {
            dailyLimit = 2;
        } else {
            dailyLimit = 1;
        }
        java.time.LocalDateTime start = date.atStartOfDay();
        java.time.LocalDateTime end = date.plusDays(1).atStartOfDay().minusSeconds(1);
        int existing = reportMapper.countGenerationLogsInPeriod(userId, "DAILY", start, end, "USER");
        if (existing >= dailyLimit) {
            reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER", "LIMIT_EXCEEDED", null, "daily limit exceeded");
            throw new IllegalStateException("LIMIT_EXCEEDED");
        }

        ReportDto dto = new ReportDto();
        dto.setUserId(userId);
        dto.setDate(date);
        dto.setType("DAILY");
        dto.setStatus("PROGRESS");
        dto.setCreatedBy("USER");

        reportMapper.insertReport(dto);

        java.util.List<MealLogDto> logs = mealMapper.selectMealLogsByUserAndDateRange(userId, date, date);
        log.info("found meal logs count={} for user={} date={}", logs != null ? logs.size() : 0, userId, date);
        int totalCalories = 0, totalProtein = 0, totalCarb = 0, totalFat = 0, mealCount = 0;
        // 디버그: 각 로그별 아이템 수 출력(문제 원인 조사용)
        if (logs != null) {
            for (MealLogDto l : logs) {
                log.debug("meal history id={} date={} itemsSize={}", l.getId(), l.getDate(), l.getItems() == null ? 0 : l.getItems().size());
            }
        }
        java.util.List<ReportMealDto> reportMeals = new java.util.ArrayList<>();
        for (MealLogDto log : logs) {
            if (log.getItems() == null) continue;
            for (MealItemDto item : log.getItems()) {
                if (item.getMealCode() == null) continue;
                FoodDetailDto fd = foodMapper.selectFoodDetailById(item.getMealCode());
                if (fd == null || fd.getNutrition() == null || item.getAmount() == null) continue;
                double factor = item.getAmount().doubleValue() / 100.0;
                int kcal = (int) Math.round((fd.getNutrition().getEnergyKcal() != null ? fd.getNutrition().getEnergyKcal() : 0.0) * factor);
                int protein = (int) Math.round((fd.getNutrition().getProteinG() != null ? fd.getNutrition().getProteinG() : 0.0) * factor);
                int carb = (int) Math.round((fd.getNutrition().getCarbohydrateG() != null ? fd.getNutrition().getCarbohydrateG() : 0.0) * factor);
                int fat = (int) Math.round((fd.getNutrition().getFatG() != null ? fd.getNutrition().getFatG() : 0.0) * factor);
                totalCalories += kcal;
                totalProtein += protein;
                totalCarb += carb;
                totalFat += fat;
                mealCount++;

                ReportMealDto rm = new ReportMealDto();
                rm.setReportId(dto.getId());
                rm.setMealId(item.getId() != null ? item.getId().intValue() : null);
                rm.setMealTime(null);
                rm.setCalories(kcal);
                rm.setProteinG(protein);
                rm.setCarbG(carb);
                rm.setFatG(fat);
                reportMapper.insertReportMeal(rm);
                reportMeals.add(rm);
            }
        }

        dto.setTotalCalories(totalCalories);
        dto.setProteinG(totalProtein);
        dto.setCarbG(totalCarb);
        dto.setFatG(totalFat);
        dto.setMealCount(mealCount);
        // 식사 데이터가 없으면 명확한 예외를 던집니다.
        if (mealCount == 0) {
            reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER", "NO_DATA", dto.getId(), "no meals");
            throw new IllegalStateException("NO_MEALS");
        }

        // AI 분석을 시도하기 전에 DTO에 meals 목록을 채웁니다.
        dto.setMeals(reportMeals);

        // AI 분석을 시도하여 결과를 리포트에 포함합니다. 실패 시에도 리포트는 생성됩니다.
        boolean aiOk = false;
        try {
            openAiService.analyzeReport(dto);
            aiOk = true;
        } catch (Exception ex) {
            log.warn("AI 분석 실패(일별): {}", ex.getMessage());
        }

        String result = aiOk ? "CREATED_WITH_AI" : "CREATED_NO_AI";
        reportMapper.insertGenerationLog(userId, "DAILY", date, null, null, "USER", result, dto.getId(), aiOk ? "ok" : "ai_failed");
        return dto;
    }

    /**
     * 일별 리포트 조회
     */
    @Override
    public ReportDto getDailyReport(int userId, LocalDate date) {
        log.info("getDailyReport user={}, date={}", userId, date);
        ReportDto dto = reportMapper.selectReportByUserTypeDate(userId, "DAILY", date);
        if (dto == null) return null;
        // load persisted insights
        try {
            java.util.List<ReportInsightDto> insights = reportMapper.selectReportInsights(dto.getId());
            if (insights != null && !insights.isEmpty()) {
                dto.setInsights(insights);
            }
            // Regardless of whether insights exist, parse aiResponse to fill coach/next/score if missing
            if (dto.getAiResponse() != null) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode root = mapper.readTree(dto.getAiResponse());
                    // only insert insights if DB had none
                    if ((dto.getInsights() == null || dto.getInsights().isEmpty()) && root.has("insights") && root.get("insights").isArray()) {
                        java.util.List<ReportInsightDto> parsed = new java.util.ArrayList<>();
                        for (JsonNode n : root.get("insights")) {
                            String kind = n.has("kind") ? n.get("kind").asText() : null;
                            String title = n.has("title") ? n.get("title").asText() : null;
                            String body = n.has("body") ? n.get("body").asText() : null;
                            if (kind != null && body != null) {
                                try { reportMapper.insertReportInsight(dto.getId(), kind, title, body); } catch (Exception ex) { log.warn("insertReportInsight failed: {}", ex.getMessage()); }
                                parsed.add(new ReportInsightDto(null, dto.getId(), kind, title, body));
                            }
                        }
                        if (!parsed.isEmpty()) dto.setInsights(parsed);
                    }
                    if ((dto.getCoachMessage() == null || dto.getCoachMessage().isBlank()) && root.has("coachMessage")) {
                        String coach = root.get("coachMessage").asText();
                        dto.setCoachMessage(coach);
                        try { reportMapper.updateReportCoachMessage(dto.getId(), coach); } catch (Exception ex) { log.warn("updateReportCoachMessage failed: {}", ex.getMessage()); }
                    }
                    if ((dto.getNextAction() == null || dto.getNextAction().isBlank()) && root.has("nextAction")) {
                        String next = root.get("nextAction").asText();
                        dto.setNextAction(next);
                        try { reportMapper.updateReportNextAction(dto.getId(), next); } catch (Exception ex) { log.warn("updateReportNextAction failed: {}", ex.getMessage()); }
                    }
                    if (root.has("score") && dto.getScore() == null) {
                        try { dto.setScore(root.get("score").asInt()); } catch (Exception ex) { /* ignore */ }
                    }
                } catch (Exception pe) {
                    log.warn("aiResponse JSON parse failed for report {}: {}", dto.getId(), pe.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("인사이트 로드 실패(일별): {}", ex.getMessage());
        }
        return dto;
    }

    /**
     * 주간 리포트 생성 - 구현은 일별과 동일한 패턴을 따릅니다.
     */
    @Override
    public ReportDto createWeeklyReport(int userId, LocalDate fromDate, LocalDate toDate) {
        log.info("createWeeklyReport user={}, from={}, to={}", userId, fromDate, toDate);
        User user = userMapper.findById(userId);
        int weeklyLimit;
        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
            weeklyLimit = 1000; // 관리자 예외: 매우 높은 한도
        } else if (user != null && "ADVANCED".equalsIgnoreCase(user.getRole())) {
            weeklyLimit = 10;
        } else {
            weeklyLimit = 5;
        }
        java.time.LocalDateTime start = fromDate.atStartOfDay();
        java.time.LocalDateTime end = toDate.plusDays(1).atStartOfDay().minusSeconds(1);
        int existing = reportMapper.countGenerationLogsInPeriod(userId, "WEEKLY", start, end, "USER");
        if (existing >= weeklyLimit) {
            reportMapper.insertGenerationLog(userId, "WEEKLY", null, fromDate, toDate, "USER", "LIMIT_EXCEEDED", null, "weekly limit exceeded");
            throw new IllegalStateException("LIMIT_EXCEEDED");
        }

        ReportDto dto = new ReportDto();
        dto.setUserId(userId);
        dto.setFromDate(fromDate);
        dto.setToDate(toDate);
        dto.setType("WEEKLY");
        dto.setStatus("PROGRESS");
        // 생성 주체를 명시합니다. 사용자 요청으로 생성되는 경우 'USER'로 설정해야
        // DB의 NOT NULL 제약조건(created_by)을 만족시킵니다.
        dto.setCreatedBy("USER");

        reportMapper.insertReport(dto);

        java.util.List<MealLogDto> logs = mealMapper.selectMealLogsByUserAndDateRange(userId, fromDate, toDate);
        log.info("found meal logs count={} for user={} from={} to={}", logs != null ? logs.size() : 0, userId, fromDate, toDate);
        int totalCalories = 0, totalProtein = 0, totalCarb = 0, totalFat = 0, mealCount = 0;
        if (logs != null) {
            for (MealLogDto l : logs) {
                log.debug("meal history id={} date={} itemsSize={}", l.getId(), l.getDate(), l.getItems() == null ? 0 : l.getItems().size());
            }
        }
        for (MealLogDto log : logs) {
            if (log.getItems() == null) continue;
            for (MealItemDto item : log.getItems()) {
                if (item.getMealCode() == null) continue;
                FoodDetailDto fd = foodMapper.selectFoodDetailById(item.getMealCode());
                if (fd == null || fd.getNutrition() == null || item.getAmount() == null) continue;
                double factor = item.getAmount().doubleValue() / 100.0;
                int kcal = (int) Math.round((fd.getNutrition().getEnergyKcal() != null ? fd.getNutrition().getEnergyKcal() : 0.0) * factor);
                int protein = (int) Math.round((fd.getNutrition().getProteinG() != null ? fd.getNutrition().getProteinG() : 0.0) * factor);
                int carb = (int) Math.round((fd.getNutrition().getCarbohydrateG() != null ? fd.getNutrition().getCarbohydrateG() : 0.0) * factor);
                int fat = (int) Math.round((fd.getNutrition().getFatG() != null ? fd.getNutrition().getFatG() : 0.0) * factor);
                totalCalories += kcal;
                totalProtein += protein;
                totalCarb += carb;
                totalFat += fat;
                mealCount++;

                ReportMealDto rm = new ReportMealDto();
                rm.setReportId(dto.getId());
                rm.setMealId(item.getId() != null ? item.getId().intValue() : null);
                rm.setMealTime(null);
                rm.setCalories(kcal);
                rm.setProteinG(protein);
                rm.setCarbG(carb);
                rm.setFatG(fat);
                reportMapper.insertReportMeal(rm);
            }
        }

        dto.setTotalCalories(totalCalories);
        dto.setProteinG(totalProtein);
        dto.setCarbG(totalCarb);
        dto.setFatG(totalFat);
        dto.setMealCount(mealCount);
        // 식사 데이터가 없으면 명확한 예외를 던집니다.
        if (mealCount == 0) {
            reportMapper.insertGenerationLog(userId, "WEEKLY", null, fromDate, toDate, "USER", "NO_DATA", dto.getId(), "no meals");
            throw new IllegalStateException("NO_MEALS");
        }

        // AI 분석을 시도하여 결과를 리포트에 포함합니다. 실패 시에도 리포트는 생성됩니다.
        boolean aiOk = false;
        try {
            openAiService.analyzeReport(dto);
            aiOk = true;
        } catch (Exception ex) {
            log.warn("AI 분석 실패(주간): {}", ex.getMessage());
        }

        String result = aiOk ? "CREATED_WITH_AI" : "CREATED_NO_AI";
        reportMapper.insertGenerationLog(userId, "WEEKLY", null, fromDate, toDate, "USER", result, dto.getId(), aiOk ? "ok" : "ai_failed");
        return dto;
    }

    /**
     * 주간 리포트 조회
     */
    @Override
    public ReportDto getWeeklyReport(int userId, LocalDate fromDate, LocalDate toDate) {
        log.info("getWeeklyReport user={}, from={}, to={}", userId, fromDate, toDate);
        ReportDto dto = reportMapper.selectReportByUserAndRange(userId, "WEEKLY", fromDate, toDate);
        if (dto == null) return null;
        try {
            java.util.List<ReportInsightDto> insights = reportMapper.selectReportInsights(dto.getId());
            if (insights != null && !insights.isEmpty()) {
                dto.setInsights(insights);
            }
            if (dto.getAiResponse() != null) {
                ObjectMapper mapper = new ObjectMapper();
                try {
                    JsonNode root = mapper.readTree(dto.getAiResponse());
                    if ((dto.getInsights() == null || dto.getInsights().isEmpty()) && root.has("insights") && root.get("insights").isArray()) {
                        java.util.List<ReportInsightDto> parsed = new java.util.ArrayList<>();
                        for (JsonNode n : root.get("insights")) {
                            String kind = n.has("kind") ? n.get("kind").asText() : null;
                            String title = n.has("title") ? n.get("title").asText() : null;
                            String body = n.has("body") ? n.get("body").asText() : null;
                            if (kind != null && body != null) {
                                try { reportMapper.insertReportInsight(dto.getId(), kind, title, body); } catch (Exception ex) { log.warn("insertReportInsight failed: {}", ex.getMessage()); }
                                parsed.add(new ReportInsightDto(null, dto.getId(), kind, title, body));
                            }
                        }
                        if (!parsed.isEmpty()) dto.setInsights(parsed);
                    }
                    if ((dto.getCoachMessage() == null || dto.getCoachMessage().isBlank()) && root.has("coachMessage")) {
                        String coach = root.get("coachMessage").asText();
                        dto.setCoachMessage(coach);
                        try { reportMapper.updateReportCoachMessage(dto.getId(), coach); } catch (Exception ex) { log.warn("updateReportCoachMessage failed: {}", ex.getMessage()); }
                    }
                    if ((dto.getNextAction() == null || dto.getNextAction().isBlank()) && root.has("nextAction")) {
                        String next = root.get("nextAction").asText();
                        dto.setNextAction(next);
                        try { reportMapper.updateReportNextAction(dto.getId(), next); } catch (Exception ex) { log.warn("updateReportNextAction failed: {}", ex.getMessage()); }
                    }
                    if (root.has("score") && dto.getScore() == null) {
                        try { dto.setScore(root.get("score").asInt()); } catch (Exception ex) { /* ignore */ }
                    }
                } catch (Exception pe) {
                    log.warn("aiResponse JSON parse failed for report {}: {}", dto.getId(), pe.getMessage());
                }
            }
        } catch (Exception ex) {
            log.warn("인사이트 로드 실패(주간): {}", ex.getMessage());
        }
        return dto;
    }

    /**
     * ID 기반 리포트 조회 (소유자 확인 포함)
     */
    @Override
    public ReportDto getReportById(int userId, int reportId) {
        log.info("getReportById user={}, reportId={}", userId, reportId);
        ReportDto dto = reportMapper.selectReportById(reportId);
        if (dto == null) return null;
        if (!userIdEquals(dto.getUserId(), userId)) return null;
        // 인사이트를 함께 로드
        try {
            java.util.List<com.ssafy.yumcoach.report.model.ReportInsightDto> insights = reportMapper.selectReportInsights(reportId);
            dto.setInsights(insights);
        } catch (Exception ex) {
            log.warn("인사이트 로드 실패: {}", ex.getMessage());
        }
        return dto;
    }

    private boolean userIdEquals(Integer a, int b) {
        return a != null && a == b;
    }
}
