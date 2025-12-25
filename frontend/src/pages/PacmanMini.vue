<template>
    <div class="wrap" tabindex="0" ref="root" @keydown.prevent="onKeyDown">
        <header class="top">
            <div class="title">
                <span class="badge">🟡</span>
                <div>
                    <div class="h1">Mini Pacman</div>
                    <div class="sub">화살표로 이동 · 점(·)을 다 먹으면 승리!</div>
                </div>
            </div>

            <div class="hud">
                <div class="pill">점수 <b>{{ score }}</b></div>
                <div class="pill">남은 점 <b>{{ pelletsLeft }}</b></div>
                <button class="btn" @click="reset()">리셋</button>
            </div>
        </header>

        <div class="stage">
            <div class="board" :style="{ gridTemplateColumns: `repeat(${W}, var(--cell))` }">
                <template v-for="(cell, i) in flat" :key="i">
                    <div class="cell" :class="cellClass(cell, i)">
                        <span v-if="cell === 'pellet'" class="pellet">·</span>
                    </div>
                </template>

                <!-- Entities -->
                <div class="entity pac" :style="entityStyle(pac.x, pac.y)">
                    <div class="pacman" :class="['dir-' + pac.dir, pacChomp ? 'chomp' : '']"></div>
                </div>

                <div v-for="g in ghosts" :key="g.id" class="entity ghost" :style="entityStyle(g.x, g.y)">
                    <div class="ghostBody" :class="g.skin">
                        <div class="eyes">
                            <span class="eye"></span><span class="eye"></span>
                        </div>
                        <div class="feet">
                            <span></span><span></span><span></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="overlay" v-if="state !== 'play'">
                <div class="card">
                    <div class="big" v-if="state === 'win'">🎉 승리!</div>
                    <div class="big" v-else>💥 잡혔어!</div>
                    <div class="msg">
                        <span>점수: <b>{{ score }}</b></span>
                        <span class="dot">·</span>
                        <span>리셋해서 다시 해보자</span>
                    </div>
                    <button class="btn primary" @click="reset()">다시 시작</button>
                </div>
            </div>
        </div>

        <footer class="tip">
            <span class="kbd">←</span><span class="kbd">↑</span><span class="kbd">→</span><span class="kbd">↓</span>
            <span class="txt">이동</span>
            <span class="sep">|</span>
            <span class="txt">보드 클릭 후 키 입력</span>
        </footer>
    </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

/**
 * 귀여운 미니 팩맨 (간단 버전)
 * - grid 기반
 * - pellet(점) 다 먹으면 승리
 * - ghost 랜덤 이동 (벽 피함)
 * - ghost와 겹치면 패배
 */

const W = 15;
const H = 11;

const root = ref(null);

const makeBoard = () =>
{
    // 기본 맵: wall / empty / pellet
    // 조금 아기자기하게 벽 배치
    const rows = [
        "###############",
        "#.............#",
        "#.###.###.###.#",
        "#.#...#...#.#.#",
        "#.#.#######.#.#",
        "#...#.....#...#",
        "###.#.###.#.###",
        "#...#...#.#...#",
        "#.###.#.###.###",
        "#.............#",
        "###############",
    ];
    // '.' -> pellet, '#' -> wall
    return rows.map((r) =>
        [...r].map((ch) => (ch === "#" ? "wall" : ch === "." ? "pellet" : "empty"))
    );
};

const board = reactive(makeBoard());

const state = ref("play"); // play | win | lose
const score = ref(0);

const pac = reactive({ x: 1, y: 1, dir: "right" });
const pacChomp = ref(false);

const ghosts = reactive([
    { id: "g1", x: 13, y: 1, dir: "left", skin: "pink" },
    { id: "g2", x: 13, y: 9, dir: "up", skin: "mint" },
]);

const flat = computed(() => board.flat());

