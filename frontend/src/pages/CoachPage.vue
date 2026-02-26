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

                                    <!-- JSON-first 응답: date_request / range_request를 버튼으로 렌더링 -->
                                    <div v-if="m.parsedPayload && (m.parsedPayload.type === 'date_request' || m.parsedPayload.type === 'range_request')">
                                        <div class="bubbleText">{{ m.parsedPayload.message || m.content }}</div>
                                        <div class="selectOptions" style="margin-top:8px; display:flex; gap:8px; flex-wrap:wrap;">
                                            <button v-for="opt in (m.parsedPayload.available_dates || m.parsedPayload.available_ranges || [])"
                                                :key="opt"
                                                class="primaryCta"
                                                :disabled="isLoading || pendingSelections[selectedChat.id]"
                                                @click="handleQuickSelect(opt)">
                                                <span v-if="pendingSelections[selectedChat.id] === opt">요청중...</span>
                                                <span v-else>{{ opt }}</span>
                                            </button>
                                        </div>
                                    </div>
                                    <div v-else class="bubbleText">{{ m.content }}</div>
                                    <!-- 챌린지로 추가 버튼: parsedPayload가 리포트 같으면 표시 -->
                                    <div v-if="m.parsedPayload && (m.parsedPayload.meals || m.parsedPayload.insights || m.parsedPayload.summary || (m.parsedPayload.aiResponse && (m.parsedPayload.aiResponse.meals || m.parsedPayload.aiResponse.summary)))" style="margin-top:8px; display:flex; gap:8px;">
                                        <button class="primaryCta" :disabled="isLoading" @click="openChallengeModalFromParsed(m.parsedPayload)">챌린지로 추가</button>
                                    </div>
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
    <ChallengeCreateModal :show="showChallengeModal" :initialData="challengeInitialData" @close="showChallengeModal = false" @create="onCreateChallenge" />
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch, reactive } from 'vue'
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import ChallengeCreateModal from '@/components/challenge/ChallengeCreateModal.vue'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'
function noop() { }

const STORAGE_KEY = 'yumcoach_chat_state_v4'
const MAX_CHATS = 10
const analysisDate = ref(dayjs().format('YYYY-MM-DD'))

// ✅ 백엔드 설정
const API_BASE_URL = ''
const CHAT_ENDPOINT = '/api/chat'

// 인증 스토어 (Pinia)에서 accessToken 사용
const auth = useAuthStore()

const messagesEl = ref(null)
const draft = ref('')
const isLoading = ref(false)

// 챌린지 모달 상태
const showChallengeModal = ref(false)
const challengeInitialData = ref(null)

// 채팅별로 사용자가 선택한(또는 선택 대기 중인) 옵션을 기록하여
// 동일 채팅 내 버튼들을 비활성화하고 로딩 UX를 표시합니다.
const pendingSelections = reactive({})

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

// Open challenge modal with a parsed report object
function openChallengeModalFromParsed(parsed)
{
    if (!parsed || typeof parsed !== 'object') return
    const ai = parsed.aiResponse && typeof parsed.aiResponse === 'object' ? parsed.aiResponse : parsed
    const initial = {
        title: (ai.summary && ai.summary.split('\n')[0]) || '새 챌린지',
        description: ai.summary || '',
        source: 'AI_RECOMMEND',
        sourceId: ai.date || ai.startDate || null,
        goalDetails: (() => {
            const gd = {}
            if (ai.nutrition && typeof ai.nutrition === 'object') {
                if (ai.nutrition.calories) gd.calories = ai.nutrition.calories
                if (ai.nutrition.protein) gd.protein = ai.nutrition.protein
                if (ai.nutrition.carbs) gd.carbs = ai.nutrition.carbs
                if (ai.nutrition.fat) gd.fat = ai.nutrition.fat
            }
            if (ai.calories && !gd.calories) gd.calories = ai.calories
            if (ai.protein && !gd.protein) gd.protein = ai.protein
            return gd
        })(),
        items: Array.isArray(ai.meals) ? ai.meals.map((m, i) => ({ text: (m.name || m.title || m.label || '').toString(), order: i+1 })) : [],
        startDate: ai.date || ai.startDate || new Date().toISOString().slice(0,10),
        durationDays: 14
    }

    challengeInitialData.value = initial
    showChallengeModal.value = true
}

