<template>
    <TopBarNavigation />
    <PacmanMini />
</template>

<script setup>
import { useRouter } from 'vue-router'
import AppShell from '@/layout/AppShell.vue'
import StubPlaceholder from '@/components/base/StubPlaceholder.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import PacmanMini from "@/pages/PacmanMini.vue";
const router = useRouter()
function goLog() { router.push('/log') }
</script>
