package com.ssafy.yumcoach.meal.model.mapper;

import com.ssafy.yumcoach.meal.model.MealItemDto;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface MealMapper {

    // 특정 유저의 특정 날짜 식사 전체 조회 (아침/점심/저녁/간식)
    List<MealLogDto> selectMealLogsByUserAndDate(
            @Param("userId") Integer userId,
            @Param("date") LocalDate date
    );

    // 기간별 조회 (옵션)
    List<MealLogDto> selectMealLogsByUserAndDateRange(
            @Param("userId") Integer userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    // meal_history 한 끼 insert
    int insertMealLog(MealLogDto mealLog);

    // meal_history 한 끼 update
    int updateMealLog(MealLogDto mealLog);

    // 해당 끼니의 음식 전체 삭제 후 다시 넣고 싶을 때
    int deleteMealItemsByHistoryId(@Param("historyId") Long historyId);

    // meal 여러 건 bulk insert
    int insertMealItems(
            @Param("historyId") Long historyId,
            @Param("items") List<MealItemDto> items
    );

    // 단일 아이템 수정 (필요하면)
    int updateMealItem(MealItemDto item);

    int deleteMealItemScoped(
            @Param("userId") long userId,
            @Param("historyId") long historyId,
            @Param("mealItemId") long mealItemId
    );

    int countMealItemsByHistoryId(@Param("historyId") long historyId);

    int deleteMealLogByIdAndUserId(
            @Param("historyId") long historyId,
            @Param("userId") long userId
    );

}
