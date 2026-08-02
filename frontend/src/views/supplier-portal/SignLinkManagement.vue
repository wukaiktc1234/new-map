<script setup lang="ts">
/**
 * 签约链接管理页面 - 管理供应商自助签约链接的生成和跟踪
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】组装各层组件，完成签约链接管理的完整页面功能
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search, Refresh, Download, Link, Copy, Clock, CircleCheck, CircleClose, Warning, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { supplierPortalApi } from '@/api/supplier-portal'
import { supplierApi } from '@/api/purchase/supplier'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import {
  type SignLinkInfo,
  type SignLinkQueryForm,
  type SignLinkStatus,
  type CreateSignLinkForm,
  SignLinkStatusOptions,
  SignLinkStatusLabelMap,
} from '@/types/supplier-portal'

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

// 签约类型选项
const SignTypeOptions = [
  { label: '采购合同', value: 'purchase' },
  { label: '框架协议', value: 'framework' },
  { label: '质量协议', value: 'quality' },
  { label: '保密协议', value: 'confidentiality' },
]

// 合同模板选项（模拟数据）
const ContractTemplateOptions = [
  { label: '标准采购合同模板', value: 'tpl-001' },
  { label: '供应商框架协议模板', value: 'tpl-002' },
  { label: '质量保证协议模板', value: 'tpl-003' },
  { label: '保密协议模板', value: 'tpl-004' },
]

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<SignLinkInfo[]>([])
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const detailData = ref<SignLinkInfo | null>(null)
const detailLoading = ref(false)

const formRef = ref<FormInstance>()

// 供应商选项
const supplierOptions = ref<Array<{ supplierId: string; supplierName: string }>>([])

// 查询表单
const queryForm = ref<SignLinkQueryForm & {
  linkName?: string
  supplierId?: string
  createTimeStart?: string
  createTimeEnd?: string
}>({
  linkName: '',
  supplierId: '',
  status: null,
  createTimeStart: '',
  createTimeEnd: '',
})

// 使用 useCrudTable 管理表格数据
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<SignLinkInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: Record<string, unknown> = {
        keyword: params.linkName || params.keyword || undefined,
        status: params.status || undefined,
        page: params.page,
        size: params.size,
      }
      return supplierPortalApi.getList(convertedParams as unknown as SignLinkQueryForm)
    },
  } as unknown as CrudApi<SignLinkInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 生成链接表单数据
const formData = reactive<Partial<CreateSignLinkForm> & {
  linkName: string
  supplierId: string
  signType: string
  contractTemplateId: string
  expireDays: number
  remark: string
}>({
  linkName: '',
  supplierId: '',
  signType: 'purchase',
  contractTemplateId: '',
  expireDays: 30,
  remark: '',
  eContractId: '',
  contactPhone: '',
  contactEmail: '',
})

// 统计数据（计算属性）
const statistics = computed(() => ({
  total: pagination?.total || 0,
  active: tableData.value.filter(r => r.status === 'sent' || r.status === 'viewed').length,
  used: tableData.value.filter(r => r.status === 'signed').length,
  expired: tableData.value.filter(r => r.status === 'expired' || r.status === 'rejected').length,
}))

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'contractName', label: '链接名称', minWidth: 180, slot: 'linkName' },
  { prop: 'supplierName', label: '供应商', minWidth: 140, slot: 'supplierName' },
  { prop: 'signType', label: '签约类型', minWidth: 110, slot: 'signType' },
  { prop: 'expireTime', label: '有效期', minWidth: 160, slot: 'expireTime' },
  { prop: 'createTime', label: '创建时间', minWidth: 160, slot: 'createTime' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
  { prop: 'useCount', label: '使用次数', minWidth: 100, slot: 'useCount' },
  { prop: '_operation', label: '操作', width: 300, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  linkName: [
    { required: true, message: '请输入链接名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  supplierId: [
    { required: true, message: '请选择供应商', trigger: 'change' },
  ],
  signType: [
    { required: true, message: '请选择签约类型', trigger: 'change' },
  ],
  expireDays: [
    { required: true, message: '请设置有效期', trigger: 'blur' },
  ],
  contractTemplateId: [
    { required: true, message: '请选择合同模板', trigger: 'change' },
  ],
}

// ==================== 方法 ====================

/** 加载供应商选项 */
async function loadSupplierOptions() {
  try {
    const res = await supplierApi.getList({ page: 1, size: 1000, status: 'active' })
    const records = res?.records || []
    supplierOptions.value = records.map(s => ({
      supplierId: s.supplierId,
      supplierName: s.supplierName,
    }))
  } catch {
    supplierOptions.value = []
  }
}

