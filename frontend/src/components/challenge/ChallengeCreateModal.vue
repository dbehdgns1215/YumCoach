  <template>
    <div class="modal-overlay" v-if="show" @click.self="onCancel">
      <BaseCard class="modal">
        <template #header>
          <h3 style="margin:0">새 챌린지 생성</h3>
        </template>

        <!-- 제목 -->
        <label>제목
          <input v-model="title" placeholder="예: 30일 단백질 챌린지" />
        </label>

        <!-- 설명 -->
        <label>설명 (선택)
          <textarea v-model="description" rows="2" placeholder="이 챌린지에 대한 설명"></textarea>
        </label>

        <!-- 🔥 목표 직접 입력 섹션 -->
        <div class="goals-section">
          <h4>목표 설정 <small>(원하는 항목만 입력하세요)</small></h4>

          <div class="goal-grid">
            <!-- 칼로리 -->
            <label class="goal-item">
              <div class="goal-header">
                <input type="checkbox" v-model="goals.calories.enabled" />
                <span class="goal-label">칼로리</span>
              </div>
              <div class="input-row">
                <input class="value-input" type="number" v-model.number="goals.calories.value"
                  :disabled="!goals.calories.enabled" placeholder="1500" />
                <span class="unit">kcal / 일</span>
              </div>
            </label>

            <!-- 단백질 -->
            <label class="goal-item">
              <div class="goal-header">
                <input type="checkbox" v-model="goals.protein.enabled" />
                <span class="goal-label">단백질</span>
              </div>
              <div class="input-row">
                <input class="value-input" type="number" v-model.number="goals.protein.value"
                  :disabled="!goals.protein.enabled" placeholder="200" />
                <span class="unit">g / 일</span>
              </div>
            </label>

            <!-- 탄수화물 -->
            <label class="goal-item">
              <div class="goal-header">
                <input type="checkbox" v-model="goals.carbs.enabled" />
                <span class="goal-label">탄수화물</span>
              </div>
              <div class="input-row">
                <input class="value-input" type="number" v-model.number="goals.carbs.value"
                  :disabled="!goals.carbs.enabled" placeholder="250" />
                <span class="unit">g / 일</span>
              </div>
            </label>

            <!-- 지방 -->
            <label class="goal-item">
              <div class="goal-header">
                <input type="checkbox" v-model="goals.fat.enabled" />
                <span class="goal-label">지방</span>
              </div>
              <div class="input-row">
                <input class="value-input" type="number" v-model.number="goals.fat.value" :disabled="!goals.fat.enabled"
                  placeholder="60" />
                <span class="unit">g / 일</span>
              </div>
            </label>

            <!-- 체중 -->
            <label class="goal-item">
              <div class="goal-header">
                <input type="checkbox" v-model="goals.weight.enabled" />
                <span class="goal-label">체중 변화</span>
              </div>
              <div class="input-row">
                <input class="value-input" type="number" step="0.1" v-model.number="goals.weight.value"
                  :disabled="!goals.weight.enabled" placeholder="-5" />
                <span class="unit">kg (전체 기간)</span>
              </div>
            </label>
          </div>

          <!-- 운동 -->
          <label class="goal-item-full">
            <div class="goal-header">
              <input type="checkbox" v-model="goals.exercise.enabled" />
              <span class="goal-label">운동</span>
            </div>
            <input type="text" v-model="goals.exercise.value" :disabled="!goals.exercise.enabled"
              placeholder="예: 30분 걷기, 근력 운동 3세트" />
          </label>

          <!-- 습관 -->
          <label class="goal-item-full">
            <div class="goal-header">
              <input type="checkbox" v-model="goals.habit.enabled" />
              <span class="goal-label">습관</span>
            </div>
            <input type="text" v-model="goals.habit.value" :disabled="!goals.habit.enabled"
              placeholder="예: 야식 안 먹기, 아침 거르지 않기" />
          </label>
        </div>

        <!-- 기간 -->
        <div class="date-range">
          <label>시작일
            <div class="input-row">
              <input class="value-input" type="date" v-model="startDate" />
              <span class="unit"></span>
            </div>
          </label>
          <label>기간
            <div class="input-row">
              <input class="value-input" type="number" v-model.number="durationDays" placeholder="30" />
              <span class="unit">일</span>
            </div>
          </label>
        </div>

        <!-- 체크리스트 항목 (선택) -->
        <label>실천 항목 (선택사항)
          <textarea v-model="itemsText" rows="4"
            placeholder="한 줄에 하나씩 입력하세요&#10;예:&#10;아침에 계란 3개 먹기&#10;점심에 닭가슴살 200g 먹기&#10;저녁마다 산책 20분하기">
  </textarea>
        </label>

        <template #footer>
          <div class="actions">
            <BaseButton variant="secondary" @click="onCancel">취소</BaseButton>
            <BaseButton variant="primary" @click="onCreate" :disabled="!hasAnyGoal">생성</BaseButton>
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
const startDate = ref(new Date().toISOString().slice(0, 10))
const durationDays = ref(30)
const itemsText = ref('')

