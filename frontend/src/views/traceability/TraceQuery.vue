<script setup lang="ts">
/**
 * 溯源查询页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】通过溯源码查询食品完整供应链信息，展示溯源链条
 * 【依赖】L3(PageHeader/StatCard/StatusTag)
 *
 * 功能：
 * - 溯源码/产品名称搜索
 * - 扫码查询入口
 * - 最近查询历史
 * - 溯源链条展示（产品基本信息、溯源流程图、各环节详情、质检报告）
 * - 标签页切换（溯源链条、质检信息、供应链信息）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Refresh, Food as FoodIcon, Location, User, Clock, Document } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { traceCodeApi } from '@/api/traceability'
import type { TraceCodeVO, TraceChainNodeVO } from '@/types/traceability'
import { ChainNodeTypeMap, TraceCodeStatusMap } from '@/types/traceability'

// ==================== 类型定义 ====================

/** 最近查询记录 */
interface HistoryItem {
  traceCode: string
  productName: string
  queryTime: string
}

/** 溯源环节类型 */
type TraceStage = 'supplier' | 'purchase' | 'production' | 'inspection' | 'warehouse' | 'delivery' | 'sale'

// ==================== 响应式数据 ====================

/** 搜索关键词（溯源码/产品名称） */
const searchKeyword = ref('')

/** 查询加载状态 */
const loading = ref(false)

/** 是否已执行查询 */
const hasQueried = ref(false)

/** 查询结果数据 */
const traceResult = ref<TraceCodeVO | null>(null)

/** 当前激活的标签页 */
const activeTab = ref('chain')

/** 扫码查询弹窗显隐 */
const scanDialogVisible = ref(false)

/** 最近查询历史（模拟本地存储） */
const queryHistory = ref<HistoryItem[]>([
  { traceCode: 'TC2026070100001', productName: '有机蔬菜沙拉', queryTime: '2026-07-05 14:30' },
  { traceCode: 'TC2026070100002', productName: '红烧牛肉面', queryTime: '2026-07-04 09:15' },
  { traceCode: 'TC2026070100003', productName: '清蒸鲈鱼', queryTime: '2026-07-03 18:45' },
])

/** 溯源环节配置（用于流程图展示） */
const traceStages = reactive<Array<{ key: TraceStage; label: string; icon: string }>>([
  { key: 'supplier', label: '供应商', icon: 'OfficeBuilding' },
  { key: 'purchase', label: '采购入库', icon: 'Box' },
  { key: 'production', label: '生产加工', icon: 'Setting' },
  { key: 'inspection', label: '质检', icon: 'CircleCheck' },
  { key: 'warehouse', label: '仓储', icon: 'Warehouse' },
  { key: 'delivery', label: '配送', icon: 'Van' },
  { key: 'sale', label: '销售', icon: 'ShoppingCart' },
])

// ==================== 计算属性 ====================

/** 溯源链节点（按类型分组） */
const chainNodesByType = computed(() => {
  const nodes = traceResult.value?.chainNodes || []
  const grouped: Record<number, TraceChainNodeVO[]> = {}
  nodes.forEach(node => {
    if (!grouped[node.nodeType]) {
      grouped[node.nodeType] = []
    }
    grouped[node.nodeType].push(node)
  })
  return grouped
})

/** 已完成的环节数 */
const completedStagesCount = computed(() => {
  return Object.keys(chainNodesByType.value).length
})

/** 统计数据 */
const statistics = computed(() => ({
  totalNodes: traceResult.value?.chainNodes?.length || 0,
  passedInspection: 0,
  totalInspection: 0,
  riskLevel: traceResult.value?.riskLevel || 0,
}))

// ==================== 方法 ====================

/** 执行查询 */
async function handleSearch(): Promise<void> {
  if (!searchKeyword.value.trim()) {
    ElMessage.warning('请输入溯源码或产品名称')
    return
  }

  loading.value = true
  hasQueried.value = true
  try {
    const result = await traceCodeApi.queryByTraceCode(searchKeyword.value.trim())
    traceResult.value = result

    if (result) {
      addToHistory(result)
    }
  } catch (error) {
    ElMessage.error('查询失败，请稍后重试')
    traceResult.value = null
  } finally {
    loading.value = false
  }
}