/** 搜索 */
function handleSearch() {
  pagination.current = 1
  refresh()
}

/** 重置 */
function handleReset() {
  queryForm.value.linkName = ''
  queryForm.value.supplierId = ''
  queryForm.value.status = null
  queryForm.value.createTimeStart = ''
  queryForm.value.createTimeEnd = ''
  pagination.current = 1
  refresh()
}

/** 选中行变化 */
function handleSelectionChange(rows: SignLinkInfo[]) {
  selectedRows.value = rows
}

/** 打开生成链接对话框 */
function handleCreate() {
  Object.assign(formData, {
    linkName: '',
    supplierId: '',
    signType: 'purchase',
    contractTemplateId: '',
    expireDays: 30,
    remark: '',
    eContractId: '',
    contactPhone: '',
    contactEmail: '',
  })
  createDialogVisible.value = true
}

/** 批量生成链接 */
function handleBatchCreate() {
  ElMessage.info('批量生成功能开发中')
}

/** 导出 */
async function handleExport() {
  ElMessage.info('导出功能开发中')
}

/** 提交生成链接 */
async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const supplier = supplierOptions.value.find(s => s.supplierId === formData.supplierId)
    const submitData: CreateSignLinkForm = {
      eContractId: formData.contractTemplateId || '',
      contactPhone: formData.contactPhone || '',
      contactEmail: formData.contactEmail,
      expireDays: formData.expireDays || 30,
      remark: formData.remark,
    }
    await supplierPortalApi.createLink(submitData)
    ElMessage.success('链接生成成功')
    createDialogVisible.value = false
    refresh()
  } catch (error) {
    ElMessage.error('链接生成失败')
  } finally {
    submitLoading.value = false
  }
}

