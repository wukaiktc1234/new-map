/**
 * 知识库管理类型定义
 * 对接员工端(employee-frontend)知识库模块，提供管理端CRUD能力
 */

/** 知识库分类 */
export type KnowledgeCategory = 'safety' | 'service' | 'manual' | 'policy'

/** 知识库分类选项 */
export const KnowledgeCategoryOptions: { value: KnowledgeCategory; label: string; icon: string }[] = [
  { value: 'safety', label: '安全规范', icon: 'Shield' },
  { value: 'service', label: '服务标准', icon: 'Service' },
  { value: 'manual', label: '操作手册', icon: 'Document' },
  { value: 'policy', label: '公司制度', icon: 'Notebook' },
]

/** 文章发布状态 */
export type ArticlePublishStatus =
  | 'draft'              // 草稿
  | 'pending_review'     // 待部门审核
  | 'pending_final'      // 待店长终审（仅重要内容）
  | 'published'          // 已发布
  | 'archived'           // 已归档
  | 'rejected'           // 已退回

/** 文章发布状态选项 */
export const ArticlePublishStatusOptions: { value: ArticlePublishStatus; label: string }[] = [
  { value: 'draft', label: '草稿' },
  { value: 'pending_review', label: '待部门审核' },
  { value: 'pending_final', label: '待店长终审' },
  { value: 'published', label: '已发布' },
  { value: 'archived', label: '已归档' },
  { value: 'rejected', label: '已退回' },
]

/** 文章发布状态到StatusTag映射 */
export const ArticlePublishStatusTagMap: Record<ArticlePublishStatus, string> = {
  draft: 'default',
  pending_review: 'pending',
  pending_final: 'warning',
  published: 'active',
  archived: 'inactive',
  rejected: 'error',
}

/** 测验题目难度 */
export type QuizDifficulty = 'normal' | 'required'

/** 测验选项 */
export interface QuizOption {
  id: string      // A/B/C/D
  text: string
}

/** 测验题目 */
export interface QuizQuestion {
  id: string
  question: string
  options: QuizOption[]
  correctAnswerId: string
  /** 普通题/必会题（一票否决） */
  difficulty: QuizDifficulty
}

/** 知识库文章（管理端版本） */
export interface KnowledgeArticle {
  id: string
  title: string
  summary: string
  category: KnowledgeCategory
  icon?: string
  /** Markdown格式内容 */
  content: string
  coverUrl?: string
  author?: string
  /** 发布状态 */
  publishStatus: ArticlePublishStatus
  /** 阅读次数 */
  viewCount: number
  tags?: string[]
  /** 关联测验题目 */
  quiz?: QuizQuestion[]
  publishTime?: string
  updateTime?: string
  createTime?: string

  /* ===== 审核流程字段（新增） ===== */
  /** 作者ID */
  authorId?: string
  /** 作者所属部门 */
  authorDepartment?: string
  /** 审核人ID */
  reviewerId?: string
  /** 审核人姓名 */
  reviewerName?: string
  /** 审核时间 */
  reviewedAt?: string
  /** 审核意见 */
  reviewComment?: string
  /** 退回原因 */
  rejectReason?: string
  /** 发布人ID */
  publishedBy?: string
  /** 是否为重要内容（需店长终审） */
  isImportant?: boolean
  /** 版本号 */
  version?: number
}

/** 知识库文章表单数据 */
export interface KnowledgeArticleFormData {
  title: string
  summary: string
  category: KnowledgeCategory
  icon?: string
  content: string
  coverUrl?: string
  author?: string
  publishStatus: ArticlePublishStatus
  tags?: string[]
  quiz?: QuizQuestion[]
  /** 是否为重要内容（需店长终审） */
  isImportant?: boolean
  /** 作者所属部门 */
  authorDepartment?: string
}

/** 知识库查询参数 */
export interface KnowledgeArticleQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  category?: KnowledgeCategory
  publishStatus?: ArticlePublishStatus
}

/** 知识库分类统计 */
export interface KnowledgeCategoryStat {
  category: KnowledgeCategory
  label: string
  count: number
  publishedCount: number
}

/* ============================================================
 * 员工端学习记录类型（对接 employee-frontend 学习数据）
 * ============================================================ */

/** 学习状态 */
export type StudyStatus = 'not_started' | 'in_progress' | 'completed'

/** 学习状态选项 */
export const StudyStatusOptions: { value: StudyStatus; label: string }[] = [
  { value: 'not_started', label: '未开始' },
  { value: 'in_progress', label: '学习中' },
  { value: 'completed', label: '已完成' },
]

/** 学习状态到StatusTag映射 */
export const StudyStatusTagMap: Record<StudyStatus, string> = {
  not_started: 'default',
  in_progress: 'warning',
  completed: 'success',
}

/** 测验通过状态 */
export type QuizPassStatus = 'not_attempted' | 'passed' | 'failed'

/** 测验通过状态选项 */
export const QuizPassStatusOptions: { value: QuizPassStatus; label: string }[] = [
  { value: 'not_attempted', label: '未答题' },
  { value: 'passed', label: '已通过' },
  { value: 'failed', label: '未通过' },
]

