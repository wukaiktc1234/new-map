// 审批模块类型定义

/** 审批状态枚举 */
export type ApprovalStatus = 'pending' | 'approved' | 'rejected' | 'cancelled' | 'processing'

/** 审批类型枚举 */
export type ApprovalType = 'leave' | 'overtime' | 'swap' | 'travel' | 'reimbursement' | 'requisition'

/** 审批类型中文标签映射 */
export const ApprovalTypeLabels: Record<ApprovalType, string> = {
  leave: '请假申请',
  overtime: '加班申请',
  swap: '调班换班',
  travel: '出差申请',
  reimbursement: '费用报销',
  requisition: '物品领用',
}

/** 审批类型图标映射（Element Plus icon组件名） */
export const ApprovalTypeIcons: Record<ApprovalType, string> = {
  leave: 'Tickets',
  overtime: 'AlarmClock',
  swap: 'RefreshRight',
  travel: 'Location',
  reimbursement: 'Wallet',
  requisition: 'ShoppingBag',
}

/** 审批类型颜色映射（使用项目CSS变量值） */
export const ApprovalTypeColors: Record<ApprovalType, string> = {
  leave: 'var(--fts-warning)',        // 橙色 - 请假
  overtime: 'var(--fts-primary)',     // 蓝色 - 加班
  swap: '#7B61FF',                    // 紫色 - 换班（暂无对应CSS变量）
  travel: 'var(--fts-success)',       // 绿色 - 出差
  reimbursement: 'var(--fts-error)',  // 红色 - 报销
  requisition: 'var(--fts-info)',     // 灰色 - 领用
}

/** 审批状态中文标签映射 */
export const ApprovalStatusLabels: Record<ApprovalStatus, string> = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已驳回',
  cancelled: '已撤回',
  processing: '审批中',
}

/** 审批列表项 */
export interface ApprovalItem {
  id: string
  type: ApprovalType
  title: string
  applicantName: string
  applicantAvatar?: string
  summary: string
  status: ApprovalStatus
  createdAt: string
  urgency: UrgencyLevel
}

/** 审批详情（继承列表项） */
export interface ApprovalDetail extends ApprovalItem {
  approvalNo: string
  /** 发起人层级（1-5，对应 UserLevel 枚举） */
  applicantLevel: number
  formFields: FormField[]
  attachments: Attachment[]
  flowNodes: FlowNode[]
  comments: Comment[]
  /** 审批参考信息（按审批类型差异化展示业务上下文数据） */
  contextData?: ContextData
}

// ========== 审批参考信息（ContextData） ==========

/** 关联任务/指令（跨模块联动：出差任务→审批→报销） */
export interface RelatedTask {
  id: string
  title: string
  type: 'travel_order' | 'project_task' | 'client_meeting' | 'training' | 'inspection'
  status: 'active' | 'completed' | 'cancelled'
  source: string
  issueDate: string
  issuerName: string
  link?: string
}

/** 请假上下文信息 */
export interface LeaveContext {
  annualTotal: number
  annualUsed: number
  annualRemaining: number
  sickBalance?: number
  personalBalance?: number
  compensatoryBalance?: number
  yearLeaveCount: number
  monthLeaveDays?: number
  consecutiveDaysWarning?: boolean
  lastLeaveDate?: string
  leaveTrend?: Array<{ month: string; days: number; type: string }>
}

/** 报销费用明细项 */
export interface ExpenseLineItem {
  date: string
  category: string
  description: string
  amount: string
  receiptNo: string
}

/** 报销上下文信息 */
export interface ExpenseContext {
  items: ExpenseLineItem[]
  monthlyTotal: string
  monthlyBudget: string
  monthlyUsagePercent: number
  yearTotal: string
  yearBudget: string
  receiptCount: number
  relatedTravel?: RelatedTask
  policyNotes?: string
}

/** 加班上下文信息 */
export interface OvertimeContext {
  monthHours: number
  monthLimit: number
  monthUsagePercent: number
  quarterHours: number
  quarterLimit: number
  deptAvgHours: number
  compensateType: '调休' | '加班费' | '混合'
  compensatoryBalance?: number
  recentRecords?: Array<{ date: string; hours: number; type: string }>
}

/** 换班上下文信息 */
export interface SwapContext {
  partnerConfirmed: boolean
  partnerName: string
  confirmTime?: string
  coveragePlan: string
  recentSwapCount: number
  swapLimit: number
  originalShiftDetail?: { date: string; time: string; position: string }
  targetShiftDetail?: { date: string; time: string; position: string }
}

