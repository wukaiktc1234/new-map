<script setup lang="ts">
/**
 * 员工智能画像
 * 整合综合画像、人才盘点、风险预测三大能力
 */
import { ref, computed, onMounted, watch, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import type { EChartsOption } from 'echarts'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { employeeIntelligenceApi } from '@/api/employee-intelligence'
import {
  type EmployeeProfile,
  type TalentInventoryItem,
  type QuadrantStat,
  type SuccessionPlanItem,
  type EmployeeRisk,
  type RiskStat,
  type RiskType,
  type RiskLevel,
  type TalentQuadrant,
  type WorkTrajectoryNode,
  RiskLevelLabelMap,
  RiskTypeLabelMap,
  RiskTypeIconMap,
  TalentQuadrantLabelMap,
  RenewalRecommendationLabelMap,
  ReadinessLabelMap,
  PerformanceGradeLabelMap,
} from '@/types/employee-intelligence'

/* ===== Tab 状态 ===== */
const activeTab = ref('profile')
const loading = ref(false)

/* ===== 综合画像 ===== */
const employeeList = ref<Array<{ employeeId: string; employeeName: string; department: string; position: string }>>([])
const selectedEmployeeId = ref('')
const profile = ref<EmployeeProfile | null>(null)
const profileLoading = ref(false)
const radarChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let radarChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

/* ===== 人才盘点 ===== */
const talentList = ref<TalentInventoryItem[]>([])
const quadrantStats = ref<QuadrantStat[]>([])
const successionPlans = ref<SuccessionPlanItem[]>([])

/* ===== 风险预测 ===== */
const riskList = ref<EmployeeRisk[]>([])
const riskStats = ref<RiskStat[]>([])
const riskTypeFilter = ref<'all' | RiskType>('all')
const riskLevelFilter = ref<'all' | RiskLevel>('all')
const riskDetailVisible = ref(false)
const currentRisk = ref<EmployeeRisk | null>(null)

/* ===== CSS 变量获取 ===== */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim() || 'var(--fts-primary)'
}

/* ===== 综合画像：员工选择 ===== */
async function loadEmployeeList() {
  try {
    employeeList.value = await employeeIntelligenceApi.getEmployeeList()
    if (employeeList.value.length > 0 && !selectedEmployeeId.value) {
      selectedEmployeeId.value = employeeList.value[0].employeeId
      await loadProfile()
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载员工列表失败')
  }
}

async function loadProfile() {
  if (!selectedEmployeeId.value) return
  profileLoading.value = true
  try {
    profile.value = await employeeIntelligenceApi.getEmployeeProfile(selectedEmployeeId.value)
    await nextTick()
    renderRadarChart()
    renderTrendChart()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载画像失败')
  } finally {
    profileLoading.value = false
  }
}

/* ===== 雷达图渲染 ===== */
function renderRadarChart() {
  if (!radarChartRef.value || !profile.value) return
  if (!radarChart) {
    radarChart = echarts.init(radarChartRef.value)
  }
  const dimensions = profile.value.radarDimensions
  const indicator = dimensions.map(d => ({ name: d.name, max: d.fullScore }))
  const values = dimensions.map(d => d.score)
  const primaryColor = getCssVar('--fts-primary')

  const option: EChartsOption = {
    tooltip: { trigger: 'item' },
    radar: {
      indicator,
      shape: 'polygon',
      splitNumber: 5,
      axisName: { color: getCssVar('--fts-text-secondary'), fontSize: 12 },
      splitLine: { lineStyle: { color: getCssVar('--fts-border') } },
      splitArea: { areaStyle: { color: ['transparent', getCssVar('--fts-bg-tertiary')] } },
      axisLine: { lineStyle: { color: getCssVar('--fts-border') } },
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '能力画像',
        areaStyle: { color: `${primaryColor}33` },
        lineStyle: { color: primaryColor, width: 2 },
        itemStyle: { color: primaryColor },
      }],
    }],
  }
  radarChart.setOption(option)
}

/* ===== 绩效趋势图渲染 ===== */
function renderTrendChart() {
  if (!trendChartRef.value || !profile.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }
  const trend = profile.value.performanceTrend
  const primaryColor = getCssVar('--fts-primary')
  const successColor = getCssVar('--fts-success')

  const option: EChartsOption = {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', top: '10%', containLabel: true },
    xAxis: {
      type: 'category',
      data: trend.map(t => t.period),
      axisLine: { lineStyle: { color: getCssVar('--fts-border') } },
      axisLabel: { color: getCssVar('--fts-text-secondary'), fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 50,
      max: 100,
      axisLine: { show: false },
      axisLabel: { color: getCssVar('--fts-text-secondary') },
      splitLine: { lineStyle: { color: getCssVar('--fts-border'), type: 'dashed' } },
    },
    series: [{
      type: 'line',
      data: trend.map(t => t.score),
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      lineStyle: { color: primaryColor, width: 3 },
      itemStyle: { color: primaryColor, borderColor: successColor, borderWidth: 2 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: `${primaryColor}40` },
          { offset: 1, color: `${primaryColor}05` },
        ]),
      },
      markLine: {
        silent: true,
        data: [{ yAxis: 90, lineStyle: { color: successColor, type: 'dashed' }, label: { formatter: '优秀线' } }],
      },
    }],
  }
  trendChart.setOption(option)
}

