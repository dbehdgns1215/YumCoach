<template>
    <div class="landing" data-node-id="48:5">
        <TopBarNavigation @signin="goSignin" @signup="goSignup" />

        <main class="content">
            <!-- Hero image from Figma -->
            <section class="gifSection" aria-label="Demo GIF">
                <img class="gif" :src="gifSrc" alt="Demo animation" />
            </section>

            <section class="taglines">
                <Transition mode="out-in" name="rollUp">
                    <div :key="currentIndex" class="line-wrapper">
                        <h2 class="line">
                            {{ lines[currentIndex] }}
                        </h2>
                    </div>
                </Transition>
            </section>

        </main>

        <AppFooter theme="brand" />
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ref, onMounted, onUnmounted } from 'vue'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'
import AppFooter from '@/layout/AppFooter.vue'

const router = useRouter()
import mascotGif from '@/assets/landing.gif'
const gifSrc = mascotGif

const lines = [
    '식사는 잘 챙기셨나요?',
    '어떻게 먹냐에 따라 컨디션이 달라져요!',
    '저희가 도와드릴게요!',
    '오늘 컨디션은 어떠세요?'
]

const currentIndex = ref(0)
let intervalId = null

onMounted(() =>
{
    // 3.6초마다 다음 텍스트로 전환
    intervalId = setInterval(() =>
    {
        currentIndex.value = (currentIndex.value + 1) % lines.length
    }, 3600)
})

onUnmounted(() =>
{
    if (intervalId) clearInterval(intervalId)
})

function goSignin() { router.push('/report') }
function goSignup() { router.push('/log') }
</script>

<style scoped>
.landing {
    background: #fff;
}

.content {
    display: flex;
    flex-direction: column;
    align-items: center;
}

.hero {
    width: 100%;
    max-width: 1160px;
    margin: 40px auto 0;
}

.hero img {
    width: 100%;
    height: auto;
    display: block;
}

.taglines {
    text-align: center;
    color: #4880EE;
    margin: 16px 0 24px;
    padding: 0 0 20px;
    min-height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.line-wrapper {
    width: 100%;
}

.line {
    font-weight: 900;
    font-size: 24px;
    margin: 10px 0;
}

.question {
    margin-top: 24px;
    font-size: 22px;
}

/* Transition animation */
.rollUp-enter-active,
.rollUp-leave-active {
    transition: all 0.2s ease-in-out;
}

.rollUp-enter-from {
    opacity: 0;
    transform: translateY(100%);
}

.rollUp-leave-to {
    opacity: 0;
    transform: translateY(-100%);
}

.rollUp-enter-to,
.rollUp-leave-from {
    opacity: 1;
    transform: translateY(0);
}

.gifSection {
    display: flex;
    justify-content: center;
    padding: 40px 0 10px;
    width: 100%;
}

.gif {
    width: min(720px, 90vw);
    height: auto;
    border-radius: 16px;
}

@media (min-width: 768px) {
    .line {
        font-size: 28px;
    }
}
</style>