/** 测验通过状态到StatusTag映射 */
export const QuizPassStatusTagMap: Record<QuizPassStatus, string> = {
  not_attempted: 'default',
  passed: 'success',
  failed: 'error',
}

/** 员工学习记录（单条记录表示某员工对某文章的学习情况） */
export interface ArticleStudyRecord {
  id: string
  /** 文章ID */
  articleId: string
  /** 文章标题（冗余字段，便于展示） */
  articleTitle: string
  /** 文章分类 */
  articleCategory: KnowledgeCategory
  /** 员工ID */
  employeeId: string
  /** 员工姓名 */
  employeeName: string
  /** 员工工号 */
  employeeNo: string
  /** 所属门店 */
  storeName: string
  /** 学习状态 */
  studyStatus: StudyStatus
  /** 阅读进度（0-100） */
  readProgress: number
  /** 累计阅读时长（秒） */
  readDuration: number
  /** 最后阅读时间 */
  lastReadTime: string
  /** 首次阅读时间 */
  firstReadTime: string
  /** 测验状态 */
  quizStatus: QuizPassStatus
  /** 测验得分（0-100） */
  quizScore?: number
  /** 测验答题次数 */
  quizAttemptCount: number
  /** 最后测验时间 */
  lastQuizTime?: string
  /** 是否获得证书（关联培训模块） */
  certified: boolean
}

/** 学习记录查询参数 */
export interface StudyRecordQueryParams {
  page?: number
  pageSize?: number
  keyword?: string
  articleId?: string
  employeeId?: string
  studyStatus?: StudyStatus
  quizStatus?: QuizPassStatus
  category?: KnowledgeCategory
}

/** 学习记录统计 */
export interface StudyRecordStatistics {
  /** 总学习记录数 */
  totalRecords: number
  /** 已完成学习人数 */
  completedCount: number
  /** 学习中人数 */
  inProgressCount: number
  /** 测验通过人数 */
  quizPassedCount: number
  /** 平均阅读进度 */
  avgReadProgress: number
  /** 平均测验得分 */
  avgQuizScore: number
  /** 发证数量 */
  certifiedCount: number
}

/* ============================================================
 * 知识库审核流程（多部门联邦式审核）
 * ============================================================ */

/** 审核动作类型 */
export type ReviewAction =
  | 'submit'           // 提交审核
  | 'dept_approve'     // 部门审核通过
  | 'dept_reject'      // 部门审核退回
  | 'final_approve'    // 店长终审通过
  | 'final_reject'     // 店长终审退回
  | 'publish'          // 直接发布（普通内容）
  | 'archive'          // 归档

/** 审核动作选项 */
export const ReviewActionOptions: { value: ReviewAction; label: string }[] = [
  { value: 'submit', label: '提交审核' },
  { value: 'dept_approve', label: '部门审核通过' },
  { value: 'dept_reject', label: '部门审核退回' },
  { value: 'final_approve', label: '店长终审通过' },
  { value: 'final_reject', label: '店长终审退回' },
  { value: 'publish', label: '直接发布' },
  { value: 'archive', label: '归档' },
]

/** 审核操作人角色 */
export type ReviewerRole = 'contributor' | 'dept_manager' | 'hr_manager' | 'super_admin'

/** 审核操作人角色选项 */
export const ReviewerRoleOptions: { value: ReviewerRole; label: string }[] = [
  { value: 'contributor', label: '内容贡献者' },
  { value: 'dept_manager', label: '部门管理员' },
  { value: 'hr_manager', label: '人事管理员' },
  { value: 'super_admin', label: '超级管理员' },
]

/** 审核记录 */
export interface ReviewRecord {
  /** 记录ID */
  recordId: string
  /** 文章ID */
  articleId: string
  /** 审核动作 */
  action: ReviewAction
  /** 操作人ID */
  operatorId: string
  /** 操作人姓名 */
  operatorName: string
  /** 操作人角色 */
  operatorRole: ReviewerRole
  /** 操作时间 */
  createdAt: string
  /** 审核意见 */
  comment?: string
  /** 退回原因（action 为 dept_reject 或 final_reject 时有值） */
  rejectReason?: string
}

/** 审核记录查询参数 */
export interface ReviewRecordQueryParams {
  articleId?: string
  operatorId?: string
  action?: ReviewAction
}

/** 提交审核表单 */
export interface SubmitReviewForm {
  /** 是否为重要内容（决定是否需要店长终审） */
  isImportant: boolean
}

/** 部门审核表单 */
export interface DeptReviewForm {
  /** 审核通过（true）或退回（false） */
  approved: boolean
  /** 审核意见 */
  comment?: string
  /** 退回原因（approved 为 false 时必填） */
  rejectReason?: string
}

/** 店长终审表单 */
export interface FinalReviewForm {
  /** 终审通过（true）或退回（false） */
  approved: boolean
  /** 审核意见 */
  comment?: string
  /** 退回原因（approved 为 false 时必填） */
  rejectReason?: string
}
