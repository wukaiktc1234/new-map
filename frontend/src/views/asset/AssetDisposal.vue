<script setup lang="ts">
/**
 * 资产处置页面 - 基于 ModernEmployee 黄金模板重构
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理资产的报废、变卖、捐赠等处置操作
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Delete, Upload } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { disposalApi, assetApi } from '@/api/asset'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type { DisposalRecord, DisposalType, DisposalStatus, Asset, PageResponse } from '@/types/asset'
import { yuanToFen, fenToYuanNumber as fenToYuan, formatFenToYuan as formatYuan } from '@/utils/money'
import {
  DisposalTypeOptions,
  DisposalStatusTagMap,
  AssetStatus,
} from '@/types/asset'

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

interface DisposalQueryForm {
  disposalNo: string
  assetName: string
  disposalType: DisposalType | ''
  status: DisposalStatus | ''
  dateRange: string[]
}

interface DisposalFormData {
  assetId: string
  disposalType: DisposalType
  disposalReason: string
  disposalAmount: number
  applyDate: string
  remark: string
  attachmentUrls: string[]
}

interface ApprovalRecord {
  id: string
  approverName: string
  approvalTime: string
  approvalResult: string
  approvalComment: string
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<DisposalRecord[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const approvalVisible = ref(false)
const currentRecord = ref<DisposalRecord | null>(null)
const approvalForm = reactive({
  approved: true,
  comment: '',
})

const formRef = ref<FormInstance>()
const approvalFormRef = ref<FormInstance>()

// 查询表单
const queryForm = ref<DisposalQueryForm>({
  disposalNo: '',
  assetName: '',
  disposalType: '',
  status: '',
  dateRange: [],
})

// 资产列表（用于对话框中的资产选择）
const assetList = ref<Asset[]>([])
const assetLoading = ref(false)

// 使用 useCrudTable 管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<DisposalRecord, DisposalQueryForm>({
  api: {
    getList: async (params: DisposalQueryForm & { page: number; size: number }): Promise<PageResponse<DisposalRecord>> => {
      const res = await disposalApi.getList({
        status: params.status || undefined,
        disposalType: params.disposalType || undefined,
      })
      let records = res.records
      // 前端筛选：处置单号
      if (params.disposalNo) {
        const kw = params.disposalNo.toLowerCase()
        records = records.filter(r => r.disposalNo.toLowerCase().includes(kw))
      }
      // 前端筛选：资产名称
      if (params.assetName) {
        const kw = params.assetName.toLowerCase()
        records = records.filter(r => r.assetName.toLowerCase().includes(kw))
      }
      // 前端筛选：日期范围
      if (params.dateRange && params.dateRange.length === 2) {
        const [start, end] = params.dateRange
        records = records.filter(r => {
          const date = r.createTime?.split('T')[0] || r.disposalDate
          if (!date) return true
          return date >= start && date <= end
        })
      }
      return {
        records,
        total: records.length,
        current: params.page,
        size: params.size,
        pages: Math.ceil(records.length / params.size),
      }
    },
  } as unknown as CrudApi<DisposalRecord, DisposalQueryForm>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<DisposalFormData>({
  assetId: '',
  disposalType: 'scrap' as DisposalType,
  disposalReason: '',
  disposalAmount: 0,
  applyDate: '',
  remark: '',
  attachmentUrls: [],
})

// 统计数据
const statistics = computed(() => {
  const records = tableData.value
  const pendingCount = records.filter(r => r.status === DisposalStatus.PENDING).length
  const approvedCount = records.filter(r => r.status === DisposalStatus.APPROVED).length
  const completedCount = records.filter(r => r.status === DisposalStatus.COMPLETED).length

  // 本月处置数量
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const thisMonthCount = records.filter(r => {
    const date = r.disposalDate || r.createTime
    return date && date.startsWith(currentMonth) && r.status === DisposalStatus.COMPLETED
  }).length

  return {
    pending: pendingCount,
    processing: approvedCount,
    completed: completedCount,
    thisMonth: thisMonthCount,
  }
})

// 可处置资产列表（仅在用/闲置/维修中）
const disposableAssets = computed(() => {
  return assetList.value.filter(a =>
    a.status === AssetStatus.ACTIVE ||
    a.status === AssetStatus.IDLE ||
    a.status === AssetStatus.MAINTENANCE,
  )
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'disposalNo', label: '处置单号', minWidth: 140, slot: 'disposalNo' },
  { prop: 'assetName', label: '资产名称', minWidth: 140, slot: 'assetName' },
  { prop: 'assetCode', label: '资产编码', minWidth: 130, slot: 'assetCode' },
  { prop: 'disposalType', label: '处置类型', minWidth: 100, slot: 'disposalType' },
  { prop: 'disposalReason', label: '处置原因', minWidth: 150, slot: 'disposalReason' },
  { prop: 'disposalAmount', label: '处置金额', minWidth: 120, slot: 'disposalAmount' },
  { prop: 'applyDate', label: '申请日期', minWidth: 120, slot: 'applyDate' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 260, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  assetId: [{ required: true, message: '请选择处置资产', trigger: 'change' }],
  disposalType: [{ required: true, message: '请选择处置类型', trigger: 'change' }],
  disposalReason: [{ required: true, message: '请输入处置原因', trigger: 'blur' }],
  disposalAmount: [{ required: true, message: '请输入处置金额', trigger: 'blur' }],
  applyDate: [{ required: true, message: '请选择申请日期', trigger: 'change' }],
}

const approvalRules: FormRules = {
  comment: [
    {
      validator: (_rule: unknown, value: string, callback: (error?: Error) => void) => {
        if (!approvalForm.approved && !value?.trim()) {
          callback(new Error('驳回时请填写驳回原因'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.disposalNo = ''
  queryForm.value.assetName = ''
  queryForm.value.disposalType = ''
  queryForm.value.status = ''
  queryForm.value.dateRange = []
  refresh()
}

function handleSelectionChange(rows: DisposalRecord[]) {
  selectedRows.value = rows
}

async function loadAssetList() {
  assetLoading.value = true
  try {
    const res = await assetApi.getList({ page: 1, pageSize: 500 })
    assetList.value = res.records
  } catch {
    assetList.value = []
  } finally {
    assetLoading.value = false
  }
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    assetId: '',
    disposalType: 'scrap' as DisposalType,
    disposalReason: '',
    disposalAmount: 0,
    applyDate: new Date().toISOString().split('T')[0],
    remark: '',
    attachmentUrls: [],
  })
  dialogVisible.value = true
}

async function handleEdit(row: DisposalRecord) {
  if (row.status !== DisposalStatus.PENDING) {
    ElMessage.warning('仅待审批状态的处置申请可编辑')
    return
  }
  isEdit.value = true
  try {
    const detail = await disposalApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        assetId: detail.assetId,
        disposalType: detail.disposalType,
        disposalReason: detail.remark || '',
        disposalAmount: fenToYuan(detail.disposalAmount),
        applyDate: detail.disposalDate || detail.createTime?.split('T')[0] || '',
        remark: detail.remark || '',
        attachmentUrls: [],
      })
      dialogVisible.value = true
    }
  } catch {
    ElMessage.error('加载处置详情失败')
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
      disposalType: formData.disposalType,
      disposalAmount: yuanToFen(formData.disposalAmount),
      disposalDate: formData.applyDate,
      remark: formData.disposalReason || formData.remark || '',
    }

    if (isEdit.value && currentRecord.value) {
      ElMessage.info('后端暂不支持更新处置记录')
    } else {
      await disposalApi.create(submitData)
      ElMessage.success('处置申请提交成功')
    }
    dialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '提交失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row: DisposalRecord) {
  try {
    const detail = await disposalApi.getById(row.id)
    currentRecord.value = detail
    detailVisible.value = true
  } catch {
    ElMessage.error('获取详情失败')
  }
}

function handleApproval(row: DisposalRecord) {
  if (row.status !== DisposalStatus.PENDING) {
    ElMessage.warning('仅待审批状态的申请可审批')
    return
  }
  currentRecord.value = row
  approvalForm.approved = true
  approvalForm.comment = ''
  approvalVisible.value = true
}

async function handleApprovalSubmit() {
  if (!approvalFormRef.value || !currentRecord.value) return

  const valid = await approvalFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    await disposalApi.approve({
      id: currentRecord.value.id,
      approved: approvalForm.approved,
      rejectReason: approvalForm.approved ? undefined : approvalForm.comment,
    })
    ElMessage.success(approvalForm.approved ? '审批通过' : '已驳回')
    approvalVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error('审批失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDelete(row: DisposalRecord) {
  if (row.status !== DisposalStatus.PENDING) {
    ElMessage.warning('仅待审批状态的申请可删除')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要删除处置申请「${row.disposalNo}」吗？此操作不可撤销！`,
      '删除确认',
      { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' },
    )
    ElMessage.info('后端暂不支持删除处置记录')
  } catch (error: unknown) {
    if (error !== 'cancel') {
      // 静默处理
    }
  }
}

async function handleExport() {
  ElMessage.info('导出功能开发中')
}

function beforeUpload(rawFile: UploadRawFile): boolean {
  const isLt10M = rawFile.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

function handleUploadSuccess(_response: unknown, _file: UploadFile) {
  ElMessage.success('附件上传成功')
}

// ==================== 辅助方法 ====================
// 注：金额转换 fenToYuan / formatYuan 已统一委托给 utils/money（见文件顶部 import）

function getStatusInfo(status: DisposalStatus) {
  return DisposalStatusTagMap[status] || { status: 'info', label: status }
}

function getDisposalTypeLabel(type: DisposalType): string {
  return DisposalTypeOptions.find(o => o.value === type)?.label || type
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

// 模拟审批记录
function getApprovalRecords(_record: DisposalRecord): ApprovalRecord[] {
  return [
    {
      id: '1',
      approverName: '张经理',
      approvalTime: '2024-01-15 10:30:00',
      approvalResult: '通过',
      approvalComment: '同意处置',
    },
  ]
}

onMounted(() => {
  loadAssetList()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="资产处置" description="管理资产的报废、变卖、捐赠等处置操作">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增处置申请
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Clock" label="待审批" :value="String(statistics.pending)" color-type="warning" variant="bordered" />
      <StatCard icon="Setting" label="处置中" :value="String(statistics.processing)" color-type="info" variant="bordered" />
      <StatCard icon="CircleCheck" label="已完成" :value="String(statistics.completed)" color-type="success" variant="bordered" />
      <StatCard icon="Calendar" label="本月处置资产" :value="String(statistics.thisMonth)" color-type="primary" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.disposalNo"
            placeholder="处置单号"
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
            v-model="queryForm.disposalType"
            placeholder="处置类型"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in DisposalTypeOptions"
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
              v-for="(info, key) in DisposalStatusTagMap"
              :key="key"
              :label="info.label"
              :value="key"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            :teleported="false"
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
          <el-button
            type="success"
            size="default"
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
        <!-- 处置单号列 -->
        <template #disposalNo="{ row }">
          <span class="disposal-no">{{ row.disposalNo }}</span>
        </template>

        <!-- 资产名称列 -->
        <template #assetName="{ row }">
          <span class="asset-name">{{ row.assetName }}</span>
        </template>

        <!-- 资产编码列 -->
        <template #assetCode="{ row }">
          <span class="asset-code">{{ row.assetCode }}</span>
        </template>

        <!-- 处置类型列 -->
        <template #disposalType="{ row }">
          <StatusTag status="info" :label="getDisposalTypeLabel(row.disposalType)" size="small" variant="light" />
        </template>

        <!-- 处置原因列 -->
        <template #disposalReason="{ row }">
          <span class="reason-text">{{ row.remark || '-' }}</span>
        </template>

        <!-- 处置金额列 -->
        <template #disposalAmount="{ row }">
          <span class="amount-text">{{ formatYuan(row.disposalAmount) }}</span>
        </template>

        <!-- 申请日期列 -->
        <template #applyDate="{ row }">
          <span class="date-text">{{ formatDate(row.createTime) }}</span>
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
              link
              type="primary"
              size="default"
              :disabled="row.status !== DisposalStatus.PENDING"
              @click.stop="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              link
              type="success"
              size="default"
              :disabled="row.status !== DisposalStatus.PENDING"
              @click.stop="handleApproval(row)"
            >
              审批
            </el-button>
            <el-button
              link
              type="danger"
              size="default"
              :disabled="row.status !== DisposalStatus.PENDING"
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
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑处置申请' : '新增处置申请'"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="选择资产" prop="assetId">
          <el-select
            v-model="formData.assetId"
            placeholder="请选择要处置的资产"
            filterable
            :teleported="false"
            style="width: 100%"
            :loading="assetLoading"
          >
            <el-option
              v-for="asset in disposableAssets"
              :key="asset.id"
              :label="`${asset.assetCode} - ${asset.assetName}`"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="处置类型" prop="disposalType">
              <el-select v-model="formData.disposalType" placeholder="请选择处置类型" :teleported="false" style="width: 100%">
                <el-option
                  v-for="opt in DisposalTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="处置金额" prop="disposalAmount">
              <el-input-number
                v-model="formData.disposalAmount"
                :min="0"
                :precision="2"
                :step="100"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="处置原因" prop="disposalReason">
          <el-input
            v-model="formData.disposalReason"
            type="textarea"
            :rows="3"
            placeholder="请输入处置原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="申请日期" prop="applyDate">
              <el-date-picker
                v-model="formData.applyDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="请选择申请日期"
                :teleported="false"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="备注">
              <el-input v-model="formData.remark" placeholder="请输入备注" maxlength="100" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="附件上传">
          <el-upload
            action="#"
            :show-file-list="true"
            :before-upload="beforeUpload"
            :on-success="handleUploadSuccess"
            :auto-upload="false"
            multiple
          >
            <el-button size="default" type="primary">
              <el-icon><Upload /></el-icon>
              点击上传
            </el-button>
            <template #tip>
              <div class="upload-tip">支持上传图片、PDF等文件，单个文件不超过10MB</div>
            </template>
          </el-upload>
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
      v-model="detailVisible"
      title="处置详情"
      width="720px"
      destroy-on-close
    >
      <template v-if="currentRecord">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="处置单号">{{ currentRecord.disposalNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="getStatusInfo(currentRecord.status).status" :label="getStatusInfo(currentRecord.status).label" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="处置类型">{{ getDisposalTypeLabel(currentRecord.disposalType) }}</el-descriptions-item>
            <el-descriptions-item label="申请日期">{{ formatDate(currentRecord.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="申请人">{{ currentRecord.applicantName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="处置日期">{{ currentRecord.disposalDate || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 资产信息 -->
        <div class="detail-section">
          <div class="section-title">资产信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="资产编码">{{ currentRecord.assetCode }}</el-descriptions-item>
            <el-descriptions-item label="资产名称">{{ currentRecord.assetName }}</el-descriptions-item>
            <el-descriptions-item label="资产分类">{{ currentRecord.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="账面净值">{{ formatYuan(currentRecord.netValue) }}</el-descriptions-item>
            <el-descriptions-item label="处置金额">{{ formatYuan(currentRecord.disposalAmount) }}</el-descriptions-item>
            <el-descriptions-item label="处置损益">
              <span :class="currentRecord.gainLoss >= 0 ? 'gain-positive' : 'gain-negative'">
                {{ currentRecord.gainLoss >= 0 ? '+' : '' }}{{ formatYuan(currentRecord.gainLoss) }}
              </span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 处置明细 -->
        <div class="detail-section">
          <div class="section-title">处置明细</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="处置原因">{{ currentRecord.remark || '-' }}</el-descriptions-item>
            <el-descriptions-item v-if="currentRecord.rejectReason" label="驳回原因">{{ currentRecord.rejectReason }}</el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 审批记录 -->
        <div class="detail-section">
          <div class="section-title">审批记录</div>
          <el-timeline>
            <el-timeline-item
              v-for="record in getApprovalRecords(currentRecord)"
              :key="record.id"
              :timestamp="record.approvalTime"
              placement="top"
            >
              <div class="approval-record">
                <span class="approver">{{ record.approverName }}</span>
                <StatusTag :status="record.approvalResult === '通过' ? 'success' : 'error'" :label="record.approvalResult" size="small" />
                <span v-if="record.approvalComment" class="approval-comment">{{ record.approvalComment }}</span>
              </div>
            </el-timeline-item>
            <el-timeline-item timestamp="提交申请" placement="top" type="primary">
              <div class="approval-record">
                <span class="approver">{{ currentRecord.applicantName || '申请人' }}</span>
                <StatusTag status="info" label="提交申请" size="small" />
              </div>
            </el-timeline-item>
          </el-timeline>
        </div>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog
      v-model="approvalVisible"
      title="审批处置申请"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="currentRecord">
        <div class="approval-info">
          <p><strong>处置单号：</strong>{{ currentRecord.disposalNo }}</p>
          <p><strong>资产名称：</strong>{{ currentRecord.assetName }}</p>
          <p><strong>处置类型：</strong>{{ getDisposalTypeLabel(currentRecord.disposalType) }}</p>
          <p><strong>处置金额：</strong>{{ formatYuan(currentRecord.disposalAmount) }}</p>
        </div>

        <el-form ref="approvalFormRef" :model="approvalForm" :rules="approvalRules" label-width="80px">
          <el-form-item label="审批结果">
            <el-radio-group v-model="approvalForm.approved">
              <el-radio :value="true">通过</el-radio>
              <el-radio :value="false">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审批意见" prop="comment">
            <el-input
              v-model="approvalForm.comment"
              type="textarea"
              :rows="4"
              :placeholder="approvalForm.approved ? '请输入审批意见（选填）' : '请输入驳回原因（必填）'"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </template>

      <template #footer>
        <el-button @click="approvalVisible = false">取消</el-button>
        <el-button
          :type="approvalForm.approved ? 'success' : 'danger'"
          :loading="submitLoading"
          @click="handleApprovalSubmit"
        >
          {{ approvalForm.approved ? '确认通过' : '确认驳回' }}
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

// 表格文字样式
.disposal-no {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.asset-name {
  color: var(--fts-text-primary);
  font-weight: var(--fts-font-weight-medium);
}

.asset-code {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

.reason-text {
  color: var(--fts-text-secondary);
}

.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
  font-variant-numeric: tabular-nums;
}

.date-text {
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
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

// 详情对话框
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

.gain-positive {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.gain-negative {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium);
}

// 审批记录
.approval-record {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

.approver {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

.approval-comment {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 审批对话框
.approval-info {
  background: var(--fts-bg-page);
  padding: var(--fts-space-4);
  border-radius: var(--fts-card-radius);
  margin-bottom: var(--fts-space-4);

  p {
    margin: 0 0 var(--fts-space-2) 0;
    color: var(--fts-text-secondary);

    &:last-child {
      margin-bottom: 0;
    }

    strong {
      color: var(--fts-text-primary);
      font-weight: var(--fts-font-weight-medium);
    }
  }
}

// 上传提示
.upload-tip {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-1);
}

// 响应式
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
