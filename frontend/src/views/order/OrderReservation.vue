<script setup lang="ts">
/**
 * 预约管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理顾客预约订单，包括到店预约和座位预约
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Calendar, User, Phone } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import ReservationDetailDrawer from './components/ReservationDetailDrawer.vue'
import { useLayoutStore } from '@/stores/layout'
import { reservationApi } from '@/api/order'
import type { ReservationCreateForm } from '@/api/order/reservation'
import { tableApi } from '@/api/store-ops/table'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { Reservation, ReservationStatusValue, ReservationQueryForm } from '@/types/order'

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

interface TableOption {
  tableId: number
  tableName: string
  tableCode: string
  seatsCount: number
  storeId: number
}

interface ReservationDetailRow {
  id: string
  resNo: string
  storeName: string
  customerName: string
  phone: string
  resTime: string
  guestCount: number
  tableNo: string
  status: string
  createdBy: string
  /** 确认时间 */
  confirmedBy: string
  /** 备注/特殊要求 */
  notes: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const currentReservationId = ref<number | null>(null)

const detailDrawerVisible = ref(false)
const currentDetail = ref<ReservationDetailRow | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  status: '' as '' | ReservationStatusValue,
  dateRange: [] as string[],
  storeId: '' as number | string,
})

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<Reservation, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: ReservationQueryForm = {
        page: params.page,
        size: params.size,
        status: params.status ? (params.status as ReservationStatusValue) : undefined,
      }
      if (params.keyword) {
        queryParams.keyword = params.keyword
      }
      if (params.dateRange && params.dateRange.length === 2) {
        queryParams.date = params.dateRange[0]
        queryParams.dateEnd = params.dateRange[1]
      }
      return reservationApi.getList(queryParams)
    },
  } as unknown as CrudApi<Reservation, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 预约统计
const statistics = ref({
  todayReservations: 0,
  pendingConfirm: 0,
  confirmed: 0,
  cancelled: 0,
  arrived: 0,
  cancelRate: 0,
})

// 桌台选项
const tableOptions = ref<TableOption[]>([])
const tableOptionsLoading = ref(false)

// 门店选项（从真实 API 加载，使用共享 composable）
const { storeOptions, loading: storeOptionsLoading, getStoreName } = useStoreOptions(true)

// ==================== 表单数据 ====================

const formData = reactive<Partial<ReservationCreateForm> & { tableName?: string }>({
  customerName: '',
  customerPhone: '',
  tableId: 0,
  storeId: undefined,
  reservationDate: '',
  reservationTime: '',
  peopleCount: 2,
  depositAmount: '0',
  remark: '',
})

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  customerName: [
    { required: true, message: '请输入预约人姓名', trigger: 'blur' },
    { min: 1, max: 50, message: '姓名长度 1-50 字符', trigger: 'blur' },
  ],
  customerPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号', trigger: 'blur' },
  ],
  tableId: [
    { required: true, message: '请选择桌台', trigger: 'change' },
  ],
  reservationDate: [
    { required: true, message: '请选择预约日期', trigger: 'change' },
  ],
  reservationTime: [
    { required: true, message: '请选择预约时间', trigger: 'change' },
  ],
  peopleCount: [
    { required: true, message: '请输入用餐人数', trigger: 'blur' },
    { type: 'number', min: 1, max: 50, message: '人数范围 1-50', trigger: 'blur' },
  ],
}

// ==================== 计算属性 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'reservationCode', label: '预约编号', minWidth: 160, slot: 'reservationCode' },
  { prop: 'customerName', label: '预约人', minWidth: 100, slot: 'customerName' },
  { prop: 'customerPhone', label: '联系电话', minWidth: 120, slot: 'customerPhone' },
  { prop: 'reservationDate', label: '预约日期', minWidth: 110, slot: 'reservationDate' },
  { prop: 'reservationTime', label: '预约时间', minWidth: 100, slot: 'reservationTime' },
  { prop: 'peopleCount', label: '用餐人数', width: 90, slot: 'peopleCount' },
  { prop: 'tableId', label: '桌号/包间', minWidth: 120, slot: 'tableInfo' },
  { prop: 'status', label: '预约状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'storeName', label: '门店', minWidth: 100, slot: 'storeName' },
  { prop: '_operation', label: '操作', width: 100, fixed: 'right', slot: 'operation' },
])

// 表格数据（keyword已由后端过滤，前端的filteredTableData已移除）

// ==================== 方法 ====================

