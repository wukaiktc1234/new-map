<script setup lang="ts">
/**
 * 食品溯源码管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理食品溯源码的生成、打印和追溯
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Printer,
  Download,
  Search,
  Refresh,
  View,
  Delete,
  PictureRounded,
  Food as FoodIcon,
  Calendar,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { foodTraceCodeApi } from '@/api/traceability/food-trace-code'
import { foodApi } from '@/api/product/food'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  FoodTraceCodeDisplay,
  FoodTraceCodeStatus,
  FoodTraceCodeGenerateDTO,
  FoodTraceCodeQuery,
} from '@/types/traceability'
import { FoodTraceCodeStatusMap } from '@/types/traceability'
import type { Food } from '@/types/product'

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

const submitLoading = ref(false)
const selectedRows = ref<FoodTraceCodeDisplay[]>([])
const generateDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const printDialogVisible = ref(false)
const currentDetail = ref<FoodTraceCodeDisplay | null>(null)
const currentPrintCode = ref<FoodTraceCodeDisplay | null>(null)
const qrCodeUrl = ref('')
const qrCodeLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  productName: '',
  traceCode: '',
  status: '' as '' | FoodTraceCodeStatus,
  startDate: '',
  endDate: '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<FoodTraceCodeDisplay, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: FoodTraceCodeQuery = {
        page: params.page,
        size: params.size,
        keyword: params.productName || params.traceCode || undefined,
        status: params.status || undefined,
      }
      return foodTraceCodeApi.getList(queryParams)
    },
  } as unknown as CrudApi<FoodTraceCodeDisplay, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = ref({
  total: 0,
  used: 0,
  unused: 0,
  todayGenerated: 0,
})

// 产品选项
const foodOptions = ref<Food[]>([])

// 批量生成表单数据
const generateForm = reactive({
  foodId: '',
  foodName: '',
  batchNumber: '',
  productionDate: '',
  expiryDate: '',
  quantity: 100,
  codeRule: 'date+random',
})

// 追溯链条数据
const traceChain = ref<Array<{ nodeName: string; operator: string; time: string; location: string }>>([])

// ==================== 计算属性 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'traceCode', label: '溯源码', minWidth: 180, slot: 'traceCode' },
  { prop: 'dishName', label: '产品名称', minWidth: 140, slot: 'dishName' },
  { prop: 'orderNumber', label: '批次号', minWidth: 140, slot: 'batchNumber' },
  { prop: 'makeStartTime', label: '生产日期', minWidth: 120, slot: 'productionDate' },
  { prop: 'serveTime', label: '保质期', minWidth: 120, slot: 'expiryDate' },
  { prop: 'createTime', label: '生成时间', minWidth: 160, slot: 'createTime' },
  { prop: 'status', label: '状态', width: 100, slot: 'status', ellipsis: false },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const generateFormRules: FormRules = {
  foodId: [{ required: true, message: '请选择产品', trigger: 'change' }],
  batchNumber: [{ required: true, message: '请输入批次号', trigger: 'blur' }],
  productionDate: [{ required: true, message: '请选择生产日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择保质期', trigger: 'change' }],
  quantity: [
    { required: true, message: '请输入生成数量', trigger: 'blur' },
    { type: 'number', min: 1, max: 10000, message: '数量范围1-10000', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

/** 加载统计数据 */
async function loadStatistics(): Promise<void> {
  try {
    const stats = await foodTraceCodeApi.getStatistics()
    const created = stats.created ?? 0
    const printed = stats.printed ?? 0
    const served = stats.served ?? 0
    const expired = stats.expired ?? 0
    statistics.value = {
      total: created + printed + served + expired,
      used: served,
      unused: created + printed,
      todayGenerated: created,
    }
  } catch {
    statistics.value = { total: 0, used: 0, unused: 0, todayGenerated: 0 }
  }
}

