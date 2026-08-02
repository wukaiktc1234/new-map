<script setup lang="ts">
/**
 * 采购计划页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成采购计划的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 重构要点：
 * ✅ 使用 .modern-page + PageHeader 替代 StandardPage
 * ✅ 使用 stats-section 响应式4列网格 + variant="bordered"
 * ✅ 使用 table-section + pagination-wrapper 分页分离模式
 * ✅ 使用 CSS 变量（无硬编码颜色）
 * ✅ 符合 project_rules.md 规范
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Delete, MagicStick } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { purchasePlanApi, materialArchiveApi, supplierApi } from '@/api/purchase'
import { storeInventoryApi } from '@/api/store-ops/store-inventory'
import { purchasePlanConverter } from '@/api/purchase/converters'
import { departmentApi } from '@/api/hr/department'
import type { DepartmentDTO } from '@/types/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  PurchasePlanInfo,
  PurchasePlanItem,
  PurchasePlanQueryForm,
  PurchasePlanFormData,
  PurchasePlanStatus,
} from '@/types/purchase-plan'
import { PurchasePlanStatusOptions } from '@/types/purchase-plan'
import type { MaterialArchiveInfo } from '@/types/purchase-archive'
import type { SupplierInfo } from '@/types/purchase-supplier'

const layoutStore = useLayoutStore()
const router = useRouter()

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

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<PurchasePlanInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  planNo: '',
  keyword: '',
  status: '' as '' | PurchasePlanStatus,
  startDate: '',
  endDate: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PurchasePlanInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: PurchasePlanQueryForm & { page?: number; size?: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.planNo) queryParams.planNo = params.planNo
      if (params.keyword) queryParams.keyword = params.keyword
      if (params.status) queryParams.status = params.status as PurchasePlanStatus
      if (params.startDate) queryParams.startDate = params.startDate
      if (params.endDate) queryParams.endDate = params.endDate
      return purchasePlanApi.getList(queryParams)
    },
  } as unknown as CrudApi<PurchasePlanInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

/**
 * 表单数据状态类型
 */
type PlanFormState = Partial<PurchasePlanFormData> & {
  planId: string
  planNo: string
}

const formData = reactive<PlanFormState>({
  planId: '',
  planNo: '',
  departmentId: '',
  planDate: '',
  remark: '',
  items: [],
})

// 统计数据
const statistics = computed(() => {
  const total = pagination.total || 0
  const executing = tableData.value.filter(item => item.status === 'executing').length
  const completed = tableData.value.filter(item => item.status === 'completed').length
  const thisMonthAmount = tableData.value
    .filter(item => {
      if (!item.planDate) return false
      const now = new Date()
      const planDate = new Date(item.planDate)
      return planDate.getFullYear() === now.getFullYear() && planDate.getMonth() === now.getMonth()
    })
    .reduce((sum, item) => sum + (item.totalAmount || 0), 0)
  return {
    total,
    executing,
    completed,
    thisMonthAmount: thisMonthAmount.toFixed(2),
  }
})

/** 部门选项（从人事组织架构树扁平化加载） */
const departmentOptions = ref<{ label: string; value: string }[]>([])
/** 部门选项加载状态 */
const departmentOptionsLoading = ref(false)

/** 将部门树扁平化为下拉选项 */
function flattenDepartmentTree(tree: DepartmentDTO[], prefix = ''): { label: string; value: string }[] {
  const result: { label: string; value: string }[] = []
  for (const dept of tree) {
    result.push({ label: prefix + dept.name, value: dept.id ?? '' })
    if (dept.children && dept.children.length > 0) {
      result.push(...flattenDepartmentTree(dept.children, prefix + '　'))
    }
  }
  return result
}

/** 异步加载部门选项 */
async function loadDepartmentOptions(): Promise<void> {
  departmentOptionsLoading.value = true
  try {
    const tree = await departmentApi.getDepartmentTree()
    departmentOptions.value = flattenDepartmentTree(tree || [])
  } catch {
    departmentOptions.value = []
    ElMessage.error('加载部门数据失败')
  } finally {
    departmentOptionsLoading.value = false
  }
}

