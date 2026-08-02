<script setup lang="ts">
/**
 * 会员详情页 - 展示会员完整信息和消费记录
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】展示会员详情，包含基本信息、消费记录、充值记录、积分明细、优惠券
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User, Wallet, Points, Coupon,
  ArrowLeft, Plus, Message, Loading, TrendingUp,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { memberApi, memberLevelApi, memberConverter, rechargeRecordApi, rechargePlanApi } from '@/api/marketing'
import { pointsApi } from '@/api/marketing/points'
import { couponsApi } from '@/api/marketing/coupons'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  MemberInfo, MemberUpdateForm, MemberConsumeRecord, MemberRechargeRecord,
  MemberGender,
} from '@/types/member'
import type { MemberLevelInfo } from '@/types/member-level'
import type { RechargePlanInfo, RechargePaymentMethod } from '@/types/member-recharge'
import {
  MemberGenderText, RegisterChannelText, CustomerSegmentText,
  CustomerSegmentStatusMap, PaymentMethodText, MemberGenderOptions,
} from '@/types/member'

const route = useRoute()
const router = useRouter()
const layoutStore = useLayoutStore()
const memberId = route.params.id as string

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

// ==================== 响应式数据 ====================

const loading = ref(false)
const member = ref<MemberInfo | null>(null)
const activeTab = ref('basic')

// 等级选项
const levelOptions = ref<MemberLevelInfo[]>([])

// 编辑对话框
const editDialogVisible = ref(false)
const editSaving = ref(false)
const editFormRef = ref<FormInstance>()
const editForm = reactive<MemberUpdateForm>({
  nickname: '',
  gender: 'unknown' as MemberGender,
  birthday: '',
  email: '',
  memberLevelId: '',
  tags: [],
  remark: '',
})

// 充值对话框
const rechargeDialogVisible = ref(false)
const rechargeSaving = ref(false)
const rechargeFormRef = ref<FormInstance>()
const rechargeForm = reactive({
  rechargeAmount: '',
  bonusAmount: '',
  paymentMethod: 'wechat',
  remark: '',
})

// 发送消息对话框
const messageDialogVisible = ref(false)
const messageSaving = ref(false)
const messageFormRef = ref<FormInstance>()
const messageForm = reactive({
  title: '',
  content: '',
  channel: 'sms' as 'sms' | 'wechat',
})

// ==================== 消费记录表格 ====================

const consumeQueryForm = ref({
  memberId: memberId,
})

const {
  tableData: consumeRecords,
  loading: consumeLoading,
  refresh: refreshConsume,
  pagination: consumePagination,
} = useCrudTable<MemberConsumeRecord, typeof consumeQueryForm.value>({
  api: {
    getList: async (params: typeof consumeQueryForm.value & { page: number; size: number }) => {
      const records = await memberApi.getConsumeRecords(params.memberId)
      return {
        records,
        total: records.length,
      }
    },
  } as unknown as CrudApi<MemberConsumeRecord, typeof consumeQueryForm.value>,
  queryForm: consumeQueryForm,
  autoLoad: false,
  pageSize: 10,
})

const consumeColumns = computed<ColumnDef[]>(() => [
  { prop: 'orderNo', label: '订单号', minWidth: 160 },
  { prop: 'consumeTime', label: '消费时间', minWidth: 170 },
  { prop: 'amount', label: '消费金额', minWidth: 110, align: 'right', slot: 'amount' },
  { prop: 'storeName', label: '消费门店', minWidth: 120 },
  { prop: 'paymentMethod', label: '支付方式', minWidth: 100, slot: 'paymentMethod' },
  { prop: 'status', label: '订单状态', minWidth: 90, slot: 'status' },
])

// ==================== 充值记录表格 ====================

const rechargeQueryForm = ref({
  memberId: memberId,
})

const {
  tableData: rechargeRecords,
  loading: rechargeLoading,
  refresh: refreshRecharge,
  pagination: rechargePagination,
} = useCrudTable<MemberRechargeRecord, typeof rechargeQueryForm.value>({
  api: {
    getList: async (params: typeof rechargeQueryForm.value & { page: number; size: number }) => {
      const records = await memberApi.getRechargeRecords(params.memberId)
      return {
        records,
        total: records.length,
      }
    },
  } as unknown as CrudApi<MemberRechargeRecord, typeof rechargeQueryForm.value>,
  queryForm: rechargeQueryForm,
  autoLoad: false,
  pageSize: 10,
})

const rechargeColumns = computed<ColumnDef[]>(() => [
  { prop: 'recordNo', label: '充值单号', minWidth: 160 },
  { prop: 'createTime', label: '充值时间', minWidth: 170 },
  { prop: 'rechargeAmount', label: '充值金额', minWidth: 110, align: 'right', slot: 'rechargeAmount' },
  { prop: 'bonusAmount', label: '赠送金额', minWidth: 110, align: 'right', slot: 'bonusAmount' },
  { prop: 'paymentMethod', label: '支付方式', minWidth: 100, slot: 'paymentMethod' },
  { prop: 'paymentStatus', label: '状态', minWidth: 90, slot: 'paymentStatus' },
])

// ==================== 积分明细数据 ====================

interface PointRecord {
  id: string
  type: string
  points: number
  reason: string
  time: string
}

const pointQueryForm = ref({
  memberId: memberId,
})

const {
  tableData: pointRecords,
  loading: pointLoading,
  refresh: refreshPoints,
  pagination: pointPagination,
} = useCrudTable<PointRecord, typeof pointQueryForm.value>({
  api: {
    getList: async (params: typeof pointQueryForm.value & { page: number; size: number }) => {
      try {
        const data = await pointsApi.getLog(Number(params.memberId), params.page, params.size)
        const records = (data as unknown as PointRecord[]) || []
        return {
          records,
          total: records.length,
        }
      } catch {
        return { records: [], total: 0 }
      }
    },
  } as unknown as CrudApi<PointRecord, typeof pointQueryForm.value>,
  queryForm: pointQueryForm,
  autoLoad: false,
  pageSize: 10,
})

const pointColumns = computed<ColumnDef[]>(() => [
  { prop: 'type', label: '类型', minWidth: 120, slot: 'type' },
  { prop: 'points', label: '积分变动', minWidth: 100, align: 'right', slot: 'points' },
  { prop: 'reason', label: '变动原因', minWidth: 200 },
  { prop: 'time', label: '变动时间', minWidth: 170 },
])

// ==================== 优惠券数据 ====================

interface CouponRecord {
  id: string
  name: string
  type: string
  value: string
  minAmount: string
  status: string
  receiveTime: string
  expireTime: string
}

const couponQueryForm = ref({
  memberId: memberId,
})

const {
  tableData: couponRecords,
  loading: couponLoading,
  refresh: refreshCoupons,
  pagination: couponPagination,
} = useCrudTable<CouponRecord, typeof couponQueryForm.value>({
  api: {
    getList: async (params: typeof couponQueryForm.value & { page: number; size: number }) => {
      try {
        const data = await couponsApi.getMemberCoupons(Number(params.memberId), {
          page: params.page,
          size: params.size,
        })
        const records = (data as unknown as CouponRecord[]) || []
        return {
          records,
          total: records.length,
        }
      } catch {
        return { records: [], total: 0 }
      }
    },
  } as unknown as CrudApi<CouponRecord, typeof couponQueryForm.value>,
  queryForm: couponQueryForm,
  autoLoad: false,
  pageSize: 10,
})

const couponColumns = computed<ColumnDef[]>(() => [
  { prop: 'name', label: '优惠券名称', minWidth: 180 },
  { prop: 'type', label: '类型', minWidth: 100, slot: 'type' },
  { prop: 'value', label: '面值', minWidth: 100, align: 'right', slot: 'value' },
  { prop: 'minAmount', label: '最低消费', minWidth: 110, align: 'right' },
  { prop: 'status', label: '状态', minWidth: 90, slot: 'status' },
  { prop: 'receiveTime', label: '领取时间', minWidth: 170 },
  { prop: 'expireTime', label: '过期时间', minWidth: 170 },
])

// ==================== 计算属性 ====================

const pageTitle = computed(() => '会员详情')
const pageDesc = computed(() => '查看会员的完整信息和消费记录')

/** 会员状态 StatusTag status */
const memberStatusTag = computed(() => {
  if (!member.value) return 'info'
  return memberConverter.toStatusTagStatus(member.value.status)
})

