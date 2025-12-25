<template>
  <teleport to="body">
    <div v-if="open" class="backdrop" @click.self="close">
      <!-- mobile bottom-sheet 느낌 -->
      <div ref="sheet" class="sheet" :class="{ dragging }" :style="{ transform: `translateY(${translateY}px)` }"
        role="dialog" aria-modal="true" @pointerdown="onPointerDown" @pointermove="onPointerMove"
        @pointerup="onPointerUp" @pointercancel="onPointerUp">
        <div class="grabberWrap">
          <div class="grabber" />
        </div>

        <div class="top">
          <div class="title">Advanced 리포트 🔒</div>
          <button class="x" @click="close">✕</button>
        </div>

        <div class="sub">
          지금은 <b>캐주얼 리포트</b>로도 충분히 잘하고 있어요 🙂<br />
          더 깊게 파고들고 싶을 때 Advanced가 딱 도와줄게요.
        </div>

        <!-- 혜택 3개 -->
        <div class="benefits">
          <div class="benefit">📅 요일별 패턴: 주말/평일 습관을 한눈에</div>
          <div class="benefit">⏰ 카카오톡 알림: 매일 아침마다 분석 리포트를 받아보기</div>
          <div class="benefit">🥗 영양소 추세: 목표 대비 탄단지 흐름</div>
        </div>

        <!-- 플랜 2개 -->
        <div class="plans">
          <button class="plan" :class="{ selected: selectedPlan === 'monthly' }" @click="selectedPlan = 'monthly'">
            <div class="planTop">
              <div class="planName">월간</div>
              <div class="pill">가볍게 시작</div>
            </div>
            <div class="priceRow">
              <div class="price">₩4,900</div>
              <div class="per">/월</div>
            </div>
            <div class="planSub">언제든 해지 가능</div>
          </button>

          <button class="plan" :class="{ selected: selectedPlan === 'yearly' }" @click="selectedPlan = 'yearly'">
            <div class="planTop">
              <div class="planName">연간</div>
              <div class="pill strong">추천</div>
            </div>
            <div class="priceRow">
              <div class="price">₩39,000</div>
              <div class="per">/년</div>
            </div>
            <div class="planSub">월간 대비 약 34% 절약 ✨</div>
          </button>
        </div>

        <!-- 샘플 프리뷰 -->
        <div class="sample">
          <div class="tile" />
          <div class="tile" />
          <div class="tile" />
          <div class="hint">미리보기예요 (실제 데이터는 가입 후!)</div>
        </div>

        <div class="actions">
          <button class="btn secondary" @click="close">나중에 볼게요</button>
          <button class="btn primary" @click="upgrade">
            {{ selectedPlan === 'yearly' ? '연간으로 시작' : '월간으로 시작' }}
          </button>
        </div>

        <div class="fineprint">
          <strong>결제 및 환불 안내</strong><br>
          - 결제 시점부터 Advanced 리포트 서비스가 즉시 제공됩니다.<br>
          - 구독 해지는 현재 결제주기 종료일 이후 자동 갱신이 중단됩니다. 단, 이미 결제된 기간에 대해서는 환불이 제공되지 않습니다.<br>
          - 결제일로부터 7일 이내에는 전액 환불을 요청하실 수 있으며, 7일 이후에는 이용 내역과 사유를 토대로 부분 환불 여부를 검토합니다.<br>
          - 서비스 장애로 정상적인 이용이 불가한 경우 환불 또는 이용기간 연장 등 보상을 제공할 수 있습니다.<br>
          - 환불/결제 문의는 yumcoach_support@gmail.com으로 접수해 주세요.<br>
          - 문의 접수 시 주문번호와 계정 정보를 함께 전달해 주시면 빠르게 확인해 드립니다.
        </div>
      </div>
    </div>

    <!-- 결제 모달 -->
    <TossPaymentModal :open="showPaymentModal" :plan="selectedPlan" :amount="planPrices[selectedPlan]"
      @close="handlePaymentClose" @success="handlePaymentSuccess" @fail="handlePaymentFail" />
  </teleport>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import TossPaymentModal from './TossPaymentModal.vue'

const props = defineProps({
  open: { type: Boolean, default: false },
})
const emit = defineEmits(['close', 'upgrade'])

const selectedPlan = ref('yearly')
const showPaymentModal = ref(false)

// 플랜별 금액 설정
const planPrices = {
  monthly: 4900,
  yearly: 39000
}

/** Esc 닫기 */
function onKeydown(e)
{
  if (e.key === 'Escape' && props.open) close()
}
onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))

/** 드래그 닫기 (바텀시트 느낌) */
const sheet = ref(null)
const dragging = ref(false)
const translateY = ref(0)
let startY = 0
let pointerId = null

const DRAG_CLOSE_THRESHOLD = 140 // 이 이상 내려가면 닫기
const DRAG_MAX = 320 // 시각적으로 너무 내려가지 않게

function close()
{
  translateY.value = 0
  dragging.value = false
  showPaymentModal.value = false
  emit('close')
}

