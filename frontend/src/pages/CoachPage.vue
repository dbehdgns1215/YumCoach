<template>
    <TopBarNavigation />
    <AppShell title="코치" subtitle="질문하면 바로 요약해줘요" footerTheme="brand" @primary="noop">
        <div class="coachChatPage">
            <!-- Left: chat list -->
            <aside class="sidebar">
                <div class="sidebarHeader">
                    <div class="sidebarTitle">채팅 목록</div>

                    <button class="newChatBtn" :disabled="chats.length >= MAX_CHATS" @click="createNewChat"
                        title="새 채팅">
                        + 새 채팅
                    </button>
                </div>

                <div class="chatList">
                    <div v-for="c in chats" :key="c.id" class="chatItem" :class="{ active: c.id === selectedChatId }"
                        role="button" tabindex="0" @click="selectChat(c.id)" @keydown.enter="selectChat(c.id)">

                        <div class="chatItemMain">
                            <div class="chatName">{{ c.title }}</div>
                            <div class="chatMeta">
                                <span class="chatCount">{{ c.messages.length }}개</span>
                                <span class="dot">•</span>
                                <span class="chatTime">{{ formatTime(c.updatedAt) }}</span>
                            </div>
                        </div>

                        <button class="deleteBtn" title="삭제" @click.stop="openDeleteModal(c.id)">
                            ×
                        </button>
                    </div>
                </div>

                <div class="sidebarFooter">
                    <div class="hint">
                        최대 <b>{{ MAX_CHATS }}</b>개까지 만들 수 있어요.
                    </div>
                </div>
            </aside>

            <!-- Right: chat room -->
            <section class="chatRoom">
                <header class="roomHeader">
                    <div class="roomTitle">
                        {{ selectedChat?.title ?? "채팅을 선택하세요" }}
                    </div>
                    <div class="roomSubtitle">
                        {{
                            selectedChat
                                ? "유저 메시지는 오른쪽, AI 응답은 왼쪽에 표시돼요."
                                : "왼쪽에서 채팅을 선택하거나 새 채팅을 만들어주세요."
                        }}
                    </div>
                </header>

                <div ref="messagesEl" class="messages">
                    <template v-if="!selectedChat">
                        <div class="emptyState">
                            <div class="emptyCard">
                                <div class="emptyTitle">챗봇을 시작해볼까요?</div>
                                <div class="emptyDesc">
                                    왼쪽에서 <b>새 채팅</b>을 만들거나 기존 채팅을 선택하면 이곳에 대화가 표시돼요.
                                </div>
                                <button class="primaryCta" @click="createNewChat" :disabled="chats.length >= MAX_CHATS">
                                    + 새 채팅 만들기
                                </button>
                            </div>
                        </div>
                    </template>

                    <template v-else>
                        <div v-if="selectedChat.messages.length === 0" class="emptyInChat">
                            <div class="pill">첫 질문을 입력해보세요 🙂</div>
                        </div>

                        <div v-for="m in selectedChat.messages" :key="m.id" class="msgRow"
                            :class="m.role === 'user' ? 'right' : 'left'">
                            <div class="bubble" :class="m.role">
                                <div v-if="m.role === 'ai' && m.detected_hashtag" class="hashtagBadge">
                                    {{ m.detected_hashtag }}
                                </div>

                                <div class="bubbleText">{{ m.content }}</div>
                                <div class="bubbleMeta">{{ formatTime(m.createdAt) }}</div>
                            </div>
                        </div>

                        <div v-if="isLoading" class="loadingWrap">
                            <div class="loadingLabel">AI가 답변을 작성 중이에요…</div>
                            <div class="progress">
                                <div class="bar" />
                            </div>
                        </div>
                    </template>
                </div>

                <footer class="composer">
                    <div class="inputWrap">
                        <textarea v-model="draft" class="input" placeholder="메시지를 입력하세요…" rows="1"
                            :disabled="!selectedChat || isLoading" @compositionstart="onCompositionStart"
                            @compositionend="onCompositionEnd" @keydown="onKeyDown" />
                        <button class="sendBtn" :disabled="!canSend" @click="send">
                            보내기
                        </button>
                    </div>

                    <!-- ✅ 기존 안내문 유지 -->
                    <div class="composerHint">Enter로 전송, Shift+Enter로 줄바꿈</div>
                </footer>
            </section>
        </div>

        <!-- Delete Confirm Modal -->
        <div v-if="deleteModal.open" class="modalOverlay" @click.self="closeDeleteModal">
            <div class="modalCard" role="dialog" aria-modal="true">
                <div class="modalTitle">채팅을 삭제할까요?</div>
                <div class="modalDesc">
                    <b>{{ deleteTargetTitle }}</b>의 대화 내용이 모두 삭제돼요. 이 작업은 되돌릴 수 없어요.
                </div>
                <div class="modalActions">
                    <button class="modalBtn ghost" @click="closeDeleteModal">취소</button>
                    <button class="modalBtn danger" @click="confirmDelete">삭제</button>
                </div>
            </div>
        </div>
    </AppShell>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'

