/**
 * 员工智能画像 API
 *
 * TODO: 后端API待实现，暂保留mock数据。
 */
import type {
  EmployeeProfile,
  TalentInventoryItem,
  QuadrantStat,
  SuccessionPlanItem,
  EmployeeRisk,
  RiskStat,
  RiskType,
  RiskLevel,
  TalentQuadrant,
} from '@/types/employee-intelligence'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/** 模拟员工列表（用于画像选择） */
const employeeList = [
  { employeeId: 'EMP001', employeeName: '张伟', department: '总店-后厨', position: '行政总厨' },
  { employeeId: 'EMP002', employeeName: '李娜', department: '总店-前厅', position: '前厅经理' },
  { employeeId: 'EMP003', employeeName: '王强', department: '分店A-后厨', position: '厨师长' },
  { employeeId: 'EMP004', employeeName: '赵敏', department: '总店-财务', position: '财务主管' },
  { employeeId: 'EMP005', employeeName: '陈浩', department: '分店B-前厅', position: '服务员' },
  { employeeId: 'EMP006', employeeName: '刘洋', department: '总店-采购', position: '采购专员' },
  { employeeId: 'EMP007', employeeName: '孙丽', department: '总店-后厨', position: '副厨' },
  { employeeId: 'EMP008', employeeName: '周明', department: '分店A-前厅', position: '领班' },
]