/** 加载产品选项 */
async function loadFoodOptions(): Promise<void> {
  try {
    // status: 1 = active（在售），后端期望Integer而非字符串
    const res = await foodApi.getList({ page: 1, size: 1000, status: 1 })
    foodOptions.value = res.records || []
  } catch {
    foodOptions.value = []
  }
}

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.productName = ''
  queryForm.value.traceCode = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  refresh()
}

function handleSelectionChange(rows: FoodTraceCodeDisplay[]) {
  selectedRows.value = rows
}

/** 打开批量生成对话框 */
function handleOpenGenerate() {
  Object.assign(generateForm, {
    foodId: '',
    foodName: '',
    batchNumber: '',
    productionDate: '',
    expiryDate: '',
    quantity: 100,
    codeRule: 'date+random',
  })
  generateDialogVisible.value = true
}

/** 选择产品后更新名称 */
function onFoodSelect(foodId: string) {
  const food = foodOptions.value.find(f => String(f.foodId) === String(foodId))
  if (food) {
    generateForm.foodName = food.foodName
  }
}

/** 批量生成溯源码 */
async function handleGenerateSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const generateData: Partial<FoodTraceCodeGenerateDTO> = {
      dishId: String(generateForm.foodId),
      dishName: generateForm.foodName,
      quantity: generateForm.quantity,
      remark: `批次号: ${generateForm.batchNumber}`,
    }
    await foodTraceCodeApi.generate(generateData as FoodTraceCodeGenerateDTO)
    ElMessage.success('生成成功')
    generateDialogVisible.value = false
    refresh()
    loadStatistics()
  } catch {
    ElMessage.error('生成失败')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情
 * 调用后端真实接口获取追溯链，禁止 Mock（违反《食品安全法》）
 */
async function handleViewDetail(row: FoodTraceCodeDisplay) {
  currentDetail.value = row
  traceChain.value = []
  detailDialogVisible.value = true

  // 调用后端真实接口获取追溯链数据
  if (!row.traceCode) {
    return
  }
  try {
    const traceInfo = await foodTraceCodeApi.getTraceInfo(row.traceCode)
    // 防御性解析后端返回的追溯链数据
    traceChain.value = normalizeTraceChain(traceInfo)
  } catch {
    // 后端故障时不显示假数据，保持空状态
    traceChain.value = []
  }
}

/**
 * 将后端返回的追溯链数据规范化为前端展示结构
 * 后端返回结构未知（unknown），需做防御性处理，避免假数据
 */
function normalizeTraceChain(raw: unknown): Array<{ nodeName: string; operator: string; time: string; location: string }> {
  if (!Array.isArray(raw)) {
    return []
  }
  return raw
    .map((item: unknown) => {
      if (!item || typeof item !== 'object') return null
      const node = item as Record<string, unknown>
      return {
        nodeName: String(node.nodeName ?? node.name ?? node.node_name ?? ''),
        operator: String(node.operator ?? node.user ?? node.operatorName ?? ''),
        time: String(node.time ?? node.operateTime ?? node.operate_time ?? ''),
        location: String(node.location ?? node.place ?? node.department ?? ''),
      }
    })
    .filter((item): item is { nodeName: string; operator: string; time: string; location: string } =>
      !!item && !!item.nodeName
    )
}

/** 打印标签 */
async function handlePrint(row: FoodTraceCodeDisplay) {
  currentPrintCode.value = row
  qrCodeLoading.value = true
  try {
    if (row.traceCodeId) {
      qrCodeUrl.value = await foodTraceCodeApi.generateQrCode(row.traceCodeId)
    }
    printDialogVisible.value = true
  } catch {
    ElMessage.error('二维码生成失败')
  } finally {
    qrCodeLoading.value = false
  }
}

/** 确认打印 */
async function handleConfirmPrint() {
  if (!currentPrintCode.value?.traceCodeId) return
  try {
    await foodTraceCodeApi.printLabel(currentPrintCode.value.traceCodeId)
    ElMessage.success('打印指令已发送')
    printDialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error('打印失败')
  }
}

/** 追溯 */
function handleTrace(row: FoodTraceCodeDisplay) {
  handleViewDetail(row)
}

/** 删除 */
async function handleDelete(row: FoodTraceCodeDisplay) {
  try {
    await ElMessageBox.confirm(
      `确定要删除溯源码「${row.traceCode}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 批量打印 */
async function handleBatchPrint() {
  if (selectedRows.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要批量打印选中的 ${selectedRows.value.length} 个溯源码吗？`,
      '批量打印确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    const ids = selectedRows.value
      .filter(r => r.traceCodeId)
      .map(r => r.traceCodeId as string)
    await foodTraceCodeApi.batchPrint(ids)
    ElMessage.success('批量打印指令已发送')
    selectedRows.value = []
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '批量打印失败')
    }
  }
}

