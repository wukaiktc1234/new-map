import type { PayslipItem, SalaryDetail, SalaryDeduction } from '@/types/salary'
import { mockDelay } from './mock/delays'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

const mockPayslips: PayslipItem[] = [
  {
    id: 'sal-001',
    yearMonth: '2026-05',
    periodLabel: '2026年5月',
    grossAmount: 6300,
    netAmount: 5730,
    status: 'confirmed',
    isRead: true,
    confirmedAt: '2026-05-28T10:00:00',
    detail: {
      baseSalary: 5000,
      overtimePay: 800,
      bonus: 300,
      allowance: 200,
      deductions: [
        { name: '养老保险', amount: 140, category: 'social' },
        { name: '医疗保险', amount: 70, category: 'social' },
        { name: '失业保险', amount: 35, category: 'social' },
        { name: '住房公积金', amount: 240, category: 'social' },
        { name: '个人所得税', amount: 85, category: 'tax' },
      ],
      totalDeduction: 570,
      grossPay: 6300,
      netPay: 5730,
      workDays: 22,
      overtimeHours: 8,
    },
  },
  {
    id: 'sal-002',
    yearMonth: '2026-04',
    periodLabel: '2026年4月',
    grossAmount: 6600,
    netAmount: 6104,
    status: 'confirmed',
    isRead: true,
    confirmedAt: '2026-04-25T15:30:00',
    detail: {
      baseSalary: 5000,
      overtimePay: 1200,
      bonus: 200,
      allowance: 200,
      deductions: [
        { name: '养老保险', amount: 122, category: 'social' },
        { name: '医疗保险', amount: 61, category: 'social' },
        { name: '失业保险', amount: 31, category: 'social' },
        { name: '住房公积金', amount: 210, category: 'social' },
        { name: '个人所得税', amount: 72, category: 'tax' },
      ],
      totalDeduction: 496,
      grossPay: 6600,
      netPay: 6104,
      workDays: 21,
      overtimeHours: 12,
    },
  },
  {
    id: 'sal-003',
    yearMonth: '2026-03',
    periodLabel: '2026年3月',
    grossAmount: 6100,
    netAmount: 5674,
    status: 'confirmed',
    isRead: true,
    confirmedAt: '2026-03-27T09:00:00',
    detail: {
      baseSalary: 4800,
      overtimePay: 600,
      bonus: 500,
      allowance: 200,
      deductions: [
        { name: '养老保险', amount: 107, category: 'social' },
        { name: '医疗保险', amount: 54, category: 'social' },
        { name: '失业保险', amount: 27, category: 'social' },
        { name: '住房公积金', amount: 183, category: 'social' },
        { name: '个人所得税', amount: 55, category: 'tax' },
      ],
      totalDeduction: 426,
      grossPay: 6100,
      netPay: 5674,
      workDays: 20,
      overtimeHours: 6,
    },
  },
  {
    id: 'sal-004',
    yearMonth: '2026-06',
    periodLabel: '2026年6月',
    grossAmount: 0,
    netAmount: 0,
    status: 'draft',
    isRead: false,
  },
]

export const salaryApi = {
  async getPayslips(params: { page: number; size: number }): Promise<PageResult<PayslipItem>> {
    await mockDelay()
    const start = (params.page - 1) * params.size
    const records = mockPayslips.slice(start, start + params.size)
    return { records, total: mockPayslips.length, current: params.page, size: params.size }
  },

  async getPayslipDetail(id: string): Promise<PayslipItem> {
    await mockDelay()
    const item = mockPayslips.find(p => p.id === id)
    if (!item) throw new Error('工资条不存在')
    return { ...item }
  },

  async confirmPayslip(id: string): Promise<void> {
    await mockDelay(100, 300)
    const item = mockPayslips.find(p => p.id === id)
    if (!item) throw new Error('工资条不存在')

    // 状态校验：非 draft 状态不可确认
    if (item.status !== 'draft') {
      throw new Error('当前状态不可确认')
    }

    // 金额校验：金额为0不可确认
    if (!item.detail || item.detail.grossPay <= 0) {
      throw new Error('工资数据不完整，无法确认')
    }

    item.status = 'confirmed'
    item.isRead = true
    item.confirmedAt = new Date().toISOString()
  },

  async disputePayslip(id: string, reason: string): Promise<void> {
    await mockDelay(200, 500)
    const item = mockPayslips.find(p => p.id === id)
    if (!item) throw new Error('工资条不存在')
    if (item.status !== 'confirmed') throw new Error('只能对已确认的工资条提出异议')
    item.status = 'disputed'
    item.disputeReason = reason
  },
}