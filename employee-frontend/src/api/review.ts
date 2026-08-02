import type { ReviewItem, ReviewDetail, ReviewScore, ReviewHistoryNode, ReviewStatus } from '@/types/review'
import { mockDelay } from './mock'

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
}

const mockReviews: ReviewDetail[] = [
  {
    id: 'rv001',
    period: 'monthly',
    periodLabel: '2026年5月',
    status: 'completed',
    totalScore: 87.5,
    grade: 'A',
    reviewerName: '王建国',
    createdAt: '2026-05-01T00:00:00',
    completedAt: '2026-05-08T16:30:00',
    deadline: null,
    scores: [
      { dimension: 'attendance', selfScore: 95, managerScore: 95, maxScore: 100, weight: 20, comment: '全勤，无迟到早退' },
      { dimension: 'work_quality', selfScore: 85, managerScore: 82, maxScore: 100, weight: 30, comment: '菜品品质稳定，偶有摆盘不规范' },
      { dimension: 'teamwork', selfScore: 90, managerScore: 88, maxScore: 100, weight: 15, comment: '配合度高，主动协助新人' },
      { dimension: 'customer_service', selfScore: 85, managerScore: 90, maxScore: 100, weight: 15, comment: '顾客反馈良好' },
      { dimension: 'efficiency', selfScore: 80, managerScore: 85, maxScore: 100, weight: 10, comment: '出餐速度达标' },
      { dimension: 'professional_skill', selfScore: 85, managerScore: 85, maxScore: 100, weight: 10, comment: '刀工和火候掌握良好' },
    ],
    managerComment: '整体表现良好，出勤率高，工作态度端正。建议在菜品摆盘方面加强练习。',
    hrComment: '考核结果公正，同意评定。继续保持。',
    selfComment: '本月工作认真负责，按时完成所有任务。团队协作良好。',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h1', action: '考核发布', operator: '系统', time: '2026-05-01 09:00', detail: '5月月度考核已发布' },
      { id: 'h2', action: '员工自评', operator: '我', time: '2026-05-02 14:30', detail: '完成自评打分和说明' },
      { id: 'h3', action: '主管评定', operator: '王建国', time: '2026-05-05 10:15', detail: '完成主管评分和评语' },
      { id: 'h4', action: 'HR审核', operator: '陈美华', time: '2026-05-08 16:30', detail: 'HR审核通过，考核结果确定' },
    ],
  },
  {
    id: 'rv002',
    period: 'monthly',
    periodLabel: '2026年4月',
    status: 'completed',
    totalScore: 91.0,
    grade: 'A',
    reviewerName: '王建国',
    createdAt: '2026-04-01T00:00:00',
    completedAt: '2026-04-07T15:00:00',
    deadline: null,
    scores: [
      { dimension: 'attendance', selfScore: 100, managerScore: 100, maxScore: 100, weight: 20, comment: '全勤' },
      { dimension: 'work_quality', selfScore: 88, managerScore: 90, maxScore: 100, weight: 30, comment: '表现优秀' },
      { dimension: 'teamwork', selfScore: 90, managerScore: 92, maxScore: 100, weight: 15, comment: '团队协作能力强' },
      { dimension: 'customer_service', selfScore: 88, managerScore: 90, maxScore: 100, weight: 15, comment: '服务态度好' },
      { dimension: 'efficiency', selfScore: 85, managerScore: 88, maxScore: 100, weight: 10, comment: '效率较高' },
      { dimension: 'professional_skill', selfScore: 85, managerScore: 86, maxScore: 100, weight: 10, comment: '技能稳定' },
    ],
    managerComment: '本月表现稳定，继续保持。',
    hrComment: '同意评定结果。',
    selfComment: '认真完成本职工作。',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h5', action: '考核发布', operator: '系统', time: '2026-04-01 09:00', detail: '4月月度考核已发布' },
      { id: 'h6', action: '员工自评', operator: '我', time: '2026-04-02 11:00', detail: '完成自评' },
      { id: 'h7', action: '主管评定', operator: '王建国', time: '2026-04-05 09:30', detail: '完成主管评定' },
      { id: 'h8', action: 'HR审核', operator: '陈美华', time: '2026-04-07 15:00', detail: 'HR审核通过' },
    ],
  },
  {
    id: 'rv003',
    period: 'monthly',
    periodLabel: '2026年3月',
    status: 'completed',
    totalScore: 78.0,
    grade: 'B',
    reviewerName: '王建国',
    createdAt: '2026-03-01T00:00:00',
    completedAt: '2026-03-10T17:00:00',
    deadline: null,
    scores: [
      { dimension: 'attendance', selfScore: 80, managerScore: 75, maxScore: 100, weight: 20, comment: '有1次迟到记录' },
      { dimension: 'work_quality', selfScore: 82, managerScore: 78, maxScore: 100, weight: 30, comment: '质量有所下滑' },
      { dimension: 'teamwork', selfScore: 85, managerScore: 80, maxScore: 100, weight: 15, comment: '需要更多配合' },
      { dimension: 'customer_service', selfScore: 80, managerScore: 78, maxScore: 100, weight: 15, comment: '客户反馈一般' },
      { dimension: 'efficiency', selfScore: 75, managerScore: 78, maxScore: 100, weight: 10, comment: '效率待提升' },
      { dimension: 'professional_skill', selfScore: 80, managerScore: 80, maxScore: 100, weight: 10, comment: '技能达标' },
    ],
    managerComment: '本月迟到一次，工作状态有所下滑，请调整。',
    hrComment: '建议加强管理，改进工作状态。',
    selfComment: '本月因为搬家影响了状态，下月会调整。',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h9', action: '考核发布', operator: '系统', time: '2026-03-01 09:00', detail: '3月月度考核已发布' },
      { id: 'h10', action: '员工自评', operator: '我', time: '2026-03-03 10:00', detail: '完成自评' },
      { id: 'h11', action: '主管评定', operator: '王建国', time: '2026-03-07 14:00', detail: '完成主管评定' },
      { id: 'h12', action: 'HR审核', operator: '陈美华', time: '2026-03-10 17:00', detail: 'HR审核通过' },
    ],
  },
  {
    id: 'rv004',
    period: 'quarterly',
    periodLabel: '2026年Q1',
    status: 'completed',
    totalScore: 85.5,
    grade: 'A',
    reviewerName: '王建国',
    createdAt: '2026-01-01T00:00:00',
    completedAt: '2026-01-20T11:00:00',
    deadline: null,
    scores: [
      { dimension: 'attendance', selfScore: 90, managerScore: 92, maxScore: 100, weight: 20, comment: 'Q1出勤良好' },
      { dimension: 'work_quality', selfScore: 85, managerScore: 84, maxScore: 100, weight: 30, comment: '质量稳步提升' },
      { dimension: 'teamwork', selfScore: 88, managerScore: 86, maxScore: 100, weight: 15, comment: '协作积极' },
      { dimension: 'customer_service', selfScore: 85, managerScore: 85, maxScore: 100, weight: 15, comment: '服务水平稳定' },
      { dimension: 'efficiency', selfScore: 82, managerScore: 82, maxScore: 100, weight: 10, comment: '效率正常' },
      { dimension: 'professional_skill', selfScore: 83, managerScore: 84, maxScore: 100, weight: 10, comment: '技能持续提升' },
    ],
    managerComment: 'Q1整体表现良好，进步明显。',
    hrComment: 'Q1考核结果确认。',
    selfComment: 'Q1学到了很多新技能，会继续努力。',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h13', action: '考核发布', operator: '系统', time: '2026-01-01 09:00', detail: 'Q1季度考核已发布' },
      { id: 'h14', action: '员工自评', operator: '我', time: '2026-01-10 09:00', detail: '完成Q1自评' },
      { id: 'h15', action: '主管评定', operator: '王建国', time: '2026-01-15 16:00', detail: '完成Q1主管评定' },
      { id: 'h16', action: 'HR审核', operator: '陈美华', time: '2026-01-20 11:00', detail: 'Q1考核完成' },
    ],
  },
  {
    id: 'rv005',
    period: 'monthly',
    periodLabel: '2026年6月',
    status: 'self_review',
    totalScore: null,
    grade: null,
    reviewerName: '王建国',
    createdAt: '2026-06-01T00:00:00',
    completedAt: null,
    deadline: '2026-06-07T23:59:59',
    scores: [
      { dimension: 'attendance', selfScore: null, managerScore: null, maxScore: 100, weight: 20, comment: '' },
      { dimension: 'work_quality', selfScore: null, managerScore: null, maxScore: 100, weight: 30, comment: '' },
      { dimension: 'teamwork', selfScore: null, managerScore: null, maxScore: 100, weight: 15, comment: '' },
      { dimension: 'customer_service', selfScore: null, managerScore: null, maxScore: 100, weight: 15, comment: '' },
      { dimension: 'efficiency', selfScore: null, managerScore: null, maxScore: 100, weight: 10, comment: '' },
      { dimension: 'professional_skill', selfScore: null, managerScore: null, maxScore: 100, weight: 10, comment: '' },
    ],
    managerComment: '',
    hrComment: '',
    selfComment: '',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h17', action: '考核发布', operator: '系统', time: '2026-06-01 09:00', detail: '6月月度考核已发布，请在6月5日前完成自评' },
    ],
  },
  {
    id: 'rv006',
    period: 'monthly',
    periodLabel: '2026年5月（补评）',
    status: 'manager_review',
    totalScore: null,
    grade: null,
    reviewerName: '王建国',
    createdAt: '2026-05-20T00:00:00',
    completedAt: null,
    deadline: '2026-06-10T23:59:59',
    scores: [
      { dimension: 'attendance', selfScore: 90, managerScore: null, maxScore: 100, weight: 20, comment: '' },
      { dimension: 'work_quality', selfScore: 85, managerScore: null, maxScore: 100, weight: 30, comment: '' },
      { dimension: 'teamwork', selfScore: 88, managerScore: null, maxScore: 100, weight: 15, comment: '' },
      { dimension: 'customer_service', selfScore: 82, managerScore: null, maxScore: 100, weight: 15, comment: '' },
      { dimension: 'efficiency', selfScore: 80, managerScore: null, maxScore: 100, weight: 10, comment: '' },
      { dimension: 'professional_skill', selfScore: 85, managerScore: null, maxScore: 100, weight: 10, comment: '' },
    ],
    managerComment: '',
    hrComment: '',
    selfComment: '5月补评自评已完成，等待主管评定。',
    appealReason: null,
    appealResult: null,
    history: [
      { id: 'h18', action: '考核发布', operator: '系统', time: '2026-05-20 09:00', detail: '5月补评考核已发布' },
      { id: 'h19', action: '员工自评', operator: '我', time: '2026-05-22 14:00', detail: '完成自评打分和说明' },
    ],
  },
]