/** 模拟员工画像数据 */
const mockProfiles: Record<string, EmployeeProfile> = {
  EMP001: {
    employeeId: 'EMP001',
    employeeName: '张伟',
    employeeCode: 'FTS2018001',
    department: '总店-后厨',
    position: '行政总厨',
    entryDate: '2018-03-15',
    tenureMonths: 99,
    contractEndDate: '2027-03-14',
    radarDimensions: [
      { name: '专业技能', score: 95, fullScore: 100 },
      { name: '绩效表现', score: 92, fullScore: 100 },
      { name: '出勤纪律', score: 88, fullScore: 100 },
      { name: '团队协作', score: 90, fullScore: 100 },
      { name: '创新能力', score: 85, fullScore: 100 },
      { name: '领导力', score: 93, fullScore: 100 },
    ],
    overallScore: 91,
    performanceGrade: 'S',
    potentialLevel: 5,
    workTrajectory: [
      { nodeId: 'T1', date: '2018-03-15', eventType: 'entry', title: '入职', description: '入职担任厨师长', fromValue: '', toValue: '厨师长' },
      { nodeId: 'T2', date: '2019-06-01', eventType: 'training', title: '完成食品安全管理师认证', description: '通过国家食品安全管理师资格考试', toValue: '证书编号 FS201900123' },
      { nodeId: 'T3', date: '2020-01-10', eventType: 'promotion', title: '晋升', description: '因管理能力突出晋升为行政总厨', fromValue: '厨师长', toValue: '行政总厨' },
      { nodeId: 'T4', date: '2020-12-01', eventType: 'award', title: '年度最佳员工', description: '荣获公司年度最佳员工称号' },
      { nodeId: 'T5', date: '2022-03-15', eventType: 'contract', title: '续签劳动合同', description: '续签5年期劳动合同', fromValue: '2022-03-14', toValue: '2027-03-14' },
      { nodeId: 'T6', date: '2023-08-20', eventType: 'award', title: '菜品创新奖', description: '主导研发的"溯源招牌菜"获行业创新奖' },
      { nodeId: 'T7', date: '2024-05-10', eventType: 'salary_change', title: '调薪', description: '年度调薪，涨幅15%', fromValue: '18000', toValue: '20700' },
    ],
    skills: [
      { skillId: 'S1', skillName: '中式烹饪', proficiency: 5, category: '专业技能', isCore: true, lastUsedTime: '2026-06-20' },
      { skillId: 'S2', skillName: '菜品研发', proficiency: 5, category: '专业技能', isCore: true, lastUsedTime: '2026-06-18' },
      { skillId: 'S3', skillName: '食品安全管理', proficiency: 5, category: '合规管理', isCore: true, lastUsedTime: '2026-06-19' },
      { skillId: 'S4', skillName: '团队管理', proficiency: 4, category: '管理能力', isCore: true, lastUsedTime: '2026-06-21' },
      { skillId: 'S5', skillName: '成本控制', proficiency: 4, category: '经营管理', isCore: false, lastUsedTime: '2026-06-15' },
      { skillId: 'S6', skillName: 'HACCP体系', proficiency: 4, category: '合规管理', isCore: false, lastUsedTime: '2026-05-30' },
      { skillId: 'S7', skillName: '西式烹饪', proficiency: 3, category: '专业技能', isCore: false, lastUsedTime: '2026-04-12' },
      { skillId: 'S8', skillName: '营养搭配', proficiency: 4, category: '专业技能', isCore: false, lastUsedTime: '2026-06-10' },
    ],
    trainingRecords: [
      { recordId: 'TR1', courseName: '食品安全管理师认证', trainingType: 'certification', completeTime: '2019-06-01', score: 92, passed: true, certificateNo: 'FS201900123' },
      { recordId: 'TR2', courseName: 'HACCP体系内审员', trainingType: 'external', completeTime: '2021-09-15', score: 88, passed: true, certificateNo: 'HACCP20210915' },
      { recordId: 'TR3', courseName: '餐饮业成本管理', trainingType: 'internal', completeTime: '2022-11-20', score: 95, passed: true },
      { recordId: 'TR4', courseName: '领导力提升工作坊', trainingType: 'external', completeTime: '2023-04-10', score: 90, passed: true },
      { recordId: 'TR5', courseName: '新食品安全法解读', trainingType: 'online', completeTime: '2024-08-05', score: 85, passed: true },
    ],
    performanceTrend: [
      { period: '2024-Q1', score: 88, grade: 'A' },
      { period: '2024-Q2', score: 90, grade: 'A' },
      { period: '2024-Q3', score: 92, grade: 'S' },
      { period: '2024-Q4', score: 91, grade: 'S' },
      { period: '2025-Q1', score: 93, grade: 'S' },
      { period: '2025-Q2', score: 91, grade: 'S' },
    ],
    achievements: [
      '主导研发"溯源招牌菜"系列，年销售额提升23%',
      '建立后厨HACCP体系，食品安全零事故5年',
      '培养3名厨师长，1名副厨晋升管理岗',
      '推动菜品标准化，出餐效率提升30%',
    ],
    strengths: ['菜品创新能力突出', '团队管理经验丰富', '食品安全意识强', '成本控制能力优秀'],
    improvements: ['可加强数字化运营能力', '建议拓展国际化视野'],
    renewalRecommendation: 'strong',
    renewalReason: '核心管理人才，绩效持续卓越，建议长期保留并赋予更大职责',
  },
  EMP002: {
    employeeId: 'EMP002',
    employeeName: '李娜',
    employeeCode: 'FTS2019008',
    department: '总店-前厅',
    position: '前厅经理',
    entryDate: '2019-07-01',
    tenureMonths: 83,
    contractEndDate: '2026-06-30',
    radarDimensions: [
      { name: '专业技能', score: 88, fullScore: 100 },
      { name: '绩效表现', score: 90, fullScore: 100 },
      { name: '出勤纪律', score: 95, fullScore: 100 },
      { name: '团队协作', score: 92, fullScore: 100 },
      { name: '创新能力', score: 80, fullScore: 100 },
      { name: '领导力', score: 85, fullScore: 100 },
    ],
    overallScore: 88,
    performanceGrade: 'A',
    potentialLevel: 4,
    workTrajectory: [
      { nodeId: 'T1', date: '2019-07-01', eventType: 'entry', title: '入职', description: '入职担任前厅领班', toValue: '前厅领班' },
      { nodeId: 'T2', date: '2021-01-15', eventType: 'promotion', title: '晋升', description: '晋升为前厅经理', fromValue: '前厅领班', toValue: '前厅经理' },
      { nodeId: 'T3', date: '2022-07-01', eventType: 'contract', title: '续签劳动合同', description: '续签4年期劳动合同', fromValue: '2022-06-30', toValue: '2026-06-30' },
      { nodeId: 'T4', date: '2023-12-01', eventType: 'award', title: '优秀管理者', description: '荣获公司优秀管理者称号' },
      { nodeId: 'T5', date: '2024-08-10', eventType: 'training', title: '完成服务质量管理培训', description: '参加服务质量管理高级研修班' },
    ],
    skills: [
      { skillId: 'S1', skillName: '前厅服务', proficiency: 5, category: '专业技能', isCore: true, lastUsedTime: '2026-06-21' },
      { skillId: 'S2', skillName: '客户关系管理', proficiency: 4, category: '经营管理', isCore: true, lastUsedTime: '2026-06-20' },
      { skillId: 'S3', skillName: '团队管理', proficiency: 4, category: '管理能力', isCore: true, lastUsedTime: '2026-06-21' },
      { skillId: 'S4', skillName: '投诉处理', proficiency: 5, category: '专业技能', isCore: false, lastUsedTime: '2026-06-19' },
      { skillId: 'S5', skillName: 'POS系统操作', proficiency: 4, category: '专业技能', isCore: false, lastUsedTime: '2026-06-21' },
      { skillId: 'S6', skillName: '员工培训', proficiency: 4, category: '管理能力', isCore: false, lastUsedTime: '2026-06-15' },
    ],
    trainingRecords: [
      { recordId: 'TR1', courseName: '餐饮服务礼仪', trainingType: 'internal', completeTime: '2019-09-15', score: 92, passed: true },
      { recordId: 'TR2', courseName: '客户关系管理师', trainingType: 'certification', completeTime: '2021-03-20', score: 88, passed: true, certificateNo: 'CRM20210320' },
      { recordId: 'TR3', courseName: '服务质量管理高级研修', trainingType: 'external', completeTime: '2024-08-10', score: 90, passed: true },
    ],
    performanceTrend: [
      { period: '2024-Q1', score: 85, grade: 'A' },
      { period: '2024-Q2', score: 87, grade: 'A' },
      { period: '2024-Q3', score: 89, grade: 'A' },
      { period: '2024-Q4', score: 88, grade: 'A' },
      { period: '2025-Q1', score: 90, grade: 'A' },
      { period: '2025-Q2', score: 91, grade: 'S' },
    ],
    achievements: [
      '客户满意度从85%提升至94%',
      '前厅团队人员流失率下降40%',
      '建立客户档案管理系统，会员复购率提升25%',
    ],
    strengths: ['客户服务意识强', '团队稳定性高', '投诉处理能力优秀'],
    improvements: ['可加强数据分析能力', '建议提升创新思维'],
    renewalRecommendation: 'strong',
    renewalReason: '合同即将到期，绩效持续优秀，建议立即启动续签流程并赋予店长培养计划',
  },
}

