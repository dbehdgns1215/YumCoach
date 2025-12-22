package com.ssafy.yumcoach.meal.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ssafy.yumcoach.meal.enums.MealType;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// API 응답용 묶음 DTO - 날짜별 끼니 목록
public class MealHistoryResponseDto implements Serializable {

    private Integer userId;
    private LocalDate date;
    private List<MealLogDto> meals; // 하루 식사 전체
}
