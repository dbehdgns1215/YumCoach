<template>
  <section class="page auth">
    <div class="auth-card">
      <h2>로그인</h2>
      <p class="muted">계정을 입력하고 서비스를 이용하세요.</p>
      <form class="form-stack" @submit.prevent="handleLogin">
        <label>
          <span>이메일</span>
          <input type="email" v-model="formData.email" placeholder="example@email.com" required />
        </label>
        <label>
          <span>비밀번호</span>
          <input type="password" v-model="formData.password" placeholder="••••••••" required />
        </label>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <button type="submit" class="primary-button" :disabled="loading">
          {{ loading ? '로그인 중...' : '로그인' }}
        </button>
      </form>
      <p class="muted">
        계정이 없으신가요?
        <router-link to="/signup" class="text-link">회원가입</router-link>
      </p>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../services/api';
import { authStore } from '../services/store';

const router = useRouter();

const formData = ref({
  email: '',
  password: ''
});

const errorMessage = ref('');
const loading = ref(false);

const handleLogin = async () => {
  errorMessage.value = '';
  loading.value = true;

  try {
    const response = await userApi.signin(formData.value);
    
    // 토큰 저장
    authStore.setTokens(response.accessToken, response.refreshToken);
    
    // 사용자 정보 저장
    authStore.setUser({
      id: response.userId,
      email: response.email,
      name: response.name
    });
    
    // 홈으로 이동
    router.push('/');
  } catch (error) {
    errorMessage.value = error.message || '로그인에 실패했습니다.';
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.error-message {
  color: #e74c3c;
  font-size: 0.9rem;
  margin: 0;
}
</style>

