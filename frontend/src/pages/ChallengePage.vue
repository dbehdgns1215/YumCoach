<template>
    <TopBarNavigation />
    <AppShell title="챌린지" subtitle="도전해요!" footerTheme="brand" @primary="noop">
        <div style="padding:5px">
            <!-- 활성 챌린지 -->
            <ChallengeList 
                :challenges="activeChallenges" 
                @create="openCreate" 
                @update="onUpdate"
                @complete="completeChallenge"
                @delete="deleteChallenge"
            />

            <!-- 완료된 챌린지 (접기/펼치기) -->
            <div v-if="completedChallenges.length > 0" class="completed-section">
                <div class="section-header" @click="showCompleted = !showCompleted">
                    <h3>완료된 챌린지 ({{ completedChallenges.length }})</h3>
                    <span>{{ showCompleted ? '▼' : '▶' }}</span>
                </div>
                <ChallengeList 
                    v-if="showCompleted"
                    :challenges="completedChallenges" 
                    :readonly="true"
                />
            </div>

            <!-- 생성 모달 -->
            <ChallengeCreateModal 
                :show="showCreate" 
                :initialData="initialData"
                @close="closeCreate" 
                @create="createFromModal" 
            />
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
onMounted(async () => {
    await loadChallenges()
})

async function loadChallenges() {
    try {
        const res = await api.get('/challenges', {
            headers: { 'X-USER-ID': '42' }
        })
        challenges.value = res.data.data || []
    } catch (e) {
        console.error('[ChallengePage] load failed', e)
    }
}

function openCreate(data = null) { 
    initialData.value = data
    showCreate.value = true 
}

function closeCreate() {
    showCreate.value = false
    initialData.value = null
}

function onUpdate(updated) {
    const idx = challenges.value.findIndex(c => c.id === updated.id)
    if (idx >= 0) challenges.value[idx] = updated
}

async function createFromModal(payload) {
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

        const res = await api.post('/challenges', body, {
            headers: { 'X-USER-ID': '42' }
        })
        
        const newChallenge = res.data.data
        challenges.value.unshift(newChallenge)
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

async function completeChallenge(challengeId) {
    try {
        await api.patch(`/challenges/${challengeId}/complete`, null, {
            headers: { 'X-USER-ID': '42' }
        })
        
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

async function deleteChallenge(challengeId) {
    if (!confirm('정말 삭제하시겠습니까?')) return
    
    try {
        await api.delete(`/challenges/${challengeId}`, {
            headers: { 'X-USER-ID': '42' }
        })
        
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