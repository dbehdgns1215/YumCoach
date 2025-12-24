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
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer totalSuccessDays;
    private BigDecimal successRate;

    private String source;
    private Long sourceId;

    private Boolean aiGenerated;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    // 🔥 추가: 아이템 리스트
    private List<ChallengeItemDto> items;

    // 🔥 추가: 최근 7일 로그
    private List<ChallengeDailyLogDto> recentLogs;
}