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
    private Integer currentStreak;      // 현재 연속 달성
    private Integer maxStreak;          // 최대 연속 달성
    private Integer totalSuccessDays;   // 총 성공 일수
    private BigDecimal achievementRate; // 달성률 (성공 리포트 / 전체 리포트)
    private BigDecimal progressRate;    // 진행도 (경과일 / 전체 기간)

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