const pelletsLeft = computed(() =>
{
    let c = 0;
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) if (board[y][x] === "pellet") c++;
    return c;
});

function isWall(x, y)
{
    return board[y]?.[x] === "wall";
}
function inBounds(x, y)
{
    return x >= 0 && x < W && y >= 0 && y < H;
}

function cellClass(cell, idx)
{
    const x = idx % W;
    const y = Math.floor(idx / W);

    const isHome =
        (x === 1 && y === 1) ||
        ghosts.some((g) => g.x === x && g.y === y) ||
        (x === 13 && y === 9) ||
        (x === 13 && y === 1);

    return {
        wall: cell === "wall",
        empty: cell !== "wall",
        home: isHome,
    };
}

function entityStyle(x, y)
{
    return {
        left: `calc(${x} * var(--cell))`,
        top: `calc(${y} * var(--cell))`,
    };
}

function eatPelletIfAny()
{
    if (board[pac.y][pac.x] === "pellet") {
        board[pac.y][pac.x] = "empty";
        score.value += 10;
    }
    if (pelletsLeft.value === 0) state.value = "win";
}

function checkCollision()
{
    if (ghosts.some((g) => g.x === pac.x && g.y === pac.y)) {
        state.value = "lose";
    }
}

function movePac(dx, dy, dir)
{
    if (state.value !== "play") return;

    pac.dir = dir;
    const nx = pac.x + dx;
    const ny = pac.y + dy;
    if (!inBounds(nx, ny) || isWall(nx, ny)) return;

    pac.x = nx;
    pac.y = ny;

    pacChomp.value = true;
    setTimeout(() => (pacChomp.value = false), 80);

    eatPelletIfAny();
    checkCollision();
}

function onKeyDown(e)
{
    if (state.value !== "play") return;

    switch (e.key) {
        case "ArrowLeft":
            movePac(-1, 0, "left");
            break;
        case "ArrowRight":
            movePac(1, 0, "right");
            break;
        case "ArrowUp":
            movePac(0, -1, "up");
            break;
        case "ArrowDown":
            movePac(0, 1, "down");
            break;
    }
}

function neighbors(x, y)
{
    return [
        { dx: -1, dy: 0, dir: "left" },
        { dx: 1, dy: 0, dir: "right" },
        { dx: 0, dy: -1, dir: "up" },
        { dx: 0, dy: 1, dir: "down" },
    ]
        .map((d) => ({ ...d, x: x + d.dx, y: y + d.dy }))
        .filter((p) => inBounds(p.x, p.y) && !isWall(p.x, p.y));
}

function stepGhost(g)
{
    if (state.value !== "play") return;

    const opts = neighbors(g.x, g.y);
    if (opts.length === 0) return;

    // 되도록 직진 + 가끔 방향 전환 (귀여운 멍청함)
    const straight = opts.find((o) => o.dir === g.dir);
    const turnChance = Math.random();

    let pick;
    if (straight && turnChance > 0.35) {
        pick = straight;
    } else {
        pick = opts[Math.floor(Math.random() * opts.length)];
    }

    g.x = pick.x;
    g.y = pick.y;
    g.dir = pick.dir;

    checkCollision();
}

let ghostTimer = null;

function reset()
{
    // 보드 초기화
    const fresh = makeBoard();
    for (let y = 0; y < H; y++) for (let x = 0; x < W; x++) board[y][x] = fresh[y][x];

    score.value = 0;
    state.value = "play";

    pac.x = 1;
    pac.y = 1;
    pac.dir = "right";

    ghosts[0].x = 13;
    ghosts[0].y = 1;
    ghosts[0].dir = "left";

    ghosts[1].x = 13;
    ghosts[1].y = 9;
    ghosts[1].dir = "up";

    // 시작 칸 점은 바로 먹게
    eatPelletIfAny();

    // 포커스
    requestAnimationFrame(() => root.value?.focus?.());
}

