<script setup lang="ts">
/**
 * 库存盘点管理页面（增强版 - 财务参与 + 盘点计划）
 * 功能：盘点记录、盘点计划、新建盘点单、审核流程、差异分析
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryCheckApi, inventoryApi, warehouseApi, inventoryCheckConverter, inventoryAdjustApi } from '@/api/warehouse'
import type { InventoryCheckInfo, CheckStatus, CheckType, CheckMode, InventoryCheckQueryForm, InventoryCheckCreateForm, CheckTeamConfig, InventoryCheckPlanInfo, CheckPlanCycle, CheckPlanStatus } from '@/types/warehouse-check'
import type { WarehouseInfo } from '@/types/warehouse'
import type { InventoryInfo } from '@/types/warehouse-inventory'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== Tab 切换 ===== */
const activeTab = ref('records')

/* ===== 数据状态 ===== */
const tableData = ref<InventoryCheckInfo[]>([])
const searchForm = ref<InventoryCheckQueryForm & { keyword?: string; dateRange: [string, string] | null }>({
  checkStatus: undefined,
  startDate: '',
  endDate: '',
  keyword: '',
  dateRange: null,
})

/* ===== 仓库选项 ===== */
const warehouseOptions = ref<WarehouseInfo[]>([])

async function loadWarehouseOptions() {
  try {
    const res = await warehouseApi.getActiveList()
    warehouseOptions.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载仓库列表失败')
  }
}

/* ===== 人员选项（Mock） ===== */
const userOptions = [
  { id: 'EMP001', name: '张经理', dept: '管理层' },
  { id: 'EMP002', name: '赵仓管', dept: '仓储部' },
  { id: 'EMP005', name: '孙冷库', dept: '仓储部' },
  { id: 'EMP008', name: '周盘点员', dept: '仓储部' },
  { id: 'EMP009', name: '吴盘点员', dept: '仓储部' },
  { id: 'EMP010', name: '钱财务', dept: '财务部' },
  { id: 'EMP011', name: '郑会计', dept: '财务部' },
  { id: 'EMP012', name: '王出纳', dept: '财务部' },
]

const financeUsers = userOptions.filter(u => u.dept === '财务部')
const warehouseUsers = userOptions.filter(u => u.dept === '仓储部')

/* ===== 统计卡片 ===== */
const statistics = computed(() => {
  const all = tableData.value
  const checking = all.filter(r => r.checkStatus === 'checking').length
  const pending = all.filter(r => r.checkStatus === 'pending' || r.checkStatus === 'approved').length
  const completed = all.filter(r => r.checkStatus === 'completed').length
  const cancelled = all.filter(r => r.checkStatus === 'cancelled').length
  const diffTotal = all.reduce((sum, r) => sum + (r.items?.filter(i => i.diffQty !== 0).length || 0), 0)
  return [
    { icon: 'Loading', label: '盘点中', value: checking, colorType: 'info' as StatColorType },
    { icon: 'Warning', label: '待审核', value: pending, colorType: 'warning' as StatColorType },
    { icon: 'Check', label: '已完成', value: completed, colorType: 'success' as StatColorType },
    { icon: 'CircleClose', label: '已取消', value: cancelled, colorType: 'info' as StatColorType },
    { icon: 'DataLine', label: '差异项总数', value: diffTotal, colorType: 'error' as StatColorType },
  ]
})

/* ===== 盘点记录表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'checkCode', label: '盘点单号', minWidth: 145 },
  { prop: 'warehouseName', label: '仓库', minWidth: 130 },
  { prop: 'checkDate', label: '盘点日期', minWidth: 120 },
  { prop: 'totalItems', label: '品项数', minWidth: 85, align: 'center', slot: 'totalItems' },
  { prop: 'diffCount', label: '差异项', minWidth: 85, align: 'center', slot: 'diffCount' },
  { prop: 'checkMode', label: '盘点模式', minWidth: 90, slot: 'checkMode' },
  { prop: 'createUserName', label: '盘点人', minWidth: 90 },
  { prop: 'checkStatus', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: InventoryCheckQueryForm & { page?: number; size?: number } = {
      checkStatus: searchForm.value.checkStatus || undefined,
      startDate: searchForm.value.startDate || undefined,
      endDate: searchForm.value.endDate || undefined,
      checkCode: searchForm.value.keyword || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryCheckApi.getList(params)
    // getList 返回 InventoryCheckInfo[]（数组）
    tableData.value = Array.isArray(res) ? res : []
    pagination.total = tableData.value.length
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载盘点列表失败')
  } finally {
    loading.value = false
  }
}

function resetSearchForm() {
  searchForm.value = { checkStatus: undefined, startDate: '', endDate: '', keyword: '', dateRange: null }
}

/* ===== 盘点状态选项 ===== */
const checkStatusOptions: { label: string; value: CheckStatus }[] = [
  { label: '待盘点', value: 'pending' },
  { label: '盘点中', value: 'checking' },
  { label: '已审核', value: 'approved' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' },
]

/* ===== 盘点模式标签 ===== */
function getCheckModeLabel(mode: CheckMode): string {
  return mode === 'blind' ? '盲盘' : '明盘'
}

function getCheckModeStatusTagStatus(mode: CheckMode): string {
  return mode === 'blind' ? 'warning' : 'success'
}

/* ===== 新建盘点单 ===== */
const createDialogVisible = ref(false)
const createForm = ref<InventoryCheckCreateForm & {
  checkMode: CheckMode
  freezeInventory: boolean
  varianceThreshold: number
  leaderId: string
  counterIds: string[]
  financeSupervisorId: string
  warehouseManagerId: string
}>({
  warehouseId: '',
  checkType: 'open',
  checkDate: '',
  participatingDepts: ['仓储部', '财务部'],
  remark: '',
  checkMode: 'blind',
  freezeInventory: true,
  varianceThreshold: 5,
  leaderId: '',
  counterIds: [],
  financeSupervisorId: '',
  warehouseManagerId: '',
})
const createFormRef = ref()
const createFormRules = {
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  checkType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }],
  checkDate: [{ required: true, message: '请选择盘点日期', trigger: 'change' }],
  checkMode: [{ required: true, message: '请选择盘点模式', trigger: 'change' }],
  leaderId: [{ required: true, message: '请选择盘点负责人', trigger: 'change' }],
  counterIds: [{ required: true, message: '请选择盘点员', trigger: 'change' }],
  financeSupervisorId: [{ required: true, message: '请选择财务监督员', trigger: 'change' }],
}