function onCreateChallenge(payload)
{
    // 기본 동작: 콘솔에 출력하고 모달 닫기
    try { console.debug('[CoachPage] Create challenge payload', payload) } catch(e){}
    showChallengeModal.value = false
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
function normalizeChatResponse(data) {
    // 서버가 문자열로 주는 경우(기존 흐름)
    if (typeof data === 'string') {
        let raw = data.trim()

        // 제거: 코드펜스, 백틱, ```json ``` 등
        raw = raw.replace(/```(?:json)?\s*/i, '').replace(/```\s*$/i, '')
        raw = raw.replace(/`/g, '')

        const tryParse = (s) => {
            try { return JSON.parse(s) } catch { return null }
        }

        // helper: extract balanced JSON object/array even with nested braces, ignoring braces inside strings
        const extractBalanced = (s) => {
            const startIdx = s.search(/[\{\[]/)
            if (startIdx === -1) return null
            const openChar = s[startIdx]
            const closeChar = openChar === '{' ? '}' : ']'
            let depth = 0
            let inString = false
            let escape = false
            for (let i = startIdx; i < s.length; i++) {
                const ch = s[i]
                if (escape) { escape = false; continue }
                if (ch === '\\') { escape = true; continue }
                if (ch === '"') { inString = !inString; continue }
                if (inString) continue
                if (ch === openChar) depth++
                else if (ch === closeChar) {
                    depth--
                    if (depth === 0) return s.slice(startIdx, i + 1)
                }
            }
            return null
        }

        // If whole string looks like JSON, try parse directly
        if ((raw.startsWith('{') && raw.endsWith('}')) || (raw.startsWith('[') && raw.endsWith(']'))) {
            const p = tryParse(raw)
            if (p) { data = p }
            else {
                // try to extract balanced part
                const sub = extractBalanced(raw)
                if (sub) {
                    let parsed = tryParse(sub)
                    if (!parsed) {
                        // try unescaping common escapes
                        const unescaped = sub.replace(/\\"/g, '"').replace(/\\\\/g, '\\')
                        parsed = tryParse(unescaped)
                    }
                    if (parsed) data = parsed
                    else return { text: cleanForChat(raw), hashtag: '' }
                } else return { text: cleanForChat(raw), hashtag: '' }
            }
        } else {
            // try to extract JSON object/array substring from arbitrary text
            const sub = extractBalanced(raw)
            if (sub) {
                let parsed = tryParse(sub)
                if (!parsed) {
                    const unescaped = sub.replace(/\\"/g, '"').replace(/\\\\/g, '\\')
                    parsed = tryParse(unescaped)
                }
                if (parsed) data = parsed
                else return { text: cleanForChat(raw), hashtag: '' }
            } else {
                // fallback: maybe JSON is double-quoted string like "{\"type\":...}"
                const dqMatch = raw.match(/\"\{[\s\S]*\}\"/)
                if (dqMatch) {
                    const inner = dqMatch[0].slice(2, -2) // remove leading \" and trailing \"
                    const unescaped = inner.replace(/\\"/g, '"').replace(/\\\\/g, '\\')
                    const parsed = tryParse(unescaped)
                    if (parsed) data = parsed
                    else return { text: cleanForChat(raw), hashtag: '' }
                } else return { text: cleanForChat(raw), hashtag: '' }
            }
        }

    }

    // 서버가 이미 JSON 객체 형태로 응답한 경우
    if (data && typeof data === 'object') {
        const hashtag =
            (typeof data?.detected_hashtag === 'string' && data.detected_hashtag) ||
            (typeof data?.hashtag === 'string' && data.hashtag) ||
            ''

        // 🔥 1. daily_report 타입 처리
        if (data.type === 'daily_report') {
            let formatted = `📊 일간 분석 결과 (${data.analysis_date})\n\n`
            
            // 영양소 요약
            formatted += `📈 영양소 요약\n`
            formatted += `• 칼로리: ${data.totals.calories} kcal\n`
            formatted += `• 탄수화물: ${data.totals.carbs} g\n`
            formatted += `• 단백질: ${data.totals.protein} g\n`
            formatted += `• 지방: ${data.totals.fat} g\n`
            formatted += `• 식사 횟수: ${data.meals}끼\n\n`

            // 경고사항
            if (data.warnings && data.warnings.length > 0) {
                formatted += `⚠️ 주의사항\n`
                data.warnings.forEach(w => formatted += `• ${w}\n`)
                formatted += `\n`
            }

            
            // 실천 규칙
            if (data.rules && data.rules.length > 0) {
                formatted += `✅ 오늘의 실천 규칙\n`
                data.rules.forEach((r, i) => {
                    formatted += `${i + 1}. ${r.title}\n`
                    formatted += `   → ${r.action}\n`
                })
                formatted += `\n`
            }

            // 추천 음식
            if (data.recommended_foods && data.recommended_foods.length > 0) {
                formatted += `🥗 추천 음식\n`
                data.recommended_foods.forEach(f => {
                    formatted += `• ${f.food}: ${f.reason}\n`
                })
                formatted += `\n`
            }

            // 요약
            if (data.summary) {
                formatted += `💬 ${data.summary}\n`
            }

            return { text: formatted, hashtag, parsed: data }
        }

        // 🔥 2. weekly_report 타입 처리
        if (data.type === 'weekly_report') {
            let formatted = `📈 주간 분석 결과\n`
            formatted += `기간: ${data.period.from} ~ ${data.period.to}\n\n`

            // 평균 영양소
            formatted += `📊 일평균 영양소\n`
            formatted += `• 칼로리: ${data.averages.calories} kcal\n`
            formatted += `• 탄수화물: ${data.averages.carbs} g\n`
            formatted += `• 단백질: ${data.averages.protein} g\n`
            formatted += `• 지방: ${data.averages.fat} g\n`
            formatted += `• 기록 일수: ${data.recorded_days}일\n\n`

            // 개선 포인트
            if (data.improvements && data.improvements.length > 0) {
                formatted += `🎯 개선 포인트\n`
                data.improvements.forEach(imp => formatted += `• ${imp}\n`)
                formatted += `\n`
            }

            // 실천 규칙
            if (data.rules && data.rules.length > 0) {
                formatted += `✅ 다음 주 실천 규칙\n`
                data.rules.forEach((r, i) => {
                    formatted += `${i + 1}. ${r.title}\n`
                    formatted += `   → ${r.action}\n`
                })
                formatted += `\n`
            }

            // 코치 메모
            if (data.coach_note) {
                formatted += `💬 ${data.coach_note}\n`
            }

            return { text: formatted, hashtag, parsed: data }
        }

        // 🔥 3. error 타입 처리
        if (data.type === 'error') {
            return { 
                text: `⚠️ ${data.message || '오류가 발생했습니다.'}`, 
                hashtag, 
                parsed: data 
            }
        }

        // 🔥 4. date_request / range_request (기존 로직 유지)
        if (data.type === 'date_request' || data.type === 'range_request') {
            const message = typeof data.message === 'string' ? data.message : JSON.stringify(data)
            return { text: cleanForChat(message), hashtag, parsed: data }
        }

        // 🔥 5. 기존 리포트 포맷 처리 (백업용)
        try {
            const looksLikeReport = (obj) => {
                if (!obj || typeof obj !== 'object') return false
                const keys = Object.keys(obj)
                const reportIndicators = ['aiResponse','reportType','meals','nutrition','insights','summary','date','startDate','endDate']
                return reportIndicators.some(k => keys.includes(k))
            }

            let reportObj = null
            if (looksLikeReport(data)) reportObj = data
            else if (data.aiResponse && typeof data.aiResponse === 'object' && looksLikeReport(data.aiResponse)) 
                reportObj = data.aiResponse

            if (reportObj) {
                let out = ''
                if (typeof reportObj.summary === 'string' && reportObj.summary.trim()) {
                    out += reportObj.summary.trim() + '\n\n'
                }

                if (Array.isArray(reportObj.meals) && reportObj.meals.length) {
                    out += '식사 내역:\n'
                    reportObj.meals.forEach((m) => {
                        const name = m.name || m.title || m.label || ''
                        const kcal = (m.kcal || m.calories) ? ` — ${m.kcal || m.calories} kcal` : ''
                        const qty = m.quantity ? ` (${m.quantity})` : ''
                        out += `- ${name}${qty}${kcal}\n`
                    })
                    out += '\n'
                }

                if (reportObj.insights) {
                    if (typeof reportObj.insights === 'string') {
                        out += '인사이트:\n' + reportObj.insights.trim() + '\n\n'
                    } else if (Array.isArray(reportObj.insights) && reportObj.insights.length) {
                        out += '인사이트:\n'
                        reportObj.insights.forEach(i => out += `- ${typeof i === 'string' ? i : JSON.stringify(i)}\n`)
                        out += '\n'
                    }
                }

                if (reportObj.nutrition && typeof reportObj.nutrition === 'object') {
                    out += '영양 성분:\n'
                    Object.entries(reportObj.nutrition).forEach(([k, v]) => {
                        out += `- ${k}: ${v}\n`
                    })
                    out += '\n'
                }

                if (reportObj.date) out += `날짜: ${reportObj.date}\n`
                if (reportObj.startDate && reportObj.endDate) 
                    out += `기간: ${reportObj.startDate} ~ ${reportObj.endDate}\n`

                if (!out.trim()) out = JSON.stringify(reportObj, null, 2)

                return { text: cleanForChat(out), hashtag }
            }
        } catch (e) {
            console.warn('report formatter failed', e)
        }

        // 기타 응답 처리
        let rawText =
            (typeof data?.response === 'string' && data.response) ||
            (typeof data?.reply === 'string' && data.reply) ||
            (typeof data?.message === 'string' && data.message) ||
            ''

        rawText = (rawText ?? '').trim()

        let parsed = null
        if (rawText.startsWith('{') && rawText.endsWith('}')) {
            try {
                parsed = JSON.parse(rawText)
                if (parsed && typeof parsed === 'object' && 
                    (parsed.type === 'date_request' || parsed.type === 'range_request')) {
                    const msg = typeof parsed.message === 'string' ? parsed.message : rawText
                    return { text: cleanForChat(msg), hashtag, parsed }
                }
            } catch {
                // 무시
            }
        }

        return { text: cleanForChat(rawText || '응답 형식이 올바르지 않아요.'), hashtag }
    }

    return { text: '응답 형식이 올바르지 않아요.', hashtag: '' }
}



async function callChatAPI(messageText, overrideReportData = null) {
    const url = `${API_BASE_URL}${CHAT_ENDPOINT}`

    if (!auth.user) {
        throw new Error('로그인 정보가 없습니다')
    }

    const payload = {
        message: messageText,
        user_id: String(auth.user.id),
        analysisDate: analysisDate.value,
        user_profile: {
            height: auth.user.height,
            weight: auth.user.weight,
            goal: auth.user.goal,
        },
        report_data: overrideReportData ?? null,
    }

    const res = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(auth.accessToken && {
                Authorization: `Bearer ${auth.accessToken}`,
            }),
        },
        credentials: 'include',
        body: JSON.stringify(payload),
    })

    if (!res.ok) {
        const txt = await res.text()
        throw new Error(`API 요청 실패 (${res.status}) ${txt}`)
    }

    const ct = res.headers.get('content-type') || ''
    if (ct.includes('application/json')) return await res.json()
    return await res.text()
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
        // 로그인 토큰이 있고 사용자 정보가 없으면 서버에서 /me로 사용자 정보를 보강
        if (auth?.accessToken && !auth.user) {
            try {
                await auth.checkAuth()
            } catch (e) {
                console.warn('auth.checkAuth failed', e)
            }
        }
        // 만약 사용자가 리포트 태그만 보냈다면(예: #일간리포트, #일일리포트, #주간리포트)
        // 명시적으로 report_data=null을 전송하여 서버/프롬프트에 "리포트 없음" 신호를 보냅니다.
        const reportTagPattern = /#(일간리포트|일일리포트|주간리포트)/
        const shouldSendNullReport = reportTagPattern.test(text.trim()) && text.trim().split(/\s+/).length === 1

        const raw = await callChatAPI(text, shouldSendNullReport ? null : undefined)
        const { text: replyText, hashtag, parsed } = normalizeChatResponse(raw)

        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content: replyText,
            detected_hashtag: hashtag,
            parsedPayload: parsed || null,
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

/* ---------------------------
   Quick select handler (date/range 버튼 클릭)
--------------------------- */
async function handleQuickSelect(option)
{
    if (!selectedChat.value) return
    if (isLoading.value) return

    const chat = selectedChat.value
    // 바로 유저 메시지로 추가 (사용자가 단독으로 날짜/범위를 보낸 것처럼)
    chat.messages.push({ id: uid('m'), role: 'user', content: option, createdAt: nowISO() })
    bumpChat(chat.id)
    persist()
    await nextTick()
    scrollToBottom()

    // 서버에 단독 날짜/범위 문자열 전송 (기존 send() 흐름과 비슷)
    // 이 채팅에서 선택 대기 상태로 표시
    pendingSelections[chat.id] = option
    isLoading.value = true
    try {
        // 컨텍스트 해시태그가 있으면 옵션 앞에 붙여서 전송합니다.
        let contextTag = ''
        for (let i = chat.messages.length - 1; i >= 0; i--) {
            const mm = chat.messages[i]
            if (mm.role === 'ai' && (mm.parsedPayload || mm.detected_hashtag)) {
                contextTag = mm.detected_hashtag || ''
                break
            }
        }
        const sendText = contextTag ? `${contextTag} ${option}` : option

        // 날짜/범위이면 백엔드에서 실제 리포트를 가져와 report_data로 포함
        let reportData = null
        try {
            const parsed = parseDateOpt(option)
                if (parsed && parsed.type === 'date') {
                const iso = parsed.date.toISOString().slice(0, 10)
                const r = await fetch(`${API_BASE_URL}/api/reports/daily?date=${iso}`, {
                    method: 'GET',
                    headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                    credentials: 'include'
                })
                if (r.ok) reportData = await r.json()
                else {
                    console.warn('Report not found for', iso, r.status)
                    // 명시적으로 null을 전달하여 서버/프롬프트에 리포트 없음 신호를 보냄
                    reportData = null
                }
                } else if (parsed && parsed.type === 'range') {
                const fromIso = parsed.start.toISOString().slice(0, 10)
                const r = await fetch(`${API_BASE_URL}/api/reports/weekly?fromDate=${fromIso}`, {
                    method: 'GET',
                    headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                    credentials: 'include'
                })
                if (r.ok) reportData = await r.json()
                else {
                    console.warn('Weekly report not found for', fromIso, r.status)
                    reportData = null
                }
            }
        } catch (e) {
            // 무시: reportData는 null일 수 있음
            console.warn('report fetch failed', e)
        }

        // 디버그: 전송할 payload 확인
                try {
                    const debugProfile = auth.user
                    console.debug('[CoachPage] outgoing chat payload', {
                        message: sendText,
                        user_id: auth.user.id,
                        user_profile: auth.user,
                        report_data: reportData,
                    })
                } catch(e) {}
        const raw = await callChatAPI(sendText, reportData)
        const { text: replyText, hashtag, parsed } = normalizeChatResponse(raw)

        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content: replyText,
            detected_hashtag: hashtag,
            parsedPayload: parsed || null,
            createdAt: nowISO(),
        })
        // 선택 완료 상태 해제
        delete pendingSelections[chat.id]
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
            parsedPayload: null,
            createdAt: nowISO(),
        })
        // 선택 실패시에도 해제
        delete pendingSelections[chat.id]
        bumpChat(chat.id)
        persist()
        await nextTick()
        scrollToBottom()
    }
}

// 요약 요청 (선택 옵션 옆의 '요약' 버튼)
async function handleSummaryRequest(option)
{
    if (!selectedChat.value) return
    if (isLoading.value) return

    const chat = selectedChat.value
    // 사용자 메시지로 추가
    chat.messages.push({ id: uid('m'), role: 'user', content: `요약: ${option}`, createdAt: nowISO() })
    bumpChat(chat.id)
    persist()
    await nextTick()
    scrollToBottom()

    pendingSelections[chat.id] = option
    isLoading.value = true
    try {
        // 컨텍스트 해시태그가 있으면 요약 요청 앞에 붙여서 전송
        let contextTag = ''
        for (let i = chat.messages.length - 1; i >= 0; i--) {
            const mm = chat.messages[i]
            if (mm.role === 'ai' && (mm.parsedPayload || mm.detected_hashtag)) {
                contextTag = mm.detected_hashtag || ''
                break
            }
        }
        const payloadText = contextTag ? `${contextTag} 요약: ${option}` : `요약: ${option}`

        // 요약도 가능하면 DB 리포트 포함
        let reportData = null
        try {
            const parsed = parseDateOpt(option)
            if (parsed && parsed.type === 'date') {
                    const iso = parsed.date.toISOString().slice(0, 10)
                    const r = await fetch(`${API_BASE_URL}/api/reports/daily?date=${iso}`, {
                        method: 'GET',
                        headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                        credentials: 'include'
                    })
                if (r.ok) reportData = await r.json()
            } else if (parsed && parsed.type === 'range') {
                const fromIso = parsed.start.toISOString().slice(0, 10)
                const r = await fetch(`${API_BASE_URL}/api/reports/weekly?fromDate=${fromIso}`, {
                    method: 'GET',
                    headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                    credentials: 'include'
                })
                if (r.ok) reportData = await r.json()
            }
        } catch (e) {
            console.warn('report fetch failed', e)
        }

        const raw = await callChatAPI(payloadText, reportData)
        const { text: replyText, hashtag, parsed } = normalizeChatResponse(raw)

        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content: replyText,
            detected_hashtag: hashtag,
            parsedPayload: parsed || null,
            createdAt: nowISO(),
        })
        delete pendingSelections[chat.id]
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
            parsedPayload: null,
            createdAt: nowISO(),
        })
        delete pendingSelections[chat.id]
        bumpChat(chat.id)
        persist()
        await nextTick()
        scrollToBottom()
    }
}

// Helper: given a parsed object from parseDateOpt, fetch daily or weekly report
async function fetchReportForParsed(parsed) {
    if (!parsed) return null
    try {
        if (parsed.type === 'date') {
            const iso = parsed.date.toISOString().slice(0, 10)
            const r = await fetch(`${API_BASE_URL}/api/reports/daily?date=${iso}`, {
                method: 'GET',
                headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                credentials: 'include'
            })
            if (r.ok) return await r.json()
            console.warn('Report not found for', iso, r.status)
            return null
        } else if (parsed.type === 'range') {
            const fromIso = parsed.start.toISOString().slice(0, 10)
            const r = await fetch(`${API_BASE_URL}/api/reports/weekly?fromDate=${fromIso}`, {
                method: 'GET',
                headers: Object.assign({ 'Accept': 'application/json' }, auth?.accessToken ? { Authorization: `Bearer ${auth.accessToken}` } : {}),
                credentials: 'include'
            })
            if (r.ok) return await r.json()
            console.warn('Weekly report not found for', fromIso, r.status)
            return null
        }
    } catch (e) {
        console.warn('report fetch failed', e)
        return null
    }
    return null
}

// 추천 요청: 현재 컨텍스트(선택된 날짜/범위 또는 최근 선택)를 찾아 report_data로 보내고 '#추천' 요청
async function handleRecommendation()
{
    if (!selectedChat.value) return
    if (isLoading.value) return

    const chat = selectedChat.value
    chat.messages.push({ id: uid('m'), role: 'user', content: '#추천', createdAt: nowISO() })
    bumpChat(chat.id)
    persist()
    await nextTick()
    scrollToBottom()

    pendingSelections[chat.id] = '#추천'
    isLoading.value = true
    try {
        // 찾을 수 있는 context hashtag 또는 parsedPayload 확인
        let contextTag = ''
        for (let i = chat.messages.length - 1; i >= 0; i--) {
            const mm = chat.messages[i]
            if (mm.role === 'ai' && (mm.parsedPayload || mm.detected_hashtag)) {
                contextTag = mm.detected_hashtag || ''
                break
            }
        }

        // 우선 최근 사용자 메시지에서 날짜/범위를 추출
        let reportData = null
        for (let i = chat.messages.length - 1; i >= 0; i--) {
            const mm = chat.messages[i]
            if (mm.role === 'user') {
                const p = parseDateOpt(mm.content)
                if (p) {
                    reportData = await fetchReportForParsed(p)
                    if (reportData) break
                }
            }
            if (mm.role === 'ai' && mm.parsedPayload && (mm.parsedPayload.type === 'date_request' || mm.parsedPayload.type === 'range_request')) {
                // use the most recent available option (rightmost)
                const opts = mm.parsedPayload.available_dates || mm.parsedPayload.available_ranges || []
                const pick = Array.isArray(opts) && opts.length ? opts[opts.length - 1] : null
                if (pick) {
                    const p2 = parseDateOpt(typeof pick === 'string' ? pick : (pick.raw || pick))
                    if (p2) {
                        reportData = await fetchReportForParsed(p2)
                        if (reportData) break
                    }
                }
            }
        }

        const sendText = contextTag ? `${contextTag} #추천` : '#추천'

        // 디버그 로깅
        try { console.debug('[CoachPage] recommendation payload', { message: sendText, user_id: auth.user?.id, report_data: reportData }) } catch(e) {}

        const raw = await callChatAPI(sendText, reportData)
        const { text: replyText, hashtag, parsed } = normalizeChatResponse(raw)

        isLoading.value = false
        chat.messages.push({
            id: uid('m'),
            role: 'ai',
            content: replyText,
            detected_hashtag: hashtag,
            parsedPayload: parsed || null,
            createdAt: nowISO(),
        })
        delete pendingSelections[chat.id]
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
                `추천 요청 중 문제가 발생했어요.\n` +
                `- 원인: ${err?.message ?? '알 수 없음'}\n\n` +
                `백엔드 서버/토큰/주소를 확인해주세요.`,
            detected_hashtag: '',
            parsedPayload: null,
            createdAt: nowISO(),
        })
        delete pendingSelections[chat.id]
        bumpChat(chat.id)
        persist()
        await nextTick()
        scrollToBottom()
    }
}

