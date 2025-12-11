package com.ssafy.yumcoach.food.model.mapper;

import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.FoodItemDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FoodMapper {

    /**
     * food_id로 식품 기본정보 + 대표 영양정보 조회
     */
    FoodDetailDto selectFoodDetailById(@Param("foodId") String foodId);

    /**
     * 식품 이름/대표식품 이름/카테고리명으로 검색
     */
    List<FoodItemDto> searchFoodItems(
            @Param("keyword") String keyword,
            @Param("limit") int limit
    );

    /**
     * 단순히 food_items만 food_id 기준으로 조회하고 싶을 때
     */
    FoodItemDto selectFoodItemById(@Param("foodId") String foodId);
}
