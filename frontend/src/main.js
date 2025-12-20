import { createApp } from "vue";
import { createPinia } from "pinia";
// Pinia 퍼시스트 플러그인: 세션스토리지에 상태를 유지하도록 설정합니다.
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import App from "./App.vue";
import router from "./router";
import "./styles/tokens.css";

const app = createApp(App);
const pinia = createPinia();
// 세션 스토리지에 상태를 저장하도록 플러그인 등록
pinia.use(piniaPluginPersistedstate)
app.use(pinia);
app.use(router);
app.mount("#app");
