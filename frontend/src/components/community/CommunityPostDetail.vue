<template>
    <TopBarNavigation />

    <AppShell title="커뮤니티" subtitle="게시글" footerTheme="brand">
        <div v-if="loading" class="loading">로딩 중...</div>

        <div v-else-if="!post" class="empty">
            게시글을 불러올 수 없어요
        </div>

        <template v-else>
            <!-- 게시글 본문 -->
            <div class="postCard">
                <div class="postTitle">{{ post.title }}</div>

                <div class="postMeta">
                    <span class="author">{{ post.userName }}</span>
                    <span class="dot">·</span>
                    <span>{{ formatDate(post.createdAt) }}</span>
                </div>

                <div class="postContent">{{ post.content }}</div>

                <div class="postActions">
                    <button class="actionBtn">💬 {{ comments.length }}</button>
                </div>
            </div>

            <!-- 댓글 -->
            <div class="commentSection">
                <div class="commentTitle">댓글 {{ comments.length }}</div>

                <div v-for="c in comments" :key="c.id" class="comment">
                    <div class="commentHeader">
                        <span class="commentAuthor">{{ c.userName }}</span>
                        <span class="commentTime">{{ formatDate(c.createdAt) }}</span>
                    </div>
                    <div class="commentBody">{{ c.content }}</div>

                    <button class="deleteBtn" @click="deleteCommentFn(c.id)">삭제</button>
                </div>
            </div>

            <!-- ✅ 댓글 입력 (사이즈/배경 개선 버전만 적용) -->
            <div class="commentInputBar">
                <div class="commentInputInner">
                    <input v-model="newComment" class="commentInput" placeholder="댓글을 입력하세요" />
                    <button class="sendBtn" :disabled="submitting || !newComment.trim()" @click="addComment">
                        {{ submitting ? '등록중' : '등록' }}
                    </button>
                </div>
            </div>
        </template>
    </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import AppShell from '@/layout/AppShell.vue'
import { getPost, getComments, createComment, deleteComment } from '@/api/community.js'

const route = useRoute()
const postId = Number(route.params.id)

const post = ref(null)
const comments = ref([])
const newComment = ref('')
const loading = ref(false)
const submitting = ref(false)

async function loadComments()
{
    comments.value = await getComments(postId)
}

async function loadPost()
{
    loading.value = true
    try {
        post.value = await getPost(postId)
        await loadComments()
    } catch (e) {
        console.error('게시글 로드 실패:', e)
        alert('게시글을 불러올 수 없습니다')
    } finally {
        loading.value = false
    }
}

async function addComment()
{
    if (!newComment.value.trim()) return

    submitting.value = true
    try {
        await createComment(postId, { content: newComment.value })
        newComment.value = ''
        await loadComments()
    } catch (e) {
        console.error('댓글 작성 실패:', e)
        alert('댓글 작성에 실패했습니다')
    } finally {
        submitting.value = false
    }
}

async function deleteCommentFn(commentId)
{
    if (!confirm('댓글을 삭제하시겠습니까?')) return

    try {
        await deleteComment(commentId)
        comments.value = comments.value.filter(c => c.id !== commentId)
    } catch (e) {
        console.error('댓글 삭제 실패:', e)
        alert('댓글 삭제에 실패했습니다')
    }
}

function formatDate(dateString)
{
    if (!dateString) return ''

    const date = new Date(dateString)
    const now = new Date()
    const diff = now - date

    const minutes = Math.floor(diff / 60000)
    const hours = Math.floor(diff / 3600000)
    const days = Math.floor(diff / 86400000)

    if (minutes < 1) return '방금'
    if (minutes < 60) return `${minutes}분 전`
    if (hours < 24) return `${hours}시간 전`
    if (days < 7) return `${days}일 전`

    return date.toLocaleDateString('ko-KR', { month: 'long', day: 'numeric' })
}

onMounted(loadPost)
</script>

<style scoped>
/* Loading & Empty */
.loading,
.empty {
    text-align: center;
    padding: 40px 20px;
    color: var(--muted);
    font-size: 14px;
}

/* 게시글 카드 */
.postCard {
    background: var(--surface);
    border-radius: 14px;
    padding: 16px;
    margin-bottom: 16px;
}

.postTitle {
    font-size: 18px;
    font-weight: 700;
    margin-bottom: 6px;
    word-break: break-word;
}

.postMeta {
    font-size: 12px;
    color: var(--muted);
    margin-bottom: 16px;
}

.dot {
    margin: 0 4px;
}

.postContent {
    font-size: 15px;
    line-height: 1.6;
    white-space: pre-line;
    margin-bottom: 16px;
    color: var(--text);
}

.postActions {
    display: flex;
    gap: 8px;
    padding-top: 12px;
    border-top: 1px solid var(--border);
}

.actionBtn {
    background: transparent;
    border: 1px solid var(--border);
    padding: 6px 12px;
    border-radius: 999px;
    font-size: 13px;
    color: var(--text);
    cursor: pointer;
    transition: all 0.2s ease;
}

.actionBtn:hover {
    background: var(--card);
}

/* 댓글 섹션 */
.commentSection {
    background: var(--surface);
    border-radius: 14px;
    padding: 16px;
    margin-bottom: 12px;
}

.commentTitle {
    font-weight: 700;
    font-size: 15px;
    margin-bottom: 12px;
    color: var(--text);
}

.comment {
    background: var(--card);
    border-radius: 0;
    padding: 12px 0;
    border-bottom: 1px solid var(--border);
    position: relative;
}

.comment:last-child {
    border-bottom: none;
}

.commentHeader {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    color: var(--muted);
    margin-bottom: 6px;
}

.commentAuthor {
    font-weight: 600;
}

.commentBody {
    font-size: 14px;
    line-height: 1.4;
    color: var(--text);
    margin-bottom: 4px;
}

.deleteBtn {
    background: transparent;
    border: none;
    color: var(--muted);
    font-size: 12px;
    padding: 0;
    cursor: pointer;
    text-decoration: underline;
}

.deleteBtn:hover {
    color: var(--text);
}

.commentInputBar {
    background: var(--surface);
    border-radius: 14px;
    border: 1px solid var(--border);
    padding: 12px 16px;
    margin-bottom: 16px;
}

.commentInputInner {
    display: flex;
    gap: 10px;
    align-items: center;
}

.commentInput {
    flex: 1;
    height: 42px;
    border-radius: 999px;
    border: 1px solid var(--border);
    padding: 0 14px;
    font-size: 14px;
    background: #fff;
    color: var(--text);
}

.commentInput:focus {
    outline: none;
    border-color: var(--primary);
    box-shadow: 0 0 0 3px rgba(0, 0, 0, 0.06);
}

.sendBtn {
    height: 42px;
    padding: 0 16px;
    border-radius: 999px;
    border: 1px solid var(--primary);
    background: var(--primary);
    color: #fff;
    font-weight: 800;
    font-size: 14px;
    cursor: pointer;
    transition: all 0.15s ease;
    white-space: nowrap;
}

.sendBtn:hover:not(:disabled) {
    transform: translateY(-1px);
}

.sendBtn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}
</style>
