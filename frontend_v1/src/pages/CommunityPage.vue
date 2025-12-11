<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { communityApi } from '../services/api'

const router = useRouter()
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)

const loadPosts = async (page = 1) => {
  loading.value = true
  try {
    const response = await communityApi.getPosts(page, 10)
    posts.value = response.posts
    currentPage.value = response.currentPage
    totalPages.value = response.totalPages
  } catch (error) {
    console.error('게시글 목록 조회 오류:', error)
  } finally {
    loading.value = false
  }
}

const openPost = (id) => {
  router.push({ name: 'CommunityPost', params: { id } })
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <section class="page community">
    <div class="section card community-board">
      <div class="community-board__header">
        <div>
          <h2>게시판</h2>
          <p class="muted">챌린지 경험, 식단, 팁을 공유하고 서로 응원해요.</p>
        </div>
        <div class="community-board__header-actions">
          <router-link to="/community/new" class="primary-button">글쓰기</router-link>
        </div>
      </div>
      <div class="community-board__controls">
        <select>
          <option value="title">제목</option>
          <option value="author">작성자</option>
        </select>
        <input placeholder="검색어" />
        <button class="ghost-button">검색</button>
      </div>
      <div class="community-board__table">
        <div class="community-table__head">
          <span>번호</span>
          <span>카테고리</span>
          <span>제목</span>
          <span>작성자</span>
          <span>작성일</span>
        </div>
        <p v-if="loading" style="text-align: center; padding: 20px; margin: 0;">로딩 중...</p>
        <p v-else-if="posts.length === 0" style="text-align: center; padding: 20px; margin: 0;">게시글이 없습니다.</p>
        <div
          v-else
          v-for="post in posts"
          :key="post.id"
          class="community-table__row"
          @click="openPost(post.id)"
        >
          <span>{{ post.id }}</span>
          <span><span class="category-badge">{{ post.category || '경험' }}</span></span>
          <span>{{ post.title }}</span>
          <span>{{ post.userName }}</span>
          <span>{{ new Date(post.createdAt).toLocaleDateString() }}</span>
        </div>
      </div>
      <div v-if="totalPages > 1" class="community-board__pagination">
        <button 
          class="ghost-button" 
          :disabled="currentPage === 1"
          @click="loadPosts(currentPage - 1)"
        >
          이전
        </button>
        <button 
          v-for="page in totalPages" 
          :key="page"
          :class="page === currentPage ? 'primary-button is-active' : 'ghost-button'"
          @click="loadPosts(page)"
        >
          {{ page }}
        </button>
        <button 
          class="ghost-button" 
          :disabled="currentPage === totalPages"
          @click="loadPosts(currentPage + 1)"
        >
          다음
        </button>
      </div>
    </div>
  </section>
</template>
