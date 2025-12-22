// 음식 관련 API 모듈
import {
  transformSearchFood,
  transformNutrition,
} from "../utils/foodTransform.js";

const baseUrl = import.meta.env.VITE_API_BASE_URL;

/**
 * 음식 검색 API 호출
 * @param {string} keyword
 * @returns {Promise<Array>} 변환된 음식 목록
 */
export async function fetchFoodSearch(keyword) {
  const encodedKeyword = encodeURIComponent(keyword.trim());
  const response = await fetch(
    `${baseUrl}/foods/search?keyword=${encodedKeyword}&limit=50`
  );
  if (!response.ok) throw new Error("음식 검색 실패");
  const data = await response.json();
  return (data.foods || data).map(transformSearchFood);
}

/**
 * 음식 상세정보 API 호출 (영양정보 포함)
 * @param {number|string} foodId
 * @returns {Promise<Object>} 변환된 영양정보
 */
export async function fetchFoodDetail(foodId) {
  const response = await fetch(`${baseUrl}/foods/${foodId}`);
  if (!response.ok) throw new Error("음식 상세정보 조회 실패");
  const data = await response.json();
  return transformNutrition(data.nutrition || {});
}
