<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { communityPosts } from '../data/communityPosts'

const route = useRoute()
const postId = Number(route.params.id)
const post = communityPosts.find((item) => item.id === postId)

const likeCount = ref(post ? post.likes : 0)
const commentDraft = ref('')
const comments = ref(post ? [...post.comments] : [])

const addComment = () => {
  if (!commentDraft.value.trim()) return
  comments.value.push({
    id: Date.now(),
    author: '현재 사용자',
    text: commentDraft.value.trim(),
    date: new Date().toISOString().split('T')[0],
  })
  commentDraft.value = ''
}

const reportPost = () => {
  alert('신고 접수가 완료되었습니다. 운영자가 확인합니다.')
}
</script>

<template>
  <section v-if="post" class="page community-post">
    <div class="section card">
      <header class="post-header">
        <div>
          <h2>{{ post.title }}</h2>
          <p class="muted">{{ post.author }} · {{ post.date }}</p>
        </div>
        <div class="post-actions">
          <button type="button" class="ghost-button" @click="reportPost">신고</button>
          <button type="button" class="primary-button" @click="likeCount++">좋아요 {{ likeCount }}</button>
        </div>
      </header>
      <p class="post-body">{{ post.body }}</p>
    </div>
    <div class="section card">
      <h3>댓글</h3>
      <ul class="comment-list">
        <li v-for="comment in comments" :key="comment.id">
          <strong>{{ comment.author }}</strong>
          <span class="muted"> · {{ comment.date }}</span>
          <p>{{ comment.text }}</p>
        </li>
      </ul>
      <form class="comment-form" @submit.prevent="addComment">
        <textarea v-model="commentDraft" rows="3" placeholder="댓글을 입력하세요"></textarea>
        <button type="submit" class="primary-button">댓글 쓰기</button>
      </form>
    </div>
  </section>
  <section v-else class="page">
    <div class="section card">
      <p>게시물을 찾을 수 없습니다.</p>
    </div>
  </section>
</template>
