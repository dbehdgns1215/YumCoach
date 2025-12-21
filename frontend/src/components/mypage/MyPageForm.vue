<style scoped>
.mypage-form { display:flex; flex-direction:column; gap:16px; }
.grid { display:flex; gap:16px; }
.col { flex:1; display:flex; flex-direction:column; gap:8px; }
.label { font-weight:800; font-size:13px; color:var(--muted); }
.input { padding:10px; border-radius:8px; border:1px solid var(--border); }
.input.small { width:140px; }
.checkboxes { display:flex; gap:8px; flex-wrap:wrap; margin-top:8px }
.restrictions { display:flex; flex-direction:column; gap:8px }
.restriction-row { display:flex; gap:8px; align-items:center }
.add-row { margin-top:8px }
.actions { display:flex; gap:12px; margin-top:12px }
.btn { border-radius:8px; padding:8px 10px; }
.ghost { background:transparent; border:1px solid var(--border); }
.secondary { background:var(--primary-soft); color:var(--primary); }
</style>
<template>
  <form class="mypage-form" @submit.prevent="onSave">
    <section class="grid">
      <div class="col">
        <label class="label">이름</label>
        <input v-model="form.user.name" class="input" :disabled="true" />

        <label class="label">이메일</label>
        <input v-model="form.user.email" class="input" :disabled="true" />

        <label class="label">닉네임</label>
        <input v-model="form.user.nickname" class="input" :disabled="!editMode" />

        <label class="label">전화번호</label>
        <input v-model="form.user.phone" class="input" :disabled="!editMode" />

        <label class="label">성별</label>
        <select v-model="form.user.gender" class="input" :disabled="!editMode">
          <option value="">선택 안함</option>
          <option value="male">남성</option>
          <option value="female">여성</option>
          <option value="other">기타</option>
        </select>

        <label class="label">나이</label>
        <input type="number" v-model.number="form.user.age" class="input" :disabled="!editMode" />
      </div>

      <div class="col">
        <label class="label">키 (cm)</label>
        <input type="number" v-model.number="form.health.height" class="input" :disabled="!editMode" />

        <label class="label">몸무게 (kg)</label>
        <input type="number" v-model.number="form.health.weight" class="input" :disabled="!editMode" />

        <label class="label">활동량</label>
        <select v-model.number="form.health.activityLevel" class="input" :disabled="!editMode">
          <option :value="0">거의 움직이지 않음 (0회/주)</option>
          <option :value="1">가벼운 활동 (1-2회/주)</option>
          <option :value="2">보통 활동 (3-4회/주)</option>
          <option :value="3">활발한 활동 (5회 이상/주)</option>
        </select>

        <div class="checkboxes">
          <label><input type="checkbox" v-model="form.health.diabetes" :disabled="!editMode" /> 당뇨</label>
          <label><input type="checkbox" v-model="form.health.highBloodPressure" :disabled="!editMode" /> 고혈압</label>
          <label><input type="checkbox" v-model="form.health.hyperlipidemia" :disabled="!editMode" /> 이상지질혈증</label>
          <label><input type="checkbox" v-model="form.health.kidneyDisease" :disabled="!editMode" /> 신장질환</label>
        </div>
      </div>
    </section>

    <BaseCard>
      <template #header>
        <div class="card-title">식이 제한</div>
      </template>
      <div class="restrictions">
        <div class="add-row" v-if="editMode">
          <button type="button" class="btn secondary" @click="addRestriction">추가</button>
        </div>
        <div v-for="(r, idx) in form.dietRestrictions" :key="idx" class="restriction-row">
          <select v-model="r.restrictionType" class="input small" :disabled="!editMode">
            <option value="">선택</option>
            <option value="ALLERGY">알레르기</option>
            <option value="INTOLERANCE">불내증</option>
            <option value="DISFAVOR">비선호</option>
            <option value="MEDICAL">의학적 제한</option>
            <option value="ETHICAL">종교, 윤리적 선택</option>
          </select>
          <input v-model="r.restrictionValue" class="input" :disabled="!editMode" placeholder="값 (예: 땅콩)" />
          <button type="button" class="btn ghost" @click="removeRestriction(idx)" v-if="editMode">삭제</button>
        </div>
      </div>
    </BaseCard>

    <div class="actions">
      <BaseButton :variant="editMode ? 'primary' : 'ghost'" @click="onToggleEdit">{{ editMode ? '저장' : '수정' }}</BaseButton>
    </div>
  </form>
</template>

<script setup>
import { reactive, onMounted, ref } from 'vue'
import api from '@/lib/api'
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

