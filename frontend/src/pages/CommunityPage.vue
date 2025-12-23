<template>
    <TopBarNavigation />

    <AppShell title="커뮤니티" subtitle="함께 이야기 나눠요" footerTheme="brand" @primary="onWrite">
        <!-- 게시글 리스트 -->
        <div class="postListContainer">
            <div v-if="loading" class="loading">로딩 중...</div>

            <div v-else-if="posts.length === 0" class="empty">
                아직 게시글이 없어요
            </div>

            <div v-else class="postList">
                <div v-for="post in posts" :key="post.id" class="postCard" @click="openPost(post.id)">
                    <div class="postHeader">
                        <div class="title">{{ post.title }}</div>
                        <div class="comments">💬 {{ post.commentCount || 0 }}</div>
                    </div>

                    <div class="contentPreview">
                        {{ preview(post.content) }}
                    </div>

                    <div class="postMeta">
                        <span class="author">{{ post.userName }}</span>
                        <span class="dot">·</span>
                        <span>{{ formatDate(post.createdAt) }}</span>
                    </div>
                </div>
            </div>

            <!-- 페이지네이션 (서버 페이징 기준) -->
            <div v-if="totalPages > 1" class="pagination">
                <button @click="prevPage" :disabled="currentPage === 1" class="paginationBtn">
                    ‹ 이전
                </button>

                <div class="pageInfo">
                    {{ currentPage }} / {{ totalPages }}
                </div>

                <button @click="nextPage" :disabled="currentPage === totalPages" class="paginationBtn">
                    다음 ›
                </button>
            </div>
        </div>

        <!-- 글쓰기 버튼 -->
        <button class="writeFab" @click="onWrite">
            <span class="pencilIcon">✏️</span>
        </button>
    </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import { getPosts } from '@/api/community.js'

const router = useRouter()

// 서버 페이징 기준 상태
const posts = ref([])
const loading = ref(false)
const currentPage = ref(1)     // ⚠️ 백엔드가 0-based면 getPosts에서 -1 처리
const totalPages = ref(1)
const pageSize = 10

async function loadPosts()
{
    loading.value = true
    try {
        // getPosts(category, page, size) 형태로 호출한다고 가정
        // 카테고리 없으면 'all'로 고정
        const res = await getPosts('all', currentPage.value, pageSize)

        // 백엔드 응답: { posts: [...], totalPages, currentPage, totalCount }
        posts.value = res.posts || []
        totalPages.value = res.totalPages || 1

        // (선택) 백엔드가 currentPage를 내려주면 동기화하고 싶을 때:
        // if (res.currentPage) currentPage.value = res.currentPage
    } catch (e) {
        console.error('게시글 로드 실패:', e)
    } finally {
        loading.value = false
    }
}

function openPost(id)
{
    router.push(`/community/${id}`)
}

function onWrite()
{
    router.push('/community/write')
}

function prevPage()
{
    if (currentPage.value > 1) {
        currentPage.value--
        loadPosts()
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }
}

function nextPage()
{
    if (currentPage.value < totalPages.value) {
        currentPage.value++
        loadPosts()
        window.scrollTo({ top: 0, behavior: 'smooth' })
    }
}

function preview(content)
{
    const text = content || ''
    return text.length > 100 ? `${text.substring(0, 100)}...` : text
}

// 날짜 포맷팅 (상대 시간)
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

onMounted(() =>
{
    loadPosts()
})
</script>

<style scoped>
/* Post List Container */
.postListContainer {
    background: var(--surface);
    border-radius: 14px;
    padding: 16px;
    margin-top: 16px;
}

/* Post list */
.postList {
    display: flex;
    flex-direction: column;
    gap: 0;
    margin-bottom: 20px;
}

.postCard {
    background: var(--card);
    border-radius: 0;
    padding: 14px;
    cursor: pointer;
    box-shadow: none;
    transition: all 0.2s ease;
    border-bottom: 1px solid var(--border);
}

.postCard:hover {
    background: var(--surface);
}

.postHeader {
    display: flex;
    justify-content: space-between;
    align-items: center;
}

.title {
    font-weight: 700;
    font-size: 15px;
    flex: 1;
    word-break: break-word;
}

.comments {
    font-size: 13px;
    color: var(--muted);
    white-space: nowrap;
    margin-left: 8px;
}

.contentPreview {
    font-size: 14px;
    color: var(--text);
    margin: 6px 0 10px;
    line-height: 1.4;
}

.postMeta {
    font-size: 12px;
    color: var(--muted);
}

.dot {
    margin: 0 4px;
}

/* Loading & Empty */
.loading,
.empty {
    text-align: center;
    padding: 40px 20px;
    color: var(--muted);
    font-size: 14px;
}

/* Pagination */
.pagination {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 16px;
    padding: 20px 0 0 0;
    border-top: 1px solid var(--border);
}

.paginationBtn {
    padding: 8px 16px;
    border-radius: 8px;
    border: 1px solid var(--border);
    background: var(--card);
    color: var(--text);
    font-size: 14px;
    cursor: pointer;
    transition: all 0.2s ease;
}

.paginationBtn:hover:not(:disabled) {
    background: var(--brand);
    color: white;
    border-color: var(--brand);
}

.paginationBtn:disabled {
    opacity: 0.4;
    cursor: not-allowed;
}

.pageInfo {
    font-size: 14px;
    font-weight: 600;
    color: var(--text);
}

/* Write FAB */
.writeFab {
    position: fixed;
    bottom: 80px;
    right: 16px;
    width: 60px;
    height: 60px;
    border-radius: 50%;
    border: none;
    background: var(--brand);
    color: white;
    font-size: 24px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    cursor: pointer;
    transition: all 0.3s ease;
    display: flex;
    align-items: center;
    justify-content: center;
}

.writeFab:hover {
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.2);
    transform: scale(1.08);
}

.writeFab:active {
    transform: scale(0.95);
}

.pencilIcon {
    display: inline-block;
    animation: pulse 2s infinite;
}

@keyframes pulse {

    0%,
    100% {
        transform: scale(1);
    }

    50% {
        transform: scale(1.1);
    }
}
</style>
