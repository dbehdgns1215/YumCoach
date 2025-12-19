<template>
    <section class="feature" :class="[theme, alignClass]">
        <div class="left">
            <div class="badge" v-if="badge">
                <span class="dot" aria-hidden="true">●</span>
                <span>{{ badge }}</span>
            </div>
            <h2 class="title">{{ title }}</h2>
            <p class="desc">{{ description }}</p>
            <a v-if="linkText && linkHref" :href="linkHref" class="desc-link">{{ linkText }}</a>
        </div>
        <div class="card">
            <div class="icon" :style="{ backgroundColor: iconBgColor }" v-if="iconSrc || icon">
                <img v-if="iconSrc" :src="iconSrc" :alt="iconAlt || 'icon'" />
                <span v-else>{{ icon }}</span>
            </div>
            <div class="card-title">{{ cardTitle }}</div>
            <div class="card-sub" v-if="cardSub">{{ cardSub }}</div>
            <div class="card-link" v-if="cta">{{ cta }}</div>
        </div>
    </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    badge: { type: String, default: '' },
    title: { type: String, required: true },
    description: { type: String, required: true },
    cardTitle: { type: String, required: true },
    cardSub: { type: String, default: '' },
    cta: { type: String, default: '' },
    icon: { type: String, default: '✓' },
    iconSrc: { type: String, default: '' },
    iconAlt: { type: String, default: '' },
    iconBgColor: { type: String, default: '#e6f0ff' },
    linkText: { type: String, default: '' },
    linkHref: { type: String, default: '' },
    theme: { type: String, default: 'light' }, // light | brand
    align: { type: String, default: 'right' }, // right | left
})

const alignClass = computed(() => (props.align === 'left' ? 'align-left' : 'align-right'))
</script>

<style scoped>
.feature {
    display: grid;
    grid-template-columns: 1fr;
    grid-template-areas:
        "left"
        "card";
    gap: 24px;
    align-items: center;
    background: #f8fafc;
    border-radius: 20px;
    padding: 28px;
}

.feature.brand {
    background: #f3f6ff;
}

.feature.align-left {
    grid-template-columns: 1fr;
}

.feature.align-right {
    grid-template-columns: 1fr;
}

.left {
    display: flex;
    flex-direction: column;
    gap: 10px;
    grid-area: left;
}

.badge {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #2563eb;
    font-weight: 800;
    font-size: 13px;
}

.dot {
    font-size: 10px;
}

.title {
    font-weight: 900;
    font-size: 24px;
    color: #1f2937;
    line-height: 1.35;
}

.desc {
    margin: 0;
    color: #4b5563;
    font-size: 15px;
    line-height: 1.6;
}

.desc-link {
    display: inline-flex;
    align-items: center;
    margin-top: 12px;
    color: #2563eb;
    font-weight: 700;
    font-size: 14px;
    text-decoration: none;
    transition: color 0.2s;
}

.desc-link:hover {
    color: #1d4ed8;
    text-decoration: underline;
}

.card {
    background: #fff;
    border-radius: 18px;
    padding: 22px 20px;
    box-shadow: 0 18px 30px rgba(0, 0, 0, 0.06);
    text-align: center;
    min-width: 230px;
    grid-area: card;
}

.icon {
    height: 10rem;
    width: 10rem;
    margin: 0 auto 12px;
    border-radius: 50%;
    background: #e6f0ff;
    color: #2563eb;
    display: grid;
    place-items: center;
    font-size: 22px;
    font-weight: 900;
}

.icon img {
    width: 60%;
    height: 60%;
    object-fit: contain;
}

.card-title {
    font-weight: 800;
    color: #1f2937;
    font-size: 16px;
}

.card-sub {
    color: #6b7280;
    font-size: 14px;
    margin-top: 6px;
}

.card-link {
    color: #2563eb;
    font-weight: 800;
    font-size: 13px;
    margin-top: 10px;
}

@media (min-width: 960px) {
    .feature {
        grid-template-columns: 1.1fr 0.9fr;
        grid-template-areas: "left card";
        padding: 40px;
    }

    .feature.align-left {
        grid-template-columns: 0.9fr 1.1fr;
        grid-template-areas: "card left";
    }

    .feature.align-right {
        grid-template-columns: 1.1fr 0.9fr;
        grid-template-areas: "left card";
    }

    .title {
        font-size: 28px;
    }

    .desc {
        font-size: 16px;
    }
}
</style>
