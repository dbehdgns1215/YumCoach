/**
 * 식사 타입 관련 상수
 */

// UI에서 사용할 식사 종류 키
export const MEAL_KEYS = ["breakfast", "lunch", "dinner", "snack", "midnight"];

// 한글 라벨
export const MEAL_LABELS = {
  breakfast: "아침",
  lunch: "점심",
  dinner: "저녁",
  snack: "간식",
  midnight: "야식",
};

// API 응답의 mealType -> 로컬 키로 변환
export const MEAL_TYPE_TO_KEY = {
  BREAKFAST: "breakfast",
  LUNCH: "lunch",
  DINNER: "dinner",
  SNACK: "snack",
  MIDNIGHT: "midnight",
};

// 로컬 키 -> API mealType으로 변환
export const KEY_TO_MEAL_TYPE = {
  breakfast: "BREAKFAST",
  lunch: "LUNCH",
  dinner: "DINNER",
  snack: "SNACK",
  midnight: "MIDNIGHT",
};
