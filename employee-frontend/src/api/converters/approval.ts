/**
 * 审批数据转换器
 * 负责审批相关数据在后端与前端之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 *
 * 转换规则：
 * - 审批状态: 前端语义化字符串 <-> 后端数字编码
 * - 紧急程度: 前端语义化字符串 <-> 后端数字编码
 * - 金额: 后端分(整数) <-> 前端元(浮点字符串)
 * - 日期: ISO字符串 <-> 展示格式
 */

import type {
  ApprovalItem,
  ApprovalDetail,
  ApprovalStatus,
  ApprovalType,
  ApprovalTab,
  FlowNode,
  Comment,
  FormField,
  ContextData,
  LeaveContext,
  OvertimeContext,
  SwapContext,
  TravelContext,
  ExpenseContext,
  RequisitionContext,
  RiskWarning,
  Attachment,
  RelatedTask,
} from '@/types/approval'
import { ApprovalTypeLabels, ApprovalStatusLabels, UrgencyLevelLabels } from '@/types/approval'

// ============================================================
// 后端数据类型定义（模拟真实后端 DTO 结构）
// ============================================================

/** 后端审批状态码 */
type BackendStatus = 0 | 1 | 2 | 3 | 4

/** 后端审批类型码 */
type BackendType = 'leave' | 'overtime' | 'swap' | 'travel' | 'reimbursement' | 'requisition'

/** 后端原始审批记录 */
export interface RawApprovalItem {
  id: string
  approval_no: string
  type: BackendType
  title: string
  applicant_id: string
  applicant_name: string
  summary: string
  status: BackendStatus
  urgency: 0 | 1
  created_at: string
  updated_at: string
}

/** 后端原始审批详情 */
export interface RawApprovalDetail extends RawApprovalItem {
  /** 发起人层级 */
  applicant_level: number
  /** 表单字段 */
  form_fields: Array<{
    field_key: string
    field_label: string
    field_value: string | number
    field_type?: string
  }>
  /** 附件列表 */
  attachments: Array<{
    file_id: string
    file_name: string
    file_size: number
    file_url: string
    file_type: string
  }>
  /** 流程节点 */
  flow_nodes: Array<{
    node_id: string
    node_role: string
    user_id: string
    user_name: string
    node_status: 0 | 1 | 2 | 3
    action_text?: string
    action_time?: string
  }>
  /** 评论列表 */
  comments: Array<{
    comment_id: string
    user_id: string
    user_name: string
    content: string
    created_at: string
  }>
  /** 上下文数据（按类型差异化） */
  context_data?: Record<string, unknown>
  /** 风控预警 */
  risk_warning?: Record<string, unknown> | null
}

// ============================================================
// 映射常量
// ============================================================

/** 后端状态码 -> 前端状态字符串 */
const STATUS_MAP: Record<BackendStatus, ApprovalStatus> = {
  0: 'pending',
  1: 'approved',
  2: 'rejected',
  3: 'cancelled',
  4: 'processing',
}

/** 前端状态字符串 -> 后端状态码 */
const STATUS_REVERSE_MAP: Record<ApprovalStatus, BackendStatus> = {
  pending: 0,
  approved: 1,
  rejected: 2,
  cancelled: 3,
  processing: 4,
}

/** 后端紧急度 -> 前端紧急度 */
const URGENCY_MAP: Record<0 | 1, 'normal' | 'urgent'> = {
  0: 'normal',
  1: 'urgent',
}

/** 前端紧急度 -> 后端紧急度 */
const URGENCY_REVERSE_MAP: Record<'normal' | 'urgent', 0 | 1> = {
  normal: 0,
  urgent: 1,
}

// ============================================================
// 工具函数
// ============================================================

/**
 * ISO 时间字符串截取为展示格式 YYYY-MM-DD HH:mm
 */
function formatDateTime(isoStr: string): string {
  if (!isoStr) return ''
  // 处理 "2026-06-04T10:30:00" 格式
  return isoStr.replace('T', ' ').slice(0, 16)
}

/**
 * ISO 时间字符串截取为日期格式 YYYY-MM-DD
 */
function formatDate(isoStr: string): string {
  if (!isoStr) return ''
  return isoStr.slice(0, 10)
}

