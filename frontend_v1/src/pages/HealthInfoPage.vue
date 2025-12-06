<script setup>
import { ref, onMounted } from 'vue'
import { userApi } from '../services/api'

const healthInfo = ref({
  height: 0,
  weight: 0,
  diabetes: false,
  highBloodPressure: false,
  hyperlipidemia: false,
  kidneyDisease: false,
})

const editInfo = ref({ ...healthInfo.value })
const showModal = ref(false)
const loading = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

// BMI 계산
const calculateBMI = () => {
  if (healthInfo.value.height > 0 && healthInfo.value.weight > 0) {
    const heightInMeters = healthInfo.value.height / 100
    return (healthInfo.value.weight / (heightInMeters * heightInMeters)).toFixed(1)
  }
  return '0.0'
}

// 건강정보 조회
const fetchHealthInfo = async () => {
  loading.value = true
  errorMessage.value = ''
  
  try {
    const data = await userApi.getUserHealth()
    if (data) {
      healthInfo.value = {
        height: data.height || 0,
        weight: data.weight || 0,
        diabetes: data.diabetes || false,
        highBloodPressure: data.highBloodPressure || false,
        hyperlipidemia: data.hyperlipidemia || false,
        kidneyDisease: data.kidneyDisease || false,
      }
    }
  } catch (error) {
    errorMessage.value = error.message || '건강정보를 불러오는데 실패했습니다.'
  } finally {
    loading.value = false
  }
}

const openModal = () => {
  editInfo.value = { ...healthInfo.value }
  showModal.value = true
  errorMessage.value = ''
  successMessage.value = ''
}

const closeModal = () => {
  showModal.value = false
  errorMessage.value = ''
  successMessage.value = ''
}

const saveHealthInfo = async () => {
  loading.value = true
  errorMessage.value = ''
  successMessage.value = ''
  
  try {
    await userApi.updateUserHealth(editInfo.value)
    healthInfo.value = { ...editInfo.value }
    successMessage.value = '건강정보가 저장되었습니다.'
    
    setTimeout(() => {
      closeModal()
      successMessage.value = ''
    }, 1500)
  } catch (error) {
    errorMessage.value = error.message || '건강정보 저장에 실패했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchHealthInfo()
})
</script>

<template>
  <section class="page health">
    <div v-if="loading && !showModal" class="loading">로딩 중...</div>
    <div v-if="errorMessage && !showModal" class="error-message">{{ errorMessage }}</div>
    
    <div class="section card health-card">
      <div class="health-card__header">
        <div>
          <h2>건강 정보 입력</h2>
          <p class="muted">기본 신체 정보와 질병 정보를 설정해 주세요.</p>
        </div>
        <button type="button" class="primary-button" @click="openModal" :disabled="loading">건강 정보 수정</button>
      </div>
      <div class="health-card__body">
        <div class="health-stat">
          <p class="muted">키 (cm)</p>
          <strong>{{ healthInfo.height || '-' }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">몸무게 (kg)</p>
          <strong>{{ healthInfo.weight || '-' }}</strong>
        </div>
        <div class="health-stat">
          <p class="muted">BMI</p>
          <strong>{{ calculateBMI() }}</strong>
        </div>
      </div>
      
      <div class="health-card__diseases">
        <h3>질병 정보</h3>
        <div class="disease-list">
          <div class="disease-item">
            <span>당뇨병</span>
            <span :class="['status', healthInfo.diabetes ? 'yes' : 'no']">
              {{ healthInfo.diabetes ? '있음' : '없음' }}
            </span>
          </div>
          <div class="disease-item">
            <span>고혈압</span>
            <span :class="['status', healthInfo.highBloodPressure ? 'yes' : 'no']">
              {{ healthInfo.highBloodPressure ? '있음' : '없음' }}
            </span>
          </div>
          <div class="disease-item">
            <span>고지혈증</span>
            <span :class="['status', healthInfo.hyperlipidemia ? 'yes' : 'no']">
              {{ healthInfo.hyperlipidemia ? '있음' : '없음' }}
            </span>
          </div>
          <div class="disease-item">
            <span>신장 질환</span>
            <span :class="['status', healthInfo.kidneyDisease ? 'yes' : 'no']">
              {{ healthInfo.kidneyDisease ? '있음' : '없음' }}
            </span>
          </div>
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
        
        <p v-if="errorMessage" class="error-message">{{ errorMessage }}</p>
        <p v-if="successMessage" class="success-message">{{ successMessage }}</p>
        
        <form class="health-form" @submit.prevent="saveHealthInfo">
          <label>
            <span>키 (cm)</span>
            <input type="number" v-model.number="editInfo.height" min="0" max="300" required />
          </label>
          <label>
            <span>몸무게 (kg)</span>
            <input type="number" v-model.number="editInfo.weight" min="0" max="300" required />
          </label>
          
          <div class="disease-checkboxes">
            <h4>질병 정보</h4>
            <label class="checkbox-label">
              <input type="checkbox" v-model="editInfo.diabetes" />
              <span>당뇨병</span>
            </label>
            <label class="checkbox-label">
              <input type="checkbox" v-model="editInfo.highBloodPressure" />
              <span>고혈압</span>
            </label>
            <label class="checkbox-label">
              <input type="checkbox" v-model="editInfo.hyperlipidemia" />
              <span>고지혈증</span>
            </label>
            <label class="checkbox-label">
              <input type="checkbox" v-model="editInfo.kidneyDisease" />
              <span>신장 질환</span>
            </label>
          </div>
          
          <button type="submit" class="primary-button" :disabled="loading">
            {{ loading ? '저장 중...' : '정보 저장' }}
          </button>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.loading {
  text-align: center;
  padding: 2rem;
  color: #666;
}

.error-message {
  color: #e74c3c;
  background-color: #fadbd8;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.success-message {
  color: #27ae60;
  background-color: #d5f4e6;
  padding: 0.75rem 1rem;
  border-radius: 4px;
  margin-bottom: 1rem;
}

.health-card__diseases {
  margin-top: 2rem;
  padding-top: 2rem;
  border-top: 1px solid #e0e0e0;
}

.health-card__diseases h3 {
  margin-bottom: 1rem;
  font-size: 1.2rem;
}

.disease-list {
  display: grid;
  gap: 0.75rem;
}

.disease-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.disease-item span:first-child {
  font-weight: 500;
}

.status {
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.875rem;
  font-weight: 500;
}

.status.yes {
  background-color: #ffe5e5;
  color: #d63031;
}

.status.no {
  background-color: #d5f4e6;
  color: #27ae60;
}

.disease-checkboxes {
  margin: 1.5rem 0;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 8px;
}

.disease-checkboxes h4 {
  margin-bottom: 1rem;
  font-size: 1rem;
}

.checkbox-label {
  display: flex;
  align-items: center;
  margin-bottom: 0.75rem;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: auto;
  margin-right: 0.5rem;
  cursor: pointer;
}

.checkbox-label span {
  font-weight: 400;
}
</style>
