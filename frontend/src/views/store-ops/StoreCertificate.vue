<script setup lang="ts">
import { ref, onMounted, computed, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Document, Check, Clock, CircleClose, Plus, Loading, ArrowDown, Bell } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable, { type DataTableColumn } from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import CertificateRenewalDialog from './components/CertificateRenewalDialog.vue'
import type { RenewalData } from './components/CertificateRenewalDialog.vue'
import ApprovalDialog from '@/components/business/ApprovalDialog.vue'
import ApprovalTimeline from '@/components/business/ApprovalTimeline.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { certificateApi } from '@/api/store-ops/certificate'
import { certificateDataConverter } from '@/api/store-ops/converters'
import { exportToCSV } from '@/utils/export-csv'
import { fenToYuan } from '@/utils/money'
import type { Certificate, CreateCertificateDTO, CertificateRenewalRecord, CertificateExpenseRecord, ExpenseStatus, CreateExpenseDTO, ExpenseAuditLog } from '@/types/store-operation'

const { pagination } = useStandardPage()

/** 处理分页大小变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
}

/** 处理当前页变化 */
function handleCurrentChange(page: number): void {
  pagination.current = page
}

// 从URL参数获取需要高亮的证件ID（用于待办事项联动 - spec.md F-004）
const route = useRoute()
const highlightCertId = ref<string>('')
const isFromTask = ref(false)

onMounted(() => {
  // 检查是否从待办事项页面跳转过来
  const urlHighlightId = route.query.highlightId as string | undefined
  if (urlHighlightId) {
    highlightCertId.value = urlHighlightId
    isFromTask.value = true
    ElMessage.info('已定位到即将到期的证件，请及时办理续期')
  }

  loadData()
  // P0阶段：初始化时加载费用记录
  loadExpenseRecords()
})

const searchForm = ref({ certType: '', certStatus: '', keyword: '' })
const loading = ref(false)

// ==================== 角色模拟（演示审批流程权限） ====================
// TODO: 审批流程接口待后端实现。当前角色切换为前端演示用途，
// 实际角色权限应通过后端 RBAC 接口获取，审批人姓名应从用户服务获取。
type UserRole = 'staff' | 'store_manager' | 'finance'
const currentRole = ref<UserRole>('store_manager')
const roleLabels: Record<UserRole, string> = {
  staff: '普通员工',
  store_manager: '店长',
  finance: '财务',
}
const roleNames: Record<UserRole, string> = {
  staff: '张小明',
  store_manager: '王店长',
  finance: '李财务',
}

/** 客户端临时 ID 自增计数器（替代 Math.random 伪造 ID） */
let clientIdCounter = 0
/** 生成客户端临时 ID（格式：前缀_时间戳_序号），后端落地后应使用后端返回的真实 ID */
function generateClientTempId(prefix: string): string {
  clientIdCounter += 1
  return `${prefix}_${Date.now()}_${clientIdCounter}`
}

/** 当前角色是否有权执行指定费用状态的操作 */
function canOperateExpense(status: ExpenseStatus | null): boolean {
  if (!status) return false
  switch (status) {
    case 'unreimbursed': return currentRole.value === 'staff' || currentRole.value === 'store_manager'
    case 'submitted': return currentRole.value === 'store_manager'
    case 'manager_approved': return currentRole.value === 'finance'
    case 'finance_approved': return currentRole.value === 'finance'
    case 'reimbursed': return false
    default: return false
  }
}

// TODO P0: 审计日志必须持久化到后端，禁止仅存内存。
// 当前 expenseAuditLogs 仅保存在前端内存中，页面刷新后丢失，存在法律合规风险。
// 后端审批流程接口落地后，所有审批操作日志必须通过 API 持久化到数据库，
// 且审计日志表应仅允许 INSERT，禁止 UPDATE 和 DELETE（符合审计规范）。
const expenseAuditLogs = ref<ExpenseAuditLog[]>([])

/** 记录审批操作日志（当前仅存内存，TODO: 待后端审计日志接口落地后改为调用真实 API 持久化） */
function addAuditLog(expenseId: string, action: ExpenseAuditLog['action'], fromStatus: ExpenseStatus, toStatus: ExpenseStatus, remark: string): void {
  expenseAuditLogs.value.push({
    // 修复 P0：移除 Math.random 伪造 ID，改用时间戳+自增序号生成的客户端临时 ID
    // 后端落地后应使用后端返回的真实 logId
    logId: generateClientTempId('LOG'),
    expenseId,
    action,
    operatorRole: currentRole.value,
    operatorName: roleNames[currentRole.value],
    remark,
    operateTime: new Date().toISOString(),
    fromStatus,
    toStatus,
  })
}

/** 获取指定费用记录的审批日志 */
function getExpenseAuditLogs(expenseId: string): ExpenseAuditLog[] {
  return expenseAuditLogs.value
    .filter(log => log.expenseId === expenseId)
    .sort((a, b) => new Date(a.operateTime).getTime() - new Date(b.operateTime).getTime())
}

/** 获取审批操作的中文名称 */
function getAuditActionLabel(action: ExpenseAuditLog['action']): string {
  const map: Record<ExpenseAuditLog['action'], string> = {
    submit: '提交报销',
    approve: '店长审批',
    reject: '驳回',
    finance_approve: '财务审核',
    write_off: '核销确认',
  }
  return map[action] || action
}

/** 获取审批操作的角色标签 */
function getAuditRoleLabel(role: UserRole): string {
  return roleLabels[role] || role
}

// ==================== 流程通知机制（模拟消息推送） ====================

/** 流程通知记录 */
interface FlowNotification {
  notifyId: string
  expenseId: string
  notifyType: 'next_approver' | 'result' | 'reject'
  targetRole: UserRole
  targetName: string
  message: string
  notifyTime: string
  read: boolean
}

const flowNotifications = ref<FlowNotification[]>([])

/** 获取每个审批环节完成后需要通知的下一角色 */
function getNextApprover(status: ExpenseStatus): { role: UserRole; name: string } | null {
  switch (status) {
    case 'submitted': return { role: 'store_manager', name: roleNames.store_manager }
    case 'manager_approved': return { role: 'finance', name: roleNames.finance }
    case 'finance_approved': return { role: 'finance', name: roleNames.finance }
    default: return null
  }
}

/** 发送流程通知（模拟） */
function sendFlowNotification(expenseId: string, status: ExpenseStatus, certName: string, action: 'submit' | 'approve' | 'finance_approve' | 'write_off' | 'reject'): void {
  const now = new Date().toISOString()

  if (action === 'reject') {
    // 驳回通知提交人
    flowNotifications.value.push({
      notifyId: generateClientTempId('NTF'),
      expenseId,
      notifyType: 'reject',
      targetRole: 'staff',
      targetName: roleNames.staff,
      message: `您提交的"${certName}"报销申请已被驳回，请查看原因并重新提交`,
      notifyTime: now,
      read: false,
    })
    ElMessage.info(`[通知] 已通知${roleNames.staff}：报销申请被驳回`)
    return
  }

  // 通知下一审批人
  const nextApprover = getNextApprover(status)
  if (nextApprover) {
    const actionLabels: Record<string, string> = {
      submit: '已提交报销申请',
      approve: '店长已审批通过',
      finance_approve: '财务已审核通过',
      write_off: '费用已核销完成',
    }
    flowNotifications.value.push({
      notifyId: generateClientTempId('NTF'),
      expenseId,
      notifyType: 'next_approver',
      targetRole: nextApprover.role,
      targetName: nextApprover.name,
      message: `"${certName}"${actionLabels[action] || '状态已变更'}，请及时处理`,
      notifyTime: now,
      read: false,
    })
    ElMessage.info(`[通知] 已通知${nextApprover.name}（${roleLabels[nextApprover.role]}）处理`)
  }

  // 核销完成时通知提交人
  if (action === 'write_off') {
    flowNotifications.value.push({
      notifyId: generateClientTempId('NTF'),
      expenseId,
      notifyType: 'result',
      targetRole: 'staff',
      targetName: roleNames.staff,
      message: `"${certName}"报销费用已核销完成，请确认`,
      notifyTime: now,
      read: false,
    })
    ElMessage.info(`[通知] 已通知${roleNames.staff}：费用核销完成`)
  }
}

/** 获取指定费用的通知记录 */
function getExpenseNotifications(expenseId: string): FlowNotification[] {
  return flowNotifications.value
    .filter(n => n.expenseId === expenseId)
    .sort((a, b) => new Date(a.notifyTime).getTime() - new Date(b.notifyTime).getTime())
}

// 续期审批对话框相关（统一审批组件）
const renewalApprovalVisible = ref(false)
const renewalApprovalBusinessId = ref('')
const renewalApprovalBusinessType = ref('cert_renewal')
const renewalApprovalNodeName = ref('店长审批')

