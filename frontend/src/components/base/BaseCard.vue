<template>
  <section class="card" :class="{ clickable }" @click="onClick">
    <header v-if="$slots.header" class="header">
      <slot name="header" />
    </header>
    <div class="content">
      <slot />
    </div>
    <footer v-if="$slots.footer" class="footer">
      <slot name="footer" />
    </footer>
  </section>
</template>

<script setup>
const props = defineProps({
  clickable: { type: Boolean, default: false },
})
const emit = defineEmits(['click'])

function onClick() {
  if (props.clickable) emit('click')
}
</script>

<style scoped>
.card{
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: var(--r-card);
  box-shadow: var(--shadow);
  padding: var(--space-4);
}
.clickable{ cursor:pointer; }
.header{ margin-bottom: var(--space-3); }
.footer{ margin-top: var(--space-3); }
</style>
