// API Base URL
const API_BASE_URL = 'http://localhost:8080/api';

// axios 대신 fetch 사용
class ApiService {
  constructor(baseURL) {
    this.baseURL = baseURL;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const token = localStorage.getItem('accessToken');

    const config = {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    };

    if (token && !options.skipAuth) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }

    try {
      const response = await fetch(url, config);
      
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || '요청 처리 중 오류가 발생했습니다.');
      }

      return await response.json();
    } catch (error) {
      console.error('API Request Error:', error);
      throw error;
    }
  }

  get(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'GET' });
  }

  post(endpoint, data, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  put(endpoint, data, options = {}) {
    return this.request(endpoint, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data),
    });
  }

  delete(endpoint, options = {}) {
    return this.request(endpoint, { ...options, method: 'DELETE' });
  }
}

const api = new ApiService(API_BASE_URL);

// User API
export const userApi = {
  // 회원가입
  signup: (data) => api.post('/user/signup', data, { skipAuth: true }),
  
  // 로그인
  signin: (data) => api.post('/user/signin', data, { skipAuth: true }),
  
  // 로그아웃
  signout: () => api.post('/user/signout'),
  
  // 토큰 갱신
  refreshToken: (refreshToken) => api.post('/user/refresh', { refreshToken }, { skipAuth: true }),
  
  // 내 정보 조회
  getMyInfo: () => api.get('/user/me'),
  
  // 건강정보 조회
  getUserHealth: () => api.get('/user/health'),
  
  // 건강정보 수정
  updateUserHealth: (data) => api.put('/user/health', data),
};

export default api;
