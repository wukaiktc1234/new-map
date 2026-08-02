<script setup lang="ts">
/**
 * 操作审计页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】展示系统操作日志和审计记录，支持搜索、筛选、导出和详情查看
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download, Refresh, Search } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import {
  auditLogApi,
  AUDIT_OPERATION_TYPE_OPTIONS,
  type AuditLogItem,
  type AuditLogQueryParams,
  type AuditLogStatistics,
  type AuditOperationType,
  type AuditResponseStatus,
} from '@/api/system/audit-log'

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

// ==================== 响应式数据 ====================

const exportLoading = ref(false)
const detailDialogVisible = ref(false)
const currentDetail = ref<AuditLogItem | null>(null)

/** 统计数据 */
const statisticsData = ref<AuditLogStatistics>({
  total: 0,
  highRisk: 0,
  criticalRisk: 0,
  failedOperations: 0,
  sensitiveOperations: 0,
})

/**
 * 今日操作数
 * TODO: 审计统计接口待后端实现。AuditLogStatistics 暂无 todayCount 字段，
 * 当前显示 0，禁止按比例伪造（审计页面本身不能有假数据，这是合规底线）。
 */
const todayCount = ref(0)
/**
 * 登录次数
 * TODO: 审计统计接口待后端实现。AuditLogStatistics 暂无 loginCount 字段，
 * 当前显示 0，禁止按比例伪造。
 */
const loginCount = ref(0)

// 查询表单
const queryForm = ref({
  username: '',
  module: '',
  operationType: '' as AuditOperationType | '',
  responseStatus: '' as AuditResponseStatus | '',
  startTime: '',
  endTime: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<AuditLogItem, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const apiParams: AuditLogQueryParams = {
        current: params.page,
        size: params.size,
        username: params.username || undefined,
        module: params.module || undefined,
        operationType: params.operationType || undefined,
        startTime: params.startTime || undefined,
        endTime: params.endTime || undefined,
      }
      const res = await auditLogApi.getList(apiParams)
      return {
        records: res.records || [],
        total: res.total || 0,
      }
    },
  } as unknown as CrudApi<AuditLogItem, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 操作模块选项
const moduleOptions = [
  { value: 'system', label: '系统管理' },
  { value: 'user', label: '用户管理' },
  { value: 'role', label: '角色管理' },
  { value: 'product', label: '菜品管理' },
  { value: 'order', label: '订单管理' },
  { value: 'purchase', label: '采购管理' },
  { value: 'warehouse', label: '仓储管理' },
  { value: 'finance', label: '财务管理' },
  { value: 'hr', label: '人事管理' },
]

// 操作结果选项
const resultOptions = [
  { value: 'SUCCESS', label: '成功' },
  { value: 'FAILED', label: '失败' },
  { value: 'PARTIAL', label: '部分成功' },
]

