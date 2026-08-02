<script setup lang="ts">
/**
 * 员工管理页面
 *
 * 【重构说明】
 * - 移除全部 Mock 数据（原 15 条硬编码员工记录）
 * - 全部对接真实后端 EmployeeController（/v1/employees）
 * - 列表/统计/新增/编辑/删除全部走真实 API
 * - 部门下拉使用 useDepartmentOptions 接入真实 API
 *
 * 【字段映射】后端 Employee ↔ 本地 Employee
 * - name ↔ name
 * - employeeCode ↔ employeeNo
 * - departmentName ↔ department
 * - positionName ↔ position
 * - hireDate ↔ entryDate
 * - baseSalary ↔ salary
 * - emergencyContact ↔ emergencyContact
 * - employmentType ↔ employmentType
 * - status ↔ status
 * - id (string) ↔ id (string)
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { useStatCardFilter } from '@/composables/useStatCardFilter'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { employeeApi } from '@/api/hr'
import type { Employee as BackendEmployee, EmployeeStatistics } from '@/types/hr'
import EmployeeFormDialog from './components/EmployeeFormDialog.vue'
import EmployeeDetailDialog from './components/EmployeeDetailDialog.vue'
import EmployeeTransferDialog from './components/EmployeeTransferDialog.vue'

const { pagination } = useStandardPage()

/** 处理分页大小变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
}

/** 处理当前页变化 */
function handleCurrentChange(page: number): void {
  pagination.current = page
}

/**
 * 本地员工接口（兼容 EmployeeFormDialog / EmployeeDetailDialog）
 * 字段命名沿用旧 Mock 结构，通过 fromBackend 转换器与后端对接
 */
interface Employee {
  id: string
  name: string
  employeeNo: string
  gender?: string
  department: string
  departmentId?: string
  position: string
  positionId?: string
  phone: string
  email: string
  entryDate: string
  birthday: string
  education: string
  emergencyContact: string
  status: string
  salary?: number
  employmentType?: string
  lifecycleStatus?: string
  retirementDate?: string
  reemploymentDate?: string
  agreementNo?: string
  workLocationType?: string
  storeId?: string
  warehouseId?: string
  workLocationName?: string
}

const searchForm = ref({ department: '', departmentId: '', status: '', employmentType: '', keyword: '' })
const loading = ref(false)
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const editingEmployee = ref<Employee | null>(null)
const currentEmployee = ref<Employee | null>(null)
const rehireDialogVisible = ref(false)
const transferDialogVisible = ref(false)
const transferEmployee = ref<Employee | null>(null)

/** 全量员工列表（从后端加载） */
const employees = ref<Employee[]>([])

/** 当前筛选后的记录（前端过滤） */
const filteredRecords = ref<Employee[]>([])

/* ===== 部门下拉（接入真实 API） ===== */
const { departmentOptions } = useDepartmentOptions(true)

/* ===== 全局统计数据（独立于列表筛选条件，反映全量员工状态） ===== */
const globalStats = ref<EmployeeStatistics | null>(null)

/** 加载全局统计数据 */
async function loadGlobalStats(): Promise<void> {
  try {
    globalStats.value = await employeeApi.getStatistics()
  } catch {
    // 静默失败，统计卡片显示 0
  }
}

/** 统计卡片数据（基于全局 API 统计，不受列表筛选影响） */
const statistics = computed(() => {
  const s = globalStats.value
  return [
    { key: 'active', icon: 'User', label: '在职员工', value: s?.active ?? 0, colorType: 'primary' as const, filterField: 'status', filterValue: 'active' },
    { key: 'newThisMonth', icon: 'TrendCharts', label: '本月新增', value: s?.newThisMonth ?? 0, colorType: 'success' as const },
    { key: 'probation', icon: 'Clock', label: '试用期', value: s?.probation ?? 0, colorType: 'warning' as const, filterField: 'status', filterValue: 'probation' },
    { key: 'inactive', icon: 'Switch', label: '已离职', value: s?.inactive ?? 0, colorType: 'error' as const, filterField: 'status', filterValue: 'inactive' },
    { key: 'overAge', icon: 'Clock', label: '超龄返聘', value: s?.overAge ?? 0, colorType: 'info' as const, filterField: 'employmentType', filterValue: 'over_age' },
  ]
})

/* ===== 统计卡片点击筛选（Toggle取消 + 多卡片互斥） ===== */
const { activeStatKey, handleStatClick, clearActiveStat } = useStatCardFilter({
  searchForm,
  onSearch: handleSearch,
})

