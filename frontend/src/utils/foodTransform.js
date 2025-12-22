// 음식/영양정보 변환 유틸리티

/**
 * 음식 목록 API 응답을 컴포넌트 형식으로 변환
 * @param {Object} apiFood
 * @returns {{id:number|string,name:string,representativeFoodName?:string,servingSize?:number,weight?:number,per100g:{kcal:number,protein:number,carbs:number,fat:number}}}
 */
export function transformSearchFood(apiFood) {
  return {
    id: apiFood.foodId,
    name: apiFood.foodName,
    representativeFoodName: apiFood.representativeFoodName,
    servingSize: apiFood.servingSize,
    weight: apiFood.weight,
    per100g: {
      kcal: 0,
      protein: 0,
      carbs: 0,
      fat: 0,
    },
  };
}

/**
 * API 영양정보 데이터를 컴포넌트 형식으로 변환
 * @param {Object} apiNutrition
 * @returns {{kcal:number,protein:number,carbs:number,fat:number}}
 */
export function transformNutrition(apiNutrition) {
  return {
    kcal: apiNutrition?.energyKcal ?? 0,
    protein: apiNutrition?.proteinG ?? 0,
    carbs: apiNutrition?.carbohydrateG ?? 0,
    fat: apiNutrition?.fatG ?? 0,
  };
}
