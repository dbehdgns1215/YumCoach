import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import LoginPage from '../pages/LoginPage.vue'
import SignupPage from '../pages/SignupPage.vue'
import DietRecordPage from '../pages/DietRecordPage.vue'
import ChallengePage from '../pages/ChallengePage.vue'
import HealthInfoPage from '../pages/HealthInfoPage.vue'
import CommunityPage from '../pages/CommunityPage.vue'
import CommunityNewPostPage from '../pages/CommunityNewPostPage.vue'
import CommunityPostPage from '../pages/CommunityPostPage.vue'

const routes = [
  { path: '/', name: 'Home', component: HomePage },
  { path: '/login', name: 'Login', component: LoginPage },
  { path: '/signup', name: 'Signup', component: SignupPage },
  { path: '/diet-record', name: 'DietRecord', component: DietRecordPage },
  { path: '/challenge', name: 'Challenge', component: ChallengePage },
  { path: '/health-info', name: 'HealthInfo', component: HealthInfoPage },
  { path: '/community', name: 'Community', component: CommunityPage },
  { path: '/community/new', name: 'CommunityNew', component: CommunityNewPostPage },
  { path: '/community/:id', name: 'CommunityPost', component: CommunityPostPage, props: true },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
