/**
 * 知识库智能化 API
 *
 * TODO: 后端API待实现，暂保留mock数据。
 */
import type {
  AITag,
  LearningPath,
  KnowledgeGraph,
  LearningPathDifficulty,
} from '@/types/knowledge-intelligence'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/** AI标签 Mock 数据（颜色值为mock演示数据，后端API实现后将移除） */
const mockAITags: AITag[] = [
  { tagId: 'T001', tagName: '食品安全', category: '合规', articleCount: 28, confidence: 95, color: '#f56c6c' },
  { tagId: 'T002', tagName: '操作规范', category: '技能', articleCount: 22, confidence: 92, color: '#e6a23c' },
  { tagId: 'T003', tagName: '卫生管理', category: '合规', articleCount: 18, confidence: 90, color: '#67c23a' },
  { tagId: 'T004', tagName: '设备维护', category: '技能', articleCount: 15, confidence: 88, color: '#409eff' },
  { tagId: 'T005', tagName: '客户服务', category: '服务', articleCount: 12, confidence: 85, color: '#909399' },
  { tagId: 'T006', tagName: '应急处置', category: '安全', articleCount: 10, confidence: 87, color: '#f56c6c' },
  { tagId: 'T007', tagName: '采购管理', category: '管理', articleCount: 8, confidence: 82, color: '#9c27b0' },
  { tagId: 'T008', tagName: '团队建设', category: '管理', articleCount: 6, confidence: 78, color: '#00bcd4' },
]

/** 学习路径 Mock 数据 */
const mockLearningPaths: LearningPath[] = [
  {
    pathId: 'LP001',
    pathName: '新员工入职学习路径',
    description: '新入职员工的必修学习路径，涵盖食品安全、操作规范、卫生管理等基础知识',
    targetPosition: '全体员工',
    targetDepartment: '所有部门',
    difficulty: 'beginner',
    totalDuration: 180,
    nodes: [
      { nodeId: 'N1', articleId: 'A001', articleTitle: '食品安全法基础知识', order: 1, duration: 30, required: true },
      { nodeId: 'N2', articleId: 'A002', articleTitle: '个人卫生规范', order: 2, duration: 20, required: true },
      { nodeId: 'N3', articleId: 'A003', articleTitle: '厨房操作规范', order: 3, duration: 40, required: true },
      { nodeId: 'N4', articleId: 'A004', articleTitle: '应急处置流程', order: 4, duration: 30, required: true },
      { nodeId: 'N5', articleId: 'A005', articleTitle: '客户服务基础', order: 5, duration: 30, required: false },
      { nodeId: 'N6', articleId: 'A006', articleTitle: '企业文化与价值观', order: 6, duration: 30, required: true },
    ],
    learnerCount: 45,
    completionRate: 78,
    status: 'in_progress',
    createTime: '2026-01-15',
  },
  {
    pathId: 'LP002',
    pathName: '厨师专业技能提升路径',
    description: '针对厨房员工的进阶技能培训，包含刀工、烹饪技巧、菜品创新等',
    targetPosition: '厨师',
    targetDepartment: '厨房',
    difficulty: 'intermediate',
    totalDuration: 240,
    nodes: [
      { nodeId: 'N7', articleId: 'A007', articleTitle: '刀工技巧进阶', order: 1, duration: 45, required: true },
      { nodeId: 'N8', articleId: 'A008', articleTitle: '烹饪温度与时间控制', order: 2, duration: 40, required: true },
      { nodeId: 'N9', articleId: 'A009', articleTitle: '食材搭配原理', order: 3, duration: 35, required: true, dependsOn: ['N7', 'N8'] },
      { nodeId: 'N10', articleId: 'A010', articleTitle: '菜品创新方法论', order: 4, duration: 40, required: false, dependsOn: ['N9'] },
      { nodeId: 'N11', articleId: 'A011', articleTitle: '厨房设备维护', order: 5, duration: 30, required: true },
      { nodeId: 'N12', articleId: 'A012', articleTitle: '成本控制与减少浪费', order: 6, duration: 50, required: true },
    ],
    learnerCount: 18,
    completionRate: 56,
    status: 'in_progress',
    createTime: '2026-02-01',
  },
  {
    pathId: 'LP003',
    pathName: '店长管理能力提升路径',
    description: '面向店长和储备店长的管理技能培训，涵盖团队管理、运营分析、客户关系等',
    targetPosition: '店长',
    targetDepartment: '管理',
    difficulty: 'advanced',
    totalDuration: 300,
    nodes: [
      { nodeId: 'N13', articleId: 'A013', articleTitle: '团队管理与激励', order: 1, duration: 50, required: true },
      { nodeId: 'N14', articleId: 'A014', articleTitle: '门店运营数据分析', order: 2, duration: 60, required: true },
      { nodeId: 'N15', articleId: 'A015', articleTitle: '客户关系管理', order: 3, duration: 45, required: true },
      { nodeId: 'N16', articleId: 'A016', articleTitle: '成本核算与利润优化', order: 4, duration: 55, required: true, dependsOn: ['N14'] },
      { nodeId: 'N17', articleId: 'A017', articleTitle: '危机公关处理', order: 5, duration: 40, required: true },
      { nodeId: 'N18', articleId: 'A018', articleTitle: '员工绩效面谈技巧', order: 6, duration: 50, required: true, dependsOn: ['N13'] },
    ],
    learnerCount: 6,
    completionRate: 42,
    status: 'in_progress',
    createTime: '2026-03-01',
  },
]

