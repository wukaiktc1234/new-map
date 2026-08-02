<script setup lang="ts">
/**
 * 采购合同管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成采购合同管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, DocumentCopy, Search, Refresh, Download, Upload } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadUserFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { purchaseContractApi, supplierApi } from '@/api/purchase'
import { purchaseContractConverter } from '@/api/purchase'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  PurchaseContractInfo,
  PurchaseContractStatus,
  PurchaseContractQueryForm,
  PurchaseContractFormData,
} from '@/types/purchase-contract'
import { PurchaseContractStatusOptions } from '@/types/purchase-contract'
import type { SupplierInfo } from '@/types/purchase-supplier'

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
  align?: string
}

/** 表单数据类型（扩展 PurchaseContractFormData，添加前端独有字段） */
type ContractFormState = Partial<PurchaseContractFormData> & {
  contractNo: string
  contractType: string
  signDate: string
  paymentMethod: string
  settlementMethod: string
  contractContent: string
  templateId: string
  customTemplateName: string
  attachments: UploadUserFile[]
}

/** 合同模板数据（占位，等待后端合同模板 API） */
const contractTemplates: Record<string, string> = {}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<PurchaseContractInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const detailData = ref<PurchaseContractInfo | null>(null)
const detailActiveTab = ref('basic')

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref<PurchaseContractQueryForm & { keyword?: string; supplierId?: string; contractType?: string }>({
  contractNo: '',
  status: null,
  supplierId: '',
  keyword: '',
  contractType: '',
})

// 日期范围
const dateRange = ref<[string, string] | null>(null)

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<PurchaseContractInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: PurchaseContractQueryForm & { page?: number; size?: number } = {
        ...params,
      }
      if (dateRange.value) {
        queryParams.startDate = dateRange.value[0]
        queryParams.endDate = dateRange.value[1]
      }
      return purchaseContractApi.getList(queryParams)
    },
  } as unknown as CrudApi<PurchaseContractInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<ContractFormState>({
  contractNo: '',
  contractName: '',
  supplierId: '',
  supplierName: '',
  contractType: '',
  totalAmount: 0,
  signDate: '',
  startDate: '',
  endDate: '',
  paymentMethod: '',
  settlementMethod: '',
  paymentTerms: '',
  deliveryTerms: '',
  contractContent: '',
  remark: '',
  templateId: '',
  customTemplateName: '',
  attachments: [],
})

// 供应商选项
const supplierOptions = ref<SupplierInfo[]>([])
const selectedSupplier = ref<SupplierInfo | null>(null)

// 合同类型选项
const contractTypeOptions = [
  { label: '原材料采购合同', value: 'raw_material' },
  { label: '辅料采购合同', value: 'auxiliary' },
  { label: '设备采购合同', value: 'equipment' },
  { label: '服务采购合同', value: 'service' },
  { label: '其他合同', value: 'other' },
]

// 付款方式选项
const paymentMethodOptions = [
  { label: '银行转账', value: 'bank_transfer' },
  { label: '承兑汇票', value: 'acceptance' },
  { label: '现金', value: 'cash' },
  { label: '其他', value: 'other' },
]

// 结算方式选项
const settlementMethodOptions = [
  { label: '月结', value: 'monthly' },
  { label: '现结', value: 'immediate' },
  { label: '预付', value: 'prepaid' },
  { label: '货到付款', value: 'cod' },
]

// ==================== 计算属性 ====================

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(item => item.status === 'active').length,
  completed: tableData.value.filter(item => item.status === 'terminated').length,
  expiringSoon: tableData.value.filter(item => isExpiringSoon(item.endDate)).length,
}))

