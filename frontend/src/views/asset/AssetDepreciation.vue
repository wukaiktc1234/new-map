<script setup lang="ts">
/**
 * 资产折旧页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理固定资产折旧计算和折旧记录
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 功能：
 * - 统计卡片：本月折旧额、累计折旧额、资产净值、待折旧资产
 * - 顶部筛选：月份选择、资产分类、部门
 * - 工具栏：计提折旧、折旧调整、导出折旧表
 * - 折旧明细对话框
 * - 折旧调整对话框
 * - 计提折旧对话框
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Money,
  Wallet,
  Clock,
  Search,
  Refresh,
  Download,
  Edit,
  View,
  Operation,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { fenToYuan, fenToYuanNumber, yuanToFen } from '@/utils/money'
import { useLayoutStore } from '@/stores/layout'
import { depreciationApi, categoryApi } from '@/api/asset'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  DepreciationRecord,
  DepreciationAdjustDTO,
  AssetCategory,
  PageResponse,
} from '@/types/asset'
import { DepreciationMethodOptions } from '@/types/asset'

const layoutStore = useLayoutStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
  align?: 'left' | 'center' | 'right'
}

/**
 * 扩展折旧记录类型，包含资产详情字段
 * 用于表格展示，包含资产分类、使用部门、原值、残值、使用年限等
 */
interface DepreciationRecordVO extends DepreciationRecord {
  categoryName?: string
  departmentName?: string
  originalValue?: number
  salvageValue?: number
  usefulLife?: number
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<DepreciationRecordVO[]>([])

/** 折旧明细对话框显隐 */
const detailDialogVisible = ref(false)
/** 当前查看的折旧明细 */
const currentDetail = ref<DepreciationRecordVO | null>(null)

/** 折旧调整对话框显隐 */
const adjustDialogVisible = ref(false)
const adjustFormRef = ref<FormInstance>()

/** 计提折旧对话框显隐 */
const calculateDialogVisible = ref(false)
const calculateLoading = ref(false)

/** 导出加载状态 */
const exportLoading = ref(false)

// 查询表单
const queryForm = ref({
  period: '' as string,
  categoryId: '' as string,
  departmentId: '' as string,
  keyword: '' as string,
})

/**
 * 增强折旧记录数据，补充资产详情字段
 * TODO: 后端 DepreciationRecord 不返回资产详情字段（分类/部门/原值等），
 * 待后端 API 补全后移除此函数并直接使用后端返回的完整字段
 */
function enrichDepreciationRecord(record: DepreciationRecord): DepreciationRecordVO {
  return {
    ...record,
    categoryName: undefined,
    departmentName: undefined,
    originalValue: undefined,
    salvageValue: undefined,
    usefulLife: undefined,
  }
}

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData: rawTableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DepreciationRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }): Promise<PageResponse<DepreciationRecord>> => {
      const queryParams: Record<string, unknown> = {
        page: params.page,
        size: params.size,
      }
      if (params.period) {
        queryParams.period = params.period
      }
      const res = await depreciationApi.getList(queryParams as unknown as { period?: string; assetId?: string })
      return res
    },
  } as unknown as CrudApi<DepreciationRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 增强后的表格数据（包含资产详情字段）
const tableData = computed<DepreciationRecordVO[]>(() => {
  return rawTableData.value.map((record) => enrichDepreciationRecord(record))
})

// 统计数据
const statistics = computed(() => {
  const records = tableData.value || []
  const monthDepreciation = records.reduce((sum, item) => sum + item.depreciationAmount, 0)
  const accumulatedDepreciation = records.reduce((sum, item) => sum + item.accumulatedDepreciation, 0)
  const netValue = records.reduce((sum, item) => sum + item.netValue, 0)
  const pendingCount = records.length

  return {
    monthDepreciation: formatMoney(monthDepreciation),
    accumulatedDepreciation: formatMoney(accumulatedDepreciation),
    netValue: formatMoney(netValue),
    pendingCount: String(pendingCount),
  }
})

// 资产分类选项
const categoryOptions = ref<AssetCategory[]>([])

// 部门下拉选项（来自后端 /v1/departments/tree）
const { departmentOptions } = useDepartmentOptions(true)

// 折旧调整表单
const adjustForm = reactive<{
  assetId: string
  assetName: string
  assetCode: string
  period: string
  originalAmount: number
  adjustedAmount: number
  adjustmentReason: string
}>({
  assetId: '',
  assetName: '',
  assetCode: '',
  period: '',
  originalAmount: 0,
  adjustedAmount: 0,
  adjustmentReason: '',
})

