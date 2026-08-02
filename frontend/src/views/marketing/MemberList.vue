<script setup lang="ts">
/**
 * 会员列表页面
 *
 * 【层级】L6 - 应用层(View)
 * 【职责】管理会员信息和会员档案
 * 【依赖】L3(PageHeader/DataTable/StatCard/StatusTag)
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Upload, Download, Search, Refresh,
  User, Message, Wallet, TrendingUp,
} from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadFile, UploadRawFile } from 'element-plus'

import PageHeader from '@/components/core/PageHeader.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatCard from '@/components/core/StatCard.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useLayoutStore } from '@/stores/layout'
import { memberApi, memberLevelApi, memberConverter, rechargeRecordApi, rechargePlanApi } from '@/api/marketing'
import { useCrudTable, type CrudApi } from '@/composables/useCrudTable'
import type {
  MemberInfo, MemberCreateForm, MemberUpdateForm, MemberQueryForm,
  MemberConsumeRecord, MemberRechargeRecord, MemberStatsOverview,
} from '@/types/member'
import type { MemberLevelInfo } from '@/types/member-level'
import { MemberStatusOptions, MemberGenderOptions } from '@/types/member'

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

// ==================== 响应式数据 ====================

const submitLoading = ref(false)
const selectedRows = ref<MemberInfo[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const detailVisible = ref(false)
const rechargeVisible = ref(false)
const currentMember = ref<MemberInfo | null>(null)
const detailLoading = ref(false)
const statsLoading = ref(false)

const formRef = ref<FormInstance>()
const rechargeFormRef = ref<FormInstance>()

// 统计数据
const statistics = ref<MemberStatsOverview>({
  totalMembers: 0,
  newToday: 0,
  activeMembers: 0,
  totalBalance: '0.00',
  newThisMonth: 0,
  dormantMembers: 0,
  atRiskCount: 0,
  rechargeThisMonth: '0.00',
  consumeThisMonth: '0.00',
  consumeRatio: '0',
  avgOrderAmount: '0.00',
  repurchaseRate: '0',
  levelDistribution: [],
  channelDistribution: [],
  weeklyGrowthTrend: [],
  monthlyConsumeTrend: [],
  segmentDistribution: [],
})

// 消费记录、充值记录、积分变动记录
const consumeRecords = ref<MemberConsumeRecord[]>([])
const rechargeRecords = ref<MemberRechargeRecord[]>([])
const pointRecords = ref<Array<{ id: string; type: string; points: number; reason: string; time: string }>>([])

// 等级选项
const levelOptions = ref<MemberLevelInfo[]>([])

// 查询表单
const queryForm = ref({
  keyword: '',
  memberLevelId: '' as string | '',
  status: '' as string | '',
  startCreatedAt: '' as string | '',
  endCreatedAt: '' as string | '',
})

// 使用useCrudTable管理表格数据和分页
const {
  tableData,
  loading,
  refresh,
  pagination,
} = useCrudTable<MemberInfo, typeof queryForm.value>({
  api: {
    getList: async (params: typeof queryForm.value & { page: number; size: number }) => {
      const convertedParams: MemberQueryForm & { page: number; size: number } = {
        page: params.page,
        size: params.size,
      }
      if (params.keyword) {
        convertedParams.nickname = params.keyword
        convertedParams.phone = params.keyword
        convertedParams.memberNo = params.keyword
      }
      if (params.memberLevelId) {
        convertedParams.memberLevelId = params.memberLevelId
      }
      if (params.status) {
        convertedParams.status = params.status
      }
      if (params.startCreatedAt) {
        convertedParams.startCreatedAt = params.startCreatedAt
      }
      if (params.endCreatedAt) {
        convertedParams.endCreatedAt = params.endCreatedAt
      }
      return memberApi.getList(convertedParams)
    },
  } as unknown as CrudApi<MemberInfo, typeof queryForm.value>,
  queryForm,
  autoLoad: true,
})

// 表单数据
const formData = reactive<Partial<MemberCreateForm> & {
  memberCardType: string
  initialBalance: string
  initialPoints: number
}>({
  phone: '',
  nickname: '',
  gender: 'unknown',
  birthday: '',
  email: '',
  memberLevelId: '',
  memberCardType: 'normal',
  initialBalance: '0',
  initialPoints: 0,
  remark: '',
})

// 充值表单
const rechargeForm = reactive({
  memberId: '',
  memberName: '',
  rechargeAmount: '0',
  bonusAmount: '0',
  paymentMethod: 'wechat',
  remark: '',
})

// ==================== 表格列定义 ====================

const columns = computed<ColumnDef[]>(() => [
  { prop: 'memberNo', label: '会员卡号', minWidth: 130, slot: 'memberNo' },
  { prop: 'nickname', label: '会员姓名', minWidth: 110, slot: 'nickname' },
  { prop: 'phone', label: '手机号', minWidth: 120, slot: 'phone' },
  { prop: 'levelName', label: '会员等级', minWidth: 100, slot: 'levelName' },
  { prop: 'balance', label: '账户余额', minWidth: 110, slot: 'balance', align: 'right' },
  { prop: 'points', label: '积分', minWidth: 80, slot: 'points', align: 'right' },
  { prop: 'orderCount', label: '消费次数', minWidth: 90, slot: 'orderCount', align: 'center' },
  { prop: 'createdAt', label: '注册时间', minWidth: 160, slot: 'createdAt' },
  { prop: 'status', label: '会员状态', minWidth: 100, slot: 'status' },
  { prop: '_operation', label: '操作', width: 320, fixed: 'right', slot: 'operation' },
])

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入会员姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在2到20个字符之间', trigger: 'blur' },
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

// ==================== 方法 ====================

function handleSearch() {
  refresh()
}

function handleReset() {
  queryForm.value.keyword = ''
  queryForm.value.memberLevelId = ''
  queryForm.value.status = ''
  queryForm.value.startCreatedAt = ''
  queryForm.value.endCreatedAt = ''
  refresh()
}

function handleSelectionChange(rows: MemberInfo[]) {
  selectedRows.value = rows
}

function handleCreate() {
  isEdit.value = false
  Object.assign(formData, {
    phone: '',
    nickname: '',
    gender: 'unknown',
    birthday: '',
    email: '',
    memberLevelId: '',
    memberCardType: 'normal',
    initialBalance: '0',
    initialPoints: 0,
    remark: '',
  })
  dialogVisible.value = true
}

async function handleEdit(row: MemberInfo) {
  isEdit.value = true
  try {
    const detail = await memberApi.getById(row.id)
    if (detail) {
      Object.assign(formData, {
        phone: detail.phone,
        nickname: detail.nickname,
        gender: detail.gender,
        birthday: detail.birthday,
        email: detail.email,
        memberLevelId: detail.memberLevelId,
        memberCardType: 'normal',
        initialBalance: detail.balance,
        initialPoints: 0,
        remark: detail.remark,
      })
    }
    dialogVisible.value = true
  } catch {
    ElMessage.error('加载会员详情失败')
  }
}

async function handleSubmit() {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: MemberCreateForm = {
      phone: formData.phone || '',
      nickname: formData.nickname || '',
      gender: formData.gender as 'unknown' | 'male' | 'female',
      birthday: formData.birthday || '',
      email: formData.email || '',
      memberLevelId: formData.memberLevelId || '',
      remark: formData.remark || '',
    }

    if (isEdit.value && currentMember.value) {
      const updateData: MemberUpdateForm = {
        nickname: submitData.nickname,
        gender: submitData.gender,
        birthday: submitData.birthday,
        email: submitData.email,
        memberLevelId: submitData.memberLevelId,
        tags: [],
        remark: submitData.remark,
      }
      await memberApi.update(currentMember.value.id, updateData)
      ElMessage.success('更新成功')
    } else {
      await memberApi.create(submitData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    refresh()
    loadStatistics()
  } catch (error) {
    ElMessage.error(isEdit.value ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleDetail(row: MemberInfo) {
  currentMember.value = row
  detailVisible.value = true
  detailLoading.value = true
  try {
    const [consumes, recharges] = await Promise.all([
      memberApi.getConsumeRecords(row.id),
      memberApi.getRechargeRecords(row.id),
    ])
    consumeRecords.value = consumes
    rechargeRecords.value = recharges
    // TODO: 后端暂无积分变动记录 API，当前置空，待后端接口就绪后接入
    pointRecords.value = []
  } catch {
    ElMessage.error('加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

function handleRecharge(row: MemberInfo) {
  currentMember.value = row
  rechargeForm.memberId = row.id
  rechargeForm.memberName = row.nickname
  rechargeForm.rechargeAmount = '0'
  rechargeForm.bonusAmount = '0'
  rechargeForm.paymentMethod = 'wechat'
  rechargeForm.remark = ''
  rechargeVisible.value = true
  loadRechargePlans()
}

async function handleRechargeSubmit() {
  if (!rechargeFormRef.value) return

  const valid = await rechargeFormRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    // 充值接口为方案制（rechargeRecordApi.recharge 需要 planId），
    // 按充值金额匹配充值方案后调用真实 API，涉及真实资金流转，禁止假成功。
    const plan = rechargePlanList.value.find(p => p.rechargeAmount === String(rechargeForm.rechargeAmount))
    if (!plan) {
      ElMessage.warning('未找到匹配的充值方案，请前往「充值管理」页面配置后再充值')
      return
    }
    await rechargeRecordApi.recharge(
      rechargeForm.memberId,
      plan.planId,
      rechargeForm.paymentMethod as RechargePaymentMethod,
    )
    ElMessage.success('充值成功')
    rechargeVisible.value = false
    refresh()
    loadStatistics()
  } catch {
    ElMessage.error('充值失败')
  } finally {
    submitLoading.value = false
  }
}

async function handleLevelAdjust(row: MemberInfo) {
  currentMember.value = row
  ElMessage.info('调整等级功能开发中')
}

async function handleStatusToggle(row: MemberInfo) {
  const isFrozen = row.status === 'frozen'
  const isActive = row.status === 'active'
  let action = ''
  let targetStatus = ''

  if (isActive) {
    action = '冻结'
    targetStatus = 'frozen'
  } else if (isFrozen) {
    action = '启用'
    targetStatus = 'active'
  } else {
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要${action}会员「${row.nickname}」吗？`,
      '操作确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    if (targetStatus === 'frozen') {
      await memberApi.freeze(row.id)
    } else {
      await memberApi.unfreeze(row.id)
    }
    ElMessage.success(`${action}成功`)
    refresh()
  } catch (error: unknown) {
    if (error !== 'cancel') {
      ElMessage.error(error instanceof Error ? error.message : '操作失败')
    }
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
  ElMessage.info('批量导入功能开发中')
}

async function handleExport(): Promise<void> {
  try {
    ElMessage.info('导出功能开发中')
  } catch {
    ElMessage.error('导出失败，请稍后重试')
  }
}

function handleSendMessage() {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请先选择要发送消息的会员')
    return
  }
  ElMessage.info(`已选择 ${selectedRows.value.length} 位会员，发送消息功能开发中`)
}

async function loadStatistics() {
  statsLoading.value = true
  try {
    const stats = await memberApi.getStatsOverview()
    if (stats) {
      statistics.value = stats
    }
  } catch {
    // API失败时使用默认值
  } finally {
    statsLoading.value = false
  }
}

async function loadLevelOptions() {
  try {
    levelOptions.value = await memberLevelApi.getList({ status: 'active' })
  } catch {
    levelOptions.value = []
  }
}

// ==================== 辅助方法 ====================

function getStatusColor(status: string): string {
  return memberConverter.toStatusTagStatus(status as 'active' | 'frozen' | 'blacklisted' | 'cancelled')
}

function getStatusLabel(status: string): string {
  return memberConverter.toStatusLabel(status as 'active' | 'frozen' | 'blacklisted' | 'cancelled')
}

function getLevelColor(levelCode: string): string {
  return memberConverter.toLevelStatus(levelCode)
}

function getLevelLabel(levelCode: string, levelName: string): string {
  return levelName || memberConverter.toLevelLabel(levelCode)
}

function formatTime(iso: string): string {
  if (!iso) return '-'
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatMoney(value: string): string {
  if (!value) return '0.00'
  const num = parseFloat(value)
  return isNaN(num) ? '0.00' : num.toFixed(2)
}

// TODO: 后端 MemberInfo 暂无积分字段（无积分体系版本），积分显示占位，待后端积分模块就绪后对接
function getPoints(_row: MemberInfo): string {
  return '--'
}

onMounted(() => {
  loadLevelOptions()
  loadStatistics()
})
</script>

<template>
  <div class="modern-page">
    <!-- 页面头部 -->
    <PageHeader title="会员列表" description="管理会员信息和会员档案">
      <el-button type="primary" size="default" @click="handleCreate">
        <el-icon :size="16"><Plus /></el-icon>新增会员
      </el-button>
    </PageHeader>

    <!-- 统计卡片区 -->
    <section class="stats-section">
      <StatCard
        icon="User"
        label="会员总数"
        :value="String(statistics.totalMembers)"
        color-type="primary"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="UserFilled"
        label="今日新增"
        :value="String(statistics.newToday)"
        color-type="success"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="TrendCharts"
        label="活跃会员"
        :value="String(statistics.activeMembers)"
        color-type="warning"
        variant="bordered"
        :loading="statsLoading"
      />
      <StatCard
        icon="Wallet"
        label="会员总余额"
        :value="'¥' + statistics.totalBalance"
        color-type="info"
        variant="bordered"
        :loading="statsLoading"
      />
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
          <el-select
            v-model="queryForm.memberLevelId"
            placeholder="会员等级"
            clearable
            style="width: 130px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="level in levelOptions"
              :key="level.levelId"
              :label="level.levelName"
              :value="level.levelId"
            />
          </el-select>
          <el-select
            v-model="queryForm.status"
            placeholder="会员状态"
            clearable
            style="width: 120px"
            size="default"
            @change="handleSearch"
          >
            <el-option
              v-for="opt in MemberStatusOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
          <el-date-picker
            v-model="queryForm.startCreatedAt"
            type="date"
            placeholder="注册开始日期"
            value-format="YYYY-MM-DD"
            style="width: 150px"
            size="default"
            :teleported="false"
            @change="handleSearch"
          />
          <el-date-picker
            v-model="queryForm.endCreatedAt"
            type="date"
            placeholder="注册结束日期"
            value-format="YYYY-MM-DD"
            style="width: 150px"
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
          <el-button type="success" size="default" @click="handleCreate">
            <el-icon :size="14"><Plus /></el-icon>新增会员
          </el-button>
          <el-upload
            :show-file-list="false"
            :before-upload="beforeUpload"
            :http-request="handleImport"
            accept=".xlsx,.xls"
          >
            <el-button size="default" class="action-btn--import">
              <el-icon :size="14"><Upload /></el-icon>批量导入
            </el-button>
          </el-upload>
          <el-button size="default" class="action-btn--export" @click="handleExport">
            <el-icon :size="14"><Download /></el-icon>导出
          </el-button>
          <el-button
            type="warning"
            size="default"
            :disabled="selectedRows.length === 0"
            @click="handleSendMessage"
          >
            <el-icon :size="14"><Message /></el-icon>发送消息
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
        <template #memberNo="{ row }">
          <span class="member-no">{{ row.memberNo }}</span>
        </template>

        <!-- 会员姓名列 -->
        <template #nickname="{ row }">
          <div class="member-cell">
            <el-avatar :size="36" :icon="User" shape="circle" />
            <div class="member-info">
              <div class="member-name">{{ row.nickname }}</div>
            </div>
          </div>
        </template>

        <!-- 手机号列 -->
        <template #phone="{ row }">
          <span class="phone-text">{{ row.phone }}</span>
        </template>

        <!-- 会员等级列 -->
        <template #levelName="{ row }">
          <StatusTag
            :status="getLevelColor(row.levelCode)"
            :label="getLevelLabel(row.levelCode, row.levelName)"
            size="small"
            variant="light"
          />
        </template>

        <!-- 账户余额列 -->
        <template #balance="{ row }">
          <span class="balance-text">¥{{ formatMoney(row.balance) }}</span>
        </template>

        <!-- 积分列 -->
        <template #points="{ row }">
          <span class="points-text">{{ getPoints(row) }}</span>
        </template>

        <!-- 消费次数列 -->
        <template #orderCount="{ row }">
          <span class="count-text">{{ row.orderCount }}</span>
        </template>

        <!-- 注册时间列 -->
        <template #createdAt="{ row }">
          <span class="time-text">{{ formatTime(row.createdAt) }}</span>
        </template>

        <!-- 会员状态列 -->
        <template #status="{ row }">
          <StatusTag
            :status="getStatusColor(row.status)"
            :label="getStatusLabel(row.status)"
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
            <el-button link type="primary" size="default" @click.stop="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="success" size="default" @click.stop="handleRecharge(row)">
              充值
            </el-button>
            <el-button link type="warning" size="default" @click.stop="handleLevelAdjust(row)">
              调整等级
            </el-button>
            <el-button
              v-if="row.status === 'active' || row.status === 'frozen'"
              link
              :type="row.status === 'frozen' ? 'primary' : 'danger'"
              size="default"
              @click.stop="handleStatusToggle(row)"
            >
              {{ row.status === 'frozen' ? '启用' : '禁用' }}
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
          @current-change="refresh"
          @size-change="refresh"
        />
      </div>
    </section>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑会员' : '新增会员'"
      width="700px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <!-- 基本信息 -->
        <div class="form-section">
          <div class="section-title">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="姓名" prop="nickname">
                <el-input v-model="formData.nickname" placeholder="请输入会员姓名" maxlength="20" show-word-limit />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="formData.phone" placeholder="请输入手机号" maxlength="11" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="性别">
                <el-select v-model="formData.gender" placeholder="请选择性别" style="width: 100%" :teleported="false">
                  <el-option
                    v-for="opt in MemberGenderOptions"
                    :key="opt.value"
                    :label="opt.label"
                    :value="opt.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="生日">
                <el-date-picker
                  v-model="formData.birthday"
                  type="date"
                  placeholder="请选择生日"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                  :teleported="false"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 会员信息 -->
        <div class="form-section">
          <div class="section-title">会员信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="会员等级" prop="memberLevelId">
                <el-select
                  v-model="formData.memberLevelId"
                  placeholder="请选择会员等级"
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option
                    v-for="level in levelOptions"
                    :key="level.levelId"
                    :label="level.levelName"
                    :value="level.levelId"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="会员卡类型">
                <el-select
                  v-model="formData.memberCardType"
                  placeholder="请选择会员卡类型"
                  style="width: 100%"
                  :teleported="false"
                >
                  <el-option label="普通卡" value="normal" />
                  <el-option label="银卡" value="silver" />
                  <el-option label="金卡" value="gold" />
                  <el-option label="钻石卡" value="diamond" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 账户信息 -->
        <div class="form-section" v-if="!isEdit">
          <div class="section-title">账户信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="初始余额">
                <el-input-number
                  v-model="formData.initialBalance"
                  :precision="2"
                  :min="0"
                  :max="999999"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="初始积分">
                <el-input-number
                  v-model="formData.initialPoints"
                  :min="0"
                  :max="999999"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <!-- 备注 -->
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注"
            maxlength="200"
            show-word-limit
          />
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
      title="会员详情"
      width="800px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <div v-loading="detailLoading" element-loading-text="正在加载详情...">
        <template v-if="currentMember">
          <!-- 基本信息 -->
          <div class="detail-section">
            <div class="detail-title">基本信息</div>
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="会员卡号">{{ currentMember.memberNo }}</el-descriptions-item>
              <el-descriptions-item label="会员姓名">{{ currentMember.nickname }}</el-descriptions-item>
              <el-descriptions-item label="手机号">{{ currentMember.phone }}</el-descriptions-item>
              <el-descriptions-item label="性别">
                {{ MemberGenderOptions.find(g => g.value === currentMember.gender)?.label || '未知' }}
              </el-descriptions-item>
              <el-descriptions-item label="生日">{{ currentMember.birthday || '-' }}</el-descriptions-item>
              <el-descriptions-item label="邮箱">{{ currentMember.email || '-' }}</el-descriptions-item>
              <el-descriptions-item label="会员等级">{{ currentMember.levelName }}</el-descriptions-item>
              <el-descriptions-item label="注册时间">{{ formatTime(currentMember.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="会员状态">
                <StatusTag
                  :status="getStatusColor(currentMember.status)"
                  :label="getStatusLabel(currentMember.status)"
                  size="small"
                  variant="light"
                />
              </el-descriptions-item>
              <el-descriptions-item label="注册渠道">{{ currentMember.registerChannel }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 账户信息 -->
          <div class="detail-section">
            <div class="detail-title">账户信息</div>
            <el-descriptions :column="3" border size="small">
              <el-descriptions-item label="账户余额">¥{{ formatMoney(currentMember.balance) }}</el-descriptions-item>
              <el-descriptions-item label="当前积分">{{ getPoints(currentMember) }}</el-descriptions-item>
              <el-descriptions-item label="消费次数">{{ currentMember.orderCount }}次</el-descriptions-item>
              <el-descriptions-item label="累计充值">¥{{ formatMoney(currentMember.totalRecharge) }}</el-descriptions-item>
              <el-descriptions-item label="累计消费">¥{{ formatMoney(currentMember.totalConsume) }}</el-descriptions-item>
              <el-descriptions-item label="最后消费">{{ formatTime(currentMember.lastOrderTime) }}</el-descriptions-item>
            </el-descriptions>
          </div>

          <!-- 消费记录 -->
          <div class="detail-section">
            <div class="detail-title">消费记录</div>
            <el-table :data="consumeRecords" size="small" border max-height="200">
              <el-table-column prop="orderNo" label="订单号" min-width="150" />
              <el-table-column prop="amount" label="消费金额" width="110" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.amount) }}</template>
              </el-table-column>
              <el-table-column prop="storeName" label="消费门店" min-width="120" />
              <el-table-column prop="consumeTime" label="消费时间" width="160">
                <template #default="{ row }">{{ formatTime(row.consumeTime) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="consumeRecords.length === 0" description="暂无消费记录" :image-size="60" />
          </div>

          <!-- 充值记录 -->
          <div class="detail-section">
            <div class="detail-title">充值记录</div>
            <el-table :data="rechargeRecords" size="small" border max-height="200">
              <el-table-column prop="recordNo" label="充值单号" min-width="150" />
              <el-table-column prop="planName" label="充值方案" min-width="120" />
              <el-table-column prop="rechargeAmount" label="充值金额" width="100" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.rechargeAmount) }}</template>
              </el-table-column>
              <el-table-column prop="bonusAmount" label="赠送金额" width="100" align="right">
                <template #default="{ row }">¥{{ formatMoney(row.bonusAmount) }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="充值时间" width="160">
                <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
              </el-table-column>
            </el-table>
            <el-empty v-if="rechargeRecords.length === 0" description="暂无充值记录" :image-size="60" />
          </div>

          <!-- 积分变动记录 -->
          <div class="detail-section">
            <div class="detail-title">积分变动记录</div>
            <el-table :data="pointRecords" size="small" border max-height="200">
              <el-table-column prop="type" label="类型" width="100">
                <template #default="{ row }">
                  <StatusTag
                    :status="row.type === 'earn' ? 'success' : 'warning'"
                    :label="row.type === 'earn' ? '获得' : '消耗'"
                    size="small"
                    variant="light"
                  />
                </template>
              </el-table-column>
              <el-table-column prop="points" label="积分" width="100" align="right">
                <template #default="{ row }">
                  <span :class="row.points > 0 ? 'text-success' : 'text-warning'">
                    {{ row.points > 0 ? '+' : '' }}{{ row.points }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="原因" min-width="150" />
              <el-table-column prop="time" label="时间" width="160" />
            </el-table>
          </div>
        </template>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="handleEdit(currentMember!)">编辑</el-button>
        <el-button type="success" @click="handleRecharge(currentMember!)">充值</el-button>
      </template>
    </el-dialog>

    <!-- 充值对话框 -->
    <el-dialog
      v-model="rechargeVisible"
      title="会员充值"
      width="500px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form ref="rechargeFormRef" :model="rechargeForm" :rules="rechargeFormRules" label-width="100px">
        <el-form-item label="会员姓名">
          <el-input v-model="rechargeForm.memberName" disabled />
        </el-form-item>
        <el-form-item label="充值金额" prop="rechargeAmount">
          <el-input-number
            v-model="rechargeForm.rechargeAmount"
            :precision="2"
            :min="0.01"
            :max="999999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="赠送金额">
          <el-input-number
            v-model="rechargeForm.bonusAmount"
            :precision="2"
            :min="0"
            :max="999999"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-select
            v-model="rechargeForm.paymentMethod"
            placeholder="请选择支付方式"
            style="width: 100%"
            :teleported="false"
          >
            <el-option label="微信支付" value="wechat" />
            <el-option label="支付宝" value="alipay" />
            <el-option label="现金" value="cash" />
            <el-option label="银行卡" value="bank_card" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="rechargeForm.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleRechargeSubmit">
          确认充值
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

  :deep(.el-table__header-wrapper th) {
    border-bottom: 2px solid var(--fts-border-primary);
  }

  :deep(.el-table__row td) {
    border-bottom: 1px solid var(--fts-border-primary);
  }
}

// 会员卡号
.member-no {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
  font-family: monospace;
}

// 会员信息单元格
.member-cell {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  white-space: nowrap;
  overflow: hidden;
}

.member-info {
  display: flex;
  flex-direction: column;
  overflow: hidden;

  .member-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    font-size: var(--fts-font-size-base);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
}

// 手机号
.phone-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
}

// 余额
.balance-text {
  font-weight: var(--fts-font-weight-semibold);
  font-variant-numeric: tabular-nums;
  color: var(--fts-primary);
}

// 积分
.points-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
}

// 次数
.count-text {
  font-variant-numeric: tabular-nums;
  color: var(--fts-text-primary);
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
  flex-wrap: wrap;
}

// 分页区域
.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
}

// 表单分区
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

// 详情分区
.detail-section {
  margin-bottom: var(--fts-space-4);

  .detail-title {
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
    margin-bottom: var(--fts-space-3);
    padding-left: var(--fts-space-2);
    border-left: 3px solid var(--fts-primary);
  }
}

// 文字颜色
.text-success {
  color: var(--fts-success);
  font-weight: var(--fts-font-weight-medium);
}

.text-warning {
  color: var(--fts-warning);
  font-weight: var(--fts-font-weight-medium);
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
    flex-wrap: wrap;
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
