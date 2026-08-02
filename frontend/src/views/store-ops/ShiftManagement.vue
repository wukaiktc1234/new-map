<script setup lang="ts">
/**
 * 班次管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理门店员工的排班和交接班
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 *
 * 功能：
 * - 统计卡片：今日班次、在岗人数、待交接、异常班次
 * - 顶部筛选：门店选择、日期选择、班次类型
 * - 视图切换：排班日历视图 / 列表视图
 * - 列表视图表格：班次编号、门店、班次类型、开始/结束时间、负责员工、状态、交接状态、操作
 * - 排班日历（周视图）：按日期和班次展示排班，显示值班员工
 * - 新增/编辑排班对话框
 * - 交接班对话框
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Calendar, List, SwitchButton } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { employeeApi } from '@/api/hr'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import type { Employee } from '@/types/hr'

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

/** 班次类型 */
type ShiftType = 'morning' | 'noon' | 'evening' | 'full'

/** 班次状态 */
type ShiftStatus = 'pending' | 'ongoing' | 'completed' | 'cancelled'

/** 交接状态 */
type HandoverStatus = 'not_started' | 'in_progress' | 'completed' | 'exception'

/** 班次记录 */
interface ShiftRecord {
  shiftId: string
  shiftCode: string
  storeId: string
  storeName: string
  shiftType: ShiftType
  shiftDate: string
  startTime: string
  endTime: string
  employeeIds: string[]
  employeeNames: string[]
  status: ShiftStatus
  handoverStatus: HandoverStatus
  remark?: string
  createTime: string
}

/** 班次表单数据 */
interface ShiftFormData {
  shiftId?: string
  storeId: string
  shiftType: ShiftType
  shiftDate: string
  startTime: string
  endTime: string
  employeeIds: string[]
  remark?: string
}

/** 交接班表单数据 */
interface HandoverFormData {
  shiftId: string
  shiftCode: string
  handoverPersonId: string
  handoverPersonName: string
  successorId: string
  successorName: string
  handoverContent: string
  revenueAmount: string
  remark?: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<ShiftRecord[]>([])
const dialogVisible = ref(false)
const handoverDialogVisible = ref(false)
const isEdit = ref(false)
const viewMode = ref<'list' | 'calendar'>('list')

const formRef = ref<FormInstance>()
const handoverFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  storeId: '' as string,
  shiftDate: '' as string,
  shiftType: '' as ShiftType | '',
  status: '' as ShiftStatus | '',
})

// 班次本地数据源（TODO: 班次管理后端 API 落地后替换为真实接口调用）
const mockShiftData = ref<ShiftRecord[]>([])

// 使用 useCrudTable 管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<ShiftRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      // TODO: 班次管理后端 API 落地后替换为真实接口调用（当前基于本地 mockShiftData 过滤）
      let filtered = [...mockShiftData.value]
      if (params.storeId) {
        filtered = filtered.filter(s => s.storeId === params.storeId)
      }
      if (params.shiftDate) {
        filtered = filtered.filter(s => s.shiftDate === params.shiftDate)
      }
      if (params.shiftType) {
        filtered = filtered.filter(s => s.shiftType === params.shiftType)
      }
      if (params.status) {
        filtered = filtered.filter(s => s.status === params.status)
      }
      const start = (params.page - 1) * params.size
      const end = start + params.size
      return {
        records: filtered.slice(start, end),
        total: filtered.length,
        current: params.page,
        size: params.size,
        pages: Math.ceil(filtered.length / params.size),
      }
    },
  } as unknown as CrudApi<ShiftRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
  pageSize: 20,
})

// 表单数据
const formData = reactive<ShiftFormData>({
  storeId: '',
  shiftType: 'morning',
  shiftDate: '',
  startTime: '06:00',
  endTime: '14:00',
  employeeIds: [],
  remark: '',
})

// 交接班表单数据
const handoverFormData = reactive<HandoverFormData>({
  shiftId: '',
  shiftCode: '',
  handoverPersonId: '',
  handoverPersonName: '',
  successorId: '',
  successorName: '',
  handoverContent: '',
  revenueAmount: '',
  remark: '',
})

// 门店选项（从真实 API 加载，使用共享 composable）
const { storeOptions, getStoreName } = useStoreOptions(true)

