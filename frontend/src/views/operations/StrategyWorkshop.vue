<script setup lang="ts">
/**
 * 运营策略工坊 - L3策略工具页面
 *
 * 提供促销活动编排、效果评估、排班优化、菜品优化等运营动作工具。
 * 让运营团队从"看数据"升级为"做动作"。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Plus,
  Timer,
  Money,
  TrendCharts,
  Shop,
  DataAnalysis,
  Calendar,
  Promotion,
} from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { strategyApi } from '@/api/operations/strategy'
import type { Campaign, ChannelType, CampaignStatus, CampaignFormData } from '@/api/operations/strategy'

/* ===== 类型定义 ===== */

// Campaign / ChannelType / CampaignStatus 类型从 @/api/operations/strategy 导入，保持与 API 层一致

/** 效果等级 */
type EffectGrade = 'S' | 'A' | 'B' | 'C'

/** 渠道配置映射 */
const channelConfig: Record<ChannelType, { label: string; tagStatus: string }> = {
  wechat_moments: { label: '微信朋友圈', tagStatus: 'info' },
  douyin: { label: '抖音短视频', tagStatus: 'active' },
  meituan: { label: '美团/大众点评', tagStatus: 'warning' },
  offline: { label: '线下地推', tagStatus: 'pending' },
  sms: { label: '短信', tagStatus: 'error' },
}

/** 状态配置映射 */
const statusConfig: Record<CampaignStatus, { label: string; tagStatus: string }> = {
  in_progress: { label: '进行中', tagStatus: 'in_progress' },
  completed: { label: '已完成', tagStatus: 'completed' },
  pending: { label: '待上线', tagStatus: 'pending' },
}

/* ===== Composable 初始化 ===== */

// 门店下拉选项（接入真实后端 API：/v1/store-operation/stores/active）
const { storeOptions } = useStoreOptions(true)

const { activeTab, loading, pagination } = useStandardPage({
  defaultTab: 'composer',
})

/* ===== 全局统计卡片（Tab 上方统一展示，按 activeTab 切换内容） ===== */
interface StatsCardConfig {
  icon: string
  value: string | number
  label: string
  colorType: 'primary' | 'success' | 'warning' | 'info' | 'error'
}

const currentStatsCards = computed<StatsCardConfig[]>(() => {
  if (activeTab.value === 'composer') {
    return [
      { icon: 'Timer', value: tab1Stats.value.activeCount, label: '进行中活动', colorType: 'primary' },
      { icon: 'Money', value: `¥${tab1Stats.value.totalSpent.toLocaleString()}`, label: '本月花费', colorType: 'info' },
      { icon: 'TrendCharts', value: tab1Stats.value.avgRoi, label: '平均ROI', colorType: 'success' },
      { icon: 'Shop', value: tab1Stats.value.coveredStores, label: '覆盖门店数', colorType: 'warning' },
    ]
  }
  if (activeTab.value === 'evaluation') {
    return [
      { icon: 'Money', value: `¥${tab2Stats.value.totalBudget.toLocaleString()}`, label: '总投放预算', colorType: 'primary' },
      { icon: 'DataAnalysis', value: `¥${tab2Stats.value.totalActual.toLocaleString()}`, label: '总实际花费', colorType: 'info' },
      { icon: 'Promotion', value: tab2Stats.value.totalConversions, label: '总转化人数', colorType: 'success' },
      { icon: 'TrendCharts', value: tab2Stats.value.avgRoi, label: '平均ROI', colorType: 'warning' },
    ]
  }
  // Tab3/Tab4 暂无统计卡片
  return []
})

/* ===== 响应式状态 ===== */

// 活动列表（从 API 加载，失败时由 API 层回退到 Mock）
const campaigns = ref<Campaign[]>([])
const filteredCampaigns = ref<Campaign[]>([])

// 搜索表单
const searchForm = reactive({
  dateRange: [] as string[],
  channel: '' as ChannelType | '',
  status: '' as CampaignStatus | '',
})

// 对话框状态
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingCampaign = ref<Campaign | null>(null)

// 表单数据
const campaignForm = reactive({
  name: '',
  channel: '' as ChannelType | '',
  dateRange: null as [string, string] | null,
  startDate: '',
  endDate: '',
  budget: undefined as number | undefined,
  targetConversionRate: undefined as number | undefined,
  storeIds: [] as string[],
})

