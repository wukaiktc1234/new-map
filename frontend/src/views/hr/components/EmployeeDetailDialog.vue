<script setup lang="ts">
/**
 * 员工详情查看对话框（共用组件）
 *
 * 【复用场景】
 * - HREmployee.vue：员工档案管理页查看员工详情
 * - HROrganization.vue：组织架构页查看节点下员工详情
 *
 * 【数据闭环设计】
 * - Tab 1 基本信息：员工基础信息 + 用工信息 + 超龄返聘信息
 * - Tab 2 合同信息：该员工的合同列表（调用 contractApi.getList）
 * - Tab 3 培训学习：该员工的培训学习记录（调用 trainingApi.getStudyRecords）
 * - Tab 4 健康证：该员工的健康证信息（调用 healthCertApi.getList 过滤）
 *
 * 【设计原则】
 * - 使用 --fts-* CSS 变量，不硬编码颜色
 * - Tab 懒加载，切换时才请求对应数据
 * - 完整档案在对话框内展示，不再跳转到不存在的路由
 */
import { ref, computed, watch } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { contractApi } from '@/api/hr/contract'
import { trainingApi } from '@/api/hr/training'
import { healthCertificateApi } from '@/api/hr/health-certificate'
import type { EmployeeContract } from '@/types/hr/contract'
import type { StudyRecord } from '@/types/hr/training'
import type { HealthCertificate } from '@/types/healthCertificate'

/* ===== 合同类型标签映射（兼容后端实体常量与前端类型定义） ===== */
const contractTypeLabelMap: Record<string, string> = {
  // 后端 EmployeeLaborContract 实体常量
  'fixed-term': '固定期限',
  'open-ended': '无固定期限',
  project: '项目制',
  // 前端 ContractType 定义
  labor: '劳动合同',
  over_age: '超龄用工协议',
  intern: '实习协议',
  part_time: '兼职协议',
  confidentiality: '保密协议',
  non_compete: '竞业限制协议',
  dispatch: '劳务派遣',
  outsourcing: '劳务外包',
}

/* ===== 合同状态标签映射（兼容后端实体常量与前端类型定义） ===== */
const contractStatusLabelMap: Record<string, string> = {
  // 后端 EmployeeLaborContract 实体常量
  draft: '草稿',
  pending: '待签署',
  signed: '已签署',
  active: '生效中',
  expired: '已到期',
  terminated: '已终止',
  // 前端 ContractStatus 定义
  pending_approval: '审批中',
  pending_sign: '待签署',
  company_signed: '公司已签',
  expiring: '即将到期',
  renewed: '已续签',
  archived: '已归档',
}

/* ===== 健康证适用岗位关键词 ===== */
const healthCertIncludeKeywords = ['店长', '员工', '店员', '仓储', '仓库', '厨师', '后厨', '前厅', '配送', '分拣', '理货', '营业员', '导购']
const healthCertExcludeKeywords = ['财务', '人事', '行政', '客服', '文员', '助理']

/** 员工信息（与 HREmployee.vue 中的 Employee 接口对齐） */
export interface EmployeeDetail {
  id: number | string
  name: string
  employeeNo?: string
  department?: string
  departmentId?: string
  position?: string
  positionId?: string
  phone?: string
  email?: string
  entryDate?: string
  birthday?: string
  education?: string
  emergencyContact?: string
  status: string
  salary?: number
  employmentType?: string
  lifecycleStatus?: string
  retirementDate?: string
  reemploymentDate?: string
  agreementNo?: string
  workLocationType?: string
  storeId?: string
  warehouseId?: string
  workLocationName?: string
}

const props = defineProps<{
  visible: boolean
  employee: EmployeeDetail | null
}>()

const emit = defineEmits<{
  (e: 'update:visible', val: boolean): void
}>()

/* ===== Tab 状态 ===== */
const activeTab = ref('basic')

/* ===== 关联数据状态 ===== */
const contractLoading = ref(false)
const contractList = ref<EmployeeContract[]>([])
const trainingLoading = ref(false)
const trainingRecords = ref<StudyRecord[]>([])
const healthCertLoading = ref(false)
const healthCertList = ref<HealthCertificate[]>([])

/* ===== 已加载标记（避免重复请求） ===== */
const loadedTabs = ref<Set<string>>(new Set())

/** 超龄返聘法律风险提示 */
const overAgeLegalWarning = computed(() => {
  if (!props.employee || props.employee.employmentType !== 'over_age') return ''
  const hasRetirement = !!props.employee.retirementDate
  const hasReemployment = !!props.employee.reemploymentDate
  const hasAgreement = !!props.employee.agreementNo
  if (!hasRetirement || !hasReemployment) {
    return '该员工为超龄返聘人员，但缺少退休日期或返聘日期信息，请完善相关资料。'
  }
  if (!hasAgreement) {
    return '该员工为超龄返聘人员，但未关联劳务协议编号，请及时签订劳务协议。'
  }
  return ''
})

