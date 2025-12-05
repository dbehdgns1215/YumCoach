<template>
  <section class="page auth">
    <div class="auth-card">
      <h2>로그인</h2>
      <p class="muted">계정을 입력하고 서비스를 이용하세요.</p>
      <form class="form-stack">
        <label>
          <span>이메일</span>
          <input type="email" placeholder="example@email.com" />
        </label>
        <label>
          <span>비밀번호</span>
          <input type="password" placeholder="••••••••" />
        </label>
        <button type="button" class="primary-button">로그인</button>
      </form>
      <p class="muted">
        계정이 없으신가요?
        <router-link to="/signup" class="text-link">회원가입</router-link>
      </p>
    </div>
  </section>
</template>