/* ===== 计算属性 ===== */

/** Tab1 统计指标 */
const tab1Stats = computed(() => {
  const activeCount = campaigns.value.filter((c) => c.status === 'in_progress').length
  const totalSpent = campaigns.value
    .filter((c) => c.status === 'in_progress')
    .reduce((sum, c) => sum + c.actualCost, 0)
  const avgRoi = (() => {
    const active = campaigns.value.filter((c) => c.status === 'in_progress' && c.roi > 0)
    if (active.length === 0) return 0
    return active.reduce((sum, c) => sum + c.roi, 0) / active.length
  })()
  const coveredStores = new Set(campaigns.value.filter((c) => c.status === 'in_progress').flatMap((c) => c.storeIds)).size

  return { activeCount, totalSpent, avgRoi: avgRoi.toFixed(1), coveredStores }
})

/** Tab2 统计指标 */
const tab2Stats = computed(() => {
  const totalBudget = campaigns.value.reduce((sum, c) => sum + c.budget, 0)
  const totalActual = campaigns.value.reduce((sum, c) => sum + c.actualCost, 0)
  // 假设转化人数 = 实际花费 * 转化率 / 100（简化计算）
  const totalConversions = Math.round(
    campaigns.value.filter((c) => c.actualCost > 0).reduce((sum, c) => sum + (c.actualCost * c.conversionRate) / 100, 0),
  )
  const allRoi = campaigns.value.filter((c) => c.roi > 0)
  const avgRoi = allRoi.length > 0 ? allRoi.reduce((sum, c) => sum + c.roi, 0) / allRoi.length : 0

  return { totalBudget, totalActual, totalConversions, avgRoi: avgRoi.toFixed(1) }
})

/** 渠道表现汇总 */
const channelSummary = computed(() => {
  const summaryMap: Record<
    string,
    { channel: ChannelType; label: string; totalSpend: number; totalConversions: number; bestCampaign: string }
  > = {}

  campaigns.value.forEach((campaign) => {
    if (campaign.actualCost <= 0) return
    if (!summaryMap[campaign.channel]) {
      summaryMap[campaign.channel] = {
        channel: campaign.channel as ChannelType,
        label: channelConfig[campaign.channel as ChannelType].label,
        totalSpend: 0,
        totalConversions: 0,
        bestCampaign: campaign.name,
      }
    }
    const entry = summaryMap[campaign.channel]
    entry.totalSpend += campaign.actualCost
    entry.totalConversions += Math.round((campaign.actualCost * campaign.conversionRate) / 100)
    // ROI最高的作为最佳活动
    if (campaign.roi > 0) {
      const existingBest = campaigns.value.find((c) => c.name === entry.bestCampaign)
      if (!existingBest || campaign.roi > existingBest.roi) {
        entry.bestCampaign = campaign.name
      }
    }
  })

  return Object.values(summaryMap).sort((a, b) => b.totalSpend - a.totalSpend)
})

/** 获取效果等级 */
function getEffectGrade(roi: number): { grade: EffectGrade; tagStatus: string } {
  if (roi >= 4) return { grade: 'S', tagStatus: 'success' }
  if (roi >= 3) return { grade: 'A', tagStatus: 'active' }
  if (roi >= 2) return { grade: 'B', tagStatus: 'warning' }
  return { grade: 'C', tagStatus: 'error' }
}

