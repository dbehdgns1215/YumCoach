package com.ssafy.yumcoach.meal.todo.model.service;

import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.NutritionFactsPrimaryDto;
import com.ssafy.yumcoach.food.model.service.FoodService;
import com.ssafy.yumcoach.meal.enums.MealType;
import com.ssafy.yumcoach.meal.model.MealItemDto;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import com.ssafy.yumcoach.meal.model.service.MealService;
import com.ssafy.yumcoach.meal.todo.model.MealTodoDto;
import com.ssafy.yumcoach.meal.todo.model.mapper.MealTodoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MealTodoServiceImpl implements MealTodoService {

    private final MealTodoMapper mealTodoMapper;
    private final MealService mealService;
    private final FoodService foodService;

    @Override
    public List<MealTodoDto> getTodos(Integer userId) {
        return mealTodoMapper.findByUser(userId);
    }

    @Override
    public List<MealTodoDto> getTodosByMealType(Integer userId, MealType mealType) {
        return mealTodoMapper.findByUserAndMealType(userId, mealType);
    }

    @Transactional
    @Override
    public void createTodo(
            Integer userId,
            MealType mealType,
            String foodCode,
            String foodName,
            Integer defaultGrams) {
        MealTodoDto todo = new MealTodoDto();
        todo.setUserId(userId);
        todo.setMealType(mealType);
        todo.setFoodCode(foodCode);
        todo.setFoodName(foodName);
        todo.setDefaultGrams(defaultGrams);

        mealTodoMapper.insert(todo);
    }

    @Transactional
    @Override
    public void consumeTodo(Integer userId, Long todoId, LocalDate date) {
        MealTodoDto todo = mealTodoMapper.findById(todoId, userId);
        if (todo == null) {
            throw new IllegalArgumentException("TODO not found");
        }

        Integer intUserId = Math.toIntExact(userId);

        // 영양정보 조회 및 계산
        Double kcal = null;
        Double protein = null;
        Double carbs = null;
        Double fat = null;

        try {
            FoodDetailDto foodDetail = foodService.getFoodDetail(todo.getFoodCode());
            if (foodDetail != null && foodDetail.getNutrition() != null) {
                NutritionFactsPrimaryDto nutrition = foodDetail.getNutrition();
                double factor = todo.getDefaultGrams() / 100.0;

                // 100g 기준 영양정보를 실제 섭취량(grams)에 맞게 계산
                kcal = nutrition.getEnergyKcal() != null ? nutrition.getEnergyKcal() * factor : 0.0;
                protein = nutrition.getProteinG() != null ? nutrition.getProteinG() * factor : 0.0;
                carbs = nutrition.getCarbohydrateG() != null ? nutrition.getCarbohydrateG() * factor : 0.0;
                fat = nutrition.getFatG() != null ? nutrition.getFatG() * factor : 0.0;

                // 소수점 둘째자리까지 반올림
                kcal = Math.round(kcal * 100.0) / 100.0;
                protein = Math.round(protein * 100.0) / 100.0;
                carbs = Math.round(carbs * 100.0) / 100.0;
                fat = Math.round(fat * 100.0) / 100.0;
            }
        } catch (Exception e) {
            // 영양정보 조회 실패 시 로그만 남기고 계속 진행
            System.err.println("Failed to fetch nutrition info for foodCode: " + todo.getFoodCode());
            e.printStackTrace();
        }

        MealItemDto item = MealItemDto.builder()
                .mealCode(todo.getFoodCode())
                .mealName(todo.getFoodName())
                .amount(todo.getDefaultGrams())
                .kcal(kcal)
                .protein(protein)
                .carbs(carbs)
                .fat(fat)
                .build();

        MealLogDto mealLog = MealLogDto.builder()
                .userId(intUserId)
                .date(date)
                .mealType(todo.getMealType())
                .items(List.of(item))
                .build();

        mealService.saveMealLog(mealLog);

        mealTodoMapper.deleteById(todoId, userId);
    }

    @Override
    public void deleteTodo(Integer userId, Long todoId) {
        mealTodoMapper.deleteById(todoId, userId);
    }

}
