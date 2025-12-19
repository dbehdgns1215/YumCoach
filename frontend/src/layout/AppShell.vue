<template>
  <div class="shell">
    <div class="main">
      <header class="topbar">
        <div class="left">
          <div class="pageTitle">{{ title }}</div>
          <div class="pageSub" v-if="subtitle">{{ subtitle }}</div>
        </div>
        <div class="right">
          <button class="pill" @click="$emit('primary')">식단 추가</button>
        </div>
      </header>

      <div class="content">
        <slot />

      </div>
      <AppFooter :theme="footerTheme" />
      <nav class="tabs" v-if="!isDesktop">
        <RouterLink v-for="item in items" :key="item.key" class="tab" :class="{ active: activeKey === item.key }"
          :to="item.path">
          {{ item.label }}
        </RouterLink>
      </nav>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import { useResponsive } from '@/composables/useResponsive'
import AppFooter from '@/layout/AppFooter.vue'

defineProps({
  title: { type: String, required: true },
  subtitle: { type: String, default: '' },
  footerTheme: { type: String, default: 'light' },
})
defineEmits(['primary'])

const route = useRoute()
const { isDesktop } = useResponsive(1200)

const items = computed(() => route.meta?.navItems || [])
const activeKey = computed(() => route.meta?.navKey || '')
</script>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
}

.brand {
  font-weight: 900;
  font-size: 18px;
  margin-bottom: var(--space-4);
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.item {
  padding: 10px 12px;
  border-radius: 12px;
  color: var(--muted);
  font-weight: 800;
}

.item.active {
  background: var(--primary-soft);
  color: var(--primary);
}

.main {
  flex: 1;
  min-width: 0;
}

.topbar {
  padding: var(--space-4);
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: var(--container);
  margin: 0 auto;
}

.pageTitle {
  font-weight: 900;
  font-size: 18px;
}

.pageSub {
  color: var(--muted);
  font-size: 12px;
  margin-top: 4px;
}

.pill {
  border: 0;
  background: var(--primary);
  color: #fff;
  padding: 10px 14px;
  border-radius: 999px;
  font-weight: 900;
  cursor: pointer;
}

.content {
  max-width: var(--container);
  margin: 0 auto;
  padding: 0 var(--space-4) 90px;
}

.tabs {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, .92);
  backdrop-filter: blur(8px);
  border-top: 1px solid var(--border);
  display: flex;
  justify-content: space-around;
  padding: 10px 0;
}

.tab {
  color: var(--muted);
  font-weight: 900;
  font-size: 12px;
}

.tab.active {
  color: var(--primary);
}
</style>
