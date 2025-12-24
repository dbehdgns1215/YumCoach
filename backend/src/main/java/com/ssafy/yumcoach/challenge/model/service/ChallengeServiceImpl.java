package com.ssafy.yumcoach.challenge.model.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.yumcoach.challenge.model.Challenge;
import com.ssafy.yumcoach.challenge.model.ChallengeDailyLog;
import com.ssafy.yumcoach.challenge.model.ChallengeItem;
import com.ssafy.yumcoach.challenge.model.mapper.ChallengeMapper;
import com.ssafy.yumcoach.challenge.model.ChallengeCreateRequest;
import com.ssafy.yumcoach.challenge.model.ChallengeDailyLogDto;
import com.ssafy.yumcoach.challenge.model.ChallengeDto;
import com.ssafy.yumcoach.challenge.model.ChallengeItemDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChallengeServiceImpl implements ChallengeService {

    private final ChallengeMapper challengeMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public Long createChallenge(Integer userId, ChallengeCreateRequest request) {
        log.info("[ChallengeService] createChallenge userId={}, request={}", userId, request);

        // 1. Challenge 엔티티 생성
        LocalDate startDate = LocalDate.parse(request.getStartDate());
        LocalDate endDate = startDate.plusDays(request.getDurationDays() - 1);

        String goalDetailsJson = null;
        try {
            goalDetailsJson = objectMapper.writeValueAsString(request.getGoalDetails());
        } catch (JsonProcessingException e) {
            log.error("goalDetails JSON 변환 실패", e);
            goalDetailsJson = "{}";
        }

        Challenge challenge = Challenge.builder()
                .userId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .goalType(request.getGoalType())
                .goalDetails(goalDetailsJson)
                .startDate(startDate)
                .endDate(endDate)
                .status("ACTIVE")
                .currentStreak(0)
                .maxStreak(0)
                .totalSuccessDays(0)
                .achievementRate(BigDecimal.ZERO)
                .progressRate(BigDecimal.ZERO)
                .source(request.getSource() != null ? request.getSource() : "MANUAL")
                .sourceId(request.getSourceId())
                .aiGenerated(false)
                .build();

        challengeMapper.insertChallenge(challenge);
        Long challengeId = challenge.getId();

        log.info("[ChallengeService] Challenge created with id={}", challengeId);

        // 2. ChallengeItems 저장
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (ChallengeCreateRequest.ItemRequest itemReq : request.getItems()) {
                ChallengeItem item = ChallengeItem.builder()
                        .challengeId(challengeId)
                        .itemText(itemReq.getText())
                        .itemType("ACTION")
                        .orderIdx(itemReq.getOrder())
                        .done(false)
                        .build();
                challengeMapper.insertChallengeItem(item);
            }
            log.info("[ChallengeService] {} items created for challengeId={}", request.getItems().size(), challengeId);
        }

        return challengeId;
    }

    @Override
    public ChallengeDto getChallengeById(Long challengeId, Integer userId) {
        log.debug("[ChallengeService] getChallengeById challengeId={}, userId={}", challengeId, userId);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null || !challenge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("챌린지를 찾을 수 없거나 권한이 없습니다.");
        }

        return convertToDto(challenge);
    }

    @Override
    public List<ChallengeDto> getChallengesByUserId(Integer userId) {
        log.debug("[ChallengeService] getChallengesByUserId userId={}", userId);

        List<Challenge> challenges = challengeMapper.selectChallengesByUserId(userId);
        return challenges.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChallengeDto> getActiveChallenges(Integer userId) {
        log.debug("[ChallengeService] getActiveChallenges userId={}", userId);

        List<Challenge> challenges = challengeMapper.selectActiveChallengesByUserId(userId);
        if (challenges == null || challenges.isEmpty()) {
            log.debug("[ChallengeService] selectActiveChallengesByUserId returned empty for userId={}", userId);
            return Collections.emptyList();
        }
        String ids = challenges.stream().map(c -> String.valueOf(c.getId())).collect(Collectors.joining(","));
        log.debug("[ChallengeService] selectActiveChallengesByUserId returned {} rows for userId={}, ids={}", challenges.size(), userId, ids);
        return challenges.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateChallenge(Long challengeId, Integer userId, ChallengeCreateRequest request) {
        log.info("[ChallengeService] updateChallenge challengeId={}, userId={}", challengeId, userId);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null || !challenge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("챌린지를 찾을 수 없거나 권한이 없습니다.");
        }

        // 기본 정보 업데이트
        challenge.setTitle(request.getTitle());
        challenge.setDescription(request.getDescription());
        challenge.setGoalType(request.getGoalType());

        String goalDetailsJson = null;
        try {
            goalDetailsJson = objectMapper.writeValueAsString(request.getGoalDetails());
        } catch (JsonProcessingException e) {
            log.error("goalDetails JSON 변환 실패", e);
        }
        challenge.setGoalDetails(goalDetailsJson);

        challengeMapper.updateChallenge(challenge);

        // 기존 아이템 삭제 후 재생성
        if (request.getItems() != null) {
            challengeMapper.deleteChallengeItemsByChallengeId(challengeId);
            for (ChallengeCreateRequest.ItemRequest itemReq : request.getItems()) {
                ChallengeItem item = ChallengeItem.builder()
                        .challengeId(challengeId)
                        .itemText(itemReq.getText())
                        .itemType("ACTION")
                        .orderIdx(itemReq.getOrder())
                        .done(false)
                        .build();
                challengeMapper.insertChallengeItem(item);
            }
        }

        log.info("[ChallengeService] Challenge updated challengeId={}", challengeId);
    }

    @Override
    @Transactional
    public void completeChallenge(Long challengeId, Integer userId) {
        log.info("[ChallengeService] completeChallenge challengeId={}, userId={}", challengeId, userId);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null || !challenge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("챌린지를 찾을 수 없거나 권한이 없습니다.");
        }

        challengeMapper.updateChallengeStatus(challengeId, "COMPLETED");
        log.info("[ChallengeService] Challenge completed challengeId={}", challengeId);
    }

    @Override
    @Transactional
    public void deleteChallenge(Long challengeId, Integer userId) {
        log.info("[ChallengeService] deleteChallenge challengeId={}, userId={}", challengeId, userId);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null || !challenge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("챌린지를 찾을 수 없거나 권한이 없습니다.");
        }

        // 연관된 아이템, 로그도 함께 삭제 (CASCADE)
        challengeMapper.deleteChallengeItemsByChallengeId(challengeId);
        challengeMapper.deleteDailyLogsByChallengeId(challengeId);
        challengeMapper.deleteChallenge(challengeId);

        log.info("[ChallengeService] Challenge deleted challengeId={}", challengeId);
    }
    @Override
    @Transactional
    public void toggleChallengeItem(Long itemId, Integer userId, Boolean done) {
        log.info("[ChallengeService] toggleChallengeItem itemId={}, done={}", itemId, done);

        // 🔥 권한 체크 추가
        ChallengeItem existingItem = challengeMapper.selectItemById(itemId);
        if (existingItem == null) {
            throw new IllegalArgumentException("항목을 찾을 수 없습니다.");
        }

        Challenge challenge = challengeMapper.selectChallengeById(existingItem.getChallengeId());
        if (challenge == null || !challenge.getUserId().equals(userId)) {
            throw new IllegalArgumentException("권한이 없습니다.");
        }

        // 아이템 업데이트
        ChallengeItem item = ChallengeItem.builder()
                .id(itemId)
                .done(done)
                .doneAt(done ? java.time.LocalDateTime.now() : null)
                .build();

        challengeMapper.updateChallengeItem(item);

        // 🔥 실시간 진행도 업데이트
        // 오늘 날짜로 DailyLog 재계산
        LocalDate today = LocalDate.now();
        recordDailyLog(challenge.getId(), today, new HashMap<>());

        log.info("[ChallengeService] Item toggled and progress updated - itemId={}, challengeId={}",
                itemId, challenge.getId());
    }

    @Override
    public Long selectChallengeIdByItemId(Long itemId) {
        ChallengeItem item = challengeMapper.selectItemById(itemId);
        return item != null ? item.getChallengeId() : null;
    }

    @Override
    @Transactional
    public void recordDailyLog(Long challengeId, LocalDate logDate, Map<String, Object> reportData) {
        log.info("[ChallengeService] recordDailyLog challengeId={}, logDate={}", challengeId, logDate);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null) {
            log.warn("[ChallengeService] Challenge not found: {}", challengeId);
            return;
        }

        // 목표값 파싱
        Map<String, Object> goalDetails = null;
        try {
            goalDetails = objectMapper.readValue(challenge.getGoalDetails(), Map.class);
        } catch (JsonProcessingException e) {
            log.error("goalDetails 파싱 실패", e);
            return;
        }

        // 목표 타입별 달성 여부 계산
        String targetValue = extractTargetValue(challenge.getGoalType(), goalDetails);
        String actualValue;
        boolean isAchieved;
        BigDecimal achievementRate;

        // 수동 체크 타입 (EXERCISE, HABIT)
        if (isManualCheckType(challenge.getGoalType())) {
            // 체크리스트 항목 완료 여부로 판단
            List<ChallengeItem> items = challengeMapper.selectItemsByChallengeId(challengeId);
            int totalItems = items.size();
            int doneItems = (int) items.stream().filter(ChallengeItem::getDone).count();

            actualValue = doneItems + "/" + totalItems + " 완료";
            isAchieved = totalItems > 0 && doneItems == totalItems; // 모두 체크해야 달성
            achievementRate = totalItems > 0
                    ? BigDecimal.valueOf((doneItems * 100.0) / totalItems).setScale(2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
        }
        // 자동 추적 타입 (PROTEIN, CALORIE, CARBS, FAT)
        else {
            actualValue = extractActualValue(challenge.getGoalType(), reportData);
            isAchieved = checkAchievement(challenge.getGoalType(), goalDetails, reportData);
            achievementRate = calculateAchievementRate(challenge.getGoalType(), goalDetails, reportData);
        }

        // 기존 로그 확인
        ChallengeDailyLog existingLog = challengeMapper.selectDailyLog(challengeId, logDate);

        String reportDataJson = null;
        try {
            reportDataJson = objectMapper.writeValueAsString(reportData);
        } catch (JsonProcessingException e) {
            log.error("reportData JSON 변환 실패", e);
        }

        if (existingLog == null) {
            // 신규 로그 생성
            ChallengeDailyLog newLog = ChallengeDailyLog.builder()
                    .challengeId(challengeId)
                    .logDate(logDate)
                    .targetValue(targetValue)
                    .actualValue(actualValue)
                    .isAchieved(isAchieved)
                    .achievementRate(achievementRate)
                    .reportData(reportDataJson)
                    .build();
            challengeMapper.insertDailyLog(newLog);
        } else {
            // 기존 로그 업데이트
            existingLog.setActualValue(actualValue);
            existingLog.setIsAchieved(isAchieved);
            existingLog.setAchievementRate(achievementRate);
            existingLog.setReportData(reportDataJson);
            challengeMapper.updateDailyLog(existingLog);
        }

        // 챌린지 진척도 업데이트
        updateChallengeProgress(challengeId);
    }

    private boolean isManualCheckType(String goalType) {
        return "EXERCISE".equals(goalType) || "HABIT".equals(goalType);
    }

    @Override
    @Transactional
    public void updateChallengeProgress(Long challengeId) {
        log.debug("[ChallengeService] updateChallengeProgress challengeId={}", challengeId);

        Challenge challenge = challengeMapper.selectChallengeById(challengeId);
        if (challenge == null) {
            log.warn("챌린지 없음: {}", challengeId);
            return;
        }

        // 전체 로그 조회
        List<ChallengeDailyLog> allLogs = challengeMapper.selectRecentLogs(challengeId, 365);

        // 연속 달성 계산
        int currentStreak = calculateCurrentStreak(allLogs);
        int maxStreak = calculateMaxStreak(allLogs);

        // 총 성공 일수
        int totalSuccessDays = (int) allLogs.stream()
                .filter(ChallengeDailyLog::getIsAchieved)
                .count();

        // 🔥 개선: 2가지 지표 계산
        
        // 1. 달성률 (Achievement Rate): 작성한 리포트 중 성공 비율
        double achievementRate = 0.0;
        int totalReports = allLogs.size();
        if (totalReports > 0) {
            achievementRate = (totalSuccessDays * 100.0) / totalReports;
        }

        // 2. 진행도 (Progress Rate): 전체 기간 중 경과 비율
        double progressRate = 0.0;
        LocalDate today = LocalDate.now();
        LocalDate startDate = challenge.getStartDate();
        LocalDate endDate = challenge.getEndDate();
        
        if (!today.isBefore(startDate)) {
            long elapsedDays = java.time.temporal.ChronoUnit.DAYS.between(startDate, today) + 1;
            long totalDays = challenge.getDurationDays();
            
            // 종료일 지났으면 100%
            if (today.isAfter(endDate)) {
                progressRate = 100.0;
            } else {
                progressRate = Math.min(100.0, (elapsedDays * 100.0) / totalDays);
            }
        }

        log.info("[ChallengeService] Progress calculated - challengeId={}, " +
                        "reports={}, successDays={}, achievementRate={:.1f}%, " +
                        "progressRate={:.1f}%, currentStreak={}, maxStreak={}",
                challengeId, totalReports, totalSuccessDays, achievementRate,
                progressRate, currentStreak, maxStreak);

        // 🔥 수정: progressRate 파라미터 추가
        challengeMapper.updateChallengeProgress(
                challengeId,
                currentStreak,
                maxStreak,
                totalSuccessDays,
                achievementRate,
                progressRate
        );
    }

    // ===== Private Helper Methods =====

    private ChallengeDto convertToDto(Challenge challenge) {
        // Items 조회
        List<ChallengeItem> items = challengeMapper.selectItemsByChallengeId(challenge.getId());
        List<ChallengeItemDto> itemDtos = items.stream()
                .map(item -> ChallengeItemDto.builder()
                        .id(item.getId())
                        .challengeId(item.getChallengeId())
                        .text(item.getItemText())
                        .itemType(item.getItemType())
                        .targetDate(item.getTargetDate())
                        .order(item.getOrderIdx())
                        .done(item.getDone())
                        .doneAt(item.getDoneAt())
                        .build())
                .collect(Collectors.toList());

        // Recent logs 조회
        List<ChallengeDailyLog> logs = challengeMapper.selectRecentLogs(challenge.getId(), 7);
        List<ChallengeDailyLogDto> logDtos = logs.stream()
                .map(log -> ChallengeDailyLogDto.builder()
                        .id(log.getId())
                        .challengeId(log.getChallengeId())
                        .logDate(log.getLogDate())
                        .targetValue(log.getTargetValue())
                        .actualValue(log.getActualValue())
                        .isAchieved(log.getIsAchieved())
                        .achievementRate(log.getAchievementRate())
                        .aiFeedback(log.getAiFeedback())
                        .build())
                .collect(Collectors.toList());

        return ChallengeDto.builder()
                .id(challenge.getId())
                .userId(challenge.getUserId())
                .title(challenge.getTitle())
                .description(challenge.getDescription())
                .goalType(challenge.getGoalType())
                .goalDetails(challenge.getGoalDetails())
                .startDate(challenge.getStartDate())
                .endDate(challenge.getEndDate())
                .durationDays(challenge.getDurationDays())
                .status(challenge.getStatus())
                .currentStreak(challenge.getCurrentStreak())
                .maxStreak(challenge.getMaxStreak())
                .totalSuccessDays(challenge.getTotalSuccessDays())
                .achievementRate(challenge.getAchievementRate())
                .progressRate(challenge.getProgressRate())
                .source(challenge.getSource())
                .sourceId(challenge.getSourceId())
                .aiGenerated(challenge.getAiGenerated())
                .createdAt(challenge.getCreatedAt())
                .updatedAt(challenge.getUpdatedAt())
                .completedAt(challenge.getCompletedAt())
                .items(itemDtos)
                .recentLogs(logDtos)
                .build();
    }

    private String extractTargetValue(String goalType, Map<String, Object> goalDetails) {
        switch (goalType) {
            case "PROTEIN":
                return goalDetails.getOrDefault("protein", "0g").toString();
            case "CALORIE":
                return goalDetails.getOrDefault("calories", "0kcal").toString();
            case "WEIGHT":
                return goalDetails.getOrDefault("weight", "0kg").toString();
            case "WATER":
                return goalDetails.getOrDefault("water", "0L").toString();
            default:
                return goalDetails.toString();
        }
    }

    private String extractActualValue(String goalType, Map<String, Object> reportData) {
        switch (goalType) {
            case "PROTEIN":
                return reportData.getOrDefault("totalProtein", 0) + "g";
            case "CALORIE":
                return reportData.getOrDefault("totalCalories", 0) + "kcal";
            case "WATER":
                return reportData.getOrDefault("waterIntake", 0) + "L";
            default:
                return "N/A";
        }
    }

    private boolean checkAchievement(String goalType, Map<String, Object> goalDetails, Map<String, Object> reportData) {
        switch (goalType) {
            case "PROTEIN":
                int targetProtein = parseNumber(goalDetails.getOrDefault("protein", "0g").toString());
                int actualProtein = (int) reportData.getOrDefault("totalProtein", 0);
                return actualProtein >= targetProtein;

            case "CALORIE":
                int targetCalories = parseNumber(goalDetails.getOrDefault("calories", "0kcal").toString());
                int actualCalories = (int) reportData.getOrDefault("totalCalories", 0);
                return Math.abs(actualCalories - targetCalories) <= (targetCalories * 0.1); // ±10%

            case "WATER":
                double targetWater = parseDouble(goalDetails.getOrDefault("water", "0L").toString());
                double actualWater = (double) reportData.getOrDefault("waterIntake", 0.0);
                return actualWater >= targetWater;

            default:
                return false;
        }
    }

    private BigDecimal calculateAchievementRate(String goalType, Map<String, Object> goalDetails, Map<String, Object> reportData) {
        switch (goalType) {
            case "PROTEIN":
                int targetProtein = parseNumber(goalDetails.getOrDefault("protein", "0g").toString());
                int actualProtein = (int) reportData.getOrDefault("totalProtein", 0);
                if (targetProtein == 0) return BigDecimal.ZERO;
                return BigDecimal.valueOf((actualProtein * 100.0) / targetProtein).setScale(2, RoundingMode.HALF_UP);

            case "CALORIE":
                int targetCalories = parseNumber(goalDetails.getOrDefault("calories", "0kcal").toString());
                int actualCalories = (int) reportData.getOrDefault("totalCalories", 0);
                if (targetCalories == 0) return BigDecimal.ZERO;
                return BigDecimal.valueOf((actualCalories * 100.0) / targetCalories).setScale(2, RoundingMode.HALF_UP);

            default:
                return BigDecimal.ZERO;
        }
    }

    private int parseNumber(String value) {
        return Integer.parseInt(value.replaceAll("[^0-9]", ""));
    }

    private double parseDouble(String value) {
        return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
    }

    private int calculateCurrentStreak(List<ChallengeDailyLog> logs) {
        if (logs.isEmpty()) return 0;

        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        for (ChallengeDailyLog log : logs) {
            if (log.getLogDate().equals(expectedDate) && log.getIsAchieved()) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }
        return streak;
    }

    private int calculateMaxStreak(List<ChallengeDailyLog> logs) {
        int maxStreak = 0;
        int currentStreak = 0;

        for (ChallengeDailyLog log : logs) {
            if (log.getIsAchieved()) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else {
                currentStreak = 0;
            }
        }
        return maxStreak;
    }
}