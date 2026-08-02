/**
 * 审批工作流模块类型定义
 *
 * 通用审批流程类型，供所有业务模块（采购、财务、人事等）共享使用
 */

/** 审批操作类型 */
export type ApprovalAction = 'submit' | 'approve' | 'reject' | 'withdraw' | 'delegate' | 'timeout'

/** 审批状态 */
export type ApprovalStatus = 'pending' | 'approved' | 'rejected' | 'withdrawn' | 'delegated' | 'timeout'

/** 审批人类型 */
export type ApproverType = 'role' | 'superior' | 'form_field' | 'specific_user'

/** 审批模式 */
export type ApproveMode = 'or' | 'and'

/** 超时处理方式 */
export type TimeoutAction = 'escalate' | 'auto_pass' | 'auto_reject' | 'notify'

/** 业务类型 */
export type BusinessType = 'purchase_request' | 'purchase_order' | 'purchase_plan' | 'contract' | 'payment' | 'expense' | 'material_request' | 'cert_renewal' | 'cert_expense_reimburse' | 'purchase_contract' | 'purchase_settlement' | 'employee_onboarding' | 'employee_leave' | 'finance_expense'

/** 权限模板编码 */
export type TemplateCode = 'enterprise-chain' | 'standard-chain' | 'centralized-single' | 'custom'

/** 企业运营模式 */
export type EnterpriseMode = 'single_store' | 'chain_small' | 'chain_large'

/** 条件操作符 */
export type ConditionOperator = 'lt' | 'lte' | 'gt' | 'gte' | 'eq' | 'contains'

/** 条件字段 */
export type ConditionField = 'totalAmount' | 'purchaseType' | 'supplierLevel'

/** 审批节点 */
export interface ApprovalNode {
  /** 节点ID */
  nodeId: string
  /** 节点名称 */
  nodeName: string
  /** 节点顺序 */
  nodeOrder: number
  /** 审批人类型 */
  approverType: ApproverType
  /** 审批人值（角色编码/上级编码/用户ID等） */
  approverValue: string
  /** 审批模式：or-或签/and-会签 */
  approveMode: ApproveMode
  /** 超时时间（小时） */
  timeoutHours: number
  /** 超时处理方式 */
  timeoutAction: TimeoutAction
}

/** 审批节点配置（单店模式简化版） */
export interface ApprovalNodeConfig {
  /** 节点名称 */
  nodeName: string
  /** 节点类型 */
  nodeType: 'submit' | 'approve' | 'cc' | 'end'
  /** 审批人角色编码 */
  approverRole: string
  /** 审批人名称 */
  approverName: string
  /** 是否必经节点 */
  required: boolean
}

/** 审批流程配置（按企业模式区分） */
export interface ApprovalFlowConfig {
  /** 业务类型 */
  businessType: BusinessType
  /** 企业运营模式 */
  mode: EnterpriseMode
  /** 审批节点配置列表 */
  nodes: ApprovalNodeConfig[]
}

/** 审批条件 */
export interface ApprovalCondition {
  /** 条件ID */
  conditionId: string
  /** 条件字段 */
  field: ConditionField
  /** 条件操作符 */
  operator: ConditionOperator
  /** 条件值 */
  value: number | string
  /** 跳转目标节点ID列表 */
  targetNodeIds: string[]
}

