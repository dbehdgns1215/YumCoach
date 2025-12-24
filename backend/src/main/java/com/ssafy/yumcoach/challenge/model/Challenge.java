package com.ssafy.yumcoach.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Challenge {
    private Long id;
    private Integer userId;

    // 기본 정보
    private String title;
    private String description;

    // 목표
    private String goalType; // PROTEIN, CALORIE, WEIGHT, WATER, EXERCISE, HABIT, COMBINED
    private String goalDetails; // JSON string

    // 기간
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;

    // 진척도
    private String status; // PLANNING, ACTIVE, COMPLETED, FAILED, ABANDONED
    private Integer currentStreak;
    private Integer maxStreak;
    private Integer totalSuccessDays;
    private BigDecimal successRate;

    // 생성 출처
    private String source; // CHATBOT, REPORT, MANUAL
    private Long sourceId;
    private String sourceData; // JSON string

    // AI 관련
    private Boolean aiGenerated;
    private String aiPrompt;

    // 메타
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}