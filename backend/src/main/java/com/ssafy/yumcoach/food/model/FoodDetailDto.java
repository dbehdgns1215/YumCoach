package com.ssafy.yumcoach.food.model;

import lombok.Data;

/**
 * 한 식품(food_items) + 대표 영양정보(nutrition_facts_primary)를
 * 한 번에 내려주기 위한 DTO
 */
@Data
public class FoodDetailDto {

    private FoodItemDto food;                  // 기본 식품 정보
    private NutritionFactsPrimaryDto nutrition; // 대표 영양 성분
}