/** 审批流程（后端返回的完整数据） */
export interface ApprovalWorkflow {
  /** 流程ID */
  workflowId: string
  /** 流程名称 */
  workflowName: string
  /** 业务类型 */
  businessType: BusinessType
  /** 权限模板编码 */
  templateCode: TemplateCode
  /** 是否启用 */
  enabled: boolean
  /** 审批节点列表 */
  nodes: ApprovalNode[]
  /** 条件分支列表 */
  conditions: ApprovalCondition[]
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 审批流程表单数据（新建/更新） */
export interface ApprovalWorkflowFormData {
  /** 流程名称 */
  workflowName: string
  /** 业务类型 */
  businessType: BusinessType
  /** 权限模板编码 */
  templateCode: TemplateCode
  /** 是否启用 */
  enabled: boolean
  /** 审批节点列表 */
  nodes: ApprovalNode[]
  /** 条件分支列表 */
  conditions: ApprovalCondition[]
}

/** 审批流程查询参数 */
export interface ApprovalWorkflowQueryForm {
  /** 业务类型 */
  businessType?: BusinessType
  /** 权限模板编码 */
  templateCode?: TemplateCode
  /** 是否启用 */
  enabled?: boolean
  /** 关键词搜索 */
  keyword?: string
}

// ============================================================
// 选项常量（供 el-select 等组件使用）
// ============================================================

/** 业务类型选项 */
export const BusinessTypeOptions: { value: BusinessType; label: string }[] = [
  { value: 'purchase_request', label: '采购申请' },
  { value: 'purchase_order', label: '采购订单' },
  { value: 'purchase_plan', label: '采购计划' },
  { value: 'purchase_contract', label: '采购合同' },
  { value: 'purchase_settlement', label: '采购结算' },
  { value: 'contract', label: '合同签署' },
  { value: 'payment', label: '付款审批' },
  { value: 'expense', label: '费用报销' },
  { value: 'material_request', label: '物资需求审核' },
  { value: 'cert_renewal', label: '证件续期' },
  { value: 'cert_expense_reimburse', label: '证件费用报销' },
  { value: 'employee_onboarding', label: '员工入职' },
  { value: 'employee_leave', label: '员工请假' },
  { value: 'finance_expense', label: '财务报销' },
]

/** 权限模板选项 */
export const TemplateCodeOptions: { value: TemplateCode; label: string }[] = [
  { value: 'enterprise-chain', label: '大型连锁' },
  { value: 'standard-chain', label: '标准连锁' },
  { value: 'centralized-single', label: '集中式单店' },
  { value: 'custom', label: '完全自定义' },
]

/** 审批人类型选项 */
export const ApproverTypeOptions: { value: ApproverType; label: string }[] = [
  { value: 'role', label: '角色' },
  { value: 'superior', label: '上级' },
  { value: 'form_field', label: '表单字段' },
  { value: 'specific_user', label: '指定用户' },
]

/** 审批模式选项 */
export const ApproveModeOptions: { value: ApproveMode; label: string }[] = [
  { value: 'or', label: '或签（任一通过）' },
  { value: 'and', label: '会签（全部通过）' },
]

/** 超时处理选项 */
export const TimeoutActionOptions: { value: TimeoutAction; label: string }[] = [
  { value: 'escalate', label: '自动升级' },
  { value: 'auto_pass', label: '自动通过' },
  { value: 'auto_reject', label: '自动驳回' },
  { value: 'notify', label: '仅通知' },
]

/** 条件字段选项 */
export const ConditionFieldOptions: { value: ConditionField; label: string }[] = [
  { value: 'totalAmount', label: '金额' },
  { value: 'purchaseType', label: '采购类型' },
  { value: 'supplierLevel', label: '供应商等级' },
]

/** 条件操作符选项 */
export const ConditionOperatorOptions: { value: ConditionOperator; label: string }[] = [
  { value: 'lt', label: '小于' },
  { value: 'lte', label: '小于等于' },
  { value: 'gt', label: '大于' },
  { value: 'gte', label: '大于等于' },
  { value: 'eq', label: '等于' },
  { value: 'contains', label: '包含' },
]

/** 审批记录（后端返回的原始数据） */
export interface ApprovalAuditLog {
  /** 审批记录ID（后端字段 logId） */
  logId: string
  /** 业务ID */
  businessId: string
  /** 业务类型 */
  businessType: string
  /** 审批节点名称 */
  nodeName: string
  /** 操作人姓名 */
  operatorName: string
  /** 操作类型：submit/approve/reject/withdraw/delegate/timeout */
  action: ApprovalAction
  /** 审批意见（后端字段 opinion） */
  opinion: string
  /** 操作时间（ISO 8601） */
  operateTime: string
  /** 委托目标人姓名（仅 delegate 操作有值） */
  delegateToName?: string
  /** 审批流程ID */
  workflowId?: string
  /** 审批节点ID */
  nodeId?: string
  /** 审批人类型 */
  approverType?: string
  /** 审批人姓名 */
  approverName?: string
}

/** 审批记录（前端展示用） */
export interface ApprovalAuditLogDisplay {
  /** 审批记录ID */
  auditLogId: string
  /** 审批节点名称 */
  nodeName: string
  /** 操作人姓名 */
  operatorName: string
  /** 操作类型（前端语义化字符串） */
  action: ApprovalAction
  /** 操作类型标签（中文） */
  actionLabel: string
  /** 审批意见 */
  comment: string
  /** 操作时间（格式化后：YYYY-MM-DD HH:mm:ss） */
  operateTime: string
  /** 委托目标人姓名 */
  delegateToName?: string
}

/** 审批请求参数 */
export interface ApprovalActionParams {
  /** 业务ID */
  businessId: string
  /** 业务类型 */
  businessType: string
  /** 审批意见 */
  comment: string
}

/** 当前审批节点信息 */
export interface ApprovalCurrentNode {
  /** 节点ID */
  nodeId?: string
  /** 节点名称 */
  nodeName: string
  /** 审批人类型 */
  approverType: ApproverType
  /** 审批人名称 */
  approverName?: string
  /** 审批人角色 */
  approverRole?: string
  /** 节点状态 */
  status?: 'pending' | 'completed' | 'waiting'
}

// ============================================================
// 单店模式审批流程配置
// ============================================================

/** 单店模式审批流程配置 */
export const SINGLE_STORE_APPROVAL_FLOWS: Record<string, ApprovalNodeConfig[]> = {
  purchase_order: [
    { nodeName: '提交订单', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  purchase_contract: [
    { nodeName: '提交合同', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  purchase_settlement: [
    { nodeName: '提交结算', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '财务确认', nodeType: 'approve', approverRole: 'finance', approverName: '财务', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  material_request: [
    { nodeName: '提交需求', nodeType: 'submit', approverRole: 'store_staff', approverName: '门店员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  cert_renewal: [
    { nodeName: '提交续期', nodeType: 'submit', approverRole: 'store_staff', approverName: '门店员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  cert_expense_reimburse: [
    { nodeName: '提交报销', nodeType: 'submit', approverRole: 'store_staff', approverName: '门店员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '财务确认', nodeType: 'approve', approverRole: 'finance', approverName: '财务', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  employee_onboarding: [
    { nodeName: '提交入职', nodeType: 'submit', approverRole: 'hr', approverName: '人事', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  employee_leave: [
    { nodeName: '提交请假', nodeType: 'submit', approverRole: 'employee', approverName: '员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  finance_expense: [
    { nodeName: '提交报销', nodeType: 'submit', approverRole: 'employee', approverName: '员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '财务确认', nodeType: 'approve', approverRole: 'finance', approverName: '财务', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  purchase_request: [
    { nodeName: '提交申请', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  purchase_plan: [
    { nodeName: '提交计划', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  contract: [
    { nodeName: '提交合同', nodeType: 'submit', approverRole: 'purchaser', approverName: '采购员', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  payment: [
    { nodeName: '提交付款', nodeType: 'submit', approverRole: 'finance', approverName: '财务', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
  expense: [
    { nodeName: '提交报销', nodeType: 'submit', approverRole: 'employee', approverName: '员工', required: true },
    { nodeName: '店长审批', nodeType: 'approve', approverRole: 'store_manager', approverName: '店长', required: true },
    { nodeName: '完成', nodeType: 'end', approverRole: '', approverName: '', required: true },
  ],
}
