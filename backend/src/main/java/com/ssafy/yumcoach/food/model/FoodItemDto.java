package com.ssafy.yumcoach.food.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FoodItemDto {

    private String foodId;
    private String foodName;

    private String representativeFoodName;

    private String servingSize; // "1포(30g)" 같은 문자열일 가능성, 칼로리 기준
    private String weight;      // g 단위 문자열일 수도 있음, 한 팩(포장) 기준

    private LocalDate dataCreated;
    private LocalDate dataReference;
}
