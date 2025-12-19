<template>
    <header class="topbar" data-name="Header">
        <div class="left">
            <router-link to="/landing">
                <img class="logo" :src="logoSrc" alt="YumCoach" />
            </router-link>
        </div>
        <div class="right" v-if="!hideActions">
            <button class="btn ghost" @click="$emit('signin')">로그인</button>
            <button class="btn primary" @click="$emit('signup')">회원가입</button>
        </div>
    </header>
</template>

<script setup>
import logoSrc from '@/assets/logo.png'

defineProps({
    hideActions: { type: Boolean, default: false }
})

defineEmits(['signin', 'signup'])
</script>

<style scoped>
.topbar {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    background: #fff;
    border-bottom: 1px solid var(--border);
}

.logo {
    height: 50px;
    width: auto;
    cursor: pointer;
}

.left a {
    display: flex;
    align-items: center;
    text-decoration: none;
}

.right {
    display: flex;
    gap: 12px;
}

.btn {
    padding: 8px 16px;
    border-radius: 30px;
    font-weight: 800;
    cursor: pointer;
    font-size: 14px;
    border: 1px solid #c7c7c7;
}

.primary {
    background: #4880EE;
    /* YumBlue from design */
    color: #fff;
}

.ghost {
    background: #fff;
    color: #7b7b7b;
}
</style>
