<script setup lang="ts">
/**
 * 智能补货建议页面（多模型对比版）
 * 功能：模型选择、多模型对比视图、单模型视图、采购建议列表、提交采购建议、建议记录
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, InfoFilled, Setting } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryWarningApi, inventoryStatsApi, inventoryStatsConverter } from '@/api/warehouse'
import type { PurchaseSuggestionItem, AIModelType, ModelSuggestionResult } from '@/types/warehouse-warning'
import type { InventoryStatsOverview } from '@/types/warehouse-stats'
import type { UrgencyLevel, SuggestionStatus, SuggestionRecord, SuggestionRecordItem } from '@/types/warehouse-smart-restock'
import type { StatColorType } from '@/types/stat'

const router = useRouter()

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== Tab 切换 ===== */
const activeTab = ref('suggestions')

/* ===== 搜索表单 ===== */
const searchForm = ref<{
  urgency: string
  keyword: string
}>({
  urgency: '',
  keyword: '',
})

function resetSearchForm() {
  searchForm.value = { urgency: '', keyword: '' }
}

/* ===== 紧急程度选项 ===== */
const urgencyOptions = [
  { label: '紧急', value: 'urgent' },
  { label: '高', value: 'high' },
  { label: '中', value: 'medium' },
  { label: '低', value: 'low' },
]

/* ===== 模型选择 ===== */
const selectedModel = ref<AIModelType | 'all'>('all')

/** 可用模型列表 */
const availableModels = computed(() => {
  const models: { id: AIModelType | 'all'; name: string; description: string; available: boolean }[] = [
    { id: 'all', name: '全部对比', description: '同时展示所有模型的建议结果，方便对比分析', available: true },
    { id: 'rule-engine', name: '规则引擎', description: '基于安全库存与历史消耗的规则计算，始终可用', available: true },
    { id: 'ai-model-a', name: 'AI模型-A', description: '基于机器学习的需求预测模型，考虑季节性与趋势', available: hasAIModels.value },
    { id: 'ai-model-b', name: 'AI模型-B', description: '基于深度学习的综合预测模型，考虑多维度因素', available: hasAIModels.value },
  ]
  return models
})

/** 是否配置了AI模型 */
const hasAIModels = ref(true)

/** AI模型配置数量 */
const aiModelCount = computed(() => hasAIModels.value ? 2 : 0)

/** 模型配置状态提示 */
const modelConfigNotice = computed(() => {
  if (!hasAIModels.value) {
    return '未配置AI模型，当前仅使用规则引擎。前往系统设置配置AI模型'
  }
  return `已配置 ${aiModelCount.value} 个AI模型`
})

/** 前往系统设置AI配置页 */
function goToAIConfig() {
  router.push('/system/ai-model-config')
}

/* ===== 统计数据 ===== */
const statsOverview = ref<InventoryStatsOverview | null>(null)
const suggestions = ref<PurchaseSuggestionItem[]>([])

async function loadStats() {
  try {
    const res = await inventoryStatsApi.getOverview()
    statsOverview.value = res
  } catch (error: unknown) {
    // 统计加载失败不阻塞页面
    console.error('加载统计概览失败', error)
  }
}

/** 根据安全库存与当前库存差值计算紧急程度 */
function calcUrgency(item: PurchaseSuggestionItem): UrgencyLevel {
  const gap = item.safetyStock - item.currentStock
  if (gap <= 0) return 'low'
  const ratio = item.currentStock / item.safetyStock
  if (ratio <= 0.3) return 'urgent'
  if (ratio <= 0.6) return 'high'
  if (ratio <= 0.9) return 'medium'
  return 'low'
}

/** 紧急程度 → StatusTag status */
function urgencyToStatusTagStatus(level: UrgencyLevel): string {
  return inventoryStatsConverter.toUrgencyStatusTagStatus(level)
}

/** 紧急程度 → 中文标签 */
function urgencyToLabel(level: UrgencyLevel): string {
  return inventoryStatsConverter.toUrgencyLabel(level)
}

