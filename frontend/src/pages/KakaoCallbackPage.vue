<template>
    <div style="padding:24px;">카카오 로그인 처리 중...</div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()

onMounted(async () =>
{
    const q = route.query.code
    const code = Array.isArray(q) ? q[0] : q

    // 에러 쿼리 파라미터가 있으면 사용자에게 상세 안내
    const error = Array.isArray(route.query.error) ? route.query.error[0] : route.query.error
    const errorDescription = Array.isArray(route.query.error_description)
        ? route.query.error_description[0]
        : route.query.error_description

    if (!code) {
        console.error('[kakao callback] query:', window.location.search)
        const msg = errorDescription || error || '인가 코드가 없어요. 다시 시도해주세요.'
        alert(msg)
        return router.replace('/login')
    }

    try {
        const auth = useAuthStore()

        await auth.loginWithKakaoCode({
            code,
            redirectUri: import.meta.env.VITE_KAKAO_REDIRECT_URI,
        })

        // ✅ 토큰/쿠키 기반 로그인 모두를 위해 상태 확정(선택이지만 추천)
        await auth.checkAuth()

        router.replace('/log')
    } catch (e) {
        alert(e?.response?.data?.message || e?.message || '카카오 로그인 실패')
        router.replace('/login')
    }
})
</script>
