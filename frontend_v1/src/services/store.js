import { reactive } from 'vue';

/**
 * 인증 상태 관리 스토어
 * 
 * JWT 토큰은 HttpOnly Cookie에 저장되므로:
 * - JavaScript에서 토큰에 직접 접근 불가 (XSS 공격 방어)
 * - 서버 API 호출 시 자동으로 Cookie 전송
 * - localStorage 사용 안 함 (보안 강화)
 * 
 * 인증 상태 확인 방법:
 * - 서버에 /api/user/me 요청하여 사용자 정보 확인
 * - 성공 시: 로그인 상태
 * - 실패 시: 비로그인 상태
 */
export const authStore = reactive({
  user: null,
  
  /**
   * 로그인 여부 확인
   * Cookie는 JavaScript에서 읽을 수 없으므로 user 존재 여부로 판단
   */
  isAuthenticated() {
    return !!this.user;
  },
  
  /**
   * 사용자 정보 설정
   * 로그인 성공 시 호출
   */
  setUser(user) {
    this.user = user;
  },
  
  /**
   * 로그아웃
   * 사용자 정보만 클리어 (Cookie는 서버에서 삭제)
   */
  clearAuth() {
    this.user = null;
  },
});
