/**
 * 共享响应式内存数据库
 *
 * 所有Mock API共享此单例，确保：
 * 1. 创建审批 → 审批列表立即可见
 * 2. 审批操作 → 详情数据同步更新
 * 3. 跨页面数据一致性
 */
import { reactive } from 'vue'
import type { ApprovalItem, ApprovalDetail, ApprovalStatus, ApprovalType, ApprovalTab } from '@/types/approval'

export interface MockDatabase {
  approvals: Record<string, ApprovalDetail>
  tombstone: string[]
}

import { UserLevel } from '@/types/permission'

const STORAGE_KEY = 'employee_mock_db'

function loadFromStorage(): MockDatabase {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const parsed = JSON.parse(raw)
      // 结构校验：防止 localStorage 数据损坏导致运行时崩溃
      const approvals = (parsed && typeof parsed.approvals === 'object' && !Array.isArray(parsed.approvals))
        ? parsed.approvals
        : {}
      const tombstone = (parsed && Array.isArray(parsed.tombstone))
        ? parsed.tombstone
        : []
      return { approvals, tombstone }
    }
  } catch {
    // 忽略解析错误，使用默认空数据库
  }
  return { approvals: {}, tombstone: [] }
}

function saveToStorage(state: MockDatabase): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ approvals: state.approvals, tombstone: state.tombstone }))
  } catch {
    // 忽略存储错误
  }
}

function createInitialState(): MockDatabase {
  return { approvals: {}, tombstone: [] }
}

// [M7] 使用 loadFromStorage() 初始化，确保 localStorage 中已有数据不会丢失
// createInitialState() 返回空对象会忽略已持久化的 Mock 数据
const db = reactive<MockDatabase>(loadFromStorage())

export function getDb(): MockDatabase {
  return db
}

export function setApproval(id: string, detail: ApprovalDetail): void {
  db.approvals[id] = { ...detail }
  saveToStorage(db)
}

export function removeApproval(id: string): void {
  delete db.approvals[id]
  if (!db.tombstone.includes(id)) {
    db.tombstone.push(id)
  }
  saveToStorage(db)
}

export function updateApprovalStatus(id: string, status: ApprovalStatus): void {
  if (db.approvals[id]) {
    db.approvals[id] = { ...db.approvals[id], status }
    saveToStorage(db)
  }
}

export function getApproval(id: string): ApprovalDetail | undefined {
  return db.approvals[id] ? { ...db.approvals[id] } : undefined
}

export function getAllApprovals(): ApprovalDetail[] {
  return Object.values(db.approvals).map(a => ({ ...a }))
}

export function getApprovalsByTab(
  tab: ApprovalTab,
  currentUserRole: string,
  options?: { userLevel?: UserLevel; currentUserName?: string },
): ApprovalItem[] {
  const { userLevel, currentUserName } = options || {}
  const all = getAllApprovals()
    .filter(a => !db.tombstone.includes(a.id))
    .map(toApprovalItem)

  switch (tab) {
    case 'pending':
      return all.filter(a => a.status === 'pending' || a.status === 'processing')
    case 'initiated':
      return all.filter(a => a.applicantName === (currentUserName ?? ''))
    case 'cc':
      return all.filter(a => a.applicantName !== (currentUserName ?? '') && a.status !== 'cancelled')
    case 'approved':
      return all.filter(a => a.status === 'approved')
    case 'rejected':
      return all.filter(a => a.status === 'rejected')
    case 'completed': {
      // 角色分级数据隔离：
      // L1/L2 员工/主管：只能看到自己的已完成记录
      // L3+ 经理及以上：可查看团队全员已完成记录（管理 oversight）
      const completed = all.filter(a =>
        a.status === 'approved' || a.status === 'rejected' || a.status === 'cancelled'
      )
      if (userLevel !== undefined && userLevel < UserLevel.MANAGER && currentUserName) {
        return completed.filter(a => a.applicantName === currentUserName)
      }
      return completed
    }
    case 'role':
      return all
    default:
      return all
  }
}

