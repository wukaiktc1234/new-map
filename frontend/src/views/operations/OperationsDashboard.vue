<script setup lang="ts">
/**
 * 运营总览 - Operations Dashboard（V4 数据看板版）
 * 
 * 设计定位：
 *   - 融合"数据看板"的直观性与"健康仪表盘"的决策价值
 *   - 作为运营中心第一入口，提供全局概览 + 快速导航
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  Shop, Money, Warning, Document,
  CircleCheck, CircleClose, WarningFilled
} from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import { dashboardApi } from '@/api/operations/dashboard'
import type {
  HealthMetric,
  TimelineEvent,
  AlertSummary,
  StoreSnapshot
} from '@/api/operations/dashboard'

const router = useRouter()

// ========== Tab 状态（参考税务管理页面 el-tabs 用法） ==========
const activeTab = ref<'overview' | 'health'>('overview')

/** Tab 切换回调（参考税务管理 handleTabChange） */
function handleTabChange(_tab: string | number): void {
  // 当前 Tab 切换无额外数据加载需求，预留扩展点
  // 后续可根据 tab 名加载对应数据（如 health Tab 加载健康度详情）
}

// ========== 数据源（初始为空，由 API 填充；失败时 API 内部回退到 Mock） ==========
const healthMetrics = ref<HealthMetric[]>([])

const weekEvents = ref<TimelineEvent[]>([])

const alertSummaries = ref<AlertSummary[]>([])

const todayRevenue = ref(0)
const dayOverDayChange = ref(0)
const todayOrders = ref(0)
const activeStores = ref(0)
const avgTurnover = ref(0)
const targetProgress = ref(0)

const storeSnapshot = ref<StoreSnapshot[]>([])

// ========== 计算属性 ==========
const healthScore = computed(() => {
  const weights = [0.35, 0.25, 0.20, 0.20]
  let score = 0
  healthMetrics.value.forEach((metric, index) => {
    let metricScore = 0
    if (metric.unit === '%' && metric.label.includes('异常')) {
      metricScore = Math.max(0, 100 - metric.value * 4)
    } else {
      metricScore = Math.min(100, (metric.value / metric.target) * 100)
    }
    score += metricScore * weights[index]
  })
  return Math.round(score)
})

const healthLevel = computed(() => {
  if (healthScore.value >= 85) return { text: '优秀', color: 'var(--fts-success)', icon: 'CircleCheck' }
  if (healthScore.value >= 70) return { text: '良好', color: 'var(--fts-warning)', icon: 'WarningFilled' }
  return { text: '需关注', color: 'var(--fts-error)', icon: 'CircleClose' }
})

const healthBorderColor = computed(() => healthLevel.value.color + '40')
const healthTrend = computed(() => ({ value: 3, direction: 'up' as const }))

const avgStoreRevenue = computed(() => {
  const activeStores = storeSnapshot.value.filter(s => s.status !== 'inactive')
  if (activeStores.length === 0) return 0
  return activeStores.reduce((sum, s) => sum + s.revenue, 0) / activeStores.length
})

function getStoreBarStyle(store: { revenue: number; status: string }) {
  const widthPct = Math.min(100, (store.revenue / avgStoreRevenue.value) * 100)
  let bgColor = 'var(--fts-error)'
  if (store.revenue >= avgStoreRevenue.value * 0.8) bgColor = 'var(--fts-success)'
  else if (store.revenue >= avgStoreRevenue.value * 0.5) bgColor = 'var(--fts-warning)'
  return { width: `${widthPct}%`, backgroundColor: bgColor }
}

function goToPage(path: string): void {
  router.push(path)
}

function handleAlertClick(): void {
  router.push('/operations/alert-command-center')
}

function handleReportClick(): void {
  router.push('/operations/reports')
}

// ========== 数据加载（真实 API + Mock fallback） ==========
/** 加载看板统计数据（KPI、健康度、事件、预警摘要） */
async function loadDashboardStats(): Promise<void> {
  try {
    const stats = await dashboardApi.getDashboardStats()
    todayRevenue.value = stats.todayRevenue
    dayOverDayChange.value = stats.dayOverDayChange
    todayOrders.value = stats.todayOrders
    activeStores.value = stats.activeStores
    avgTurnover.value = stats.avgTurnover
    targetProgress.value = stats.targetProgress
    healthMetrics.value = stats.healthMetrics
    weekEvents.value = stats.weekEvents
    alertSummaries.value = stats.alertSummaries
  } catch {
    // dashboardApi 内部已有 Mock fallback，理论上不会抛出；此处静默兜底
  }
}

