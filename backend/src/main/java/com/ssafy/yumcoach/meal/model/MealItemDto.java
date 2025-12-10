package com.ssafy.yumcoach.meal.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
// 한 음식 단위 DTO - 음식 상세 한 줄, meal 테이블
public class MealItemDto implements Serializable {

    // meal.id (PK)
    private Integer id;

    // meal.history_id (FK -> meal_history.id)
    private Integer historyId;

    // meal.meal_code
    private String mealCode;

    // meal.meal_name
    private String mealName;

    // meal.amount (섭취량, g 기준이면 gram 개념)
    private Integer amount;
}
