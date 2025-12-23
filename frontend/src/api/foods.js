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

/**
 * 음식 이미지 분석 API 호출
 * AI 서버의 /analysis/food/upload/multi 엔드포인트 호출
 * @param {File} imageFile 이미지 파일
 * @returns {Promise<Array>} 인식된 음식 이름 목록 [{ name: "김치" }, { name: "샐러드" }, ...]
 */
export async function analyzeFoodImage(imageFile) {
  const formData = new FormData();
  formData.append("file", imageFile);

  // AI 서버 주소 (환경변수 또는 기본값)
  const aiBaseUrl =
    import.meta.env.VITE_AI_API_BASE_URL || "http://localhost:8111";

  const res = await fetch(`${aiBaseUrl}/analysis/food/upload/multi`, {
    method: "POST",
    body: formData,
  });

  if (!res.ok) {
    throw new Error(`이미지 분석 실패: ${res.statusText}`);
  }

  const data = await res.json();

  // 반환된 items 리스트 반환
  if (data.items && Array.isArray(data.items)) {
    return data.items; // [{ name: "김치" }, { name: "샐러드" }, ...]
  }

  throw new Error(data.message || "이미지 분석에 실패했습니다.");
}