/** 加载预约统计（直接从后端获取，无需额外查询） */
async function loadStats(): Promise<void> {
  try {
    const res = await reservationApi.getStats()
    statistics.value = {
      todayReservations: res.todayReservations ?? 0,
      pendingConfirm: res.pendingConfirm ?? 0,
      confirmed: res.confirmed ?? 0,
      cancelled: res.cancelled ?? 0,
      arrived: res.arrived ?? 0,
      cancelRate: res.cancelRate ?? 0,
    }
  } catch {
    statistics.value = { todayReservations: 0, pendingConfirm: 0, confirmed: 0, cancelled: 0, arrived: 0, cancelRate: 0 }
  }
}

/** 加载桌台选项 */
async function loadTableOptions(): Promise<void> {
  tableOptionsLoading.value = true
  try {
    const res = await tableApi.getList({ page: 1, size: 100 })
    tableOptions.value = (res?.records || []).map((t) => ({
      tableId: t.tableId,
      tableName: t.tableName,
      tableCode: t.tableCode,
      seatsCount: t.seatsCount,
      storeId: t.storeId ?? 0,
    }))
  } catch {
    tableOptions.value = []
  } finally {
    tableOptionsLoading.value = false
  }
}

/** 构建默认表单数据 */
function buildDefaultFormData(): void {
  const tomorrow = new Date()
  tomorrow.setDate(tomorrow.getDate() + 1)
  const yyyy = tomorrow.getFullYear()
  const mm = String(tomorrow.getMonth() + 1).padStart(2, '0')
  const dd = String(tomorrow.getDate()).padStart(2, '0')

  Object.assign(formData, {
    customerName: '',
    customerPhone: '',
    tableId: 0,
    storeId: undefined,
    reservationDate: `${yyyy}-${mm}-${dd}`,
    reservationTime: '18:00:00',
    peopleCount: 2,
    depositAmount: '0',
    remark: '',
  })
}

/** 搜索 */
function handleSearch(): void {
  refresh()
}

/** 重置 */
function handleReset(): void {
  queryForm.value.keyword = ''
  queryForm.value.status = ''
  queryForm.value.dateRange = []
  queryForm.value.storeId = ''
  refresh()
}

/** 新增预约 */
function handleCreate(): void {
  isEdit.value = false
  currentReservationId.value = null
  buildDefaultFormData()
  loadTableOptions()
  dialogVisible.value = true
}

/** 编辑预约 */
async function handleEdit(row: Reservation): Promise<void> {
  isEdit.value = true
  currentReservationId.value = row.reservationId
  try {
    const detail = await reservationApi.getById(row.reservationId)
    await loadTableOptions()
    // 根据桌台ID获取所属门店ID
    const table = tableOptions.value.find((t) => t.tableId === detail.tableId)
    Object.assign(formData, {
      customerName: detail.customerName,
      customerPhone: detail.customerPhone,
      tableId: detail.tableId,
      storeId: detail.storeId ?? table?.storeId ?? undefined,
      reservationDate: detail.reservationDate,
      reservationTime: detail.reservationTime,
      peopleCount: detail.peopleCount,
      depositAmount: detail.depositAmount,
      remark: '',
    })
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载预约详情失败')
  }
}

/** 确认预约 */
async function handleConfirm(row: Reservation): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要确认预约「${row.reservationCode}」吗？`,
      '确认预约',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await reservationApi.confirm(row.reservationId)
    ElMessage.success('预约已确认')
    refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '确认预约失败')
    }
  }
}

/** 标记到店 */
async function handleArrive(row: Reservation): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要将预约「${row.reservationCode}」标记为已到店吗？`,
      '标记到店',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    await reservationApi.arrive(row.reservationId)
    ElMessage.success('已标记到店')
    refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '标记到店失败')
    }
  }
}

/** 取消预约（支持输入取消原因） */
async function handleCancel(row: Reservation): Promise<void> {
  try {
    const { value: reason } = await ElMessageBox.prompt(
      `确定要取消预约「${row.reservationCode}」吗？`,
      '取消预约',
      {
        confirmButtonText: '确定取消',
        cancelButtonText: '返回',
        type: 'warning',
        inputPlaceholder: '请输入取消原因（可选）',
        inputType: 'textarea',
      }
    )
    await reservationApi.cancel(row.reservationId, reason || '')
    ElMessage.success('预约已取消')
    refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '取消预约失败')
    }
  }
}