// 统计卡片数据
const statistics = computed(() => ({
  todayOps: todayCount.value,
  loginCount: loginCount.value,
  failedOps: statisticsData.value.failedOperations,
  total: statisticsData.value.total,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'id', label: '日志编号', minWidth: 100 },
  { prop: 'username', label: '操作人', minWidth: 100 },
  { prop: 'module', label: '操作模块', minWidth: 110 },
  { prop: 'operationType', label: '操作类型', minWidth: 100, slot: 'operationType' },
  { prop: 'operation', label: '操作描述', minWidth: 200, ellipsis: true },
  { prop: 'ip', label: '请求IP', minWidth: 130 },
  { prop: 'responseStatus', label: '操作结果', minWidth: 100, slot: 'responseStatus' },
  { prop: 'createdAt', label: '操作时间', minWidth: 160, slot: 'createdAt' },
  { prop: 'executionTime', label: '耗时', minWidth: 90, slot: 'executionTime' },
  { prop: '_operation', label: '操作', width: 100, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  refresh()
  loadStatistics()
}

function handleReset() {
  queryForm.value.username = ''
  queryForm.value.module = ''
  queryForm.value.operationType = ''
  queryForm.value.responseStatus = ''
  queryForm.value.startTime = ''
  queryForm.value.endTime = ''
  refresh()
  loadStatistics()
}

function handleRefresh() {
  refresh()
  loadStatistics()
}

/**
 * 加载统计信息
 */
async function loadStatistics() {
  try {
    const params: { startDate?: string; endDate?: string } = {}
    if (queryForm.value.startTime) params.startDate = queryForm.value.startTime
    if (queryForm.value.endTime) params.endDate = queryForm.value.endTime
    const res = await auditLogApi.getStatistics(params)
    if (res) {
      statisticsData.value = res
      // 修复 P0：原代码按比例伪造今日操作数（total * 0.15）和登录次数（total * 0.08），
      // 审计页面本身不能有假数据，这是合规底线。
      // TODO: 待后端 AuditLogStatistics 接口增加 todayCount/loginCount 字段后，从 API 获取真实值。
      // 当前保持 todayCount 和 loginCount 为 0，显示真实空状态。
    }
  } catch {
    // 统计加载失败保持默认值
  }
}

/**
 * 查看详情
 */
function handleViewDetail(row: AuditLogItem) {
  currentDetail.value = row
  detailDialogVisible.value = true
}

/**
 * 导出日志
 */
async function handleExport() {
  exportLoading.value = true
  try {
    const params: AuditLogQueryParams = {
      username: queryForm.value.username || undefined,
      module: queryForm.value.module || undefined,
      operationType: queryForm.value.operationType || undefined,
      startTime: queryForm.value.startTime || undefined,
      endTime: queryForm.value.endTime || undefined,
    }
    const blob = await auditLogApi.export(params)
    if (!blob) {
      ElMessage.error('导出失败，请稍后重试')
      return
    }
    const now = new Date()
    const pad = (n: number): string => String(n).padStart(2, '0')
    const fileName = `操作审计日志_${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}_${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}.csv`
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = fileName
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '导出失败')
    } else {
      ElMessage.error('导出失败')
    }
  } finally {
    exportLoading.value = false
  }
}

// ==================== 辅助方法 ====================

function getOperationTypeLabel(type: string): string {
  const opt = AUDIT_OPERATION_TYPE_OPTIONS.find(o => o.value === type)
  return opt?.label || type
}

function getOperationTypeStatus(type: string): string {
  const map: Record<string, string> = {
    LOGIN: 'info',
    LOGOUT: 'info',
    CREATE: 'success',
    UPDATE: 'warning',
    DELETE: 'error',
    EXPORT: 'warning',
    VIEW: 'info',
    AUTH: 'active',
    SYSTEM: 'active',
    SECURITY: 'active',
  }
  return map[type] || 'info'
}

function getResponseStatusStatus(status: string): string {
  const map: Record<string, string> = {
    SUCCESS: 'success',
    FAILED: 'error',
    PARTIAL: 'warning',
  }
  return map[status] || 'info'
}

function getResponseStatusLabel(status: string): string {
  const map: Record<string, string> = {
    SUCCESS: '成功',
    FAILED: '失败',
    PARTIAL: '部分成功',
  }
  return map[status] || status
}

function formatExecutionTime(time: number | null): string {
  if (time === null || time === undefined) return '-'
  if (time < 1000) return `${time}ms`
  return `${(time / 1000).toFixed(2)}s`
}

function getExecutionTimeClass(time: number | null): string {
  if (time === null || time === undefined) return ''
  if (time < 100) return 'exec-time-fast'
  if (time > 1000) return 'exec-time-slow'
  return ''
}

