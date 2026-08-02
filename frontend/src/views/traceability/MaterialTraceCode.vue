<script setup lang="ts">
/**
 * 物料溯源码管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理原材料/物料溯源码的生成和追溯
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Printer,
  Download,
  Search,
  Refresh,
  Box,
  Delete,
  View,
  Connection,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { materialTraceCodeApi, traceabilityDataConverter } from '@/api/traceability'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  MaterialTraceCodeDisplay,
  MaterialTraceCodeQuery,
  MaterialTraceCodeStatus,
  MaterialTraceCodeGenerateDTO,
} from '@/types/traceability'
import { MaterialTraceCodeStatusMap } from '@/types/traceability'

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
  align?: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<MaterialTraceCodeDisplay[]>([])
const generateDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const currentDetail = ref<MaterialTraceCodeDisplay | null>(null)
const statistics = reactive({
  total: 0,
  used: 0,
  unused: 0,
  todayGenerated: 0,
})

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  materialName: '',
  traceCode: '',
  supplier: '',
  status: '' as '' | MaterialTraceCodeStatus,
  startDate: '',
  endDate: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<MaterialTraceCodeDisplay, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: MaterialTraceCodeQuery = {
        page: params.page,
        size: params.size,
        keyword: params.traceCode || params.materialName || undefined,
        status: params.status || undefined,
      }
      return materialTraceCodeApi.getList(queryParams)
    },
  } as unknown as CrudApi<MaterialTraceCodeDisplay, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 批量生成表单数据
const generateFormData = reactive<Partial<MaterialTraceCodeGenerateDTO>>({
  productId: undefined,
  productName: '',
  supplierId: undefined,
  supplierName: '',
  batchNumber: '',
  productionDate: '',
  generateCount: 10,
  remark: '',
})

// 物料选项
const materialOptions = ref<Array<{ id: number; name: string; code: string }>>([])
// 供应商选项
const supplierOptions = ref<Array<{ id: number; name: string }>>([])

// 统计数据
async function loadStatistics(): Promise<void> {
  try {
    const stats = await materialTraceCodeApi.getStatistics()
    const inStock = stats.in_stock ?? 0
    const picked = stats.picked ?? 0
    const used = stats.used ?? 0
    const total = inStock + picked + used + (stats.pending ?? 0)
    statistics.total = total
    statistics.used = used + picked
    statistics.unused = inStock + (stats.pending ?? 0)
    statistics.todayGenerated = stats.today_generated ?? 0
  } catch {
    // 静默失败
  }
}

// 加载物料选项
async function loadMaterialOptions(): Promise<void> {
  try {
    // 从采购档案API获取物料列表（此处留空，待后续对接实际API）
    materialOptions.value = []
  } catch {
    materialOptions.value = []
  }
}

// 加载供应商选项
async function loadSupplierOptions(): Promise<void> {
  try {
    // 从供应商API获取列表（此处留空，待后续对接实际API）
    supplierOptions.value = []
  } catch {
    supplierOptions.value = []
  }
}

// 页面挂载时加载统计数据和选项
onMounted(() => {
  loadStatistics()
  loadMaterialOptions()
  loadSupplierOptions()
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'traceCode', label: '溯源码', minWidth: 180, slot: 'traceCode', ellipsis: true },
  { prop: 'productName', label: '物料名称', minWidth: 120, slot: 'productName' },
  { prop: 'supplierName', label: '供应商', minWidth: 120, slot: 'supplierName' },
  { prop: 'batchNumber', label: '批次号', minWidth: 130, slot: 'batchNumber' },
  { prop: 'inboundTime', label: '入库日期', minWidth: 120, slot: 'inboundTime' },
  { prop: 'createTime', label: '生成时间', minWidth: 160, slot: 'createTime' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const generateFormRules: FormRules = {
  productName: [
    { required: true, message: '请选择物料', trigger: 'change' },
  ],
  supplierName: [
    { required: true, message: '请选择供应商', trigger: 'change' },
  ],
  batchNumber: [
    { required: true, message: '请输入批次号', trigger: 'blur' },
  ],
  productionDate: [
    { required: true, message: '请选择生产日期/入库日期', trigger: 'change' },
  ],
  generateCount: [
    { required: true, message: '请输入生成数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '生成数量在1到10000之间', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.materialName = ''
  queryForm.value.traceCode = ''
  queryForm.value.supplier = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
}

function handleSelectionChange(rows: MaterialTraceCodeDisplay[]) {
  selectedRows.value = rows
}

/** 打开批量生成对话框 */
function handleBatchGenerate() {
  Object.assign(generateFormData, {
    productId: undefined,
    productName: '',
    supplierId: undefined,
    supplierName: '',
    batchNumber: '',
    productionDate: '',
    generateCount: 10,
    remark: '',
  })
  generateDialogVisible.value = true
}