function noop() { }

const STORAGE_KEY = 'yumcoach_chat_state_v4'
const MAX_CHATS = 10

// ✅ 백엔드 설정
const API_BASE_URL = 'http://localhost:8282'
const CHAT_ENDPOINT = '/api/chat'

// ⚠️ 샘플 토큰(필요시 실제 토큰 소스로 교체)
const AUTH_TOKEN =
    'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjcsImlhdCI6MTc2NjYzNjAzOSwiZXhwIjoxNzY2NzIyNDM5fQ.-toxK4GlBygqhjBvVCuimoSjrV3y8e7XrtwGcPYVfb8'

// ✅ 요청 payload에 들어갈 사용자 데이터(앱에서 가져오면 여기만 교체)
const userContext = ref({
    user_id: '42',
    user_profile: {
        height: 175,
        weight: 70,
        goal: 'diet',
    },
    report_data: {
        bmi: 22.9,
        body_fat: 18.3,
    },
})

const messagesEl = ref(null)
const draft = ref('')
const isLoading = ref(false)

// ✅ IME 조합 버그 방지
const isComposing = ref(false)

const chats = ref([])
const selectedChatId = ref('')

// ✅ 삭제 확인 모달
const deleteModal = ref({ open: false, chatId: null })

const selectedChat = computed(() => chats.value.find(c => c.id === selectedChatId.value) || null)
const deleteTargetTitle = computed(() =>
{
    const c = chats.value.find(x => x.id === deleteModal.value.chatId)
    return c?.title ?? '이 채팅'
})

const canSend = computed(() =>
{
    if (!selectedChat.value) return false
    if (isLoading.value) return false
    return draft.value.trim().length > 0
})

function cleanForChat(text)
{
    if (typeof text !== 'string') return ''
    // JSON처럼 보이지 않게 순수 본문만 다듬기
    return text
        .replace(/\r\n/g, '\n')
        .replace(/\\n/g, '\n')
        .replace(/\n{3,}/g, '\n\n')      // 과한 줄바꿈 정리
        .replace(/^\s+|\s+$/g, '')       // 양끝 공백 제거
}


function uid(prefix = 'id')
{
    return `${prefix}_${Math.random().toString(16).slice(2)}_${Date.now().toString(16)}`
}
function nowISO()
{
    return new Date().toISOString()
}
function formatTime(iso)
{
    try {
        const d = new Date(iso)
        const hh = String(d.getHours()).padStart(2, '0')
        const mm = String(d.getMinutes()).padStart(2, '0')
        return `${hh}:${mm}`
    } catch {
        return ''
    }
}

function persist()
{
    localStorage.setItem(
        STORAGE_KEY,
        JSON.stringify({
            chats: chats.value,
            selectedChatId: selectedChatId.value,
        })
    )
}
function restore()
{
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return false
    try {
        const parsed = JSON.parse(raw)
        if (Array.isArray(parsed.chats)) chats.value = parsed.chats
        if (typeof parsed.selectedChatId === 'string') selectedChatId.value = parsed.selectedChatId
        return true
    } catch {
        return false
    }
}