/* ===== 人才盘点：九宫格分布 ===== */
const quadrantGrid = computed(() => {
  // 3x3 网格：行为潜力（高/中/低），列为绩效（低/中/高）
  const grid: Record<string, TalentInventoryItem[]> = {
    'high-high': [], 'high-mid': [], 'high-low': [],
    'mid-high': [], 'mid-mid': [], 'mid-low': [],
    'low-high': [], 'low-mid': [], 'low-low': [],
  }
  for (const item of talentList.value) {
    const pKey = item.potentialScore >= 4 ? 'high' : item.potentialScore >= 3 ? 'mid' : 'low'
    const perfKey = item.performanceScore >= 4 ? 'high' : item.performanceScore >= 3 ? 'mid' : 'low'
    grid[`${pKey}-${perfKey}`].push(item)
  }
  return grid
})

function getQuadrantKey(potential: number, performance: number): string {
  const pKey = potential >= 4 ? 'high' : potential >= 3 ? 'mid' : 'low'
  const perfKey = performance >= 4 ? 'high' : performance >= 3 ? 'mid' : 'low'
  return `${pKey}-${perfKey}`
}

const quadrantMeta: Record<string, { label: string; class: string }> = {
  'high-high': { label: '明星员工', class: 'quad-star' },
  'high-mid': { label: '高潜人才', class: 'quad-high-potential' },
  'high-low': { label: '潜力股', class: 'quad-potential' },
  'mid-high': { label: '中坚力量', class: 'quad-solid' },
  'mid-mid': { label: '核心员工', class: 'quad-core' },
  'mid-low': { label: '待发展', class: 'quad-new' },
  'low-high': { label: '业务专家', class: 'quad-expert' },
  'low-mid': { label: '稳定员工', class: 'quad-core' },
  'low-low': { label: '待改进', class: 'quad-underperformer' },
}

/* ===== 风险预测：统计卡片 ===== */
const riskSummary = computed(() => {
  const high = riskList.value.filter(r => r.riskLevel === 'high' || r.riskLevel === 'critical').length
  const medium = riskList.value.filter(r => r.riskLevel === 'medium').length
  const low = riskList.value.filter(r => r.riskLevel === 'low').length
  const pending = riskList.value.filter(r => r.status === 'pending').length
  return { total: riskList.value.length, high, medium, low, pending }
})

const filteredRisks = computed(() => {
  return riskList.value.filter(r => {
    if (riskTypeFilter.value !== 'all' && r.riskType !== riskTypeFilter.value) return false
    if (riskLevelFilter.value !== 'all' && r.riskLevel !== riskLevelFilter.value) return false
    return true
  })
})

/* ===== 风险详情 ===== */
function openRiskDetail(risk: EmployeeRisk) {
  currentRisk.value = risk
  riskDetailVisible.value = true
}

async function handleRiskStatusChange(status: EmployeeRisk['status']) {
  if (!currentRisk.value) return
  try {
    await employeeIntelligenceApi.updateRiskStatus(currentRisk.value.riskId, status)
    currentRisk.value.status = status
    const item = riskList.value.find(r => r.riskId === currentRisk.value?.riskId)
    if (item) item.status = status
    ElMessage.success('状态已更新')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '更新失败')
  }
}

/* ===== 工具函数 ===== */
function getRiskLevelStatus(level: RiskLevel): string {
  const map: Record<RiskLevel, string> = {
    low: 'info',
    medium: 'warning',
    high: 'error',
    critical: 'error',
  }
  return map[level] || 'info'
}

function getRiskStatusStatus(status: EmployeeRisk['status']): string {
  const map: Record<EmployeeRisk['status'], string> = {
    pending: 'error',
    handling: 'warning',
    resolved: 'success',
    ignored: 'info',
  }
  return map[status] || 'info'
}

function getRiskStatusLabel(status: EmployeeRisk['status']): string {
  const map: Record<EmployeeRisk['status'], string> = {
    pending: '待处理',
    handling: '处理中',
    resolved: '已解决',
    ignored: '已忽略',
  }
  return map[status] || status
}

function getPerformanceGradeStatus(grade: string): string {
  const map: Record<string, string> = {
    S: 'error',
    A: 'success',
    B: 'primary',
    C: 'info',
    D: 'warning',
  }
  return map[grade] || 'info'
}

function getRenewalStatus(rec: string): string {
  const map: Record<string, string> = {
    strong: 'success',
    normal: 'primary',
    cautious: 'warning',
    not_recommended: 'error',
  }
  return map[rec] || 'info'
}

function getTrajectoryIcon(type: WorkTrajectoryNode['eventType']): string {
  const map: Record<WorkTrajectoryNode['eventType'], string> = {
    entry: 'User',
    transfer: 'Switch',
    promotion: 'Top',
    salary_change: 'Money',
    award: 'Trophy',
    contract: 'Document',
    training: 'Reading',
  }
  return map[type] || 'InfoFilled'
}

