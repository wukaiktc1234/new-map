<script setup lang="ts">
/**
 * 物资需求提报页面（其他部门/门店统一入口）
 *
 * 【定位】需求侧单据：各部门/门店在此提交采购需求（采购申请），审批后由采购部门在
 *         「采购申请」页生成采购订单。
 * 【OA 化】申请人、所属部门由后端从登录上下文强制绑定（前端不可修改、不可伪造）。
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Delete, Check, Close, Warning, Clock, User } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { purchaseRequestApi } from '@/api/purchase/request'
import { purchaseRequestConverter } from '@/api/purchase/converters'
import { materialArchiveApi } from '@/api/purchase/archive'
import { storeInventoryApi } from '@/api/store-ops/store-inventory'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { usePermissionStore } from '@/stores/permission'
import type { PurchaseRequest, PurchaseRequestItem } from '@/types/purchase-request'

const layoutStore = useLayoutStore()
const permissionStore = usePermissionStore()

// ==================== 类型定义 ====================

interface ColumnDef {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
  align?: 'left' | 'center' | 'right'
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<PurchaseRequest[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<PurchaseRequest | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  requestNo: '',
  departmentName: '',
  createByName: '',
  status: '' as string,
  startDate: '' as string,
  endDate: '' as string,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PurchaseRequest, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      return purchaseRequestApi.getList({
        page: params.page,
        size: params.size,
        requestNo: params.requestNo || undefined,
        departmentName: params.departmentName || undefined,
        applicantName: params.createByName || undefined,
        status: params.status || undefined,
        startDate: params.startDate || undefined,
        endDate: params.endDate || undefined,
      })
    },
  } as unknown as CrudApi<PurchaseRequest, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 统计数据
const statistics = reactive({
  draft: 0,
  pending: 0,
  approved: 0,
  myRequest: 0,
})

async function loadStatistics(): Promise<void> {
  try {
    const stats = await purchaseRequestApi.getStatistics()
    statistics.draft = stats.draft
    statistics.pending = stats.pending
    statistics.approved = stats.approved
    statistics.myRequest = stats.total
  } catch {
    statistics.draft = 0
    statistics.pending = 0
    statistics.approved = 0
    statistics.myRequest = 0
  }
}

// 状态选项
const statusOptions = [
  { label: '草稿', value: 'draft' },
  { label: '待审核', value: 'pending' },
  { label: '已审批', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已完成', value: 'completed' },
]

// 紧急程度选项
const priorityOptions = [
  { label: '低', value: 'low' },
  { label: '普通', value: 'normal' },
  { label: '高', value: 'high' },
  { label: '紧急', value: 'urgent' },
]

// ==================== 物料选项（明细下拉） ====================

const materialOptions = ref<Array<{ materialId: string; materialName: string; unit: string; referencePrice: number }>>([])
/** 商品ID → 最新单价（门店库存 unit_cost，元） */
const latestPriceMap = ref(new Map<string, number>())

async function loadMaterialOptions(): Promise<void> {
  try {
    const deptId = permissionStore.userInfo?.departmentId
    const [archiveRes, inventoryRes] = await Promise.all([
      // 按申请人部门过滤（部门匹配或通用物料）；档案无部门数据的用户可见全部
      materialArchiveApi.getList({
        page: 1,
        size: 1000,
        status: 'active',
        departmentId: deptId ? String(deptId) : undefined,
      }),
      storeInventoryApi.getList({ page: 1, size: 1000 }).catch(() => null),
    ])
    materialOptions.value = (archiveRes?.records || []).map(m => ({
      materialId: m.materialId,
      materialName: m.materialName,
      unit: m.unit,
      referencePrice: m.referencePrice,
    }))
    const map = new Map<string, number>()
    for (const inv of inventoryRes?.records || []) {
      const price = Number(inv.unitCost)
      if (inv.materialId && price > 0) {
        map.set(inv.materialId, price)
      }
    }
    latestPriceMap.value = map
  } catch {
    materialOptions.value = []
  }
}

// ==================== 表单数据 ====================

interface FormItemData {
  materialId: string
  materialName: string
  materialCode: string
  specification: string
  quantity: number
  unit: string
  estimatedPrice: number
  subtotalAmount: number
  remark: string
}

interface FormDataState {
  title: string
  priority: string
  expectedDate: string
  description: string
  items: FormItemData[]
}

const formData = reactive<FormDataState>({
  title: '',
  priority: 'normal',
  expectedDate: '',
  description: '',
  items: [],
})

