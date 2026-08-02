<template>
  <div class="modern-page">
    <PageHeader
      title="菜品定价"
      description="管理菜品/套餐售价、成本和利润率，支持批量调价与历史记录查询"
      icon-name="Money"
    >
      <el-button
        type="primary"
        size="default"
        v-permission="'product:pricing:batch'"
        :disabled="selectedRows.length === 0"
        @click="handleBatchPrice"
      >
        <el-icon :size="16"><Plus /></el-icon>批量调价
      </el-button>
    </PageHeader>

    <el-tabs v-model="activeTab" class="page-tabs" @tab-change="handleTabChange">
      <!-- ============ Tab 1: 菜品定价 ============ -->
      <el-tab-pane label="菜品定价" name="food">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select
                v-model="foodQuery.categoryId"
                placeholder="分类"
                clearable
                style="width: 140px"
                size="default"
                @change="handleSearch"
              >
                <el-option v-for="cat in categoryOptions" :key="cat.value" :label="cat.label" :value="cat.value" />
              </el-select>
              <el-select
                v-model="foodQuery.status"
                placeholder="状态"
                clearable
                style="width: 110px"
                size="default"
                @change="handleSearch"
              >
                <el-option label="在售" value="active" />
                <el-option label="停售" value="inactive" />
                <el-option label="售罄" value="soldout" />
              </el-select>
              <el-input
                v-model="foodQuery.keyword"
                placeholder="请输入菜品名称或编码"
                clearable
                style="width: 220px"
                size="default"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
              <el-button size="default" @click="handleReset">重置</el-button>
            </div>
          </div>
        </div>

        <section class="table-section">
          <DataTable
            :data="foodTableData"
            :columns="foodColumns"
            :loading="foodLoading"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            @selection-change="handleSelectionChange"
          >
            <template #foodName="{ row }">
              <div class="food-cell">
                <div class="food-name">{{ row.foodName }}</div>
                <div class="food-code">{{ row.foodCode }}</div>
              </div>
            </template>

            <template #salePrice="{ row }">
              <span class="price-text">{{ row.salePrice }}</span>
            </template>

            <template #costPrice="{ row }">
              <span class="cost-text">{{ row.costPrice }}</span>
            </template>

            <template #profitRate="{ row }">
              <span :class="getProfitRateClass(row.profitRate)">{{ row.profitRate }}%</span>
            </template>

            <template #foodStatus="{ row }">
              <StatusTag :status="row.foodStatus" :label="getStatusLabel(row.foodStatus)" size="small" />
            </template>

            <template #actions="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'product:pricing:edit'"
                @click.stop="handleSinglePrice(row)"
              >
                调价
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click.stop="handleViewHistory(row, 'FOOD')"
              >
                记录
              </el-button>
            </template>
          </DataTable>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="foodPagination.current"
              v-model:page-size="foodPagination.pageSize"
              :total="foodPagination.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              background
              @size-change="(s: number) => handleFoodSizeChange(s)"
              @current-change="(p: number) => handleFoodPageChange(p)"
            />
          </div>
        </section>
      </el-tab-pane>

      <!-- ============ Tab 2: 套餐定价 ============ -->
      <el-tab-pane label="套餐定价" name="combo">
        <div class="advanced-search-panel">
          <div class="toolbar-row">
            <div class="toolbar-left">
              <el-select
                v-model="comboQuery.status"
                placeholder="状态"
                clearable
                style="width: 110px"
                size="default"
                @change="handleSearch"
              >
                <el-option label="启用" value="active" />
                <el-option label="停用" value="inactive" />
              </el-select>
              <el-input
                v-model="comboQuery.keyword"
                placeholder="请输入套餐名称或编码"
                clearable
                style="width: 220px"
                size="default"
                @keyup.enter="handleSearch"
                @clear="handleSearch"
              />
            </div>
            <div class="toolbar-right">
              <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
              <el-button size="default" @click="handleReset">重置</el-button>
            </div>
          </div>
        </div>

        <section class="table-section">
          <DataTable
            :data="comboTableData"
            :columns="comboColumns"
            :loading="comboLoading"
            :stripe="layoutStore.tableStriped"
            :hover="layoutStore.tableHover"
            :border="false"
            @selection-change="handleSelectionChange"
          >
            <template #comboName="{ row }">
              <div class="food-cell">
                <div class="food-name">{{ row.comboName }}</div>
                <div class="food-code">{{ row.comboCode }}</div>
              </div>
            </template>

            <template #comboPrice="{ row }">
              <span class="price-text">{{ row.comboPrice }}</span>
            </template>

            <template #totalCost="{ row }">
              <span class="cost-text">¥{{ Number(row.totalCost ?? 0).toFixed(2) }}</span>
            </template>

            <template #profitRate="{ row }">
              <span :class="getProfitRateClass(Number(row.profitRate ?? 0))">
                {{ Number(row.profitRate ?? 0).toFixed(1) }}%
              </span>
            </template>

            <template #comboStatus="{ row }">
              <StatusTag
                :status="row.comboStatus === 'active' ? 'active' : 'inactive'"
                :label="row.comboStatus === 'active' ? '启用' : '停用'"
                size="small"
              />
            </template>

            <template #actions="{ row }">
              <el-button
                link
                type="primary"
                size="small"
                v-permission="'product:pricing:edit'"
                @click.stop="handleSingleComboPrice(row)"
              >
                调价
              </el-button>
              <el-button
                link
                type="primary"
                size="small"
                @click.stop="handleViewHistory(row, 'COMBO')"
              >
                记录
              </el-button>
            </template>
          </DataTable>

          <div class="pagination-wrapper">
            <el-pagination
              v-model:current-page="comboPagination.current"
              v-model:page-size="comboPagination.pageSize"
              :total="comboPagination.total"
              :page-sizes="[20, 50, 100]"
              layout="total, sizes, prev, pager, next"
              background
              @size-change="(s: number) => handleComboSizeChange(s)"
              @current-change="(p: number) => handleComboPageChange(p)"
            />
          </div>
        </section>
      </el-tab-pane>
    </el-tabs>

    <!-- ============ 定价历史记录区 ============ -->
    <section class="history-section">
      <div class="history-header">
        <div class="history-title">
          <el-icon><Clock /></el-icon>
          <span>定价历史记录</span>
        </div>
        <div class="history-toolbar">
          <el-select
            v-model="historyQuery.productType"
            placeholder="产品类型"
            clearable
            style="width: 120px"
            size="default"
            @change="loadHistory"
          >
            <el-option label="全部" value="" />
            <el-option label="菜品" value="FOOD" />
            <el-option label="套餐" value="COMBO" />
          </el-select>
          <el-date-picker
            v-model="historyDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            @change="handleHistoryDateChange"
          />
          <el-button type="primary" size="default" @click="loadHistory">查询</el-button>
        </div>
      </div>

      <DataTable
        :data="historyData"
        :columns="historyColumns"
        :loading="historyLoading"
        :selectable="false"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
      >
        <template #productType="{ row }">
          <StatusTag
            :status="row.productType === 'COMBO' ? 'warning' : 'info'"
            :label="row.productType === 'COMBO' ? '套餐' : '菜品'"
            size="small"
          />
        </template>

        <template #oldPrice="{ row }">
          <span class="cost-text">{{ formatHistoryPrice(row.oldPrice) }}</span>
        </template>

        <template #newPrice="{ row }">
          <span :class="getNewPriceClass(row)">{{ formatHistoryPrice(row.newPrice) }}</span>
        </template>

        <template #changeRate="{ row }">
          <span :class="getChangeRateClass(row)">
            {{ getChangeRateText(row) }}
          </span>
        </template>

        <template #pricingStrategy="{ row }">
          <span class="strategy-text">{{ getStrategyLabel(row.adjustType) }}</span>
        </template>

        <template #remark="{ row }">
          <span class="remark-text">{{ row.remark || '-' }}</span>
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="historyPagination.current"
          v-model:page-size="historyPagination.pageSize"
          :total="historyPagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="(s: number) => handleHistorySizeChange(s)"
          @current-change="(p: number) => handleHistoryPageChange(p)"
        />
      </div>
    </section>

    <BatchPricingDialog ref="batchDialogRef" @success="handlePricingSuccess" />
    <PriceHistoryDialog ref="priceHistoryRef" />
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'DishPricing' })

