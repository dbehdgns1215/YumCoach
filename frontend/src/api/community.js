import api from "@/lib/api.js";

/**
 * 커뮤니티 게시글 목록 조회
 * @param {string} category 카테고리 (all, free, question, review)
 * @param {number} page 페이지 번호 (1부터 시작)
 * @param {number} size 페이지당 개수
 * @returns {Promise<Object>} { content: [...], totalElements, totalPages, number }
 */
export async function getPosts(category = "all", page = 1, size = 10) {
  const params = { page, size };
  if (category !== "all") {
    params.category = category;
  }
  const { data } = await api.get("/community", { params });

  //   console.log(data);
  return data || { content: [], totalElements: 0, totalPages: 0, number: 0 };
}

/**
 * 게시글 상세 조회
 * @param {number} postId 게시글 ID
 * @returns {Promise<Object>} 게시글 상세 정보
 */
export async function getPost(postId) {
  const { data } = await api.get(`/community/${postId}`);
  return data;
}

/**
 * 게시글 작성
 * @param {Object} payload { category, title, content }
 * @returns {Promise<Object>} 생성된 게시글
 */
export async function createPost(payload) {
  const { data } = await api.post(`/community`, payload);
  return data;
}

/**
 * 게시글 수정
 * @param {number} postId 게시글 ID
 * @param {Object} payload { category, title, content }
 * @returns {Promise<Object>} 수정된 게시글
 */
export async function updatePost(postId, payload) {
  const { data } = await api.put(`/community/${postId}`, payload);
  return data;
}

/**
 * 게시글 삭제
 * @param {number} postId 게시글 ID
 * @returns {Promise<void>}
 */
export async function deletePost(postId) {
  await api.delete(`/community/${postId}`);
}

/**
 * 댓글 목록 조회
 * @param {number} postId 게시글 ID
 * @returns {Promise<Array>} 댓글 목록
 */
export async function getComments(postId) {
  const { data } = await api.get(`/community/${postId}/comments`);
  return Array.isArray(data) ? data : data.comments || [];
}

/**
 * 댓글 작성
 * @param {number} postId 게시글 ID
 * @param {Object} payload { content }
 * @returns {Promise<Object>} 생성된 댓글
 */
export async function createComment(postId, payload) {
  const { data } = await api.post(`/community/${postId}/comments`, payload);
  return data;
}

/**
 * 댓글 삭제
 * @param {number} commentId 댓글 ID
 * @returns {Promise<void>}
 */
export async function deleteComment(commentId) {
  await api.delete(`/community/comments/${commentId}`);
}
