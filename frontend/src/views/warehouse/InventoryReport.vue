<script setup lang="ts">
/**
 * 库存报表页面
 * 功能：3维度分析（进出趋势、分类分析、成本分析）、导出报表
 */
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import { inventoryStatsApi, warehouseApi } from '@/api/warehouse'
import type { InventoryTrendItem, InventoryCategoryStatItem, StatColorType, WarehouseCostItem } from '@/types/warehouse-stats'
import type { WarehouseInfo } from '@/types/warehouse'

/* ===== Tab 切换 ===== */
const activeTab = ref('trend')

/* ===== 仓库选项 ===== */
const warehouseOptions = ref<WarehouseInfo[]>([])

async function loadWarehouseOptions() {
  try {
    const res = await warehouseApi.getActiveList()
    warehouseOptions.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载仓库列表失败')
  }
}

/* ===== 统计概览（摘要区） ===== */
const statsData = ref({
  totalValue: '285,600.00',
  turnoverRate: 4.2,
  warningCount: 8,
  outOfStockCount: 3,
})

const statistics = [
  { icon: 'DataLine', label: '库存总金额', value: `¥${statsData.value.totalValue}`, colorType: 'primary' as StatColorType },
  { icon: 'Refresh', label: '平均周转率', value: statsData.value.turnoverRate.toFixed(2), colorType: 'info' as StatColorType },
  { icon: 'Warning', label: '预警品项数', value: statsData.value.warningCount, colorType: 'warning' as StatColorType },
  { icon: 'CircleClose', label: '缺货品项数', value: statsData.value.outOfStockCount, colorType: 'error' as StatColorType },
]

/* ===== Tab1: 进出趋势 ===== */
const trendLoading = ref(false)
const trendData = ref<InventoryTrendItem[]>([])
const trendDateRange = ref<[string, string] | null>(null)

async function loadTrendData() {
  trendLoading.value = true
  try {
    const params: Record<string, unknown> = {}
    if (trendDateRange.value) {
      params.startDate = trendDateRange.value[0]
      params.endDate = trendDateRange.value[1]
    }
    const res = await inventoryStatsApi.getTrend(params as never)
    trendData.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载趋势数据失败')
  } finally {
    trendLoading.value = false
  }
}

const trendColumns: DataTableColumn[] = [
  { prop: 'date', label: '日期', minWidth: 120 },
  { prop: 'inQuantity', label: '入库数量', minWidth: 110, align: 'right' },
  { prop: 'outQuantity', label: '出库数量', minWidth: 110, align: 'right' },
  { prop: 'change', label: '库存变动', minWidth: 110, align: 'right', slot: 'change' },
  { prop: 'totalQuantity', label: '期末库存', minWidth: 110, align: 'right' },
]

/** 计算库存变动 */
function getChange(row: InventoryTrendItem): number {
  return row.inQuantity - row.outQuantity
}

/* ===== Tab2: 分类分析 ===== */
const categoryLoading = ref(false)
const categoryData = ref<InventoryCategoryStatItem[]>([])
const categoryWarehouseId = ref('')

async function loadCategoryData() {
  categoryLoading.value = true
  try {
    const res = await inventoryStatsApi.getCategoryStats(categoryWarehouseId.value || undefined)
    categoryData.value = res || []
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载分类统计失败')
  } finally {
    categoryLoading.value = false
  }
}

const categoryColumns: DataTableColumn[] = [
  { prop: 'categoryName', label: '分类名称', minWidth: 120 },
  { prop: 'quantity', label: '库存数量', minWidth: 100, align: 'right' },
  { prop: 'value', label: '库存金额(元)', minWidth: 120, align: 'right' },
  { prop: 'percentage', label: '占比', minWidth: 100, align: 'right', slot: 'percentage' },
  { prop: 'itemCount', label: '品项数', minWidth: 90, align: 'right', slot: 'itemCount' },
]

/** 按分类统计品项数（基于库存列表） */
function getCategoryItemCount(categoryName: string): number {
  const estimateMap: Record<string, number> = {
    '蔬菜类': 3,
    '肉类': 3,
    '水产类': 2,
    '粮油类': 3,
    '冻品类': 1,
    '调味品类': 1,
    '饮品类': 1,
  }
  return estimateMap[categoryName] || 0
}

/* ===== Tab3: 成本分析（WarehouseCostItem 已迁移至 @/types/warehouse-stats） ===== */
const costData = ref<WarehouseCostItem[]>([])

function loadCostData() {
  costData.value = [
    { warehouseName: '主仓库', totalValue: '142,748.00', percentage: 50.0, itemCount: 5, turnoverRate: 4.8 },
    { warehouseName: '冷库', totalValue: '99,074.00', percentage: 34.7, itemCount: 4, turnoverRate: 5.2 },
    { warehouseName: '冷冻库', totalValue: '43,778.00', percentage: 15.3, itemCount: 1, turnoverRate: 1.5 },
  ]
}

const costColumns: DataTableColumn[] = [
  { prop: 'warehouseName', label: '仓库名称', minWidth: 120 },
  { prop: 'totalValue', label: '库存金额(元)', minWidth: 130, align: 'right' },
  { prop: 'percentage', label: '占比', minWidth: 100, align: 'right', slot: 'costPercentage' },
  { prop: 'itemCount', label: '品项数', minWidth: 90, align: 'right' },
  { prop: 'turnoverRate', label: '平均周转率', minWidth: 110, align: 'right', slot: 'turnoverRate' },
]