function selectChat(id)
{
    selectedChatId.value = id
    nextTick(scrollToBottom)
    persist()
}

function createNewChat()
{
    if (chats.value.length >= MAX_CHATS) return
    const n = chats.value.length + 1
    const chat = { id: uid('chat'), title: `코치 채팅 ${n}`, updatedAt: nowISO(), messages: [] }
    chats.value.unshift(chat)
    selectedChatId.value = chat.id
    draft.value = ''
    isLoading.value = false
    nextTick(scrollToBottom)
    persist()
}

function bumpChat(chatId)
{
    const idx = chats.value.findIndex(c => c.id === chatId)
    if (idx < 0) return
    const chat = chats.value[idx]
    chat.updatedAt = nowISO()
    chats.value.splice(idx, 1)
    chats.value.unshift(chat)
}

/* ---------------------------
   Delete confirm modal
--------------------------- */
function openDeleteModal(chatId)
{
    deleteModal.value.open = true
    deleteModal.value.chatId = chatId
}
function closeDeleteModal()
{
    deleteModal.value.open = false
    deleteModal.value.chatId = null
}
function confirmDelete()
{
    const id = deleteModal.value.chatId
    if (!id) return
    closeDeleteModal()
    deleteChat(id)
}
function deleteChat(id)
{
    const idx = chats.value.findIndex(c => c.id === id)
    if (idx < 0) return
    const wasSelected = selectedChatId.value === id
    chats.value.splice(idx, 1)
    if (wasSelected) selectedChatId.value = chats.value[0]?.id ?? ''
    persist()
    nextTick(scrollToBottom)
}

/* ---------------------------
   Backend call + robust parsing
--------------------------- */
function normalizeChatResponse(data)
{
    // 문자열이면 그대로
    if (typeof data === 'string') {
        return { text: cleanForChat(data), hashtag: '' }
    }

    // 1차로 후보 텍스트 확보
    let rawText =
        (typeof data?.response === 'string' && data.response) ||
        (typeof data?.reply === 'string' && data.reply) ||
        (typeof data?.message === 'string' && data.message) ||
        ''

    const hashtag =
        (typeof data?.detected_hashtag === 'string' && data.detected_hashtag) ||
        (typeof data?.hashtag === 'string' && data.hashtag) ||
        ''

    // ✅ [추가] rawText 자체가 "JSON 문자열"이면 한 번 더 파싱
    rawText = (rawText ?? '').trim()
    if (rawText.startsWith('{') && rawText.endsWith('}')) {
        try {
            const inner = JSON.parse(rawText)
            // inner = { response: "..." } 형태면 그걸 본문으로 사용
            if (typeof inner?.response === 'string') rawText = inner.response
            else if (typeof inner?.reply === 'string') rawText = inner.reply
        } catch {
            // 파싱 실패하면 그냥 원문 출력(디버깅용)
        }
    }

    return {
        text: cleanForChat(rawText || '응답 형식이 올바르지 않아요.'),
        hashtag,
    }
}



async function callChatAPI(messageText)
{
    const url = `${API_BASE_URL}${CHAT_ENDPOINT}`
    const payload = {
        message: messageText,
        user_id: userContext.value.user_id,
        user_profile: userContext.value.user_profile,
        report_data: userContext.value.report_data,
    }

    const res = await fetch(url, {
        method: 'POST',
        headers: {
            accept: '*/*',
            'Content-Type': 'application/json',
            Authorization: `Bearer ${AUTH_TOKEN}`,
        },
        body: JSON.stringify(payload),
    })

    if (!res.ok) {
        const txt = await res.text().catch(() => '')
        throw new Error(`API 요청 실패 (${res.status}) ${txt}`)
    }

    // ✅ 서버가 JSON이지만 content-type이 애매한 경우 대비
    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) {
        return await res.json()
    } else {
        const text = await res.text()
        try {
            return JSON.parse(text)
        } catch {
            return text
        }
    }
}

