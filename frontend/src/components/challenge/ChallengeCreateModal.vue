<template>
  <div class="modal-overlay" v-if="show" @click.self="onCancel">
    <BaseCard class="modal">
      <template #header>
        <h3 style="margin:0">새 챌린지 생성</h3>
      </template>

      <!-- 제목 -->
      <label>제목
        <input v-model="title" placeholder="예: 단백질 200g 먹기" />
      </label>

      <!-- 설명 -->
      <label>설명 (선택)
        <textarea v-model="description" rows="2" placeholder="이 챌린지에 대한 설명을 입력하세요" />
      </label>

      <!-- 목표 타입 -->
      <label>목표 타입
        <select v-model="goalType">
          <option value="PROTEIN">단백질</option>
          <option value="CALORIE">칼로리</option>
          <option value="WEIGHT">체중</option>
          <option value="WATER">물 섭취</option>
          <option value="EXERCISE">운동</option>
          <option value="HABIT">습관</option>
          <option value="COMBINED">복합 목표</option>
        </select>
      </label>

      <!-- 목표 상세 (타입별로 다르게) -->
      <div class="goal-details">
        <!-- 단백질 -->
        <div v-if="goalType === 'PROTEIN'">
          <label>목표 단백질 (g/일)
            <input type="number" v-model.number="goalProtein" placeholder="200" />
          </label>
        </div>

        <!-- 칼로리 -->
        <div v-else-if="goalType === 'CALORIE'">
          <label>목표 칼로리 (kcal/일)
            <input type="number" v-model.number="goalCalories" placeholder="1500" />
          </label>
        </div>

        <!-- 체중 -->
        <div v-else-if="goalType === 'WEIGHT'">
          <label>목표 체중 변화 (kg)
            <input type="number" v-model.number="goalWeightChange" placeholder="-5" />
          </label>
          <small style="color:var(--muted)">감량은 음수(-), 증량은 양수(+)로 입력</small>
        </div>

        <!-- 물 -->
        <div v-else-if="goalType === 'WATER'">
          <label>목표 물 섭취량 (L/일)
            <input type="number" step="0.1" v-model.number="goalWater" placeholder="2.0" />
          </label>
        </div>

        <!-- 운동 -->
        <div v-else-if="goalType === 'EXERCISE'">
          <label>운동 목표
            <input v-model="goalExercise" placeholder="예: 30분 걷기" />
          </label>
        </div>

        <!-- 습관 -->
        <div v-else-if="goalType === 'HABIT'">
          <label>습관 설명
            <input v-model="goalHabit" placeholder="예: 야식 안 먹기" />
          </label>
        </div>

        <!-- 복합 -->
        <div v-else-if="goalType === 'COMBINED'">
          <label>복합 목표 (JSON)
            <textarea v-model="goalCombined" rows="3" placeholder='{"protein": "200g", "calories": "1500kcal"}' />
          </label>
        </div>
      </div>

      <!-- 기간 -->
      <div class="date-range">
        <label>시작일
          <input type="date" v-model="startDate" />
        </label>
        <label>기간 (일)
          <input type="number" v-model.number="durationDays" placeholder="30" />
        </label>
      </div>

      <!-- 체크리스트 항목 (선택) -->
      <label>실천 항목 (한 줄에 하나씩, 선택사항)
        <textarea v-model="itemsText" rows="4" placeholder="아침에 계란 3개 먹기&#10;점심에 닭가슴살 200g 먹기" />
      </label>

      <template #footer>
        <div class="actions">
          <BaseButton variant="secondary" @click="onCancel">취소</BaseButton>
          <BaseButton variant="primary" @click="onCreate">생성</BaseButton>
        </div>
      </template>
    </BaseCard>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({ 
    show: Boolean, 
    initialData: Object 
})
const emit = defineEmits(['close', 'create'])

const title = ref('')
const description = ref('')
const goalType = ref('PROTEIN')

