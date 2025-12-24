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

    <teleport to="body">
        <div v-if="resultOpen" class="result-overlay">
            <div class="result-modal" :class="resultSuccess ? 'success' : 'error'">
                <button class="close-btn" @click="closeResult" aria-label="닫기">✕</button>

                <div v-if="resultLoading" class="loading">
                    <div class="spinner"></div>
                    <p>결제를 승인하는 중입니다...</p>
                </div>

                <template v-else>
                    <div v-if="resultSuccess" class="result-content success">
                        <div class="icon">✓</div>
                        <h3>결제가 완료되었습니다!</h3>
                        <p class="message">YumCoach Advanced 구독을 시작합니다.</p>

                        <div class="details">
                            <div class="detail-row">
                                <span class="label">주문번호</span>
                                <span class="value">{{ resultData.orderId }}</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">결제금액</span>
                                <span class="value">{{ formatAmount(resultData.amount) }}</span>
                            </div>
                            <div class="detail-row">
                                <span class="label">결제수단</span>
                                <span class="value">{{ resultData.method }}</span>
                            </div>
                        </div>

                        <div class="actions">
                            <button class="btn primary" @click="closeResult">확인</button>
                        </div>
                    </div>

                    <div v-else class="result-content error">
                        <div class="icon">✕</div>
                        <h3>결제 승인 실패</h3>
                        <p class="message">{{ resultError }}</p>

                        <div class="actions">
                            <button class="btn secondary" @click="closeResult">닫기</button>
                            <button class="btn primary" @click="handlePayment">다시 시도</button>
                        </div>
                    </div>
                </template>
            </div>
        </div>
    </teleport>
</template>

