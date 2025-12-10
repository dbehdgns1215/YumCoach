package com.ssafy.yumcoach.meal.controller;

import com.ssafy.yumcoach.meal.model.MealLogDto;
import com.ssafy.yumcoach.meal.model.service.MealService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meals")
@RequiredArgsConstructor
public class MealController {

    private final MealService mealService;

    /**
     * 특정 날짜의 식사 조회
     * 예: GET /api/meals?userId=1&date=2025-12-10
     */
    @GetMapping
    public ResponseEntity<@NonNull List<MealLogDto>> getMealsByDate(
            @RequestParam Integer userId,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        List<MealLogDto> meals = mealService.getMealsByDate(userId, date);

        if (meals == null || meals.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(meals);
    }

    /**
     * 한 끼 저장
     * 예: POST /api/meals
     * {
     * "userId": 3,
     * "date": "2025-12-10",
     * "mealType": "BREAKFAST",
     * "items": [
     * {
     * "mealCode": "APL001",
     * "mealName": "Apple",
     * "amount": 150
     * },
     * {
     * "mealCode": "BAN001",
     * "mealName": "Banana",
     * "amount": 100
     * }
     * ]
     * }
     * 예상 response: "식사 기록이 등록되었습니다."
     */
    @PostMapping
    public ResponseEntity<@NonNull String> saveMealLog(@RequestBody MealLogDto mealLogDto) {

        // 필수 값 검증
        if (mealLogDto.getUserId() == 0 || mealLogDto.getDate() == null || mealLogDto.getMealType() == null) {
            return ResponseEntity.badRequest().body("userId, date, mealType는 필수값입니다.");
        }

        mealService.saveMealLog(mealLogDto);

        return ResponseEntity.ok("식사 기록이 등록되었습니다.");
    }

}