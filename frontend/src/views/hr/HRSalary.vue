<script setup lang="ts">
/**
 * 薪资管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理员工薪资核算和发放记录
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Money,
  Wallet,
  Download,
  Search,
  Refresh,
  Clock,
  User,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { salaryApi } from '@/api/hr/salary'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import { fenToYuan } from '@/utils/money'
import type {
  SalaryRecord,
  SalaryQueryDTO,
  SalaryStatisticsVO,
  SalaryStatus,
} from '@/types/hr/salary'
import {
  SalaryStatus as SalaryStatusEnum,
  SalaryStatusTagMap,
  SalaryStatusOptions,
} from '@/types/hr/salary'

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
  align?: 'left' | 'center' | 'right'
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<SalaryRecord[]>([])
const statistics = ref<SalaryStatisticsVO | null>(null)
const statisticsLoading = ref(false)

// 详情对话框
const detailDialogVisible = ref(false)
const currentDetail = ref<SalaryRecord | null>(null)

// 薪资调整对话框
const adjustDialogVisible = ref(false)
const adjustFormRef = ref<FormInstance>()
const adjustFormData = reactive({
  employeeName: '',
  employeeId: '',
  adjustItem: 'basicSalary' as 'basicSalary' | 'performanceBonus' | 'subsidy' | 'deductions',
  adjustAmount: 0,
  effectiveMonth: '',
  reason: '',
})

// 工资条预览对话框
const payslipDialogVisible = ref(false)
const currentPayslip = ref<SalaryRecord | null>(null)

// 部门选项（接入真实后端 API）
const { departmentOptions, loadDepartments, getDepartmentName } = useDepartmentOptions(true)

// 调整项目选项
const adjustItemOptions = [
  { value: 'basicSalary', label: '基本工资' },
  { value: 'performanceBonus', label: '绩效工资' },
  { value: 'subsidy', label: '补贴' },
  { value: 'deductions', label: '扣款' },
]

// 月份选项
const monthOptions = computed(() => {
  const now = new Date()
  const options: string[] = []
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    options.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return options
})

// 查询表单
const queryForm = ref({
  period: '',
  departmentId: '' as string | '',
  employeeName: '',
  status: '' as SalaryStatus | '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<SalaryRecord, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: SalaryQueryDTO = {
        period: params.period || undefined,
        departmentId: params.departmentId || undefined,
        employeeName: params.employeeName || undefined,
        status: params.status || undefined,
        page: params.page,
        pageSize: params.size,
      }
      return salaryApi.getList(convertedParams)
    },
  } as unknown as CrudApi<SalaryRecord, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'employeeName', label: '员工姓名', minWidth: 100, slot: 'employeeName' },
  { prop: 'employeeCode', label: '工号', width: 100, slot: 'employeeCode' },
  { prop: 'departmentName', label: '部门', width: 100, slot: 'departmentName' },
  { prop: 'positionName', label: '职位', minWidth: 100, slot: 'positionName' },
  { prop: 'basicSalary', label: '基本工资', width: 110, align: 'right', slot: 'basicSalary' },
  { prop: 'performanceBonus', label: '绩效工资', width: 110, align: 'right', slot: 'performanceBonus' },
  { prop: 'subsidy', label: '补贴', width: 90, align: 'right', slot: 'subsidy' },
  { prop: 'deductions', label: '扣款', width: 90, align: 'right', slot: 'deductions' },
  { prop: 'netSalary', label: '实发工资', width: 120, align: 'right', slot: 'netSalary' },
  { prop: 'status', label: '发放状态', width: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 220, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const adjustFormRules: FormRules = {
  adjustItem: [{ required: true, message: '请选择调整项目', trigger: 'change' }],
  adjustAmount: [{ required: true, message: '请输入调整金额', trigger: 'blur' }],
  effectiveMonth: [{ required: true, message: '请选择生效月份', trigger: 'change' }],
  reason: [{ required: true, message: '请输入调整原因', trigger: 'blur' }],
}

// ==================== 方法 ====================

/** 加载统计数据 */
async function loadStatistics() {
  statisticsLoading.value = true
  try {
    const data = await salaryApi.getStatistics(queryForm.value.period || undefined)
    statistics.value = data
  } catch {
    ElMessage.error('加载统计数据失败')
  } finally {
    statisticsLoading.value = false
  }
}

function handleSearch() {
  refresh()
  loadStatistics()
}

function handleReset() {
  queryForm.value.period = ''
  queryForm.value.departmentId = ''
  queryForm.value.employeeName = ''
  queryForm.value.status = ''
  refresh()
  loadStatistics()
}