const statistics = computed(() => {
  const items = suggestions.value
  const total = items.length
  const urgentCount = items.filter(i => calcUrgency(i) === 'urgent').length
  const totalCost = items.reduce((sum, i) => sum + (parseFloat(i.estimatedCost) || 0), 0)
  const overview = statsOverview.value
  return [
    { icon: 'Warning', label: '需补货品项', value: total, colorType: 'warning' as StatColorType },
    { icon: 'CircleClose', label: '紧急补货', value: urgentCount, colorType: 'error' as StatColorType },
    { icon: 'BankCard', label: '建议采购额', value: `¥${totalCost.toLocaleString()}`, colorType: 'primary' as StatColorType },
    { icon: 'TrendCharts', label: '预警总数', value: overview?.warningCount ?? 0, colorType: 'success' as StatColorType },
  ]
})

/* ===== 单模型视图表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'materialName', label: '物料名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'currentStock', label: '当前库存', minWidth: 100, align: 'center' },
  { prop: 'safetyStock', label: '安全库存', minWidth: 100, align: 'center' },
  { prop: 'suggestedQuantity', label: '建议采购量', minWidth: 120, align: 'center' },
  { prop: 'estimatedCost', label: '预估费用', minWidth: 110, align: 'center' },
  { prop: 'supplierName', label: '推荐供应商', minWidth: 130 },
  { prop: 'urgency', label: '紧急程度', minWidth: 100, slot: 'urgency' },
]

/* ===== 全部对比视图表格列 ===== */
const compareColumns = computed(() => {
  const base: DataTableColumn[] = [
    { prop: 'materialName', label: '物料名称', minWidth: 120, showOverflowTooltip: true },
    { prop: 'specification', label: '规格', minWidth: 90 },
    { prop: 'currentStock', label: '当前库存', minWidth: 90, align: 'center' },
    { prop: 'safetyStock', label: '安全库存', minWidth: 90, align: 'center' },
  ]
  const modelCols: DataTableColumn[] = []
  if (hasAIModels.value) {
    modelCols.push(
      { prop: 'ruleEngineQty', label: '规则引擎建议量', minWidth: 120, align: 'center', slot: 'ruleEngineQty' },
      { prop: 'aiModelAQty', label: 'AI模型-A建议量', minWidth: 120, align: 'center', slot: 'aiModelAQty' },
      { prop: 'aiModelBQty', label: 'AI模型-B建议量', minWidth: 120, align: 'center', slot: 'aiModelBQty' },
    )
  } else {
    modelCols.push(
      { prop: 'ruleEngineQty', label: '规则引擎建议量', minWidth: 120, align: 'center', slot: 'ruleEngineQty' },
    )
  }
  const tail: DataTableColumn[] = [
    { prop: 'finalSuggestedQty', label: '最终建议量', minWidth: 110, align: 'center', slot: 'finalSuggestedQty' },
  ]
  return [...base, ...modelCols, ...tail]
})

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const res = await inventoryWarningApi.getPurchaseSuggestions()
    suggestions.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载采购建议失败')
  } finally {
    loading.value = false
  }
}

/** 根据搜索条件过滤 */
const filteredData = computed(() => {
  let result = [...suggestions.value]
  const { urgency, keyword } = searchForm.value
  if (urgency) result = result.filter(i => calcUrgency(i) === urgency)
  if (keyword) result = result.filter(i => i.materialName.includes(keyword))
  return result
})

/* ===== 对比视图辅助函数 ===== */

/** 获取指定模型结果 */
function getModelResult(item: PurchaseSuggestionItem, modelId: string): ModelSuggestionResult | undefined {
  return item.modelResults?.find(m => m.modelId === modelId)
}

/** 置信度 → 中文标签 */
function confidenceToLabel(confidence: 'high' | 'medium' | 'low'): string {
  return inventoryStatsConverter.toConfidenceLabel(confidence)
}

/** 置信度 → StatusTag status */
function confidenceToStatus(confidence: 'high' | 'medium' | 'low'): string {
  return inventoryStatsConverter.toConfidenceStatusTagStatus(confidence)
}

/** 计算最终建议量（取平均） */
function calcFinalSuggestedQty(item: PurchaseSuggestionItem): number {
  const results = item.modelResults
  if (!results || results.length === 0) return item.suggestedQuantity
  const sum = results.reduce((s, r) => s + r.suggestedQuantity, 0)
  return Math.round(sum / results.length)
}

/** 判断模型是否一致（最大差异不超过20%） */
function isModelsAgree(item: PurchaseSuggestionItem): boolean {
  const results = item.modelResults
  if (!results || results.length <= 1) return true
  const quantities = results.map(r => r.suggestedQuantity)
  const max = Math.max(...quantities)
  const min = Math.min(...quantities)
  if (min === 0) return max === 0
  return (max - min) / min <= 0.2
}