// 목표별 세부 값
const goalProtein = ref(200)
const goalCalories = ref(1500)
const goalWeightChange = ref(-5)
const goalWater = ref(2.0)
const goalExercise = ref('')
const goalHabit = ref('')
const goalCombined = ref('')

const startDate = ref(new Date().toISOString().slice(0, 10))
const durationDays = ref(30)
const itemsText = ref('')

// initialData로 폼 채우기 (챗봇/리포트에서 넘어온 경우)
watch(() => props.initialData, (data) => {
    if (!data) return
    
    title.value = data.title || ''
    description.value = data.description || ''
    goalType.value = data.goalType || 'PROTEIN'
    
    if (data.goalDetails) {
        const details = typeof data.goalDetails === 'string' 
            ? JSON.parse(data.goalDetails) 
            : data.goalDetails
        
        if (details.protein) goalProtein.value = parseInt(details.protein)
        if (details.calories) goalCalories.value = parseInt(details.calories)
        if (details.weight) goalWeightChange.value = parseFloat(details.weight)
        if (details.water) goalWater.value = parseFloat(details.water)
        if (details.exercise) goalExercise.value = details.exercise
        if (details.habit) goalHabit.value = details.habit
    }
    
    if (data.items) {
        itemsText.value = data.items.map(it => it.text || it).join('\n')
    }
}, { immediate: true })

// goalDetails 계산
const computedGoalDetails = computed(() => {
    switch (goalType.value) {
        case 'PROTEIN':
            return { protein: `${goalProtein.value}g`, frequency: 'daily' }
        case 'CALORIE':
            return { calories: `${goalCalories.value}kcal`, frequency: 'daily' }
        case 'WEIGHT':
            return { weight: `${goalWeightChange.value}kg`, duration: `${durationDays.value}days` }
        case 'WATER':
            return { water: `${goalWater.value}L`, frequency: 'daily' }
        case 'EXERCISE':
            return { exercise: goalExercise.value, frequency: 'daily' }
        case 'HABIT':
            return { habit: goalHabit.value, frequency: 'daily' }
        case 'COMBINED':
            try {
                return JSON.parse(goalCombined.value)
            } catch {
                return {}
            }
        default:
            return {}
    }
})

function onCancel() { 
    emit('close') 
}

function onCreate() {
    const items = itemsText.value
        .split(/\r?\n/)
        .map((t, i) => ({ id: Date.now() + i, text: t.trim() }))
        .filter(x => x.text)
    
    const payload = {
        title: title.value || '새 챌린지',
        description: description.value,
        goalType: goalType.value,
        goalDetails: computedGoalDetails.value,
        startDate: startDate.value,
        durationDays: durationDays.value,
        items: items,
        source: props.initialData?.source || 'MANUAL',
        sourceId: props.initialData?.sourceId
    }
    
    console.debug('[ChallengeCreateModal] onCreate', payload)
    emit('create', payload)
}
</script>

<style scoped>
.modal-overlay {
    position: fixed;
    inset: 0;
    background: rgba(16, 24, 40, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    z-index: 9999;
}
.modal {
    background: var(--surface);
    padding: 24px;
    border-radius: var(--r-card);
    width: 100%;
    max-width: 560px;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: var(--shadow-lg);
    border: 1px solid var(--border);
}
label {
    display: block;
    margin-top: 16px;
    font-size: 14px;
    font-weight: 500;
    color: var(--text);
}
label:first-of-type {
    margin-top: 0;
}
input, textarea, select {
    width: 100%;
    padding: 10px 12px;
    margin-top: 6px;
    border: 1px solid var(--border);
    border-radius: 8px;
    background: var(--surface);
    color: var(--text);
    font-size: 14px;
}
.date-range {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 12px;
}
.goal-details {
    margin-top: 12px;
    padding: 16px;
    background: var(--surface-dim);
    border-radius: 8px;
    border: 1px solid var(--border);
}
.actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 20px;
}
</style>