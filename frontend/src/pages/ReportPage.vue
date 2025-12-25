<template>
  <TopBarNavigation />
  <AppShell title="식단 리포트" :subtitle="periodLabel" footerTheme="brand" @primary="onAddMeal">
    <div class="grid">
      <div class="colMain">
        <div class="report-controls" style="display:flex; flex-direction:column; gap:10px; margin-bottom:12px;">
          <div style="display:flex; align-items:center; gap:12px;">
            <ReportTabs :mode="mode" @update:mode="updateMode" />
            <div style="flex:1"></div>
            <!-- create button moved to header (right) -->
            <BaseButton :disabled="selectionState !== 'today'" variant="primary" @click="openCreateModal = true"
              style="margin-right:8px; width: 76%; align-items: center;">리포트 생성</BaseButton>
            <!-- locate button: go to default (yesterday / last week) -->
            <button class="locateBtn" @click="goToDefault" title="초기 위치로 이동" aria-label="초기 위치로 이동">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg"
                aria-hidden="true">
                <circle cx="12" cy="12" r="7" stroke="currentColor" stroke-width="1.5" />
                <circle cx="12" cy="12" r="2" fill="currentColor" />
              </svg>
            </button>
          </div>

          <!-- Horizontal date / week selector -->
          <div class="pickerStrip">
            <div class="stripInner" ref="stripRef">
              <button v-for="d in pickerItems" :key="d.key" class="stripItem"
                :class="['stripItem', { active: d.key === currentKey, week: mode === 'weekly' }]"
                @click="onSelectItem(d)">
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
        <!-- 상단으로 이동: 생성 버튼 제거(중복) 및 결과 초기화 버튼 제거 -->
        <ReportHero :score="score" :period-label="periodLabel" :summary-title="displayHeroTitle"
          :summary-line="displayHeroLine" />

        <div style="margin-top:12px;">
          <div v-if="devResult && displayInsights.length > 0" class="insights">
            <!-- Render in explicit order: good, warn on first row; keep + paywall on second row -->
            <InsightCard v-if="orderedInsights[0]" :kind="orderedInsights[0].kind" :title="orderedInsights[0].title" :body="orderedInsights[0].body" />
            <InsightCard v-if="orderedInsights[1]" :kind="orderedInsights[1].kind" :title="orderedInsights[1].title" :body="orderedInsights[1].body" />

            <InsightCard v-if="orderedInsights[2]" :kind="orderedInsights[2].kind" :title="orderedInsights[2].title" :body="orderedInsights[2].body" />

            <!-- Use existing AdvancedPreview (더 자세한 분석 보기) next to `keep` -->
            <AdvancedPreview @open="openPaywall = true" />
          </div>
          <!-- When there's no devResult, hero displays the empty-state (title/score) so no extra placeholder here -->
        </div>

        <!-- AdvancedPreview moved into insights area next to 'keep' -->
      </div>

      <div class="colRail">
        <template v-if="devResult">
          <CoachCard :message="displayCoachMessage" />
          <NextActionCard :action-text="displayNextAction" @save="onSavePlan" @register="onRegisterAsChallenge" />
        </template>
        <template v-else>
          <!-- keep rail visually balanced when empty -->
          <div style="min-height:160px"></div>
        </template>
      </div>
    </div>

    <PaywallModal :open="openPaywall" @close="openPaywall = false" @upgrade="onUpgrade" />
    <CreateReportModal :open="openCreateModal" :mode="mode" :date="selectedDate" :weekStart="selectedWeekStart"
      :selectionState="selectionState" @close="handleModalClose" @created="handleModalCreated"
      @error="handleModalError" />
    <ChallengeCreateModal :show="showChallengeModal" :initialData="{ items: challengeInitialItems, goalDetails: challengeInitialGoalDetails }" @close="showChallengeModal = false" @create="handleCreateChallenge" />
    <ToastContainer />
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
import ChallengeCreateModal from '@/components/challenge/ChallengeCreateModal.vue'
import { parseNumberedList } from '@/utils/parseReportSuggestions'
import CoachCard from '@/components/report/CoachCard.vue'
import AdvancedPreview from '@/components/report/AdvancedPreview.vue'
import PaywallModal from '@/components/paywall/PaywallModal.vue'
import BaseButton from '@/components/base/BaseButton.vue'
import ReportTabs from '@/components/report/ReportTabs.vue'
import CreateReportModal from '@/components/report/CreateReportModal.vue'
import ToastContainer from '@/components/ui/ToastContainer.vue'
import { showToast } from '@/lib/toast.js'
// Date/Week pickers implemented inline below; removed Fancy components
import { defaultCoachMessage, defaultNextAction, defaultInsights, noReportMessage } from '@/lib/reportDefaults.js'
import BaseCard from '@/components/base/BaseCard.vue'

