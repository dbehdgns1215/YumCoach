<template>
  <TopBarNavigation />
  <AppShell title="이번 주 식단 리포트" :subtitle="periodLabel" footerTheme="brand" @primary="onAddMeal">
    <div class="grid">
      <div class="colMain">
        <div style="display:flex; gap:8px; margin-bottom:8px;">
          <BaseButton variant="primary" @click="createAndAnalyze">리포트 생성 및 AI 분석</BaseButton>
          <BaseButton variant="secondary" @click="clearResult">결과 초기화</BaseButton>
        </div>
        <ReportHero :score="score" :period-label="periodLabel" :summary-title="heroTitle" :summary-line="heroLine" />

        <div class="insights">
          <InsightCard 
            v-for="(ins, idx) in displayInsights" 
            :key="idx" 
            :kind="ins.kind" 
            :title="ins.title" 
            :body="ins.body" 
          />
        </div>

        <AdvancedPreview @open="openPaywall = true" />
      </div>

      <div class="colRail">
        <CoachCard :message="displayCoachMessage" />
        <NextActionCard :action-text="displayNextAction" @save="onSavePlan" />
      </div>
    </div>

    <PaywallModal :open="openPaywall" @close="openPaywall = false" @upgrade="onUpgrade" />
  </AppShell>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '@/lib/api.js'

import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import ReportHero from '@/components/report/ReportHero.vue'
import InsightCard from '@/components/report/InsightCard.vue'
import NextActionCard from '@/components/report/NextActionCard.vue'
import CoachCard from '@/components/report/CoachCard.vue'
import AdvancedPreview from '@/components/report/AdvancedPreview.vue'
import PaywallModal from '@/components/paywall/PaywallModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const router = useRouter()

const periodLabel = ref('3월 11일 – 3월 17일')
const score = ref(78)
const heroTitle = ref('이번 주는 꽤 괜찮았어요 🙂')
const heroLine = ref('전체적으로 괜찮았어요. 간식 타이밍만 조금 아쉬워요.')

const openPaywall = ref(false)
const devResult = ref(null)
const devError = ref(null)
const devLoading = ref(false)
const analyzeLoading = ref(false)
const analyzeResult = ref(null)

// insights에서 coach, action 추출
const displayCoachMessage = computed(() => {
  if (!devResult.value?.insights) return '이번 주는 식사 간격이 꽤 안정적이었어요. 간식 타이밍만 조금 앞당기면 더 좋아질 것 같아요.'
  const coach = devResult.value.insights.find(i => i.kind === 'coach')
  return coach?.body || '이번 주는 식사 간격이 꽤 안정적이었어요. 간식 타이밍만 조금 앞당기면 더 좋아질 것 같아요.'
})

const displayNextAction = computed(() => {
  if (!devResult.value?.insights) return '늦은 간식 대신 단백질 요거트를 미리 준비해보세요.'
  const action = devResult.value.insights.find(i => i.kind === 'action')
  return action?.body || '늦은 간식 대신 단백질 요거트를 미리 준비해보세요.'
})

// good, warn, keep만 필터링
const displayInsights = computed(() => {
  if (!devResult.value?.insights) {
    return [
      { kind: 'good', title: '잘하고 있어요', body: '단백질 섭취가 대부분의 날에서 목표에 가까웠어요.' },
      { kind: 'warn', title: '조금 아쉬워요', body: '야식이 늦은 시간에 몰린 날이 몇 번 있었어요.' },
      { kind: 'keep', title: '이건 유지해요', body: '점심 식단 균형이 좋아서 전체 컨디션에 도움이 됐어요.' }
    ]
  }
  return devResult.value.insights.filter(i => 
    i.kind === 'good' || i.kind === 'warn' || i.kind === 'keep'
  )
})

async function createAndAnalyze() {
  devError.value = null
  devResult.value = null
  analyzeResult.value = null
  devLoading.value = true
  analyzeLoading.value = false
  try {
    const today = new Date()
    const iso = `${today.getFullYear()}-${String(today.getMonth()+1).padStart(2,'0')}-${String(today.getDate()).padStart(2,'0')}`
    const res = await api.post('/reports/daily', { date: iso })
    const created = res.data
    devResult.value = created
  } catch (e) {
    if (e?.response?.status === 429) {
      devError.value = { error: '생성 한도를 초과했습니다. 잠시 후 다시 시도하세요.' }
    } else {
      devError.value = e?.response?.data || e.message
    }
  } finally {
    devLoading.value = false
  }
}

function clearResult() {
  devResult.value = null
  devError.value = null
}

function onAddMeal() {
  router.push('/log')
}

function onSavePlan() {
  console.log('saved tomorrow plan')
}

function onUpgrade(payload) {
  openPaywall.value = false
  console.log('selected plan:', payload?.plan)
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