/** 知识图谱 Mock 数据 */
const mockKnowledgeGraph: KnowledgeGraph = {
  nodes: [
    { id: 'tag_food_safety', label: '食品安全', type: 'tag', size: 40, articleCount: 28 },
    { id: 'tag_hygiene', label: '卫生管理', type: 'tag', size: 32, articleCount: 18 },
    { id: 'tag_operation', label: '操作规范', type: 'tag', size: 35, articleCount: 22 },
    { id: 'tag_equipment', label: '设备维护', type: 'tag', size: 28, articleCount: 15 },
    { id: 'tag_service', label: '客户服务', type: 'tag', size: 25, articleCount: 12 },
    { id: 'tag_emergency', label: '应急处置', type: 'tag', size: 22, articleCount: 10 },
    { id: 'cat_compliance', label: '合规类', type: 'category', size: 30 },
    { id: 'cat_skill', label: '技能类', type: 'category', size: 30 },
    { id: 'cat_service', label: '服务类', type: 'category', size: 25 },
    { id: 'art_001', label: '食品安全法基础', type: 'article', size: 18 },
    { id: 'art_002', label: '个人卫生规范', type: 'article', size: 16 },
    { id: 'art_003', label: '厨房操作规范', type: 'article', size: 18 },
    { id: 'art_004', label: '应急处置流程', type: 'article', size: 15 },
  ],
  edges: [
    { source: 'tag_food_safety', target: 'cat_compliance', weight: 10, relation: 'belongs_to' },
    { source: 'tag_hygiene', target: 'cat_compliance', weight: 9, relation: 'belongs_to' },
    { source: 'tag_operation', target: 'cat_skill', weight: 9, relation: 'belongs_to' },
    { source: 'tag_equipment', target: 'cat_skill', weight: 8, relation: 'belongs_to' },
    { source: 'tag_service', target: 'cat_service', weight: 8, relation: 'belongs_to' },
    { source: 'tag_emergency', target: 'cat_compliance', weight: 7, relation: 'belongs_to' },
    { source: 'art_001', target: 'tag_food_safety', weight: 10, relation: 'tag_of' },
    { source: 'art_002', target: 'tag_hygiene', weight: 9, relation: 'tag_of' },
    { source: 'art_003', target: 'tag_operation', weight: 9, relation: 'tag_of' },
    { source: 'art_003', target: 'tag_food_safety', weight: 7, relation: 'tag_of' },
    { source: 'art_004', target: 'tag_emergency', weight: 10, relation: 'tag_of' },
    { source: 'tag_food_safety', target: 'tag_hygiene', weight: 8, relation: 'related_to' },
    { source: 'tag_food_safety', target: 'tag_operation', weight: 7, relation: 'related_to' },
    { source: 'tag_operation', target: 'tag_equipment', weight: 8, relation: 'related_to' },
  ],
}

export const knowledgeIntelligenceApi = {
  /** 获取AI标签列表 */
  async getAITags(): Promise<AITag[]> {
    await delay()
    return mockAITags
  },

  /** 获取学习路径列表 */
  async getLearningPaths(difficulty?: LearningPathDifficulty): Promise<LearningPath[]> {
    await delay()
    let result = [...mockLearningPaths]
    if (difficulty) result = result.filter(p => p.difficulty === difficulty)
    return result
  },

  /** 获取学习路径详情 */
  async getLearningPathById(id: string): Promise<LearningPath | null> {
    await delay()
    return mockLearningPaths.find(p => p.pathId === id) || null
  },

  /** 获取知识图谱 */
  async getKnowledgeGraph(): Promise<KnowledgeGraph> {
    await delay()
    return mockKnowledgeGraph
  },
}

export default knowledgeIntelligenceApi