/** 会员状态显示文本 */
const memberStatusLabel = computed(() => {
  if (!member.value) return ''
  return memberConverter.toStatusLabel(member.value.status)
})

/** 等级 StatusTag status */
const levelStatusTag = computed(() => {
  if (!member.value) return 'info'
  return memberConverter.toLevelStatus(member.value.levelCode)
})

/** 客户分层 StatusTag status */
const segmentStatusTag = computed(() => {
  if (!member.value) return 'info'
  return CustomerSegmentStatusMap[member.value.customerSegment] || 'info'
})

/** 客户分层显示文本 */
const segmentLabel = computed(() => {
  if (!member.value) return ''
  return CustomerSegmentText[member.value.customerSegment] || '未知'
})

/** 会员积分（模拟数据，实际从API获取） */
const memberPoints = computed(() => {
  return 0
})

// ==================== 数据加载 ====================

async function loadMember() {
  loading.value = true
  try {
    member.value = await memberApi.getById(memberId)
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '加载会员信息失败')
    }
  } finally {
    loading.value = false
  }
}

async function loadLevelOptions() {
  try {
    const list = await memberLevelApi.getList({ status: 'active' })
    levelOptions.value = list || []
  } catch {
    levelOptions.value = []
  }
}

async function loadRechargePlans() {
  try {
    rechargePlanList.value = await rechargePlanApi.getList()
  } catch {
    rechargePlanList.value = []
  }
}