export interface ApprovalStats {
  totalApplications: number
  approvedCount: number
  rejectedCount: number
  cancelledCount: number
  pendingCount: number
  passRate: number
  leaveDaysUsed: number
  leaveDaysTotal: number
  reimbursementTotal: number
  overtimeHours: number
  travelCount: number
  swapCount: number
}

export function getApprovalStats(currentUserName?: string): ApprovalStats {
  const all = getAllApprovals().filter(a => !db.tombstone.includes(a.id))
  const myRecords = all.filter(a => a.applicantName === (currentUserName ?? ''))
  const approved = myRecords.filter(a => a.status === 'approved').length
  const rejected = myRecords.filter(a => a.status === 'rejected').length
  const cancelled = myRecords.filter(a => a.status === 'cancelled').length
  const pending = myRecords.filter(a => a.status === 'pending' || a.status === 'processing').length
  const completed = approved + rejected + cancelled

  // [M4] 从实际审批记录动态计算统计值，替代硬编码数据
  // 按类型分类汇总，从 formFields 中提取天数/金额等字段（带防御性处理）
  const leaveApprovals = myRecords.filter(a => a.type === 'leave')
  const reimbursementApprovals = myRecords.filter(a => a.type === 'reimbursement')
  const overtimeApprovals = myRecords.filter(a => a.type === 'overtime')
  const travelApprovals = myRecords.filter(a => a.type === 'travel')
  const swapApprovals = myRecords.filter(a => a.type === 'swap')

  // 从 formFields 中尝试提取请假天数（找不到则按每条记录1天估算）
  const leaveDaysUsed = leaveApprovals.length > 0
    ? leaveApprovals.reduce((sum, a) => {
        const daysField = a.formFields?.find(f =>
          f.label?.includes('天') || f.label?.includes('天数') || f.label?.includes('时长')
        )
        return sum + (daysField ? parseFloat(String(daysField.value)) || 1 : 1)
      }, 0)
    : 0

  // 报销金额：从 formFields 提取或按每条记录估算
  const reimbursementTotal = reimbursementApprovals.length > 0
    ? reimbursementApprovals.reduce((sum, a) => {
        const amountField = a.formFields?.find(f =>
          f.label?.includes('金额') || f.label?.includes('费用') || f.label?.includes('元')
        )
        return sum + (amountField ? parseFloat(String(amountField.value)) || 500 : 500)
      }, 0)
    : 0

  // 加班时长：从 formFields 提取或按每条2小时估算
  const overtimeHours = overtimeApprovals.length > 0
    ? overtimeApprovals.reduce((sum, a) => {
        const hoursField = a.formFields?.find(f =>
          f.label?.includes('小时') || f.label?.includes('时长')
        )
        return sum + (hoursField ? parseFloat(String(hoursField.value)) || 2 : 2)
      }, 0)
    : 0

  return {
    totalApplications: myRecords.length,
    approvedCount: approved,
    rejectedCount: rejected,
    cancelledCount: cancelled,
    pendingCount: pending,
    passRate: completed > 0 ? Math.round((approved / completed) * 100) : 100,
    leaveDaysUsed,
    leaveDaysTotal: Math.max(leaveDaysUsed * 2.5, 13), // 基于已用天数估算总额度
    reimbursementTotal,
    overtimeHours,
    travelCount: travelApprovals.length,
    swapCount: swapApprovals.length,
  }
}

function toApprovalItem(detail: ApprovalDetail): ApprovalItem {
  return {
    id: detail.id,
    type: detail.type,
    title: detail.title,
    applicantName: detail.applicantName,
    applicantAvatar: detail.applicantAvatar,
    summary: detail.summary,
    status: detail.status,
    createdAt: detail.createdAt,
    urgency: detail.urgency,
  }
}

export function resetDb(): void {
  db.approvals = {}
  db.tombstone = []
  localStorage.removeItem(STORAGE_KEY)
}

export function isDbEmpty(): boolean {
  return Object.keys(db.approvals).length === 0
}