/** 是否显示健康证Tab（仅食品接触类岗位需要） */
const showHealthCertTab = computed(() => {
  if (!props.employee) return false
  const position = (props.employee.position || '').toLowerCase()
  const department = (props.employee.department || '').toLowerCase()
  if (healthCertExcludeKeywords.some(k => position.includes(k) || department.includes(k))) {
    return false
  }
  return healthCertIncludeKeywords.some(k => position.includes(k) || department.includes(k))
})

/** 监听对话框打开，重置状态 */
watch(() => props.visible, (val) => {
  if (val) {
    activeTab.value = 'basic'
    loadedTabs.value.clear()
    contractList.value = []
    trainingRecords.value = []
    healthCertList.value = []
  }
})

/** Tab 切换时懒加载关联数据 */
watch(activeTab, (tab) => {
  if (!props.employee || loadedTabs.value.has(tab)) return
  if (tab === 'contracts') loadContracts()
  else if (tab === 'training') loadTrainingRecords()
  else if (tab === 'healthCert') loadHealthCerts()
})

/* ===== 加载合同信息 ===== */
async function loadContracts() {
  if (!props.employee) return
  contractLoading.value = true
  try {
    const res = await contractApi.getList({ employeeId: props.employee.id, page: 1, pageSize: 50 } as never)
    contractList.value = res?.records || []
    loadedTabs.value.add('contracts')
  } catch {
    contractList.value = []
  } finally {
    contractLoading.value = false
  }
}

/* ===== 加载培训学习记录 ===== */
async function loadTrainingRecords() {
  if (!props.employee) return
  trainingLoading.value = true
  try {
    const res = await trainingApi.getStudyRecords({ employeeId: String(props.employee.id), page: 1, pageSize: 50 } as never)
    trainingRecords.value = res?.records || []
    loadedTabs.value.add('training')
  } catch {
    trainingRecords.value = []
  } finally {
    trainingLoading.value = false
  }
}

/* ===== 加载健康证信息 ===== */
async function loadHealthCerts() {
  if (!props.employee) return
  healthCertLoading.value = true
  try {
    const res = await healthCertificateApi.getList({ keyword: props.employee.name, page: 1, pageSize: 50 })
    healthCertList.value = (res?.records || []).filter(c => c.employeeId === String(props.employee!.id))
    loadedTabs.value.add('healthCert')
  } catch {
    healthCertList.value = []
  } finally {
    healthCertLoading.value = false
  }
}

/** 用工类型标签 */
function getEmploymentTypeLabel(type: string): string {
  const map: Record<string, string> = {
    full_time: '全职', part_time: '兼职', intern: '实习生',
    dispatch: '劳务派遣', over_age: '超龄返聘',
  }
  return map[type] || '全职'
}

/** 生命周期状态标签 */
function getLifecycleStatusLabel(status?: string): string {
  const map: Record<string, string> = {
    candidate: '候选人', onboarding: '入职办理中', probation: '试用期',
    active: '正式在职', over_age_employed: '超龄返聘',
    resigning: '离职办理中', resigned: '已离职', retired: '已退休',
  }
  return map[status || 'active'] || '正式在职'
}

/** 合同状态标签 */
function getContractStatusLabel(status?: string): string {
  return (status && contractStatusLabelMap[status]) || status || '-'
}

/** 合同类型标签 */
function getContractTypeLabel(type?: string): string {
  return (type && contractTypeLabelMap[type]) || type || '-'
}

/** 日期格式化 */
function formatDate(dateStr?: string): string {
  if (!dateStr) return '-'
  return dateStr
}

/** 薪资格式化 */
function formatSalary(salary?: number): string {
  if (!salary && salary !== 0) return '-'
  return `¥ ${salary.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })} 元`
}