/** 默认画像（未配置员工） */
function generateDefaultProfile(emp: { employeeId: string; employeeName: string; department: string; position: string }): EmployeeProfile {
  return {
    employeeId: emp.employeeId,
    employeeName: emp.employeeName,
    employeeCode: `FTS${2018000 + parseInt(emp.employeeId.replace('EMP', ''))}`,
    department: emp.department,
    position: emp.position,
    entryDate: '2021-05-10',
    tenureMonths: 60,
    contractEndDate: '2026-05-09',
    radarDimensions: [
      { name: '专业技能', score: 78, fullScore: 100 },
      { name: '绩效表现', score: 82, fullScore: 100 },
      { name: '出勤纪律', score: 90, fullScore: 100 },
      { name: '团队协作', score: 85, fullScore: 100 },
      { name: '创新能力', score: 70, fullScore: 100 },
      { name: '领导力', score: 72, fullScore: 100 },
    ],
    overallScore: 80,
    performanceGrade: 'B',
    potentialLevel: 3,
    workTrajectory: [
      { nodeId: 'T1', date: '2021-05-10', eventType: 'entry', title: '入职', description: `入职担任${emp.position}`, toValue: emp.position },
      { nodeId: 'T2', date: '2023-05-10', eventType: 'contract', title: '续签劳动合同', description: '续签3年期劳动合同', fromValue: '2023-05-09', toValue: '2026-05-09' },
    ],
    skills: [
      { skillId: 'S1', skillName: '岗位基础技能', proficiency: 3, category: '专业技能', isCore: true, lastUsedTime: '2026-06-21' },
      { skillId: 'S2', skillName: '团队协作', proficiency: 4, category: '软技能', isCore: false, lastUsedTime: '2026-06-21' },
    ],
    trainingRecords: [
      { recordId: 'TR1', courseName: '新员工入职培训', trainingType: 'internal', completeTime: '2021-05-15', score: 85, passed: true },
    ],
    performanceTrend: [
      { period: '2024-Q4', score: 78, grade: 'B' },
      { period: '2025-Q1', score: 80, grade: 'B' },
      { period: '2025-Q2', score: 82, grade: 'B' },
    ],
    achievements: ['按时完成岗位工作任务', '积极参与团队协作'],
    strengths: ['工作态度端正', '团队配合度好'],
    improvements: ['建议加强专业技能提升', '可主动承担更多责任'],
    renewalRecommendation: 'normal',
    renewalReason: '工作表现稳定，建议按常规流程续签',
  }
}