/* ===== 批量选择 ===== */
const selectedSuggestions = ref<PurchaseSuggestionItem[]>([])

function handleSelectionChange(selection: PurchaseSuggestionItem[]) {
  selectedSuggestions.value = selection
}

/* ===== 提交采购建议弹窗 ===== */
const suggestionDialogVisible = ref(false)
const suggestionForm = ref<{
  items: PurchaseSuggestionItem[]
  suggestedSupplierId: string
  suggestedSupplierName: string
  remark: string
}>({
  items: [],
  suggestedSupplierId: '',
  suggestedSupplierName: '',
  remark: '',
})

/** 供应商选项（Mock） */
const supplierOptions = [
  { supplierId: 'SUP001', supplierName: '双汇冷鲜肉直供' },
  { supplierId: 'SUP002', supplierName: '益海嘉里粮油' },
  { supplierId: 'SUP003', supplierName: '蒙牛乳业' },
]

/** 建议总金额 */
const suggestionTotalAmount = computed(() => {
  return suggestionForm.value.items.reduce((sum, i) => sum + (parseFloat(i.estimatedCost) || 0), 0)
})

/** 打开单行提交建议弹窗 */
function handleGenerateSuggestion(row: PurchaseSuggestionItem) {
  suggestionForm.value = {
    items: [row],
    suggestedSupplierId: '',
    suggestedSupplierName: '',
    remark: '',
  }
  suggestionDialogVisible.value = true
}

/** 打开批量提交建议弹窗 */
function handleBatchGenerateSuggestion() {
  if (selectedSuggestions.value.length === 0) {
    ElMessage.warning('请先选择要提交建议的物料')
    return
  }
  suggestionForm.value = {
    items: [...selectedSuggestions.value],
    suggestedSupplierId: '',
    suggestedSupplierName: '',
    remark: '',
  }
  suggestionDialogVisible.value = true
}

/** 建议供应商变更 */
function handleSuggestedSupplierChange(val: string) {
  const sup = supplierOptions.find(s => s.supplierId === val)
  suggestionForm.value.suggestedSupplierName = sup?.supplierName || ''
}

/** 提交采购建议 */
function handleSuggestionSubmit() {
  if (!suggestionForm.value.suggestedSupplierId) {
    ElMessage.warning('请选择建议供应商')
    return
  }
  // 生成建议单号
  const suggestionNo = `WS${Date.now().toString().slice(-8)}`
  // 添加到建议记录
  suggestionHistory.value.unshift({
    suggestionNo,
    materialCount: suggestionForm.value.items.length,
    totalAmount: suggestionTotalAmount.value,
    suggestedSupplier: suggestionForm.value.suggestedSupplierName,
    status: 'pending',
    statusName: '待审核',
    createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
    purchaseRequestNo: '',
    items: suggestionForm.value.items.map(i => ({
      materialName: i.materialName,
      quantity: i.suggestedQuantity,
      unitPrice: parseFloat(i.estimatedCost) || 0,
      amount: (parseFloat(i.estimatedCost) || 0) * i.suggestedQuantity,
    })),
  })
  suggestionDialogVisible.value = false
  selectedSuggestions.value = []
  ElMessage.success({
    message: '采购建议已提交至采购模块，采购团队将审核处理',
    duration: 5000,
    showClose: true,
  })
}

/* ===== 建议记录 Tab ===== */

