<template>
  <BaseCard>
    <template #header>
      <div class="head">
        <div>
          <div class="title">이번 주는 이런 느낌이에요 🙂</div>
          <div class="subtitle">{{ periodLabel }}</div>
        </div>
        <div class="badge">Score</div>
      </div>
    </template>

    <div class="hero">
      <div class="gauge">
        <svg viewBox="0 0 120 120" class="svg">
          <circle class="track" cx="60" cy="60" r="46" />
          <circle
            class="progress"
            cx="60" cy="60" r="46"
            :style="progressStyle"
          />
        </svg>
        <div class="score">
          <div class="num">{{ score }}</div>
          <div class="cap">점</div>
        </div>
      </div>

      <div class="summary">
        <div class="summaryTitle">{{ summaryTitle }}</div>
        <div class="summaryLine">{{ summaryLine }}</div>
      </div>
    </div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
  score: { type: Number, required: true },       // 0~100
  periodLabel: { type: String, required: true },
  summaryTitle: { type: String, required: true },
  summaryLine: { type: String, required: true },
})

const circumference = 2 * Math.PI * 46
const clamped = computed(() => Math.max(0, Math.min(100, props.score)))
const dashOffset = computed(() => circumference * (1 - clamped.value / 100))

const progressStyle = computed(() => ({
  strokeDasharray: `${circumference}px`,
  strokeDashoffset: `${dashOffset.value}px`,
}))
</script>

<style scoped>
.head{ display:flex; align-items:flex-start; justify-content:space-between; gap:var(--space-3); }
.title{ font-weight:800; font-size:16px; }
.subtitle{ color:var(--muted); font-size:12px; margin-top:4px; }
.badge{
  padding:6px 10px;
  border-radius:999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight:800;
  font-size:12px;
}
.hero{ display:flex; gap:var(--space-4); align-items:center; }
.gauge{ position:relative; width:120px; height:120px; flex:0 0 auto; }
.svg{ width:120px; height:120px; transform: rotate(-90deg); }
.track{ fill:none; stroke: var(--border); stroke-width: 12; }
.progress{
  fill:none;
  stroke: var(--primary);
  stroke-width: 12;
  stroke-linecap: round;
  transition: stroke-dashoffset 600ms ease;
}
.score{
  position:absolute; inset:0;
  display:flex; align-items:center; justify-content:center;
  flex-direction:column;
}
.num{ font-size:28px; font-weight:900; line-height:1; }
.cap{ font-size:12px; color:var(--muted); margin-top:2px; }
.summaryTitle{ font-size:14px; font-weight:900; margin-bottom:6px; }
.summaryLine{ font-size:14px; color:var(--muted); line-height:1.4; }

@media (max-width: 420px){
  .hero{ flex-direction:column; align-items:flex-start; }
}
</style>