<script setup>
import { onMounted, onUnmounted, ref, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/lib/api'

const props = defineProps({
    open: { type: Boolean, default: false },
    plan: { type: String, required: true }, // 'monthly' or 'yearly'
    amount: { type: Number, required: true },
})

const emit = defineEmits(['close', 'success', 'fail'])

const route = useRoute()
const router = useRouter()

const isProcessing = ref(false)
const payButton = ref(null)

const resultOpen = ref(false)
const resultSuccess = ref(false)
const resultError = ref('')
const resultLoading = ref(false)
const resultData = ref({})

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

function getReturnUrl()
{
    const url = new URL(window.location.href)
    url.search = ''
    url.hash = ''
    return url.toString().replace(/\/?$/, '')
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

        // 결제 요청 (Redirect 방식) - 현재 페이지로 다시 돌아오도록 설정해 결과 모달을 바로 띄움
        const returnUrl = getReturnUrl()
        await widgets.requestPayment({
            orderId: generateOrderId(),
            orderName: props.plan === 'yearly'
                ? 'YumCoach Advanced 연간 구독'
                : 'YumCoach Advanced 월간 구독',
            // 같은 페이지로 돌아오되 결제 결과 쿼리를 포함해 오버레이로 처리
            successUrl: `${returnUrl}?plan=${props.plan}`,
            failUrl: `${returnUrl}?plan=${props.plan}`,
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

async function handleRedirectResult()
{
    const { paymentKey, orderId, amount } = route.query
    if (!paymentKey || !orderId || !amount) return

    resultLoading.value = true
    resultOpen.value = true
    try {
        const { data } = await api.post('/payments/confirm', {
            paymentKey,
            orderId,
            amount: parseInt(amount, 10),
            planType: route.query.plan || props.plan,
        })

        resultSuccess.value = true
        resultError.value = ''
        resultData.value = {
            orderId: data.orderId,
            amount: data.totalAmount,
            method: getPaymentMethodName(data.method),
            approvedAt: data.approvedAt,
        }
        emit('success', resultData.value)
    } catch (error) {
        console.error('결제 승인 실패:', error)
        resultSuccess.value = false
        resultError.value = error.message || '결제 승인 중 오류가 발생했습니다.'
        emit('fail', error)
    } finally {
        resultLoading.value = false
        // 쿼리 정리
        const nextQuery = { ...route.query }
        delete nextQuery.paymentKey
        delete nextQuery.orderId
        delete nextQuery.amount
        router.replace({ query: nextQuery })
    }
}

function closeResult()
{
    resultOpen.value = false
    resultError.value = ''
}

function getPaymentMethodName(method)
{
    const methodNames = {
        '카드': '신용/체크카드',
        '가상계좌': '가상계좌',
        '계좌이체': '계좌이체',
        '휴대폰': '휴대폰 소액결제',
        '상품권': '상품권',
        '간편결제': '간편결제',
    }
    return methodNames[method] || method
}

function formatAmount(amount)
{
    if (!amount) return '₩0'
    return `₩${amount.toLocaleString()}`
}

// Esc 키로 닫기
function onKeydown(e)
{
    if (e.key === 'Escape' && props.open && !isProcessing.value) {
        close()
    }
    if (e.key === 'Escape' && resultOpen.value && !resultLoading.value) {
        closeResult()
    }
}

onMounted(() =>
{
    window.addEventListener('keydown', onKeydown)
    handleRedirectResult()
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

watch(
    () => route.query.paymentKey,
    () =>
    {
        handleRedirectResult()
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

.result-overlay {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: rgba(17, 24, 39, 0.55);
    backdrop-filter: blur(6px);
    z-index: 11000;
}

.result-modal {
    position: relative;
    width: min(520px, 100%);
    background: #ffffff;
    border-radius: 20px;
    padding: 32px 28px;
    box-shadow: 0 22px 70px rgba(0, 0, 0, 0.35);
    border: 1px solid rgba(255, 255, 255, 0.35);
    text-align: center;
}

.close-btn {
    position: absolute;
    top: 14px;
    right: 14px;
    width: 36px;
    height: 36px;
    border-radius: 12px;
    border: none;
    background: #f3f4f6;
    font-size: 18px;
    font-weight: 700;
    color: #1f2937;
    cursor: pointer;
    transition: all 0.2s;
}

.close-btn:hover {
    background: #e5e7eb;
    transform: translateY(-1px);
}

.result-content {
    display: flex;
    flex-direction: column;
    gap: 14px;
}

.result-content .icon {
    width: 76px;
    height: 76px;
    margin: 0 auto;
    color: #ffffff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 46px;
    font-weight: bold;
}

.result-content.success .icon {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.result-content.error .icon {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    animation: shake 0.5s ease-out;
}

.result-content h3 {
    font-size: 24px;
    font-weight: 900;
    margin: 8px 0 0 0;
    color: #111827;
}

.result-content .message {
    font-size: 15px;
    color: #4b5563;
    margin: 0;
    line-height: 1.6;
}

.details {
    background: #f9fafb;
    border-radius: 14px;
    padding: 16px 18px;
    text-align: left;
    border: 1px solid #e5e7eb;
}

.detail-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 0;
    border-bottom: 1px solid #e5e7eb;
}

.detail-row:last-child {
    border-bottom: none;
}

.label {
    font-size: 13px;
    font-weight: 700;
    color: #6b7280;
}

.value {
    font-size: 15px;
    font-weight: 900;
    color: #111827;
}

.actions {
    display: flex;
    gap: 10px;
    margin-top: 6px;
}

.btn {
    flex: 1;
    padding: 14px;
    border: none;
    border-radius: 12px;
    font-size: 15px;
    font-weight: 800;
    cursor: pointer;
    transition: all 0.2s;
}

.btn.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
    box-shadow: 0 10px 25px rgba(102, 126, 234, 0.35);
}

.btn.primary:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 28px rgba(102, 126, 234, 0.4);
}

.btn.secondary {
    background: #f3f4f6;
    color: #1f2937;
}

.btn.secondary:hover {
    background: #e5e7eb;
}

.loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16px;
    color: #1f2937;
}

.spinner {
    width: 56px;
    height: 56px;
    border: 5px solid rgba(102, 126, 234, 0.25);
    border-top-color: #667eea;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

.loading p {
    margin: 0;
    font-size: 16px;
    font-weight: 700;
}

@keyframes shake {

    0%,
    100% {
        transform: translateX(0);
    }

    10%,
    30%,
    50%,
    70%,
    90% {
        transform: translateX(-8px);
    }

    20%,
    40%,
    60%,
    80% {
        transform: translateX(8px);
    }
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
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

    .result-modal {
        padding: 28px 22px;
    }

    .result-content h3 {
        font-size: 21px;
    }
}
</style>
