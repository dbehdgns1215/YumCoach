<template>
    <teleport to="body">
        <div v-if="open" class="backdrop" @click.self="$emit('close')">
            <div class="sheet" role="dialog" aria-modal="true">
                <div class="top">
                    <div>
                        <div class="title">{{ mealTitle }} 메뉴 추가</div>
                        <div class="sub">메뉴 1개 + 그램 입력 → 영양정보 자동 계산</div>
                    </div>
                    <button class="x" @click="$emit('close')">✕</button>
                </div>

                <input class="search" v-model="q" placeholder="예: 닭가슴살, 바나나, 현미밥…" @keyup.enter="searchFoods" />
                <button class="searchBtn" @click="searchFoods">검색</button>

                <div class="results">
                    <div v-if="loading" class="loading">검색 중...</div>
                    <div v-else-if="displayedFoods.length === 0" class="noResults">
                        음식을 검색해주세요.
                    </div>
                    <button v-for="f in displayedFoods" :key="f.id" class="result"
                        :class="{ selected: selected?.id === f.id }" @click="select(f)">
                        <div class="name">{{ f.name }}</div>
                        <div class="meta">100g 기준 | {{ f.per100g.kcal }}kcal | P {{ f.per100g.protein }}g</div>
                    </button>
                </div>

                <!-- 페이지네이션 -->
                <div v-if="totalPages > 1" class="pagination">
                    <button class="pageBtn" :disabled="currentPage === 1" @click="currentPage--">
                        ←
                    </button>
                    <span class="pageInfo">{{ currentPage }} / {{ totalPages }}</span>
                    <button class="pageBtn" :disabled="currentPage === totalPages" @click="currentPage++">
                        →
                    </button>
                </div>

                <div class="gramsRow">
                    <div class="label">그램</div>
                    <input class="grams" type="number" min="0" v-model.number="grams" />
                    <div class="unit">g</div>
                </div>

                <div class="preview" v-if="selected">
                    <div class="pTitle">예상 영양 ({{ grams }}g)</div>
                    <div class="pLine">
                        {{ calc.kcal }}kcal · P {{ calc.protein }}g · C {{ calc.carbs }}g · F {{ calc.fat }}g
                    </div>
                </div>

                <div class="actions">
                    <button class="btn ghost" @click="$emit('close')">취소</button>
                    <button class="btn primary" :disabled="!canAdd" @click="add">
                        추가
                    </button>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { fetchFoodSearch, fetchFoodDetail } from '../../api/foods.js'
import { DEFAULT_GRAMS, ITEMS_PER_PAGE, DECIMAL_PLACES } from '../../constants/nutrition.js'

const props = defineProps({
    open: { type: Boolean, default: false },
    mealTitle: { type: String, default: '' },
})
const emit = defineEmits(['close', 'add'])

// ============================================
// 상태 (State)
// ============================================

const q = ref('')
const grams = ref(DEFAULT_GRAMS)
const selected = ref(null)
const loading = ref(false)
const loadingDetail = ref(false)
const allFoods = ref([])
const currentPage = ref(1)

// ============================================
// 상태 관리 함수
// ============================================

/**
 * 모달을 초기 상태로 리셋
 */
function resetModal()
{
    q.value = ''
    grams.value = DEFAULT_GRAMS
    selected.value = null
    allFoods.value = []
    currentPage.value = 1
}

// ============================================
// 사용자 인터랙션 함수
// ============================================

/**
 * 음식 검색 실행
 * 빈 검색어는 무시하고, API 오류 시에도 진행
 */
async function searchFoods()
{
    // 빈 검색어 확인
    if (!q.value.trim()) {
        allFoods.value = []
        currentPage.value = 1
        return
    }

    loading.value = true
    try {
        const foods = await fetchFoodSearch(q.value)
        allFoods.value = foods
        currentPage.value = 1
    } catch (error) {
        console.error('검색 중 오류 발생:', error)
        allFoods.value = []
    } finally {
        loading.value = false
    }
}

/**
 * 음식 상세정보 로드 및 선택
 * 상세정보 조회 실패 시에도 기본 정보로 진행
 * @param {Object} food - 선택된 음식 객체
 */
async function select(food)
{
    loadingDetail.value = true
    try {
        // 음식 상세정보(영양정보) 조회
        const nutrition = await fetchFoodDetail(food.id)
        selected.value = {
            ...food,
            per100g: nutrition
        }
    } catch (error) {
        // 조회 실패 시 기본 영양정보로 진행
        console.warn('상세정보 조회 실패, 기본 정보로 진행:', error)
        selected.value = food
    } finally {
        loadingDetail.value = false
    }
}

/**
 * 선택된 음식을 식사에 추가
 */
function add()
{
    if (!canAdd.value) return
    emit('add', {
        foodId: selected.value.id,
        name: selected.value.name,
        grams: Number(grams.value),
        per100g: selected.value.per100g,
    })
}

// ============================================
// 계산식 (Computed)
// ============================================

