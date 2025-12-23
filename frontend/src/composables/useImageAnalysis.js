import { ref } from "vue";
import { analyzeFoodImage } from "../api/foods.js";

/**
 * 이미지 분석 기능 composable
 */
export function useImageAnalysis() {
  const fileInput = ref(null);
  const analyzingImage = ref(false);
  const analyzedFoods = ref([]);
  const showAnalyzedList = ref(false);

  /**
   * 파일 input 클릭 트리거
   */
  function triggerFileInput() {
    if (fileInput.value) {
      fileInput.value.click();
    }
  }

  /**
   * 이미지 파일 선택 시 처리
   */
  async function handleImageSelect(event) {
    const file = event.target.files?.[0];
    if (!file) return;

    analyzingImage.value = true;
    try {
      const items = await analyzeFoodImage(file);
      analyzedFoods.value = items;
      showAnalyzedList.value = true;
      console.log("음식 분석 완료:", items);
    } catch (error) {
      console.error("이미지 분석 중 오류:", error);
      alert("이미지 분석에 실패했습니다. 다시 시도해주세요.");
    } finally {
      analyzingImage.value = false;
      if (fileInput.value) {
        fileInput.value.value = "";
      }
    }
  }

  /**
   * 상태 초기화
   */
  function reset() {
    analyzingImage.value = false;
    analyzedFoods.value = [];
    showAnalyzedList.value = false;
  }

  return {
    fileInput,
    analyzingImage,
    analyzedFoods,
    showAnalyzedList,
    triggerFileInput,
    handleImageSelect,
    reset,
  };
}