/** 出差上下文信息 */
export interface TravelContext {
  policyLimit: string
  estimatedActual: string
  yearTripCount: number
  advancePayment: string
  relatedTask?: RelatedTask
  hotelStandard: string
  transportAllowance: string
  dailyAllowance: string
  similarTrips?: Array<{ destination: string; dates: string; amount: string }>
}

/** 领用上下文信息 */
export interface RequisitionContext {
  stockQuantity: number
  stockUnit: string
  monthlyAvg: number
  lastRequisitionDate: string
  lastRequisitionQuantity: number
  departmentBudget: string
  departmentUsed: string
  supplierInfo?: string
  leadTimeDays: number
}

/** 审批参考信息联合类型（按审批类型区分） */
export type ContextData =
  | { type: 'leave'; data: LeaveContext; riskWarning?: RiskWarning }
  | { type: 'overtime'; data: OvertimeContext; riskWarning?: RiskWarning }
  | { type: 'swap'; data: SwapContext; riskWarning?: RiskWarning }
  | { type: 'travel'; data: TravelContext; riskWarning?: RiskWarning }
  | { type: 'reimbursement'; data: ExpenseContext; riskWarning?: RiskWarning }
  | { type: 'requisition'; data: RequisitionContext; riskWarning?: RiskWarning }

// ========== HR风控预警系统 ==========

/** 风险等级 */
export type RiskLevel = 'info' | 'warning' | 'danger' | 'prohibited'

/** 风险类别 */
export type RiskCategory = 'health' | 'compliance' | 'policy' | 'budget' | 'schedule'

/** HR风控预警信息 */
export interface RiskWarning {
  /** 风险等级：info(建议注意) / warning(需谨慎) / danger(高风险) / prohibited(HR禁止) */
  level: RiskLevel
  /** 风险类别 */
  category: RiskCategory
  /** 预警标题（一行摘要） */
  title: string
  /** 详细说明（多行，支持换行） */
  description: string
  /** 数据依据（触发此警告的具体数值） */
  evidence: string
  /** HR建议操作 */
  suggestion: string
  /** 预警来源 */
  source: 'hr_system_auto' | 'hr_manual_review' | 'policy_engine'
  /** 生成时间 */
  generatedAt: string
  /** HR审核人（手动审核时） */
  reviewerName?: string
  /** 关联法规/制度条款 */
  regulationRef?: string
}

/** 风险等级中文标签 */
export const RiskLevelLabels: Record<RiskLevel, string> = {
  info: '建议',
  warning: '注意',
  danger: '警告',
  prohibited: '禁止',
}

/** 风险等级对应的视觉样式映射 */
export const RiskLevelConfig: Record<RiskLevel, { color: string; bg: string; border: string; icon: string }> = {
  info: {
    color: 'var(--fts-text-secondary)',
    bg: 'rgba(var(--fts-info-rgb), 0.04)',
    border: 'rgba(var(--fts-info-rgb), 0.12)',
    icon: 'InfoFilled',
  },
  warning: {
    color: 'var(--fts-warning)',
    bg: 'rgba(var(--fts-warning-rgb), 0.06)',
    border: 'rgba(var(--fts-warning-rgb), 0.18)',
    icon: 'WarningFilled',
  },
  danger: {
    color: 'var(--fts-error)',
    bg: 'rgba(var(--fts-error-rgb), 0.06)',
    border: 'rgba(var(--fts-error-rgb), 0.18)',
    icon: 'CircleCloseFilled',
  },
  prohibited: {
    color: '#DC2626',
    bg: 'rgba(220, 38, 38, 0.08)',
    border: 'rgba(220, 38, 38, 0.25)',
    icon: 'Lock',
  },
}

/** 表单字段 */
export interface FormField {
  label: string
  value: string | number
  type?: 'text' | 'date' | 'number' | 'select' | 'textarea' | 'table'
  options?: string[]
  tableData?: TableRow[]
}

/** 明细表格行（报销/采购用） */
export interface TableRow {
  id: string
  cells: Record<string, string | number>
}

/** 附件信息 */
export interface Attachment {
  name: string
  size: string
  url: string
  type: 'pdf' | 'image' | 'doc' | 'other'
}

