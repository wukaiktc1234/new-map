<template>
  <el-dialog
    :model-value="modelValue"
    width="660px"
    class="retrieve-dialog"
    :show-close="false"
    :lock-scroll="false"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <template #header>
      <div class="rd-header">
        <div class="rd-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/></svg>
        </div>
        <span class="rd-title">取单</span>
        <span v-if="allOrders.length > 0" class="rd-badge">{{ allOrders.length }}</span>
        <button class="rd-close" aria-label="关闭" @click="$emit('update:modelValue', false)">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>

    <div class="rd-body" v-loading="hangingOrderStore.loading" element-loading-text="正在读取挂单...">
      <div v-if="allOrders.length > 0" class="rd-toolbar">
        <el-input
          v-model="keyword"
          class="rd-search"
          placeholder="搜索菜品 / 备注"
          clearable
          :prefix-icon="Search"
          @keydown.enter.stop="toggleActive"
        />
        <div class="rd-summary" data-test="retrieve-summary">
          共 <b>{{ filteredOrders.length }}</b> 张 · 合计 <b>¥{{ filteredTotal }}</b>
        </div>
      </div>

      <div v-if="allOrders.length === 0" class="rd-empty" data-test="retrieve-empty">
        <svg viewBox="0 0 24 24" fill="none" class="rd-empty-icon"><path d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.58a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.58 13H4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
        <p>暂无挂单</p>
        <span class="rd-empty-sub">挂单后的订单会显示在这里</span>
      </div>

      <div v-else-if="filteredOrders.length === 0" class="rd-empty">
        <p>没有匹配的挂单</p>
        <span class="rd-empty-sub">换个菜品名或备注关键字试试</span>
      </div>

      <div v-else class="rd-list-wrap">
        <div class="rd-list" data-test="retrieve-list">
          <!--
            交互设计：
              · 点行体 = 展开/收起完整明细（不动数据，用来核对）
              · 行尾「取单」按钮 = 立即取单（一步，不需要先展开）
              · 双击行 = 快捷取单（熟练收银员的路径）
            这样"快速取单"和"先核对再取"都只要 1 次点击，且不会因为看不清而取错。
          -->
          <div
            v-for="(order, idx) in filteredOrders"
            :key="order.id"
            class="rd-card"
            :class="{ 'is-active': idx === activeIndex, 'is-expanded': expandedId === order.id }"
            :data-test="'retrieve-row'"
            role="button"
            tabindex="0"
            @click="toggleExpand(order)"
            @mouseenter="activeIndex = idx"
            @keydown.enter.prevent="toggleExpand(order)"
          >
            <div class="rd-main">
              <span class="rd-col-time">
                <b>{{ formatClock(order.holdTime) }}</b>
                <small>{{ formatDay(order.holdTime) }}</small>
              </span>

              <span class="rd-col-items">
                <span class="rd-items-line">
                  <template v-for="(item, i) in order.items.slice(0, 2)" :key="i">
                    <span class="rd-item">{{ item.name }}×{{ item.quantity }}</span>
                  </template>
                  <span v-if="order.items.length > 2" class="rd-item-more">+{{ order.items.length - 2 }}</span>
                </span>
                <span v-if="order.remark" class="rd-remark">{{ order.remark }}</span>
              </span>

              <span class="rd-col-qty">{{ countOf(order) }} 件</span>
              <span class="rd-col-amount">¥{{ order.totalAmount.toFixed(2) }}</span>

              <button
                type="button"
                class="rd-take"
                data-test="retrieve-take"
                @click.stop="$emit('select', order)"
              >
                取单
              </button>

              <el-icon class="rd-chevron" :class="{ 'is-open': expandedId === order.id }">
                <ArrowRight />
              </el-icon>
            </div>

            <!-- 展开区：完整菜品清单，用于"两张单长得差不多"时的核对 -->
            <div v-if="expandedId === order.id" class="rd-detail" data-test="retrieve-detail">
              <div class="rd-detail-meta">
                <span>挂单 {{ order.holdTime }}</span>
                <span v-if="order.remark" class="rd-detail-remark">备注：{{ order.remark }}</span>
              </div>
              <div class="rd-detail-grid">
                <div v-for="(item, i) in order.items" :key="i" class="rd-detail-item">
                  <span class="rd-di-name">{{ item.name }}</span>
                  <span class="rd-di-qty">×{{ item.quantity }}</span>
                  <span class="rd-di-price">¥{{ (item.price * item.quantity).toFixed(2) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div class="rd-list-mask" aria-hidden="true"></div>
      </div>
    </div>

    <template #footer>
      <div class="rd-footer">
        <span v-if="allOrders.length > 0" class="rd-hint">↑↓ 选择 · Enter 展开核对 · Shift+Enter 或右侧「取单」直接取单</span>
        <span v-else class="rd-hint" />
        <button class="rd-btn rd-btn-cancel" @click="$emit('update:modelValue', false)">取消</button>
        <button
          v-if="allOrders.length > 0"
          class="rd-btn rd-btn-clear"
          @click="$emit('clear')"
        >
          清空全部
        </button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { ArrowRight, Close, Search } from '@element-plus/icons-vue'
import { useHangingOrderStore, type HeldOrder } from '@/stores/hanging-order'

interface Props { modelValue: boolean }

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', order: HeldOrder): void
  (e: 'clear'): void
}>()

const hangingOrderStore = useHangingOrderStore()
const keyword = ref('')
const activeIndex = ref(0)
const expandedId = ref<string | null>(null)

const allOrders = computed<HeldOrder[]>(() => hangingOrderStore.heldOrders)

const filteredOrders = computed<HeldOrder[]>(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return allOrders.value
  return allOrders.value.filter((order) => {
    if (order.remark && order.remark.toLowerCase().includes(kw)) return true
    return order.items.some((item) => (item.name || '').toLowerCase().includes(kw))
  })
})

