<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Sell, Edit, Search, Camera } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { inventoryApi, warehouseApi } from '@/api/warehouse'
import { inventoryConverter } from '@/api/warehouse/converters'
import type { InventoryInfo, InventoryIncreaseForm } from '@/types/warehouse-inventory'
import type { WarehouseInfo } from '@/types/warehouse'

const router = useRouter()
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 20, total: 0 })
const inventoryList = ref<InventoryInfo[]>([])
const warehouseOptions = ref<WarehouseInfo[]>([])

/* ===== 搜索表单 ===== */
const searchForm = ref<{
  warehouseId: string
  status: string
  materialName: string
}>({
  warehouseId: '',
  status: '',
  materialName: '',
})

/** 库存状态选项 */
const statusOptions = [
  { label: '正常', value: 'normal' },
  { label: '预警', value: 'warning' },
  { label: '冻结', value: 'frozen' },
  { label: '过期', value: 'expired' },
]

/** 筛选后的库存列表 */
const filteredInventoryList = computed(() => {
  let result = inventoryList.value
  if (searchForm.value.warehouseId) {
    result = result.filter(i => i.warehouseId === searchForm.value.warehouseId)
  }
  if (searchForm.value.status) {
    result = result.filter(i => i.status === searchForm.value.status)
  }
  if (searchForm.value.materialName) {
    const keyword = searchForm.value.materialName.toLowerCase()
    result = result.filter(i => i.materialName.toLowerCase().includes(keyword))
  }
  return result
})

/** 搜索 */
function handleSearch() {
  pagination.value.current = 1
}

/** 重置搜索 */
function handleResetSearch() {
  searchForm.value = { warehouseId: '', status: '', materialName: '' }
  pagination.value.current = 1
}

/** 统计卡片数据 */
const statistics = computed(() => {
  const total = inventoryList.value.length
  const warningCount = inventoryList.value.filter(i => i.status === 'warning').length
  const normalCount = inventoryList.value.filter(i => i.status === 'normal').length
  const outOfStockCount = inventoryList.value.filter(i => i.quantity === 0).length
  return [
    { icon: 'Box', label: '库存品类', value: String(total), colorType: 'primary' as const },
    { icon: 'WarningFilled', label: '库存预警', value: String(warningCount), colorType: 'warning' as const },
    { icon: 'Check', label: '库存充足', value: String(normalCount), colorType: 'success' as const },
    { icon: 'Goods', label: '缺货产品', value: String(outOfStockCount), colorType: 'error' as const },
  ]
})

const columns = [
  { prop: 'materialName', label: '产品名称', minWidth: 150 },
  { prop: 'specification', label: '规格', minWidth: 120 },
  { prop: 'batchNo', label: '批次号', minWidth: 140 },
  { prop: 'quantity', label: '库存数量', minWidth: 90, align: 'center' as const },
  { prop: 'availableQuantity', label: '可用数量', minWidth: 90, align: 'center' as const },
  { prop: 'unit', label: '单位', minWidth: 60, align: 'center' as const },
  { prop: 'warehouseId', label: '仓库', minWidth: 100, slot: 'warehouse' },
  { prop: 'expiryDate', label: '保质期至', minWidth: 110 },
  { prop: 'status', label: '状态', minWidth: 80, slot: 'status' },
]

/** 获取仓库名称 */
function getWarehouseName(warehouseId: string): string {
  const wh = warehouseOptions.value.find(w => w.warehouseId === warehouseId)
  return wh?.warehouseName || warehouseId
}

/** 加载库存列表 */
async function loadInventory() {
  loading.value = true
  try {
    const result = await inventoryApi.getList({
      page: pagination.value.current,
      size: pagination.value.pageSize,
    })
    inventoryList.value = result.records
    pagination.value.total = result.total
  } catch {
    // mock fallback已在API层处理
  } finally {
    loading.value = false
  }
}

/** 加载仓库选项 */
async function loadWarehouses() {
  try {
    warehouseOptions.value = await warehouseApi.getActiveList()
  } catch {
    // mock fallback已在API层处理
  }
}