/* ---------------------------
   Helpers: 날짜/범위 레이블 생성
--------------------------- */
function parseDateOpt(opt)
{
    // supports YYYY-MM-DD or MM.DD or MM.DD~MM.DD ranges or YYYY-MM-DD~YYYY-MM-DD
    if (typeof opt !== 'string') return null
    if (opt.includes('~')) {
        const [a, b] = opt.split('~').map(s => s.trim())
        return { type: 'range', start: parseLooseDate(a), end: parseLooseDate(b) }
    }
    return { type: 'date', date: parseLooseDate(opt) }
}

function parseLooseDate(s)
{
    if (!s) return null
    // YYYY-MM-DD
    const isoMatch = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
    if (isoMatch) return new Date(`${isoMatch[1]}-${isoMatch[2]}-${isoMatch[3]}T00:00:00`)

    // MM.DD
    const mdMatch = s.match(/^(\d{1,2})\.(\d{1,2})$/)
    if (mdMatch) {
        const now = new Date()
        const year = now.getFullYear()
        let d = new Date(`${year}-${String(mdMatch[1]).padStart(2,'0')}-${String(mdMatch[2]).padStart(2,'0')}T00:00:00`)
        // if parsed date is in future, assume previous year
        if (d > now) d.setFullYear(year - 1)
        return d
    }

    return null
}

