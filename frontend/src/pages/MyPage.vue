<template>
    <TopBarNavigation />
    <AppShell title="마이페이지" subtitle="나의 정보를 관리해요" footerTheme="brand" @primary="noop">
        <div class="my-page">
            <BaseCard>
                <template #header>
                    <div class="card-title">프로필</div>
                </template>
                <MyPageForm />
            </BaseCard>
        </div>
    </AppShell>
</template>

<script setup>
import AppShell from '@/layout/AppShell.vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import BaseCard from '@/components/base/BaseCard.vue'
import MyPageForm from '@/components/mypage/MyPageForm.vue'

function noop() { }
</script>
