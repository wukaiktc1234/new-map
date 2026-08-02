/** 知识库文章分类 */
export type KnowledgeCategory = 'safety' | 'service' | 'manual' | 'policy'

/** 分类显示名称映射 */
export const KNOWLEDGE_CATEGORY_LABELS: Record<KnowledgeCategory, string> = {
  safety: '安全规范',
  service: '服务标准',
  manual: '操作手册',
  policy: '公司制度',
}

/** 随堂测验选项 */
export interface QuizOption {
  /** 选项标识 A/B/C/D */
  id: string
  /** 选项文本 */
  text: string
}

/** 题目难度等级 */
export type QuizDifficulty = 'normal' | 'required'

/** 随堂测验题目 */
export interface QuizQuestion {
  /** 题目ID */
  id: string
  /** 题目文本 */
  question: string
  /** 选项列表（3-4个） */
  options: QuizOption[]
  /** 正确答案的 option id */
  correctAnswerId: string
  /** 难度等级：普通（计入正确率）| 必会（一票否决，错即不通过） */
  difficulty?: QuizDifficulty
}

/** 阅读学习状态（用于培训联动） */
export interface ReadingSession {
  /** 是否在学习模式中 */
  isLearningMode: boolean
  /** 开始阅读时间戳 */
  startedAt: number
  /** 已阅读秒数 */
  readSeconds: number
  /** 最小要求阅读秒数 */
  minRequiredSeconds: number
  /** 是否已满足最小阅读时长 */
  hasMetMinTime: boolean
  /** 测验是否已完成 */
  quizCompleted: boolean
  /** 测验是否通过（正确率>=60%） */
  quizPassed: boolean
  /** 已选择的答案 { questionId: optionId } */
  answers: Record<string, string>
}

/** 知识库文章 */
export interface KnowledgeArticle {
  /** 文章ID */
  id: string
  /** 标题 */
  title: string
  /** 摘要 */
  summary: string
  /** 分类 */
  category: KnowledgeCategory
  /** 图标标识（用于分类图标展示） */
  icon?: string
  /** 内容（Markdown格式） */
  content: string
  /** 封面图URL（可选） */
  coverUrl?: string
  /** 作者（Mock 数据可能缺失） */
  author?: string
  /** 发布时间 ISO 8601（Mock 数据可能缺失） */
  publishTime?: string
  /** 更新时间 ISO 8601（Mock 数据可能缺失） */
  updateTime?: string
  /** 阅读次数（Mock 数据可能缺失） */
  viewCount?: number
  /** 是否已收藏（Mock 数据可能缺失） */
  isFavorited?: boolean
  /** 关联的随堂测验题目（用于培训验证） */
  quiz?: QuizQuestion[]
  /** 标签列表（用于分类筛选和展示） */
  tags?: string[]
}

/** 文章列表查询参数 */
export interface KnowledgeQueryParams {
  /** 关键词搜索 */
  keyword?: string
  /** 分类筛选 */
  category?: KnowledgeCategory | 'all'
  /** 页码（从1开始） */
  page?: number
  /** 每页条数 */
  size?: number
}

/** 文章列表响应 */
export interface KnowledgeListResponse {
  records: KnowledgeArticle[]
  total: number
}
