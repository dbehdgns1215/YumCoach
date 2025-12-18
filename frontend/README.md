# YumCoach Frontend

Vue 3 + Vite 기반의 식단 관리 애플리케이션 프론트엔드입니다.

## 디렉토리 구조

```
src/
├── components/       # Vue 컴포넌트 (.vue 파일)
│   ├── base/        # 기본 재사용 컴포넌트 (Button, Card 등)
│   ├── log/         # 기록 페이지 전용 컴포넌트
│   ├── report/      # 리포트 페이지 전용 컴포넌트
│   └── paywall/     # 결제 관련 컴포넌트
├── composables/     # 재사용 가능한 로직 함수 (.js 파일)
│   └── useResponsive.js  # 반응형 레이아웃 로직
├── layout/          # 레이아웃 컴포넌트
│   └── AppShell.vue # 메인 레이아웃 (사이드바, 탭바 등)
├── pages/           # 페이지 컴포넌트 (라우팅 대상)
│   ├── HomePage.vue
│   ├── LogPage.vue
│   ├── ReportPage.vue
│   └── CoachPage.vue
├── router/          # Vue Router 설정
├── styles/          # 전역 스타일
│   ├── tokens.css   # 디자인 토큰 (색상, 간격 등)
│   └── utils.css    # CSS 유틸리티 클래스
└── utils/           # 유틸리티 함수
    ├── date.js      # 날짜 관련 함수
    └── nutrition.js # 영양소 계산 함수
```

## 개발 가이드

### Components vs Composables

- **components**: UI를 렌더링하는 Vue 컴포넌트 파일
  - `<template>`, `<script>`, `<style>` 포함
  - 예: `BaseButton.vue`, `MealSection.vue`
- **composables**: 재사용 가능한 로직을 추출한 JavaScript 함수
  - Vue Composition API를 활용한 상태/로직 관리
  - `use-` prefix 사용 권장
  - UI 렌더링 없이 로직만 제공
  - 예: `useResponsive()`, `useAuth()`

### Utils vs Composables

- **utils**: 순수 함수, Vue 의존성 없음
  - 예: `formatDate()`, `calculateNutrition()`
- **composables**: Vue의 reactivity를 활용하는 함수
  - ref, computed, onMounted 등 사용
  - 예: `useResponsive()`, `useFetch()`

## 개발 시작

```bash
npm install
npm run dev
```

## 기술 스택

- Vue 3 (Composition API)
- Vite
- Vue Router

---
