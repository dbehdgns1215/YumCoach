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

                <input class="search" v-model="search.q.value" placeholder="예: 닭가슴살, 바나나, 현미밥…"
                    @keyup.enter="search.searchFoods" />
                <div class="searchActions">
                    <button class="searchBtn" @click="search.searchFoods">검색</button>
                    <button class="photoBtn" @click="imageAnalysis.triggerFileInput" title="카메라로 음식 분석">
                        📷
                    </button>
                </div>

                <!-- 숨겨진 파일 input -->
                <input ref="fileInputElement" type="file" accept="image/*" style="display:none"
                    @change="imageAnalysis.handleImageSelect" />

                <div class="results">
                    <!-- 분석 모드 -->
                    <div v-if="imageAnalysis.showAnalyzedList.value">
                        <div v-if="imageAnalysis.analyzingImage.value" class="loading">분석 중...</div>
                        <div v-else class="analyzedContainer">
                            <div class="sectionTitle">인식된 음식</div>
                            <button v-for="food in imageAnalysis.analyzedFoods.value" :key="food.name"
                                class="analyzedFood" :class="{ selected: selection.selectedFoods.value[food.name] }"
                                @click="selection.searchAnalyzedFood(food.name)">
                                <span class="foodName">{{ food.name }}</span>
                                <span v-if="selection.selectedFoods.value[food.name]" class="checkmark">✓</span>
                            </button>

                            <!-- 선택된 음식 리스트 -->
                            <div v-if="Object.keys(selection.selectedFoods.value).length > 0" class="selectedList">
                                <div class="sectionTitle">선택된 음식</div>
                                <div v-for="(food, foodName) in selection.selectedFoods.value" :key="foodName"
                                    class="selectedItem">
                                    <div class="itemInfo">
                                        <div class="itemName">{{ food.name }}</div>
                                        <input v-model.number="food.grams" type="number" min="0" class="grams" />
                                        <span class="gUnit">g</span>
                                    </div>
                                    <button class="removeBtn" @click="selection.removeSelectedFood(foodName)">✕</button>
                                </div>
                            </div>
                        </div>
                    </div>

                    <!-- 검색 모드 -->
                    <div v-else>
                        <div v-if="search.loading.value" class="loading">검색 중...</div>
                        <div v-else-if="search.displayedFoods.value.length === 0" class="noResults">
                            음식을 검색해주세요.
                        </div>
                        <button v-for="f in search.displayedFoods.value" :key="f.id" class="result"
                            :class="{ selected: search.selected.value?.id === f.id }" @click="search.select(f)">
                            <div class="name">{{ f.name }}</div>
                        </button>
                    </div>
                </div>

                <!-- 페이지네이션 (검색 모드에만) -->
                <div v-if="!imageAnalysis.showAnalyzedList.value && search.totalPages.value > 1" class="pagination">
                    <button class="pageBtn" :disabled="search.currentPage.value === 1"
                        @click="search.currentPage.value--">
                        ←
                    </button>
                    <span class="pageInfo">{{ search.currentPage.value }} / {{ search.totalPages.value }}</span>
                    <button class="pageBtn" :disabled="search.currentPage.value === search.totalPages.value"
                        @click="search.currentPage.value++">
                        →
                    </button>
                </div>

                <div class="gramsRow" v-if="!imageAnalysis.showAnalyzedList.value && search.selected.value">
                    <div class="label">그램</div>
                    <input class="grams" type="number" min="0" v-model.number="singleAdd.grams.value" />
                    <div class="unit">g</div>
                </div>

                <div v-if="imageAnalysis.analyzingImage.value" class="analyzing">
                    분석 중... 🤔
                </div>

                <div class="preview" v-else-if="!imageAnalysis.showAnalyzedList.value && search.selected.value">
                    <div class="pTitle">예상 영양 ({{ singleAdd.grams.value }}g)</div>
                    <div class="pLine">
                        {{ singleAdd.calc.value.kcal }}kcal · P {{ singleAdd.calc.value.protein }}g · C {{
                            singleAdd.calc.value.carbs }}g · F {{ singleAdd.calc.value.fat }}g
                    </div>
                </div>

                <div class="actions">
                    <button class="btn ghost" @click="$emit('close')">
                        취소
                    </button>

                    <!-- 단일 검색 모드일 때만 TODO에 추가 버튼 표시 -->
                    <button v-if="!imageAnalysis.showAnalyzedList.value && singleAdd.canAdd.value" class="btn secondary"
                        @click="addToTodos">
                        📝 TODO에 추가
                    </button>

                    <button v-if="imageAnalysis.showAnalyzedList.value" class="btn primary"
                        :disabled="Object.keys(selection.selectedFoods.value).length === 0" @click="addAllSelected">
                        {{ Object.keys(selection.selectedFoods.value).length }}개 추가
                    </button>
                    <button v-else class="btn primary" :disabled="!singleAdd.canAdd.value" @click="add">
                        추가
                    </button>
                </div>
            </div>
        </div>

        <!-- 음식 상세 선택 모달 -->
        <div v-if="selection.showFoodDetailModal.value" class="backdrop detailBackdrop"
            @click.self="selection.closeFoodDetailModal">
            <div class="sheet detailSheet" role="dialog" aria-modal="true">
                <div class="top">
                    <div>
                        <div class="title">{{ selection.selectedAnalyzedFood.value }} 선택</div>
                        <div class="sub">정확한 음식을 선택해주세요</div>
                    </div>
                    <button class="x" @click="selection.closeFoodDetailModal">✕</button>
                </div>

                <div class="results">
                    <div v-if="selection.loading.value" class="loading">검색 중...</div>
                    <div v-else-if="selection.allFoods.value.length === 0" class="noResults">
                        음식이 없습니다.
                    </div>
                    <button v-for="f in selection.allFoods.value" :key="f.id" class="result"
                        @click="selection.selectDetailFood(f)">
                        <div class="name">{{ f.name }}</div>
                    </button>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { watch, ref } from 'vue'