const filteredTotal = computed(() =>
  filteredOrders.value.reduce((sum, order) => sum + (order.totalAmount || 0), 0).toFixed(2)
)

const countOf = (order: HeldOrder): number =>
  order.items.reduce((sum, item) => sum + (item.quantity || 0), 0)

const formatClock = (value: string): string => {
  const match = /(\d{2}:\d{2}:\d{2})/.exec(value || '')
  return match ? match[1].slice(0, 5) : (value || '--:--')
}

const formatDay = (value: string): string => {
  const match = /(\d{4})-(\d{2})-(\d{2})/.exec(value || '')
  return match ? `${match[2]}-${match[3]}` : ''
}

/** 行体点击：展开/收起（不取单），同一时刻只展开一张 */
const toggleExpand = (order: HeldOrder): void => {
  expandedId.value = expandedId.value === order.id ? null : order.id
}

/** 键盘 Enter：等价于点行体（展开核对），不会误取单 */
const toggleActive = (): void => {
  const target = filteredOrders.value[activeIndex.value]
  if (target) toggleExpand(target)
}

const moveActive = (delta: number): void => {
  const total = filteredOrders.value.length
  if (total === 0) return
  activeIndex.value = (activeIndex.value + delta + total) % total
  void nextTick(() => {
    const el = document.querySelectorAll('.rd-card')[activeIndex.value] as HTMLElement | undefined
    el?.scrollIntoView({ block: 'nearest' })
  })
}

const onKeydown = (event: KeyboardEvent): void => {
  if (!props.modelValue) return
  const target = event.target as HTMLElement | null
  if (target && target.tagName === 'INPUT') return
  if (event.key === 'ArrowDown') {
    event.preventDefault()
    moveActive(1)
  } else if (event.key === 'ArrowUp') {
    event.preventDefault()
    moveActive(-1)
  } else if (event.key === 'Enter') {
    event.preventDefault()
    // Shift+Enter = 直接取单（键盘熟练路径）；单独 Enter = 展开核对
    if (event.shiftKey) {
      const target = filteredOrders.value[activeIndex.value]
      if (target) emit('select', target)
      return
    }
    toggleActive()
  }
}

watch(
  () => props.modelValue,
  async (visible) => {
    if (visible) {
      keyword.value = ''
      activeIndex.value = 0
      expandedId.value = null
      await hangingOrderStore.fetchHeldOrders()
      window.addEventListener('keydown', onKeydown)
    } else {
      window.removeEventListener('keydown', onKeydown)
    }
  }
)

watch(filteredOrders, () => {
  if (activeIndex.value > filteredOrders.value.length - 1) activeIndex.value = 0
  if (expandedId.value && !filteredOrders.value.some((o) => o.id === expandedId.value)) {
    expandedId.value = null
  }
})