/* ===== 盘点类型选项 ===== */
const checkTypeOptions: { label: string; value: CheckType }[] = [
  { label: '盲盘（推荐）', value: 'blind' },
  { label: '明盘', value: 'open' },
  { label: '循环盘点', value: 'cycle' },
]

/* ===== 盘点模式选项 ===== */
const checkModeOptions: { label: string; value: CheckMode }[] = [
  { label: '盲盘（推荐，盘点员不可见账面数量）', value: 'blind' },
  { label: '明盘（盘点员可见账面数量）', value: 'open' },
]

/* ===== 仓库库存自动加载 ===== */
const warehouseInventory = ref<InventoryInfo[]>([])
const inventoryLoading = ref(false)

async function handleWarehouseChange(warehouseId: string) {
  if (!warehouseId) {
    warehouseInventory.value = []
    return
  }
  inventoryLoading.value = true
  try {
    const res = await inventoryApi.getList({ warehouseId, page: 1, size: 200 })
    warehouseInventory.value = res?.records || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载库存数据失败')
    warehouseInventory.value = []
  } finally {
    inventoryLoading.value = false
  }
}

function handleOpenCreate() {
  createForm.value = {
    warehouseId: '',
    checkType: 'open',
    checkDate: '',
    participatingDepts: ['仓储部', '财务部'],
    remark: '',
    checkMode: 'blind',
    freezeInventory: true,
    varianceThreshold: 5,
    leaderId: '',
    counterIds: [],
    financeSupervisorId: '',
    warehouseManagerId: '',
  }
  warehouseInventory.value = []
  createDialogVisible.value = true
}

async function handleCreateSubmit() {
  try {
    await createFormRef.value?.validate()
    const team: CheckTeamConfig = {
      leaderId: createForm.value.leaderId,
      leaderName: userOptions.find(u => u.id === createForm.value.leaderId)?.name || '',
      counterIds: createForm.value.counterIds,
      counterNames: createForm.value.counterIds.map(id => userOptions.find(u => u.id === id)?.name || ''),
      financeSupervisorId: createForm.value.financeSupervisorId,
      financeSupervisorName: financeUsers.find(u => u.id === createForm.value.financeSupervisorId)?.name || '',
      warehouseManagerId: createForm.value.warehouseManagerId,
      warehouseManagerName: warehouseUsers.find(u => u.id === createForm.value.warehouseManagerId)?.name || '',
    }
    const submitData: InventoryCheckCreateForm = {
      warehouseId: createForm.value.warehouseId,
      checkType: createForm.value.checkType,
      checkDate: createForm.value.checkDate,
      participatingDepts: createForm.value.participatingDepts,
      remark: createForm.value.remark,
      checkMode: createForm.value.checkMode,
      checkTeam: team,
      freezeInventory: createForm.value.freezeInventory,
      varianceThreshold: createForm.value.varianceThreshold,
    }
    await inventoryCheckApi.create(submitData)
    ElMessage.success('盘点单创建成功')
    createDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '创建失败')
  }
}

/* ===== 审核盘点单 ===== */
async function handleApprove(row: InventoryCheckInfo) {
  try {
    await ElMessageBox.confirm('确认审核通过该盘点单？', '审核确认', {
      confirmButtonText: '通过',
      cancelButtonText: '驳回',
      distinguishCancelAndClose: true,
      type: 'warning',
    })
    await inventoryCheckApi.approve(row.checkId, true)
    ElMessage.success('审核通过')
    loadData()
  } catch (action: unknown) {
    if (action === 'cancel') {
      try {
        await inventoryCheckApi.approve(row.checkId, false, '驳回')
        ElMessage.success('已驳回')
        loadData()
      } catch (error: unknown) {
        if (error instanceof Error) ElMessage.error(error.message || '操作失败')
      }
    }
  }
}

/* ===== 查看详情 ===== */
const detailDialogVisible = ref(false)
const currentDetail = ref<InventoryCheckInfo | null>(null)

