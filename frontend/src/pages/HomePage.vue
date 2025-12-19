<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="오늘" subtitle="오늘은 가볍게 체크해요" footerTheme="brand" @primary="goLog">
        <StubPlaceholder text="Home (stub)" />
    </AppShell>
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
const router = useRouter()
function goLog() { router.push('/log') }
</script>