// 对话框相关
const dialogVisible = ref(false)
const dialogTitle = ref('新增证件')
const dialogMode = ref<'create' | 'update'>('create')
const editingCertId = ref<string | null>(null)

// 续期对话框相关
const renewalDialogVisible = ref(false)
const renewingCert = ref<Certificate | null>(null)
const renewalHistoryDialogVisible = ref(false)
const selectedCertHistory = ref<CertificateRenewalRecord[]>([])
const historyLoading = ref(false)

// 费用记录相关（P0阶段 - 证照续期后续联动）
const expenseRecords = ref<CertificateExpenseRecord[]>([])

// 表单数据
const certForm = reactive({
  certName: '',
  certType: 'business_license' as Certificate['certType'],
  certNumber: '',
  holderType: 'company' as 'company' | 'employee',
  holderId: '',
  holderName: '',
  issueDate: '',
  expiryDate: '',
  issuer: '',
  fileUrl: '',  // 证件照片URL
})

// 证件照片相关
const certPhotoList = ref<any[]>([])
const showPhotoViewer = ref(false)
const previewPhotoUrl = ref('')

// 表单校验规则
const formRules = {
  certName: [{ required: true, message: '请输入证件名称', trigger: 'blur' }],
  certType: [{ required: true, message: '请选择证件类型', trigger: 'change' }],
  holderName: [{ required: true, message: '请输入持有人/单位', trigger: 'blur' }],
  issueDate: [{ required: true, message: '请选择发证日期', trigger: 'change' }],
  expiryDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

const formRef = ref()

const columns: DataTableColumn[] = [
  { prop: 'certName', label: '证件名称', minWidth: 100, showOverflowTooltip: true },
  { prop: 'certType', label: '证件类型', width: 150, slot: 'certType' },
  { prop: 'holderName', label: '持有人/单位', minWidth: 85, showOverflowTooltip: true },
  { prop: 'issueDate', label: '发证日期', width: 100 },
  { prop: 'expiryDate', label: '到期日期', width: 100 },
  { prop: 'daysLeft', label: '剩余天数', width: 75, align: 'center', slot: 'daysLeft' },
  { prop: 'status', label: '状态', width: 105, slot: 'status' },
  { prop: 'expenseStatus', label: '续期费用状态', width: 160, slot: 'expenseStatus' },
]

/** 所有数据（用于搜索过滤） */
const allRecords = ref<Certificate[]>([])
/** 当前显示的筛选后数据 */
const filteredRecords = ref<Certificate[]>([])

/** 获取证件类型中文标签 */
function getCertTypeLabel(type: Certificate['certType']): string {
  const map: Record<Certificate['certType'], string> = {
    business_license: '营业执照',
    catering_license: '餐饮服务许可证',
    hygiene_license: '卫生许可证',
    safety_license: '消防安全合格证',
    health_certificate: '健康证',
    pollution_license: '排污许可证',
    other: '其他'
  }
  return map[type] || type
}

/**
 * 到期计算工具函数
 * 计算证件到期状态和剩余天数
 * @param expiryDate - 到期日期字符串 (YYYY-MM-DD 格式)
 * @returns 包含剩余天数和到期等级的对象
 */
function calculateExpiry(expiryDate: string): { daysLeft: number; level: 'safe' | 'warning' | 'danger' | 'expired' } {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expiry = new Date(expiryDate)
  const diffTime = expiry.getTime() - today.getTime()
  const daysLeft = Math.ceil(diffTime / (1000 * 60 * 60 * 24))

  let level: 'safe' | 'warning' | 'danger' | 'expired' = 'safe'
  if (daysLeft < 0) level = 'expired'
  else if (daysLeft <= 60) level = 'danger'
  else if (daysLeft <= 90) level = 'warning'

  return { daysLeft, level }
}

/** 获取到期状态对应的徽章类名 */
function getExpiryBadgeClass(level: 'safe' | 'warning' | 'danger' | 'expired'): string {
  const classMap: Record<string, string> = {
    safe: 'expiry-badge-safe',
    warning: 'expiry-badge-warning',
    danger: 'expiry-badge-danger',
    expired: 'expiry-badge-expired'
  }
  return classMap[level] || ''
}

/** 获取剩余天数样式类（用于数字颜色区分） */
function getDaysLeftClass(row: Certificate): string {
  if (!row.expiryDate) return 'days-normal'
  const { level } = calculateExpiry(row.expiryDate)
  switch (level) {
    case 'safe': return 'days-safe'
    case 'warning': return 'days-warning'
    case 'danger': return 'days-danger'
    case 'expired': return 'days-expired'
    default: return 'days-normal'
  }
}

/** 获取到期状态显示文本 */
function getExpiryText(cert: Certificate): string {
  if (!cert.expiryDate) return '-'

  const { daysLeft, level } = calculateExpiry(cert.expiryDate)

  switch (level) {
    case 'safe':
      return `有效期至 ${cert.expiryDate}`
    case 'warning':
      return `⚠️ 还剩${daysLeft}天`
    case 'danger':
      return `🔴 还剩${daysLeft}天`
    case 'expired':
      return `✕ 已过期${Math.abs(daysLeft)}天`
    default:
      return '-'
  }
}

/** 获取证件的费用状态（从费用记录中查找最新的一条） */
function getExpenseStatus(certificateId: string): ExpenseStatus | null {
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === certificateId)
  if (certExpenses.length === 0) return null
  // 按更新时间倒序，取最新的状态
  const latestExpense = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  return latestExpense.expenseStatus
}

/** 获取证件的费用状态显示标签 */
function getExpenseStatusLabel(status: ExpenseStatus | null): string {
  switch (status) {
    case 'unreimbursed': return '待报销'
    case 'submitted': return '已提交'
    case 'manager_approved': return '店长已批'
    case 'finance_approved': return '财务已审'
    case 'reimbursed': return '已核销'
    default: return '-'
  }
}

/** 获取费用状态的 StatusTag status 值 */
function getExpenseStatusTagValue(status: ExpenseStatus | null): string {
  switch (status) {
    case 'unreimbursed': return 'warning'
    case 'submitted': return 'info'
    case 'manager_approved': return 'primary'
    case 'finance_approved': return 'primary'
    case 'reimbursed': return 'success'
    default: return 'default'
  }
}

/** 根据当前费用状态分发对应的操作（含角色权限校验） */
function handleExpenseAction(row: Certificate): void {
  const status = getExpenseStatus(row.certificateId)
  if (!canOperateExpense(status)) {
    const requiredRole = status === 'submitted' ? '店长' : status === 'manager_approved' || status === 'finance_approved' ? '财务' : '员工/店长'
    ElMessage.warning(`当前角色（${roleLabels[currentRole.value]}）无权执行此操作，需要${requiredRole}角色`)
    return
  }
  switch (status) {
    case 'unreimbursed':
      openReimburseDialog(row)
      break
    case 'submitted':
      openApprovalDialog(row, 'approve')
      break
    case 'manager_approved':
      openApprovalDialog(row, 'finance_review')
      break
    case 'finance_approved':
      openWriteOffDialog(row)
      break
  }
}

/** 处理续期费用记录创建事件（P0阶段联动） */

/** P1阶段：报销流程对话框 - 完整审批链 */
const reimburseDialogVisible = ref(false)
const reimburseTargetCert = ref<Certificate | null>(null)
const reimburseFormRef = ref()
const reimburseForm = reactive({
  reimburseNo: '',
  expenseType: '',
  remark: '',
})
const reimburseRules = {
  expenseType: [{ required: true, message: '请选择费用类型', trigger: 'change' }],
  remark: [{ required: true, message: '请输入报销说明', trigger: 'blur' }],
}

/** 审批流程对话框 */
const approvalDialogVisible = ref(false)
const approvalTargetCert = ref<Certificate | null>(null)
const approvalAction = ref<'approve' | 'reject' | 'finance_review' | 'write_off'>('approve')
const approvalRemark = ref('')

/** 核销确认对话框 */
const writeOffDialogVisible = ref(false)
const writeOffTargetCert = ref<Certificate | null>(null)
const writeOffFormRef = ref()
const writeOffForm = reactive({
  writeOffAmount: 0,
  voucherNo: '',
  remark: '',
})
const writeOffRules = {
  writeOffAmount: [{ required: true, message: '请输入核销金额', trigger: 'blur' }],
  voucherNo: [{ required: true, message: '请输入凭证号', trigger: 'blur' }],
}

/**
 * 报销审批流程步骤定义
 * 未报销 → 提交报销 → 店长审批 → 财务审核 → 核销完成
 */