async function handleViewDetail(row: InventoryCheckInfo) {
  try {
    const res = await inventoryCheckApi.getById(row.checkId)
    currentDetail.value = res
    detailDialogVisible.value = true
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载详情失败')
  }
}

/** 审批流当前步骤 */
const currentStep = computed(() => {
  if (!currentDetail.value) return 0
  const d = currentDetail.value
  if (d.financeConfirmStatus === 'confirmed') return 3
  if (d.warehouseApprovalStatus === 'approved') return 2
  if (d.checkStatus === 'checking' || d.checkStatus === 'approved') return 1
  return 0
})

/** 差异分析 */
const varianceAnalysis = computed(() => {
  if (!currentDetail.value?.items?.length) return null
  const items = currentDetail.value.items
  const diffItems = items.filter(i => i.diffQty !== 0)
  const totalBookValue = items.reduce((sum, i) => sum + Math.abs(i.bookQty * parseFloat(i.diffAmount || '0') / (i.diffQty || 1)), 0)
  const totalDiffAmount = diffItems.reduce((sum, i) => sum + parseFloat(i.diffAmount || '0'), 0)
  const varianceRate = totalBookValue > 0 ? (Math.abs(totalDiffAmount) / totalBookValue * 100) : 0
  const threshold = currentDetail.value.varianceThreshold || 5
  const exceededItems = diffItems.filter(i => {
    if (i.bookQty === 0) return i.diffQty !== 0
    return Math.abs(i.diffQty / i.bookQty * 100) > threshold
  })
  return {
    totalDiffAmount: totalDiffAmount.toFixed(2),
    varianceRate: varianceRate.toFixed(2),
    exceededItems,
    isExceedingThreshold: varianceRate > threshold,
  }
})

/** 判断是否有差异项 */
const hasDiffItems = computed(() => {
  if (!currentDetail.value?.items?.length) return false
  return currentDetail.value.items.some(i => i.diffQty !== 0)
})

/** 从盘点结果生成调整单（盘盈盘亏分别生成） */
async function generateAdjustFromCheck() {
  if (!currentDetail.value) return
  const check = currentDetail.value
  const diffItems = check.items.filter(i => i.diffQty !== 0)
  if (diffItems.length === 0) {
    ElMessage.warning('没有差异项，无需生成调整单')
    return
  }
  try {
    const gainItems = diffItems.filter(i => i.diffQty > 0)
    const lossItems = diffItems.filter(i => i.diffQty < 0)

    await ElMessageBox.confirm(
      `将基于盘点单 ${check.checkCode} 生成调整单。${gainItems.length > 0 ? `盘盈 ${gainItems.length} 项` : ''}${gainItems.length > 0 && lossItems.length > 0 ? '，' : ''}${lossItems.length > 0 ? `盘亏 ${lossItems.length} 项` : ''}。确认生成？`,
      '生成调整单',
      { confirmButtonText: '确认生成', cancelButtonText: '取消', type: 'info' }
    )

    // 盘盈调整单
    if (gainItems.length > 0) {
      const adjustData = {
        adjustType: 'gain' as const,
        warehouseId: check.warehouseId,
        adjustReason: '盘点差异调整（盘盈）',
        referenceCheckCode: check.checkCode,
        participatingDepts: check.participatingDepts || ['仓储部'],
        items: gainItems.map(item => ({
          materialId: item.inventoryId,
          materialName: item.materialName,
          specification: item.specification,
          unit: item.unit,
          beforeQuantity: item.bookQty,
          adjustQuantity: item.diffQty,
          reason: item.reason || '盘点盘盈',
        })),
        remark: `由盘点单 ${check.checkCode} 生成（盘盈）`,
      }
      await inventoryAdjustApi.create(adjustData)
      ElMessage.success(`盘盈调整单已生成，共 ${gainItems.length} 项`)
    }

    // 盘亏调整单
    if (lossItems.length > 0) {
      const adjustData = {
        adjustType: 'loss' as const,
        warehouseId: check.warehouseId,
        adjustReason: '盘点差异调整（盘亏）',
        referenceCheckCode: check.checkCode,
        participatingDepts: check.participatingDepts || ['仓储部'],
        items: lossItems.map(item => ({
          materialId: item.inventoryId,
          materialName: item.materialName,
          specification: item.specification,
          unit: item.unit,
          beforeQuantity: item.bookQty,
          adjustQuantity: item.diffQty,
          reason: item.reason || '盘点盘亏',
        })),
        remark: `由盘点单 ${check.checkCode} 生成（盘亏）`,
      }
      await inventoryAdjustApi.create(adjustData)
      ElMessage.success(`盘亏调整单已生成，共 ${lossItems.length} 项`)
    }

    detailDialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '生成调整单失败')
    }
  }
}

/* ============================================================ */
/* ===== 盘点计划 Tab ===== */
/* ============================================================ */

