import type {
  ApprovalItem,
  ApprovalDetail,
  ApprovalTab,
  ApprovalFormData,
  ApprovalType,
  ApprovalStatus,
  FlowNode,
} from '@/types/approval'
import { ApprovalTypeLabels } from '@/types/approval'
import { UserLevel } from '@/types/permission'
import { mockDelay, getApproval, getApprovalsByTab, setApproval, getApprovalStats, seedApprovalData } from './mock'
import type { ApprovalStats } from './mock/db'
import { useAuthContext } from '@/composables/useAuthContext'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

/** 获取 AuthContext 单例 */
const auth = useAuthContext()

export const approvalApi = {
  /** 获取审批列表 */
  async getList(params: { tab: ApprovalTab; page: number; size: number; keyword?: string }): Promise<PageResult<ApprovalItem>> {
    seedApprovalData()
    await mockDelay()

    // 从 AuthContext 获取身份信息，统一数据隔离入口
    let sourceData = getApprovalsByTab(params.tab, '', {
      userLevel: auth.userLevel.value,
      currentUserName: auth.userName.value,
    })

    if (params.keyword) {
      const keyword = params.keyword.toLowerCase()
      sourceData = sourceData.filter(
        (item) =>
          item.title.toLowerCase().includes(keyword) ||
          item.applicantName.toLowerCase().includes(keyword) ||
          item.summary.toLowerCase().includes(keyword),
      )
    }

    const total = sourceData.length
    const start = (params.page - 1) * params.size
    const records = sourceData.slice(start, start + params.size)

    return { records, total, current: params.page, size: params.size }
  },

  /** 获取审批详情 */
  async getDetail(id: string): Promise<ApprovalDetail> {
    seedApprovalData()
    await mockDelay()

    const detail = getApproval(id)
    if (!detail) {
      throw new Error('审批记录不存在')
    }
    return detail
  },

  /** 发起审批 */
  async create(data: ApprovalFormData): Promise<ApprovalDetail> {
    // [M9] 校验审批类型是否合法
    const validTypes: ApprovalType[] = ['leave', 'overtime', 'reimbursement', 'travel', 'swap', 'requisition']
    if (!validTypes.includes(data.type)) {
      throw new Error(`不支持的审批类型: ${data.type}`)
    }

    seedApprovalData()
    await mockDelay(200, 600)

    const now = new Date().toISOString()
    const timestamp = Date.now()
    const id = `ap${timestamp}`
    const approvalNo = `AP${now.replace(/[-:T]/g, '').slice(0, 8)}${String(Math.floor(Math.random() * 1000)).padStart(3, '0')}`

    const title = `${ApprovalTypeLabels[data.type]}`
    // 防御性编程：fields 可能为 undefined/null，确保始终为对象
    const fields = (data.fields && typeof data.fields === 'object') ? data.fields as Record<string, unknown> : {}

    const formFields = Object.entries(fields).map(([key, value]) => ({
      label: key,
      value: String(value ?? ''),
      type: 'text' as const,
    }))

    const newDetail: ApprovalDetail = {
      id,
      type: data.type,
      title,
      applicantName: auth.userName.value,
      summary: `${ApprovalTypeLabels[data.type]}申请`,
      status: 'pending',
      createdAt: now,
      urgency: 'normal',
      approvalNo,
      applicantLevel: 1,
      formFields,
      attachments: [],
      flowNodes: [
        { id: 'fn1', role: '发起人', userName: auth.userName.value, status: 'completed', action: '提交申请', time: now.slice(5, 16).replace('T', ' ') },
        { id: 'fn2', role: '门店店长', userName: '王建国', status: 'current' },
        { id: 'fn3', role: '人事审批', userName: '陈美华', status: 'pending' },
      ],
      comments: [],
    }

    setApproval(id, newDetail)
    return newDetail
  },

  /** 审批操作（通过/驳回/转交/催办） */
  async action(
    id: string,
    action: 'approve' | 'reject' | 'transfer' | 'urge',
    data?: { comment?: string; transferTo?: string },
  ): Promise<void> {
    await mockDelay(100, 300)

    const detail = getApproval(id)
    if (!detail) throw new Error('审批记录不存在')

    const now = new Date()
    const timeStr = now.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })

    const updatedNodes: FlowNode[] = detail.flowNodes.map((n) => {
      if (n.status === 'current') {
        if (action === 'approve') {
          return { ...n, status: 'completed', action: '同意', time: timeStr }
        }
        if (action === 'reject') {
          return { ...n, status: 'completed', action: '驳回', time: timeStr }
        }
        // [M8] transfer：标记当前节点为"转交至 {target}"
        if (action === 'transfer') {
          const targetName = data?.transferTo || '未知审批人'
          return { ...n, status: 'completed', action: `转交至 ${targetName}`, time: timeStr }
        }
        // [M8] urge：不改变当前节点状态（催办仅记录，不推进流程）
        return n
      }
      if (n.status === 'pending') {
        if (action === 'reject') {
          return { ...n, status: 'completed', action: '无需处理', time: timeStr }
        }
        // [M8] transfer 时将第一个 pending 节点推进为 current（转交目标接手）
        if (action === 'transfer') {
          // 仅在第一次遇到 pending 时推进，后续 pending 保持不变
          return n
        }
        // approve 时 pending 节点保持不变，由下面的逻辑推进
        return n
      }
      return n
    })

    // approve 时：将第一个 pending 节点推进为 current
    if (action === 'approve') {
      const firstPendingIdx = updatedNodes.findIndex(n => n.status === 'pending')
      if (firstPendingIdx >= 0) {
        updatedNodes[firstPendingIdx] = { ...updatedNodes[firstPendingIdx], status: 'current' }
      }
    }

    // [M8] transfer 时：将第一个 pending 节点推进为 current（转交目标成为当前审批人）
    if (action === 'transfer') {
      const firstPendingIdx = updatedNodes.findIndex(n => n.status === 'pending')
      if (firstPendingIdx >= 0) {
        updatedNodes[firstPendingIdx] = { ...updatedNodes[firstPendingIdx], status: 'current' }
      }
    }

    // 如果没有 pending 节点了，说明审批链走完，状态为 approved
    const hasPending = updatedNodes.some(n => n.status === 'pending' || n.status === 'current')
    const newStatus: ApprovalStatus = action === 'reject' ? 'rejected' : (hasPending ? 'processing' : 'approved')
    const comments = [...detail.comments]
    if (data?.comment) {
      comments.push({
        id: `c${Date.now()}`,
        userId: 'u-reviewer',
        userName: '审批人',
        content: data.comment,
        createdAt: now.toISOString(),
      })
    }

    // [M8] transfer：添加转交记录到评论区
    if (action === 'transfer') {
      const targetName = data?.transferTo || '未知审批人'
      comments.push({
        id: `c${Date.now()}_transfer`,
        userId: 'u-system',
        userName: '系统',
        content: `已转交至 ${targetName}`,
        createdAt: now.toISOString(),
      })
    }

    // [M8] urge：添加催办记录到评论区（不改变流程状态）
    if (action === 'urge') {
      comments.push({
        id: `c${Date.now()}_urge`,
        userId: 'u-system',
        userName: '系统',
        content: data?.comment || '催办提醒：请尽快处理此审批',
        createdAt: now.toISOString(),
      })
    }

    const updatedDetail: ApprovalDetail = {
      ...detail,
      status: newStatus,
      flowNodes: updatedNodes,
      comments,
    }

    setApproval(id, updatedDetail)
  },

  /** 撤回审批 */
  async withdraw(id: string): Promise<void> {
    await mockDelay(80, 200)

    const detail = getApproval(id)
    if (!detail) throw new Error('审批记录不存在')

    // 权限校验：只有申请人本人可以撤回
    if (detail.applicantName !== auth.userName.value) {
      throw new Error('只能撤回自己发起的审批')
    }

    // 状态校验：只有 pending 或 processing 状态的审批可以撤回
    if (detail.status !== 'pending' && detail.status !== 'processing') {
      throw new Error('当前状态不可撤回')
    }

    const now = new Date()
    const timeStr = now.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })

    const updatedNodes: FlowNode[] = detail.flowNodes.map(n => {
      if (n.status === 'current' || n.status === 'pending') {
        return { ...n, status: 'completed', action: '已撤回', time: timeStr }
      }
      return n
    })

    const updatedDetail: ApprovalDetail = {
      ...detail,
      status: 'cancelled',
      flowNodes: updatedNodes,
    }

    setApproval(id, updatedDetail)
  },

  /** 获取个人审批统计数据 */
  async getStats(): Promise<ApprovalStats> {
    seedApprovalData()
    await mockDelay(30, 100)
    return getApprovalStats(auth.userName.value)
  },
}