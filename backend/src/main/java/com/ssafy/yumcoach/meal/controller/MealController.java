package com.ssafy.yumcoach.meal.controller;

import com.ssafy.yumcoach.auth.principal.CustomUserPrincipal;
import com.ssafy.yumcoach.meal.model.MealLogDto;
import com.ssafy.yumcoach.meal.model.service.MealService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
     * 특정 날짜의 식사 기록 조회 API
     *
     * GET /api/meals?date={yyyy-MM-dd}
     *
     * @param date     조회할 날짜 (yyyy-MM-dd 형식, 필수)
     *
     * Request Example:
     *  GET /api/meals?date=2025-12-10
     *
     * Response:
     * - 200 OK: List<MealLogDto> (해당 날짜의 모든 식사 기록 목록)
     * - 204 No Content: 해당 날짜의 기록 없음
     *
     * @return 해당 날짜에 기록된 식사 목록 또는 NoContent
     */
    @GetMapping
    public ResponseEntity<@NonNull List<MealLogDto>> getMealsByDate(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {

        List<MealLogDto> meals = mealService.getMealsByDate(user.getUserId(), date);
        if (meals == null || meals.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(meals);
    }

    /**
     * 특정 기간 동안의 식사 기록 조회 API
     *
     * GET /api/meals/range?startDate={yyyy-MM-dd}&endDate={yyyy-MM-dd}
     *
     * @param startDate  시작 날짜 (yyyy-MM-dd 형식)
     * @param endDate    종료 날짜 (yyyy-MM-dd 형식)
     *
     * Request Example:
     *  GET /api/meals/range?startDate=2025-12-01&endDate=2025-12-10
     *
     * Response:
     * - 200 OK: List<MealLogDto> (기간 내 식사 기록 전체)
     * - 204 No Content: 기간 내 기록 없음
     *
     * @return 기간 내 식사 기록 목록 또는 NoContent
     */
    @GetMapping("/range")
    public ResponseEntity<List<MealLogDto>> getMealsByDateRange(
            @AuthenticationPrincipal CustomUserPrincipal user,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        List<MealLogDto> meals = mealService.getMealsByDateRange(user.getUserId(), startDate, endDate);
        return meals == null || meals.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(meals);
    }

    /**
     * 한 끼 식사 기록 저장 API
     *
     * POST /api/meals
     *
     * @param mealLogDto 저장할 식사 정보 DTO
     *
     * Required Fields:
     * - date: 식사 날짜 (yyyy-MM-dd)
     * - mealType: 식사 타입 (BREAKFAST, LUNCH, DINNER, SNACK)
     * - items: 식사 구성(food items)
     *
     * Request Example:
     *  {
     *    "userId": 3,
     *    "date": "2025-12-10",
     *    "mealType": "BREAKFAST",
     *    "items": [
     *      { "mealCode": "APL001", "mealName": "Apple", "amount": 150 },
     *      { "mealCode": "BAN001", "mealName": "Banana", "amount": 100 }
     *    ]
     *  }
     *
     * Response:
     * - 200 OK: "식사 기록이 등록되었습니다."
     * - 400 Bad Request: 필수값 누락 시
     *
     * @return 저장 성공 메시지
     */
    @PostMapping
    public ResponseEntity<@NonNull String> saveMealLog(@AuthenticationPrincipal CustomUserPrincipal user,
                                                       @RequestBody MealLogDto mealLogDto) {

        mealLogDto.setUserId(user.getUserId());

        // 필수 값 검증
        if (mealLogDto.getUserId() == 0 || mealLogDto.getDate() == null || mealLogDto.getMealType() == null) {
            return ResponseEntity.badRequest().body("userId, date, mealType는 필수값입니다.");
        }

        mealService.saveMealLog(mealLogDto);

        return ResponseEntity.ok("식사 기록이 등록되었습니다.");
    }

    /**
     * 식사 기록 수정 API
     *
     * PUT /api/meals/{mealLogId}
     *
     * @param mealLogId  수정할 식사 기록 ID
     * @param mealLogDto 수정할 내용 DTO (mealLogId는 내부에서 세팅됨)
     *
     * Request Example:
     *  PUT /api/meals/10
     *  {
     *    "userId": 3,
     *    "date": "2025-12-10",
     *    "mealType": "LUNCH",
     *    "items": [
     *      { "mealCode": "CHK001", "mealName": "Chicken Breast", "amount": 200 }
     *    ]
     *  }
     *
     * Response:
     * - 200 OK: "식사 기록이 수정되었습니다."
     *
     * @return 수정 성공 메시지
     */
    @PutMapping("/{mealLogId}")
    public ResponseEntity<@NonNull String> updateMealLog(
            @PathVariable Long mealLogId,
            @RequestBody MealLogDto mealLogDto) {

        mealLogDto.setId(mealLogId);
        mealService.updateMealLog(mealLogDto);

        return ResponseEntity.ok("식사 기록이 수정되었습니다.");
    }

    /**
     * 식사 기록 삭제 API
     *
     * DELETE /api/meals/{mealLogId}
     *
     * @param mealLogId 삭제할 식사 기록 ID
     *
     * Request Example:
     *  DELETE /api/meals/15
     *
     * Response:
     * - 200 OK: "식사 기록이 삭제되었습니다."
     *
     * @return 삭제 성공 메시지
     */
    @DeleteMapping("/{mealLogId}")
    public ResponseEntity<@NonNull String> deleteMealLog(@PathVariable Long mealLogId) {

        mealService.deleteMealLog(mealLogId);
        return ResponseEntity.ok("식사 기록이 삭제되었습니다.");
    }

}