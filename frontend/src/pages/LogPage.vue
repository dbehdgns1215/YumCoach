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
                        <div class="railTitle">오늘의 식단 계획</div>
                    </template>
                    <div v-if="mealTodos.length === 0" class="railText">
                        아직 계획된 식단이 없어요.<br />
                        음식 추가 시 📝 버튼을 눌러보세요!
                    </div>
                    <div v-else class="todoManageList">
                        <div v-for="todo in mealTodos" :key="todo.id" class="todoManageItem">
                            <div class="todoManageInfo">
                                <div class="todoManageMeta">
                                    <span class="todoManageType">{{ getMealTypeLabel(todo.mealType) }}</span>
                                </div>
                                <div class="todoManageName">{{ todo.foodName }}</div>
                                <div class="todoManageGrams">{{ todo.defaultGrams }}g</div>
                            </div>
                            <div class="todoManageActions">
                                <button class="todoManageAdd" @click="addTodoToMeal(todo)" title="식사로 기록">
                                    +
                                </button>
                                <button class="todoManageDelete" @click="deleteTodo(todo.id)" title="삭제">
                                    ✕
                                </button>
                            </div>
                        </div>
                    </div>
                </BaseCard>
            </div>
        </div>

        <FoodAddModal :open="modalOpen" :meal-title="modalMealTitle" @close="modalOpen = false" @add="addFoodToMeal"
            @add-to-todos="addToTodos" />
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
import { getMealTodos, consumeMealTodo, createMealTodo, deleteMealTodo } from '@/api/mealTodos.js'
import api from '@/lib/api.js'
import DaySummaryCard from '@/components/log/DaySummaryCard.vue'

import { startOfWeek, formatDate, formatDateDot, addDays, today as getToday } from '@/utils/date'
import { sumNutrition } from '@/utils/nutrition'
import { transformMealsToUI, updateItemNutrition } from '@/utils/mealTransform'
import { useNutritionCache } from '@/composables/useNutritionCache.js'
import { MEAL_KEYS, MEAL_LABELS, KEY_TO_MEAL_TYPE } from '@/constants/mealTypes'

const mealKeys = MEAL_KEYS
const mealLabels = MEAL_LABELS

// 영양정보 캐시 사용
const { getBatchNutrition, nutritionCache } = useNutritionCache()

// ---- TODO 상태
const mealTodos = ref([])

// TODO 목록 로드
async function loadMealTodos()
{
    try {
        const todos = await getMealTodos()
        mealTodos.value = todos
    } catch (e) {
        console.error('TODO 로드 실패:', e)
    }
}

