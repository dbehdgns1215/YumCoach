<template>
    <div class="landing" data-node-id="48:5">
        <TopBarNavigation @signin="goSignin" @signup="goSignup" />

        <main class="content">
            <!-- Hero image from Figma -->
            <section class="gifSection" aria-label="Demo GIF">
                <img class="gif" :src="gifSrc" alt="Demo animation" />
            </section>

            <section class="taglines">
                <h2>식사는 잘 챙기셨나요?</h2>
                <h2>어떻게 먹냐에 따라 컨디션이 달라져요!</h2>
                <h2>저희가 도와드릴게요!</h2>
                <h3 class="question">오늘 컨디션은 어떠세요?</h3>
            </section>


        </main>

        <footer class="footer" />
    </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import TopBarNavigation from '@/components/landing/TopBarNavigation.vue'

const router = useRouter()
import mascotGif from '@/assets/landing.gif'
const gifSrc = mascotGif

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
}

.taglines h2 {
    font-weight: 900;
    font-size: 24px;
    margin: 10px 0;
}

.question {
    margin-top: 24px;
    font-size: 22px;
}

.gifSection {
    display: flex;
    justify-content: center;
    padding: 40px 0 80px;
    width: 100%;
}

.gif {
    width: min(720px, 90vw);
    height: auto;
    border-radius: 16px;
}

.footer {
    height: 200px;
    background: #4880EE;
}

@media (min-width: 768px) {
    .taglines h2 {
        font-size: 28px;
    }

    .question {
        font-size: 26px;
    }
}
</style>