/** 加载门店绩效列表 */
async function loadStoreSnapshot(): Promise<void> {
  try {
    const result = await dashboardApi.getStoreList(1, 100)
    storeSnapshot.value = result.records
  } catch {
    // dashboardApi 内部已有 Mock fallback，理论上不会抛出；此处静默兜底
  }
}

onMounted(() => {
  loadDashboardStats()
  loadStoreSnapshot()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="运营总览" description="企业级经营数据中心 · 全局态势一屏掌握">
      <template #extra>
        <div class="health-indicator">
          <span class="health-indicator__label">综合健康度</span>
          <span class="health-indicator__score" :style="{ color: healthLevel.color }">{{ healthScore }}</span>
          <el-icon :size="14" :color="healthLevel.color"><component :is="healthLevel.icon" /></el-icon>
          <span class="health-indicator__level" :style="{ color: healthLevel.color }">{{ healthLevel.text }}</span>
          <span class="health-indicator__trend" :class="{ 'is-up': healthTrend.direction === 'up' }">
            {{ healthTrend.direction === 'up' ? '+' : '' }}{{ healthTrend.value }}
          </span>
        </div>
      </template>
    </PageHeader>

    <div class="content-wrapper">
    <!-- 第一区：KPI 网格（4 列，参考税务管理 stats-section 规范，强制4列不响应式收缩） -->
    <section class="stats-section" :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }">
      <StatCard icon="Money" label="今日总营收" :value="'¥' + todayRevenue.toLocaleString()" color-type="primary" variant="bordered" :trend="dayOverDayChange">
        <template #extra>
          <div class="kpi-extra">
            <span class="kpi-extra__label">目标完成</span>
            <span class="kpi-extra__value">{{ targetProgress }}%</span>
            <el-progress :percentage="targetProgress" :stroke-width="5" :show-text="false" color="var(--fts-primary)" class="kpi-extra__progress" />
          </div>
        </template>
      </StatCard>
      <StatCard icon="Shop" label="在营门店" :value="activeStores" color-type="info" variant="bordered" />
      <StatCard icon="List" label="今日订单" :value="todayOrders" color-type="warning" variant="bordered" />
      <StatCard icon="Timer" label="平均翻台率" :value="String(avgTurnover)" color-type="success" variant="bordered" />
    </section>

    <!-- 第二区：Tab 内容（参考税务管理 el-tabs border-card 结构） -->
    <section class="tab-section">
      <el-tabs v-model="activeTab" type="border-card" @tab-change="handleTabChange">
        <!-- Tab 1: 经营总览（KPI + 门店快照 + 周事件 + 预警） -->
        <el-tab-pane label="经营总览" name="overview">
          <div class="main-row">
            <div class="panel card-panel">
              <div class="panel-header">
                <span class="panel-title"><el-icon><Shop /></el-icon> 门店今日快照</span>
                <span class="panel-action" @click="goToPage('/operations/live-monitor')">实时监控 →</span>
              </div>
              <div class="store-list">
                <div v-for="store in storeSnapshot" :key="store.name" class="store-row" :class="{ 'store-row--inactive': store.status === 'inactive', 'store-row--warn': store.status === 'warning' }">
                  <span class="store-name">{{ store.name }}</span>
                  <span class="store-rev">¥{{ store.revenue.toLocaleString() }}</span>
                  <span class="store-orders">{{ store.orders }}单</span>
                  <div class="store-bar-wrap">
                    <div class="store-bar" :style="getStoreBarStyle(store)" />
                  </div>
                </div>
              </div>
              <div class="panel-footer">
                共 {{ storeSnapshot.filter(s => s.status !== 'inactive').length }} 家在营 · 平均 ¥{{ Math.round(avgStoreRevenue).toLocaleString() }}/店
              </div>
            </div>

            <div class="panel card-panel">
              <div class="panel-header">
                <span class="panel-title"><el-icon><Document /></el-icon> 本周关键事件</span>
              </div>
              <div class="timeline-list">
                <div v-for="(ev, idx) in weekEvents" :key="idx" class="tl-item">
                  <span class="tl-dot" :class="'tl-dot--' + ev.type" />
                  <div class="tl-body">
                    <span class="tl-date">{{ ev.date }}</span>
                    <span class="tl-title">{{ ev.title }}</span>
                    <p v-if="ev.description" class="tl-desc">{{ ev.description }}</p>
                  </div>
                </div>
              </div>
            </div>

            <div class="right-col">
              <div class="panel card-panel">
                <div class="panel-header">
                  <span class="panel-title"><el-icon><Warning /></el-icon> 最新预警</span>
                  <span class="panel-action" @click="goToPage('/operations/alert-command-center')">全部 →</span>
                </div>
                <div class="alert-list">
                  <div v-for="a in alertSummaries" :key="a.id" class="alert-entry" @click="handleAlertClick()">
                    <span class="alert-dot" :class="'alert-dot--' + a.level" />
                    <div class="alert-body">
                      <span class="alert-store">{{ a.storeName }}</span>
                      <span class="alert-msg">{{ a.message }}</span>
                    </div>
                    <span class="alert-time">{{ a.time }}</span>
                  </div>
                </div>
              </div>

              <div class="panel card-panel">
                <div class="panel-header">
                  <span class="panel-title"><el-icon><Document /></el-icon> 经营报表</span>
                </div>
                <div class="report-grid">
                  <div class="report-item" @click="handleReportClick()">
                    <span class="report-badge report-badge--primary">日</span>
                    <span class="report-name">日报表</span>
                  </div>
                  <div class="report-item" @click="handleReportClick()">
                    <span class="report-badge report-badge--success">周</span>
                    <span class="report-name">周报表</span>
                  </div>
                  <div class="report-item" @click="handleReportClick()">
                    <span class="report-badge report-badge--warning">月</span>
                    <span class="report-name">月报表</span>
                  </div>
                  <div class="report-item" @click="handleReportClick()">
                    <span class="report-badge report-badge--error">利</span>
                    <span class="report-name">利润分析</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 健康度分析 -->
        <el-tab-pane label="健康度分析" name="health">
          <div class="health-grid">
            <div v-for="metric in healthMetrics" :key="metric.label" class="panel card-panel health-card">
              <div class="panel-header">
                <span class="panel-title">{{ metric.label }}</span>
                <span class="health-tag" :class="'health-tag--' + (metric.value >= metric.target ? 'good' : 'warn')">
                  {{ metric.value >= metric.target ? '达标' : '待提升' }}
                </span>
              </div>
              <div class="health-card__body">
                <span class="health-card__value">{{ metric.value }}{{ metric.unit }}</span>
                <span class="health-card__target">目标 {{ metric.target }}{{ metric.unit }}</span>
                <el-progress
                  :percentage="Math.min(100, Math.round((metric.value / metric.target) * 100))"
                  :stroke-width="8"
                  :show-text="false"
                  :color="metric.value >= metric.target ? 'var(--fts-success)' : 'var(--fts-warning)'"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
/* 根容器使用全局 .modern-page（参考税务管理规范），无需自定义 .ops-dashboard */

/* 内容区无水平内边距，让 stats-section 与 tab-section 铺满 main 宽度 */
.content-wrapper {
  padding: 0;
}

/* KPI 网格使用全局 .stats-section（4 列），仅需补充 KPI 卡片内 extra 区域样式 */
.kpi-extra {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-top: var(--fts-space-1);

  &__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-primary);
  }

  &__progress {
    width: 60px;
  }
}