function getTrajectoryColor(type: WorkTrajectoryNode['eventType']): string {
  const map: Record<WorkTrajectoryNode['eventType'], string> = {
    entry: 'var(--fts-primary)',
    transfer: 'var(--fts-info)',
    promotion: 'var(--fts-success)',
    salary_change: 'var(--fts-warning)',
    award: 'var(--fts-error)',
    contract: 'var(--fts-primary)',
    training: 'var(--fts-info)',
  }
  return map[type] || 'var(--fts-primary)'
}

function formatTenure(months: number): string {
  const years = Math.floor(months / 12)
  const remainMonths = months % 12
  if (years === 0) return `${remainMonths}个月`
  if (remainMonths === 0) return `${years}年`
  return `${years}年${remainMonths}个月`
}

function getProficiencyLabel(level: number): string {
  return ['入门', '了解', '熟练', '精通', '专家'][level - 1] || '未知'
}

/* ===== 数据加载 ===== */
async function loadTalentData() {
  loading.value = true
  try {
    const [talent, stats, plans] = await Promise.all([
      employeeIntelligenceApi.getTalentInventory(),
      employeeIntelligenceApi.getQuadrantStats(),
      employeeIntelligenceApi.getSuccessionPlans(),
    ])
    talentList.value = talent
    quadrantStats.value = stats
    successionPlans.value = plans
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载人才数据失败')
  } finally {
    loading.value = false
  }
}

async function loadRiskData() {
  loading.value = true
  try {
    const [risks, stats] = await Promise.all([
      employeeIntelligenceApi.getEmployeeRisks(),
      employeeIntelligenceApi.getRiskStats(),
    ])
    riskList.value = risks
    riskStats.value = stats
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载风险数据失败')
  } finally {
    loading.value = false
  }
}

async function handleTabChange(name: string) {
  if (name === 'talent' && talentList.value.length === 0) {
    await loadTalentData()
  } else if (name === 'risk' && riskList.value.length === 0) {
    await loadRiskData()
  }
}

/* ===== 窗口大小变化 ===== */
function handleResize() {
  radarChart?.resize()
  trendChart?.resize()
}

/* ===== 员工切换 ===== */
watch(selectedEmployeeId, () => {
  if (selectedEmployeeId.value) loadProfile()
})