/** 模拟人才盘点数据 */
const mockTalentInventory: TalentInventoryItem[] = [
  { employeeId: 'EMP001', employeeName: '张伟', department: '总店-后厨', position: '行政总厨', performanceScore: 5, potentialScore: 5, quadrant: 'star', tenureMonths: 99, isHighPotential: true, isSuccessor: false, isKeyPosition: true, summary: '核心管理人才，绩效卓越，具备战略思维' },
  { employeeId: 'EMP002', employeeName: '李娜', department: '总店-前厅', position: '前厅经理', performanceScore: 4, potentialScore: 5, quadrant: 'high_potential', tenureMonths: 83, isHighPotential: true, isSuccessor: true, isKeyPosition: true, summary: '高潜人才，客户服务能力突出，可培养为店长' },
  { employeeId: 'EMP003', employeeName: '王强', department: '分店A-后厨', position: '厨师长', performanceScore: 4, potentialScore: 4, quadrant: 'solid', tenureMonths: 60, isHighPotential: false, isSuccessor: true, isKeyPosition: true, summary: '中坚力量，技术扎实，管理能力稳步提升' },
  { employeeId: 'EMP004', employeeName: '赵敏', department: '总店-财务', position: '财务主管', performanceScore: 5, potentialScore: 3, quadrant: 'expert', tenureMonths: 72, isHighPotential: false, isSuccessor: false, isKeyPosition: true, summary: '业务专家，财务专业能力强，深度方向人才' },
  { employeeId: 'EMP007', employeeName: '孙丽', department: '总店-后厨', position: '副厨', performanceScore: 3, potentialScore: 5, quadrant: 'high_potential', tenureMonths: 36, isHighPotential: true, isSuccessor: true, isKeyPosition: false, summary: '高潜人才，学习能力强，建议加速培养' },
  { employeeId: 'EMP008', employeeName: '周明', department: '分店A-前厅', position: '领班', performanceScore: 3, potentialScore: 4, quadrant: 'core', tenureMonths: 30, isHighPotential: false, isSuccessor: false, isKeyPosition: false, summary: '核心员工，工作稳定，有发展潜力' },
  { employeeId: 'EMP006', employeeName: '刘洋', department: '总店-采购', position: '采购专员', performanceScore: 4, potentialScore: 3, quadrant: 'solid', tenureMonths: 48, isHighPotential: false, isSuccessor: false, isKeyPosition: false, summary: '中坚力量，采购经验丰富，业绩稳定' },
  { employeeId: 'EMP005', employeeName: '陈浩', department: '分店B-前厅', position: '服务员', performanceScore: 2, potentialScore: 3, quadrant: 'risk', tenureMonths: 12, isHighPotential: false, isSuccessor: false, isKeyPosition: false, summary: '需关注，绩效有下滑趋势，建议辅导' },
]