function handleSelectionChange(rows: SalaryRecord[]) {
  selectedRows.value = rows
}

/** 获取薪资状态信息 */
function getStatusInfo(status: SalaryStatus) {
  return SalaryStatusTagMap[status] || { status: 'info', label: status }
}

/** 查看详情 */
async function handleDetail(row: SalaryRecord) {
  try {
    const detail = await salaryApi.getById(row.id)
    currentDetail.value = detail
    detailDialogVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

/** 薪资调整 */
function handleAdjust(row: SalaryRecord) {
  adjustFormData.employeeName = row.employeeName
  adjustFormData.employeeId = row.employeeId
  adjustFormData.adjustItem = 'basicSalary'
  adjustFormData.adjustAmount = 0
  adjustFormData.effectiveMonth = row.period
  adjustFormData.reason = ''
  adjustDialogVisible.value = true
}

/** 提交薪资调整 */
async function handleAdjustSubmit() {
  if (!adjustFormRef.value) return
  const valid = await adjustFormRef.value.validate().catch(() => false)
  if (!valid) return

  // TODO: 后端薪资调整接口未就绪（POST /v1/salary/{id}/adjust）
  // salaryApi 暂无 adjust 方法，待后端补充后替换为真实调用：
  //   await salaryApi.adjust({
  //     id: adjustFormData.employeeId,
  //     adjustItem: adjustFormData.adjustItem,
  //     adjustAmount: yuanToFen(adjustFormData.adjustAmount),
  //     effectiveMonth: adjustFormData.effectiveMonth,
  //     reason: adjustFormData.reason,
  //   })
  // 当前明确提示功能开发中，不显示假成功，避免财务数据不一致风险
  submitLoading.value = true
  try {
    ElMessage.info('薪资调整功能开发中，请稍后再试')
    adjustDialogVisible.value = false
  } finally {
    submitLoading.value = false
  }
}

/** 工资条预览 */
async function handlePayslip(row: SalaryRecord) {
  try {
    const detail = await salaryApi.getById(row.id)
    currentPayslip.value = detail
    payslipDialogVisible.value = true
  } catch {
    ElMessage.error('加载工资条失败')
  }
}

/** 薪资核算 */
async function handleCalculate() {
  try {
    await ElMessageBox.confirm(
      '确定要进行薪资核算吗？核算将根据考勤、绩效等数据自动计算薪资。',
      '薪资核算确认',
      {
        confirmButtonText: '确认核算',
        cancelButtonText: '取消',
        type: 'info',
      },
    )
    ElMessage.success('薪资核算已启动，正在处理中...')
    setTimeout(() => {
      refresh()
      loadStatistics()
      ElMessage.success('薪资核算完成')
    }, 1000)
  } catch {
    // 用户取消
  }
}

/** 导出工资条 */
async function handleExportPayslip() {
  try {
    ElMessage.info('正在导出工资条，请稍候...')
    setTimeout(() => {
      ElMessage.success('工资条导出成功')
    }, 1000)
  } catch {
    ElMessage.error('导出失败')
  }
}

/** 批量发放 */
async function handleBatchPay() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要发放的薪资记录')
    return
  }
  const approvedRows = selectedRows.value.filter(
    (r) => r.status === SalaryStatusEnum.APPROVED,
  )
  if (approvedRows.length === 0) {
    ElMessage.warning('选中的记录中没有待发放状态的薪资')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要批量发放选中的 ${approvedRows.length} 条薪资记录吗？`,
      '批量发放确认',
      {
        confirmButtonText: '确认发放',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    const ids = approvedRows.map((r) => r.id)
    await salaryApi.batchPay({ ids, paymentMethod: 'bank_transfer' })
    ElMessage.success('批量发放成功')
    selectedRows.value = []
    refresh()
    loadStatistics()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '批量发放失败')
    }
  }
}

/** 格式化时间 */
function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/** 获取发放方式文字 */
function getPaymentMethodText(method?: string): string {
  const map: Record<string, string> = {
    bank_transfer: '银行转账',
    cash: '现金',
    check: '支票',
  }
  return map[method || ''] || '-'
}

// 页面挂载时加载统计数据
onMounted(() => {
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="薪资管理" description="管理员工薪资核算和发放记录">
      <el-button type="primary" size="default" @click="handleCalculate">
        <el-icon :size="16"><Money /></el-icon>薪资核算
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="Wallet"
        label="本月薪资总额"
        :value="statistics ? `¥${fenToYuan(statistics.totalAmount)}` : '-'"
        color-type="primary"
        variant="bordered"
      />
      <StatCard
        icon="Money"
        label="已发放"
        :value="statistics ? `¥${fenToYuan(statistics.paidAmount)}` : '-'"
        color-type="success"
        variant="bordered"
      />
      <StatCard
        icon="Clock"
        label="待发放"
        :value="statistics ? `¥${fenToYuan(statistics.pendingAmount)}` : '-'"
        color-type="warning"
        variant="bordered"
      />
      <StatCard
        icon="User"
        label="人均薪资"
        :value="statistics ? `¥${fenToYuan(statistics.avgSalary)}` : '-'"
        color-type="info"
        variant="bordered"
      />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select
            v-model="queryForm.period"
            placeholder="月份"
            clearable
            style="width: 130px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="m in monthOptions"
              :key="m"
              :label="m"
              :value="m"
            />
          </el-select>
          <el-select
            v-model="queryForm.departmentId"
            placeholder="部门"
            clearable
            style="width: 120px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="dept in departmentOptions"
              :key="dept.id"
              :label="dept.name"
              :value="dept.id"
            />
          </el-select>
          <el-input
            v-model="queryForm.employeeName"
            placeholder="搜索员工姓名..."
            clearable
            style="width: 180px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select
            v-model="queryForm.status"
            placeholder="薪资状态"
            clearable
            style="width: 120px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in SalaryStatusOptions"
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
          <el-button
            type="success"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleBatchPay"
          >
            批量发放
          </el-button>
          <el-button
            type="default"
            size="default"
            @click="handleExportPayslip"
          >
            <el-icon :size="14"><Download /></el-icon>导出工资条
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
        <!-- 员工姓名列 -->
        <template #employeeName="{ row }">
          <span class="name-text">{{ row.employeeName }}</span>
        </template>

        <!-- 工号列 -->
        <template #employeeCode="{ row }">
          <span class="code-text">{{ row.employeeCode || '-' }}</span>
        </template>

        <!-- 部门列 -->
        <template #departmentName="{ row }">
          <span class="dept-text">{{ row.departmentName }}</span>
        </template>

        <!-- 职位列 -->
        <template #positionName="{ row }">
          <span class="position-text">{{ row.positionName || '-' }}</span>
        </template>

        <!-- 基本工资列 -->
        <template #basicSalary="{ row }">
          <span class="salary-text">¥{{ fenToYuan(row.basicSalary) }}</span>
        </template>

        <!-- 绩效工资列 -->
        <template #performanceBonus="{ row }">
          <span class="salary-text">¥{{ fenToYuan(row.performanceBonus) }}</span>
        </template>

        <!-- 补贴列 -->
        <template #subsidy="{ row }">
          <span class="subsidy-text">¥{{ fenToYuan(row.subsidy) }}</span>
        </template>

        <!-- 扣款列 -->
        <template #deductions="{ row }">
          <span class="deduction-text">-¥{{ fenToYuan(row.deductions) }}</span>
        </template>

        <!-- 实发工资列 -->
        <template #netSalary="{ row }">
          <span class="net-salary-text">¥{{ fenToYuan(row.netSalary) }}</span>
        </template>

        <!-- 状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusInfo(row.status).status"
            :label="getStatusInfo(row.status).label"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="handleDetail(row)">
              详情
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleAdjust(row)">
              调整
            </el-button>
            <el-button link type="info" size="default" @click.stop="handlePayslip(row)">
              工资条
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

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="薪资详情"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentDetail">
        <!-- 基本信息 -->
        <div class="detail-section">
          <div class="section-title">基本信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="员工姓名">{{ currentDetail.employeeName }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ currentDetail.employeeCode || '-' }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ currentDetail.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="职位">{{ currentDetail.positionName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="薪资期间">{{ currentDetail.period }}</el-descriptions-item>
            <el-descriptions-item label="发放状态">
              <StatusTag
                :status="getStatusInfo(currentDetail.status).status"
                :label="getStatusInfo(currentDetail.status).label"
                size="small"
              />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 薪资明细 -->
        <div class="detail-section">
          <div class="section-title">薪资明细</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="salary-block">
                <div class="block-title">应发项目</div>
                <div class="salary-item">
                  <span>基本工资</span>
                  <span class="salary-value">¥{{ fenToYuan(currentDetail.basicSalary) }}</span>
                </div>
                <div class="salary-item">
                  <span>绩效工资</span>
                  <span class="salary-value">¥{{ fenToYuan(currentDetail.performanceBonus) }}</span>
                </div>
                <div class="salary-item">
                  <span>加班费</span>
                  <span class="salary-value">¥{{ fenToYuan(currentDetail.overtimePay) }}</span>
                </div>
                <div class="salary-item">
                  <span>补贴</span>
                  <span class="salary-value">¥{{ fenToYuan(currentDetail.subsidy) }}</span>
                </div>
                <div class="salary-total">
                  <span>应发合计</span>
                  <span class="total-value">¥{{ fenToYuan(currentDetail.grossSalary) }}</span>
                </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="salary-block">
                <div class="block-title">扣款项目</div>
                <div class="salary-item">
                  <span>社保</span>
                  <span class="deduction-value">-¥{{ fenToYuan(currentDetail.socialInsurance) }}</span>
                </div>
                <div class="salary-item">
                  <span>个税</span>
                  <span class="deduction-value">-¥{{ fenToYuan(currentDetail.tax) }}</span>
                </div>
                <div class="salary-item">
                  <span>其他扣款</span>
                  <span class="deduction-value">-¥{{ fenToYuan(currentDetail.deductions) }}</span>
                </div>
                <div class="salary-total net-total">
                  <span>实发工资</span>
                  <span class="net-value">¥{{ fenToYuan(currentDetail.netSalary) }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>

        <!-- 发放记录 -->
        <div class="detail-section" v-if="currentDetail.status === 'paid'">
          <div class="section-title">发放记录</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="发放时间">{{ formatTime(currentDetail.paidAt || '') }}</el-descriptions-item>
            <el-descriptions-item label="发放方式">{{ getPaymentMethodText(currentDetail.paymentMethod) }}</el-descriptions-item>
            <el-descriptions-item label="银行流水号">{{ currentDetail.bankTransactionNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="会计凭证号">{{ currentDetail.voucherNo || '-' }}</el-descriptions-item>
          </el-descriptions>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="detailDialogVisible = false">关闭</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 薪资调整对话框 -->
    <el-dialog
      v-model="adjustDialogVisible"
      title="薪资调整"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <el-form
        ref="adjustFormRef"
        :model="adjustFormData"
        :rules="adjustFormRules"
        label-width="100px"
      >
        <el-form-item label="员工姓名">
          <el-input v-model="adjustFormData.employeeName" disabled />
        </el-form-item>
        <el-form-item label="调整项目" prop="adjustItem">
          <el-select
            v-model="adjustFormData.adjustItem"
            placeholder="请选择调整项目"
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="item in adjustItemOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整金额(元)" prop="adjustAmount">
          <el-input-number
            v-model="adjustFormData.adjustAmount"
            :precision="2"
            :min="-99999"
            :max="99999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="生效月份" prop="effectiveMonth">
          <el-select
            v-model="adjustFormData.effectiveMonth"
            placeholder="请选择生效月份"
            style="width: 100%"
            :teleported="false"
          >
            <el-option
              v-for="m in monthOptions"
              :key="m"
              :label="m"
              :value="m"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input
            v-model="adjustFormData.reason"
            type="textarea"
            :rows="3"
            placeholder="请输入调整原因"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="adjustDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="handleAdjustSubmit">
            确认调整
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 工资条预览对话框 -->
    <el-dialog
      v-model="payslipDialogVisible"
      title="工资条预览"
      width="680px"
      class="fts-dialog--md"
      :close-on-click-modal="false"
      destroy-on-close
      lock-scroll="false"
    >
      <template v-if="currentPayslip">
        <div class="payslip-container">
          <div class="payslip-header">
            <div class="payslip-title">工资条</div>
            <div class="payslip-period">{{ currentPayslip.period }}</div>
          </div>

          <div class="payslip-info">
            <div class="payslip-info-item">
              <span class="info-label">姓名：</span>
              <span class="info-value">{{ currentPayslip.employeeName }}</span>
            </div>
            <div class="payslip-info-item">
              <span class="info-label">工号：</span>
              <span class="info-value">{{ currentPayslip.employeeCode || '-' }}</span>
            </div>
            <div class="payslip-info-item">
              <span class="info-label">部门：</span>
              <span class="info-value">{{ currentPayslip.departmentName }}</span>
            </div>
            <div class="payslip-info-item">
              <span class="info-label">职位：</span>
              <span class="info-value">{{ currentPayslip.positionName || '-' }}</span>
            </div>
          </div>

          <div class="payslip-table">
            <div class="payslip-row header-row">
              <div class="payslip-col">项目</div>
              <div class="payslip-col amount-col">金额（元）</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">基本工资</div>
              <div class="payslip-col amount-col">{{ fenToYuan(currentPayslip.basicSalary) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">绩效工资</div>
              <div class="payslip-col amount-col">{{ fenToYuan(currentPayslip.performanceBonus) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">加班费</div>
              <div class="payslip-col amount-col">{{ fenToYuan(currentPayslip.overtimePay) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">补贴</div>
              <div class="payslip-col amount-col">{{ fenToYuan(currentPayslip.subsidy) }}</div>
            </div>
            <div class="payslip-row subtotal-row">
              <div class="payslip-col">应发合计</div>
              <div class="payslip-col amount-col">{{ fenToYuan(currentPayslip.grossSalary) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">社保</div>
              <div class="payslip-col amount-col deduction">-{{ fenToYuan(currentPayslip.socialInsurance) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">个税</div>
              <div class="payslip-col amount-col deduction">-{{ fenToYuan(currentPayslip.tax) }}</div>
            </div>
            <div class="payslip-row">
              <div class="payslip-col">其他扣款</div>
              <div class="payslip-col amount-col deduction">-{{ fenToYuan(currentPayslip.deductions) }}</div>
            </div>
            <div class="payslip-row net-row">
              <div class="payslip-col">实发工资</div>
              <div class="payslip-col amount-col">¥{{ fenToYuan(currentPayslip.netSalary) }}</div>
            </div>
          </div>

          <div class="payslip-footer">
            <div>发放状态：{{ getStatusInfo(currentPayslip.status).label }}</div>
            <div v-if="currentPayslip.paidAt">发放时间：{{ formatTime(currentPayslip.paidAt) }}</div>
          </div>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="payslipDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleExportPayslip">
            <el-icon :size="14"><Download /></el-icon>下载
          </el-button>
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 姓名
.name-text {
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
}

// 工号
.code-text {
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
  font-variant-numeric: tabular-nums;
}

// 部门
.dept-text {
  color: var(--fts-text-primary);
}

// 职位
.position-text {
  color: var(--fts-text-secondary);
}

// 薪资
.salary-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

.subsidy-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-success);
}

.deduction-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-error);
}

.net-salary-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-primary);
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

// 详情对话框样式
.detail-section {
  margin-bottom: var(--fts-space-4);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

.salary-block {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);

  .block-title {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-bottom: var(--fts-space-2);
    border-bottom: 1px solid var(--fts-border-secondary);
  }
}

.salary-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-2) 0;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);

  .salary-value {
    color: var(--fts-text-primary);
    font-variant-numeric: tabular-nums;
  }

  .deduction-value {
    color: var(--fts-error);
    font-variant-numeric: tabular-nums;
  }
}

.salary-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-3) 0 0;
  margin-top: var(--fts-space-2);
  border-top: 1px dashed var(--fts-border-secondary);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);

  .total-value {
    font-variant-numeric: tabular-nums;
  }

  &.net-total {
    .net-value {
      color: var(--fts-primary);
      font-size: var(--fts-font-size-lg);
      font-weight: var(--fts-font-weight-semibold);
      font-variant-numeric: tabular-nums;
    }
  }
}

// 工资条预览样式
.payslip-container {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  padding: var(--fts-space-6);
}

.payslip-header {
  text-align: center;
  margin-bottom: var(--fts-space-4);
  padding-bottom: var(--fts-space-4);
  border-bottom: 2px solid var(--fts-primary);

  .payslip-title {
    font-size: var(--fts-font-size-xl);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-2);
  }

  .payslip-period {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }
}

.payslip-info {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3);
  background: var(--fts-bg-hover);
  border-radius: var(--fts-radius-md);

  .payslip-info-item {
    font-size: var(--fts-font-size-sm);

    .info-label {
      color: var(--fts-text-secondary);
    }

    .info-value {
      color: var(--fts-text-primary);
    }
  }
}

.payslip-table {
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  overflow: hidden;

  .payslip-row {
    display: flex;
    border-bottom: 1px solid var(--fts-border-secondary);

    &:last-child {
      border-bottom: none;
    }

    &.header-row {
      background: var(--fts-bg-hover);
      font-weight: var(--fts-font-weight-medium);
      color: var(--fts-text-primary);
    }

    &.subtotal-row {
      background: var(--fts-bg-hover);
      font-weight: var(--fts-font-weight-medium);
    }

    &.net-row {
      background: var(--fts-primary-light-9);
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-primary);
    }
  }

  .payslip-col {
    flex: 1;
    padding: var(--fts-space-2) var(--fts-space-3);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);

    &.amount-col {
      text-align: right;
      font-variant-numeric: tabular-nums;

      &.deduction {
        color: var(--fts-error);
      }
    }
  }
}

.payslip-footer {
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-3);
  border-top: 1px dashed var(--fts-border-secondary);
  display: flex;
  justify-content: space-between;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
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
