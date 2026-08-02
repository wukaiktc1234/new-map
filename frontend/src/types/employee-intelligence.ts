/**
 * 员工智能画像类型定义
 */

/** 风险等级 */
export type RiskLevel = 'low' | 'medium' | 'high' | 'critical'

/** 风险类型 */
export type RiskType = 'turnover' | 'performance' | 'health' | 'compliance' | 'burnout'

/** 人才九宫格象限（绩效-潜力） */
export type TalentQuadrant =
  | 'star'           // 高绩效-高潜力（明星）
  | 'high_potential' // 中绩效-高潜力（高潜）
  | 'solid'          // 高绩效-中潜力（中坚）
  | 'core'           // 中绩效-中潜力（核心）
  | 'underperformer' // 低绩效-低潜力（待改进）
  | 'risk'           // 中绩效-低潜力（风险）
  | 'new'            // 新员工待评估
  | 'expert'         // 高绩效-低潜力（专家）
  | 'potential'      // 低绩效-高潜力（潜力股）

/** 续签推荐等级 */
export type RenewalRecommendation = 'strong' | 'normal' | 'cautious' | 'not_recommended'

/** 雷达图维度 */
export interface ProfileRadarDimension {
  /** 维度名称 */
  name: string
  /** 得分 0-100 */
  score: number
  /** 满分 */
  fullScore: number
}

/** 工作轨迹节点 */
export interface WorkTrajectoryNode {
  nodeId: string
  /** 时间 */
  date: string
  /** 事件类型：entry/transfer/promotion/salary_change/award/contract */
  eventType: 'entry' | 'transfer' | 'promotion' | 'salary_change' | 'award' | 'contract' | 'training'
  /** 事件标题 */
  title: string
  /** 事件描述 */
  description: string
  /** 变更前 */
  fromValue?: string
  /** 变更后 */
  toValue?: string
}

/** 技能标签 */
export interface SkillTag {
  skillId: string
  skillName: string
  /** 熟练度 1-5 */
  proficiency: number
  /** 技能分类 */
  category: string
  /** 是否核心技能 */
  isCore: boolean
  /** 最近使用时间 */
  lastUsedTime?: string
}

/** 培训记录 */
export interface TrainingRecord {
  recordId: string
  courseName: string
  /** 培训类型 */
  trainingType: 'internal' | 'external' | 'online' | 'certification'
  /** 完成时间 */
  completeTime: string
  /** 成绩 */
  score?: number
  /** 是否通过 */
  passed: boolean
  /** 证书编号 */
  certificateNo?: string
}

/** 绩效趋势点 */
export interface PerformanceTrendPoint {
  /** 评估周期：如 2025-Q1 */
  period: string
  /** 绩效得分 */
  score: number
  /** 等级 */
  grade: 'S' | 'A' | 'B' | 'C' | 'D'
}

/** 员工综合画像 */
export interface EmployeeProfile {
  employeeId: string
  employeeName: string
  employeeCode: string
  avatar?: string
  department: string
  position: string
  /** 入职日期 */
  entryDate: string
  /** 司龄（月） */
  tenureMonths: number
  /** 当前合同到期日期 */
  contractEndDate: string
  /** 雷达图维度（技能/绩效/出勤/合作/创新/领导力） */
  radarDimensions: ProfileRadarDimension[]
  /** 综合评分 */
  overallScore: number
  /** 绩效等级 */
  performanceGrade: 'S' | 'A' | 'B' | 'C' | 'D'
  /** 潜力等级 1-5 */
  potentialLevel: number
  /** 工作轨迹 */
  workTrajectory: WorkTrajectoryNode[]
  /** 技能标签 */
  skills: SkillTag[]
  /** 培训记录 */
  trainingRecords: TrainingRecord[]
  /** 绩效趋势 */
  performanceTrend: PerformanceTrendPoint[]
  /** 关键成就 */
  achievements: string[]
  /** 优势项 */
  strengths: string[]
  /** 待提升项 */
  improvements: string[]
  /** 续签推荐 */
  renewalRecommendation: RenewalRecommendation
  /** 推荐理由 */
  renewalReason: string
}

/** 人才盘点项（九宫格中的一格） */
export interface TalentInventoryItem {
  employeeId: string
  employeeName: string
  department: string
  position: string
  /** 绩效得分 1-5 */
  performanceScore: number
  /** 潜力得分 1-5 */
  potentialScore: number
  /** 所在象限 */
  quadrant: TalentQuadrant
  /** 司龄（月） */
  tenureMonths: number
  /** 是否高潜人才 */
  isHighPotential: boolean
  /** 是否继任候选人 */
  isSuccessor: boolean
  /** 关键岗位 */
  isKeyPosition: boolean
  /** 画像摘要 */
  summary: string
}

