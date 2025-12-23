import { ref, computed } from "vue";
import { fetchFoodSearch, fetchFoodDetail } from "../api/foods.js";
import { ITEMS_PER_PAGE } from "../constants/nutrition.js";

/**
 * 음식 검색 기능 composable
 */
export function useFoodSearch() {
  const q = ref("");
  const loading = ref(false);
  const loadingDetail = ref(false);
  const allFoods = ref([]);
  const currentPage = ref(1);
  const selected = ref(null);

  /**
   * 음식 검색 실행
   */
  async function searchFoods() {
    if (!q.value.trim()) {
      allFoods.value = [];
      currentPage.value = 1;
      return;
    }

    loading.value = true;
    try {
      const foods = await fetchFoodSearch(q.value);
      allFoods.value = foods;
      currentPage.value = 1;
    } catch (error) {
      console.error("검색 중 오류 발생:", error);
      allFoods.value = [];
    } finally {
      loading.value = false;
    }
  }

  /**
   * 음식 상세정보 로드 및 선택
   */
  async function select(food) {
    loadingDetail.value = true;
    try {
      const nutrition = await fetchFoodDetail(food.id);
      selected.value = {
        ...food,
        per100g: nutrition,
      };
    } catch (error) {
      console.warn("상세정보 조회 실패, 기본 정보로 진행:", error);
      selected.value = food;
    } finally {
      loadingDetail.value = false;
    }
  }

  /**
   * 전체 페이지 수 계산
   */
  const totalPages = computed(() =>
    Math.ceil(allFoods.value.length / ITEMS_PER_PAGE)
  );

  /**
   * 현재 페이지의 음식 목록
   */
  const displayedFoods = computed(() => {
    const start = (currentPage.value - 1) * ITEMS_PER_PAGE;
    const end = start + ITEMS_PER_PAGE;
    return allFoods.value.slice(start, end);
  });

  /**
   * 상태 초기화
   */
  function reset() {
    q.value = "";
    loading.value = false;
    loadingDetail.value = false;
    allFoods.value = [];
    currentPage.value = 1;
    selected.value = null;
  }

  return {
    q,
    loading,
    loadingDetail,
    allFoods,
    currentPage,
    selected,
    searchFoods,
    select,
    totalPages,
    displayedFoods,
    reset,
  };
}
