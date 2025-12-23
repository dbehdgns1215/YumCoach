import { reactive } from "vue";
import { fetchFoodDetail } from "../api/foods.js";

// 전역 영양정보 캐시
const nutritionCache = reactive({});

// 진행 중인 요청 추적 (중복 방지)
const pendingRequests = new Map();

/**
 * 영양정보 캐싱 composable
 */
export function useNutritionCache() {
  /**
   * 영양정보 가져오기 (캐시 우선)
   */
  async function getNutrition(foodId) {
    // 캐시에 있으면 즉시 반환
    if (nutritionCache[foodId]) {
      return nutritionCache[foodId];
    }

    // 이미 요청 중이면 해당 Promise 반환 (중복 요청 방지)
    if (pendingRequests.has(foodId)) {
      return pendingRequests.get(foodId);
    }

    // 새로운 요청
    const promise = fetchFoodDetail(foodId)
      .then((nutrition) => {
        nutritionCache[foodId] = nutrition;
        pendingRequests.delete(foodId);
        return nutrition;
      })
      .catch((error) => {
        pendingRequests.delete(foodId);
        throw error;
      });

    pendingRequests.set(foodId, promise);
    return promise;
  }

  /**
   * 여러 음식의 영양정보를 병렬로 가져오기
   */
  async function getBatchNutrition(foodIds) {
    const uniqueIds = [...new Set(foodIds)];
    const promises = uniqueIds.map((id) =>
      getNutrition(id).catch((err) => {
        console.warn(`음식 ${id} 영양정보 로드 실패:`, err);
        return null;
      })
    );

    const results = await Promise.all(promises);

    // foodId를 key로 하는 객체 반환
    const nutritionMap = {};
    uniqueIds.forEach((id, index) => {
      if (results[index]) {
        nutritionMap[id] = results[index];
      }
    });

    return nutritionMap;
  }

  /**
   * 캐시 초기화
   */
  function clearCache() {
    Object.keys(nutritionCache).forEach((key) => {
      delete nutritionCache[key];
    });
  }

  /**
   * 특정 항목 캐시 삭제
   */
  function removeFromCache(foodId) {
    delete nutritionCache[foodId];
  }

  return {
    nutritionCache,
    getNutrition,
    getBatchNutrition,
    clearCache,
    removeFromCache,
  };
}