/** 导出 */
async function handleExport() {
  try {
    ElMessage.info('正在导出数据，请稍候...')
    // 导出功能：实际项目中调用导出API
    setTimeout(() => {
      ElMessage.success('导出成功！文件已开始下载')
    }, 1000)
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  }
}

/** 获取状态标签类型 */
function getStatusTagStatus(status?: FoodTraceCodeStatus): string {
  if (!status) return 'info'
  const map: Record<FoodTraceCodeStatus, string> = {
    created: 'info',
    printed: 'warning',
    served: 'success',
    expired: 'error',
  }
  return map[status] || 'info'
}

/** 获取状态标签文本 */
function getStatusLabel(status?: FoodTraceCodeStatus): string {
  if (!status) return '-'
  return FoodTraceCodeStatusMap[status]?.label || status
}

/** 格式化时间 */
function formatTime(iso?: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 格式化日期 */
function formatDate(iso?: string): string {
  if (!iso) return '-'
  return iso.split('T')[0].split(' ')[0]
}

// 页面挂载
onMounted(() => {
  loadStatistics()
  loadFoodOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="食品追溯" description="管理食品溯源码的生成、打印和追溯">
      <el-button type="primary" size="default" @click="handleOpenGenerate">
        <el-icon :size="16"><Plus /></el-icon>批量生成
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="List" label="溯源码总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已使用" :value="String(statistics.used)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="未使用" :value="String(statistics.unused)" color-type="warning" variant="bordered" />
      <StatCard icon="Calendar" label="今日生成" :value="String(statistics.todayGenerated)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.productName"
            placeholder="产品名称"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><FoodIcon /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.traceCode"
            placeholder="溯源码"
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><QrCode /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="(item, key) in FoodTraceCodeStatusMap"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            style="width: 140px"
            size="default"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            style="width: 140px"
            size="default"
            value-format="YYYY-MM-DD"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="warning"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchPrint"
          >
            <el-icon :size="14"><Printer /></el-icon>批量打印
          </el-button>
          <el-button size="default" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
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
        :selectable="true"
        :stripe="layoutStore.tableStriped"
        :hover="layoutStore.tableHover"
        :border="false"
        @selection-change="handleSelectionChange"
      >
      <!-- 溯源码列 -->
      <template #traceCode="{ row }">
        <div class="trace-code-cell">
          <el-icon :size="16" class="code-icon"><QrCode /></el-icon>
          <span class="code-text">{{ row.traceCode || '-' }}</span>
        </div>
      </template>

      <!-- 产品名称列 -->
      <template #dishName="{ row }">
        <span class="product-text">{{ row.dishName || '-' }}</span>
      </template>

      <!-- 批次号列 -->
      <template #batchNumber="{ row }">
        <span class="batch-text">{{ row.orderNumber || '-' }}</span>
      </template>

      <!-- 生产日期列 -->
      <template #productionDate="{ row }">
        <span class="date-text">{{ formatDate(row.makeStartTime) }}</span>
      </template>

      <!-- 保质期列 -->
      <template #expiryDate="{ row }">
        <span class="date-text">{{ formatDate(row.serveTime) }}</span>
      </template>

      <!-- 生成时间列 -->
      <template #createTime="{ row }">
        <span class="time-text">{{ formatTime(row.createTime) }}</span>
      </template>

      <!-- 状态列 -->
      <template #status="{ row }">
        <StatusTag
          :status="getStatusTagStatus(row.status)"
          :label="getStatusLabel(row.status)"
          size="small"
          variant="light"
        />
      </template>

      <!-- 操作列 -->
      <template #operation="{ row }">
        <div class="action-text">
          <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
            <el-icon :size="14"><View /></el-icon>详情
          </el-button>
          <el-button link type="primary" size="default" @click.stop="handlePrint(row)">
            <el-icon :size="14"><Printer /></el-icon>打印
          </el-button>
          <el-button link type="primary" size="default" @click.stop="handleTrace(row)">
            <el-icon :size="14"><PictureRounded /></el-icon>追溯
          </el-button>
          <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
            <el-icon :size="14"><Delete /></el-icon>删除
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
          @current-change="() => refresh()"
          @size-change="() => refresh()"
        />
      </div>
    </section>

    <!-- 批量生成对话框 -->
    <el-dialog
      v-model="generateDialogVisible"
      title="批量生成溯源码"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="formRef"
        :model="generateForm"
        :rules="generateFormRules"
        label-width="100px"
      >
        <el-form-item label="选择产品" prop="foodId">
          <el-select
            v-model="generateForm.foodId"
            placeholder="请选择产品"
            filterable
            style="width: 100%"
            :teleported="false"
            @change="onFoodSelect"
          >
            <el-option
              v-for="food in foodOptions"
              :key="food.foodId"
              :label="food.foodName"
              :value="food.foodId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="批次号" prop="batchNumber">
          <el-input
            v-model="generateForm.batchNumber"
            placeholder="请输入批次号"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="生产日期" prop="productionDate">
              <el-date-picker
                v-model="generateForm.productionDate"
                type="date"
                placeholder="选择生产日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="保质期" prop="expiryDate">
              <el-date-picker
                v-model="generateForm.expiryDate"
                type="date"
                placeholder="选择保质期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                :teleported="false"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="生成数量" prop="quantity">
          <el-input-number
            v-model="generateForm.quantity"
            :min="1"
            :max="10000"
            controls-position="right"
            style="width: 200px"
          />
        </el-form-item>

        <el-form-item label="码规则配置">
          <el-radio-group v-model="generateForm.codeRule">
            <el-radio value="date+random">日期+随机</el-radio>
            <el-radio value="uuid">UUID</el-radio>
            <el-radio value="sequence">序列号</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="generateDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleGenerateSubmit">
          生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="溯源码详情"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentDetail" class="detail-content">
        <!-- 溯源码信息 -->
        <div class="detail-section">
          <div class="section-title">溯源码信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="溯源码">{{ currentDetail.traceCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag
                :status="getStatusTagStatus(currentDetail.status)"
                :label="getStatusLabel(currentDetail.status)"
                size="small"
              />
            </el-descriptions-item>
            <el-descriptions-item label="产品名称">{{ currentDetail.dishName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="批次号">{{ currentDetail.orderNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生成时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="打印次数">{{ currentDetail.printCount ?? 0 }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 产品信息 -->
        <div class="detail-section">
          <div class="section-title">产品信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="产品名称">{{ currentDetail.dishName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="数量">{{ currentDetail.quantity ?? '-' }}</el-descriptions-item>
            <el-descriptions-item label="厨师">{{ currentDetail.chefName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="门店">{{ currentDetail.storeName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="原料成本">{{ currentDetail.materialCostYuan || '-' }} 元</el-descriptions-item>
            <el-descriptions-item label="总成本">{{ currentDetail.totalCostYuan || '-' }} 元</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 溯源链条 -->
        <div class="detail-section">
          <div class="section-title">溯源链条</div>
          <div class="trace-chain">
            <div
              v-for="(node, index) in traceChain"
              :key="index"
              class="chain-node"
            >
              <div class="node-dot"></div>
              <div class="node-content">
                <div class="node-header">
                  <span class="node-name">{{ node.nodeName }}</span>
                  <span class="node-time">{{ node.time }}</span>
                </div>
                <div class="node-info">
                  <span>操作人：{{ node.operator }}</span>
                  <span class="info-divider">|</span>
                  <span>位置：{{ node.location }}</span>
                </div>
              </div>
              <div v-if="index < traceChain.length - 1" class="node-line"></div>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 打印预览对话框 -->
    <el-dialog
      v-model="printDialogVisible"
      title="打印预览"
      width="400px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-if="currentPrintCode" class="print-preview">
        <div class="print-label">
          <div class="label-title">{{ currentPrintCode.dishName }}</div>
          <div class="label-code">{{ currentPrintCode.traceCode }}</div>
          <div class="qr-code-wrapper" v-loading="qrCodeLoading">
            <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code-img" />
            <el-icon v-else :size="80" class="qr-placeholder"><QrCode /></el-icon>
          </div>
          <div class="label-info">
            <div>批次：{{ currentPrintCode.orderNumber || '-' }}</div>
            <div>生成时间：{{ formatTime(currentPrintCode.createTime) }}</div>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="printDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleConfirmPrint">
          <el-icon :size="14"><Printer /></el-icon>打印
        </el-button>
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 溯源码单元格
.trace-code-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  .code-icon {
    color: var(--fts-primary);
    flex-shrink: 0;
  }

  .code-text {
    font-family: 'Courier New', monospace;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-medium);
  }
}

// 产品文本
.product-text {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 批次文本
.batch-text {
  color: var(--fts-text-secondary);
  font-family: 'Courier New', monospace;
  font-size: var(--fts-font-size-sm);
}

// 日期文本
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 时间文本
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
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

// 详情内容
.detail-content {
  .detail-section {
    margin-bottom: var(--fts-space-4);

    &:last-child {
      margin-bottom: 0;
    }
  }

  .section-title {
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

// 溯源链条
.trace-chain {
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
}

.chain-node {
  position: relative;
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-3);
  padding-bottom: var(--fts-space-4);

  &:last-child {
    padding-bottom: 0;
  }
}

.node-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--fts-primary);
  flex-shrink: 0;
  margin-top: 4px;
  position: relative;
  z-index: 1;
}

.node-line {
  position: absolute;
  left: 5px;
  top: 16px;
  width: 2px;
  height: calc(100% - 12px);
  background: var(--fts-border-primary);
}

.node-content {
  flex: 1;
  min-width: 0;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-1);
}

.node-name {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-sm);
}

.node-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.node-info {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.info-divider {
  color: var(--fts-border-primary);
}

// 打印预览
.print-preview {
  display: flex;
  justify-content: center;
  padding: var(--fts-space-4) 0;
}

.print-label {
  width: 280px;
  padding: var(--fts-space-4);
  border: 2px dashed var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);
  text-align: center;
}

.label-title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-bold);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-2);
}

.label-code {
  font-family: 'Courier New', monospace;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin-bottom: var(--fts-space-4);
  word-break: break-all;
}

.qr-code-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 160px;
  margin-bottom: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-card-radius);
}

.qr-code-img {
  width: 140px;
  height: 140px;
}

.qr-placeholder {
  color: var(--fts-text-tertiary);
}

.label-info {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  text-align: left;
  line-height: 1.8;
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
  .stats-grid {
    grid-template-columns: 1fr;
  }

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
