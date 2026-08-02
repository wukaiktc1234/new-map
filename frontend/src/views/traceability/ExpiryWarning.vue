<script setup lang="ts">
/**
 * 临期预警看板页面
 *
 * 功能：
 * - 调用 expiryAlertApi.getDashboard() 获取看板聚合数据（红/黄/绿分级统计）
 * - 调用 expiryAlertApi.getExpiringSoon() 获取临期列表
 * - 调用 expiryAlertApi.getExpired() 获取已过期列表
 * - 一键报损 / 一键退货（ElMessageBox.prompt 输入备注 + 二次确认）
 * - 使用 AlertLevelMap / ExpiryHandlingStatusMap 进行状态映射
 *
 * 对接策略：直接调用真实后端 API，无 Mock 降级
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import { expiryAlertApi } from '@/api/traceability'
import type {
  ExpiryAlert,
  ExpiryAlertQuery,
  ExpiryDashboard,
  AlertLevel,
  ExpiryHandlingStatus,
} from '@/types/traceability'
import { AlertLevelMap, ExpiryHandlingStatusMap } from '@/types/traceability'
import type { StatColorType } from '@/types/stat'

/** 预警类型 Tab */
type WarningTab = 'expiring' | 'expired'

/* ===== 状态 ===== */
const loading = ref(false)
const activeTab = ref<WarningTab>('expiring')

/** 临期预警列表（黄色/绿色预警） */
const expiringList = ref<ExpiryAlert[]>([])

/** 已过期列表（红色预警） */
const expiredList = ref<ExpiryAlert[]>([])

/** 看板聚合数据 */
const dashboard = ref<ExpiryDashboard | null>(null)

/** 搜索表单 */
const searchForm = ref({
  materialName: '',
  batchNo: '',
  supplierName: '',
})

/* ===== 加载数据 ===== */

/** 构建查询参数 */
function buildQuery(): ExpiryAlertQuery {
  return {
    page: 1,
    size: 100,
    materialName: searchForm.value.materialName || undefined,
    batchNo: searchForm.value.batchNo || undefined,
  }
}

/** 加载看板数据 */
async function loadDashboard(): Promise<void> {
  try {
    dashboard.value = await expiryAlertApi.getDashboard()
  } catch (error) {
    // 看板加载失败不阻塞主流程，统计卡片显示 0
    dashboard.value = null
  }
}

/** 加载临期列表 */
async function loadExpiringList(): Promise<void> {
  loading.value = true
  try {
    const res = await expiryAlertApi.getExpiringSoon(buildQuery())
    expiringList.value = res?.records || []
  } catch (error) {
    ElMessage.error('加载临期预警数据失败')
    expiringList.value = []
  } finally {
    loading.value = false
  }
}

/** 加载已过期列表 */
async function loadExpiredList(): Promise<void> {
  loading.value = true
  try {
    const res = await expiryAlertApi.getExpired(buildQuery())
    expiredList.value = res?.records || []
  } catch (error) {
    ElMessage.error('加载已过期数据失败')
    expiredList.value = []
  } finally {
    loading.value = false
  }
}

/** 加载当前 Tab 数据 */
async function loadData(): Promise<void> {
  if (activeTab.value === 'expiring') {
    await loadExpiringList()
  } else {
    await loadExpiredList()
  }
}

/** Tab 切换 */
async function handleTabChange(tab: WarningTab): Promise<void> {
  activeTab.value = tab
  resetSearchForm()
  await loadData()
}

/* ===== 筛选后的列表（基于搜索表单的供应商名称） =====
 * 注：materialName / batchNo 已通过 API 参数过滤，supplierName 在前端过滤
 * （后端 ExpiryAlertQuery 通过 supplierId 过滤，前端按名称兜底过滤）
 */
const filteredList = computed<ExpiryAlert[]>(() => {
  const source = activeTab.value === 'expiring' ? expiringList.value : expiredList.value
  let result = [...source]
  if (searchForm.value.supplierName) {
    result = result.filter((r) => r.supplierName?.includes(searchForm.value.supplierName))
  }
  // 按剩余天数升序排序（临期：剩余天数少的在前；过期：剩余天数负值绝对值大的在前）
  result.sort((a, b) => (a.remainingDays ?? 999) - (b.remainingDays ?? 999))
  return result
})

