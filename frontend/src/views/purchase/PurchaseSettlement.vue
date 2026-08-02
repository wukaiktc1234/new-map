<script setup lang="ts">
/**
 * 采购结算页面 - 基于 ModernEmployee 黄金模板
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】采购结算单管理，包含结算单创建、发票管理、付款记录等功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Money, Document } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { purchaseSettlementApi, supplierApi } from '@/api/purchase'
import { purchaseSettlementConverter } from '@/api/purchase/converters'
import type {
  PurchaseSettlementInfo,
  PurchaseSettlementStatus,
  PurchaseSettlementFormData,
} from '@/types/purchase-settlement'
import { PurchaseSettlementStatusOptions } from '@/types/purchase-settlement'

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

/** 供应商选项 */
interface SupplierOption {
  supplierId: string
  supplierName: string
}

/** 付款记录项 */
interface PaymentRecordItem {
  paymentId: string
  paymentNo: string
  paymentAmount: number
  paymentMethod: string
  paymentTime: string
  voucherNo: string
  operator: string
  remark: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<PurchaseSettlementInfo[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const paymentDialogVisible = ref(false)
const isEdit = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  settlementNo: '',
  supplierId: '',
  status: '' as PurchaseSettlementStatus | '',
  startDate: '',
  endDate: '',
})

