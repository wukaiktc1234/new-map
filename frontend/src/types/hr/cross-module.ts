/**
 * 跨模块数据流转类型定义
 * 定义各模块间数据交换的统一接口
 */

/** 数据来源模块 */
export type SourceModule = 'store' | 'hr' | 'asset' | 'recharge' | 'purchase' | 'finance'

/** 数据目标模块 */
export type TargetModule = 'store' | 'hr' | 'asset' | 'recharge' | 'purchase' | 'finance'

/** 跨模块事件状态 */
export type CrossModuleEventStatus = 'pending' | 'received' | 'processed' | 'rejected'

/** 跨模块事件类型 */
export type CrossModuleEventType =
  // 门店→人事
  | 'attendance_submission'       // 考勤提交
  | 'recruitment_request'         // 招聘需求
  | 'health_cert_expense'         // 健康证报销申请
  // 人事→门店
  | 'employee_onboard'            // 员工入职通知
  | 'employee_resign'             // 员工离职通知
  | 'health_cert_expiry_warning'  // 健康证到期预警
  | 'training_completed'          // 培训完成通知
  // 人事→财务
  | 'salary_submission'           // 薪资提交
  | 'expense_reimbursement'       // 报销申请
  | 'social_insurance_payment'    // 社保缴纳
  | 'resign_compensation'         // 离职补偿
  // 资产→财务
  | 'depreciation_voucher'        // 折旧凭证
  | 'asset_disposal'              // 资产处置
  | 'maintenance_expense'         // 维修费用
  // 储值→财务
  | 'recharge_finance_log'        // 充值财务流水
  | 'refund_approval'             // 退款审批

/** 跨模块事件 */
export interface CrossModuleEvent {
  eventId: string
  sourceModule: SourceModule
  targetModule: TargetModule
  eventType: CrossModuleEventType
  /** 业务单据ID */
  businessId: string
  /** 数据载荷 */
  payload: Record<string, unknown>
  status: CrossModuleEventStatus
  createTime: string
  processTime?: string
  processBy?: string
  processRemark?: string
}

/** 待办任务（各模块概览页展示） */
export interface PendingTask {
  id: string
  title: string
  description: string
  sourceModule: SourceModule
  eventType: CrossModuleEventType
  businessId: string
  /** 优先级 */
  priority: 'high' | 'medium' | 'low'
  /** 截止时间 */
  deadline?: string
  createTime: string
  /** 跳转路由 */
  routePath?: string
}

/** 审批流程配置 */
export interface ApprovalFlowConfig {
  id: string
  /** 审批类型 */
  approvalType: string
  approvalTypeName: string
  /** 审批步骤 */
  steps: ApprovalFlowStep[]
  /** 是否启用 */
  enabled: boolean
}

/** 审批流程步骤 */
export interface ApprovalFlowStep {
  step: number
  stepName: string
  /** 审批角色 */
  approverRole: string
  /** 审批人ID（可选，指定具体人员） */
  approverId?: string
  /** 是否必须（跳过则自动通过） */
  required: boolean
}