/**
 * 전체 페이지 수 계산
 */
const totalPages = computed(() => Math.ceil(allFoods.value.length / ITEMS_PER_PAGE))

/**
 * 현재 페이지의 음식 목록 (페이지네이션)
 */
const displayedFoods = computed(() =>
{
    const start = (currentPage.value - 1) * ITEMS_PER_PAGE
    const end = start + ITEMS_PER_PAGE
    return allFoods.value.slice(start, end)
})

/**
 * 입력된 그램 수를 기반으로 계산된 영양정보
 * 100g 기준 영양정보에 (입력 그램 / 100) 계수를 곱함
 */
const calc = computed(() =>
{
    if (!selected.value) {
        return { kcal: 0, protein: 0, carbs: 0, fat: 0 }
    }

    const factor = Number(grams.value || 0) / 100
    const nutrition = selected.value.per100g

    return {
        kcal: Math.round(nutrition.kcal * factor),
        protein: Math.round(nutrition.protein * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
        carbs: Math.round(nutrition.carbs * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
        fat: Math.round(nutrition.fat * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
    }
})

/**
 * 음식 추가 버튼 활성화 여부
 * 음식이 선택되고 그램이 0보다 커야 활성화
 */
const canAdd = computed(() => !!selected.value && Number(grams.value) > 0)

// ============================================
// 라이프사이클 (Watchers)
// ============================================

/**
 * 모달 열림/닫힘에 따른 상태 초기화
 */
watch(() => props.open, (isOpen) =>
{
    if (isOpen) {
        resetModal()
    }
})
</script>

<style scoped>
.backdrop {
    position: fixed;
    inset: 0;
    background: rgba(16, 24, 40, .45);
    display: flex;
    align-items: flex-end;
    justify-content: center;
    padding: 12px;
    z-index: 9999;
}

.sheet {
    width: min(640px, 100%);
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: 18px;
    box-shadow: var(--shadow);
    padding: 14px;
    max-height: 86vh;
    overflow: auto;
}

.top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 12px;
}

.title {
    font-weight: 1000;
    font-size: 15px;
}

.sub {
    color: var(--muted);
    font-weight: 800;
    font-size: 12px;
    margin-top: 4px;
}

.x {
    width: 36px;
    height: 36px;
    border-radius: 12px;
    border: 1px solid var(--border);
    background: transparent;
    cursor: pointer;
    transition: all 0.2s;
}

.x:hover {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, 0.35);
}

.search {
    width: 100%;
    margin-top: 12px;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 12px;
    font-weight: 900;
}

.searchBtn {
    margin-top: 8px;
    width: 100%;
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 14px;
    padding: 12px;
    font-weight: 1000;
    cursor: pointer;
    transition: all 0.2s;
}

.searchBtn:hover {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, 0.35);
}

.results {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-height: 60px;
}

.loading {
    text-align: center;
    color: var(--muted);
    padding: 20px;
    font-weight: 800;
}

.noResults {
    text-align: center;
    color: var(--muted);
    padding: 20px;
    font-weight: 800;
}

.result {
    text-align: left;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 10px 12px;
    background: #fff;
    cursor: pointer;
    transition: all 0.2s;
}

.result:hover {
    border-color: rgba(47, 107, 255, 0.35);
    background: rgba(47, 107, 255, 0.05);
}

.result.selected {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, .35);
}

.name {
    font-weight: 1000;
}

.meta {
    color: var(--muted);
    font-weight: 800;
    font-size: 12px;
    margin-top: 4px;
}

.pagination {
    margin-top: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
}

.pageBtn {
    width: 36px;
    height: 36px;
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 8px;
    cursor: pointer;
    font-weight: 900;
    transition: all 0.2s;
}

.pageBtn:hover:not(:disabled) {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, 0.35);
}

.pageBtn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.pageInfo {
    font-weight: 900;
    font-size: 12px;
}

.gramsRow {
    margin-top: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
}

.label {
    font-weight: 1000;
}

.grams {
    width: 120px;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 10px 12px;
    font-weight: 1000;
}

.unit {
    color: var(--muted);
    font-weight: 1000;
}

.preview {
    margin-top: 12px;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 12px;
    background: #fff;
}

.pTitle {
    font-weight: 1000;
    font-size: 12px;
    color: var(--muted);
}

.pLine {
    margin-top: 6px;
    font-weight: 1000;
}

.actions {
    margin-top: 14px;
    display: flex;
    gap: 10px;
}

.btn {
    flex: 1;
    border: 0;
    border-radius: 14px;
    padding: 12px 14px;
    font-weight: 1000;
    cursor: pointer;
    transition: all 0.2s;
}

.primary {
    background: var(--primary);
    color: #fff;
}

.ghost {
    background: #fff;
    color: var(--text);
    border: 1px solid var(--border);
}

.btn:disabled {
    opacity: .55;
    cursor: not-allowed;
}

@media (min-width: 900px) {
    .backdrop {
        align-items: center;
    }
}
</style>
