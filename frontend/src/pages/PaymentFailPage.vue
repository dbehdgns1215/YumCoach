<template>
    <div class="overlay">
        <div class="modal">
            <button class="close-btn" @click="goHome" aria-label="닫기">✕</button>

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
.overlay {
    position: fixed;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 24px;
    background: radial-gradient(circle at 15% 20%, rgba(240, 147, 251, 0.25), rgba(245, 87, 108, 0.15)),
        linear-gradient(135deg, rgba(240, 147, 251, 0.6) 0%, rgba(245, 87, 108, 0.6) 100%);
    backdrop-filter: blur(6px);
    z-index: 1000;
}

.modal {
    position: relative;
    width: min(480px, 100%);
    background: #ffffff;
    border-radius: 20px;
    padding: 40px 32px;
    box-shadow: 0 25px 70px rgba(0, 0, 0, 0.35);
    text-align: center;
    border: 1px solid rgba(255, 255, 255, 0.35);
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

.icon {
    width: 80px;
    height: 80px;
    margin: 0 auto 16px;
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
    margin: 0 0 8px 0;
    color: #111827;
}

.message {
    font-size: 15px;
    color: #4b5563;
    margin: 0 0 12px 0;
    line-height: 1.6;
}

.error-code {
    background: #fef2f2;
    border: 1px solid #fecaca;
    border-radius: 12px;
    padding: 12px 16px;
    margin: 8px 0 24px 0;
    font-size: 14px;
    font-weight: 700;
    color: #dc2626;
}

.actions {
    display: flex;
    gap: 10px;
    margin-top: 10px;
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
    .modal {
        padding: 32px 24px;
    }

    h1 {
        font-size: 22px;
    }

    .icon {
        width: 64px;
        height: 64px;
        font-size: 34px;
    }
}
</style>