const suggestionHistory = ref<SuggestionRecord[]>([
  {
    suggestionNo: 'WS20260601001',
    materialCount: 3,
    totalAmount: 2850,
    suggestedSupplier: '双汇冷鲜肉直供',
    status: 'converted',
    statusName: '已转采购',
    createTime: '2026-06-01 09:30:00',
    purchaseRequestNo: 'PR20260601005',
    items: [
      { materialName: '大白菜', quantity: 100, unitPrice: 2.5, amount: 250 },
      { materialName: '土豆', quantity: 80, unitPrice: 3, amount: 240 },
      { materialName: '胡萝卜', quantity: 60, unitPrice: 4, amount: 240 },
    ],
  },
  {
    suggestionNo: 'WS20260605001',
    materialCount: 2,
    totalAmount: 1560,
    suggestedSupplier: '海之鲜水产',
    status: 'accepted',
    statusName: '已采纳',
    createTime: '2026-06-05 14:20:00',
    purchaseRequestNo: '',
    items: [
      { materialName: '鸡胸肉', quantity: 40, unitPrice: 18, amount: 720 },
      { materialName: '五花肉', quantity: 30, unitPrice: 28, amount: 840 },
    ],
  },
  {
    suggestionNo: 'WS20260608001',
    materialCount: 1,
    totalAmount: 800,
    suggestedSupplier: '益海嘉里粮油',
    status: 'modified',
    statusName: '已调整',
    createTime: '2026-06-08 10:15:00',
    purchaseRequestNo: 'PR20260608003',
    remark: '采购部将大豆油数量从20调整为15',
    items: [
      { materialName: '大豆油', quantity: 20, unitPrice: 40, amount: 800 },
    ],
  },
  {
    suggestionNo: 'WS20260610001',
    materialCount: 2,
    totalAmount: 520,
    suggestedSupplier: '川味调料批发',
    status: 'pending',
    statusName: '待审核',
    createTime: '2026-06-10 16:45:00',
    purchaseRequestNo: '',
    items: [
      { materialName: '花椒', quantity: 10, unitPrice: 35, amount: 350 },
      { materialName: '干辣椒', quantity: 15, unitPrice: 11, amount: 165 },
    ],
  },
  {
    suggestionNo: 'WS20260612001',
    materialCount: 1,
    totalAmount: 200,
    suggestedSupplier: '金龙粮油批发',
    status: 'rejected',
    statusName: '已拒绝',
    createTime: '2026-06-12 11:30:00',
    purchaseRequestNo: '',
    remark: '当前库存充足，无需采购',
    items: [
      { materialName: '食盐', quantity: 10, unitPrice: 20, amount: 200 },
    ],
  },
])

const suggestionHistoryColumns: DataTableColumn[] = [
  { prop: 'suggestionNo', label: '建议单号', minWidth: 150 },
  { prop: 'materialCount', label: '物料数', minWidth: 80, align: 'center' },
  { prop: 'totalAmount', label: '建议金额(元)', minWidth: 120, align: 'right' },
  { prop: 'suggestedSupplier', label: '建议供应商', minWidth: 130 },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'suggestionStatus' },
  { prop: 'createTime', label: '提交时间', minWidth: 160 },
]

/** 建议状态 → StatusTag status */
function suggestionStatusToStatusTagStatus(status: SuggestionStatus): string {
  return inventoryStatsConverter.toSuggestionStatusTagStatus(status)
}

/* ===== 建议详情弹窗 ===== */
const detailDialogVisible = ref(false)
const detailData = ref<SuggestionRecord | null>(null)

/** 查看建议详情 */
function handleViewDetail(row: SuggestionRecord) {
  detailData.value = row
  detailDialogVisible.value = true
}

