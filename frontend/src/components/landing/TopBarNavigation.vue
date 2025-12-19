<template>
    <header class="topbar" data-name="Header">
        <div class="left">
            <router-link to="/landing">
                <img class="logo" :src="logoSrc" alt="YumCoach" />
            </router-link>
        </div>

        <!-- Before Login -->
        <div class="right" v-if="!isLoggedIn && !hideActions">
            <router-link to="/login" class="btn ghost">로그인</router-link>
            <router-link to="/signup" class="btn primary">회원가입</router-link>
        </div>

        <!-- After Login -->
        <nav class="nav-menu" v-if="isLoggedIn">
            <router-link to="/home" class="nav-item">홈</router-link>
            <router-link to="/log" class="nav-item">식단 등록</router-link>
            <router-link to="/report" class="nav-item">리포트</router-link>
            <router-link to="/community" class="nav-item">커뮤니티</router-link>
            <router-link to="/challenge" class="nav-item">챌린지</router-link>
            <router-link to="/coach" class="nav-item">챗봇</router-link>
            <router-link to="/mypage" class="nav-item">마이페이지</router-link>
        </nav>
    </header>
</template>

<script setup>
import logoSrc from '@/assets/logo.png'

defineProps({
    hideActions: { type: Boolean, default: false },
    isLoggedIn: { type: Boolean, default: false }
})
</script>

<style scoped>
.topbar {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    background: #fff;
    border-bottom: 1px solid var(--border);
}

.logo {
    height: 50px;
    width: auto;
    cursor: pointer;
}

.left a {
    display: flex;
    align-items: center;
    text-decoration: none;
}

.right {
    display: flex;
    gap: 12px;
}

.btn {
    padding: 8px 16px;
    border-radius: 30px;
    font-weight: 800;
    cursor: pointer;
    font-size: 14px;
    border: 1px solid #c7c7c7;
    text-decoration: none;
    display: inline-block;
    text-align: center;
    transition: all 0.2s;
}

.primary {
    background: #4880EE;
    color: #fff;
}

.ghost {
    background: #fff;
    color: #7b7b7b;
}

.nav-menu {
    display: flex;
    gap: 24px;
    flex: 1;
    justify-content: center;
}

.nav-item {
    font-size: 14px;
    font-weight: 600;
    color: #6b7280;
    text-decoration: none;
    transition: color 0.2s;
    padding: 8px 0;
    border-bottom: 2px solid transparent;
}

.nav-item:hover {
    color: #4880ee;
}

.nav-item.router-link-active {
    color: #4880ee;
    border-bottom-color: #4880ee;
}
</style>
