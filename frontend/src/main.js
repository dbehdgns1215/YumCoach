import { createApp } from "vue";
import { createPinia } from "pinia";
// Pinia 퍼시스트 플러그인: 세션스토리지에 상태를 유지하도록 설정합니다.
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from "./App.vue";
import router from "./router";
import "./styles/tokens.css";
import { useAuthStore } from './stores/auth'	

const app = createApp(App);
const pinia = createPinia();
// 세션 스토리지에 상태를 저장하도록 플러그인 등록
pinia.use(piniaPluginPersistedstate)
app.use(pinia);

// 앱 마운트 전에 인증 상태를 초기화합니다 (차단 방식)
(async () => {
	try {
		const auth = useAuthStore()
		await auth.checkAuth()
	} catch (e) {
		// 초기 인증 검사 실패 시에도 앱은 계속 마운트합니다
		console.warn('auth.checkAuth() failed:', e)
	}

	app.use(router);
	app.mount("#app");
})()

window.addEventListener('storage', (e) => {
  if (e.key === 'logout') {
    const auth = useAuthStore()
    auth.accessToken = null
    auth.user = null
    auth.isAuthenticated = false
    try { sessionStorage.removeItem('auth') } catch (err) {}
    router.push('/landing').catch(()=>{})
  }
})