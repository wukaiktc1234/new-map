<script setup lang="ts">
/**
 * 报表筛选栏（管理层视角 - 精简版）
 *
 * 功能：日期范围 + 门店选择 + 刷新
 */
import { computed } from 'vue'
import { RefreshRight, Filter } from '@element-plus/icons-vue'
import logger from '@/utils/logger'

/** 门店选项结构（与 useStoreOptions composable 的 StoreOption 一致） */
interface StoreOption {
  storeId: number
  storeCode: string
  storeName: string
}

interface Props {
  modelValue: {
    dateRange: [string, string]
    selectedStores: string[]
    channel: string
    compareType: string
  }
  loading?: boolean
  storeOptions: StoreOption[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: Props['modelValue']): void
  (e: 'filter-change'): void
  (e: 'refresh'): void
}>()

const filterState = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 日期快捷选项
const dateShortcuts = [
  { text: '今日', value: () => getDateRange(0) },
  { text: '昨日', value: () => getDateRange(1, 1) },
  { text: '近7天', value: () => getDateRange(6) },
  { text: '近30天', value: () => getDateRange(29) },
  { text: '本月', value: () => getMonthRange() },
  { text: '上月', value: () => getLastMonthRange() }
]

function getDateRange(daysBefore: number, endOffset = 0): [string, string] {
  const end = new Date()
  end.setDate(end.getDate() - endOffset)
  const start = new Date(end)
  start.setDate(start.getDate() - daysBefore)
  return [formatDate(start), formatDate(end)]
}

function getMonthRange(): [string, string] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth(), 1)
  return [formatDate(start), formatDate(now)]
}

function getLastMonthRange(): [string, string] {
  const now = new Date()
  const start = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  const end = new Date(now.getFullYear(), now.getMonth(), 0)
  return [formatDate(start), formatDate(end)]
}

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0]
}

function handleDateChange(): void {
  emit('filter-change')
}

function handleStoreChange(): void {
  if (filterState.value.selectedStores.length > 10) {
    filterState.value.selectedStores = filterState.value.selectedStores.slice(0, 10)
    logger.warn('FILTER', '门店选择超过10个，已自动截断')
  }
  emit('filter-change')
}

function handleRefresh(): void {
  emit('refresh')
}
</script>

<template>
  <div class="report-filter-bar">
    <div class="filter-row">
      <!-- 日期范围 -->
      <div class="filter-item">
        <span class="filter-label">时间范围</span>
        <el-date-picker
          v-model="filterState.dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          :disabled="loading"
          @change="handleDateChange"
        />
      </div>

      <!-- 门店选择 -->
      <div class="filter-item">
        <span class="filter-label">门店</span>
        <el-select
          v-model="filterState.selectedStores"
          multiple
          collapse-tags
          collapse-tags-tooltip
          placeholder="选择门店"
          :disabled="loading"
          :max-collapse-tags="3"
          @change="handleStoreChange"
        >
          <el-option
            v-for="store in storeOptions"
            :key="store.storeId"
            :label="store.storeName"
            :value="String(store.storeId)"
          />
        </el-select>
      </div>

      <!-- 刷新按钮 -->
      <div class="filter-actions">
        <el-button
          :icon="RefreshRight"
          :loading="loading"
          @click="handleRefresh"
        >
          刷新
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.report-filter-bar {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.filter-label {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: var(--fts-space-2);
}

@media (max-width: 768px) {
  .filter-row {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-item {
    width: 100%;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-end;
  }
}
</style>
