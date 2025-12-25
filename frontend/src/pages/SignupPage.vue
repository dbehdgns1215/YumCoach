<template>
    <div class="signup-page">
        <TopBarNavigation :hideActions="true" />
        <div class="content">
            <div class="signup-container">
                <h1 class="title">회원가입</h1>

                <form @submit.prevent="handleSignup" class="signup-form">
                    <div v-if="serverError" class="error-message" style="text-align:center">{{ serverError }}</div>
                    <div class="form-group">
                        <label for="email" class="label">이메일 주소</label>
                        <div class="input-action">
                            <input id="email" v-model="email" type="email" class="input" :class="{ error: emailError }"
                                @input="emailError = ''" @keyup.enter.prevent="sendEmailCode" />
                            <button type="button" class="btn small" :disabled="sendLoading || !email"
                                @click="sendEmailCode">{{ sendLoading ? '전송 중…' : '이메일 인증하기' }}</button>
                        </div>
                        <span v-if="emailError" class="error-message">{{ emailError }}</span>

                        <div class="input-action">
                            <input v-model="emailCode" type="text" class="input small" placeholder="인증번호 6자리"
                                @keyup.enter.prevent="verifyEmailCode" />
                            <button type="button" class="btn small" :disabled="verifyLoading || !emailCode || !email"
                                @click="verifyEmailCode">{{ verifyLoading ? '확인 중…' : '확인' }}</button>
                        </div>
                        <div v-if="verifyMessage" class="verify-message"
                            :class="{ success: emailVerified, error: !emailVerified }">{{ verifyMessage }}</div>
                    </div>

                    <div class="form-group">
                        <label for="password" class="label">비밀번호</label>
                        <input id="password" v-model="password" type="password" class="input"
                            :class="{ error: passwordError }" @input="passwordError = ''" />
                        <span v-if="passwordError" class="error-message">{{ passwordError }}</span>
                    </div>

                    <div class="form-group">
                        <label for="passwordConfirm" class="label">비밀번호 확인</label>
                        <input id="passwordConfirm" v-model="passwordConfirm" type="password" class="input"
                            :class="{ error: passwordConfirmError }" @input="passwordConfirmError = ''" />
                        <span v-if="passwordConfirmError" class="error-message">{{ passwordConfirmError }}</span>
                    </div>

                    <div class="form-group">
                        <label for="name" class="label">이름</label>
                        <input id="name" v-model="name" type="text" class="input" />
                    </div>
                    <div class="form-group">
                        <label for="phone" class="label">전화번호</label>
                        <input id="phone" v-model="phone" type="tel" class="input" placeholder="010-1234-5678" />
                        <span v-if="phoneError" class="error-message">{{ phoneError }}</span>
                    </div>
                    <div class="form-group">
                        <label for="referralCode" class="label">추천인 코드(선택)</label>
                        <input id="referralCode" v-model="referralCode" type="text" class="input" />
                    </div>

                    <div class="checkbox-group">
                        <label class="checkbox-label">
                            <input type="checkbox" v-model="agreePrivacy" class="checkbox" />
                            <span>[필수] 개인정보 수집 및 이용동의</span>
                        </label>
                    </div>

                    <div class="login-link">
                        <span>이미 계정이 있으신가요? </span>
                        <router-link to="/login" class="link">로그인</router-link>
                    </div>

                    <button type="submit" class="signup-button" :disabled="isSubmitting || !emailVerified">{{
                        isSubmitting ? '가입 중...' :
                        '계정 만들기' }}</button>
                </form>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import api from '@/lib/api.js'

const router = useRouter()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')
const name = ref('')
const phone = ref('')
const referralCode = ref('')
const agreePrivacy = ref(false)
const agreeMarketing = ref(false)

const emailError = ref('')
const passwordError = ref('')
const passwordConfirmError = ref('')
const phoneError = ref('')
const serverError = ref('')
const isSubmitting = ref(false)
const sendLoading = ref(false)
const verifyLoading = ref(false)
const emailCode = ref('')
const emailVerified = ref(false)
const verifyMessage = ref('')

// 이메일 변경 시 인증 상태 초기화
watch(email, () =>
{
    emailVerified.value = false
    verifyMessage.value = ''
})

