// axios 인스턴스와 인증 인터셉터
// - 모든 요청에는 Pinia에 저장된 AccessToken(AT)을 Authorization 헤더로 붙입니다.
// - 기본적으로 쿠키(withCredentials)는 보내지 않습니다. (RT는 자동 전송하지 않음)
// - 응답에서 401이 오면 한 번만 refresh()를 호출해 AT를 갱신한 뒤 원래 요청을 재시도합니다.

import axios from "axios";
import { useAuthStore } from "../stores/auth";

// 중앙에서 baseUrl을 관리합니다.
// .env의 VITE_API_BASE_URL을 우선 사용하고, 없으면 '/api'로 대체합니다.
export const baseUrl = import.meta.env.VITE_API_BASE_URL || "/api";

// axios 인스턴스 생성: 기본적으로 withCredentials를 false로 둡니다.
const api = axios.create({
  baseURL: baseUrl,
  withCredentials: false,
});

// 요청 인터셉터: AT가 있으면 Authorization 헤더에 추가
api.interceptors.request.use(
  (config) => {
    try {
      const auth = useAuthStore();
      if (auth?.accessToken) {
        config.headers = config.headers || {};
        config.headers["Authorization"] = `Bearer ${auth.accessToken}`;
      }
    } catch (e) {
      // Pinia가 아직 준비되지 않았거나 다른 문제일 경우 무시
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 응답 인터셉터: 401 -> refresh() -> 원래 요청 재시도
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    const status = error?.response?.status;

    // 401이면서 아직 재시도하지 않은 요청이면 refresh 시도
    if (status === 401 && originalRequest && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const auth = useAuthStore();
        // refresh는 RT 쿠키를 보내야 하므로 직접 axios로 호출 (withCredentials: true)
        await auth.refresh();

        // 갱신된 accessToken을 헤더에 붙여 재요청
        if (auth.accessToken) {
          originalRequest.headers = originalRequest.headers || {};
          originalRequest.headers[
            "Authorization"
          ] = `Bearer ${auth.accessToken}`;
          return api(originalRequest);
        }
      } catch (refreshErr) {
        // refresh 실패하면 로그아웃 처리
        try {
          const auth = useAuthStore();
          await auth.logout();
        } catch (e) {}
        return Promise.reject(refreshErr);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
export { api };
