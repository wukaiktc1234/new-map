<script setup lang="ts">
/**
 * 合同智能分析抽屉组件
 * 从独立页面改造为抽屉，嵌入HRContract.vue中使用
 *
 * 整合到期提醒、续签推荐、风险识别三大能力
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { contractIntelligenceApi } from '@/api/contract-intelligence'
import {
  type ExpiringContract,
  type ContractRisk,
  type ExpiryStats,
  type RenewalStats,
  type RiskLevel,
  type RenewRecommendation,
  RenewRecommendationLabelMap,
  RiskLevelLabelMap,
} from '@/types/contract-intelligence'

/* ===== Props & Emits ===== */
const props = defineProps<{
  visible: boolean
}>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  'navigate-contract': []
}>()

/* ===== 数据状态 ===== */
const loading = ref(false)
const activeTab = ref('expiry')
const expiringContracts = ref<ExpiringContract[]>([])
const contractRisks = ref<ContractRisk[]>([])
const expiryStats = ref<ExpiryStats>({ expired: 0, urgent: 0, warning: 0, notice: 0 })
const renewalStats = ref<RenewalStats>({ total: 0, strong: 0, normal: 0, cautious: 0, notRecommended: 0 })

/* ===== 过滤 ===== */
const expiryFilter = ref<'all' | 'expired' | 'urgent' | 'warning' | 'notice'>('all')
const riskFilter = ref<'all' | RiskLevel>('all')

const filteredExpiring = computed(() => {
  if (expiryFilter.value === 'all') return expiringContracts.value
  return expiringContracts.value.filter(c => c.alertLevel === expiryFilter.value)
})

const filteredRisks = computed(() => {
  if (riskFilter.value === 'all') return contractRisks.value
  return contractRisks.value.filter(r => r.riskLevel === riskFilter.value)
})

/* ===== 统计卡片 ===== */
const statsCards = computed(() => [
  { icon: 'AlarmClock', label: '已过期', value: expiryStats.value.expired, colorType: 'error' as const },
  { icon: 'Warning', label: '7天内到期', value: expiryStats.value.urgent, colorType: 'warning' as const },
  { icon: 'Calendar', label: '30天内到期', value: expiryStats.value.warning, colorType: 'primary' as const },
  { icon: 'Bell', label: '90天内到期', value: expiryStats.value.notice, colorType: 'info' as const },
])

/* ===== 续签推荐分布 ===== */
const renewalDistribution = computed(() => [
  { label: '强烈推荐', value: renewalStats.value.strong, color: 'var(--fts-success)' },
  { label: '建议续签', value: renewalStats.value.normal, color: 'var(--fts-primary)' },
  { label: '谨慎续签', value: renewalStats.value.cautious, color: 'var(--fts-warning)' },
  { label: '不建议', value: renewalStats.value.notRecommended, color: 'var(--fts-error)' },
])