onMounted(() =>
{
    // 시작 시 첫 점 처리 + 키 입력 받기
    eatPelletIfAny();
    root.value?.focus?.();

    ghostTimer = setInterval(() =>
    {
        // 유령 이동은 살짝 느리게
        stepGhost(ghosts[0]);
        if (Math.random() > 0.2) stepGhost(ghosts[1]);
    }, 220);
});

onBeforeUnmount(() =>
{
    if (ghostTimer) clearInterval(ghostTimer);
});
</script>

<style scoped>
.wrap {
    --cell: 28px;
    --bg: #0b1020;
    --panel: rgba(255, 255, 255, 0.06);
    --line: rgba(255, 255, 255, 0.12);
    --text: rgba(255, 255, 255, 0.92);
    --muted: rgba(255, 255, 255, 0.65);

    color: var(--text);
    background: radial-gradient(1200px 500px at 20% 0%, rgba(255, 214, 102, 0.18), transparent 60%),
        radial-gradient(900px 500px at 80% 20%, rgba(112, 255, 220, 0.14), transparent 55%),
        var(--bg);
    border: 1px solid var(--line);
    border-radius: 20px;
    padding: 14px;
    max-width: 560px;
    margin: 18px auto;
    box-shadow: 0 18px 70px rgba(0, 0, 0, 0.45);
    outline: none;
}

.top {
    display: flex;
    gap: 12px;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
}

.title {
    display: flex;
    gap: 10px;
    align-items: center;
}

.badge {
    width: 40px;
    height: 40px;
    display: grid;
    place-items: center;
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid var(--line);
    border-radius: 14px;
    font-size: 20px;
}

.h1 {
    font-weight: 800;
    letter-spacing: 0.2px;
}

.sub {
    font-size: 12px;
    color: var(--muted);
}

.hud {
    display: flex;
    gap: 8px;
    align-items: center;
}

.pill {
    font-size: 12px;
    padding: 8px 10px;
    border-radius: 999px;
    background: var(--panel);
    border: 1px solid var(--line);
    white-space: nowrap;
}

.btn {
    font-size: 12px;
    padding: 8px 10px;
    border-radius: 12px;
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.08);
    color: var(--text);
    cursor: pointer;
}

.btn:hover {
    background: rgba(255, 255, 255, 0.12);
}

.btn.primary {
    background: rgba(255, 214, 102, 0.18);
    border-color: rgba(255, 214, 102, 0.35);
}

.stage {
    position: relative;
    padding: 10px;
    border-radius: 18px;
    background: rgba(0, 0, 0, 0.18);
    border: 1px solid var(--line);
}

.board {
    position: relative;
    display: grid;
    gap: 0px;
    width: calc(var(--cell) * 15);
    height: calc(var(--cell) * 11);
    margin: 0 auto;
    border-radius: 16px;
    overflow: hidden;
    background: rgba(10, 14, 30, 0.55);
}

.cell {
    width: var(--cell);
    height: var(--cell);
    display: grid;
    place-items: center;
    font-size: 18px;
    user-select: none;
}

.cell.wall {
    background: linear-gradient(180deg, rgba(84, 120, 255, 0.35), rgba(84, 120, 255, 0.12));
    border: 1px solid rgba(120, 160, 255, 0.20);
}

.cell.empty {
    background: radial-gradient(circle at 50% 40%, rgba(255, 255, 255, 0.035), transparent 60%);
}

.cell.home {
    box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.04);
}

.pellet {
    font-size: 18px;
    opacity: 0.9;
    transform: translateY(-1px);
    color: rgba(255, 255, 255, 0.85);
}

.entity {
    position: absolute;
    width: var(--cell);
    height: var(--cell);
    transition: left 90ms linear, top 90ms linear;
    pointer-events: none;
}