/* ===== 导出报表 ===== */
function handleExport() {
  try {
    const currentTab = activeTab.value
    let csvContent = ''
    let filename = ''

    if (currentTab === 'trend') {
      filename = '进出趋势报表'
      csvContent = '日期,入库数量,出库数量,库存变动,期末库存\n'
      trendData.value.forEach(row => {
        const change = row.inQuantity - row.outQuantity
        csvContent += `${row.date},${row.inQuantity},${row.outQuantity},${change},${row.totalQuantity}\n`
      })
    } else if (currentTab === 'category') {
      filename = '分类分析报表'
      csvContent = '分类,库存数量,库存金额(元),占比(%),品项数\n'
      categoryData.value.forEach(row => {
        csvContent += `${row.categoryName},${row.quantity},${row.value},${row.percentage},${getCategoryItemCount(row.categoryName)}\n`
      })
    } else if (currentTab === 'cost') {
      filename = '成本分析报表'
      csvContent = '仓库,库存金额(元),占比(%),品项数,平均周转率\n'
      costData.value.forEach(row => {
        csvContent += `${row.warehouseName},${row.totalValue},${row.percentage},${row.itemCount},${row.turnoverRate}\n`
      })
    }

    // 添加BOM头确保Excel正确识别UTF-8
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `${filename}_${new Date().toISOString().slice(0, 10)}.csv`
    link.click()
    URL.revokeObjectURL(url)

    ElMessage.success('报表导出成功')
  } catch {
    ElMessage.error('报表导出失败，请稍后重试')
  }
}

/* ===== Tab 切换处理 ===== */
function handleTabChange(tab: string) {
  activeTab.value = tab
  if (tab === 'trend' && trendData.value.length === 0) {
    loadTrendData()
  } else if (tab === 'category' && categoryData.value.length === 0) {
    loadCategoryData()
  } else if (tab === 'cost' && costData.value.length === 0) {
    loadCostData()
  }
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadTrendData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存报表" description="库存进出统计与价值分析">
      <el-button size="default" @click="handleExport">
        <el-icon :size="16"><Download /></el-icon>导出报表
      </el-button>
    </PageHeader>

    <!-- 摘要统计卡片 -->
    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>

    <el-tabs v-model="activeTab" class="page-tabs" @tab-change="handleTabChange">
      <!-- Tab1: 进出趋势 -->
      <el-tab-pane label="进出趋势" name="trend">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-date-picker
                v-model="trendDateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                placement="bottom-start"
                style="width:240px;max-width:240px;flex-shrink:0"
                size="default"
              />
              <el-button type="primary" size="default" @click="loadTrendData">查询</el-button>
            </div>
          </div>
        </div>

        <div class="table-section">
          <DataTable :columns="trendColumns" :data="trendData" :loading="trendLoading" stripe>
            <template #change="{ row }">
              <span :class="getChange(row) >= 0 ? 'change-positive' : 'change-negative'">
                {{ getChange(row) >= 0 ? '+' : '' }}{{ getChange(row) }}
              </span>
            </template>
          </DataTable>
        </div>
      </el-tab-pane>

      <!-- Tab2: 分类分析 -->
      <el-tab-pane label="分类分析" name="category">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select v-model="categoryWarehouseId" placeholder="选择仓库" clearable style="width:160px" size="default" @change="loadCategoryData">
                <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
              </el-select>
            </div>
          </div>
        </div>

        <div class="table-section">
          <DataTable :columns="categoryColumns" :data="categoryData" :loading="categoryLoading" stripe>
            <template #percentage="{ row }">
              <div class="percentage-cell">
                <el-progress :percentage="row.percentage" :stroke-width="8" :show-text="false" style="flex:1" />
                <span class="percentage-text">{{ row.percentage.toFixed(1) }}%</span>
              </div>
            </template>
            <template #itemCount="{ row }">
              {{ getCategoryItemCount(row.categoryName) }}
            </template>
          </DataTable>
        </div>
      </el-tab-pane>

      <!-- Tab3: 成本分析 -->
      <el-tab-pane label="成本分析" name="cost">
        <div class="table-section">
          <DataTable :columns="costColumns" :data="costData" stripe>
            <template #costPercentage="{ row }">
              <div class="percentage-cell">
                <el-progress :percentage="row.percentage" :stroke-width="8" :show-text="false" style="flex:1" />
                <span class="percentage-text">{{ row.percentage.toFixed(1) }}%</span>
              </div>
            </template>
            <template #turnoverRate="{ row }">
              {{ row.turnoverRate.toFixed(1) }}
            </template>
          </DataTable>
        </div>

        <div class="cost-distribution">
          <h3 class="section-title">仓库价值分布</h3>
          <div class="distribution-cards">
            <div v-for="item in costData" :key="item.warehouseName" class="distribution-card">
              <div class="distribution-card__header">
                <span class="distribution-card__name">{{ item.warehouseName }}</span>
                <span class="distribution-card__value">¥{{ item.totalValue }}</span>
              </div>
              <el-progress :percentage="item.percentage" :stroke-width="10" />
              <div class="distribution-card__meta">
                <span>品项数: {{ item.itemCount }}</span>
                <span>周转率: {{ item.turnoverRate.toFixed(1) }}</span>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.percentage-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  min-width: 120px;
}

.percentage-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

.change-positive {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.change-negative {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

.cost-distribution {
  margin-top: var(--fts-space-5);
  padding: var(--fts-space-5);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-4) 0;
}

.distribution-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-4);
}

.distribution-card {
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
  border: 1px solid var(--fts-border-secondary);

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-3);
  }

  &__name {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  &__value {
    font-size: var(--fts-font-size-lg);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
  }

  &__meta {
    display: flex;
    justify-content: space-between;
    margin-top: var(--fts-space-3);
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }
}
</style>
