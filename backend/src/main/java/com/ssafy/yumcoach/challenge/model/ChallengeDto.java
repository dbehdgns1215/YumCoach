package com.ssafy.yumcoach.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDto {
    private Long id;
    private Integer userId;

    private String title;
    private String description;

    private String goalType;
    private String goalDetails; // JSON string

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    private String status;
    private Integer currentStreak;      // 현재 연속 달성
    private Integer maxStreak;          // 최대 연속 달성
    private Integer totalSuccessDays;   // 총 성공 일수
    private BigDecimal achievementRate; // 달성률 (성공 리포트 / 전체 리포트)
    private BigDecimal progressRate;    // 진행도 (경과일 / 전체 기간)

    private String source;
    private Long sourceId;

    private Boolean aiGenerated;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    private List<ChallengeItemDto> items;
    private List<ChallengeDailyLogDto> recentLogs;
}