export type LeaveType = 'annual' | 'sick' | 'personal' | 'compensatory' | 'maternity' | 'paternity' | 'marriage' | 'bereavement' | 'other'

export const LEAVE_TYPE_LABEL_MAP: Record<LeaveType, string> = {
  annual: '年假',
  sick: '病假',
  personal: '事假',
  compensatory: '调休',
  maternity: '产假',
  paternity: '陪产假',
  marriage: '婚假',
  bereavement: '丧假',
  other: '其他',
}

export type LeaveStatus = 'pending' | 'approved' | 'rejected' | 'cancelled' | 'withdrawn'

export const LEAVE_STATUS_LABEL_MAP: Record<LeaveStatus, string> = {
  pending: '待审批',
  approved: '已通过',
  rejected: '已拒绝',
  cancelled: '已取消',
  withdrawn: '已撤回',
}

export interface LeaveBalance {
  type: LeaveType
  total: number
  used: number
  remaining: number
}

export interface LeaveForm {
  leaveType: LeaveType
  startDate: string
  endDate: string
  days: number
  reason: string
  attachments?: string[]
}

export interface ApprovalNode {
  role: string
  approverName: string
  status: 'pending' | 'approved' | 'rejected'
  actionAt?: string
  comment?: string
}

export interface LeaveRecord {
  id: string
  form: LeaveForm
  status: LeaveStatus
  submittedAt: string
  approvalHistory: ApprovalNode[]
  canWithdraw: boolean
}
