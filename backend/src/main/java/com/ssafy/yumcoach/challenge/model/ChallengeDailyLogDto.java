package com.ssafy.yumcoach.challenge.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeDailyLogDto {
    private Long id;
    private Long challengeId;
    private LocalDate logDate;

    private String targetValue;
    private String actualValue;

    private Boolean isAchieved;
    private BigDecimal achievementRate;

    private String aiFeedback;
}