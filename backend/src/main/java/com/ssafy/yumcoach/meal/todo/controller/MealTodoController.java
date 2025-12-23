package com.ssafy.yumcoach.meal.todo.controller;

import com.ssafy.yumcoach.auth.principal.CustomUserPrincipal;
import com.ssafy.yumcoach.meal.enums.MealType;
import com.ssafy.yumcoach.meal.todo.model.MealTodoDto;
import com.ssafy.yumcoach.meal.todo.model.service.MealTodoService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meal-todos")
@RequiredArgsConstructor
public class MealTodoController {

    private final MealTodoService mealTodoService;

    /**
     * GET /meal-todos
     * GET /meal-todos?mealType=DINNER
     */
    @GetMapping
    public List<MealTodoDto> getTodos(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam(required = false) MealType mealType
    ) {
        return (mealType == null)
                ? mealTodoService.getTodos(user.getUserId())
                : mealTodoService.getTodosByMealType(user.getUserId(), mealType);
    }

    /**
     * POST /meal-todos
     *
     * 예:
     * /meal-todos?userId=1&mealType=DINNER&foodCode=FD_001&foodName=닭가슴살&defaultGrams=150
     */
    @PostMapping
    public ResponseEntity<@NonNull String> createTodo(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam MealType mealType,
            @RequestParam String foodCode,
            @RequestParam String foodName,
            @RequestParam Integer defaultGrams
    ) {
        mealTodoService.createTodo(
                user.getUserId(),
                mealType,
                foodCode,
                foodName,
                defaultGrams
        );

        return ResponseEntity.ok("식사 TODO가 등록되었습니다.");
    }

    /**
     * POST /meal-todos/{id}/consume
     *
     * 예:
     * /meal-todos/10/consume?userId=1&date=2025-03-22
     */
    @PostMapping("/{id}/consume")
    public void consumeTodo(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @PathVariable("id") Long todoId,
            @RequestParam LocalDate date
    ) {
        mealTodoService.consumeTodo(user.getUserId(), todoId, date);
    }

    /**
     * DELETE /meal-todos/{id}
     */
    @DeleteMapping("/{id}")
    public void deleteTodo(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @PathVariable("id") Long todoId
    ) {
        mealTodoService.deleteTodo(user.getUserId(), todoId);
    }
}