const columns = [
  { prop: 'employeeNo', label: '工号', minWidth: 90 },
  { prop: 'name', label: '姓名', minWidth: 85 },
  { prop: 'department', label: '部门', minWidth: 100, slot: 'department' },
  { prop: 'position', label: '岗位', minWidth: 110 },
  { prop: 'workLocationName', label: '工作归属', minWidth: 110, slot: 'workLocation' },
  { prop: 'employmentType', label: '用工类型', minWidth: 100, slot: 'employmentType' },
  { prop: 'phone', label: '手机号', minWidth: 120 },
  { prop: 'entryDate', label: '入职日期', minWidth: 110 },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' },
]

function getEmploymentTypeLabel(type: string): string {
  const map: Record<string, string> = { full_time: '全职', part_time: '兼职', intern: '实习生', dispatch: '劳务派遣', over_age: '超龄返聘', 'full-time': '全职', 'part-time': '兼职' }
  return map[type] || '全职'
}

/** 员工状态映射：后端 1/0/2 → 前端语义字符串 */
const employeeStatusMap: Record<number | string, string> = { 1: 'active', 0: 'inactive', 2: 'probation' }

function toFrontendStatus(value: unknown): string {
  const n = Number(value)
  return employeeStatusMap[n] || String(value ?? 'active')
}

function toBackendStatus(status: string): number {
  const map: Record<string, number> = { active: 1, inactive: 0, probation: 2 }
  return map[status] ?? 1
}

/**
 * 后端 Employee → 本地 Employee 转换器
 * 处理字段名差异，保证与 EmployeeFormDialog / EmployeeDetailDialog 兼容
 */
function fromBackendEmployee(raw: BackendEmployee): Employee {
  const rawAny = raw as Record<string, unknown>
  const workLocationType = String(rawAny.workLocationType ?? 'HEADQUARTERS')
  const storeId = rawAny.storeId != null ? String(rawAny.storeId) : undefined
  const warehouseId = rawAny.warehouseId != null ? String(rawAny.warehouseId) : undefined
  let workLocationName = '总部'
  if (workLocationType === 'STORE') workLocationName = storeId ? `门店 ${storeId}` : '门店'
  if (workLocationType === 'WAREHOUSE') workLocationName = warehouseId ? `仓库 ${warehouseId}` : '仓库'
  return {
    id: String(raw.id ?? rawAny.employeeId ?? ''),
    name: String(rawAny.name ?? raw.employeeName ?? ''),
    employeeNo: String(raw.employeeCode ?? rawAny.employeeNo ?? ''),
    gender: String(rawAny.gender ?? 'other'),
    department: String(rawAny.departmentName ?? raw.departmentName ?? ''),
    departmentId: raw.departmentId,
    position: String(rawAny.positionName ?? raw.positionName ?? ''),
    positionId: raw.positionId != null ? String(raw.positionId) : undefined,
    phone: String(raw.phone ?? ''),
    email: String(raw.email ?? ''),
    entryDate: formatDate(rawAny.hireDate ?? rawAny.entryDate),
    birthday: String(rawAny.birthday ?? ''),
    education: String(rawAny.education ?? ''),
    emergencyContact: String(rawAny.emergencyContact ?? rawAny.emergencyPhone ?? ''),
    status: toFrontendStatus(raw.status),
    salary: toYuan(rawAny.baseSalary ?? rawAny.salary),
    employmentType: normalizeEmploymentType(rawAny.employmentType),
    lifecycleStatus: String(rawAny.lifecycleStatus ?? ''),
    retirementDate: formatDate(rawAny.retirementDate),
    reemploymentDate: formatDate(rawAny.reemploymentDate),
    agreementNo: String(rawAny.agreementNo ?? ''),
    workLocationType,
    storeId,
    warehouseId,
    workLocationName,
  }
}

/** 格式化日期字符串（支持 LocalDateTime / LocalDate / 字符串） */
function formatDate(value: unknown): string {
  if (!value) return ''
  if (typeof value === 'string') {
    // 兼容 ISO 8601（T 分隔）与后端空格分隔的 LocalDateTime
    return value.split(/[T ]/)[0]
  }
  return String(value)
}

/** 薪分 → 元 */
function toYuan(value: unknown): number | undefined {
  if (value == null) return undefined
  const num = typeof value === 'number' ? value : Number(value)
  return isNaN(num) ? undefined : num
}

/** 统一用工类型（兼容 full-time / full_time 两种写法） */
function normalizeEmploymentType(value: unknown): string | undefined {
  if (!value) return undefined
  const v = String(value)
  return v.replace('-', '_')
}

/** 批量转换 */
function fromBackendEmployeeList(list: BackendEmployee[] | null | undefined): Employee[] {
  return (list || []).map(fromBackendEmployee)
}

/**
 * 本地 Employee → 后端创建/更新 payload
 * 字段映射遵循 EmployeeCreateDTO / EmployeeUpdateDTO 的字段命名
 */