/** 关闭对话框 */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="员工完整档案"
    width="1200px"
    class="fts-dialog--xl"
    :close-on-click-modal="false"
    :destroy-on-close="true"
    :lock-scroll="false"
    @update:model-value="handleClose"
  >
    <template v-if="employee">
      <!-- 超龄返聘法律风险提示 -->
      <el-alert
        v-if="overAgeLegalWarning"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: var(--fts-space-4);"
      >
        <template #title>法律风险提示</template>
        <div style="line-height: 1.6; margin-top: var(--fts-space-1);">{{ overAgeLegalWarning }}</div>
      </el-alert>

      <!-- Tab 结构：基本信息 | 合同信息 | 培训学习 | 健康证 -->
      <el-tabs v-model="activeTab">
        <!-- ===== Tab 1: 基本信息 ===== -->
        <el-tab-pane label="基本信息" name="basic">
          <el-descriptions title="基本信息" :column="2" border style="margin-bottom: var(--fts-space-4);">
            <el-descriptions-item label="姓名">{{ employee.name }}</el-descriptions-item>
            <el-descriptions-item label="工号">{{ employee.employeeNo }}</el-descriptions-item>
            <el-descriptions-item label="部门">{{ employee.department || '-' }}</el-descriptions-item>
            <el-descriptions-item label="岗位">{{ employee.position || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ employee.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ employee.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="学历">{{ employee.education || '-' }}</el-descriptions-item>
            <el-descriptions-item label="紧急联系人">{{ employee.emergencyContact || '-' }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <StatusTag :status="employee.status" size="small" />
            </el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ formatDate(employee.birthday) }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions title="用工信息" :column="2" border style="margin-bottom: var(--fts-space-4);">
            <el-descriptions-item label="用工类型">
              <StatusTag
                v-if="employee.employmentType"
                :status="employee.employmentType === 'over_age' ? 'warning' : 'info'"
                :label="getEmploymentTypeLabel(employee.employmentType)"
                size="small"
              />
              <span v-else>全职</span>
            </el-descriptions-item>
            <el-descriptions-item label="生命周期">{{ getLifecycleStatusLabel(employee.lifecycleStatus) }}</el-descriptions-item>
            <el-descriptions-item label="入职日期">{{ formatDate(employee.entryDate) }}</el-descriptions-item>
            <el-descriptions-item label="当前薪资">{{ formatSalary(employee.salary) }}</el-descriptions-item>
          </el-descriptions>

          <el-descriptions
            v-if="employee.employmentType === 'over_age'"
            title="超龄返聘信息"
            :column="2"
            border
          >
            <el-descriptions-item label="退休日期">{{ formatDate(employee.retirementDate) }}</el-descriptions-item>
            <el-descriptions-item label="返聘日期">{{ formatDate(employee.reemploymentDate) }}</el-descriptions-item>
            <el-descriptions-item label="劳务协议编号" :span="2">{{ employee.agreementNo || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>

        <!-- ===== Tab 2: 合同信息（数据闭环：员工→合同关联展示） ===== -->
        <el-tab-pane label="合同信息" name="contracts">
          <el-table :data="contractList" v-loading="contractLoading" border size="small" style="width: 100%;">
            <el-table-column prop="contractNo" label="合同编号" min-width="120" />
            <el-table-column prop="contractType" label="合同类型" width="100">
              <template #default="{ row }">{{ getContractTypeLabel(row.contractType) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <StatusTag :status="row.status" :label="getContractStatusLabel(row.status)" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="开始日期" width="110" />
            <el-table-column prop="endDate" label="结束日期" width="110">
              <template #default="{ row }">{{ row.endDate || '无固定期限' }}</template>
            </el-table-column>
            <el-table-column prop="signDate" label="签订日期" width="110" />
            <el-table-column prop="carrier" label="载体" width="70">
              <template #default="{ row }">
                <StatusTag :status="(row.carrier || 'electronic') === 'electronic' ? 'active' : 'info'" :label="(row.carrier || 'electronic') === 'electronic' ? '电子' : '纸质'" size="small" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!contractLoading && contractList.length === 0" description="暂无合同记录" :image-size="60" />
        </el-tab-pane>

        <!-- ===== Tab 3: 培训学习记录（数据闭环：员工→培训关联展示） ===== -->
        <el-tab-pane label="培训学习" name="training">
          <el-table :data="trainingRecords" v-loading="trainingLoading" border size="small" style="width: 100%;">
            <el-table-column prop="courseName" label="课程名称" min-width="150" />
            <el-table-column prop="studyTime" label="学习时间" width="160" />
            <el-table-column prop="duration" label="学习时长" width="90">
              <template #default="{ row }">{{ row.duration ? `${row.duration}分钟` : '-' }}</template>
            </el-table-column>
            <el-table-column prop="progress" label="进度" width="80">
              <template #default="{ row }">{{ row.progress != null ? `${row.progress}%` : '-' }}</template>
            </el-table-column>
            <el-table-column prop="score" label="成绩" width="70">
              <template #default="{ row }">{{ row.score != null ? row.score : '-' }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <StatusTag :status="row.status === 'completed' ? 'active' : row.status === 'in_progress' ? 'pending' : 'info'" :label="row.status === 'completed' ? '已完成' : row.status === 'in_progress' ? '进行中' : '未开始'" size="small" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!trainingLoading && trainingRecords.length === 0" description="暂无培训学习记录" :image-size="60" />
        </el-tab-pane>

        <!-- ===== Tab 4: 健康证信息（仅食品接触类岗位显示） ===== -->
        <el-tab-pane v-if="showHealthCertTab" label="健康证" name="healthCert">
          <el-table :data="healthCertList" v-loading="healthCertLoading" border size="small" style="width: 100%;">
            <el-table-column prop="certificateNo" label="证书编号" min-width="120" />
            <el-table-column prop="issueDate" label="发证日期" width="110" />
            <el-table-column prop="expiryDate" label="有效期至" width="110" />
            <el-table-column prop="issuingAuthority" label="发证机关" min-width="120" />
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <StatusTag :status="row.status === 'valid' ? 'active' : row.status === 'expired' ? 'error' : 'warning'" :label="row.status === 'valid' ? '有效' : row.status === 'expired' ? '已过期' : '即将过期'" size="small" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!healthCertLoading && healthCertList.length === 0" description="暂无健康证记录" :image-size="60" />
        </el-tab-pane>
      </el-tabs>
    </template>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>
