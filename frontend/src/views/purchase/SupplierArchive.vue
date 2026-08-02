<script setup lang="ts">
/**
 * 供应商档案页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理供应商信息、资质、评级的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Upload, Download, Search, Refresh, OfficeBuilding } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { supplierApi } from '@/api/purchase'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  SupplierInfo,
  SupplierFormData,
  SupplierQueryForm,
  SupplierStatus,
  SupplierType,
  SupplierLevel,
} from '@/types/purchase-supplier'
import {
  SupplierTypeOptions,
  SupplierLevelOptions,
  SupplierStatusOptions,
  SettlementMethodOptions,
  SupplyCategoryOptions,
} from '@/types/purchase-supplier'

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
const selectedRows = ref<SupplierInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const currentDetail = ref<SupplierInfo | null>(null)

const formRef = ref<FormInstance>()

// 查询表单
interface SupplierSearchForm {
  keyword: string
  contactPerson: string
  contactPhone: string
  supplierType: SupplierType | null
  status: SupplierStatus | null
}

const queryForm = ref<SupplierSearchForm>({
  keyword: '',
  contactPerson: '',
  contactPhone: '',
  supplierType: null,
  status: null,
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<SupplierInfo, SupplierSearchForm>({
  api: {
    getList: async (params: SupplierSearchForm & { page: number; size: number }) => {
      const apiParams: SupplierQueryForm & { page?: number; size?: number } = {
        keyword: params.keyword || undefined,
        status: params.status || undefined,
        page: params.page,
        size: params.size,
      }
      return supplierApi.getList(apiParams)
    },
  } as unknown as CrudApi<SupplierInfo, SupplierSearchForm>,
  queryForm,
  autoLoad: true,
})

// 表单数据
type SupplierFormState = Partial<SupplierFormData> & {
  status: SupplierStatus
}

const formData = reactive<SupplierFormState>({
  supplierCode: '',
  supplierName: '',
  shortName: '',
  unifiedSocialCode: '',
  supplierType: 'raw_material',
  supplierLevel: 'qualified',
  industry: '',
  registeredCapital: 0,
  establishDate: '',
  contactPerson: '',
  contactPhone: '',
  email: '',
  address: '',
  bankName: '',
  bankAccount: '',
  settlementMethod: 'monthly',
  paymentTerms: 30,
  supplyCategories: [],
  minOrderQuantity: 0,
  deliveryArea: '',
  remark: '',
  status: 'active',
})

// 统计数据
const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(item => item.status === 'active').length,
  pending: tableData.value.filter(item => item.supplierLevel === 'temporary').length,
  blacklist: tableData.value.filter(item => item.status === 'frozen' || item.status === 'eliminated').length,
}))

const importLoading = ref(false)
const exportLoading = ref(false)

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'supplierCode', label: '供应商编码', minWidth: 130 },
  { prop: 'supplierName', label: '供应商名称', minWidth: 180, ellipsis: true },
  { prop: 'contactPerson', label: '联系人', minWidth: 100 },
  { prop: 'contactPhone', label: '联系电话', minWidth: 120 },
  { prop: 'supplierType', label: '供应商类型', minWidth: 100, slot: 'supplierType' },
  { prop: 'supplierLevel', label: '等级', minWidth: 80, slot: 'supplierLevel' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status', ellipsis: false },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  supplierName: [
    { required: true, message: '请输入供应商名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在2到100个字符之间', trigger: 'blur' },
  ],
  supplierType: [{ required: true, message: '请选择供应商类型', trigger: 'change' }],
  supplierLevel: [{ required: true, message: '请选择供应商等级', trigger: 'change' }],
  contactPerson: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value = {
    keyword: '',
    contactPerson: '',
    contactPhone: '',
    supplierType: null,
    status: null,
  }
  refresh()
}

function handleSelectionChange(rows: SupplierInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    supplierCode: '',
    supplierName: '',
    shortName: '',
    unifiedSocialCode: '',
    supplierType: 'raw_material',
    supplierLevel: 'qualified',
    industry: '',
    registeredCapital: 0,
    establishDate: '',
    contactPerson: '',
    contactPhone: '',
    email: '',
    address: '',
    bankName: '',
    bankAccount: '',
    settlementMethod: 'monthly',
    paymentTerms: 30,
    supplyCategories: [],
    minOrderQuantity: 0,
    deliveryArea: '',
    remark: '',
    status: 'active',
  })
  dialogVisible.value = true
}

async function handleEdit(row: SupplierInfo) {
  isEdit.value = true
  try {
    const detail = await supplierApi.getById(row.supplierId)
    if (detail) {
      Object.assign(formData, {
        ...detail,
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载供应商详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = { ...formData }

    if (isEdit.value && formData.supplierId) {
      await supplierApi.update(formData.supplierId, submitData as unknown as Partial<SupplierFormData>)
      ElMessage.success('更新成功')
    } else {
      await supplierApi.create(submitData as unknown as SupplierFormData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

function beforeUpload(rawFile: UploadRawFile): boolean {
  const allowedTypes = [
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  ]

  if (!allowedTypes.includes(rawFile.type) && !rawFile.name.endsWith('.xlsx') && !rawFile.name.endsWith('.xls')) {
    ElMessage.error('只支持 Excel 格式的文件（.xlsx 或 .xls）')
    return false
  }

  const isLt5M = rawFile.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('文件大小不能超过 5MB!')
    return false
  }

  return true
}

async function handleImport(uploadFile: UploadFile): Promise<void> {
  if (!uploadFile.raw) {
    ElMessage.error('请选择要导入的文件')
    return
  }

  try {
    importLoading.value = true
    ElMessage.info('导入功能开发中')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导入失败，请检查文件格式')
  } finally {
    importLoading.value = false
  }
}

async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('导出功能开发中')
  } catch (error) {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

async function handleView(row: SupplierInfo) {
  try {
    const detail = await supplierApi.getById(row.supplierId)
    if (detail) {
      currentDetail.value = detail
      detailVisible.value = true
    }
  } catch {
    ElMessage.error('加载供应商详情失败')
  }
}

async function handleBlacklist(row: SupplierInfo) {
  try {
    const { value } = await ElMessageBox.prompt(
      '加入黑名单后该供应商将无法参与新的采购业务，请谨慎操作',
      `加入黑名单「${row.supplierName}」`,
      {
        confirmButtonText: '确定加入',
        cancelButtonText: '取消',
        type: 'warning',
        inputPlaceholder: '请输入加入黑名单的原因',
        inputValidator: (val: string) => val?.trim() ? true : '原因不能为空',
      },
    )
    await supplierApi.updateStatus(row.supplierId, 'frozen', value.trim())
    ElMessage.success('已加入黑名单')
    refresh()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleRestore(row: SupplierInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要将供应商「${row.supplierName}」从黑名单恢复吗？`,
      '恢复确认',
      {
        confirmButtonText: '确定恢复',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    await supplierApi.updateStatus(row.supplierId, 'active')
    ElMessage.success('已恢复正常状态')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

async function handleDelete(row: SupplierInfo): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定要删除供应商「${row.supplierName}」吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await supplierApi.delete(row.supplierId)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string): string {
  const map: Record<string, string> = {
    active: 'success',
    inactive: 'info',
    frozen: 'error',
    eliminated: 'error',
  }
  return map[status] || 'info'
}

function getStatusLabel(status: string): string {
  const map: Record<string, string> = {
    active: '正常合作',
    inactive: '停用',
    frozen: '黑名单',
    eliminated: '已淘汰',
  }
  return map[status] || status
}

function getSupplierTypeLabel(type: string): string {
  const opt = SupplierTypeOptions.find(o => o.value === type)
  return opt?.label || type
}

function getSupplierLevelLabel(level: string): string {
  const opt = SupplierLevelOptions.find(o => o.value === level)
  return opt?.label || level
}

function getSupplierLevelColor(level: string): string {
  const map: Record<string, string> = {
    strategic: 'primary',
    qualified: 'success',
    alternative: 'warning',
    temporary: 'info',
  }
  return map[level] || 'info'
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatAmount(val: number): string {
  return val != null ? val.toLocaleString('zh-CN') : '0'
}
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部（仅放主操作按钮） -->
    <PageHeader title="供应商档案" description="管理供应商信息、资质、评级">
      <el-button v-permission="'purchase:supplier:create'" type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增供应商
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="OfficeBuilding" label="供应商总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="CircleCheck" label="正常合作" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="Clock" label="待评估" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Warning" label="黑名单" :value="String(statistics.blacklist)" color-type="error" variant="bordered" />
    </section>

    <!-- 工具栏面板（搜索筛选 + 导入导出） -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="供应商名称/编码"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.contactPerson"
            placeholder="联系人"
            clearable
            style="width: 130px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-input
            v-model="queryForm.contactPhone"
            placeholder="联系电话"
            clearable
            style="width: 140px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.supplierType"
            placeholder="供应商类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in SupplierTypeOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="状态"
            clearable
            style="width: 110px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in SupplierStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
            :disabled="importLoading"
          >
            <el-button size="default" class="action-btn--import" :loading="importLoading">
              <el-icon :size="14"><Upload /></el-icon>导入
            </el-button>
          </el-upload>
          <el-button
            size="default"
            class="action-btn--export"
            :loading="exportLoading"
            @click="handleExport"
          >
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
        <!-- 供应商类型列 -->
        <template #supplierType="{ row }">
          <StatusTag :status="'primary'" :label="getSupplierTypeLabel(row.supplierType)" size="small" variant="light" />
        </template>

        <!-- 等级列 -->
        <template #supplierLevel="{ row }">
          <StatusTag :status="getSupplierLevelColor(row.supplierLevel)" :label="getSupplierLevelLabel(row.supplierLevel)" size="small" variant="light" />
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusColor(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 创建时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button v-permission="'purchase:supplier:view'" link type="primary" size="default" @click.stop="handleView(row)">
              详情
            </el-button>
            <el-button v-permission="'purchase:supplier:edit'" link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'active'"
              v-permission="'purchase:supplier:edit'"
              link
              type="warning"
              size="default"
              @click.stop="handleBlacklist(row)"
            >
              加入黑名单
            </el-button>
            <el-button
              v-if="row.status === 'frozen' || row.status === 'eliminated'"
              v-permission="'purchase:supplier:edit'"
              link
              type="success"
              size="default"
              @click.stop="handleRestore(row)"
            >
              恢复
            </el-button>
            <el-button v-permission="'purchase:supplier:delete'" link type="danger" size="default" @click.stop="handleDelete(row)">
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
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑供应商' : '新增供应商'"
      width="1100px"
      class="fts-dialog--wide"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="供应商名称" prop="supplierName">
                <el-input v-model="formData.supplierName" placeholder="请输入供应商名称" maxlength="100" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="供应商编码" prop="supplierCode">
                <el-input v-model="formData.supplierCode" :disabled="isEdit" placeholder="留空则自动生成" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="供应商类型" prop="supplierType">
                <el-select v-model="formData.supplierType" placeholder="请选择类型" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in SupplierTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="供应商等级" prop="supplierLevel">
                <el-select v-model="formData.supplierLevel" placeholder="请选择等级" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in SupplierLevelOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系人" prop="contactPerson">
                <el-input v-model="formData.contactPerson" placeholder="请输入联系人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话" prop="contactPhone">
                <el-input v-model="formData.contactPhone" placeholder="请输入联系电话" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="联系邮箱" prop="email">
                <el-input v-model="formData.email" placeholder="请输入邮箱" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="状态" prop="status">
                <el-radio-group v-model="formData.status">
                  <el-radio value="active">正常</el-radio>
                  <el-radio value="inactive">停用</el-radio>
                </el-radio-group>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="地址" prop="address">
                <el-input v-model="formData.address" placeholder="请输入地址" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 资质信息 -->
        <div class="form-section">
          <div class="section-title">资质信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="简称">
                <el-input v-model="formData.shortName" placeholder="请输入简称" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="统一社会信用代码">
                <el-input v-model="formData.unifiedSocialCode" placeholder="请输入统一社会信用代码" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="所属行业">
                <el-input v-model="formData.industry" placeholder="请输入所属行业" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="注册资本(元)">
                <el-input-number v-model="formData.registeredCapital" :min="0" :precision="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="成立日期">
                <el-date-picker v-model="formData.establishDate" type="date" placeholder="选择日期" :teleported="false" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="供货品类">
                <el-select v-model="formData.supplyCategories" multiple placeholder="请选择供货品类" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in SupplyCategoryOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 财务信息 -->
        <div class="form-section">
          <div class="section-title">财务信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="开户银行">
                <el-input v-model="formData.bankName" placeholder="请输入开户银行" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="银行账号">
                <el-input v-model="formData.bankAccount" placeholder="请输入银行账号" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="结算方式">
                <el-select v-model="formData.settlementMethod" placeholder="请选择结算方式" :teleported="false" style="width: 100%">
                  <el-option v-for="opt in SettlementMethodOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="账期(天)">
                <el-input-number v-model="formData.paymentTerms" :min="0" controls-position="right" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailVisible"
      title="供应商详情"
      width="1100px"
      class="fts-dialog--wide"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="供应商名称">{{ currentDetail.supplierName }}</el-descriptions-item>
          <el-descriptions-item label="供应商编码">{{ currentDetail.supplierCode }}</el-descriptions-item>
          <el-descriptions-item label="供应商类型">
            <StatusTag status="primary" :label="getSupplierTypeLabel(currentDetail.supplierType)" size="small" variant="light" />
          </el-descriptions-item>
          <el-descriptions-item label="供应商等级">
            <StatusTag :status="getSupplierLevelColor(currentDetail.supplierLevel)" :label="getSupplierLevelLabel(currentDetail.supplierLevel)" size="small" variant="light" />
          </el-descriptions-item>
          <el-descriptions-item label="联系人">{{ currentDetail.contactPerson }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ currentDetail.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="联系邮箱">{{ currentDetail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <StatusTag :status="getStatusColor(currentDetail.status)" :label="getStatusLabel(currentDetail.status)" size="small" variant="light" />
          </el-descriptions-item>
          <el-descriptions-item label="地址" :span="2">{{ currentDetail.address || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="资质信息" :column="2" border class="detail-section">
          <el-descriptions-item label="简称">{{ currentDetail.shortName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码">{{ currentDetail.unifiedSocialCode || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所属行业">{{ currentDetail.industry || '-' }}</el-descriptions-item>
          <el-descriptions-item label="注册资本(元)">{{ formatAmount(currentDetail.registeredCapital) }}</el-descriptions-item>
          <el-descriptions-item label="成立日期">{{ currentDetail.establishDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="供货品类">{{ currentDetail.mainCategories || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="财务信息" :column="2" border class="detail-section">
          <el-descriptions-item label="开户银行">{{ currentDetail.bankName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="银行账号">{{ currentDetail.bankAccount || '-' }}</el-descriptions-item>
          <el-descriptions-item label="结算方式">
            {{ SettlementMethodOptions.find(o => o.value === currentDetail.settlementMethod)?.label || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="账期(天)">{{ currentDetail.paymentTerms }}天</el-descriptions-item>
        </el-descriptions>

        <el-descriptions title="备注信息" :column="1" border class="detail-section">
          <el-descriptions-item label="备注">{{ currentDetail.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ formatTime(currentDetail.createTime) }}</el-descriptions-item>
        </el-descriptions>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button type="primary" @click="detailVisible = false">关闭</el-button>
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

  .action-btn--import,
  .action-btn--export {
    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: var(--fts-primary);
      border-color: var(--fts-primary-light-5);
      background-color: var(--fts-primary-light-9);
    }
  }
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

// 表单分区样式
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

// 详情区块间距
.detail-section {
  margin-top: var(--fts-space-4);
}

// 时间
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