/** 盘点计划 Mock 数据 */
const planList = ref<InventoryCheckPlanInfo[]>([
  {
    planId: 'CP001',
    planName: '主仓库月度盲盘',
    warehouseId: 'WH001',
    warehouseName: '主仓库',
    checkType: 'blind',
    checkMode: 'blind',
    plannedDate: '2026-07-01',
    cycle: 'monthly',
    cycleName: '每月',
    status: 'active',
    financeSupervisorId: 'EMP010',
    financeSupervisorName: '钱财务',
    lastExecutedTime: '2026-06-01 09:00:00',
    nextExecuteTime: '2026-07-01 09:00:00',
    remark: '每月1号主仓库盲盘',
    createTime: '2026-01-01 08:00:00',
    updateTime: '2026-06-01 10:00:00',
  },
  {
    planId: 'CP002',
    planName: '冷库周度盘点',
    warehouseId: 'WH002',
    warehouseName: '冷库',
    checkType: 'open',
    checkMode: 'open',
    plannedDate: '2026-06-23',
    cycle: 'weekly',
    cycleName: '每周',
    status: 'active',
    financeSupervisorId: 'EMP011',
    financeSupervisorName: '郑会计',
    lastExecutedTime: '2026-06-16 08:00:00',
    nextExecuteTime: '2026-06-23 08:00:00',
    remark: '每周一冷库明盘',
    createTime: '2026-02-01 09:00:00',
    updateTime: '2026-06-16 10:00:00',
  },
  {
    planId: 'CP003',
    planName: '冷冻库季度盘点',
    warehouseId: 'WH003',
    warehouseName: '冷冻库',
    checkType: 'blind',
    checkMode: 'blind',
    plannedDate: '2026-07-01',
    cycle: 'quarterly',
    cycleName: '每季度',
    status: 'active',
    financeSupervisorId: 'EMP010',
    financeSupervisorName: '钱财务',
    lastExecutedTime: '2026-04-01 09:00:00',
    nextExecuteTime: '2026-07-01 09:00:00',
    remark: '每季度初冷冻库盲盘',
    createTime: '2026-01-01 08:00:00',
    updateTime: '2026-04-01 10:00:00',
  },
  {
    planId: 'CP004',
    planName: '总店仓库每日抽盘',
    warehouseId: 'WH004',
    warehouseName: '总店仓库',
    checkType: 'cycle',
    checkMode: 'open',
    plannedDate: '2026-06-18',
    cycle: 'daily',
    cycleName: '每日',
    status: 'inactive',
    financeSupervisorId: 'EMP012',
    financeSupervisorName: '王出纳',
    lastExecutedTime: '2026-05-31 18:00:00',
    nextExecuteTime: '',
    remark: '已停用，改为周盘',
    createTime: '2026-03-01 09:00:00',
    updateTime: '2026-06-01 10:00:00',
  },
  {
    planId: 'CP005',
    planName: '分店仓库一次性盘点',
    warehouseId: 'WH005',
    warehouseName: '分店仓库',
    checkType: 'open',
    checkMode: 'open',
    plannedDate: '2026-06-25',
    cycle: 'once',
    cycleName: '一次',
    status: 'active',
    financeSupervisorId: 'EMP010',
    financeSupervisorName: '钱财务',
    lastExecutedTime: '',
    nextExecuteTime: '2026-06-25 09:00:00',
    remark: '分店开业前盘点',
    createTime: '2026-06-10 10:00:00',
    updateTime: '2026-06-10 10:00:00',
  },
])

