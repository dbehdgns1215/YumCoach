package com.ssafy.yumcoach.meal.todo.model.service;

import com.ssafy.yumcoach.meal.enums.MealType;
import com.ssafy.yumcoach.meal.todo.model.MealTodoDto;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface MealTodoService {

    List<MealTodoDto> getTodos(Integer userId);
    List<MealTodoDto> getTodosByMealType(Integer userId, MealType mealType);

    void createTodo(Integer userId, MealType mealType, String foodCode, String foodName, Integer defaultGrams);

    void consumeTodo(Integer userId, Long todoId, java.time.LocalDate date);

    void deleteTodo(Integer userId, Long todoId);
}
