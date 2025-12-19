<template>
    <footer class="app-footer" :class="theme">
        <div class="inner">
            <div class="brand">YumCoach</div>
            <div class="links">
                <a href="#" @click.prevent>SSAFY 14기</a>
                <span class="dot">•</span>
                <a href="#" @click.prevent>대전 6반</a>
                <span class="dot">•</span>
                <a href="#" @click.prevent>김아린 유동훈</a>
                <a href="https://github.com/dbehdgns1215/YumCoach" target="_blank" rel="noopener noreferrer">
                    <img :src="gitIcon" alt="GitHub" class="github-icon" />
                </a>
            </div>
            <div class="copy">© {{ new Date().getFullYear() }} YumCoach. All rights reserved.</div>
        </div>
    </footer>
</template>

<script setup>
import gitIcon from '@/assets/git.png'

const props = defineProps({
    theme: { type: String, default: 'light' }, // light | brand
})
</script>

<style scoped>
.app-footer {
    border-top: 1px solid var(--border);
    background: var(--surface);
}

.app-footer.brand {
    background: #4880EE;
    border-top-color: rgba(255, 255, 255, .2);
    color: #fff;
}

.inner {
    max-width: var(--container);
    margin: 0 auto;
    padding: 24px var(--space-4);
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.brand {
    font-weight: 900;
}

.links {
    display: flex;
    gap: 10px;
    align-items: center;
    color: var(--muted);
    font-size: 12px;
}

.app-footer.brand .links {
    color: rgba(255, 255, 255, .9);
}

.dot {
    opacity: .6;
}

.copy {
    color: var(--muted);
    font-size: 12px;
}

.app-footer.brand .copy {
    color: rgba(255, 255, 255, .85);
}

.github-link {
    margin-top: 4px;
}

.github-icon {
    width: 24px;
    height: 24px;
}
</style>
