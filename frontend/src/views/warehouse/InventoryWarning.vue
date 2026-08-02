<script setup lang="ts">
/**
 * 库存预警管理页面
 * 功能：预警记录列表、处理预警、手动生成预警、补货跳转
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryWarningApi, inventoryStatsApi, warehouseApi, inventoryWarningConverter } from '@/api/warehouse'
import type { InventoryWarningRecordInfo, WarningType, WarningRecordQueryForm, WarningHandleForm } from '@/types/warehouse-warning'
import type { WarehouseInfo } from '@/types/warehouse'
import type { InventoryStatsOverview } from '@/types/warehouse-stats'

const router = useRouter()
const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 数据状态 ===== */
const tableData = ref<InventoryWarningRecordInfo[]>([])
const searchForm = ref<WarningRecordQueryForm & { keyword?: string }>({
  warningType: undefined,
  warehouseId: undefined,
  materialName: '',
})

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

/* ===== 统计数据 ===== */
const statsOverview = ref<InventoryStatsOverview | null>(null)

async function loadStats() {
  try {
    const res = await inventoryStatsApi.getOverview()
    statsOverview.value = res
  } catch (error: unknown) {
    // 统计加载失败不阻塞页面
    console.error('加载预警统计失败', error)
  }
}

const statistics = computed(() => {
  const overview = statsOverview.value
  const total = overview?.warningCount ?? 0
  const lowStock = overview?.warningCount ?? 0
  const outOfStock = overview?.outOfStockCount ?? 0
  const handled = tableData.value.filter(r => r.status === 1).length
  return [
    { icon: 'Warning', label: '预警总数', value: total, colorType: 'warning' as const },
    { icon: 'DataLine', label: '低库存', value: lowStock, colorType: 'primary' as const },
    { icon: 'CircleClose', label: '已断货', value: outOfStock, colorType: 'error' as const },
    { icon: 'Check', label: '已处理', value: handled, colorType: 'success' as const },
  ]
})

/* ===== 表格列 ===== */
const columns: DataTableColumn[] = [
  { prop: 'materialName', label: '物料名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'warehouseName', label: '仓库', minWidth: 120 },
  { prop: 'currentStock', label: '当前库存', minWidth: 100, align: 'center' },
  { prop: 'threshold', label: '安全库存', minWidth: 100, align: 'center' },
  { prop: 'gap', label: '缺口', minWidth: 80, align: 'center', slot: 'gap' },
  { prop: 'warningType', label: '预警类型', minWidth: 100, slot: 'warningType' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
]

/* ===== 数据加载 ===== */
async function loadData() {
  loading.value = true
  try {
    const params: WarningRecordQueryForm = {
      warningType: searchForm.value.warningType || undefined,
      warehouseId: searchForm.value.warehouseId || undefined,
      materialName: searchForm.value.materialName || undefined,
      page: pagination.current,
      size: pagination.size,
    }
    const res = await inventoryWarningApi.getPage(params)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载预警列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 搜索/重置 ===== */
function resetSearchForm() {
  searchForm.value = { warningType: undefined, warehouseId: undefined, materialName: '' }
}

/* ===== 处理预警 ===== */
const handleDialogVisible = ref(false)
const handleForm = ref<WarningHandleForm>({ handleRemark: '' })
const handlingId = ref('')

function openHandleDialog(row: InventoryWarningRecordInfo) {
  handlingId.value = row.warningId
  handleForm.value = { handleRemark: '' }
  handleDialogVisible.value = true
}

async function submitHandle() {
  if (!handleForm.value.handleRemark?.trim()) {
    ElMessage.warning('请填写处理说明')
    return
  }
  try {
    await inventoryWarningApi.handle(handlingId.value, handleForm.value)
    ElMessage.success('预警已处理')
    handleDialogVisible.value = false
    loadData()
    loadStats()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '处理失败')
  }
}

/* ===== 手动生成预警 ===== */
async function handleGenerate() {
  try {
    await ElMessageBox.confirm('确认手动生成预警？系统将扫描所有库存数据。', '生成预警', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info',
    })
    await inventoryWarningApi.generate()
    ElMessage.success('预警生成成功')
    loadData()
    loadStats()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || '生成预警失败')
    }
  }
}

/* ===== 补货跳转 ===== */
function goToRestock(row: InventoryWarningRecordInfo) {
  router.push({ name: 'SmartRestock', query: { materialName: row.materialName } })
}

/* ===== 预警类型选项 ===== */
const warningTypeOptions: { label: string; value: WarningType }[] = [
  { label: '低库存', value: 'low_stock' },
  { label: '高库存', value: 'high_stock' },
  { label: '临期', value: 'expiring_soon' },
  { label: '过期', value: 'expired' },
]

/* ===== 缺口计算 ===== */
function getGap(row: InventoryWarningRecordInfo): number {
  return Math.max(0, row.threshold - row.currentStock)
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadStats()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存预警" description="低于安全库存的自动预警提醒">
      <el-button type="primary" size="default" @click="handleGenerate">
        <el-icon :size="16"><Refresh /></el-icon>生成预警
      </el-button>
    </PageHeader>

    <div class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.warningType" placeholder="预警类型" clearable style="width:130px" size="default">
            <el-option v-for="opt in warningTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-select v-model="searchForm.warehouseId" placeholder="仓库" clearable style="width:130px" size="default">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
          <el-input v-model="searchForm.materialName" placeholder="搜索物料名称" clearable style="width:180px" size="default" @keyup.enter="stdSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
          <el-button size="default" @click="stdReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe>
        <template #gap="{ row }">
          {{ getGap(row) }}
        </template>
        <template #warningType="{ row }">
          <StatusTag :status="inventoryWarningConverter.toTypeStatusTagStatus(row.warningType)" :label="inventoryWarningConverter.toTypeLabel(row.warningType)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag :status="inventoryWarningConverter.toStatusTagStatus(row.status)" :label="inventoryWarningConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button v-if="row.status !== 1" link type="primary" size="small" @click="openHandleDialog(row)">处理</el-button>
          <el-button link type="primary" size="small" @click="goToRestock(row)">补货</el-button>
        </template>
      </DataTable>
    </div>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="total,prev,pager,next,jumper"
        @size-change="(val: number) => { pagination.size = val; loadData() }"
        @current-change="(val: number) => { pagination.current = val; loadData() }"
      />
    </div>

    <!-- 处理预警弹窗 -->
    <el-dialog v-model="handleDialogVisible" title="处理预警" width="480px" class="fts-dialog--sm" destroy-on-close lock-scroll="false">
      <el-form label-width="80px">
        <el-form-item label="处理说明">
          <el-input v-model="handleForm.handleRemark" type="textarea" :rows="4" placeholder="请输入处理说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitHandle">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stats-section { grid-template-columns: repeat(4, 1fr); }
</style>
