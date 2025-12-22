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
    latenight: [],
  };

  if (!meals || !Array.isArray(meals)) return result;

  meals.forEach((meal) => {
    const mealKey = MEAL_TYPE_TO_KEY[meal.mealType] || "snack";
    meal.items.forEach((item) => {
      result[mealKey].push({
        id: item.id,
        foodId: item.mealCode,
        name: item.mealName,
        grams: item.amount || 0,
        per100g: { kcal: 0, protein: 0, carbs: 0, fat: 0 },
      });
    });
  });

  return result;
}
