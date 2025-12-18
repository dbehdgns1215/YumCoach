<template>
    <div class="stub">
        <slot>{{ text }}</slot>
    </div>
</template>

<script setup>
defineProps({
    text: { type: String, default: 'Coming soon...' },
})
</script>

<style scoped>
.stub {
    background: var(--surface);
    border: 1px solid var(--border);
    border-radius: var(--r-card);
    box-shadow: var(--shadow);
    padding: 20px;
    color: var(--muted);
    font-weight: 900;
}
</style>