// 员工选项
const employeeOptions = ref<Employee[]>([])

// ==================== 统计数据 ====================

const statistics = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  const todayShifts = mockShiftData.value.filter(s => s.shiftDate === today)
  const onDutyCount = mockShiftData.value.filter(s => s.status === 'ongoing').reduce(
    (sum, s) => sum + s.employeeIds.length, 0
  )
  const pendingHandover = mockShiftData.value.filter(
    s => s.handoverStatus === 'in_progress' || s.handoverStatus === 'not_started'
  ).length
  const exceptionCount = mockShiftData.value.filter(
    s => s.handoverStatus === 'exception' || s.status === 'cancelled'
  ).length
  return {
    todayShifts: todayShifts.length,
    onDuty: onDutyCount,
    pendingHandover,
    exception: exceptionCount,
  }
})

// ==================== 日历视图相关 ====================

const calendarWeekStart = ref('')

function getCurrentWeekStart(): string {
  const now = new Date()
  const day = now.getDay() || 7
  const monday = new Date(now)
  monday.setDate(now.getDate() - day + 1)
  return monday.toISOString().split('T')[0]
}

function getWeekDates() {
  const start = calendarWeekStart.value || getCurrentWeekStart()
  const startDate = new Date(start)
  const weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  const result: { date: string; weekday: string; isWeekend: boolean }[] = []
  for (let i = 0; i < 7; i++) {
    const d = new Date(startDate)
    d.setDate(startDate.getDate() + i)
    const dateStr = d.toISOString().split('T')[0]
    result.push({
      date: dateStr,
      weekday: weekdays[i],
      isWeekend: i >= 5,
    })
  }
  return result
}

const weekDates = computed(() => getWeekDates())

function getShiftsByDate(date: string): ShiftRecord[] {
  return mockShiftData.value.filter(s => s.shiftDate === date)
}

function goToPrevWeek(): void {
  const start = calendarWeekStart.value || getCurrentWeekStart()
  const d = new Date(start)
  d.setDate(d.getDate() - 7)
  calendarWeekStart.value = d.toISOString().split('T')[0]
}

function goToNextWeek(): void {
  const start = calendarWeekStart.value || getCurrentWeekStart()
  const d = new Date(start)
  d.setDate(d.getDate() + 7)
  calendarWeekStart.value = d.toISOString().split('T')[0]
}

function goToCurrentWeek(): void {
  calendarWeekStart.value = getCurrentWeekStart()
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'shiftCode', label: '班次编号', minWidth: 140, slot: 'shiftCode' },
  { prop: 'storeName', label: '门店', minWidth: 120, slot: 'storeName' },
  { prop: 'shiftType', label: '班次类型', minWidth: 100, slot: 'shiftType' },
  { prop: 'startTime', label: '开始时间', minWidth: 90, slot: 'startTime' },
  { prop: 'endTime', label: '结束时间', minWidth: 90, slot: 'endTime' },
  { prop: 'employeeNames', label: '负责员工', minWidth: 160, slot: 'employeeNames' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'handoverStatus', label: '交接状态', minWidth: 100, slot: 'handoverStatus' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  storeId: [{ required: true, message: '请选择门店', trigger: 'change' }],
  shiftType: [{ required: true, message: '请选择班次类型', trigger: 'change' }],
  shiftDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  employeeIds: [{ required: true, message: '请选择负责员工', trigger: 'change' }],
}

const handoverFormRules: FormRules = {
  handoverPersonId: [{ required: true, message: '请选择交接人', trigger: 'change' }],
  successorId: [{ required: true, message: '请选择接班人', trigger: 'change' }],
  handoverContent: [{ required: true, message: '请填写交接内容', trigger: 'blur' }],
  revenueAmount: [{ required: true, message: '请填写营业额', trigger: 'blur' }],
}

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.storeId = ''
  queryForm.value.shiftDate = ''
  queryForm.value.shiftType = ''
  queryForm.value.status = ''
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: ShiftRecord[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    storeId: '',
    shiftType: 'morning' as ShiftType,
    shiftDate: new Date().toISOString().split('T')[0],
    startTime: '06:00',
    endTime: '14:00',
    employeeIds: [],
    remark: '',
  })
  dialogVisible.value = true
}