/** 查看详情（从API获取完整数据） */
async function handleViewDetail(row: Reservation): Promise<void> {
  try {
    const detail = await reservationApi.getById(row.reservationId)
    const tableName = tableOptions.value.find((t) => t.tableId === detail.tableId)?.tableName || ''
    const resTime = `${detail.reservationDate || ''} ${detail.reservationTime || ''}`.trim()
    // 优先使用 API 返回的 storeId，其次从桌台关联获取
    const storeName = detail.storeId
      ? getStoreName(detail.storeId)
      : getStoreNameByTableId(detail.tableId)
    currentDetail.value = {
      id: String(detail.reservationId),
      resNo: detail.reservationCode,
      storeName: storeName,
      customerName: detail.customerName || '',
      phone: detail.customerPhone || '',
      resTime,
      guestCount: detail.peopleCount,
      tableNo: tableName,
      status: detail.status,
      createdBy: '',
      confirmedBy: detail.confirmTime || '',
      notes: detail.remark || '',
    }
    detailDrawerVisible.value = true
  } catch {
    ElMessage.error('获取预约详情失败')
  }
}

/** 提交表单 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: ReservationCreateForm = {
      customerName: formData.customerName || '',
      customerPhone: formData.customerPhone || '',
      tableId: Number(formData.tableId) || 0,
      storeId: formData.storeId ? Number(formData.storeId) : undefined,
      reservationDate: formData.reservationDate || '',
      reservationTime: formData.reservationTime || '',
      peopleCount: Number(formData.peopleCount) || 0,
      depositAmount: formData.depositAmount || '0',
      remark: formData.remark,
    }

    if (isEdit.value && currentReservationId.value) {
      await reservationApi.update(currentReservationId.value, submitData)
      ElMessage.success('预约更新成功')
    } else {
      await reservationApi.create(submitData)
      ElMessage.success('预约创建成功')
    }

    dialogVisible.value = false
    refresh()
    loadStats()
  } catch (error: unknown) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/** 导出预约列表为CSV */
