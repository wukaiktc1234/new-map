/**
 * 超龄劳动者类型定义
 * 依据《超龄劳动者基本权益保障暂行规定》设计
 */

/** 用工类型 */
export type EmploymentType =
  | 'full_time'        // 全职
  | 'part_time'        // 兼职
  | 'intern'           // 实习生
  | 'dispatch'         // 劳务派遣
  | 'over_age'         // 超龄返聘

/** 用工类型选项 */
export const EmploymentTypeOptions: { value: EmploymentType; label: string }[] = [
  { value: 'full_time', label: '全职' },
  { value: 'part_time', label: '兼职' },
  { value: 'intern', label: '实习生' },
  { value: 'dispatch', label: '劳务派遣' },
  { value: 'over_age', label: '超龄返聘' },
]

/** 员工生命周期状态（扩展） */
export type EmployeeLifecycleStatus =
  | 'candidate'          // 候选人
  | 'onboarding'         // 入职办理中
  | 'probation'          // 试用期内
  | 'active'             // 正式在职
  | 'over_age_employed'  // 超龄返聘
  | 'resigning'          // 离职办理中
  | 'resigned'           // 已离职
  | 'retired'            // 已退休（未返聘）

/** 员工生命周期状态选项 */
export const EmployeeLifecycleStatusOptions: { value: EmployeeLifecycleStatus; label: string }[] = [
  { value: 'candidate', label: '候选人' },
  { value: 'onboarding', label: '入职办理中' },
  { value: 'probation', label: '试用期' },
  { value: 'active', label: '正式在职' },
  { value: 'over_age_employed', label: '超龄返聘' },
  { value: 'resigning', label: '离职办理中' },
  { value: 'resigned', label: '已离职' },
  { value: 'retired', label: '已退休' },
]

/** 超龄劳动者信息 */
export interface OverAgeWorkerInfo {
  id: string
  employeeId: string
  employeeName: string
  /** 退休日期 */
  retirementDate: string
  /** 返聘日期 */
  reemploymentDate: string
  /** 劳务协议编号 */
  agreementNo: string
  /** 协议开始日期 */
  agreementStartDate: string
  /** 协议结束日期 */
  agreementEndDate: string
  /** 工伤保险参保号 */
  workInjuryInsuranceNo?: string
  /** 工伤保险到期日 */
  workInjuryInsuranceExpiry?: string
  /** 体检到期日 */
  healthCheckExpiry?: string
  /** 体检结果 */
  healthCheckResult?: 'qualified' | 'restricted' | 'unqualified'
  /** 限制岗位列表 */
  restrictedPositions?: string[]
  /** 备注 */
  remark?: string
  /** 创建时间 */
  createTime?: string
  /** 更新时间 */
  updateTime?: string
}

/** 超龄劳动者表单数据 */
export interface OverAgeWorkerFormData {
  employeeId: string
  retirementDate: string
  reemploymentDate: string
  agreementNo: string
  agreementStartDate: string
  agreementEndDate: string
  workInjuryInsuranceNo?: string
  workInjuryInsuranceExpiry?: string
  healthCheckExpiry?: string
  healthCheckResult?: 'qualified' | 'restricted' | 'unqualified'
  restrictedPositions?: string[]
  remark?: string
}

/** 超龄劳动者查询参数 */
export interface OverAgeWorkerQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  healthCheckResult?: string
  agreementExpiringDays?: number
}