function formatDateTime(dateStr: string | null): string {
  if (!dateStr) return '-'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return dateStr
  const pad = (n: number): string => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

function formatJson(str: string | null): string {
  if (!str) return '-'
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

function getModuleLabel(module: string | null): string {
  if (!module) return '-'
  const opt = moduleOptions.find(o => o.value === module)
  return opt?.label || module
}

// 页面挂载时加载统计数据
onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="操作审计" description="查看系统操作日志和审计记录" />

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="今日操作数" :value="String(statistics.todayOps)" color-type="primary" variant="bordered" />
      <StatCard icon="User" label="登录次数" :value="String(statistics.loginCount)" color-type="success" variant="bordered" />
      <StatCard icon="Warning" label="异常操作" :value="String(statistics.failedOps)" color-type="error" variant="bordered" />
      <StatCard icon="DataLine" label="总操作数" :value="String(statistics.total)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导出刷新） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.username"
            placeholder="操作人搜索"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.module"
            placeholder="操作模块"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in moduleOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.operationType"
            placeholder="操作类型"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in AUDIT_OPERATION_TYPE_OPTIONS"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.responseStatus"
            placeholder="操作结果"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in resultOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 180px"
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
          <el-button
            type="success"
            size="default"
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出日志
          </el-button>
          <el-button size="default" @click="handleRefresh">
            <el-icon :size="14"><Refresh /></el-icon>刷新
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
        <!-- 操作类型列 -->
        <template #operationType="{ row }">
          <StatusTag :status="getOperationTypeStatus(row.operationType)" :label="getOperationTypeLabel(row.operationType)" size="small" variant="light" />
        </template>

        <!-- 操作结果列 -->
        <template #responseStatus="{ row }">
          <StatusTag :status="getResponseStatusStatus(row.responseStatus)" :label="getResponseStatusLabel(row.responseStatus)" size="small" variant="light" />
        </template>

        <!-- 操作时间列 -->
        <template #createdAt="{ row }">
          <span class="time-text">{{ formatDateTime(row.createdAt) }}</span>
        </template>

        <!-- 耗时列 -->
        <template #executionTime="{ row }">
          <span :class="getExecutionTimeClass(row.executionTime)">{{ formatExecutionTime(row.executionTime) }}</span>
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
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </section>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="操作日志详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-descriptions v-if="currentDetail" :column="2" border>
        <el-descriptions-item label="日志编号">{{ currentDetail.id }}</el-descriptions-item>
        <el-descriptions-item label="操作时间">{{ formatDateTime(currentDetail.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ currentDetail.username || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用户ID">{{ currentDetail.userId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作模块">{{ getModuleLabel(currentDetail.module) }}</el-descriptions-item>
        <el-descriptions-item label="操作类型">
          <StatusTag :status="getOperationTypeStatus(currentDetail.operationType)" :label="getOperationTypeLabel(currentDetail.operationType)" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="操作描述" :span="2">{{ currentDetail.operation || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求IP">{{ currentDetail.ip || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作结果">
          <StatusTag :status="getResponseStatusStatus(currentDetail.responseStatus)" :label="getResponseStatusLabel(currentDetail.responseStatus)" size="small" />
        </el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentDetail.requestMethod || '-' }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ formatExecutionTime(currentDetail.executionTime) }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ currentDetail.requestUrl || '-' }}</el-descriptions-item>
        <el-descriptions-item label="会话ID">{{ currentDetail.sessionId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="业务Key">{{ currentDetail.businessKey || '-' }}</el-descriptions-item>
        <el-descriptions-item label="UserAgent" :span="2">{{ currentDetail.userAgent || '-' }}</el-descriptions-item>
      </el-descriptions>

      <!-- 请求参数 -->
      <div class="detail-section" style="margin-top: 16px">
        <div class="section-title">请求参数</div>
        <pre class="json-block">{{ formatJson(currentDetail?.requestParams || null) }}</pre>
      </div>

      <!-- 返回结果 -->
      <div class="detail-section" style="margin-top: 16px">
        <div class="section-title">返回结果</div>
        <pre class="json-block">{{ formatJson(currentDetail?.responseBody || null) }}</pre>
      </div>

      <!-- 异常信息（仅当有错误时显示） -->
      <div v-if="currentDetail?.errorMessage" class="detail-section" style="margin-top: 16px">
        <div class="section-title section-title--error">异常信息</div>
        <pre class="json-block json-block--error">{{ currentDetail.errorMessage }}</pre>
      </div>

      <template #footer>
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

// 时间
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 耗时着色（使用 CSS 变量）
.exec-time-fast {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.exec-time-slow {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
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

// 详情对话框区块
.detail-section {
  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);

    &--error {
      color: var(--fts-error);
    }
  }
}

// JSON 展示块
.json-block {
  margin: 0;
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  font-family: 'Cascadia Code', 'Fira Code', monospace;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-primary);
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;

  &--error {
    background: var(--fts-error-light-9);
    color: var(--fts-error);
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