async function sendEmailCode()
{
    serverError.value = ''
    verifyMessage.value = ''
    if (!email.value) {
        emailError.value = '이메일을 입력해주세요.'
        return
    }
    sendLoading.value = true
    try {
        await api.post('/auth/email/send', { email: email.value })
        verifyMessage.value = '인증번호를 전송했습니다. 메일을 확인해주세요.'
    } catch (err) {
        const msg = err?.response?.data?.message || err?.message
        verifyMessage.value = msg || '인증번호 전송 실패'
    } finally {
        sendLoading.value = false
    }
}

async function verifyEmailCode()
{
    serverError.value = ''
    emailVerified.value = false
    verifyLoading.value = true
    try {
        const res = await api.post('/auth/email/verify', { email: email.value, code: emailCode.value })
        emailVerified.value = true
        verifyMessage.value = res?.data?.message || '인증 성공'
    } catch (err) {
        const msg = err?.response?.data?.message || err?.message
        verifyMessage.value = msg || '인증 실패: 인증번호를 확인해주세요.'
    } finally {
        verifyLoading.value = false
    }
}

async function handleSignup()
{
    // 유효성 검사 초기화
    emailError.value = ''
    passwordError.value = ''
    passwordConfirmError.value = ''
    phoneError.value = ''
    serverError.value = ''

    let hasError = false

    if (!email.value) {
        emailError.value = '이메일을 입력해주세요.'
        hasError = true
    }

    if (!password.value) {
        passwordError.value = '새 비밀번호를 입력해주세요.'
        hasError = true
    }

    if (!passwordConfirm.value) {
        passwordConfirmError.value = '비밀번호를 확인해주세요.'
        hasError = true
    } else if (password.value !== passwordConfirm.value) {
        passwordConfirmError.value = '비밀번호가 일치하지 않습니다.'
        hasError = true
    }

    if (!agreePrivacy.value) {
        alert('개인정보 수집 및 이용에 동의해주세요.')
        hasError = true
    }

    // 선택 항목인 전화번호 형식 검사 (입력된 경우에만)
    if (phone.value && !/^01[0-9]-\d{3,4}-\d{4}$/.test(phone.value)) {
        phoneError.value = '전화번호 형식은 010-1234-5678 형식이어야 합니다.'
        hasError = true
    }

    // 이메일 인증 여부 확인
    if (!emailVerified.value) {
        serverError.value = '이메일 인증을 완료해주세요.'
        hasError = true
    }

    if (hasError) {
        return
    }

    const payload = {
        email: email.value,
        password: password.value,
        name: name.value,
        phone: phone.value,
        referralCode: referralCode.value,
        code: emailCode.value
    }

    isSubmitting.value = true
    try {
        await api.post('/user/signup', payload)
        router.push('/login')
    } catch (err) {
        const msg = err?.response?.data?.error || err?.response?.data?.message || err?.message
        serverError.value = msg || '가입 중 오류'
    } finally {
        isSubmitting.value = false
    }
}
</script>

<style scoped>
.signup-page {
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

.signup-container {
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

.signup-form {
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

.verify-row {
    display: flex;
    gap: 8px;
    align-items: center;
    margin-top: 8px
}

.input-action {
    display: flex;
    gap: 8px;
    align-items: center;
}

.input-action .input {
    flex: 1;
}

.btn.small {
    height: 36px;
    padding: 0 12px;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    background: #fff;
    font-weight: 700;
    cursor: pointer
}

.input.small {
    height: 36px
}

.verify-message {
    font-size: 12px
}

.verify-message.success {
    color: #16a34a
}

.verify-message.error {
    color: #ef4444
}

.checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.checkbox-label {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    font-size: 13px;
    color: #4b5563;
    cursor: pointer;
    line-height: 1.5;
}

.checkbox {
    margin-top: 2px;
    width: 16px;
    height: 16px;
    cursor: pointer;
    flex-shrink: 0;
}

.login-link {
    text-align: center;
    font-size: 14px;
    color: #6b7280;
}

.link {
    color: #4880ee;
    text-decoration: none;
    font-weight: 600;
    transition: color 0.2s;
}

.link:hover {
    color: #3b6fd9;
    text-decoration: underline;
}

.signup-button {
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

.signup-button:hover {
    background: #3b6fd9;
}

@media (max-width: 480px) {
    .signup-container {
        padding: 32px 24px;
    }

    .title {
        font-size: 24px;
    }
}
</style>
