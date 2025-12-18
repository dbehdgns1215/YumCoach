<template>
    <BaseCard>
        <div class="row">
            <button class="navBtn" @click="$emit('prev')">←</button>

            <div class="days">
                <button v-for="d in days" :key="d.key" class="day" :class="{ active: d.key === selectedKey }"
                    @click="$emit('select', d.date)">
                    <div class="dow">{{ d.dow }}</div>
                    <div class="num">{{ d.num }}</div>
                </button>
            </div>

            <button class="navBtn" @click="$emit('next')">→</button>
        </div>
    </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
    weekStart: { type: Date, required: true },
    selectedDate: { type: Date, required: true },
})
defineEmits(['select', 'prev', 'next'])

const dowMap = ['일', '월', '화', '수', '목', '금', '토']

function keyOf(d)
{
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd}`
}

const selectedKey = computed(() => keyOf(props.selectedDate))

const days = computed(() =>
{
    const list = []
    for (let i = 0; i < 7; i++) {
        const d = new Date(props.weekStart)
        d.setDate(d.getDate() + i)
        list.push({
            date: d,
            key: keyOf(d),
            dow: dowMap[d.getDay()],
            num: d.getDate(),
        })
    }
    return list
})
</script>

<style scoped>
.row {
    display: flex;
    align-items: center;
    gap: 10px;
}

.navBtn {
    width: 40px;
    height: 40px;
    border-radius: 14px;
    border: 1px solid var(--border);
    background: #fff;
    font-weight: 900;
    cursor: pointer;
}

.days {
    flex: 1;
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 8px;
}

.day {
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 14px;
    padding: 10px 0;
    cursor: pointer;
}

.day.active {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, .35);
}

.dow {
    font-size: 12px;
    color: var(--muted);
    font-weight: 900;
}

.num {
    font-size: 14px;
    font-weight: 1000;
    margin-top: 2px;
}
</style>
