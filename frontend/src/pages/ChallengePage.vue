<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="챌린지" subtitle="도전해요!" @primary="noop">
        <StubPlaceholder text="Challenge (stub)" />
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'

function noop() { }
</script>