const router = useRouter()

// period label shows currently selected date or week range
function fmtKoreanDate(iso) { const d = new Date(iso + 'T00:00:00'); return `${d.getMonth() + 1}월 ${d.getDate()}일` }
const periodLabel = computed(() =>
{
  if (mode.value === 'daily') return fmtKoreanDate(selectedDate.value)
  const start = new Date(selectedWeekStart.value + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate() + 6)
  return `${start.getMonth() + 1}월 ${start.getDate()}일 – ${end.getMonth() + 1}월 ${end.getDate()}일`
})
const mode = ref('daily')

// selectedDate: ISO YYYY-MM-DD (local, avoid toISOString timezone shifts)
function isoDate(d) { const y = d.getFullYear(); const m = String(d.getMonth() + 1).padStart(2, '0'); const day = String(d.getDate()).padStart(2, '0'); return `${y}-${m}-${day}` }
const today = new Date()
const yesterday = new Date(today); yesterday.setDate(today.getDate() - 1)
const selectedDate = ref(isoDate(yesterday))

// selectedWeekStart: ISO date for week-start (use Monday)
function startOfWeek(d) { const dd = new Date(d); const day = dd.getDay(); const diff = (day === 0 ? -6 : 1 - day); dd.setDate(dd.getDate() + diff); dd.setHours(0, 0, 0, 0); return dd }
const lastWeekStart = startOfWeek(new Date()); lastWeekStart.setDate(lastWeekStart.getDate() - 7)
const selectedWeekStart = ref(isoDate(lastWeekStart))

import { onMounted } from 'vue'
import { nextTick } from 'vue'
import { ref as localRef } from 'vue'

// strip ref for centering selected item
const stripRef = localRef(null)

// current key used for active class (ISO)
const currentKey = computed(() => mode.value === 'daily' ? selectedDate.value : selectedWeekStart.value)

// generate picker items depending on mode
const pickerItems = computed(() =>
{
  if (mode.value === 'daily') {
    const list = []
    const center = new Date(selectedDate.value + 'T00:00:00')
    const todayIsoLocal = isoDate(new Date())
    const y = new Date(); y.setDate(y.getDate() - 1); const yesterdayIsoLocal = isoDate(y)
    const t = new Date(); t.setDate(t.getDate() + 1); const tomorrowIsoLocal = isoDate(t)
    for (let i = -3; i <= 3; i++) {
      const d = new Date(center)
      d.setDate(center.getDate() + i)
      const key = isoDate(d)
      const dow = ['일', '월', '화', '수', '목', '금', '토'][d.getDay()]
      list.push({ key, labelTop: dow, labelBottom: `${d.getMonth() + 1}/${d.getDate()}`, date: key, isToday: key === todayIsoLocal, isYesterday: key === yesterdayIsoLocal, isTomorrow: key === tomorrowIsoLocal })
    }
    return list
  } else {
    // weeks: show 7 weeks centered on selectedWeekStart
    const list = []
    const center = new Date(selectedWeekStart.value + 'T00:00:00')
    const thisWeekStartIso = isoDate(startOfWeek(new Date()))
    const prev = new Date(thisWeekStartIso + 'T00:00:00'); prev.setDate(prev.getDate() - 7); const prevWeekStartIso = isoDate(prev)
    const next = new Date(thisWeekStartIso + 'T00:00:00'); next.setDate(next.getDate() + 7); const nextWeekStartIso = isoDate(next)
    for (let i = -3; i <= 3; i++) {
      const start = new Date(center)
      start.setDate(center.getDate() + i * 7)
      const end = new Date(start); end.setDate(start.getDate() + 6)
      const key = isoDate(start)
      // compute ISO week number for the start date
      const { week } = getISOWeekNumber(key)
      const startLabel = `${start.getMonth() + 1}/${start.getDate()}`
      const endLabel = `${end.getMonth() + 1}/${end.getDate()}`
      // top: 'xx주차', bottom: 'M/D - M/D'
      list.push({ key, labelTop: `${week}주차`, labelBottom: `${startLabel} - ${endLabel}`, weekStart: key, isThisWeek: key === thisWeekStartIso, isPrevWeek: key === prevWeekStartIso, isNextWeek: key === nextWeekStartIso })
    }
    return list
  }
})

