<template>
  <div class="call-history-panel">
    <div class="filter-bar">
      <el-input
        v-model="keyword"
        class="keyword-input"
        placeholder="取餐号 / 订单号，模糊搜索"
        clearable
        @keyup.enter="handleSearch"
        @clear="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>

      <el-radio-group v-model="range" class="range-group">
        <el-radio-button v-for="item in RANGE_OPTIONS" :key="item.value" :value="item.value">
          {{ item.label }}
        </el-radio-button>
      </el-radio-group>

      <el-select v-model="status" class="status-select" placeholder="全部状态">
        <el-option v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
      </el-select>

      <el-button type="primary" :loading="loading" @click="handleSearch">
        <el-icon><Search /></el-icon>
        <span>查询</span>
      </el-button>
      <el-button @click="handleReset">
        <el-icon><RefreshLeft /></el-icon>
        <span>重置</span>
      </el-button>
    </div>

    <div class="result-meta">
      <span>共 <strong class="meta-num">{{ total }}</strong> 条记录</span>
      <span v-if="total > 0" class="meta-divider">·</span>
      <span v-if="total > 0">第 {{ page }} / {{ maxPage }} 页</span>
      <span class="meta-hint">时间范围：{{ currentRangeLabel }}<template v-if="currentStatusLabel !== '全部'"> · 状态：{{ currentStatusLabel }}</template></span>
    </div>

    <div class="table-wrap" v-loading="loading" element-loading-text="加载中...">
      <el-table :data="records" row-key="id" size="small" max-height="52vh" class="history-table">
        <el-table-column label="取餐号" width="150">
          <template #default="{ row }">
            <div class="call-no">{{ row.callNumber || row.orderNumber || '-' }}</div>
            <div class="call-meta">
              <span class="meta-chip type">{{ row.orderType || '堂食' }}</span>
              <span v-if="row.tableNumber" class="meta-chip">{{ row.tableNumber }}</span>
              <span class="meta-chip">{{ row.itemCount ?? 0 }}份</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="订单号" width="170" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="order-number">{{ row.orderNumber || '-' }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" round effect="light">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="呼叫次数" width="98">
          <template #default="{ row }">
            <span class="call-count" :class="{ repeated: Number(row.callCount || 0) > 1 }">
              {{ row.callCount ?? 0 }} 次
            </span>
          </template>
        </el-table-column>

        <el-table-column label="首次呼叫" width="160">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.firstCallTime, '—') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="取餐时间" width="160">
          <template #default="{ row }">
            <span class="time-cell" :class="{ muted: !row.pickTime }">{{ formatDateTime(row.pickTime, '—') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="过号时间" width="160">
          <template #default="{ row }">
            <span class="time-cell" :class="{ muted: !resolvePassTime(row) }">{{ formatDateTime(resolvePassTime(row), '—') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">
            <span class="time-cell">{{ formatDateTime(row.createTime, '—') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="212" fixed="right">
          <template #default="{ row }">
            <div v-if="showRowActions(row.status)" class="row-actions">
              <el-button
                size="small"
                type="success"
                :loading="row._busy === 'pickup'"
                :disabled="!!row._busy && row._busy !== 'pickup'"
                @click="runRowAction(row, 'pickup')"
              >
                取餐
              </el-button>
              <el-button
                size="small"
                type="warning"
                plain
                :loading="row._busy === 'pass'"
                :disabled="!!row._busy && row._busy !== 'pass'"
                @click="runRowAction(row, 'pass')"
              >
                过号
              </el-button>
              <el-button
                v-if="row.status === 'called'"
                size="small"
                type="primary"
                plain
                :loading="row._busy === 'recall'"
                :disabled="!!row._busy && row._busy !== 'recall'"
                @click="runRowAction(row, 'recall')"
              >
                重叫
              </el-button>
            </div>
            <span v-else class="no-action">{{ row.status === 'picked' ? '已完成' : '已过号' }}</span>
          </template>
        </el-table-column>

        <template #empty>
          <div class="table-empty">
            <el-empty description="没有符合条件的叫号记录" :image-size="90">
              <div class="empty-tip">换个条件试试：清空关键字，或把时间范围切到「近30天 / 全部」</div>
            </el-empty>
          </div>
        </template>
      </el-table>
    </div>

    <div class="pagination-bar">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :page-sizes="PAGE_SIZES"
        :total="total"
        background
        layout="total, sizes, prev, pager, next, jumper"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RefreshLeft, Search } from '@element-plus/icons-vue'
import request from '@/api/request'
import { formatDateTime } from '@/utils/format'

/** 叫号状态：pending 待取餐 / called 已呼叫 / picked 已取餐 / passed 已过号 */
type CallStatus = 'pending' | 'called' | 'picked' | 'passed'
/** 时间快捷筛选 */
type HistoryRange = 'today' | '7d' | '30d' | 'all'
/** 行内操作类型 */
type RowAction = 'pickup' | 'pass' | 'recall'
/** el-tag 语义色 */
type TagType = 'primary' | 'success' | 'warning' | 'info' | 'danger'

interface CallHistoryRecord {
  id: number
  orderId: string
  orderNumber: string
  callNumber?: string
  tableNumber?: string
  orderType?: string
  itemCount?: number
  status: CallStatus | string
  callCount?: number
  firstCallTime?: string
  lastCallTime?: string
  pickTime?: string
  passTime?: string
  waitSeconds?: number
  createTime?: string
  updateTime?: string
}

/** 列表行（附加按钮 loading 标记，后端不返回该字段） */
interface HistoryRow extends CallHistoryRecord {
  _busy?: RowAction
}

/** GET /v1/call-number/history 解包后的分页体 */
interface HistoryPage {
  records?: CallHistoryRecord[]
  total?: number
  current?: number
  size?: number
}

const emit = defineEmits<{ (e: 'refresh-stats'): void }>()

const RANGE_OPTIONS: Array<{ label: string; value: HistoryRange }> = [
  { label: '今日', value: 'today' },
  { label: '近7天', value: '7d' },
  { label: '近30天', value: '30d' },
  { label: '全部', value: 'all' }
]

const STATUS_OPTIONS: Array<{ label: string; value: string }> = [
  { label: '全部', value: '' },
  { label: '待取餐', value: 'pending' },
  { label: '已呼叫', value: 'called' },
  { label: '已取餐', value: 'picked' },
  { label: '已过号', value: 'passed' }
]

const PAGE_SIZES = [10, 20, 50]

const keyword = ref('')
const range = ref<HistoryRange>('today')
const status = ref('')
const page = ref(1)
const size = ref(20)
const total = ref(0)
const records = ref<HistoryRow[]>([])
const loading = ref(false)

const maxPage = computed(() => {
  const pageSize = size.value > 0 ? size.value : 20
  return Math.max(1, Math.ceil(total.value / pageSize))
})

const currentRangeLabel = computed(
  () => RANGE_OPTIONS.find(item => item.value === range.value)?.label ?? '今日'
)

const currentStatusLabel = computed(
  () => STATUS_OPTIONS.find(item => item.value === status.value)?.label ?? '全部'
)

/**
 * 抽出后端 message：拦截器业务错误会 reject({ message })，axios 错误挂在 originalError.response.data.message
 */
const resolveErrorMessage = (error: unknown, fallback: string): string => {
  if (typeof error === 'string' && error.trim()) {
    return error
  }
  if (error && typeof error === 'object') {
    const err = error as {
      message?: unknown
      originalError?: { response?: { data?: { message?: unknown } } }
    }
    const backendMessage = err.originalError?.response?.data?.message
    if (typeof backendMessage === 'string' && backendMessage.trim()) {
      return backendMessage
    }
    if (typeof err.message === 'string' && err.message.trim()) {
      return err.message
    }
  }
  return fallback
}

const statusText = (value: string): string => {
  const textMap: Record<string, string> = {
    pending: '待取餐',
    called: '已呼叫',
    picked: '已取餐',
    passed: '已过号'
  }
  return textMap[value] ?? value
}

const statusTagType = (value: string): TagType => {
  switch (value) {
    case 'pending':
      return 'warning'
    case 'called':
      return 'primary'
    case 'picked':
      return 'success'
    case 'passed':
      return 'info'
    default:
      return 'info'
  }
}

const showRowActions = (value: string): boolean => value !== 'picked' && value !== 'passed'

/**
 * 过号时间兜底：后端 CallRecord 未序列化 passTime 时，用记录更新时间代替（过号必然触发更新）
 */
const resolvePassTime = (row: HistoryRow): string | undefined =>
  row.passTime ?? (row.status === 'passed' ? row.updateTime : undefined)

/**
 * 拉取历史分页数据（后端 data 是 { records, total, current, size }，兼容老数组返回）
 * @param allowPageFallback - 当前页被操作清空时是否回退一页重试
 */
const fetchHistory = async (allowPageFallback = true): Promise<void> => {
  loading.value = true
  try {
    const params: Record<string, string | number> = {
      range: range.value,
      page: page.value,
      size: size.value
    }
    const trimmedKeyword = keyword.value.trim()
    if (trimmedKeyword) {
      params.keyword = trimmedKeyword
    }
    if (status.value) {
      params.status = status.value
    }

    const res = (await request.get('/v1/call-number/history', {
      params
    })) as unknown as HistoryPage | HistoryRow[] | null

    const list: HistoryRow[] = Array.isArray(res)
      ? res.map(row => ({ ...row }))
      : Array.isArray(res?.records)
        ? (res.records as HistoryRow[]).map(row => ({ ...row }))
        : []
    records.value = list
    total.value = Array.isArray(res) ? list.length : Number(res?.total ?? list.length) || 0

    // 行内操作把当前页清空后自动回退，避免停在空白页
    if (allowPageFallback && list.length === 0 && total.value > 0 && page.value > 1) {
      page.value = Math.max(1, Math.min(page.value - 1, maxPage.value))
      await fetchHistory(false)
    }
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '加载叫号历史失败'))
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = (): void => {
  page.value = 1
  void fetchHistory()
}

const handleReset = (): void => {
  // range/status 的 watcher 已经会重置页码并重新请求，避免重复打接口
  const filterChanged = range.value !== 'today' || status.value !== ''
  keyword.value = ''
  range.value = 'today'
  status.value = ''
  size.value = 20
  page.value = 1
  if (!filterChanged) {
    void fetchHistory()
  }
}

const handlePageChange = (nextPage: number): void => {
  if (nextPage === page.value) {
    return
  }
  page.value = nextPage
  void fetchHistory()
}

const handleSizeChange = (nextSize: number): void => {
  if (nextSize === size.value) {
    return
  }
  size.value = nextSize
  page.value = 1
  void fetchHistory()
}

const actionLabel = (action: RowAction): string => {
  const labelMap: Record<RowAction, string> = {
    pickup: '取餐',
    pass: '过号',
    recall: '重叫'
  }
  return labelMap[action]
}

/**
 * 行内操作：取餐 / 过号 / 重叫，成功后刷新当前页并通知父级刷新统计
 */
const runRowAction = async (row: HistoryRow, action: RowAction): Promise<void> => {
  if (row._busy) {
    return
  }
  row._busy = action
  try {
    const url =
      action === 'pickup'
        ? '/v1/call-number/pickup'
        : action === 'pass'
          ? '/v1/call-number/pass'
          : '/v1/call-number/recall'
    await request.post(url, { orderId: row.orderId })
    ElMessage.success(`${actionLabel(action)}成功：${row.callNumber || row.orderNumber || row.orderId}`)
    emit('refresh-stats')
    await fetchHistory()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, `${actionLabel(action)}失败`))
  } finally {
    row._busy = undefined
  }
}

// 任何筛选变化都重新请求并回到第一页
watch([range, status], () => {
  page.value = 1
  void fetchHistory()
})

onMounted(() => {
  void fetchHistory()
})

defineExpose({ refresh: (): void => void fetchHistory() })
</script>

<style scoped>
.call-history-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.keyword-input {
  width: 260px;
}

.status-select {
  width: 140px;
}

.filter-bar :deep(.el-radio-button__inner) {
  padding: 8px 14px;
  font-size: 13px;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.meta-num {
  color: var(--el-color-primary);
  font-size: 14px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.meta-divider {
  color: var(--el-border-color);
}

.meta-hint {
  margin-left: auto;
  color: var(--el-text-color-placeholder);
}

.table-wrap {
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 10px;
  overflow: hidden;
  min-height: 180px;
}

.history-table :deep(.el-table__cell) {
  padding: 6px 0;
}

.call-no {
  font-size: 20px;
  font-weight: 800;
  line-height: 1.15;
  letter-spacing: 0.5px;
  color: var(--el-color-primary);
  font-variant-numeric: tabular-nums;
}

.call-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 4px;
}

.meta-chip {
  padding: 0 5px;
  border-radius: 4px;
  font-size: 11px;
  line-height: 16px;
  color: var(--el-text-color-secondary);
  background: var(--el-fill-color-light);
}

.meta-chip.type {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.order-number {
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, monospace;
  font-size: 12px;
  color: var(--el-text-color-regular);
}

.call-count {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  font-variant-numeric: tabular-nums;
}

.call-count.repeated {
  color: var(--el-color-warning);
  font-weight: 700;
}

.time-cell {
  font-size: 12px;
  color: var(--el-text-color-regular);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.time-cell.muted {
  color: var(--el-text-color-placeholder);
}

.row-actions {
  display: flex;
  gap: 6px;
}

.row-actions :deep(.el-button) {
  margin-left: 0;
}

.no-action {
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.table-empty {
  padding: 18px 0;
}

.empty-tip {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
}
</style>