/** 商品档案选项 */
const materialOptions = ref<MaterialArchiveInfo[]>([])
/** 商品ID → 最新单价（门店库存 unit_cost，元） */
const latestPriceMap = ref(new Map<string, number>())
/** 商品档案加载状态 */
const materialOptionsLoading = ref(false)

/** 供应商选项 */
const supplierOptions = ref<SupplierInfo[]>([])
/** 供应商加载状态 */
const supplierOptionsLoading = ref(false)

/** 计算计划总金额 */
const planTotalAmount = computed(() => {
  return formData.items.reduce((sum, item) => sum + (item.quantity * item.estimatedPrice || 0), 0)
})

/** 添加明细行 */
function addItem(): void {
  formData.items.push({
    materialId: '',
    materialName: '',
    specification: '',
    quantity: 1,
    unit: '',
    estimatedPrice: 0,
    isTempMaterial: false,
    supplierId: '',
    supplierName: '',
    remark: '',
  })
}

/** 删除明细行 */
function removeItem(index: number): void {
  formData.items.splice(index, 1)
}

/** 选择物料后自动填充（单价取最新单价：门店库存 unit_cost，无则 0 手动填写） */
function handleMaterialSelect(index: number): void {
  const item = formData.items[index]
  const mat = materialOptions.value.find(m => m.materialId === item.materialId)
  if (mat) {
    item.materialName = mat.materialName
    item.specification = mat.spec
    item.unit = mat.unit
    item.estimatedPrice = latestPriceMap.value.get(mat.materialId) ?? 0
    item.supplierId = mat.supplierId
    item.supplierName = mat.supplierName
  }
}

/** 选择供应商后更新名称 */
function handleSupplierSelect(index: number): void {
  const item = formData.items[index]
  const sup = supplierOptions.value.find(s => s.supplierId === item.supplierId)
  if (sup) {
    item.supplierName = sup.supplierName
  }
}

/** 重新计算单行小计 */
function recalcItemSubtotal(index: number): void {
  // 小计已通过 planTotalAmount 计算，无需单独存储
}

/** 异步加载商品档案选项（含最新单价映射：门店库存 unit_cost） */
async function loadMaterialOptions(): Promise<void> {
  materialOptionsLoading.value = true
  try {
    const [archiveRes, inventoryRes] = await Promise.all([
      materialArchiveApi.getList({ page: 1, size: 1000, status: 'active' }),
      storeInventoryApi.getList({ page: 1, size: 1000 }).catch(() => null),
    ])
    materialOptions.value = archiveRes?.records || []
    const map = new Map<string, number>()
    for (const inv of inventoryRes?.records || []) {
      const price = Number(inv.unitCost)
      if (inv.materialId && price > 0) {
        map.set(inv.materialId, price)
      }
    }
    latestPriceMap.value = map
  } catch {
    materialOptions.value = []
  } finally {
    materialOptionsLoading.value = false
  }
}

/** 异步加载供应商选项 */
async function loadSupplierOptions(): Promise<void> {
  supplierOptionsLoading.value = true
  try {
    const res = await supplierApi.getList({ status: 'active', page: 1, size: 1000 })
    supplierOptions.value = res?.records || []
  } catch {
    supplierOptions.value = []
  } finally {
    supplierOptionsLoading.value = false
  }
}

// 页面挂载时异步加载选项
onMounted(() => {
  loadDepartmentOptions()
  loadMaterialOptions()
  loadSupplierOptions()
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'planNo', label: '计划编号', minWidth: 145, slot: 'planNo' },
  { prop: 'planDate', label: '计划日期', minWidth: 110, slot: 'planDate' },
  { prop: 'departmentName', label: '部门', minWidth: 100, slot: 'departmentName' },
  { prop: 'createByName', label: '负责人', minWidth: 90, slot: 'createByName' },
  { prop: 'totalAmount', label: '计划金额(元)', minWidth: 120, slot: 'totalAmount' },
  { prop: 'itemCount', label: '物料数', minWidth: 80, slot: 'itemCount' },
  { prop: 'progress', label: '执行进度', minWidth: 140, slot: 'progress' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status', ellipsis: false },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  departmentId: [
    { required: true, message: '请选择部门', trigger: 'change' },
  ],
  planDate: [
    { required: true, message: '请选择计划日期', trigger: 'change' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.planNo = ''
  queryForm.value.keyword = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
}

function handleSelectionChange(rows: PurchasePlanInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    planId: '',
    planNo: `PP${Date.now().toString(36).toUpperCase()}`,
    departmentId: '',
    planDate: '',
    remark: '',
    items: [],
  })
  addItem()
  dialogVisible.value = true
}