function toBackendPayload(local: Employee): Record<string, unknown> {
  return {
    name: local.name,
    gender: local.gender || 'other',
    employeeCode: local.employeeNo,
    departmentId: local.departmentId,
    positionId: local.positionId,
    phone: local.phone,
    email: local.email,
    hireDate: local.entryDate ? `${local.entryDate.split(' ')[0]} 00:00:00` : undefined,
    status: toBackendStatus(local.status),
    baseSalary: local.salary,
    employmentType: local.employmentType,
    emergencyContact: local.emergencyContact,
    workLocationType: local.workLocationType || 'HEADQUARTERS',
    storeId: local.workLocationType === 'STORE' ? local.storeId : undefined,
    warehouseId: local.workLocationType === 'WAREHOUSE' ? local.warehouseId : undefined,
  }
}

/** 加载员工列表（全量，前端分页+过滤） */
async function loadList(): Promise<void> {
  loading.value = true
  try {
    const list = await employeeApi.getEmployees()
    employees.value = fromBackendEmployeeList(list)
    filteredRecords.value = [...employees.value]
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载员工列表失败')
    employees.value = []
    filteredRecords.value = []
  } finally {
    loading.value = false
  }
}

/** 前端过滤（基于已加载的全量数据） */
function applyFilter(): void {
  let r = [...employees.value]
  if (searchForm.value.departmentId) {
    r = r.filter(e => e.departmentId === searchForm.value.departmentId)
  } else if (searchForm.value.department) {
    r = r.filter(e => e.department === searchForm.value.department)
  }
  if (searchForm.value.status) r = r.filter(e => e.status === searchForm.value.status)
  if (searchForm.value.employmentType) {
    r = r.filter(e => e.employmentType === searchForm.value.employmentType)
  }
  if (searchForm.value.keyword) {
    const kw = searchForm.value.keyword.toLowerCase()
    r = r.filter(e => e.name.toLowerCase().includes(kw) || e.employeeNo.toLowerCase().includes(kw))
  }
  filteredRecords.value = r
  pagination.total = r.length
  pagination.current = 1
}

function handleSearch() {
  applyFilter()
}

function handleReset() {
  searchForm.value = { department: '', departmentId: '', status: '', employmentType: '', keyword: '' }
  clearActiveStat()
  filteredRecords.value = [...employees.value]
  pagination.total = filteredRecords.value.length
  pagination.current = 1
}

function handleView(row: Employee) {
  currentEmployee.value = row
  detailDialogVisible.value = true
}

function handleRowDblclick(row: Employee) {
  handleView(row)
}

function handleOpenAdd() {
  editingEmployee.value = null
  dialogVisible.value = true
}

function handleEdit(row: Employee) {
  editingEmployee.value = { ...row }
  dialogVisible.value = true
}