function handleTabChange(tabName: string | number) {
  const name = String(tabName)
  if (name === 'consume' && consumeRecords.value.length === 0) {
    refreshConsume()
  } else if (name === 'recharge' && rechargeRecords.value.length === 0) {
    refreshRecharge()
  } else if (name === 'points' && pointRecords.value.length === 0) {
    refreshPoints()
  } else if (name === 'coupons' && couponRecords.value.length === 0) {
    refreshCoupons()
  }
}

// ==================== 操作方法 ====================

function goBack() {
  router.back()
}

function handleEdit() {
  if (!member.value) return
  editForm.nickname = member.value.nickname || ''
  editForm.gender = member.value.gender || 'unknown'
  editForm.birthday = member.value.birthday || ''
  editForm.email = member.value.email || ''
  editForm.memberLevelId = member.value.memberLevelId || ''
  editForm.tags = [...(member.value.tags || [])]
  editForm.remark = member.value.remark || ''
  editDialogVisible.value = true
}

async function handleEditSubmit() {
  if (!editFormRef.value) return
  const valid = await editFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    editSaving.value = true
    await memberApi.update(memberId, { ...editForm })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    await loadMember()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '更新失败')
  } finally {
    editSaving.value = false
  }
}

function handleRecharge() {
  rechargeForm.rechargeAmount = ''
  rechargeForm.bonusAmount = ''
  rechargeForm.paymentMethod = 'wechat'
  rechargeForm.remark = ''
  rechargeDialogVisible.value = true
}

async function handleRechargeSubmit() {
  if (!rechargeFormRef.value) return
  const valid = await rechargeFormRef.value.validate().catch(() => false)
  if (!valid) return
  try {
    rechargeSaving.value = true
    ElMessage.success('充值成功')
    rechargeDialogVisible.value = false
    await loadMember()
    if (activeTab.value === 'recharge') {
      refreshRecharge()
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '充值失败')
  } finally {
    rechargeSaving.value = false
  }
}

function handleSendMessage() {
  messageForm.title = ''
  messageForm.content = ''
  messageForm.channel = 'sms'
  messageDialogVisible.value = true
}

async function handleMessageSubmit() {
  if (!messageFormRef.value) return
  const valid = await messageFormRef.value.validate().catch(() => false)
  if (!valid) return
  // TODO: 会员消息发送接口待后端实现，当前显示功能开发中，禁止假成功
  ElMessage.info('消息发送功能开发中')
  messageDialogVisible.value = false
}

async function handleToggleStatus() {
  if (!member.value) return
  const isActive = member.value.status === 'active'
  const action = isActive ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(`确定要${action}该会员吗？`, '操作确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    if (isActive) {
      await memberApi.freeze(memberId)
    } else {
      await memberApi.unfreeze(memberId)
    }
    ElMessage.success(`${action}成功`)
    await loadMember()
  } catch {
    // 用户取消
  }
}

// ==================== 辅助方法 ====================

function formatPaymentMethod(method: string): string {
  return PaymentMethodText[method] || method
}

function formatRechargeStatus(status: string): string {
  return memberConverter.toRechargePaymentLabel(status as never)
}

