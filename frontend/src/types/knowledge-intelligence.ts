/**
 * 知识库智能化类型定义
 */

/** 学习路径难度 */
export type LearningPathDifficulty = 'beginner' | 'intermediate' | 'advanced'

/** 学习路径状态 */
export type LearningPathStatus = 'not_started' | 'in_progress' | 'completed'

/** AI标签 */
export interface AITag {
  tagId: string
  tagName: string
  /** 标签分类 */
  category: string
  /** 关联文章数 */
  articleCount: number
  /** AI置信度 0-100 */
  confidence: number
  /** 标签颜色 */
  color: string
}

/** 学习路径节点 */
export interface LearningPathNode {
  nodeId: string
  articleId: string
  articleTitle: string
  /** 节点顺序 */
  order: number
  /** 预计学习时长（分钟） */
  duration: number
  /** 是否必修 */
  required: boolean
  /** 依赖节点ID */
  dependsOn?: string[]
}

/** 学习路径 */
export interface LearningPath {
  pathId: string
  pathName: string
  description: string
  /** 适用岗位 */
  targetPosition: string
  /** 适用部门 */
  targetDepartment: string
  difficulty: LearningPathDifficulty
  /** 总时长（分钟） */
  totalDuration: number
  /** 节点列表 */
  nodes: LearningPathNode[]
  /** 学习人数 */
  learnerCount: number
  /** 完成率 */
  completionRate: number
  status: LearningPathStatus
  createTime: string
}

/** 知识图谱节点 */
export interface KnowledgeGraphNode {
  id: string
  label: string
  /** 节点类型：article/tag/category */
  type: 'article' | 'tag' | 'category'
  /** 节点大小（根据关联数） */
  size: number
  /** 关联文章数 */
  articleCount?: number
}

/** 知识图谱边 */
export interface KnowledgeGraphEdge {
  source: string
  target: string
  /** 关联强度 1-10 */
  weight: number
  /** 关联类型：tag_of/related_to/belongs_to */
  relation: 'tag_of' | 'related_to' | 'belongs_to'
}

/** 知识图谱 */
export interface KnowledgeGraph {
  nodes: KnowledgeGraphNode[]
  edges: KnowledgeGraphEdge[]
}

/** 学习路径难度选项 */
export const LearningPathDifficultyOptions = [
  { label: '入门', value: 'beginner' as const },
  { label: '进阶', value: 'intermediate' as const },
  { label: '高级', value: 'advanced' as const },
]

/** 学习路径难度标签映射 */
export const LearningPathDifficultyLabelMap: Record<LearningPathDifficulty, string> = {
  beginner: '入门',
  intermediate: '进阶',
  advanced: '高级',
}

/** 学习路径状态标签映射 */
export const LearningPathStatusLabelMap: Record<LearningPathStatus, string> = {
  not_started: '未开始',
  in_progress: '进行中',
  completed: '已完成',
}