/** 模拟继任计划数据 */
const mockSuccessionPlans: SuccessionPlanItem[] = [
  {
    positionId: 'POS001',
    positionName: '行政总厨',
    department: '总店-后厨',
    currentHolder: '张伟',
    readiness: '1-2year',
    candidates: [
      { employeeId: 'EMP007', employeeName: '孙丽', currentDepartment: '总店-后厨', currentPosition: '副厨', matchScore: 78, readiness: '1-2year', gaps: ['需要更多跨部门管理经验', '建议加强成本控制能力'] },
      { employeeId: 'EMP003', employeeName: '王强', currentDepartment: '分店A-后厨', currentPosition: '厨师长', matchScore: 72, readiness: '2-3year', gaps: ['需要总店管理经验', '建议提升菜品研发能力'] },
    ],
  },
  {
    positionId: 'POS002',
    positionName: '前厅经理',
    department: '总店-前厅',
    currentHolder: '李娜',
    readiness: '1-2year',
    candidates: [
      { employeeId: 'EMP008', employeeName: '周明', currentDepartment: '分店A-前厅', currentPosition: '领班', matchScore: 68, readiness: '2-3year', gaps: ['需要总店前厅经验', '建议加强客户关系管理能力'] },
    ],
  },
  {
    positionId: 'POS003',
    positionName: '分店店长',
    department: '分店A',
    currentHolder: '（空缺）',
    readiness: 'ready',
    candidates: [
      { employeeId: 'EMP002', employeeName: '李娜', currentDepartment: '总店-前厅', currentPosition: '前厅经理', matchScore: 85, readiness: 'ready', gaps: [] },
      { employeeId: 'EMP003', employeeName: '王强', currentDepartment: '分店A-后厨', currentPosition: '厨师长', matchScore: 70, readiness: '1-2year', gaps: ['需要前厅管理经验'] },
    ],
  },
]

