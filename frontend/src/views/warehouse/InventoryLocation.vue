<script setup lang="ts">
/**
 * 库位管理页面
 * 功能：库位列表、新建/编辑、查看库存、详情弹窗、使用率可视化、批量操作
 */
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { inventoryLocationApi, warehouseApi, locationConverter, inventoryConverter, inventoryApi } from '@/api/warehouse'
import type { InventoryLocationInfo, LocationType, LocationStatus, LocationCreateForm, LocationUpdateForm } from '@/types/warehouse-location'
import type { InventoryInfo } from '@/types/warehouse-inventory'
import type { WarehouseInfo } from '@/types/warehouse'
import type { StatColorType } from '@/types/warehouse-stats'

const { loading, pagination, handleSearch: stdSearch, handleReset: stdReset } = useStandardPage({
  onSearch: loadData,
  onReset: () => { resetSearchForm(); loadData() },
})

/* ===== 搜索表单 ===== */
const searchForm = ref<{
  warehouseId: string
  locationType: LocationType | ''
  keyword: string
}>({
  warehouseId: '',
  locationType: '',
  keyword: '',
})

function resetSearchForm() {
  searchForm.value = { warehouseId: '', locationType: '', keyword: '' }
}

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

/* ===== 库位类型选项 ===== */
const locationTypeOptions: { label: string; value: LocationType }[] = [
  { label: '货架', value: 'shelf' },
  { label: '地面', value: 'floor' },
  { label: '冷藏区', value: 'cold_storage' },
  { label: '冷冻区', value: 'freezer' },
]

/* ===== 仓库容量摘要 ===== */
const capacityStats = computed(() => {
  const allLocations = tableData.value
  const total = allLocations.length
  const used = allLocations.filter(l => l.usageRate > 0).length
  const free = total - used
  const avgRate = total > 0 ? allLocations.reduce((sum, l) => sum + l.usageRate, 0) / total : 0
  return [
    { icon: 'Grid', label: '总库位数', value: total, colorType: 'primary' as StatColorType },
    { icon: 'Box', label: '已用库位', value: used, colorType: 'success' as StatColorType },
    { icon: 'Files', label: '空闲库位', value: free, colorType: 'info' as StatColorType },
    { icon: 'DataLine', label: '平均使用率', value: `${avgRate.toFixed(1)}%`, colorType: 'warning' as StatColorType },
  ]
})

/* ===== 存放品项数计算 ===== */
// 通过 inventoryApi.getByLocationId 异步加载，失败时由 API 内部回退到 Mock
const locationItemCountMap = ref<Record<string, number>>({})

function getLocationItemCount(locationId: string): number {
  return locationItemCountMap.value[locationId] ?? 0
}

/** 批量加载当前页库位的存放品项数 */
async function loadLocationItemCounts() {
  const counts: Record<string, number> = {}
  await Promise.all(
    tableData.value.map(async (loc) => {
      try {
        const items = await inventoryApi.getByLocationId(loc.locationId)
        counts[loc.locationId] = items?.length ?? 0
      } catch {
        counts[loc.locationId] = 0
      }
    })
  )
  locationItemCountMap.value = counts
}

/* ===== 使用率颜色 ===== */
function getUsageColor(rate: number): string {
  if (rate >= 80) return 'var(--fts-error)'
  if (rate >= 50) return 'var(--fts-warning)'
  return 'var(--fts-success)'
}

/* ===== 表格列 ===== */
const columns = [
  { prop: 'locationCode', label: '库位编码', minWidth: 130 },
  { prop: 'warehouseName', label: '所属仓库', minWidth: 120 },
  { prop: 'locationType', label: '库位类型', minWidth: 110, slot: 'locationType' },
  { prop: 'maxCapacity', label: '最大容量', minWidth: 90 },
  { prop: 'currentQuantity', label: '已用容量', minWidth: 90 },
  { prop: 'usageRate', label: '使用率', minWidth: 140, slot: 'usageRate' },
  { prop: 'itemCount', label: '存放品项数', minWidth: 100, slot: 'itemCount' },
  { prop: 'status', label: '状态', minWidth: 85, slot: 'status' },
]

