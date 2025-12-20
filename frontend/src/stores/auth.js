// 인증 상태를 관리하는 Pinia 스토어
// - AccessToken은 메모리(스토어)에 저장하고, PersistedState Plugin을 통해 Session Storage에 보관합니다.
// - RefreshToken(RT)은 HttpOnly 쿠키로 서버가 관리하므로 클라이언트에서 직접 저장하지 않습니다.
import { defineStore } from 'pinia'
import axios from 'axios'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    // accessToken과 user는 PersistedState Plugin으로 복원됩니다.
    accessToken: null,
    user: null,
    isAuthenticated: false,
    loading: false,
    isRefreshing: false,
    _refreshPromise: null,
  }),
  actions: {
    setAccessToken(token) {
      // accessToken을 스토어에 설정합니다. PersistedState Plugin이 세션스토리지에 동기화합니다.
      this.accessToken = token
      this.isAuthenticated = !!token
    },

    async login(credentials) {
      this.loading = true
      try {
        const res = await axios.post('/api/user/signin', credentials, { withCredentials: true })
        const data = res.data || {}
        if (data.accessToken) this.setAccessToken(data.accessToken)
        if (data.user) {
          this.user = data.user
        }
        return data
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await axios.post('/api/user/logout', null, { withCredentials: true })
      } catch (e) {
        // 로그아웃 시 네트워크 오류는 무시
      } finally {
        this.accessToken = null
        this.user = null
        this.isAuthenticated = false
      }
    },

    async refresh() {
      if (this._refreshPromise) return this._refreshPromise
      this.isRefreshing = true
      this._refreshPromise = (async () => {
        try {
          const res = await axios.post('/api/user/refresh', null, { withCredentials: true })
          const data = res.data || {}
          if (data.accessToken) {
            this.setAccessToken(data.accessToken)
            return data.accessToken
          }
          throw new Error('refresh에서 accessToken을 반환하지 않음')
        } catch (err) {
          this.accessToken = null
          this.user = null
          this.isAuthenticated = false
          throw err
        } finally {
          this.isRefreshing = false
          this._refreshPromise = null
        }
      })()
      return this._refreshPromise
    },

    async checkAuth() {
      this.loading = true
      try {
        // 이미 accessToken이 있으면 /me 호출로 유효성 검증
        if (this.accessToken) {
          try {
            const res = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
            this.user = res.data
            this.isAuthenticated = true
            return true
          } catch (err) {
            const status = err?.response?.status
            if (status === 401) {
              // 401이면 refresh 시도 후 재요청
              await this.refresh()
              const res2 = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
              this.user = res2.data
              this.isAuthenticated = true
              return true
            }
            this.accessToken = null
            this.isAuthenticated = false
            return false
          }
        }

        // accessToken이 없으면 refresh로 토큰 받기 시도 (RT 쿠키 자동 전송)
        try {
          await this.refresh()
          const res = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
          this.user = res.data
          this.isAuthenticated = true // 헤더에 자동으로 RT 쿠키가 포함되어 전송됨
          return true
        } catch (err) {
          this.isAuthenticated = false // 헤더에 RT 쿠키 포함하지 않음 (기본값)
          return false
        }
      } finally {
        this.loading = false
      }
    },
  },
  // Pinia persistedstate 플러그인 설정: accessToken과 user를 세션스토리지에 저장
  persist: {
    key: 'auth',
    storage: typeof window !== 'undefined' ? sessionStorage : undefined,
    paths: ['accessToken', 'user'],
  },
})