import { useFoodSearch } from '../../composables/useFoodSearch.js'
import { useImageAnalysis } from '../../composables/useImageAnalysis.js'
import { useFoodSelection } from '../../composables/useFoodSelection.js'
import { useSingleFoodAdd } from '../../composables/useSingleFoodAdd.js'
import { DEFAULT_GRAMS, DECIMAL_PLACES } from '../../constants/nutrition.js'

const props = defineProps({
    open: { type: Boolean, default: false },
    mealTitle: { type: String, default: '' },
})
const emit = defineEmits(['close', 'add', 'add-to-todos'])

// Template ref
const fileInputElement = ref(null)

// Composables 사용
const search = useFoodSearch()
const imageAnalysis = useImageAnalysis()
const selection = useFoodSelection()
const singleAdd = useSingleFoodAdd(search.selected)

// fileInput ref 연결
watch(fileInputElement, (el) =>
{
    if (el) {
        imageAnalysis.fileInput.value = el
    }
})

// ============================================
// 헬퍼 함수
// ============================================

/**
 * 모달을 초기 상태로 리셋
 */
function resetModal()
{
    search.reset()
    imageAnalysis.reset()
    selection.reset()
    singleAdd.reset()
}

/**
 * 선택된 음식을 식사에 추가 (단일 검색 모드)
 */
function add()
{
    if (!singleAdd.canAdd.value) return
    emit('add', {
        foodId: search.selected.value.id,
        name: search.selected.value.name,
        grams: Number(singleAdd.grams.value),
        per100g: search.selected.value.per100g,
        calc: singleAdd.calc.value,
    })
    resetModal()
}

/**
 * TODO에 추가
 */
function addToTodos()
{
    if (!singleAdd.canAdd.value) return
    emit('add-to-todos', {
        foodId: search.selected.value.id,
        name: search.selected.value.name,
        grams: Number(singleAdd.grams.value),
        per100g: search.selected.value.per100g,
        calc: singleAdd.calc.value,
    })
    resetModal()
}

/**
 * 선택된 모든 음식을 한 번에 추가
 */
function addAllSelected()
{
    const foods = Object.values(selection.selectedFoods.value)
    if (foods.length === 0) {
        alert('추가할 음식을 선택해주세요.')
        return
    }

    foods.forEach(food =>
    {
        emit('add', {
            foodId: food.id,
            name: food.name,
            grams: food.grams,
            per100g: food.per100g,
            calc: calculateNutrition(food),
        })
    })

    resetModal()
    imageAnalysis.showAnalyzedList.value = false
    emit('close')
}

/**
 * 음식의 영양정보 계산
 */
function calculateNutrition(food)
{
    const factor = Number(food.grams || 0) / 100
    const nutrition = food.per100g

    return {
        kcal: Math.round(nutrition.kcal * factor),
        protein: Math.round(nutrition.protein * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
        carbs: Math.round(nutrition.carbs * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
        fat: Math.round(nutrition.fat * factor * DECIMAL_PLACES) / DECIMAL_PLACES,
    }
}

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

<style src="../../styles/FoodAddModal.css" scoped></style>
