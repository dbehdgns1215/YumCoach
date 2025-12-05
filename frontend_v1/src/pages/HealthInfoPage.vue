<script setup>
import { ref } from 'vue'

const healthInfo = ref({
  height: 175,
  weight: 68,
  age: 29,
  gender: '남',
  activity: '낮음',
  notes: '알레르기, 식이 제한, 목표 체중 등을 적어주세요.',
})

const editInfo = ref({ ...healthInfo.value })
const showModal = ref(false)

const openModal = () => {
  editInfo.value = { ...healthInfo.value }
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const saveHealthInfo = () => {
  healthInfo.value = { ...editInfo.value }
  closeModal()
}
</script>

<template>
  <section class="page health">
    <div class="section card health-card">
      <div class="health-card__header">
        <div>
          <h2>건강 정보 입력</h2>
          <p class="muted">기본 신체 정보와 목표를 설정해 주세요.</p>
        </div>
        <button type="button" class="primary-button" @click="openModal">건강 정보 수정</button>
      </div>
      <div class="health-card__body">
        <div class="health-stat">
          <p class="muted">키 (cm)</p>
          <strong>{{ healthInfo.height }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">몸무게 (kg)</p>
          <strong>{{ healthInfo.weight }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">나이</p>
          <strong>{{ healthInfo.age }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">성별</p>
          <strong>{{ healthInfo.gender }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">활동량</p>
          <strong>{{ healthInfo.activity }}</strong>
        </div>
        <div class="health-notes">
          <p class="muted">건강 목표/주의 사항</p>
          <p>{{ healthInfo.notes }}</p>
        </div>
      </div>
    </div>
    <div class="section card summary">
      <h3>현재 상태 요약</h3>
      <div class="summary-grid">
        <div>
          <p class="muted">체중 목표</p>
          <strong>63kg</strong>
        </div>
        <div>
          <p class="muted">BMI (추정)</p>
          <strong>22.5</strong>
        </div>
        <div>
          <p class="muted">목표 활동량</p>
          <strong>주 5회</strong>
        </div>
        <div>
          <p class="muted">알레르기</p>
          <strong>없음</strong>
        </div>
      </div>
    </div>

    <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>건강 정보 수정</h3>
          <button type="button" class="modal-close" @click="closeModal" aria-label="닫기">×</button>
        </div>
        <p class="muted">기존 정보를 확인하고 필요하다면 수정하세요.</p>
        <form class="health-form">
          <label>
            <span>키 (cm)</span>
            <input type="number" v-model="editInfo.height" />
          </label>
          <label>
            <span>몸무게 (kg)</span>
            <input type="number" v-model="editInfo.weight" />
          </label>
          <label>
            <span>나이</span>
            <input type="number" v-model="editInfo.age" />
          </label>
          <label>
            <span>성별</span>
            <select v-model="editInfo.gender">
              <option>남</option>
              <option>여</option>
            </select>
          </label>
          <label>
            <span>활동량</span>
            <select v-model="editInfo.activity">
              <option>낮음</option>
              <option>보통</option>
              <option>높음</option>
            </select>
          </label>
          <label class="full">
            <span>건강 목표/주의 사항</span>
            <textarea rows="3" v-model="editInfo.notes"></textarea>
          </label>
          <button type="button" class="primary-button" @click="saveHealthInfo">정보 저장</button>
        </form>
      </div>
    </div>
  </section>
</template>