import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Clock } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import BatchPricingDialog from './components/BatchPricingDialog.vue'
import PriceHistoryDialog from './components/PriceHistoryDialog.vue'
import { useLayoutStore } from '@/stores/layout'
import { pricingApi } from '@/api/product/pricing'
import { foodApi } from '@/api/product/food'
import { comboApi } from '@/api/product/combo'
import { categoryApi } from '@/api/product/category'
import { productDataConverter } from '@/api/product/converters'
import type {
  PricingRecord,
  PricingProductType,
  BatchPricingItem,
  CategoryOption,
  Food,
  DishComboRow,
  ProductStatus,
} from '@/types/product'

const layoutStore = useLayoutStore()
const batchDialogRef = ref<InstanceType<typeof BatchPricingDialog>>()
const priceHistoryRef = ref<InstanceType<typeof PriceHistoryDialog>>()

// ============================================================
// Tab 切换状态
// ============================================================
const activeTab = ref<'food' | 'combo'>('food')

// ============================================================
// 当前选中的产品行（用于批量调价按钮）
// ============================================================
const selectedRows = ref<Array<Food | DishComboRow>>([])

// ============================================================
// 分类选项（菜品 tab 使用）
// ============================================================
const categoryOptions = ref<CategoryOption[]>([])

// ============================================================
// 菜品查询表单与表格
// ============================================================
const foodQuery = reactive({
  keyword: '',
  categoryId: undefined as number | undefined,
  status: undefined as ProductStatus | undefined,
})
const foodTableData = ref<Food[]>([])
const foodLoading = ref(false)
// 默认分页 size 至少 20，避免数据被截断（菜品定价需展示全部菜品，含停售/售罄）
const foodPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const foodColumns: DataTableColumn[] = [
  { prop: 'foodName', label: '菜品信息', minWidth: 180, slot: 'foodName' },
  { prop: 'categoryName', label: '分类', minWidth: 110 },
  { prop: 'salePrice', label: '售价(元)', minWidth: 105, slot: 'salePrice' },
  { prop: 'costPrice', label: '成本(元)', minWidth: 105, slot: 'costPrice' },
  { prop: 'profit', label: '利润(元)', minWidth: 95 },
  { prop: 'profitRate', label: '利润率(%)', minWidth: 95, slot: 'profitRate' },
  { prop: 'foodStatus', label: '状态', minWidth: 85, slot: 'foodStatus' },
]

