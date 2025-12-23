package com.ssafy.yumcoach.meal.todo.model.service;

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
            Integer defaultGrams
    ) {
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

        MealItemDto item = MealItemDto.builder()
                .mealCode(todo.getFoodCode())
                .mealName(todo.getFoodName())
                .amount(todo.getDefaultGrams())
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
