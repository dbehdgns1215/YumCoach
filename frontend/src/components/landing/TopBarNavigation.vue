<template>
    <header class="topbar" data-name="Header">
        <div class="left">
            <router-link :to="logoTo" @click="onLogoClick">
                <img class="logo" :src="logoSrc" alt="YumCoach" />
            </router-link>
        </div>

        <!-- Before Login -->
        <div class="right" v-if="!loggedIn && !hideActions">
            <router-link to="/login" class="btn ghost">로그인</router-link>
            <router-link to="/signup" class="btn primary">회원가입</router-link>
        </div>

        <!-- After Login - Desktop -->
        <nav class="nav-menu" v-if="loggedIn && isDesktop">
            <router-link to="/log" class="nav-item">식단 등록</router-link>
            <router-link to="/report" class="nav-item">리포트</router-link>
            <router-link to="/community" class="nav-item">커뮤니티</router-link>
            <router-link to="/challenge" class="nav-item">챌린지</router-link>
            <router-link to="/coach" class="nav-item">챗봇</router-link>
            <router-link to="/mypage" class="nav-item">마이페이지</router-link>
        </nav>

        <!-- After Login - Mobile Hamburger -->
        <button v-if="loggedIn && !isDesktop" class="hamburger" @click="menuOpen = !menuOpen">
            <span></span>
            <span></span>
            <span></span>
        </button>

        <!-- Logout 버튼 (데스크탑 오른쪽) -->
        <div class="right" v-if="loggedIn && !hideActions && isDesktop">
            <button class="btn ghost" @click="handleLogout">로그아웃</button>
        </div>

        <!-- Mobile Menu Drawer -->
        <nav v-if="loggedIn && !isDesktop && menuOpen" class="mobile-menu">
            <router-link to="/log" class="mobile-nav-item" @click="menuOpen = false">식단 등록</router-link>
            <router-link to="/report" class="mobile-nav-item" @click="menuOpen = false">리포트</router-link>
            <router-link to="/community" class="mobile-nav-item" @click="menuOpen = false">커뮤니티</router-link>
            <router-link to="/challenge" class="mobile-nav-item" @click="menuOpen = false">챌린지</router-link>
            <router-link to="/coach" class="mobile-nav-item" @click="menuOpen = false">챗봇</router-link>
            <router-link to="/mypage" class="mobile-nav-item" @click="menuOpen = false">마이페이지</router-link>
            <button class="mobile-nav-item" @click="(menuOpen = false, handleLogout())">로그아웃</button>
        </nav>
    </header>
</template>

<script setup>
// 헤더 리팩토링: 인증 상태는 Pinia `auth` 스토어에서 읽습니다.
// - 이 컴포넌트는 네트워크 호출을 직접 하지 않습니다.
// - 보여줄 로그인 상태는 부모에서 명시적으로 전달된 `isLoggedIn` 프롭이 있으면 그 값을 우선 사용하고,
//   없으면 스토어의 `isAuthenticated`를 사용합니다.
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import logoSrc from '@/assets/logo.png'

const logoTo = computed(() => (loggedIn.value ? "/log" : "/landing"));

const clickCount = ref(0);
let clickTimer = null;

// “빠르게”의 기준(원하는 대로 조절)
const TRIPLE_WINDOW_MS = 500;

const props = defineProps({
    hideActions: { type: Boolean, default: false }
})

const auth = useAuthStore()
const router = useRouter()

const menuOpen = ref(false)
const isDesktop = ref(true)

const checkIsDesktop = () =>
{
    isDesktop.value = window.innerWidth >= 960
}

onMounted(() =>
{
    checkIsDesktop()
    window.addEventListener('resize', checkIsDesktop)
})

onUnmounted(() =>
{
    window.removeEventListener('resize', checkIsDesktop)

    if (clickTimer) {
        clearTimeout(clickTimer);
        clickTimer = null;
    }
})

// 로그인 여부는 항상 Pinia `auth` 스토어를 기준으로 결정합니다
const loggedIn = computed(() => auth.isAuthenticated)

async function handleLogout()
{
    try {
        await auth.logout()
    } finally {
        // 로그아웃 후 랜딩 페이지로 이동
        router.push('/landing').catch(() => { })
    }
}

function onLogoClick(e)
{
    // 로그인 전에는 그냥 정상 이동
    if (!loggedIn.value) return;

    clickCount.value += 1;

    // 첫 클릭이면 타이머 시작
    if (!clickTimer) {
        clickTimer = setTimeout(() =>
        {
            // 시간 내 3번 못 채우면 리셋 (기본 이동은 router-link가 이미 처리)
            clickCount.value = 0;
            clickTimer = null;
        }, TRIPLE_WINDOW_MS);
    }

    // 3번 성공: 기본 이동 막고 /mini로
    if (clickCount.value >= 3) {
        e.preventDefault(); // router-link 기본 이동 막기
        clearTimeout(clickTimer);
        clickTimer = null;
        clickCount.value = 0;

        router.push("/mini").catch(() => { });
    }
}
</script>

<style scoped>
.topbar {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
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
    padding: 12px 20px;
}

.right {
    display: flex;
    gap: 12px;
    padding: 12px 20px;
    align-self: flex-end;
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

/* Hamburger Button */
.hamburger {
    display: flex;
    flex-direction: column;
    gap: 5px;
    background: none;
    border: none;
    cursor: pointer;
    padding: 12px 20px;
    align-self: flex-end;
}

.hamburger span {
    width: 24px;
    height: 3px;
    background: #374151;
    border-radius: 2px;
    transition: all 0.3s;
}

/* Mobile Menu */
.mobile-menu {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    background: #fff;
    border-bottom: 1px solid var(--border);
    display: flex;
    flex-direction: column;
    padding: 12px 20px;
    gap: 12px;
    z-index: 9;
}

.mobile-nav-item {
    font-size: 14px;
    font-weight: 600;
    color: #6b7280;
    text-decoration: none;
    padding: 8px 0;
    transition: color 0.2s;
}

.mobile-nav-item:hover {
    color: #4880ee;
}

.mobile-nav-item.router-link-active {
    color: #4880ee;
}
</style>
