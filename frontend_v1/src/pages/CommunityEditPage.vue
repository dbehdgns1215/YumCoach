<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi } from '../services/api'

const route = useRoute()
const router = useRouter()
const postId = Number(route.params.id)

const form = ref({
  title: '',
  category: '경험',
  body: '',
})

const loading = ref(false)
const errorMessage = ref('')

// 기존 게시글 불러오기
const loadPost = async () => {
  loading.value = true
  try {
    const post = await communityApi.getPost(postId)
    form.value.title = post.title
    form.value.category = post.category || '경험'
    form.value.body = post.content
  } catch (error) {
    console.error('게시글 조회 오류:', error)
    errorMessage.value = '게시글을 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

// 게시글 수정
const submitPost = async () => {
  errorMessage.value = ''
  loading.value = true
  
  try {
    await communityApi.updatePost(postId, {
      title: form.value.title,
      content: form.value.body,
      category: form.value.category
    })
    
    alert(`게시물이 수정되었습니다.`)
    router.push({ name: 'CommunityPost', params: { id: postId } })
  } catch (error) {
    console.error('게시글 수정 오류:', error)
    errorMessage.value = error.message || '게시글 수정 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadPost()
})
</script>

<template>
  <section class="page community-new">
    <div class="section card community-new__panel">
      <div class="community-new__header">
        <h2>게시글 수정</h2>
      </div>
      <p class="muted">게시글을 수정하세요.</p>
      <form class="community-new__form" @submit.prevent="submitPost">
        <label>
          <span>제목</span>
          <input v-model="form.title" placeholder="제목을 입력하세요" required />
        </label>
        <label>
          <span>카테고리</span>
          <select v-model="form.category">
            <option value="경험">경험</option>
            <option value="식단">식단</option>
            <option value="팁">팁</option>
          </select>
        </label>
        <label>
          <span>내용</span>
          <textarea v-model="form.body" rows="8" placeholder="내용을 입력하세요" required></textarea>
        </label>
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <div class="community-new__actions">
          <button type="button" class="ghost-button" @click="router.back()">취소</button>
          <button type="submit" class="primary-button" :disabled="loading">
            {{ loading ? '수정 중...' : '수정' }}
          </button>
        </div>
      </form>
    </div>
  </section>
</template>