/** 计算建议详情的 el-steps 当前步骤 */
function getSuggestionStep(status: SuggestionStatus): number {
  if (status === 'pending') return 0
  if (status === 'accepted' || status === 'modified' || status === 'rejected' || status === 'converted') return 2
  return 0
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadStats()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="智能补货建议" description="基于库存与消耗自动计算的补货方案" />

    <!-- 模型配置状态提示 -->
    <div class="model-status-bar">
      <div class="model-status__notice">
        <el-icon :size="16" class="model-status__icon">
          <InfoFilled />
        </el-icon>
        <span>{{ modelConfigNotice }}</span>
      </div>
      <el-button link type="primary" size="small" @click="goToAIConfig">
        <el-icon :size="14"><Setting /></el-icon>模型配置
      </el-button>
    </div>

    <!-- 模型选择区 -->
    <div class="model-selection">
      <span class="model-selection__label">选择模型：</span>
      <el-radio-group v-model="selectedModel" size="default">
        <el-radio-button
          v-for="model in availableModels"
          :key="model.id"
          :value="model.id"
          :disabled="!model.available"
        >
          {{ model.name }}
          <el-tooltip :content="model.description" placement="top">
            <el-icon :size="14" class="model-info-icon"><InfoFilled /></el-icon>
          </el-tooltip>
        </el-radio-button>
      </el-radio-group>
    </div>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <el-tabs v-model="activeTab" class="restock-tabs">
      <!-- Tab1: 补货建议 -->
      <el-tab-pane label="补货建议" name="suggestions">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select v-model="searchForm.urgency" placeholder="紧急程度" clearable style="width:130px" size="default">
                <el-option v-for="opt in urgencyOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-input v-model="searchForm.keyword" placeholder="搜索物料名称" clearable style="width:180px" size="default" @keyup.enter="stdSearch">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <div class="toolbar-right">
              <el-button v-if="selectedSuggestions.length > 0" type="warning" size="default" @click="handleBatchGenerateSuggestion">
                批量提交建议（{{ selectedSuggestions.length }}项）
              </el-button>
              <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
              <el-button size="default" @click="stdReset">重置</el-button>
            </div>
          </div>
        </div>

        <!-- 全部对比视图 -->
        <div v-if="selectedModel === 'all'" class="table-section">
          <DataTable :columns="compareColumns" :data="filteredData" :loading="loading" stripe @selection-change="handleSelectionChange">
            <template #ruleEngineQty="{ row }">
              <div class="model-qty-cell">
                <span>{{ getModelResult(row, 'rule-engine')?.suggestedQuantity ?? row.suggestedQuantity }}</span>
                <StatusTag
                  v-if="getModelResult(row, 'rule-engine')"
                  :status="confidenceToStatus(getModelResult(row, 'rule-engine')!.confidence)"
                  :label="confidenceToLabel(getModelResult(row, 'rule-engine')!.confidence)"
                  size="small"
                />
              </div>
            </template>
            <template #aiModelAQty="{ row }">
              <div class="model-qty-cell">
                <span>{{ getModelResult(row, 'ai-model-a')?.suggestedQuantity ?? '-' }}</span>
                <StatusTag
                  v-if="getModelResult(row, 'ai-model-a')"
                  :status="confidenceToStatus(getModelResult(row, 'ai-model-a')!.confidence)"
                  :label="confidenceToLabel(getModelResult(row, 'ai-model-a')!.confidence)"
                  size="small"
                />
              </div>
            </template>
            <template #aiModelBQty="{ row }">
              <div class="model-qty-cell">
                <span>{{ getModelResult(row, 'ai-model-b')?.suggestedQuantity ?? '-' }}</span>
                <StatusTag
                  v-if="getModelResult(row, 'ai-model-b')"
                  :status="confidenceToStatus(getModelResult(row, 'ai-model-b')!.confidence)"
                  :label="confidenceToLabel(getModelResult(row, 'ai-model-b')!.confidence)"
                  size="small"
                />
              </div>
            </template>
            <template #finalSuggestedQty="{ row }">
              <span :class="isModelsAgree(row) ? 'agree-qty' : 'disagree-qty'">
                {{ calcFinalSuggestedQty(row) }}
              </span>
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleGenerateSuggestion(row)">提交建议</el-button>
            </template>
          </DataTable>
        </div>

        <!-- 单模型视图 -->
        <div v-else class="table-section">
          <DataTable :columns="columns" :data="filteredData" :loading="loading" stripe @selection-change="handleSelectionChange">
            <template #urgency="{ row }">
              <StatusTag :status="urgencyToStatusTagStatus(calcUrgency(row))" :label="urgencyToLabel(calcUrgency(row))" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleGenerateSuggestion(row)">提交建议</el-button>
            </template>
          </DataTable>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            :page-size="pagination.size"
            :total="filteredData.length"
            layout="total,prev,pager,next,jumper"
            @size-change="(val: number) => { pagination.size = val }"
            @current-change="(val: number) => { pagination.current = val }"
          />
        </div>
      </el-tab-pane>

      <!-- Tab2: 建议记录 -->
      <el-tab-pane label="建议记录" name="history">
        <div class="table-section">
          <DataTable :columns="suggestionHistoryColumns" :data="suggestionHistory" stripe>
            <template #suggestionStatus="{ row }">
              <StatusTag :status="suggestionStatusToStatusTagStatus(row.status)" :label="row.statusName" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleViewDetail(row)">查看详情</el-button>
            </template>
          </DataTable>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 提交采购建议弹窗 -->
    <el-dialog v-model="suggestionDialogVisible" title="提交采购建议" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <el-table :data="suggestionForm.items" border size="small" style="width:100%">
        <el-table-column prop="materialName" label="物料名称" min-width="120" />
        <el-table-column prop="currentStock" label="当前库存" width="90" align="center" />
        <el-table-column prop="safetyStock" label="安全库存" width="90" align="center" />
        <el-table-column prop="suggestedQuantity" label="建议采购量" width="100" align="center" />
        <el-table-column prop="estimatedCost" label="单价(元)" width="100" align="right" />
        <el-table-column label="小计(元)" width="100" align="right">
          <template #default="{ row }">
            {{ (parseFloat(row.estimatedCost) * row.suggestedQuantity).toFixed(2) }}
          </template>
        </el-table-column>
      </el-table>
      <div class="order-summary">
        <span>共 {{ suggestionForm.items.length }} 项物料</span>
        <span class="order-total">建议金额：¥{{ suggestionTotalAmount.toLocaleString() }}</span>
      </div>

      <el-form label-width="110px" style="margin-top: var(--fts-space-4)">
        <el-form-item label="建议供应商" required>
          <el-select v-model="suggestionForm.suggestedSupplierId" placeholder="请选择建议供应商" :teleported="false" style="width:100%" @change="handleSuggestedSupplierChange">
            <el-option v-for="sup in supplierOptions" :key="sup.supplierId" :label="sup.supplierName" :value="sup.supplierId" />
          </el-select>
          <div class="suggestion-hint">此为建议供应商，采购团队可审核后调整</div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="suggestionForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="suggestionDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSuggestionSubmit">提交建议</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 建议详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="建议详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="建议单号">{{ detailData.suggestionNo }}</el-descriptions-item>
          <el-descriptions-item label="建议供应商">{{ detailData.suggestedSupplier }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="suggestionStatusToStatusTagStatus(detailData.status)" :label="detailData.statusName" size="small" />
          </el-descriptions-item>
        </el-descriptions>

        <!-- 流程步骤 -->
        <div class="detail-steps">
          <el-steps :active="getSuggestionStep(detailData.status)" finish-status="success" align-center>
            <el-step title="提交建议" description="仓库提交补货建议" />
            <el-step title="采购审核" description="采购团队审核建议" />
            <el-step title="处理结果" :description="detailData.statusName" />
          </el-steps>
        </div>

        <!-- 物料明细 -->
        <el-divider content-position="left">物料明细</el-divider>
        <el-table :data="detailData.items" border size="small" style="width:100%">
          <el-table-column prop="materialName" label="物料名称" min-width="120" />
          <el-table-column prop="quantity" label="建议数量" width="100" align="center" />
          <el-table-column label="单价(元)" width="100" align="right">
            <template #default="{ row }">{{ row.unitPrice.toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="100" align="right">
            <template #default="{ row }">{{ row.amount.toFixed(2) }}</template>
          </el-table-column>
        </el-table>

        <!-- 调整备注 -->
        <div v-if="detailData.status === 'modified' && detailData.remark" class="detail-remark">
          <el-alert :title="detailData.remark" type="info" :closable="false" show-icon />
        </div>

        <!-- 关联采购申请 -->
        <div v-if="detailData.status === 'converted' && detailData.purchaseRequestNo" class="detail-link">
          关联采购申请：{{ detailData.purchaseRequestNo }}
        </div>

        <!-- 拒绝原因 -->
        <div v-if="detailData.status === 'rejected' && detailData.remark" class="detail-remark">
          <el-alert :title="`拒绝原因：${detailData.remark}`" type="error" :closable="false" show-icon />
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section { grid-template-columns: repeat(4, 1fr); }

.restock-tabs {
  :deep(.el-tabs__header) {
    padding: 0 var(--fts-space-6);
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.model-status-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-base);
  border: 1px solid var(--fts-border-secondary);
}

.model-status__notice {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.model-status__icon {
  color: var(--fts-primary);
}

.model-selection {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-base);
}

.model-selection__label {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  white-space: nowrap;
}

.model-info-icon {
  margin-left: var(--fts-space-1);
  color: var(--fts-text-tertiary);
  vertical-align: middle;
  cursor: help;
}

.model-qty-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  justify-content: center;
}

.agree-qty {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.disagree-qty {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
}

.order-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-top: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-base);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.order-total {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
  font-size: var(--fts-font-size-base);
}

.suggestion-hint {
  margin-top: var(--fts-space-1);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.detail-steps {
  margin: var(--fts-space-5) 0;
}

.detail-remark {
  margin-top: var(--fts-space-4);
}

.detail-link {
  margin-top: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-base);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  font-weight: var(--fts-font-weight-medium);
}
</style>