const EXPENSE_WORKFLOW_STEPS = [
  { key: 'unreimbursed', label: '待报销', icon: 'Document', description: '续期费用已产生，等待提交报销' },
  { key: 'submitted', label: '已提交', icon: 'Upload', description: '报销申请已提交，等待店长审批' },
  { key: 'manager_approved', label: '店长已批', icon: 'UserFilled', description: '店长已审批通过，等待财务审核' },
  { key: 'finance_approved', label: '财务已审', icon: 'Money', description: '财务已审核通过，等待核销' },
  { key: 'reimbursed', label: '已核销', icon: 'CircleCheck', description: '费用已核销完成' },
] as const

/** 获取当前费用状态在审批流程中的步骤索引 */
function getExpenseStepIndex(status: ExpenseStatus | null): number {
  if (!status) return -1
  const idx = EXPENSE_WORKFLOW_STEPS.findIndex(s => s.key === status)
  return idx >= 0 ? idx : 0
}

/** 获取当前费用状态对应的操作按钮文本 */
function getExpenseActionLabel(status: ExpenseStatus | null): string {
  switch (status) {
    case 'unreimbursed': return '提交报销'
    case 'submitted': return '店长审批'
    case 'manager_approved': return '财务审核'
    case 'finance_approved': return '确认核销'
    default: return ''
  }
}

/** 获取当前费用状态对应的操作类型 */
function getExpenseActionType(status: ExpenseStatus | null): 'primary' | 'warning' | 'success' | 'info' {
  switch (status) {
    case 'unreimbursed': return 'primary'
    case 'submitted': return 'warning'
    case 'manager_approved': return 'success'
    case 'finance_approved': return 'success'
    default: return 'info'
  }
}

/** 获取证件最新费用金额 */
function getLatestExpenseAmount(certificateId: string): string {
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === certificateId)
  if (certExpenses.length === 0) return '0.00'
  const latest = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  // 修复：使用统一金额工具 fenToYuan 替代直接 (amount / 100).toFixed(2) 转换
  return fenToYuan(latest.amount)
}

/** 打开报销申请对话框 */
function openReimburseDialog(row: Certificate) {
  reimburseTargetCert.value = row
  // 修复 P0：原代码使用 Date.now() 伪造报销单号（BX+时间戳），属于伪造业务单据。
  // 报销单号应由后端在创建报销记录时生成并返回。当前置空，提交后由后端生成。
  // TODO: 报销单号生成接口待后端实现，提交报销后从后端响应中获取真实单号。
  reimburseForm.reimburseNo = ''
  reimburseForm.expenseType = ''
  reimburseForm.remark = ''
  reimburseDialogVisible.value = true
}

/** 提交报销申请 */
async function submitReimburse() {
  if (!reimburseFormRef.value) return
  try {
    await reimburseFormRef.value.validate()
  } catch { return }

  if (!reimburseTargetCert.value) return
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === reimburseTargetCert.value!.certificateId)
  const latestExpense = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  if (!latestExpense) return

  const fromStatus = latestExpense.expenseStatus
  // 提交报销后状态变为"已提交"，等待店长审批
  await handleUpdateExpenseStatus(latestExpense.expenseId, 'submitted')
  addAuditLog(latestExpense.expenseId, 'submit', fromStatus, 'submitted', reimburseForm.remark)
  sendFlowNotification(latestExpense.expenseId, 'submitted', reimburseTargetCert.value.certName, 'submit')
  reimburseDialogVisible.value = false
  // 报销单号由后端生成（参见 openReimburseDialog 的 TODO），此处不展示前端伪造的单号
  ElMessage.success('报销申请已提交，等待店长审批')
}

/** 打开审批流程对话框（店长审批/财务审核） */
function openApprovalDialog(row: Certificate, action: 'approve' | 'finance_review') {
  approvalTargetCert.value = row
  approvalAction.value = action
  approvalRemark.value = ''
  approvalDialogVisible.value = true
}

/** 执行审批操作 */
async function executeApproval() {
  if (!approvalTargetCert.value) return
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === approvalTargetCert.value!.certificateId)
  const latestExpense = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  if (!latestExpense) return

  const fromStatus = latestExpense.expenseStatus
  let newStatus: ExpenseStatus
  let successMsg: string
  let auditAction: ExpenseAuditLog['action']

  if (approvalAction.value === 'approve') {
    newStatus = 'manager_approved'
    successMsg = '店长审批通过，已转交财务审核'
    auditAction = 'approve'
  } else {
    newStatus = 'finance_approved'
    successMsg = '财务审核通过，可进行核销操作'
    auditAction = 'finance_approve'
  }

  await handleUpdateExpenseStatus(latestExpense.expenseId, newStatus)
  addAuditLog(latestExpense.expenseId, auditAction, fromStatus, newStatus, approvalRemark.value)
  sendFlowNotification(latestExpense.expenseId, newStatus, approvalTargetCert.value.certName, auditAction)
  approvalDialogVisible.value = false
  ElMessage.success(successMsg)
}

/** 驳回报销申请 */
async function rejectApproval() {
  if (!approvalTargetCert.value) return
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === approvalTargetCert.value!.certificateId)
  const latestExpense = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  if (!latestExpense) return

  const fromStatus = latestExpense.expenseStatus
  await handleUpdateExpenseStatus(latestExpense.expenseId, 'unreimbursed')
  addAuditLog(latestExpense.expenseId, 'reject', fromStatus, 'unreimbursed', approvalRemark.value || '驳回')
  sendFlowNotification(latestExpense.expenseId, 'unreimbursed', approvalTargetCert.value.certName, 'reject')
  approvalDialogVisible.value = false
  ElMessage.warning('已驳回，报销申请退回至待报销状态')
}

/** 打开核销确认对话框 */
function openWriteOffDialog(row: Certificate) {
  writeOffTargetCert.value = row
  const amount = getLatestExpenseAmount(row.certificateId)
  writeOffForm.writeOffAmount = parseFloat(amount)
  writeOffForm.voucherNo = ''
  writeOffForm.remark = ''
  writeOffDialogVisible.value = true
}

/** 提交核销确认 */
async function submitWriteOff() {
  if (!writeOffFormRef.value) return
  try {
    await writeOffFormRef.value.validate()
  } catch { return }

  if (!writeOffTargetCert.value) return
  const certExpenses = expenseRecords.value.filter(e => e.certificateId === writeOffTargetCert.value!.certificateId)
  const latestExpense = certExpenses.sort((a, b) =>
    new Date(b.updateTime).getTime() - new Date(a.updateTime).getTime()
  )[0]
  if (!latestExpense) return

  const fromStatus = latestExpense.expenseStatus
  await handleUpdateExpenseStatus(latestExpense.expenseId, 'reimbursed')
  addAuditLog(latestExpense.expenseId, 'write_off', fromStatus, 'reimbursed', `凭证号：${writeOffForm.voucherNo}${writeOffForm.remark ? '；' + writeOffForm.remark : ''}`)
  sendFlowNotification(latestExpense.expenseId, 'reimbursed', writeOffTargetCert.value.certName, 'write_off')
  writeOffDialogVisible.value = false
  ElMessage.success(`核销完成，凭证号：${writeOffForm.voucherNo}`)
}
async function handleExpenseCreated(expenseData: CreateExpenseDTO) {
  try {
    // 调用 API 创建费用记录（真实 API + Mock fallback）
    const result = await certificateApi.createExpenseRecord(expenseData)
    if (result.success) {
      // 刷新费用记录列表
      await loadExpenseRecords()
      ElMessage.success(`已生成待报销记录，金额：¥${expenseData.amount.toFixed(2)}`)
    } else {
      ElMessage.warning(result.message || '费用记录创建未成功')
    }
  } catch (error) {
    console.error('[handleExpenseCreated] 创建费用记录失败:', error)
    // 静默失败，不影响续期主流程
  }
}

/** 加载所有费用记录 */
async function loadExpenseRecords() {
  try {
    // 调用 API 获取全部费用记录（后端无对应端点，自动走 Mock fallback）
    const result = await certificateApi.getAllExpenseRecords({})
    expenseRecords.value = result.records
  } catch (error) {
    console.error('[loadExpenseRecords] 加载费用记录失败:', error)
  }
}

/** 更新费用报销状态 */
async function handleUpdateExpenseStatus(expenseId: string, newStatus: ExpenseStatus) {
  try {
    // 调用 API 更新费用状态（后端无对应端点，自动走 Mock fallback）
    const result = await certificateApi.updateExpenseStatus({
      expenseId,
      expenseStatus: newStatus,
    })
    if (result.success) {
      await loadExpenseRecords()
      ElMessage.success('状态已更新')
    } else {
      ElMessage.error(result.message || '更新失败')
    }
  } catch (error) {
    console.error('[handleUpdateExpenseStatus] 更新失败:', error)
    ElMessage.error('更新失败，请重试')
  }
}

