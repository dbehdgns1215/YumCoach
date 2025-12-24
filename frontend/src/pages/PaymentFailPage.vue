<template>
    <div class="container">
        <div class="card">
            <div class="icon">✕</div>
            <h1>결제 실패</h1>
            <p class="message">{{ errorMessage }}</p>

            <div class="error-code" v-if="errorCode">
                오류 코드: {{ errorCode }}
            </div>

            <div class="actions">
                <button class="btn secondary" @click="goHome">홈으로</button>
                <button class="btn primary" @click="retry">다시 시도</button>
            </div>
        </div>
    </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const errorMessage = ref('결제에 실패했습니다.')
const errorCode = ref('')

onMounted(() =>
{
    // URL 쿼리에서 에러 정보 추출
    const { code, message } = route.query

    if (message) {
        errorMessage.value = decodeURIComponent(message)
    }

    if (code) {
        errorCode.value = code
    }
})

function goHome()
{
    router.push('/')
}

function retry()
{
    // 이전 페이지로 돌아가기
    // router.go(-1)
    router.push('/report')
}
</script>

<style scoped>
.container {
    min-height: 100vh;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.card {
    width: min(500px, 100%);
    background: #ffffff;
    border-radius: 24px;
    padding: 48px 32px;
    box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
    text-align: center;
}

.icon {
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
    margin: 0 0 20px 0;
    line-height: 1.6;
}

.error-code {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 12px;
    padding: 12px 16px;
    margin-bottom: 32px;
    font-size: 14px;
    font-weight: 600;
    color: #dc2626;
}

.actions {
    display: flex;
    gap: 12px;
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

@media (max-width: 640px) {
    .card {
        padding: 32px 24px;
    }

    h1 {
        font-size: 24px;
    }

    .icon {
        width: 64px;
        height: 64px;
        font-size: 36px;
    }
}
</style>