/* ===== 表格数据 ===== */
const tableData = ref<InventoryLocationInfo[]>([])

async function loadData() {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      page: pagination.current,
      size: pagination.size,
    }
    if (searchForm.value.warehouseId) params.warehouseId = searchForm.value.warehouseId
    if (searchForm.value.locationType) params.locationType = locationConverter.toBackendType(searchForm.value.locationType as LocationType)
    if (searchForm.value.keyword) params.locationCode = searchForm.value.keyword

    const res = await inventoryLocationApi.getList(params as never)
    tableData.value = res?.records || []
    pagination.total = res?.total || 0
    // 异步加载各库位的存放品项数（不阻塞主流程）
    loadLocationItemCounts()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载库位列表失败')
  } finally {
    loading.value = false
  }
}

/* ===== 批量选择 ===== */
const selectedRows = ref<InventoryLocationInfo[]>([])

function handleSelectionChange(rows: InventoryLocationInfo[]) {
  selectedRows.value = rows
}

async function batchToggleStatus(newStatus: number) {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择库位')
    return
  }
  const action = newStatus === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确认批量${action}选中的 ${selectedRows.value.length} 个库位？`, `批量${action}`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    for (const row of selectedRows.value) {
      await inventoryLocationApi.toggleStatus(row.locationId, newStatus)
    }
    ElMessage.success(`批量${action}成功`)
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || `批量${action}失败`)
    }
  }
}

/* ===== 新建/编辑库位弹窗 ===== */
const dialogVisible = ref(false)
const dialogTitle = ref('新建库位')
const editingId = ref<string | null>(null)
const formRef = ref()
const submitLoading = ref(false)

const defaultForm = (): LocationCreateForm & { status?: LocationStatus } => ({
  warehouseId: '',
  locationCode: '',
  locationName: '',
  locationType: 'shelf',
  maxCapacity: undefined,
  remark: '',
})

const form = ref(defaultForm())

const formRules = {
  warehouseId: [{ required: true, message: '请选择所属仓库', trigger: 'change' }],
  locationCode: [{ required: true, message: '请输入库位编码', trigger: 'blur' }],
  locationName: [{ required: true, message: '请输入库位名称', trigger: 'blur' }],
  locationType: [{ required: true, message: '请选择库位类型', trigger: 'change' }],
}

function openCreateDialog() {
  editingId.value = null
  dialogTitle.value = '新建库位'
  form.value = defaultForm()
  dialogVisible.value = true
}

function openEditDialog(row: InventoryLocationInfo) {
  editingId.value = row.locationId
  dialogTitle.value = '编辑库位'
  form.value = {
    warehouseId: String(row.warehouseId),
    locationCode: row.locationCode,
    locationName: row.locationName,
    locationType: row.locationType,
    maxCapacity: row.maxCapacity,
    remark: row.remark || '',
  }
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    if (editingId.value) {
      const updateData: LocationUpdateForm = {
        locationName: form.value.locationName,
        locationType: form.value.locationType,
        maxCapacity: form.value.maxCapacity,
        remark: form.value.remark,
      }
      await inventoryLocationApi.update(editingId.value, updateData)
      ElMessage.success('更新库位成功')
    } else {
      const createData: LocationCreateForm = {
        warehouseId: form.value.warehouseId,
        locationCode: form.value.locationCode,
        locationName: form.value.locationName,
        locationType: form.value.locationType,
        maxCapacity: form.value.maxCapacity,
        remark: form.value.remark,
      }
      await inventoryLocationApi.create(createData)
      ElMessage.success('创建库位成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

/* ===== 启用/停用库位 ===== */
async function toggleStatus(row: InventoryLocationInfo) {
  const isActive = row.status === 'active'
  const action = isActive ? '停用' : '启用'
  const newStatus = isActive ? 0 : 1
  try {
    await ElMessageBox.confirm(`确认${action}库位「${row.locationCode}」？`, `${action}库位`, {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await inventoryLocationApi.toggleStatus(row.locationId, newStatus)
    ElMessage.success(`${action}成功`)
    loadData()
  } catch (error: unknown) {
    if (error instanceof Error && error.message !== 'cancel') {
      ElMessage.error(error.message || `${action}失败`)
    }
  }
}

/* ===== 查看库存弹窗 ===== */
const inventoryDialogVisible = ref(false)
const inventoryDialogLoading = ref(false)
const currentLocation = ref<InventoryLocationInfo | null>(null)
const locationInventoryList = ref<InventoryInfo[]>([])

const inventoryColumns: DataTableColumn[] = [
  { prop: 'materialName', label: '物料名称', minWidth: 130, showOverflowTooltip: true },
  { prop: 'specification', label: '规格', minWidth: 110, showOverflowTooltip: true },
  { prop: 'batchNo', label: '批次号', minWidth: 130 },
  { prop: 'quantity', label: '数量', minWidth: 80, align: 'center' },
  { prop: 'availableQuantity', label: '可用数量', minWidth: 90, align: 'center' },
  { prop: 'unitCost', label: '单位成本', minWidth: 90, align: 'right' },
  { prop: 'status', label: '状态', minWidth: 80, slot: 'invStatus' },
]

async function openInventoryDialog(row: InventoryLocationInfo) {
  currentLocation.value = row
  inventoryDialogVisible.value = true
  inventoryDialogLoading.value = true
  try {
    // 调用真实API获取库位库存，失败时由 API 内部回退到 Mock
    locationInventoryList.value = await inventoryApi.getByLocationId(row.locationId)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载库存数据失败')
    locationInventoryList.value = []
  } finally {
    inventoryDialogLoading.value = false
  }
}

/* ===== 详情弹窗 ===== */
const detailDialogVisible = ref(false)
const detailData = ref<InventoryLocationInfo | null>(null)
const detailInventoryList = ref<InventoryInfo[]>([])

/** 最近操作记录（mock数据） */
interface OperationLog {
  time: string
  type: string
  description: string
  operator: string
}

const detailOperationLogs = ref<OperationLog[]>([])

async function openDetailDialog(row: InventoryLocationInfo) {
  detailData.value = row
  detailDialogVisible.value = true
  // 调用真实API获取库位库存，失败时由 API 内部回退到 Mock
  try {
    detailInventoryList.value = await inventoryApi.getByLocationId(row.locationId)
  } catch {
    detailInventoryList.value = []
  }
  // 生成mock操作记录
  detailOperationLogs.value = [
    { time: '2026-06-15 14:30:00', type: '入库', description: `大白菜入库 50斤`, operator: '赵仓管' },
    { time: '2026-06-15 10:00:00', type: '出库', description: `大白菜出库 20斤`, operator: '刘店长' },
    { time: '2026-06-14 16:00:00', type: '入库', description: `土豆入库 30斤`, operator: '赵仓管' },
    { time: '2026-06-14 09:00:00', type: '调整', description: `盘点调整 -5斤`, operator: '赵仓管' },
    { time: '2026-06-13 15:00:00', type: '入库', description: `西红柿入库 40斤`, operator: '赵仓管' },
  ]
}

/* ===== 初始化 ===== */
onMounted(() => {
  loadWarehouseOptions()
  loadData()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库位管理" description="仓库货架、货位空间规划与使用率">
      <el-button type="primary" size="default" @click="openCreateDialog">
        <el-icon :size="16"><Plus /></el-icon>新建库位
      </el-button>
    </PageHeader>

    <!-- 仓库容量摘要 -->
    <div class="stats-section">
      <StatCard v-for="stat in capacityStats" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </div>

    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.warehouseId" placeholder="所属仓库" clearable style="width:140px" size="default">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
          <el-select v-model="searchForm.locationType" placeholder="库位类型" clearable style="width:130px" size="default">
            <el-option v-for="opt in locationTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索库位编码" clearable style="width:180px" size="default" @keyup.enter="stdSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button size="default" :disabled="selectedRows.length === 0" @click="batchToggleStatus(1)">批量启用</el-button>
          <el-button size="default" :disabled="selectedRows.length === 0" @click="batchToggleStatus(0)">批量停用</el-button>
          <el-button type="primary" size="default" @click="stdSearch">查询</el-button>
          <el-button size="default" @click="stdReset">重置</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <DataTable :columns="columns" :data="tableData" :loading="loading" stripe :actions-width="260" @selection-change="handleSelectionChange">
        <template #locationType="{ row }">
          <StatusTag :status="locationConverter.toTypeStatusTagStatus(row.locationType)" :label="locationConverter.toTypeLabel(row.locationType)" size="small" />
        </template>
        <template #usageRate="{ row }">
          <div class="usage-rate-cell">
            <el-progress :percentage="row.usageRate" :color="getUsageColor(row.usageRate)" :stroke-width="8" :show-text="false" style="flex:1" />
            <span class="usage-rate-text">{{ row.usageRate.toFixed(1) }}%</span>
          </div>
        </template>
        <template #itemCount="{ row }">
          {{ getLocationItemCount(row.locationId) }}
        </template>
        <template #status="{ row }">
          <StatusTag :status="locationConverter.toStatusTagStatus(row.status)" :label="locationConverter.toStatusLabel(row.status)" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="openDetailDialog(row)">详情</el-button>
          <el-button link type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
          <el-button link type="primary" size="small" @click="openInventoryDialog(row)">查看库存</el-button>
          <el-button link :type="row.status === 'active' ? 'danger' : 'success'" size="small" @click="toggleStatus(row)">
            {{ row.status === 'active' ? '停用' : '启用' }}
          </el-button>
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

    <!-- 新建/编辑库位弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="680px" class="fts-dialog--md" destroy-on-close lock-scroll="false">
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="90px">
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="form.warehouseId" placeholder="请选择所属仓库" :teleported="false" style="width:100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位编码" prop="locationCode">
          <el-input v-model="form.locationCode" placeholder="请输入库位编码" :disabled="!!editingId" />
        </el-form-item>
        <el-form-item label="库位名称" prop="locationName">
          <el-input v-model="form.locationName" placeholder="请输入库位名称" />
        </el-form-item>
        <el-form-item label="库位类型" prop="locationType">
          <el-select v-model="form.locationType" placeholder="请选择库位类型" :teleported="false" style="width:100%">
            <el-option v-for="opt in locationTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="最大容量" prop="maxCapacity">
          <el-input-number v-model="form.maxCapacity" :min="0" :precision="0" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看库存弹窗 -->
    <el-dialog v-model="inventoryDialogVisible" title="库位库存" width="960px" class="fts-dialog--lg" destroy-on-close lock-scroll="false">
      <template v-if="currentLocation">
        <div class="location-info-header">
          <el-descriptions :column="3" border size="small">
            <el-descriptions-item label="库位编码">{{ currentLocation.locationCode }}</el-descriptions-item>
            <el-descriptions-item label="库位名称">{{ currentLocation.locationName }}</el-descriptions-item>
            <el-descriptions-item label="库位类型">
              <StatusTag :status="locationConverter.toTypeStatusTagStatus(currentLocation.locationType)" :label="locationConverter.toTypeLabel(currentLocation.locationType)" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="所属仓库">{{ currentLocation.warehouseName }}</el-descriptions-item>
            <el-descriptions-item label="使用率">
              <div class="usage-rate-cell">
                <el-progress :percentage="currentLocation.usageRate" :color="getUsageColor(currentLocation.usageRate)" :stroke-width="8" :show-text="false" style="flex:1" />
                <span class="usage-rate-text">{{ currentLocation.usageRate.toFixed(1) }}%</span>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="容量">{{ currentLocation.currentQuantity }} / {{ currentLocation.maxCapacity }}</el-descriptions-item>
          </el-descriptions>
        </div>
        <div class="dialog-table-section">
          <DataTable :columns="inventoryColumns" :data="locationInventoryList" :loading="inventoryDialogLoading" stripe size="small">
            <template #invStatus="{ row }">
              <StatusTag :status="inventoryConverter.toStatusTagStatus(row.status)" :label="inventoryConverter.toStatusLabel(row.status)" size="small" />
            </template>
          </DataTable>
        </div>
        <div v-if="locationInventoryList.length === 0 && !inventoryDialogLoading" class="empty-hint">
          该库位暂无库存记录
        </div>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailDialogVisible" title="库位详情" width="1200px" class="fts-dialog--xl" destroy-on-close lock-scroll="false">
      <template v-if="detailData">
        <el-descriptions :column="3" border size="default">
          <el-descriptions-item label="库位编码">{{ detailData.locationCode }}</el-descriptions-item>
          <el-descriptions-item label="库位名称">{{ detailData.locationName }}</el-descriptions-item>
          <el-descriptions-item label="库位类型">
            <StatusTag :status="locationConverter.toTypeStatusTagStatus(detailData.locationType)" :label="locationConverter.toTypeLabel(detailData.locationType)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="所属仓库">{{ detailData.warehouseName }}</el-descriptions-item>
          <el-descriptions-item label="最大容量">{{ detailData.maxCapacity }}</el-descriptions-item>
          <el-descriptions-item label="已用容量">{{ detailData.currentQuantity }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="locationConverter.toStatusTagStatus(detailData.status)" :label="locationConverter.toStatusLabel(detailData.status)" size="small" />
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ detailData.updateTime }}</el-descriptions-item>
          <el-descriptions-item label="使用率" :span="3">
            <div class="usage-rate-cell">
              <el-progress :percentage="detailData.usageRate" :color="getUsageColor(detailData.usageRate)" :stroke-width="10" :show-text="false" style="flex:1" />
              <span class="usage-rate-text">{{ detailData.usageRate.toFixed(1) }}%</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.remark" label="备注" :span="3">{{ detailData.remark }}</el-descriptions-item>
        </el-descriptions>

        <!-- 当前库存 -->
        <div class="detail-section">
          <h4 class="detail-section__title">当前库存</h4>
          <DataTable :columns="inventoryColumns" :data="detailInventoryList" stripe size="small">
            <template #invStatus="{ row }">
              <StatusTag :status="inventoryConverter.toStatusTagStatus(row.status)" :label="inventoryConverter.toStatusLabel(row.status)" size="small" />
            </template>
          </DataTable>
          <div v-if="detailInventoryList.length === 0" class="empty-hint">
            该库位暂无库存记录
          </div>
        </div>

        <!-- 最近操作 -->
        <div class="detail-section">
          <h4 class="detail-section__title">最近操作</h4>
          <el-table :data="detailOperationLogs" size="small" stripe>
            <el-table-column prop="time" label="时间" min-width="160" />
            <el-table-column prop="type" label="类型" min-width="80">
              <template #default="{ row }">
                <StatusTag :status="row.type === '入库' ? 'success' : row.type === '出库' ? 'primary' : 'warning'" :label="row.type" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="description" label="描述" min-width="180" />
            <el-table-column prop="operator" label="操作人" min-width="100" />
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.usage-rate-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  min-width: 120px;
}

.usage-rate-text {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  white-space: nowrap;
}

.location-info-header {
  margin-bottom: var(--fts-space-4);
}

.dialog-table-section {
  margin-top: var(--fts-space-3);
}

.empty-hint {
  text-align: center;
  padding: var(--fts-space-6) 0;
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-sm);
}

.detail-section {
  margin-top: var(--fts-space-5);

  &__title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
    margin: 0 0 var(--fts-space-3) 0;
  }
}
</style>
