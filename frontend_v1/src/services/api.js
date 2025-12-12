// API Base URL - 환경변수 사용
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8282/api';

// axios 대신 fetch 사용
class ApiService {
  constructor(baseURL) {
    this.baseURL = baseURL;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;

    const config = {
      ...options,
      credentials: 'include', // Cookie 자동 전송 (중요!)
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    };

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
  signup: (data) => api.post('/user/signup', data),
  
  // 로그인 (Cookie로 토큰 수신)
  signin: (data) => api.post('/user/signin', data),
  
  // 로그아웃 (Cookie 삭제)
  signout: () => api.post('/user/signout'),
  
  // 토큰 갱신 (Cookie에서 자동으로 Refresh Token 전송)
  refreshToken: () => api.post('/user/refresh'),
  
  // 내 정보 조회 (Cookie에서 자동으로 Access Token 전송)
  getMyInfo: () => api.get('/user/me'),
  
  // 건강정보 조회 (Cookie에서 자동으로 Access Token 전송)
  getUserHealth: () => api.get('/user/health'),
  
  // 건강정보 수정 (Cookie에서 자동으로 Access Token 전송)
  updateUserHealth: (data) => api.put('/user/health', data),
};

// Community API
export const communityApi = {
  // 게시글 목록 조회
  getPosts: (page = 1, size = 10) => api.get(`/community?page=${page}&size=${size}`),
  
  // 게시글 상세 조회
  getPost: (id) => api.get(`/community/${id}`),
  
  // 게시글 작성
  createPost: (data) => api.post('/community', data),
  
  // 게시글 수정
  updatePost: (id, data) => api.put(`/community/${id}`, data),
  
  // 게시글 삭제
  deletePost: (id) => api.delete(`/community/${id}`),
  
  // 댓글 목록 조회
  getComments: (postId) => api.get(`/community/${postId}/comments`),
  
  // 댓글 작성
  createComment: (postId, data) => api.post(`/community/${postId}/comments`, data),
  
  // 댓글 삭제
  deleteComment: (commentId) => api.delete(`/community/comments/${commentId}`),
};

export default api;