/** 表格列定义 - Tab1（可编辑） */
const tableColumnsTab1 = [
  { prop: 'name', label: '活动名称', minWidth: 180, slot: 'name' },
  { prop: 'channel', label: '投放渠道', minWidth: 140, slot: 'channel' },
  { prop: 'period', label: '活动周期', minWidth: 200, slot: 'period' },
  { prop: 'budget', label: '预算(元)', minWidth: 100, align: 'right' as const },
  { prop: 'actualCost', label: '实际花费(元)', minWidth: 110, align: 'right' as const },
  { prop: 'conversionRate', label: '转化率', minWidth: 80, align: 'center' as const, slot: 'conversionRate' },
  { prop: 'roi', label: 'ROI', minWidth: 70, align: 'center' as const, slot: 'roi' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/** 表格列定义 - Tab2（只读+评级） */
const tableColumnsTab2 = [
  { prop: 'name', label: '活动名称', minWidth: 180, slot: 'name' },
  { prop: 'channel', label: '投放渠道', minWidth: 140, slot: 'channel' },
  { prop: 'period', label: '活动周期', minWidth: 200, slot: 'period' },
  { prop: 'budget', label: '预算(元)', minWidth: 100, align: 'right' as const },
  { prop: 'actualCost', label: '实际花费(元)', minWidth: 110, align: 'right' as const },
  { prop: 'conversionRate', label: '转化率', minWidth: 80, align: 'center' as const, slot: 'conversionRate' },
  { prop: 'roi', label: 'ROI', minWidth: 70, align: 'center' as const, slot: 'roi' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'grade', label: '效果评级', minWidth: 90, align: 'center' as const, slot: 'grade' },
]

/* ===== 方法 ===== */

/** 搜索过滤 */
function handleSearch() {
  filteredCampaigns.value = campaigns.value.filter((campaign) => {
    // 渠道筛选
    if (searchForm.channel && campaign.channel !== searchForm.channel) return false
    // 状态筛选
    if (searchForm.status && campaign.status !== searchForm.status) return false
    // 日期范围筛选
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      const start = new Date(searchForm.dateRange[0])
      const end = new Date(searchForm.dateRange[1])
      const campStart = new Date(campaign.startDate)
      const campEnd = new Date(campaign.endDate)
      if (campStart > end || campEnd < start) return false
    }
    return true
  })
}

/** 重置搜索 */
function handleReset() {
  searchForm.dateRange = []
  searchForm.channel = ''
  searchForm.status = ''
  filteredCampaigns.value = [...campaigns.value]
}

/** 打开新建对话框 */
function handleCreate() {
  dialogMode.value = 'create'
  editingCampaign.value = null
  resetForm()
  dialogVisible.value = true
}

/** 打开编辑对话框 */
function handleEdit(row: Campaign) {
  dialogMode.value = 'edit'
  editingCampaign.value = row
  campaignForm.name = row.name
  campaignForm.channel = row.channel
  campaignForm.startDate = row.startDate
  campaignForm.endDate = row.endDate
  campaignForm.budget = row.budget
  campaignForm.targetConversionRate = row.targetConversionRate
  campaignForm.storeIds = [...row.storeIds]
  dialogVisible.value = true
}

/** 重置表单 */
function resetForm() {
  campaignForm.name = ''
  campaignForm.channel = ''
  campaignForm.startDate = ''
  campaignForm.endDate = ''
  campaignForm.budget = undefined
  campaignForm.targetConversionRate = undefined
  campaignForm.storeIds = []
}

/** 构建表单数据（用于 API 调用） */
function buildFormData(): CampaignFormData {
  return {
    name: campaignForm.name.trim(),
    channel: campaignForm.channel,
    startDate: campaignForm.startDate,
    endDate: campaignForm.endDate,
    budget: campaignForm.budget,
    targetConversionRate: campaignForm.targetConversionRate,
    storeIds: [...campaignForm.storeIds],
  }
}

/** 提交表单（调用 strategyApi，失败时由 API 层回退到本地操作） */
async function handleSubmit() {
  // 基础校验
  if (!campaignForm.name.trim()) {
    ElMessage.warning('请输入活动名称')
    return
  }
  if (!campaignForm.channel) {
    ElMessage.warning('请选择投放渠道')
    return
  }
  if (!campaignForm.startDate || !campaignForm.endDate) {
    ElMessage.warning('请选择活动日期范围')
    return
  }

  const formData = buildFormData()

  if (dialogMode.value === 'create') {
    // 调用 API 创建活动（API 内部已处理 Mock 回退）
    const newCampaign = await strategyApi.create(formData)
    campaigns.value.unshift(newCampaign)
    filteredCampaigns.value = [...campaigns.value]
    ElMessage({
      message: '活动创建成功，状态为「待上线」。发布后将自动同步至收银端、小程序端等渠道',
      type: 'success',
      duration: 4000,
      showClose: true
    })
  } else if (editingCampaign.value) {
    const idx = campaigns.value.findIndex((c) => c.id === editingCampaign.value!.id)
    if (idx !== -1) {
      // 调用 API 更新活动（保留原 actualCost/conversionRate/roi 等字段）
      const updatedCampaign = await strategyApi.update(
        editingCampaign.value.id,
        formData,
        campaigns.value[idx].status,
      )
      campaigns.value[idx] = {
        ...campaigns.value[idx],
        ...updatedCampaign,
        // 保留后端未返回的执行数据（actualCost/conversionRate/roi）
        actualCost: campaigns.value[idx].actualCost,
        conversionRate: campaigns.value[idx].conversionRate,
        roi: campaigns.value[idx].roi,
      }
      filteredCampaigns.value = [...campaigns.value]
      ElMessage.success('活动更新成功')
    }
  }

  dialogVisible.value = false
}

/** 发布活动（调用 strategyApi 通知后端，失败时由 API 层静默处理） */
async function handlePublish(row: Campaign) {
  const idx = campaigns.value.findIndex((c) => c.id === row.id)
  if (idx !== -1) {
    // 调用 API 发布活动（API 内部已处理 Mock 回退）
    await strategyApi.publish(row.id)
    campaigns.value[idx] = { ...campaigns.value[idx], status: 'in_progress' as CampaignStatus }
    filteredCampaigns.value = [...campaigns.value]
    ElMessage({
      message: `「${row.name}」已发布，正在同步至各渠道（收银端、小程序端、美团等）`,
      type: 'success',
      duration: 5000,
      showClose: true
    })
  }
}

/** 效果分析跳转 */
function handleAnalyze(row: Campaign) {
  activeTab.value = 'evaluation'
  ElMessage.info(`正在查看「${row.name}」的效果评估`)
}

/** 加载活动列表（从 API 加载，失败时由 API 层回退到 Mock） */
async function loadCampaigns() {
  loading.value = true
  try {
    const list = await strategyApi.getList()
    campaigns.value = list
    filteredCampaigns.value = [...list]
  } catch (error) {
    console.error('[StrategyWorkshop] 加载活动列表失败:', error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCampaigns()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="运营策略" description="基于数据洞察的运营动作编排工具" />

    <!-- 统计卡片（Tab 上方统一展示，按 activeTab 切换内容；强制4列不响应式收缩） -->
    <section
      v-if="currentStatsCards.length > 0"
      class="stats-section"
      :style="{ gridTemplateColumns: 'repeat(4, 1fr)' }"
    >
      <StatCard
        v-for="stat in currentStatsCards"
        :key="stat.label"
        :icon="stat.icon"
        :value="stat.value"
        :label="stat.label"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- Tab 切换（参考税务管理 el-tabs border-card 结构） -->
    <section class="tab-section">
      <el-tabs v-model="activeTab" type="border-card">
      <!-- ========== Tab 1: 促销活动编排 ========== -->
      <el-tab-pane label="促销活动编排" name="composer">
        <!-- 搜索工具栏 -->
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-row__filters">
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                style="width: 260px"
              />
              <el-select v-model="searchForm.channel" placeholder="投放渠道" clearable style="width: 160px">
                <el-option
                  v-for="(config, key) in channelConfig"
                  :key="key"
                  :label="config.label"
                  :value="key"
                />
              </el-select>
              <el-select v-model="searchForm.status" placeholder="活动状态" clearable style="width: 140px">
                <el-option
                  v-for="(config, key) in statusConfig"
                  :key="key"
                  :label="config.label"
                  :value="key"
                />
              </el-select>
            </div>
            <div class="toolbar-row__actions">
              <el-button type="success" :icon="Plus" @click="handleCreate">新建活动</el-button>
              <el-button type="primary" @click="handleSearch">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </div>
        </div>

        <!-- 活动列表表格 -->
        <div class="table-section">
          <DataTable
            :data="filteredCampaigns"
            :columns="tableColumnsTab1"
            :loading="loading"
            :selectable="false"
          >
            <!-- 活动名称 -->
            <template #name="{ row }">
              <span class="campaign-name">{{ row.name }}</span>
            </template>
            <!-- 投放渠道 -->
            <template #channel="{ row }">
              <StatusTag :status="channelConfig[row.channel as ChannelType]?.tagStatus || 'info'" :label="channelConfig[row.channel as ChannelType]?.label || row.channel" />
            </template>
            <!-- 活动周期 -->
            <template #period="{ row }">
              <span class="period-text">{{ row.startDate }} ~ {{ row.endDate }}</span>
            </template>
            <!-- 转化率 -->
            <template #conversionRate="{ row }">
              <span class="conversion-rate">{{ row.conversionRate }}%</span>
            </template>
            <!-- ROI -->
            <template #roi="{ row }">
              <span
                class="roi-value"
                :class="{
                  'roi--high': row.roi >= 3,
                  'roi--medium': row.roi >= 2 && row.roi < 3,
                  'roi--low': row.roi > 0 && row.roi < 2,
                  'roi--zero': row.roi === 0,
                }"
              >
                {{ row.roi > 0 ? `${row.roi.toFixed(1)}x` : '-' }}
              </span>
            </template>
            <!-- 状态 -->
            <template #status="{ row }">
              <StatusTag :status="statusConfig[row.status as CampaignStatus]?.tagStatus || 'info'" />
            </template>
            <!-- 操作列 -->
            <template #actions="{ row }">
              <el-button v-if="row.status === 'pending'" link type="success" size="small" @click="handlePublish(row)">发布</el-button>
              <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button v-if="row.status !== 'pending'" link type="primary" size="small" @click="handleAnalyze(row)">效果分析</el-button>
            </template>
          </DataTable>
        </div>
      </el-tab-pane>

      <!-- ========== Tab 2: 促销效果评估 ========== -->
      <el-tab-pane label="促销效果评估" name="evaluation">
        <!-- ROI 表格（只读视图） -->
        <div class="table-section">
          <DataTable
            :data="filteredCampaigns.filter((c) => c.status !== 'pending')"
            :columns="tableColumnsTab2"
            :loading="loading"
            :selectable="false"
          >
            <template #name="{ row }">
              <span class="campaign-name">{{ row.name }}</span>
            </template>
            <template #channel="{ row }">
              <StatusTag :status="channelConfig[row.channel as ChannelType]?.tagStatus || 'info'" :label="channelConfig[row.channel as ChannelType]?.label || row.channel" />
            </template>
            <template #period="{ row }">
              <span class="period-text">{{ row.startDate }} ~ {{ row.endDate }}</span>
            </template>
            <template #conversionRate="{ row }">
              <span class="conversion-rate">{{ row.conversionRate }}%</span>
            </template>
            <template #roi="{ row }">
              <span
                class="roi-value"
                :class="{
                  'roi--high': row.roi >= 3,
                  'roi--medium': row.roi >= 2 && row.roi < 3,
                  'roi--low': row.roi > 0 && row.roi < 2,
                  'roi--zero': row.roi === 0,
                }"
              >
                {{ row.roi > 0 ? `${row.roi.toFixed(1)}x` : '-' }}
              </span>
            </template>
            <template #status="{ row }">
              <StatusTag :status="statusConfig[row.status as CampaignStatus]?.tagStatus || 'info'" />
            </template>
            <template #grade="{ row }">
              <span class="effect-grade" :class="`effect-grade--${getEffectGrade(row.roi).grade.toLowerCase()}`">
                {{ getEffectGrade(row.roi).grade }}
              </span>
            </template>
          </DataTable>
        </div>

        <!-- 渠道表现汇总 -->
        <div class="channel-summary-section">
          <h3 class="section-title">渠道表现汇总</h3>
          <div class="channel-cards">
            <div v-for="item in channelSummary" :key="item.channel" class="channel-card">
              <div class="channel-card__header">
                <StatusTag :status="channelConfig[item.channel]?.tagStatus || 'info'" :label="item.label" size="medium" />
              </div>
              <div class="channel-card__body">
                <div class="channel-card__metric">
                  <span class="channel-card__metric-label">总投入</span>
                  <span class="channel-card__metric-value">¥{{ item.totalSpend.toLocaleString() }}</span>
                </div>
                <div class="channel-card__metric">
                  <span class="channel-card__metric-label">转化人数</span>
                  <span class="channel-card__metric-value">{{ item.totalConversions }}人</span>
                </div>
                <div class="channel-card__metric">
                  <span class="channel-card__metric-label">最佳活动</span>
                  <span class="channel-card__metric-value channel-card__best-name">{{ item.bestCampaign }}</span>
                </div>
              </div>
            </div>
            <div v-if="channelSummary.length === 0" class="channel-empty">
              暂无已完成活动的渠道数据
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    </section>

    <!-- 新建/编辑活动对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogMode === 'create' ? '新建促销活动' : '编辑促销活动'"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="110px" class="campaign-form">
        <el-form-item label="活动名称" required>
          <el-input v-model="campaignForm.name" placeholder="请输入活动名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="投放渠道" required>
          <el-select v-model="campaignForm.channel" placeholder="请选择投放渠道" style="width: 100%" :teleported="false">
            <el-option
              v-for="(config, key) in channelConfig"
              :key="key"
              :label="config.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="活动周期" required>
          <el-date-picker
            v-model="campaignForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
            :teleported="false"
            @change="(val: any) => {
              if (val && val.length === 2) {
                campaignForm.startDate = val[0]
                campaignForm.endDate = val[1]
              }
            }"
          />
        </el-form-item>
        <el-form-item label="预算(元)">
          <el-input-number v-model="campaignForm.budget" :min="0" :max="500000" :step="1000" style="width: 100%" placeholder="请输入预算金额" />
        </el-form-item>
        <el-form-item label="目标转化率(%)">
          <el-input-number v-model="campaignForm.targetConversionRate" :min="1" :max="100" :step="1" style="width: 100%" placeholder="请输入目标转化率" />
        </el-form-item>
        <el-form-item label="参与门店">
          <el-checkbox-group v-model="campaignForm.storeIds">
            <el-checkbox v-for="store in storeOptions" :key="store.storeId" :value="String(store.storeId)" :label="store.storeName" />
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
/* ========== Tab 内统计区域（4列，参考税务管理 stats-section 规范） ========== */
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-5);
}

