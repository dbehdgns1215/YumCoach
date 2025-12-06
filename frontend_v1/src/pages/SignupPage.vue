<template>
  <section class="page auth">
    <div class="auth-card">
      <h2>회원가입</h2>
      <p class="muted">YumYumCoach를 위한 프로필을 만들어주세요.</p>
      <form class="form-stack" @submit.prevent="handleSignup">
        <label>
          <span>이름</span>
          <input type="text" v-model="formData.name" placeholder="홍길동" required />
        </label>
        <label>
          <span>이메일</span>
          <input type="email" v-model="formData.email" placeholder="example@email.com" required />
        </label>
        <label>
          <span>비밀번호</span>
          <input type="password" v-model="formData.password" placeholder="8자 이상" required />
        </label>
        <label>
          <span>비밀번호 확인</span>
          <input type="password" v-model="formData.passwordConfirm" placeholder="비밀번호 확인" required />
        </label>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
        <button type="submit" class="primary-button" :disabled="loading">
          {{ loading ? '처리 중...' : '계정 생성' }}
        </button>
      </form>
      <p class="muted">
        이미 계정이 있으신가요?
        <router-link to="/login" class="text-link">로그인</router-link>
      </p>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { userApi } from '../services/api';

const router = useRouter();

const formData = ref({
  name: '',
  email: '',
  password: '',
  passwordConfirm: ''
});

const errorMessage = ref('');
const successMessage = ref('');
const loading = ref(false);

const handleSignup = async () => {
  errorMessage.value = '';
  successMessage.value = '';

  // 비밀번호 확인
  if (formData.value.password !== formData.value.passwordConfirm) {
    errorMessage.value = '비밀번호가 일치하지 않습니다.';
    return;
  }

  loading.value = true;

  try {
    await userApi.signup({
      name: formData.value.name,
      email: formData.value.email,
      password: formData.value.password
    });
    
    successMessage.value = '회원가입이 완료되었습니다. 로그인 페이지로 이동합니다.';
    
    // 2초 후 로그인 페이지로 이동
    setTimeout(() => {
      router.push('/login');
    }, 2000);
  } catch (error) {
    errorMessage.value = error.message || '회원가입에 실패했습니다.';
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

.success-message {
  color: #27ae60;
  font-size: 0.9rem;
  margin: 0;
}
</style>

