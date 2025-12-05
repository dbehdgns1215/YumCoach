<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()
const navLinks = [
  { name: '홈', path: '/' },
  { name: '식단', path: '/diet-record' },
  { name: '챌린지', path: '/challenge' },
  { name: '건강 정보', path: '/health-info' },
  { name: '커뮤니티', path: '/community' },
]
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
        <router-link to="/login" class="ghost-button">로그인</router-link>
        <button class="primary-button">로그아웃</button>
      </div>
    </header>
    <main>
      <router-view />
    </main>
  </div>
</template>
