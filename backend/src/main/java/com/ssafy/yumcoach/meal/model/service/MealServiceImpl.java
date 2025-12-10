package com.ssafy.yumcoach.meal.model.service;

import com.ssafy.yumcoach.meal.mapper.MealMapper;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealServiceImpl implements MealService {

    private final MealMapper mealMapper;

    @Override
    public List<MealLogDto> getMealsByDate(Integer userId, LocalDate date) {
        return mealMapper.selectMealLogsByUserAndDate(userId, date);
    }

    @Override
    public List<MealLogDto> getMealsByDateRange(Integer userId, LocalDate startDate, LocalDate endDate) {
        return mealMapper.selectMealLogsByUserAndDateRange(userId, startDate, endDate);
    }

    /**
     * 저장 방식:
     * 1) meal_history insert
     * 2) meal bulk insert
     */
    @Transactional
    @Override
    public void saveMealLog(MealLogDto mealLog) {
        // 1) 한 끼 기록 저장
        mealMapper.insertMealLog(mealLog);  // auto generated key → mealLog.id

        // 2) 음식 목록 저장
        if (mealLog.getItems() != null && !mealLog.getItems().isEmpty()) {
            mealMapper.insertMealItems(mealLog.getId(), mealLog.getItems());
        }
    }

    /**
     * 수정 방식:
     * 1) meal_history update
     * 2) 기존 meal 삭제 → 새로 insert
     */
    @Transactional
    @Override
    public void updateMealLog(MealLogDto mealLog) {
        // 1) 한 끼 메타 수정
        mealMapper.updateMealLog(mealLog);

        // 2) 기존 아이템 전체 삭제
        mealMapper.deleteMealItemsByHistoryId(mealLog.getId());

        // 3) 새로운 아이템 추가
        if (mealLog.getItems() != null && !mealLog.getItems().isEmpty()) {
            mealMapper.insertMealItems(mealLog.getId(), mealLog.getItems());
        }
    }

    /**
     * 삭제 방식:
     * 1) meal 테이블 삭제
     * 2) meal_history 삭제
     */
    @Transactional
    @Override
    public void deleteMealLog(Long mealLogId) {
        mealMapper.deleteMealItemsByHistoryId(mealLogId);
        // meal_history 삭제 쿼리 필요하면 추가
        // ex: mealMapper.deleteMealLog(mealLogId); 추가 작성 가능
    }
}
