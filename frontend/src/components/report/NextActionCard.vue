<template>
  <BaseCard>
    <template #header>
      <div class="title">{{ title }}</div>
    </template>

    <div class="action">{{ actionText }}</div>

    <template #footer>
      <BaseButton variant="primary" @click="$emit('save')">
        {{ buttonLabel }}
      </BaseButton>
    </template>
  </BaseCard>
</template>

<script setup>
import BaseCard from '@/components/base/BaseCard.vue'
import BaseButton from '@/components/base/BaseButton.vue'

defineProps({
  actionText: { type: String, required: true },
  title: { type: String, required: false, default: '내일은 이것 하나만 해봐요' },
  buttonLabel: { type: String, required: false, default: '내일 플랜 저장' },
})
defineEmits(['save'])
</script>

<style scoped>
.title{ font-weight:900; font-size:14px; }
.action{ font-size:14px; color:var(--muted); line-height:1.45; white-space: pre-wrap; }
</style>