/* Tab 区段（参考税务管理 .tab-section） */
.tab-section {
  margin-top: var(--fts-space-4);
}

.card-panel {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-4);
  padding-bottom: var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-light);
}

.panel-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.panel-action {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  cursor: pointer;

  &:hover { text-decoration: underline; }
}

.health-indicator {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);

  &__label { font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary); }
  &__score { font-size: var(--fts-font-size-xl); font-weight: 800; line-height: 1; }
  &__level { font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-semibold); }
  &__trend {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-success);

    &.is-up { color: var(--fts-success); }
  }
}

/* 健康度卡片网格（Tab 2） */
.health-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4);
}

.health-card {
  &__body {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }

  &__value {
    font-size: var(--fts-font-size-xl);
    font-weight: 700;
    color: var(--fts-text-primary);
  }

  &__target {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
  }
}

.health-tag {
  padding: 2px var(--fts-space-2);
  border-radius: var(--fts-radius-sm);
  font-size: var(--fts-font-size-xs);
  font-weight: 500;

  &--good {
    background: rgba(var(--fts-success-rgb), 0.1);
    color: var(--fts-success);
  }

  &--warn {
    background: rgba(var(--fts-warning-rgb), 0.1);
    color: var(--fts-warning);
  }
}

.main-row {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr 0.9fr;
  gap: var(--fts-space-4);
}

