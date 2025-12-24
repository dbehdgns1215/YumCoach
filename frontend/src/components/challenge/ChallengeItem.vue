<template>
  <div class="challenge-item">
    <input type="checkbox" v-model="localDone" @change="toggleDone" />
    <span :class="{done: localDone}">{{ item.text }}</span>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
const props = defineProps({ item: Object })
const emit = defineEmits(['update'])

const localDone = ref(!!props.item.done)
watch(() => props.item.done, v => localDone.value = !!v)
function toggleDone() {
  emit('update', { ...props.item, done: localDone.value })
}
</script>

<style scoped>
.challenge-item { display:flex; gap:12px; align-items:center; padding:8px 0; font-size:14px }
.challenge-item input[type="checkbox"]{width:18px;height:18px;border-radius:4px;border:1px solid var(--border)}
.challenge-item .done { text-decoration: line-through; color:var(--muted) }
</style>