/** 查看详情 */
async function handleViewDetail(row: SignLinkInfo) {
  detailLoading.value = true
  detailDialogVisible.value = true
  try {
    const detail = await supplierPortalApi.getById(row.linkId)
    detailData.value = detail
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

/** 复制链接 */
function handleCopyLink(row: SignLinkInfo) {
  const url = `${window.location.origin}/portal/sign?token=${row.token}`
  navigator.clipboard.writeText(url).then(() => {
    ElMessage.success('链接已复制到剪贴板')
  }).catch(() => {
    ElMessage.info('复制失败，请手动复制')
  })
}

/** 续期链接 */
async function handleRenew(row: SignLinkInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要续期链接「${row.contractName}」吗？续期后有效期将延长30天。`,
      '续期确认',
      {
        confirmButtonText: '确认续期',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    ElMessage.success('续期成功')
    refresh()
  } catch {
    // 用户取消
  }
}

/** 作废链接 */
async function handleRevoke(row: SignLinkInfo) {
  try {
    await ElMessageBox.confirm(
      `作废链接后供应商将无法通过该链接签署，确定作废？`,
      '作废链接',
      {
        confirmButtonText: '确认作废',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await supplierPortalApi.revokeLink(row.linkId)
    ElMessage.success('链接已作废')
    refresh()
  } catch {
    // 用户取消
  }
}

/** 删除链接 */
async function handleDelete(row: SignLinkInfo) {
  try {
    await ElMessageBox.confirm(
      `确定要删除链接「${row.contractName}」吗？此操作不可撤销！`,
      '删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    ElMessage.success('删除成功')
    refresh()
  } catch {
    // 用户取消
  }
}

/** 获取状态标签类型 */
function getStatusTagType(status: SignLinkStatus): string {
  const map: Record<SignLinkStatus, string> = {
    pending: 'inactive',
    sent: 'warning',
    viewed: 'primary',
    signed: 'success',
    rejected: 'error',
    expired: 'inactive',
  }
  return map[status] || 'info'
}

/** 获取状态标签文字 */
function getStatusLabel(status: SignLinkStatus): string {
  return SignLinkStatusLabelMap[status] || status
}

/** 获取签约类型文字 */
function getSignTypeLabel(type: string): string {
  const opt = SignTypeOptions.find(o => o.value === type)
  return opt?.label || type
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 模拟签约记录和访问记录
const mockSignRecords = computed(() => {
  if (!detailData.value) return []
  return [
    {
      recordId: '1',
      signerName: '张三',
      signTime: '2026-06-20 14:30:00',
      signIp: '192.168.1.100',
      status: 'success',
    },
  ]
})

const mockVisitRecords = computed(() => {
  if (!detailData.value) return []
  return [
    {
      recordId: '1',
      visitTime: '2026-06-20 10:00:00',
      visitIp: '192.168.1.100',
      visitDevice: 'Chrome / Windows',
    },
    {
      recordId: '2',
      visitTime: '2026-06-20 14:25:00',
      visitIp: '192.168.1.100',
      visitDevice: 'Chrome / Windows',
    },
  ]
})

onMounted(() => {
  loadSupplierOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="签署链接" description="管理供应商自助签约链接的生成和跟踪">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>生成链接
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Link" label="链接总数" :value="String(statistics.total)" color-type="primary" variant="bordered" />
      <StatCard icon="Promotion" label="有效链接" :value="String(statistics.active)" color-type="success" variant="bordered" />
      <StatCard icon="CircleCheck" label="已使用" :value="String(statistics.used)" color-type="warning" variant="bordered" />
      <StatCard icon="CircleClose" label="已过期" :value="String(statistics.expired)" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.linkName"
            placeholder="链接名称"
            clearable
            style="width: 180px"
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
            style="width: 180px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="sup in supplierOptions"
              :key="sup.supplierId"
              :label="sup.supplierName"
              :value="sup.supplierId"
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
              v-for="opt in SignLinkStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.createTimeStart"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 150px"
            size="default"
            @change="handleSearch"
          />
          <span class="date-range-separator">至</span>
          <el-date-picker
            v-model="queryForm.createTimeEnd"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 150px"
            size="default"
            @change="handleSearch"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button size="default" @click="handleBatchCreate">
            <el-icon :size="14"><Plus /></el-icon>批量生成
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
        <!-- 链接名称列 -->
        <template #linkName="{ row }">
          <div class="link-name-cell">
            <el-icon class="link-icon"><Link /></el-icon>
            <span class="link-name">{{ row.contractName }}</span>
          </div>
        </template>

        <!-- 供应商列 -->
        <template #supplierName="{ row }">
          <span class="supplier-text">{{ row.supplierName }}</span>
        </template>

        <!-- 签约类型列 -->
        <template #signType="{ row }">
          <span class="sign-type-text">{{ getSignTypeLabel('purchase') }}</span>
        </template>

        <!-- 有效期列 -->
        <template #expireTime="{ row }">
          <span class="expire-text">{{ formatTime(row.expireTime) }}</span>
        </template>

        <!-- 创建时间列 -->
        <template #createTime="{ row }">
          <span class="time-text">{{ formatTime(row.createTime) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag :status="getStatusTagType(row.status)" :label="getStatusLabel(row.status)" size="small" variant="light" />
        </template>

        <!-- 使用次数列 -->
        <template #useCount="{ row }">
          <span class="use-count-text">{{ row.status === 'signed' ? 1 : 0 }}</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleViewDetail(row)">
              详情
            </el-button>
            <el-button link type="primary" size="default" @click.stop="handleCopyLink(row)">
              复制链接
            </el-button>
            <el-button
              v-if="row.status !== 'signed' && row.status !== 'expired' && row.status !== 'rejected'"
              link
              type="warning"
              size="default"
              @click.stop="handleRenew(row)"
            >
              续期
            </el-button>
            <el-button
              v-if="row.status !== 'signed' && row.status !== 'expired' && row.status !== 'rejected'"
              link
              type="danger"
              size="default"
              @click.stop="handleRevoke(row)"
            >
              作废
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

    <!-- 生成链接对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="生成签约链接"
      width="640px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="链接名称" prop="linkName">
          <el-input v-model="formData.linkName" placeholder="请输入链接名称" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="供应商" prop="supplierId">
          <el-select
            v-model="formData.supplierId"
            placeholder="请选择供应商"
            filterable
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="sup in supplierOptions"
              :key="sup.supplierId"
              :label="sup.supplierName"
              :value="sup.supplierId"
            />
          </el-select>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="签约类型" prop="signType">
              <el-select v-model="formData.signType" placeholder="请选择签约类型" style="width: 100%" :teleported="false">
                <el-option
                  v-for="opt in SignTypeOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="有效期(天)" prop="expireDays">
              <el-input-number v-model="formData.expireDays" :min="1" :max="365" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="合同模板" prop="contractTemplateId">
          <el-select
            v-model="formData.contractTemplateId"
            placeholder="请选择合同模板"
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="tpl in ContractTemplateOptions"
              :key="tpl.value"
              :label="tpl.label"
              :value="tpl.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注信息" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定生成
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="链接详情"
      width="720px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" element-loading-text="加载中...">
        <template v-if="detailData">
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="section-title">基本信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="链接名称">{{ detailData.contractName }}</el-descriptions-item>
              <el-descriptions-item label="合同编号">{{ detailData.eContractNo }}</el-descriptions-item>
              <el-descriptions-item label="供应商">{{ detailData.supplierName }}</el-descriptions-item>
              <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
              <el-descriptions-item label="链接状态">
                <StatusTag :status="getStatusTagType(detailData.status)" :label="getStatusLabel(detailData.status)" size="small" variant="light" />
              </el-descriptions-item>
              <el-descriptions-item label="有效期">{{ formatTime(detailData.expireTime) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatTime(detailData.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="创建人">{{ detailData.createBy }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 签约记录 -->
          <div class="detail-section">
            <div class="section-title">签约记录</div>
            <el-table :data="mockSignRecords" size="small" border style="width: 100%">
              <el-table-column prop="signerName" label="签署人" min-width="100" />
              <el-table-column prop="signTime" label="签署时间" min-width="160" />
              <el-table-column prop="signIp" label="签署IP" min-width="120" />
              <el-table-column prop="status" label="状态" min-width="80">
                <template #default="{ row }">
                  <StatusTag v-if="row.status === 'success'" status="success" label="成功" size="small" variant="light" />
                  <StatusTag v-else status="error" label="失败" size="small" variant="light" />
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 链接访问记录 -->
          <div class="detail-section">
            <div class="section-title">链接访问记录</div>
            <el-table :data="mockVisitRecords" size="small" border style="width: 100%">
              <el-table-column prop="visitTime" label="访问时间" min-width="160" />
              <el-table-column prop="visitIp" label="访问IP" min-width="120" />
              <el-table-column prop="visitDevice" label="访问设备" min-width="180" />
            </el-table>
          </div>

          <!-- 拒绝原因 -->
          <div v-if="detailData.rejectReason" class="detail-section">
            <div class="section-title">拒绝原因</div>
            <el-alert type="error" :closable="false">
              <template #title>{{ detailData.rejectReason }}</template>
            </el-alert>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
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

.date-range-separator {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-base);
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

// 链接名称单元格
.link-name-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  white-space: nowrap;
  overflow: hidden;
}

.link-icon {
  color: var(--fts-primary);
  font-size: 16px;
}

.link-name {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

// 供应商文字
.supplier-text {
  color: var(--fts-text-primary);
}

// 签约类型文字
.sign-type-text {
  color: var(--fts-text-primary);
}

// 有效期文字
.expire-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 时间文字
.time-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-secondary);
}

// 使用次数文字
.use-count-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
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
  margin-bottom: var(--fts-space-4);

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
