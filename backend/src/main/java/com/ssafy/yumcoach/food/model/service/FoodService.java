package com.ssafy.yumcoach.food.model.service;

import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.FoodItemDto;

import java.util.List;

public interface FoodService {
    public FoodDetailDto getFoodDetail(String foodId);
    public List<FoodItemDto> searchFood(String keyword, int limit);
}
