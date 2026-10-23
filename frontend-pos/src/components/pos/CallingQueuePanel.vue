<template>
  <section class="queue-panel" :class="`tone-${tone}`">
    <header class="panel-head">
      <span class="panel-dot"></span>
      <h2 class="panel-title">{{ title }}</h2>
      <span class="panel-badge">{{ items.length }}</span>
    </header>

    <div ref="viewportRef" class="panel-viewport">
      <div ref="gridRef" class="tile-grid">
        <div
          v-for="item in items"
          :key="item.key"
          class="tile"
          :class="{ 'is-current': item.current }"
        >
          <span class="tile-number">{{ item.number }}</span>
          <span class="tile-sub">
            <span class="tile-time">{{ item.time }}</span>
            <span v-if="item.meta" class="tile-meta">{{ item.meta }}</span>
          </span>
        </div>
      </div>
      <div v-if="items.length === 0" class="panel-empty">{{ emptyText }}</div>
    </div>

    <div v-if="overflowing" class="panel-fade" aria-hidden="true"></div>
  </section>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

/** 大屏号牌数据（由父组件按状态分流后传入） */
interface DisplayTile {
  /** 稳定唯一键（orderId） */
  key: string
  /** 取餐号（A006），缺失时由父组件回落订单号 */
  number: string
  /** 已格式化的时间文案（统一走 @/utils/format） */
  time: string
  /** 附加信息：件数 / 呼叫次数 */
  meta?: string
  /** 是否为当前大屏正在播报的号码 */
  current?: boolean
}

const props = defineProps<{
  title: string
  tone: 'making' | 'ready'
  emptyText: string
  items: DisplayTile[]
}>()

const viewportRef = ref<HTMLElement | null>(null)
const gridRef = ref<HTMLElement | null>(null)
/** 内容是否超出容器（超出才自动滚动，未超出保持静止） */
const overflowing = ref(false)

/** 自动滚动速度（像素/秒）：慢速平移，保证 3~5 米外也能逐条读清 */
const SCROLL_SPEED = 26
/** 滚到底部后的停留时间（毫秒），留给顾客阅读窗口 */
const BOTTOM_PAUSE = 1600

let rafId = 0
let lastTs = 0
let acc = 0
let paused = false
let pauseTimer: number | null = null
let observer: ResizeObserver | null = null

const cancelPause = () => {
  if (pauseTimer !== null) {
    window.clearTimeout(pauseTimer)
    pauseTimer = null
  }
  paused = false
}

const stopScroll = () => {
  if (rafId) {
    window.cancelAnimationFrame(rafId)
    rafId = 0
  }
  cancelPause()
  acc = 0
  lastTs = 0
}

const measure = () => {
  const el = viewportRef.value
  if (!el) {
    overflowing.value = false
    return
  }
  const max = el.scrollHeight - el.clientHeight
  overflowing.value = max > 4
  if (!overflowing.value && el.scrollTop !== 0) {
    el.scrollTop = 0
  }
}

const step = (ts: number) => {
  const el = viewportRef.value
  if (!el) {
    rafId = 0
    return
  }
  if (!lastTs) {
    lastTs = ts
  }
  // 限幅 delta：切后台再回来时不会瞬间跳过整屏
  const delta = Math.min((ts - lastTs) / 1000, 0.2)
  lastTs = ts
  const max = el.scrollHeight - el.clientHeight

  if (!paused && max > 4) {
    acc += SCROLL_SPEED * delta
    if (acc >= 1) {
      const move = Math.floor(acc)
      acc -= move
      el.scrollTop = Math.min(el.scrollTop + move, max)
      if (el.scrollTop >= max) {
        paused = true
        pauseTimer = window.setTimeout(() => {
          pauseTimer = null
          paused = false
          if (viewportRef.value) {
            viewportRef.value.scrollTop = 0
          }
        }, BOTTOM_PAUSE)
      }
    }
  }

  rafId = window.requestAnimationFrame(step)
}

const restartScroll = () => {
  stopScroll()
  measure()
  if (overflowing.value) {
    rafId = window.requestAnimationFrame(step)
  }
}