// 表格数据和分页
const tableData = ref<PurchaseSettlementInfo[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

// 统计数据
const stats = reactive({
  pending: 0,
  settling: 0,
  completed: 0,
  monthlyAmount: 0,
})

// 供应商选项
const supplierOptions = ref<SupplierOption[]>([])
const supplierOptionsLoading = ref(false)

// 当前查看的详情数据
const detailData = ref<PurchaseSettlementInfo | null>(null)

// 付款记录列表
const paymentRecords = ref<PaymentRecordItem[]>([])

// ==================== 表单数据 ====================

type SettlementFormState = Partial<PurchaseSettlementFormData> & {
  supplierId: string
  supplierName: string
  orderIds: string[]
  invoiceNo: string
  invoiceStatus: number
  paymentPlan: string
}

const formData = reactive<SettlementFormState>({
  supplierId: '',
  supplierName: '',
  orderIds: [],
  totalAmount: 0,
  dueDate: '',
  paymentMethod: '',
  invoiceNo: '',
  invoiceStatus: 0,
  paymentPlan: '',
  remark: '',
})

// 采购订单选项（待后端接口支持）
interface OrderOption {
  orderId: string
  orderNo: string
  supplierId: string
  supplierName: string
  totalAmount: number
}

const orderOptions = ref<OrderOption[]>([])
const orderOptionsLoading = ref(false)

// ==================== 计算属性 ====================

/** 已选订单总金额（自动计算结算金额） */
const selectedOrdersTotal = computed(() => {
  return formData.orderIds.reduce((sum, orderId) => {
    const order = orderOptions.value.find(o => o.orderId === orderId)
    return sum + (order?.totalAmount || 0)
  }, 0)
})

// ==================== 数据加载 ====================

/** 获取列表数据 */
async function fetchData() {
  loading.value = true
  try {
    const res = await purchaseSettlementApi.getList({
      settlementNo: queryForm.value.settlementNo || undefined,
      supplierId: queryForm.value.supplierId || undefined,
      status: (queryForm.value.status || undefined) as PurchaseSettlementStatus | undefined,
      startDate: queryForm.value.startDate || undefined,
      endDate: queryForm.value.endDate || undefined,
      page: pagination.current,
      size: pagination.pageSize,
    })
    tableData.value = res.records
    pagination.total = res.total
    updateStats(res.records)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

/** 更新统计数据 */
function updateStats(records: PurchaseSettlementInfo[]) {
  stats.pending = records.filter(r => r.status === 'pending').length
  stats.settling = records.filter(r => r.status === 'partial' || r.status === 'finance_reviewing').length
  stats.completed = records.filter(r => r.status === 'completed').length
  
  // 本月结算金额（模拟计算本月已完成的结算单金额）
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  stats.monthlyAmount = records
    .filter(r => r.status === 'completed' && r.createTime?.startsWith(currentMonth))
    .reduce((sum, r) => sum + r.totalAmount, 0)
}

/** 加载供应商选项 */
async function loadSupplierOptions() {
  supplierOptionsLoading.value = true
  try {
    const res = await supplierApi.getList({ page: 1, size: 100, status: 'active' })
    supplierOptions.value = (res.records || []).map((s: any) => ({
      supplierId: s.supplierId,
      supplierName: s.supplierName,
    }))
  } catch {
    supplierOptions.value = []
  } finally {
    supplierOptionsLoading.value = false
  }
}

/** 加载采购订单选项（根据供应商筛选） */
async function loadOrderOptions(_supplierId?: string) {
  orderOptionsLoading.value = true
  try {
    // TODO: 待后端提供「按供应商查询可结算订单」接口后替换为真实 API 调用
    // 当前无对应后端接口，先返回空列表，避免显示假数据
    orderOptions.value = []
  } catch {
    orderOptions.value = []
  } finally {
    orderOptionsLoading.value = false
  }
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'settlementNo', label: '结算单号', minWidth: 160, slot: 'settlementNo' },
  { prop: 'supplierName', label: '供应商', minWidth: 140, slot: 'supplierName' },
  { prop: 'orderCount', label: '关联订单数', minWidth: 100, align: 'center', slot: 'orderCount' },
  { prop: 'totalAmount', label: '结算金额', minWidth: 120, align: 'right', slot: 'totalAmount' },
  { prop: 'paidAmount', label: '已付金额', minWidth: 120, align: 'right', slot: 'paidAmount' },
  { prop: 'unpaidAmount', label: '未付金额', minWidth: 120, align: 'right', slot: 'unpaidAmount' },
  { prop: 'createTime', label: '结算日期', minWidth: 160, slot: 'createTime' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  totalAmount: [{ required: true, message: '请输入结算金额', trigger: 'blur' }],
  dueDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  queryForm.value.settlementNo = ''
  queryForm.value.supplierId = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  pagination.current = 1
  fetchData()
}

function handleSelectionChange(rows: PurchaseSettlementInfo[]) {
  selectedRows.value = rows
}

function handleSizeChange(size: number) {
  pagination.pageSize = size
  pagination.current = 1
  fetchData()
}

function handleCurrentChange(page: number) {
  pagination.current = page
  fetchData()
}

/** 新增结算单 */
function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    supplierId: '',
    supplierName: '',
    orderIds: [],
    totalAmount: 0,
    dueDate: '',
    paymentMethod: '',
    invoiceNo: '',
    invoiceStatus: 0,
    paymentPlan: '',
    remark: '',
  })
  dialogVisible.value = true
}

