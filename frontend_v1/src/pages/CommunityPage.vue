<script setup>
import { communityPosts } from '../data/communityPosts'
import { useRouter } from 'vue-router'

const router = useRouter()
const openPost = (id) => {
  router.push({ name: 'CommunityPost', params: { id } })
}
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
          <span>제목</span>
          <span>작성자</span>
          <span>작성일</span>
        </div>
        <div
          v-for="post in communityPosts"
          :key="post.id"
          class="community-table__row"
          @click="openPost(post.id)"
        >
          <span>{{ post.id }}</span>
          <span>{{ post.title }}</span>
          <span>{{ post.author }}</span>
          <span>{{ post.date }}</span>
        </div>
      </div>
      <div class="community-board__pagination">
        <button class="ghost-button">이전</button>
        <button class="primary-button is-active">1</button>
        <button class="ghost-button">2</button>
        <button class="ghost-button">다음</button>
      </div>
    </div>
  </section>
</template>
