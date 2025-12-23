<template>
  <TopBarNavigation />
  <AppShell title="이번 주 식단 리포트" :subtitle="periodLabel" footerTheme="brand" @primary="onAddMeal">
    <div class="grid">
      <div class="colMain">
        <div class="report-controls" style="display:flex; flex-direction:column; gap:10px; margin-bottom:12px;">
          <div style="display:flex; align-items:center; gap:12px;">
            <ReportTabs :mode="mode" @update:mode="updateMode" />
            <div style="flex:1"></div>
            <!-- locate button: go to default (yesterday / last week) -->
            <button class="locateBtn" @click="goToDefault" title="초기 위치로 이동" aria-label="초기 위치로 이동">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <circle cx="12" cy="12" r="7" stroke="currentColor" stroke-width="1.5" />
                <circle cx="12" cy="12" r="2" fill="currentColor" />
              </svg>
            </button>
          </div>

          <!-- Horizontal date / week selector -->
          <div class="pickerStrip">
            <div class="stripInner" ref="stripRef">
              <button v-for="d in pickerItems" :key="d.key" class="stripItem" :class="['stripItem', { active: d.key===currentKey, week: mode==='weekly' } ]" @click="onSelectItem(d)">
                <div class="itemTop">{{ d.labelTop }}</div>
                <div class="itemBottom">{{ d.labelBottom }}</div>
                <!-- small contextual boxes for yesterday/tomorrow or prev/next week -->
                <div v-if="d.isYesterday" class="subBadge">어제</div>
                <div v-if="d.isTomorrow" class="subBadge">내일</div>
                <div v-if="d.isPrevWeek" class="subBadge">저번주</div>
                <div v-if="d.isNextWeek" class="subBadge">다음주</div>
                <div v-if="d.isToday" class="badge">오늘</div>
                <div v-if="d.isThisWeek" class="badge">이번주</div>
              </button>
            </div>
          </div>
        </div>
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
import { ref, computed, watch } from 'vue'
import { ref as _ref } from 'vue'
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
import ReportTabs from '@/components/report/ReportTabs.vue'
// Date/Week pickers implemented inline below; removed Fancy components

const router = useRouter()

// period label shows currently selected date or week range
function fmtKoreanDate(iso){ const d=new Date(iso+'T00:00:00'); return `${d.getMonth()+1}월 ${d.getDate()}일` }
const periodLabel = computed(()=>{
  if(mode.value === 'daily') return fmtKoreanDate(selectedDate.value)
  const start = new Date(selectedWeekStart.value + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate()+6)
  return `${start.getMonth()+1}월 ${start.getDate()}일 – ${end.getMonth()+1}월 ${end.getDate()}일`
})
const mode = ref('daily')

// selectedDate: ISO YYYY-MM-DD (local, avoid toISOString timezone shifts)
function isoDate(d){ const y=d.getFullYear(); const m=String(d.getMonth()+1).padStart(2,'0'); const day=String(d.getDate()).padStart(2,'0'); return `${y}-${m}-${day}` }
const today = new Date()
const yesterday = new Date(today); yesterday.setDate(today.getDate()-1)
const selectedDate = ref(isoDate(yesterday))

// selectedWeekStart: ISO date for week-start (use Monday)
function startOfWeek(d){ const dd = new Date(d); const day = dd.getDay(); const diff = (day===0? -6 : 1 - day); dd.setDate(dd.getDate()+diff); dd.setHours(0,0,0,0); return dd }
const lastWeekStart = startOfWeek(new Date()); lastWeekStart.setDate(lastWeekStart.getDate()-7)
const selectedWeekStart = ref(isoDate(lastWeekStart))

import { onMounted } from 'vue'
import { nextTick } from 'vue'
import { ref as localRef } from 'vue'

// strip ref for centering selected item
const stripRef = localRef(null)

// current key used for active class (ISO)
const currentKey = computed(() => mode.value === 'daily' ? selectedDate.value : selectedWeekStart.value)

