<script setup lang="ts">
/**
 * 考勤管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理员工考勤记录和考勤统计
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Calendar, List, DataAnalysis, Edit } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { attendanceApi } from '@/api/hr/attendance'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type {
  AttendanceRecord,
  AttendanceQueryParams,
  AttendanceStatistics,
  AttendanceStatus,
} from '@/types/hr/attendance'
import {
  AttendanceStatusOptions,
  AttendanceStatusTagMap,
} from '@/types/hr/attendance'

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
const activeView = ref<'list' | 'calendar' | 'stats'>('list')
const detailVisible = ref(false)
const currentDetail = ref<AttendanceRecord | null>(null)
const makeupVisible = ref(false)
const currentMakeupRecord = ref<AttendanceRecord | null>(null)
const formRef = ref<FormInstance>()
const statistics = ref<AttendanceStatistics | null>(null)

// 部门选项（接入真实后端 API）
const { departmentOptions } = useDepartmentOptions(true)

// 查询表单
const queryForm = ref({
  keyword: '',
  departmentId: '',
  yearMonth: '',
  status: '' as '' | AttendanceStatus,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<AttendanceRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: AttendanceQueryParams = {
        page: params.page,
        pageSize: params.size,
        keyword: params.keyword || undefined,
        departmentId: params.departmentId || undefined,
        yearMonth: params.yearMonth || undefined,
        status: params.status || undefined,
      }
      return attendanceApi.getList(convertedParams)
    },
  } as unknown as CrudApi<AttendanceRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 补卡申请表单
const makeupForm = reactive({
  attendanceDate: '',
  makeupType: 'clock_in' as 'clock_in' | 'clock_out',
  makeupTime: '',
  reason: '',
})

// 补卡表单校验规则
const makeupFormRules: FormRules = {
  attendanceDate: [{ required: true, message: '请选择补卡日期', trigger: 'change' }],
  makeupType: [{ required: true, message: '请选择补卡类型', trigger: 'change' }],
  makeupTime: [{ required: true, message: '请选择补卡时间', trigger: 'change' }],
  reason: [{ required: true, message: '请输入补卡原因', trigger: 'blur' }],
}

// ==================== 计算属性 ====================

// 统计数据
const statsData = computed(() => ({
  normal: statistics.value?.normal ?? 0,
  late: statistics.value?.late ?? 0,
  earlyLeave: tableData.value.filter(item => item.status === 'early_leave').length,
  absent: statistics.value?.absent ?? 0,
}))

// 表格列定义
const columns = computed<ColumnDef[]>(() => [
  { prop: 'employeeName', label: '员工姓名', minWidth: 100, slot: 'employeeName' },
  { prop: 'employeeCode', label: '工号', minWidth: 100, slot: 'employeeCode' },
  { prop: 'departmentName', label: '部门', minWidth: 100, slot: 'departmentName' },
  { prop: 'attendanceDate', label: '日期', minWidth: 110, slot: 'attendanceDate' },
  { prop: 'clockInTime', label: '上班打卡时间', minWidth: 120, slot: 'clockInTime' },
  { prop: 'clockOutTime', label: '下班打卡时间', minWidth: 120, slot: 'clockOutTime' },
  { prop: 'workHours', label: '工作时长', minWidth: 90, slot: 'workHours', align: 'center' },
  { prop: 'status', label: '考勤状态', minWidth: 90, slot: 'status' },
  { prop: 'abnormalType', label: '异常类型', minWidth: 100, slot: 'abnormalType' },
  { prop: '_operation', label: '操作', width: 180, fixed: 'right', slot: 'operation' },
])

// 日历视图数据（按日期分组）
const calendarData = computed(() => {
  const map = new Map<string, AttendanceRecord[]>()
  tableData.value.forEach(record => {
    const date = record.attendanceDate
    if (!map.has(date)) {
      map.set(date, [])
    }
    map.get(date)!.push(record)
  })
  return map
})

// 统计视图数据（按部门统计出勤率）
const departmentStats = computed(() => {
  const deptMap = new Map<string, { total: number; normal: number; late: number; absent: number; leave: number }>()
  
  tableData.value.forEach(record => {
    const dept = record.departmentName || '未分配'
    if (!deptMap.has(dept)) {
      deptMap.set(dept, { total: 0, normal: 0, late: 0, absent: 0, leave: 0 })
    }
    const stats = deptMap.get(dept)!
    stats.total++
    if (record.status === 'normal') stats.normal++
    else if (record.status === 'late') stats.late++
    else if (record.status === 'absent') stats.absent++
    else if (record.status === 'leave') stats.leave++
  })

  return Array.from(deptMap.entries()).map(([dept, stats]) => ({
    departmentName: dept,
    total: stats.total,
    normal: stats.normal,
    late: stats.late,
    absent: stats.absent,
    leave: stats.leave,
    attendanceRate: stats.total > 0 ? Math.round((stats.normal / stats.total) * 100) : 0,
  }))
})

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.departmentId = ''
  queryForm.value.yearMonth = ''
  queryForm.value.status = ''
  refresh()
}

function handleViewDetail(row: AttendanceRecord) {
  currentDetail.value = { ...row }
  detailVisible.value = true
}

function handleMakeupApply(row: AttendanceRecord) {
  currentMakeupRecord.value = { ...row }
  makeupForm.attendanceDate = row.attendanceDate
  makeupForm.makeupType = 'clock_in'
  makeupForm.makeupTime = ''
  makeupForm.reason = ''
  makeupVisible.value = true
}

async function handleMakeupSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // TODO: 后端补卡申请接口未就绪（POST /v1/attendance/makeup）
  // attendanceApi 暂无 makeup/apply 方法，待后端补充后替换为真实调用：
  //   await attendanceApi.applyMakeup({
  //     employeeId: currentMakeupRecord.value?.employeeId,
  //     attendanceDate: makeupForm.attendanceDate,
  //     makeupType: makeupForm.makeupType,
  //     makeupTime: makeupForm.makeupTime,
  //     reason: makeupForm.reason,
  //   })
  // 当前明确提示功能开发中，不显示假成功
  submitLoading.value = true
  try {
    ElMessage.info('补卡申请功能开发中，请稍后再试')
    makeupVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

// 获取考勤状态标签
function getStatusLabel(status: AttendanceStatus): string {
  return AttendanceStatusOptions.find(s => s.value === status)?.label || status
}

// 获取考勤状态Tag状态
function getStatusTagStatus(status: AttendanceStatus): string {
  return AttendanceStatusTagMap[status] || 'default'
}

// 格式化打卡时间（仅显示时分）
function formatClockTime(time?: string): string {
  if (!time) return '-'
  const parts = time.split(' ')
  if (parts.length < 2) return time
  return parts[1].substring(0, 5)
}

// 计算工作时长
function calcWorkHours(row: AttendanceRecord): string {
  if (!row.clockInTime || !row.clockOutTime) return '-'
  const inTime = new Date(row.clockInTime).getTime()
  const outTime = new Date(row.clockOutTime).getTime()
  if (outTime <= inTime) return '-'
  const hours = (outTime - inTime) / (1000 * 60 * 60)
  return hours.toFixed(1) + 'h'
}

// 获取异常类型
function getAbnormalType(row: AttendanceRecord): string {
  if (row.status === 'normal' || row.status === 'overtime') return '-'
  if (row.status === 'late') return `迟到${row.lateMinutes || 0}分钟`
  if (row.status === 'early_leave') return `早退${row.earlyLeaveMinutes || 0}分钟`
  if (row.status === 'absent') return '缺勤'
  if (row.status === 'leave') return '请假'
  if (row.status === 'miss_clock') return '漏打卡'
  return getStatusLabel(row.status)
}

// 加载统计数据
async function loadStatistics() {
  try {
    statistics.value = await attendanceApi.getStatistics()
  } catch {
    statistics.value = null
  }
}

// 切换视图
function switchView(view: 'list' | 'calendar' | 'stats') {
  activeView.value = view
}

onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="考勤排班" description="管理员工考勤记录和考勤统计">
      <el-button type="primary" size="default" @click="refresh">
        <el-icon :size="16"><Refresh /></el-icon>刷新数据
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="CircleCheck" label="今日出勤" :value="String(statsData.normal)" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="迟到" :value="String(statsData.late)" color-type="warning" variant="bordered" />
      <StatCard icon="Clock" label="早退" :value="String(statsData.earlyLeave)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleClose" label="缺勤" :value="String(statsData.absent)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-date-picker
            v-model="queryForm.yearMonth"
            type="month"
            placeholder="选择月份"
            value-format="YYYY-MM"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.departmentId"
            placeholder="部门选择"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.label"
              :value="dept.id"
            />
          </el-select>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索员工姓名/工号..."
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
          <!-- 视图切换 -->
          <el-radio-group v-model="activeView" size="default" @change="switchView">
            <el-radio-button value="list">
              <el-icon><List /></el-icon>
              <span style="margin-left: 4px">列表视图</span>
            </el-radio-button>
            <el-radio-button value="calendar">
              <el-icon><Calendar /></el-icon>
              <span style="margin-left: 4px">日历视图</span>
            </el-radio-button>
            <el-radio-button value="stats">
              <el-icon><DataAnalysis /></el-icon>
              <span style="margin-left: 4px">统计视图</span>
            </el-radio-button>
          </el-radio-group>
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
        </div>
      </div>
    </div>

    <!-- 列表视图 -->
    <section v-if="activeView === 'list'" class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <!-- 员工姓名列 -->
        <template #employeeName="{ row }">
          <span class="name-text">{{ row.employeeName }}</span>
        </template>

        <!-- 工号列 -->
        <template #employeeCode="{ row }">
          <span class="code-text">{{ row.employeeCode || '-' }}</span>
        </template>

        <!-- 部门列 -->
        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName || '-' }}</span>
        </template>

        <!-- 日期列 -->
        <template #attendanceDate="{ row }">
          <span class="date-text">{{ row.attendanceDate }}</span>
        </template>

        <!-- 上班打卡时间列 -->
        <template #clockInTime="{ row }">
          <span class="time-text">{{ formatClockTime(row.clockInTime) }}</span>
        </template>

        <!-- 下班打卡时间列 -->
        <template #clockOutTime="{ row }">
          <span class="time-text">{{ formatClockTime(row.clockOutTime) }}</span>
        </template>

        <!-- 工作时长列 -->
        <template #workHours="{ row }">
          <span class="hours-text">{{ calcWorkHours(row) }}</span>
        </template>

        <!-- 考勤状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusTagStatus(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 异常类型列 -->
        <template #abnormalType="{ row }">
          <span :class="['abnormal-text', { 'abnormal-normal': row.status === 'normal' }]">
            {{ getAbnormalType(row) }}
          </span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleMakeupApply(row)">
              补卡申请
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

    <!-- 日历视图 -->
    <section v-else-if="activeView === 'calendar'" class="calendar-section">
      <div class="calendar-container">
        <div class="calendar-header">
          <span v-for="day in ['日', '一', '二', '三', '四', '五', '六']" :key="day" class="calendar-weekday">
            {{ day }}
          </span>
        </div>
        <div class="calendar-body">
          <div
            v-for="day in 31"
            :key="day"
            class="calendar-day"
            :class="{ 'calendar-day-empty': day > new Date().getDate() + 10 }"
          >
            <div class="day-number">{{ day }}</div>
            <div v-if="calendarData.get(`2026-06-${String(day).padStart(2, '0')}`)" class="day-records">
              <div
                v-for="record in calendarData.get(`2026-06-${String(day).padStart(2, '0')}`)?.slice(0, 3)"
                :key="record.id"
                class="day-record-item"
              >
                <StatusTag :status="getStatusTagStatus(record.status)" :label="record.employeeName" size="small" variant="light" />
              </div>
              <div v-if="(calendarData.get(`2026-06-${String(day).padStart(2, '0')}`)?.length || 0) > 3" class="day-more">
                +{{ (calendarData.get(`2026-06-${String(day).padStart(2, '0')}`)?.length || 0) - 3 }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 统计视图 -->
    <section v-else-if="activeView === 'stats'" class="stats-view-section">
      <div class="stats-table-wrapper">
        <h4 class="stats-title">部门出勤率统计</h4>
        <el-table :data="departmentStats" stripe style="width: 100%">
          <el-table-column prop="departmentName" label="部门" min-width="120" />
          <el-table-column prop="total" label="总人数" width="100" align="center" />
          <el-table-column prop="normal" label="正常出勤" width="100" align="center" />
          <el-table-column prop="late" label="迟到" width="80" align="center">
            <template #default="{ row }">
              <span class="stat-warning">{{ row.late }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="absent" label="缺勤" width="80" align="center">
            <template #default="{ row }">
              <span class="stat-error">{{ row.absent }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="leave" label="请假" width="80" align="center" />
          <el-table-column prop="attendanceRate" label="出勤率" width="120" align="center">
            <template #default="{ row }">
              <div class="attendance-rate">
                <el-progress
                  :percentage="row.attendanceRate"
                  :stroke-width="8"
                  :color="row.attendanceRate >= 90 ? 'var(--fts-success)' : row.attendanceRate >= 70 ? 'var(--fts-warning)' : 'var(--fts-error)'"
                  :show-text="false"
                  style="width: 80px; margin-right: 8px"
                />
                <span>{{ row.attendanceRate }}%</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="考勤详情"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="工号">{{ currentDetail.employeeCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="部门">{{ currentDetail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="职位">{{ currentDetail.positionName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="考勤日期">{{ currentDetail.attendanceDate }}</el-descriptions-item>
          <el-descriptions-item label="考勤状态">
            <StatusTag :status="getStatusTagStatus(currentDetail.status)" :label="getStatusLabel(currentDetail.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="上班打卡">{{ formatClockTime(currentDetail.clockInTime) }}</el-descriptions-item>
          <el-descriptions-item label="下班打卡">{{ formatClockTime(currentDetail.clockOutTime) }}</el-descriptions-item>
          <el-descriptions-item label="工作时长">{{ calcWorkHours(currentDetail) }}</el-descriptions-item>
          <el-descriptions-item label="迟到(分钟)">{{ currentDetail.lateMinutes ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="早退(分钟)">{{ currentDetail.earlyLeaveMinutes ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="加班(小时)">{{ currentDetail.overtimeHours ?? '-' }}</el-descriptions-item>
          <el-descriptions-item label="异常类型" :span="2">{{ getAbnormalType(currentDetail) }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ currentDetail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleMakeupApply(currentDetail!)">补卡申请</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 补卡申请对话框 -->
    <el-dialog
      v-model="makeupVisible"
      title="补卡申请"
      width="680px"
      class="fts-dialog--md"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="makeupForm" :rules="makeupFormRules" label-width="100px">
        <el-form-item label="补卡日期" prop="attendanceDate">
          <el-date-picker
            v-model="makeupForm.attendanceDate"
            type="date"
            placeholder="请选择补卡日期"
            value-format="YYYY-MM-DD"
            :teleported="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="补卡类型" prop="makeupType">
          <el-select v-model="makeupForm.makeupType" placeholder="请选择补卡类型" :teleported="false" style="width: 100%">
            <el-option label="上班打卡" value="clock_in" />
            <el-option label="下班打卡" value="clock_out" />
          </el-select>
        </el-form-item>
        <el-form-item label="补卡时间" prop="makeupTime">
          <el-time-picker
            v-model="makeupForm.makeupTime"
            placeholder="请选择补卡时间"
            value-format="HH:mm:ss"
            :teleported="false"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="补卡原因" prop="reason">
          <el-input
            v-model="makeupForm.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入补卡原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="makeupVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleMakeupSubmit">
            提交申请
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

// 日历视图
.calendar-section {
  padding: var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
}

.calendar-container {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  overflow: hidden;
}

.calendar-header {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  background: var(--fts-bg-secondary);
  border-bottom: 1px solid var(--fts-border-primary);
}

.calendar-weekday {
  padding: var(--fts-space-3);
  text-align: center;
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
}

.calendar-body {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
}

.calendar-day {
  min-height: 100px;
  padding: var(--fts-space-2);
  border-right: 1px solid var(--fts-border-primary);
  border-bottom: 1px solid var(--fts-border-primary);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &:nth-child(7n) {
    border-right: none;
  }

  &.calendar-day-empty {
    background: var(--fts-bg-secondary);
    opacity: 0.5;
  }
}

.day-number {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.day-records {
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow: hidden;
}

.day-record-item {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.day-more {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  text-align: center;
}

// 统计视图
.stats-view-section {
  padding: var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
}

.stats-table-wrapper {
  .stats-title {
    margin: 0 0 var(--fts-space-4);
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }
}

.attendance-rate {
  display: flex;
  align-items: center;
  justify-content: center;
  font-variant-numeric: tabular-nums;
}

.stat-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
}

.stat-error {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
}

// 文字样式
.name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.code-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.dept-text {
  color: var(--fts-text-primary);
}

.date-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.time-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.hours-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
  font-weight: var(--fts-font-weight-medium);
}

.abnormal-text {
  color: var(--fts-warning);
  font-size: var(--fts-font-size-sm);

  &.abnormal-normal {
    color: var(--fts-success);
  }
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

  .calendar-section,
  .stats-view-section {
    padding: var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }
}
</style>
