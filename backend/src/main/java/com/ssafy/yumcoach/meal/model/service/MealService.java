package com.ssafy.yumcoach.meal.model.service;

import com.ssafy.yumcoach.meal.model.MealLogDto;

import java.time.LocalDate;
import java.util.List;

public interface MealService {

    // 특정 날짜의 식사 조회
    List<MealLogDto> getMealsByDate(Integer userId, LocalDate date);

    // 기간별 식사 조회 (옵션)
    List<MealLogDto> getMealsByDateRange(Integer userId, LocalDate startDate, LocalDate endDate);

    // 식사 로그 저장 (한 끼)
    void saveMealLog(MealLogDto mealLog);

    // 식사 로그 수정
    void updateMealLog(MealLogDto mealLog);

    // 식사 로그 삭제
    void deleteMealLog(Long mealLogId);
}
