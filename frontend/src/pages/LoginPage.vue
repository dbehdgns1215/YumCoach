<template>
    <div class="login-page">
        <TopBarNavigation :hideActions="true" />
        <div class="content">
            <div class="login-container">
                <h1 class="title">로그인</h1>

                <form @submit.prevent="handleLogin" class="login-form">
                    <div class="form-group">
                        <label for="email" class="label">이메일 주소</label>
                        <input id="email" v-model="email" type="email" class="input" :class="{ error: emailError }"
                            @input="emailError = ''" />
                        <span v-if="emailError" class="error-message">{{ emailError }}</span>
                    </div>

                    <div class="form-group">
                        <label for="password" class="label">비밀번호</label>
                        <input id="password" v-model="password" type="password" class="input"
                            :class="{ error: passwordError }" @input="passwordError = ''" />
                        <span v-if="passwordError" class="error-message">{{ passwordError }}</span>
                    </div>

                    <div class="links">
                        <a href="/signup" class="link">회원가입</a>
                        <span class="divider">|</span>
                        <a href="/reset-password" class="link">비밀번호 찾기</a>
                    </div>

                    <button type="submit" class="login-button" :disabled="isSubmitting">{{ isSubmitting ? '로그인 중...' : '로그인' }}</button>
                </form>

                <div class="divider-line">
                    <span class="divider-text">또는</span>
                </div>

                <button @click="handleKakaoLogin" class="kakao-button">
                    <span class="kakao-icon"></span>
                    카카오 로그인
                </button>
            </div>
        </div>
    </div>
</template>

<script setup>
// 로그인 페이지: 인증은 Pinia `auth` 스토어에 위임합니다.
// - 로그인 요청은 `auth.login()`을 호출하고, 성공 시 리다이렉트합니다.
// - 서버 에러 메시지(field/message)는 응답을 검사하여 폼 에러로 매핑합니다.
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()

const email = ref('')
const password = ref('')
const emailError = ref('')
const passwordError = ref('')
const isSubmitting = ref(false)

async function handleLogin() {
    // 폼 에러 초기화
    emailError.value = ''
    passwordError.value = ''

    if (!email.value) {
        emailError.value = '이메일을 입력해주세요.'
        return
    }

    if (!password.value) {
        passwordError.value = '비밀번호를 입력해주세요.'
        return
    }

    const auth = useAuthStore()
    try {
        isSubmitting.value = true
        // Pinia auth.login() 호출 (스토어에서 axios로 signin, withCredentials: true)
        const data = await auth.login({ email: email.value, password: password.value })
        // 로그인 성공 시 홈으로 이동
        router.push('/home')
    } catch (err) {
        // axios 오류 응답에서 field/message 추출하여 폼 에러로 매핑
        const resp = err?.response
        const data = resp?.data
        if (data) {
            const msg = data.message || '로그인 실패'
            if (data.field === 'email') emailError.value = msg
            else if (data.field === 'password') passwordError.value = msg
            else alert(msg)
        } else {
            alert(err.message || '로그인 중 오류')
        }
    } finally {
        isSubmitting.value = false
    }
}

function handleKakaoLogin()
{
    // 카카오 로그인 로직 구현
    console.log('카카오 로그인')
    // TODO: 카카오 SDK 연동
}
</script>

<style scoped>
.login-page {
    min-height: 100vh;
    width: 100%;
    display: flex;
    flex-direction: column;
    background: #f9fafb;
}

.content {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 20px;
    width: 100%;
}

.login-container {
    width: 100%;
    max-width: 420px;
    background: #fff;
    border-radius: 12px;
    padding: 40px 32px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.title {
    font-size: 28px;
    font-weight: 900;
    color: #1f2937;
    margin: 0 0 32px 0;
}

.login-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.label {
    font-size: 14px;
    font-weight: 600;
    color: #374151;
}

.input {
    height: 48px;
    padding: 0 16px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    font-size: 15px;
    transition: border-color 0.2s;
}

.input:focus {
    outline: none;
    border-color: #4880ee;
}

.input.error {
    border-color: #ef4444;
}

.error-message {
    font-size: 13px;
    color: #ef4444;
    margin-top: -4px;
}

.links {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    margin-top: -8px;
}

.link {
    font-size: 14px;
    color: #6b7280;
    text-decoration: none;
    transition: color 0.2s;
}

.link:hover {
    color: #4880ee;
}

.divider {
    font-size: 14px;
    color: #d1d5db;
}

.login-button {
    height: 52px;
    background: #4880ee;
    color: #fff;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    transition: background 0.2s;
    margin-top: 8px;
}

.login-button:hover {
    background: #3b6fd9;
}

.divider-line {
    position: relative;
    text-align: center;
    margin: 32px 0 24px;
}

.divider-line::before {
    content: '';
    position: absolute;
    left: 0;
    top: 50%;
    width: 100%;
    height: 1px;
    background: #e5e7eb;
}

.divider-text {
    position: relative;
    display: inline-block;
    padding: 0 12px;
    background: #fff;
    font-size: 13px;
    color: #9ca3af;
}

.kakao-button {
    width: 100%;
    height: 52px;
    background: #fee500;
    color: #000000;
    border: none;
    border-radius: 8px;
    font-size: 16px;
    font-weight: 700;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    transition: background 0.2s;
}

.kakao-button:hover {
    background: #fdd835;
}

.kakao-icon {
    font-size: 20px;
}

@media (max-width: 480px) {
    .login-container {
        padding: 32px 24px;
    }

    .title {
        font-size: 24px;
    }
}
</style>