// ============================================================
// 套餐查询表单与表格
// ============================================================
const comboQuery = reactive({
  keyword: '',
  status: undefined as ProductStatus | undefined,
})
const comboTableData = ref<DishComboRow[]>([])
const comboLoading = ref(false)
// 默认分页 size 至少 20，与菜品 tab 保持一致
const comboPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const comboColumns: DataTableColumn[] = [
  { prop: 'comboName', label: '套餐信息', minWidth: 180, slot: 'comboName' },
  { prop: 'comboPrice', label: '售价(元)', minWidth: 105, slot: 'comboPrice' },
  { prop: 'totalCost', label: '成本(元)', minWidth: 105, slot: 'totalCost' },
  { prop: 'profitRate', label: '利润率(%)', minWidth: 95, slot: 'profitRate' },
  { prop: 'comboStatus', label: '状态', minWidth: 85, slot: 'comboStatus' },
]

// ============================================================
// 定价历史记录
// ============================================================
const historyData = ref<PricingRecord[]>([])
const historyLoading = ref(false)
const historyDateRange = ref<[string, string] | null>(null)
const historyQuery = reactive({
  productType: '' as string,
})
// 历史记录分页 size 与菜品/套餐保持一致，便于跨表对比
const historyPagination = reactive({
  current: 1,
  pageSize: 20,
  total: 0,
})

const historyColumns: DataTableColumn[] = [
  { prop: 'productType', label: '产品类型', minWidth: 90, slot: 'productType' },
  { prop: 'productName', label: '产品名称', minWidth: 150 },
  { prop: 'oldPrice', label: '旧售价(元)', minWidth: 100, slot: 'oldPrice' },
  { prop: 'newPrice', label: '新售价(元)', minWidth: 100, slot: 'newPrice' },
  { prop: 'changeRate', label: '调价幅度', minWidth: 110, slot: 'changeRate' },
  { prop: 'adjustType', label: '调价策略', minWidth: 100, slot: 'pricingStrategy' },
  { prop: 'operatorName', label: '操作人', minWidth: 90 },
  { prop: 'createTime', label: '调价时间', minWidth: 160 },
  { prop: 'remark', label: '备注', minWidth: 140, slot: 'remark' },
]

// ============================================================
// 生命周期
// ============================================================
onMounted(async () => {
  await Promise.all([loadCategories(), loadFoodList(), loadHistory()])
})

// ============================================================
// 数据加载：分类
// ============================================================
async function loadCategories(): Promise<void> {
  try {
    const res = await categoryApi.getCategoryOptions()
    categoryOptions.value = res || []
  } catch {
    /* ignore */
  }
}