async function handleExport(): Promise<void> {
  try {
    const allData = tableData.value
    if (allData.length === 0) {
      ElMessage.warning('暂无可导出的数据')
      return
    }
    // 构建CSV内容
    const headers = ['预约编号', '预约人', '联系电话', '预约日期', '预约时间', '用餐人数', '桌号', '状态', '门店']
    const rows = allData.map((r) => [
      r.reservationCode,
      r.customerName,
      r.customerPhone,
      r.reservationDate,
      r.reservationTime?.slice(0, 5),
      String(r.peopleCount),
      getTableInfo(r.tableId),
      getStatusLabel(r.status),
      getStoreNameByTableId(r.tableId),
    ])
    const csvContent = [headers.join(','), ...rows.map((row) => row.join(','))].join('\n')
    const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `预约列表_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// ==================== 状态映射 ====================

function getStatusType(status: string): string {
  const map: Record<string, string> = {
    pending: 'warning',
    confirmed: 'primary',
    arrived: 'success',
    cancelled: 'info',
    no_show: 'error',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    pending: '待确认',
    confirmed: '已确认',
    arrived: '已到店',
    cancelled: '已取消',
    no_show: '未到店',
  }
  return map[status] || status
}

function getTableInfo(tableId: number): string {
  const table = tableOptions.value.find((t) => t.tableId === tableId)
  return table ? `${table.tableName}（${table.seatsCount}人座）` : '-'
}

/** 通过桌台ID获取门店名称 */
function getStoreNameByTableId(tableId: number): string {
  const table = tableOptions.value.find((t) => t.tableId === tableId)
  if (!table) return '-'
  return getStoreName(table.storeId)
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadStats()
  loadTableOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="预约管理" description="查证各门店预约数据，支持搜索、筛选和导出">
      <el-button type="success" size="default" @click="handleExport">
        <el-icon :size="14"><Download /></el-icon>导出
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Calendar" label="今日预约" :value="String(statistics.todayReservations)" color-type="primary" variant="bordered" />
      <StatCard icon="Clock" label="待确认" :value="String(statistics.pendingConfirm)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="已确认" :value="String(statistics.confirmed)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已取消" :value="String(statistics.cancelled)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已到店" :value="String(statistics.arrived)" color-type="primary" variant="bordered" />
      <StatCard icon="WarningFilled" label="取消率" :value="`${statistics.cancelRate}%`" color-type="error" variant="bordered" />
    </section>

    <!-- 搜索工具栏 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="预约人/手机号搜索"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="预约状态"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option label="待确认" value="pending" />
            <el-option label="已确认" value="confirmed" />
            <el-option label="已到店" value="arrived" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="未到店" value="no_show" />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            @change="handleSearch"
          />
          <el-select
            v-model="queryForm.storeId"
            placeholder="门店选择"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="store in storeOptions"
              :key="store.storeId"
              :label="store.storeName"
              :value="store.storeId"
            />
          </el-select>
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
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <!-- 预约编号列 -->
        <template #reservationCode="{ row }">
          <span class="code-text">{{ row.reservationCode }}</span>
        </template>

        <!-- 预约人列 -->
        <template #customerName="{ row }">
          <div class="customer-cell">
            <el-icon class="customer-icon"><User /></el-icon>
            <span class="customer-name">{{ row.customerName }}</span>
          </div>
        </template>

        <!-- 联系电话列 -->
        <template #customerPhone="{ row }">
          <div class="phone-cell">
            <el-icon class="phone-icon"><Phone /></el-icon>
            <span class="phone-text">{{ row.customerPhone }}</span>
          </div>
        </template>

        <!-- 预约日期列 -->
        <template #reservationDate="{ row }">
          <span class="date-text">{{ row.reservationDate }}</span>
        </template>

        <!-- 预约时间列 -->
        <template #reservationTime="{ row }">
          <span class="time-text">{{ row.reservationTime?.slice(0, 5) }}</span>
        </template>

        <!-- 用餐人数列 -->
        <template #peopleCount="{ row }">
          <span class="people-text">{{ row.peopleCount }} 人</span>
        </template>

        <!-- 桌号/包间列 -->
        <template #tableInfo="{ row }">
          <span class="table-text">{{ getTableInfo(row.tableId) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusType(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 门店列 -->
        <template #storeName="{ row }">
          <span class="store-text">{{ getStoreNameByTableId(row.tableId) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
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

    <!-- 新增/编辑预约对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '修改预约' : '新增预约'"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预约人姓名" prop="customerName">
              <el-input v-model="formData.customerName" placeholder="请输入预约人姓名" maxlength="50" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="customerPhone">
              <el-input v-model="formData.customerPhone" placeholder="请输入手机号" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预约日期" prop="reservationDate">
              <el-date-picker
                v-model="formData.reservationDate"
                type="date"
                placeholder="选择预约日期"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                :teleported="false"
                style="width: 100%"
                :disabled-date="(d: Date) => d.getTime() < Date.now() - 86400000"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预约时间" prop="reservationTime">
              <el-time-picker
                v-model="formData.reservationTime"
                placeholder="选择预约时间"
                format="HH:mm"
                value-format="HH:mm:ss"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="用餐人数" prop="peopleCount">
              <el-input-number v-model="formData.peopleCount" :min="1" :max="50" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="桌号/包间" prop="tableId">
              <el-select
                v-model="formData.tableId"
                placeholder="请选择桌台"
                :teleported="false"
                style="width: 100%"
                filterable
                :loading="tableOptionsLoading"
              >
                <el-option
                  v-for="t in tableOptions"
                  :key="t.tableId"
                  :label="`${t.tableName}（${t.seatsCount}人座）`"
                  :value="t.tableId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="定金(元)" prop="depositAmount">
              <el-input v-model="formData.depositAmount" type="number" min="0" step="0.01" placeholder="0">
                <template #append>元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属门店" prop="storeId">
              <el-select v-model="formData.storeId" placeholder="选择门店" :teleported="false" style="width: 100%" :loading="storeOptionsLoading">
                <el-option
                  v-for="store in storeOptions"
                  :key="store.storeId"
                  :label="store.storeName"
                  :value="store.storeId"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注/特殊要求" prop="remark">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注或特殊要求"
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

    <!-- 详情抽屉 -->
    <ReservationDetailDrawer
      v-model:visible="detailDrawerVisible"
      :data="currentDetail"
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
  grid-template-columns: repeat(3, 1fr);
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

// 预约编号
.code-text {
  font-family: var(--fts-font-family-mono);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

// 预约人
.customer-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.customer-icon {
  color: var(--fts-text-tertiary);
  font-size: 14px;
}

.customer-name {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 联系电话
.phone-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.phone-icon {
  color: var(--fts-text-tertiary);
  font-size: 14px;
}

.phone-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 日期
.date-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

// 时间
.time-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

// 人数
.people-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

// 桌台
.table-text {
  color: var(--fts-text-secondary);
}

// 门店
.store-text {
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
  .stats-section {
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
