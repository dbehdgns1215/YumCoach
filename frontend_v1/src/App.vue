<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authStore } from './services/store'
import { userApi } from './services/api'

const route = useRoute()
const router = useRouter()

const navLinks = [
  { name: '홈', path: '/' },
  { name: '식단', path: '/diet-record' },
  { name: '챌린지', path: '/challenge' },
  { name: '건강 정보', path: '/health-info' },
  { name: '커뮤니티', path: '/community' },
]

const handleLogout = async () => {
  try {
    await userApi.signout()
  } catch (error) {
    console.error('로그아웃 오류:', error)
  } finally {
    authStore.clearAuth()
    router.push('/login')
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="app-header">
      <div class="brand">YumYumCoach</div>
      <nav>
        <router-link
          v-for="link in navLinks"
          :key="link.name"
          :to="link.path"
          :class="['nav-link', { active: route.path === link.path }]"
        >
          {{ link.name }}
        </router-link>
      </nav>
      <div class="header-actions">
        <template v-if="authStore.isAuthenticated()">
          <span class="user-name">{{ authStore.user?.name || '사용자' }}님</span>
          <button class="ghost-button" @click="handleLogout">로그아웃</button>
        </template>
        <template v-else>
          <router-link to="/login" class="ghost-button">로그인</router-link>
          <router-link to="/signup" class="primary-button">회원가입</router-link>
        </template>
      </div>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-name {
  color: #ffffff !important;
  font-weight: 500;
  margin-right: 1rem;
}
</style>