function daysAgoLabel(date)
{
    if (!date) return ''
    const today = new Date(); today.setHours(0,0,0,0)
    const d = new Date(date); d.setHours(0,0,0,0)
    const diff = Math.round((today - d) / (1000*60*60*24))
    if (diff === 1) return '어제'
    if (diff === 2) return '이틀 전'
    if (diff === 3) return '사흘 전'
    return `${diff}일 전`
}

function weeksAgoLabel(startDate)
{
    if (!startDate) return ''
    // compute start of current week (Mon)
    const now = new Date();
    const day = now.getDay();
    const diffToMon = (day + 6) % 7; // 0->Mon
    const thisMonday = new Date(now); thisMonday.setHours(0,0,0,0); thisMonday.setDate(now.getDate() - diffToMon)

    const s = new Date(startDate); s.setHours(0,0,0,0)
    const weeks = Math.round((thisMonday - s) / (1000*60*60*24*7))
    if (weeks === 1) return '지난 주'
    if (weeks === 2) return '이주 전'
    if (weeks === 3) return '삼주 전'
    return `${weeks}주 전`
}

function buildOptionsForMessage(m)
{
    const payload = m.parsedPayload
    if (!payload) return []

    // 요구사항: 항상 과거 세 개(또는 세 주)만 표시, 왼쪽이 가장 과거, 오른쪽이 가장 최근
    if (payload.type === 'date_request') {
        // 오늘/오늘 포함 날짜는 제외하고, 무조건 "3일전, 2일전, 1일전" 순서로 반환
        const now = new Date(); now.setHours(0,0,0,0)
        const opts = []
        for (let d = 3; d >= 1; d--) {
            const dt = new Date(now)
            dt.setDate(now.getDate() - d)
            const raw = formatMD(dt)
            opts.push({ key: raw, label: `${daysAgoLabel(dt)} (${raw})`, raw })
        }
        return opts
    }

    if (payload.type === 'range_request') {
        // 주 단위: 이번 주 제외, "3주 전, 2주 전, 지난 주" 순서(왼쪽→오른쪽)
        const now = new Date()
        const day = now.getDay()
        const diffToMon = (day + 6) % 7
        const thisMonday = new Date(now); thisMonday.setHours(0,0,0,0); thisMonday.setDate(now.getDate() - diffToMon)

        const opts = []
        for (let w = 3; w >= 1; w--) {
            const start = new Date(thisMonday)
            start.setDate(thisMonday.getDate() - (7 * w))
            const end = new Date(start); end.setDate(start.getDate() + 6)
            const raw = `${formatMD(start)}~${formatMD(end)}`
            opts.push({ key: raw, label: `${weeksAgoLabel(start)} (${formatMD(start)} ~ ${formatMD(end)})`, raw })
        }
        return opts
    }

    return []
}

function formatMD(d)
{
    if (!d) return ''
    const mm = String(d.getMonth()+1).padStart(2,'0')
    const dd = String(d.getDate()).padStart(2,'0')
    return `${mm}.${dd}`
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