/* ========== 搜索面板 ========== */
.advanced-search-panel {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.toolbar-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--fts-space-4);
  flex-wrap: wrap;

  &__filters {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-shrink: 0;
  }
}

/* ========== 表格区域 ========== */
.table-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

/* ========== 单元格自定义样式 ========== */
.campaign-name {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.period-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.conversion-rate {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.roi-value {
  font-weight: var(--fts-font-weight-bold);
  font-size: var(--fts-font-size-sm);
  padding: 2px 8px;
  border-radius: var(--fts-radius-sm);

  &--high {
    color: var(--fts-success);
    background-color: var(--fts-success-bg);
  }

  &--medium {
    color: var(--fts-warning);
    background-color: var(--fts-warning-bg);
  }

  &--low {
    color: var(--fts-error);
    background-color: var(--fts-error-bg);
  }

  &--zero {
    color: var(--fts-text-disabled);
  }
}

/* ========== 效果评级 ========== */
.effect-grade {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: var(--fts-font-weight-bold);
  font-size: var(--fts-font-size-sm);

  &--s {
    color: var(--fts-success);
    background-color: var(--fts-success-bg);
    border: 2px solid var(--fts-success);
  }

  &--a {
    color: var(--fts-primary);
    background-color: var(--fts-primary-bg);
    border: 2px solid var(--fts-primary);
  }

  &--b {
    color: var(--fts-warning);
    background-color: var(--fts-warning-bg);
    border: 2px solid var(--fts-warning);
  }

  &--c {
    color: var(--fts-error);
    background-color: var(--fts-error-bg);
    border: 2px solid var(--fts-error);
  }
}

/* ========== 渠道汇总区域（卡片化，与表格区域视觉对齐） ========== */
.channel-summary-section {
  margin-top: var(--fts-space-5);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-4);
  overflow: hidden;
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-4) 0;
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
  line-height: 1.4;
}

.channel-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: var(--fts-space-3);
}

.channel-card {
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    transform: translateY(-1px);
  }

  &__header {
    margin-bottom: var(--fts-space-3);
  }

  &__body {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }

  &__metric {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  &__metric-label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__metric-value {
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-sm);
  }

  &__best-name {
    max-width: 160px;
    text-align: right;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.channel-empty {
  grid-column: 1 / -1;
  text-align: center;
  padding: var(--fts-space-8);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

/* ========== 对话框表单 ========== */
.campaign-form {
  padding: var(--fts-space-2) 0;
}

/* ========== 响应式适配 ========== */
@media (max-width: 1200px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }

  .toolbar-row {
    flex-direction: column;
    align-items: stretch;

    &__filters,
    &__actions {
      justify-content: center;
    }
  }

  .channel-cards {
    grid-template-columns: 1fr;
  }
}
</style>
