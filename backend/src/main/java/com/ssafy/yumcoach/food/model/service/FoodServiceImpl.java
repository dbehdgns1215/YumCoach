package com.ssafy.yumcoach.food.model.service;

import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.FoodItemDto;
import com.ssafy.yumcoach.food.model.mapper.FoodMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodServiceImpl implements FoodService{

    private final FoodMapper foodMapper;

    public FoodDetailDto getFoodDetail(String foodId) {
        return foodMapper.selectFoodDetailById(foodId);
    }

    public List<FoodItemDto> searchFood(String keyword, int limit) {
        return foodMapper.searchFoodItems(keyword, limit);
    }
}