/** 重置搜索 */
function handleReset(): void {
  searchKeyword.value = ''
  traceResult.value = null
  hasQueried.value = false
  activeTab.value = 'chain'
}

/** 添加到查询历史 */
function addToHistory(result: TraceCodeVO): void {
  const newItem: HistoryItem = {
    traceCode: result.traceCode,
    productName: result.productName,
    queryTime: formatDateTime(new Date().toISOString()),
  }

  const existsIndex = queryHistory.value.findIndex(h => h.traceCode === result.traceCode)
  if (existsIndex > -1) {
    queryHistory.value.splice(existsIndex, 1)
  }

  queryHistory.value.unshift(newItem)

  if (queryHistory.value.length > 10) {
    queryHistory.value = queryHistory.value.slice(0, 10)
  }
}

/** 从历史记录查询 */
function queryFromHistory(item: HistoryItem): void {
  searchKeyword.value = item.traceCode
  handleSearch()
}

/** 打开扫码查询 */
function openScanDialog(): void {
  scanDialogVisible.value = true
}

/** 扫码查询（模拟） */
function handleScanCode(code: string): void {
  searchKeyword.value = code
  scanDialogVisible.value = false
  handleSearch()
}

/** 获取节点类型标签 */
function getNodeTypeLabel(nodeType: number): string {
  return ChainNodeTypeMap[nodeType]?.label || '未知环节'
}

/** 获取节点类型状态色 */
function getNodeTypeStatus(nodeType: number): string {
  const typeMap: Record<number, string> = {
    1: 'primary',
    2: 'success',
    3: 'warning',
    4: 'warning',
    5: 'success',
    6: 'info',
    7: 'info',
  }
  return typeMap[nodeType] || 'info'
}

/** 格式化日期时间 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取风险等级标签 */
function getRiskLevelLabel(riskLevel?: number): string {
  if (!riskLevel) return '-'
  const map: Record<number, string> = {
    1: '低风险',
    2: '中风险',
    3: '高风险',
  }
  return map[riskLevel] || '-'
}

/** 获取风险等级状态色 */
function getRiskLevelStatus(riskLevel?: number): string {
  if (!riskLevel) return 'info'
  const map: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'error',
  }
  return map[riskLevel] || 'info'
}

/** 获取状态标签 */
function getStatusLabel(status: string): string {
  const s = status as keyof typeof TraceCodeStatusMap
  return TraceCodeStatusMap[s]?.label || status
}

/** 获取状态类型 */
function getStatusType(status: string): string {
  const s = status as keyof typeof TraceCodeStatusMap
  return TraceCodeStatusMap[s]?.type || 'info'
}

// ==================== 生命周期 ====================

