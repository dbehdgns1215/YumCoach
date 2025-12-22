import api from "@/lib/api";

/**
 * 특정 날짜의 식사 목록 조회
 * @param {string} date 'YYYY-MM-DD' 형식
 * @returns {Promise<Array>} 식사 목록
 */
export async function getMealsByDate(date) {
  const { data } = await api.get("/meals", {
    params: { date },
  });
  return Array.isArray(data) ? data : data.meals || [];
}

/**
 * 식사 등록
 * @param {Object} payload { date:string('YYYY-MM-DD'), mealType:string, items:Array<{mealCode:string, mealName:string, amount:number}> }
 * @returns {Promise<Object>} 등록된 식사 응답
 */
export async function createMeal(payload) {
  const { data } = await api.post("/meals", payload);
  return data;
}