// 🔥 목표별 체크박스 + 값
const goals = ref({
  calories: { enabled: false, value: null },
  protein: { enabled: false, value: null },
  carbs: { enabled: false, value: null },
  fat: { enabled: false, value: null },
  weight: { enabled: false, value: null },
  exercise: { enabled: false, value: '' },
  habit: { enabled: false, value: '' }
})

// 최소 하나의 목표는 선택해야 함
const hasAnyGoal = computed(() =>
{
  return Object.values(goals.value).some(g => g.enabled)
})

// goalType 자동 결정
const computedGoalType = computed(() =>
{
  const enabled = Object.entries(goals.value)
    .filter(([_, g]) => g.enabled)
    .map(([key, _]) => key)

  if (enabled.length === 0) return null
  if (enabled.length === 1) {
    const single = enabled[0]
    const typeMap = {
      calories: 'CALORIE',
      protein: 'PROTEIN',
      carbs: 'CARBS',
      fat: 'FAT',
      weight: 'WEIGHT',
      exercise: 'EXERCISE',
      habit: 'HABIT'
    }
    return typeMap[single]
  }
  return 'COMBINED'
})

// goalDetails 자동 생성
const computedGoalDetails = computed(() =>
{
  const details = {}

  if (goals.value.calories.enabled && goals.value.calories.value) {
    details.calories = `${goals.value.calories.value}kcal`
  }
  if (goals.value.protein.enabled && goals.value.protein.value) {
    details.protein = `${goals.value.protein.value}g`
  }
  if (goals.value.carbs.enabled && goals.value.carbs.value) {
    details.carbs = `${goals.value.carbs.value}g`
  }
  if (goals.value.fat.enabled && goals.value.fat.value) {
    details.fat = `${goals.value.fat.value}g`
  }
  if (goals.value.weight.enabled && goals.value.weight.value) {
    details.weight = `${goals.value.weight.value}kg`
  }
  if (goals.value.exercise.enabled && goals.value.exercise.value) {
    details.exercise = goals.value.exercise.value
  }
  if (goals.value.habit.enabled && goals.value.habit.value) {
    details.habit = goals.value.habit.value
  }

  // 복합 목표는 frequency 추가
  if (Object.keys(details).length > 1 ||
    (Object.keys(details).length === 1 && !['weight'].includes(Object.keys(details)[0]))) {
    details.frequency = 'daily'
  }

  return details
})

// 🔥 수정: initialData로 폼 채우기
watch(() => props.initialData, (data) =>
{
  if (!data) return

  console.debug('[ChallengeCreateModal] Received initialData:', data)

  title.value = data.title || ''
  description.value = data.description || ''

  // 🔥 goalDetails 처리 개선
  if (data.goalDetails) {
    const details = typeof data.goalDetails === 'string'
      ? JSON.parse(data.goalDetails)
      : data.goalDetails

    console.debug('[ChallengeCreateModal] Parsed goalDetails:', details)

    // 🔥 각 목표값 적용 (숫자만 추출)
    if (details.calories !== undefined && details.calories !== null) {
      goals.value.calories.enabled = true
      goals.value.calories.value = parseFloat(details.calories)
    }
    if (details.protein !== undefined && details.protein !== null) {
      goals.value.protein.enabled = true
      goals.value.protein.value = parseFloat(details.protein)
    }
    if (details.carbs !== undefined && details.carbs !== null) {
      goals.value.carbs.enabled = true
      goals.value.carbs.value = parseFloat(details.carbs)
    }
    if (details.fat !== undefined && details.fat !== null) {
      goals.value.fat.enabled = true
      goals.value.fat.value = parseFloat(details.fat)
    }
    if (details.weight !== undefined && details.weight !== null) {
      goals.value.weight.enabled = true
      goals.value.weight.value = parseFloat(details.weight)
    }
    if (details.exercise) {
      goals.value.exercise.enabled = true
      goals.value.exercise.value = details.exercise
    }
    if (details.habit) {
      goals.value.habit.enabled = true
      goals.value.habit.value = details.habit
    }
  }

  // 🔥 items 처리
  if (data.items && Array.isArray(data.items)) {
    itemsText.value = data.items
      .map(it => (it.text || it).toString())
      .join('\n')
  }
}, { immediate: true })

