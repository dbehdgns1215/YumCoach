<template>
  <button :type="type" class="btn" :class="variant" :disabled="disabled" @click="$emit('click')">
    <slot />
  </button>
</template>

<script setup>
const props = defineProps({
  variant: { type: String, default: 'primary' }, // primary | secondary | ghost
  disabled: { type: Boolean, default: false },
  type: { type: String, default: 'button' },
})
defineEmits(['click'])
</script>

<style scoped>
.btn{
  width: 100%;
  border: 0;
  border-radius: var(--r-btn);
  padding: 14px 16px;
  font-weight: 700;
  font-size: 13px;
  cursor: pointer;
}
.primary{ background: var(--primary); color: #fff; }
.secondary{ background: var(--primary-soft); color: var(--primary); }
.ghost{ background: transparent; color: var(--text); border:1px solid var(--border); }
.btn:disabled{ opacity:.55; cursor:not-allowed; }
</style>