/** 流程节点 */
export interface FlowNode {
  id: string
  role: string
  userName: string
  avatar?: string
  status: 'completed' | 'current' | 'pending' | 'skipped'
  action?: string
  time?: string
}

/** 评论/审批意见 */
export interface Comment {
  id: string
  userId: string
  userName: string
  avatar?: string
  content: string
  createdAt: string
}

/** 发起审批表单数据 */
export interface ApprovalFormData {
  type: ApprovalType
  /** 紧急程度 */
  urgency?: UrgencyLevel
  fields: Record<string, unknown>
  attachments?: File[]
}

/** 审批Tab类型 */
export type ApprovalTab = 'pending' | 'initiated' | 'cc' | 'approved' | 'rejected' | 'completed' | 'role'

/** 审批Tab中文标签映射 */
export const ApprovalTabLabels: Record<ApprovalTab, string> = {
  pending: '需要我确认',
  initiated: '我发起的',
  cc: '抄送我的',
  approved: '已通过',
  rejected: '已驳回',
  completed: '已完成',
  role: '角色审批',
}

// ========== 紧急程度 ==========

/** 紧急程度枚举 */
export type UrgencyLevel = 'normal' | 'urgent'

export const UrgencyLevelLabels: Record<UrgencyLevel, string> = {
  normal: '普通',
  urgent: '紧急',
}

// ========== 各审批类型表单数据结构 ==========

/** 请假表单数据 */
export interface LeaveFormData {
  leaveType: string
  startDate: string
  endDate: string
  duration: number
  reason: string
}

/** 加班表单数据 */
export interface OvertimeFormData {
  overtimeDate: string
  startTime: string
  endTime: string
  duration: number
  reason: string
  isCompensatoryLeave: boolean
}

/** 调班表单数据 */
export interface SwapFormData {
  swapType: 'exchange' | 'adjust'
  originalShift: string
  targetShift: string
  exchangeWith: string
  reason: string
}

/** 出差表单数据 */
export interface TravelFormData {
  destination: string
  startDate: string
  endDate: string
  transportMethod: string
  budgetAmount: number
  reason: string
}

/** 费用明细项 */
export interface ExpenseItem {
  id: string
  expenseType: string
  date: string
  amount: number
  remark: string
}

/** 报销表单数据 */
export interface ReimbursementFormData {
  reimbursementType: string
  expenses: ExpenseItem[]
  totalAmount: number
  paymentMethod: string
  bankAccount: string
  description: string
}

/** 领用表单数据 */
export interface RequisitionFormData {
  category: string
  itemName: string
  quantity: number
  purpose: string
  expectedDate: string
}

/** 所有表单数据的联合类型（用于提交） */
export type AnyApprovalFormData =
  | LeaveFormData
  | OvertimeFormData
  | SwapFormData
  | TravelFormData
  | ReimbursementFormData
  | RequisitionFormData

// ========== 表单选项定义 ==========

/** 请假类型选项 */
export const LeaveTypeOptions = [
  { label: '年假', value: 'annual' },
  { label: '事假', value: 'personal' },
  { label: '病假', value: 'sick' },
  { label: '调休', value: 'compensatory' },
  { label: '其他', value: 'other' },
] as const

/** 交通方式选项 */
export const TransportOptions = [
  { label: '飞机', value: 'plane' },
  { label: '高铁', value: 'train' },
  { label: '汽车', value: 'car' },
  { label: '其他', value: 'other' },
] as const

/** 费用类型选项 */
export const ExpenseTypeOptions = [
  { label: '交通费', value: 'transport' },
  { label: '住宿费', value: 'hotel' },
  { label: '餐饮费', value: 'meal' },
  { label: '办公用品', value: 'office' },
  { label: '其他', value: 'other' },
] as const

/** 收款方式选项 */
export const PaymentOptions = [
  { label: '银行卡', value: 'bank' },
  { label: '支付宝', value: 'alipay' },
  { label: '微信', value: 'wechat' },
] as const

/** 物品类别选项 */
export const CategoryOptions = [
  { label: '厨房用品', value: 'kitchen' },
  { label: '清洁用品', value: 'cleaning' },
  { label: '办公耗材', value: 'office_supplies' },
  { label: '制服工装', value: 'uniform' },
  { label: '其他', value: 'other' },
] as const

/** 紧急程度选项 */
export const UrgencyOptions = [
  { label: '普通', value: 'normal' as const },
  { label: '紧急', value: 'urgent' as const },
]
