<template>
  <div v-if="open" class="modal-backdrop" @click.self="onCancel">
    <div class="modal-content" role="dialog" aria-modal="true">
      <div class="modalHeader">
        <h3>리포트 생성 확인</h3>
        <p class="muted">선택한 기간의 리포트를 생성하면 AI 분석이 실행됩니다.</p>
      </div>

      <div class="modalBody">
        <div class="row">
          <div class="label">기간</div>
          <div class="value">{{ periodLabel }}</div>
        </div>

        <div class="row">
          <div class="label">동작</div>
          <div class="value">{{ mode === 'daily' ? '일별 리포트 생성' : '주간 리포트 생성' }}</div>
        </div>

        <div class="row">
          <div class="label">권한</div>
          <div class="value">{{ permissionText }}</div>
        </div>

        <div v-if="quotaLimit !== null" class="row">
          <div class="label">잔여 횟수</div>
          <div class="value">{{ quotaRemaining }} / {{ quotaLimit }}</div>
        </div>

        <div v-if="note" class="note">{{ note }}</div>

        <div v-if="error" class="error">{{ error }}</div>
      </div>

      <div class="modalFooter" :class="{ loadingState: loading }">
        <template v-if="loading">
          <img :src="loadingGif" alt="생성 중" class="loadingGif" />
          <BaseButton class="smallBtn" variant="secondary" @click="onCancel">취소</BaseButton>
        </template>
        <template v-else>
          <BaseButton variant="secondary" @click="onCancel">취소</BaseButton>
          <BaseButton :disabled="confirmDisabled" variant="primary" @click="onConfirm">
            생성 및 분석 실행
          </BaseButton>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { showToast } from '@/lib/toast.js'
import BaseButton from '@/components/base/BaseButton.vue'
import { api } from '@/lib/api.js'
import { noReportMessage } from '@/lib/reportDefaults.js'
import loadingGif from '@/assets/로딩3.gif'

const props = defineProps({
  open: Boolean,
  mode: { type: String, default: 'daily' },
  date: { type: String, default: '' },
  weekStart: { type: String, default: '' },
  selectionState: { type: String, default: 'today' }
})
const emit = defineEmits(['close', 'created', 'error'])

const loading = ref(false)
const error = ref('')
const quotaUsed = ref(null)
const quotaLimit = ref(null)
const quotaRemaining = ref(null)

const periodLabel = computed(() =>
{
  if (props.mode === 'daily') return props.date || noReportMessage
  if (!props.weekStart) return noReportMessage
  const start = new Date(props.weekStart + 'T00:00:00')
  const end = new Date(start); end.setDate(start.getDate() + 6)
  return `${start.getMonth() + 1}월 ${start.getDate()}일 – ${end.getMonth() + 1}월 ${end.getDate()}일`
})

const permissionText = computed(() =>
{
  // No server-side quota endpoint available in current codebase.
  // Show guidance and selectionState-based message.
  if (props.selectionState === 'future') return '미래 기간은 생성할 수 없습니다.'
  if (props.selectionState === 'past') return '과거 기간은 조회만 가능합니다.'
  return '생성 가능 — 생성 시 AI 분석이 수행됩니다.'
})

const confirmDisabled = computed(() =>
{
  if (props.selectionState !== 'today') return true
  if (quotaRemaining.value !== null && quotaRemaining.value <= 0) return true
  return false
})

async function fetchQuota()
{
  quotaUsed.value = null
  quotaLimit.value = null
  quotaRemaining.value = null
  try {
    const params = { type: props.mode }
    if (props.mode === 'daily' && props.date) params.date = props.date
    if (props.mode === 'weekly' && props.weekStart) params.fromDate = props.weekStart
    const res = await api.get('/reports/quota', { params })
    quotaUsed.value = res.data.used
    quotaLimit.value = res.data.limit
    quotaRemaining.value = res.data.remaining
  } catch (e) {
    // ignore quota fetch errors
  }
}

watch(() => props.open, (v) => { if (v) fetchQuota() })

async function onConfirm()
{
  error.value = ''
  if (confirmDisabled.value) return
  loading.value = true
  try {
    if (props.mode === 'daily') {
      const res = await api.post('/reports/daily', { date: props.date })
      emit('created', res.data)
    } else {
      // compute end date from weekStart
      const start = new Date(props.weekStart + 'T00:00:00')
      const end = new Date(start); end.setDate(start.getDate() + 6)
      const res = await api.post('/reports/weekly', { fromDate: props.weekStart, toDate: `${end.getFullYear()}-${String(end.getMonth() + 1).padStart(2, '0')}-${String(end.getDate()).padStart(2, '0')}` })
      emit('created', res.data)
    }
    emit('close')
  } catch (e) {
    if (e?.response?.status === 429) {
      error.value = '생성 한도를 초과했습니다. 잠시 후 다시 시도하세요.'
      showToast(error.value, 'error')
    } else if (e?.response?.data?.error) {
      error.value = e.response.data.error
      showToast(error.value, 'error')
    } else {
      error.value = e?.message || '생성 중 오류가 발생했습니다.'
      showToast(error.value, 'error')
    }
    emit('error', error.value)
  } finally {
    loading.value = false
  }
}

function onCancel()
{
  error.value = ''
  emit('close')
}
</script>

<style scoped>
.modalHeader h3 {
  margin: 0 0 6px 0
}

.muted {
  color: var(--muted);
  font-size: 13px
}

.modalBody {
  padding: 12px 0
}

.row {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 10px
}

.label {
  width: 80px;
  color: var(--muted);
  font-size: 13px
}

.value {
  font-weight: 700
}

.note {
  margin-top: 8px;
  font-size: 13px;
  color: var(--muted)
}

.error {
  margin-top: 8px;
  color: var(--danger);
  font-weight: 700
}

.modalFooter {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  margin-top: 12px
}

.modalFooter.loadingState {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px
}

.smallBtn {
  padding: 8px 12px;
  font-size: 13px;
  min-width: 0
}

.loadingGif {
  width: 200px;
  max-width: 100%;
  height: auto;
  display: block;
}

/* simple modal backdrop and content styles */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1200
}

.modal-content {
  background: #fff;
  padding: 18px;
  border-radius: 12px;
  width: 520px;
  max-width: 94%;
  box-shadow: 0 20px 50px rgba(16, 24, 40, 0.2)
}
</style>