// generate picker items depending on mode
const pickerItems = computed(() => {
  if (mode.value === 'daily') {
    const list = []
    const center = new Date(selectedDate.value + 'T00:00:00')
    const todayIsoLocal = isoDate(new Date())
    const y = new Date(); y.setDate(y.getDate()-1); const yesterdayIsoLocal = isoDate(y)
    const t = new Date(); t.setDate(t.getDate()+1); const tomorrowIsoLocal = isoDate(t)
    for (let i = -3; i <= 3; i++) {
      const d = new Date(center)
      d.setDate(center.getDate() + i)
      const key = isoDate(d)
      const dow = ['일','월','화','수','목','금','토'][d.getDay()]
      list.push({ key, labelTop: dow, labelBottom: `${d.getMonth()+1}/${d.getDate()}`, date: key, isToday: key === todayIsoLocal, isYesterday: key === yesterdayIsoLocal, isTomorrow: key === tomorrowIsoLocal })
    }
    return list
  } else {
    // weeks: show 7 weeks centered on selectedWeekStart
    const list = []
    const center = new Date(selectedWeekStart.value + 'T00:00:00')
    const thisWeekStartIso = isoDate(startOfWeek(new Date()))
    const prev = new Date(thisWeekStartIso + 'T00:00:00'); prev.setDate(prev.getDate()-7); const prevWeekStartIso = isoDate(prev)
    const next = new Date(thisWeekStartIso + 'T00:00:00'); next.setDate(next.getDate()+7); const nextWeekStartIso = isoDate(next)
    for (let i = -3; i <= 3; i++) {
      const start = new Date(center)
      start.setDate(center.getDate() + i*7)
      const end = new Date(start); end.setDate(start.getDate()+6)
      const key = isoDate(start)
      // compute ISO week number for the start date
      const { week } = getISOWeekNumber(key)
      const startLabel = `${start.getMonth()+1}/${start.getDate()}`
      const endLabel = `${end.getMonth()+1}/${end.getDate()}`
      // top: 'xx주차', bottom: 'M/D - M/D'
      list.push({ key, labelTop: `${week}주차`, labelBottom: `${startLabel} - ${endLabel}`, weekStart: key, isThisWeek: key === thisWeekStartIso, isPrevWeek: key === prevWeekStartIso, isNextWeek: key === nextWeekStartIso })
    }
    return list
  }
})

// week number and label helper
function getISOWeekNumber(dIn){
  const d = new Date(dIn + 'T00:00:00')
  // Copy date so don't modify original
  const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
  // Set to nearest Thursday: current date + 4 - current day number
  const dayNum = date.getUTCDay() || 7
  date.setUTCDate(date.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(),0,1))
  const weekNo = Math.ceil((((date - yearStart) / 86400000) + 1)/7)
  return { year: date.getUTCFullYear(), week: weekNo }
}

const currentWeekLabel = computed(()=>{
  if(mode.value !== 'weekly') return ''
  const start = new Date(selectedWeekStart.value + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate()+6)
  const { year, week } = getISOWeekNumber(selectedWeekStart.value)
  return `${year}년 ${String(week).padStart(2,'0')}주 (${start.getMonth()+1}/${start.getDate()}–${end.getMonth()+1}/${end.getDate()})`
})

function onSelectItem(item){
  if (mode.value === 'daily'){
    selectedDate.value = item.date
    fetchDaily(selectedDate.value)
  } else {
    selectedWeekStart.value = item.weekStart
    fetchWeekly(selectedWeekStart.value)
  }
}

function goToDefault(){
  // go to yesterday or last week depending on mode
  const now = new Date()
  const y = new Date(now); y.setDate(now.getDate()-1)
  const yesterdayIsoLocal = isoDate(y)
  const lw = startOfWeek(new Date()); lw.setDate(lw.getDate()-7)
  const lastWeekIso = isoDate(lw)

  if (mode.value === 'daily'){
    selectedDate.value = yesterdayIsoLocal
    fetchDaily(selectedDate.value)
  } else {
    selectedWeekStart.value = lastWeekIso
    fetchWeekly(selectedWeekStart.value)
  }
  // center after updating
  nextTick(() => setTimeout(() => centerSelected(), 60))
}

// center selected element on changes
async function centerSelected(){
  await nextTick()
  const strip = stripRef.value
  if (!strip) return
  const active = strip.querySelector('.stripItem.active')
  if (!active) return
  active.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
}

// watch selection to auto-center
watch(() => currentKey.value, () => { centerSelected() })

async function fetchDaily(date){
  devError.value = null
  devLoading.value = true
  try{
    const res = await api.get(`/reports/daily?date=${date}`)
    devResult.value = res.data
  }catch(e){
    if (e?.response?.status === 404) {
      devResult.value = null
      devError.value = '해당 날짜의 리포트가 없습니다.'
    } else {
      devError.value = e?.response?.data || e.message
    }
  }finally{ devLoading.value = false }
}

async function fetchWeekly(weekStart){
  devError.value = null
  devLoading.value = true
  try{
    const res = await api.get(`/reports/weekly?weekStart=${weekStart}`)
    devResult.value = res.data
  }catch(e){
    if (e?.response?.status === 404) {
      devResult.value = null
      devError.value = '해당 주차의 리포트가 없습니다.'
    } else {
      devError.value = e?.response?.data || e.message
    }
  }finally{ devLoading.value = false }
}

