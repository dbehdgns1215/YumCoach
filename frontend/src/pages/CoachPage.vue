<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="코치" subtitle="질문하면 바로 요약해줘요" footerTheme="brand" @primary="noop">
        <StubPlaceholder text="Coach (stub)" />
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
function noop() { }
</script>
