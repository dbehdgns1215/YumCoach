import { createRouter, createWebHistory } from 'vue-router'
import { authStore } from '../services/store'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import SignupPage from '../pages/SignupPage.vue'
import DietRecordPage from '../pages/DietRecordPage.vue'
import ChallengePage from '../pages/ChallengePage.vue'
import HealthInfoPage from '../pages/HealthInfoPage.vue'
import CommunityPage from '../pages/CommunityPage.vue'
import CommunityNewPostPage from '../pages/CommunityNewPostPage.vue'
import CommunityPostPage from '../pages/CommunityPostPage.vue'
import GuestHome from '../pages/GuestHome.vue'

const routes = [
  { path: '/', name: 'Home', component: HomePage, meta: { requiresAuth: true } },
  { path: '/guest', name: 'Guest', component: GuestHome },
  { path: '/login', name: 'Login', component: LoginPage },
  { path: '/signup', name: 'Signup', component: SignupPage },
  { path: '/diet-record', name: 'DietRecord', component: DietRecordPage, meta: { requiresAuth: true } },
  { path: '/challenge', name: 'Challenge', component: ChallengePage, meta: { requiresAuth: true } },
  { path: '/health-info', name: 'HealthInfo', component: HealthInfoPage, meta: { requiresAuth: true } },
  { path: '/community', name: 'Community', component: CommunityPage, meta: { requiresAuth: true } },
  { path: '/community/new', name: 'CommunityNew', component: CommunityNewPostPage, meta: { requiresAuth: true } },
  { path: '/community/:id', name: 'CommunityPost', component: CommunityPostPage, props: true, meta: { requiresAuth: true } },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 네비게이션 가드
router.beforeEach(async (to, from, next) => {
  // 사용자 정보가 없으면 Cookie로 복구 시도
  if (!authStore.user) {
    try {
      const { userApi } = await import('../services/api')
      const user = await userApi.getMyInfo()
      authStore.setUser(user)
    } catch (error) {
      // Access Token이 만료된 경우 Refresh Token으로 재발급 시도
      try {
        const { userApi } = await import('../services/api')
        await userApi.refreshToken()
        // 재발급 성공 후 다시 사용자 정보 조회
        const user = await userApi.getMyInfo()
        authStore.setUser(user)
      } catch (refreshError) {
        // Refresh Token도 만료됨 - 로그인 필요
        console.log('토큰 갱신 실패:', refreshError.message)
      }
    }
  }
  
  const isAuthenticated = authStore.isAuthenticated()
  
  // 인증이 필요한 페이지
  if (to.meta.requiresAuth && !isAuthenticated) {
    next('/login')
  }
  // 로그인/회원가입 페이지는 인증된 사용자가 접근하면 홈으로
  else if ((to.path === '/login' || to.path === '/signup') && isAuthenticated) {
    next('/')
  }
  else {
    next()
  }
})

export default router