onMounted(() => {
  loadEmployeeList()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  radarChart?.dispose()
  trendChart?.dispose()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="员工画像" description="综合画像 · 人才盘点 · 风险预测" />

    <el-tabs v-model="activeTab" class="intelligence-tabs" @tab-change="handleTabChange">
      <!-- ============ Tab 1: 综合画像 ============ -->
      <el-tab-pane label="综合画像" name="profile">
        <!-- 员工选择器 -->
        <section class="selector-section">
          <span class="selector-label">选择员工：</span>
          <el-select
            v-model="selectedEmployeeId"
            placeholder="请选择员工"
            filterable
            style="width: 280px"
          >
            <el-option
              v-for="emp in employeeList"
              :key="emp.employeeId"
              :label="`${emp.employeeName} - ${emp.department} ${emp.position}`"
              :value="emp.employeeId"
            />
          </el-select>
        </section>

        <div v-loading="profileLoading" class="profile-container">
          <template v-if="profile">
            <!-- 上半部分：基本信息 + 雷达图 + 绩效趋势 -->
            <section class="profile-top">
              <!-- 基本信息卡片 -->
              <div class="profile-card info-card">
                <div class="card-header">
                  <span class="card-title">基本信息</span>
                  <StatusTag
                    :status="getPerformanceGradeStatus(profile.performanceGrade)"
                    :label="`绩效${profile.performanceGrade}`"
                    size="small"
                  />
                </div>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-label">姓名</span>
                    <span class="info-value">{{ profile.employeeName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">工号</span>
                    <span class="info-value">{{ profile.employeeCode }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">部门</span>
                    <span class="info-value">{{ profile.department }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">职位</span>
                    <span class="info-value">{{ profile.position }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">司龄</span>
                    <span class="info-value">{{ formatTenure(profile.tenureMonths) }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">入职日期</span>
                    <span class="info-value">{{ profile.entryDate }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-label">合同到期</span>
                    <span class="info-value">{{ profile.contractEndDate }}</span>
                  </div>
                </div>
                <div class="score-section">
                  <div class="overall-score">
                    <span class="score-label">综合评分</span>
                    <span class="score-value">{{ profile.overallScore }}</span>
                  </div>
                  <div class="potential-section">
                    <span class="potential-label">潜力等级</span>
                    <el-rate v-model="profile.potentialLevel" disabled size="small" />
                  </div>
                </div>
                <div class="renewal-section">
                  <div class="renewal-tag">
                    <StatusTag
                      :status="getRenewalStatus(profile.renewalRecommendation)"
                      :label="RenewalRecommendationLabelMap[profile.renewalRecommendation]"
                      size="small"
                    />
                  </div>
                  <div class="renewal-reason">{{ profile.renewalReason }}</div>
                </div>
              </div>

              <!-- 雷达图卡片 -->
              <div class="profile-card chart-card">
                <div class="card-header">
                  <span class="card-title">能力画像</span>
                </div>
                <div ref="radarChartRef" class="chart-container" />
              </div>

              <!-- 绩效趋势卡片 -->
              <div class="profile-card chart-card">
                <div class="card-header">
                  <span class="card-title">绩效趋势</span>
                </div>
                <div ref="trendChartRef" class="chart-container" />
              </div>
            </section>

            <!-- 中间部分：优势 + 待提升 + 关键成就 -->
            <section class="profile-middle">
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">核心优势</span>
                </div>
                <div class="tag-list">
                  <StatusTag v-for="s in profile.strengths" :key="s" status="success" :label="s" size="small" />
                </div>
              </div>
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">待提升项</span>
                </div>
                <div class="tag-list">
                  <StatusTag v-for="i in profile.improvements" :key="i" status="warning" :label="i" size="small" />
                </div>
              </div>
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">关键成就</span>
                </div>
                <ul class="achievement-list">
                  <li v-for="a in profile.achievements" :key="a">{{ a }}</li>
                </ul>
              </div>
            </section>

            <!-- 下半部分：技能 + 培训记录 -->
            <section class="profile-skills">
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">技能标签</span>
                </div>
                <div class="skill-list">
                  <div v-for="skill in profile.skills" :key="skill.skillId" class="skill-item">
                    <div class="skill-header">
                      <span class="skill-name">
                        <el-icon v-if="skill.isCore" class="core-icon"><Star /></el-icon>
                        {{ skill.skillName }}
                      </span>
                      <span class="skill-proficiency">{{ getProficiencyLabel(skill.proficiency) }}</span>
                    </div>
                    <el-progress
                      :percentage="skill.proficiency * 20"
                      :stroke-width="6"
                      :show-text="false"
                      :color="skill.proficiency >= 4 ? 'var(--fts-success)' : 'var(--fts-primary)'"
                    />
                    <div class="skill-meta">
                      <span class="skill-category">{{ skill.category }}</span>
                      <span v-if="skill.lastUsedTime" class="skill-last">最近使用：{{ skill.lastUsedTime }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 工作轨迹时间线 -->
            <section class="profile-trajectory">
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">工作轨迹</span>
                </div>
                <el-timeline class="trajectory-timeline">
                  <el-timeline-item
                    v-for="node in profile.workTrajectory"
                    :key="node.nodeId"
                    :timestamp="node.date"
                    placement="top"
                    :color="getTrajectoryColor(node.eventType)"
                  >
                    <div class="trajectory-content">
                      <div class="trajectory-title">{{ node.title }}</div>
                      <div class="trajectory-desc">{{ node.description }}</div>
                      <div v-if="node.fromValue || node.toValue" class="trajectory-change">
                        <span v-if="node.fromValue">{{ node.fromValue }}</span>
                        <el-icon v-if="node.fromValue && node.toValue"><Right /></el-icon>
                        <span v-if="node.toValue">{{ node.toValue }}</span>
                      </div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </section>

            <!-- 培训记录 -->
            <section class="profile-training">
              <div class="profile-card">
                <div class="card-header">
                  <span class="card-title">培训记录</span>
                </div>
                <el-table :data="profile.trainingRecords" stripe size="small">
                  <el-table-column prop="courseName" label="课程名称" min-width="180" />
                  <el-table-column label="培训类型" width="100">
                    <template #default="{ row }">
                      <StatusTag status="info" size="small" :label="({ internal: '内部', external: '外部', online: '线上', certification: '认证' } as Record<string, string>)[row.trainingType]" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="completeTime" label="完成时间" width="110" />
                  <el-table-column label="成绩" width="80">
                    <template #default="{ row }">
                      <span :style="{ color: row.passed ? 'var(--fts-success)' : 'var(--fts-error)' }">{{ row.score || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column label="结果" width="80">
                    <template #default="{ row }">
                      <StatusTag :status="row.passed ? 'success' : 'error'" :label="row.passed ? '通过' : '未通过'" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column prop="certificateNo" label="证书编号" width="140" />
                </el-table>
              </div>
            </section>
          </template>
        </div>
      </el-tab-pane>

      <!-- ============ Tab 2: 人才盘点 ============ -->
      <el-tab-pane label="人才盘点" name="talent">
        <div v-loading="loading">
          <!-- 九宫格 -->
          <section class="quadrant-section">
            <div class="section-title">人才九宫格（潜力 × 绩效）</div>
            <div class="quadrant-grid">
              <div class="quadrant-axis y-axis">
                <div class="axis-label">高</div>
                <div class="axis-label">潜<br>力</div>
                <div class="axis-label">低</div>
              </div>
              <div class="quadrant-cells">
                <div
                  v-for="(meta, key) in quadrantMeta"
                  :key="key"
                  :class="['quadrant-cell', meta.class]"
                >
                  <div class="cell-header">
                    <span class="cell-label">{{ meta.label }}</span>
                    <span class="cell-count">{{ quadrantGrid[key]?.length || 0 }}人</span>
                  </div>
                  <div class="cell-employees">
                    <div
                      v-for="emp in (quadrantGrid[key] || []).slice(0, 4)"
                      :key="emp.employeeId"
                      class="employee-chip"
                      :title="emp.summary"
                    >
                      {{ emp.employeeName }}
                    </div>
                    <div v-if="(quadrantGrid[key]?.length || 0) > 4" class="employee-more">
                      +{{ quadrantGrid[key].length - 4 }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="quadrant-axis x-axis">
              <span>低</span>
              <span>绩效</span>
              <span>高</span>
            </div>
          </section>

          <!-- 人才列表 -->
          <section class="talent-list-section">
            <div class="section-title">人才清单</div>
            <el-table :data="talentList" stripe size="small">
              <el-table-column prop="employeeName" label="员工" width="80" />
              <el-table-column prop="department" label="部门" width="100" />
              <el-table-column prop="position" label="职位" width="100" />
              <el-table-column label="绩效" width="120">
                <template #default="{ row }">
                  <el-rate v-model="row.performanceScore" disabled size="small" />
                </template>
              </el-table-column>
              <el-table-column label="潜力" width="120">
                <template #default="{ row }">
                  <el-rate v-model="row.potentialScore" disabled size="small" />
                </template>
              </el-table-column>
              <el-table-column label="象限" width="110">
                <template #default="{ row }">
                  <StatusTag status="info" size="small" :label="TalentQuadrantLabelMap[row.quadrant as TalentQuadrant]" />
                </template>
              </el-table-column>
              <el-table-column label="司龄" width="90">
                <template #default="{ row }">{{ formatTenure(row.tenureMonths) }}</template>
              </el-table-column>
              <el-table-column label="标签" width="180">
                <template #default="{ row }">
                  <StatusTag v-if="row.isHighPotential" status="success" label="高潜" size="small" />
                  <StatusTag v-if="row.isSuccessor" status="active" label="继任" size="small" />
                  <StatusTag v-if="row.isKeyPosition" status="warning" label="关键岗" size="small" />
                </template>
              </el-table-column>
              <el-table-column prop="summary" label="画像摘要" min-width="200" show-overflow-tooltip />
            </el-table>
          </section>

          <!-- 继任计划 -->
          <section class="succession-section">
            <div class="section-title">继任计划</div>
            <div class="succession-grid">
              <div v-for="plan in successionPlans" :key="plan.positionId" class="succession-card">
                <div class="succession-header">
                  <div class="position-info">
                    <span class="position-name">{{ plan.positionName }}</span>
                    <span class="position-dept">{{ plan.department }}</span>
                  </div>
                  <StatusTag
                    :status="plan.readiness === 'ready' ? 'success' : 'primary'"
                    :label="ReadinessLabelMap[plan.readiness]"
                    size="small"
                  />
                </div>
                <div class="current-holder">现任：{{ plan.currentHolder }}</div>
                <div class="candidates">
                  <div class="candidates-title">继任候选人（{{ plan.candidates.length }}）</div>
                  <div v-for="c in plan.candidates" :key="c.employeeId" class="candidate-item">
                    <div class="candidate-header">
                      <span class="candidate-name">{{ c.employeeName }}</span>
                      <span class="candidate-match" :style="{ color: c.matchScore >= 80 ? 'var(--fts-success)' : 'var(--fts-primary)' }">
                        匹配度 {{ c.matchScore }}%
                      </span>
                    </div>
                    <div class="candidate-info">{{ c.currentDepartment }} · {{ c.currentPosition }}</div>
                    <div class="candidate-readiness">
                      <StatusTag
                        :status="c.readiness === 'ready' ? 'success' : 'primary'"
                        :label="ReadinessLabelMap[c.readiness]"
                        size="small"
                      />
                    </div>
                    <div v-if="c.gaps.length > 0" class="candidate-gaps">
                      <span class="gaps-label">差距：</span>
                      <span v-for="(g, idx) in c.gaps" :key="idx" class="gap-item">{{ g }}</span>
                    </div>
                  </div>
                  <div v-if="plan.candidates.length === 0" class="no-candidate">暂无候选人</div>
                </div>
              </div>
            </div>
          </section>
        </div>
      </el-tab-pane>

      <!-- ============ Tab 3: 风险预测 ============ -->
      <el-tab-pane label="风险预测" name="risk">
        <div v-loading="loading">
          <!-- 风险统计 -->
          <section class="stats-section">
            <StatCard icon="Warning" label="风险总数" :value="riskSummary.total" color-type="primary" variant="bordered" />
            <StatCard icon="CircleCloseFilled" label="高/极高风险" :value="riskSummary.high" color-type="error" variant="bordered" />
            <StatCard icon="WarningFilled" label="中风险" :value="riskSummary.medium" color-type="warning" variant="bordered" />
            <StatCard icon="Bell" label="待处理" :value="riskSummary.pending" color-type="error" variant="bordered" />
          </section>

          <!-- 过滤栏 -->
          <section class="filter-section">
            <div class="filter-group">
              <span class="filter-label">风险类型：</span>
              <el-radio-group v-model="riskTypeFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button v-for="(_, type) in RiskTypeLabelMap" :key="type" :value="type">{{ RiskTypeLabelMap[type as RiskType] }}</el-radio-button>
              </el-radio-group>
            </div>
            <div class="filter-group">
              <span class="filter-label">风险等级：</span>
              <el-radio-group v-model="riskLevelFilter" size="small">
                <el-radio-button value="all">全部</el-radio-button>
                <el-radio-button value="critical">极高</el-radio-button>
                <el-radio-button value="high">高</el-radio-button>
                <el-radio-button value="medium">中</el-radio-button>
                <el-radio-button value="low">低</el-radio-button>
              </el-radio-group>
            </div>
          </section>

          <!-- 风险列表 -->
          <section class="risk-list-section">
            <el-table :data="filteredRisks" stripe size="small">
              <el-table-column label="风险等级" width="100">
                <template #default="{ row }">
                  <StatusTag :status="getRiskLevelStatus(row.riskLevel)" :label="RiskLevelLabelMap[row.riskLevel as RiskLevel]" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="风险类型" width="110">
                <template #default="{ row }">{{ RiskTypeLabelMap[row.riskType as RiskType] }}</template>
              </el-table-column>
              <el-table-column prop="employeeName" label="员工" width="80" />
              <el-table-column prop="department" label="部门" width="100" />
              <el-table-column prop="position" label="职位" width="100" />
              <el-table-column label="风险概率" width="120">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.riskScore"
                    :stroke-width="8"
                    :color="row.riskScore >= 70 ? 'var(--fts-error)' : row.riskScore >= 50 ? 'var(--fts-warning)' : 'var(--fts-info)'"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="detectedTime" label="发现时间" width="110" />
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <StatusTag :status="getRiskStatusStatus(row.status)" :label="getRiskStatusLabel(row.status)" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openRiskDetail(row)">详情</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无风险项" :image-size="60" />
              </template>
            </el-table>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 风险详情对话框 -->
    <el-dialog v-model="riskDetailVisible" title="风险详情" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <template v-if="currentRisk">
        <div class="risk-detail">
          <!-- 风险概览 -->
          <div class="detail-header">
            <div class="detail-employee">
              <span class="employee-name">{{ currentRisk.employeeName }}</span>
              <span class="employee-info">{{ currentRisk.department }} · {{ currentRisk.position }}</span>
            </div>
            <div class="detail-tags">
              <StatusTag :status="getRiskLevelStatus(currentRisk.riskLevel)" :label="RiskLevelLabelMap[currentRisk.riskLevel]" size="small" />
              <StatusTag :status="getRiskStatusStatus(currentRisk.status)" :label="getRiskStatusLabel(currentRisk.status)" size="small" />
              <StatusTag status="info" size="small" :label="RiskTypeLabelMap[currentRisk.riskType]" />
            </div>
          </div>

          <!-- 风险评分 -->
          <div class="detail-score">
            <div class="score-circle">
              <div class="score-num">{{ currentRisk.riskScore }}</div>
              <div class="score-label">风险概率</div>
            </div>
            <div class="score-desc">
              <div class="desc-title">风险描述</div>
              <div class="desc-content">{{ currentRisk.description }}</div>
              <div class="desc-time">发现时间：{{ currentRisk.detectedTime }}</div>
            </div>
          </div>

          <!-- 风险因素 -->
          <div class="detail-factors">
            <div class="section-title-small">风险因素分析</div>
            <div v-for="factor in currentRisk.factors" :key="factor.factorName" class="factor-item">
              <div class="factor-header">
                <span class="factor-name">
                  <el-icon v-if="factor.triggered" color="var(--fts-error)"><WarningFilled /></el-icon>
                  <el-icon v-else color="var(--fts-success)"><CircleCheckFilled /></el-icon>
                  {{ factor.factorName }}
                </span>
                <span class="factor-weight">权重 {{ (factor.weight * 100).toFixed(0) }}%</span>
              </div>
              <div class="factor-content">
                <span class="factor-value">实际值：{{ factor.actualValue }}</span>
                <span class="factor-desc">{{ factor.description }}</span>
              </div>
            </div>
          </div>

          <!-- 处理建议 -->
          <div class="detail-recommendations">
            <div class="section-title-small">处理建议</div>
            <ol class="recommendation-list">
              <li v-for="(rec, idx) in currentRisk.recommendations" :key="idx">{{ rec }}</li>
            </ol>
          </div>
        </div>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="riskDetailVisible = false">关闭</el-button>
          <el-button v-if="currentRisk?.status === 'pending'" type="warning" @click="handleRiskStatusChange('handling')">开始处理</el-button>
          <el-button v-if="currentRisk?.status !== 'resolved'" type="success" @click="handleRiskStatusChange('resolved')">标记已解决</el-button>
          <el-button v-if="currentRisk?.status === 'pending'" @click="handleRiskStatusChange('ignored')">忽略</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.intelligence-tabs {
  :deep(.el-tabs__content) {
    padding-top: var(--fts-space-2);
  }
}

/* ===== 综合画像 ===== */
.selector-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);

  .selector-label {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-secondary);
  }
}

.profile-container {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.profile-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-light);

  .card-title {
    font-size: var(--fts-font-size-base);
    font-weight: 600;
    color: var(--fts-text-primary);
  }
}

.profile-top {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--fts-space-4);
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-3);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .info-label {
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }

  .info-value {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    font-weight: 500;
  }
}

.score-section {
  display: flex;
  justify-content: space-around;
  padding: var(--fts-space-3) 0;
  border-top: 1px solid var(--fts-border-light);
  border-bottom: 1px solid var(--fts-border-light);
  margin-bottom: var(--fts-space-3);

  .overall-score {
    text-align: center;

    .score-label {
      display: block;
      font-size: 12px;
      color: var(--fts-text-tertiary);
      margin-bottom: 4px;
    }

    .score-value {
      font-size: 32px;
      font-weight: 700;
      color: var(--fts-primary);
    }
  }

  .potential-section {
    text-align: center;

    .potential-label {
      display: block;
      font-size: 12px;
      color: var(--fts-text-tertiary);
      margin-bottom: 4px;
    }
  }
}

.renewal-section {
  .renewal-tag {
    margin-bottom: var(--fts-space-2);
  }

  .renewal-reason {
    font-size: 13px;
    color: var(--fts-text-secondary);
    line-height: 1.5;
  }
}

.chart-card {
  display: flex;
  flex-direction: column;
}

.chart-container {
  flex: 1;
  min-height: 280px;
  width: 100%;
}

.profile-middle {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: var(--fts-space-4);
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-2);
}

.achievement-list {
  margin: 0;
  padding-left: var(--fts-space-4);

  li {
    font-size: 13px;
    color: var(--fts-text-secondary);
    line-height: 1.8;
  }
}

.profile-skills {
  .skill-list {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--fts-space-3);
  }

  .skill-item {
    padding: var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
  }

  .skill-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);

    .skill-name {
      font-size: 13px;
      font-weight: 500;
      color: var(--fts-text-primary);
      display: flex;
      align-items: center;
      gap: 4px;

      .core-icon {
        color: var(--fts-warning);
      }
    }

    .skill-proficiency {
      font-size: 12px;
      color: var(--fts-text-tertiary);
    }
  }

  .skill-meta {
    display: flex;
    justify-content: space-between;
    margin-top: var(--fts-space-2);
    font-size: 11px;
    color: var(--fts-text-tertiary);
  }
}

.trajectory-timeline {
  padding: var(--fts-space-2) 0;

  .trajectory-content {
    .trajectory-title {
      font-size: 14px;
      font-weight: 500;
      color: var(--fts-text-primary);
      margin-bottom: 4px;
    }

    .trajectory-desc {
      font-size: 13px;
      color: var(--fts-text-secondary);
      line-height: 1.5;
    }

    .trajectory-change {
      display: flex;
      align-items: center;
      gap: var(--fts-space-2);
      margin-top: var(--fts-space-1);
      font-size: 12px;
      color: var(--fts-text-tertiary);
    }
  }
}

/* ===== 人才盘点 ===== */
.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
}

.section-title-small {
  font-size: 14px;
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-light);
}

.quadrant-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.quadrant-grid {
  display: grid;
  grid-template-columns: 40px 1fr;
  gap: var(--fts-space-2);
}

.quadrant-axis {
  display: flex;
  align-items: center;
  justify-content: space-around;
  font-size: 12px;
  color: var(--fts-text-tertiary);

  &.y-axis {
    flex-direction: column;
    padding: var(--fts-space-2) 0;
  }

  .axis-label {
    text-align: center;
  }
}

.quadrant-cells {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  grid-template-rows: repeat(3, 1fr);
  gap: var(--fts-space-2);
}

.quadrant-cell {
  padding: var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  min-height: 120px;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--fts-border-light);

  .cell-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-2);

    .cell-label {
      font-size: 13px;
      font-weight: 600;
    }

    .cell-count {
      font-size: 12px;
      color: var(--fts-text-tertiary);
    }
  }

  .cell-employees {
    display: flex;
    flex-wrap: wrap;
    gap: 4px;
  }

  .employee-chip {
    padding: 2px 8px;
    background: var(--fts-bg-card);
    border-radius: 10px;
    font-size: 11px;
    color: var(--fts-text-primary);
  }

  .employee-more {
    padding: 2px 8px;
    font-size: 11px;
    color: var(--fts-text-tertiary);
  }

  &.quad-star { background: var(--fts-warning-bg); .cell-label { color: var(--fts-warning); } }
  &.quad-high-potential { background: var(--fts-primary-bg); .cell-label { color: var(--fts-primary); } }
  &.quad-solid { background: var(--fts-success-bg); .cell-label { color: var(--fts-success); } }
  &.quad-core { background: var(--fts-info-bg); .cell-label { color: var(--fts-info); } }
  &.quad-expert { background: var(--fts-primary-bg); .cell-label { color: var(--fts-primary); } }
  &.quad-potential { background: var(--fts-info-bg); .cell-label { color: var(--fts-info); } }
  &.quad-new { background: var(--fts-bg-secondary); .cell-label { color: var(--fts-text-secondary); } }
  &.quad-underperformer { background: var(--fts-error-bg); .cell-label { color: var(--fts-error); } }
  &.quad-risk { background: var(--fts-warning-bg); .cell-label { color: var(--fts-warning); } }
}