async function handleEdit(row: PurchasePlanInfo) {
  isEdit.value = true
  try {
    const detail = await purchasePlanApi.getById(row.planId)
    if (detail) {
      Object.assign(formData, {
        planId: detail.planId,
        planNo: detail.planNo,
        departmentId: detail.departmentId,
        planDate: detail.planDate,
        remark: detail.remark,
        items: detail.items.map(item => ({ ...item })),
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载采购计划详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (formData.items.length === 0) {
    ElMessage.warning('请至少添加一条计划明细')
    return
  }

  submitLoading.value = true
  try {
    const submitData: PurchasePlanFormData = {
      departmentId: formData.departmentId || '',
      planDate: formData.planDate || '',
      remark: formData.remark || '',
      items: formData.items,
    }

    if (isEdit.value && formData.planId) {
      await purchasePlanApi.update(formData.planId, submitData)
      ElMessage.success('更新成功')
    } else {
      await purchasePlanApi.create(submitData)
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

/**
 * 生成计划（从库存预警）
 */
async function handleGeneratePlan(): Promise<void> {
  try {
    await ElMessageBox.confirm(
      '将根据当前库存预警数据自动生成采购计划，是否继续？',
      '生成计划确认',
      {
        confirmButtonText: '确定生成',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    const plan = await purchasePlanApi.generateFromStock()
    ElMessage.success(`已生成采购计划「${plan.planNo}」，共 ${plan.items?.length || 0} 项明细，请人工核对后提交审批`)
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '生成采购计划失败')
    }
  }
}

/**
 * 导出采购计划数据
 */
async function handleExport(): Promise<void> {
  try {
    await purchasePlanApi.exportExcel({
      planNo: queryForm.value.planNo || undefined,
      status: queryForm.value.status || undefined,
      keyword: queryForm.value.keyword || undefined,
    })
    ElMessage.success('导出成功')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  }
}

/**
 * 审批通过采购计划
 */
async function handleApprove(row: PurchasePlanInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要审批通过采购计划「${row.planNo}」吗？`,
      '审批确认',
      {
        confirmButtonText: '通过',
        cancelButtonText: '取消',
        type: 'success',
      },
    )
    await purchasePlanApi.approve(row.planId)
    ElMessage.success('审批通过')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '审批失败')
    }
  }
}

/**
 * 审批拒绝采购计划
 */
async function handleReject(row: PurchasePlanInfo): Promise<void> {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入驳回原因`,
      '驳回确认',
      {
        confirmButtonText: '确定驳回',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '请输入驳回原因',
        inputValidator: (value) => {
          if (!value || value.trim().length === 0) {
            return '请输入驳回原因'
          }
          return true
        },
      },
    )
    await purchasePlanApi.reject(row.planId, value)
    ElMessage.success('已驳回')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 开始执行采购计划
 */
async function handleExecute(row: PurchasePlanInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要开始执行采购计划「${row.planNo}」吗？`,
      '开始执行确认',
      {
        confirmButtonText: '开始执行',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await purchasePlanApi.execute(row.planId)
    ElMessage.success('已开始执行')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/**
 * 根据采购计划生成采购订单（业务链：计划 → 订单）
 */
async function handleGenerateOrder(row: PurchasePlanInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要根据采购计划「${row.planNo}」生成采购订单吗？`,
      '生成采购订单确认',
      {
        confirmButtonText: '确定生成',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await purchasePlanApi.generateOrder(row.planId)
    ElMessage.success('采购订单生成成功')
    // 业务链衔接：跳转采购订单列表并定位新订单（按计划单号过滤+高亮）
    router.push({ path: '/purchase/orders', query: { requestNo: row.planNo } })
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '生成采购订单失败')
    }
  }
}

/**
 * 删除采购计划
 */
async function handleDelete(row: PurchasePlanInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除采购计划「${row.planNo}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await purchasePlanApi.delete(row.planId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/**
 * 提交审批
 */
async function handleSubmitApproval(row: PurchasePlanInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要提交采购计划「${row.planNo}」审批吗？`,
      '提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await purchasePlanApi.submit(row.planId)
    ElMessage.success('提交成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '提交失败')
    }
  }
}

// ==================== 详情对话框 ====================

const detailVisible = ref(false)
const detailData = ref<(PurchasePlanInfo & { items: PurchasePlanItem[] }) | null>(null)

async function handleView(row: PurchasePlanInfo) {
  try {
    const res = await purchasePlanApi.getById(row.planId)
    if (res) {
      detailData.value = res
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

/**
 * 计算执行进度百分比（基于状态）
 * 草稿 0% → 待审批 20% → 已审批 40% → 执行中 70% → 已完成 100% → 已拒绝 0%
 */
function getProgressPercent(status: PurchasePlanStatus): number {
  const progressMap: Record<PurchasePlanStatus, number> = {
    draft: 0,
    pending: 20,
    approved: 40,
    executing: 70,
    completed: 100,
    rejected: 0,
  }
  return progressMap[status] ?? 0
}

/**
 * 获取进度条状态颜色类型
 */
function getProgressStatus(status: PurchasePlanStatus): '' | 'success' | 'exception' | 'warning' {
  if (status === 'completed') return 'success'
  if (status === 'rejected') return 'exception'
  if (status === 'executing') return ''
  return 'warning'
}

/**
 * 获取步骤条当前步骤索引
 * 草稿: 0, 待审批: 1, 已审批: 2, 执行中: 3, 已完成: 4, 已拒绝: 0
 */
function getStepIndex(status: PurchasePlanStatus): number {
  const stepMap: Record<PurchasePlanStatus, number> = {
    draft: 0,
    pending: 1,
    approved: 2,
    executing: 3,
    completed: 4,
    rejected: 0,
  }
  return stepMap[status] ?? 0
}

// ==================== 辅助方法 ====================

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

function formatAmount(amount: number): string {
  return `¥${amount.toFixed(2)}`
}
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="采购计划" description="制定和管理采购计划，跟踪执行情况">
      <el-button v-permission="'purchase:plan:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增计划
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="计划总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Loading" label="执行中" :value="String(statistics.executing)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Money" label="本月计划金额" :value="statistics.thisMonthAmount" color-type="warning" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.planNo"
            placeholder="计划编号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索关键词..."
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PurchasePlanStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            size="default"
            style="width: 130px"
            value-format="YYYY-MM-DD"
            :teleported="false"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            size="default"
            style="width: 130px"
            value-format="YYYY-MM-DD"
            :teleported="false"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button v-permission="'purchase:plan:create'" type="success" size="default" @click="handleGeneratePlan">
            <el-icon :size="14"><MagicStick /></el-icon>生成计划
          </el-button>
          <el-button
            v-permission="'purchase:plan:export'"
            type="default"
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
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
        <!-- 计划编号列 -->
        <template #planNo="{ row }">
          <span class="plan-no-text">{{ row.planNo }}</span>
        </template>

        <!-- 计划日期列 -->
        <template #planDate="{ row }">
          <span class="date-text">{{ formatDate(row.planDate) }}</span>
        </template>

        <!-- 部门列 -->
        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName || '-' }}</span>
        </template>

        <!-- 负责人列 -->
        <template #createByName="{ row }">
          <span class="creator-text">{{ row.createByName || '-' }}</span>
        </template>

        <!-- 计划金额列 -->
        <template #totalAmount="{ row }">
          <span class="amount-text">{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <!-- 物料数列 -->
        <template #itemCount="{ row }">
          <span class="count-text">{{ row.itemCount }}</span>
        </template>

        <!-- 执行进度列 -->
        <template #progress="{ row }">
          <div class="progress-wrapper">
            <el-progress
              :percentage="getProgressPercent(row.status)"
              :status="getProgressStatus(row.status)"
              :stroke-width="6"
              :show-text="true"
            />
          </div>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="purchasePlanConverter.toStatusTagStatus(row.status)"
            :label="purchasePlanConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 创建时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button
              v-permission="'purchase:plan:view'"
              link
              type="primary"
              size="default"
              @click.stop="handleView(row)"
            >
              详情
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:plan:edit'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:plan:submit'"
              link
              type="success"
              size="default"
              @click.stop="handleSubmitApproval(row)"
            >
              提交审批
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:plan:approve'"
              link
              type="warning"
              size="default"
              @click.stop="handleApprove(row)"
            >
              审批通过
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              v-permission="'purchase:plan:approve'"
              link
              type="danger"
              size="default"
              @click.stop="handleReject(row)"
            >
              驳回
            </el-button>
            <el-button
              v-if="row.status === 'approved'"
              v-permission="'purchase:plan:execute'"
              link
              type="success"
              size="default"
              @click.stop="handleExecute(row)"
            >
              开始执行
            </el-button>
            <el-button
              v-if="row.status === 'approved' || row.status === 'executing'"
              v-permission="'purchase:plan:create'"
              link
              type="primary"
              size="default"
              @click.stop="handleGenerateOrder(row)"
            >
              生成订单
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-permission="'purchase:plan:delete'"
              link
              type="danger"
              size="default"
              @click.stop="handleDelete(row)"
            >
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
          @current-change="refresh"
          @size-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑采购计划' : '新增采购计划'"
      width="1200px"
      class="fts-dialog--xl"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="计划编号">
                <el-input v-model="formData.planNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="计划日期" prop="planDate">
                <el-date-picker
                  v-model="formData.planDate"
                  type="date"
                  placeholder="请选择计划日期"
                  style="width: 100%"
                  value-format="YYYY-MM-DD"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属部门" prop="departmentId">
                <el-select
                  v-model="formData.departmentId"
                  placeholder="请选择部门"
                  :teleported="false"
                  :loading="departmentOptionsLoading"
                  style="width: 100%"
                >
                  <el-option
                    v-for="dept in departmentOptions"
                    :key="dept.value"
                    :label="dept.label"
                    :value="dept.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="负责人">
                <el-input value="当前用户" disabled />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 计划明细 -->
        <div class="form-section" v-loading="materialOptionsLoading || supplierOptionsLoading">
          <div class="section-title">
            <span>计划明细</span>
            <el-button type="primary" size="small" :icon="Plus" @click="addItem">
              添加物料
            </el-button>
          </div>

          <el-table :data="formData.items" border style="width: 100%" :resizable="false" class="plan-items-table">
            <el-table-column label="物料名称" min-width="200">
              <template #default="{ row, $index }">
                <el-select
                  v-model="row.materialId"
                  filterable
                  placeholder="选择物料"
                  style="width: 100%"
                  :teleported="true"
                  :popper-options="{ strategy: 'fixed' }"
                  @change="() => handleMaterialSelect($index)"
                >
                  <el-option
                    v-for="mat in materialOptions"
                    :key="mat.materialId"
                    :label="mat.materialName"
                    :value="mat.materialId"
                  >
                    <div class="mat-option">
                      <span class="mat-option__name">{{ mat.materialName }}</span>
                      <span class="mat-option__sub">{{ mat.spec ? `[${mat.spec}]` : '' }}{{ mat.supplierName ? ` · ${mat.supplierName}` : '' }}</span>
                    </div>
                  </el-option>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="120">
              <template #default="{ row }">
                <span>{{ row.specification || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="计划数量" width="120" align="center">
              <template #default="{ row, $index }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="99999"
                  :controls="false"
                  style="width: 100%"
                  @change="() => recalcItemSubtotal($index)"
                />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80" align="center">
              <template #default="{ row }">{{ row.unit || '-' }}</template>
            </el-table-column>
            <el-table-column label="预计单价(元)" width="120" align="center">
              <template #default="{ row }">
                <span>¥{{ row.estimatedPrice?.toFixed(2) || '0.00' }}</span>
              </template>
            </el-table-column>
            <el-table-column label="预计金额(元)" width="130" align="center">
              <template #default="{ row }">
                <span class="amount-text">¥{{ (row.quantity * row.estimatedPrice).toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="供应商" min-width="140">
              <template #default="{ row, $index }">
                <el-select
                  v-model="row.supplierId"
                  filterable
                  placeholder="选择供应商"
                  clearable
                  style="width: 100%"
                  :teleported="false"
                  @change="() => handleSupplierSelect($index)"
                >
                  <el-option
                    v-for="sup in supplierOptions"
                    :key="sup.supplierId"
                    :label="sup.supplierName"
                    :value="sup.supplierId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="50" align="center" fixed="right">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.items.length" class="empty-tip">
            <el-text type="info">暂未添加物料，点击上方"添加物料"按钮开始配置</el-text>
          </div>

          <!-- 金额汇总 -->
          <div class="amount-summary">
            <div class="summary-row">
              <span>计划金额合计：</span>
              <strong class="amount-value">¥{{ planTotalAmount.toFixed(2) }}</strong>
            </div>
          </div>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="500"
            show-word-limit
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
      title="采购计划详情"
      width="1200px"
      class="fts-dialog--xl"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="detailData">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border class="detail-descriptions">
          <el-descriptions-item label="计划编号">{{ detailData.planNo }}</el-descriptions-item>
          <el-descriptions-item label="计划日期">{{ detailData.planDate }}</el-descriptions-item>
          <el-descriptions-item label="所属部门">{{ detailData.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ detailData.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="计划金额">
            <span class="amount-text">¥{{ detailData.totalAmount.toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="物料数">{{ detailData.itemCount }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchasePlanConverter.toStatusTagStatus(detailData.status)"
              :label="purchasePlanConverter.toStatusLabel(detailData.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="执行进度">
            <el-progress
              :percentage="getProgressPercent(detailData.status)"
              :status="getProgressStatus(detailData.status)"
              :stroke-width="8"
            />
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailData.updateTime) }}</el-descriptions-item>
        </el-descriptions>

        <!-- 执行进度 -->
        <el-divider content-position="left">执行进度</el-divider>
        <div class="progress-detail">
          <el-steps :active="getStepIndex(detailData.status)" finish-status="success" size="small">
            <el-step title="创建草稿" />
            <el-step title="提交审批" />
            <el-step title="审批通过" />
            <el-step title="执行中" />
            <el-step title="已完成" />
          </el-steps>
        </div>

        <!-- 计划明细 -->
        <el-divider content-position="left">计划明细</el-divider>
        <el-table :data="detailData.items" size="small" border style="width: 100%">
          <el-table-column prop="materialName" label="物料名称" min-width="150" />
          <el-table-column prop="specification" label="规格" min-width="100">
            <template #default="{ row }">{{ row.specification || '-' }}</template>
          </el-table-column>
          <el-table-column prop="quantity" label="数量" width="80" align="center" />
          <el-table-column prop="unit" label="单位" width="60" align="center" />
          <el-table-column label="预计单价(元)" width="110" align="right">
            <template #default="{ row }">¥{{ row.estimatedPrice?.toFixed(2) || '0.00' }}</template>
          </el-table-column>
          <el-table-column label="预计金额(元)" width="120" align="right">
            <template #default="{ row }">
              <span class="amount-text">¥{{ (row.quantity * row.estimatedPrice).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="supplierName" label="供应商" min-width="100">
            <template #default="{ row }">{{ row.supplierName || '-' }}</template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="100">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </el-table-column>
        </el-table>

        <!-- 备注 -->
        <el-divider content-position="left" v-if="detailData.remark">备注</el-divider>
        <p v-if="detailData.remark" class="detail-remark">{{ detailData.remark }}</p>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
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

  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
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

// 进度条包装
.progress-wrapper {
  display: flex;
  align-items: center;
  padding: 0 var(--fts-space-2);
}

// 表单区块
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

// 金额汇总
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

// 计划编号
.plan-no-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-family: var(--fts-font-family-mono);
}

// 日期
.date-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 部门
.dept-text {
  color: var(--fts-text-primary);
}

// 负责人
.creator-text {
  color: var(--fts-text-primary);
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 数量
.count-text {
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

// 详情对话框样式
.detail-descriptions {
  margin-bottom: var(--fts-space-2);
}

.progress-detail {
  padding: var(--fts-space-4) 0;
}

.detail-remark {
  color: var(--fts-text-secondary);
  line-height: 1.6;
  margin: 0;
  padding: 0 var(--fts-space-2);
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
