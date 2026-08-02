<script setup lang="ts">
/**
 * 采购退货页面
 *
 * 功能：
 * - 退货单列表查询
 * - 新增/编辑退货单（必须关联原入库单，支持部分物料退货）
 * - 审批退货单
 * - 现金退款到账确认
 * - 查看详情
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Document, Delete, Check, Close } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { purchaseReturnApi, purchaseReturnConverter, purchaseStockinApi } from '@/api/purchase'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { fenToYuanNumber, formatFenToYuan } from '@/utils/money'
import type {
  PurchaseReturn,
  PurchaseReturnCreateParams,
  PurchaseReturnUpdateParams,
  PurchaseReturnQueryParams,
  PurchaseReturnApproveParams,
  PurchaseReturnRefundMethod,
} from '@/types/purchase-return'
import type { PurchaseStockinInfo, PurchaseStockinItem } from '@/types/purchase-stockin'

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

interface ReturnFormItem {
  stockinItemId: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  unitPriceFen: number
  stockinQuantity: number
  returnQuantity: number
  returnReason: string
  batchNo: string
}

interface FormDataState {
  returnId: string
  stockinId: string
  stockinNo: string
  supplierId: string
  supplierName: string
  returnDate: string
  refundMethod: PurchaseReturnRefundMethod
  remark: string
  items: ReturnFormItem[]
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<PurchaseReturn[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const approveVisible = ref(false)
const currentDetail = ref<PurchaseReturn | null>(null)
const approveForm = reactive({ status: 'approved', remark: '' })

const formRef = ref<FormInstance>()
const approveFormRef = ref<FormInstance>()

const queryForm = ref<PurchaseReturnQueryParams>({
  returnNo: '',
  stockinNo: '',
  status: '' as '' | PurchaseReturn['status'],
  startDate: '',
  endDate: '',
})

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PurchaseReturn, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: PurchaseReturnQueryParams & { page: number; size: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.returnNo) queryParams.returnNo = params.returnNo
      if (params.stockinNo) queryParams.stockinNo = params.stockinNo
      if (params.status) queryParams.status = params.status
      if (params.startDate) queryParams.startDate = params.startDate
      if (params.endDate) queryParams.endDate = params.endDate
      return purchaseReturnApi.getList(queryParams)
    },
  } as unknown as CrudApi<PurchaseReturn, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

const statistics = computed(() => {
  const pending = tableData.value.filter(r => r.status === 'pending').length
  const approved = tableData.value.filter(r => r.status === 'approved').length
  const rejected = tableData.value.filter(r => r.status === 'rejected').length
  const totalAmount = tableData.value.reduce((sum, r) => sum + (r.totalAmount || 0), 0)
  return { pending, approved, rejected, totalAmount }
})

const statusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已完成', value: 'completed' },
]

const refundMethodOptions = [
  { label: '冲抵货款', value: 'offset' },
  { label: '现金退款', value: 'cash' },
]

const formData = reactive<FormDataState>({
  returnId: '',
  stockinId: '',
  stockinNo: '',
  supplierId: '',
  supplierName: '',
  returnDate: '',
  refundMethod: 'offset',
  remark: '',
  items: [],
})

const stockinOptions = ref<PurchaseStockinInfo[]>([])
const stockinOptionLoading = ref(false)

const totalAmount = computed(() => {
  return formData.items.reduce((sum, item) => {
    return sum + item.returnQuantity * fenToYuanNumber(item.unitPriceFen)
  }, 0)
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'returnNo', label: '退货单号', minWidth: 160, slot: 'returnNo' },
  { prop: 'stockinNo', label: '原入库单号', minWidth: 160, slot: 'stockinNo' },
  { prop: 'supplierName', label: '供应商', minWidth: 140, slot: 'supplierName' },
  { prop: 'returnDate', label: '退货日期', minWidth: 120, slot: 'returnDate' },
  { prop: 'totalQuantity', label: '退货数量', minWidth: 100, align: 'right' },
  { prop: 'totalAmount', label: '退货金额', minWidth: 120, slot: 'totalAmount', align: 'right' },
  { prop: 'refundMethod', label: '退款方式', minWidth: 110, slot: 'refundMethod' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  stockinId: [{ required: true, message: '请选择原入库单', trigger: 'change' }],
  returnDate: [{ required: true, message: '请选择退货日期', trigger: 'change' }],
  refundMethod: [{ required: true, message: '请选择退款方式', trigger: 'change' }],
}

const approveFormRules: FormRules = {
  status: [{ required: true, message: '请选择审批结果', trigger: 'change' }],
  remark: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (approveForm.status === 'rejected' && !value?.trim()) {
          callback(new Error('拒绝时请填写拒绝原因'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.returnNo = ''
  queryForm.value.stockinNo = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: PurchaseReturn[]) {
  selectedRows.value = rows
}

async function searchStockin(keyword: string) {
  stockinOptionLoading.value = true
  try {
    const res = await purchaseStockinApi.getList({
      status: 'completed',
      stockinNo: keyword,
      page: 1,
      size: 20,
    })
    stockinOptions.value = res.records
  } catch {
    stockinOptions.value = []
  } finally {
    stockinOptionLoading.value = false
  }
}

async function handleStockinChange(stockinId: string) {
  if (!stockinId) {
    formData.stockinNo = ''
    formData.supplierId = ''
    formData.supplierName = ''
    formData.items = []
    return
  }
  await loadStockinDetail(stockinId)
}

async function loadStockinDetail(stockinId: string, existingItems?: PurchaseReturn['items']) {
  try {
    const stockin = await purchaseStockinApi.getById(stockinId)
    if (!stockin) {
      ElMessage.error('原入库单不存在')
      return
    }
    formData.stockinNo = stockin.stockinNo
    formData.supplierId = stockin.supplierId
    formData.supplierName = stockin.supplierName
    formData.items = (stockin.items ?? []).map((item: PurchaseStockinItem) => {
      const exist = existingItems?.find(i => i.stockinItemId === item.stockinItemId)
      return {
        stockinItemId: item.stockinItemId || '',
        materialId: item.materialId,
        materialName: item.materialName,
        specification: item.specification || '',
        unit: item.unit || '',
        unitPriceFen: item.unitPriceFen || 0,
        stockinQuantity: item.quantity,
        returnQuantity: exist ? exist.quantity : 0,
        returnReason: exist ? exist.returnReason : '',
        batchNo: exist ? exist.batchNo : (item.batchNo || ''),
      }
    })
  } catch {
    ElMessage.error('加载原入库单详情失败')
  }
}

function calcItemAmount(item: ReturnFormItem) {
  return item.returnQuantity * fenToYuanNumber(item.unitPriceFen)
}

function resetForm() {
  Object.assign(formData, {
    returnId: '',
    stockinId: '',
    stockinNo: '',
    supplierId: '',
    supplierName: '',
    returnDate: '',
    refundMethod: 'offset',
    remark: '',
    items: [],
  })
  stockinOptions.value = []
}

async function handleCreate() {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
  await searchStockin('')
}

async function handleEdit(row: PurchaseReturn) {
  isEdit.value = true
  resetForm()
  try {
    const detail = await purchaseReturnApi.getById(row.id)
    if (!detail) return
    Object.assign(formData, {
      returnId: detail.id,
      stockinId: detail.stockinId,
      stockinNo: detail.stockinNo,
      supplierId: detail.supplierId,
      supplierName: detail.supplierName,
      returnDate: detail.returnDate,
      refundMethod: detail.refundMethod,
      remark: detail.remark,
    })
    await loadStockinDetail(detail.stockinId, detail.items)
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载退货单详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const returnItems = formData.items.filter(item => item.returnQuantity > 0)
  if (returnItems.length === 0) {
    ElMessage.warning('请至少配置一条退货明细')
    return
  }

  for (const item of returnItems) {
    if (item.returnQuantity > item.stockinQuantity) {
      ElMessage.warning(`物料「${item.materialName}」退货数量不能超过入库数量`)
      return
    }
  }

  submitLoading.value = true
  try {
    const items = returnItems.map(item => ({
      stockinItemId: item.stockinItemId,
      materialId: item.materialId,
      quantity: item.returnQuantity,
      returnReason: item.returnReason || undefined,
      batchNo: item.batchNo || undefined,
    }))

    if (isEdit.value && formData.returnId) {
      const updateData: PurchaseReturnUpdateParams = {
        returnId: formData.returnId,
        stockinId: formData.stockinId,
        returnDate: formData.returnDate,
        refundMethod: formData.refundMethod,
        remark: formData.remark,
        items,
      }
      await purchaseReturnApi.update(formData.returnId, updateData)
      ElMessage.success('更新成功')
    } else {
      const createData: PurchaseReturnCreateParams = {
        stockinId: formData.stockinId,
        returnDate: formData.returnDate,
        refundMethod: formData.refundMethod,
        remark: formData.remark,
        items,
      }
      await purchaseReturnApi.create(createData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleView(row: PurchaseReturn) {
  try {
    const detail = await purchaseReturnApi.getById(row.id)
    if (detail) {
      currentDetail.value = detail
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

async function handleDelete(row: PurchaseReturn) {
  try {
    await ElMessageBox.confirm(
      `确定要删除退货单「${row.returnNo}」吗？此操作不可撤销！`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
    await purchaseReturnApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

function handleApprove(row: PurchaseReturn) {
  approveForm.status = 'approved'
  approveForm.remark = ''
  currentDetail.value = row
  approveVisible.value = true
}

async function handleApproveSubmit() {
  if (!approveFormRef.value || !currentDetail.value) return
  const valid = await approveFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const params: PurchaseReturnApproveParams = {
      status: approveForm.status,
      remark: approveForm.remark,
    }
    await purchaseReturnApi.approve(currentDetail.value.id, params)
    ElMessage.success(approveForm.status === 'approved' ? '审批通过' : '已驳回')
    approveVisible.value = false
    refresh()
  } catch {
    ElMessage.error('审批操作失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleComplete(row: PurchaseReturn) {
  try {
    await ElMessageBox.confirm(
      `确认供应商退款已到账？退货单号：${row.returnNo}`,
      '到账确认',
      { confirmButtonText: '确认到账', cancelButtonText: '取消', type: 'info' },
    )
    await purchaseReturnApi.complete(row.id)
    ElMessage.success('确认到账成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatAmount(amount: number): string {
  return purchaseReturnConverter.formatYuan(amount)
}

onMounted(() => {
  searchStockin('')
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="采购退货" description="管理采购退货申请、审批及退款跟踪">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增退货
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Clock" label="待审批" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Check" label="已通过" :value="String(statistics.approved)" color-type="success" variant="bordered" />
      <StatCard icon="Close" label="已驳回" :value="String(statistics.rejected)" color-type="danger" variant="bordered" />
      <StatCard icon="Document" label="退货总额" :value="formatAmount(statistics.totalAmount)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.returnNo"
            placeholder="退货单号"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.stockinNo"
            placeholder="原入库单号"
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
              v-for="opt in statusOptions"
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
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
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
        <template #returnNo="{ row }">
          <span class="return-no-text">{{ row.returnNo }}</span>
        </template>

        <template #stockinNo="{ row }">
          <span class="stockin-no-text">{{ row.stockinNo }}</span>
        </template>

        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName }}</span>
        </template>

        <template #returnDate="{ row }">
          <span class="time-text">{{ row.returnDate }}</span>
        </template>

        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <template #refundMethod="{ row }">
          <StatusTag
            :status="purchaseReturnConverter.toRefundMethodTagStatus(row.refundMethod)"
            :label="purchaseReturnConverter.toRefundMethodLabel(row.refundMethod)"
            size="small"
            variant="light"
          />
        </template>

        <template #status="{ row }">
          <StatusTag
            :status="purchaseReturnConverter.toStatusTagStatus(row.status)"
            :label="purchaseReturnConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleView(row)">详情</el-button>
            <el-button
              v-if="row.status === 'pending'"
              link type="primary" size="default"
              @click.stop="handleEdit(row)"
            >编辑</el-button>
            <el-button
              v-if="row.status === 'pending'"
              link type="warning" size="default"
              @click.stop="handleApprove(row)"
            >审批</el-button>
            <el-button
              v-if="row.status === 'approved' && row.refundMethod === 'cash'"
              link type="success" size="default"
              @click.stop="handleComplete(row)"
            >确认到账</el-button>
            <el-button
              v-if="row.status === 'pending'"
              link type="danger" size="default"
              @click.stop="handleDelete(row)"
            >删除</el-button>
          </div>
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑采购退货' : '新增采购退货'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="原入库单" prop="stockinId">
              <el-select
                v-model="formData.stockinId"
                placeholder="请选择或搜索原入库单"
                clearable
                filterable
                remote
                reserve-keyword
                :disabled="isEdit"
                :teleported="false"
                :loading="stockinOptionLoading"
                :remote-method="searchStockin"
                style="width: 100%"
                @change="handleStockinChange"
              >
                <el-option
                  v-for="opt in stockinOptions"
                  :key="opt.stockinId"
                  :label="`${opt.stockinNo} | ${opt.supplierName}`"
                  :value="opt.stockinId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="供应商">
              <el-input v-model="formData.supplierName" disabled />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="退货日期" prop="returnDate">
              <el-date-picker
                v-model="formData.returnDate"
                type="date"
                placeholder="请选择退货日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="退款方式" prop="refundMethod">
              <el-select
                v-model="formData.refundMethod"
                placeholder="请选择退款方式"
                :teleported="false"
                style="width: 100%"
              >
                <el-option
                  v-for="opt in refundMethodOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 退货明细 -->
        <div class="form-section">
          <div class="section-title">
            <span>退货明细（从原入库单选择物料并填写退货数量）</span>
          </div>

          <el-table :data="formData.items" border style="width: 100%" class="return-items-table">
            <el-table-column label="物料名称" min-width="180">
              <template #default="{ row }">
                <span>{{ row.materialName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="100">
              <template #default="{ row }">
                <span>{{ row.specification || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="70" align="center">
              <template #default="{ row }">
                <span>{{ row.unit || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="入库数量" width="100" align="right">
              <template #default="{ row }">
                <span>{{ row.stockinQuantity }}</span>
              </template>
            </el-table-column>
            <el-table-column label="单价(元)" width="110" align="right">
              <template #default="{ row }">
                <span>{{ formatAmount(fenToYuanNumber(row.unitPriceFen)) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="退货数量" width="130" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.returnQuantity"
                  :min="0"
                  :max="row.stockinQuantity"
                  :precision="3"
                  controls-position="right"
                  size="small"
                  style="width: 100%"
                />
              </template>
            </el-table-column>
            <el-table-column label="退货原因" min-width="140">
              <template #default="{ row }">
                <el-input v-model="row.returnReason" placeholder="请输入退货原因" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="批次号" width="130">
              <template #default="{ row }">
                <el-input v-model="row.batchNo" placeholder="批次号" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="小计(元)" width="110" align="right">
              <template #default="{ row }">
                <span class="subtotal-text">¥{{ formatAmount(calcItemAmount(row)) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.items.length" class="empty-tip">
            <el-text type="info">请先选择原入库单以加载可退货物料</el-text>
          </div>

          <div class="amount-summary">
            <div class="summary-row">
              <span>退货总额：</span>
              <strong class="amount-value">¥{{ formatAmount(totalAmount) }}</strong>
            </div>
          </div>
        </div>

        <el-form-item label="备注说明">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="采购退货详情"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border size="default">
          <el-descriptions-item label="退货单号">{{ currentDetail.returnNo }}</el-descriptions-item>
          <el-descriptions-item label="原入库单号">{{ currentDetail.stockinNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ currentDetail.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="退货日期">{{ currentDetail.returnDate }}</el-descriptions-item>
          <el-descriptions-item label="退款方式">
            <StatusTag
              :status="purchaseReturnConverter.toRefundMethodTagStatus(currentDetail.refundMethod)"
              :label="purchaseReturnConverter.toRefundMethodLabel(currentDetail.refundMethod)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchaseReturnConverter.toStatusTagStatus(currentDetail.status)"
              :label="purchaseReturnConverter.toStatusLabel(currentDetail.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="退货总数量" :span="1">{{ currentDetail.totalQuantity }}</el-descriptions-item>
          <el-descriptions-item label="退货总金额">
            <span class="detail-amount">¥{{ formatAmount(currentDetail.totalAmount) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(currentDetail.updateTime) }}</el-descriptions-item>
          <el-descriptions-item v-if="currentDetail.approvalRemark" label="审批备注" :span="2">
            <span>{{ currentDetail.approvalRemark }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">退货明细</el-divider>
        <el-table :data="currentDetail.items" border size="small" v-if="currentDetail.items?.length">
          <el-table-column prop="materialName" label="物料名称" min-width="140" />
          <el-table-column prop="specification" label="规格" min-width="100" />
          <el-table-column prop="unit" label="单位" width="70" align="center" />
          <el-table-column prop="quantity" label="退货数量" width="100" align="right" />
          <el-table-column label="单价(元)" width="110" align="right">
            <template #default="{ row }">{{ formatAmount(row.unitPrice) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="110" align="right">
            <template #default="{ row }">{{ formatAmount(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column prop="returnReason" label="退货原因" min-width="120" />
          <el-table-column prop="batchNo" label="批次号" width="110" />
        </el-table>
        <el-empty v-else description="暂无明细" :image-size="60" />
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog
      v-model="approveVisible"
      title="审批采购退货"
      width="480px"
      class="fts-dialog--sm"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-alert
          :title="`退货单号：${currentDetail.returnNo}`"
          :description="`原入库单：${currentDetail.stockinNo}，供应商：${currentDetail.supplierName}`"
          type="info"
          :closable="false"
          show-icon
          class="approve-alert"
        />

        <el-form
          ref="approveFormRef"
          :model="approveForm"
          :rules="approveFormRules"
          label-width="90px"
          style="margin-top: var(--fts-space-4)"
        >
          <el-form-item label="审批结果" prop="status">
            <el-radio-group v-model="approveForm.status">
              <el-radio value="approved">
                <el-icon style="color: var(--fts-success); vertical-align: -2px; margin-right: 4px;"><Check /></el-icon>
                通过
              </el-radio>
              <el-radio value="rejected">
                <el-icon style="color: var(--fts-error); vertical-align: -2px; margin-right: 4px;"><Close /></el-icon>
                拒绝
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="审批意见" prop="remark">
            <el-input
              v-model="approveForm.remark"
              type="textarea"
              :rows="4"
              :placeholder="approveForm.status === 'rejected' ? '请输入拒绝原因（必填）' : '请输入审批意见（选填）'"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="approveVisible = false">取消</el-button>
          <el-button
            :type="approveForm.status === 'approved' ? 'success' : 'danger'"
            :loading="submitLoading"
            @click="handleApproveSubmit"
          >
            {{ approveForm.status === 'approved' ? '确认通过' : '确认拒绝' }}
          </el-button>
        </div>
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

.return-no-text,
.stockin-no-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.supplier-text {
  color: var(--fts-text-primary);
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.action-text {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

.subtotal-text {
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.amount-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-3);
  gap: var(--fts-space-4);

  .summary-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }

  .amount-value {
    color: var(--fts-primary);
    font-size: var(--fts-font-size-lg);
  }
}

.detail-amount {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-primary);
  font-size: var(--fts-font-size-base);
}

.approve-alert {
  margin-bottom: var(--fts-space-2);
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
