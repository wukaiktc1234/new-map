<script setup lang="ts">
/**
 * 电子合同管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】电子合同的签署、存证和归档管理
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, Document, Edit } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { electronicContractApi, supplierApi, purchaseContractApi } from '@/api/purchase'
import { electronicContractConverter } from '@/api/purchase/converters'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { ElectronicContractInfo, ElectronicContractQueryForm, ElectronicContractStatus } from '@/types/purchase-electronic-contract'
import { ElectronicContractStatusOptions } from '@/types/purchase-electronic-contract'
import type { ElectronicSignLog, ElectronicEvidence } from '@/api/purchase/electronic-contract'
import type { SupplierInfo } from '@/types/purchase-supplier'
import ElectronicContractForm from './components/ElectronicContractForm.vue'
import ElectronicContractDetail, { type DetailAction } from './components/ElectronicContractDetail.vue'
import SignConfirmDialog from './components/SignConfirmDialog.vue'
import InvalidateDialog from './components/InvalidateDialog.vue'

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
}

interface ContractOption {
  contractId: string
  contractNo: string
  contractName: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<ElectronicContractInfo[]>([])

// 表单对话框
const formDialogVisible = ref(false)
const isEdit = ref(false)
const currentId = ref('')
const editData = ref<ElectronicContractInfo | null>(null)

// 详情对话框
const detailDialogVisible = ref(false)
const detailData = ref<ElectronicContractInfo | null>(null)
const signLogs = ref<ElectronicSignLog[]>([])
const signLogsLoading = ref(false)
const evidence = ref<ElectronicEvidence | null>(null)
const evidenceLoading = ref(false)
const loadedTabs = ref<Set<string>>(new Set())

// 签署对话框
const signDialogVisible = ref(false)
const signTarget = ref<ElectronicContractInfo | null>(null)
const signDialogTitle = ref('')

// 作废对话框
const invalidateDialogVisible = ref(false)
const invalidateTarget = ref<ElectronicContractInfo | null>(null)

// 供应商选项
const supplierOptions = ref<SupplierInfo[]>([])

// 关联采购合同选项
const contractOptions = ref<ContractOption[]>([])

// ==================== 查询表单 ====================

const queryForm = ref<ElectronicContractQueryForm & { keyword?: string }>({
  eContractNo: '',
  status: '' as ElectronicContractStatus | '',
  supplierId: '',
  startDate: '',
  endDate: '',
  keyword: '',
})

// ==================== useCrudTable ====================

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<ElectronicContractInfo, typeof queryForm.value>({
  api: {
    getList: async (params) => {
      const res = await electronicContractApi.getList({
        eContractNo: params.eContractNo || undefined,
        keyword: params.keyword || undefined,
        status: params.status || undefined,
        supplierId: params.supplierId || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
        page: params.page,
        size: params.size,
      })
      return res
    },
  } as unknown as CrudApi<ElectronicContractInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// ==================== 统计数据 ====================

const statistics = computed(() => ({
  total: pagination?.total || 0,
  pendingSign: tableData.value.filter(item => item.status === 'pending_sign').length,
  signing: tableData.value.filter(item => item.status === 'draft').length,
  completed: tableData.value.filter(item => item.status === 'signed').length,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'eContractNo', label: '合同编号', minWidth: 160, slot: 'eContractNo' },
  { prop: 'contractName', label: '合同名称', minWidth: 200, slot: 'contractName', ellipsis: true },
  { prop: 'supplierName', label: '供应商', minWidth: 140, slot: 'supplierName' },
  { prop: 'contractType', label: '合同类型', minWidth: 100, slot: 'contractType' },
  { prop: 'totalAmount', label: '合同金额', minWidth: 120, slot: 'totalAmount' },
  { prop: 'createTime', label: '发起时间', minWidth: 160, slot: 'createTime' },
  { prop: 'status', label: '签署状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.eContractNo = ''
  queryForm.value.status = ''
  queryForm.value.supplierId = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  queryForm.value.keyword = ''
  refresh()
}

function handleSelectionChange(rows: ElectronicContractInfo[]) {
  selectedRows.value = rows
}

async function loadSupplierOptions() {
  try {
    const res = await supplierApi.getList({ status: 'active', page: 1, size: 1000 })
    supplierOptions.value = res?.records || []
  } catch {
    supplierOptions.value = []
  }
}

async function loadContractOptions() {
  try {
    const res = await purchaseContractApi.getList({ status: 'active', page: 1, size: 100 })
    const records = res?.records || []
    contractOptions.value = records.map(c => ({
      contractId: c.contractId,
      contractNo: c.contractNo,
      contractName: c.contractName,
    }))
  } catch {
    contractOptions.value = []
  }
}

function handleCreate() {
  isEdit.value = false
  currentId.value = ''
  editData.value = null
  formDialogVisible.value = true
}

function handleEdit(row: ElectronicContractInfo) {
  isEdit.value = true
  currentId.value = row.eContractId
  editData.value = row
  formDialogVisible.value = true
}

function onFormSuccess() {
  formDialogVisible.value = false
  refresh()
}

async function loadSignLogs(id: string) {
  signLogsLoading.value = true
  try {
    signLogs.value = await electronicContractApi.getSignLogs(id)
  } catch {
    signLogs.value = []
  } finally {
    signLogsLoading.value = false
  }
}

async function loadEvidence(id: string) {
  evidenceLoading.value = true
  try {
    evidence.value = await electronicContractApi.getEvidence(id)
  } catch {
    evidence.value = null
  } finally {
    evidenceLoading.value = false
  }
}

async function handleView(row: ElectronicContractInfo) {
  try {
    const detail = await electronicContractApi.getById(row.eContractId)
    if (detail) {
      detailData.value = detail
      signLogs.value = []
      evidence.value = null
      loadedTabs.value.clear()
      detailDialogVisible.value = true
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '获取详情失败')
  }
}

function handleDetailTabChange(tab: string) {
  if (!detailData.value || loadedTabs.value.has(tab)) return
  const id = detailData.value.eContractId
  if (tab === 'signing') {
    loadSignLogs(id)
    loadEvidence(id)
  }
  loadedTabs.value.add(tab)
}

function handleDetailAction(action: DetailAction, target: ElectronicContractInfo) {
  detailDialogVisible.value = false
  switch (action) {
    case 'initiateSigning':
      handleInitiateSigning(target)
      break
    case 'confirmSigned':
      handleConfirmSigned(target)
      break
    case 'archive':
      handleArchive(target)
      break
    case 'terminate':
      handleTerminate(target)
      break
  }
}

function handleInitiateSigning(row: ElectronicContractInfo) {
  signTarget.value = row
  signDialogTitle.value = `发起签署 - ${row.contractName}`
  signDialogVisible.value = true
}

function handleConfirmSigned(row: ElectronicContractInfo) {
  signTarget.value = row
  signDialogTitle.value = `确认签署 - ${row.contractName}`
  signDialogVisible.value = true
}

function onSignSuccess() {
  signDialogVisible.value = false
  refresh()
}

function handleInvalidate(row: ElectronicContractInfo) {
  invalidateTarget.value = row
  invalidateDialogVisible.value = true
}

function onInvalidateSuccess() {
  invalidateDialogVisible.value = false
  refresh()
}

async function handleArchive(row: ElectronicContractInfo | null) {
  if (!row) return
  try {
    await ElMessageBox.confirm(
      '归档后合同将转入电子档案库，可随时调阅但不可修改。确定归档？',
      '确认归档',
      { confirmButtonText: '确认归档', cancelButtonText: '取消', type: 'info' }
    )
    ElMessage.success('合同已归档')
    refresh()
  } catch {
    // 用户取消
  }
}

async function handleTerminate(row: ElectronicContractInfo) {
  try {
    await ElMessageBox.confirm(
      `终止电子合同「${row.contractName}」将导致合同失去法律效力，已签署的数字签名和存证将标记为"已终止"。确定继续？`,
      '终止合同 - 风险提示',
      { confirmButtonText: '继续', cancelButtonText: '取消', type: 'warning' }
    )
    const { value: reason } = await ElMessageBox.prompt('请输入终止原因', '终止原因', {
      confirmButtonText: '确定终止',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '终止原因不能为空',
    })
    await electronicContractApi.invalidate(row.eContractId, { reason })
    ElMessage.success('合同已终止')
    refresh()
  } catch {
    // 用户取消
  }
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getContractTypeLabel(type: string): string {
  if (!type) return '采购合同'
  const map: Record<string, string> = {
    purchase: '采购合同',
    service: '服务合同',
    equipment: '设备合同',
    food: '食品供应合同',
  }
  return map[type] || '采购合同'
}

onMounted(() => {
  loadSupplierOptions()
  loadContractOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="电子合同" description="管理电子合同的签署、存证和归档">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>发起签署
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="合同总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Warning" label="待签署" :value="String(statistics.pendingSign)" color-type="warning" variant="bordered" />
      <StatCard icon="Edit" label="签署中" :value="String(statistics.signing)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.eContractNo"
            placeholder="合同编号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Document /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.keyword"
            placeholder="合同名称"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.supplierId"
            placeholder="供应商"
            clearable
            filterable
            style="width: 150px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="s in supplierOptions"
              :key="s.supplierId"
              :label="s.supplierName"
              :value="s.supplierId"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in ElectronicContractStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button type="success" size="default" @click="handleCreate">
            <el-icon :size="14"><Plus /></el-icon>发起签署
          </el-button>
          <el-button size="default" @click="() => ElMessage.info('合同模板管理功能开发中')">
            合同模板管理
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
        <!-- 合同编号列 -->
        <template #eContractNo="{ row }">
          <span class="contract-no-text">{{ row.eContractNo || '-' }}</span>
        </template>

        <!-- 合同名称列 -->
        <template #contractName="{ row }">
          <span class="contract-name-text">{{ row.contractName || '-' }}</span>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName || '-' }}</span>
        </template>

        <!-- 合同类型列 -->
        <template #contractType="{ row }">
          <span class="type-text">{{ getContractTypeLabel(row.contractType || '') }}</span>
        </template>

        <!-- 合同金额列 -->
        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ electronicContractConverter.formatYuan(row.totalAmount) }}</span>
        </template>

        <!-- 发起时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="electronicContractConverter.toStatusTagStatus(row.status)"
            :label="electronicContractConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              size="default"
              @click.stop="handleInitiateSigning(row)"
            >
              发起签署
            </el-button>
            <el-button
              v-if="row.status === 'pending_sign'"
              link
              type="primary"
              size="default"
              @click.stop="handleConfirmSigned(row)"
            >
              确认签署
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'signed'"
              link
              type="danger"
              size="default"
              @click.stop="handleInvalidate(row)"
            >
              作废
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

    <!-- 新建/编辑表单对话框 -->
    <ElectronicContractForm
      v-model:visible="formDialogVisible"
      :is-edit="isEdit"
      :edit-data="editData"
      :current-id="currentId"
      :supplier-options="supplierOptions"
      :contract-options="contractOptions"
      @success="onFormSuccess"
    />

    <!-- 详情对话框 -->
    <ElectronicContractDetail
      v-model:visible="detailDialogVisible"
      :detail-data="detailData"
      :sign-logs="signLogs"
      :sign-logs-loading="signLogsLoading"
      :evidence="evidence"
      :evidence-loading="evidenceLoading"
      @tab-change="handleDetailTabChange"
      @action="handleDetailAction"
    />

    <!-- 签署确认对话框 -->
    <SignConfirmDialog
      v-model:visible="signDialogVisible"
      :target="signTarget"
      :title="signDialogTitle"
      @success="onSignSuccess"
    />

    <!-- 作废确认对话框 -->
    <InvalidateDialog
      v-model:visible="invalidateDialogVisible"
      :target="invalidateTarget"
      @success="onInvalidateSuccess"
    />
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

// 合同编号
.contract-no-text {
  font-family: var(--fts-font-family-mono, 'Courier New', monospace);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm);
}

// 合同名称
.contract-name-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 供应商
.supplier-text {
  color: var(--fts-text-primary);
}

// 合同类型
.type-text {
  color: var(--fts-text-secondary);
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
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