/**
 * 后端金额（分）转前端金额（元）
 * @param fen - 后端整数（单位：分）
 * @returns 前端浮点字符串（单位：元）
 */
function fenToYuan(fen: number | null | undefined): string {
  if (fen == null) return '0.00'
  return (fen / 100).toFixed(2)
}

/**
 * 前端金额（元）转后端金额（分）
 * @param yuan - 前端浮点字符串或数字（单位：元）
 * @returns 后端整数（单位：分）
 */
function yuanToFen(yuan: string | number): number {
  if (typeof yuan === 'number') return Math.round(yuan * 100)
  const num = parseFloat(yuan)
  return isNaN(num) ? 0 : Math.round(num * 100)
}

// ============================================================
// 导出转换器对象
// ============================================================

export const approvalDataConverter = {
  // ----------------------------------------------------------
  // 状态转换
  // ----------------------------------------------------------

  /**
   * 后端状态码转前端状态字符串
   * @param status - 后端状态码
   * @returns 前端状态字符串，无效值返回 'pending'
   */
  toFrontendStatus(status: number | null | undefined): ApprovalStatus {
    if (status == null || !STATUS_MAP[status as BackendStatus]) return 'pending'
    return STATUS_MAP[status as BackendStatus]
  },

  /**
   * 前端状态字符串转后端状态码
   * @param status - 前端状态字符串
   * @returns 后端状态码
   */
  toBackendStatus(status: ApprovalStatus): BackendStatus {
    return STATUS_REVERSE_MAP[status] ?? 0
  },

  /**
   * 获取状态中文标签
   * @param status - 前端状态字符串
   * @returns 中文标签
   */
  getStatusLabel(status: ApprovalStatus): string {
    return ApprovalStatusLabels[status] ?? status
  },

  // ----------------------------------------------------------
  // 紧急程度转换
  // ----------------------------------------------------------

  /**
   * 后端紧急度转前端紧急度
   * @param urgency - 后端紧急度（0普通/1紧急）
   * @returns 前端紧急度
   */
  toFrontendUrgency(urgency: number | null | undefined): 'normal' | 'urgent' {
    if (urgency == null) return 'normal'
    return URGENCY_MAP[urgency as 0 | 1] ?? 'normal'
  },

  /**
   * 前端紧急度转后端紧急度
   * @param urgency - 前端紧急度
   * @returns 后端紧急度
   */
  toBackendUrgency(urgency: 'normal' | 'urgent'): 0 | 1 {
    return URGENCY_REVERSE_MAP[urgency] ?? 0
  },

  // ----------------------------------------------------------
  // 列表项转换
  // ----------------------------------------------------------

  /**
   * 后端原始审批记录转前端列表项
   * @param raw - 后端原始数据
   * @returns 前端 ApprovalItem 对象
   */
  toItem(raw: RawApprovalItem): ApprovalItem {
    return {
      id: raw.id,
      type: raw.type as ApprovalType,
      title: raw.title,
      applicantName: raw.applicant_name,
      summary: raw.summary,
      status: this.toFrontendStatus(raw.status),
      createdAt: formatDateTime(raw.created_at),
      urgency: this.toFrontendUrgency(raw.urgency),
    }
  },

  /**
   * 批量转换为列表项
   * @param list - 后端原始列表
   * @returns 前端 ApprovalItem 数组
   */
  toItemList(list: RawApprovalItem[]): ApprovalItem[] {
    return list.map(item => this.toItem(item))
  },

  // ----------------------------------------------------------
  // 详情转换
  // ----------------------------------------------------------

  /**
   * 后端原始详情转前端完整详情
   * @param raw - 后端原始详情
   * @returns 前端 ApprovalDetail 对象
   */
  toDetail(raw: RawApprovalDetail): ApprovalDetail {
    return {
      id: raw.id,
      type: raw.type as ApprovalType,
      title: raw.title,
      applicantName: raw.applicant_name,
      summary: raw.summary,
      status: this.toFrontendStatus(raw.status),
      createdAt: formatDateTime(raw.created_at),
      urgency: this.toFrontendUrgency(raw.urgency),
      approvalNo: raw.approval_no,
      applicantLevel: raw.applicant_level,
      formFields: this.toFormFields(raw.form_fields),
      attachments: this.toAttachments(raw.attachments),
      flowNodes: this.toFlowNodes(raw.flow_nodes),
      comments: this.toComments(raw.comments),
      contextData: raw.context_data ? this.toContextData(raw.type, raw.context_data) : undefined,
    }
  },

  // ----------------------------------------------------------
  // 子结构转换
  // ----------------------------------------------------------

  /**
   * 后端表单字段数组转前端 FormField[]
   */
  toFormFields(fields: RawApprovalDetail['form_fields']): FormField[] {
    return fields.map(f => ({
      label: f.field_label,
      value: f.field_value,
      type: (f.field_type as FormField['type']) || 'text',
    }))
  },

  /**
   * 后端附件数组转前端 Attachment[]
   */
  toAttachments(attachments: RawApprovalDetail['attachments']): NonNullable<ApprovalDetail['attachments']> {
    return attachments.map(a => ({
      name: a.file_name,
      size: a.file_size > 1024 * 1024
        ? `${(a.file_size / (1024 * 1024)).toFixed(1)}MB`
        : `${Math.ceil(a.file_size / 1024)}KB`,
      url: a.file_url,
      type: (a.file_type as Attachment['type']) || 'other',
    }))
  },

  /**
   * 后端流程节点数组转前端 FlowNode[]
   */
  toFlowNodes(nodes: RawApprovalDetail['flow_nodes']): FlowNode[] {
    const NODE_STATUS_MAP: Record<number, FlowNode['status']> = {
      0: 'pending', 1: 'completed', 2: 'current', 3: 'skipped',
    }
    return nodes.map(n => ({
      id: n.node_id,
      role: n.node_role,
      userName: n.user_name,
      status: NODE_STATUS_MAP[n.node_status] ?? 'pending',
      action: n.action_text,
      time: n.action_time ? formatDateTime(n.action_time) : undefined,
    }))
  },

  /**
   * 后端评论数组转前端 Comment[]
   */
  toComments(comments: RawApprovalDetail['comments']): Comment[] {
    return comments.map(c => ({
      id: c.comment_id,
      userId: c.user_id,
      userName: c.user_name,
      content: c.content,
      createdAt: formatDateTime(c.created_at),
    }))
  },

  // ----------------------------------------------------------
  // ContextData 按类型转换
  // ----------------------------------------------------------

  /**
   * 根据审批类型转换上下文数据
   * @param type - 审批类型
   * @param ctx - 后端原始上下文数据
   * @returns 前端 ContextData 联合类型
   */
  toContextData(type: BackendType, ctx: Record<string, unknown>): ContextData {
    switch (type) {
      case 'leave':
        return { type: 'leave', data: this.toLeaveContext(ctx) }
      case 'overtime':
        return { type: 'overtime', data: this.toOvertimeContext(ctx) }
      case 'swap':
        return { type: 'swap', data: this.toSwapContext(ctx) }
      case 'travel':
        return { type: 'travel', data: this.toTravelContext(ctx) }
      case 'reimbursement':
        return { type: 'reimbursement', data: this.toExpenseContext(ctx) }
      case 'requisition':
        return { type: 'requisition', data: this.toRequisitionContext(ctx) }
      default:
        return { type: 'leave', data: {} as LeaveContext }
    }
  },

  /** 请假上下文转换 */
  toLeaveContext(ctx: Record<string, unknown>): LeaveContext {
    return {
      annualTotal: Number(ctx.annual_total ?? 5),
      annualUsed: Number(ctx.annual_used ?? 2),
      annualRemaining: Number(ctx.annual_remaining ?? 3),
      sickBalance: ctx.sick_balance != null ? Number(ctx.sick_balance) : undefined,
      personalBalance: ctx.personal_balance != null ? Number(ctx.personal_balance) : undefined,
      compensatoryBalance: ctx.compensatory_balance != null ? Number(ctx.compensatory_balance) : undefined,
      yearLeaveCount: Number(ctx.year_leave_count ?? 8),
      consecutiveDaysWarning: Boolean(ctx.consecutive_days_warning),
      lastLeaveDate: ctx.last_leave_date as string | undefined,
      leaveTrend: (ctx.leave_trend as Array<{ month: string; days: number; type: string }>)?.map(item => ({
        month: item.month,
        days: item.days,
        type: item.type,
      })),
    }
  },

  /** 加班上下文转换 */
  toOvertimeContext(ctx: Record<string, unknown>): OvertimeContext {
    return {
      monthHours: Number(ctx.month_hours ?? 12),
      monthLimit: Number(ctx.month_limit ?? 36),
      monthUsagePercent: Number(ctx.month_usage_percent ?? 33),
      quarterHours: Number(ctx.quarter_hours ?? 28),
      quarterLimit: Number(ctx.quarter_limit ?? 108),
      deptAvgHours: Number(ctx.dept_avg_hours ?? 15),
      compensateType: (ctx.compensate_type as OvertimeContext['compensateType']) ?? '调休',
      compensatoryBalance: ctx.compensatory_balance != null ? Number(ctx.compensatory_balance) : undefined,
      recentRecords: (ctx.recent_records as Array<{ date: string; hours: number; type: string }>)?.map(r => ({
        date: r.date,
        hours: r.hours,
        type: r.type,
      })),
    }
  },

  /** 换班上下文转换 */
  toSwapContext(ctx: Record<string, unknown>): SwapContext {
    return {
      partnerConfirmed: Boolean(ctx.partner_confirmed),
      partnerName: String(ctx.partner_name ?? ''),
      confirmTime: ctx.confirm_time as string | undefined,
      coveragePlan: String(ctx.coverage_plan ?? ''),
      recentSwapCount: Number(ctx.recent_swap_count ?? 1),
      swapLimit: Number(ctx.swap_limit ?? 3),
      originalShiftDetail: ctx.original_shift_detail ? {
        date: String((ctx.original_shift_detail as Record<string, unknown>).date ?? ''),
        time: String((ctx.original_shift_detail as Record<string, unknown>).time ?? ''),
        position: String((ctx.original_shift_detail as Record<string, unknown>).position ?? ''),
      } : undefined,
      targetShiftDetail: ctx.target_shift_detail ? {
        date: String((ctx.target_shift_detail as Record<string, unknown>).date ?? ''),
        time: String((ctx.target_shift_detail as Record<string, unknown>).time ?? ''),
        position: String((ctx.target_shift_detail as Record<string, unknown>).position ?? ''),
      } : undefined,
    }
  },

  /** 出差上下文转换 */
  toTravelContext(ctx: Record<string, unknown>): TravelContext {
    return {
      policyLimit: fenToYuan(Number(ctx.policy_limit)),
      estimatedActual: fenToYuan(Number(ctx.estimated_actual)),
      yearTripCount: Number(ctx.year_trip_count ?? 3),
      advancePayment: fenToYuan(Number(ctx.advance_payment)),
      hotelStandard: String(ctx.hotel_standard ?? '标准间'),
      transportAllowance: fenToYuan(Number(ctx.transport_allowance)),
      dailyAllowance: fenToYuan(Number(ctx.daily_allowance)),
      similarTrips: (ctx.similar_trips as Array<{ destination: string; dates: string; amount: number }>)?.map(t => ({
        destination: t.destination,
        dates: t.dates,
        amount: fenToYuan(t.amount),
      })),
    }
  },

  /** 报销上下文转换（含金额分→元） */
  toExpenseContext(ctx: Record<string, unknown>): ExpenseContext {
    const items = (ctx.items as Array<Record<string, unknown>>)?.map(item => ({
      date: formatDate(String(item.date ?? '')),
      category: String(item.category ?? ''),
      description: String(item.description ?? ''),
      amount: fenToYuan(Number(item.amount)),
      receiptNo: String(item.receipt_no ?? ''),
    })) ?? []

    return {
      items,
      monthlyTotal: fenToYuan(Number(ctx.monthly_total)),
      monthlyBudget: fenToYuan(Number(ctx.monthly_budget)),
      monthlyUsagePercent: Number(ctx.monthly_usage_percent ?? 45),
      yearTotal: fenToYuan(Number(ctx.year_total)),
      yearBudget: fenToYuan(Number(ctx.year_budget)),
      receiptCount: Number(ctx.receipt_count ?? items.length),
      relatedTravel: ctx.related_task ? {
        id: String((ctx.related_task as Record<string, unknown>).id ?? ''),
        title: String((ctx.related_task as Record<string, unknown>).title ?? ''),
        type: ((ctx.related_task as Record<string, unknown>).type as RelatedTask['type']) ?? 'travel_order',
        status: ((ctx.related_task as Record<string, unknown>).status as RelatedTask['status']) ?? 'active',
        source: String((ctx.related_task as Record<string, unknown>).source ?? ''),
        issueDate: String((ctx.related_task as Record<string, unknown>).issue_date ?? ''),
        issuerName: String((ctx.related_task as Record<string, unknown>).issuer_name ?? ''),
      } : undefined,
      policyNotes: String(ctx.policy_notes ?? ''),
    }
  },

  /** 领用上下文转换 */
  toRequisitionContext(ctx: Record<string, unknown>): RequisitionContext {
    return {
      stockQuantity: Number(ctx.stock_quantity ?? 50),
      stockUnit: String(ctx.stock_unit ?? '个'),
      monthlyAvg: Number(ctx.monthly_avg ?? 8),
      lastRequisitionDate: String(ctx.last_requisition_date ?? ''),
      lastRequisitionQuantity: Number(ctx.last_requisition_quantity ?? 5),
      departmentBudget: fenToYuan(Number(ctx.department_budget)),
      departmentUsed: fenToYuan(Number(ctx.department_used)),
      supplierInfo: ctx.supplier_info ? String(ctx.supplier_info) : undefined,
      leadTimeDays: Number(ctx.lead_time_days ?? 3),
    }
  },

  // ----------------------------------------------------------
  // 风控预警转换
  // ----------------------------------------------------------

  /**
   * 后端风控预警数据转前端 RiskWarning
   */
  toRiskWarning(raw: Record<string, unknown> | null): RiskWarning | undefined {
    if (!raw) return undefined
    const LEVEL_MAP: Record<string, RiskWarning['level']> = {
      info: 'info', warning: 'warning', danger: 'danger', prohibited: 'prohibited',
    }
    const CATEGORY_MAP: Record<string, RiskWarning['category']> = {
      health: 'health', compliance: 'compliance', policy: 'policy',
      budget: 'budget', schedule: 'schedule',
    }
    return {
      level: LEVEL_MAP[String(raw.level)] ?? 'info',
      category: CATEGORY_MAP[String(raw.category)] ?? 'policy',
      title: String(raw.title ?? ''),
      description: String(raw.description ?? ''),
      evidence: String(raw.evidence ?? ''),
      suggestion: String(raw.suggestion ?? ''),
      source: (raw.source as RiskWarning['source']) ?? 'hr_system_auto',
      generatedAt: formatDateTime(String(raw.generated_at ?? '')),
      reviewerName: raw.reviewer_name as string | undefined,
      regulationRef: raw.regulation_ref as string | undefined,
    }
  },

  // ----------------------------------------------------------
  // 提交方向转换（前端→后端）
  // ----------------------------------------------------------

  /**
   * 前端表单数据转为后端提交格式
   * 统一处理金额元→分、日期格式等
   * @param type - 审批类型
   * @param fields - 前端表单字段键值对
   * @returns 后端可接收的标准化字段对象
   */
  toSubmitFields(type: ApprovalType, fields: Record<string, unknown>): Record<string, unknown> {
    const result: Record<string, unknown> = { ...fields }

    // 按类型处理特殊字段
    switch (type) {
      case 'reimbursement':
        // 报销：金额字段需要元→分
        if ('totalAmount' in fields && typeof fields.totalAmount === 'number') {
          result.totalAmount = yuanToFen(fields.totalAmount)
        }
        if ('expenses' in fields && Array.isArray(fields.expenses)) {
          result.expenses = (fields.expenses as Array<Record<string, unknown>>).map(exp => ({
            ...exp,
            amount: typeof exp.amount === 'number' ? yuanToFen(exp.amount) : exp.amount,
          }))
        }
        break

      case 'travel':
        // 出差：预算金额元→分
        if ('budgetAmount' in fields && typeof fields.budgetAmount === 'number') {
          result.budgetAmount = yuanToFen(fields.budgetAmount)
        }
        break

      default:
        break
    }

    return result
  },
}
