package com.ssafy.yumcoach.food.controller;

import com.ssafy.yumcoach.food.model.FoodDetailDto;
import com.ssafy.yumcoach.food.model.FoodItemDto;
import com.ssafy.yumcoach.food.model.service.FoodService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/foods")
@RequiredArgsConstructor
public class FoodController {

    private final FoodService foodService;

    /**
     * 단일 식품 상세정보 조회 API
     *
     * 식품 ID(food_id)에 해당하는 기본 식품 정보(food_items)와
     * 대표 영양성분 정보(nutrition_facts_primary)를 함께 조회한다.
     *
     * 예: GET /api/foods/FOD12345
     *
     * @param foodId 조회할 식품 ID (PK)
     *
     * Request Example:
     *   GET /api/foods/FD00123
     *
     * Response:
     * - 200 OK: FoodDetailDto (기본정보 + 영양성분 묶음)
     * - 404 Not Found: 해당 foodId 존재하지 않음
     *
     * Response Body Example:
     * {
     *   "food": {
     *      "foodId": "FD00123",
     *      "foodName": "사과",
     *      "majorCategoryName": "과일류",
     *      "middleCategoryName": "사과",
     *      "servingSize": "100g",
     *      "dataCreated": "2024-01-10",
     *      ...
     *   },
     *   "nutrition": {
     *      "energyKcal": 52.00,
     *      "proteinG": 0.30,
     *      "fatG": 0.20,
     *      "carbohydrateG": 14.00,
     *      "sugarsG": 10.00,
     *      "dietaryFiberG": 2.00,
     *      ...
     *   }
     * }
     *
     * @return 식품 상세정보 또는 NotFound
     */
    @GetMapping("/{foodId}")
    public ResponseEntity<@NonNull FoodDetailDto> getFoodDetail(@PathVariable String foodId) {

        FoodDetailDto detail = foodService.getFoodDetail(foodId);

        return detail == null
                ? ResponseEntity.notFound().build()
                : ResponseEntity.ok(detail);
    }

    /**
     * 식품 검색 API
     *
     * 키워드를 기준으로 food_items 테이블에서 식품 이름, 대표식품 이름, 또는
     * 카테고리명(대분류/중분류/소분류)에 대해 LIKE 검색을 수행한다.
     *
     * 예: GET /api/foods/search?keyword=apple&limit=50
     *
     * @param keyword 검색할 식품명 또는 키워드 (필수)
     * @param limit   검색 결과 제한 개수 (기본값 50)
     *
     * Request Example:
     *   GET /api/foods/search?keyword=banana
     *   GET /api/foods/search?keyword=과일&limit=20
     *
     * Response:
     * - 200 OK: List<FoodItemDto> (식품 기본정보 목록)
     * - 204 No Content: 검색된 식품 없음
     *
     * Response Body Example:
     * [
     *   {
     *     "foodId": "FD00321",
     *     "foodName": "바나나",
     *     "majorCategoryName": "과일류",
     *     "middleCategoryName": "바나나",
     *     "servingSize": "100g",
     *     ...
     *   },
     *   {
     *     "foodId": "FD00322",
     *     "foodName": "바나나칩",
     *     "majorCategoryName": "과자류",
     *     ...
     *   }
     * ]
     *
     * @return 식품 기본정보 목록 또는 NoContent
     */
    @GetMapping("/search")
    public ResponseEntity<@NonNull List<FoodItemDto>> searchFoods(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int limit) {

        List<FoodItemDto> foods = foodService.searchFood(keyword, limit);

        return foods == null || foods.isEmpty()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.ok(foods);
    }
}
