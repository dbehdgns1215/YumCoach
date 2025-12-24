package com.ssafy.yumcoach.challenge.entity;

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
public class ChallengeDailyLog {
    private Long id;
    private Long challengeId;
    private LocalDate logDate;

    // 목표 vs 실제
    private String targetValue;
    private String actualValue;

    // 달성 여부
    private Boolean isAchieved;
    private BigDecimal achievementRate;

    // 리포트 연동
    private Long fromReportId;
    private String reportData; // JSON string

    // 피드백
    private String aiFeedback;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}