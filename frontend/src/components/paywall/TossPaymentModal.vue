<template>
    <teleport to="body">
        <div v-if="open" class="backdrop" @click.self="close">
            <div class="modal" role="dialog" aria-modal="true">
                <div class="header">
                    <h2>결제하기</h2>
                    <button class="x" @click="close">✕</button>
                </div>

                <div class="content">
                    <!-- 결제 위젯이 렌더링될 컨테이너 -->
                    <div id="payment-method" class="payment-widget"></div>
                    <div id="agreement" class="agreement-widget"></div>

                    <button ref="payButton" class="pay-button" @click="handlePayment" :disabled="isProcessing">
                        {{ isProcessing ? '처리 중...' : '결제하기' }}
                    </button>
                </div>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'

const props = defineProps({
    open: { type: Boolean, default: false },
    plan: { type: String, required: true }, // 'monthly' or 'yearly'
    amount: { type: Number, required: true },
})

const emit = defineEmits(['close', 'success', 'fail'])

const isProcessing = ref(false)
const payButton = ref(null)

let tossPayments = null
let widgets = null
let paymentMethodWidget = null
let agreementWidget = null

// 테스트용 클라이언트 키 (실제 운영에서는 환경변수로 관리)
const TOSS_CLIENT_KEY = import.meta.env.VITE_TOSS_CLIENT_KEY || 'test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm'
const CUSTOMER_KEY = generateCustomerKey()

function generateCustomerKey()
{
    // 로그인된 사용자의 고유 ID를 사용하는 것이 좋습니다
    // 여기서는 UUID 형식으로 생성
    return `customer_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
}

function generateOrderId()
{
    return `order_${Date.now()}_${Math.random().toString(36).substring(2, 9)}`
}

async function initializePaymentWidget()
{
    try {
        // Toss Payments SDK 로드 대기
        if (!window.TossPayments) {
            console.error('TossPayments SDK가 로드되지 않았습니다.')
            return
        }

        // Toss Payments 초기화
        tossPayments = window.TossPayments(TOSS_CLIENT_KEY)

        // 결제위젯 초기화
        widgets = tossPayments.widgets({
            customerKey: CUSTOMER_KEY,
        })

        // 결제 금액 설정
        await widgets.setAmount({
            currency: 'KRW',
            value: props.amount,
        })

        // 결제 UI 렌더링
        paymentMethodWidget = await widgets.renderPaymentMethods({
            selector: '#payment-method',
            variantKey: 'DEFAULT',
        })

        // 약관 UI 렌더링
        agreementWidget = await widgets.renderAgreement({
            selector: '#agreement',
            variantKey: 'AGREEMENT',
        })

        console.log('결제 위젯 초기화 완료')
    } catch (error) {
        console.error('결제 위젯 초기화 실패:', error)
        alert('결제 위젯을 불러오는데 실패했습니다. 페이지를 새로고침해주세요.')
    }
}

async function handlePayment()
{
    if (isProcessing.value) return

    try {
        isProcessing.value = true

        // 결제 요청 (Redirect 방식)
        await widgets.requestPayment({
            orderId: generateOrderId(),
            orderName: props.plan === 'yearly'
                ? 'YumCoach Advanced 연간 구독'
                : 'YumCoach Advanced 월간 구독',
            // 플랜 정보를 successUrl에 포함해 백엔드에 전달
            successUrl: `${window.location.origin}/payment/success?plan=${props.plan}`,
            failUrl: `${window.location.origin}/payment/fail`,
            customerEmail: 'customer@example.com', // 실제 사용자 이메일로 교체
            customerName: '김유저', // 실제 사용자 이름으로 교체
            customerMobilePhone: '01012345678', // 실제 사용자 전화번호로 교체 (선택)
        })
    } catch (error) {
        console.error('결제 요청 실패:', error)

        if (error.code === 'USER_CANCEL') {
            // 사용자가 결제를 취소한 경우
            alert('결제가 취소되었습니다.')
        } else if (error.code === 'INVALID_PARAMETER') {
            alert('결제 정보가 올바르지 않습니다.')
        } else {
            alert('결제 요청 중 오류가 발생했습니다.')
        }

        isProcessing.value = false
    }
}

function close()
{
    emit('close')
}

async function cleanup()
{
    try {
        if (paymentMethodWidget) {
            await paymentMethodWidget.destroy()
            paymentMethodWidget = null
        }
        if (agreementWidget) {
            await agreementWidget.destroy()
            agreementWidget = null
        }
    } catch (error) {
        console.error('위젯 정리 실패:', error)
    }
}

// Esc 키로 닫기
function onKeydown(e)
{
    if (e.key === 'Escape' && props.open && !isProcessing.value) {
        close()
    }
}

onMounted(() =>
{
    window.addEventListener('keydown', onKeydown)
})

onUnmounted(() =>
{
    window.removeEventListener('keydown', onKeydown)
    cleanup()
})

// 모달이 열릴 때 결제 위젯 초기화
watch(
    () => props.open,
    async (newVal) =>
    {
        if (newVal) {
            await nextTick()
            await initializePaymentWidget()
        } else {
            await cleanup()
        }
    }
)

// 금액이 변경되면 업데이트
watch(
    () => props.amount,
    async (newAmount) =>
    {
        if (widgets && props.open) {
            try {
                await widgets.setAmount({
                    currency: 'KRW',
                    value: newAmount,
                })
            } catch (error) {
                console.error('금액 업데이트 실패:', error)
            }
        }
    }
)
</script>

<style scoped>
.backdrop {
    position: fixed;
    inset: 0;
    background: rgba(16, 24, 40, 0.6);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    z-index: 10000;
    backdrop-filter: blur(4px);
}

.modal {
    width: min(640px, 100%);
    max-height: 90vh;
    background: var(--surface, #ffffff);
    border: 1px solid var(--border, #e5e7eb);
    border-radius: 20px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    overflow: hidden;
    display: flex;
    flex-direction: column;
}

.header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    border-bottom: 1px solid var(--border, #e5e7eb);
    background: #fff;
}

.header h2 {
    font-size: 20px;
    font-weight: 900;
    margin: 0;
}

.x {
    border: 1px solid var(--border, #e5e7eb);
    background: transparent;
    border-radius: 12px;
    width: 36px;
    height: 36px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    transition: all 0.2s;
}

.x:hover {
    background: #f3f4f6;
}

.content {
    padding: 24px;
    overflow-y: auto;
    flex: 1;
}

.payment-widget {
    min-height: 300px;
    margin-bottom: 20px;
}

.agreement-widget {
    margin-bottom: 20px;
}

.pay-button {
    width: 100%;
    background: var(--primary, #2f6bff);
    color: #ffffff;
    border: none;
    border-radius: 14px;
    padding: 16px;
    font-size: 16px;
    font-weight: 900;
    cursor: pointer;
    transition: all 0.2s;
}

.pay-button:hover:not(:disabled) {
    background: #1e5aef;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(47, 107, 255, 0.3);
}

.pay-button:disabled {
    background: #9ca3af;
    cursor: not-allowed;
    opacity: 0.7;
}

@media (max-width: 768px) {
    .modal {
        width: 100%;
        max-height: 95vh;
        border-radius: 16px 16px 0 0;
        align-self: flex-end;
    }

    .content {
        padding: 16px;
    }
}
</style>
