import { ref, computed } from "vue";
import { DEFAULT_GRAMS, DECIMAL_PLACES } from "../constants/nutrition.js";

/**
 * 단일 음식 추가 기능 composable
 * @param {Ref} selectedFood - 외부에서 관리되는 선택된 음식 ref
 */
export function useSingleFoodAdd(selectedFood) {
  const grams = ref(DEFAULT_GRAMS);

  /**
   * 입력된 그램 수를 기반으로 계산된 영양정보
   */
  const calc = computed(() => {
    if (!selectedFood.value) {
      return { kcal: 0, protein: 0, carbs: 0, fat: 0 };
    }

    const factor = Number(grams.value || 0) / 100;
    const nutrition = selectedFood.value.per100g;

    return {
      kcal: Math.round(nutrition.kcal * factor),
      protein:
        Math.round(nutrition.protein * factor * DECIMAL_PLACES) /
        DECIMAL_PLACES,
      carbs:
        Math.round(nutrition.carbs * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
      fat: Math.round(nutrition.fat * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
    };
  });

  /**
   * 음식 추가 버튼 활성화 여부
   */
  const canAdd = computed(
    () => !!selectedFood.value && Number(grams.value) > 0
  );

  /**
   * 상태 초기화
   */
  function reset() {
    grams.value = DEFAULT_GRAMS;
  }

  return {
    grams,
    calc,
    canAdd,
    reset,
  };
}
