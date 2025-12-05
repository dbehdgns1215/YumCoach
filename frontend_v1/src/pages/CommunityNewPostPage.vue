<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = ref({
  title: '',
  category: '경험',
  body: '',
})

const submitPost = () => {
  alert(`게시물 '${form.value.title}'이(가) 게시되었습니다.`)
  router.push({ name: 'Community' })
}
</script>

<template>
  <section class="page community-new">
    <div class="section card community-new__panel">
      <div class="community-new__header">
        <h2>글쓰기</h2>
      </div>
      <p class="muted">챌린지 경험, 식단, 팁을 자유롭게 공유하세요.</p>
      <form class="community-new__form" @submit.prevent="submitPost">
        <label>
          <span>제목</span>
          <input v-model="form.title" placeholder="제목을 입력하세요" required />
        </label>
        <label>
          <span>카테고리</span>
          <select v-model="form.category">
            <option>경험</option>
            <option>식단</option>
            <option>팁</option>
          </select>
        </label>
        <label>
          <span>내용</span>
          <textarea v-model="form.body" rows="8" placeholder="내용을 입력하세요" required></textarea>
        </label>
        <div class="community-new__actions">
          <button type="button" class="ghost-button" @click="router.back()">취소</button>
          <button type="submit" class="primary-button">등록</button>
        </div>
      </form>
    </div>
  </section>
</template>