.right-col {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.store-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.store-row {
  display: grid;
  grid-template-columns: 1fr auto auto 70px;
  gap: var(--fts-space-3);
  align-items: center;
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  transition: background-color var(--fts-duration-fast);

  &:hover { background: var(--fts-bg-hover); }
  &--inactive { opacity: 0.45; }
  &--warn { background: rgba(var(--fts-warning-rgb), 0.04); }
}

.store-name {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.store-rev {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.store-orders {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.store-bar-wrap {
  height: 4px;
  background: var(--fts-bg-secondary);
  border-radius: 2px;
  overflow: hidden;
}

.store-bar {
  height: 100%;
  border-radius: 2px;
  transition: width 0.4s ease;
}

.panel-footer {
  margin-top: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-light);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  text-align: center;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
  position: relative;
  padding-left: var(--fts-space-5);

  &::before {
    content: '';
    position: absolute;
    left: 7px; top: 8px; bottom: 8px;
    width: 2px;
    background: var(--fts-border-light);
  }
}

.tl-item {
  display: flex;
  gap: var(--fts-space-3);
  position: relative;
}

.tl-dot {
  position: absolute;
  left: calc(-1 * var(--fts-space-5) + 3px);
  top: 4px;
  width: 10px; height: 10px;
  border-radius: 50%;
  border: 2px solid var(--fts-bg-card);

  &--milestone { background: var(--fts-primary); }
  &--alert { background: var(--fts-error); }
  &--info { background: var(--fts-info); }
  &--success { background: var(--fts-success); }
}

.tl-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.tl-date { font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary); }
.tl-title { font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary); }
.tl-desc { font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary); line-height: 1.4; }

.alert-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.alert-entry {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2);
  border-radius: var(--fts-radius-sm);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast);

  &:hover { background: var(--fts-bg-hover); }
}

.alert-dot {
  width: 8px; height: 8px;
  border-radius: 50%; flex-shrink: 0;

  &--error { background: var(--fts-error); }
  &--warning { background: var(--fts-warning); }
  &--info { background: var(--fts-info); }
}

.alert-body {
  flex: 1; min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.alert-store { font-size: var(--fts-font-size-xs); font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary); }
.alert-msg { font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.alert-time { font-size: var(--fts-font-size-xs); color: var(--fts-text-tertiary); flex-shrink: 0; }

.report-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-2);
}

.report-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  cursor: pointer;
  transition: all var(--fts-duration-fast);
  border: 1px solid transparent;

  &:hover {
    background: var(--fts-bg-hover);
    border-color: var(--fts-border-hover);
  }
}

.report-badge {
  width: 28px; height: 28px;
  border-radius: var(--fts-radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 700;
  color: var(--fts-text-inverse, #fff);
  flex-shrink: 0;

  &--primary { background: var(--fts-primary); }
  &--success { background: var(--fts-success); }
  &--warning { background: var(--fts-warning); }
  &--error { background: var(--fts-error); }
}

.report-name { font-size: var(--fts-font-size-sm); font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary); }

@media (max-width: 1400px) {
  .main-row { grid-template-columns: 1fr 1fr; }
  .right-col { grid-column: span 2; }
}

@media (max-width: 1024px) {
  .main-row { grid-template-columns: 1fr; }
  .health-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .health-indicator { flex-wrap: wrap; gap: var(--fts-space-2); }
}
</style>
