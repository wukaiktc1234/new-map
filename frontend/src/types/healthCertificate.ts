// 报销状态类型
export type ExpenseStatus = 'pending' | 'approved' | 'rejected' | 'paid' | 'reimbursed' | 'none';

// 审批记录类型
export interface ApprovalRecord {
  approver: string;
  approveDate: string;
  status: 'approved' | 'rejected';
  comment?: string;
}

// 健康证报销类型
export interface HealthCertificateExpense {
  id: string;
  healthCertificateId: string;
  employeeId: string;
  employeeName: string;
  amount: number;
  invoiceType: string;
  invoiceNumber: string;
  invoiceDate: string;
  invoiceAttachments: string[];
  reason: string;
  note?: string;
  status: ExpenseStatus;
  applyDate: string;
  approveDate?: string;
  reimburseDate?: string;
  rejectReason?: string;
  approvalHistory: ApprovalRecord[];
  expenseType: 'new' | 'renewal';
}

// 健康证审核状态类型
export type HealthCertificateApprovalStatus = 'pending' | 'approved' | 'rejected' | 'draft';

// 健康证状态类型
export type HealthCertificateStatus = 'valid' | 'expiring' | 'expired';

/**
 * 健康证全局统计数据
 * 用于健康证管理页面顶部统计卡片展示（不受列表筛选条件影响）
 */
export interface HealthCertificateStatistics {
  /** 健康证总数 */
  total: number
  /** 有效健康证数 */
  valid: number
  /** 即将到期健康证数 */
  expiring: number
  /** 已到期健康证数 */
  expired: number
}

// 健康证类型定
export interface HealthCertificate {
  id: string;
  employeeId: string;
  employeeName: string;
  store: string;
  certificateNumber: string;
  issueDate: string;
  expiryDate: string;
  status: HealthCertificateStatus;
  issuer: string;
  note?: string;
  expenseStatus?: ExpenseStatus;
  // 审核相关字段
  approvalStatus: HealthCertificateApprovalStatus;
  approvalBy?: string;
  approvalDate?: string;
  rejectReason?: string;
  submissionDate?: string;
  submittedBy?: string;
  certificateImage?: string;
  lastExpenseId?: string;
  // 操作人相关字  operator?: string;
  operationTime?: string;
  expiryDays?: number;
  // 软删除相关字
  deleted: boolean;
  deletedAt?: string;
  deletedBy?: string;
}

// 健康证审批请求类型
export interface HealthCertificateApprovalRequest {
  id: string;
  status: 'approved' | 'rejected';
  comment?: string;
  rejectReason?: string;
}

// 健康证报销申请类型
export interface HealthCertificateExpenseRequest {
  healthCertificateId: string;
  amount: number;
  invoiceType: string;
  invoiceNumber: string;
  invoiceDate: string;
  invoiceAttachments: string[];
  reason: string;
  note?: string;
  expenseType: 'new' | 'renewal';
}

// 报销查询参数类型
export interface ExpenseQueryParams {
  employeeId?: string;
  healthCertificateId?: string;
  status?: ExpenseStatus;
  startDate?: string;
  endDate?: string;
  page?: number;
  pageSize?: number;
}

// 导出参数类型
export interface ExportParams {
  startDate?: string;
  endDate?: string;
  status?: HealthCertificateStatus;
  store?: string;
  format?: 'excel' | 'csv' | 'pdf';
}
