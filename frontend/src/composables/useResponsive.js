import { computed, ref, onMounted, onUnmounted } from "vue";

/**
 * 반응형 레이아웃을 위한 composable
 * @param {number} breakpoint - 데스크탑 기준 픽셀 (기본: 1200)
 */
export function useResponsive(breakpoint = 1200) {
  const width = ref(window.innerWidth);

  const onResize = () => {
    width.value = window.innerWidth;
  };

  onMounted(() => window.addEventListener("resize", onResize));
  onUnmounted(() => window.removeEventListener("resize", onResize));

  const isDesktop = computed(() => width.value >= breakpoint);
  const isMobile = computed(() => width.value < breakpoint);

  return {
    width,
    isDesktop,
    isMobile,
  };
}
