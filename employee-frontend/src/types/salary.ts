export interface SalaryDeduction {
  name: string
  amount: number
  category: 'social' | 'tax' | 'other'
}

export interface SalaryDetail {
  baseSalary: number
  overtimePay: number
  bonus: number
  allowance: number
  deductions: SalaryDeduction[]
  totalDeduction: number
  grossPay: number
  netPay: number
  workDays: number
  overtimeHours: number
}

export interface PayslipItem {
  id: string
  yearMonth: string
  periodLabel: string
  grossAmount: number
  netAmount: number
  status: 'draft' | 'confirmed' | 'disputed'
  isRead: boolean
  confirmedAt?: string
  detail?: SalaryDetail
  /** 异议原因（disputed 状态时存在） */
  disputeReason?: string
}