/** 分页变更 */
function handlePageChange(page: number) {
  pagination.value.current = page
  loadInventory()
}
function handleSizeChange(size: number) {
  pagination.value.pageSize = size
  pagination.value.current = 1
  loadInventory()
}

/** 跳转到出库页面 */
function goToOutbound() {
  router.push('/warehouse/outbound')
}

/** 跳转到调整页面 */
function goToAdjust() {
  router.push('/warehouse/adjust')
}

/* ===== 入库物料选项（来自当前库存列表） ===== */
const materialOptions = computed(() => {
  const seen = new Set<string>()
  return inventoryList.value
    .filter(inv => {
      if (seen.has(inv.materialId)) return false
      seen.add(inv.materialId)
      return true
    })
    .map(inv => ({ materialId: inv.materialId, materialName: inv.materialName }))
})

/* ===== 扫码入库 ===== */
const scanStockinVisible = ref(false)
const scanStockinBarcode = ref('')

function handleScanStockin() {
  scanStockinVisible.value = true
  scanStockinBarcode.value = ''
}

function handleScanStockinSubmit() {
  if (!scanStockinBarcode.value) return
  // 查找匹配物料
  const found = inventoryList.value.find(
    inv => inv.materialId === scanStockinBarcode.value || inv.materialName.includes(scanStockinBarcode.value)
  )
  if (found) {
    stockinForm.value.materialId = found.materialId
  } else {
    stockinForm.value.materialId = scanStockinBarcode.value
  }
  scanStockinVisible.value = false
  ElMessage.success('扫码识别成功，已自动填充物料信息')
}

// ============================================================
// 入库对话框
// ============================================================

const stockinDialogVisible = ref(false)
const stockinFormRef = ref<FormInstance>()
const stockinSubmitting = ref(false)

const stockinForm = ref<InventoryIncreaseForm>({
  materialId: '',
  warehouseId: '',
  quantity: 0,
  unitCost: '',
  batchNo: '',
  remark: '',
})

