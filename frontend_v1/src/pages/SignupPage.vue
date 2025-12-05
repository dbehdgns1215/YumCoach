<template>
  <section class="page auth">
    <div class="auth-card">
      <h2>회원가입</h2>
      <p class="muted">YumYumCoach를 위한 프로필을 만들어주세요.</p>
      <form class="form-stack">
        <label>
          <span>이름</span>
          <input type="text" placeholder="홍길동" />
        </label>
        <label>
          <span>이메일</span>
          <input type="email" placeholder="example@email.com" />
        </label>
        <label>
          <span>비밀번호</span>
          <input type="password" placeholder="8자 이상" />
        </label>
        <label>
          <span>비밀번호 확인</span>
          <input type="password" placeholder="비밀번호 확인" />
        </label>
        <button type="button" class="primary-button">계정 생성</button>
      </form>
      <p class="muted">
        이미 계정이 있으신가요?
        <router-link to="/login" class="text-link">로그인</router-link>
      </p>
    </div>
  </section>
</template>
