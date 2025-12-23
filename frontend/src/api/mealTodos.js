import api from "@/lib/api";

/**
 * 사용자의 meal-todo 목록 조회
 * @param {string} mealType (선택) 특정 식사 타입 필터링: BREAKFAST, LUNCH, DINNER, SNACK, LATENIGHT
 * @returns {Promise<Array>} meal-todo 목록
 */
export async function getMealTodos(mealType = null) {
  const params = mealType ? { mealType } : {};
  const { data } = await api.get("/meal-todos", { params });
  return Array.isArray(data) ? data : [];
}

/**
 * meal-todo 추가
 * @param {Object} payload { mealType, foodCode, foodName, defaultGrams }
 * @returns {Promise<Object>} 생성된 meal-todo
 */
export async function createMealTodo(payload) {
  const { data } = await api.post("/meal-todos", null, {
    params: {
      mealType: payload.mealType,
      foodCode: payload.foodCode,
      foodName: payload.foodName,
      defaultGrams: payload.defaultGrams,
    },
  });
  return data;
}

/**
 * meal-todo 삭제
 * @param {number} id meal-todo ID
 * @returns {Promise<void>}
 */
export async function deleteMealTodo(id) {
  await api.delete(`/meal-todos/${id}`);
}

/**
 * meal-todo를 실제 식사로 변환 (consume)
 * @param {number} id meal-todo ID
 * @param {string} date 날짜 'YYYY-MM-DD'
 * @returns {Promise<Object>} 생성된 식사
 */
export async function consumeMealTodo(id, date) {
  const { data } = await api.post(`/meal-todos/${id}/consume`, null, {
    params: { date },
  });
  return data;
}