/** 选择物料 */
function onMaterialSelect(value: number) {
  const mat = materialOptions.value.find(m => m.id === value)
  if (mat) {
    generateFormData.productName = mat.name
  }
}

/** 选择供应商 */
function onSupplierSelect(value: number) {
  const sup = supplierOptions.value.find(s => s.id === value)
  if (sup) {
    generateFormData.supplierName = sup.name
  }
}

/** 提交批量生成 */
async function handleGenerateSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await materialTraceCodeApi.generate(generateFormData as MaterialTraceCodeGenerateDTO)
    ElMessage.success('生成成功')
    generateDialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error) {
    ElMessage.error('生成失败')
  } finally {
    submitLoading.value = false
  }
}

/** 打印 */
async function handlePrint(row: MaterialTraceCodeDisplay) {
  if (!row.traceCodeId) return
  try {
    await materialTraceCodeApi.batchPrint([row.traceCodeId])
    ElMessage.success('打印任务已发送')
  } catch {
    ElMessage.error('打印失败')
  }
}

/** 批量打印 */
async function handleBatchPrint() {
  if (selectedRows.value.length === 0) return
  const ids = selectedRows.value
    .filter(row => row.traceCodeId)
    .map(row => row.traceCodeId as string)
  if (ids.length === 0) {
    ElMessage.warning('请选择有效的溯源码')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要打印选中的 ${ids.length} 个溯源码吗？`,
      '批量打印确认',
      { type: 'info' },
    )
    await materialTraceCodeApi.batchPrint(ids)
    ElMessage.success('批量打印任务已发送')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('批量打印失败')
    }
  }
}

/** 导出 */
async function handleExport() {
  try {
    ElMessage.info('导出功能开发中...')
  } catch {
    ElMessage.error('导出失败')
  }
}

/** 查看详情 */
function handleViewDetail(row: MaterialTraceCodeDisplay) {
  currentDetail.value = row
  detailDialogVisible.value = true
}

/** 追溯 */
function handleTrace(row: MaterialTraceCodeDisplay) {
  ElMessage.info(`追溯功能开发中... 溯源码：${row.traceCode}`)
}

/** 删除 */
async function handleDelete(row: MaterialTraceCodeDisplay) {
  try {
    await ElMessageBox.confirm(
      `确定要删除溯源码「${row.traceCode}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    // 注意：materialTraceCodeApi 没有 delete 方法，这里仅作提示
    ElMessage.warning('删除功能待后端API支持')
    // refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

/**
 * 获取状态对应的 StatusTag status
 */
function getStatusTagStatus(status?: MaterialTraceCodeStatus): string {
  if (!status) return 'default'
  return traceabilityDataConverter.materialStatusToTagStatus[status] || 'default'
}

/**
 * 获取状态标签文本
 */
function getStatusLabel(status?: MaterialTraceCodeStatus): string {
  if (!status) return '-'
  return MaterialTraceCodeStatusMap[status]?.label || status
}

/**
 * 格式化日期时间
 */
function formatDateTime(dateStr?: string): string {
  if (!dateStr) return '-'
  const normalized = dateStr.replace('T', ' ')
  return normalized.split('.')[0].slice(0, 19)
}

/**
 * 格式化日期
 */
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  return dateStr.split('T')[0].split(' ')[0]
}
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="原料追溯" description="管理原材料/物料溯源码的生成和追溯">
      <el-button type="primary" size="default" @click="handleBatchGenerate">
        <el-icon :size="16"><Plus /></el-icon>批量生成
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="Box"
        label="溯源码总数"
        :value="String(statistics.total)"
        color-type="primary"
        variant="bordered"
      />
      <StatCard
        icon="CircleCheck"
        label="已使用"
        :value="String(statistics.used)"
        color-type="success"
        variant="bordered"
      />
      <StatCard
        icon="CircleClose"
        label="未使用"
        :value="String(statistics.unused)"
        color-type="warning"
        variant="bordered"
      />
      <StatCard
        icon="Calendar"
        label="今日生成"
        :value="String(statistics.todayGenerated)"
        color-type="info"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板（搜索筛选 + 操作按钮） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.materialName"
            placeholder="物料名称"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.traceCode"
            placeholder="溯源码"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="queryForm.supplier"
            placeholder="供应商"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="(item, key) in MaterialTraceCodeStatusMap"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="primary"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchPrint"
          >
            <el-icon :size="14"><Printer /></el-icon>打印
          </el-button>
          <el-button size="default" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
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
        <!-- 溯源码列 -->
        <template #traceCode="{ row }">
          <span class="trace-code-text">{{ row.traceCode || '-' }}</span>
        </template>

        <!-- 物料名称列 -->
        <template #productName="{ row }">
          <span class="material-name">{{ row.productName || '-' }}</span>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName || '-' }}</span>
        </template>

        <!-- 批次号列 -->
        <template #batchNumber="{ row }">
          <span class="batch-text">{{ row.batchNumber || '-' }}</span>
        </template>

        <!-- 入库日期列 -->
        <template #inboundTime="{ row }">
          <span class="date-text">{{ formatDate(row.inboundTime) }}</span>
        </template>

        <!-- 生成时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatDateTime(row.createTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusTagStatus(row.status as MaterialTraceCodeStatus)"
            :label="getStatusLabel(row.status as MaterialTraceCodeStatus)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="info" size="default" @click.stop="handlePrint(row)">
              打印
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleTrace(row)">
              追溯
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
              删除
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

    <!-- 批量生成对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      title="批量生成溯源码"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="generateFormData"
        :rules="generateFormRules"
        label-width="110px"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="物料" prop="productName">
              <el-select
                v-model="generateFormData.productId"
                placeholder="请选择物料"
                filterable
                style="width: 100%"
                :teleported="false"
                @change="onMaterialSelect"
              >
                <el-option
                  v-for="mat in materialOptions"
                  :key="mat.id"
                  :label="`${mat.name}（${mat.code}）`"
                  :value="mat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierName">
              <el-select
                v-model="generateFormData.supplierId"
                placeholder="请选择供应商"
                filterable
                style="width: 100%"
                :teleported="false"
                @change="onSupplierSelect"
              >
                <el-option
                  v-for="sup in supplierOptions"
                  :key="sup.id"
                  :label="sup.name"
                  :value="sup.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="批次号" prop="batchNumber">
              <el-input
                v-model="generateFormData.batchNumber"
                placeholder="请输入批次号"
                maxlength="50"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="入库日期" prop="productionDate">
              <el-date-picker
                v-model="generateFormData.productionDate"
                type="date"
                placeholder="请选择入库日期"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生成数量" prop="generateCount">
              <el-input-number
                v-model="generateFormData.generateCount"
                :min="1"
                :max="10000"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="码规则">
              <el-select
                v-model="generateFormData.remark"
                placeholder="默认规则"
                style="width: 100%"
                :teleported="false"
              >
                <el-option label="默认规则（日期+批次+序号）" value="default" />
                <el-option label="物料编码+批次号" value="material_batch" />
                <el-option label="UUID格式" value="uuid" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注">
          <el-input
            v-model="generateFormData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="submitLoading"
          @click="handleGenerateSubmit"
        >
          确定生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="溯源码详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-container">
        <!-- 溯源码信息 -->
        <div class="detail-section">
          <div class="section-title">溯源码信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="溯源码">
              <span class="detail-trace-code">{{ currentDetail.traceCode || '-' }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag
                :status="getStatusTagStatus(currentDetail.status as MaterialTraceCodeStatus)"
                :label="getStatusLabel(currentDetail.status as MaterialTraceCodeStatus)"
                size="small"
                variant="light"
              />
            </el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ formatDateTime(currentDetail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="入库时间">{{ formatDateTime(currentDetail.inboundTime) }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 物料信息 -->
        <div class="detail-section">
          <div class="section-title">物料信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="物料名称">{{ currentDetail.productName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ currentDetail.productCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ currentDetail.productCategory || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ currentDetail.batchNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数量">
              {{ currentDetail.quantity ?? '-' }} {{ currentDetail.unit || '' }}
            </el-descriptions-item>
            <el-descriptions-item label="单价">{{ currentDetail.unitPriceYuan || '-' }} 元</el-descriptions-item>
            <el-descriptions-item label="总价">{{ currentDetail.totalPriceYuan || '-' }} 元</el-descriptions-item>
            <el-descriptions-item label="生产日期">{{ currentDetail.productionDateDisplay || '-' }}</el-descriptions-item>
            <el-descriptions-item label="到期日期">{{ currentDetail.expiryDateDisplay || '-' }}</el-descriptions-item>
            <el-descriptions-item label="保质期">{{ currentDetail.shelfLifeDays ?? '-' }} 天</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 供应商信息 -->
        <div class="detail-section">
          <div class="section-title">供应商信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="供应商">{{ currentDetail.supplierName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="采购订单号">{{ currentDetail.purchaseOrderNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="仓库">{{ currentDetail.warehouseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="库位">{{ currentDetail.storageLocation || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 溯源链条 -->
        <div class="detail-section">
          <div class="section-title">溯源链条</div>
          <el-timeline>
            <el-timeline-item
              :timestamp="formatDateTime(currentDetail.createTime)"
              placement="top"
            >
              <el-card shadow="never" class="timeline-card">
                <h4>溯源码生成</h4>
                <p>系统生成溯源码</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.inboundTime"
              :timestamp="formatDateTime(currentDetail.inboundTime)"
              placement="top"
              type="success"
            >
              <el-card shadow="never" class="timeline-card">
                <h4>入库</h4>
                <p>物料入库，状态变更为在库</p>
              </el-card>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.usedTime"
              :timestamp="formatDateTime(currentDetail.usedTime)"
              placement="top"
              type="warning"
            >
              <el-card shadow="never" class="timeline-card">
                <h4>使用/领用</h4>
                <p>操作人：{{ currentDetail.usedByName || '-' }}</p>
                <p v-if="currentDetail.usagePurpose">用途：{{ currentDetail.usagePurpose }}</p>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 溯源码文字
.trace-code-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm);
}

// 物料名称
.material-name {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 供应商
.supplier-text {
  color: var(--fts-text-secondary);
}

// 批次号
.batch-text {
  color: var(--fts-text-secondary);
  font-family: var(--fts-font-family-mono);
}

// 日期
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
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

// 详情容器
.detail-container {
  .detail-section {
    margin-bottom: var(--fts-space-4);

    &:last-child {
      margin-bottom: 0;
    }
  }

  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }

  .detail-trace-code {
    font-family: var(--fts-font-family-mono);
    color: var(--fts-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  .timeline-card {
    :deep(.el-card__body) {
      padding: var(--fts-space-3);
    }

    h4 {
      margin: 0 0 var(--fts-space-1) 0;
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-primary);
    }

    p {
      margin: 0;
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);
    }
  }
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
  .stats-grid {
    grid-template-columns: 1fr;
  }

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
