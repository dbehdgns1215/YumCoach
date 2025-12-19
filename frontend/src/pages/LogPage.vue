<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="기록" :subtitle="subtitle" footerTheme="brand" @primary="openAddQuick">
        <WeekStrip :week-start="weekStart" :selected-date="selectedDate" @select="selectedDate = $event"
            @prev="shiftWeek(-7)" @next="shiftWeek(7)" />

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
import { computed, reactive, ref } from 'vue'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import BaseCard from '@/components/base/BaseCard.vue'

import WeekStrip from '@/components/log/WeekStrip.vue'
import MealSection from '@/components/log/MealSection.vue'
import FoodAddModal from '@/components/log/FoodAddModal.vue'
import DaySummaryCard from '@/components/log/DaySummaryCard.vue'

import { startOfWeek, formatDate, formatDateDot, addDays, today as getToday } from '@/utils/date'
import { sumNutrition } from '@/utils/nutrition'

const mealKeys = ['breakfast', 'lunch', 'dinner', 'snack', 'latenight']
const mealLabels = {
    breakfast: '아침',
    lunch: '점심',
    dinner: '저녁',
    snack: '간식',
    latenight: '야식',
}

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

const dayKey = computed(() => formatDate(selectedDate.value))
const dayLog = computed(() =>
{
    if (!logsByDate[dayKey.value]) logsByDate[dayKey.value] = emptyDay()
    return logsByDate[dayKey.value]
})

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

function addFoodToMeal(payload)
{
    // payload: { foodId, name, grams, per100g }
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
    if (idx >= 0) items.splice(idx, 1)
}

function updateGrams(mealKey, rowId, grams)
{
    const items = dayLog.value.meals[mealKey]
    const row = items.find(r => r.id === rowId)
    if (row) row.grams = grams
}
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
