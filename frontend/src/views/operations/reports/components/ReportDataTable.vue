<script setup lang="ts">
/**
 * 门店对比数据表格（管理层视角）
 *
 * 功能：
 * - 多店横向对比（营收/毛利率/客单价/人效/坪效）
 * - 异常数据红色高亮
 * - 支持排序
 * - 分页
 */
import { ref, computed } from 'vue'
import type { ChartData, StoreComparisonItem } from '../types/report'

interface Props {
  data: ChartData | null
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false
})

const sortField = ref('revenue')
const sortOrder = ref<'ascending' | 'descending'>('descending')

// 表格列定义
const columns = [
  { prop: 'storeName', label: '门店名称', width: 140, sortable: true },
  { prop: 'revenue', label: '营收', width: 120, sortable: true, align: 'right' as const },
  { prop: 'grossRate', label: '毛利率', width: 100, sortable: true, align: 'right' as const },
  { prop: 'avgCheck', label: '客单价', width: 100, sortable: true, align: 'right' as const },
  { prop: 'orderCount', label: '订单数', width: 100, sortable: true, align: 'right' as const },
  { prop: 'perCapitaEfficiency', label: '人效', width: 100, sortable: true, align: 'right' as const },
  { prop: 'perAreaEfficiency', label: '坪效', width: 100, sortable: true, align: 'right' as const }
]

// 门店数据
// TODO: 门店对比报表完整指标（毛利率/客单价/人效/坪效等）待后端实现，当前仅显示后端提供的营收数据
// 后端 storeRank 仅返回 { name, value }，其他指标字段后端未提供，置为 0，禁止使用 Math.random 伪造
const storeData = computed(() => {
  if (!props.data || !props.data.storeRank) return []
  return props.data.storeRank.map((item) => ({
    storeName: item.name,
    revenue: item.value,
    grossRate: 0,
    avgCheck: 0,
    orderCount: 0,
    perCapitaEfficiency: 0,
    perAreaEfficiency: 0,
    revenueYoy: 0,
    revenueMom: 0
  }))
})

// 异常判断
function isAbnormal(row: StoreComparisonItem & Record<string, unknown>): boolean {
  return (row.revenueYoy as number) < -10 || (row.grossRate as number) < 55
}

// 排序处理
const sortedData = computed(() => {
  const data = [...storeData.value]
  data.sort((a, b) => {
    const aVal = (a as Record<string, unknown>)[sortField.value] as number
    const bVal = (b as Record<string, unknown>)[sortField.value] as number
    return sortOrder.value === 'ascending' ? aVal - bVal : bVal - aVal
  })
  return data
})

function handleSortChange({ prop, order }: { prop: string; order: string }) {
  if (prop) {
    sortField.value = prop
    sortOrder.value = order as 'ascending' | 'descending'
  }
}

// 金额格式化
function formatMoney(val: number): string {
  return '¥' + (val / 100).toLocaleString()
}
</script>

<template>
  <div class="report-data-table">
    <div class="table-header">
      <h3 class="table-title">门店对比</h3>
      <span class="table-subtitle">共 {{ storeData.length }} 家门店</span>
    </div>

    <el-table
      :data="sortedData"
      :loading="loading"
      stripe
      size="small"
      @sort-change="handleSortChange"
      :default-sort="{ prop: 'revenue', order: 'descending' }"
    >
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :align="col.align"
        :sortable="col.sortable"
      >
        <template #default="{ row }">
          <span :class="{ 'abnormal-text': col.prop === 'revenue' && isAbnormal(row) }">
            <template v-if="col.prop === 'revenue'">
              {{ formatMoney(row.revenue) }}
            </template>
            <template v-else-if="col.prop === 'grossRate'">
              {{ row.grossRate.toFixed(1) }}%
            </template>
            <template v-else-if="col.prop === 'avgCheck'">
              {{ formatMoney(row.avgCheck) }}
            </template>
            <template v-else-if="col.prop === 'perCapitaEfficiency'">
              {{ formatMoney(row.perCapitaEfficiency) }}
            </template>
            <template v-else-if="col.prop === 'perAreaEfficiency'">
              {{ formatMoney(row.perAreaEfficiency) }}
            </template>
            <template v-else>
              {{ row[col.prop] }}
            </template>
          </span>
        </template>
      </el-table-column>

      <!-- 趋势列 -->
      <el-table-column label="环比" width="100" align="center">
        <template #default="{ row }">
          <span
            :class="{
              'trend-up': row.revenueMom > 0,
              'trend-down': row.revenueMom < 0,
              'trend-flat': row.revenueMom === 0
            }"
          >
            {{ row.revenueMom > 0 ? '↑' : row.revenueMom < 0 ? '↓' : '→' }}
            {{ Math.abs(row.revenueMom).toFixed(1) }}%
          </span>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <div v-if="!loading && storeData.length === 0" class="table-empty">
      <el-empty description="暂无门店数据" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.report-data-table {
  margin-top: var(--fts-space-4);
}

.table-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.table-title {
  margin: 0;
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.table-subtitle {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.abnormal-text {
  color: var(--fts-error);
  font-weight: 600;
}

.trend-up {
  color: var(--fts-success);
}

.trend-down {
  color: var(--fts-error);
}

.trend-flat {
  color: var(--fts-text-secondary);
}

.table-empty {
  padding: var(--fts-space-8) 0;
}

:deep(.el-table__row) {
  &.abnormal-row {
    background-color: var(--fts-error-bg, #fef2f2) !important;
  }
}
</style>
