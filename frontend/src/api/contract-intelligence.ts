/**
 * 合同智能管理 API
 *
 * TODO: 后端API待实现，暂保留mock数据。
 */
import type {
  ExpiringContract,
  ContractRisk,
  RenewalStats,
  ExpiryStats,
  ContractTemplateInfo,
  ContractTemplateType,
  TemplateClause,
} from '@/types/contract-intelligence'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/** 到期合同 Mock 数据 */
const mockExpiringContracts: ExpiringContract[] = [
  {
    contractId: 'EC001', contractNo: 'HT-2025-001', employeeName: '张三', department: '厨房', position: '主厨',
    contractType: 'labor', startDate: '2024-07-01', endDate: '2026-06-30', remainingDays: 9,
    alertLevel: 'urgent', recommendation: 'strong', recommendationReason: '员工绩效优秀，连续2年考核A，建议续签2年',
  },
  {
    contractId: 'EC002', contractNo: 'HT-2025-002', employeeName: '李四', department: '前厅', position: '服务员',
    contractType: 'labor', startDate: '2025-01-01', endDate: '2026-06-25', remainingDays: 4,
    alertLevel: 'urgent', recommendation: 'normal', recommendationReason: '员工表现稳定，建议续签1年',
  },
  {
    contractId: 'EC003', contractNo: 'HT-2024-088', employeeName: '王五', department: '管理', position: '店长',
    contractType: 'labor', startDate: '2023-07-01', endDate: '2026-06-21', remainingDays: 0,
    alertLevel: 'expired', recommendation: 'strong', recommendationReason: '核心管理岗位，绩效卓越，建议立即续签3年',
  },
  {
    contractId: 'EC004', contractNo: 'HT-2025-015', employeeName: '赵六', department: '采购', position: '采购员',
    contractType: 'labor', startDate: '2025-03-01', endDate: '2026-07-15', remainingDays: 24,
    alertLevel: 'warning', recommendation: 'cautious', recommendationReason: '员工近期有迟到记录，建议面谈后再决定续签',
  },
  {
    contractId: 'EC005', contractNo: 'HT-2025-022', employeeName: '孙七', department: '财务', position: '会计',
    contractType: 'labor', startDate: '2024-08-01', endDate: '2026-08-01', remainingDays: 41,
    alertLevel: 'notice', recommendation: 'normal', recommendationReason: '工作表现良好，建议续签2年',
  },
  {
    contractId: 'EC006', contractNo: 'HT-2024-099', employeeName: '周八', department: '后厨', position: '帮厨',
    contractType: 'service', startDate: '2025-06-01', endDate: '2026-06-01', remainingDays: -20,
    alertLevel: 'expired', recommendation: 'not_recommended', recommendationReason: '劳务协议已过期20天，员工已离职，建议归档',
  },
]

/** 合同风险 Mock 数据 */
const mockContractRisks: ContractRisk[] = [
  {
    riskId: 'RISK001', riskType: 'expired_unsigned', riskLevel: 'high', contractId: 'EC003',
    employeeName: '王五', department: '管理', description: '店长合同已过期，存在无合同用工风险',
    detectedTime: '2026-06-21', suggestion: '立即启动续签流程，避免劳动纠纷',
  },
  {
    riskId: 'RISK002', riskType: 'expired_unsigned', riskLevel: 'high', contractId: 'EC006',
    employeeName: '周八', department: '后厨', description: '劳务协议已过期20天未处理',
    detectedTime: '2026-06-21', suggestion: '确认员工状态，如已离职则归档，如仍在岗需补签',
  },
  {
    riskId: 'RISK003', riskType: 'salary_anomaly', riskLevel: 'medium',
    employeeName: '赵六', department: '采购', description: '薪资高于同岗位平均水平的30%',
    detectedTime: '2026-06-20', suggestion: '核实薪资构成，评估是否合理',
  },
  {
    riskId: 'RISK004', riskType: 'missing_contract', riskLevel: 'high',
    employeeName: '吴九', department: '前厅', description: '入职45天未签订劳动合同',
    detectedTime: '2026-06-18', suggestion: '根据劳动法，入职1个月内必须签订合同，请立即处理',
  },
  {
    riskId: 'RISK005', riskType: 'long_term_temp', riskLevel: 'low',
    employeeName: '郑十', department: '后厨', description: '临时工连续工作超过6个月，建议转正',
    detectedTime: '2026-06-15', suggestion: '评估员工表现，考虑签订正式劳动合同',
  },
]

