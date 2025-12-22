/**
 * 영양소 계산 유틸리티
 */

/**
 * 개별 음식 항목의 영양소 계산
 * @param {Object} row - { grams, per100g: { kcal, protein, carbs, fat }, calc?: { kcal, protein, carbs, fat } }
 * @returns {Object} { kcal, protein, carbs, fat }
 */
export function calculateNutrition(row) {
  // DB에 저장된 계산값이 있으면 우선 사용
  if (row.calc) {
    return row.calc;
  }

  // 아니면 per100g 데이터로 계산
  const factor = Number(row.grams || 0) / 100;
  return {
    kcal: (row.per100g?.kcal || 0) * factor,
    protein: (row.per100g?.protein || 0) * factor,
    carbs: (row.per100g?.carbs || 0) * factor,
    fat: (row.per100g?.fat || 0) * factor,
  };
}

/**
 * 여러 음식 항목의 영양소 합계
 * @param {Array} items - 음식 항목 배열
 * @returns {Object} { kcal, protein, carbs, fat }
 */
export function sumNutrition(items) {
  const sum = items.reduce(
    (acc, row) => {
      const nutrition = calculateNutrition(row);
      acc.kcal += nutrition.kcal;
      acc.protein += nutrition.protein;
      acc.carbs += nutrition.carbs;
      acc.fat += nutrition.fat;
      return acc;
    },
    { kcal: 0, protein: 0, carbs: 0, fat: 0 }
  );

  return roundNutrition(sum);
}

/**
 * 영양소 값 반올림
 * @param {Object} nutrition - { kcal, protein, carbs, fat }
 * @returns {Object} 반올림된 영양소 객체
 */
export function roundNutrition(nutrition) {
  return {
    kcal: Math.round(nutrition.kcal),
    protein: Math.round(nutrition.protein * 10) / 10,
    carbs: Math.round(nutrition.carbs * 10) / 10,
    fat: Math.round(nutrition.fat * 10) / 10,
  };
}

/**
 * 영양소 포맷팅 (문자열)
 * @param {Object} nutrition - { kcal, protein, carbs, fat }
 * @param {boolean} includeAll - 모든 영양소 포함 여부
 * @returns {string} 포맷된 문자열
 */
export function formatNutrition(nutrition, includeAll = false) {
  if (includeAll) {
    return `${nutrition.kcal}kcal · P ${nutrition.protein}g · C ${nutrition.carbs}g · F ${nutrition.fat}g`;
  }
  return `${nutrition.kcal}kcal · P ${nutrition.protein}g`;
}