/** 九宫格象限统计 */
export interface QuadrantStat {
  quadrant: TalentQuadrant
  count: number
  percentage: number
}

/** 继任计划项 */
export interface SuccessionPlanItem {
  positionId: string
  positionName: string
  department: string
  /** 当前任职者 */
  currentHolder: string
  /** 继任就绪度：ready/1-2year/2-3year/long_term */
  readiness: 'ready' | '1-2year' | '2-3year' | 'long_term'
  /** 候选人列表 */
  candidates: SuccessionCandidate[]
}

/** 继任候选人 */
export interface SuccessionCandidate {
  employeeId: string
  employeeName: string
  currentDepartment: string
  currentPosition: string
  /** 匹配度 0-100 */
  matchScore: number
  /** 准备度 */
  readiness: 'ready' | '1-2year' | '2-3year' | 'long_term'
  /** 差距分析 */
  gaps: string[]
}

/** 风险因素 */
export interface RiskFactor {
  /** 因素名称 */
  factorName: string
  /** 因素权重 0-1 */
  weight: number
  /** 实际值 */
  actualValue: string
  /** 是否触发 */
  triggered: boolean
  /** 说明 */
  description: string
}

/** 员工风险 */
export interface EmployeeRisk {
  riskId: string
  employeeId: string
  employeeName: string
  department: string
  position: string
  riskType: RiskType
  riskLevel: RiskLevel
  /** 风险概率 0-100 */
  riskScore: number
  /** 发现时间 */
  detectedTime: string
  /** 风险因素 */
  factors: RiskFactor[]
  /** 风险描述 */
  description: string
  /** 处理建议 */
  recommendations: string[]
  /** 处理状态 */
  status: 'pending' | 'handling' | 'resolved' | 'ignored'
}

/** 风险统计 */
export interface RiskStat {
  riskType: RiskType
  riskLevel: RiskLevel
  count: number
}

/** 风险等级选项 */
export const RiskLevelOptions = [
  { label: '低风险', value: 'low' as const, color: 'success' },
  { label: '中风险', value: 'medium' as const, color: 'warning' },
  { label: '高风险', value: 'high' as const, color: 'danger' },
  { label: '极高风险', value: 'critical' as const, color: 'danger' },
]

/** 风险等级标签映射 */
export const RiskLevelLabelMap: Record<RiskLevel, string> = {
  low: '低风险',
  medium: '中风险',
  high: '高风险',
  critical: '极高风险',
}

/** 风险类型标签映射 */
export const RiskTypeLabelMap: Record<RiskType, string> = {
  turnover: '离职风险',
  performance: '绩效下滑',
  health: '健康风险',
  compliance: '合规风险',
  burnout: '倦怠风险',
}

/** 风险类型图标映射 */
export const RiskTypeIconMap: Record<RiskType, string> = {
  turnover: 'Promotion',
  performance: 'TrendCharts',
  health: 'FirstAidKit',
  compliance: 'Warning',
  burnout: 'Sunny',
}

/** 九宫格象限标签映射 */
export const TalentQuadrantLabelMap: Record<TalentQuadrant, string> = {
  star: '明星员工',
  high_potential: '高潜人才',
  solid: '中坚力量',
  core: '核心员工',
  underperformer: '待改进',
  risk: '风险员工',
  new: '新员工',
  expert: '业务专家',
  potential: '潜力股',
}

/** 九宫格象限颜色映射 */
export const TalentQuadrantColorMap: Record<TalentQuadrant, string> = {
  star: 'star',
  high_potential: 'high-potential',
  solid: 'solid',
  core: 'core',
  underperformer: 'underperformer',
  risk: 'risk',
  new: 'new',
  expert: 'expert',
  potential: 'potential',
}

/** 续签推荐标签映射 */
export const RenewalRecommendationLabelMap: Record<RenewalRecommendation, string> = {
  strong: '强烈推荐续签',
  normal: '建议续签',
  cautious: '谨慎续签',
  not_recommended: '不建议续签',
}

/** 继任就绪度标签映射 */
export const ReadinessLabelMap: Record<SuccessionPlanItem['readiness'], string> = {
  ready: '随时就绪',
  '1-2year': '1-2年准备',
  '2-3year': '2-3年准备',
  long_term: '长期培养',
}

/** 绩效等级标签映射 */
export const PerformanceGradeLabelMap: Record<'S' | 'A' | 'B' | 'C' | 'D', string> = {
  S: '卓越',
  A: '优秀',
  B: '良好',
  C: '合格',
  D: '待改进',
}

/** 绩效等级颜色映射 */
export const PerformanceGradeColorMap: Record<'S' | 'A' | 'B' | 'C' | 'D', string> = {
  S: 'star',
  A: 'high-potential',
  B: 'solid',
  C: 'core',
  D: 'underperformer',
}