/** 模拟员工风险数据 */
const mockEmployeeRisks: EmployeeRisk[] = [
  {
    riskId: 'RISK001',
    employeeId: 'EMP005',
    employeeName: '陈浩',
    department: '分店B-前厅',
    position: '服务员',
    riskType: 'turnover',
    riskLevel: 'high',
    riskScore: 78,
    detectedTime: '2026-06-15',
    factors: [
      { factorName: '近期加班时长', weight: 0.3, actualValue: '月均加班48小时', triggered: true, description: '近3个月加班时长显著高于平均水平' },
      { factorName: '请假频次', weight: 0.2, actualValue: '近1月请假3次', triggered: true, description: '请假频次明显增加' },
      { factorName: '绩效变化', weight: 0.25, actualValue: '下降15%', triggered: true, description: '近2个季度绩效持续下滑' },
      { factorName: '司龄', weight: 0.1, actualValue: '12个月', triggered: true, description: '入职1年左右为离职高发期' },
      { factorName: '薪酬竞争力', weight: 0.15, actualValue: '低于市场8%', triggered: false, description: '薪酬处于市场中位线以下' },
    ],
    description: '该员工近3个月表现出明显的离职倾向信号，包括加班时长激增、请假频次上升、绩效下滑等多重风险因素叠加',
    recommendations: [
      '建议直属主管1周内进行一对一沟通，了解员工真实想法',
      '评估工作负荷，考虑调整排班或增加人手',
      '复盘近期绩效反馈，明确改进方向和支持措施',
      '若员工为关键岗位，可考虑调薪或晋升机会',
    ],
    status: 'pending',
  },
  {
    riskId: 'RISK002',
    employeeId: 'EMP003',
    employeeName: '王强',
    department: '分店A-后厨',
    position: '厨师长',
    riskType: 'burnout',
    riskLevel: 'medium',
    riskScore: 62,
    detectedTime: '2026-06-10',
    factors: [
      { factorName: '连续工作时长', weight: 0.3, actualValue: '连续工作60天未休', triggered: true, description: '近2个月无完整休息日' },
      { factorName: '加班频次', weight: 0.25, actualValue: '周均加班5次', triggered: true, description: '加班频次显著高于部门平均' },
      { factorName: '情绪指标', weight: 0.2, actualValue: '中性偏负', triggered: true, description: '近期工作沟通中情绪指标偏低' },
      { factorName: '工作多样性', weight: 0.15, actualValue: '单一化', triggered: false, description: '工作内容相对单一' },
      { factorName: '社交支持', weight: 0.1, actualValue: '良好', triggered: false, description: '团队关系融洽' },
    ],
    description: '该员工存在中度倦怠风险，主要表现为长期高强度工作和情绪指标下降',
    recommendations: [
      '建议安排强制休假，至少连续休息3-5天',
      '评估是否可分担部分管理职责，减轻工作压力',
      '关注员工情绪状态，必要时提供心理支持资源',
      '建议安排岗位轮换或拓展工作内容',
    ],
    status: 'handling',
  },
  {
    riskId: 'RISK003',
    employeeId: 'EMP006',
    employeeName: '刘洋',
    department: '总店-采购',
    position: '采购专员',
    riskType: 'compliance',
    riskLevel: 'medium',
    riskScore: 55,
    detectedTime: '2026-06-08',
    factors: [
      { factorName: '供应商资质审核', weight: 0.3, actualValue: '2家未及时更新', triggered: true, description: '存在2家供应商资质证书过期未及时更新' },
      { factorName: '采购流程合规', weight: 0.3, actualValue: '3笔异常', triggered: true, description: '近1月有3笔采购未走完整审批流程' },
      { factorName: '合同管理', weight: 0.2, actualValue: '1份缺失', triggered: true, description: '1份供应商合同未及时归档' },
      { factorName: '廉洁指标', weight: 0.2, actualValue: '正常', triggered: false, description: '未发现廉洁风险信号' },
    ],
    description: '采购流程合规性存在风险点，需关注流程执行规范性',
    recommendations: [
      '立即补齐2家供应商资质审核',
      '复盘3笔异常采购流程，明确责任人',
      '加强采购流程培训，重申合规要求',
      '建议建立采购合规自查清单',
    ],
    status: 'pending',
  },
  {
    riskId: 'RISK004',
    employeeId: 'EMP008',
    employeeName: '周明',
    department: '分店A-前厅',
    position: '领班',
    riskType: 'performance',
    riskLevel: 'low',
    riskScore: 35,
    detectedTime: '2026-06-05',
    factors: [
      { factorName: '绩效变化', weight: 0.4, actualValue: '下降8%', triggered: true, description: '本季度绩效较上季度小幅下降' },
      { factorName: '客户投诉', weight: 0.3, actualValue: '1次', triggered: false, description: '近1月有1次客户投诉' },
      { factorName: '团队反馈', weight: 0.3, actualValue: '正常', triggered: false, description: '团队反馈正常' },
    ],
    description: '绩效出现小幅下滑趋势，建议关注并主动辅导',
    recommendations: [
      '建议主管进行绩效面谈，了解下滑原因',
      '制定下季度绩效改进计划',
      '安排针对性技能培训',
    ],
    status: 'pending',
  },
  {
    riskId: 'RISK005',
    employeeId: 'EMP004',
    employeeName: '赵敏',
    department: '总店-财务',
    position: '财务主管',
    riskType: 'health',
    riskLevel: 'low',
    riskScore: 28,
    detectedTime: '2026-06-03',
    factors: [
      { factorName: '体检指标', weight: 0.4, actualValue: '2项异常', triggered: true, description: '近期体检有2项指标异常' },
      { factorName: '请假类型', weight: 0.3, actualValue: '病假1次', triggered: false, description: '近半年病假1次' },
      { factorName: '工作强度', weight: 0.3, actualValue: '正常', triggered: false, description: '工作强度处于正常范围' },
    ],
    description: '体检指标提示需要关注健康状况，建议安排复查',
    recommendations: [
      '建议员工尽快到医院复查异常指标',
      '关注员工工作负荷，避免过度加班',
      '可提供健康关怀资源',
    ],
    status: 'resolved',
  },
]