onMounted(() => {
  // 页面加载时不自动查询，等待用户输入
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="追溯查询" description="通过溯源码查询食品完整供应链信息">
      <el-button type="primary" size="default" @click="openScanDialog">
        <el-icon :size="16"><Search /></el-icon>扫码查询
      </el-button>
    </PageHeader>

    <!-- 搜索区域 -->
    <section class="search-section">
      <div class="search-panel">
        <div class="search-input-group">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入溯源码或产品名称..."
            size="large"
            clearable
            @keyup.enter="handleSearch"
            @clear="handleReset"
          >
            <template #prefix>
              <el-icon :size="18"><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" :loading="loading" @click="handleSearch">
            查询
          </el-button>
          <el-button size="large" @click="handleReset">
            <el-icon :size="16"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </section>

    <!-- 快速查询区域 -->
    <section class="quick-section">
      <div class="quick-card">
        <div class="quick-card__header">
          <el-icon class="quick-card__icon"><Clock /></el-icon>
          <span class="quick-card__title">最近查询</span>
        </div>
        <div class="quick-card__content">
          <div
            v-for="item in queryHistory"
            :key="item.traceCode"
            class="history-item"
            @click="queryFromHistory(item)"
          >
            <div class="history-item__code">{{ item.traceCode }}</div>
            <div class="history-item__name">{{ item.productName }}</div>
            <div class="history-item__time">{{ item.queryTime }}</div>
          </div>
          <div v-if="queryHistory.length === 0" class="empty-history">
            <el-text type="info">暂无查询记录</el-text>
          </div>
        </div>
      </div>
    </section>

    <!-- 查询结果区域 -->
    <section v-if="hasQueried" class="result-section" v-loading="loading">
      <!-- 无结果提示 -->
      <div v-if="!traceResult && !loading" class="no-result">
        <el-empty description="未找到相关溯源信息，请检查溯源码是否正确">
          <el-button type="primary" @click="handleReset">重新查询</el-button>
        </el-empty>
      </div>

      <!-- 查询结果 -->
      <template v-if="traceResult">
        <!-- 产品基本信息卡片 -->
        <div class="product-info-card">
          <div class="product-info__header">
            <div class="product-info__main">
              <el-avatar :size="64" :icon="FoodIcon" shape="square" />
              <div class="product-info__text">
                <h2 class="product-name">{{ traceResult.productName }}</h2>
                <div class="product-meta">
                  <span class="meta-item">
                    <el-icon><Document /></el-icon>
                    溯源码：{{ traceResult.traceCode }}
                  </span>
                  <span v-if="traceResult.batchNumber" class="meta-item">
                    <el-icon><Box /></el-icon>
                    批次号：{{ traceResult.batchNumber }}
                  </span>
                  <span v-if="traceResult.supplierName" class="meta-item">
                    <el-icon><OfficeBuilding /></el-icon>
                    供应商：{{ traceResult.supplierName }}
                  </span>
                </div>
              </div>
            </div>
            <div class="product-info__status">
              <StatusTag :status="getStatusType(traceResult.status)" :label="getStatusLabel(traceResult.status)" size="large" variant="light" />
              <StatusTag v-if="traceResult.riskLevel" :status="getRiskLevelStatus(traceResult.riskLevel)" :label="getRiskLevelLabel(traceResult.riskLevel)" size="large" variant="light" />
            </div>
          </div>
        </div>

        <!-- 统计卡片 -->
        <div class="stats-section">
          <StatCard icon="Connection" label="溯源节点数" :value="String(statistics.totalNodes)" color-type="primary" variant="bordered" />
          <StatCard icon="CircleCheck" label="已完成环节" :value="String(completedStagesCount)" color-type="success" variant="bordered" />
          <StatCard icon="Document" label="质检报告" :value="String(statistics.totalInspection)" color-type="info" variant="bordered" />
          <StatCard icon="View" label="扫码次数" :value="String(traceResult.scanCount || 0)" color-type="warning" variant="bordered" />
        </div>

        <!-- 标签页切换 -->
        <div class="tabs-card">
          <el-tabs v-model="activeTab" class="result-tabs">
            <el-tab-pane label="溯源链条" name="chain" />
            <el-tab-pane label="质检信息" name="inspection" />
            <el-tab-pane label="供应链信息" name="supply" />
          </el-tabs>

          <!-- 溯源链条标签页 -->
          <div v-show="activeTab === 'chain'" class="tab-content">
            <!-- 溯源流程图 -->
            <div class="trace-flow">
              <div class="trace-flow__title">溯源流程</div>
              <div class="trace-flow__timeline">
                <el-timeline>
                  <el-timeline-item
                    v-for="(stage, index) in traceStages"
                    :key="stage.key"
                    :type="index < completedStagesCount ? 'success' : 'info'"
                    :icon="index < completedStagesCount ? 'CircleCheck' : 'Clock'"
                    :timestamp="index < completedStagesCount ? '已完成' : '待完成'"
                    placement="top"
                  >
                    <div class="stage-node">
                      <div class="stage-node__label">{{ stage.label }}</div>
                    </div>
                  </el-timeline-item>
                </el-timeline>
              </div>
            </div>

            <!-- 各环节详细信息 -->
            <div class="stage-detail">
              <div class="stage-detail__title">各环节详情</div>

              <div
                v-for="(nodes, type) in chainNodesByType"
                :key="type"
                class="stage-card"
              >
                <div class="stage-card__header">
                  <StatusTag :status="getNodeTypeStatus(Number(type))" :label="getNodeTypeLabel(Number(type))" size="medium" variant="light" />
                  <span class="stage-card__count">共 {{ nodes.length }} 条记录</span>
                </div>
                <div class="stage-card__body">
                  <div
                    v-for="node in nodes"
                    :key="node.id"
                    class="node-item"
                  >
                    <div class="node-item__main">
                      <div class="node-item__name">{{ node.nodeName }}</div>
                      <div v-if="node.location" class="node-item__location">
                        <el-icon><Location /></el-icon>
                        {{ node.location }}
                      </div>
                    </div>
                    <div class="node-item__info">
                      <div v-if="node.operatorName" class="node-item__operator">
                        <el-icon><User /></el-icon>
                        {{ node.operatorName }}
                      </div>
                      <div v-if="node.operateTime" class="node-item__time">
                        <el-icon><Clock /></el-icon>
                        {{ formatDateTime(node.operateTime) }}
                      </div>
                    </div>
                    <div v-if="node.detailJson" class="node-item__detail">
                      <pre>{{ node.detailJson }}</pre>
                    </div>
                  </div>
                </div>
              </div>

              <el-empty
                v-if="Object.keys(chainNodesByType).length === 0"
                description="暂无溯源链节点数据"
              />
            </div>
          </div>

          <!-- 质检信息标签页 -->
          <div v-show="activeTab === 'inspection'" class="tab-content">
            <el-empty description="质检信息功能开发中" />
          </div>

          <!-- 供应链信息标签页 -->
          <div v-show="activeTab === 'supply'" class="tab-content">
            <el-empty description="供应链信息功能开发中" />
          </div>
        </div>
      </template>
    </section>

    <!-- 初始状态提示 -->
    <section v-if="!hasQueried" class="initial-section">
      <div class="initial-card">
        <el-icon class="initial-icon"><Search /></el-icon>
        <h3 class="initial-title">输入溯源码开始查询</h3>
        <p class="initial-desc">您可以输入溯源码或产品名称，也可以使用扫码功能快速查询</p>
        <div class="initial-actions">
          <el-button type="primary" size="large" @click="openScanDialog">
            <el-icon :size="18"><Scan /></el-icon>扫码查询
          </el-button>
        </div>
      </div>
    </section>

    <!-- 扫码查询弹窗 -->
    <el-dialog
      v-model="scanDialogVisible"
      title="扫码查询"
      width="400px"
      :close-on-click-modal="true"
    >
      <div class="scan-dialog">
        <div class="scan-placeholder">
          <el-icon :size="64"><Search /></el-icon>
          <p>请将溯源码对准扫描区域</p>
          <el-text type="info">（摄像头扫码功能需HTTPS环境，当前为演示模式）</el-text>
        </div>
        <el-divider>或</el-divider>
        <div class="manual-input">
          <el-input
            v-model="searchKeyword"
            placeholder="手动输入溯源码"
            clearable
          />
          <el-button type="primary" class="manual-btn" @click="handleScanCode(searchKeyword)">
            查询
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  padding: 0 var(--fts-space-6);
}

// 搜索区域
.search-section {
  padding: var(--fts-space-6) 0;
}

.search-panel {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-6);
}