/* ===== 加载数据 ===== */
async function loadData() {
  loading.value = true
  try {
    const [expiring, risks, eStats, rStats] = await Promise.all([
      contractIntelligenceApi.getExpiringContracts(90),
      contractIntelligenceApi.getContractRisks(),
      contractIntelligenceApi.getExpiryStats(),
      contractIntelligenceApi.getRenewalStats(),
    ])
    expiringContracts.value = expiring
    contractRisks.value = risks
    expiryStats.value = eStats
    renewalStats.value = rStats
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

/* ===== 懒加载：抽屉打开时加载数据 ===== */
watch(() => props.visible, (val) => {
  if (val) {
    loadData()
  }
})

/* ===== 工具函数 ===== */
function getAlertLevelStatus(level: string): string {
  const map: Record<string, string> = {
    expired: 'error',
    urgent: 'warning',
    warning: 'primary',
    notice: 'info',
  }
  return map[level] || 'info'
}

function getAlertLevelLabel(level: string): string {
  const map: Record<string, string> = {
    expired: '已过期',
    urgent: '紧急',
    warning: '预警',
    notice: '提醒',
  }
  return map[level] || level
}

function getRecommendationStatus(rec: RenewRecommendation): string {
  const map: Record<RenewRecommendation, string> = {
    strong: 'success',
    normal: 'primary',
    cautious: 'warning',
    not_recommended: 'error',
  }
  return map[rec] || 'info'
}

function getRiskLevelStatus(level: RiskLevel): string {
  const map: Record<RiskLevel, string> = {
    high: 'error',
    medium: 'warning',
    low: 'info',
  }
  return map[level] || 'info'
}

function getRiskTypeLabel(type: string): string {
  const map: Record<string, string> = {
    expired_unsigned: '过期未签',
    salary_anomaly: '薪资异常',
    missing_contract: '未签合同',
    type_mismatch: '类型不符',
    long_term_temp: '长期临时',
  }
  return map[type] || type
}

function formatRemainingDays(days: number): string {
  if (days < 0) return `已过期${Math.abs(days)}天`
  if (days === 0) return '今日到期'
  return `剩余${days}天`
}

/** 处理按钮点击：关闭抽屉并通知父组件跳转合同管理 */
function handleNavigateContract() {
  emit('navigate-contract')
}
</script>

<template>
  <el-drawer
    :model-value="visible"
    title="合同智能分析"
    size="75%"
    direction="rtl"
    destroy-on-close
    @close="emit('update:visible', false)"
  >
    <!-- 统计卡片 -->
    <section class="stats-section">
      <StatCard v-for="stat in statsCards" :key="stat.label" v-bind="stat" variant="bordered" />
    </section>

    <!-- 续签推荐分布 -->
    <section class="renewal-section">
      <div class="section-title">续签推荐分布</div>
      <div class="renewal-bar">
        <div v-for="item in renewalDistribution" :key="item.label" class="renewal-item">
          <div class="renewal-value" :style="{ color: item.color }">{{ item.value }}</div>
          <div class="renewal-label">{{ item.label }}</div>
        </div>
      </div>
    </section>

    <!-- Tab 区域 -->
    <el-tabs v-model="activeTab" class="dashboard-tabs">
      <!-- 到期合同 Tab -->
      <el-tab-pane label="到期合同提醒" name="expiry">
        <div class="filter-bar">
          <el-radio-group v-model="expiryFilter" size="small">
            <el-radio-button value="all">全部 ({{ expiringContracts.length }})</el-radio-button>
            <el-radio-button value="expired">已过期 ({{ expiryStats.expired }})</el-radio-button>
            <el-radio-button value="urgent">紧急 ({{ expiryStats.urgent }})</el-radio-button>
            <el-radio-button value="warning">预警 ({{ expiryStats.warning }})</el-radio-button>
            <el-radio-button value="notice">提醒 ({{ expiryStats.notice }})</el-radio-button>
          </el-radio-group>
        </div>

        <el-table v-loading="loading" :data="filteredExpiring" stripe size="small">
          <el-table-column prop="employeeName" label="员工" width="80" />
          <el-table-column prop="department" label="部门" width="80" />
          <el-table-column prop="position" label="职位" width="90" />
          <el-table-column prop="contractNo" label="合同编号" width="130" />
          <el-table-column prop="endDate" label="到期日期" width="110" />
          <el-table-column label="剩余天数" width="100">
            <template #default="{ row }">
              <span :class="['remaining-days', `remaining-${row.alertLevel}`]">{{ formatRemainingDays(row.remainingDays) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="提醒级别" width="90">
            <template #default="{ row }">
              <StatusTag :status="getAlertLevelStatus(row.alertLevel)" :label="getAlertLevelLabel(row.alertLevel)" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="续签推荐" width="120">
            <template #default="{ row }">
              <StatusTag :status="getRecommendationStatus(row.recommendation)" :label="RenewRecommendationLabelMap[row.recommendation as RenewRecommendation]" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="recommendationReason" label="推荐理由" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default>
              <el-button link type="primary" size="small" @click="handleNavigateContract">处理</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无到期合同" :image-size="60" />
          </template>
        </el-table>
      </el-tab-pane>

      <!-- 风险识别 Tab -->
      <el-tab-pane label="风险识别" name="risk">
        <div class="filter-bar">
          <el-radio-group v-model="riskFilter" size="small">
            <el-radio-button value="all">全部 ({{ contractRisks.length }})</el-radio-button>
            <el-radio-button value="high">高风险 ({{ contractRisks.filter(r => r.riskLevel === 'high').length }})</el-radio-button>
            <el-radio-button value="medium">中风险 ({{ contractRisks.filter(r => r.riskLevel === 'medium').length }})</el-radio-button>
            <el-radio-button value="low">低风险 ({{ contractRisks.filter(r => r.riskLevel === 'low').length }})</el-radio-button>
          </el-radio-group>
        </div>

        <el-table v-loading="loading" :data="filteredRisks" stripe size="small">
          <el-table-column label="风险等级" width="90">
            <template #default="{ row }">
              <StatusTag :status="getRiskLevelStatus(row.riskLevel)" :label="RiskLevelLabelMap[row.riskLevel as RiskLevel]" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="风险类型" width="100">
            <template #default="{ row }">{{ getRiskTypeLabel(row.riskType) }}</template>
          </el-table-column>
          <el-table-column prop="employeeName" label="员工" width="80" />
          <el-table-column prop="department" label="部门" width="80" />
          <el-table-column prop="description" label="风险描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="detectedTime" label="发现时间" width="110" />
          <el-table-column prop="suggestion" label="处理建议" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="80" fixed="right">
            <template #default>
              <el-button link type="primary" size="small" @click="handleNavigateContract">处理</el-button>
            </template>
          </el-table-column>
          <template #empty>
            <el-empty description="暂无风险项" :image-size="60" />
          </template>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </el-drawer>
</template>

<style scoped lang="scss">
.stats-section { grid-template-columns: repeat(4, 1fr); }

.renewal-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
}

.renewal-bar {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
}

.renewal-item {
  text-align: center;
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);

  .renewal-value {
    font-size: 28px;
    font-weight: 700;
    line-height: 1.2;
  }

  .renewal-label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
    margin-top: 4px;
  }
}

.dashboard-tabs {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.filter-bar {
  margin-bottom: var(--fts-space-3);
}

.remaining-days {
  font-weight: 600;

  &.remaining-expired { color: var(--fts-error); }
  &.remaining-urgent { color: var(--fts-warning); }
  &.remaining-warning { color: var(--fts-primary); }
  &.remaining-notice { color: var(--fts-text-tertiary); }
}
</style>
