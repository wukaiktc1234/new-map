<script setup lang="ts">
/**
 * 桌台管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理门店桌台布局和使用状态，支持图形视图和列表视图切换
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Grid, List, Search, Refresh, User, Phone, Clock } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { tableApi } from '@/api/store-ops/table'
import { diningTableDataConverter } from '@/api/store-ops/converters'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { DiningTable, DiningTableQueryForm, DiningTableStats, TableStatus, TableType } from '@/types/store-operation'

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

interface OpenTableFormData {
  peopleCount: number
  customerName: string
  customerPhone: string
}

interface TableDetailData {
  table: DiningTable | null
  orderInfo: {
    orderNumber: string
    orderAmount: string
    orderItems: number
  } | null
  customerInfo: {
    name: string
    phone: string
    peopleCount: number
    startTime: string
  } | null
}

// ==================== 响应式数据 ====================

const currentTime = ref('')
let timer: number | null = null

const viewMode = ref<'grid' | 'list'>('grid')
const selectedStore = ref('')
const selectedDate = ref(new Date())

const openTableDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const selectedTable = ref<DiningTable | null>(null)

const openTableFormRef = ref<FormInstance>()
const openTableForm = reactive<OpenTableFormData>({
  peopleCount: 2,
  customerName: '',
  customerPhone: '',
})

const tableDetail = reactive<TableDetailData>({
  table: null,
  orderInfo: null,
  customerInfo: null,
})

const statsData = ref<DiningTableStats>({
  total: 0,
  idle: 0,
  dining: 0,
  disabled: 0,
})

// 查询表单
const queryForm = ref({
  status: '' as TableStatus | '',
  tableType: '' as TableType | '',
  keyword: '',
})

// 使用useCrudTable管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DiningTable, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: DiningTableQueryForm = {
        page: params.page,
        size: params.size,
      }
      if (params.status) queryParams.status = params.status
      if (params.tableType) queryParams.tableType = params.tableType
      if (params.keyword) queryParams.keyword = params.keyword
      return tableApi.getList(queryParams)
    },
  } as unknown as CrudApi<DiningTable, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// ==================== 计算属性 ====================

const statistics = computed(() => ({
  total: statsData.value.total,
  dining: statsData.value.dining,
  idle: statsData.value.idle,
  reserved: tableData.value.filter(item => item.status === 'reserved').length,
}))

// 按区域分组的桌台数据
const tablesByArea = computed(() => {
  const groups: Record<string, DiningTable[]> = {
    hall: [],
    private_room: [],
    bar: [],
    outdoor: [],
  }
  tableData.value.forEach(table => {
    const type = table.tableType
    if (groups[type]) {
      groups[type].push(table)
    }
  })
  return groups
})

const columns = computed<ColumnDef[]>(() => [
  { prop: 'tableCode', label: '桌台号', minWidth: 100, slot: 'tableCode' },
  { prop: 'tableType', label: '区域', minWidth: 100, slot: 'tableType' },
  { prop: 'seatsCount', label: '桌型(人数)', minWidth: 110, slot: 'seatsCount' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'currentPeople', label: '当前用餐人数', minWidth: 120, slot: 'currentPeople' },
  { prop: 'openTime', label: '开台时间', minWidth: 160, slot: 'openTime' },
  { prop: 'duration', label: '已用餐时长', minWidth: 120, slot: 'duration' },
])

// ==================== 表单校验规则 ====================

const openTableRules: FormRules = {
  peopleCount: [
    { required: true, message: '请输入顾客人数', trigger: 'blur' },
    { type: 'number', min: 1, max: 50, message: '人数需在1-50之间', trigger: 'blur' },
  ],
  customerName: [
    { required: true, message: '请输入顾客姓名', trigger: 'blur' },
    { min: 1, max: 20, message: '姓名长度在1到20个字符之间', trigger: 'blur' },
  ],
  customerPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

/** 更新当前时间 */
function updateCurrentTime() {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  currentTime.value = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())} ${pad(now.getHours())}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

/** 加载统计数据 */
async function loadStats() {
  try {
    const stats = await tableApi.getStats()
    statsData.value = stats
  } catch {
    statsData.value = { total: 0, idle: 0, dining: 0, disabled: 0 }
  }
}

/** 搜索 */
function handleSearch() {
  refresh()
}