async function handleDelete(row: Employee) {
  try {
    await ElMessageBox.confirm(`确定要删除员工「${row.name}」吗？此操作不可恢复。`, '确认删除', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await employeeApi.deleteEmployee(row.id)
    ElMessage.success(`已删除: ${row.name}`)
    // 删除后重新加载列表和统计
    await Promise.all([loadList(), loadGlobalStats()])
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

function handleRehire(row: Employee) {
  currentEmployee.value = row
  rehireDialogVisible.value = true
}

function handleTransfer(row: Employee) {
  transferEmployee.value = row
  transferDialogVisible.value = true
}

function handleTransferSuccess() {
  transferDialogVisible.value = false
  transferEmployee.value = null
  Promise.all([loadList(), loadGlobalStats()])
}

/**
 * 表单提交（新增/编辑）
 * 对接真实后端 createEmployee / updateEmployee API
 */
async function handleFormSubmit(data: Omit<Employee, 'id'> & { id?: string | number }) {
  try {
    const payload = toBackendPayload(data as Employee)
    if (editingEmployee.value && editingEmployee.value.id) {
      // 编辑模式
      await employeeApi.updateEmployee(editingEmployee.value.id, payload)
      ElMessage.success('编辑成功')
    } else {
      // 新增模式
      await employeeApi.createEmployee(payload as Omit<BackendEmployee, 'id'>)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    // 提交后重新加载列表和统计
    await Promise.all([loadList(), loadGlobalStats()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  }
}

function handleDialogClose() {
  editingEmployee.value = null
}

/* ===== 初始化：并行加载列表数据和全局统计 ===== */
onMounted(() => {
  Promise.all([loadList(), loadGlobalStats()])
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="员工管理" description="员工信息管理与人事档案维护">
      <el-button type="primary" size="default" @click="handleOpenAdd">
        <el-icon :size="16"><Plus /></el-icon>新增员工
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard
        v-for="stat in statistics"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="stat.value"
        :color-type="stat.colorType"
        :class="[
          stat.filterValue ? 'stat-clickable' : '',
          { 'stat-active': activeStatKey === stat.key },
        ]"
        variant="bordered"
        @click="stat.filterValue ? handleStatClick(stat.key, stat.filterField, stat.filterValue) : undefined"
      />
    </section>

    <div class="search-bar">
      <div class="toolbar-left">
        <el-select
          v-model="searchForm.departmentId"
          placeholder="选择部门"
          clearable
          filterable
          style="width:160px"
          size="default"
          :teleported="false"
          @change="handleSearch"
        >
          <el-option
            v-for="dept in departmentOptions"
            :key="dept.id"
            :label="dept.label"
            :value="dept.id"
          />
        </el-select>
        <el-select v-model="searchForm.status" placeholder="员工状态" clearable style="width:130px" size="default" @change="handleSearch">
          <el-option label="在职" value="active" />
          <el-option label="试用期" value="probation" />
          <el-option label="超龄返聘" value="over_age_employed" />
          <el-option label="已离职" value="resigned" />
          <el-option label="已退休" value="retired" />
        </el-select>
        <el-select v-model="searchForm.employmentType" placeholder="用工类型" clearable style="width:130px" size="default" :teleported="false" @change="handleSearch">
          <el-option label="全职" value="full_time" />
          <el-option label="兼职" value="part_time" />
          <el-option label="实习生" value="intern" />
          <el-option label="劳务派遣" value="dispatch" />
          <el-option label="超龄返聘" value="over_age" />
        </el-select>
        <el-input v-model="searchForm.keyword" placeholder="搜索姓名/工号" clearable style="width:200px" size="default" :prefix-icon="Search" @keyup.enter="handleSearch" @clear="handleSearch" />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
        <el-button size="default" @click="handleReset">重置</el-button>
      </div>
    </div>

    <div class="table-section">
      <DataTable :columns="columns" :data="filteredRecords" :loading="loading" :actions-width="300" stripe @row-dblclick="handleRowDblclick">
        <template #department="{ row }">
          <StatusTag status="active" :label="row.department || '-'" size="small" />
        </template>
        <template #workLocation="{ row }">
          <StatusTag v-if="row.workLocationType" :status="row.workLocationType === 'STORE' ? 'success' : row.workLocationType === 'WAREHOUSE' ? 'warning' : 'info'" :label="row.workLocationName || '总部'" size="small" />
          <span v-else class="text-secondary">总部</span>
        </template>
        <template #employmentType="{ row }">
          <StatusTag v-if="row.employmentType" :status="row.employmentType === 'over_age' ? 'warning' : 'info'" :label="getEmploymentTypeLabel(row.employmentType)" size="small" />
          <span v-else class="text-secondary">全职</span>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="handleTransfer(row)">人事变动</el-button>
          <el-button v-if="row.lifecycleStatus === 'retired'" link type="warning" size="small" @click="handleRehire(row)">返聘登记</el-button>
          <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.size"
        :total="filteredRecords.length"
        layout="total,prev,pager,next,jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

    <!-- 员工详情对话框（共用组件） -->
    <EmployeeDetailDialog
      v-model:visible="detailDialogVisible"
      :employee="currentEmployee"
    />

    <EmployeeFormDialog
      v-model="dialogVisible"
      :edit-data="editingEmployee"
      @submit="handleFormSubmit"
      @close="handleDialogClose"
    />

    <EmployeeTransferDialog
      v-model="transferDialogVisible"
      :employee="transferEmployee"
      @success="handleTransferSuccess"
    />

    <!-- 返聘登记对话框 -->
    <el-dialog
      v-model="rehireDialogVisible"
      title="返聘登记"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      :destroy-on-close="true"
      :lock-scroll="false"
    >
      <el-form label-width="100px" label-position="right">
        <el-form-item label="退休日期">
          <el-date-picker v-model="currentEmployee!.retirementDate" type="date" placeholder="请选择退休日期" value-format="YYYY-MM-DD" class="form-date-picker" :teleported="false" />
        </el-form-item>
        <el-form-item label="返聘日期">
          <el-date-picker v-model="currentEmployee!.reemploymentDate" type="date" placeholder="请选择返聘日期" value-format="YYYY-MM-DD" class="form-date-picker" :teleported="false" />
        </el-form-item>
        <el-form-item label="劳务协议编号">
          <el-input v-model="currentEmployee!.agreementNo" placeholder="请输入劳务协议编号" clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="rehireDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="rehireDialogVisible = false">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: var(--fts-space-4);
}

/* 统计卡片可点击样式 */
.stat-clickable {
  cursor: pointer;
  transition: transform var(--fts-transition-base), box-shadow var(--fts-transition-base);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--fts-shadow-md);
  }
}

/* 统计卡片激活状态 */
.stat-active {
  outline: 2px solid var(--fts-primary);
  outline-offset: -2px;
}

.search-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }
}

.form-date-picker {
  width: 100%;
}
</style>