/* ===== 统计卡片（基于看板聚合数据） ===== */
const statsCards = computed(() => {
  const d = dashboard.value
  return [
    {
      key: 'red',
      icon: 'CircleCloseFilled',
      label: '红色预警（已过期）',
      value: d?.redCount ?? 0,
      colorType: 'error' as StatColorType,
    },
    {
      key: 'yellow',
      icon: 'WarningFilled',
      label: '黄色预警（7天内）',
      value: d?.yellowCount ?? 0,
      colorType: 'warning' as StatColorType,
    },
    {
      key: 'green',
      icon: 'CircleCheck',
      label: '绿色预警（7天外）',
      value: d?.greenCount ?? 0,
      colorType: 'success' as StatColorType,
    },
  ]
})

/* ===== 表格列定义 ===== */
const columns: DataTableColumn[] = [
  { prop: 'traceCode', label: '追溯码', minWidth: 180, showOverflowTooltip: true },
  { prop: 'materialName', label: '物料名称', minWidth: 150, showOverflowTooltip: true },
  { prop: 'batchNo', label: '批次号', minWidth: 140, showOverflowTooltip: true },
  { prop: 'supplierName', label: '供应商', minWidth: 160, showOverflowTooltip: true },
  { prop: 'expiryDate', label: '到期日期', minWidth: 110 },
  { prop: 'remainingDays', label: '剩余天数', minWidth: 110, slot: 'remainingDays', align: 'center' },
  { prop: 'alertLevel', label: '预警等级', minWidth: 110, slot: 'alertLevel', align: 'center' },
  { prop: 'handlingStatus', label: '处理状态', minWidth: 110, slot: 'handlingStatus', align: 'center' },
]

/* ===== 工具方法 ===== */

/** 获取剩余天数 StatusTag status */
function getRemainingDaysStatus(days?: number): string {
  if (days === undefined || days === null) return 'info'
  if (days < 0) return 'error'
  if (days === 0) return 'error'
  if (days <= 3) return 'error'
  if (days <= 7) return 'warning'
  return 'success'
}

/** 获取剩余天数标签文本 */
function getRemainingDaysText(days?: number): string {
  if (days === undefined || days === null) return '-'
  if (days < 0) return `已过期${Math.abs(days)}天`
  if (days === 0) return '今日到期'
  return `剩余${days}天`
}

/** 获取预警等级 StatusTag status */
function getAlertLevelStatus(level?: AlertLevel): string {
  if (!level) return 'info'
  return AlertLevelMap[level]?.status || 'info'
}

/** 获取预警等级标签文本 */
function getAlertLevelLabel(level?: AlertLevel): string {
  if (!level) return '-'
  return AlertLevelMap[level]?.label || level
}

/** 获取处理状态 StatusTag status */
function getHandlingStatusTagStatus(status?: ExpiryHandlingStatus): string {
  if (!status) return 'info'
  return ExpiryHandlingStatusMap[status]?.status || 'info'
}

/** 获取处理状态标签文本 */
function getHandlingStatusLabel(status?: ExpiryHandlingStatus): string {
  if (!status) return '-'
  return ExpiryHandlingStatusMap[status]?.label || status
}

/* ===== 操作功能 ===== */

/** 重置搜索表单 */
function resetSearchForm(): void {
  searchForm.value = {
    materialName: '',
    batchNo: '',
    supplierName: '',
  }
}

/** 搜索 */
async function handleSearch(): Promise<void> {
  await loadData()
}

/** 重置 */
async function handleReset(): Promise<void> {
  resetSearchForm()
  await loadData()
}