/** 重置 */
function handleReset() {
  queryForm.value.status = ''
  queryForm.value.tableType = ''
  queryForm.value.keyword = ''
  refresh()
}

/** 切换视图 */
function toggleView(mode: 'grid' | 'list') {
  viewMode.value = mode
}

/** 打开开台对话框 */
function handleOpenTable(row: DiningTable) {
  if (row.status !== 'idle') {
    ElMessage.warning('该桌台当前不可开台')
    return
  }
  selectedTable.value = row
  Object.assign(openTableForm, {
    peopleCount: row.seatsCount,
    customerName: '',
    customerPhone: '',
  })
  openTableDialogVisible.value = true
}

/** 确认开台 */
async function handleConfirmOpenTable() {
  if (!openTableFormRef.value) return
  const valid = await openTableFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (!selectedTable.value) return

  try {
    const statusNum = diningTableDataConverter.convertToStatusNum('dining')
    await tableApi.updateStatus(selectedTable.value.tableId, statusNum)
    ElMessage.success('开台成功')
    openTableDialogVisible.value = false
    await Promise.all([refresh(), loadStats()])
  } catch {
    ElMessage.error('开台失败')
  }
}

/** 清台 */
async function handleClearTable(row: DiningTable) {
  try {
    await ElMessageBox.confirm(
      `确定要清台「${row.tableCode}」吗？清台后该桌台将变为空闲状态。`,
      '清台确认',
      {
        confirmButtonText: '确定清台',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const statusNum = diningTableDataConverter.convertToStatusNum('idle')
    await tableApi.updateStatus(row.tableId, statusNum)
    ElMessage.success('清台成功')
    await Promise.all([refresh(), loadStats()])
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('清台失败')
    }
  }
}

/** 转台 */
function handleTransferTable(row: DiningTable) {
  selectedTable.value = row
  ElMessage.info('转台功能开发中...')
}

/** 查看详情 */
async function handleViewDetail(row: DiningTable) {
  selectedTable.value = row
  tableDetail.table = row

  if (row.status === 'dining') {
    tableDetail.orderInfo = {
      orderNumber: 'ORD' + Date.now().toString().slice(-10),
      orderAmount: '¥128.50',
      orderItems: 6,
    }
    tableDetail.customerInfo = {
      name: '张先生',
      phone: '138****8888',
      peopleCount: row.seatsCount,
      startTime: '2026-07-06 12:00:00',
    }
  } else {
    tableDetail.orderInfo = null
    tableDetail.customerInfo = null
  }

  detailDialogVisible.value = true
}

/** 获取桌台状态对应的颜色类名 */
function getTableStatusClass(status: TableStatus): string {
  const map: Record<TableStatus, string> = {
    idle: 'table-idle',
    dining: 'table-dining',
    reserved: 'table-reserved',
    maintenance: 'table-maintenance',
    disabled: 'table-disabled',
  }
  return map[status] || 'table-idle'
}

/** 获取区域标签 */
function getAreaLabel(type: TableType): string {
  return diningTableDataConverter.toTableTypeLabel(type)
}

/** 计算已用餐时长（模拟数据） */
function getDuration(row: DiningTable): string {
  if (row.status !== 'dining') return '-'
  return '1小时23分钟'
}

/** 获取开台时间（模拟数据） */
function getOpenTime(row: DiningTable): string {
  if (row.status !== 'dining') return '-'
  return '2026-07-06 12:00:00'
}

/** 获取当前用餐人数（模拟数据） */
function getCurrentPeople(row: DiningTable): number | string {
  if (row.status !== 'dining') return '-'
  return row.seatsCount
}

// ==================== 生命周期 ====================

onMounted(() => {
  updateCurrentTime()
  timer = window.setInterval(updateCurrentTime, 1000)
  loadStats()
})

onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="桌台记录" description="管理门店桌台布局和使用状态">
      <div class="header-right">
        <span class="current-time">
          <el-icon><Clock /></el-icon>
          {{ currentTime }}
        </span>
      </div>
    </PageHeader>

    <!-- 顶部工具栏：门店选择、日期选择 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <el-select v-model="selectedStore" placeholder="选择门店" clearable style="width: 180px" size="default">
          <el-option label="总店" value="1" />
          <el-option label="分店A" value="2" />
          <el-option label="分店B" value="3" />
        </el-select>
        <el-date-picker
          v-model="selectedDate"
          type="date"
          placeholder="选择日期"
          style="width: 160px"
          size="default"
          :teleported="false"
        />
      </div>
      <div class="toolbar-right">
        <el-button size="default" @click="handleReset">
          <el-icon :size="14"><Refresh /></el-icon>刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Grid" label="总桌台数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Dish" label="使用中" :value="String(statistics.dining)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleCheck" label="空闲" :value="String(statistics.idle)" color-type="success" variant="bordered" />
      <StatCard icon="Calendar" label="已预订" :value="String(statistics.reserved)" color-type="info" variant="bordered" />
    </section>

    <!-- 视图切换和搜索栏 -->
    <div class="view-toolbar">
      <div class="view-toggle">
        <el-button
          :type="viewMode === 'grid' ? 'primary' : 'default'"
          size="default"
          @click="toggleView('grid')"
        >
          <el-icon><Grid /></el-icon>图形视图
        </el-button>
        <el-button
          :type="viewMode === 'list' ? 'primary' : 'default'"
          size="default"
          @click="toggleView('list')"
        >
          <el-icon><List /></el-icon>列表视图
        </el-button>
      </div>
      <div class="search-bar">
        <el-select
          v-model="queryForm.tableType"
          placeholder="桌台类型"
          clearable
          style="width: 130px"
          size="default"
          @change="handleSearch"
        >
          <el-option label="大厅" value="hall" />
          <el-option label="包厢" value="private_room" />
          <el-option label="吧台" value="bar" />
          <el-option label="露台" value="outdoor" />
        </el-select>
        <el-select
          v-model="queryForm.status"
          placeholder="状态"
          clearable
          style="width: 110px"
          size="default"
          @change="handleSearch"
        >
          <el-option label="空闲" value="idle" />
          <el-option label="用餐中" value="dining" />
          <el-option label="已预约" value="reserved" />
          <el-option label="维护中" value="maintenance" />
          <el-option label="停用" value="disabled" />
        </el-select>
        <el-input
          v-model="queryForm.keyword"
          placeholder="搜索桌台号..."
          clearable
          style="width: 200px"
          size="default"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
        <el-button size="default" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 图形视图 -->
    <section v-if="viewMode === 'grid'" class="grid-view-section">
      <div v-for="(tables, area) in tablesByArea" :key="area" class="area-block">
        <div class="area-title">{{ getAreaLabel(area as TableType) }}</div>
        <div class="table-grid">
          <div
            v-for="table in tables"
            :key="table.tableId"
            class="table-card"
            :class="getTableStatusClass(table.status)"
            @click="handleViewDetail(table)"
          >
            <div class="table-code">{{ table.tableCode }}</div>
            <div class="table-seats">
              <el-icon :size="12"><User /></el-icon>
              {{ table.seatsCount }}人
            </div>
            <el-tooltip :content="`${table.tableName} - ${diningTableDataConverter.toStatusLabel(table.status)}`" placement="top">
              <div class="table-status-dot"></div>
            </el-tooltip>
          </div>
          <div v-if="tables.length === 0" class="empty-area">暂无桌台</div>
        </div>
      </div>
    </section>

    <!-- 列表视图 -->
    <section v-else class="table-section">
      <DataTable
        :data="tableData"
        :columns="columns"
        :loading="loading"
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        :actions-width="240"
      >
        <!-- 桌台号列 -->
        <template #tableCode="{ row }">
          <span class="table-code-text">{{ row.tableCode }}</span>
        </template>

        <!-- 区域列 -->
        <template #tableType="{ row }">
          <span class="area-text">{{ getAreaLabel(row.tableType) }}</span>
        </template>

        <!-- 桌型列 -->
        <template #seatsCount="{ row }">
          <span class="seats-text">
            <el-icon :size="12"><Users /></el-icon>
            {{ row.seatsCount }}人桌
          </span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="diningTableDataConverter.toStatusTagStatus(row.status)"
            :label="diningTableDataConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 当前用餐人数列 -->
        <template #currentPeople="{ row }">
          <span class="people-text">{{ getCurrentPeople(row) }}</span>
        </template>

        <!-- 开台时间列 -->
        <template #openTime="{ row }">
          <span class="time-text">{{ getOpenTime(row) }}</span>
        </template>

        <!-- 已用餐时长列 -->
        <template #duration="{ row }">
          <span class="duration-text">{{ getDuration(row) }}</span>
        </template>

        <!-- 操作列 -->
        <template #actions="{ row }">
          <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
            详情
          </el-button>
          <el-button
            v-if="row.status === 'idle'"
            link
            type="success"
            size="default"
            @click.stop="handleOpenTable(row)"
          >
            开台
          </el-button>
          <el-button
            v-if="row.status === 'dining'"
            link
            type="warning"
            size="default"
            @click.stop="handleClearTable(row)"
          >
            清台
          </el-button>
          <el-button
            v-if="row.status === 'dining'"
            link
            type="info"
            size="default"
            @click.stop="handleTransferTable(row)"
          >
            转台
          </el-button>
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

    <!-- 开台对话框 -->
    <el-dialog
      v-model="openTableDialogVisible"
      title="开台"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="openTableFormRef"
        :model="openTableForm"
        :rules="openTableRules"
        label-width="100px"
      >
        <el-form-item label="桌台号">
          <span class="form-static-text">{{ selectedTable?.tableCode }}</span>
        </el-form-item>
        <el-form-item label="桌型">
          <span class="form-static-text">{{ selectedTable?.seatsCount }}人桌</span>
        </el-form-item>
        <el-form-item label="顾客人数" prop="peopleCount">
          <el-input-number
            v-model="openTableForm.peopleCount"
            :min="1"
            :max="50"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="顾客姓名" prop="customerName">
          <el-input v-model="openTableForm.customerName" placeholder="请输入顾客姓名" maxlength="20" />
        </el-form-item>
        <el-form-item label="联系电话" prop="customerPhone">
          <el-input v-model="openTableForm.customerPhone" placeholder="请输入联系电话" maxlength="11" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="openTableDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmOpenTable">确认开台</el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="桌台详情"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="tableDetail.table" class="detail-content">
        <!-- 桌台信息 -->
        <div class="detail-section">
          <div class="section-title">桌台信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">桌台号：</span>
              <span class="detail-value">{{ tableDetail.table.tableCode }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">桌台名称：</span>
              <span class="detail-value">{{ tableDetail.table.tableName }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">区域：</span>
              <span class="detail-value">{{ getAreaLabel(tableDetail.table.tableType) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">座位数：</span>
              <span class="detail-value">{{ tableDetail.table.seatsCount }}人</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">状态：</span>
              <StatusTag
                :status="diningTableDataConverter.toStatusTagStatus(tableDetail.table.status)"
                :label="diningTableDataConverter.toStatusLabel(tableDetail.table.status)"
                size="small"
                variant="light"
              />
            </div>
          </div>
        </div>

        <!-- 顾客信息 -->
        <div v-if="tableDetail.customerInfo" class="detail-section">
          <div class="section-title">顾客信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">
                <el-icon><User /></el-icon>姓名：
              </span>
              <span class="detail-value">{{ tableDetail.customerInfo.name }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">
                <el-icon><Phone /></el-icon>电话：
              </span>
              <span class="detail-value">{{ tableDetail.customerInfo.phone }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">
                <el-icon><User /></el-icon>人数：
              </span>
              <span class="detail-value">{{ tableDetail.customerInfo.peopleCount }}人</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">
                <el-icon><Clock /></el-icon>开台时间：
              </span>
              <span class="detail-value">{{ tableDetail.customerInfo.startTime }}</span>
            </div>
          </div>
        </div>

        <!-- 订单信息 -->
        <div v-if="tableDetail.orderInfo" class="detail-section">
          <div class="section-title">订单信息</div>
          <div class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">订单号：</span>
              <span class="detail-value">{{ tableDetail.orderInfo.orderNumber }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">菜品数量：</span>
              <span class="detail-value">{{ tableDetail.orderInfo.orderItems }}道</span>
            </div>
            <div class="detail-item detail-item-full">
              <span class="detail-label">订单金额：</span>
              <span class="detail-value amount">{{ tableDetail.orderInfo.orderAmount }}</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button v-if="tableDetail.table?.status === 'idle'" type="primary" @click="handleOpenTable(tableDetail.table!); detailDialogVisible = false">
          开台
        </el-button>
        <el-button v-if="tableDetail.table?.status === 'dining'" type="warning" @click="handleClearTable(tableDetail.table!); detailDialogVisible = false">
          清台
        </el-button>
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

// 页面头部右侧
.header-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
}

.current-time {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  font-variant-numeric: tabular-nums;

  .el-icon {
    color: var(--fts-text-tertiary);
  }
}

// 顶部工具栏
.top-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4) 0;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
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

// 视图切换工具栏
.view-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-bottom: none;
  border-radius: var(--fts-page-radius) var(--fts-page-radius) 0 0;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
}

.view-toggle {
  display: flex;
  gap: var(--fts-space-2);
}

.search-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
}

// 图形视图区域
.grid-view-section {
  padding: 0 var(--fts-space-6) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
}

.area-block {
  padding-top: var(--fts-space-4);

  & + .area-block {
    border-top: 1px solid var(--fts-border-secondary);
    margin-top: var(--fts-space-4);
  }
}

.area-title {
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
}

.table-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: var(--fts-space-3);
}

.table-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-4) var(--fts-space-3);
  border-radius: var(--fts-card-radius);
  border: 2px solid var(--fts-border-primary);
  background: var(--fts-bg-card);
  cursor: pointer;
  transition: all var(--fts-transition-duration) var(--fts-transition-timing-function);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--fts-shadow-sm);
  }
}