const formRules: FormRules = {
  title: [{ required: true, message: '请输入申请标题', trigger: 'blur' }],
}

const totalAmount = computed(() =>
  formData.items.reduce((sum, item) => sum + (item.estimatedPrice || 0) * (item.quantity || 0), 0),
)

function addFormItem(): void {
  formData.items.push({
    materialId: '',
    materialName: '',
    materialCode: '',
    specification: '',
    quantity: 1,
    unit: '',
    estimatedPrice: 0,
    subtotalAmount: 0,
    remark: '',
  })
}

function removeFormItem(index: number): void {
  formData.items.splice(index, 1)
}

function calcItemSubtotal(item: FormItemData): void {
  item.subtotalAmount = Math.round((item.estimatedPrice || 0) * (item.quantity || 0) * 100) / 100
}

/** 物料选择：从商品档案自动带出规格/单位/名称；单价取最新价（无则 0 手动填写） */
function handleMaterialChange(item: FormItemData, materialId: string): void {
  const material = materialOptions.value.find(m => m.materialId === materialId)
  if (material) {
    item.materialId = material.materialId
    item.materialName = material.materialName
    item.unit = material.unit
    item.estimatedPrice = latestPriceMap.value.get(material.materialId) ?? 0
  } else {
    item.materialId = ''
    item.materialName = materialId?.trim() || ''
    item.unit = ''
    item.estimatedPrice = 0
  }
  calcItemSubtotal(item)
}

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'requestNo', label: '申请单号', minWidth: 150, slot: 'requestNo' },
  { prop: 'title', label: '申请标题', minWidth: 180, slot: 'title', ellipsis: false },
  { prop: 'departmentName', label: '申请部门', minWidth: 120, slot: 'departmentName' },
  { prop: 'createByName', label: '申请人', minWidth: 100, slot: 'createByName' },
  { prop: 'createTime', label: '申请日期', minWidth: 150, slot: 'createTime' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status', ellipsis: false },
  { prop: 'totalAmount', label: '总金额', minWidth: 120, slot: 'totalAmount', align: 'right' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

function formatTime(iso?: string): string {
  if (!iso) return '-'
  return iso.replace('T', ' ').slice(0, 16)
}

function formatAmount(amount: number): string {
  return Number(amount || 0).toFixed(2)
}

// ==================== 页面操作 ====================

function handleSearch(): void {
  pagination.current = 1
  refresh()
}

function handleReset(): void {
  queryForm.value.requestNo = ''
  queryForm.value.departmentName = ''
  queryForm.value.createByName = ''
  queryForm.value.status = ''
  queryForm.value.startDate = ''
  queryForm.value.endDate = ''
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: PurchaseRequest[]) {
  selectedRows.value = rows
}

function handleCreate(): void {
  isEdit.value = false
  Object.assign(formData, {
    title: '',
    priority: 'normal',
    expectedDate: '',
    description: '',
    items: [],
  })
  addFormItem()
  dialogVisible.value = true
}

async function handleEdit(row: PurchaseRequest): Promise<void> {
  isEdit.value = true
  try {
    const detail = await purchaseRequestApi.getById(row.requestId)
    if (detail) {
      Object.assign(formData, {
        title: detail.title,
        priority: detail.priority || 'normal',
        expectedDate: detail.expectedDate || '',
        description: detail.description || '',
        items: (detail.items || []).map(item => ({
          materialId: item.materialId || '',
          materialName: item.materialName || '',
          materialCode: item.materialCode || '',
          specification: item.specification || '',
          quantity: item.quantity || 1,
          unit: item.unit || '',
          estimatedPrice: item.estimatedPrice || 0,
          subtotalAmount: item.subtotalAmount || 0,
          remark: item.remark || '',
        })),
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载需求详情失败')
  }
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (formData.items.length === 0) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  if (!formData.items.every(item => item.materialName?.trim())) {
    ElMessage.warning('请填写所有明细的物料名称')
    return
  }

  submitLoading.value = true
  try {
    const items = formData.items.map(item => ({
      materialId: item.materialId || undefined,
      materialName: item.materialName,
      materialCode: item.materialCode || undefined,
      specification: item.specification || undefined,
      quantity: item.quantity,
      unit: item.unit || undefined,
      estimatedPrice: item.estimatedPrice,
      remark: item.remark || undefined,
    }))
    // OA 化：申请人/部门由后端从登录上下文强制绑定，前端不传
    const payload = {
      title: formData.title,
      requestType: 'routine',
      priority: formData.priority,
      expectedDate: formData.expectedDate || undefined,
      description: formData.description || undefined,
      items,
    }
    if (isEdit.value && currentEditRequestId.value) {
      await purchaseRequestApi.update(currentEditRequestId.value, payload as never)
    } else {
      await purchaseRequestApi.create(payload as never)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '提交成功')
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    submitLoading.value = false
  }
}

const currentEditRequestId = ref('')

async function handleView(row: PurchaseRequest): Promise<void> {
  try {
    const detail = await purchaseRequestApi.getById(row.requestId)
    if (detail) {
      currentDetail.value = detail
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('加载详情失败')
  }
}

async function handleSubmitApproval(row: PurchaseRequest): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要提交申请「${row.title}」进入审批流程吗？`,
      '提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await purchaseRequestApi.submit(row.requestId)
    ElMessage.success('提交成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '提交失败')
    }
  }
}

async function handleBatchSubmit(): Promise<void> {
  if (selectedRows.value.length === 0) return
  const draftRows = selectedRows.value.filter(r => r.status === 'draft')
  if (draftRows.length === 0) {
    ElMessage.warning('请选择草稿状态的申请进行提交')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要提交选中的 ${draftRows.length} 条申请进入审批流程吗？`,
      '批量提交确认',
      {
        confirmButtonText: '确定提交',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    for (const row of draftRows) {
      await purchaseRequestApi.submit(row.requestId)
    }
    ElMessage.success(`已提交 ${draftRows.length} 条申请`)
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '批量提交失败')
    }
  }
}

async function handleDelete(row: PurchaseRequest): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除申请「${row.title}」吗？此操作不可撤销`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await purchaseRequestApi.delete(row.requestId)
    ElMessage.success('删除成功')
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

onMounted(() => {
  loadStatistics()
  loadMaterialOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="物资需求提报" description="各部门/门店提交采购需求，审批后由采购部统一安排采购">
      <el-button v-permission="'purchase:request:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增需求
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="草稿" :value="String(statistics.draft)" color-type="warning" variant="bordered" />
      <StatCard icon="Clock" label="待审核" :value="String(statistics.pending)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="已审批" :value="String(statistics.approved)" color-type="success" variant="bordered" />
      <StatCard icon="User" label="我的申请" :value="String(statistics.myRequest)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.requestNo"
            placeholder="申请单号"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.departmentName"
            placeholder="申请部门"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-input
            v-model="queryForm.createByName"
            placeholder="申请人"
            clearable
            style="width: 120px"
            size="default"
            @keyup.enter="handleSearch"
          />
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in statusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchSubmit"
          >
            批量提交
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
        <template #requestNo="{ row }">
          <span class="request-no-text">{{ row.requestNo }}</span>
        </template>

        <template #title="{ row }">
          <span class="title-text">{{ row.title }}</span>
        </template>

        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName || '-' }}</span>
        </template>

        <template #createByName="{ row }">
          <span class="applicant-text">{{ row.createByName || '-' }}</span>
        </template>

        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <template #status="{ row }">
          <StatusTag
            :status="purchaseRequestConverter.toStatusTagStatus(row.status)"
            :label="purchaseRequestConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status === 'draft' || row.status === 'rejected'"
              v-permission="'purchase:request:edit'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:request:submit'"
              link
              type="success"
              size="default"
              @click.stop="handleSubmitApproval(row)"
            >
              提交
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              v-permission="'purchase:request:delete'"
              link
              type="danger"
              size="default"
              @click.stop="handleDelete(row)"
            >
              删除
            </el-button>
          </div>
        </template>
      </DataTable>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑需求' : '新增需求'"
      width="960px"
      class="fts-dialog--lg"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请标题" prop="title">
              <el-input v-model="formData.title" placeholder="请输入申请标题" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="紧急程度">
              <el-select v-model="formData.priority" placeholder="请选择紧急程度" :teleported="false" style="width: 100%">
                <el-option
                  v-for="opt in priorityOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- OA 化：申请人/部门由后端绑定，前端只读展示 -->
        <el-alert type="info" :closable="false" show-icon class="oa-bind-tip">
          申请人：{{ permissionStore.userInfo?.fullName || permissionStore.userInfo?.username || '-' }}　所属部门：{{ permissionStore.userInfo?.departmentName || '-' }}（由系统自动带出，不可修改）
        </el-alert>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="期望到货日期">
              <el-date-picker
                v-model="formData.expectedDate"
                type="date"
                placeholder="请选择期望到货日期"
                :teleported="false"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注说明">
              <el-input v-model="formData.description" placeholder="请输入备注说明" maxlength="200" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 明细列表 -->
        <div class="form-section">
          <div class="section-title">
            <span>需求明细</span>
            <div class="section-actions">
              <el-button type="primary" size="small" :icon="Plus" @click="addFormItem">
                添加明细
              </el-button>
            </div>
          </div>

          <el-table :data="formData.items" size="small" border style="width: 100%">
            <el-table-column label="物料名称" min-width="160">
              <template #default="{ row }">
                <el-select
                  v-model="row.materialId"
                  placeholder="请选择物料（仅档案物料，缺失请先由采购部建档）"
                  size="small"
                  filterable
                  :teleported="true"
                  :popper-options="{ strategy: 'fixed' }"
                  style="width: 100%"
                  @change="(val: string) => handleMaterialChange(row, val)"
                >
                  <el-option
                    v-for="material in materialOptions"
                    :key="material.materialId"
                    :label="material.materialName"
                    :value="material.materialId"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="规格" min-width="100">
              <template #default="{ row }">
                <el-input v-model="row.specification" placeholder="规格" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80" align="center">
              <template #default="{ row }">
                <el-input v-model="row.unit" placeholder="单位" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="申请数量" width="110" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.quantity"
                  :min="1"
                  :max="99999"
                  controls-position="right"
                  size="small"
                  style="width: 100%"
                  @change="() => calcItemSubtotal(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="预估单价" width="110" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row.estimatedPrice"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  size="small"
                  style="width: 100%"
                  @change="() => calcItemSubtotal(row)"
                />
              </template>
            </el-table-column>
            <el-table-column label="小计(元)" width="100" align="right">
              <template #default="{ row }">
                <span class="subtotal-text">¥{{ row.subtotalAmount.toFixed(2) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="" width="50" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link size="small" @click="removeFormItem($index)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="!formData.items.length" class="empty-tip">
            <el-text type="info">暂未添加明细，点击上方"添加明细"按钮开始配置</el-text>
          </div>

          <!-- 金额汇总 -->
          <div class="amount-summary">
            <div class="summary-row">
              <span>明细合计：</span>
              <strong class="amount-value">¥{{ totalAmount.toFixed(2) }}</strong>
            </div>
          </div>
        </div>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '提交' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="需求详情"
      width="960px"
      class="fts-dialog--lg"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请单号">{{ currentDetail.requestNo }}</el-descriptions-item>
          <el-descriptions-item label="申请标题">{{ currentDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="申请部门">{{ currentDetail.departmentName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ currentDetail.createByName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag
              :status="purchaseRequestConverter.toStatusTagStatus(currentDetail.status)"
              :label="purchaseRequestConverter.toStatusLabel(currentDetail.status)"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="申请日期">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
          <el-descriptions-item label="期望到货日期">{{ currentDetail.expectedDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ formatAmount(currentDetail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注说明" :span="2">{{ currentDetail.description || '-' }}</el-descriptions-item>
        </el-descriptions>

        <div class="form-section">
          <div class="section-title">
            <span>需求明细</span>
          </div>
          <el-table :data="currentDetail.items || []" size="small" border style="width: 100%" :resizable="false">
            <el-table-column type="index" label="#" width="50" align="center" />
            <el-table-column prop="materialName" label="物料名称" min-width="150" />
            <el-table-column prop="specification" label="规格" min-width="100" />
            <el-table-column prop="unit" label="单位" width="80" align="center" />
            <el-table-column prop="quantity" label="申请数量" width="100" align="right" />
            <el-table-column label="预估单价(元)" width="110" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.estimatedPrice) }}</template>
            </el-table-column>
            <el-table-column label="小计(元)" width="110" align="right">
              <template #default="{ row }">¥{{ formatAmount(row.subtotalAmount) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.oa-bind-tip {
  margin-bottom: 16px;
}

/* 与采购订单"新建订单"对话框统一：标题左侧、操作按钮右侧 */
.form-section {
  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

.amount-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-3);

  .summary-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }

  .amount-value {
    font-variant-numeric: tabular-nums;
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-semibold);
  }
}

.request-no-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.title-text {
  font-weight: var(--fts-font-weight-medium);
}

.dept-text {
  color: var(--fts-text-secondary);
}

.applicant-text {
  color: var(--fts-text-secondary);
}

.amount-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.time-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

.subtotal-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.action-text {
  display: flex;
  gap: var(--fts-space-2);
}
</style>
