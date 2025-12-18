<template>
    <BaseCard>
        <template #header>
            <div class="head">
                <div class="title">{{ title }}</div>
                <button class="add" @click="$emit('add')">+ 메뉴 추가</button>
            </div>
        </template>

        <div v-if="!items.length" class="empty">
            아직 등록된 메뉴가 없어요. 가볍게 하나 추가해볼까요? 🙂
        </div>

        <div v-else class="list">
            <div v-for="row in items" :key="row.id" class="item">
                <div class="left">
                    <div class="name">{{ row.name }}</div>
                    <div class="meta">
                        {{ rowKcal(row) }}kcal · P {{ rowP(row) }}g
                    </div>
                </div>

                <div class="right">
                    <div class="grams">
                        <input class="gramInput" type="number" min="0" :value="row.grams"
                            @input="onGrams(row.id, $event.target.value)" />
                        <span class="g">g</span>
                    </div>
                    <button class="del" @click="$emit('remove', row.id)">삭제</button>
                </div>
            </div>

            <div class="sum">
                <div class="sumTitle">합계</div>
                <div class="sumVal">
                    {{ summary.kcal }}kcal · P {{ summary.protein }}g · C {{ summary.carbs }}g · F {{ summary.fat }}g
                </div>
            </div>
        </div>
    </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import { calculateNutrition, sumNutrition, formatNutrition } from '@/utils/nutrition'

const props = defineProps({
    title: { type: String, required: true },
    items: { type: Array, default: () => [] },
})
const emit = defineEmits(['add', 'remove', 'update-grams'])

const summary = computed(() => sumNutrition(props.items))

function rowKcal(r) { return Math.round(calculateNutrition(r).kcal) }
function rowP(r) { return Math.round(calculateNutrition(r).protein * 10) / 10 }

function onGrams(rowId, value)
{
    const grams = Math.max(0, Number(value || 0))
    emit('update-grams', { rowId, grams })
}
</script>

<style scoped>
.head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
}

.title {
    font-weight: 1000;
    font-size: 15px;
}

.add {
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 999px;
    padding: 8px 12px;
    font-weight: 900;
    cursor: pointer;
}

.empty {
    color: var(--muted);
    font-size: 13px;
    line-height: 1.45;
}

.list {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    border: 1px solid var(--border);
    border-radius: 14px;
    background: #fff;
}

.name {
    font-weight: 1000;
    font-size: 14px;
}

.meta {
    color: var(--muted);
    font-size: 12px;
    margin-top: 4px;
    font-weight: 800;
}

.right {
    display: flex;
    align-items: center;
    gap: 10px;
}

.grams {
    display: flex;
    align-items: center;
    gap: 6px;
}

.gramInput {
    width: 72px;
    border: 1px solid var(--border);
    border-radius: 12px;
    padding: 8px 10px;
    font-weight: 900;
}

.g {
    color: var(--muted);
    font-weight: 900;
    font-size: 12px;
}

.del {
    border: 0;
    background: transparent;
    color: var(--muted);
    font-weight: 900;
    cursor: pointer;
}

.sum {
    margin-top: 6px;
    padding-top: 10px;
    border-top: 1px dashed var(--border);
}

.sumTitle {
    font-weight: 1000;
    font-size: 12px;
    color: var(--muted);
}

.sumVal {
    margin-top: 4px;
    font-weight: 1000;
    font-size: 13px;
}
</style>