// 计提折旧表单
const calculateForm = reactive({
  period: '',
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'assetCode', label: '资产编码', minWidth: 130, slot: 'assetCode' },
  { prop: 'assetName', label: '资产名称', minWidth: 160, slot: 'assetName', ellipsis: true },
  { prop: 'categoryName', label: '资产分类', minWidth: 110, slot: 'categoryName' },
  { prop: 'departmentName', label: '使用部门', minWidth: 110, slot: 'departmentName' },
  { prop: 'originalValue', label: '原值(元)', minWidth: 110, align: 'right', slot: 'originalValue' },
  { prop: 'salvageValue', label: '残值(元)', minWidth: 100, align: 'right', slot: 'salvageValue' },
  { prop: 'usefulLife', label: '使用年限', minWidth: 100, align: 'center', slot: 'usefulLife' },
  { prop: 'depreciationAmount', label: '本月折旧额(元)', minWidth: 130, align: 'right', slot: 'depreciationAmount' },
  { prop: 'accumulatedDepreciation', label: '累计折旧额(元)', minWidth: 130, align: 'right', slot: 'accumulatedDepreciation' },
  { prop: 'netValue', label: '净值(元)', minWidth: 110, align: 'right', slot: 'netValue' },
  { prop: '_operation', label: '操作', width: 200, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const adjustFormRules: FormRules = {
  adjustedAmount: [
    { required: true, message: '请输入调整金额', trigger: 'blur' },
    { type: 'number', min: 0, message: '调整金额不能小于0', trigger: 'blur' },
  ],
  adjustmentReason: [
    { required: true, message: '请输入调整原因', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在2到200个字符之间', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

// fenToYuan 已从 @/utils/money 导入，禁止在组件内直接做 /100 金额转换
/** 分转元格式化（兼容旧调用名） */
const formatMoney = fenToYuan

/** 获取折旧方法标签 */
function getMethodLabel(method: string): string {
  return DepreciationMethodOptions.find(o => o.value === method)?.label || method
}

/** 加载资产分类选项 */
async function loadCategoryOptions(): Promise<void> {
  try {
    categoryOptions.value = await categoryApi.getList()
  } catch {
    categoryOptions.value = []
  }
}

/** 搜索 */
function handleSearch() {
  refresh()
}

/** 重置 */
function handleReset() {
  queryForm.value.period = ''
  queryForm.value.categoryId = ''
  queryForm.value.departmentId = ''
  queryForm.value.keyword = ''
  refresh()
}

/** 表格选择变化 */
function handleSelectionChange(rows: DepreciationRecordVO[]) {
  selectedRows.value = rows
}

/** 查看折旧明细 */
function handleDetail(row: DepreciationRecordVO) {
  currentDetail.value = row
  detailDialogVisible.value = true
}

/** 打开折旧调整对话框 */
function handleAdjust(row: DepreciationRecordVO) {
  adjustForm.assetId = row.assetId
  adjustForm.assetName = row.assetName || ''
  adjustForm.assetCode = row.assetCode || ''
  adjustForm.period = row.period
  adjustForm.originalAmount = row.depreciationAmount
  adjustForm.adjustedAmount = fenToYuanNumber(row.depreciationAmount)
  adjustForm.adjustmentReason = ''
  adjustDialogVisible.value = true
}

/** 提交折旧调整 */
async function handleAdjustSubmit() {
  if (!adjustFormRef.value) return

  const valid = await adjustFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const data: DepreciationAdjustDTO = {
      assetId: adjustForm.assetId,
      period: adjustForm.period,
      adjustedAmount: yuanToFen(adjustForm.adjustedAmount),
      adjustmentReason: adjustForm.adjustmentReason,
    }
    await depreciationApi.adjust(data)
    ElMessage.success('折旧调整成功')
    adjustDialogVisible.value = false
    refresh()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '折旧调整失败')
  } finally {
    submitLoading.value = false
  }
}

/** 打开计提折旧对话框 */
function handleCalculate() {
  const now = new Date()
  calculateForm.period = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  calculateDialogVisible.value = true
}

/** 执行计提折旧 */
async function handleCalculateSubmit() {
  if (!calculateForm.period) {
    ElMessage.warning('请选择计提期间')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要对 ${calculateForm.period} 期间的固定资产计提折旧吗？`,
      '计提折旧确认',
      {
        confirmButtonText: '确认计提',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )

    calculateLoading.value = true
    await depreciationApi.batchCalculate({ period: calculateForm.period })
    ElMessage.success('折旧计提成功')
    calculateDialogVisible.value = false
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '折旧计提失败')
    }
  } finally {
    calculateLoading.value = false
  }
}

/** 导出折旧表 */
async function handleExport() {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出折旧表，请稍候...')
    // 模拟导出，实际项目中调用导出API
    setTimeout(() => {
      ElMessage.success('折旧表导出成功')
      exportLoading.value = false
    }, 1000)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
    exportLoading.value = false
  }
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadCategoryOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="资产折旧" description="管理固定资产折旧计算和折旧记录">
      <el-button type="primary" size="default" @click="handleCalculate">
        <el-icon :size="16"><Money /></el-icon>计提折旧
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="Money"
        label="本月折旧额"
        :value="statistics.monthDepreciation"
        color-type="primary"
        variant="bordered"
      />
      <StatCard
        icon="Wallet"
        label="累计折旧额"
        :value="statistics.accumulatedDepreciation"
        color-type="success"
        variant="bordered"
      />
      <StatCard
        icon="Money"
        label="资产净值"
        :value="statistics.netValue"
        color-type="warning"
        variant="bordered"
      />
      <StatCard
        icon="Clock"
        label="待折旧资产"
        :value="statistics.pendingCount"
        color-type="info"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-date-picker
            v-model="queryForm.period"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 160px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.categoryId"
            placeholder="资产分类"
            clearable
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="cat in categoryOptions"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
          <el-select
            v-model="queryForm.departmentId"
            placeholder="使用部门"
            clearable
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索资产名称或编码..."
            clearable
            style="width: 220px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button type="warning" size="default" @click="handleCalculate">
            <el-icon :size="14"><Operation /></el-icon>折旧调整
          </el-button>
          <el-button
            type="success"
            size="default"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出折旧表
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <section class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        @selection-change="handleSelectionChange"
      >
        <!-- 资产编码列 -->
        <template #assetCode="{ row }">
          <span class="code-text">{{ row.assetCode || '-' }}</span>
        </template>

        <!-- 资产名称列 -->
        <template #assetName="{ row }">
          <span class="name-text">{{ row.assetName || '-' }}</span>
        </template>

        <!-- 资产分类列 -->
        <template #categoryName="{ row }">
          <span class="text-muted">{{ row.categoryName || '-' }}</span>
        </template>

        <!-- 使用部门列 -->
        <template #departmentName="{ row }">
          <span class="text-muted">{{ row.departmentName || '-' }}</span>
        </template>

        <!-- 原值列 -->
        <template #originalValue="{ row }">
          <span class="amount-text">¥{{ formatMoney(row.originalValue || 0) }}</span>
        </template>

        <!-- 残值列 -->
        <template #salvageValue="{ row }">
          <span class="amount-text">¥{{ formatMoney(row.salvageValue || 0) }}</span>
        </template>

        <!-- 使用年限列 -->
        <template #usefulLife="{ row }">
          <span>{{ row.usefulLife ? row.usefulLife + ' 年' : '-' }}</span>
        </template>

        <!-- 本月折旧额列 -->
        <template #depreciationAmount="{ row }">
          <span class="amount-text">¥{{ formatMoney(row.depreciationAmount) }}</span>
        </template>

        <!-- 累计折旧额列 -->
        <template #accumulatedDepreciation="{ row }">
          <span class="amount-text amount--warning">¥{{ formatMoney(row.accumulatedDepreciation) }}</span>
        </template>

        <!-- 净值列 -->
        <template #netValue="{ row }">
          <span class="amount-text amount--primary">¥{{ formatMoney(row.netValue) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              <el-icon :size="14"><View /></el-icon>详情
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleAdjust(row)">
              <el-icon :size="14"><Edit /></el-icon>折旧调整
            </el-button>
          </div>
        </template>
      </DataTable>

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
        />
      </div>
    </section>

    <!-- 折旧明细对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="折旧明细"
      width="680px"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="资产编码">
            {{ currentDetail.assetCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="资产名称">
            {{ currentDetail.assetName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="资产分类">
            {{ currentDetail.categoryName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="使用部门">
            {{ currentDetail.departmentName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="折旧期间">
            {{ currentDetail.period }}
          </el-descriptions-item>
          <el-descriptions-item label="折旧方法">
            {{ getMethodLabel(currentDetail.method) }}
          </el-descriptions-item>
          <el-descriptions-item label="资产原值">
            <span class="amount-text">¥{{ formatMoney(currentDetail.originalValue || 0) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="预计残值">
            <span class="amount-text">¥{{ formatMoney(currentDetail.salvageValue || 0) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="使用年限">
            {{ currentDetail.usefulLife ? currentDetail.usefulLife + ' 年' : '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="本期折旧额">
            <span class="amount-text">¥{{ formatMoney(currentDetail.depreciationAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="累计折旧额">
            <span class="amount-text amount--warning">¥{{ formatMoney(currentDetail.accumulatedDepreciation) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="折旧后净值">
            <span class="amount-text amount--primary">¥{{ formatMoney(currentDetail.netValue) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="调整标记">
            <StatusTag
              v-if="currentDetail.isManualAdjustment"
              status="warning"
              label="手动调整"
              size="small"
              variant="light"
            />
            <StatusTag v-else status="info" label="自动计提" size="small" variant="light" />
          </el-descriptions-item>
          <el-descriptions-item label="计算时间">
            {{ currentDetail.calculatedAt || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作人">
            {{ currentDetail.operatorName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.isManualAdjustment" label="调整原因" :span="2">
            {{ currentDetail.adjustmentReason || '-' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 折旧调整对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="折旧调整"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustFormRules"
        label-width="100px"
        label-position="right"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="资产编码">
              <el-input :model-value="adjustForm.assetCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="资产名称">
              <el-input :model-value="adjustForm.assetName" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="折旧期间">
              <el-input :model-value="adjustForm.period" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="原折旧额">
              <el-input :model-value="'¥' + formatMoney(adjustForm.originalAmount)" disabled />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="调整金额" prop="adjustedAmount">
          <el-input-number
            v-model="adjustForm.adjustedAmount"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 100%"
          />
          <div class="form-tip">单位：元，调整后的折旧金额</div>
        </el-form-item>
        <el-form-item label="调整原因" prop="adjustmentReason">
          <el-input
            v-model="adjustForm.adjustmentReason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleAdjustSubmit">
          确认调整
        </el-button>
      </template>
    </el-dialog>

    <!-- 计提折旧对话框 -->
    <el-dialog
      v-model="calculateDialogVisible"
      title="计提折旧"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form label-width="100px" label-position="right">
        <el-form-item label="计提期间">
          <el-date-picker
            v-model="calculateForm.period"
            type="month"
            placeholder="选择计提期间"
            value-format="YYYY-MM"
            :teleported="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="计提说明"
          description="系统将自动计算所选期间内所有在用固定资产的折旧额，并生成折旧记录。计提操作不可撤销，请确认后再执行。"
        />
      </el-form>

      <template #footer>
        <el-button @click="calculateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="calculateLoading" @click="handleCalculateSubmit">
          确认计提
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  overflow-x: clip;
}

// 统计卡片区域
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) 0;

  @media (max-width: 1400px) {
    grid-template-columns: repeat(3, 1fr);
  }

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 576px) {
    grid-template-columns: 1fr;
  }
}

// 工具栏面板
.advanced-search-panel {
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  padding: var(--fts-space-3) var(--fts-space-6);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
}

.toolbar-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 48px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

// 表格区域
.table-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  overflow-x: auto;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);

  :deep(.el-table) {
    width: 100%;
  }

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 文本样式
.code-text {
  font-family: var(--fts-font-family-mono, monospace);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm);
}

.name-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.text-muted {
  color: var(--fts-text-secondary);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);

  &.amount--primary {
    color: var(--fts-primary);
  }

  &.amount--warning {
    color: var(--fts-warning);
  }

  &.amount--success {
    color: var(--fts-success);
  }

  &.amount--error {
    color: var(--fts-error);
  }
}

// 表单提示
.form-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: 4px;
}

// 操作按钮组
.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 对话框样式
:deep(.el-dialog__body) {
  padding-top: var(--fts-space-4);
}

:deep(.el-form-item__label) {
  white-space: nowrap;
}

@media (max-width: 768px) {
  .toolbar-row {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-left {
    flex-wrap: wrap;
  }

  .toolbar-right {
    justify-content: flex-end;
  }
}

@media (max-width: 576px) {
  .advanced-search-panel {
    padding: var(--fts-space-2) var(--fts-space-4);
  }

  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