/** 一键报损（ElMessageBox.prompt 输入备注 + 二次确认） */
async function handleScrap(row: ExpiryAlert): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入「${row.materialName || row.traceCode}」的报损备注`,
      '一键报损确认',
      {
        confirmButtonText: '确认报损',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入报损原因/备注（可选）',
        inputType: 'textarea',
      },
    )
    await expiryAlertApi.scrap(row.traceCodeId, value || undefined)
    ElMessage.success('报损处理成功')
    // 刷新当前列表与看板
    await Promise.all([loadData(), loadDashboard()])
  } catch (error) {
    if (error === 'cancel') return
    if (error instanceof Error) {
      ElMessage.error(error.message || '报损处理失败')
    }
  }
}

/** 一键退货（ElMessageBox.prompt 输入备注 + 二次确认） */
async function handleReturnGoods(row: ExpiryAlert): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入「${row.materialName || row.traceCode}」的退货备注`,
      '一键退货确认',
      {
        confirmButtonText: '确认退货',
        cancelButtonText: '取消',
        inputPlaceholder: '请输入退货原因/备注（可选）',
        inputType: 'textarea',
      },
    )
    await expiryAlertApi.returnGoods(row.traceCodeId, value || undefined)
    ElMessage.success('退货处理成功')
    await Promise.all([loadData(), loadDashboard()])
  } catch (error) {
    if (error === 'cancel') return
    if (error instanceof Error) {
      ElMessage.error(error.message || '退货处理失败')
    }
  }
}

/* ===== 初始化 ===== */
onMounted(async () => {
  await Promise.all([loadDashboard(), loadData()])
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="临期预警" description="临期与过期原料预警看板，支持一键报损与退货处理" />

    <!-- 统计卡片 -->
    <div class="stats-section">
      <StatCard
        v-for="stat in statsCards"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </div>

    <!-- Tab 切换 + 搜索栏 -->
    <div class="toolbar-section">
      <div class="toolbar-top">
        <el-radio-group v-model="activeTab" @change="handleTabChange">
          <el-radio-button value="expiring">临期预警（{{ expiringList.length }}）</el-radio-button>
          <el-radio-button value="expired">已过期（{{ expiredList.length }}）</el-radio-button>
        </el-radio-group>
      </div>
      <div class="toolbar-bottom">
        <div class="toolbar-left">
          <el-input
            v-model="searchForm.materialName"
            placeholder="物料名称"
            clearable
            style="width: 180px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-input
            v-model="searchForm.batchNo"
            placeholder="批次号"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="searchForm.supplierName"
            placeholder="供应商"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-section">
      <DataTable :columns="columns" :data="filteredList" :loading="loading" stripe>
        <!-- 剩余天数 -->
        <template #remainingDays="{ row }">
          <StatusTag
            :status="getRemainingDaysStatus(row.remainingDays)"
            :label="getRemainingDaysText(row.remainingDays)"
            size="small"
          />
        </template>

        <!-- 预警等级 -->
        <template #alertLevel="{ row }">
          <StatusTag
            :status="getAlertLevelStatus(row.alertLevel)"
            :label="getAlertLevelLabel(row.alertLevel)"
            size="small"
          />
        </template>

        <!-- 处理状态 -->
        <template #handlingStatus="{ row }">
          <StatusTag
            :status="getHandlingStatusTagStatus(row.handlingStatus)"
            :label="getHandlingStatusLabel(row.handlingStatus)"
            size="small"
          />
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <el-button
            v-if="row.handlingStatus === 'PENDING'"
            link
            type="danger"
            size="small"
            @click="handleScrap(row)"
          >
            一键报损
          </el-button>
          <el-button
            v-if="row.handlingStatus === 'PENDING'"
            link
            type="primary"
            size="small"
            @click="handleReturnGoods(row)"
          >
            一键退货
          </el-button>
        </template>
      </DataTable>

      <!-- 空状态 -->
      <EmptyState
        v-if="!loading && filteredList.length === 0"
        type="no-data"
        :title="activeTab === 'expiring' ? '暂无临期预警' : '暂无已过期记录'"
        :description="activeTab === 'expiring' ? '当前没有即将到期的原料' : '当前没有已过期的原料'"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.toolbar-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
  overflow: hidden;

  .toolbar-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--fts-space-3) var(--fts-space-6);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  .toolbar-bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-4);
    padding: var(--fts-space-3) var(--fts-space-6);
  }

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }
}

.table-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  padding: var(--fts-space-4);
}
</style>
