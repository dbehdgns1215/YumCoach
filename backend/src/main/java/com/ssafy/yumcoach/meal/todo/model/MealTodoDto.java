package com.ssafy.yumcoach.meal.todo.model;

import com.ssafy.yumcoach.meal.enums.MealType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class MealTodoDto {
    private Long id;
    private Integer userId;

    private MealType mealType;
    private String foodCode;
    private String foodName;

    private Integer defaultGrams;
    private LocalDateTime createdAt;
}
