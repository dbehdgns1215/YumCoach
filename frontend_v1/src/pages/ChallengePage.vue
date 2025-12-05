<script setup>
import { ref } from "vue";

const challengeTypes = [
  "단백질 섭취량 (g)",
  "칼로리 제한",
  "탄수화물 섭취량 (g)",
  "운동 시간 (분)",
];

const ongoingChallenges = [
  {
    title: "한 달간 단백질 충분히 먹기",
    type: "단백질 섭취량",
    current: 870,
    goal: 3000,
    progress: 29,
    dailyGoal: "하루 평균 100g 단백질 섭취하기",
    start: "2025-09-26",
    end: "2025-10-25",
    remaining: 29,
  },
  {
    title: "칼로리 제한 챌린지",
    type: "칼로리 제한",
    current: 1900,
    goal: 20000,
    progress: 10,
    dailyGoal: "하루 2000 kcal 이하 유지",
    start: "2025-09-26",
    end: "2025-10-09",
    remaining: 13,
  },
  {
    title: "탄수화물 줄여 보기",
    type: "탄수화물 섭취량",
    current: 120,
    goal: 4200,
    progress: 3,
    dailyGoal: "정제 탄수화물 줄이기",
    start: "2025-09-16",
    end: "2025-10-05",
    remaining: 9,
  },
];

const finishedChallenges = [];
const showModal = ref(false);

const openModal = () => {
  showModal.value = true;
};
const closeModal = () => {
  showModal.value = false;
};
</script>

<template>
  <section class="page challenge">
    <div class="section card challenge-hero">
      <div>
        <h2>새로운 챌린지 만들기</h2>
        <p class="muted">식단/운동 목표를 정하고 진행을 추적하세요.</p>
      </div>
      <button type="button" class="primary-button" @click="openModal">
        챌린지 추가하기
      </button>
    </div>

    <div class="section card">
      <div class="section-header">
        <h3>진행 중인 챌린지</h3>
        <div class="section-header__actions">
          <button type="button" class="ghost-button">PDF로 내보내기</button>
        </div>
      </div>
      <div class="challenge-list">
        <article
          class="challenge-item"
          v-for="item in ongoingChallenges"
          :key="item.title"
        >
          <header>
            <div>
              <h4>{{ item.title }} · {{ item.type }}</h4>
              <p class="muted">
                {{ item.current }} / {{ item.goal }} ({{ item.progress }}%)
              </p>
            </div>
            <span class="badge">남은 {{ item.remaining }}일</span>
          </header>
          <div class="progress">
            <div
              class="progress-bar"
              :style="{ width: item.progress + '%' }"
            ></div>
          </div>
          <p class="muted">{{ item.dailyGoal }}</p>
          <div class="challenge-actions">
            <button type="button" class="ghost-button">기록 추가</button>
            <button type="button" class="success-button">완료</button>
            <button type="button" class="danger-button">삭제</button>
          </div>
          <footer class="muted">{{ item.start }} ~ {{ item.end }}</footer>
        </article>
      </div>
    </div>

    <div class="section card">
      <h3>완료/종료된 챌린지</h3>
      <p class="muted">완료/종료된 챌린지가 없습니다.</p>
    </div>
    <div v-if="showModal" class="modal-backdrop" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>새로운 챌린지 만들기</h3>
          <button
            type="button"
            class="modal-close"
            @click="closeModal"
            aria-label="닫기"
          >
            ×
          </button>
        </div>
        <p class="muted">식단/운동 목표를 정하고 진행을 추적하세요.</p>
        <div class="challenge-form">
          <label>
            <span>챌린지 제목</span>
            <input type="text" placeholder="예) 한 달간 단백질 100g" />
          </label>
          <label>
            <span>챌린지 유형</span>
            <select>
              <option v-for="type in challengeTypes" :key="type">
                {{ type }}
              </option>
            </select>
          </label>
          <div class="form-row">
            <label>
              <span>목표량</span>
              <input type="number" placeholder="예) 3000" />
              <small>총 누적 목표 (기간 전체 기준)</small>
            </label>
            <label>
              <span>시작일</span>
              <input type="date" />
            </label>
            <label>
              <span>기간(일)</span>
              <input type="number" placeholder="30" />
            </label>
          </div>
          <label>
            <span>설명</span>
            <textarea
              rows="3"
              placeholder="목표 이유, 규칙 등을 적어주세요."
            ></textarea>
          </label>
          <button type="button" class="primary-button">챌린지 생성</button>
        </div>
      </div>
    </div>
  </section>
</template>
