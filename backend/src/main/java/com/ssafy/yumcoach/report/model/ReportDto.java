package com.ssafy.yumcoach.report.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.ssafy.yumcoach.report.model.ReportInsightDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportDto {
    /**
     * 리포트 요약 DTO
     *
     * - `type`: DAILY 또는 WEEKLY
     * - `date` / `fromDate`+`toDate`: 일/주 단위 기간 식별
     * - `status`: PROGRESS 또는 COMPLETED
     *
     * 이 DTO는 프론트엔드에 전달하는 리포트 요약 정보를 담습니다.
     */
    private Integer id;
    private Integer userId;
    private String type; // DAILY | WEEKLY
    private LocalDate date;
    private LocalDate fromDate;
    private LocalDate toDate;
    private String status; // PROGRESS | COMPLETED
    private Integer score;
    private Integer totalCalories;
    private Integer proteinG;
    private Integer carbG;
    private Integer fatG;
    private Integer mealCount;
    private String createdBy; // USER | SYSTEM
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReportMealDto> meals;
    private String aiResponse;
    private List<ReportInsightDto> insights;
    private String coachMessage;
    private String nextAction;
}
