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
      // 전역 axios 기본 Authorization 헤더도 설정하여
      // `api` 인스턴스가 아닌 직접 axios를 쓰는 요청도 인증될 수 있게 합니다.
      try {
        if (token) {
          axios.defaults.headers = axios.defaults.headers || {}
          axios.defaults.headers.common = axios.defaults.headers.common || {}
          axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
        } else {
          try { delete axios.defaults.headers.common['Authorization'] } catch(e) {}
        }
      } catch (e) {}
      // 디버그: 토큰 설정 시 로그 출력
      try { console.debug('[auth] setAccessToken', { token, isAuthenticated: this.isAuthenticated }) } catch(e) {}
    },

    async login(credentials) {
      this.loading = true
      try {
        const res = await axios.post('/api/user/signin', credentials, { withCredentials: true })
        const data = res.data || {}
        // 디버그: 로그인 응답 확인
        try { console.debug('[auth] login response', data) } catch(e) {}
        // 가능한 여러 필드명을 허용하여 accessToken을 추출
        const token = data.accessToken || data.token || data.jwt || null
        if (token) {
          this.setAccessToken(token)
        }

        if (data.user) {
          this.user = data.user
        }

        // 만약 토큰이 없고 user 정보도 없다면, 서버가 쿠키 기반 세션을 사용하고 있을 수 있으므로
        // /me 엔드포인트로 사용자 정보를 가져와 상태를 채워봅니다.
        if (!token && !this.user) {
          try {
            const me = await axios.get('/api/user/me', { withCredentials: true })
            this.user = me.data
            this.isAuthenticated = true
            try { console.debug('[auth] login: populated user via /me after signin') } catch(e) {}
          } catch (e) {
            // /me 실패하면 로그인은 성공적이지만 토큰/유저 정보는 없음
            try { console.debug('[auth] login: /me after signin failed') } catch(e) {}
          }
        }
        return data
      } finally {
        this.loading = false
      }
    },

    async logout() {
      try {
        await axios.post('/api/user/signout', null, { withCredentials: true })
      } catch (e) {
        // 로그아웃 시 네트워크 오류는 무시
      } finally {
          this.accessToken = null
          if (data.user) {
            this.user = data.user
            try { await this.fetchUserHealth() } catch(e) { console.debug('[auth] fetchUserHealth after login failed') }
          }
          try {
            if (typeof sessionStorage !== 'undefined') sessionStorage.removeItem('auth')
          } catch (e) {}

          // axios 전역 Authorization 헤더가 설정되어 있을 수 있으니 제거
          try { delete axios.defaults.headers.common['Authorization'] } catch (e) {}

          // 다른 탭/창에 로그아웃을 알리기 위해 localStorage 이벤트 전파
          try { if (typeof localStorage !== 'undefined') localStorage.setItem('logout', Date.now().toString()) } catch (e) {}
      }
    },

    async refresh() {
      if (this._refreshPromise) return this._refreshPromise
      this.isRefreshing = true
      this._refreshPromise = (async () => {
        try {
          const res = await axios.post('/api/user/refresh', null, { withCredentials: true })
          const data = res.data || {}
          // 우선 accessToken을 기대하지만, 없는 경우도 처리
          const token = data.accessToken || data.token || data.jwt || null
          if (token) {
            this.setAccessToken(token)
            return token
          }

          // 토큰이 없더라도 서버가 쿠키 기반으로 세션을 유지해줄 수 있음.
          // 이 경우 /me를 호출해 사용자 정보를 받아오면 인증 상태로 간주합니다.
          try {
            const me = await axios.get('/api/user/me', { withCredentials: true })
            this.user = me.data
              try { await this.fetchUserHealth() } catch(e) { console.debug('[auth] fetchUserHealth after refresh failed') }
            this.isAuthenticated = true
            try { console.debug('[auth] refresh: populated user via /me') } catch(e) {}
            return null
          } catch (e) {
            throw new Error('refresh did not return accessToken and /me failed')
          }
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
      try { try { console.debug('[auth] checkAuth start, accessToken:', this.accessToken) } catch(e) {} } catch(e) {}
      try {
        // 이미 accessToken이 있으면 /me 호출로 유효성 검증
        if (this.accessToken) {
          try {
            const res = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
            this.user = res.data
              try { await this.fetchUserHealth() } catch(e) { console.debug('[auth] fetchUserHealth after checkAuth failed') }
            this.isAuthenticated = true
            try { console.debug('[auth] checkAuth success (with existing token)', this.user) } catch(e) {}
            return true
          } catch (err) {
            const status = err?.response?.status
            if (status === 401) {
              // 401이면 refresh 시도 후 재요청
              await this.refresh()
              const res2 = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
              this.user = res2.data
              try { await this.fetchUserHealth() } catch(e) { console.debug('[auth] fetchUserHealth after checkAuth refresh failed') }
              this.isAuthenticated = true
              try { console.debug('[auth] checkAuth success (after refresh)', this.user) } catch(e) {}
              return true
            }
            this.accessToken = null
            this.isAuthenticated = false
            try { console.debug('[auth] checkAuth failed, invalid token') } catch(e) {}
            return false
          }
        }

        // accessToken이 없으면 refresh로 토큰 받기 시도 (RT 쿠키 자동 전송)
        try {
          try { console.debug('[auth] no accessToken, attempting refresh') } catch(e) {}
          await this.refresh()
          const res = await axios.get('/api/user/me', { headers: { Authorization: `Bearer ${this.accessToken}` } })
          this.user = res.data
              try { await this.fetchUserHealth() } catch(e) { console.debug('[auth] fetchUserHealth after refresh succeeded but health fetch failed') }
          this.isAuthenticated = true // 헤더에 자동으로 RT 쿠키가 포함되어 전송됨
          try { console.debug('[auth] refresh succeeded, user:', this.user) } catch(e) {}
          return true
        } catch (err) {
          this.isAuthenticated = false // 헤더에 RT 쿠키 포함하지 않음 (기본값)
          try { console.debug('[auth] refresh failed') } catch(e) {}
          return false
        }
      } finally {
        this.loading = false
      }
    },
    
    // helper to fetch user health and merge into `this.user`
    async fetchUserHealth() {
      try {
        // use Authorization header if accessToken exists, otherwise rely on withCredentials cookie
        const cfg = this.accessToken ? { headers: { Authorization: `Bearer ${this.accessToken}` } } : { withCredentials: true }
        const res = await axios.get('/api/user/health', cfg)
        const health = res.data || {}
        if (!this.user) this.user = {}
        // merge common fields used by frontend
        if (health.height != null) this.user.height = health.height
        if (health.weight != null) this.user.weight = health.weight
        // booleans
        if (health.diabetes != null) this.user.diabetes = health.diabetes
        if (health.highBloodPressure != null) this.user.highBloodPressure = health.highBloodPressure
        if (health.hyperlipidemia != null) this.user.hyperlipidemia = health.hyperlipidemia
        if (health.kidneyDisease != null) this.user.kidneyDisease = health.kidneyDisease
        if (health.activityLevel != null) this.user.activityLevel = health.activityLevel
        try { console.debug('[auth] fetchUserHealth merged', this.user) } catch(e) {}
        return health
      } catch (e) {
        try { console.debug('[auth] fetchUserHealth error', e?.response?.status) } catch(e) {}
        throw e
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
