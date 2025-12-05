<script setup>
import WeeklyStatsChart from '../components/WeeklyStatsChart.vue'

const todayRecords = [
  { meal: '간식', name: '요거트', calories: 120, protein: 8, carbs: 15, fat: 3 },
  { meal: '점심', name: '닭가슴살 150g + 밥', calories: 520, protein: 45, carbs: 60, fat: 8 },
  { meal: '아침', name: '오트밀 60g + 우유', calories: 300, protein: 15, carbs: 45, fat: 6 },
]

const targets = { calories: 2300, protein: 110, carbs: 250, fat: 65 }

const totals = todayRecords.reduce(
  (acc, entry) => {
    acc.calories += entry.calories
    acc.protein += entry.protein
    acc.carbs += entry.carbs
    acc.fat += entry.fat
    return acc
  },
  { calories: 0, protein: 0, carbs: 0, fat: 0 }
)

const formatPercent = (value, target) => {
  if (!target) return 0
  return Math.min(100, Math.round((value / target) * 100))
}

const labelMap = {
  calories: '칼로리',
  protein: '단백질',
  carbs: '탄수화물',
  fat: '지방',
}

const totalFor = (key) => totals[key]
const percentFor = (key) => formatPercent(totalFor(key), targets[key])

const historyRecords = [
  { date: '2025-09-25', meal: '점심', name: '닭가슴살 + 채소', calories: 530, protein: 48, carbs: 55, fat: 7 },
  { date: '2025-09-24', meal: '아침', name: '토스트 + 달걀', calories: 400, protein: 22, carbs: 40, fat: 12 },
]

const weeklyStats = {
  labels: ['2025-09-20', '2025-09-22', '2025-09-24', '2025-09-25', '2025-09-26'],
  datasets: [
    {
      label: '칼로리(kcal)',
      data: [0, 0, 0, 300, 940],
      borderColor: '#0066ff',
      backgroundColor: 'rgba(0, 102, 255, 0.15)',
      fill: false,
      tension: 0.4,
      pointRadius: 4,
      borderWidth: 2,
    },
    {
      label: '단백질(g)',
      data: [0, 0, 0, 20, 68],
      borderColor: '#f5678d',
      backgroundColor: 'rgba(245, 103, 141, 0.12)',
      fill: false,
      tension: 0.4,
      pointRadius: 4,
      borderWidth: 2,
    },
    {
      label: '탄수화물(g)',
      data: [0, 0, 0, 60, 120],
      borderColor: '#f4a261',
      backgroundColor: 'rgba(244, 162, 97, 0.12)',
      fill: false,
      tension: 0.4,
      pointRadius: 4,
      borderWidth: 2,
    },
    {
      label: '지방(g)',
      data: [0, 0, 0, 8, 17],
      borderColor: '#f4c542',
      backgroundColor: 'rgba(244, 197, 66, 0.12)',
      fill: false,
      tension: 0.4,
      pointRadius: 4,
      borderWidth: 2,
    },
  ],
}
</script>

<template>
  <section class="page diet-record">
    <div class="section card">
      <h2>오늘의 섭취 요약 (목표 대비)</h2>
      <div class="macro-grid">
        <div class="macro-card" v-for="(value, key) in targets" :key="key">
          <p class="muted">{{ labelMap[key] }}</p>
          <h3>{{ totalFor(key) }} / {{ value }}</h3>
          <p class="muted">{{ percentFor(key) }}%</p>
          <div class="progress">
            <div class="progress-bar" :style="{ width: percentFor(key) + '%' }"></div>
          </div>
        </div>
      </div>
    </div>

    <div class="section card two-column">
      <div>
        <div class="weekly-snapshot">
          <div class="weekly-snapshot__text">
            <h3>주간 통계 스냅샷</h3>
            <p class="muted">평균 섭취 칼로리 · 단백질 · 탄수화물 · 지방</p>
            <ul class="stat-list">
              <li>평균 섭취 칼로리: — kcal</li>
              <li>평균 단백질: — g</li>
              <li>평균 탄수화물: — g</li>
              <li>평균 지방: — g</li>
            </ul>
          </div>
          <div class="weekly-snapshot__chart">
            <WeeklyStatsChart :labels="weeklyStats.labels" :datasets="weeklyStats.datasets" />
          </div>
        </div>
      </div>
      <div>
        <h3>영양 목표</h3>
        <div class="goal-grid">
          <div>
            <p class="muted">칼로리</p>
            <strong>{{ targets.calories }}</strong>
          </div>
          <div>
            <p class="muted">단백질(g)</p>
            <strong>{{ targets.protein }}</strong>
          </div>
          <div>
            <p class="muted">탄수화물(g)</p>
            <strong>{{ targets.carbs }}</strong>
          </div>
          <div>
            <p class="muted">지방(g)</p>
            <strong>{{ targets.fat }}</strong>
          </div>
        </div>
      </div>
    </div>

    <div class="section card">
      <h3>오늘의 식단 상세</h3>
      <p class="muted">기록한 항목이 식사 유형별로 표로 나옵니다.</p>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>식사</th>
              <th>음식</th>
              <th>칼로리</th>
              <th>단백질</th>
              <th>탄수화물</th>
              <th>지방</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="entry in todayRecords" :key="entry.name">
              <td>{{ entry.meal }}</td>
              <td>{{ entry.name }}</td>
              <td>{{ entry.calories }}</td>
              <td>{{ entry.protein }}</td>
              <td>{{ entry.carbs }}</td>
              <td>{{ entry.fat }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="section card">
      <h3>식단 기록하기</h3>
      <div class="entry-form">
        <label class="entry-field entry-field--date">
          <span>날짜</span>
          <input type="date" />
        </label>
        <label class="entry-field entry-field--meal">
          <span>식사 유형</span>
          <select>
            <option>아침</option>
            <option>점심</option>
            <option>저녁</option>
            <option>간식</option>
          </select>
        </label>
        <label class="entry-field entry-field--food">
          <span>음식</span>
          <input type="text" placeholder="예) 닭가슴살 100g" />
        </label>
        <label class="entry-field entry-field--calories">
          <span>칼로리</span>
          <input type="number" placeholder="0" />
        </label>
        <label class="entry-field entry-field--protein">
          <span>단백질(g)</span>
          <input type="number" placeholder="0" />
        </label>
        <label class="entry-field entry-field--carbs">
          <span>탄수화물(g)</span>
          <input type="number" placeholder="0" />
        </label>
        <label class="entry-field entry-field--fat">
          <span>지방(g)</span>
          <input type="number" placeholder="0" />
        </label>
        <div class="entry-field entry-field--actions">
          <span class="sr-only">행동</span>
          <button type="button" class="primary-button">추가</button>
        </div>
      </div>
    </div>

    <div class="section card">
      <div class="section-header">
        <h3>식단 기록 목록</h3>
        <button type="button" class="ghost-button">전체 삭제</button>
      </div>
      <div class="table-wrapper">
        <table>
          <thead>
            <tr>
              <th>날짜</th>
              <th>식사</th>
              <th>음식</th>
              <th>칼로리</th>
              <th>단백질</th>
              <th>탄수화물</th>
              <th>지방</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="record in historyRecords" :key="record.date + record.name">
              <td>{{ record.date }}</td>
              <td>{{ record.meal }}</td>
              <td>{{ record.name }}</td>
              <td>{{ record.calories }}</td>
              <td>{{ record.protein }}</td>
              <td>{{ record.carbs }}</td>
              <td>{{ record.fat }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </section>
</template>