.search-input-group {
  display: flex;
  gap: var(--fts-space-3);
  max-width: 600px;
  margin: 0 auto;

  :deep(.el-input) {
    flex: 1;
  }
}

// 快速查询区域
.quick-section {
  padding-bottom: var(--fts-space-4);
}

.quick-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    padding: var(--fts-space-3) var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-secondary);
    background: var(--fts-bg-secondary);
  }

  &__icon {
    color: var(--fts-primary);
    font-size: 16px;
  }

  &__title {
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }

  &__content {
    display: flex;
    gap: var(--fts-space-3);
    padding: var(--fts-space-4);
    overflow-x: auto;
  }
}

.history-item {
  flex-shrink: 0;
  width: 200px;
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-card-radius);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-primary);
    background: var(--fts-primary-light-9);
  }

  &__code {
    font-family: var(--fts-font-family-mono);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
    margin-bottom: var(--fts-space-1);
  }

  &__name {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-medium);
    margin-bottom: var(--fts-space-1);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  &__time {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}

.empty-history {
  width: 100%;
  text-align: center;
  padding: var(--fts-space-4) 0;
}

// 结果区域
.result-section {
  padding-bottom: var(--fts-space-6);
}

.no-result {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-8) 0;
}

// 产品信息卡片
.product-info-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-5);
  margin-bottom: var(--fts-space-4);
}

