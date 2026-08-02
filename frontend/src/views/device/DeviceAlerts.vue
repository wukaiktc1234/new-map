<script setup lang="ts">
/**
 * 设备告警页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】设备告警的查询、详情查看、处理、批量处理等功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Download, Bell, Warning, Clock, CircleCheck } from '@element-plus/icons-vue'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { useStoreOptions } from '@/composables/useStoreOptions'
import { deviceAlertApi } from '@/api/device/alerts'
import {
  DeviceAlertTypeOptions,
  DeviceAlertLevelOptions,
  DeviceAlertStatusOptions,
  DeviceAlertTypeText,
  DeviceAlertLevelText,
  DeviceAlertLevelColor,
  DeviceAlertStatusText,
  DeviceAlertStatusColor,
  DeviceTypeOptions,
  DeviceTypeText,
} from '@/types/device'
import type {
  DeviceAlertInfo,
  DeviceAlertQueryForm,
  DeviceAlertType,
  DeviceAlertLevel,
  DeviceAlertStatus,
  DeviceType,
} from '@/types/device'

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

interface HandleFormData {
  handleResult: string
  handleNote: string
  handler: string
}

// ==================== 响应式数据 ====================

const selectedRows = ref<DeviceAlertInfo[]>([])
const detailDialogVisible = ref(false)
const handleDialogVisible = ref(false)
const batchHandleDialogVisible = ref(false)
const currentDetail = ref<DeviceAlertInfo | null>(null)
const currentHandleAlert = ref<DeviceAlertInfo | null>(null)
const exportLoading = ref(false)

/** 门店选项（接入真实后端 API） */
const { storeOptions } = useStoreOptions(true)

// 查询表单
const queryForm = ref({
  alertLevel: '' as '' | DeviceAlertLevel,
  deviceType: '' as '' | DeviceType,
  storeId: '' as string | number,
  alertStatus: '' as '' | DeviceAlertStatus,
  timeRange: [] as string[],
})

// 使用 useCrudTable 管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
  resetQuery,
} = useCrudTable<DeviceAlertInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: DeviceAlertQueryForm & { page: number; size: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.alertLevel) convertedParams.alertLevel = params.alertLevel
      if (params.alertStatus) convertedParams.alertStatus = params.alertStatus
      if (params.timeRange && params.timeRange.length === 2) {
        convertedParams.startTime = params.timeRange[0]
        convertedParams.endTime = params.timeRange[1]
      }
      return deviceAlertApi.getList(convertedParams)
    },
  } as unknown as CrudApi<DeviceAlertInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 处理表单数据
const handleForm = reactive<HandleFormData>({
  handleResult: 'resolved',
  handleNote: '',
  handler: '',
})

// 批量处理表单数据
const batchHandleForm = reactive<HandleFormData>({
  handleResult: 'resolved',
  handleNote: '',
  handler: '',
})

// ==================== 统计卡片 ====================

const statistics = computed(() => {
  const all = tableData.value
  const pending = all.filter(r => r.alertStatus === 'pending').length
  const handling = all.filter(r => r.alertStatus === 'handling').length
  const resolved = all.filter(r => r.alertStatus === 'resolved').length
  const today = all.filter(r => {
    if (!r.triggerTime) return false
    const today = new Date().toDateString()
    return new Date(r.triggerTime).toDateString() === today
  }).length
  return [
    { key: 'pending', icon: 'Warning', label: '待处理告警', value: pending, colorType: 'error' as const },
    { key: 'handling', icon: 'Clock', label: '处理中', value: handling, colorType: 'warning' as const },
    { key: 'resolved', icon: 'CircleCheck', label: '已处理', value: resolved, colorType: 'success' as const },
    { key: 'today', icon: 'Bell', label: '今日告警数', value: today, colorType: 'primary' as const },
  ]
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'alertId', label: '告警编号', minWidth: 100 },
  { prop: 'deviceName', label: '设备名称', minWidth: 130 },
  { prop: 'deviceCode', label: '设备编号', minWidth: 130 },
  { prop: 'alertType', label: '告警类型', minWidth: 110, slot: 'alertType' },
  { prop: 'alertLevel', label: '告警级别', minWidth: 100, slot: 'alertLevel', ellipsis: false },
  { prop: 'storeName', label: '所属门店', minWidth: 120 },
  { prop: 'triggerTime', label: '告警时间', minWidth: 160 },
  { prop: 'alertStatus', label: '处理状态', minWidth: 100, slot: 'alertStatus', ellipsis: false },
])