/** 计算续期历史中的费用小计 */
function calculateHistoryTotalCost(records: CertificateRenewalRecord[]): number {
  return records
    .filter(r => r.renewCost && r.renewCost > 0)
    .reduce((sum, r) => sum + (r.renewCost || 0), 0)
}

/** 获取行样式类名（背景色高亮 + 联动高亮 - spec.md F-004） */
function getRowClassName({ row }: { row: Certificate }): string {
  const classes: string[] = []

  if (!row.expiryDate) return ''

  const { level } = calculateExpiry(row.expiryDate)

  // 即将到期(≤30天): 浅黄色渐变
  if (level === 'danger') classes.push('row-expiring-soon')
  // 已过期(<0天): 浅红色渐变
  if (level === 'expired') classes.push('row-expired')

  // 从待办事项联动跳转过来的高亮（闪烁效果）
  if (highlightCertId.value && row.certificateId === highlightCertId.value) {
    classes.push('row-highlight-pulse')
  }

  return classes.join(' ')
}

/** 打开续期对话框（替代原来的ElMessageBox.prompt） */
function handleRenew(row: Certificate) {
  renewingCert.value = row
  renewalDialogVisible.value = true
}

/** 审批续期申请 */
function handleApproveRenewal(row: Certificate) {
  renewalApprovalBusinessId.value = row.certificateId
  renewalApprovalBusinessType.value = 'cert_renewal'
  renewalApprovalNodeName.value = '店长审批'
  renewalApprovalVisible.value = true
}

/** 续期审批通过回调 */
function onRenewalApproved() {
  renewalApprovalVisible.value = false
  ElMessage.success('审批通过')
  loadData()
}

/** 续期审批驳回回调 */
function onRenewalRejected() {
  renewalApprovalVisible.value = false
  ElMessage.info('已驳回')
  loadData()
}

/** 处理续期提交 */
async function handleRenewalSubmit(data: RenewalData) {
  if (!renewingCert.value) return
  loading.value = true
  try {
    // 修复 P0：原代码使用 setTimeout 模拟 API 调用并直接修改本地数据。
    // 现改为调用真实 API（certificateApi.updateCertificate）更新到期日期与证件编号，
    // 续期历史记录应由后端持久化（当前无对应端点，参见 viewRenewalHistory 的 TODO）。
    const updateData = {
      expiryDate: data.newExpiryDate,
      ...(data.newCertNumber ? { certNumber: data.newCertNumber } : {}),
    }
    const result = await certificateApi.updateCertificate(
      renewingCert.value.certificateId,
      updateData,
    )
    if (!result.success) {
      ElMessage.error(result.message || '续期失败，请重试')
      return
    }

    ElMessage.success(`"${renewingCert.value?.certName}"续期成功，新到期日：${data.newExpiryDate}`)

    // 如果是从待办任务跳转过来，提示任务完成
    if (isFromTask.value) {
      ElMessage.success('待办任务已完成！')
      isFromTask.value = false
      highlightCertId.value = ''
    }

    // 刷新数据列表（从后端重新加载，确保数据一致）
    await loadData()
  } catch (error) {
    console.error('[handleRenewalSubmit] 续期失败:', error)
    ElMessage.error('续期失败，请重试')
  } finally {
    loading.value = false
    renewingCert.value = null
  }
}

/** 查看续期历史记录 */
async function viewRenewalHistory(row: Certificate) {
  historyLoading.value = true
  renewalHistoryDialogVisible.value = true

  try {
    // 修复 P0：原代码使用 setTimeout + 硬编码 Mock 续期历史数据（含伪造的 oldExpiryDate/renewCost/operatorName）。
    // 续期历史是证照合规的重要记录，禁止展示假数据。
    // TODO: 续期历史接口待后端实现（certificateApi 暂无 getRenewalHistory 端点）。
    // 后端落地后改为调用真实 API 获取续期记录，当前显示空列表（el-empty 占位）。
    selectedCertHistory.value = []
  } catch (error) {
    console.error('[viewRenewalHistory] 获取历史记录失败:', error)
    ElMessage.error('获取续期历史失败')
    selectedCertHistory.value = []
  } finally {
    historyLoading.value = false
  }
}

/** 滚动到即将到期的证件位置 */
function scrollToExpiringCerts() {
  const tableSection = document.querySelector('.table-section')
  if (tableSection) {
    tableSection.scrollIntoView({ behavior: 'smooth', block: 'start' })
    ElMessage.info('已定位到即将到期的证件，请及时办理续期')
  }
}

/** 生成预警任务到待办事项（spec.md F-004 预警规则） */
function generateAlertTask(cert: Certificate) {
  const { daysLeft, level } = calculateExpiry(cert.expiryDate)

  if (level !== 'danger' && level !== 'warning') {
    ElMessage.info('该证件暂不需要生成预警')
    return
  }

  // 模拟创建待办任务
  const taskInfo = {
    type: 'certificate_expiry' as const,
    title: `${cert.certName}即将到期`,
    description: `${cert.holderName}的${cert.certName}将于${cert.expiryDate}到期，还剩${daysLeft}天`,
    priority: level === 'danger' ? 'high' : 'medium',
    assignee: '门店店长',
    deadline: cert.expiryDate,
    relatedId: cert.certificateId
  }

  ElMessage.success(`已生成预警任务：${taskInfo.title}`)
}

/** 统计卡片数据 - 动态计算（增强版） */
const statsCards = computed(() => {
  const totalCount = allRecords.value.length
  const activeCount = allRecords.value.filter(c => c.status === 'active').length
  const expiringCount = allRecords.value.filter(c => {
    if (!c.expiryDate) return false
    const { level } = calculateExpiry(c.expiryDate)
    return level === 'danger' || level === 'warning'
  }).length
  const expiredCount = allRecords.value.filter(c => {
    if (!c.expiryDate) return false
    const { level } = calculateExpiry(c.expiryDate)
    return level === 'expired'
  }).length

  // 计算即将到期的证件列表（用于提示）
  const expiringSoonList = allRecords.value
    .filter(c => {
      if (!c.expiryDate) return false
      const { level } = calculateExpiry(c.expiryDate)
      return level === 'danger'
    })
    .sort((a, b) => a.daysLeft - b.daysLeft)
    .slice(0, 3)

  return [
    {
      icon: 'Document',
      label: '证件总数',
      value: totalCount,
      colorType: 'primary' as const,
      tip: expiringSoonList.length > 0 ? `注意：${expiringSoonList.length}个证件即将到期` : undefined,
    },
    {
      icon: 'Check',
      label: '有效证件',
      value: activeCount,
      colorType: 'success' as const,
    },
    {
      icon: 'Clock',
      label: '即将到期',
      value: expiringCount,
      colorType: 'warning' as const,
      highlight: expiringCount > 0,
    },
    {
      icon: 'CircleClose',
      label: '已过期',
      value: expiredCount,
      colorType: 'error' as const,
      highlight: expiredCount > 0,
    },
  ]
})

/** 获取最紧急的到期提示 */
const urgentExpiryAlert = computed(() => {
  const urgentCerts = allRecords.value
    .filter(c => {
      if (!c.expiryDate) return false
      const { level } = calculateExpiry(c.expiryDate)
      return level === 'danger'
    })
    .sort((a, b) => a.daysLeft - b.daysLeft)

  if (urgentCerts.length === 0) return null

  const mostUrgent = urgentCerts[0]
  return {
    certName: mostUrgent.certName,
    daysLeft: mostUrgent.daysLeft,
    expiryDate: mostUrgent.expiryDate,
    totalUrgent: urgentCerts.length,
  }
})

