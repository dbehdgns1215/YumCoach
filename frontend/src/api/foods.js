// 음식 관련 API 모듈
import api from "../lib/api.js";
import {
  transformSearchFood,
  transformNutrition,
} from "../utils/foodTransform.js";

/**
 * 음식 검색 API 호출
 * @param {string} keyword
 * @returns {Promise<Array>} 변환된 음식 목록
 */
export async function fetchFoodSearch(keyword) {
  const q = (keyword || "").trim();
  const res = await api.get("/foods/search", {
    params: { keyword: q, limit: 50 },
  });
  const data = res.data || {};
  return (data.foods || data).map(transformSearchFood);
}

/**
 * 음식 상세정보 API 호출 (영양정보 포함)
 * @param {number|string} foodId
 * @returns {Promise<Object>} 변환된 영양정보
 */
export async function fetchFoodDetail(foodId) {
  const res = await api.get(`/foods/${foodId}`);
  const data = res.data || {};
  return transformNutrition(data.nutrition || {});
}
