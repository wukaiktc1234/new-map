/**
 * 审批流程模块数据转换器
 * 负责后端数据与前端展示数据之间的双向转换
 * 遵循规范：禁止在组件中直接做状态映射或金额元分转换
 */

import type {
  BusinessType,
  TemplateCode,
  ApproverType,
  ApprovalAction,
  ApprovalAuditLog,
  ApprovalAuditLogDisplay,
} from '@/types/approval-workflow'

// ============================================================
// 业务类型转换
// ============================================================

/** 业务类型 → 中文标签 */
const businessTypeLabelMap: Record<BusinessType, string> = {
  purchase_request: '采购申请',
  purchase_order: '采购订单',
  purchase_plan: '采购计划',
  purchase_contract: '采购合同',
  purchase_settlement: '采购结算',
  contract: '合同签署',
  payment: '付款审批',
  expense: '费用报销',
  material_request: '物资需求审核',
  cert_renewal: '证件续期',
  cert_expense_reimburse: '证件费用报销',
  employee_onboarding: '员工入职',
  employee_leave: '员工请假',
  finance_expense: '财务报销',
}

/** 业务类型 → StatusTag status */
const businessTypeStatusMap: Record<BusinessType, string> = {
  purchase_request: 'primary',
  purchase_order: 'info',
  purchase_plan: 'warning',
  purchase_contract: 'success',
  purchase_settlement: 'danger',
  contract: 'success',
  payment: 'danger',
  expense: 'default',
  material_request: 'info',
  cert_renewal: 'warning',
  cert_expense_reimburse: 'danger',
  employee_onboarding: 'primary',
  employee_leave: 'warning',
  finance_expense: 'danger',
}

// ============================================================
// 权限模板转换
// ============================================================

/** 权限模板 → 中文标签 */
const templateCodeLabelMap: Record<TemplateCode, string> = {
  'enterprise-chain': '大型连锁',
  'standard-chain': '标准连锁',
  'centralized-single': '集中式单店',
  'custom': '完全自定义',
}

/** 权限模板 → StatusTag status */
const templateCodeStatusMap: Record<TemplateCode, string> = {
  'enterprise-chain': 'danger',
  'standard-chain': 'warning',
  'centralized-single': 'primary',
  'custom': 'default',
}

// ============================================================
// 审批人类型转换
// ============================================================

/** 审批人类型 → 中文标签 */
const approverTypeLabelMap: Record<ApproverType, string> = {
  role: '角色',
  superior: '上级',
  form_field: '表单字段',
  specific_user: '指定用户',
}

/** 审批人类型 → StatusTag status */
const approverTypeStatusMap: Record<ApproverType, string> = {
  role: 'primary',
  superior: 'warning',
  form_field: 'info',
  specific_user: 'success',
}

// ============================================================
// 审批动作转换
// ============================================================

/** 审批动作 → StatusTag status */
const approvalActionStatusMap: Record<ApprovalAction, string> = {
  submit: 'primary',
  approve: 'success',
  reject: 'danger',
  withdraw: 'warning',
  delegate: 'info',
  timeout: 'default',
}

/** 审批动作 → 中文标签 */
const approvalActionLabelMap: Record<ApprovalAction, string> = {
  submit: '提交',
  approve: '通过',
  reject: '驳回',
  withdraw: '撤回',
  delegate: '委派',
  timeout: '超时',
}

// ============================================================
// 导出转换器
// ============================================================

export const approvalWorkflowConverter = {
  /** 业务类型 → 中文标签 */
  toBusinessTypeLabel(type: BusinessType): string {
    return businessTypeLabelMap[type] || type
  },

  /** 业务类型 → StatusTag status */
  toBusinessTypeStatusTagStatus(type: BusinessType): string {
    return businessTypeStatusMap[type] || 'default'
  },

  /** 权限模板 → 中文标签 */
  toTemplateCodeLabel(code: TemplateCode): string {
    return templateCodeLabelMap[code] || code
  },

  /** 权限模板 → StatusTag status */
  toTemplateCodeStatusTagStatus(code: TemplateCode): string {
    return templateCodeStatusMap[code] || 'default'
  },

  /** 审批人类型 → 中文标签 */
  toApproverTypeLabel(type: ApproverType): string {
    return approverTypeLabelMap[type] || type
  },

  /** 审批人类型 → StatusTag status */
  toApproverTypeStatusTagStatus(type: ApproverType): string {
    return approverTypeStatusMap[type] || 'default'
  },

  /** 审批动作 → StatusTag status */
  toApprovalActionStatusTagStatus(action: ApprovalAction): string {
    return approvalActionStatusMap[action] || 'default'
  },

  /** 审批动作 → 中文标签 */
  toApprovalActionLabel(action: ApprovalAction): string {
    return approvalActionLabelMap[action] || action
  },

  /** 将后端审批记录列表转换为前端展示格式 */
  convertAuditLogsToDisplay(logs: ApprovalAuditLog[]): ApprovalAuditLogDisplay[] {
    return logs.map(log => ({
      auditLogId: log.logId,
      nodeName: log.nodeName,
      operatorName: log.operatorName || log.approverName || '-',
      action: log.action,
      actionLabel: approvalActionLabelMap[log.action] || log.action,
      comment: log.opinion,
      operateTime: log.operateTime,
      delegateToName: log.delegateToName,
    }))
  },
}