/** 编辑结算单 */
async function handleEdit(row: PurchaseSettlementInfo) {
  isEdit.value = true
  try {
    const detail = await purchaseSettlementApi.getById(row.settlementId)
    if (detail) {
      Object.assign(formData, {
        supplierId: detail.supplierId,
        supplierName: detail.supplierName,
        orderIds: detail.orderId ? [detail.orderId] : [],
        totalAmount: detail.totalAmount,
        dueDate: detail.dueDate,
        paymentMethod: detail.paymentMethod,
        invoiceNo: '',
        invoiceStatus: 0,
        paymentPlan: '',
        remark: detail.remark,
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载结算单详情失败')
  }
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      ...formData,
      orderId: formData.orderIds[0] || '',
    }

    if (isEdit.value && detailData.value) {
      await purchaseSettlementApi.update(detailData.value.settlementId, submitData)
      ElMessage.success('更新成功')
    } else {
      await purchaseSettlementApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error: unknown) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情 */
async function handleView(row: PurchaseSettlementInfo) {
  try {
    const detail = await purchaseSettlementApi.getById(row.settlementId)
    if (detail) {
      detailData.value = detail
      detailVisible.value = true
      // TODO: 待后端提供「查询付款记录」接口后替换为真实 API 调用
      // 当前 settlement.ts 无 getPaymentRecords 方法，先返回空列表，避免显示假数据
      paymentRecords.value = []
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

/** 确认付款 */
async function handlePay(row: PurchaseSettlementInfo) {
  try {
    const { value } = await ElMessageBox.prompt(
      `结算单号：${row.settlementNo}\n未付金额：¥${row.unpaidAmount.toFixed(2)} 元`,
      '确认付款',
      {
        confirmButtonText: '确认付款',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '请输入付款凭证号',
        inputValidator: (val: string) => {
          return val?.trim() ? true : '付款凭证号不能为空'
        },
      },
    )
    await purchaseSettlementApi.settle(row.settlementId, {
      action: 'pay',
      voucherNo: value.trim(),
    })
    ElMessage.success('付款确认成功')
    fetchData()
  } catch {
    // 用户取消
  }
}

/** 标记完成 */
async function handleMarkComplete(row: PurchaseSettlementInfo) {
  try {
    await ElMessageBox.confirm(
      `结算单号：${row.settlementNo}\n总金额：¥${row.totalAmount.toFixed(2)} 元\n已付金额：¥${row.paidAmount.toFixed(2)} 元\n\n确认标记此结算单为已完成？`,
      '确认完成结算',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await purchaseSettlementApi.settle(row.settlementId, { action: 'complete' })
    ElMessage.success('结算已完成')
    fetchData()
  } catch {
    // 用户取消
  }
}

/** 批量结算 */
async function handleBatchSettle() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要结算的单据')
    return
  }
  // [待后端配合] 待后端提供「批量结算」接口后替换为真实 API 调用
  // 当前 settlement.ts 无批量结算方法，提示功能开发中，不显示假成功
  ElMessage.info('[待后端配合] 批量结算功能开发中，待后端接口支持')
}

/** 查看付款记录 */
async function handlePaymentRecords(row: PurchaseSettlementInfo) {
  detailData.value = row
  // TODO: 待后端提供「查询付款记录」接口后替换为真实 API 调用
  // 当前先清空付款记录，避免显示假数据
  paymentRecords.value = []
  paymentDialogVisible.value = true
}

/** 供应商变更时加载订单 */
function onSupplierChange(supplierId: string) {
  const supplier = supplierOptions.value.find(s => s.supplierId === supplierId)
  if (supplier) {
    formData.supplierName = supplier.supplierName
  }
  formData.orderIds = []
  loadOrderOptions(supplierId)
}

/** 订单选择变更时自动计算金额 */
function onOrderChange() {
  formData.totalAmount = selectedOrdersTotal.value
}

/** 格式化金额 */
function formatAmount(amount: number): string {
  return amount.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 格式化日期时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  return iso
}

// 页面挂载时加载数据
onMounted(() => {
  fetchData()
  loadSupplierOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="采购结算" description="管理采购结算单、发票、付款">
      <el-button v-permission="'purchase:settlement:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增结算
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Warning" label="待结算" :value="String(stats.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Clock" label="结算中" :value="String(stats.settling)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(stats.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Money" label="本月结算金额" :value="'¥' + formatAmount(stats.monthlyAmount)" color-type="primary" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.settlementNo"
            placeholder="结算单号"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.supplierId"
            placeholder="供应商"
            clearable
            filterable
            style="width: 160px"
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
            placeholder="结算状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PurchaseSettlementStatusOptions"
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
          <el-button
            v-permission="'purchase:settlement:approve'"
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchSettle"
          >
            批量结算
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
        <!-- 结算单号列 -->
        <template #settlementNo="{ row }">
          <span class="settlement-no">{{ row.settlementNo }}</span>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName }}</span>
        </template>

        <!-- 关联订单数列 -->
        <template #orderCount="{ row }">
          <span>{{ row.orderId ? 1 : 0 }}</span>
        </template>

        <!-- 结算金额列 -->
        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <!-- 已付金额列 -->
        <template #paidAmount="{ row }">
          <span class="paid-text">¥{{ formatAmount(row.paidAmount) }}</span>
        </template>

        <!-- 未付金额列 -->
        <template #unpaidAmount="{ row }">
          <span class="unpaid-text">¥{{ formatAmount(row.unpaidAmount) }}</span>
        </template>

        <!-- 结算日期列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="purchaseSettlementConverter.toStatusTagStatus(row.status)"
            :label="purchaseSettlementConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:settlement:view'" link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'pending' || row.status === 'partial'"
              v-permission="'purchase:settlement:edit'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'pending' || row.status === 'partial' || row.status === 'finance_reviewing'"
              v-permission="'purchase:settlement:approve'"
              link
              type="success"
              size="default"
              @click.stop="handlePay(row)"
            >
              付款
            </el-button>
            <el-button
              v-if="row.status === 'partial' || row.status === 'completed'"
              v-permission="'purchase:settlement:view'"
              link
              type="info"
              size="default"
              @click.stop="handlePaymentRecords(row)"
            >
              付款记录
            </el-button>
            <el-button
              v-if="row.status === 'partial' && row.unpaidAmount === 0"
              v-permission="'purchase:settlement:approve'"
              link
              type="warning"
              size="default"
              @click.stop="handleMarkComplete(row)"
            >
              标记完成
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
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑结算单' : '新增结算单'"
      width="1100px"
      class="fts-dialog--wide"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="供应商" prop="supplierId">
              <el-select
                v-model="formData.supplierId"
                placeholder="请选择供应商"
                filterable
                :teleported="false"
                style="width: 100%"
                @change="onSupplierChange"
              >
                <el-option
                  v-for="s in supplierOptions"
                  :key="s.supplierId"
                  :label="s.supplierName"
                  :value="s.supplierId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="dueDate">
              <el-date-picker
                v-model="formData.dueDate"
                type="date"
                placeholder="请选择到期日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 选择入库单 -->
        <div class="form-section">
          <div class="section-title">
            <span>关联入库单</span>
            <span class="section-tip">选择入库单后自动计算结算金额</span>
          </div>
          <el-select
            v-model="formData.orderIds"
            multiple
            filterable
            placeholder="请选择入库单（可多选）"
            :loading="orderOptionsLoading"
            :teleported="false"
            style="width: 100%"
            @change="onOrderChange"
          >
            <el-option
              v-for="o in orderOptions"
              :key="o.orderId"
              :label="`${o.orderNo} - ¥${o.totalAmount.toFixed(2)}`"
              :value="o.orderId"
            />
          </el-select>
        </div>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="结算金额" prop="totalAmount">
              <el-input-number
              v-model="formData.totalAmount"
              :precision="2"
              :min="0"
              controls-position="right"
              style="width: 100%"
            />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="付款方式">
              <el-select
                v-model="formData.paymentMethod"
                placeholder="请选择付款方式"
                :teleported="false"
                style="width: 100%"
              >
                <el-option label="银行转账" value="银行转账" />
                <el-option label="现金支付" value="现金支付" />
                <el-option label="支票" value="支票" />
                <el-option label="承兑汇票" value="承兑汇票" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 发票信息 -->
        <div class="form-section">
          <div class="section-title">
            <span>发票信息</span>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="发票号">
                <el-input v-model="formData.invoiceNo" placeholder="请输入发票号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="发票状态">
                <el-select v-model="formData.invoiceStatus" :teleported="false" style="width: 100%">
                  <el-option label="未开票" :value="0" />
                  <el-option label="已开票" :value="1" />
                  <el-option label="已收票" :value="2" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 付款计划 -->
        <div class="form-section">
          <div class="section-title">
            <span>付款计划</span>
          </div>
          <el-input
            v-model="formData.paymentPlan"
            type="textarea"
            :rows="2"
            placeholder="请输入付款计划说明"
          />
        </div>

        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="结算单详情"
      width="1100px"
      class="fts-dialog--wide"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="结算单号">{{ detailData.settlementNo }}</el-descriptions-item>
          <el-descriptions-item label="供应商">{{ detailData.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="采购订单">{{ detailData.orderNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchaseSettlementConverter.toStatusTagStatus(detailData.status)"
              :label="purchaseSettlementConverter.toStatusLabel(detailData.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="结算金额">¥{{ formatAmount(detailData.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="已付金额">¥{{ formatAmount(detailData.paidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="未付金额">¥{{ formatAmount(detailData.unpaidAmount) }}</el-descriptions-item>
          <el-descriptions-item label="到期日期">{{ detailData.dueDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="付款方式">{{ detailData.paymentMethod || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">付款记录</el-divider>
        <el-table v-if="paymentRecords.length > 0" :data="paymentRecords" size="small" border>
          <el-table-column prop="paymentNo" label="付款单号" min-width="140" />
          <el-table-column prop="paymentAmount" label="付款金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.paymentAmount) }}</template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="付款方式" width="100" />
          <el-table-column prop="paymentTime" label="付款时间" min-width="160" />
          <el-table-column prop="voucherNo" label="凭证号" min-width="120" />
          <el-table-column prop="operator" label="操作人" width="100" />
          <el-table-column prop="remark" label="备注" min-width="120" />
        </el-table>
        <el-empty v-else description="暂无付款记录（[待后端配合] 付款记录查询功能开发中）" :image-size="60" />
      </template>
    </el-dialog>

    <!-- 付款记录对话框 -->
    <el-dialog
      v-model="paymentDialogVisible"
      title="付款记录"
      width="1100px"
      class="fts-dialog--wide"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="detailData">
        <div class="payment-summary">
          <div class="summary-item">
            <span class="label">结算单号：</span>
            <span class="value">{{ detailData.settlementNo }}</span>
          </div>
          <div class="summary-item">
            <span class="label">总金额：</span>
            <span class="value amount">¥{{ formatAmount(detailData.totalAmount) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">已付：</span>
            <span class="value paid">¥{{ formatAmount(detailData.paidAmount) }}</span>
          </div>
          <div class="summary-item">
            <span class="label">未付：</span>
            <span class="value unpaid">¥{{ formatAmount(detailData.unpaidAmount) }}</span>
          </div>
        </div>
        <el-table v-if="paymentRecords.length > 0" :data="paymentRecords" size="default" border>
          <el-table-column prop="paymentNo" label="付款单号" min-width="140" />
          <el-table-column prop="paymentAmount" label="付款金额" width="120" align="right">
            <template #default="{ row }">¥{{ formatAmount(row.paymentAmount) }}</template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="付款方式" width="100" />
          <el-table-column prop="paymentTime" label="付款时间" min-width="160" />
          <el-table-column prop="voucherNo" label="凭证号" min-width="120" />
          <el-table-column prop="operator" label="操作人" width="100" />
          <el-table-column prop="remark" label="备注" min-width="120" />
        </el-table>
        <el-empty v-else description="暂无付款记录（[待后端配合] 付款记录查询功能开发中）" :image-size="60" />
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="paymentDialogVisible = false">关闭</el-button>
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

// 结算单号
.settlement-no {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-primary);
}

// 供应商
.supplier-text {
  color: var(--fts-text-primary);
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.paid-text {
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
  color: var(--fts-success);
}

.unpaid-text {
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
  color: var(--fts-warning);
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

// 表单分区
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

  .section-tip {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
    font-weight: var(--fts-font-weight-normal);
  }
}

// 付款汇总
.payment-summary {
  display: flex;
  gap: var(--fts-space-6);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  background: var(--fts-bg-light);
  border-radius: var(--fts-card-radius);

  .summary-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);

    .label {
      color: var(--fts-text-secondary);
      font-size: var(--fts-font-size-sm);
    }

    .value {
      font-weight: var(--fts-font-weight-medium);
      color: var(--fts-text-primary);

      &.amount {
        font-size: var(--fts-font-size-lg);
        color: var(--fts-primary);
      }

      &.paid {
        color: var(--fts-success);
      }

      &.unpaid {
        color: var(--fts-warning);
      }
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
