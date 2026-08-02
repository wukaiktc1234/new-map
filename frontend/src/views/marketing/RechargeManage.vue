<script setup lang="ts">
/**
 * 充值管理页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理会员充值订单和充值活动
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Download, Search, Refresh, Wallet, Money, TrendCharts, Coin } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { rechargeRecordApi, rechargeStatsApi, rechargePlanApi, refundApi } from '@/api/marketing/recharge'
import { memberApi } from '@/api/marketing/member'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  RechargeRecordInfo,
  RechargeRecordQueryForm,
  RechargePaymentMethod,
  RechargePaymentStatus,
  RechargePlanInfo,
  RechargeStatsOverview,
} from '@/types/member-recharge'
import type { MemberQueryForm } from '@/types/member'

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

interface MemberOption {
  memberId: string
  memberName: string
  memberPhone: string
  cardNo: string
}

// ==================== 状态映射 ====================

const paymentMethodText: Record<string, string> = {
  wechat: '微信支付',
  alipay: '支付宝',
  cash: '现金',
  bank_card: '银行卡',
  balance: '余额支付',
}

const paymentStatusText: Record<string, string> = {
  pending: '待支付',
  success: '已支付',
  failed: '支付失败',
  refunded: '已退款',
  partial_refunded: '部分退款',
}

const paymentStatusTag: Record<string, string> = {
  pending: 'pending',
  success: 'success',
  failed: 'error',
  refunded: 'warning',
  partial_refunded: 'warning',
}

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<RechargeRecordInfo[]>([])
const createDialogVisible = ref(false)
const detailDialogVisible = ref(false)
const refundDialogVisible = ref(false)
const planDialogVisible = ref(false)

const formRef = ref<FormInstance>()
const refundFormRef = ref<FormInstance>()

// 统计数据
const stats = ref<RechargeStatsOverview | null>(null)

// 充值方案列表
const planList = ref<RechargePlanInfo[]>([])

// 会员列表（从后端 API 加载）
const memberList = ref<MemberOption[]>([])

// 查询表单
const queryForm = ref({
  keyword: '',
  recordNo: '',
  paymentMethod: '' as RechargePaymentMethod | '',
  paymentStatus: '' as RechargePaymentStatus | '',
  dateRange: [] as string[],
})

// 新增充值表单
const rechargeForm = reactive({
  memberId: '',
  memberName: '',
  rechargeAmount: 0,
  bonusAmount: 0,
  paymentMethod: 'wechat' as RechargePaymentMethod,
  remark: '',
})

const rechargeFormRules: FormRules = {
  memberId: [{ required: true, message: '请选择会员', trigger: 'change' }],
  rechargeAmount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (value <= 0) {
          callback(new Error('充值金额必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  paymentMethod: [{ required: true, message: '请选择支付方式', trigger: 'change' }],
}

// 详情数据
const detailRecord = ref<RechargeRecordInfo | null>(null)

// 退款表单
const refundForm = reactive({
  refundAmount: 0,
  refundReason: '',
})

const refundFormRules: FormRules = {
  refundAmount: [
    { required: true, message: '请输入退款金额', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (error?: Error) => void) => {
        if (value <= 0) {
          callback(new Error('退款金额必须大于0'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  refundReason: [{ required: true, message: '请输入退款原因', trigger: 'blur' }],
}

// 当前退款记录
const refundRecord = ref<RechargeRecordInfo | null>(null)

// ==================== useCrudTable ====================

const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<RechargeRecordInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      // 调用真实后端 API：GET /v1/recharge-records
      const res = await rechargeRecordApi.getPage({
        page: params.page,
        size: params.size,
        keyword: params.keyword || undefined,
        recordNo: params.recordNo || undefined,
        paymentMethod: params.paymentMethod || undefined,
        paymentStatus: params.paymentStatus || undefined,
      } as RechargeRecordQueryForm & { page: number; size: number })
      return {
        records: res?.records || [],
        total: res?.total || 0,
      }
    },
  } as unknown as CrudApi<RechargeRecordInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// ==================== 计算属性 ====================

const statistics = computed(() => ({
  todayAmount: stats.value?.rechargeAmountToday || '0.00',
  todayCount: stats.value?.rechargeCountToday || 0,
  monthAmount: stats.value?.rechargeThisMonth || '0.00',
  totalAmount: stats.value?.avgRechargeAmount || '0.00',
}))

const columns = computed<ColumnDef[]>(() => [
  { prop: 'recordNo', label: '充值单号', minWidth: 160 },
  { prop: 'memberName', label: '会员姓名', minWidth: 100 },
  { prop: 'memberPhone', label: '会员卡号', minWidth: 120, slot: 'cardNo' },
  { prop: 'rechargeAmount', label: '充值金额', minWidth: 110, align: 'right', slot: 'rechargeAmount' },
  { prop: 'bonusAmount', label: '赠送金额', minWidth: 110, align: 'right', slot: 'bonusAmount' },
  { prop: 'paymentMethod', label: '支付方式', minWidth: 100, slot: 'paymentMethod' },
  { prop: 'paymentTime', label: '充值时间', minWidth: 160 },
  { prop: 'paymentStatus', label: '充值状态', minWidth: 100, slot: 'paymentStatus' },
  { prop: 'operator', label: '操作人', minWidth: 90, slot: 'operator' },
  { prop: '_operation', label: '操作', width: 160, fixed: 'right', slot: 'operation' },
])

// ==================== 方法 ====================

function handleSearch() {
  pagination.current = 1
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.recordNo = ''
  queryForm.value.paymentMethod = ''
  queryForm.value.paymentStatus = ''
  queryForm.value.dateRange = []
  pagination.current = 1
  refresh()
}

function handleSelectionChange(rows: RechargeRecordInfo[]) {
  selectedRows.value = rows
}

// 打开新增充值对话框
function openCreateDialog() {
  Object.assign(rechargeForm, {
    memberId: '',
    memberName: '',
    rechargeAmount: 0,
    bonusAmount: 0,
    paymentMethod: 'wechat' as RechargePaymentMethod,
    remark: '',
  })
  createDialogVisible.value = true
}

// 会员选择变化
function onMemberChange(memberId: string) {
  const member = memberList.value.find(m => m.memberId === memberId)
  if (member) {
    rechargeForm.memberName = member.memberName
  }
}

// 充值金额变化，自动计算赠送金额
watch(
  () => rechargeForm.rechargeAmount,
  (amount) => {
    // 简单的赠送规则：充100送10，充500送50，充1000送100，充2000送300
    if (amount >= 2000) {
      rechargeForm.bonusAmount = Math.floor(amount * 0.15)
    } else if (amount >= 1000) {
      rechargeForm.bonusAmount = Math.floor(amount * 0.1)
    } else if (amount >= 500) {
      rechargeForm.bonusAmount = Math.floor(amount * 0.1)
    } else if (amount >= 100) {
      rechargeForm.bonusAmount = Math.floor(amount * 0.1)
    } else {
      rechargeForm.bonusAmount = 0
    }
  }
)

// 提交新增充值
async function handleSubmitRecharge() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 调用真实后端 API：POST /v1/recharge-records/recharge
    const plan = planList.value.find(p => p.rechargeAmount === String(rechargeForm.rechargeAmount))
    if (!plan) {
      ElMessage.warning('未找到匹配的充值方案，请先在充值活动设置中创建')
      return
    }
    await rechargeRecordApi.recharge(
      rechargeForm.memberId,
      plan.planId,
      rechargeForm.paymentMethod,
    )
    ElMessage.success('充值成功')
    createDialogVisible.value = false
    refresh()
    loadStats()
  } catch {
    ElMessage.error('充值失败')
  } finally {
    submitLoading.value = false
  }
}

// 打开详情对话框
function openDetailDialog(row: RechargeRecordInfo) {
  detailRecord.value = row
  detailDialogVisible.value = true
}

// 打开退款对话框
function openRefundDialog(row: RechargeRecordInfo) {
  refundRecord.value = row
  refundForm.refundAmount = parseFloat(row.principalAmount)
  refundForm.refundReason = ''
  refundDialogVisible.value = true
}

// 提交退款
async function handleSubmitRefund() {
  if (!refundFormRef.value) return
  const valid = await refundFormRef.value.validate().catch(() => false)
  if (!valid) return

  if (!refundRecord.value) return

  try {
    await ElMessageBox.confirm(
      `确定要对充值单「${refundRecord.value.recordNo}」退款 ¥${refundForm.refundAmount.toFixed(2)} 吗？`,
      '退款确认',
      {
        confirmButtonText: '确定退款',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    submitLoading.value = true
    // 调用真实后端 API：POST /v1/refunds（创建退款申请）
    await refundApi.create({
      rechargeRecordId: refundRecord.value.recordId,
      requestedAmount: refundForm.refundAmount.toFixed(2),
      refundReason: refundForm.refundReason,
    })
    ElMessage.success('退款申请已提交')
    refundDialogVisible.value = false
    refresh()
    loadStats()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error('退款失败')
    }
  } finally {
    submitLoading.value = false
  }
}

// 导出充值记录
async function handleExport() {
  try {
    ElMessage.info('正在导出数据，请稍候...')
    // 调用真实后端 API：POST /v1/recharge-records/export
    await rechargeRecordApi.export({
      keyword: queryForm.value.keyword || undefined,
      recordNo: queryForm.value.recordNo || undefined,
      paymentMethod: queryForm.value.paymentMethod || undefined,
      paymentStatus: queryForm.value.paymentStatus || undefined,
    } as RechargeRecordQueryForm)
    ElMessage.success('导出成功！')
  } catch {
    ElMessage.error('导出失败')
  }
}

// 打开充值活动设置
function openPlanSettings() {
  planDialogVisible.value = true
}

// 加载统计数据
async function loadStats() {
  try {
    // 调用真实后端 API：GET /v1/recharge-stats/overview
    const data = await rechargeStatsApi.getOverview()
    stats.value = data
  } catch {
    // API 失败时置空，统计卡片会显示默认值 0
    stats.value = null
  }
}

// 加载充值方案
async function loadPlans() {
  try {
    const data = await rechargePlanApi.getList()
    planList.value = data
  } catch {
    planList.value = []
  }
}

// 加载会员列表（用于充值时选择会员）
async function loadMembers() {
  try {
    // 调用真实后端 API：GET /v1/members
    const res = await memberApi.getList({ page: 1, size: 100 } as MemberQueryForm)
    memberList.value = (res?.records || []).map(m => ({
      memberId: m.id,
      memberName: m.nickname || m.memberNo,
      memberPhone: m.phone,
      cardNo: m.memberNo,
    }))
  } catch {
    memberList.value = []
  }
}

// 获取会员卡号
function getMemberCardNo(memberPhone: string): string {
  const member = memberList.value.find(m => m.memberPhone === memberPhone)
  return member?.cardNo || '-'
}

// 格式化金额
function formatAmount(amount: string | number): string {
  const num = typeof amount === 'string' ? parseFloat(amount) : amount
  return `¥${num.toFixed(2)}`
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadStats()
  loadPlans()
  loadMembers()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="储值管理" description="管理会员充值订单和充值活动">
      <el-button type="primary" size="default" @click="openCreateDialog">
        <el-icon :size="16"><Plus /></el-icon>新增充值
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard icon="Wallet" label="今日充值额" :value="`¥${statistics.todayAmount}`" color-type="primary" variant="bordered" />
      <StatCard icon="Coin" label="今日充值笔数" :value="String(statistics.todayCount)" color-type="success" variant="bordered" />
      <StatCard icon="Money" label="本月充值额" :value="`¥${statistics.monthAmount}`" color-type="warning" variant="bordered" />
      <StatCard icon="TrendCharts" label="平均充值金额" :value="`¥${statistics.totalAmount}`" color-type="info" variant="bordered" />
    </section>

    <!-- 工具栏面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-input
            v-model="queryForm.keyword"
            placeholder="会员姓名/手机号"
            clearable
            style="width: 200px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-input
            v-model="queryForm.recordNo"
            placeholder="充值单号"
            clearable
            style="width: 160px"
            size="default"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="queryForm.paymentMethod"
            placeholder="支付方式"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option v-for="(v, k) in paymentMethodText" :key="k" :label="v" :value="k" />
          </el-select>
          <el-select
            v-model="queryForm.paymentStatus"
            placeholder="充值状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option v-for="(v, k) in paymentStatusText" :key="k" :label="v" :value="k" />
          </el-select>
          <el-date-picker
            v-model="queryForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 240px"
            size="default"
            :teleported="false"
          />
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">
            <el-icon :size="14"><Refresh /></el-icon>重置
          </el-button>
          <el-button size="default" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出充值记录
          </el-button>
          <el-button type="success" size="default" @click="openPlanSettings">
            充值活动设置
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
        <!-- 会员卡号列 -->
        <template #cardNo="{ row }">
          <span class="card-no-text">{{ getMemberCardNo(row.memberPhone) }}</span>
        </template>

        <!-- 充值金额列 -->
        <template #rechargeAmount="{ row }">
          <span class="amount-text">{{ formatAmount(row.rechargeAmount) }}</span>
        </template>

        <!-- 赠送金额列 -->
        <template #bonusAmount="{ row }">
          <span class="bonus-text">{{ formatAmount(row.bonusAmount) }}</span>
        </template>

        <!-- 支付方式列 -->
        <template #paymentMethod="{ row }">
          <span>{{ paymentMethodText[row.paymentMethod] || row.paymentMethod }}</span>
        </template>

        <!-- 充值状态列 -->
        <template #paymentStatus="{ row }">
          <StatusTag
            :status="paymentStatusTag[row.paymentStatus]"
            :label="paymentStatusText[row.paymentStatus]"
            size="small"
            variant="light"
          />
        </template>

        <!-- 操作人列 -->
        <template #operator="{ row }">
          <span class="operator-text">管理员</span>
        </template>

        <!-- 操作列 -->
        <template #operation="{ row }">
          <div class="action-text">
            <el-button link type="primary" size="default" @click.stop="openDetailDialog(row)">
              详情
            </el-button>
            <el-button
              v-if="row.paymentStatus === 'success' && row.refundStatus === 'none'"
              link
              type="danger"
              size="default"
              @click.stop="openRefundDialog(row)"
            >
              退款
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
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="(s: number) => { pagination.pageSize = s; pagination.current = 1; refresh() }"
          @current-change="(p: number) => { pagination.current = p; refresh() }"
        />
      </div>
    </section>

    <!-- 新增充值对话框 -->
    <el-dialog
      v-model="createDialogVisible"
      title="新增充值"
      width="600px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="rechargeForm" :rules="rechargeFormRules" label-width="100px">
        <el-form-item label="选择会员" prop="memberId">
          <el-select
            v-model="rechargeForm.memberId"
            placeholder="请选择会员"
            filterable
            style="width: 100%"
            :teleported="false"
            @change="onMemberChange"
          >
            <el-option
              v-for="member in memberList"
              :key="member.memberId"
              :label="`${member.memberName}（${member.memberPhone}）`"
              :value="member.memberId"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="充值金额" prop="rechargeAmount">
          <el-input-number
            v-model="rechargeForm.rechargeAmount"
            :min="0"
            :precision="2"
            :step="100"
            controls-position="right"
            style="width: 200px"
          />
          <span class="form-unit">元</span>
        </el-form-item>

        <el-form-item label="赠送金额">
          <el-input-number
            v-model="rechargeForm.bonusAmount"
            :min="0"
            :precision="2"
            :step="10"
            controls-position="right"
            style="width: 200px"
            disabled
          />
          <span class="form-unit">元（自动计算）</span>
        </el-form-item>

        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select
            v-model="rechargeForm.paymentMethod"
            placeholder="请选择支付方式"
            style="width: 200px"
            :teleported="false"
          >
            <el-option v-for="(v, k) in paymentMethodText" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="rechargeForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmitRecharge">
          确认充值
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="充值详情"
      width="700px"
      destroy-on-close
    >
      <el-descriptions v-if="detailRecord" :column="2" border>
        <el-descriptions-item label="充值单号">{{ detailRecord.recordNo }}</el-descriptions-item>
        <el-descriptions-item label="会员姓名">{{ detailRecord.memberName }}</el-descriptions-item>
        <el-descriptions-item label="会员手机号">{{ detailRecord.memberPhone }}</el-descriptions-item>
        <el-descriptions-item label="会员卡号">{{ getMemberCardNo(detailRecord.memberPhone) }}</el-descriptions-item>
        <el-descriptions-item label="充值方案">{{ detailRecord.planName }}</el-descriptions-item>
        <el-descriptions-item label="充值金额">
          <span class="amount-text">{{ formatAmount(detailRecord.rechargeAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="本金金额">{{ formatAmount(detailRecord.principalAmount) }}</el-descriptions-item>
        <el-descriptions-item label="赠送金额">
          <span class="bonus-text">{{ formatAmount(detailRecord.bonusAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="赠送积分">{{ detailRecord.bonusPoints }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ paymentMethodText[detailRecord.paymentMethod] || detailRecord.paymentMethod }}</el-descriptions-item>
        <el-descriptions-item label="充值状态">
          <StatusTag
            :status="paymentStatusTag[detailRecord.paymentStatus]"
            :label="paymentStatusText[detailRecord.paymentStatus]"
            size="small"
          />
        </el-descriptions-item>
        <el-descriptions-item label="交易流水号">{{ detailRecord.transactionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="充值时间">{{ detailRecord.paymentTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="赠送过期时间">{{ detailRecord.bonusExpireTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">管理员</el-descriptions-item>
        <el-descriptions-item v-if="detailRecord.refundStatus !== 'none'" label="退款金额">
          {{ formatAmount(detailRecord.refundAmount) }}
        </el-descriptions-item>
        <el-descriptions-item v-if="detailRecord.refundStatus !== 'none'" label="退款原因">
          {{ detailRecord.refundReason || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <template #footer>
        <el-button @click="detailDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 退款对话框 -->
    <el-dialog
      v-model="refundDialogVisible"
      title="申请退款"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <template v-if="refundRecord">
        <el-descriptions :column="2" border class="refund-info">
          <el-descriptions-item label="充值单号">{{ refundRecord.recordNo }}</el-descriptions-item>
          <el-descriptions-item label="会员姓名">{{ refundRecord.memberName }}</el-descriptions-item>
          <el-descriptions-item label="充值金额">{{ formatAmount(refundRecord.rechargeAmount) }}</el-descriptions-item>
          <el-descriptions-item label="可退本金">{{ formatAmount(refundRecord.principalAmount) }}</el-descriptions-item>
        </el-descriptions>

        <el-form ref="refundFormRef" :model="refundForm" :rules="refundFormRules" label-width="100px" class="refund-form">
          <el-form-item label="退款金额" prop="refundAmount">
            <el-input-number
              v-model="refundForm.refundAmount"
              :min="0.01"
              :max="parseFloat(refundRecord.principalAmount)"
              :precision="2"
              :step="10"
              controls-position="right"
              style="width: 200px"
            />
            <span class="form-unit">元</span>
          </el-form-item>

          <el-form-item label="退款原因" prop="refundReason">
            <el-input
              v-model="refundForm.refundReason"
              type="textarea"
              :rows="3"
              placeholder="请输入退款原因"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>

        <div class="refund-warning">
          退款仅退本金，赠送金额将相应扣减
        </div>
      </template>

      <template #footer>
        <el-button @click="refundDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="submitLoading" @click="handleSubmitRefund">
          确认退款
        </el-button>
      </template>
    </el-dialog>

    <!-- 充值活动设置对话框（简单占位） -->
    <el-dialog
      v-model="planDialogVisible"
      title="充值活动设置"
      width="800px"
      destroy-on-close
    >
      <el-empty description="充值活动配置功能开发中..." />
      <template #footer>
        <el-button @click="planDialogVisible = false">关闭</el-button>
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

// 分页包装器
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--fts-space-4);
}

// 金额文本
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-primary);
  font-variant-numeric: tabular-nums;
}

// 赠送金额文本
.bonus-text {
  color: var(--fts-success);
  font-variant-numeric: tabular-nums;
}

// 卡号文本
.card-no-text {
  color: var(--fts-text-primary);
  font-family: var(--fts-font-mono);
}

// 操作人文本
.operator-text {
  color: var(--fts-text-secondary);
}

// 操作列
.action-text {
  display: flex;
  gap: var(--fts-space-2);
}

// 表单单位
.form-unit {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}

// 退款信息
.refund-info {
  margin-bottom: var(--fts-space-4);
}

.refund-form {
  margin-bottom: var(--fts-space-4);
}

// 退款警告
.refund-warning {
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-warning-bg, rgba(245, 158, 11, 0.1));
  border-radius: var(--fts-radius-md);
  color: var(--fts-warning);
  font-size: var(--fts-font-size-sm);
  text-align: center;
}
</style>