/** 从 API 加载数据（真实 API + Mock fallback） */
async function loadData() {
  loading.value = true
  try {
    const result = await certificateApi.getCertificateList({
      page: 1,
      size: 100,
      certType: searchForm.value.certType as Certificate['certType'] || undefined,
      status: searchForm.value.certStatus as Certificate['status'] || undefined,
      holderKeyword: searchForm.value.keyword || undefined,
    })

    // 使用 DataConverter 转换数据
    allRecords.value = result.records
    filteredRecords.value = result.records
  } catch (error) {
    console.error('[StoreCertificate] 加载失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loading.value = true
  setTimeout(() => {
    let result = [...allRecords.value]
    const { certType, certStatus, keyword } = searchForm.value
    if (certType) result = result.filter(r => r.certType === certType)
    if (certStatus) result = result.filter(r => r.status === certStatus)
    if (keyword) result = result.filter(r => r.certName.includes(keyword) || r.holderName.includes(keyword))
    filteredRecords.value = result
    loading.value = false
    ElMessage.success('查询完成')
  }, 300)
}

function handleReset() {
  searchForm.value = { certType: '', certStatus: '', keyword: '' }
  filteredRecords.value = [...allRecords.value]
}

/** 打开新增对话框 */
function handleCreate() {
  dialogMode.value = 'create'
  dialogTitle.value = '新增证件'
  editingCertId.value = null
  resetForm()
  dialogVisible.value = true
}

/** 打开编辑对话框 */
function handleEdit(row: Certificate) {
  dialogMode.value = 'update'
  dialogTitle.value = '编辑证件'
  editingCertId.value = row.certificateId

  // 填充表单数据
  Object.assign(certForm, {
    certName: row.certName,
    certType: row.certType,
    certNumber: row.certNumber || '',
    holderType: row.holderType,
    holderId: row.holderId,
    holderName: row.holderName,
    issueDate: row.issueDate,
    expiryDate: row.expiryDate,
    issuer: row.issuer || '',
    fileUrl: row.fileUrl || '',  // 证件照片
  })

  // 初始化照片列表（如果有已存在的照片）
  if (row.fileUrl) {
    certPhotoList.value = [{
      name: '证件照片',
      url: row.fileUrl,
    }]
  } else {
    certPhotoList.value = []
  }

  dialogVisible.value = true
}

/** 重置表单 */
function resetForm() {
  Object.assign(certForm, {
    certName: '',
    certType: 'business_license',
    certNumber: '',
    holderType: 'company',
    holderId: '',
    holderName: '',
    issueDate: '',
    expiryDate: '',
    issuer: '',
    fileUrl: '',  // 证件照片
  })
  certPhotoList.value = []  // 清空照片列表
  formRef.value?.resetFields()
}

/** 提交表单 */
async function handleSubmit() {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    return // 校验失败
  }

  loading.value = true
  try {
    if (dialogMode.value === 'create') {
      const dto: CreateCertificateDTO = {
        certName: certForm.certName,
        certType: certForm.certType,
        certNumber: certForm.certNumber || undefined,
        holderType: certForm.holderType,
        holderId: certForm.holderId || `HOLDER${Date.now()}`,
        holderName: certForm.holderName,
        issueDate: certForm.issueDate,
        expiryDate: certForm.expiryDate,
        issuer: certForm.issuer || undefined,
      }

      const result = await certificateApi.createCertificate(dto)
      if (result.success) {
        ElMessage.success(result.message || '创建成功')
        dialogVisible.value = false
        await loadData()
      } else {
        ElMessage.error(result.message || '创建失败')
      }
    } else if (editingCertId.value) {
      const updateData = {
        certName: certForm.certName,
        certType: certForm.certType,
        certNumber: certForm.certNumber || undefined,
        holderName: certForm.holderName,
        issueDate: certForm.issueDate,
        expiryDate: certForm.expiryDate,
        issuer: certForm.issuer || undefined,
      }

      const result = await certificateApi.updateCertificate(editingCertId.value, updateData)
      if (result.success) {
        ElMessage.success(result.message || '更新成功')
        dialogVisible.value = false
        await loadData()
      } else {
        ElMessage.error(result.message || '更新失败')
      }
    }
  } catch (error) {
    console.error('[handleSubmit] 操作失败:', error)
    ElMessage.error('操作失败，请重试')
  } finally {
    loading.value = false
  }
}

/** 证件照片上传变化 */
function handleCertPhotoChange(uploadFile: any) {
  const file = uploadFile.raw
  if (!file) return

  // 文件类型验证
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('只支持 JPG/PNG/GIF 格式')
    certPhotoList.value = []
    return
  }

  // 文件大小验证（最大5MB）
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过5MB')
    certPhotoList.value = []
    return
  }

  // 保存到表单（实际项目中这里应该上传到服务器获取URL）
  certForm.fileUrl = URL.createObjectURL(file)
}

/** 移除证件照片 */
function handleCertPhotoRemove() {
  certPhotoList.value = []
  certForm.fileUrl = ''
}

/** 预览证件照片 */
function handleCertPhotoPreview(file: any) {
  previewPhotoUrl.value = file.url || certForm.fileUrl
  showPhotoViewer.value = true
}