// TODO를 실제 식사로 추가
async function addTodoToMeal(todo)
{
    const dateStr = formatDate(selectedDate.value)

    try {
        await consumeMealTodo(todo.id, dateStr)
        // TODO 목록에서 제거
        await loadMealTodos()
        // 식사 목록 다시 로드
        await loadMealsForDate(selectedDate.value)
    } catch (e) {
        console.error('TODO 추가 실패:', e)
        alert('식사 기록에 실패했습니다.')
    }
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
const loadMealsForDate = async (date) =>
{
    const key = formatDate(date)

    try {
        const meals = await getMealsByDate(key)

        if (!meals || !meals.length) {
            logsByDate[key] = emptyDay()
            return
        }

        const mealsUI = transformMealsToUI(meals)

        // 1단계: 즉시 UI에 표시 (영양정보 없이)
        logsByDate[key] = { meals: mealsUI }

        // 2단계: 백그라운드에서 영양정보 로드 (병렬 처리)
        loadNutritionForItems(mealsUI)
    } catch (e) {
        console.error('식사 데이터 로드 실패:', e)
        logsByDate[key] = emptyDay()
    }
}

// 아이템들의 영양정보를 병렬로 조회해서 업데이트 (최적화)
const loadNutritionForItems = async (mealsUI) =>
{
    // 영양정보가 필요한 foodId만 수집 (calc가 null이거나 undefined인 항목만)
    const foodIds = []
    MEAL_KEYS.forEach(mealKey =>
    {
        const items = mealsUI[mealKey]
        if (!items || !Array.isArray(items)) return

        items.forEach(item =>
        {
            // calc가 null이거나 undefined면 영양정보 조회 필요
            if (item.calc === null || item.calc === undefined) {
                foodIds.push(item.foodId)
            }
        })
    })

    // 중복 제거 및 캐시 없는 항목만 필터링
    const uniqueIds = [...new Set(foodIds)]
    const uncachedIds = uniqueIds.filter(id => !nutritionCache[id])

    // 캐시되지 않은 항목들을 한 번에 조회
    if (uncachedIds.length > 0) {
        await getBatchNutrition(uncachedIds)
    }

    // 모든 아이템에 영양정보 적용
    MEAL_KEYS.forEach(mealKey =>
    {
        const items = mealsUI[mealKey]
        if (!items || !Array.isArray(items)) return

        items.forEach(item =>
        {
            // calc가 있으면 (DB에서 저장된 값) per100g만 역계산
            if (item.calc !== null && item.calc !== undefined) {
                // calc는 이미 있으므로 per100g 역계산
                const factor = item.grams > 0 ? 100 / item.grams : 0
                item.per100g = {
                    kcal: Math.round(item.calc.kcal * factor),
                    protein: Math.round(item.calc.protein * factor * 10) / 10,
                    carbs: Math.round(item.calc.carbs * factor * 10) / 10,
                    fat: Math.round(item.calc.fat * factor * 10) / 10,
                }
            } else {
                // calc가 없으면 API에서 per100g 가져와서 계산
                const nutrition = nutritionCache[item.foodId]
                if (nutrition) {
                    updateItemNutrition(item, nutrition)
                    // calc 계산
                    const factor = item.grams / 100
                    item.calc = {
                        kcal: Math.round(nutrition.kcal * factor),
                        protein: Math.round(nutrition.protein * factor * 10) / 10,
                        carbs: Math.round(nutrition.carbs * factor * 10) / 10,
                        fat: Math.round(nutrition.fat * factor * 10) / 10,
                    }
                }
            }
        })
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
    // payload: { foodId, name, grams, per100g, calc }
    const apiPayload = {
        date: formatDate(selectedDate.value),
        mealType: KEY_TO_MEAL_TYPE[modalMealKey.value] || 'SNACK',
        items: [
            {
                mealCode: String(payload.foodId),
                mealName: payload.name,
                amount: Number(payload.grams),
                // 계산된 영양정보 저장
                kcal: payload.calc.kcal,
                protein: payload.calc.protein,
                carbs: payload.calc.carbs,
                fat: payload.calc.fat,
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
        calc: payload.calc,  // 계산된 영양정보 추가
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
    if (row) {
        row.grams = grams
        // grams 변경 시 영양정보 재계산 및 API 업데이트
        updateMealItemOnServer(row, mealKey)
    }
}

// 서버에 아이템 수정 요청
async function updateMealItemOnServer(row, mealKey)
{
    // calc가 있으면 사용, 없으면 per100g로 계산
    const calc = row.calc || {
        kcal: (row.per100g?.kcal || 0) * (row.grams / 100),
        protein: (row.per100g?.protein || 0) * (row.grams / 100),
        carbs: (row.per100g?.carbs || 0) * (row.grams / 100),
        fat: (row.per100g?.fat || 0) * (row.grams / 100),
    }

    const updatePayload = {
        mealCode: String(row.foodId),
        mealName: row.name,
        amount: Number(row.grams),
        kcal: calc.kcal,
        protein: calc.protein,
        carbs: calc.carbs,
        fat: calc.fat,
    }

    try {
        const key = formatDate(selectedDate.value)
        const mealLogId = dayLog.value.meals[mealKey]?.[0]?.historyId
        if (mealLogId) {
            // PUT /api/meals/{mealLogId}/items/{itemId}로 수정 요청
            await api.put(`/meals/${mealLogId}/items/${row.id}`, updatePayload)
            // calc 업데이트 (새로 계산한 값 저장)
            row.calc = calc
        }
    } catch (e) {
        console.warn('아이템 수정 실패:', e)
        // 실패해도 로컬 UI는 유지
    }
}

// TODO 추가 핸들러
async function addToTodos(payload)
{
    const apiPayload = {
        mealType: KEY_TO_MEAL_TYPE[modalMealKey.value] || 'SNACK',
        foodCode: String(payload.foodId),
        foodName: payload.name,
        defaultGrams: Number(payload.grams),
    }

    try {
        await createMealTodo(apiPayload)
        await loadMealTodos()
        modalOpen.value = false
    } catch (e) {
        console.error('TODO 추가 실패:', e)
        alert('식단 계획 추가에 실패했습니다.')
    }
}

// TODO 삭제
async function deleteTodo(todoId)
{
    if (!confirm('이 식단 계획을 삭제하시겠습니까?')) return

    try {
        await deleteMealTodo(todoId)
        await loadMealTodos()
    } catch (e) {
        console.error('TODO 삭제 실패:', e)
        alert('식단 계획 삭제에 실패했습니다.')
    }
}

// mealType을 한글 라벨로 변환
function getMealTypeLabel(mealType)
{
    const labels = {
        BREAKFAST: '아침',
        LUNCH: '점심',
        DINNER: '저녁',
        SNACK: '간식',
        LATENIGHT: '야식',
    }
    return labels[mealType] || mealType
}

// 페이지 로드 시 오늘 날짜의 식사 데이터 초기 로드
onMounted(() =>
{
    loadMealsForDate(selectedDate.value)
    loadMealTodos()
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

.todoManageList {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.todoManageItem {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border: 1px solid var(--border);
    border-radius: 12px;
    background: #fff;
    transition: all 0.2s;
}

.todoManageItem:hover {
    border-color: var(--brand);
    background: var(--brand-soft);
}

.todoManageInfo {
    flex: 1;
    min-width: 0;
}

.todoManageMeta {
    margin-bottom: 4px;
}

.todoManageType {
    display: inline-block;
    padding: 2px 8px;
    background: var(--brand);
    color: #fff;
    border-radius: 6px;
    font-size: 11px;
    font-weight: 900;
}

.todoManageName {
    font-weight: 900;
    font-size: 14px;
    margin-bottom: 4px;
}

.todoManageGrams {
    color: var(--muted);
    font-size: 12px;
    font-weight: 700;
}

.todoManageActions {
    display: flex;
    gap: 6px;
    flex-shrink: 0;
}

.todoManageAdd {
    width: 36px;
    height: 36px;
    border: 1px solid var(--brand);
    background: #fff;
    border-radius: 8px;
    cursor: pointer;
    color: var(--brand);
    font-weight: 900;
    font-size: 18px;
    transition: all 0.2s;
}

.todoManageAdd:hover {
    background: var(--brand);
    color: #fff;
}

.todoManageDelete {
    width: 36px;
    height: 36px;
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 8px;
    cursor: pointer;
    color: var(--muted);
    font-weight: 900;
    transition: all 0.2s;
}

.todoManageDelete:hover {
    background: #ffebee;
    border-color: #ef5350;
    color: #ef5350;
}

@media (min-width: 1200px) {
    .grid {
        grid-template-columns: 2fr 1fr;
        align-items: start;
    }
}
</style>