// ============================================================
// 数据加载：菜品列表
// ============================================================
async function loadFoodList(): Promise<void> {
  foodLoading.value = true
  try {
    const result = await foodApi.getList({
      page: foodPagination.current,
      size: foodPagination.pageSize,
      keyword: foodQuery.keyword || undefined,
      categoryId: foodQuery.categoryId,
      status: foodQuery.status ? productDataConverter.statusToBackend(foodQuery.status) : undefined,
    })
    foodTableData.value = result.records || []
    foodPagination.total = result.total || 0
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message || '加载菜品列表失败')
    foodTableData.value = []
    foodPagination.total = 0
  } finally {
    foodLoading.value = false
  }
}

function handleFoodPageChange(page: number): void {
  foodPagination.current = page
  loadFoodList()
}

function handleFoodSizeChange(size: number): void {
  foodPagination.pageSize = size
  foodPagination.current = 1
  loadFoodList()
}

// ============================================================
// 数据加载：套餐列表
// ============================================================
async function loadComboList(): Promise<void> {
  comboLoading.value = true
  try {
    const result = await comboApi.getList({
      page: comboPagination.current,
      size: comboPagination.pageSize,
      comboName: comboQuery.keyword || undefined,
      status: comboQuery.status ? productDataConverter.statusToBackend(comboQuery.status) : undefined,
    })
    comboTableData.value = result.records || []
    comboPagination.total = result.total || 0
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message || '加载套餐列表失败')
    comboTableData.value = []
    comboPagination.total = 0
  } finally {
    comboLoading.value = false
  }
}

function handleComboPageChange(page: number): void {
  comboPagination.current = page
  loadComboList()
}

function handleComboSizeChange(size: number): void {
  comboPagination.pageSize = size
  comboPagination.current = 1
  loadComboList()
}

// ============================================================
// 数据加载：定价历史记录
// ============================================================
async function loadHistory(): Promise<void> {
  historyLoading.value = true
  try {
    const params: Parameters<typeof pricingApi.queryHistory>[0] = {
      page: historyPagination.current,
      size: historyPagination.pageSize,
    }
    if (historyQuery.productType) {
      params.productType = historyQuery.productType
    }
    if (historyDateRange.value && historyDateRange.value.length === 2) {
      params.startDate = historyDateRange.value[0]
      params.endDate = historyDateRange.value[1]
    }
    const result = await pricingApi.queryHistory(params)
    historyData.value = result.records || []
    historyPagination.total = result.total || 0
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message || '加载历史记录失败')
    historyData.value = []
    historyPagination.total = 0
  } finally {
    historyLoading.value = false
  }
}

function handleHistoryPageChange(page: number): void {
  historyPagination.current = page
  loadHistory()
}

function handleHistorySizeChange(size: number): void {
  historyPagination.pageSize = size
  historyPagination.current = 1
  loadHistory()
}

function handleHistoryDateChange(): void {
  historyPagination.current = 1
  loadHistory()
}

// ============================================================
// 交互：Tab 切换、搜索、重置
// ============================================================
function handleTabChange(name: string): void {
  // 切换 Tab 时清空选择并加载对应数据（首次进入若已加载则不重复加载）
  selectedRows.value = []
  if (name === 'food') {
    if (!foodTableData.value.length) loadFoodList()
  } else if (name === 'combo') {
    if (!comboTableData.value.length) loadComboList()
  }
}

function handleSearch(): void {
  if (activeTab.value === 'food') {
    foodPagination.current = 1
    loadFoodList()
  } else {
    comboPagination.current = 1
    loadComboList()
  }
}

function handleReset(): void {
  if (activeTab.value === 'food') {
    foodQuery.keyword = ''
    foodQuery.categoryId = undefined
    foodQuery.status = undefined
    foodPagination.current = 1
    loadFoodList()
  } else {
    comboQuery.keyword = ''
    comboQuery.status = undefined
    comboPagination.current = 1
    loadComboList()
  }
}

// ============================================================
// 交互：表格选择（用于批量调价）
// ============================================================
function handleSelectionChange(selection: Array<Food | DishComboRow>): void {
  selectedRows.value = selection
}

// ============================================================
// 交互：批量调价
// ============================================================
function handleBatchPrice(): void {
  if (!selectedRows.value.length) {
    ElMessage.warning('请先选择要调价的产品')
    return
  }
  const type: PricingProductType = activeTab.value === 'food' ? 'FOOD' : 'COMBO'
  const items: BatchPricingItem[] = selectedRows.value.map(row => {
    if (activeTab.value === 'food') {
      const food = row as Food
      return {
        productId: food.foodId,
        productName: food.foodName,
        currentPrice: food.salePrice,
        costPrice: food.costPrice,
      }
    }
    const combo = row as DishComboRow
    return {
      productId: combo.comboId,
      productName: combo.comboName,
      currentPrice: combo.comboPrice,
      // 套餐行 totalCost 为数字（元），统一转为字符串
      costPrice: String(combo.totalCost ?? 0),
    }
  })
  batchDialogRef.value?.open(items, type)
}

