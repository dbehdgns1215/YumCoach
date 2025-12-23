package com.ssafy.yumcoach.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportMealDto {
    /**
     * 리포트에 포함되는 한 식사 항목의 요약 정보
     *
     * - `mealId`: 원본 `meal.id` (nullable)
     * - `mealTime`: 해당 끼니의 시각
     * - `calories`/`proteinG`/`carbG`/`fatG`: 해당 음식의 합산 영양소 (정수, 반올림)
     *
     * 이 객체는 `report_meal` 테이블과 매핑됩니다.
     */
    private Integer id;
    private Integer reportId;
    private Integer mealId; // original meal id
    private LocalDateTime mealTime;
    private Integer calories;
    private Integer proteinG;
    private Integer carbG;
    private Integer fatG;
}
