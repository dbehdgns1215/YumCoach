<template>
    <TopBarNavigation />
    <AppShell footerTheme="brand" @primary="noop">
        <div style="padding:5px">
            <!-- 활성 챌린지 -->
            <ChallengeList :challenges="activeChallenges" @create="openCreate" @update="onUpdate"
                @complete="completeChallenge" @delete="deleteChallenge" />

            <!-- 완료된 챌린지 (접기/펼치기) -->
            <div v-if="completedChallenges.length > 0" class="completed-section">
                <div class="section-header" @click="showCompleted = !showCompleted">
                    <h3>완료된 챌린지 ({{ completedChallenges.length }})</h3>
                    <span>{{ showCompleted ? '▼' : '▶' }}</span>
                </div>
                <ChallengeList v-if="showCompleted" :challenges="completedChallenges" :readonly="true" />
            </div>

            <!-- 생성 모달 -->
            <ChallengeCreateModal :show="showCreate" :initialData="initialData" @close="closeCreate"
                @create="createFromModal" />
        </div>
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import ChallengeList from '@/components/challenge/ChallengeList.vue'
import ChallengeCreateModal from '@/components/challenge/ChallengeCreateModal.vue'
import { ref, computed, onMounted } from 'vue'
import { api } from '@/lib/api.js'
import { showToast } from '@/lib/toast.js'

function noop() { }

const challenges = ref([])
const showCreate = ref(false)
const showCompleted = ref(false)
const initialData = ref(null)

// 활성 vs 완료 챌린지 분리
const activeChallenges = computed(() =>
    challenges.value.filter(c => c.status === 'ACTIVE')
)
const completedChallenges = computed(() =>
    challenges.value.filter(c => ['COMPLETED', 'FAILED', 'ABANDONED'].includes(c.status))
)

// 챌린지 목록 로드
onMounted(async () =>
{
    await loadChallenges()
})

async function loadChallenges()
{
    try {
        const res = await api.get('/challenges')
        challenges.value = res.data.data || []
    } catch (e) {
        console.error('[ChallengePage] load failed', e)
    }
}

function openCreate(data = null)
{
    initialData.value = data
    showCreate.value = true
}

function closeCreate()
{
    showCreate.value = false
    initialData.value = null
}

async function onUpdate(updated)
{
    const idx = challenges.value.findIndex(c => c.id === updated.id)
    if (idx < 0) return

    const original = JSON.parse(JSON.stringify(challenges.value[idx]))

    // 변경된 아이템 탐지
    const changedItem = updated.items.find(it =>
    {
        const orig = original.items.find(o => o.id === it.id)
        return orig && orig.done !== it.done
    })

    // 변경된 항목이 없으면 로컬만 갱신
    if (!changedItem) {
        challenges.value[idx] = updated
        return
    }

    // 서버에 토글 요청을 보내고, 서버가 반환한 최신 챌린지로 대체
    try {
        const res = await api.patch(`/challenges/items/${changedItem.id}`, { done: changedItem.done })
        const serverData = res.data && res.data.data
        if (serverData) {
            challenges.value[idx] = serverData
            showToast('항목이 업데이트되었습니다', 'success', 1200)
        } else {
            challenges.value[idx] = updated
        }
    } catch (e) {
        console.error('[ChallengePage] item update failed', e)
        showToast('항목 업데이트 실패', 'error')
        // 실패하면 로컬 변경 롤백
        challenges.value[idx] = original
    }
}

async function createFromModal(payload)
{
    console.debug('[ChallengePage] createFromModal', payload)

    try {
        const body = {
            title: payload.title,
            description: payload.description,
            goalType: payload.goalType || 'HABIT',
            goalDetails: payload.goalDetails,
            startDate: payload.startDate || new Date().toISOString().slice(0, 10),
            durationDays: payload.durationDays || 30,
            source: payload.source || 'MANUAL',
            sourceId: payload.sourceId,
            items: payload.items?.map((it, idx) => ({
                order: idx + 1,
                text: it.text || it
            })) || []
        }

        const res = await api.post('/challenges', body)
        // 서버가 생성된 챌린지 ID만 반환하므로 목록을 갱신합니다.
        await loadChallenges()
        showToast('챌린지 생성 완료! 🎉', 'success', 3000)
        closeCreate()

    } catch (e) {
        console.error('[ChallengePage] create failed', e)
        if (e?.response?.status === 401) {
            showToast('인증이 필요합니다', 'error')
        } else {
            showToast('챌린지 생성 실패', 'error')
        }
    }
}

async function completeChallenge(challengeId)
{
    try {
        await api.patch(`/challenges/${challengeId}/complete`)

        const idx = challenges.value.findIndex(c => c.id === challengeId)
        if (idx >= 0) {
            challenges.value[idx].status = 'COMPLETED'
            challenges.value[idx].completedAt = new Date().toISOString()
        }

        showToast('챌린지 완료! 축하합니다! 🎊', 'success', 3000)
    } catch (e) {
        console.error('[ChallengePage] complete failed', e)
        showToast('완료 처리 실패', 'error')
    }
}

async function deleteChallenge(challengeId)
{
    if (!confirm('정말 삭제하시겠습니까?')) return

    try {
        await api.delete(`/challenges/${challengeId}`)

        challenges.value = challenges.value.filter(c => c.id !== challengeId)
        showToast('챌린지 삭제 완료', 'success', 2000)
    } catch (e) {
        console.error('[ChallengePage] delete failed', e)
        showToast('삭제 실패', 'error')
    }
}
</script>

<style scoped>
.completed-section {
    margin-top: 24px;
    padding-top: 20px;
    border-top: 1px solid var(--border);
}

.section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    cursor: pointer;
    padding: 12px 0;
}

.section-header h3 {
    margin: 0;
    font-size: 16px;
    color: var(--muted);
}
</style>