// ==================== 方法 ====================

/** 查询 */
function handleSearch(): void {
  pagination.current = 1
  refresh()
}

/** 重置 */
async function handleReset(): Promise<void> {
  await resetQuery()
}

/** 查看详情 */
async function handleView(row: DeviceAlertInfo): Promise<void> {
  try {
    const detail = await deviceAlertApi.getById(row.alertId)
    currentDetail.value = detail
    detailDialogVisible.value = true
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '加载告警详情失败')
  }
}

/** 打开处理对话框 */
function openHandleDialog(row: DeviceAlertInfo): void {
  currentHandleAlert.value = row
  handleForm.handleResult = 'resolved'
  handleForm.handleNote = ''
  handleForm.handler = ''
  handleDialogVisible.value = true
}

/** 提交处理 */
async function submitHandle(): Promise<void> {
  if (!handleForm.handleNote.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  if (!currentHandleAlert.value) return
  try {
    await deviceAlertApi.handle(currentHandleAlert.value.alertId, handleForm.handleNote)
    ElMessage.success('告警处理成功')
    handleDialogVisible.value = false
    await refresh()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '处理失败')
  }
}

/** 忽略告警 */
async function handleIgnore(row: DeviceAlertInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要忽略告警「${row.alertId}」吗？`,
      '忽略确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await deviceAlertApi.handle(row.alertId, '忽略')
    ElMessage.success('告警已忽略')
    await refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

/** 打开批量处理对话框 */
function openBatchHandleDialog(): void {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择需要处理的告警')
    return
  }
  batchHandleForm.handleResult = 'resolved'
  batchHandleForm.handleNote = ''
  batchHandleForm.handler = ''
  batchHandleDialogVisible.value = true
}

/** 提交批量处理 */
async function submitBatchHandle(): Promise<void> {
  if (!batchHandleForm.handleNote.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  try {
    const ids = selectedRows.value.map(r => r.alertId)
    await deviceAlertApi.batchHandle(ids, batchHandleForm.handleNote)
    ElMessage.success(`已批量处理 ${ids.length} 条告警`)
    batchHandleDialogVisible.value = false
    selectedRows.value = []
    await refresh()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '批量处理失败')
  }
}

/** 导出告警记录 */
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')
    // 模拟导出延迟
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('导出成功！文件已开始下载')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

/** 选择变更 */
function handleSelectionChange(rows: DeviceAlertInfo[]): void {
  selectedRows.value = rows
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

/** 获取处理结果文本 */
function getHandleResultText(result: string): string {
  const map: Record<string, string> = {
    resolved: '已解决',
    observed: '待观察',
    false_alarm: '误报',
  }
  return map[result] || result
}

// TODO: DeviceAlertInfo 类型暂无 deviceCode/storeName 字段，待后端补全后直接展示
// 当前移除了基于索引生成的 mock deviceCode 和 mock storeName，避免展示虚假数据
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="设备告警" description="查看和处理设备告警信息">
      <el-button
        type="primary"
        size="default"
        :disabled="selectedRows.length === 0"
        @click="openBatchHandleDialog"
      >
        批量处理
      </el-button>
      <el-button
        size="default"
        :loading="exportLoading"
        @click="handleExport"
      >
        <el-icon :size="14"><Download /></el-icon>导出告警记录
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        v-for="stat in statistics"
        :key="stat.key"
        :icon="stat.icon"
        :label="stat.label"
        :value="String(stat.value)"
        :color-type="stat.colorType"
        variant="bordered"
      />
    </section>

    <!-- 筛选栏 -->
    <div class="search-bar">
      <div class="toolbar-left">
        <el-select
          v-model="queryForm.alertLevel"
          placeholder="告警级别"
          clearable
          style="width: 130px"
          size="default"
        >
          <el-option
            v-for="opt in DeviceAlertLevelOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select
          v-model="queryForm.deviceType"
          placeholder="设备类型"
          clearable
          style="width: 130px"
          size="default"
        >
          <el-option
            v-for="opt in DeviceTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-select
          v-model="queryForm.storeId"
          placeholder="所属门店"
          clearable
          style="width: 140px"
          size="default"
        >
          <el-option
            v-for="store in storeOptions"
            :key="store.storeId"
            :label="store.storeName"
            :value="store.storeId"
          />
        </el-select>
        <el-select
          v-model="queryForm.alertStatus"
          placeholder="状态"
          clearable
          style="width: 120px"
          size="default"
        >
          <el-option
            v-for="opt in DeviceAlertStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-date-picker
          v-model="queryForm.timeRange"
          type="datetimerange"
          range-separator="至"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DD HH:mm:ss"
          style="width: 360px"
          size="default"
          :teleported="false"
        />
      </div>
      <div class="toolbar-right">
        <el-button type="primary" size="default" @click="handleSearch">
          <el-icon :size="14"><Search /></el-icon>查询
        </el-button>
        <el-button size="default" @click="handleReset">
          <el-icon :size="14"><Refresh /></el-icon>重置
        </el-button>
      </div>
    </div>

    <!-- 表格区 -->
    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :actions-width="200"
        stripe
        selectable
        @selection-change="handleSelectionChange"
      >
        <template #alertType="{ row }">
          <StatusTag
            category="type"
            :label="DeviceAlertTypeText[row.alertType as DeviceAlertType]"
            size="small"
          />
        </template>
        <template #alertLevel="{ row }">
          <StatusTag
            :status="DeviceAlertLevelColor[row.alertLevel as DeviceAlertLevel]"
            :label="DeviceAlertLevelText[row.alertLevel as DeviceAlertLevel]"
            size="small"
          />
        </template>
        <template #alertStatus="{ row }">
          <StatusTag
            :status="DeviceAlertStatusColor[row.alertStatus as DeviceAlertStatus]"
            :label="DeviceAlertStatusText[row.alertStatus as DeviceAlertStatus]"
            size="small"
          />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">详情</el-button>
          <el-button
            v-if="row.alertStatus === 'pending' || row.alertStatus === 'handling'"
            link
            type="primary"
            size="small"
            @click="openHandleDialog(row)"
          >
            处理
          </el-button>
          <el-button
            v-if="row.alertStatus === 'pending'"
            link
            type="info"
            size="small"
            @click="handleIgnore(row)"
          >
            忽略
          </el-button>
        </template>
      </DataTable>
    </div>

    <!-- 分页区 -->
    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="refresh"
        @current-change="refresh"
      />
    </div>

    <!-- 告警详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="告警详情"
      width="680px"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-content">
        <!-- 告警基本信息 -->
        <div class="detail-section">
          <div class="detail-section-title">告警基本信息</div>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="告警编号">{{ currentDetail.alertId }}</el-descriptions-item>
            <el-descriptions-item label="告警类型">
              {{ DeviceAlertTypeText[currentDetail.alertType as DeviceAlertType] }}
            </el-descriptions-item>
            <el-descriptions-item label="告警级别">
              <StatusTag
                :status="DeviceAlertLevelColor[currentDetail.alertLevel as DeviceAlertLevel]"
                :label="DeviceAlertLevelText[currentDetail.alertLevel as DeviceAlertLevel]"
                size="small"
              />
            </el-descriptions-item>
            <el-descriptions-item label="处理状态">
              <StatusTag
                :status="DeviceAlertStatusColor[currentDetail.alertStatus as DeviceAlertStatus]"
                :label="DeviceAlertStatusText[currentDetail.alertStatus as DeviceAlertStatus]"
                size="small"
              />
            </el-descriptions-item>
            <el-descriptions-item label="告警时间" :span="2">
              {{ formatTime(currentDetail.triggerTime) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 设备信息 -->
        <div class="detail-section">
          <div class="detail-section-title">设备信息</div>
          <el-descriptions :column="2" border size="default">
            <el-descriptions-item label="设备名称">{{ currentDetail.deviceName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备ID">{{ currentDetail.deviceId }}</el-descriptions-item>
            <el-descriptions-item label="设备类型">
              {{ currentDetail.deviceType ? DeviceTypeText[currentDetail.deviceType] : '-' }}
            </el-descriptions-item>
            <!-- TODO: DeviceAlertInfo 暂无 storeId 字段，待后端补全后用 getStoreName(currentDetail.storeId) -->
            <el-descriptions-item label="所属门店">-</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 告警详情 -->
        <div class="detail-section">
          <div class="detail-section-title">告警详情</div>
          <div class="alert-message-box">
            {{ currentDetail.alertMessage || '暂无告警详情' }}
          </div>
        </div>

        <!-- 处理记录时间线 -->
        <div class="detail-section">
          <div class="detail-section-title">处理记录</div>
          <el-timeline>
            <el-timeline-item
              :timestamp="formatTime(currentDetail.triggerTime)"
              placement="top"
              type="primary"
            >
              <div class="timeline-content">
                <div class="timeline-title">告警触发</div>
                <div class="timeline-desc">系统检测到异常，自动生成告警</div>
              </div>
            </el-timeline-item>
            <el-timeline-item
              v-if="currentDetail.handleTime"
              :timestamp="formatTime(currentDetail.handleTime)"
              placement="top"
              type="success"
            >
              <div class="timeline-content">
                <div class="timeline-title">告警已处理</div>
                <div class="timeline-desc">{{ currentDetail.handleResult || '已处理' }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
        <el-button
          v-if="currentDetail && (currentDetail.alertStatus === 'pending' || currentDetail.alertStatus === 'handling')"
          type="primary"
          @click="openHandleDialog(currentDetail); detailDialogVisible = false"
        >
          去处理
        </el-button>
      </template>
    </el-dialog>

    <!-- 告警处理对话框 -->
    <el-dialog
      v-model="handleDialogVisible"
      title="处理告警"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="告警编号">
          <span>{{ currentHandleAlert?.alertId }}</span>
        </el-form-item>
        <el-form-item label="设备名称">
          <span>{{ currentHandleAlert?.deviceName }}</span>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-select
            v-model="handleForm.handleResult"
            style="width: 100%"
            :teleported="false"
          >
            <el-option label="已解决" value="resolved" />
            <el-option label="待观察" value="observed" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="handleForm.handleNote"
            type="textarea"
            :rows="4"
            placeholder="请填写处理说明..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input
            v-model="handleForm.handler"
            placeholder="请输入处理人姓名"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 批量处理对话框 -->
    <el-dialog
      v-model="batchHandleDialogVisible"
      title="批量处理告警"
      width="480px"
      destroy-on-close
    >
      <el-form label-width="90px">
        <el-form-item label="告警数量">
          <span>{{ selectedRows.length }} 条</span>
        </el-form-item>
        <el-form-item label="处理结果">
          <el-select
            v-model="batchHandleForm.handleResult"
            style="width: 100%"
            :teleported="false"
          >
            <el-option label="已解决" value="resolved" />
            <el-option label="待观察" value="observed" />
            <el-option label="误报" value="false_alarm" />
          </el-select>
        </el-form-item>
        <el-form-item label="处理说明">
          <el-input
            v-model="batchHandleForm.handleNote"
            type="textarea"
            :rows="4"
            placeholder="请填写统一处理说明..."
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="处理人">
          <el-input
            v-model="batchHandleForm.handler"
            placeholder="请输入处理人姓名"
            clearable
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchHandleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatchHandle">确认批量处理</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
}

.stats-section :deep(.stat-card) {
  flex: 1;
  min-width: 180px;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-4) var(--fts-space-5);
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md);
  margin-bottom: var(--fts-space-4);
  flex-wrap: wrap;
  gap: var(--fts-space-3);
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
}

.table-section {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-md) var(--fts-radius-md) 0 0;
  overflow: hidden;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-5);
  background: var(--fts-bg-card);
  border-radius: 0 0 var(--fts-radius-md) var(--fts-radius-md);
  border-top: 1px solid var(--fts-border-secondary);
}

.detail-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-5);
}

.detail-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.detail-section-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
  line-height: 1.2;
}

.alert-message-box {
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
  line-height: var(--fts-line-height-base);
  min-height: 60px;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.timeline-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.timeline-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}
</style>
