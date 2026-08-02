<script setup lang="ts">
/**
 * 会员概览页面
 * 功能：统计卡片 + 会员增长/等级分布/客户分层三个Tab
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import DataTable from '@/components/core/DataTable.vue'
import { memberApi } from '@/api/marketing'
import type { MemberStatsOverview } from '@/types/member'
import { CustomerSegmentText, CustomerSegmentStatusMap } from '@/types/member'

const stats = ref<MemberStatsOverview | null>(null)
const loading = ref(false)
const activeTab = ref('growth')

async function loadStats() {
  loading.value = true
  try { stats.value = await memberApi.getStatsOverview() }
  finally { loading.value = false }
}

/* ===== 统计卡片 ===== */
const statCards = computed(() => {
  const s = stats.value
  const rate = s && s.totalMembers > 0 ? ((s.activeMembers / s.totalMembers) * 100).toFixed(1) + '%' : '--'
  return [
    { icon: 'User', label: '会员总数', value: s?.totalMembers ?? '--', colorType: 'primary' as const },
    { icon: 'TrendCharts', label: '活跃率', value: rate, colorType: 'success' as const },
    { icon: 'Wallet', label: '储值余额', value: s?.totalBalance ? `¥${s.totalBalance}` : '--', colorType: 'warning' as const },
    { icon: 'Refresh', label: '复购率', value: s?.repurchaseRate ? `${s.repurchaseRate}%` : '--', colorType: 'info' as const },
  ]
})

/* ===== 会员增长 ===== */
type GrowthRange = '7' | '30' | '90'
type GrowthItem = { date: string; count: number; channels: { mini_program: number; app: number; pos: number }; cumulative: number }
const growthRange = ref<GrowthRange>('7')

/** 将后端 weeklyGrowthTrend 数据映射为 GrowthItem（渠道分布后端暂无，置零待对接） */
function mapGrowthData(): GrowthItem[] {
  const trend = stats.value?.weeklyGrowthTrend ?? []
  let cumulative = 0
  return trend.map(item => {
    cumulative += item.count
    return {
      date: item.date,
      count: item.count,
      channels: { mini_program: 0, app: 0, pos: 0 },
      cumulative,
    }
  })
}

const growthData = computed(() => mapGrowthData())

const maxGrowthCount = computed(() => growthData.value.length ? Math.max(...growthData.value.map(d => d.count), 1) : 1)

/** 增长汇总指标 */
const growthSummary = computed(() => {
  const data = growthData.value
  if (!data.length) return { newCount: 0, qoq: '0', dailyAvg: 0, total: 0 }
  const newCount = data.reduce((s, d) => s + d.count, 0)
  const half = Math.floor(data.length / 2)
  const first = data.slice(0, half).reduce((s, d) => s + d.count, 0)
  const second = data.slice(half).reduce((s, d) => s + d.count, 0)
  return {
    newCount, qoq: first > 0 ? (((second - first) / first) * 100).toFixed(1) : '0',
    dailyAvg: Math.round(newCount / data.length), total: data[data.length - 1]?.cumulative ?? 0,
  }
})

/** 汇总卡片配置 */
const summaryItems = computed(() => [
  { label: '本期新增', value: `${growthSummary.value.newCount}人`, cls: '' },
  { label: '环比增长', value: `${Number(growthSummary.value.qoq) >= 0 ? '+' : ''}${growthSummary.value.qoq}%`,
    cls: Number(growthSummary.value.qoq) >= 0 ? 'growth-summary__up' : 'growth-summary__down' },
  { label: '日均新增', value: `${growthSummary.value.dailyAvg}人`, cls: '' },
  { label: '累计会员', value: `${growthSummary.value.total}人`, cls: '' },
])

function shortDate(s: string): string {
  const p = s.split('-'); return p.length >= 3 ? `${p[1]}/${p[2]}` : s
}
function channelText(ch: { mini_program: number; app: number; pos: number }): string {
  return `小程序 ${ch.mini_program} | APP ${ch.app} | 门店 ${ch.pos}`
}

const growthTableColumns = [
  { prop: 'date', label: '日期', minWidth: 110 },
  { prop: 'count', label: '新增人数', minWidth: 90 },
  { prop: 'cumulative', label: '累计会员数', minWidth: 100 },
  { prop: 'channels', label: '注册渠道分布', minWidth: 200, slot: 'channels' },
]

