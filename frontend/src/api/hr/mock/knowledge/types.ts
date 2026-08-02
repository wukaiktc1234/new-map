/**
 * 员工端知识库文章类型定义（从 employee-frontend 同步）
 *
 * 此类型与员工端 @/types/knowledge 保持一致，
 * 用于在管理端直接引用员工端已写好的文章数据。
 */

/** 知识库文章分类 */
export type KnowledgeCategory = 'safety' | 'service' | 'manual' | 'policy'

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

/** 知识库文章（员工端原始结构） */
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
  /** 作者 */
  author?: string
  /** 发布时间 ISO 8601 */
  publishTime?: string
  /** 更新时间 ISO 8601 */
  updateTime?: string
  /** 阅读次数 */
  viewCount?: number
  /** 是否已收藏 */
  isFavorited?: boolean
  /** 关联的随堂测验题目（用于培训验证） */
  quiz?: QuizQuestion[]
  /** 标签列表（用于分类筛选和展示） */
  tags?: string[]
}