// 表格列定义
const columns = computed<ColumnDef[]>(() => [
  { prop: 'contractNo', label: '合同编号', minWidth: 140, slot: 'contractNo' },
  { prop: 'contractName', label: '合同名称', minWidth: 180, slot: 'contractName' },
  { prop: 'supplierName', label: '供应商', minWidth: 150, slot: 'supplierName' },
  { prop: 'contractType', label: '合同类型', minWidth: 120, slot: 'contractType' },
  { prop: 'totalAmount', label: '合同金额(元)', minWidth: 130, slot: 'totalAmount', align: 'right' },
  { prop: 'signDate', label: '签订日期', minWidth: 120, slot: 'signDate' },
  { prop: 'endDate', label: '到期日期', minWidth: 120, slot: 'endDate' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 240, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  contractName: [
    { required: true, message: '请输入合同名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' },
  ],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  contractType: [{ required: true, message: '请选择合同类型', trigger: 'change' }],
  totalAmount: [
    { required: true, message: '请输入合同金额', trigger: 'blur' },
  ],
  signDate: [{ required: true, message: '请选择签订日期', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

// ==================== 方法 ====================

/** 加载供应商选项列表 */
async function loadSupplierOptions(): Promise<void> {
  try {
    const res = await supplierApi.getList({ status: 'active', page: 1, size: 1000 })
    supplierOptions.value = res?.records || []
  } catch {
    supplierOptions.value = []
  }
}

/** 供应商选择变更 */
function handleSupplierChange(supplierId: string): void {
  const supplier = supplierOptions.value.find(s => s.supplierId === supplierId)
  selectedSupplier.value = supplier || null
  if (supplier) {
    formData.supplierName = supplier.supplierName
  } else {
    formData.supplierName = ''
  }
}

/** 搜索 */
function handleSearch(): void {
  pagination.current = 1
  refresh()
}

/** 重置 */
function handleReset(): void {
  queryForm.value = {
    contractNo: '',
    status: null,
    supplierId: '',
    keyword: '',
    contractType: '',
  }
  dateRange.value = null
  pagination.current = 1
  refresh()
}

/** 选择变化 */
function handleSelectionChange(rows: PurchaseContractInfo[]): void {
  selectedRows.value = rows
}

/** 新建合同 */
function handleCreate(): void {
  isEdit.value = false
  selectedSupplier.value = null
  Object.assign(formData, {
    contractNo: `HT${Date.now().toString(36).toUpperCase()}`,
    contractName: '',
    supplierId: '',
    supplierName: '',
    contractType: '',
    totalAmount: 0,
    signDate: '',
    startDate: '',
    endDate: '',
    paymentMethod: '',
    settlementMethod: '',
    paymentTerms: '',
    deliveryTerms: '',
    contractContent: '',
    remark: '',
    templateId: '',
    customTemplateName: '',
    attachments: [],
  })
  dialogVisible.value = true
}

/** 从模板创建 */
function handleCreateFromTemplate(): void {
  ElMessage.info('合同模板功能待后端API支持')
}

/** 编辑合同 */
async function handleEdit(row: PurchaseContractInfo): Promise<void> {
  isEdit.value = true
  try {
    const detail = await purchaseContractApi.getById(row.contractId)
    if (detail) {
      Object.assign(formData, {
        ...detail,
        contractType: '',
        paymentMethod: '',
        settlementMethod: '',
        contractContent: detail.contractContent || '',
        templateId: '',
        customTemplateName: '',
        attachments: [],
      })
      const supplier = supplierOptions.value.find(s => s.supplierId === detail.supplierId)
      selectedSupplier.value = supplier || null
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载合同详情失败')
  }
}

/** 查看详情 */
async function handleView(row: PurchaseContractInfo): Promise<void> {
  try {
    const res = await purchaseContractApi.getById(row.contractId)
    if (res) {
      detailData.value = res
      detailActiveTab.value = 'basic'
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

/** 提交表单 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      ...formData,
    }

    if (isEdit.value && formData.contractId) {
      await purchaseContractApi.update(formData.contractId, submitData as unknown as Partial<PurchaseContractFormData>)
      ElMessage.success('更新成功')
    } else {
      await purchaseContractApi.create(submitData as unknown as PurchaseContractFormData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

/** 删除合同 */
async function handleDelete(row: PurchaseContractInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `确定要删除合同「${row.contractName}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await purchaseContractApi.delete(row.contractId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

/** 终止合同 */
async function handleTerminate(row: PurchaseContractInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(
      `终止合同「${row.contractName}」将导致合同失去法律效力，相关采购订单和结算可能受到影响。确定继续？`,
      '终止合同 - 风险提示',
      {
        confirmButtonText: '继续',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const { value: reason } = await ElMessageBox.prompt('请输入终止原因', '终止合同', {
      confirmButtonText: '确定终止',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '终止原因不能为空',
    })
    await purchaseContractApi.terminate(row.contractId, { reason })
    ElMessage.success('合同已终止')
    refresh()
  } catch {
    // 用户取消
  }
}

/** 选择合同模板 */
function onContractTemplateChange(templateId: string): void {
  if (templateId === 'tpl-custom') {
    formData.contractContent = ''
    return
  }
  if (templateId && contractTemplates[templateId]) {
    formData.contractContent = contractTemplates[templateId]
  }
}

/** 附件变更 */
function handleAttachmentChange(_file: UploadFile, fileList: UploadUserFile[]): void {
  formData.attachments = fileList
}

/** 附件下载 */
function handleDownloadAttachment(_attachment: unknown): void {
  ElMessage.info('附件下载功能待后端API支持')
}

/** 判断是否即将到期（30天内） */
function isExpiringSoon(dateStr: string): boolean {
  if (!dateStr) return false
  const diff = new Date(dateStr).getTime() - Date.now()
  return diff > 0 && diff <= 30 * 24 * 60 * 60 * 1000
}

/** 判断是否已过期 */
function isExpired(dateStr: string): boolean {
  if (!dateStr) return false
  return new Date(dateStr).getTime() < Date.now()
}

/** 获取合同类型标签 */
function getContractTypeLabel(type: string): string {
  const map: Record<string, string> = {
    raw_material: '原材料采购合同',
    auxiliary: '辅料采购合同',
    equipment: '设备采购合同',
    service: '服务采购合同',
    other: '其他合同',
  }
  return map[type] || type || '-'
}

/** 格式化日期 */
function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr
}

/** 格式化金额 */
function formatAmount(amount: number): string {
  return purchaseContractConverter.formatYuan(amount)
}

// 页面挂载时加载供应商选项
onMounted(() => {
  loadSupplierOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="采购合同" description="管理采购合同的签订、执行、归档">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新建合同
      </el-button>
      <el-button size="default" @click="handleCreateFromTemplate">
        <el-icon :size="16"><DocumentCopy /></el-icon>从模板创建
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Document" label="合同总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="执行中" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleClose" label="已完成" :value="String(statistics.completed)" color-type="info" variant="bordered" />
      <StatCard icon="Warning" label="即将到期" :value="String(statistics.expiringSoon)" color-type="warning" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.contractNo"
            placeholder="合同编号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.keyword"
            placeholder="搜索供应商或合同名称..."
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.supplierId"
            placeholder="供应商"
            clearable
            filterable
            style="width: 160px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="s in supplierOptions"
              :key="s.supplierId"
              :label="s.supplierName"
              :value="s.supplierId"
            />
          </el-select>
          <el-select
            v-model="queryForm.contractType"
            placeholder="合同类型"
            clearable
            style="width: 140px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in contractTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in PurchaseContractStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
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
        <!-- 合同编号列 -->
        <template #contractNo="{ row }">
          <span class="contract-no-text">{{ row.contractNo || '-' }}</span>
        </template>

        <!-- 合同名称列 -->
        <template #contractName="{ row }">
          <span class="contract-name-text">{{ row.contractName }}</span>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName }}</span>
        </template>

        <!-- 合同类型列 -->
        <template #contractType="{ row }">
          <span class="type-text">{{ getContractTypeLabel((row as Record<string, unknown>).contractType as string) }}</span>
        </template>

        <!-- 合同金额列 -->
        <template #totalAmount="{ row }">
          <span class="amount-text">¥{{ formatAmount(row.totalAmount) }}</span>
        </template>

        <!-- 签订日期列 -->
        <template #signDate="{ row }">
          <span class="date-text">{{ formatDate(row.signDate) }}</span>
        </template>

        <!-- 到期日期列 -->
        <template #endDate="{ row }">
          <span
            :class="{
              'date-text': true,
              'text-warning': isExpiringSoon(row.endDate),
              'text-error': isExpired(row.endDate),
            }"
          >
            {{ formatDate(row.endDate) }}
          </span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="purchaseContractConverter.toStatusTagStatus(row.status)"
            :label="purchaseContractConverter.toStatusLabel(row.status)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleView(row)">
              查看
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              link
              type="danger"
              size="default"
              @click.stop="handleTerminate(row)"
            >
              终止
            </el-button>
            <el-button
              v-if="row.status === 'draft'"
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

      <!-- 分页组件 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.pageSize"
          :total="pagination.total"
          :page-sizes="[20, 50, 100]"
          layout="total, sizes, prev, pager, next"
          @size-change="refresh"
          @current-change="refresh"
        />
      </div>
    </section>

    <!-- 新建/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑采购合同' : '新建采购合同'"
      width="1200px"
      class="fts-dialog--xl"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="合同编号" prop="contractNo">
                <el-input v-model="formData.contractNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同名称" prop="contractName">
                <el-input v-model="formData.contractName" placeholder="请输入合同名称" maxlength="100" show-word-limit />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="供应商" prop="supplierId">
                <el-select
                  v-model="formData.supplierId"
                  placeholder="请选择供应商"
                  filterable
                  style="width: 100%"
                  :teleported="false"
                  @change="handleSupplierChange"
                >
                  <el-option
                    v-for="s in supplierOptions"
                    :key="s.supplierId"
                    :label="s.supplierName"
                    :value="s.supplierId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="合同类型" prop="contractType">
                <el-select
                  v-model="formData.contractType"
                  placeholder="请选择合同类型"
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option
                    v-for="opt in contractTypeOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="签订日期" prop="signDate">
                <el-date-picker
                  v-model="formData.signDate"
                  type="date"
                  placeholder="选择签订日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="生效日期" prop="startDate">
                <el-date-picker
                  v-model="formData.startDate"
                  type="date"
                  placeholder="选择生效日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="到期日期" prop="endDate">
                <el-date-picker
                  v-model="formData.endDate"
                  type="date"
                  placeholder="选择到期日期"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 金额信息 -->
        <div class="form-section">
          <div class="section-title">金额信息</div>
          <el-row :gutter="20">
            <el-col :span="8">
              <el-form-item label="合同金额" prop="totalAmount">
                <el-input-number
                  v-model="formData.totalAmount"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="付款方式">
                <el-select
                  v-model="formData.paymentMethod"
                  placeholder="请选择付款方式"
                  clearable
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option
                    v-for="opt in paymentMethodOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="结算方式">
                <el-select
                  v-model="formData.settlementMethod"
                  placeholder="请选择结算方式"
                  clearable
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option
                    v-for="opt in settlementMethodOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="付款条款">
                <el-input v-model="formData.paymentTerms" placeholder="请输入付款条款" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="交付条款">
                <el-input v-model="formData.deliveryTerms" placeholder="请输入交付条款" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 合同内容 -->
        <div class="form-section">
          <div class="section-title">合同内容</div>
          <el-form-item label="合同模板">
            <el-select
              v-model="formData.templateId"
              placeholder="选择合同模板（可选）"
              clearable
              style="width: 100%"
              :teleported="false"
              @change="onContractTemplateChange"
            >
              <el-option label="标准采购合同" value="tpl-standard" />
              <el-option label="食品供应合同" value="tpl-food" />
              <el-option label="设备采购合同" value="tpl-equipment" />
              <el-option label="自定义模板" value="tpl-custom" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="formData.templateId === 'tpl-custom'" label="模板名称">
            <el-input v-model="formData.customTemplateName" placeholder="请输入自定义模板名称" />
          </el-form-item>
          <el-form-item label="合同条款">
            <el-input
              v-model="formData.contractContent"
              type="textarea"
              :rows="8"
              placeholder="请输入合同条款内容，或选择合同模板自动填充"
            />
          </el-form-item>
        </div>

        <!-- 附件管理 -->
        <div class="form-section">
          <div class="section-title">附件管理</div>
          <el-form-item label="附件上传">
            <el-upload
              action="#"
              :auto-upload="false"
              :limit="5"
              :on-change="handleAttachmentChange"
              :file-list="formData.attachments"
            >
              <el-button size="small">
                <el-icon><Upload /></el-icon>选择文件
              </el-button>
              <template #tip>
                <div class="el-upload__tip">支持PDF/Word/图片，单文件不超过10MB，最多5个</div>
              </template>
            </el-upload>
          </el-form-item>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="采购合同详情"
      width="1200px"
      class="fts-dialog--xl"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="detailData">
        <!-- 到期预警 -->
        <el-alert
          v-if="detailData.status === 'active' && isExpiringSoon(detailData.endDate)"
          title="合同即将到期，请及时处理续签"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: var(--fts-space-3)"
        />
        <el-alert
          v-if="isExpired(detailData.endDate)"
          title="合同已过期"
          type="error"
          show-icon
          :closable="false"
          style="margin-bottom: var(--fts-space-3)"
        />

        <el-tabs v-model="detailActiveTab">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="合同编号">{{ detailData.contractNo || '-' }}</el-descriptions-item>
              <el-descriptions-item label="合同名称">{{ detailData.contractName }}</el-descriptions-item>
              <el-descriptions-item label="供应商">{{ detailData.supplierName }}</el-descriptions-item>
              <el-descriptions-item label="合同类型">{{ getContractTypeLabel((detailData as Record<string, unknown>).contractType as string) }}</el-descriptions-item>
              <el-descriptions-item label="合同金额">
                <span class="detail-amount">¥{{ formatAmount(detailData.totalAmount) }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="状态">
                <StatusTag
                  :status="purchaseContractConverter.toStatusTagStatus(detailData.status)"
                  :label="purchaseContractConverter.toStatusLabel(detailData.status)"
                  size="small"
                />
              </el-descriptions-item>
              <el-descriptions-item label="签订日期">{{ formatDate(detailData.signDate) }}</el-descriptions-item>
              <el-descriptions-item label="生效日期">{{ formatDate(detailData.startDate) }}</el-descriptions-item>
              <el-descriptions-item label="到期日期">{{ formatDate(detailData.endDate) }}</el-descriptions-item>
              <el-descriptions-item label="付款条款">{{ detailData.paymentTerms || '-' }}</el-descriptions-item>
              <el-descriptions-item label="交付条款">{{ detailData.deliveryTerms || '-' }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ detailData.createTime || '-' }}</el-descriptions-item>
              <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
            </el-descriptions>
          </el-tab-pane>

          <!-- 合同内容 -->
          <el-tab-pane label="合同内容" name="content">
            <div class="contract-content-preview">
              <div v-if="detailData.contractContent" class="contract-text">
                {{ detailData.contractContent }}
              </div>
              <el-empty v-else description="暂无合同内容" />
            </div>
          </el-tab-pane>

          <!-- 附件列表 -->
          <el-tab-pane label="附件列表" name="attachments">
            <el-table :data="[]" stripe size="small">
              <el-table-column prop="name" label="文件名" min-width="200" />
              <el-table-column prop="size" label="大小" width="100" />
              <el-table-column prop="uploadTime" label="上传时间" width="170" />
              <el-table-column prop="uploader" label="上传人" width="100" />
              <el-table-column label="操作" width="80">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleDownloadAttachment(row)">下载</el-button>
                </template>
              </el-table-column>
              <template #empty>
                <el-empty description="暂无附件" :image-size="60" />
              </template>
            </el-table>
          </el-tab-pane>
        </el-tabs>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailVisible = false">关闭</el-button>
        </div>
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

  // 表头底部强化分隔
  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  // 行间分隔线强化
  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

// 合同编号
.contract-no-text {
  font-family: 'Courier New', monospace;
  color: var(--fts-text-primary);
}

// 合同名称
.contract-name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

// 供应商
.supplier-text {
  color: var(--fts-text-primary);
}

// 合同类型
.type-text {
  color: var(--fts-text-secondary);
}

// 金额
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 日期
.date-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

.text-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
}

.text-error {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
}

// 详情金额
.detail-amount {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-primary);
  font-size: var(--fts-font-size-lg);
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

// 合同内容预览
.contract-content-preview {
  max-height: 500px;
  overflow-y: auto;
  padding: var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md);
}

.contract-text {
  line-height: 1.8;
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
  white-space: pre-wrap;
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