.table-code {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-1);
}

.table-seats {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  .el-icon {
    color: var(--fts-text-tertiary);
  }
}

.table-status-dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--fts-text-tertiary);
}

// 桌台状态颜色
.table-idle {
  border-color: var(--fts-success-light-5);
  background: var(--fts-success-light-9);

  .table-status-dot {
    background: var(--fts-success);
  }

  &:hover {
    border-color: var(--fts-success);
  }
}

.table-dining {
  border-color: var(--fts-warning-light-5);
  background: var(--fts-warning-light-9);

  .table-status-dot {
    background: var(--fts-warning);
  }

  &:hover {
    border-color: var(--fts-warning);
  }
}

.table-reserved {
  border-color: var(--fts-info-light-5);
  background: var(--fts-info-light-9);

  .table-status-dot {
    background: var(--fts-info);
  }

  &:hover {
    border-color: var(--fts-info);
  }
}

.table-maintenance {
  border-color: var(--fts-warning-light-5);
  background: var(--fts-warning-light-9);
  opacity: 0.7;

  .table-status-dot {
    background: var(--fts-warning);
  }
}

.table-disabled {
  border-color: var(--fts-border-secondary);
  background: var(--fts-bg-page);
  opacity: 0.5;
  cursor: not-allowed;

  .table-status-dot {
    background: var(--fts-text-tertiary);
  }

  &:hover {
    transform: none;
    box-shadow: none;
  }
}