/* ---------------------------
   Sending
--------------------------- */
async function send()
{
    if (!canSend.value) return
    const chat = selectedChat.value
    const text = draft.value.trim()
    draft.value = ''

    // user message
    chat.messages.push({ id: uid('m'), role: 'user', content: text, createdAt: nowISO() })
    bumpChat(chat.id)
    persist()
    await nextTick()
    scrollToBottom()

    isLoading.value = true
    persist()

    try {
        const raw = await callChatAPI(text)
        const { text: replyText, hashtag } = normalizeChatResponse(raw)

        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content: replyText,
            detected_hashtag: hashtag,
            createdAt: nowISO(),
        })
        bumpChat(chat.id)
        persist()
        await nextTick()
        scrollToBottom()
    } catch (err) {
        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content:
                `요청 중 문제가 발생했어요.\n` +
                `- 원인: ${err?.message ?? '알 수 없음'}\n\n` +
                `백엔드 서버/토큰/주소를 확인해주세요.`,
            detected_hashtag: '',
            createdAt: nowISO(),
        })
        bumpChat(chat.id)
        persist()
        await nextTick()
        scrollToBottom()
    }
}

function onCompositionStart()
{
    isComposing.value = true
}
function onCompositionEnd()
{
    isComposing.value = false
}
function onKeyDown(e)
{
    if (e.key === 'Enter' && !e.shiftKey) {
        if (e.isComposing || isComposing.value) return
        e.preventDefault()
        send()
    }
}

function scrollToBottom()
{
    const el = messagesEl.value
    if (!el) return
    el.scrollTop = el.scrollHeight
}

onMounted(() =>
{
    const ok = restore()
    if (!ok || chats.value.length === 0) createNewChat()
    if (!selectedChatId.value && chats.value[0]) selectedChatId.value = chats.value[0].id
    nextTick(scrollToBottom)
})

watch(chats, persist, { deep: true })
watch(selectedChatId, () => nextTick(scrollToBottom))
</script>

<style scoped>
/* (스타일은 이전과 동일) */

.coachChatPage {
    display: grid;
    grid-template-columns: 320px 1fr;
    gap: 16px;
    height: calc(100vh - 180px);
    min-height: 560px;
}

/* Sidebar */
.sidebar {
    background: #ffffff;
    border: 1px solid #eef1f6;
    border-radius: 16px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    box-shadow: 0 6px 24px rgba(20, 40, 80, 0.06);
}

.sidebarHeader {
    padding: 14px 14px 10px;
    border-bottom: 1px solid #eef1f6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
}

.sidebarTitle {
    font-weight: 800;
    letter-spacing: -0.2px;
    color: #1f2a44;
}

.newChatBtn {
    border: 1px solid #dbe7ff;
    background: #f2f7ff;
    color: #2563eb;
    font-weight: 700;
    padding: 8px 10px;
    border-radius: 12px;
    cursor: pointer;
    transition: transform 0.06s ease, background 0.15s ease, border-color 0.15s ease;
}

.newChatBtn:hover:not(:disabled) {
    background: #e8f1ff;
    border-color: #cfe0ff;
}

.newChatBtn:active:not(:disabled) {
    transform: translateY(1px);
}