onUnmounted(() => {
  if (typeof window !== 'undefined') window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ==================== 取单弹窗 ====================
 * 密度：一行一单 64px；空态与满载弹窗同高（固定高度），不会"打开先小后大"。
 * 安全：点行体只展开明细，取单必须点行尾按钮或双击——避免"看着差不多就取错"。
 */
.rd-header {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px) var(--pos-spacing-md, 24px);
  border-bottom: 1px solid var(--pos-border-light, #f1f5f9);
  flex: 0 0 auto;
}

.rd-icon-wrap {
  width: 32px;
  height: 32px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-primary, #e3651c);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.rd-icon-wrap svg { width: 18px; height: 18px; }

.rd-title { font-size: 16px; font-weight: 700; color: var(--pos-text-primary, #1e293b); }

.rd-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 8px;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-primary-soft, #fdf1e7);
  color: var(--pos-primary, #e3651c);
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.rd-close {
  margin-left: auto;
  width: 32px;
  height: 32px;
  border-radius: var(--pos-radius-sm, 8px);
  border: none;
  background: transparent;
  color: var(--pos-text-muted, #94a3b8);
  font-size: 15px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color var(--pos-transition-fast, 150ms ease);
}
.rd-close:hover { background: var(--pos-bg-hover, #e2e8f0); color: var(--pos-text-primary, #1e293b); }

.rd-body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px) var(--pos-spacing-md, 24px);
}

.rd-toolbar {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
}

.rd-search { flex: 1 1 auto; min-width: 160px; }

.rd-summary {
  flex: 0 0 auto;
  font-size: 12.5px;
  color: var(--pos-text-secondary, #64748b);
  white-space: nowrap;
}
.rd-summary b { color: var(--pos-primary, #e3651c); font-variant-numeric: tabular-nums; }

.rd-list-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  display: flex;
}

.rd-list {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  overflow-y: auto;
  padding-right: 2px;
  scrollbar-width: thin;
  scrollbar-color: var(--pos-border-color, #e2e8f0) transparent;
}
.rd-list::-webkit-scrollbar { width: 6px; }
.rd-list::-webkit-scrollbar-thumb { background: var(--pos-border-color, #e2e8f0); border-radius: 9999px; }

.rd-list-mask {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 16px;
  pointer-events: none;
  background: linear-gradient(to bottom, transparent, var(--pos-bg-primary, #ffffff));
}

/* 卡片：未展开时就是 64px 一行 */
.rd-card {
  border: 1px solid var(--pos-border-light, #f1f5f9);
  border-radius: var(--pos-radius-md, 12px);
  background: var(--pos-bg-primary, #ffffff);
  cursor: pointer;
  outline: none;
  transition: border-color var(--pos-transition-fast, 150ms ease),
    background-color var(--pos-transition-fast, 150ms ease),
    box-shadow var(--pos-transition-fast, 150ms ease);
}

.rd-card:hover,
.rd-card.is-active {
  border-color: rgba(227, 101, 28, 0.55);
  background: var(--pos-primary-soft, #fdf1e7);
}

.rd-card.is-expanded {
  border-color: var(--pos-primary, #e3651c);
  background: var(--pos-primary-soft, #fdf1e7);
  box-shadow: 0 4px 14px rgba(227, 101, 28, 0.14);
}

.rd-main {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) 52px 92px 64px 16px;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  min-height: 62px;
  padding: 8px 12px;
}

.rd-col-time { display: flex; flex-direction: column; line-height: 1.2; }
.rd-col-time b {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  font-size: 15px;
  color: var(--pos-text-primary, #1e293b);
}
.rd-col-time small { font-size: 11px; color: var(--pos-text-muted, #94a3b8); font-variant-numeric: tabular-nums; }

.rd-col-items { display: flex; flex-direction: column; gap: 2px; min-width: 0; }

.rd-items-line {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  overflow: hidden;
}

.rd-item {
  font-size: 13px;
  color: var(--pos-text-primary, #1e293b);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 45%;
}

.rd-item-more { flex-shrink: 0; font-size: 11.5px; font-weight: 600; color: var(--pos-primary, #e3651c); }

.rd-remark { font-size: 11.5px; color: var(--pos-text-muted, #94a3b8); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.rd-col-qty { font-size: 12px; color: var(--pos-text-secondary, #64748b); text-align: right; font-variant-numeric: tabular-nums; }

.rd-col-amount {
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
  font-variant-numeric: tabular-nums;
  font-size: 17px;
  font-weight: 800;
  color: var(--pos-text-primary, #1e293b);
  text-align: right;
}

/* 行尾「取单」：常驻按钮，不需要先展开就能一步取单 */
/* 「取单」：默认描边（一屏 6 个实心橙按钮会变成一堵色墙），
 * 悬停/当前行才填充主色；触屏上仍是大热区可点。 */
.rd-take {
  height: 34px;
  min-width: 58px;
  padding: 0 12px;
  border: 1px solid var(--pos-primary, #e3651c);
  border-radius: var(--pos-radius-sm, 8px);
  background: transparent;
  color: var(--pos-primary, #e3651c);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
}

.rd-card:hover .rd-take,
.rd-card.is-active .rd-take,
.rd-card.is-expanded .rd-take {
  background: var(--pos-primary, #e3651c);
  color: #ffffff;
}

.rd-take:hover { background: var(--pos-primary-dark, #c25515); border-color: var(--pos-primary-dark, #c25515); color: #fff; }
.rd-take:active { transform: scale(0.97); }

.rd-chevron {
  color: var(--pos-text-muted, #94a3b8);
  font-size: 13px;
  transition: transform var(--pos-transition-fast, 150ms ease);
}
.rd-chevron.is-open { transform: rotate(90deg); color: var(--pos-primary, #e3651c); }

/* 展开区：完整明细，两列网格 */
.rd-detail {
  padding: 0 12px 12px;
  border-top: 1px dashed var(--pos-border-color, #e2e8f0);
  margin: 0 0 2px;
}

.rd-detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 10px 0 8px;
  font-size: 11.5px;
  color: var(--pos-text-muted, #94a3b8);
}

.rd-detail-remark { color: var(--pos-text-secondary, #64748b); }

.rd-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 4px 20px;
}

.rd-detail-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 44px 74px;
  align-items: center;
  gap: 8px;
  padding: 4px 0;
  font-size: 12.5px;
}

.rd-di-name { color: var(--pos-text-primary, #1e293b); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.rd-di-qty { color: var(--pos-text-secondary, #64748b); text-align: right; font-variant-numeric: tabular-nums; }
.rd-di-price {
  color: var(--pos-text-primary, #1e293b);
  text-align: right;
  font-variant-numeric: tabular-nums;
  font-family: var(--pos-font-number, 'DIN Alternate', 'Roboto', sans-serif);
}

.rd-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--pos-text-muted, #94a3b8);
  font-size: 14px;
}
.rd-empty-icon { width: 48px; height: 48px; opacity: 0.25; color: var(--pos-primary, #e3651c); }
.rd-empty p { margin: 0; color: var(--pos-text-secondary, #64748b); }
.rd-empty-sub { font-size: 12.5px; }

.rd-footer {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  padding: var(--pos-spacing-sm, 16px) var(--pos-spacing-md, 24px);
  border-top: 1px solid var(--pos-border-light, #f1f5f9);
  flex: 0 0 auto;
}

.rd-hint { flex: 1; font-size: 11.5px; color: var(--pos-text-muted, #94a3b8); }

.rd-btn {
  flex: 0 0 auto;
  min-width: 96px;
  height: 40px;
  border-radius: var(--pos-radius-sm, 8px);
  border: none;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color var(--pos-transition-fast, 150ms ease),
    border-color var(--pos-transition-fast, 150ms ease),
    transform var(--pos-transition-fast, 150ms ease);
}
.rd-btn:active { transform: scale(0.98); }

.rd-btn-cancel {
  background: var(--pos-bg-secondary, #f8fafc);
  color: var(--pos-text-secondary, #64748b);
  border: 1px solid var(--pos-border-color, #e2e8f0);
}
.rd-btn-cancel:hover { border-color: var(--pos-text-muted, #94a3b8); color: var(--pos-text-primary, #1e293b); }

.rd-btn-clear {
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  border: 1px solid rgba(201, 75, 75, 0.28);
}
.rd-btn-clear:hover { background: var(--pos-danger-soft-hover, #f6dede); border-color: var(--pos-danger, #c94b4b); }
</style>