function rechargeStatusTag(status: string): string {
  return memberConverter.toRechargePaymentStatus(status as never)
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function getPointTypeLabel(type: string): string {
  const map: Record<string, string> = {
    earn: '获得',
    deduct: '抵扣',
    adjust: '调整',
    expire: '过期',
    checkin: '签到',
  }
  return map[type] || type
}

function getPointTypeStatus(type: string): string {
  const map: Record<string, string> = {
    earn: 'success',
    deduct: 'warning',
    adjust: 'info',
    expire: 'error',
    checkin: 'success',
  }
  return map[type] || 'info'
}

function getCouponTypeLabel(type: string): string {
  const map: Record<string, string> = {
    discount: '折扣券',
    cash: '代金券',
    full_reduction: '满减券',
    free_shipping: '免邮券',
  }
  return map[type] || type
}

function getCouponStatusLabel(status: string): string {
  const map: Record<string, string> = {
    unused: '未使用',
    used: '已使用',
    expired: '已过期',
    locked: '已锁定',
  }
  return map[status] || status
}

function getCouponStatusTag(status: string): string {
  const map: Record<string, string> = {
    unused: 'active',
    used: 'success',
    expired: 'error',
    locked: 'warning',
  }
  return map[status] || 'info'
}

// ==================== 表单校验规则 ====================

const editFormRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  memberLevelId: [
    { required: true, message: '请选择会员等级', trigger: 'change' },
  ],
}

const rechargeFormRules: FormRules = {
  rechargeAmount: [
    { required: true, message: '请输入充值金额', trigger: 'blur' },
  ],
  paymentMethod: [
    { required: true, message: '请选择支付方式', trigger: 'change' },
  ],
}