/** 盘点计划表格列 */
const planColumns = [
  { prop: 'planId', label: '计划编号', minWidth: 100 },
  { prop: 'planName', label: '计划名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'warehouseName', label: '盘点仓库', minWidth: 110 },
  { prop: 'checkType', label: '盘点类型', minWidth: 100, slot: 'planCheckType' },
  { prop: 'plannedDate', label: '计划日期', minWidth: 110 },
  { prop: 'cycle', label: '周期', minWidth: 90, slot: 'planCycle' },
  { prop: 'status', label: '状态', minWidth: 80, slot: 'planStatus' },
]

/** 盘点周期选项 */
const cycleOptions: { label: string; value: CheckPlanCycle }[] = [
  { label: '一次', value: 'once' },
  { label: '每日', value: 'daily' },
  { label: '每周', value: 'weekly' },
  { label: '每月', value: 'monthly' },
  { label: '每季度', value: 'quarterly' },
]

/** 盘点计划类型选项（弹窗用） */
const planCheckTypeOptions: { label: string; value: CheckType }[] = [
  { label: '明盘', value: 'open' },
  { label: '盲盘', value: 'blind' },
  { label: '循环盘点', value: 'cycle' },
]

/** 盘点类型 → 中文标签 */
function getCheckTypeLabel(type: CheckType): string {
  const map: Record<CheckType, string> = { blind: '盲盘', open: '明盘', cycle: '循环盘点' }
  return map[type] || type
}

/** 盘点类型 → StatusTag status */
function getCheckTypeStatusTagStatus(type: CheckType): string {
  const map: Record<CheckType, string> = { blind: 'warning', open: 'success', cycle: 'info' }
  return map[type] || 'default'
}

/** 盘点周期 → 中文标签 */
function getCycleLabel(cycle: CheckPlanCycle): string {
  const map: Record<CheckPlanCycle, string> = { once: '一次', daily: '每日', weekly: '每周', monthly: '每月', quarterly: '每季度' }
  return map[cycle] || cycle
}

/** 盘点计划状态 → StatusTag status */
function getPlanStatusTagStatus(status: CheckPlanStatus): string {
  return status === 'active' ? 'success' : 'info'
}

/** 盘点计划状态 → 中文标签 */
function getPlanStatusLabel(status: CheckPlanStatus): string {
  return status === 'active' ? '启用' : '停用'
}

/* ===== 新建/编辑盘点计划弹窗 ===== */
const planDialogVisible = ref(false)
const planFormRef = ref()
const planForm = ref<{
  planId: string
  planName: string
  warehouseId: string
  checkType: CheckType
  checkMode: CheckMode
  plannedDate: string
  cycle: CheckPlanCycle
  financeSupervisorId: string
  remark: string
}>({
  planId: '',
  planName: '',
  warehouseId: '',
  checkType: 'open',
  checkMode: 'open',
  plannedDate: '',
  cycle: 'once',
  financeSupervisorId: '',
  remark: '',
})

const planFormRules = {
  planName: [{ required: true, message: '请输入计划名称', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择盘点仓库', trigger: 'change' }],
  checkType: [{ required: true, message: '请选择盘点类型', trigger: 'change' }],
  plannedDate: [{ required: true, message: '请选择计划日期', trigger: 'change' }],
  cycle: [{ required: true, message: '请选择盘点周期', trigger: 'change' }],
  financeSupervisorId: [{ required: true, message: '请选择财务监督员', trigger: 'change' }],
}

function handleOpenCreatePlan() {
  planForm.value = {
    planId: '',
    planName: '',
    warehouseId: '',
    checkType: 'open',
    checkMode: 'open',
    plannedDate: '',
    cycle: 'once',
    financeSupervisorId: '',
    remark: '',
  }
  planDialogVisible.value = true
}

function handleEditPlan(row: InventoryCheckPlanInfo) {
  planForm.value = {
    planId: row.planId,
    planName: row.planName,
    warehouseId: row.warehouseId,
    checkType: row.checkType,
    checkMode: row.checkMode,
    plannedDate: row.plannedDate,
    cycle: row.cycle,
    financeSupervisorId: row.financeSupervisorId,
    remark: row.remark,
  }
  planDialogVisible.value = true
}

async function handlePlanSubmit() {
  try {
    await planFormRef.value?.validate()
    const wh = warehouseOptions.value.find(w => w.warehouseId === planForm.value.warehouseId)
    const finance = financeUsers.find(u => u.id === planForm.value.financeSupervisorId)
    const newPlan: InventoryCheckPlanInfo = {
      planId: planForm.value.planId || `CP${String(planList.value.length + 1).padStart(3, '0')}`,
      planName: planForm.value.planName,
      warehouseId: planForm.value.warehouseId,
      warehouseName: wh?.warehouseName || '',
      checkType: planForm.value.checkType,
      checkMode: planForm.value.checkMode,
      plannedDate: planForm.value.plannedDate,
      cycle: planForm.value.cycle,
      cycleName: getCycleLabel(planForm.value.cycle),
      status: 'active',
      financeSupervisorId: planForm.value.financeSupervisorId,
      financeSupervisorName: finance?.name || '',
      lastExecutedTime: '',
      nextExecuteTime: planForm.value.plannedDate,
      remark: planForm.value.remark,
      createTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
      updateTime: new Date().toISOString().slice(0, 19).replace('T', ' '),
    }
    if (planForm.value.planId) {
      const idx = planList.value.findIndex(p => p.planId === planForm.value.planId)
      if (idx >= 0) {
        newPlan.createTime = planList.value[idx].createTime
        newPlan.lastExecutedTime = planList.value[idx].lastExecutedTime
        newPlan.status = planList.value[idx].status
        planList.value[idx] = newPlan
      }
    } else {
      planList.value.push(newPlan)
    }
    ElMessage.success(planForm.value.planId ? '计划更新成功' : '计划创建成功')
    planDialogVisible.value = false
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  }
}

/** 启用/停用计划 */
async function handleTogglePlanStatus(row: InventoryCheckPlanInfo) {
  const newStatus: CheckPlanStatus = row.status === 'active' ? 'inactive' : 'active'
  const action = newStatus === 'active' ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认${action}计划「${row.planName}」？`, `${action}确认`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    row.status = newStatus
    ElMessage.success(`已${action}`)
  } catch {
    // 用户取消
  }
}

/** 立即执行计划 */
async function handleExecutePlan(row: InventoryCheckPlanInfo) {
  try {
    await ElMessageBox.confirm(
      `确认立即执行计划「${row.planName}」？将基于该计划创建盘点单。`,
      '执行确认',
      { confirmButtonText: '确定执行', cancelButtonText: '取消', type: 'info' }
    )
    ElMessage.success(`计划「${row.planName}」已触发执行，盘点单创建中...`)
    row.lastExecutedTime = new Date().toISOString().slice(0, 19).replace('T', ' ')
  } catch {
    // 用户取消
  }
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存盘点" description="库存实物盘点与差异核对，财务部门全程参与确保账实合一">
      <el-button v-if="activeTab === 'records'" type="primary" size="default" @click="handleOpenCreate">
        <el-icon :size="16"><Plus /></el-icon>新建盘点
      </el-button>
      <el-button v-else type="primary" size="default" @click="handleOpenCreatePlan">
        <el-icon :size="16"><Plus /></el-icon>新建计划
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <el-tabs v-model="activeTab" class="check-tabs">
      <!-- Tab1: 盘点记录 -->
      <el-tab-pane label="盘点记录" name="records">

        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select v-model="searchForm.checkStatus" placeholder="盘点状态" clearable style="width:130px" size="default">
                <el-option v-for="opt in checkStatusOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
              <el-date-picker
                v-model="searchForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                placement="bottom-start"
                style="width:240px;max-width:240px;flex-shrink:0"
                size="default"
                @change="(val: [string, string] | null) => { searchForm.startDate = val?.[0] || ''; searchForm.endDate = val?.[1] || '' }"
              />
              <el-input v-model="searchForm.keyword" placeholder="搜索盘点单号" clearable style="width:200px" size="default" @keyup.enter="stdSearch">
                <template #prefix><el-icon><Search /></el-icon></template>
              </el-input>
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
              <el-button size="default" @click="stdReset">重置</el-button>
            </div>
          </div>
        </div>

        <div class="table-section">
          <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
            <template #totalItems="{ row }">
              {{ row.items?.length ?? 0 }}
            </template>
            <template #diffCount="{ row }">
              {{ row.items?.filter((i: { diffQty: number }) => i.diffQty !== 0).length ?? 0 }}
            </template>
            <template #checkMode="{ row }">
              <StatusTag :status="getCheckModeStatusTagStatus(row.checkMode)" :label="getCheckModeLabel(row.checkMode)" size="small" />
            </template>
            <template #status="{ row }">
              <StatusTag :status="inventoryCheckConverter.toStatusTagStatus(row.checkStatus)" :label="inventoryCheckConverter.toStatusLabel(row.checkStatus)" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleViewDetail(row)">详情</el-button>
              <el-button v-if="row.checkStatus === 'checking'" link type="primary" size="small" @click="handleApprove(row)">审核</el-button>
            </template>
          </DataTable>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.current"
            :page-size="pagination.size"
            :total="pagination.total"
            layout="total,prev,pager,next,jumper"
            @size-change="(val: number) => { pagination.size = val; loadData() }"
            @current-change="(val: number) => { pagination.current = val; loadData() }"
          />
        </div>
      </el-tab-pane>

      <!-- Tab2: 盘点计划 -->
      <el-tab-pane label="盘点计划" name="plans">
        <div class="table-section">
          <DataTable :columns="planColumns" :data="planList" stripe>
            <template #planCheckType="{ row }">
              <StatusTag :status="getCheckTypeStatusTagStatus(row.checkType)" :label="getCheckTypeLabel(row.checkType)" size="small" />
            </template>
            <template #planCycle="{ row }">
              {{ row.cycleName }}
            </template>
            <template #planStatus="{ row }">
              <StatusTag :status="getPlanStatusTagStatus(row.status)" :label="getPlanStatusLabel(row.status)" size="small" />
            </template>
            <template #actions="{ row }">
              <el-button link type="primary" size="small" @click="handleEditPlan(row)">编辑</el-button>
              <el-button link :type="row.status === 'active' ? 'warning' : 'success'" size="small" @click="handleTogglePlanStatus(row)">
                {{ row.status === 'active' ? '停用' : '启用' }}
              </el-button>
              <el-button link type="primary" size="small" @click="handleExecutePlan(row)">立即执行</el-button>
            </template>
          </DataTable>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建盘点单弹窗 -->
    <el-dialog v-model="createDialogVisible" title="新建盘点单" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <el-form ref="createFormRef" :model="createForm" :rules="createFormRules" label-width="110px">
        <el-form-item label="仓库" prop="warehouseId">
          <el-select v-model="createForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width:100%" @change="handleWarehouseChange">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型" prop="checkType">
          <el-select v-model="createForm.checkType" placeholder="请选择盘点类型" :teleported="false" style="width:100%">
            <el-option v-for="opt in checkTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点模式" prop="checkMode">
          <el-select v-model="createForm.checkMode" placeholder="请选择盘点模式" :teleported="false" style="width:100%">
            <el-option v-for="opt in checkModeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期" prop="checkDate">
          <el-date-picker v-model="createForm.checkDate" type="date" placeholder="请选择日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>

        <!-- 盘点团队 -->
        <el-divider content-position="left">盘点团队</el-divider>
        <el-form-item label="盘点负责人" prop="leaderId">
          <el-select v-model="createForm.leaderId" placeholder="请选择盘点负责人" :teleported="false" style="width:100%">
            <el-option v-for="u in warehouseUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点员" prop="counterIds">
          <el-select v-model="createForm.counterIds" placeholder="请选择盘点员" :teleported="false" style="width:100%" multiple>
            <el-option v-for="u in warehouseUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="财务监督员" prop="financeSupervisorId">
          <el-select v-model="createForm.financeSupervisorId" placeholder="请选择财务监督员（必选）" :teleported="false" style="width:100%">
            <el-option v-for="u in financeUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓储管理员">
          <el-select v-model="createForm.warehouseManagerId" placeholder="请选择仓储管理员" :teleported="false" style="width:100%" clearable>
            <el-option v-for="u in warehouseUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>

        <!-- 盘点配置 -->
        <el-divider content-position="left">盘点配置</el-divider>
        <el-form-item label="冻结库存">
          <el-checkbox v-model="createForm.freezeInventory">盘点期间冻结库存</el-checkbox>
          <div class="form-hint">盘点期间将暂停该仓库的出入库操作，确保数据准确性</div>
        </el-form-item>
        <el-form-item label="差异率阈值">
          <el-input-number v-model="createForm.varianceThreshold" :min="1" :max="50" :step="1" style="width:160px" />
          <span style="margin-left: var(--fts-space-2)">%</span>
          <div class="form-hint">差异率超过此阈值需额外说明原因</div>
        </el-form-item>

        <!-- 仓库库存预览 -->
        <el-form-item v-if="warehouseInventory.length > 0" label="库存预览">
          <div style="width:100%">
            <el-table :data="warehouseInventory" border size="small" style="width:100%" max-height="240">
              <el-table-column prop="materialName" label="物料名称" min-width="100" />
              <el-table-column prop="specification" label="规格" min-width="80" />
              <el-table-column prop="unit" label="单位" width="60" align="center" />
              <el-table-column prop="quantity" label="系统数量" width="80" align="center" />
            </el-table>
            <div class="inventory-hint">提示：盘点时将以上述库存数据作为账面数量</div>
          </div>
        </el-form-item>
        <el-form-item v-else-if="createForm.warehouseId && !inventoryLoading" label="库存预览">
          <div class="empty-inventory-hint">该仓库暂无库存数据</div>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleCreateSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 新建/编辑盘点计划弹窗 -->
    <el-dialog v-model="planDialogVisible" :title="planForm.planId ? '编辑盘点计划' : '新建盘点计划'" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <el-form ref="planFormRef" :model="planForm" :rules="planFormRules" label-width="110px">
        <el-form-item label="计划名称" prop="planName">
          <el-input v-model="planForm.planName" placeholder="请输入计划名称" />
        </el-form-item>
        <el-form-item label="盘点仓库" prop="warehouseId">
          <el-select v-model="planForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width:100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点类型" prop="checkType">
          <el-select v-model="planForm.checkType" placeholder="请选择盘点类型" :teleported="false" style="width:100%">
            <el-option v-for="opt in planCheckTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="计划日期" prop="plannedDate">
          <el-date-picker v-model="planForm.plannedDate" type="date" placeholder="请选择计划日期" :teleported="false" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="盘点周期" prop="cycle">
          <el-select v-model="planForm.cycle" placeholder="请选择盘点周期" :teleported="false" style="width:100%">
            <el-option v-for="opt in cycleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="财务监督员" prop="financeSupervisorId">
          <el-select v-model="planForm.financeSupervisorId" placeholder="请选择财务监督员" :teleported="false" style="width:100%">
            <el-option v-for="u in financeUsers" :key="u.id" :label="u.name" :value="u.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="planForm.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="planDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handlePlanSubmit">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 盘点详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="盘点单详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="currentDetail">
        <!-- 基本信息 -->
        <el-descriptions :column="2" border>
          <el-descriptions-item label="盘点单号">{{ currentDetail.checkCode }}</el-descriptions-item>
          <el-descriptions-item label="仓库">{{ currentDetail.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="盘点类型">{{ inventoryCheckConverter.toTypeLabel(currentDetail.checkType) }}</el-descriptions-item>
          <el-descriptions-item label="盘点模式">
            <StatusTag :status="getCheckModeStatusTagStatus(currentDetail.checkMode)" :label="getCheckModeLabel(currentDetail.checkMode)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="inventoryCheckConverter.toStatusTagStatus(currentDetail.checkStatus)" :label="inventoryCheckConverter.toStatusLabel(currentDetail.checkStatus)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="盘点日期">{{ currentDetail.checkDate }}</el-descriptions-item>
          <el-descriptions-item label="冻结库存">
            <StatusTag :status="currentDetail.freezeInventory ? 'success' : 'info'" :label="currentDetail.freezeInventory ? '已冻结' : '未冻结'" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="差异率阈值">{{ currentDetail.varianceThreshold }}%</el-descriptions-item>
        </el-descriptions>

        <!-- 盘点团队 -->
        <div v-if="currentDetail.checkTeam" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">盘点团队</h4>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="盘点负责人">{{ currentDetail.checkTeam.leaderName }}</el-descriptions-item>
            <el-descriptions-item label="盘点员">{{ currentDetail.checkTeam.counterNames?.join('、') || '-' }}</el-descriptions-item>
            <el-descriptions-item label="财务监督员">
              <span class="finance-highlight">{{ currentDetail.checkTeam.financeSupervisorName }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="仓储管理员">{{ currentDetail.checkTeam.warehouseManagerName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 审批流程 -->
        <div style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">审批流程</h4>
          <el-steps :active="currentStep" align-center>
            <el-step title="仓储主管审核" :description="currentDetail.warehouseApproverName || '待审核'" />
            <el-step title="财务监督确认" :description="currentDetail.financeConfirmerName || '待确认'" />
            <el-step title="生成调整单" description="系统自动" />
          </el-steps>
        </div>

        <!-- 差异分析 -->
        <div v-if="varianceAnalysis && currentDetail.items?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">差异分析</h4>
          <div class="variance-summary">
            <div class="variance-item">
              <span class="variance-label">差异总额：</span>
              <span :class="parseFloat(varianceAnalysis.totalDiffAmount) < 0 ? 'diff-negative' : 'diff-positive'">
                {{ varianceAnalysis.totalDiffAmount }} 元
              </span>
            </div>
            <div class="variance-item">
              <span class="variance-label">差异率：</span>
              <span :class="varianceAnalysis.isExceedingThreshold ? 'diff-negative' : 'diff-positive'">
                {{ varianceAnalysis.varianceRate }}%
              </span>
              <span v-if="varianceAnalysis.isExceedingThreshold" class="variance-warning">（超过阈值 {{ currentDetail.varianceThreshold }}%）</span>
            </div>
            <div class="variance-item">
              <span class="variance-label">超阈值品项：</span>
              <span :class="varianceAnalysis.exceededItems.length > 0 ? 'diff-negative' : ''">
                {{ varianceAnalysis.exceededItems.length }} 项
              </span>
            </div>
          </div>
        </div>

        <!-- 盘点明细 -->
        <div v-if="currentDetail.items?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">盘点明细</h4>
          <el-table :data="currentDetail.items" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column prop="specification" label="规格" min-width="80" />
            <el-table-column prop="unit" label="单位" width="70" align="center" />
            <!-- 盲盘模式下隐藏账面数量 -->
            <el-table-column v-if="currentDetail.checkMode === 'open'" prop="bookQty" label="账面数量" width="90" align="center" />
            <el-table-column prop="actualQty" label="实盘数量" width="90" align="center" />
            <el-table-column prop="diffQty" label="差异数量" width="90" align="center">
              <template #default="{ row }">
                <span :class="{ 'diff-negative': row.diffQty < 0, 'diff-positive': row.diffQty > 0 }">{{ row.diffQty }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="diffAmount" label="差异金额" width="90" align="right">
              <template #default="{ row }">
                <span :class="{ 'diff-negative': parseFloat(row.diffAmount) < 0, 'diff-positive': parseFloat(row.diffAmount) > 0 }">{{ row.diffAmount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="双人确认" width="85" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.dualConfirmed" status="success" label="已确认" size="small" />
                <StatusTag v-else status="pending" label="待确认" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="拍照" width="65" align="center">
              <template #default="{ row }">
                <StatusTag v-if="row.hasPhoto" status="info" label="有" size="small" />
                <span v-else class="no-photo">-</span>
              </template>
            </el-table-column>
            <el-table-column prop="reason" label="差异原因" min-width="100" />
          </el-table>
        </div>

        <!-- 超阈值品项差异说明 -->
        <div v-if="varianceAnalysis?.exceededItems?.length" style="margin-top: var(--fts-space-4)">
          <h4 style="margin-bottom: var(--fts-space-3)">超阈值品项差异说明</h4>
          <el-table :data="varianceAnalysis.exceededItems" border size="small">
            <el-table-column prop="materialName" label="物料名称" min-width="120" />
            <el-table-column label="差异率" width="100" align="center">
              <template #default="{ row }">
                <span class="diff-negative">{{ row.bookQty > 0 ? (Math.abs(row.diffQty / row.bookQty * 100)).toFixed(1) : '100' }}%</span>
              </template>
            </el-table-column>
            <el-table-column prop="varianceExplanation" label="差异说明" min-width="200">
              <template #default="{ row }">
                <span>{{ row.varianceExplanation || row.reason || '未说明' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-descriptions :column="1" border style="margin-top: var(--fts-space-4)" size="small">
          <el-descriptions-item label="备注">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
          <el-button
            v-if="currentDetail?.checkStatus === 'completed' && hasDiffItems"
            type="primary"
            @click="generateAdjustFromCheck"
          >
            生成调整单
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.check-tabs {
  :deep(.el-tabs__header) {
    padding: 0 var(--fts-space-6);
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.stats-section { grid-template-columns: repeat(5, 1fr); }

.form-hint {
  color: var(--fts-text-secondary);
  font-size: 12px;
  line-height: 1.4;
  margin-top: 2px;
}

.inventory-hint {
  color: var(--fts-text-secondary);
  font-size: 12px;
  margin-top: var(--fts-space-1);
  line-height: 1.4;
}

.empty-inventory-hint {
  color: var(--fts-text-secondary);
  font-size: 13px;
}

.finance-highlight {
  color: var(--fts-primary);
  font-weight: var(--fts-font-weight-medium);
}

.variance-summary {
  display: flex;
  gap: var(--fts-space-6);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-base);
}

.variance-item {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-primary);
}

.variance-label {
  color: var(--fts-text-secondary);
}

.variance-warning {
  color: var(--fts-error);
  font-size: var(--fts-font-size-xs);
  margin-left: var(--fts-space-1);
}

.diff-negative {
  color: var(--fts-error);
  font-weight: 500;
}

.diff-positive {
  color: var(--fts-success);
  font-weight: 500;
}

.no-photo {
  color: var(--fts-text-tertiary);
}
</style>
