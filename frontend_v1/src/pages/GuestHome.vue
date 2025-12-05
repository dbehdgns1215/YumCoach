<template>
  <section class="page home">
    <div class="home-card">
      <h1>YumYumCoach와 함께 <span class="wave">💪</span></h1>
      <p class="description">
        나만의 건강 목표를 설정하고, 매일 식단을 기록하여<br />
        체계적으로 건강을 관리해 보세요!
      </p>

      <div class="action-group">
        <router-link class="primary-button" to="/login">
          로그인하고 시작하기
        </router-link>
      </div>
    </div>
  </section>
</template>

<script setup>
// 이 페이지는 특별한 로직이 없으므로 script setup은 비워둡니다.
</script>

<style scoped>
/* 일반적인 스타일은 전역 CSS 파일에 포함되어 있으므로, 
  여기서는 특정 로직이 없다면 추가적인 <style>은 필요하지 않습니다. 
  만약 별도의 버튼 스타일이 필요하다면 여기에 추가합니다.
*/

/* 만약 회원가입 버튼을 ghost-button으로 추가한다면, 
  두 버튼 사이의 간격을 위해 action-group에 flex 설정을 고려할 수 있습니다. 
*/
/*
.action-group {
  display: flex;
  flex-direction: column;
  gap: 1rem; 
  align-items: center;
}
*/
</style>