// week number and label helper
function getISOWeekNumber(dIn)
{
  const d = new Date(dIn + 'T00:00:00')
  // Copy date so don't modify original
  const date = new Date(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()))
  // Set to nearest Thursday: current date + 4 - current day number
  const dayNum = date.getUTCDay() || 7
  date.setUTCDate(date.getUTCDate() + 4 - dayNum)
  const yearStart = new Date(Date.UTC(date.getUTCFullYear(), 0, 1))
  const weekNo = Math.ceil((((date - yearStart) / 86400000) + 1) / 7)
  return { year: date.getUTCFullYear(), week: weekNo }
}

const currentWeekLabel = computed(() =>
{
  if (mode.value !== 'weekly') return ''
  const start = new Date(selectedWeekStart.value + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate() + 6)
  const { year, week } = getISOWeekNumber(selectedWeekStart.value)
  return `${year}년 ${String(week).padStart(2, '0')}주 (${start.getMonth() + 1}/${start.getDate()}–${end.getMonth() + 1}/${end.getDate()})`
})

function onSelectItem(item)
{
  if (mode.value === 'daily') {
    selectedDate.value = item.date
    fetchDaily(selectedDate.value)
  } else {
    selectedWeekStart.value = item.weekStart
    fetchWeekly(selectedWeekStart.value)
  }
}