/** 合同模板 Mock 数据 */
const mockContractTemplates: ContractTemplateInfo[] = [
  {
    templateId: 'TPL001', templateName: '标准劳动合同（全日制）', templateType: 'labor',
    description: '适用于全日制员工的标准劳动合同，符合最新劳动法规定',
    content: '甲方（用人单位）：\n乙方（劳动者）：\n\n根据《中华人民共和国劳动合同法》及相关法律法规，甲乙双方在平等自愿、协商一致的基础上，签订本劳动合同。',
    customClauses: [
      { clauseId: 'C001', clauseTitle: '合同期限', clauseContent: '本合同期限为__年，自__年__月__日起至__年__月__日止。', required: true, editable: true, category: 'term' },
      { clauseId: 'C002', clauseTitle: '工作内容', clauseContent: '乙方担任__岗位，工作地点为__。', required: true, editable: true, category: 'basic' },
      { clauseId: 'C003', clauseTitle: '劳动报酬', clauseContent: '乙方月工资为人民币__元，于每月__日发放。', required: true, editable: true, category: 'salary' },
      { clauseId: 'C004', clauseTitle: '社会保险', clauseContent: '甲方依法为乙方缴纳养老、医疗、失业、工伤、生育保险。', required: true, editable: false, category: 'benefit' },
      { clauseId: 'C005', clauseTitle: '保密义务', clauseContent: '乙方在职期间及离职后__年内，对甲方商业秘密负有保密义务。', required: false, editable: true, category: 'confidentiality' },
    ],
    status: 'active', version: '2.0', updateTime: '2026-01-01', createBy: 'HR总监',
  },
  {
    templateId: 'TPL002', templateName: '劳务协议（非全日制）', templateType: 'service',
    description: '适用于非全日制用工、退休返聘等劳务关系',
    content: '甲方（用工方）：\n乙方（提供劳务方）：\n\n根据《中华人民共和国民法典》及相关法律法规，就乙方向甲方提供劳务事宜，达成如下协议。',
    customClauses: [
      { clauseId: 'C101', clauseTitle: '劳务内容', clauseContent: '乙方为甲方提供__劳务，具体内容包括__。', required: true, editable: true, category: 'basic' },
      { clauseId: 'C102', clauseTitle: '劳务期限', clauseContent: '本协议期限自__年__月__日起至__年__月__日止。', required: true, editable: true, category: 'term' },
      { clauseId: 'C103', clauseTitle: '劳务报酬', clauseContent: '甲方按__标准向乙方支付劳务报酬。', required: true, editable: true, category: 'salary' },
    ],
    status: 'active', version: '1.0', updateTime: '2026-01-01', createBy: 'HR总监',
  },
  {
    templateId: 'TPL003', templateName: '实习协议', templateType: 'internship',
    description: '适用于在校学生实习',
    content: '甲方（实习单位）：\n乙方（实习生）：\n\n为明确实习期间双方权利义务，经协商达成如下协议。',
    customClauses: [
      { clauseId: 'C201', clauseTitle: '实习岗位', clauseContent: '乙方在甲方__部门__岗位实习。', required: true, editable: true, category: 'basic' },
      { clauseId: 'C202', clauseTitle: '实习期限', clauseContent: '实习期自__年__月__日至__年__月__日。', required: true, editable: true, category: 'term' },
      { clauseId: 'C203', clauseTitle: '实习补贴', clauseContent: '甲方每月向乙方支付实习补贴__元。', required: false, editable: true, category: 'salary' },
    ],
    status: 'active', version: '1.0', updateTime: '2026-01-01', createBy: 'HR总监',
  },
  {
    templateId: 'TPL004', templateName: '保密协议', templateType: 'confidentiality',
    description: '适用于接触商业秘密的员工',
    content: '甲方（公司）：\n乙方（员工）：\n\n为保护甲方商业秘密，经双方协商签订本保密协议。',
    customClauses: [
      { clauseId: 'C301', clauseTitle: '保密范围', clauseContent: '甲方商业秘密包括但不限于：客户信息、经营策略、配方工艺、财务数据等。', required: true, editable: true, category: 'confidentiality' },
      { clauseId: 'C302', clauseTitle: '保密期限', clauseContent: '乙方在职期间及离职后__年内承担保密义务。', required: true, editable: true, category: 'term' },
      { clauseId: 'C303', clauseTitle: '违约责任', clauseContent: '乙方违反保密义务的，应向甲方支付违约金__元。', required: true, editable: true, category: 'other' },
    ],
    status: 'active', version: '1.0', updateTime: '2026-01-01', createBy: 'HR总监',
  },
]