/** 停用证件（留底不删除） */
async function handleDeactivate(row: Certificate) {
  try {
    await ElMessageBox.confirm(
      `确认停用证件"${row.certName}"？停用后证件信息将保留作为留底记录。`,
      '停用确认',
      {
        confirmButtonText: '确认停用',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )

    loading.value = true
    const result = await certificateApi.updateCertificate(row.certificateId, { status: 'inactive' })

    if (result.success) {
      ElMessage.success(result.message || '已停用')
      await loadData()
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      console.error('[handleDeactivate] 停用失败:', error)
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

/** 查看证件详情（只读） */
function handleView(row: Certificate) {
  dialogTitle.value = '查看证件'
  dialogMode.value = 'update'
  editingCertId.value = row.certificateId

  Object.assign(certForm, {
    certName: row.certName,
    certType: row.certType,
    certNumber: row.certNumber || '',
    holderType: row.holderType,
    holderId: row.holderId,
    holderName: row.holderName,
    issueDate: row.issueDate,
    expiryDate: row.expiryDate,
    issuer: row.issuer || '',
  })

  dialogVisible.value = true
}

/** 是否为查看模式（只读） */
const isViewMode = computed(() => dialogTitle.value === '查看证件')

/** 导出数据 */
function handleExport() {
  if (filteredRecords.value.length === 0) {
    ElMessage.warning('暂无数据可导出')
    return
  }

  const exportColumns: { prop: keyof Certificate; label: string }[] = [
    { prop: 'certName', label: '证件名称' },
    { prop: 'certType', label: '证件类型' },
    { prop: 'holderName', label: '持有人/单位' },
    { prop: 'issueDate', label: '发证日期' },
    { prop: 'expiryDate', label: '到期日期' },
    { prop: 'daysLeft', label: '剩余天数' },
    { prop: 'status', label: '状态' },
  ]

  exportToCSV(filteredRecords.value, exportColumns, '证件管理')
  ElMessage.success('导出成功')
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="证件管理" description="各类许可证、资质证件的到期管理">
      <template #extra>
        <!-- 角色切换（演示审批流程权限） -->
        <div class="role-switcher">
          <span class="role-label">当前角色：</span>
          <el-radio-group v-model="currentRole" size="small">
            <el-radio-button value="staff">普通员工</el-radio-button>
            <el-radio-button value="store_manager">店长</el-radio-button>
            <el-radio-button value="finance">财务</el-radio-button>
          </el-radio-group>
        </div>
      </template>
    </PageHeader>
    <div class="stats-section">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" :highlight="stat.highlight" />
    </div>

    <!-- 紧急到期提醒横幅 -->
    <div v-if="urgentExpiryAlert" class="expiry-alert-banner">
      <div class="alert-content">
        <el-icon class="alert-icon"><CircleClose /></el-icon>
        <div class="alert-text">
          <strong>紧急提醒：</strong>
          {{ urgentExpiryAlert.certName }}等{{ urgentExpiryAlert.totalUrgent }}个证件即将到期，最紧急的仅剩
          <span class="days-highlight">{{ urgentExpiryAlert.daysLeft }}天</span>
        </div>
      </div>
      <el-button type="warning" size="small" @click="scrollToExpiringCerts">立即处理</el-button>
    </div>
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.certType" placeholder="证件类型" clearable style="width:130px" size="default">
            <el-option label="经营许可" value="business_license" />
            <el-option label="服务许可" value="catering_license" />
            <el-option label="卫生许可" value="hygiene_license" />
            <el-option label="安全许可" value="safety_license" />
            <el-option label="健康证" value="health_certificate" />
            <el-option label="排污许可" value="pollution_license" />
            <el-option label="其他" value="other" />
          </el-select>
          <el-select v-model="searchForm.certStatus" placeholder="证件状态" clearable style="width:120px" size="default">
            <el-option label="有效" value="active" />
            <el-option label="即将到期" value="expiring" />
            <el-option label="已过期" value="expired" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索证件名称或持有人" clearable style="width:220px" size="default" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
          <el-button type="success" size="default" @click="handleCreate">
            <el-icon><Plus /></el-icon>
            新增证件
          </el-button>
          <el-button type="info" size="default" @click="handleExport">导出</el-button>
        </div>
      </div>
    </div>

    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="filteredRecords"
        :loading="loading"
        stripe
        :row-class-name="getRowClassName"
        :actions-width="210"
      >
        <template #certType="{ row }">
          <StatusTag status="info" :label="getCertTypeLabel(row.certType)" size="small" />
        </template>
        <!-- 剩余天数 + 状态指示 -->
        <template #daysLeft="{ row }">
          <div class="days-left-cell">
            <span class="days-number" :class="getDaysLeftClass(row)">{{ row.daysLeft > 0 ? `${row.daysLeft}` : '-' }}</span>
            <span class="days-unit">天</span>
          </div>
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status === 'active' ? 'success' : row.status === 'expiring' ? 'warning' : 'danger'" :label="row.status === 'active' ? '有效' : row.status === 'expiring' ? '即将到期' : '已过期'" size="small" />
        </template>
        <!-- 续期费用状态列（P1阶段 - 审批流程可视化+角色权限） -->
        <template #expenseStatus="{ row }">
          <div class="expense-status-cell">
            <template v-if="getExpenseStatus(row.certificateId)">
              <StatusTag
                :status="getExpenseStatusTagValue(getExpenseStatus(row.certificateId))"
                :label="getExpenseStatusLabel(getExpenseStatus(row.certificateId))"
                size="small"
              />
              <el-button
                v-if="canOperateExpense(getExpenseStatus(row.certificateId))"
                link
                :type="getExpenseActionType(getExpenseStatus(row.certificateId))"
                size="small"
                @click="handleExpenseAction(row)"
              >
                {{ getExpenseActionLabel(getExpenseStatus(row.certificateId)) }}
              </el-button>
              <el-tooltip v-else :content="`需要${getExpenseStatus(row.certificateId) === 'submitted' ? '店长' : getExpenseStatus(row.certificateId) === 'manager_approved' || getExpenseStatus(row.certificateId) === 'finance_approved' ? '财务' : '员工'}角色`" placement="top">
                <el-button link type="info" size="small" disabled>
                  {{ getExpenseActionLabel(getExpenseStatus(row.certificateId)) }}
                </el-button>
              </el-tooltip>
            </template>
            <span v-else class="no-expense-text">-</span>
          </div>
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" @click="handleView(row)">查看</el-button>
          <el-button
            v-if="calculateExpiry(row.expiryDate).level !== 'safe' && row.status !== 'inactive'"
            link
            type="warning"
            size="small"
            @click="handleRenew(row)"
          >
            续期
          </el-button>
          <el-button
            v-if="calculateExpiry(row.expiryDate).level !== 'safe' && row.status !== 'inactive'"
            link
            type="primary"
            size="small"
            @click="handleApproveRenewal(row)"
          >
            审批
          </el-button>
          <el-button
            v-if="row.status !== 'inactive'"
            link
            type="info"
            size="small"
            @click="viewRenewalHistory(row)"
          >
            历史
          </el-button>
          <el-button
            v-if="row.status !== 'inactive'"
            link
            type="danger"
            size="small"
            @click="handleDeactivate(row)"
          >
            停用
          </el-button>
        </template>
      </DataTable>
    </div>
    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="pagination.current" :page-size="pagination.size" :total="filteredRecords.length" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>

    <!-- 新增/编辑/查看对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="560px"
      :close-on-click-modal="false"
      :lock-scroll="false"
      append-to-body
      destroy-on-close
      :align-center="true"
      class="certificate-form-dialog"
    >
      <el-form
        ref="formRef"
        :model="certForm"
        :rules="formRules"
        label-width="100px"
        size="default"
        :disabled="isViewMode"
      >
        <el-form-item label="证件名称" prop="certName">
          <el-input v-model="certForm.certName" placeholder="请输入证件名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="证件类型" prop="certType">
              <el-select
                v-model="certForm.certType"
                placeholder="请选择"
                style="width: 100%"
                :teleported="false"
              >
                <el-option label="营业执照" value="business_license" />
                <el-option label="餐饮服务许可证" value="catering_license" />
                <el-option label="卫生许可证" value="hygiene_license" />
                <el-option label="消防安全合格证" value="safety_license" />
                <el-option label="健康证" value="health_certificate" />
                <el-option label="排污许可证" value="pollution_license" />
                <el-option label="其他" value="other" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件编号" prop="certNumber">
              <el-input v-model="certForm.certNumber" placeholder="选填" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="持有人类型" prop="holderType">
              <el-radio-group v-model="certForm.holderType">
                <el-radio value="company">单位</el-radio>
                <el-radio value="employee">个人</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="持有人/单位" prop="holderName">
              <el-input v-model="certForm.holderName" placeholder="请输入" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="发证日期" prop="issueDate">
              <el-date-picker v-model="certForm.issueDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" :teleported="false" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期日期" prop="expiryDate">
              <el-date-picker v-model="certForm.expiryDate" type="date" placeholder="选择日期" style="width: 100%" value-format="YYYY-MM-DD" :teleported="false" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="发证机关" prop="issuer">
          <el-input v-model="certForm.issuer" placeholder="选填" />
        </el-form-item>
        <el-form-item label="证件照片">
          <div class="cert-photo-upload">
            <el-upload
              action=""
              :auto-upload="false"
              :file-list="certPhotoList"
              :on-change="handleCertPhotoChange"
              :on-remove="handleCertPhotoRemove"
              :on-preview="handleCertPhotoPreview"
              accept="image/jpeg,image/png,image/gif"
              :limit="1"
              list-type="picture-card"
              class="photo-uploader"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="photo-tip">建议拍摄清晰的正本照片，支持JPG/PNG格式</div>
          </div>

          <!-- 图片预览 -->
          <el-image-viewer
            v-if="showPhotoViewer"
            :url-list="[previewPhotoUrl]"
            @close="showPhotoViewer = false"
          />
        </el-form-item>
      </el-form>
      <!-- 查看模式下显示审批记录 -->
      <template v-if="isViewMode && editingCertId">
        <el-divider content-position="left">审批记录</el-divider>
        <ApprovalTimeline :business-id="editingCertId" business-type="cert_renewal" />
      </template>
      <template #footer>
        <template v-if="isViewMode">
          <el-button @click="dialogVisible = false">关闭</el-button>
        </template>
        <template v-else>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="loading" @click="handleSubmit">确定</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 续期对话框 -->
    <CertificateRenewalDialog
      v-model="renewalDialogVisible"
      :certificate="renewingCert"
      @submit="handleRenewalSubmit"
      @expense-created="handleExpenseCreated"
    />

    <!-- 续期历史记录对话框 -->
    <el-dialog
      v-model="renewalHistoryDialogVisible"
      title="续期历史记录"
      width="800px"
      :close-on-click-modal="true"
      destroy-on-close
    >
      <div v-if="historyLoading" class="history-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>

      <template v-else-if="selectedCertHistory.length > 0">
        <!-- 费用小计统计（P0阶段） -->
        <div v-if="calculateHistoryTotalCost(selectedCertHistory) > 0" class="history-cost-summary">
          <span class="summary-label">续期费用合计：</span>
          <span class="summary-amount">¥{{ calculateHistoryTotalCost(selectedCertHistory).toFixed(2) }}</span>
          <span class="summary-count">（共 {{ selectedCertHistory.filter(r => r.renewCost && r.renewCost > 0).length }} 条有费用记录）</span>
        </div>

        <el-table
          :data="selectedCertHistory"
          stripe
          border
          size="small"
          max-height="400px"
        >
          <el-table-column prop="newExpiryDate" label="新到期日期" width="120" />
          <el-table-column prop="oldExpiryDate" label="原到期日期" width="120" />
          <el-table-column prop="newCertNumber" label="新证件编号" min-width="150" show-overflow-tooltip>
            <template #default="{ row }">
              {{ row.newCertNumber || '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="renewCost" label="费用（元）" width="110" align="right">
            <template #default="{ row }">
              <span v-if="row.renewCost && row.renewCost > 0" class="cost-value">¥{{ row.renewCost.toFixed(2) }}</span>
              <span v-else class="no-cost-text">-</span>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
          <el-table-column prop="operatorName" label="操作人" width="100" />
          <el-table-column prop="renewTime" label="续期时间" width="160">
            <template #default="{ row }">
              {{ new Date(row.renewTime).toLocaleString('zh-CN') }}
            </template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="暂无续期记录" />

      <template #footer>
        <el-button @click="renewalHistoryDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- ========== 报销申请对话框（含审批流程可视化） ========== -->
    <el-dialog
      v-model="reimburseDialogVisible"
      title="续期费用报销申请"
      width="620px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- 审批流程步骤条 -->
      <div class="workflow-steps">
        <el-steps :active="0" align-center size="small" finish-status="success">
          <el-step v-for="step in EXPENSE_WORKFLOW_STEPS" :key="step.key" :title="step.label" :description="step.description" />
        </el-steps>
      </div>

      <el-form :model="reimburseForm" :rules="reimburseRules" ref="reimburseFormRef" label-width="100px" size="default">
        <el-form-item label="证件名称">
          <span>{{ reimburseTargetCert?.certName }}</span>
        </el-form-item>
        <el-form-item label="续期费用">
          <span class="expense-amount">¥{{ reimburseTargetCert ? getLatestExpenseAmount(reimburseTargetCert.certificateId) : '0.00' }}</span>
        </el-form-item>
        <el-form-item label="报销单号" prop="reimburseNo">
          <el-input v-model="reimburseForm.reimburseNo" placeholder="系统自动生成" disabled />
        </el-form-item>
        <el-form-item label="费用类型" prop="expenseType">
          <el-select v-model="reimburseForm.expenseType" placeholder="请选择" style="width:100%" :teleported="false">
            <el-option label="证件办理费" value="certificate_fee" />
            <el-option label="体检费" value="health_check" />
            <el-option label="培训费" value="training" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="报销说明" prop="remark">
          <el-input v-model="reimburseForm.remark" type="textarea" :rows="3" placeholder="请输入报销说明（如：2026年度健康证续期）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reimburseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReimburse">提交报销</el-button>
      </template>
    </el-dialog>

    <!-- ========== 审批流程对话框（店长审批/财务审核） ========== -->
    <el-dialog
      v-model="approvalDialogVisible"
      :title="approvalAction === 'approve' ? '店长审批' : '财务审核'"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- 审批流程步骤条 -->
      <div class="workflow-steps">
        <el-steps
          :active="approvalAction === 'approve' ? 1 : 2"
          align-center
          size="small"
          finish-status="success"
        >
          <el-step v-for="step in EXPENSE_WORKFLOW_STEPS" :key="step.key" :title="step.label" :description="step.description" />
        </el-steps>
      </div>

      <div class="approval-info">
        <div class="approval-info-row">
          <span class="approval-label">证件名称：</span>
          <span>{{ approvalTargetCert?.certName }}</span>
        </div>
        <div class="approval-info-row">
          <span class="approval-label">报销金额：</span>
          <span class="expense-amount">¥{{ approvalTargetCert ? getLatestExpenseAmount(approvalTargetCert.certificateId) : '0.00' }}</span>
        </div>
        <div class="approval-info-row">
          <span class="approval-label">当前环节：</span>
          <StatusTag
            :status="approvalAction === 'approve' ? 'info' : 'primary'"
            :label="approvalAction === 'approve' ? '待店长审批' : '待财务审核'"
            size="small"
          />
        </div>
        <div class="approval-info-row">
          <span class="approval-label">审批角色：</span>
          <StatusTag
            :status="currentRole === (approvalAction === 'approve' ? 'store_manager' : 'finance') ? 'success' : 'danger'"
            :label="`${roleLabels[currentRole]}（${roleNames[currentRole]}）`"
            size="small"
          />
        </div>
      </div>

      <!-- 审批历史记录 -->
      <div v-if="approvalTargetCert && getExpenseAuditLogs(expenseRecords.find(e => e.certificateId === approvalTargetCert?.certificateId)?.expenseId || '').length > 0" class="audit-trail-section">
        <div class="audit-trail-title">审批记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="log in getExpenseAuditLogs(expenseRecords.find(e => e.certificateId === approvalTargetCert?.certificateId)?.expenseId || '')"
            :key="log.logId"
            :type="log.action === 'reject' ? 'danger' : 'primary'"
            :timestamp="new Date(log.operateTime).toLocaleString('zh-CN')"
            placement="top"
          >
            <div class="audit-item">
              <span class="audit-action">{{ getAuditActionLabel(log.action) }}</span>
              <span class="audit-operator">{{ log.operatorName }}（{{ getAuditRoleLabel(log.operatorRole) }}）</span>
              <span v-if="log.remark" class="audit-remark">：{{ log.remark }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 流程通知记录 -->
      <div v-if="approvalTargetCert && getExpenseNotifications(expenseRecords.find(e => e.certificateId === approvalTargetCert?.certificateId)?.expenseId || '').length > 0" class="notification-section">
        <div class="notification-title">流程通知</div>
        <div v-for="ntf in getExpenseNotifications(expenseRecords.find(e => e.certificateId === approvalTargetCert?.certificateId)?.expenseId || '')" :key="ntf.notifyId" class="notification-item">
          <el-icon :size="14" class="notification-icon"><Bell /></el-icon>
          <span class="notification-target">{{ ntf.targetName }}（{{ roleLabels[ntf.targetRole] }}）</span>
          <span class="notification-msg">{{ ntf.message }}</span>
        </div>
      </div>

      <el-form label-width="100px" size="default">
        <el-form-item label="审批意见">
          <el-input v-model="approvalRemark" type="textarea" :rows="3" placeholder="请输入审批意见（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rejectApproval">驳回</el-button>
        <el-button type="primary" @click="executeApproval">
          {{ approvalAction === 'approve' ? '审批通过' : '审核通过' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- ========== 核销确认对话框（含审批流程可视化+审计追踪） ========== -->
    <el-dialog
      v-model="writeOffDialogVisible"
      title="续期费用核销确认"
      width="680px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <!-- 审批流程步骤条 -->
      <div class="workflow-steps">
        <el-steps :active="3" align-center size="small" finish-status="success">
          <el-step v-for="step in EXPENSE_WORKFLOW_STEPS" :key="step.key" :title="step.label" :description="step.description" />
        </el-steps>
      </div>

      <!-- 审批历史记录 -->
      <div v-if="writeOffTargetCert && getExpenseAuditLogs(expenseRecords.find(e => e.certificateId === writeOffTargetCert?.certificateId)?.expenseId || '').length > 0" class="audit-trail-section">
        <div class="audit-trail-title">审批记录</div>
        <el-timeline>
          <el-timeline-item
            v-for="log in getExpenseAuditLogs(expenseRecords.find(e => e.certificateId === writeOffTargetCert?.certificateId)?.expenseId || '')"
            :key="log.logId"
            :type="log.action === 'reject' ? 'danger' : 'primary'"
            :timestamp="new Date(log.operateTime).toLocaleString('zh-CN')"
            placement="top"
          >
            <div class="audit-item">
              <span class="audit-action">{{ getAuditActionLabel(log.action) }}</span>
              <span class="audit-operator">{{ log.operatorName }}（{{ getAuditRoleLabel(log.operatorRole) }}）</span>
              <span v-if="log.remark" class="audit-remark">：{{ log.remark }}</span>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 流程通知记录 -->
      <div v-if="writeOffTargetCert && getExpenseNotifications(expenseRecords.find(e => e.certificateId === writeOffTargetCert?.certificateId)?.expenseId || '').length > 0" class="notification-section">
        <div class="notification-title">流程通知</div>
        <div v-for="ntf in getExpenseNotifications(expenseRecords.find(e => e.certificateId === writeOffTargetCert?.certificateId)?.expenseId || '')" :key="ntf.notifyId" class="notification-item">
          <el-icon :size="14" class="notification-icon"><Bell /></el-icon>
          <span class="notification-target">{{ ntf.targetName }}（{{ roleLabels[ntf.targetRole] }}）</span>
          <span class="notification-msg">{{ ntf.message }}</span>
        </div>
      </div>

      <el-form :model="writeOffForm" :rules="writeOffRules" ref="writeOffFormRef" label-width="100px" size="default">
        <el-form-item label="证件名称">
          <span>{{ writeOffTargetCert?.certName }}</span>
        </el-form-item>
        <el-form-item label="报销金额">
          <span class="expense-amount">¥{{ writeOffTargetCert ? getLatestExpenseAmount(writeOffTargetCert.certificateId) : '0.00' }}</span>
        </el-form-item>
        <el-form-item label="核销金额" prop="writeOffAmount">
          <el-input-number v-model="writeOffForm.writeOffAmount" :min="0" :precision="2" :step="10" style="width:100%" />
        </el-form-item>
        <el-form-item label="凭证号" prop="voucherNo">
          <el-input v-model="writeOffForm.voucherNo" placeholder="请输入财务凭证号" maxlength="30" />
        </el-form-item>
        <el-form-item label="核销备注" prop="remark">
          <el-input v-model="writeOffForm.remark" type="textarea" :rows="2" placeholder="核销备注（可选）" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="writeOffDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitWriteOff">确认核销</el-button>
      </template>
    </el-dialog>

    <!-- 续期审批对话框（统一审批组件） -->
    <ApprovalDialog
      v-model:visible="renewalApprovalVisible"
      :business-id="renewalApprovalBusinessId"
      :business-type="renewalApprovalBusinessType"
      :current-node-name="renewalApprovalNodeName"
      @approved="onRenewalApproved"
      @rejected="onRenewalRejected"
    />
  </div>
</template>

<style scoped>
.stats-section { grid-template-columns: repeat(4, 1fr); }

/* ========== 到期状态徽章样式 ========== */

/* 剩余天数单元格样式 */
.days-left-cell {
  display: inline-flex;
  align-items: baseline;
  gap: 2px;
}

.days-number {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  line-height: 1.2;
}

.days-unit {
  font-size: var(--fts-font-size-xs);
  color: var(--el-text-color-secondary, #909399);
}

/* safe: 绿色 - 有效 */
.days-safe {
  color: var(--fts-success, #67c23a);
}

/* warning: 黄色 - 即将到期 */
.days-warning {
  color: var(--fts-warning-dark, #b88230);
}

/* danger: 红色 - 紧急 */
.days-danger {
  color: var(--fts-error, #f56c6c);
  font-weight: 700;
}

/* expired: 深红 + 删除线 - 已过期 */
.days-expired {
  color: var(--fts-error, #f56c6c);
  text-decoration: line-through;
}

.days-normal {
  color: var(--el-text-color-primary, #303133);
}

.expiry-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-left: var(--fts-space-2);
  padding: 2px var(--fts-space-2);
  border-radius: var(--fts-border-radius-sm);
  font-size: var(--fts-font-size-xs);
  font-weight: 500;
  line-height: 1.4;
}

/* safe: 绿色背景 - 有效 */
.expiry-badge-safe {
  background-color: var(--fts-success-light, rgba(103, 194, 58, 0.1));
  color: var(--fts-success);
  border: 1px solid var(--fts-success-light, rgba(103, 194, 58, 0.2));
}

/* warning: 黄色渐变 - 还剩25-60天 */
.expiry-badge-warning {
  background: linear-gradient(135deg, rgba(230, 162, 60, 0.15) 0%, rgba(255, 192, 0, 0.1) 100%);
  color: var(--fts-warning-dark, #b88230);
  border: 1px solid rgba(230, 162, 60, 0.3);
}

/* danger: 红色渐变 - 还剩5-30天 */
.expiry-badge-danger {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.15) 0%, rgba(255, 77, 79, 0.1) 100%);
  color: var(--fts-error);
  border: 1px solid rgba(245, 108, 108, 0.3);
  animation: pulse-danger 2s ease-in-out infinite;
}

/* expired: 红色背景+脉冲动画 - 已过期 */
.expiry-badge-expired {
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.2) 0%, rgba(220, 53, 69, 0.15) 100%);
  color: var(--fts-error);
  border: 1px solid rgba(245, 108, 108, 0.4);
  animation: pulse-danger 1.5s ease-in-out infinite;
}

/* 过期脉冲动画 */
@keyframes pulse-danger {
  0%, 100% {
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.3);
  }
  50% {
    opacity: 0.85;
    box-shadow: 0 0 8px 2px rgba(245, 108, 108, 0.2);
  }
}

/* 续期按钮 */
.renewal-btn {
  margin-left: var(--fts-space-1);
  font-size: var(--fts-font-size-xs);
  padding: 0 4px;
}

/* ========== 行级背景色高亮 ========== */

/* 即将到期(≤30天): 浅黄色渐变 */
:deep(.row-expiring-soon) {
  background: linear-gradient(90deg, rgba(255, 243, 205, 0.4) 0%, rgba(255, 249, 230, 0.2) 100%) !important;
}

/* 已过期(<0天): 浅红色渐变 */
:deep(.row-expired) {
  background: linear-gradient(90deg, rgba(253, 223, 222, 0.4) 0%, rgba(255, 240, 240, 0.2) 100%) !important;
}

/* 联动高亮闪烁效果（spec.md F-004） */
:deep(.row-highlight-pulse) {
  animation: highlight-pulse 1.5s ease-in-out 3;
  border-left: 4px solid var(--fts-warning);
}

@keyframes highlight-pulse {
  0%, 100% {
    background-color: rgba(230, 162, 60, 0.05);
    box-shadow: 0 0 0 0 rgba(230, 162, 60, 0.4);
  }
  50% {
    background-color: rgba(230, 162, 60, 0.15);
    box-shadow: 0 0 12px 4px rgba(230, 162, 60, 0.3);
  }
}

/* 续期历史记录样式 */
.history-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-8);
  color: var(--el-text-color-secondary, #909399);
  font-size: var(--fts-font-size-sm);
}

/* 紧急到期提醒横幅 */
.expiry-alert-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: linear-gradient(135deg, rgba(245, 108, 108, 0.08) 0%, rgba(255, 77, 79, 0.05) 100%);
  border: 1px solid rgba(245, 108, 108, 0.25);
  border-left: 4px solid var(--fts-error);
  border-radius: var(--fts-border-radius-base);
  animation: alert-pulse 2s ease-in-out infinite;
}

.alert-content {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex: 1;
}

.alert-icon {
  font-size: 20px;
  color: var(--fts-error);
  animation: icon-shake 1s ease-in-out infinite;
}

.alert-text {
  font-size: var(--fts-font-size-sm);
  color: var(--el-text-color-primary, #303133);
  line-height: 1.5;
}

.days-highlight {
  font-weight: 700;
  color: var(--fts-error);
  font-size: var(--fts-font-size-base);
  padding: 0 4px;
}

@keyframes alert-pulse {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.15);
  }
  50% {
    box-shadow: 0 0 12px 4px rgba(245, 108, 108, 0.1);
  }
}

@keyframes icon-shake {
  0%, 100% { transform: rotate(0deg); }
  25% { transform: rotate(-5deg); }
  75% { transform: rotate(5deg); }
}

/* ========== 证件照片上传样式 ========== */
.cert-photo-upload {
  width: 100%;
}

.photo-uploader {
  width: fit-content;
}

.photo-tip {
  margin-top: var(--fts-space-2);
  font-size: var(--fts-font-size-xs);
  color: var(--el-text-color-placeholder, #c0c4cc);
  line-height: 1.4;
}

/* ========== P0/P1阶段 - 续期费用状态样式 ========== */

/* 费用状态单元格容器 */
.expense-status-cell {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
}

/* 无费用记录时的占位文本 */
.no-expense-text {
  color: var(--el-text-color-placeholder, #c0c4cc);
  font-size: var(--fts-font-size-sm);
}

/* 报销/核销对话框中的金额高亮 */
.expense-amount {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-primary);
}

/* 审批流程步骤条 */
.workflow-steps {
  margin-bottom: var(--fts-space-5);
  padding: var(--fts-space-4) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-border-radius-md);
  border: 1px solid var(--fts-border-color-light);
}

/* 审批信息区 */
.approval-info {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
  background: var(--fts-bg-primary-light);
  border-radius: var(--fts-border-radius-md);
  border: 1px solid var(--fts-border-color-light);
}

.approval-info-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-sm);
}

.approval-label {
  color: var(--fts-text-secondary);
  min-width: 80px;
  flex-shrink: 0;
}

/* 续期历史 - 费用小计统计 */
.history-cost-summary {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-4);
  margin-bottom: var(--fts-space-3);
  background: var(--el-fill-color-lighter, #f5f7fa);
  border-radius: var(--fts-border-radius-base);
  border-left: 3px solid var(--fts-warning, #e6a23c);
}

.summary-label {
  font-size: var(--fts-font-size-sm);
  color: var(--el-text-color-secondary, #909399);
}

.summary-amount {
  font-size: var(--fts-font-size-base);
  font-weight: 700;
  color: var(--fts-warning, #e6a23c);
}

.summary-count {
  font-size: var(--fts-font-size-xs);
  color: var(--el-text-color-placeholder, #c0c4cc);
}

/* 费用金额显示 */
.cost-value {
  color: var(--fts-warning-dark, #b88230);
  font-weight: 600;
}

.no-cost-text {
  color: var(--el-text-color-placeholder, #c0c4cc);
}

/* ========== 角色切换器样式 ========== */
.role-switcher {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  .role-label {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    white-space: nowrap;
  }
}

/* ========== 审计追踪区域样式 ========== */
.audit-trail-section {
  margin-top: var(--fts-space-4);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-border-radius-md);
  border: 1px solid var(--fts-border-color-light);
}

.audit-trail-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-color-light);
}

.audit-item {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
  font-size: var(--fts-font-size-sm);
  line-height: 1.6;
}

.audit-action {
  font-weight: 600;
  color: var(--fts-primary);
}

.audit-operator {
  color: var(--fts-text-secondary);
}

.audit-remark {
  color: var(--fts-text-tertiary);
  font-size: var(--fts-font-size-xs);
}

/* ========== 流程通知区域样式 ========== */
.notification-section {
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-border-radius-md);
  border: 1px solid var(--fts-border-color-light);
}

.notification-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-2);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-color-light);
}

.notification-item {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);
  padding: var(--fts-space-1) 0;
  font-size: var(--fts-font-size-sm);
  line-height: 1.5;
}

.notification-icon {
  color: var(--fts-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.notification-target {
  font-weight: 600;
  color: var(--fts-primary);
  white-space: nowrap;
}

.notification-msg {
  color: var(--fts-text-secondary);
}
</style>