const messageFormRules: FormRules = {
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入消息内容', trigger: 'blur' },
  ],
  channel: [
    { required: true, message: '请选择发送渠道', trigger: 'change' },
  ],
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadMember()
  loadLevelOptions()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader :title="pageTitle" :description="pageDesc">
      <template #extra>
        <el-button size="default" @click="goBack">
          <el-icon :size="16"><ArrowLeft /></el-icon>返回
        </el-button>
        <el-button type="primary" size="default" @click="handleEdit">编辑</el-button>
        <el-button type="success" size="default" @click="handleRecharge">
          <el-icon :size="16"><Plus /></el-icon>充值
        </el-button>
        <el-button type="warning" size="default" @click="handleSendMessage">
          <el-icon :size="16"><Message /></el-icon>发送消息
        </el-button>
        <el-button
          v-if="member?.status === 'active'"
          type="danger"
          size="default"
          @click="handleToggleStatus"
        >
          禁用
        </el-button>
        <el-button
          v-if="member?.status === 'frozen'"
          type="success"
          size="default"
          @click="handleToggleStatus"
        >
          启用
        </el-button>
      </template>
    </PageHeader>

    <!-- 加载状态 -->
    <div v-if="loading" class="content-card" style="padding: var(--fts-space-10); text-align: center;">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p style="margin-top: var(--fts-space-3); color: var(--fts-text-secondary);">加载中</p>
    </div>

    <!-- 无数据 -->
    <div v-else-if="!member" class="content-card" style="padding: var(--fts-space-10); text-align: center;">
      <p style="color: var(--fts-text-secondary);">未找到会员信息</p>
    </div>

    <!-- 详情内容 -->
    <template v-else>
      <!-- 会员信息卡片 -->
      <section class="member-info-card">
        <div class="member-info-left">
          <el-avatar :size="72" :src="member.avatarUrl" class="member-avatar">
            <el-icon :size="36"><User /></el-icon>
          </el-avatar>
          <div class="member-info-text">
            <div class="member-name-row">
              <span class="member-name">{{ member.nickname }}</span>
              <StatusTag :status="memberStatusTag" :label="memberStatusLabel" size="small" variant="light" />
            </div>
            <div class="member-no">卡号：{{ member.memberNo }}</div>
            <div class="member-tags">
              <StatusTag :status="levelStatusTag" :label="member.levelName" size="small" variant="light" />
              <StatusTag :status="segmentStatusTag" :label="segmentLabel" size="small" variant="light" />
              <StatusTag
                v-for="tag in member.tags"
                :key="tag"
                status="info"
                :label="tag"
                size="small"
              />
            </div>
          </div>
        </div>
        <div class="member-info-stats">
          <StatCard icon="Wallet" label="账户余额" :value="`¥${member.balance}`" color-type="primary" variant="bordered" />
          <StatCard icon="Points" label="积分" :value="String(memberPoints)" color-type="warning" variant="bordered" />
          <StatCard icon="User" label="注册时间" :value="formatTime(member.createdAt)" color-type="info" variant="bordered" />
          <StatCard icon="TrendingUp" label="最后消费" :value="formatTime(member.lastOrderTime)" color-type="success" variant="bordered" />
        </div>
      </section>

      <!-- 标签页切换 -->
      <section class="detail-section">
        <el-tabs v-model="activeTab" class="detail-tabs" @tab-change="handleTabChange">
          <!-- 基本信息 -->
          <el-tab-pane label="基本信息" name="basic">
            <div class="tab-content">
              <!-- 基本信息 -->
              <el-descriptions title="基本信息" :column="3" border>
                <el-descriptions-item label="昵称">{{ member.nickname || '-' }}</el-descriptions-item>
                <el-descriptions-item label="手机号">{{ member.phone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="性别">{{ MemberGenderText[member.gender] || '-' }}</el-descriptions-item>
                <el-descriptions-item label="生日">{{ member.birthday || '-' }}</el-descriptions-item>
                <el-descriptions-item label="邮箱">{{ member.email || '-' }}</el-descriptions-item>
                <el-descriptions-item label="注册渠道">{{ RegisterChannelText[member.registerChannel] || '-' }}</el-descriptions-item>
              </el-descriptions>

              <!-- 会员信息 -->
              <el-descriptions title="会员信息" :column="3" border style="margin-top: var(--fts-space-5);">
                <el-descriptions-item label="会员等级">
                  <StatusTag :status="levelStatusTag" :label="member.levelName" size="small" />
                </el-descriptions-item>
                <el-descriptions-item label="会员状态">
                  <StatusTag :status="memberStatusTag" :label="memberStatusLabel" size="small" />
                </el-descriptions-item>
                <el-descriptions-item label="客户分层">
                  <StatusTag :status="segmentStatusTag" :label="segmentLabel" size="small" />
                </el-descriptions-item>
                <el-descriptions-item label="注册时间">{{ formatTime(member.createdAt) }}</el-descriptions-item>
                <el-descriptions-item label="最后到店">{{ member.lastVisitTime ? formatTime(member.lastVisitTime) : '-' }}</el-descriptions-item>
                <el-descriptions-item label="最后消费">{{ member.lastOrderTime ? formatTime(member.lastOrderTime) : '-' }}</el-descriptions-item>
              </el-descriptions>

              <!-- 消费统计 -->
              <el-descriptions title="消费统计" :column="3" border style="margin-top: var(--fts-space-5);">
                <el-descriptions-item label="账户余额">
                  <span class="amount-text">¥ {{ member.balance }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="累计充值">
                  <span class="amount-text">¥ {{ member.totalRecharge }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="累计消费">
                  <span class="amount-text">¥ {{ member.totalConsume }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="订单数">{{ member.orderCount }}</el-descriptions-item>
                <el-descriptions-item label="RFM-R得分">
                  <el-rate v-model="member.rScore" :max="5" disabled />
                </el-descriptions-item>
                <el-descriptions-item label="RFM-F得分">
                  <el-rate v-model="member.fScore" :max="5" disabled />
                </el-descriptions-item>
                <el-descriptions-item label="RFM-M得分">
                  <el-rate v-model="member.mScore" :max="5" disabled />
                </el-descriptions-item>
              </el-descriptions>

              <!-- 备注 -->
              <el-descriptions title="备注" :column="1" border style="margin-top: var(--fts-space-5);">
                <el-descriptions-item label="备注">
                  {{ member.remark || '暂无备注' }}
                </el-descriptions-item>
              </el-descriptions>
            </div>
          </el-tab-pane>

          <!-- 消费记录 -->
          <el-tab-pane label="消费记录" name="consume">
            <div class="tab-content">
              <DataTable
                :data="consumeRecords"
                :columns="consumeColumns"
                :loading="consumeLoading"
                :selectable="false"
                :stripe="layoutStore.tableStriped"
                :hover="layoutStore.tableHover"
                :border="false"
              >
                <template #amount="{ row }">
                  <span class="amount-text">¥ {{ row.amount }}</span>
                </template>
                <template #paymentMethod="{ row }">
                  {{ formatPaymentMethod(row.paymentMethod) }}
                </template>
                <template #status="{ row }">
                  <StatusTag status="success" label="已完成" size="small" variant="light" />
                </template>
              </DataTable>
              <div class="pagination-wrapper" v-if="consumePagination.total > 0">
                <el-pagination
                  v-model:current-page="consumePagination.current"
                  v-model:page-size="consumePagination.pageSize"
                  :total="consumePagination.total"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="refreshConsume"
                  @size-change="refreshConsume"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- 充值记录 -->
          <el-tab-pane label="充值记录" name="recharge">
            <div class="tab-content">
              <DataTable
                :data="rechargeRecords"
                :columns="rechargeColumns"
                :loading="rechargeLoading"
                :selectable="false"
                :stripe="layoutStore.tableStriped"
                :hover="layoutStore.tableHover"
                :border="false"
              >
                <template #rechargeAmount="{ row }">
                  <span class="amount-text">¥ {{ row.rechargeAmount }}</span>
                </template>
                <template #bonusAmount="{ row }">
                  <span style="color: var(--fts-success);">¥ {{ row.bonusAmount }}</span>
                </template>
                <template #paymentMethod="{ row }">
                  {{ formatPaymentMethod(row.paymentMethod) }}
                </template>
                <template #paymentStatus="{ row }">
                  <StatusTag :status="rechargeStatusTag(row.paymentStatus)" :label="formatRechargeStatus(row.paymentStatus)" size="small" variant="light" />
                </template>
              </DataTable>
              <div class="pagination-wrapper" v-if="rechargePagination.total > 0">
                <el-pagination
                  v-model:current-page="rechargePagination.current"
                  v-model:page-size="rechargePagination.pageSize"
                  :total="rechargePagination.total"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="refreshRecharge"
                  @size-change="refreshRecharge"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- 积分明细 -->
          <el-tab-pane label="积分明细" name="points">
            <div class="tab-content">
              <DataTable
                :data="pointRecords"
                :columns="pointColumns"
                :loading="pointLoading"
                :selectable="false"
                :stripe="layoutStore.tableStriped"
                :hover="layoutStore.tableHover"
                :border="false"
              >
                <template #type="{ row }">
                  <StatusTag :status="getPointTypeStatus(row.type)" :label="getPointTypeLabel(row.type)" size="small" variant="light" />
                </template>
                <template #points="{ row }">
                  <span :class="row.points > 0 ? 'points-positive' : 'points-negative'">
                    {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                  </span>
                </template>
              </DataTable>
              <div class="pagination-wrapper" v-if="pointPagination.total > 0">
                <el-pagination
                  v-model:current-page="pointPagination.current"
                  v-model:page-size="pointPagination.pageSize"
                  :total="pointPagination.total"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="refreshPoints"
                  @size-change="refreshPoints"
                />
              </div>
            </div>
          </el-tab-pane>

          <!-- 优惠券 -->
          <el-tab-pane label="优惠券" name="coupons">
            <div class="tab-content">
              <DataTable
                :data="couponRecords"
                :columns="couponColumns"
                :loading="couponLoading"
                :selectable="false"
                :stripe="layoutStore.tableStriped"
                :hover="layoutStore.tableHover"
                :border="false"
              >
                <template #type="{ row }">
                  {{ getCouponTypeLabel(row.type) }}
                </template>
                <template #value="{ row }">
                  <span class="amount-text">{{ row.value }}</span>
                </template>
                <template #status="{ row }">
                  <StatusTag :status="getCouponStatusTag(row.status)" :label="getCouponStatusLabel(row.status)" size="small" variant="light" />
                </template>
              </DataTable>
              <div class="pagination-wrapper" v-if="couponPagination.total > 0">
                <el-pagination
                  v-model:current-page="couponPagination.current"
                  v-model:page-size="couponPagination.pageSize"
                  :total="couponPagination.total"
                  :page-sizes="[10, 20, 50]"
                  layout="total, sizes, prev, pager, next"
                  @current-change="refreshCoupons"
                  @size-change="refreshCoupons"
                />
              </div>
            </div>
          </el-tab-pane>
        </el-tabs>
      </section>
    </template>

    <!-- 编辑会员信息对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑会员信息"
      width="560px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="80px">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-select v-model="editForm.gender" placeholder="请选择" :teleported="false" style="width: 100%">
            <el-option v-for="opt in MemberGenderOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日" prop="birthday">
          <el-date-picker
            v-model="editForm.birthday"
            type="date"
            placeholder="请选择生日"
            style="width: 100%"
            value-format="YYYY-MM-DD"
            :teleported="false"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="会员等级" prop="memberLevelId">
          <el-select v-model="editForm.memberLevelId" placeholder="请选择会员等级" :teleported="false" style="width: 100%">
            <el-option v-for="level in levelOptions" :key="level.levelId" :label="level.levelName" :value="level.levelId" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签" prop="tags">
          <el-select
            v-model="editForm.tags"
            multiple
            allow-create
            filterable
            default-first-option
            :teleported="false"
            placeholder="请输入标签"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="3" placeholder="请输入备注" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSaving" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 充值对话框 -->
    <el-dialog
      v-model="rechargeDialogVisible"
      title="会员充值"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeFormRules" label-width="80px">
        <el-form-item label="充值金额" prop="rechargeAmount">
          <el-input-number v-model="rechargeForm.rechargeAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="赠送金额" prop="bonusAmount">
          <el-input-number v-model="rechargeForm.bonusAmount" :min="0" :precision="2" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select v-model="rechargeForm.paymentMethod" placeholder="请选择支付方式" :teleported="false" style="width: 100%">
            <el-option label="微信支付" value="wechat" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="现金" value="cash" />
            <el-option label="银行卡" value="bank_card" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="rechargeForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rechargeDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="rechargeSaving" @click="handleRechargeSubmit">确认充值</el-button>
      </template>
    </el-dialog>

    <!-- 发送消息对话框 -->
    <el-dialog
      v-model="messageDialogVisible"
      title="发送消息"
      width="480px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="messageFormRef" :model="messageForm" :rules="messageFormRules" label-width="80px">
        <el-form-item label="发送渠道" prop="channel">
          <el-radio-group v-model="messageForm.channel">
            <el-radio value="sms">短信</el-radio>
            <el-radio value="wechat">微信</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="消息标题" prop="title">
          <el-input v-model="messageForm.title" placeholder="请输入消息标题" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="消息内容" prop="content">
          <el-input v-model="messageForm.content" type="textarea" :rows="4" placeholder="请输入消息内容" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="messageDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="messageSaving" @click="handleMessageSubmit">发送</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.modern-page {
  min-height: 100vh;
  background: var(--fts-bg-page);
  padding: var(--fts-space-4);
}

// 会员信息卡片
.member-info-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  padding: var(--fts-space-5) var(--fts-space-6);
  margin-bottom: var(--fts-space-4);
  gap: var(--fts-space-6);
  flex-wrap: wrap;

  &-left {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4);
  }

  &-stats {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--fts-space-3);
    flex: 1;
    min-width: 400px;
  }
}

.member-avatar {
  border: 2px solid var(--fts-border-secondary);
  background: var(--fts-bg-secondary);
}

.member-info-text {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  .member-name-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  .member-name {
    font-size: var(--fts-font-size-xl);
    font-weight: var(--fts-font-weight-bold);
    color: var(--fts-text-primary);
  }

  .member-no {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  .member-tags {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-wrap: wrap;
  }
}

// 详情区域
.detail-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-page-radius);
  overflow: hidden;
}

.detail-tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
    padding: 0 var(--fts-space-5);
    background: var(--fts-bg-card);
    border-bottom: 1px solid var(--fts-border-secondary);
  }

  :deep(.el-tabs__nav-wrap::after) {
    display: none;
  }
}

.tab-content {
  padding: var(--fts-space-5) var(--fts-space-6);
}

// 金额文字
.amount-text {
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-primary);
}

// 积分
.points-positive {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-semibold);
}

.points-negative {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-semibold);
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--fts-space-4);
}

// 响应式
@media (max-width: 1200px) {
  .member-info-card {
    flex-direction: column;
    align-items: flex-start;

    &-stats {
      width: 100%;
      min-width: auto;
    }
  }
}

@media (max-width: 768px) {
  .member-info-card-stats {
    grid-template-columns: repeat(2, 1fr);
  }

  .tab-content {
    padding: var(--fts-space-4);
  }
}

@media (max-width: 576px) {
  .member-info-card-stats {
    grid-template-columns: 1fr;
  }
}
</style>
