<template>
    <div class="overlay">
        <div v-if="!loading" class="modal">
            <button class="close-btn" @click="goHome" aria-label="닫기">✕</button>

            <div v-if="isSuccess" class="content success">
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

                <div class="actions">
                    <button class="btn primary" @click="goHome">홈으로 돌아가기</button>
                </div>
            </div>

            <div v-else class="content error">
                <div class="icon">✕</div>
                <h1>결제 승인 실패</h1>
                <p class="message">{{ errorMessage }}</p>

                <div class="actions">
                    <button class="btn secondary" @click="goHome">홈으로</button>
                    <button class="btn primary" @click="retry">다시 시도</button>
                </div>
            </div>
        </div>

        <div v-else class="modal loading">
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
.overlay {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: radial-gradient(circle at 20% 20%, rgba(102, 126, 234, 0.25), rgba(118, 75, 162, 0.1)),
        linear-gradient(135deg, rgba(102, 126, 234, 0.55) 0%, rgba(118, 75, 162, 0.55) 100%);
    backdrop-filter: blur(6px);
    z-index: 1000;
}

.modal {
    position: relative;
    width: min(520px, 100%);
    background: #ffffff;
    border-radius: 20px;
    padding: 40px 32px;
    box-shadow: 0 25px 70px rgba(0, 0, 0, 0.35);
    text-align: center;
    border: 1px solid rgba(255, 255, 255, 0.35);
}

.content {
    display: flex;
    flex-direction: column;
    gap: 18px;
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

.success .icon {
    width: 80px;
    height: 80px;
    margin: 0 auto;
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
    margin: 0 auto;
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
    font-size: 26px;
    font-weight: 900;
    margin: 10px 0 0 0;
    color: #111827;
}

.message {
    font-size: 15px;
    color: #4b5563;
    margin: 0 0 14px 0;
    line-height: 1.6;
}

.details {
    background: #f9fafb;
    border-radius: 14px;
    padding: 16px 18px;
    margin-top: 6px;
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
    margin-top: 8px;
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
    width: 60px;
    height: 60px;
    border: 5px solid rgba(102, 126, 234, 0.25);
    border-top-color: #667eea;
    border-radius: 50%;
    animation: spin 1s linear infinite;
}

.loading p {
    font-size: 16px;
    font-weight: 700;
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
    .modal {
        padding: 32px 24px;
    }

    h1 {
        font-size: 22px;
    }

    .icon {
        width: 64px !important;
        height: 64px !important;
        font-size: 34px !important;
    }
}
</style>
