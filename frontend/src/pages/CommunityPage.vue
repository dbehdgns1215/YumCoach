<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="커뮤니티" subtitle="함께 이야기 나눠요" @primary="noop">
        <StubPlaceholder text="Community (stub)" />
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'

function noop() { }
</script>