.x-axis {
  display: grid;
  grid-template-columns: 40px 1fr;
  margin-top: var(--fts-space-2);

  span {
    grid-column: 2;
    display: flex;
    justify-content: space-around;
    font-size: 12px;
    color: var(--fts-text-tertiary);
  }
}

.talent-list-section,
.succession-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

.succession-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-4);
}

.succession-card {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  padding: var(--fts-space-3);

  .succession-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--fts-space-2);

    .position-info {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .position-name {
      font-size: 14px;
      font-weight: 600;
      color: var(--fts-text-primary);
    }

    .position-dept {
      font-size: 12px;
      color: var(--fts-text-tertiary);
    }
  }

  .current-holder {
    font-size: 12px;
    color: var(--fts-text-secondary);
    margin-bottom: var(--fts-space-3);
    padding-bottom: var(--fts-space-2);
    border-bottom: 1px dashed var(--fts-border);
  }

  .candidates-title {
    font-size: 12px;
    color: var(--fts-text-tertiary);
    margin-bottom: var(--fts-space-2);
  }

  .candidate-item {
    padding: var(--fts-space-2);
    background: var(--fts-bg-card);
    border-radius: var(--fts-radius-sm);
    margin-bottom: var(--fts-space-2);

    .candidate-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 4px;

      .candidate-name {
        font-size: 13px;
        font-weight: 500;
        color: var(--fts-text-primary);
      }

      .candidate-match {
        font-size: 12px;
        font-weight: 600;
      }
    }

    .candidate-info {
      font-size: 11px;
      color: var(--fts-text-tertiary);
      margin-bottom: 4px;
    }

    .candidate-readiness {
      margin-bottom: 4px;
    }

    .candidate-gaps {
      font-size: 11px;
      color: var(--fts-text-tertiary);

      .gaps-label {
        color: var(--fts-text-secondary);
      }

      .gap-item {
        display: inline-block;
        margin-right: var(--fts-space-1);
      }
    }
  }

  .no-candidate {
    text-align: center;
    font-size: 12px;
    color: var(--fts-text-tertiary);
    padding: var(--fts-space-3);
  }
}