function handleGrowthDetail(row: GrowthItem) {
  ElMessage.info(`查看 ${row.date} 的增长详情`)
}

const growthAnalysis = computed(() => {
  const data = growthData.value
  if (!data.length) return []
  const insights: string[] = []

  // 周末vs工作日分析
  const weekendDays = data.filter(d => {
    const day = new Date(d.date).getDay()
    return day === 0 || day === 6
  })
  const weekdayDays = data.filter(d => {
    const day = new Date(d.date).getDay()
    return day !== 0 && day !== 6
  })
  if (weekendDays.length && weekdayDays.length) {
    const wkndAvg = weekendDays.reduce((s, d) => s + d.count, 0) / weekendDays.length
    const wkdayAvg = weekdayDays.reduce((s, d) => s + d.count, 0) / weekdayDays.length
    if (wkndAvg > wkdayAvg) {
      insights.push(`周末日均新增${Math.round(wkndAvg)}人，高于工作日${Math.round(wkdayAvg)}人，建议加强周末营销活动`)
    } else {
      insights.push(`工作日日均新增${Math.round(wkdayAvg)}人，高于周末${Math.round(wkndAvg)}人`)
    }
  }

  // 渠道分析
  const totalMini = data.reduce((s, d) => s + d.channels.mini_program, 0)
  const totalApp = data.reduce((s, d) => s + d.channels.app, 0)
  const totalPos = data.reduce((s, d) => s + d.channels.pos, 0)
  const totalAll = totalMini + totalApp + totalPos
  if (totalAll > 0) {
    const topChannel = totalMini >= totalApp && totalMini >= totalPos ? '小程序' : totalApp >= totalPos ? 'APP' : '门店'
    const topPct = Math.round(Math.max(totalMini, totalApp, totalPos) / totalAll * 100)
    insights.push(`${topChannel}渠道贡献占比最高(${topPct}%)，建议持续优化${topChannel}体验`)
  }

  // 增长趋势
  const half = Math.floor(data.length / 2)
  const firstHalf = data.slice(0, half).reduce((s, d) => s + d.count, 0)
  const secondHalf = data.slice(half).reduce((s, d) => s + d.count, 0)
  if (firstHalf > 0) {
    const change = ((secondHalf - firstHalf) / firstHalf * 100).toFixed(1)
    insights.push(Number(change) >= 0
      ? `后半段增长${change}%，增长趋势良好`
      : `后半段下降${Math.abs(Number(change))}%，需关注增长放缓趋势`)
  }

  return insights
})

onMounted(() => { loadStats() })
</script>