const stockinRules: FormRules = {
  materialId: [{ required: true, message: '请输入物料ID', trigger: 'blur' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入入库数量', trigger: 'blur' }],
}

/** 打开入库对话框 */
function handleStockin() {
  stockinForm.value = {
    materialId: '',
    warehouseId: '',
    quantity: 0,
    unitCost: '',
    batchNo: '',
    remark: '',
  }
  stockinDialogVisible.value = true
}

/** 快捷扫码入库：打开入库对话框后自动打开扫码 */
function handleQuickScanStockin() {
  handleStockin()
  nextTick(() => {
    handleScanStockin()
  })
}

/** 提交入库 */
async function submitStockin() {
  if (!stockinFormRef.value) return
  const valid = await stockinFormRef.value.validate().catch(() => false)
  if (!valid) return

  stockinSubmitting.value = true
  try {
    await inventoryApi.increase(stockinForm.value)
    ElMessage.success('入库成功')
    stockinDialogVisible.value = false
    loadInventory()
  } catch {
    // mock fallback已在API层处理
  } finally {
    stockinSubmitting.value = false
  }
}

onMounted(() => {
  loadInventory()
  loadWarehouses()
})
</script>

<template>
  <div class="modern-page">
    <PageHeader title="库存管理" description="查看和管理所有产品的库存状况">
      <el-button type="primary" size="default" @click="handleStockin">
        <el-icon :size="16"><Plus /></el-icon>
        库存入库
      </el-button>
      <el-button size="default" @click="handleQuickScanStockin">
        <el-icon :size="16"><Camera /></el-icon>
        扫码入库
      </el-button>
      <el-button size="default" @click="goToOutbound">
        <el-icon :size="16"><Sell /></el-icon>
        库存出库
      </el-button>
      <el-button size="default" @click="goToAdjust">
        <el-icon :size="16"><Edit /></el-icon>
        库存调整
      </el-button>
    </PageHeader>

    <section class="stats-section">
      <StatCard v-for="stat in statistics" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="stat.value" :color-type="stat.colorType" variant="bordered" />
    </section>

    <section class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="searchForm.warehouseId"
            placeholder="选择仓库"
            clearable
            style="width:150px"
            size="default"
          >
            <el-option
              v-for="wh in warehouseOptions"
              :key="wh.warehouseId"
              :label="wh.warehouseName"
              :value="wh.warehouseId"
            />
          </el-select>
          <el-select
            v-model="searchForm.status"
            placeholder="库存状态"
            clearable
            style="width:130px"
            size="default"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-input
            v-model="searchForm.materialName"
            placeholder="搜索物料名称"
            clearable
            style="width:200px"
            size="default"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleResetSearch">重置</el-button>
        </div>
      </div>
    </section>

    <section class="table-section">
      <DataTable :data="filteredInventoryList" :columns="columns" :loading="loading" stripe>
        <template #warehouse="{ row }">
          {{ getWarehouseName(row.warehouseId) }}
        </template>
        <template #status="{ row }">
          <StatusTag :status="inventoryConverter.toStatusTagStatus(row.status)" :label="inventoryConverter.toStatusLabel(row.status)" size="small" variant="light" />
        </template>
      </DataTable>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <!-- 入库对话框 -->
    <el-dialog v-model="stockinDialogVisible" title="库存入库" width="680px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="stockinFormRef" :model="stockinForm" :rules="stockinRules" label-width="90px">
        <el-form-item label="物料ID" prop="materialId">
          <el-select
            v-model="stockinForm.materialId"
            filterable
            allow-create
            default-first-option
            placeholder="搜索或输入物料ID"
            :teleported="false"
            style="width: 100%"
          >
            <el-option v-for="mat in materialOptions" :key="mat.materialId" :label="`${mat.materialName} (${mat.materialId})`" :value="mat.materialId" />
          </el-select>
        </el-form-item>
        <el-form-item label="扫码入库">
          <el-button size="small" type="success" @click="handleScanStockin">
            <el-icon :size="14"><Camera /></el-icon>
            扫码识别物料
          </el-button>
        </el-form-item>
        <el-form-item label="入库仓库" prop="warehouseId">
          <el-select v-model="stockinForm.warehouseId" placeholder="请选择仓库" :teleported="false" style="width: 100%">
            <el-option v-for="wh in warehouseOptions" :key="wh.warehouseId" :label="wh.warehouseName" :value="wh.warehouseId" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库数量" prop="quantity">
          <el-input-number v-model="stockinForm.quantity" :min="1" :max="99999" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价(元)" prop="unitCost">
          <el-input v-model="stockinForm.unitCost" placeholder="请输入单价" />
        </el-form-item>
        <el-form-item label="批次号" prop="batchNo">
          <el-input v-model="stockinForm.batchNo" placeholder="请输入批次号" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="stockinForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stockinDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="stockinSubmitting" @click="submitStockin">确认入库</el-button>
      </template>
    </el-dialog>

    <!-- 扫码入库弹窗 -->
    <el-dialog v-model="scanStockinVisible" title="扫码识别物料" width="480px" destroy-on-close>
      <div class="scan-instructions">请使用扫码枪扫描物料条码，或手动输入条码后按回车</div>
      <el-input
        v-model="scanStockinBarcode"
        placeholder="扫描或输入条码"
        size="large"
        @keyup.enter="handleScanStockinSubmit"
      >
        <template #prefix><el-icon><Camera /></el-icon></template>
      </el-input>
      <template #footer>
        <el-button @click="scanStockinVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!scanStockinBarcode" @click="handleScanStockinSubmit">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page { min-height: 100vh; background: var(--fts-bg-page); overflow-x: clip; }
.stats-section { padding: var(--fts-space-4) 0; }
.advanced-search-panel {
  padding: var(--fts-space-4) 0;
  .toolbar-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-3);
  }
  .toolbar-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
    flex-wrap: wrap;
  }
  .toolbar-right {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }
}
.table-section { padding: 0 var(--fts-space-6) var(--fts-space-6); overflow-x: auto; background: var(--fts-bg-card); border: 1px solid var(--fts-border-primary); border-radius: var(--fts-page-radius); }
.pagination-wrapper { display: flex; justify-content: flex-end; padding: var(--fts-space-4) var(--fts-space-6); background: var(--fts-bg-card); }
.scan-instructions {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  margin-bottom: var(--fts-space-3);
}
</style>