/* ===== 风险预测 ===== */
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
}

.filter-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  .filter-group {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);

    .filter-label {
      font-size: 13px;
      color: var(--fts-text-secondary);
      min-width: 70px;
    }
  }
}

.risk-list-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  box-shadow: var(--fts-shadow-sm);
}

/* ===== 风险详情对话框 ===== */
.risk-detail {
  .detail-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-light);
    margin-bottom: var(--fts-space-3);

    .detail-employee {
      display: flex;
      flex-direction: column;
      gap: 2px;

      .employee-name {
        font-size: 16px;
        font-weight: 600;
        color: var(--fts-text-primary);
      }

      .employee-info {
        font-size: 12px;
        color: var(--fts-text-tertiary);
      }
    }

    .detail-tags {
      display: flex;
      gap: var(--fts-space-2);
    }
  }

  .detail-score {
    display: flex;
    gap: var(--fts-space-4);
    padding: var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
    margin-bottom: var(--fts-space-4);

    .score-circle {
      width: 100px;
      height: 100px;
      border-radius: 50%;
      background: linear-gradient(135deg, var(--fts-error), var(--fts-warning));
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      color: var(--fts-text-inverse);
      flex-shrink: 0;

      .score-num {
        font-size: 28px;
        font-weight: 700;
      }

      .score-label {
        font-size: 11px;
      }
    }

    .score-desc {
      flex: 1;

      .desc-title {
        font-size: 13px;
        font-weight: 600;
        color: var(--fts-text-primary);
        margin-bottom: var(--fts-space-2);
      }

      .desc-content {
        font-size: 13px;
        color: var(--fts-text-secondary);
        line-height: 1.6;
        margin-bottom: var(--fts-space-2);
      }

      .desc-time {
        font-size: 12px;
        color: var(--fts-text-tertiary);
      }
    }
  }

  .detail-factors {
    margin-bottom: var(--fts-space-4);

    .factor-item {
      padding: var(--fts-space-2);
      background: var(--fts-bg-secondary);
      border-radius: var(--fts-radius-sm);
      margin-bottom: var(--fts-space-2);

      .factor-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 4px;

        .factor-name {
          font-size: 13px;
          font-weight: 500;
          color: var(--fts-text-primary);
          display: flex;
          align-items: center;
          gap: 4px;
        }

        .factor-weight {
          font-size: 11px;
          color: var(--fts-text-tertiary);
        }
      }

      .factor-content {
        display: flex;
        flex-direction: column;
        gap: 2px;
        font-size: 12px;
        color: var(--fts-text-secondary);

        .factor-value {
          color: var(--fts-text-primary);
        }
      }
    }
  }

  .detail-recommendations {
    .recommendation-list {
      margin: 0;
      padding-left: var(--fts-space-4);

      li {
        font-size: 13px;
        color: var(--fts-text-secondary);
        line-height: 1.8;
      }
    }
  }
}

/* ===== 响应式 ===== */
@media (max-width: 1200px) {
  .profile-top,
  .profile-middle {
    grid-template-columns: 1fr;
  }

  .succession-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .quadrant-cells {
    grid-template-columns: repeat(3, 1fr);
  }

  .quadrant-cell {
    min-height: 80px;
  }
}
</style>