// 号码集合变化（新增/取餐/过号）后重新量算并回到顶部重新滚动
watch(
  () => props.items.map((item) => item.key).join('|'),
  () => {
    void nextTick(restartScroll)
  }
)

onMounted(() => {
  restartScroll()
  if (typeof ResizeObserver !== 'undefined') {
    observer = new ResizeObserver(() => {
      measure()
      if (overflowing.value && !rafId) {
        rafId = window.requestAnimationFrame(step)
      }
    })
    if (viewportRef.value) {
      observer.observe(viewportRef.value)
    }
    if (gridRef.value) {
      observer.observe(gridRef.value)
    }
  }
})

onBeforeUnmount(() => {
  stopScroll()
  observer?.disconnect()
  observer = null
})
</script>

<style scoped>
/* ==================== 区块外框 ==================== */
.queue-panel {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 14px 18px 18px;
  border-radius: 22px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.075), rgba(255, 255, 255, 0.025));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.08),
    0 18px 40px rgba(0, 0, 0, 0.38);
  overflow: hidden;
}

.queue-panel.tone-making {
  --tone: #f59e0b;
  --tone-soft: rgba(245, 158, 11, 0.16);
}

.queue-panel.tone-ready {
  --tone: #34d399;
  --tone-soft: rgba(52, 211, 153, 0.16);
}

/* ==================== 区块标题 + 数量角标 ==================== */
.panel-head {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 4px 12px;
  margin-bottom: 10px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.panel-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--tone);
  box-shadow: 0 0 14px var(--tone);
  animation: panelPulse 2.4s ease-in-out infinite;
}

@keyframes panelPulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.55; transform: scale(0.86); }
}

.panel-title {
  margin: 0;
  font-size: clamp(20px, 1.7vw, 30px);
  font-weight: 800;
  letter-spacing: 0.14em;
  color: #ffffff;
  white-space: nowrap;
}

.panel-badge {
  margin-left: auto;
  min-width: 52px;
  padding: 3px 14px;
  border-radius: 999px;
  text-align: center;
  font-size: clamp(17px, 1.4vw, 24px);
  font-weight: 900;
  font-variant-numeric: tabular-nums;
  color: #0b1220;
  background: var(--tone);
  box-shadow: 0 0 20px var(--tone-soft);
}

/* ==================== 号牌滚动视口 ==================== */
.panel-viewport {
  position: relative;
  flex: 1 1 auto;
  min-height: 0;
  overflow: hidden;
  overscroll-behavior: contain;
  scrollbar-width: none;
}

.panel-viewport::-webkit-scrollbar {
  display: none;
}

.tile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(clamp(120px, 11vw, 172px), 1fr));
  grid-auto-rows: min-content;
  gap: 12px;
  padding: 2px;
  align-content: start;
}

/* ==================== 号牌 ==================== */
.tile {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 12px 10px;
  border-radius: 14px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-left: 4px solid var(--tone);
  background: rgba(255, 255, 255, 0.06);
  animation: tileIn 0.38s ease both;
}

@keyframes tileIn {
  from { opacity: 0; transform: translateY(10px) scale(0.96); }
  to { opacity: 1; transform: none; }
}

.tile.is-current {
  background: var(--tone-soft);
  border-color: var(--tone);
  box-shadow: 0 0 0 2px var(--tone) inset, 0 0 22px var(--tone-soft);
}

.tile-number {
  font-size: clamp(28px, 2.6vw, 44px);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 0.06em;
  font-variant-numeric: tabular-nums;
  color: #ffffff;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.45);
  word-break: break-all;
}

.tile.is-current .tile-number {
  color: var(--tone);
}

.tile-sub {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: clamp(12px, 1vw, 16px);
  color: rgba(255, 255, 255, 0.55);
  letter-spacing: 0.04em;
  font-variant-numeric: tabular-nums;
}

.tile-meta {
  padding: 0 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.72);
}

/* ==================== 空态与渐隐遮罩 ==================== */
.panel-empty {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(18px, 1.5vw, 26px);
  letter-spacing: 0.1em;
  color: rgba(255, 255, 255, 0.35);
}

.panel-fade {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 56px;
  pointer-events: none;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0), rgba(15, 23, 42, 0.92));
}
</style>