function upgrade()
{
  // 결제 모달 열기
  showPaymentModal.value = true
}

function handlePaymentClose()
{
  showPaymentModal.value = false
}

function handlePaymentSuccess(data)
{
  console.log('결제 성공:', data)
  showPaymentModal.value = false
  emit('upgrade', { plan: selectedPlan.value, success: true })
  close()
}

function handlePaymentFail(error)
{
  console.error('결제 실패:', error)
  showPaymentModal.value = false
  alert('결제에 실패했습니다. 다시 시도해주세요.')
}

function isInteractiveTarget(el)
{
  // 버튼/링크/인풋 위에서 드래그 시작하면 스크롤/클릭이 우선되게
  return !!el.closest('button,a,input,textarea,select')
}

function onPointerDown(e)
{
  // 모바일/데스크탑 모두 pointer로 처리
  if (isInteractiveTarget(e.target)) return
  pointerId = e.pointerId
  startY = e.clientY
  dragging.value = true
  try { e.currentTarget.setPointerCapture(pointerId) } catch { }
}

function onPointerMove(e)
{
  if (!dragging.value || e.pointerId !== pointerId) return
  const dy = e.clientY - startY
  if (dy <= 0) {
    translateY.value = 0
    return
  }
  translateY.value = Math.min(DRAG_MAX, dy)
}

function onPointerUp(e)
{
  if (!dragging.value || e.pointerId !== pointerId) return
  dragging.value = false

  if (translateY.value >= DRAG_CLOSE_THRESHOLD) {
    close()
  } else {
    translateY.value = 0
  }
  pointerId = null
}

/** 모달 열릴 때 약간의 등장 감 */
watch(
  () => props.open,
  async (v) =>
  {
    if (v) {
      await nextTick()
      translateY.value = 0
    }
  }
)
</script>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  background: rgba(16, 24, 40, .45);
  display: flex;
  align-items: flex-end;
  /* bottom sheet */
  justify-content: center;
  padding: 12px;
  z-index: 9999;
}

/* bottom sheet 기본: 모바일은 아래에서, 데스크탑은 가운데에 가까운 느낌 */
.sheet {
  width: min(560px, 100%);
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: var(--shadow);
  padding: 14px 16px 16px;
  will-change: transform;
  transition: transform 220ms ease;
  max-height: min(86vh, 820px);
  overflow: auto;
}

.dragging {
  transition: none;
}

.grabberWrap {
  display: flex;
  justify-content: center;
  padding: 6px 0 8px;
}

.grabber {
  width: 44px;
  height: 5px;
  border-radius: 999px;
  background: rgba(16, 24, 40, .12);
}

.top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-weight: 900;
  font-size: 16px;
}

.x {
  border: 1px solid var(--border);
  background: transparent;
  border-radius: 12px;
  width: 36px;
  height: 36px;
  cursor: pointer;
}

.sub {
  margin-top: 10px;
  color: var(--muted);
  font-size: 13px;
  line-height: 1.5;
}

.benefits {
  margin-top: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.benefit {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 14px;
  padding: 10px 12px;
  font-weight: 900;
  font-size: 13px;
}

.plans {
  margin-top: 14px;
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.plan {
  text-align: left;
  width: 100%;
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 16px;
  padding: 12px;
  cursor: pointer;
}

.plan.selected {
  border-color: rgba(47, 107, 255, .55);
  box-shadow: 0 12px 26px rgba(47, 107, 255, .10);
}

.planTop {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 10px;
}

.planName {
  font-weight: 900;
  font-size: 14px;
}

.pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--primary-soft);
  color: var(--primary);
  font-weight: 900;
  font-size: 12px;
}

.pill.strong {
  background: var(--primary);
  color: #fff;
}

.priceRow {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 8px;
}

.price {
  font-weight: 1000;
  font-size: 20px;
}

.per {
  color: var(--muted);
  font-weight: 900;
  font-size: 12px;
}

.planSub {
  margin-top: 4px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
}

.sample {
  margin-top: 14px;
  border: 1px solid var(--border);
  border-radius: 14px;
  padding: 12px;
  background: linear-gradient(180deg, #fff, #f4f6ff);
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}

.tile {
  height: 56px;
  border-radius: 14px;
  border: 1px solid var(--border);
  background: rgba(255, 255, 255, .8);
  filter: blur(1px);
}

.hint {
  grid-column: 1 / -1;
  margin-top: 2px;
  color: var(--muted);
  font-size: 12px;
  font-weight: 800;
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

.secondary {
  background: var(--primary-soft);
  color: var(--primary);
}

.fineprint {
  margin-top: 20px;
  color: var(--muted);
  font-size: 10px;
}

/* 데스크탑에서는 중앙에 더 가깝게(대화형 모달 느낌) */
@media (min-width: 900px) {
  .backdrop {
    align-items: center;
  }

  .sheet {
    max-height: min(78vh, 820px);
  }

  .plans {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