function onCancel()
{
  emit('close')
}

function onCreate()
{
  if (!hasAnyGoal.value) {
    alert('최소 하나의 목표를 선택하세요')
    return
  }

  const goalDetails = computedGoalDetails.value

  console.debug('[ChallengeCreateModal] onCreate goalDetails:', goalDetails)

  // 🔥 빈 객체 검증
  const meaningfulKeys = Object.keys(goalDetails).filter(k => k !== 'frequency')
  if (meaningfulKeys.length === 0) {
    alert('목표값을 입력해주세요')
    return
  }

  const items = itemsText.value
    .split(/\r?\n/)
    .map((t, i) => ({ text: t.trim(), order: i + 1 }))
    .filter(x => x.text)

  const payload = {
    title: title.value || '새 챌린지',
    description: description.value,
    goalType: computedGoalType.value,
    goalDetails: goalDetails,  // 객체 그대로 전송
    startDate: startDate.value,
    durationDays: durationDays.value,
    items: items,
    source: props.initialData?.source || 'MANUAL',
    sourceId: props.initialData?.sourceId
  }

  console.debug('[ChallengeCreateModal] Final payload:', payload)

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
  padding: 28px 48px 28px 28px;
  border-radius: var(--r-card);
  width: 100%;
  max-width: 900px;
  max-height: 92vh;
  overflow-y: auto;
  scrollbar-gutter: stable;
  box-shadow: var(--shadow-lg);
  border: 1px solid var(--border);
}

label {
  display: block;
  margin-top: 8px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text);
}

.modal>label:first-of-type {
  margin-top: 0;
}

.goal-grid .goal-item {
  margin-top: 8px;
}

input:not([type="checkbox"]),
textarea,
select {
  width: 100%;
  padding: 10px 12px;
  margin-top: 6px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--surface);
  color: var(--text);
  font-size: 14px;
}

input:disabled {
  background: var(--surface-dim);
  color: var(--muted);
  cursor: not-allowed;
}

.date-range {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.goals-section {
  margin: 20px 0;
  padding: 20px;
  background: var(--surface-dim);
  border-radius: 12px;
  border: 1px solid var(--border);
}

.goals-section h4 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.goals-section h4 small {
  font-weight: 400;
  color: var(--muted);
  font-size: 13px;
}

.goal-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 12px;
}

.goal-item,
.goal-item-full {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  background: var(--surface);
  border-radius: 8px;
  border: 1px solid var(--border);
  justify-content: space-between;
  min-height: 80px;
}

.goal-item-full {
  grid-column: 1 / -1;
  margin-top: 8px;
  padding: 8px;
  gap: 3px;
}

.goal-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.goal-header input[type="checkbox"] {
  width: 18px;
  height: 18px;
  margin: 0;
  cursor: pointer;
}

.goal-label {
  font-weight: 600;
  font-size: 14px;
  color: var(--text);
}

.goal-item-full .goal-label {
  font-size: 13px;
}

.goal-item-full input:not([type="checkbox"]) {
  padding: 6px 8px;
  font-size: 13px;
  height: 36px;
}

.goal-item input:not([type="checkbox"]),
.goal-item-full input:not([type="checkbox"]) {
  margin: 0;
  padding: 8px 10px;
}

.input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.value-input {
  flex: 1 1 auto;
  width: auto;
  box-sizing: border-box;
  min-width: 0;
  height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
}

.goal-item .unit {
  display: inline-block;
  width: 96px;
  text-align: right;
  font-size: 12px;
  color: var(--muted);
  line-height: 40px;
  white-space: nowrap;
}

.date-range .input-row {
  width: 100%;
}

@media (max-width: 480px) {
  .goal-item .unit {
    width: 72px;
    font-size: 11px;
    line-height: 36px;
  }
}

.date-range .value-input {
  height: 40px;
  padding: 8px 12px;
  border-radius: 8px;
}

.unit {
  font-size: 12px;
  color: var(--muted);
  margin-top: 4px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

@media (max-width: 768px) {
  .goal-grid {
    grid-template-columns: 1fr;
  }
}
</style>