/** 获取员工列表（用于画像选择） */
async function getEmployeeList(): Promise<typeof employeeList> {
  await delay()
  return employeeList
}

/** 获取员工综合画像 */
async function getEmployeeProfile(employeeId: string): Promise<EmployeeProfile> {
  await delay()
  const emp = employeeList.find(e => e.employeeId === employeeId)
  if (!emp) throw new Error('员工不存在')
  return mockProfiles[employeeId] || generateDefaultProfile(emp)
}

/** 获取人才盘点列表 */
async function getTalentInventory(): Promise<TalentInventoryItem[]> {
  await delay()
  return mockTalentInventory
}

/** 获取九宫格象限统计 */
async function getQuadrantStats(): Promise<QuadrantStat[]> {
  await delay()
  const counts: Record<TalentQuadrant, number> = {
    star: 0, high_potential: 0, solid: 0, core: 0,
    underperformer: 0, risk: 0, new: 0, expert: 0, potential: 0,
  }
  for (const item of mockTalentInventory) {
    counts[item.quadrant]++
  }
  const total = mockTalentInventory.length
  return (Object.keys(counts) as TalentQuadrant[]).map(q => ({
    quadrant: q,
    count: counts[q],
    percentage: total > 0 ? Math.round((counts[q] / total) * 100) : 0,
  }))
}

/** 获取继任计划列表 */
async function getSuccessionPlans(): Promise<SuccessionPlanItem[]> {
  await delay()
  return mockSuccessionPlans
}

/** 获取员工风险列表 */
async function getEmployeeRisks(filter?: { riskType?: RiskType; riskLevel?: RiskLevel }): Promise<EmployeeRisk[]> {
  await delay()
  if (!filter) return mockEmployeeRisks
  return mockEmployeeRisks.filter(r => {
    if (filter.riskType && r.riskType !== filter.riskType) return false
    if (filter.riskLevel && r.riskLevel !== filter.riskLevel) return false
    return true
  })
}

/** 获取风险统计 */
async function getRiskStats(): Promise<RiskStat[]> {
  await delay()
  const stats: RiskStat[] = []
  const typeLevelMap = new Map<string, number>()
  for (const risk of mockEmployeeRisks) {
    const key = `${risk.riskType}-${risk.riskLevel}`
    typeLevelMap.set(key, (typeLevelMap.get(key) || 0) + 1)
  }
  for (const [key, count] of typeLevelMap) {
    const [riskType, riskLevel] = key.split('-') as [RiskType, RiskLevel]
    stats.push({ riskType, riskLevel, count })
  }
  return stats
}

/** 更新风险处理状态 */
async function updateRiskStatus(riskId: string, status: EmployeeRisk['status']): Promise<void> {
  await delay()
  const risk = mockEmployeeRisks.find(r => r.riskId === riskId)
  if (risk) risk.status = status
}

export const employeeIntelligenceApi = {
  getEmployeeList,
  getEmployeeProfile,
  getTalentInventory,
  getQuadrantStats,
  getSuccessionPlans,
  getEmployeeRisks,
  getRiskStats,
  updateRiskStatus,
}