function handleEdit(row: ShiftRecord) {
  isEdit.value = true
  Object.assign(formData, {
    shiftId: row.shiftId,
    storeId: row.storeId,
    shiftType: row.shiftType,
    shiftDate: row.shiftDate,
    startTime: row.startTime,
    endTime: row.endTime,
    employeeIds: [...row.employeeIds],
    remark: row.remark || '',
  })
  dialogVisible.value = true
}

function handleDetail(row: ShiftRecord) {
  ElMessageBox.alert(
    `
    <div style="text-align: left;">
      <p><strong>班次编号：</strong>${row.shiftCode}</p>
      <p><strong>门店：</strong>${row.storeName}</p>
      <p><strong>班次类型：</strong>${getShiftTypeLabel(row.shiftType)}</p>
      <p><strong>日期：</strong>${row.shiftDate}</p>
      <p><strong>时间：</strong>${row.startTime} - ${row.endTime}</p>
      <p><strong>负责员工：</strong>${row.employeeNames.join('、')}</p>
      <p><strong>状态：</strong>${getShiftStatusLabel(row.status)}</p>
      <p><strong>交接状态：</strong>${getHandoverStatusLabel(row.handoverStatus)}</p>
      ${row.remark ? `<p><strong>备注：</strong>${row.remark}</p>` : ''}
    </div>
    `,
    '班次详情',
    { dangerouslyUseHTMLString: true }
  )
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))

    if (isEdit.value && formData.shiftId) {
      const index = mockShiftData.value.findIndex(s => s.shiftId === formData.shiftId)
      if (index !== -1) {
        const store = storeOptions.value.find(s => String(s.storeId) === formData.storeId)
        const employees = employeeOptions.value.filter(e =>
          formData.employeeIds.includes(String(e.employeeId))
        )
        mockShiftData.value[index] = {
          ...mockShiftData.value[index],
          storeId: formData.storeId,
          storeName: store?.storeName || '',
          shiftType: formData.shiftType,
          shiftDate: formData.shiftDate,
          startTime: formData.startTime,
          endTime: formData.endTime,
          employeeIds: formData.employeeIds,
          employeeNames: employees.map(e => e.employeeName),
          remark: formData.remark,
        }
      }
      ElMessage.success('更新成功')
    } else {
      const store = storeOptions.value.find(s => String(s.storeId) === formData.storeId)
      const employees = employeeOptions.value.filter(e =>
        formData.employeeIds.includes(String(e.employeeId))
      )
      const newShift: ShiftRecord = {
        shiftId: `SH${Date.now()}`,
        shiftCode: `SHIFT-${formData.shiftDate.slice(5).replace(/-/g, '')}-${String(mockShiftData.value.length + 1).padStart(3, '0')}`,
        storeId: formData.storeId,
        storeName: store?.storeName || '',
        shiftType: formData.shiftType,
        shiftDate: formData.shiftDate,
        startTime: formData.startTime,
        endTime: formData.endTime,
        employeeIds: formData.employeeIds,
        employeeNames: employees.map(e => e.employeeName),
        status: 'pending',
        handoverStatus: 'not_started',
        remark: formData.remark,
        createTime: new Date().toISOString(),
      }
      mockShiftData.value.unshift(newShift)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: ShiftRecord): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除班次「${row.shiftCode}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 300))
    mockShiftData.value = mockShiftData.value.filter(s => s.shiftId !== row.shiftId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

function handleHandover(row: ShiftRecord) {
  if (row.employeeIds.length === 0) {
    ElMessage.warning('该班次无员工，无法进行交接')
    return
  }
  handoverFormData.shiftId = row.shiftId
  handoverFormData.shiftCode = row.shiftCode
  handoverFormData.handoverPersonId = row.employeeIds[0]
  handoverFormData.handoverPersonName = row.employeeNames[0]
  handoverFormData.successorId = ''
  handoverFormData.successorName = ''
  handoverFormData.handoverContent = ''
  handoverFormData.revenueAmount = ''
  handoverFormData.remark = ''
  handoverDialogVisible.value = true
}

async function handleHandoverSubmit() {
  if (!handoverFormRef.value) return

  const valid = await handoverFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    const index = mockShiftData.value.findIndex(s => s.shiftId === handoverFormData.shiftId)
    if (index !== -1) {
      mockShiftData.value[index].handoverStatus = 'completed'
    }
    ElMessage.success('交接成功')
    handoverDialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error('交接失败')
  } finally {
    submitLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getShiftTypeLabel(type: ShiftType): string {
  const map: Record<ShiftType, string> = {
    morning: '早班',
    noon: '中班',
    evening: '晚班',
    full: '全天',
  }
  return map[type] || type
}

function getShiftTypeColor(type: ShiftType): string {
  const map: Record<ShiftType, string> = {
    morning: 'primary',
    noon: 'warning',
    evening: 'info',
    full: 'success',
  }
  return map[type] || 'info'
}

function getShiftStatusLabel(status: ShiftStatus): string {
  const map: Record<ShiftStatus, string> = {
    pending: '待开始',
    ongoing: '进行中',
    completed: '已完成',
    cancelled: '已取消',
  }
  return map[status] || status
}

function getShiftStatusColor(status: ShiftStatus): string {
  const map: Record<ShiftStatus, string> = {
    pending: 'inactive',
    ongoing: 'active',
    completed: 'success',
    cancelled: 'error',
  }
  return map[status] || 'info'
}

function getHandoverStatusLabel(status: HandoverStatus): string {
  const map: Record<HandoverStatus, string> = {
    not_started: '未开始',
    in_progress: '交接中',
    completed: '已完成',
    exception: '异常',
  }
  return map[status] || status
}

function getHandoverStatusColor(status: HandoverStatus): string {
  const map: Record<HandoverStatus, string> = {
    not_started: 'inactive',
    in_progress: 'warning',
    completed: 'success',
    exception: 'error',
  }
  return map[status] || 'info'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function loadEmployeeOptions(): Promise<void> {
  try {
    const res = await employeeApi.getList({ current: 1, size: 100 })
    employeeOptions.value = res.records || []
  } catch {
    // API失败时降级为空数组
    employeeOptions.value = []
  }
}

// 班次类型变化时更新默认时间
function onShiftTypeChange() {
  const timeMap: Record<ShiftType, { start: string; end: string }> = {
    morning: { start: '06:00', end: '14:00' },
    noon: { start: '10:00', end: '18:00' },
    evening: { start: '14:00', end: '22:00' },
    full: { start: '09:00', end: '21:00' },
  }
  const times = timeMap[formData.shiftType]
  if (times) {
    formData.startTime = times.start
    formData.endTime = times.end
  }
}

onMounted(() => {
  loadEmployeeOptions()
  calendarWeekStart.value = getCurrentWeekStart()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="排班管理" description="管理门店员工的排班和交接班">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增排班
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Calendar" label="今日班次" :value="String(statistics.todayShifts)" color-type="primary" variant="bordered" />
      <StatCard icon="User" label="在岗人数" :value="String(statistics.onDuty)" color-type="success" variant="bordered" />
      <StatCard icon="SwitchButton" label="待交接" :value="String(statistics.pendingHandover)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="异常班次" :value="String(statistics.exception)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.storeId"
            placeholder="选择门店"
            clearable
            style="width: 150px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="String(store.storeId)"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.shiftDate"
            type="date"
            placeholder="选择日期"
            style="width: 150px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.shiftType"
            placeholder="班次类型"
            clearable
            style="width: 120px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option label="早班" value="morning" />
            <el-option label="中班" value="noon" />
            <el-option label="晚班" value="evening" />
            <el-option label="全天" value="full" />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option label="待开始" value="pending" />
            <el-option label="进行中" value="ongoing" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-radio-group v-model="viewMode" size="default" class="view-toggle">
            <el-radio-button value="list">
              <el-icon><List /></el-icon>
              <span>列表视图</span>
            </el-radio-button>
            <el-radio-button value="calendar">
              <el-icon><Calendar /></el-icon>
              <span>日历视图</span>
            </el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="default" @click="handleSearch">
            <el-icon :size="14"><Search /></el-icon>查询
          </el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 数据表格区域（列表视图） -->
    <section v-show="viewMode === 'list'" class="table-section">
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
        <!-- 班次编号列 -->
        <template #shiftCode="{ row }">
          <span class="code-text">{{ row.shiftCode }}</span>
        </template>

        <!-- 门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ row.storeName }}</span>
        </template>

        <!-- 班次类型列 -->
        <template #shiftType="{ row }">
          <StatusTag :status="getShiftTypeColor(row.shiftType)" :label="getShiftTypeLabel(row.shiftType)" size="small" variant="light" />
        </template>

        <!-- 开始时间列 -->
        <template #startTime="{ row }">
          <span class="time-text">{{ row.startTime }}</span>
        </template>

        <!-- 结束时间列 -->
        <template #endTime="{ row }">
          <span class="time-text">{{ row.endTime }}</span>
        </template>

        <!-- 负责员工列 -->
        <template #employeeNames="{ row }">
          <div class="employee-list">
            <span v-for="(name, idx) in row.employeeNames" :key="idx" class="employee-tag">
              {{ name }}
            </span>
          </div>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getShiftStatusColor(row.status)" :label="getShiftStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 交接状态列 -->
        <template #handoverStatus="{ row }">
          <StatusTag :status="getHandoverStatusColor(row.handoverStatus)" :label="getHandoverStatusLabel(row.handoverStatus)" size="small" variant="light" />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleHandover(row)">
              交接
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

    <!-- 日历视图（周视图） -->
    <section v-show="viewMode === 'calendar'" class="calendar-section">
      <div class="calendar-toolbar">
        <div class="calendar-toolbar__left">
          <el-button size="small" @click="goToPrevWeek">上一周</el-button>
          <el-button size="small" @click="goToCurrentWeek">本周</el-button>
          <el-button size="small" @click="goToNextWeek">下一周</el-button>
        </div>
        <div class="calendar-toolbar__center">
          <span class="calendar-range">{{ weekDates[0]?.date }} 至 {{ weekDates[6]?.date }}</span>
        </div>
        <div class="calendar-toolbar__right">
          <span class="calendar-hint">共 {{ mockShiftData.length }} 条排班</span>
        </div>
      </div>

      <div class="calendar-grid">
        <div
          v-for="day in weekDates"
          :key="day.date"
          class="calendar-day"
          :class="{ 'calendar-day--weekend': day.isWeekend }"
        >
          <div class="calendar-day__header">
            <span class="calendar-day__weekday">{{ day.weekday }}</span>
            <span class="calendar-day__date">{{ day.date.slice(5) }}</span>
          </div>
          <div class="calendar-day__body">
            <div
              v-for="shift in getShiftsByDate(day.date)"
              :key="shift.shiftId"
              class="calendar-shift"
              :class="`calendar-shift--${shift.shiftType}`"
            >
              <span class="calendar-shift__name">{{ shift.storeName }}</span>
              <StatusTag :status="getShiftTypeColor(shift.shiftType)" :label="getShiftTypeLabel(shift.shiftType)" size="small" />
              <span class="calendar-shift__time">{{ shift.startTime }}-{{ shift.endTime }}</span>
              <span class="calendar-shift__employees">{{ shift.employeeNames.join('、') }}</span>
            </div>
            <div v-if="getShiftsByDate(day.date).length === 0" class="calendar-empty">
              无排班
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 新增/编辑排班对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑排班' : '新增排班'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="门店" prop="storeId">
              <el-select
                v-model="formData.storeId"
                placeholder="请选择门店"
                style="width: 100%"
                :teleported="false"
              >
                <el-option
                  v-for="store in storeOptions"
                  :key="store.storeId"
                  :label="store.storeName"
                  :value="String(store.storeId)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="班次类型" prop="shiftType">
              <el-select
                v-model="formData.shiftType"
                placeholder="请选择班次类型"
                style="width: 100%"
                :teleported="false"
                @change="onShiftTypeChange"
              >
                <el-option label="早班" value="morning" />
                <el-option label="中班" value="noon" />
                <el-option label="晚班" value="evening" />
                <el-option label="全天" value="full" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="日期" prop="shiftDate">
              <el-date-picker
                v-model="formData.shiftDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="formData.startTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="开始时间"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="formData.endTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="结束时间"
                style="width: 100%"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="负责员工" prop="employeeIds">
          <el-select
            v-model="formData.employeeIds"
            multiple
            filterable
            placeholder="请选择负责员工"
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="emp in employeeOptions"
              :key="emp.employeeId"
              :label="emp.employeeName"
              :value="String(emp.employeeId)"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 交接班对话框 -->
    <el-dialog
      v-model="handoverDialogVisible"
      title="交接班"
      width="550px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="handoverFormRef" :model="handoverFormData" :rules="handoverFormRules" label-width="100px">
        <el-form-item label="交接班次">
          <span class="form-value">{{ handoverFormData.shiftCode }}</span>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="交接人" prop="handoverPersonId">
              <el-select
                v-model="handoverFormData.handoverPersonId"
                placeholder="请选择交接人"
                style="width: 100%"
                :teleported="false"
              >
                <el-option
                  v-for="emp in employeeOptions"
                  :key="emp.employeeId"
                  :label="emp.employeeName"
                  :value="String(emp.employeeId)"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="接班人" prop="successorId">
              <el-select
                v-model="handoverFormData.successorId"
                placeholder="请选择接班人"
                style="width: 100%"
                :teleported="false"
              >
                <el-option
                  v-for="emp in employeeOptions"
                  :key="emp.employeeId"
                  :label="emp.employeeName"
                  :value="String(emp.employeeId)"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="交接内容" prop="handoverContent">
          <el-input
            v-model="handoverFormData.handoverContent"
            type="textarea"
            :rows="3"
            placeholder="请填写交接内容，如工作事项、注意事项等"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="营业额交接" prop="revenueAmount">
          <el-input-number
            v-model="handoverFormData.revenueAmount"
            :precision="2"
            :min="0"
            controls-position="right"
            style="width: 100%"
          />
          <span class="form-hint">单位：元</span>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="handoverFormData.remark"
            type="textarea"
            :rows="2"
            placeholder="其他需要说明的事项"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="handoverDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleHandoverSubmit">
          确认交接
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

// 视图切换按钮组
.view-toggle {
  margin-right: var(--fts-space-2);

  :deep(.el-radio-button__inner) {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);

    .el-icon {
      font-size: 14px;
    }
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

// 班次编号
.code-text {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 门店名称
.store-text {
  color: var(--fts-text-primary);
}

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 员工列表
.employee-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.employee-tag {
  display: inline-block;
  padding: 2px 8px;
  background: var(--fts-bg-secondary);
  border-radius: 4px;
  font-size: 12px;
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

// ==================== 日历视图样式 ====================
.calendar-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) 0;
}

.calendar-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;

  &__left {
    display: flex;
    gap: var(--fts-space-2);
  }

  &__center {
    flex: 1;
    text-align: center;
  }

  &__right {
    color: var(--fts-text-tertiary);
    font-size: var(--fts-font-size-sm);
  }
}

.calendar-range {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.calendar-hint {
  font-size: var(--fts-font-size-sm);
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: var(--fts-space-2);
  min-height: 360px;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  padding: var(--fts-space-3);
}

.calendar-day {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-card-radius);
  display: flex;
  flex-direction: column;
  min-height: 340px;
  overflow: hidden;

  &--weekend {
    background: var(--fts-bg-hover);
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: var(--fts-space-2) var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-secondary);
    background: var(--fts-bg-secondary);
  }

  &__weekday {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  &__date {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }

  &__body {
    flex: 1;
    padding: var(--fts-space-2);
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1);
  }
}

.calendar-shift {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: var(--fts-space-2);
  background: var(--fts-bg-secondary);
  border-radius: calc(var(--fts-card-radius) - 2px);
  border-left: 3px solid var(--fts-primary);

  &--morning {
    border-left-color: var(--fts-primary);
  }

  &--noon {
    border-left-color: var(--fts-warning);
  }

  &--evening {
    border-left-color: var(--fts-info);
  }

  &--full {
    border-left-color: var(--fts-success);
  }

  &__name {
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
  }

  &__time {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__employees {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-secondary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.calendar-empty {
  text-align: center;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
  padding: var(--fts-space-3) 0;
}

// 表单辅助样式
.form-value {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.form-hint {
  display: block;
  margin-top: var(--fts-space-1);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

// 响应式
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

  .calendar-grid {
    grid-template-columns: 1fr;
  }

  .calendar-toolbar {
    flex-direction: column;
    gap: var(--fts-space-2);
  }
}
</style>