<template>
  <div class="modern-page">
    <PageHeader title="会员概览" description="会员经营数据与客户分析" />
    <section class="stats-section">
      <StatCard v-for="card in statCards" :key="card.label" :icon="card.icon" :label="card.label"
        :value="String(card.value)" :color-type="card.colorType" variant="bordered" />
    </section>
    <div class="content-card">
      <el-tabs v-model="activeTab" class="page-tabs">
        <!-- 会员增长 -->
        <el-tab-pane label="会员增长" name="growth">
          <div v-if="growthData.length" v-loading="loading" class="growth-section">
            <el-radio-group v-model="growthRange" size="small" class="growth-range">
              <el-radio-button value="7">近7天</el-radio-button>
              <el-radio-button value="30">近30天</el-radio-button>
              <el-radio-button value="90">近90天</el-radio-button>
            </el-radio-group>
            <div class="growth-summary">
              <div v-for="item in summaryItems" :key="item.label" class="growth-summary__item">
                <span class="growth-summary__label">{{ item.label }}</span>
                <span class="growth-summary__value" :class="item.cls">{{ item.value }}</span>
              </div>
            </div>
            <div class="growth-chart__bars">
              <div v-for="item in growthData" :key="item.date" class="growth-chart__item">
                <span class="growth-chart__count">{{ item.count }}</span>
                <div class="growth-chart__bar" :style="{ height: (item.count / maxGrowthCount * 100) + '%' }" />
                <span class="growth-chart__date">{{ shortDate(item.date) }}</span>
              </div>
            </div>
            <DataTable :columns="growthTableColumns" :data="growthData" :loading="loading" :selectable="false" :actions-width="90">
              <template #channels="{ row }">
                <span class="growth-table__channels">{{ channelText(row.channels) }}</span>
              </template>
              <template #actions="{ row }">
                <el-button link type="primary" size="small" @click="handleGrowthDetail(row)">查看详情</el-button>
              </template>
            </DataTable>
            <div v-if="growthAnalysis.length" class="growth-analysis">
              <h4 class="growth-analysis__title">增长分析</h4>
              <ul class="growth-analysis__list">
                <li v-for="(insight, idx) in growthAnalysis" :key="idx">{{ insight }}</li>
              </ul>
            </div>
          </div>
          <el-empty v-else description="暂无增长数据" />
        </el-tab-pane>
        <!-- 等级分布 -->
        <el-tab-pane label="等级分布" name="level">
          <el-table :data="stats?.levelDistribution ?? []" v-loading="loading" stripe>
            <el-table-column prop="levelName" label="等级名称" min-width="120" />
            <el-table-column prop="count" label="会员数" min-width="100" align="center" />
            <el-table-column label="占比" min-width="100" align="center">
              <template #default="{ row }">{{ row.percentage }}%</template>
            </el-table-column>
            <el-table-column label="占比图" min-width="200">
              <template #default="{ row }">
                <el-progress :percentage="row.percentage" :stroke-width="10" :show-text="false" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <!-- 客户分层 -->
        <el-tab-pane label="客户分层" name="segment">
          <el-table :data="stats?.segmentDistribution ?? []" v-loading="loading" stripe>
            <el-table-column prop="segmentName" label="分层" min-width="140" />
            <el-table-column prop="count" label="会员数" min-width="100" align="center" />
            <el-table-column label="占比" min-width="100" align="center">
              <template #default="{ row }">{{ row.percentage }}%</template>
            </el-table-column>
            <el-table-column label="状态" min-width="120" align="center">
              <template #default="{ row }">
                <StatusTag :status="CustomerSegmentStatusMap[row.segment] || 'info'"
                  :label="CustomerSegmentText[row.segment] || row.segmentName" size="small" />
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<style scoped lang="scss">
.modern-page { min-height: 100vh; background: var(--fts-bg-page); }
.stats-section { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--fts-space-4); padding: var(--fts-space-4) 0; }
.content-card {
  background: var(--fts-bg-card); border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius); padding: var(--fts-space-5) var(--fts-space-6);
}
.growth-section { display: flex; flex-direction: column; gap: var(--fts-space-4); }
.growth-range { align-self: flex-start; margin-left: var(--fts-space-2); }
.growth-summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: var(--fts-space-4); }
.growth-summary__item {
  display: flex; flex-direction: column; gap: var(--fts-space-1);
  padding: var(--fts-space-3) var(--fts-space-4); background: var(--fts-bg-page); border-radius: var(--fts-radius-md);
}
.growth-summary__label { font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary); }
.growth-summary__value { font-size: var(--fts-font-size-lg); font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary); }
.growth-summary__up { color: var(--fts-success); }
.growth-summary__down { color: var(--fts-error); }
.growth-chart__bars { display: flex; align-items: flex-end; gap: var(--fts-space-2); height: 180px; padding: 0 var(--fts-space-2); }
.growth-chart__item {
  flex: 1; display: flex; flex-direction: column; align-items: center;
  justify-content: flex-end; height: 100%; gap: var(--fts-space-1); min-width: 0;
}
.growth-chart__count { font-size: var(--fts-font-size-xs); font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary); }
.growth-chart__bar {
  width: 100%; max-width: 48px; min-height: 4px;
  border-radius: var(--fts-radius-sm) var(--fts-radius-sm) 0 0; background: var(--fts-primary);
  transition: height var(--fts-duration-normal) var(--fts-easing-default);
}
.growth-chart__date {
  font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary);
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%;
}
.growth-table__channels { font-size: var(--fts-font-size-xs); color: var(--fts-text-secondary); }
.growth-analysis { padding: var(--fts-space-4); background: var(--fts-bg-page); border-radius: var(--fts-radius-md); }
.growth-analysis__title {
  margin: 0 0 var(--fts-space-3); font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium); color: var(--fts-text-primary);
}
.growth-analysis__list {
  margin: 0; padding-left: var(--fts-space-5); font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary); line-height: 1.8;
}
</style>