.newChatBtn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.chatList {
    padding: 10px;
    overflow: auto;
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.chatItem {
    width: 100%;
    border: 1px solid #eef1f6;
    background: #ffffff;
    border-radius: 14px;
    padding: 12px 12px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    position: relative;
    transition: background 0.15s ease, border-color 0.15s ease, transform 0.06s ease;
}

.chatItem:hover {
    background: #f7fbff;
    border-color: #dbe7ff;
}

.chatItem:active {
    transform: translateY(1px);
}

.chatItem.active {
    background: #eef5ff;
    border-color: #cfe0ff;
}

.chatItemMain {
    text-align: left;
    min-width: 0;
}

.chatName {
    font-weight: 800;
    color: #1f2a44;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.chatMeta {
    margin-top: 6px;
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 12px;
    color: #6b7280;
}

.dot {
    opacity: 0.6;
}

.chatCount {
    font-weight: 600;
}

.deleteBtn {
    width: 28px;
    height: 28px;
    border-radius: 10px;
    border: 1px solid #e5e7eb;
    background: #ffffff;
    color: #6b7280;
    font-size: 18px;
    line-height: 1;
    cursor: pointer;
    opacity: 0;
    pointer-events: none;
    transition: opacity 0.12s ease, background 0.15s ease, border-color 0.15s ease;
}

.chatItem:hover .deleteBtn {
    opacity: 1;
    pointer-events: auto;
}

.deleteBtn:hover {
    background: #fff1f2;
    border-color: #fecdd3;
    color: #e11d48;
}

.sidebarFooter {
    padding: 12px 14px;
    border-top: 1px solid #eef1f6;
    background: #fbfdff;
}

.hint {
    font-size: 12px;
    color: #6b7280;
}

/* Chat room */
.chatRoom {
    background: #ffffff;
    border: 1px solid #eef1f6;
    border-radius: 16px;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    box-shadow: 0 6px 24px rgba(20, 40, 80, 0.06);
}

.roomHeader {
    padding: 16px 16px 12px;
    border-bottom: 1px solid #eef1f6;
    background: linear-gradient(180deg, #f5f9ff, #ffffff);
}

.roomTitle {
    font-weight: 900;
    letter-spacing: -0.3px;
    color: #1f2a44;
}

.roomSubtitle {
    margin-top: 6px;
    font-size: 13px;
    color: #6b7280;
}

.messages {
    flex: 1;
    overflow: auto;
    padding: 16px;
    background:
        radial-gradient(1200px 400px at 20% -10%, rgba(37, 99, 235, 0.08), transparent 55%),
        radial-gradient(900px 500px at 90% 10%, rgba(99, 102, 241, 0.08), transparent 60%),
        #ffffff;
}

.emptyState {
    height: 100%;
    display: grid;
    place-items: center;
}

.emptyCard {
    width: min(520px, 100%);
    border: 1px solid #e7efff;
    background: #f7fbff;
    border-radius: 18px;
    padding: 18px;
}

.emptyTitle {
    font-weight: 900;
    color: #1f2a44;
    font-size: 18px;
}

.emptyDesc {
    margin-top: 8px;
    color: #475569;
    font-size: 14px;
    line-height: 1.45;
}

.primaryCta {
    margin-top: 14px;
    border: 1px solid #cfe0ff;
    background: #2563eb;
    color: #ffffff;
    font-weight: 800;
    padding: 10px 12px;
    border-radius: 14px;
    cursor: pointer;
}

.primaryCta:disabled {
    opacity: 0.6;
    cursor: not-allowed;
}

.emptyInChat {
    display: flex;
    justify-content: center;
    margin-top: 18px;
}

.pill {
    display: inline-flex;
    padding: 10px 12px;
    border-radius: 999px;
    border: 1px solid #e7efff;
    background: #f7fbff;
    color: #334155;
    font-weight: 700;
}

.msgRow {
    display: flex;
    margin-bottom: 10px;
}

.msgRow.left {
    justify-content: flex-start;
}

.msgRow.right {
    justify-content: flex-end;
}

.bubble {
    max-width: min(680px, 84%);
    border-radius: 16px;
    padding: 10px 12px;
    border: 1px solid #eef1f6;
    background: #ffffff;
    color: #0f172a;
    box-shadow: 0 6px 18px rgba(20, 40, 80, 0.06);
    white-space: pre-wrap;
    word-break: break-word;
    position: relative;
}

.bubble.user {
    background: #2563eb;
    border-color: #2563eb;
    color: #ffffff;
    border-top-right-radius: 8px;
}

.bubble.ai {
    background: #ffffff;
    border-color: #dbe7ff;
    border-top-left-radius: 8px;
}

.bubbleMeta {
    margin-top: 6px;
    font-size: 11px;
    opacity: 0.75;
}

.hashtagBadge {
    display: inline-flex;
    align-items: center;
    font-size: 12px;
    font-weight: 800;
    color: #2563eb;
    background: #eef5ff;
    border: 1px solid #dbe7ff;
    border-radius: 999px;
    padding: 4px 8px;
    margin-bottom: 8px;
}

.loadingWrap {
    margin-top: 12px;
    padding: 12px;
    border-radius: 16px;
    border: 1px solid #e7efff;
    background: #f7fbff;
}

.loadingLabel {
    font-size: 13px;
    color: #334155;
    font-weight: 700;
    margin-bottom: 10px;
}

.progress {
    height: 10px;
    background: #e7efff;
    border-radius: 999px;
    overflow: hidden;
}

.bar {
    height: 100%;
    width: 40%;
    background: #2563eb;
    border-radius: 999px;
    animation: indeterminate 1.1s infinite ease-in-out;
}

@keyframes indeterminate {
    0% {
        transform: translateX(-90%);
    }

    50% {
        transform: translateX(90%);
    }

    100% {
        transform: translateX(-90%);
    }
}

.composer {
    border-top: 1px solid #eef1f6;
    padding: 12px;
    background: #ffffff;
}

.inputWrap {
    display: flex;
    gap: 10px;
    align-items: flex-end;
}

.input {
    width: 100%;
    resize: none;
    border: 1px solid #dbe7ff;
    background: #fbfdff;
    border-radius: 14px;
    padding: 10px 12px;
    font-size: 14px;
    line-height: 1.35;
    outline: none;
    min-height: 44px;
    max-height: 140px;
}

.input:focus {
    border-color: #2563eb;
    box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.sendBtn {
    border: 1px solid #cfe0ff;
    background: #2563eb;
    color: #ffffff;
    font-weight: 900;
    padding: 10px 14px;
    border-radius: 14px;
    cursor: pointer;
    transition: transform 0.06s ease, opacity 0.15s ease;
}

.sendBtn:active:not(:disabled) {
    transform: translateY(1px);
}

.sendBtn:disabled {
    opacity: 0.55;
    cursor: not-allowed;
}

.composerHint {
    margin-top: 8px;
    font-size: 12px;
    color: #6b7280;
}

/* Modal */
.modalOverlay {
    position: fixed;
    inset: 0;
    background: rgba(15, 23, 42, 0.35);
    display: grid;
    place-items: center;
    z-index: 9999;
    padding: 16px;
}

.modalCard {
    width: min(420px, 100%);
    background: #ffffff;
    border-radius: 18px;
    border: 1px solid #eef1f6;
    box-shadow: 0 18px 60px rgba(15, 23, 42, 0.25);
    padding: 16px;
}

.modalTitle {
    font-weight: 900;
    color: #0f172a;
    letter-spacing: -0.2px;
}

.modalDesc {
    margin-top: 8px;
    color: #475569;
    font-size: 14px;
    line-height: 1.5;
}

.modalActions {
    margin-top: 14px;
    display: flex;
    justify-content: flex-end;
    gap: 10px;
}

.modalBtn {
    border-radius: 14px;
    padding: 10px 12px;
    font-weight: 900;
    cursor: pointer;
    border: 1px solid #e5e7eb;
    background: #ffffff;
}

.modalBtn.ghost {
    background: #ffffff;
    border-color: #e5e7eb;
    color: #0f172a;
}

.modalBtn.danger {
    background: #e11d48;
    border-color: #e11d48;
    color: #ffffff;
}

.modalBtn:active {
    transform: translateY(1px);
}

@media (max-width: 980px) {
    .coachChatPage {
        grid-template-columns: 1fr;
        height: auto;
        min-height: auto;
    }

    .sidebar {
        max-height: 320px;
    }

    .chatRoom {
        min-height: 560px;
    }
}
</style>