.product-info__header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--fts-space-4);
}

.product-info__main {
  display: flex;
  gap: var(--fts-space-4);
  align-items: center;
}

.product-info__text {
  flex: 1;
}

.product-name {
  margin: 0 0 var(--fts-space-2) 0;
  font-size: var(--fts-font-size-2xl);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
}

.product-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-4);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  .el-icon {
    font-size: 14px;
  }
}

.product-info__status {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  align-items: flex-end;
}

// 统计卡片
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);

  @media (max-width: 1200px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 标签页卡片
.tabs-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

.result-tabs {
  padding: 0 var(--fts-space-4);
  border-bottom: 1px solid var(--fts-border-secondary);

  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }
}

.tab-content {
  padding: var(--fts-space-5);
}

// 溯源流程图
.trace-flow {
  margin-bottom: var(--fts-space-6);
}

.trace-flow__title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-4);
}

.trace-flow__timeline {
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-5);

  :deep(.el-timeline) {
    margin: 0;
  }

  :deep(.el-timeline-item__wrapper) {
    padding-bottom: var(--fts-space-4);
  }

  :deep(.el-timeline-item:last-child .el-timeline-item__wrapper) {
    padding-bottom: 0;
  }
}

.stage-node {
  &__label {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }
}

// 各环节详情
.stage-detail__title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-4);
}

.stage-card {
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-card-radius);
  margin-bottom: var(--fts-space-4);
  overflow: hidden;

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--fts-space-3) var(--fts-space-4);
    border-bottom: 1px solid var(--fts-border-secondary);
    background: var(--fts-bg-card);
  }

  &__count {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__body {
    padding: var(--fts-space-4);
  }
}

.node-item {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-3);

  &:last-child {
    margin-bottom: 0;
  }

  &__main {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: var(--fts-space-2);
  }

  &__name {
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
  }

  &__location {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__info {
    display: flex;
    gap: var(--fts-space-4);
    flex-wrap: wrap;
  }

  &__operator,
  &__time {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);

    .el-icon {
      font-size: 14px;
    }
  }

  &__detail {
    margin-top: var(--fts-space-3);
    padding: var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);

    pre {
      margin: 0;
      font-family: var(--fts-font-family-mono);
      font-size: var(--fts-font-size-xs);
      color: var(--fts-text-secondary);
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}

// 初始状态
.initial-section {
  padding: var(--fts-space-8) 0;
}

.initial-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-8);
  text-align: center;
}

.initial-icon {
  font-size: 64px;
  color: var(--fts-primary);
  margin-bottom: var(--fts-space-4);
}

.initial-title {
  margin: 0 0 var(--fts-space-2) 0;
  font-size: var(--fts-font-size-xl);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.initial-desc {
  margin: 0 0 var(--fts-space-6) 0;
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-secondary);
}

.initial-actions {
  display: flex;
  justify-content: center;
  gap: var(--fts-space-3);
}

// 扫码弹窗
.scan-dialog {
  text-align: center;
}

.scan-placeholder {
  padding: var(--fts-space-6) 0;
  color: var(--fts-text-secondary);

  .el-icon {
    color: var(--fts-primary);
    margin-bottom: var(--fts-space-3);
  }

  p {
    margin: 0 0 var(--fts-space-2) 0;
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.manual-input {
  display: flex;
  gap: var(--fts-space-2);

  .el-input {
    flex: 1;
  }
}

.manual-btn {
  flex-shrink: 0;
}

// 响应式
@media (max-width: 768px) {
  .modern-page {
    padding: 0 var(--fts-space-4);
  }

  .search-input-group {
    flex-direction: column;

    :deep(.el-input) {
      width: 100%;
    }
  }

  .product-info__header {
    flex-direction: column;
  }

  .product-info__status {
    flex-direction: row;
    align-items: center;
  }

  .product-info__main {
    flex-direction: column;
    text-align: center;
  }

  .product-meta {
    justify-content: center;
  }
}
</style>
