<template>
  <AppShell title="이번 주 식단 리포트" :subtitle="periodLabel" @primary="onAddMeal">
    <div class="grid">
      <div class="colMain">
        <ReportHero :score="score" :period-label="periodLabel" :summary-title="heroTitle" :summary-line="heroLine" />

        <div class="insights">
          <InsightCard kind="good" title="잘하고 있어요" body="단백질 섭취가 대부분의 날에서 목표에 가까웠어요." />
          <InsightCard kind="warn" title="조금 아쉬워요" body="야식이 늦은 시간에 몰린 날이 몇 번 있었어요." />
          <InsightCard kind="keep" title="이건 유지해요" body="점심 식단 균형이 좋아서 전체 컨디션에 도움이 됐어요." />
        </div>

        <AdvancedPreview @open="openPaywall = true" />
      </div>

      <div class="colRail">
        <CoachCard :message="coachMessage" />
        <NextActionCard :action-text="nextAction" @save="onSavePlan" />
      </div>
    </div>

    <PaywallModal :open="openPaywall" @close="openPaywall = false" @upgrade="onUpgrade" />

  </AppShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import AppShell from '@/layout/AppShell.vue'
import ReportHero from '@/components/report/ReportHero.vue'
import InsightCard from '@/components/report/InsightCard.vue'
import NextActionCard from '@/components/report/NextActionCard.vue'
import CoachCard from '@/components/report/CoachCard.vue'
import AdvancedPreview from '@/components/report/AdvancedPreview.vue'
import PaywallModal from '@/components/paywall/PaywallModal.vue'

const router = useRouter()

const periodLabel = ref('3월 11일 – 3월 17일')

/** 점수는 항상 노출(확정) */
const score = ref(78)

const heroTitle = ref('이번 주는 꽤 괜찮았어요 🙂')
const heroLine = ref('전체적으로 괜찮았어요. 간식 타이밍만 조금 아쉬워요.')

const coachMessage = ref(
  '이번 주는 식사 간격이 꽤 안정적이었어요. 간식 타이밍만 조금 앞당기면 더 좋아질 것 같아요.'
)

const nextAction = ref('늦은 간식 대신 단백질 요거트를 미리 준비해보세요.')

const openPaywall = ref(false)

function onAddMeal()
{
  router.push('/log')
}
function onSavePlan()
{
  console.log('saved tomorrow plan')
}
function onUpgrade(payload)
{
  openPaywall.value = false
  console.log('selected plan:', payload?.plan) // 'monthly' | 'yearly'
  alert(`${payload?.plan === 'yearly' ? '연간' : '월간'} 플랜 결제는 곧 준비할게요 🙂`)
}

</script>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-4);
}

.colRail {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.colMain {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.insights {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-4);
}

@media (min-width: 768px) {
  .insights {
    grid-template-columns: 1fr 1fr;
  }
}

@media (min-width: 1200px) {
  .grid {
    grid-template-columns: 2fr 1fr;
    align-items: start;
  }

  .insights {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
