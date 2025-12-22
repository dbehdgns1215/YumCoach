import api from "@/lib/api";

/**
 * 식사 등록
 * @param {Object} payload { date:string('YYYY-MM-DD'), mealType:string, items:Array<{mealCode:string, mealName:string, amount:number}> }
 * @returns {Promise<Object>} 등록된 식사 응답
 */
export async function createMeal(payload) {
  const { data } = await api.post("/meals", payload);
  return data;
}
