import { ref } from "vue";
import { fetchFoodSearch, fetchFoodDetail } from "../api/foods.js";
import { DEFAULT_GRAMS } from "../constants/nutrition.js";

/**
 * 음식 선택 및 상세 모달 기능 composable
 */
export function useFoodSelection() {
  const selectedFoods = ref({});
  const showFoodDetailModal = ref(false);
  const selectedAnalyzedFood = ref("");
  const loading = ref(false);
  const loadingDetail = ref(false);
  const allFoods = ref([]);
  const currentPage = ref(1);

  /**
   * 분석된 음식으로 상세 선택 모달 열기
   */
  async function searchAnalyzedFood(foodName) {
    selectedAnalyzedFood.value = foodName;

    loading.value = true;
    try {
      const foods = await fetchFoodSearch(foodName);
      allFoods.value = foods;
      currentPage.value = 1;
      showFoodDetailModal.value = true;
    } catch (error) {
      console.error("검색 중 오류 발생:", error);
      allFoods.value = [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * 음식 상세 선택 모달 닫기
   */
  function closeFoodDetailModal() {
    showFoodDetailModal.value = false;
    selectedAnalyzedFood.value = "";
    allFoods.value = [];
  }

  /**
   * 음식 상세 선택 모달에서 음식 선택
   */
  async function selectDetailFood(food) {
    loadingDetail.value = true;
    try {
      const nutrition = await fetchFoodDetail(food.id);
      selectedFoods.value[selectedAnalyzedFood.value] = {
        id: food.id,
        name: food.name,
        per100g: nutrition,
        grams: DEFAULT_GRAMS,
      };
      closeFoodDetailModal();
    } catch (error) {
      console.error("상세정보 조회 실패:", error);
      selectedFoods.value[selectedAnalyzedFood.value] = {
        id: food.id,
        name: food.name,
        per100g: {},
        grams: DEFAULT_GRAMS,
      };
      closeFoodDetailModal();
    } finally {
      loadingDetail.value = false;
    }
  }

  /**
   * 선택된 음식 제거
   */
  function removeSelectedFood(foodName) {
    delete selectedFoods.value[foodName];
  }

  /**
   * 상태 초기화
   */
  function reset() {
    selectedFoods.value = {};
    showFoodDetailModal.value = false;
    selectedAnalyzedFood.value = "";
    loading.value = false;
    loadingDetail.value = false;
    allFoods.value = [];
    currentPage.value = 1;
  }

  return {
    selectedFoods,
    showFoodDetailModal,
    selectedAnalyzedFood,
    loading,
    loadingDetail,
    allFoods,
    currentPage,
    searchAnalyzedFood,
    closeFoodDetailModal,
    selectDetailFood,
    removeSelectedFood,
    reset,
  };
}
