<template>
  <BaseCard>
    <template #header>
      <div class="h">
        <div class="icon">{{ icon }}</div>
        <div class="t">
          <div class="title">{{ title }}</div>
          <div v-if="subtitle" class="subtitle">{{ subtitle }}</div>
        </div>
      </div>
    </template>
    <div class="body">{{ body }}</div>
  </BaseCard>
</template>

<script setup>
import { computed } from 'vue'
import BaseCard from '@/components/base/BaseCard.vue'

const props = defineProps({
  kind: { type: String, required: true }, // good | warn | keep
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  body: { type: String, required: true },
})

const icon = computed(() => {
  const map = { good: '✅', warn: '⚠️', keep: '⭐' }
  return map[props.kind] || '✨'
})
</script>

<style scoped>
.h{ display:flex; gap:var(--space-2); align-items:flex-start; }
.icon{
  width:32px; height:32px;
  border-radius:10px;
  display:flex; align-items:center; justify-content:center;
  font-size:16px;
  background: var(--bg);
  border:1px solid var(--border);
}
.title{ font-weight:900; font-size:14px; }
.subtitle{ color:var(--muted); font-size:12px; margin-top:3px; }
.body{ color:var(--text); font-size:14px; line-height:1.45; }
</style>
