export type ReviewPeriod = 'monthly' | 'quarterly' | 'yearly'
export type ReviewStatus = 'draft' | 'self_review' | 'manager_review' | 'hr_review' | 'completed' | 'appealing' | 'appeal_resolved'
export type ReviewDimension = 'attendance' | 'work_quality' | 'teamwork' | 'customer_service' | 'efficiency' | 'professional_skill'

export const ReviewPeriodLabels: Record<ReviewPeriod, string> = {
  monthly: '月度考核',
  quarterly: '季度考核',
  yearly: '年度考核',
}

export const ReviewStatusLabels: Record<ReviewStatus, string> = {
  draft: '待开启',
  self_review: '待自评',
  manager_review: '主管评定中',
  hr_review: 'HR审核中',
  completed: '已完成',
  appealing: '申诉中',
  appeal_resolved: '申诉已处理',
}

export const ReviewDimensionLabels: Record<ReviewDimension, string> = {
  attendance: '出勤纪律',
  work_quality: '工作质量',
  teamwork: '团队协作',
  customer_service: '客户服务',
  efficiency: '工作效率',
  professional_skill: '专业技能',
}

export interface ReviewScore {
  dimension: ReviewDimension
  selfScore: number | null
  managerScore: number | null
  maxScore: number
  weight: number
  comment: string
}

export interface ReviewItem {
  id: string
  period: ReviewPeriod
  periodLabel: string
  status: ReviewStatus
  totalScore: number | null
  grade: string | null
  reviewerName: string
  createdAt: string
  completedAt: string | null
  /** 截止日期（ISO格式），仅待处理状态有值 */
  deadline: string | null
}

export interface ReviewDetail extends ReviewItem {
  scores: ReviewScore[]
  managerComment: string
  hrComment: string
  selfComment: string
  appealReason: string | null
  appealResult: string | null
  history: ReviewHistoryNode[]
}

export interface ReviewHistoryNode {
  id: string
  action: string
  operator: string
  time: string
  detail: string
}

export const GradeLabels: Record<string, string> = {
  S: '卓越 (S)',
  A: '优秀 (A)',
  B: '良好 (B)',
  C: '合格 (C)',
  D: '待改进 (D)',
}

export const GradeColors: Record<string, string> = {
  S: 'var(--fts-success)',
  A: 'var(--fts-primary)',
  B: 'var(--fts-info)',
  C: 'var(--fts-warning)',
  D: 'var(--fts-error)',
}