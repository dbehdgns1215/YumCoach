<template>
  <div class="challenge-list">
    <div class="header" v-if="!readonly">
      <h2>내 챌린지</h2>
      <BaseButton class="create-challenge-btn" variant="primary" @click="$emit('create')">
        새 챌린지 만들기
      </BaseButton>
    </div>

    <div v-if="!challenges || challenges.length === 0" class="empty">
      챌린지가 없습니다.
    </div>

    <div v-for="c in challenges" :key="c.id" class="challenge-card">
      <BaseCard>
        <template #header>
          <div class="card-header">
            <div class="title-section">
              <div class="status-badge" :class="c.status">
                {{ statusText(c.status) }}
              </div>
              <h3>{{ c.title }}</h3>
              <div class="desc">{{ c.description }}</div>
            </div>

            <!-- 🔥 수정: 통계 섹션 -->
            <div class="stats-section">
              <div class="stat">
                <span class="stat-label">연속 달성</span>
                <span class="stat-value">{{ c.currentStreak || 0 }}일 🔥</span>
              </div>
              <div class="stat">
                <span class="stat-label">목표 달성률</span>
                <span class="stat-value">{{ formatRate(c.achievementRate) }}%</span>
              </div>
            </div>
          </div>
        </template>

        <!-- 목표 표시 -->
        <div class="goal-display">
          <strong>목표:</strong> {{ formatGoal(c.goalType, c.goalDetails) }}
        </div>

        <!-- 🔥 수정: 진행도 바 -->
        <div class="progress-section">
          <div class="progress-label">
            <span>전체 진행도</span>
            <span>{{ formatRate(c.progressRate) }}%</span>
          </div>
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: formatRate(c.progressRate) + '%' }" />
          </div>
          <div class="progress-text">
            {{ c.totalSuccessDays || 0 }}일 성공 / {{ elapsedDays(c) }}일 경과 (전체 {{ c.durationDays || 30 }}일)
          </div>
        </div>

        <!-- 체크리스트 항목 -->
        <div class="items" v-if="c.items && c.items.length > 0">
          <h4>실천 항목</h4>
          <ChallengeItem v-for="it in c.items" :key="it.id" :item="it" @update="onItemUpdate(c, $event)" />
        </div>

        <!-- 액션 버튼 -->
        <template #footer v-if="!readonly">
          <div class="card-actions">
            <BaseButton v-if="c.status === 'ACTIVE'" variant="success" @click="$emit('complete', c.id)">
              완료 처리
            </BaseButton>
            <BaseButton variant="secondary" @click="$emit('delete', c.id)">
              삭제
            </BaseButton>
          </div>
        </template>
      </BaseCard>
    </div>
  </div>
</template>

<script setup>
import ChallengeItem from './ChallengeItem.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const props = defineProps({
  challenges: {
    type: Array,
    default: () => []
  },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['create', 'update', 'complete', 'delete'])

function statusText(status)
{
  const map = {
    'ACTIVE': '진행중',
    'COMPLETED': '완료',
    'FAILED': '실패',
    'ABANDONED': '포기'
  }
  return map[status] || status
}

function formatGoal(type, details)
{
  // 🔥 안전한 체크 추가
  if (!details) return '-'

  let parsed
  try {
    parsed = typeof details === 'string' ? JSON.parse(details) : details
  } catch (e) {
    console.error('Failed to parse goalDetails:', details, e)
    return '-'
  }

  // 🔥 parsed가 여전히 null/undefined이면 리턴
  if (!parsed || typeof parsed !== 'object') {
    return '-'
  }

  const formats = {
    'PROTEIN': () => parsed.protein ? `단백질 ${parsed.protein} 매일 섭취` : '-',
    'CALORIE': () => parsed.calories ? `칼로리 ${parsed.calories} 목표` : '-',
    'WEIGHT': () => parsed.weight ? `체중 ${parsed.weight} 변화` : '-',
    'WATER': () => parsed.water ? `물 ${parsed.water} 매일 마시기` : '-',
    'EXERCISE': () => parsed.exercise ? `${parsed.exercise} 매일 실천` : '-',
    'HABIT': () => parsed.habit ? `${parsed.habit} 습관 만들기` : '-',
    'CARBS': () => parsed.carbs ? `탄수화물 ${parsed.carbs} 목표` : '-',
    'FAT': () => parsed.fat ? `지방 ${parsed.fat} 목표` : '-',
    'COMBINED': () =>
    {
      try {
        return Object.entries(parsed)
          .filter(([k]) => k !== 'frequency')
          .map(([k, v]) => `${k}: ${v}`)
          .join(', ') || '-'
      } catch (e) {
        console.error('Failed to format COMBINED goal:', parsed, e)
        return JSON.stringify(parsed)
      }
    }
  }

  return formats[type]?.() || JSON.stringify(parsed)
}

// 🔥 추가: BigDecimal을 소수점 1자리로 포맷
function formatRate(rate)
{
  if (!rate) return 0
  return typeof rate === 'number' ? rate.toFixed(1) : parseFloat(rate).toFixed(1)
}

// 🔥 추가: 경과일 계산
function elapsedDays(challenge)
{
  const today = new Date()
  const start = new Date(challenge.startDate)
  const end = new Date(challenge.endDate)

  if (today < start) return 0
  if (today > end) return challenge.durationDays

  const elapsed = Math.floor((today - start) / (1000 * 60 * 60 * 24)) + 1
  return Math.min(elapsed, challenge.durationDays)
}

function onItemUpdate(challenge, updatedItem)
{
  const updated = {
    ...challenge,
    items: challenge.items.map(i => i.id === updatedItem.id ? updatedItem : i)
  }
  emit('update', updated)
}
</script>

<style scoped>
.create-challenge-btn {
  width: 100%;
  /* 기존 레이아웃 유지 */
  max-width: 150px;
}



.challenge-list {
  padding: var(--space-3);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header h2 {
  margin: 0;
  font-size: 18px;
}

.challenge-card {
  margin-top: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.title-section {
  flex: 1;
}

.title-section h3 {
  margin: 8px 0 4px 0;
  font-size: 18px;
}

.desc {
  color: var(--muted);
  font-size: 14px;
  margin-top: 4px;
}

.status-badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-badge.ACTIVE {
  background: #E7F5FF;
  color: #1971C2;
}

.status-badge.COMPLETED {
  background: #D3F9D8;
  color: #2F9E44;
}

.status-badge.FAILED,
.status-badge.ABANDONED {
  background: #FFE3E3;
  color: #C92A2A;
}

.stats-section {
  display: flex;
  gap: 16px;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 12px;
  background: var(--surface-dim);
  border-radius: 8px;
}

.stat-label {
  font-size: 12px;
  color: var(--muted);
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--primary);
  margin-top: 4px;
}

.goal-display {
  padding: 12px;
  background: var(--surface-dim);
  border-radius: 8px;
  margin: 16px 0;
  font-size: 14px;
}

.progress-section {
  margin: 16px 0;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--muted);
  margin-bottom: 6px;
}

.progress-label span:last-child {
  font-weight: 700;
  color: var(--primary);
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--surface-dim);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4C6EF5, #748FFC);
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: var(--muted);
  margin-top: 8px;
  text-align: right;
}

.items {
  margin-top: 16px;
}

.items h4 {
  margin: 0 0 12px 0;
  font-size: 14px;
  color: var(--muted);
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.empty {
  color: var(--muted);
  padding: 24px;
  text-align: center;
}
</style>