onMounted(()=>{
  // 기본: 어제 / 저번 주
  mode.value = 'daily' // ensure default mode is daily
  // ensure selectedDate is yesterday (local) and fetch
  const now = new Date()
  const y = new Date(now); y.setDate(now.getDate()-1)
  selectedDate.value = isoDate(y)
  // ensure selected week start is last week as well
  const lw = startOfWeek(new Date()); lw.setDate(lw.getDate()-7)
  selectedWeekStart.value = isoDate(lw)
  fetchDaily(selectedDate.value)
  // center selected in strip after initial render; call twice with small delay to cover rendering timing
  centerSelected()
  setTimeout(() => centerSelected(), 80)
})

function updateMode(v){
  mode.value = v
  if(v === 'daily') {
    fetchDaily(selectedDate.value)
  } else {
    fetchWeekly(selectedWeekStart.value)
  }
  // ensure newly selected item is centered after mode switch
  nextTick(() => setTimeout(() => centerSelected(), 60))
}

function doFetch(){
  if(mode.value === 'daily') fetchDaily(selectedDate.value)
  else fetchWeekly(selectedWeekStart.value)
}
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
    if(mode.value === 'daily'){
      const res = await api.post('/reports/daily', { date: selectedDate.value })
      devResult.value = res.data
    } else {
      const res = await api.post('/reports/weekly', { weekStart: selectedWeekStart.value })
      devResult.value = res.data
    }
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

.pickerStrip{ overflow-x:auto; -webkit-overflow-scrolling:touch; display:flex; justify-content:center; }
.stripInner{ display:inline-flex; gap:10px; padding:6px 4px; justify-content:center; }
/* fixed box size for both daily and weekly to avoid layout jumps */
.stripItem{ width:112px; height:96px; padding:12px 10px; border-radius:12px; border:1px solid var(--border); background:#fff; cursor:pointer; display:flex; flex-direction:column; align-items:center; justify-content:center; gap:6px; box-sizing: border-box; }
.stripItem .itemTop{ font-weight:900; color:var(--muted); font-size:12px; line-height:1 }
.stripItem .itemBottom{ font-weight:900; font-size:14px; line-height:1; white-space:nowrap }

/* weekly: top shows week number, emphasized color */

.stripItem.week .itemTop{ font-size:12px; font-weight:900; color:var(--muted) }
.stripItem.week .itemBottom{ font-size:14px; margin-top:2px }

/* active (visual emphasis without changing box size) */
.stripItem.active{ background:linear-gradient(90deg,#f0f7ff,#eef9ff); border-color:rgba(47,107,255,.18); box-shadow:0 8px 20px rgba(47,107,255,.06) }
.stripItem.active.week{ box-shadow:0 12px 26px rgba(47,107,255,.10) }

/* daily selected: make bottom (date) larger for emphasis */
.stripItem:not(.week).active .itemBottom{ font-size:18px; font-weight:1000; color:inherit }
.stripItem:not(.week).active .itemTop{ font-size:13px; color:var(--muted) }

/* badge for today / this week */
.stripItem{ position:relative }
.badge{
  position:absolute;
  bottom:5px;
  left:50%;
  transform:translateX(-50%);
  background: rgba(255,255,255,0.98);
  border: 1px solid rgba(16,24,40,0.06);
  padding: 4px 8px;
  border-radius: 999px;
  font-size:11px;
  color:var(--muted);
  box-shadow: 0 6px 14px rgba(16,24,40,0.06);
  z-index: 3;
  pointer-events: none;
}

/* small contextual badge shown above the main badge (어제/내일/저번주/다음주) */
.subBadge{
  position:absolute;
  bottom:5px; /* same vertical placement as main badge */
  left:50%;
  transform:translateX(-50%);
  background: rgba(255,255,255,0.98);
  border: 1px solid rgba(16,24,40,0.06);
  padding: 4px 8px;
  border-radius: 999px;
  font-size:11px;
  color:var(--muted);
  box-shadow: 0 6px 14px rgba(16,24,40,0.06);
  z-index: 3;
  pointer-events: none;
}

/* ensure subBadge and badge stack predictably when both present */
.stripItem .badge{ z-index:4 }

/* ensure badge doesn't overlap when item is active */
.stripItem.active .badge{ transform: translateX(-50%) translateY(-4px); }

@media (min-width: 768px) {
  .insights {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 767px) {
  .stripItem{ width:88px; height:80px; padding:10px }
  .stripInner{ gap:8px }
}

/* locate button style */
.locateBtn{ width:36px; height:36px; border-radius:10px; border:1px solid rgba(16,24,40,0.06); background:#fff; display:inline-flex; align-items:center; justify-content:center; cursor:pointer; color:var(--muted); }
.locateBtn:hover{ background: #f6f9ff; color: #2f6bff }

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