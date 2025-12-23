<template>
    <TopBarNavigation />

    <AppShell title="글쓰기" subtitle="커뮤니티에 글을 남겨보세요" footerTheme="brand">
        <!-- 제목 -->
        <div class="section">
            <div class="label">제목</div>
            <input v-model="title" class="titleInput" placeholder="제목을 입력하세요" />
        </div>

        <!-- 본문 -->
        <div class="section">
            <div class="label">내용</div>
            <textarea v-model="content" class="contentInput" placeholder="내용을 입력하세요" rows="8" />
        </div>


        <div class="buttonRow">
            <button class="btn cancel" type="button" @click="cancel" :disabled="submitting">
                취소
            </button>
            <button class="btn submit" type="button" @click="submit"
                :disabled="submitting || !title.trim() || !content.trim()">
                {{ submitting ? '등록중' : '등록' }}
            </button>

        </div>
    </AppShell>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import { createPost } from '@/api/community.js'

const router = useRouter()

const title = ref('')
const content = ref('')
const submitting = ref(false)

async function submit()
{
    if (!title.value.trim() || !content.value.trim()) {
        alert('제목과 내용을 입력해주세요')
        return
    }

    submitting.value = true
    try {
        const payload = {
            category: 'free', // 현재 고정
            title: title.value,
            content: content.value,
        }

        const result = await createPost(payload)

        // 작성 완료 후 상세 페이지로 이동
        router.replace(`/community/${result.id}`)
    } catch (e) {
        console.error('게시글 작성 실패:', e)
        alert('게시글 작성에 실패했습니다')
    } finally {
        submitting.value = false
    }
}

function cancel()
{
    router.back() // 또는 router.push('/community')
}
</script>

<style scoped>
.section {
    margin-bottom: 20px;
}

.label {
    font-size: 13px;
    font-weight: 700;
    margin-bottom: 6px;
}

.titleInput {
    width: 100%;
    border-radius: 10px;
    border: 1px solid var(--border);
    padding: 10px 12px;
    font-size: 15px;
}

.contentInput {
    width: 100%;
    border-radius: 12px;
    border: 1px solid var(--border);
    padding: 12px;
    font-size: 14px;
    line-height: 1.6;
    resize: vertical;
}


.buttonRow {
    display: flex;
    gap: 10px;
    margin-top: 16px;
    background: var(--card);
    padding: 16px;
    border-radius: 14px;
    border: 1px solid var(--border);
}

.btn {
    flex: 1;
    border: none;
    border-radius: 12px;
    padding: 12px 14px;
    font-size: 15px;
    font-weight: 900;
    cursor: pointer;
}

.btn:disabled {
    opacity: 0.5;
    cursor: not-allowed;
}

.btn.cancel {
    background: var(--surface);
    border: 1px solid var(--border);
    color: var(--text);
}

.btn.submit {
    background: var(--primary);
    /* brand보다 한 단계 진한 컬러 */
    color: #fff;
    box-shadow: 0 4px 10px rgba(0, 0, 0, 0.12);
}
</style>
