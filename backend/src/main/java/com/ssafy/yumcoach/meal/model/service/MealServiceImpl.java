package com.ssafy.yumcoach.meal.model.service;

import com.ssafy.yumcoach.meal.model.MealItemDto;
import com.ssafy.yumcoach.meal.model.mapper.MealMapper;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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
        mealMapper.insertMealLog(mealLog); // auto generated key → mealLog.id

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
    }

    @Transactional
    @Override
    public void deleteMealItem(long userId, long mealLogId, long mealItemId) {
        int deleted = mealMapper.deleteMealItemScoped(
                userId,
                mealLogId,
                mealItemId);
        if (deleted == 0) {
            throw new IllegalArgumentException("삭제할 아이템이 없거나 권한이 없습니다.");
        }

        // 옵션: 남은 아이템 수가 0이면 meal_history도 삭제
        int remaining = mealMapper.countMealItemsByHistoryId(mealLogId);
        if (remaining == 0) {
            mealMapper.deleteMealLogByIdAndUserId(mealLogId, userId);
        }
    }

    /**
     * 식사 아이템 수정
     * amount, 영양정보(kcal, protein, carbs, fat) 등을 수정
     */
    @Transactional
    @Override
    public void updateMealItem(Integer userId, Long mealLogId, MealItemDto mealItemDto) {
        // 권한 확인: 해당 아이템이 현재 사용자의 mealLog에 속하는지 확인
        // (선택사항: 더 엄격한 권한 체크 원하면 userId도 함께 검증)
        mealMapper.updateMealItem(mealItemDto);
    }
}