// ============================================================
// 交互：单条调价（复用 BatchPricingDialog）
// ============================================================
function handleSinglePrice(row: Food): void {
  const item: BatchPricingItem = {
    productId: row.foodId,
    productName: row.foodName,
    currentPrice: row.salePrice,
    costPrice: row.costPrice,
  }
  batchDialogRef.value?.open([item], 'FOOD')
}

function handleSingleComboPrice(row: DishComboRow): void {
  const item: BatchPricingItem = {
    productId: row.comboId,
    productName: row.comboName,
    currentPrice: row.comboPrice,
    costPrice: String(row.totalCost ?? 0),
  }
  batchDialogRef.value?.open([item], 'COMBO')
}

// ============================================================
// 交互：查看单产品历史记录
// ============================================================
function handleViewHistory(row: Food | DishComboRow, type: PricingProductType): void {
  const productId = type === 'FOOD' ? (row as Food).foodId : (row as DishComboRow).comboId
  const productName = type === 'FOOD' ? (row as Food).foodName : (row as DishComboRow).comboName
  priceHistoryRef.value?.open(String(productId), productName)
}

// ============================================================
// 调价成功回调：刷新当前列表与历史记录
// ============================================================
function handlePricingSuccess(): void {
  if (activeTab.value === 'food') {
    loadFoodList()
  } else {
    loadComboList()
  }
  // 重置历史记录分页并刷新
  historyPagination.current = 1
  loadHistory()
}

// ============================================================
// 工具函数
// ============================================================
function getStatusLabel(status: string): string {
  return productDataConverter.getStatusLabel(status as ProductStatus) || status
}

function getProfitRateClass(rate: number): string {
  if (rate >= 60) return 'profit-high'
  if (rate >= 35) return 'profit-normal'
  return 'profit-low'
}

/** 格式化历史记录价格显示 */
function formatHistoryPrice(price: string | number | null | undefined): string {
  if (price == null || price === '') return '0.00'
  return Number(price).toFixed(2)
}

/** 新售价样式：上涨红色，下跌绿色 */
function getNewPriceClass(row: PricingRecord): string {
  const diff = Number(row.newPrice) - Number(row.oldPrice)
  if (diff > 0) return 'price-up'
  if (diff < 0) return 'price-down'
  return 'price-flat'
}

/** 调价幅度样式 */
function getChangeRateClass(row: PricingRecord): string {
  const diff = Number(row.newPrice) - Number(row.oldPrice)
  if (diff > 0) return 'change-up'
  if (diff < 0) return 'change-down'
  return 'change-flat'
}

/** 调价幅度文本：+0.50 (5.0%)↑ / -0.30 (3.0%)↓ */
function getChangeRateText(row: PricingRecord): string {
  const oldPrice = Number(row.oldPrice)
  const newPrice = Number(row.newPrice)
  const diff = newPrice - oldPrice
  if (Math.abs(diff) < 0.005) return '-'
  const percent = oldPrice > 0 ? (diff / oldPrice) * 100 : 0
  const arrow = diff > 0 ? '↑' : '↓'
  return `${diff > 0 ? '+' : ''}${diff.toFixed(2)} (${percent.toFixed(1)}%) ${arrow}`
}

/** 调价策略中文标签 */
function getStrategyLabel(strategy: string | undefined): string {
  if (!strategy) return '-'
  const map: Record<string, string> = {
    manual: '手动调价',
    MANUAL: '手动',
    MARKUP: '加价',
    DISCOUNT: '折扣',
    ROUND: '取整',
    batch: '批量调价',
  }
  return map[strategy] || strategy
}
</script>

<style scoped lang="scss">
.food-name {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.food-code {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: 2px;
}

.food-cell {
  display: flex;
  flex-direction: column;
}

.price-text {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-success);
  font-variant-numeric: tabular-nums;
}

.cost-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.profit-high {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.profit-normal {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
}

.profit-low {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

// 历史记录区
.history-section {
  margin-top: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-6) var(--fts-space-6);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-page-radius);
}

.history-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
  padding-bottom: var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.history-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);

  .el-icon {
    color: var(--fts-primary);
  }
}

.history-toolbar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-wrap: wrap;
}

// 历史记录价格样式
.price-up {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.price-down {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
  font-variant-numeric: tabular-nums;
}

.price-flat {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.change-up {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

.change-down {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.change-flat {
  color: var(--fts-text-secondary);
}

.strategy-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

.remark-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}
</style>
