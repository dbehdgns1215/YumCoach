<template>
    <BaseCard>
        <template #header>
            <div class="title">오늘 합계</div>
        </template>

        <div class="big">
            <div class="kcal">{{ summary.kcal }}</div>
            <div class="kcalUnit">kcal</div>
        </div>

        <div class="row">
            <div class="chip">단백질 <b>{{ summary.protein }}g</b></div>
            <div class="chip">탄수화물 <b>{{ summary.carbs }}g</b></div>
            <div class="chip">지방 <b>{{ summary.fat }}g</b></div>
        </div>
    </BaseCard>
</template>

<script setup>
import BaseCard from '@/components/base/BaseCard.vue'
defineProps({
    summary: { type: Object, required: true },
})
</script>

<style scoped>
.title {
    font-weight: 1000;
    font-size: 14px;
}

.big {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-top: 4px;
}

.kcal {
    font-size: 34px;
    font-weight: 1100;
}

.kcalUnit {
    color: var(--muted);
    font-weight: 900;
}

.row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 12px;
}

.chip {
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 999px;
    padding: 8px 10px;
    font-weight: 900;
    font-size: 12px;
    color: var(--muted);
}

.chip b {
    color: var(--text);
}
</style>
