<template>
  <div class="toastRoot" aria-live="polite">
    <div v-for="t in toasts" :key="t.id" :class="['toast', t.type]">{{ t.message }}</div>
  </div>
</template>

<script setup>
import { toRefs } from 'vue'
import { toasts } from '@/lib/toast.js'
</script>

<style scoped>
.toastRoot{ position: fixed; right: 16px; bottom: 20px; display:flex; flex-direction:column; gap:8px; z-index:2000 }
.toast{ padding:10px 14px; border-radius:10px; color:#fff; box-shadow:0 6px 18px rgba(0,0,0,0.12); font-weight:700 }
.toast.error{ background: #d64545 }
.toast.info{ background: #2f6bff }
.toast.success{ background: #22c55e }
</style>
