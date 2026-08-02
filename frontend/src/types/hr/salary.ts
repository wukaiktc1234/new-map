/**
 * 薪资管理类型定义
 * 包含薪资计算、发放、财务对接等全流程类型
 */

/** 薪资状态 */
export enum SalaryStatus {
  /** 草稿 - 未确认 */
  DRAFT = 'draft',
  /** 待审核 - 已提交待审批 */
  PENDING = 'pending',
  /** 待发放 - 已审核待发放 */
  APPROVED = 'approved',
  /** 已发放 - 已完成发放 */
  PAID = 'paid',
  /** 已撤销 */
  CANCELLED = 'cancelled',
}

/** 薪资状态 → StatusTag映射 */
export const SalaryStatusTagMap: Record<SalaryStatus, { status: string; label: string }> = {
  [SalaryStatus.DRAFT]: { status: 'info', label: '草稿' },
  [SalaryStatus.PENDING]: { status: 'warning', label: '待审核' },
  [SalaryStatus.APPROVED]: { status: 'info', label: '待发放' },
  [SalaryStatus.PAID]: { status: 'active', label: '已发放' },
  [SalaryStatus.CANCELLED]: { status: 'inactive', label: '已撤销' },
}

/** 薪资状态选项 */
export const SalaryStatusOptions = Object.entries(SalaryStatusTagMap).map(([value, { label }]) => ({
  value,
  label,
}))

/** 薪资项类型 */
export type SalaryItemType = 'base' | 'performance' | 'overtime' | 'subsidy' | 'deduction' | 'social' | 'tax'

