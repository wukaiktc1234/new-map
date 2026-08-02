<script setup lang="ts">
/**
 * 资产维护页面 - 管理资产的维修保养和维护记录
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成资产维护管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { maintenanceApi, assetApi } from '@/api/asset'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { MaintenanceRecord, Asset, MaintenanceStatus, RepairType } from '@/types/asset'
import { MaintenanceStatusTagMap, RepairTypeOptions } from '@/types/asset'
import { fenToYuan, yuanToFen } from '@/utils/money'

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
const selectedRows = ref<MaintenanceRecord[]>([])
const dialogVisible = ref(false)
const detailDialogVisible = ref(false)
const isEdit = ref(false)
const currentDetail = ref<MaintenanceRecord | null>(null)
const exportLoading = ref(false)

const formRef = ref<FormInstance>()

// 查询表单
const queryForm = ref({
  keyword: '',
  maintenanceNo: '',
  assetName: '',
  maintenanceType: '' as RepairType | '',
  status: '' as MaintenanceStatus | '',
  dateRange: [] as string[],
})

// 资产列表（用于对话框中选择资产）
const assetList = ref<Asset[]>([])
const assetListLoading = ref(false)

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<MaintenanceRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const queryParams: Record<string, unknown> = {
        page: params.page,
        size: params.size,
      }
      if (params.status) {
        queryParams.status = params.status
      }
      const res = await maintenanceApi.getList(queryParams as { assetId?: string; status?: MaintenanceStatus })
      let records = res.records

      // 前端过滤：维护单号、资产名称、维护类型、日期范围
      if (params.maintenanceNo) {
        const kw = params.maintenanceNo.toLowerCase()
        records = records.filter(m => m.id.toLowerCase().includes(kw))
      }
      if (params.assetName) {
        const kw = params.assetName.toLowerCase()
        records = records.filter(m =>
          (m.assetName?.toLowerCase().includes(kw)) ||
          (m.assetCode?.toLowerCase().includes(kw))
        )
      }
      if (params.maintenanceType) {
        records = records.filter(m => m.repairType === params.maintenanceType)
      }
      if (params.keyword) {
        const kw = params.keyword.toLowerCase()
        records = records.filter(m =>
          (m.id.toLowerCase().includes(kw)) ||
          (m.assetName?.toLowerCase().includes(kw)) ||
          (m.assetCode?.toLowerCase().includes(kw)) ||
          (m.faultDescription?.toLowerCase().includes(kw))
        )
      }
      if (params.dateRange && params.dateRange.length === 2) {
        const [start, end] = params.dateRange
        records = records.filter(m => {
          const t = m.startTime || m.createTime
          return t >= start && t <= end + ' 23:59:59'
        })
      }

      return {
        ...res,
        records,
        total: records.length,
      }
    },
  } as unknown as CrudApi<MaintenanceRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据类型
type MaintenanceFormState = {
  id: string
  assetId: string
  assetName: string
  assetCode: string
  repairType: RepairType
  faultDescription: string
  estimatedCost: number
  startTime: string
  expectedCompleteDate: string
  vendorName: string
  remark: string
}

const formData = reactive<MaintenanceFormState>({
  id: '',
  assetId: '',
  assetName: '',
  assetCode: '',
  repairType: 'internal',
  faultDescription: '',
  estimatedCost: 0,
  startTime: '',
  expectedCompleteDate: '',
  vendorName: '',
  remark: '',
})

// 统计数据
const statistics = computed(() => {
  const list = tableData.value
  const pendingCount = list.filter(m => m.status === 'pending').length
  const inProgressCount = list.filter(m => m.status === 'in_progress').length
  const completedCount = list.filter(m => m.status === 'completed').length

  // 计算本月维护费用
  const now = new Date()
  const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const thisMonthCost = list
    .filter(m => {
      const t = m.completedTime || m.startTime || m.createTime
      return t && t.startsWith(thisMonth)
    })
    .reduce((sum, m) => sum + m.repairCost, 0)

  return {
    pending: pendingCount,
    inProgress: inProgressCount,
    completed: completedCount,
    thisMonthCost: fenToYuan(thisMonthCost),
  }
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'id', label: '维护单号', minWidth: 140, slot: 'maintenanceNo' },
  { prop: 'assetName', label: '资产名称', minWidth: 140, slot: 'assetName' },
  { prop: 'assetCode', label: '资产编码', minWidth: 130, slot: 'assetCode' },
  { prop: 'repairType', label: '维护类型', minWidth: 110, slot: 'repairType' },
  { prop: 'faultDescription', label: '维护原因', minWidth: 180, slot: 'faultDescription' },
  { prop: 'repairCost', label: '维护费用(元)', minWidth: 120, slot: 'repairCost' },
  { prop: 'startTime', label: '送修日期', minWidth: 150, slot: 'startTime' },
  { prop: 'completedTime', label: '完成日期', minWidth: 150, slot: 'completedTime' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  assetId: [{ required: true, message: '请选择资产', trigger: 'change' }],
  repairType: [{ required: true, message: '请选择维护类型', trigger: 'change' }],
  faultDescription: [{ required: true, message: '请输入维护原因', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择送修日期', trigger: 'change' }],
}

// ==================== 工具函数 ====================
// fenToYuan/yuanToFen 已从 @/utils/money 导入，禁止在组件内直接做 *100//100 金额转换

function getStatusInfo(status: MaintenanceStatus) {
  return MaintenanceStatusTagMap[status] || { status: 'info', label: status }
}

function getRepairTypeLabel(type: string) {
  return RepairTypeOptions.find(o => o.value === type)?.label || type
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

function formatDateTime(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.replace('T', ' ').slice(0, 16)
}

// 生成维护单号
function generateMaintenanceNo(): string {
  const now = new Date()
  const dateStr = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}`
  const random = Math.random().toString(36).substring(2, 6).toUpperCase()
  return `MT${dateStr}${random}`
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.maintenanceNo = ''
  queryForm.value.assetName = ''
  queryForm.value.maintenanceType = ''
  queryForm.value.status = ''
  queryForm.value.keyword = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: MaintenanceRecord[]) {
  selectedRows.value = rows
}

// 加载资产列表
async function loadAssetList(): Promise<void> {
  assetListLoading.value = true
  try {
    const res = await assetApi.getList({ page: 1, pageSize: 1000 })
    assetList.value = res.records || []
  } catch {
    assetList.value = []
  } finally {
    assetListLoading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    id: generateMaintenanceNo(),
    assetId: '',
    assetName: '',
    assetCode: '',
    repairType: 'internal',
    faultDescription: '',
    estimatedCost: 0,
    startTime: new Date().toISOString().slice(0, 10),
    expectedCompleteDate: '',
    vendorName: '',
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: MaintenanceRecord) {
  isEdit.value = true
  try {
    const detail = await maintenanceApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        id: detail.id,
        assetId: detail.assetId,
        assetName: detail.assetName || '',
        assetCode: detail.assetCode || '',
        repairType: detail.repairType,
        faultDescription: detail.faultDescription,
        estimatedCost: Number(fenToYuan(detail.repairCost)),
        startTime: detail.startTime?.slice(0, 10) || '',
        expectedCompleteDate: detail.completedTime?.slice(0, 10) || '',
        vendorName: detail.vendorName || '',
        remark: detail.resultDescription || '',
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载维护详情失败')
  }
}

async function handleDetail(row: MaintenanceRecord) {
  try {
    const detail = await maintenanceApi.getById(row.id)
    if (detail) {
      currentDetail.value = detail
      detailDialogVisible.value = true
    }
  } catch {
    ElMessage.error('获取详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData = {
      assetId: formData.assetId,
      faultDescription: formData.faultDescription,
      repairType: formData.repairType,
      vendorId: formData.vendorName,
      estimatedCost: yuanToFen(formData.estimatedCost),
    }

    if (isEdit.value && formData.id) {
      ElMessage.info('编辑功能暂不支持')
    } else {
      await maintenanceApi.create(submitData)
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

// 完成维护
async function handleComplete(row: MaintenanceRecord) {
  try {
    await ElMessageBox.confirm(`确定要完成资产「${row.assetName}」的维护吗？`, '完成确认', {
      confirmButtonText: '确定完成',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await maintenanceApi.complete(row.id, {
      resultDescription: '维护完成',
      repairCost: row.repairCost,
    })
    ElMessage.success('维护已完成')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
  }
}

// 删除维护记录
async function handleDelete(row: MaintenanceRecord) {
  try {
    await ElMessageBox.confirm(`确定要删除该维护记录吗？此操作不可撤销！`, '删除确认', {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await maintenanceApi.delete(row.id)
    ElMessage.success('删除成功')
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '删除失败')
    }
  }
}

// 导出维护记录
async function handleExport(): Promise<void> {
  try {
    exportLoading.value = true
    ElMessage.info('正在导出数据，请稍候...')

    // 生成CSV格式数据
    const headers = ['维护单号', '资产名称', '资产编码', '维护类型', '维护原因', '维护费用(元)', '送修日期', '完成日期', '状态']
    const rows = tableData.value.map(item => [
      item.id,
      item.assetName || '',
      item.assetCode || '',
      getRepairTypeLabel(item.repairType),
      item.faultDescription,
      fenToYuan(item.repairCost),
      formatDate(item.startTime),
      formatDate(item.completedTime || ''),
      getStatusInfo(item.status).label,
    ])

    const csvContent = [headers.join(','), ...rows.map(row => row.join(','))].join('\n')
    const BOM = '\uFEFF'
    const blob = new Blob([BOM + csvContent], { type: 'text/csv;charset=utf-8' })

    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[T:]/g, '-')
    link.download = `资产维护记录_${timestamp}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('导出成功！文件已开始下载')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  } finally {
    exportLoading.value = false
  }
}

// 选择资产时更新资产信息
function handleAssetSelect() {
  const asset = assetList.value.find(a => a.id === formData.assetId)
  if (asset) {
    formData.assetName = asset.assetName
    formData.assetCode = asset.assetCode
  }
}

// 生命周期
onMounted(() => {
  loadAssetList()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="资产维护" description="管理资产的维修保养和维护记录">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增维护
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Warning" label="待维护" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="SetUp" label="维护中" :value="String(statistics.inProgress)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Coin" label="本月维护费用" :value="`¥${statistics.thisMonthCost}`" color-type="primary" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.maintenanceNo"
            placeholder="维护单号"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.assetName"
            placeholder="资产名称"
            clearable
            style="width: 150px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.maintenanceType"
            placeholder="维护类型"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in RepairTypeOptions"
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
            <el-option v-for="(info, key) in MaintenanceStatusTagMap" :key="key" :label="info.label" :value="key" />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
            size="default"
            :teleported="false"
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
            :loading="exportLoading"
            @click="handleExport"
          >
            <el-icon :size="14"><Download /></el-icon>导出维护记录
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
        <!-- 维护单号列 -->
        <template #maintenanceNo="{ row }">
          <span class="maintenance-no">{{ row.id }}</span>
        </template>

        <!-- 资产名称列 -->
        <template #assetName="{ row }">
          <span class="asset-name">{{ row.assetName || '-' }}</span>
        </template>

        <!-- 资产编码列 -->
        <template #assetCode="{ row }">
          <span class="asset-code">{{ row.assetCode || '-' }}</span>
        </template>

        <!-- 维护类型列 -->
        <template #repairType="{ row }">
          <StatusTag status="info" :label="getRepairTypeLabel(row.repairType)" size="small" variant="light" />
        </template>

        <!-- 维护原因列 -->
        <template #faultDescription="{ row }">
          <span class="fault-desc" :title="row.faultDescription">{{ row.faultDescription || '-' }}</span>
        </template>

        <!-- 维护费用列 -->
        <template #repairCost="{ row }">
          <span class="cost-text">¥{{ fenToYuan(row.repairCost) }}</span>
        </template>

        <!-- 送修日期列 -->
        <template #startTime="{ row }">
          <span class="date-text">{{ formatDate(row.startTime) }}</span>
        </template>

        <!-- 完成日期列 -->
        <template #completedTime="{ row }">
          <span class="date-text">{{ formatDate(row.completedTime || '') }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusInfo(row.status).status" :label="getStatusInfo(row.status).label" size="small" variant="light" />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button
              v-if="row.status !== 'completed'"
              link
              type="primary"
              size="default"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              v-if="row.status === 'in_progress'"
              link
              type="success"
              size="default"
              @click.stop="handleComplete(row)"
            >
              完成
            </el-button>
            <el-button link type="danger" size="default" @click.stop="handleDelete(row)">
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
      :title="isEdit ? '编辑维护' : '新增维护'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维护单号" prop="id">
              <el-input v-model="formData.id" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="选择资产" prop="assetId">
              <el-select
                v-model="formData.assetId"
                placeholder="请选择资产"
                filterable
                :teleported="false"
                style="width: 100%"
                @change="handleAssetSelect"
              >
                <el-option
                  v-for="asset in assetList"
                  :key="asset.id"
                  :label="`${asset.assetCode} - ${asset.assetName}`"
                  :value="asset.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="维护类型" prop="repairType">
              <el-select v-model="formData.repairType" placeholder="请选择维护类型" :teleported="false" style="width: 100%">
                <el-option v-for="opt in RepairTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计费用(元)">
              <el-input-number v-model="formData.estimatedCost" :precision="2" :min="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="送修日期" prop="startTime">
              <el-date-picker
                v-model="formData.startTime"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择送修日期"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计完成日期">
              <el-date-picker
                v-model="formData.expectedCompleteDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择预计完成日期"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="24">
            <el-form-item label="维护服务商">
              <el-input v-model="formData.vendorName" placeholder="请输入维护服务商名称" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="维护原因" prop="faultDescription">
          <el-input v-model="formData.faultDescription" type="textarea" :rows="3" placeholder="请输入维护原因描述" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注信息" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="维护详情"
      width="720px"
      destroy-on-close
    >
      <template v-if="currentDetail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="维护单号">{{ currentDetail.id }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusInfo(currentDetail.status).status" :label="getStatusInfo(currentDetail.status).label" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="资产名称">{{ currentDetail.assetName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="资产编码">{{ currentDetail.assetCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="维护类型">{{ getRepairTypeLabel(currentDetail.repairType) }}</el-descriptions-item>
            <el-descriptions-item label="维护服务商">{{ currentDetail.vendorName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="送修日期">{{ formatDate(currentDetail.startTime) }}</el-descriptions-item>
            <el-descriptions-item label="完成日期">{{ formatDate(currentDetail.completedTime || '') }}</el-descriptions-item>
            <el-descriptions-item label="维护费用">¥{{ fenToYuan(currentDetail.repairCost) }}</el-descriptions-item>
            <el-descriptions-item label="维修人">{{ currentDetail.repairmanName || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 维护明细 -->
        <div class="detail-section">
          <div class="section-title">维护明细</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="维护原因">{{ currentDetail.faultDescription || '-' }}</el-descriptions-item>
            <el-descriptions-item label="维护结果">{{ currentDetail.resultDescription || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 维护费用明细 -->
        <div class="detail-section">
          <div class="section-title">维护费用明细</div>
          <el-table :data="[{ name: '维护费用', amount: currentDetail.repairCost }]" size="small" border style="width: 100%">
            <el-table-column prop="name" label="费用项目" />
            <el-table-column prop="amount" label="金额(元)" align="right">
              <template #default="{ row }">
                <span>¥{{ fenToYuan(row.amount) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <div class="cost-total">
            <span>合计：</span>
            <strong class="total-amount">¥{{ fenToYuan(currentDetail.repairCost) }}</strong>
          </div>
        </div>

        <!-- 维护记录时间线 -->
        <div class="detail-section">
          <div class="section-title">维护记录时间线</div>
          <el-timeline>
            <el-timeline-item :timestamp="formatDateTime(currentDetail.createTime)" placement="top">
              <h4>创建维护单</h4>
              <p>维护单已创建，状态：待维护</p>
            </el-timeline-item>
            <el-timeline-item :timestamp="formatDateTime(currentDetail.startTime)" placement="top">
              <h4>开始维护</h4>
              <p>资产开始维护，状态：维护中</p>
            </el-timeline-item>
            <el-timeline-item v-if="currentDetail.completedTime" :timestamp="formatDateTime(currentDetail.completedTime)" placement="top" type="success">
              <h4>维护完成</h4>
              <p>资产维护完成，状态：已完成</p>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>

      <template #footer>
        <el-button type="primary" @click="detailDialogVisible = false">关闭</el-button>
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

// 维护单号
.maintenance-no {
  font-family: var(--fts-font-family-mono);
  color: var(--fts-text-primary);
}

// 资产名称
.asset-name {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

// 资产编码
.asset-code {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 故障描述
.fault-desc {
  color: var(--fts-text-primary);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

// 费用
.cost-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 日期
.date-text {
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

// 详情区域
.detail-section {
  margin-bottom: var(--fts-space-5);

  &:last-child {
    margin-bottom: 0;
  }
}

.section-title {
  font-weight: var(--fts-font-weight-semibold);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-left: var(--fts-space-2);
  border-left: 3px solid var(--fts-primary);
}

// 费用合计
.cost-total {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  font-size: var(--fts-font-size-base);
  color: var(--fts-text-primary);
  background: var(--fts-bg-soft);
  border: 1px solid var(--fts-border-primary);
  border-top: none;
  border-radius: 0 0 var(--fts-card-radius) var(--fts-card-radius);

  .total-amount {
    color: var(--fts-primary);
    font-size: var(--fts-font-size-lg);
    margin-left: var(--fts-space-2);
  }
}

// 时间线
:deep(.el-timeline-item__content) {
  color: var(--fts-text-secondary);
}

:deep(.el-timeline-item__timestamp) {
  color: var(--fts-text-tertiary);
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
