/**
 * 식사 데이터 변환 유틸리티
 */
import { MEAL_TYPE_TO_KEY } from "@/constants/mealTypes";

/**
 * API 응답의 식사 데이터를 UI 형태로 변환
 * @param {Array} meals API 응답 배열
 * @returns {Object} { breakfast: [...], lunch: [...], ... }
 */
export function transformMealsToUI(meals) {
  const result = {
    breakfast: [],
    lunch: [],
    dinner: [],
    snack: [],
    midnight: [],
  };

  if (!meals || !Array.isArray(meals)) return result;

  meals.forEach((meal) => {
    const mealKey = MEAL_TYPE_TO_KEY[meal.mealType] || "snack";

    // items가 없거나 배열이 아니면 건너뛰기
    if (!meal.items || !Array.isArray(meal.items)) {
      return;
    }

    meal.items.forEach((item) => {
      // DB에 저장된 영양정보가 있으면 사용 (0도 유효한 값으로 간주)
      const calc =
        item.kcal !== undefined && item.kcal !== null
          ? {
              kcal: item.kcal || 0,
              protein: item.protein || 0,
              carbs: item.carbs || 0,
              fat: item.fat || 0,
            }
          : null;

      result[mealKey].push({
        id: item.id,
        historyId: item.historyId || meal.id,
        foodId: item.mealCode,
        name: item.mealName,
        grams: item.amount || 0,
        per100g: { kcal: 0, protein: 0, carbs: 0, fat: 0 },
        calc: calc, // DB에서 받은 계산값
      });
    });
  });

  return result;
}

/**
 * 아이템의 영양정보 업데이트
 * @param {Object} item 식사 아이템
 * @param {Object} nutrition 영양정보 { kcal, protein, carbs, fat }
 */
export function updateItemNutrition(item, nutrition) {
  if (item && nutrition) {
    item.per100g = {
      kcal: nutrition.kcal || 0,
      protein: nutrition.protein || 0,
      carbs: nutrition.carbs || 0,
      fat: nutrition.fat || 0,
    };
  }
}