.empty-area {
  grid-column: 1 / -1;
  text-align: center;
  padding: var(--fts-space-6);
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
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

// 表格文本样式
.table-code-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.area-text {
  color: var(--fts-text-primary);
}

.seats-text {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--fts-text-primary);

  .el-icon {
    color: var(--fts-text-tertiary);
  }
}

.people-text {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.duration-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 表单静态文本
.form-static-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 详情对话框
.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.detail-section {
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-page);
}

.section-title {
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-3) var(--fts-space-4);
}

.detail-item {
  display: flex;
  align-items: center;
  font-size: var(--fts-font-size-sm);

  &.detail-item-full {
    grid-column: 1 / -1;
  }
}

.detail-label {
  color: var(--fts-text-tertiary);
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;

  .el-icon {
    color: var(--fts-text-tertiary);
  }
}

.detail-value {
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;

  &.amount {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-primary);
  }
}

// 响应式
@media (max-width: 768px) {
  .view-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-bar {
    justify-content: flex-end;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 576px) {
  .top-toolbar {
    padding: var(--fts-space-2) 0;
  }

  .view-toolbar {
    padding: var(--fts-space-3) var(--fts-space-4);
  }

  .grid-view-section,
  .table-section {
    padding: 0 var(--fts-space-4) var(--fts-space-4);
  }

  .pagination-wrapper {
    padding: var(--fts-space-4);
    justify-content: center;
  }

  .table-grid {
    grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  }
}
</style>
