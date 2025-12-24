<template>
    <div class="container">
        <div class="card" v-if="!loading">
            <div v-if="isSuccess" class="success">
                <div class="icon">✓</div>
                <h1>결제가 완료되었습니다!</h1>
                <p class="message">YumCoach Advanced 구독을 시작합니다.</p>

                <div class="details">
                    <div class="detail-row">
                        <span class="label">주문번호</span>
                        <span class="value">{{ paymentData.orderId }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">결제금액</span>
                        <span class="value">{{ formatAmount(paymentData.amount) }}</span>
                    </div>
                    <div class="detail-row">
                        <span class="label">결제수단</span>
                        <span class="value">{{ paymentData.method }}</span>
                    </div>
                </div>

                <button class="btn primary" @click="goHome">
                    홈으로 돌아가기
                </button>
            </div>

            <div v-else class="error">
                <div class="icon">✕</div>
                <h1>결제 승인 실패</h1>
                <p class="message">{{ errorMessage }}</p>

                <div class="actions">
                    <button class="btn secondary" @click="goHome">홈으로</button>
                    <button class="btn primary" @click="retry">다시 시도</button>
                </div>
            </div>
        </div>

        <div v-else class="loading">
            <div class="spinner"></div>
            <p>결제를 승인하는 중입니다...</p>
        </div>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/lib/api'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const isSuccess = ref(false)
const errorMessage = ref('')
const paymentData = ref({})

onMounted(async () =>
{
    await confirmPayment()
})

async function confirmPayment()
{
    try {
        // URL 쿼리 파라미터에서 결제 정보 추출
        const { paymentKey, orderId, amount, plan } = route.query

        if (!paymentKey || !orderId || !amount) {
            throw new Error('필수 결제 정보가 누락되었습니다.')
        }

        // 백엔드 API로 결제 승인 요청 (axios 인스턴스 사용: 자동 Authorization 헤더)
        const { data } = await api.post('/payments/confirm', {
            paymentKey,
            orderId,
            amount: parseInt(amount),
            // 로그인 연동 시 userId 추가 가능
            // userId: currentUser?.id,
            planType: plan || undefined,
        })

        // 결제 성공
        isSuccess.value = true
        paymentData.value = {
            orderId: data.orderId,
            amount: data.totalAmount,
            method: getPaymentMethodName(data.method),
            approvedAt: data.approvedAt,
        }

    } catch (error) {
        console.error('결제 승인 실패:', error)
        isSuccess.value = false
        errorMessage.value = error.message || '결제 승인 중 오류가 발생했습니다.'
    } finally {
        loading.value = false
    }
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

function goHome()
{
    router.push('/')
}

function retry()
{
    router.push('/')
}
</script>

<style scoped>
.container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.card {
    width: min(500px, 100%);
    background: #ffffff;
    border-radius: 24px;
    padding: 48px 32px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    text-align: center;
}

.success .icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 48px;
    font-weight: bold;
    animation: scaleIn 0.5s ease-out;
}

.error .icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 24px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
    color: #ffffff;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 48px;
    font-weight: bold;
    animation: shake 0.5s ease-out;
}

h1 {
    font-size: 28px;
    font-weight: 900;
    margin: 0 0 12px 0;
    color: #1f2937;
}

.message {
    font-size: 16px;
    color: #6b7280;
    margin: 0 0 32px 0;
    line-height: 1.6;
}

.details {
    background: #f9fafb;
    border-radius: 16px;
    padding: 20px;
    margin-bottom: 32px;
    text-align: left;
}

.detail-row {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 0;
    border-bottom: 1px solid #e5e7eb;
}

.detail-row:last-child {
    border-bottom: none;
}

.label {
    font-size: 14px;
    font-weight: 600;
    color: #6b7280;
}

.value {
    font-size: 16px;
    font-weight: 900;
    color: #1f2937;
}

.actions {
    display: flex;
    gap: 12px;
    margin-top: 24px;
}

.btn {
    flex: 1;
    padding: 16px;
    border: none;
    border-radius: 14px;
    font-size: 16px;
    font-weight: 900;
    cursor: pointer;
    transition: all 0.2s;
}

.btn.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: #ffffff;
}

.btn.primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4);
}

.btn.secondary {
    background: #f3f4f6;
    color: #1f2937;
}

.btn.secondary:hover {
    background: #e5e7eb;
}

.loading {
    text-align: center;
    color: #ffffff;
}

.spinner {
    width: 60px;
    height: 60px;
    margin: 0 auto 24px;
    border: 4px solid rgba(255, 255, 255, 0.3);
    border-top-color: #ffffff;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

.loading p {
    font-size: 18px;
    font-weight: 600;
    margin: 0;
}

@keyframes scaleIn {
    from {
        transform: scale(0);
        opacity: 0;
    }

    to {
        transform: scale(1);
        opacity: 1;
    }
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
        transform: translateX(-10px);
    }

    20%,
    40%,
    60%,
    80% {
        transform: translateX(10px);
    }
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

@media (max-width: 640px) {
    .card {
        padding: 32px 24px;
    }

    h1 {
        font-size: 24px;
    }

    .icon {
        width: 64px !important;
        height: 64px !important;
        font-size: 36px !important;
    }
}
</style>
