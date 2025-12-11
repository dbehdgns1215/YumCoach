<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { communityApi } from '../services/api'
import { authStore } from '../services/store'

const route = useRoute()
const router = useRouter()
const postId = Number(route.params.id)

const post = ref(null)
const loading = ref(false)
const errorMessage = ref('')

// 현재 로그인한 사용자가 작성자인지 확인
const isAuthor = computed(() => {
  return post.value && authStore.user && post.value.userId === authStore.user.id
})

const likeCount = ref(0)
const commentDraft = ref('')
const comments = ref([])
const commentLoading = ref(false)

// 게시글 상세 조회
const loadPost = async () => {
  loading.value = true
  errorMessage.value = ''
  try {
    post.value = await communityApi.getPost(postId)
  } catch (error) {
    console.error('게시글 조회 오류:', error)
    errorMessage.value = '게시글을 불러올 수 없습니다.'
  } finally {
    loading.value = false
  }
}

// 댓글 목록 조회
const loadComments = async () => {
  try {
    comments.value = await communityApi.getComments(postId)
  } catch (error) {
    console.error('댓글 조회 오류:', error)
  }
}

// 댓글 작성
const addComment = async () => {
  if (!commentDraft.value.trim()) return
  
  commentLoading.value = true
  try {
    await communityApi.createComment(postId, {
      content: commentDraft.value.trim()
    })
    commentDraft.value = ''
    await loadComments() // 댓글 목록 새로고침
  } catch (error) {
    console.error('댓글 작성 오류:', error)
    alert('댓글 작성 중 오류가 발생했습니다.')
  } finally {
    commentLoading.value = false
  }
}

// 댓글 삭제
const deleteComment = async (commentId) => {
  if (!confirm('댓글을 삭제하시겠습니까?')) return
  
  try {
    await communityApi.deleteComment(commentId)
    await loadComments() // 댓글 목록 새로고침
  } catch (error) {
    console.error('댓글 삭제 오류:', error)
    alert('댓글 삭제 중 오류가 발생했습니다.')
  }
}


// 게시글 삭제
const deletePost = async () => {
  if (!confirm('게시글을 삭제하시겠습니까?')) return
  
  try {
    await communityApi.deletePost(postId)
    alert('게시글이 삭제되었습니다.')
    router.push({ name: 'Community' })
  } catch (error) {
    console.error('게시글 삭제 오류:', error)
    alert('게시글 삭제 중 오류가 발생했습니다.')
  }
}

// 게시글 수정 페이지로 이동
const editPost = () => {
  router.push({ name: 'CommunityEdit', params: { id: postId } })
}

const reportPost = () => {
  alert('신고 접수가 완료되었습니다. 운영자가 확인합니다.')
}

onMounted(async () => {
  await loadPost()
  await loadComments()
})
</script>

<template>
  <section class="page community-post">
    <div v-if="loading" class="section card">
      <p>로딩 중...</p>
    </div>
    <div v-else-if="errorMessage" class="section card">
      <p class="error-message">{{ errorMessage }}</p>
      <button class="primary-button" @click="router.push({ name: 'Community' })">목록으로</button>
    </div>
    <template v-else-if="post">
      <div class="section card">
        <header class="post-header">
          <div>
            <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 8px;">
              <span class="category-badge">{{ post.category || '경험' }}</span>
              <h2 style="margin: 0;">{{ post.title }}</h2>
            </div>
            <p class="muted">{{ post.userName }} · {{ new Date(post.createdAt).toLocaleDateString() }}</p>
          </div>
          <div class="post-actions">
            <button v-if="isAuthor" type="button" class="ghost-button" @click="editPost">수정</button>
            <button v-if="isAuthor" type="button" class="ghost-button" @click="deletePost">삭제</button>
            <button type="button" class="ghost-button" @click="reportPost">신고</button>
          </div>
        </header>
        <p class="post-body">{{ post.content }}</p>
      </div>
      <div class="section card">
        <h3>댓글 ({{ comments.length }})</h3>
        <ul class="comment-list">
          <li v-for="comment in comments" :key="comment.id">
            <div style="display: flex; justify-content: space-between; align-items: center;">
              <div>
                <strong>{{ comment.userName }}</strong>
                <span class="muted"> · {{ new Date(comment.createdAt).toLocaleDateString() }}</span>
              </div>
              <button 
                v-if="authStore.user && comment.userId === authStore.user.id"
                type="button" 
                class="ghost-button" 
                style="padding: 4px 8px; font-size: 12px;"
                @click="deleteComment(comment.id)"
              >
                삭제
              </button>
            </div>
            <p>{{ comment.content }}</p>
          </li>
        </ul>
        <form class="comment-form" @submit.prevent="addComment">
          <textarea v-model="commentDraft" rows="3" placeholder="댓글을 입력하세요" :disabled="commentLoading"></textarea>
          <button type="submit" class="primary-button" :disabled="commentLoading">
            {{ commentLoading ? '작성 중...' : '댓글 쓰기' }}
          </button>
        </form>
      </div>
    </template>
  </section>
</template>
