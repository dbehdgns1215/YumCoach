<template>
  <BaseCard>
    <template #header>
      <div class="h">
        <div class="avatar">🤖</div>
        <div>
          <div class="title">AI 코치의 한마디</div>
          <div class="sub">가볍게 방향만 잡아줄게요</div>
        </div>
      </div>
    </template>
    <div class="msg">{{ message }}</div>
  </BaseCard>
</template>

<script setup>
import BaseCard from '@/components/base/BaseCard.vue'

defineProps({
  message: { type: String, required: true },
})
</script>

<style scoped>
.h{ display:flex; gap:var(--space-2); align-items:center; }
.avatar{
  width:36px; height:36px; border-radius:14px;
  display:flex; align-items:center; justify-content:center;
  background: var(--primary-soft);
  border:1px solid var(--border);
}
.title{ font-weight:900; font-size:14px; }
.sub{ color:var(--muted); font-size:12px; margin-top:2px; }
.msg{ font-size:14px; line-height:1.45; color:var(--text); }
</style>