const form = reactive({
  user: { name: '', email: '', nickname: '', phone: '', gender: '', age: null },
  health: { height: null, weight: null, diabetes: false, highBloodPressure: false, hyperlipidemia: false, kidneyDisease: false, activityLevel: 0 },
  dietRestrictions: [],
})

const editMode = ref(false)

function mapFromResponse(payload) {
  form.user.name = payload.user?.name || ''
  form.user.email = payload.user?.email || ''
  form.user.nickname = payload.user?.nickname || payload.user?.name || ''
  form.user.phone = payload.user?.phone || ''
  form.user.gender = payload.user?.gender || ''
  form.user.age = payload.user?.age || null

  const h = payload.health || {}
  form.health.height = h.height || null
  form.health.weight = h.weight || null
  form.health.diabetes = !!h.diabetes
  form.health.highBloodPressure = !!h.highBloodPressure
  form.health.hyperlipidemia = !!h.hyperlipidemia
  form.health.kidneyDisease = !!h.kidneyDisease
  // activityLevel from server may be number or string; normalize to integer enum
  if (typeof h.activityLevel === 'number') {
    form.health.activityLevel = h.activityLevel
  } else if (typeof h.activityLevel === 'string') {
    const map = { 'SEDENTARY': 0, 'LIGHT': 1, 'MODERATE': 2, 'ACTIVE': 3 }
    form.health.activityLevel = map[h.activityLevel] ?? (Number(h.activityLevel) || 0)
  } else {
    form.health.activityLevel = 0
  }

  form.dietRestrictions = (payload.dietRestrictions || []).map(r => ({ restrictionType: r.restrictionType, restrictionValue: r.restrictionValue }))
}

async function load() {
  try {
    const res = await api.get('/user/mypage')
    const data = res.data && res.data.data ? res.data.data : null
    if (data) mapFromResponse(data)
  } catch (e) {
    console.error('Failed to load mypage', e)
  }
}

function addRestriction() {
  // 이미 비어있는 항목이 있으면 추가하지 않음
  const hasEmpty = form.dietRestrictions.some(r => {
    const t = r?.restrictionType
    const v = r?.restrictionValue
    return !t || !v || (typeof t === 'string' && t.trim() === '') || (typeof v === 'string' && v.trim() === '')
  })
  if (hasEmpty) return
  form.dietRestrictions.push({ restrictionType: '', restrictionValue: '' })
}

function removeRestriction(idx) {
  form.dietRestrictions.splice(idx, 1)
}

async function onSave() {
  try {
    // 빈 식이제한 항목은 전송하지 않음
    const filteredRestrictions = (form.dietRestrictions || []).filter(r => {
      if (!r) return false
      const t = r.restrictionType
      const v = r.restrictionValue
      if (!t || !v) return false
      if (typeof t === 'string' && t.trim() === '') return false
      if (typeof v === 'string' && v.trim() === '') return false
      return true
    })

    const payload = {
      nickname: form.user.nickname,
      phone: form.user.phone,
      gender: form.user.gender,
      age: form.user.age,
      height: form.health.height,
      weight: form.health.weight,
      diabetes: form.health.diabetes,
      highBloodPressure: form.health.highBloodPressure,
      hyperlipidemia: form.health.hyperlipidemia,
      kidneyDisease: form.health.kidneyDisease,
      activityLevel: form.health.activityLevel,
      dietRestrictions: filteredRestrictions,
    }
    const res = await api.put('/user/mypage', payload)
    if (res.data && res.data.success) {
      editMode.value = false
      await load()
      alert('저장되었습니다.')
    } else {
      alert('저장에 실패했습니다.')
    }
  } catch (e) {
    console.error('Save failed', e)
    alert('저장 중 오류가 발생했습니다.')
  }
}

function onToggleEdit() {
  if (editMode.value) {
    onSave()
  } else {
    editMode.value = true
  }
}

onMounted(load)
</script>

<style scoped>
.mypage-form { display:flex; flex-direction:column; gap:16px; }
.grid { display:flex; gap:16px; }
.col { flex:1; display:flex; flex-direction:column; gap:8px; }
.label { font-weight:800; font-size:13px; color:var(--muted); }
.input { padding:10px; border-radius:8px; border:1px solid var(--border); }
.input.small { width:140px; }
.checkboxes { display:flex; gap:8px; flex-wrap:wrap; margin-top:8px }
.restrictions { display:flex; flex-direction:column; gap:8px }
.restriction-row { display:flex; gap:8px; align-items:center }
.add-row { margin-top:8px }
.actions { display:flex; gap:12px; margin-top:12px }
.btn { border-radius:8px; padding:8px 10px; }
.ghost { background:transparent; border:1px solid var(--border); }
.secondary { background:var(--primary-soft); color:var(--primary); }
</style>