.pacman {
    width: 100%;
    height: 100%;
    border-radius: 999px;
    background: radial-gradient(circle at 30% 30%, #fff2b0, #ffd666 55%, #ffbf3f);
    box-shadow: 0 10px 22px rgba(255, 214, 102, 0.18);
    position: relative;
    transform: rotate(0deg);
}

/* mouth */
.pacman::before {
    content: "";
    position: absolute;
    inset: 0;
    border-radius: 999px;
    background:
        conic-gradient(from 25deg, transparent 0 50deg, rgba(11, 16, 32, 0.95) 50deg 310deg, transparent 310deg);
    opacity: 0.92;
    transform: scale(1.02);
    transition: clip-path 80ms ease;
    /* 마우스처럼 보이게: 위 conic로 한 조각 비움 */
    mix-blend-mode: multiply;
}

/* chomp animation */
.pacman.chomp::before {
    background:
        conic-gradient(from 10deg, transparent 0 25deg, rgba(11, 16, 32, 0.95) 25deg 335deg, transparent 335deg);
}

/* direction */
.pacman.dir-right {
    transform: rotate(0deg);
}

.pacman.dir-left {
    transform: rotate(180deg);
}

.pacman.dir-up {
    transform: rotate(270deg);
}

.pacman.dir-down {
    transform: rotate(90deg);
}

.ghostBody {
    width: 100%;
    height: 100%;
    border-radius: 14px 14px 12px 12px;
    position: relative;
    box-shadow: 0 14px 26px rgba(0, 0, 0, 0.25);
    border: 1px solid rgba(255, 255, 255, 0.12);
    overflow: hidden;
}

.ghostBody.pink {
    background: linear-gradient(180deg, rgba(255, 120, 200, 0.95), rgba(255, 120, 200, 0.70));
}

.ghostBody.mint {
    background: linear-gradient(180deg, rgba(112, 255, 220, 0.95), rgba(112, 255, 220, 0.68));
}

.eyes {
    position: absolute;
    top: 7px;
    left: 6px;
    display: flex;
    gap: 5px;
}

.eye {
    width: 8px;
    height: 10px;
    background: rgba(255, 255, 255, 0.95);
    border-radius: 999px;
    position: relative;
}

.eye::after {
    content: "";
    width: 4px;
    height: 4px;
    background: rgba(18, 22, 40, 0.9);
    border-radius: 999px;
    position: absolute;
    top: 3px;
    left: 2px;
}

.feet {
    position: absolute;
    bottom: -2px;
    left: 0;
    right: 0;
    display: flex;
}

.feet span {
    flex: 1;
    height: 10px;
    background: rgba(255, 255, 255, 0.18);
    clip-path: polygon(0 0, 100% 0, 50% 100%);
    opacity: 0.6;
}

.overlay {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    background: rgba(0, 0, 0, 0.35);
    backdrop-filter: blur(6px);
    border-radius: 18px;
}

.card {
    width: min(360px, 92%);
    padding: 16px;
    border-radius: 18px;
    border: 1px solid var(--line);
    background: rgba(15, 18, 38, 0.75);
    box-shadow: 0 22px 70px rgba(0, 0, 0, 0.55);
    text-align: center;
}

.big {
    font-weight: 900;
    font-size: 22px;
    margin-bottom: 8px;
}

.msg {
    font-size: 13px;
    color: var(--muted);
    display: flex;
    justify-content: center;
    gap: 8px;
    margin-bottom: 12px;
}

.msg .dot {
    opacity: 0.5;
}

.tip {
    margin-top: 10px;
    display: flex;
    gap: 6px;
    align-items: center;
    justify-content: center;
    color: var(--muted);
    font-size: 12px;
}

.kbd {
    padding: 3px 7px;
    border-radius: 8px;
    border: 1px solid var(--line);
    background: rgba(255, 255, 255, 0.06);
    color: var(--text);
    font-size: 11px;
}

.sep {
    margin: 0 6px;
    opacity: 0.4;
}
</style>