export const reviewApi = {
  async getList(params: { page: number; size: number }): Promise<PageResult<ReviewItem>> {
    await mockDelay()
    const items: ReviewItem[] = mockReviews.map(r => ({
      id: r.id,
      period: r.period,
      periodLabel: r.periodLabel,
      status: r.status,
      totalScore: r.totalScore,
      grade: r.grade,
      reviewerName: r.reviewerName,
      createdAt: r.createdAt,
      completedAt: r.completedAt,
      deadline: r.deadline || null,
    }))
    const start = (params.page - 1) * params.size
    const records = items.slice(start, start + params.size)
    return { records, total: items.length, current: params.page, size: params.size }
  },

  async getDetail(id: string): Promise<ReviewDetail> {
    await mockDelay()
    const detail = mockReviews.find(r => r.id === id)
    if (!detail) throw new Error('考核记录不存在')
    return { ...detail }
  },

  async submitSelfReview(id: string, data: { scores: { dimension: string; score: number; comment: string }[]; comment: string }): Promise<void> {
    await mockDelay(200, 500)
    const detail = mockReviews.find(r => r.id === id)
    if (!detail) throw new Error('考核记录不存在')
    detail.scores.forEach(s => {
      const submitted = data.scores.find(d => d.dimension === s.dimension)
      if (submitted) {
        s.selfScore = submitted.score
        s.comment = submitted.comment
      }
    })
    detail.selfComment = data.comment
    detail.status = 'manager_review'
    detail.history.push({
      id: `h${Date.now()}`,
      action: '员工自评',
      operator: '我',
      time: new Date().toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }),
      detail: '完成自评打分',
    })
  },

  async submitAppeal(id: string, reason: string): Promise<void> {
    await mockDelay(200, 500)
    const detail = mockReviews.find(r => r.id === id)
    if (!detail) throw new Error('考核记录不存在')
    detail.status = 'appealing'
    detail.appealReason = reason
    detail.appealResult = null
    detail.history.push({
      id: `h${Date.now()}`,
      action: '提交申诉',
      operator: '我',
      time: new Date().toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }),
      detail: `申诉原因：${reason}`,
    })
  },
}