<template>
  <div class="tabs">
    <button :class="['tab', { active: mode==='daily' }]" @click="$emit('update:mode','daily')">일간</button>
    <button :class="['tab', { active: mode==='weekly' }]" @click="$emit('update:mode','weekly')">주간</button>
  </div>
</template>

<script setup>
const props = defineProps({ mode: { type: String, default: 'daily' } })
</script>

<style scoped>
.tabs{ display:inline-flex; gap:8px; background:transparent; }
.tab{ padding:8px 14px; border-radius:999px; border:1px solid transparent; cursor:pointer; font-weight:800; background:transparent }
.tab.active{ background: linear-gradient(90deg,#2f6bff,#3fb0ff); color:white; box-shadow:0 6px 18px rgba(47,107,255,.18)}
.tab:not(.active){ border:1px solid rgba(16,24,40,.06); color:var(--muted) }
</style>