function goToDefault()
{
  // go to yesterday or last week depending on mode
  const now = new Date()
  const y = new Date(now); y.setDate(now.getDate() - 1)
  const yesterdayIsoLocal = isoDate(y)
  const lw = startOfWeek(new Date()); lw.setDate(lw.getDate() - 7)
  const lastWeekIso = isoDate(lw)

  if (mode.value === 'daily') {
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
async function centerSelected()
{
  await nextTick()
  const strip = stripRef.value
  if (!strip) return
  const active = strip.querySelector('.stripItem.active')
  if (!active) return
  try {
    active.scrollIntoView({ behavior: 'smooth', inline: 'center', block: 'nearest' })
  } catch (err) {
    // element may have been detached concurrently; ignore to avoid uncaught errors
    console.warn('[ReportPage] centerSelected scrollIntoView failed', err)
  }
}

// watch selection to auto-center
watch(() => currentKey.value, () => { centerSelected() })

async function fetchDaily(date)
{
  devError.value = null
  devLoading.value = true
  try {
    const res = await api.get(`/reports/daily?date=${date}`)
    devResult.value = res.data
  } catch (e) {
    if (e?.response?.status === 404) {
      devResult.value = null
      devError.value = '해당 날짜의 리포트가 없습니다.'
      // 리포트가 없을 때 히어로/점수 리셋
      heroTitle.value = noReportMessage
      heroLine.value = ''
      score.value = 0
    } else {
      devError.value = e?.response?.data || e.message
      // 기타 에러일 때도 히어로를 안전하게 초기화
      heroTitle.value = noReportMessage
      heroLine.value = ''
      score.value = 0
    }
  } finally { devLoading.value = false }
}

async function fetchWeekly(weekStart)
{
  devError.value = null
  devLoading.value = true
  try {
    // backend expects `fromDate` query param (week start YYYY-MM-DD)
    const res = await api.get(`/reports/weekly?fromDate=${weekStart}`)
    devResult.value = res.data
  } catch (e) {
    if (e?.response?.status === 404) {
      devResult.value = null
      devError.value = '해당 주차의 리포트가 없습니다.'
      heroTitle.value = noReportMessage
      heroLine.value = ''
      score.value = 0
    } else {
      devError.value = e?.response?.data || e.message
      heroTitle.value = noReportMessage
      heroLine.value = ''
      score.value = 0
    }
  } finally { devLoading.value = false }
}

onMounted(() =>
{
  // 기본: 어제 / 저번 주
  mode.value = 'daily' // ensure default mode is daily
  // ensure selectedDate is yesterday (local) and fetch
  const now = new Date()
  const y = new Date(now); y.setDate(now.getDate() - 1)
  selectedDate.value = isoDate(y)
  // ensure selected week start is last week as well
  const lw = startOfWeek(new Date()); lw.setDate(lw.getDate() - 7)
  selectedWeekStart.value = isoDate(lw)
  fetchDaily(selectedDate.value)
  // center selected in strip after initial render; call twice with small delay to cover rendering timing
  centerSelected()
  setTimeout(() => centerSelected(), 80)
})

function updateMode(v)
{
  mode.value = v
  if (v === 'daily') {
    fetchDaily(selectedDate.value)
  } else {
    fetchWeekly(selectedWeekStart.value)
  }
  // ensure newly selected item is centered after mode switch
  nextTick(() => setTimeout(() => centerSelected(), 60))
}

function doFetch()
{
  if (mode.value === 'daily') fetchDaily(selectedDate.value)
  else fetchWeekly(selectedWeekStart.value)
}
const score = ref(78)
const heroTitle = ref('이번 주는 꽤 괜찮았어요 🙂')
const heroLine = ref('전체적으로 괜찮았어요. 간식 타이밍만 조금 아쉬워요.')

// UI용 가공 제목/부제: 모드(일간/주간)에 따라 문구 조정
const displayHeroTitle = computed(() =>
{
  const t = (heroTitle.value || '').trim()
  if (!t) return mode.value === 'daily' ? '오늘은 아직 리포트가 없습니다.' : '이번 주는 아직 리포트가 없습니다.'

  if (mode.value === 'daily') {
    // 이미 '오늘' 계열이면 그대로, '이번 주' 계열은 '오늘'으로 변환, 그 외는 '오늘은 '을 앞에 붙임
    if (/오늘/.test(t)) return t
    if (/이번\s*주/.test(t) || /주차/.test(t) || /주/.test(t) && /주/.test(t)) return t.replace(/이번\s*주/g, '오늘의')
    return `오늘은 ${t}`
  } else {
    // weekly
    if (/이번/.test(t) || /주차/.test(t)) return t
    if (/오늘/.test(t)) return t.replace(/오늘/g, '이번 주는')
    return `이번 주는 ${t}`
  }
})

const displayHeroLine = computed(() =>
{
  const l = (heroLine.value || '').trim()
  if (!l) return ''
  return l
})

const openPaywall = ref(false)
const openCreateModal = ref(false)
const showChallengeModal = ref(false)
const challengeInitialItems = ref([])
const challengeInitialGoalDetails = ref(null)
const devResult = ref(null)
const devError = ref(null)
const devLoading = ref(false)
const analyzeLoading = ref(false)
const analyzeResult = ref(null)

// 🔥 수정: handleCreateChallenge
async function handleCreateChallenge(payload)
{
  try {
    console.debug('[ReportPage] handleCreateChallenge payload:', payload)
    
    // 🔥 goalDetails를 JSON 문자열로 변환
    const body = {
      title: payload.title,
      description: payload.description,
      goalType: payload.goalType,
      goalDetails: payload.goalDetails,
      startDate: payload.startDate,
      durationDays: payload.durationDays,
      items: payload.items,
      source: payload.source,
      sourceId: payload.sourceId
    }
    
    console.debug('[ReportPage] Sending to backend:', body)
    
    const res = await api.post('/challenges', payload)
    
    showToast('챌린지가 생성되었습니다.')
    showChallengeModal.value = false
    
    // 선택적: 챌린지 페이지로 이동
    // router.push(`/challenges/${res.data.challengeId}`)
  } catch (e) {
    console.error('[ReportPage] Failed to create challenge:', e)
    const errorMsg = e?.response?.data?.error || e?.message || '챌린지 생성에 실패했습니다.'
    showToast(errorMsg, 'error')
  }
}

// 업데이트: devResult가 들어오면 top-level 값을 우선 사용하고, 없으면 aiResponse 문자열을 파싱해서 채웁니다.
watch(devResult, (val) =>
{
  // 값이 있을 때만 처리 (값이 없을 때는 기존 UI 상태 유지)
  if (!val) return

  // score
  if (val.score !== undefined && val.score !== null) {
    score.value = val.score
  } else if (val.aiResponse) {
    try {
      const parsed = typeof val.aiResponse === 'string' ? JSON.parse(val.aiResponse) : val.aiResponse
      if (parsed && parsed.score !== undefined) score.value = parsed.score
    } catch (e) {
      // 파싱 실패 시 기존 score 유지
    }
  }

  // heroTitle / heroLine
  if (val.heroTitle) {
    heroTitle.value = val.heroTitle
    heroLine.value = val.heroLine || heroLine.value
  } else if (val.aiResponse) {
    try {
      const parsed = typeof val.aiResponse === 'string' ? JSON.parse(val.aiResponse) : val.aiResponse
      if (parsed?.heroTitle) heroTitle.value = parsed.heroTitle
      if (parsed?.heroLine) heroLine.value = parsed.heroLine
    } catch (e) {
      // 파싱 실패 시 기존 값 유지
    }
  }

  if (val.overallAssessment) overallAssessment.value = val.overallAssessment
})

// insights에서 coach, action 추출
// determine whether selected period is in the past, today, or future
const selectionState = computed(() =>
{
  const todayIso = isoDate(new Date())
  if (mode.value === 'daily') {
    const sel = selectedDate.value
    if (sel < todayIso) return 'past'
    if (sel === todayIso) return 'today'
    return 'future'
  } else {
    // weekly: compare week range to today
    const start = selectedWeekStart.value
    const s = new Date(start + 'T00:00:00')
    const e = new Date(s); e.setDate(s.getDate() + 6)
    const endIso = isoDate(e)
    if (endIso < todayIso) return 'past'
    if (start > todayIso) return 'future'
    return 'today'
  }
})

const displayCoachMessage = computed(() =>
{
  if (!devResult.value) return ''
  if (devResult.value?.coachMessage) return devResult.value.coachMessage
  const coach = devResult.value.insights?.find(i => i.kind === 'coach')
  return coach?.body || ''
})

const displayNextAction = computed(() =>
{
  if (!devResult.value) return ''
  if (devResult.value?.nextAction) return devResult.value.nextAction
  const action = devResult.value.insights?.find(i => i.kind === 'action')
  return action?.body || ''
})

// good, warn, keep만 필터링
const displayInsights = computed(() =>
{
  if (!devResult.value?.insights) return []
  return devResult.value.insights.filter(i =>
    i.kind === 'good' || i.kind === 'warn' || i.kind === 'keep'
  )
})

// orderedInsights: prefer ordering good, warn, keep (take first of each group)
const orderedInsights = computed(() => {
  if (!devResult.value?.insights) return []
  const goods = devResult.value.insights.filter(i => i.kind === 'good')
  const warns = devResult.value.insights.filter(i => i.kind === 'warn')
  const keeps = devResult.value.insights.filter(i => i.kind === 'keep')
  const out = []
  if (goods.length) out.push(goods[0])
  if (warns.length) out.push(warns[0])
  if (keeps.length) out.push(keeps[0])
  return out
})

async function createAndAnalyze()
{
  devError.value = null
  devResult.value = null
  analyzeResult.value = null
  devLoading.value = true
  analyzeLoading.value = false
  try {
    if (mode.value === 'daily') {
      const res = await api.post('/reports/daily', { date: selectedDate.value })
      devResult.value = res.data
    } else {
      // backend createWeekly expects fromDate/toDate in body
      const start = new Date(selectedWeekStart.value + 'T00:00:00')
      const end = new Date(start); end.setDate(start.getDate() + 6)
      const res = await api.post('/reports/weekly', { fromDate: selectedWeekStart.value, toDate: isoDate(end) })
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

// show errors via toast when devError changes
watch(devError, (v) =>
{
  if (v) {
    const msg = typeof v === 'string' ? v : (v?.error || JSON.stringify(v))
    showToast(msg, 'error')
  }
})

function clearResult()
{
  devResult.value = null
  devError.value = null
  // clear 버튼은 UI를 초기 상태(리포트 없음)로 되돌림
  heroTitle.value = noReportMessage
  heroLine.value = ''
  score.value = 0
}

function onAddMeal()
{
  router.push('/log')
}

function onSavePlan()
{
  console.log('saved tomorrow plan')
}

function onRegisterAsChallenge() {
  const txt = displayNextAction.value || ''
  const items = parseNumberedList(txt)
  const goalDetails = parseItemsToGoalDetails(items)
  console.debug('[ReportPage] Registering as challenge:', {
    items,
    goalDetails
  })
  challengeInitialItems.value = items
  // Attempt to extract explicit goal numbers from the parsed items
  challengeInitialGoalDetails.value = parseItemsToGoalDetails(items)
  showChallengeModal.value = true
}

// parse items like "탄수화물 244g 더 섭취" -> { carbs: '244g' }
function parseItemsToGoalDetails(items) {
  if (!items || !items.length) return null
  const details = {}

  // keywords and regexes
  const kw = '(칼로리|열량|kcal|calories?|단백질|탄수화물|지방|체중)'
  const reBefore = new RegExp(kw + "\\s*[:\\-]?\\s*(\\d+(?:\\.\\d+)?)(?:\\s*(g|kg|kcal))?", 'i')
  const reAfter = new RegExp("(\\d+(?:\\.\\d+)?)(?:\\s*(g|kg|kcal))?\\s*" + kw, 'i')
  const reOnlyKcal = /(\d+(?:\.\d+)?)\s*(kcal)/i

  // collect calorie candidates with simple priority scoring
  const calorieCandidates = []

  for (const it of items) {
    const text = (it.text || it).toString()
    let m = text.match(reBefore)
    let key, val, unit
    if (m) {
      key = m[1]
      val = m[2]
      unit = (m[3] || '').toLowerCase()
    } else {
      m = text.match(reAfter)
      if (m) {
        val = m[1]
        unit = (m[2] || '').toLowerCase()
        key = m[3]
      } else {
        const mm = text.match(reOnlyKcal)
        if (mm) {
          val = mm[1]
          unit = (mm[2] || '').toLowerCase()
          key = 'kcal'
        }
      }
    }

    if (!val) {
      // attempt to extract other nutrient values (protein/carbs/fat/weight) only when keywords exist
      const textLower = text.toLowerCase()
      const numMatch = text.match(/(\d+(?:\.\d+)?)(?:\s*(g|kg|kcal))?/)
      if (textLower.includes('단백질') && numMatch) {
        details.protein = `${numMatch[1]}${(numMatch[2]||'g')}`
      }
      if (textLower.includes('탄수') && numMatch) {
        details.carbs = `${numMatch[1]}${(numMatch[2]||'g')}`
      }
      if (textLower.includes('지방') && numMatch) {
        details.fat = `${numMatch[1]}${(numMatch[2]||'g')}`
      }
      if ((textLower.includes('체중') || (numMatch && /kg/i.test(numMatch[2]||''))) && numMatch) {
        details.weight = `${numMatch[1]}${(numMatch[2]||'kg')}`
      }
      continue
    }

    const textLower = text.toLowerCase()
    const numeric = Number(val)

    // compute priority: higher -> more likely overall goal
    let priority = 0
    // explicit goal wording
    if (/(목표|목표에|목표로|목표에 맞추|목표로 맞추|목표치|목표값)/.test(textLower)) priority += 100
    if (/(총|전체|전체적으로|전체 섭취)/.test(textLower)) priority += 50
    // negative weight for distribution/per-meal hints
    if (/(끼|분배|재분배|각|당|끼당|회당|나누)/.test(textLower)) priority -= 40
    if (/(재분배|분배)/.test(textLower)) priority -= 30
    // shorter contextual hints that imply per-meal
    if (/(3끼|3끼를|세끼|세 끼|끼당|한끼|한 끼)/.test(textLower)) priority -= 30

    // record candidate if it's calorie-like
    const k = (key || '').toString().toLowerCase()
    if (k.includes('칼로리') || k === 'kcal' || /calorie/.test(k)) {
      calorieCandidates.push({ value: numeric, unit: unit || 'kcal', text, priority })
      continue
    }

    // fallback: if keyword indicates other nutrient
    if (k.includes('단백질')) details.protein = `${val}${unit || 'g'}`
    else if (k.includes('탄수')) details.carbs = `${val}${unit || 'g'}`
    else if (k.includes('지방')) details.fat = `${val}${unit || 'g'}`
    else if (k.includes('체중') || unit === 'kg') details.weight = `${val}${unit || 'kg'}`
  }

  // choose best calorie candidate
  if (calorieCandidates.length) {
    calorieCandidates.sort((a, b) => {
      if (b.priority !== a.priority) return b.priority - a.priority
      return b.value - a.value
    })
    const best = calorieCandidates[0]
    details.calories = `${best.value}${best.unit || 'kcal'}`
  }

  return Object.keys(details).length ? details : null
}


// 중복된 onUpgrade 함수를 하나로 통합
function onUpgrade(payload) {
  openPaywall.value = false
  console.log('selected plan:', payload?.plan)
  alert(`${payload?.plan === 'yearly' ? '연간' : '월간'} 플랜 결제는 곧 준비할게요 🙂`)
}

function handleModalClose()
{
  openCreateModal.value = false
}

function handleModalCreated(payload)
{
  // payload is the created ReportDto from backend
  devResult.value = payload
  openCreateModal.value = false
}

function handleModalError(msg)
{
  devError.value = msg
}
</script>

<style scoped>
.placeholder {
  padding: 18px;
}

.ph-grid {
  display: flex;
  gap: 12px;
  align-items: center
}

.ph-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  background: linear-gradient(180deg, #fbfdff, #f6f8fb);
  border: 1px solid rgba(16, 24, 40, 0.04)
}

.ph-title {
  font-weight: 800;
  margin-bottom: 4px
}

.ph-sub {
  color: var(--muted);
  font-size: 13px
}

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

.pickerStrip {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  display: flex;
  justify-content: center;
}

.stripInner {
  display: inline-flex;
  gap: 10px;
  padding: 6px 4px;
  justify-content: center;
}

/* fixed box size for both daily and weekly to avoid layout jumps */
.stripItem {
  width: 112px;
  height: 96px;
  padding: 12px 10px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  box-sizing: border-box;
}

.stripItem .itemTop {
  font-weight: 900;
  color: var(--muted);
  font-size: 12px;
  line-height: 1
}

.stripItem .itemBottom {
  font-weight: 900;
  font-size: 14px;
  line-height: 1;
  white-space: nowrap
}

/* weekly: top shows week number, emphasized color */

.stripItem.week .itemTop {
  font-size: 12px;
  font-weight: 900;
  color: var(--muted)
}

.stripItem.week .itemBottom {
  font-size: 14px;
  margin-top: 2px
}

/* active (visual emphasis without changing box size) */
.stripItem.active {
  background: linear-gradient(90deg, #f0f7ff, #eef9ff);
  border-color: rgba(47, 107, 255, .18);
  box-shadow: 0 8px 20px rgba(47, 107, 255, .06)
}

.stripItem.active.week {
  box-shadow: 0 12px 26px rgba(47, 107, 255, .10)
}

/* daily selected: make bottom (date) larger for emphasis */
.stripItem:not(.week).active .itemBottom {
  font-size: 18px;
  font-weight: 1000;
  color: inherit
}

.stripItem:not(.week).active .itemTop {
  font-size: 13px;
  color: var(--muted)
}

/* badge for today / this week */
.stripItem {
  position: relative
}

.badge {
  position: absolute;
  bottom: 5px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(16, 24, 40, 0.06);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--muted);
  box-shadow: 0 6px 14px rgba(16, 24, 40, 0.06);
  z-index: 3;
  pointer-events: none;
}

/* small contextual badge shown above the main badge (어제/내일/저번주/다음주) */
.subBadge {
  position: absolute;
  bottom: 5px;
  /* same vertical placement as main badge */
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(16, 24, 40, 0.06);
  padding: 4px 8px;
  border-radius: 999px;
  font-size: 11px;
  color: var(--muted);
  box-shadow: 0 6px 14px rgba(16, 24, 40, 0.06);
  z-index: 3;
  pointer-events: none;
}

/* ensure subBadge and badge stack predictably when both present */
.stripItem .badge {
  z-index: 4
}

/* ensure badge doesn't overlap when item is active (keep same vertical position) */
.stripItem.active .badge {
  transform: translateX(-50%);
}

@media (min-width: 768px) {
  .insights {
    grid-template-columns: 1fr 1fr;
  }
}

.paywallCard {
  padding: 16px;
  border-radius: 12px;
  border: 1px solid var(--border);
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

@media (max-width: 767px) {
  .stripItem {
    width: 88px;
    height: 80px;
    padding: 10px
  }

  .stripInner {
    gap: 8px
  }
}

/* locate button style */
.locateBtn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid rgba(16, 24, 40, 0.06);
  background: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--muted);
}

.locateBtn:hover {
  background: #f6f9ff;
  color: #2f6bff
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