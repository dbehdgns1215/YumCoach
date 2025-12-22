<template>
    <BaseCard>
        <!-- 캘린더 토글 버튼 -->
        <div class="header">
            <button class="calendarToggle" @click="showCalendar = !showCalendar">
                📅
            </button>

            <!-- 캘린더 팝오버 모달 -->
            <div v-if="showCalendar" class="calendarModal">
                <div class="calendarContent">
                    <div class="calendarHeader">
                        <button class="monthNavBtn" @click="prevMonth">←</button>
                        <div class="monthTitle">{{ calendarYear }}년 {{ calendarMonth }}월</div>
                        <button class="monthNavBtn" @click="nextMonth">→</button>
                    </div>

                    <div class="calendarGrid">
                        <div v-for="dow in ['일', '월', '화', '수', '목', '금', '토']" :key="dow" class="dowHeader">
                            {{ dow }}
                        </div>
                        <button v-for="day in calendarDays" :key="`${day.date}`" class="calendarDay" :class="{
                            active: day.key === selectedKey,
                            otherMonth: !day.currentMonth,
                            hasRecord: day.hasRecord
                        }" :disabled="!day.currentMonth" @click="selectDate(day.date)">
                            {{ day.num }}
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 주간 스트립 섹션 -->
        <div class="weekSection">
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
        </div>
    </BaseCard>
</template>

<script setup>
import { computed, ref } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
    weekStart: { type: Date, required: true },
    selectedDate: { type: Date, required: true },
    records: { type: Object, default: () => ({}) }, // { 'YYYY-MM-DD': true, ... }
})
const emit = defineEmits(['select', 'prev', 'next'])

const dowMap = ['일', '월', '화', '수', '목', '금', '토']

// 캘린더 표시 여부
const showCalendar = ref(false)

// 캘린더 뷰 상태 (기본값: selectedDate의 월)
const calendarYear = ref(props.selectedDate.getFullYear())
const calendarMonth = ref(props.selectedDate.getMonth() + 1)

function keyOf(d)
{
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    return `${yyyy}-${mm}-${dd}`
}

const selectedKey = computed(() => keyOf(props.selectedDate))

// 주간 스트립
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

// 캘린더 생성
const calendarDays = computed(() =>
{
    const list = []
    const firstDay = new Date(calendarYear.value, calendarMonth.value - 1, 1)
    const lastDay = new Date(calendarYear.value, calendarMonth.value, 0)
    const startDate = new Date(firstDay)
    startDate.setDate(startDate.getDate() - firstDay.getDay())

    // 6주 분량 (42일)
    for (let i = 0; i < 42; i++) {
        const d = new Date(startDate)
        d.setDate(d.getDate() + i)
        const dateKey = keyOf(d)
        list.push({
            date: d,
            key: dateKey,
            num: d.getDate(),
            currentMonth: d.getMonth() === calendarMonth.value - 1,
            hasRecord: props.records[dateKey] || false,
        })
    }
    return list
})

function prevMonth()
{
    if (calendarMonth.value === 1) {
        calendarYear.value--
        calendarMonth.value = 12
    } else {
        calendarMonth.value--
    }
}

function nextMonth()
{
    if (calendarMonth.value === 12) {
        calendarYear.value++
        calendarMonth.value = 1
    } else {
        calendarMonth.value++
    }
}

function selectDate(date)
{
    // selectedDate의 월로 캘린더 뷰 업데이트
    calendarYear.value = date.getFullYear()
    calendarMonth.value = date.getMonth() + 1
    // 부모 컴포넌트에 날짜 선택 알림
    emit('select', date)
    // 캘린더 닫기
    showCalendar.value = false
}
</script>

<style scoped>
.header {
    padding: 8px;
    position: relative;
}

.calendarToggle {
    width: 32px;
    height: 32px;
    padding: 0;
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 8px;
    font-size: 16px;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.calendarToggle:hover {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, 0.35);
}

.calendarModal {
    position: absolute;
    top: 100%;
    left: 0;
    margin-top: 4px;
    background: #fff;
    border: 1px solid var(--border);
    border-radius: 8px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    z-index: 100;
    animation: popIn 0.2s ease-out;
}

@keyframes popIn {
    from {
        opacity: 0;
        transform: translateY(-4px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.calendarContent {
    padding: 8px;
}

.calendarHeader {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    margin-bottom: 6px;
}

.monthNavBtn {
    width: 16px;
    height: 16px;
    border-radius: 2px;
    border: 1px solid var(--border);
    background: #fff;
    cursor: pointer;
    font-weight: 900;
    font-size: 8px;
    padding: 0;
    display: flex;
    align-items: center;
    justify-content: center;
}

.monthTitle {
    font-weight: 900;
    font-size: 11px;
    min-width: 80px;
    text-align: center;
}

.calendarGrid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 0;
}

.dowHeader {
    font-size: 8px;
    font-weight: 900;
    color: var(--muted);
    text-align: center;
    padding: 2px 0;
}

.calendarDay {
    aspect-ratio: 1;
    border: 1px solid var(--border);
    background: #fff;
    border-radius: 2px;
    font-size: 9px;
    cursor: pointer;
    transition: all 0.2s;
    min-width: 20px;
    min-height: 20px;
    padding: 0;
    line-height: 1;
    display: flex;
    align-items: center;
    justify-content: center;
}

.calendarDay:disabled {
    opacity: 0.3;
    cursor: not-allowed;
}

.calendarDay.active {
    background: var(--primary-soft);
    border-color: rgba(47, 107, 255, 0.35);
    font-weight: 900;
}

.calendarDay.hasRecord {
    font-weight: 900;
    color: var(--primary);
}

.calendarDay.otherMonth {
    color: var(--muted);
}

.weekSection {
    padding: 12px;
}

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
