<template>
    <teleport to="body">
        <div v-if="open" class="backdrop" @click.self="$emit('close')">
            <div class="sheet" role="dialog" aria-modal="true">
                <div class="top">
                    <div>
                        <div class="title">{{ mealTitle }} · 메뉴 추가</div>
                        <div class="sub">메뉴 1개 + 그램 입력 → 영양정보 자동 계산</div>
                    </div>
                    <button class="x" @click="$emit('close')">✕</button>
                </div>

                <input class="search" v-model="q" placeholder="예: 닭가슴살, 바나나, 현미밥…" />

                <div class="results">
                    <button v-for="f in filtered" :key="f.id" class="result"
                        :class="{ selected: selected?.id === f.id }" @click="select(f)">
                        <div class="name">{{ f.name }}</div>
                        <div class="meta">100g 기준 · {{ f.per100g.kcal }}kcal · P {{ f.per100g.protein }}g</div>
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

const props = defineProps({
    open: { type: Boolean, default: false },
    mealTitle: { type: String, default: '' },
})
const emit = defineEmits(['close', 'add'])

const q = ref('')
const grams = ref(100)
const selected = ref(null)

// mock DB (나중에 API로 교체)
const mockFoodDB = [
    { id: 'chicken', name: '닭가슴살', per100g: { kcal: 165, protein: 31, carbs: 0, fat: 3.6 } },
    { id: 'rice', name: '현미밥', per100g: { kcal: 111, protein: 2.6, carbs: 23, fat: 0.9 } },
    { id: 'banana', name: '바나나', per100g: { kcal: 89, protein: 1.1, carbs: 23, fat: 0.3 } },
    { id: 'yogurt', name: '그릭요거트(무가당)', per100g: { kcal: 97, protein: 10, carbs: 3.6, fat: 4.5 } },
    { id: 'egg', name: '삶은 달걀', per100g: { kcal: 155, protein: 13, carbs: 1.1, fat: 11 } },
]

const filtered = computed(() =>
{
    const s = q.value.trim()
    if (!s) return mockFoodDB
    return mockFoodDB.filter(f => f.name.includes(s))
})

function select(f)
{
    selected.value = f
}

const calc = computed(() =>
{
    if (!selected.value) return { kcal: 0, protein: 0, carbs: 0, fat: 0 }
    const factor = (Number(grams.value || 0)) / 100
    const p = selected.value.per100g
    return {
        kcal: Math.round(p.kcal * factor),
        protein: Math.round(p.protein * factor * 10) / 10,
        carbs: Math.round(p.carbs * factor * 10) / 10,
        fat: Math.round(p.fat * factor * 10) / 10,
    }
})

const canAdd = computed(() => !!selected.value && Number(grams.value) > 0)

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

// 모달 열릴 때 초기화
watch(() => props.open, (v) =>
{
    if (v) {
        q.value = ''
        grams.value = 100
        selected.value = null
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
}

.search {
    width: 100%;
    margin-top: 12px;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 12px;
    font-weight: 900;
}

.results {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.result {
    text-align: left;
    border: 1px solid var(--border);
    border-radius: 14px;
    padding: 10px 12px;
    background: #fff;
    cursor: pointer;
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
