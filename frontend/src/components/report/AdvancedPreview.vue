<template>
  <BaseCard class="wrap" clickable @click="$emit('open')">
    <div class="top">
      <div class="title">🔒 더 자세한 분석 보기 (Advanced)</div>
      <!-- <div class="cta">미리보기</div> -->
    </div>
    <!-- <div class="desc">요일별 패턴 · 영양소 추세 · 식사 시간 분석</div>ㄴㄴㄴ -->

    <div class="preview">
      <div class="tile" style="height: 50px;" />
      <div class="tile" style="height: 50px;" />
      <div class="tile" style="height: 50px;" />
      <div class="overlay">Advanced에서 더 자세히 볼 수 있어요</div>
    </div>
  </BaseCard>
</template>

<script setup>
import BaseCard from '@/components/base/BaseCard.vue'
defineEmits(['open'])
</script>

<style scoped>
.wrap{ position:relative; }
.top{ display:flex; justify-content:space-between; align-items:center; gap:var(--space-3); }
.title{ font-weight:900; font-size:14px; }
.cta{
  padding:6px 10px; border-radius:999px;
  border:1px solid var(--border);
  color:var(--muted); font-size:12px; font-weight:800;
}
.desc{ margin-top:6px; color:var(--muted); font-size:13px; }
.preview{
  position:relative;
  margin-top: var(--space-3);
  display:grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-2);
}
.tile{
  height:70px;
  border-radius: 14px;
  border:1px solid var(--border);
  background: linear-gradient(180deg, #fff, #f4f6ff);
  filter: blur(1px);
}
.overlay{
  position:absolute; inset:0;
  display:flex; align-items:center; justify-content:center;
  text-align:center;
  padding: 0 12px;
  font-weight:900;
  color: rgba(16,24,40,.72);
}
</style>
