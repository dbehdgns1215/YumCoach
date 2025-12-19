<template>
    <TopBarNavigation :isLoggedIn="true" />
    <AppShell title="마이페이지" subtitle="나의 정보를 관리해요" @primary="noop">
        <StubPlaceholder text="MyPage (stub)" />
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'

function noop() { }
</script>