/** 薪资记录实体 */
export interface SalaryRecord {
  id: string
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 员工编号 */
  employeeCode?: string
  /** 部门ID */
  departmentId: string
  /** 部门名称 */
  departmentName: string
  /** 岗位名称 */
  positionName?: string
  /** 薪资期间 (YYYY-MM) */
  period: string
  /** 基本工资（分） */
  basicSalary: number
  /** 绩效奖金（分） */
  performanceBonus: number
  /** 加班费（分） */
  overtimePay: number
  /** 补贴（分） */
  subsidy: number
  /** 扣款项（分，正数表示扣除） */
  deductions: number
  /** 社保扣款（分） */
  socialInsurance: number
  /** 个税（分） */
  tax: number
  /** 应发工资（分） */
  grossSalary: number
  /** 实发工资（分） */
  netSalary: number
  /** 状态 */
  status: SalaryStatus
  /** 发放时间 */
  paidAt?: string
  /** 发放方式 */
  paymentMethod?: 'bank_transfer' | 'cash' | 'check'
  /** 银行流水号（财务对接） */
  bankTransactionNo?: string
  /** 会计凭证号（财务对接） */
  voucherNo?: string
  /** 财务同步状态 */
  financeSyncStatus?: 'pending' | 'synced' | 'failed'
  /** 财务同步时间 */
  financeSyncedAt?: string
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 薪资查询DTO */
export interface SalaryQueryDTO {
  period?: string
  departmentId?: string
  employeeName?: string
  status?: SalaryStatus | ''
  page?: number
  pageSize?: number
}

/** 薪资审核DTO */
export interface SalaryApprovalDTO {
  id: string
  approved: boolean
  remark?: string
}

/** 薪资发放DTO */
export interface SalaryPaymentDTO {
  id: string
  paymentMethod: 'bank_transfer' | 'cash' | 'check'
  remark?: string
}

/** 薪资批量发放DTO */
export interface SalaryBatchPaymentDTO {
  ids: string[]
  paymentMethod: 'bank_transfer' | 'cash' | 'check'
}

/** 薪资统计VO */
export interface SalaryStatisticsVO {
  /** 总人数 */
  totalCount: number
  /** 已发放人数 */
  paidCount: number
  /** 待发放人数 */
  pendingCount: number
  /** 本月薪资总额（分） */
  totalAmount: number
  /** 已发放金额（分） */
  paidAmount: number
  /** 待发放金额（分） */
  pendingAmount: number
  /** 人均薪资（分） */
  avgSalary: number
}

/** 财务对接 - 薪资同步数据 */
export interface SalaryFinanceSyncData {
  /** 薪资记录ID */
  salaryId: string
  /** 员工姓名 */
  employeeName: string
  /** 薪资期间 */
  period: string
  /** 实发工资（分） */
  netSalary: number
  /** 发放方式 */
  paymentMethod: string
  /** 银行流水号 */
  bankTransactionNo?: string
  /** 会计科目编码 */
  accountCode: string
  /** 摘要 */
  summary: string
}

/* ============================================================
 * 薪资账期（月份总账）- 两级视图重构
 * 主表为账期（按自然月汇总），从表为该账期下的个人薪资明细
 * ============================================================ */

/** 账期状态 */
export enum SalaryBatchStatus {
  /** 草稿 - 薪资计算中/未确认 */
  DRAFT = 'draft',
  /** 待审核 - 已提交待审批 */
  PENDING = 'pending',
  /** 待发放 - 已审核待发放 */
  APPROVED = 'approved',
  /** 发放中 - 部分已发放 */
  PAYING = 'paying',
  /** 已发放 - 全部发放完成 */
  PAID = 'paid',
  /** 已撤销 */
  CANCELLED = 'cancelled',
}

/** 账期状态 → StatusTag映射 */
export const SalaryBatchStatusTagMap: Record<SalaryBatchStatus, { status: string; label: string }> = {
  [SalaryBatchStatus.DRAFT]: { status: 'info', label: '草稿' },
  [SalaryBatchStatus.PENDING]: { status: 'warning', label: '待审核' },
  [SalaryBatchStatus.APPROVED]: { status: 'info', label: '待发放' },
  [SalaryBatchStatus.PAYING]: { status: 'pending', label: '发放中' },
  [SalaryBatchStatus.PAID]: { status: 'active', label: '已发放' },
  [SalaryBatchStatus.CANCELLED]: { status: 'inactive', label: '已撤销' },
}

/** 账期状态选项 */
export const SalaryBatchStatusOptions = Object.entries(SalaryBatchStatusTagMap).map(([value, { label }]) => ({
  value,
  label,
}))

/** 薪资账期（月份总账） */
export interface SalaryBatch {
  /** 账期ID */
  id: string
  /** 账期编号（如 SAL202606） */
  batchNo: string
  /** 薪资期间 (YYYY-MM) */
  period: string
  /** 账期名称（如 2026年6月薪资） */
  batchName: string
  /** 状态 */
  status: SalaryBatchStatus
  /** 涉及员工数 */
  employeeCount: number
  /** 应发总额（分） */
  totalGrossAmount: number
  /** 实发总额（分） */
  totalNetAmount: number
  /** 基本工资总额（分） */
  totalBasicAmount: number
  /** 绩效奖金总额（分） */
  totalPerformanceAmount: number
  /** 加班费总额（分） */
  totalOvertimeAmount: number
  /** 补贴总额（分） */
  totalSubsidyAmount: number
  /** 扣款总额（分） */
  totalDeductionAmount: number
  /** 社保总额（分） */
  totalSocialInsuranceAmount: number
  /** 个税总额（分） */
  totalTaxAmount: number
  /** 已发放人数 */
  paidCount: number
  /** 已发放金额（分） */
  paidAmount: number
  /** 计算开始时间 */
  calcStartTime?: string
  /** 计算完成时间 */
  calcEndTime?: string
  /** 审核人 */
  approvedBy?: string
  /** 审核时间 */
  approvedAt?: string
  /** 发放时间 */
  paidAt?: string
  /** 发放方式 */
  paymentMethod?: 'bank_transfer' | 'cash' | 'check'
  /** 数据来源标记（智能化联动） */
  dataSources?: SalaryBatchDataSource[]
  /** 备注 */
  remark?: string
  /** 创建人 */
  createdBy?: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 数据来源标记（智能化联动考勤、加班等） */
export interface SalaryBatchDataSource {
  /** 来源类型 */
  type: 'attendance' | 'overtime' | 'leave' | 'performance' | 'social_insurance' | 'tax'
  /** 来源描述 */
  label: string
  /** 是否已同步 */
  synced: boolean
  /** 同步时间 */
  syncedAt?: string
  /** 涉及记录数 */
  recordCount?: number
}

/** 账期查询DTO */
export interface SalaryBatchQueryDTO {
  /** 薪资期间范围-开始 */
  periodStart?: string
  /** 薪资期间范围-结束 */
  periodEnd?: string
  /** 状态 */
  status?: SalaryBatchStatus | ''
  /** 关键字（账期编号/名称） */
  keyword?: string
  /** 页码 */
  page?: number
  /** 每页条数 */
  pageSize?: number
}

/** 账期创建DTO */
export interface SalaryBatchCreateDTO {
  /** 薪资期间 (YYYY-MM) */
  period: string
  /** 账期名称 */
  batchName?: string
  /** 部门ID列表（不传为全部部门） */
  departmentIds?: string[]
  /** 是否自动联动数据来源 */
  autoSyncData?: boolean
  /** 备注 */
  remark?: string
}

/** 账期审核DTO */
export interface SalaryBatchApprovalDTO {
  /** 账期ID */
  batchId: string
  /** 是否通过 */
  approved: boolean
  /** 审核意见 */
  remark?: string
}

/** 账期发放DTO */
export interface SalaryBatchPayDTO {
  /** 账期ID */
  batchId: string
  /** 发放方式 */
  paymentMethod: 'bank_transfer' | 'cash' | 'check'
  /** 备注 */
  remark?: string
}

/** 账期统计VO */
export interface SalaryBatchStatisticsVO {
  /** 账期总数 */
  totalBatches: number
  /** 草稿数 */
  draftCount: number
  /** 待审核数 */
  pendingCount: number
  /** 待发放数 */
  approvedCount: number
  /** 已发放数 */
  paidCount: number
  /** 本年度实发总额（分） */
  yearlyNetAmount: number
  /** 本月实发总额（分） */
  monthlyNetAmount: number
  /** 涉及员工总数 */
  totalEmployees: number
}

/** 薪资项明细（用于展示薪资构成和数据来源联动） */
export interface SalaryItemDetail {
  /** 薪资项类型 */
  type: SalaryItemType
  /** 薪资项名称 */
  label: string
  /** 金额（分） */
  amount: number
  /** 计算公式/说明 */
  formula?: string
  /** 数据来源 */
  dataSource?: string
  /** 来源记录数 */
  sourceRecordCount?: number
}

/** 薪资明细（含构成明细，用于账期详情中的个人明细） */
export interface SalaryRecordDetail extends SalaryRecord {
  /** 薪资构成明细 */
  items: SalaryItemDetail[]
  /** 考勤汇总（联动数据） */
  attendanceSummary?: {
    /** 应出勤天数 */
    shouldAttendDays: number
    /** 实际出勤天数 */
    actualAttendDays: number
    /** 迟到次数 */
    lateCount: number
    /** 早退次数 */
    earlyLeaveCount: number
    /** 旷工天数 */
    absentDays: number
    /** 请假天数 */
    leaveDays: number
    /** 病假天数 */
    sickLeaveDays: number
    /** 加班时长（小时） */
    overtimeHours: number
  }
}