export const contractIntelligenceApi = {
  /** 获取到期合同列表 */
  async getExpiringContracts(daysRange = 90): Promise<ExpiringContract[]> {
    await delay()
    return mockExpiringContracts.filter(c => c.remainingDays <= daysRange)
  },

  /** 获取到期提醒统计 */
  async getExpiryStats(): Promise<ExpiryStats> {
    await delay()
    return {
      expired: mockExpiringContracts.filter(c => c.remainingDays < 0).length,
      urgent: mockExpiringContracts.filter(c => c.remainingDays >= 0 && c.remainingDays <= 7).length,
      warning: mockExpiringContracts.filter(c => c.remainingDays > 7 && c.remainingDays <= 30).length,
      notice: mockExpiringContracts.filter(c => c.remainingDays > 30 && c.remainingDays <= 90).length,
    }
  },

  /** 获取合同风险列表 */
  async getContractRisks(): Promise<ContractRisk[]> {
    await delay()
    return mockContractRisks
  },

  /** 获取续签推荐统计 */
  async getRenewalStats(): Promise<RenewalStats> {
    await delay()
    return {
      total: mockExpiringContracts.length,
      strong: mockExpiringContracts.filter(c => c.recommendation === 'strong').length,
      normal: mockExpiringContracts.filter(c => c.recommendation === 'normal').length,
      cautious: mockExpiringContracts.filter(c => c.recommendation === 'cautious').length,
      notRecommended: mockExpiringContracts.filter(c => c.recommendation === 'not_recommended').length,
    }
  },

  /** 获取合同模板列表 */
  async getTemplates(type?: ContractTemplateType): Promise<ContractTemplateInfo[]> {
    await delay()
    let result = [...mockContractTemplates]
    if (type) result = result.filter(t => t.templateType === type)
    return result
  },

  /** 获取模板详情 */
  async getTemplateById(id: string): Promise<ContractTemplateInfo | null> {
    await delay()
    return mockContractTemplates.find(t => t.templateId === id) || null
  },

  /** 保存合同模板 */
  async saveTemplate(data: Partial<ContractTemplateInfo>): Promise<ContractTemplateInfo> {
    await delay()
    if (data.templateId) {
      const index = mockContractTemplates.findIndex(t => t.templateId === data.templateId)
      if (index !== -1) {
        Object.assign(mockContractTemplates[index], data, { updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19) })
        return mockContractTemplates[index]
      }
    }
    const newTemplate: ContractTemplateInfo = {
      templateId: `TPL${Date.now()}`,
      templateName: data.templateName || '',
      templateType: data.templateType || 'custom',
      description: data.description || '',
      content: data.content || '',
      customClauses: data.customClauses || [],
      status: 'active',
      version: '1.0',
      updateTime: new Date().toISOString().replace('T', ' ').substring(0, 19),
      createBy: '当前用户',
    }
    mockContractTemplates.push(newTemplate)
    return newTemplate
  },

  /** 删除模板 */
  async deleteTemplate(id: string): Promise<void> {
    await delay()
    const index = mockContractTemplates.findIndex(t => t.templateId === id)
    if (index !== -1) mockContractTemplates.splice(index, 1)
  },
}

export default contractIntelligenceApi
