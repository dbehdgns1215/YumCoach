<template>
    <TopBarNavigation />
    <AppShell title="기록" :subtitle="subtitle" footerTheme="brand">
        <WeekStrip :week-start="weekStart" :selected-date="selectedDate" :records="recordDates"
            @select="selectDateAndShiftWeek($event)" @prev="shiftWeek(-7)" @next="shiftWeek(7)" />

        <div class="grid">
            <div class="colMain">
                <MealSection v-for="mealKey in mealKeys" :key="mealKey" :title="mealLabels[mealKey]"
                    :items="dayLog.meals[mealKey]" @add="openAdd(mealKey)" @remove="removeItem(mealKey, $event)"
                    @update-grams="updateGrams(mealKey, $event.rowId, $event.grams)" />
            </div>

            <div class="colRail">
                <DaySummaryCard :summary="daySummary" />
                <BaseCard>
                    <template #header>
                        <div class="railTitle">이번 주 요약</div>
                    </template>
                    <div class="railText">
                        주간 리포트는 <b>/report</b>에서 확인해요 🙂<br />
                        (여긴 기록 중심!)
                    </div>
                </BaseCard>
            </div>
        </div>

        <FoodAddModal :open="modalOpen" :meal-title="modalMealTitle" @close="modalOpen = false" @add="addFoodToMeal" />
    </AppShell>
</template>

<script setup>
import { computed, reactive, ref, onMounted } from 'vue'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import BaseCard from '@/components/base/BaseCard.vue'

import WeekStrip from '@/components/log/WeekStrip.vue'
import MealSection from '@/components/log/MealSection.vue'
import FoodAddModal from '@/components/log/FoodAddModal.vue'
import { createMeal, getMealsByDate, deleteMealItem } from '@/api/meals.js'
import DaySummaryCard from '@/components/log/DaySummaryCard.vue'

import { startOfWeek, formatDate, formatDateDot, addDays, today as getToday } from '@/utils/date'
import { sumNutrition } from '@/utils/nutrition'
import { transformMealsToUI } from '@/utils/mealTransform'
import { MEAL_KEYS, MEAL_LABELS, KEY_TO_MEAL_TYPE } from '@/constants/mealTypes'

const mealKeys = MEAL_KEYS
const mealLabels = MEAL_LABELS

// ---- 날짜/주간
const today = getToday()
const weekStart = ref(startOfWeek(today))
const selectedDate = ref(new Date(today.getFullYear(), today.getMonth(), today.getDate()))

const subtitle = computed(() => formatDateDot(selectedDate.value))

function shiftWeek(deltaDays)
{
    weekStart.value = addDays(weekStart.value, deltaDays)
    selectedDate.value = addDays(selectedDate.value, deltaDays)
}

function selectDateAndShiftWeek(date)
{
    selectedDate.value = new Date(date.getFullYear(), date.getMonth(), date.getDate())
    weekStart.value = startOfWeek(selectedDate.value)
    loadMealsForDate(selectedDate.value)
}

// ---- 로그 상태 (일단 메모리. 나중에 API/DB 연동)
const logsByDate = reactive({}) // { 'YYYY-MM-DD': { meals: { breakfast:[...], ... } } }

function emptyDay()
{
    return {
        meals: {
            breakfast: [],
            lunch: [],
            dinner: [],
            snack: [],
            latenight: [],
        },
    }
}

// 기록이 있는 날짜 맵
const recordDates = computed(() =>
{
    const dates = {}
    Object.keys(logsByDate).forEach(dateKey =>
    {
        dates[dateKey] = true
    })
    return dates
})

const dayKey = computed(() => formatDate(selectedDate.value))
const dayLog = computed(() =>
{
    if (!logsByDate[dayKey.value]) logsByDate[dayKey.value] = emptyDay()
    return logsByDate[dayKey.value]
})

// API에서 식사 데이터 로드 (날짜 변경 시)
const loadMealsForDate = (date) =>
{
    const key = formatDate(date)
    getMealsByDate(key)
        .then(meals =>
        {
            if (!meals || !meals.length) {
                logsByDate[key] = emptyDay()
                return
            }

            const mealsUI = transformMealsToUI(meals)
            logsByDate[key] = { meals: mealsUI }
        })
        .catch(e =>
        {
            console.error('식사 데이터 로드 실패:', e)
            logsByDate[key] = emptyDay()
        })
}

// ---- 영양 합산
const daySummary = computed(() =>
{
    const meals = dayLog.value.meals
    const all = mealKeys.flatMap(k => meals[k])
    return sumNutrition(all)
})

// ---- 모달(음식 추가)
const modalOpen = ref(false)
const modalMealKey = ref('breakfast')
const modalMealTitle = computed(() => mealLabels[modalMealKey.value])

function openAdd(mealKey)
{
    modalMealKey.value = mealKey
    modalOpen.value = true
}
function openAddQuick()
{
    // 상단 “식단 추가” → 가장 최근/기본으로 간식 열거나, 시간대 기반으로 추천해도 됨
    openAdd('snack')
}

async function addFoodToMeal(payload)
{
    // payload: { foodId, name, grams, per100g }
    const apiPayload = {
        date: formatDate(selectedDate.value),
        mealType: KEY_TO_MEAL_TYPE[modalMealKey.value] || 'SNACK',
        items: [
            {
                mealCode: String(payload.foodId),
                mealName: payload.name,
                amount: Number(payload.grams),
            }
        ]
    }

    try {
        await createMeal(apiPayload)
    } catch (e) {
        console.error('식사 등록 실패:', e)
        // 실패해도 로컬 UI 업데이트는 진행
    }

    // 2) 로컬 UI에 추가
    const row = {
        id: crypto.randomUUID ? crypto.randomUUID() : String(Date.now() + Math.random()),
        foodId: payload.foodId,
        name: payload.name,
        grams: payload.grams,
        per100g: payload.per100g,
    }
    dayLog.value.meals[modalMealKey.value].push(row)
    modalOpen.value = false
}

function removeItem(mealKey, rowId)
{
    const items = dayLog.value.meals[mealKey]
    const idx = items.findIndex(r => r.id === rowId)
    if (idx >= 0) {
        const item = items[idx]
        // API 호출로 삭제 (mealLogId, mealItemId)
        deleteMealItem(item.historyId, item.id)
            .then(() =>
            {
                items.splice(idx, 1)
            })
            .catch(e =>
            {
                console.error('식사 아이템 삭제 실패:', e)
            })
    }
}

function updateGrams(mealKey, rowId, grams)
{
    const items = dayLog.value.meals[mealKey]
    const row = items.find(r => r.id === rowId)
    if (row) row.grams = grams
}

// 페이지 로드 시 오늘 날짜의 식사 데이터 초기 로드
onMounted(() =>
{
    loadMealsForDate(selectedDate.value)
})
</script>

<style scoped>
.grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: var(--space-4);
    margin-top: var(--space-4);
}

.colMain {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
}

.colRail {
    display: flex;
    flex-direction: column;
    gap: var(--space-4);
}

.railTitle {
    font-weight: 900;
    font-size: 14px;
}

.railText {
    color: var(--muted);
    line-height: 1.45;
    font-size: 13px;
}

@media (min-width: 1200px) {
    .grid {
        grid-template-columns: 2fr 1fr;
        align-items: start;
    }
}
</style>
