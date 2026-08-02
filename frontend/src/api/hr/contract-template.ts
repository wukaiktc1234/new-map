/**
 * 合同模板管理API
 * 对应后端: /v1/hr/contract-template（ContractTemplateController）
 *
 * 后端已有完整接口：CRUD + 预览（变量替换） + 复制 + 启停 + 统计
 * 8种合同类型对应8套标准模板
 */
import { get, post, put, del } from '../request'
import type {
  ContractTemplate,
  ContractTemplateFormData,
  ContractTemplateQueryParams,
  ContractTemplatePreviewData,
  ContractType,
  TemplateStatus,
} from '../../types/hr/contract'

function delay(ms = 300) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

// ============================================================
// Mock 数据：8种合同类型的标准模板
// ============================================================

const mockTemplateList: ContractTemplate[] = [
  {
    id: 'TPL001',
    templateCode: 'STANDARD_LABOR_CONTRACT',
    templateName: '标准劳动合同',
    contractType: 'labor',
    version: '2.0',
    status: 'active',
    templateContent: '<div>标准劳动合同正文模板（含{{employeeName}}、{{startDate}}等变量）</div>',
    templateVariables: [
      { name: 'employeeName', description: '员工姓名', type: 'string', required: true, example: '张伟' },
      { name: 'startDate', description: '合同开始日期', type: 'date', required: true, example: '2026-01-01' },
      { name: 'endDate', description: '合同结束日期', type: 'date', required: false, example: '2029-01-01' },
      { name: 'salary', description: '月工资标准', type: 'currency', required: true, example: '8000' },
      { name: 'departmentName', description: '部门名称', type: 'string', required: true, example: '前厅部' },
      { name: 'positionName', description: '岗位名称', type: 'string', required: true, example: '前厅经理' },
    ],
    description: '适用于标准全日制用工，依据《劳动合同法》制定',
    createdBy: '系统管理员',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 10:00:00',
  },
  {
    id: 'TPL002',
    templateCode: 'OVER_AGE_AGREEMENT',
    templateName: '超龄用工协议',
    contractType: 'over_age',
    version: '1.0',
    status: 'active',
    templateContent: '<div>超龄用工协议正文模板（依据2026.7.1生效的《超龄劳动者基本权益保障暂行规定》）</div>',
    templateVariables: [
      { name: 'employeeName', description: '员工姓名', type: 'string', required: true },
      { name: 'retirementDate', description: '退休日期', type: 'date', required: true },
      { name: 'pensionLocation', description: '养老金发放地', type: 'string', required: true },
      { name: 'insurancePolicyNo', description: '意外伤害保险单号', type: 'string', required: true },
    ],
    description: '适用于超龄返聘人员，依据2026年新规制定',
    createTime: '2026-06-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL003',
    templateCode: 'INTERN_AGREEMENT',
    templateName: '实习协议',
    contractType: 'intern',
    version: '1.5',
    status: 'active',
    templateContent: '<div>实习协议正文模板</div>',
    templateVariables: [
      { name: 'internSchool', description: '实习学校', type: 'string', required: true },
      { name: 'internMajor', description: '实习专业', type: 'string', required: true },
      { name: 'internPeriod', description: '实习期限（月）', type: 'number', required: true },
    ],
    description: '适用于职业学校学生实习',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL004',
    templateCode: 'PART_TIME_AGREEMENT',
    templateName: '非全日制用工协议',
    contractType: 'part_time',
    version: '1.0',
    status: 'active',
    templateContent: '<div>非全日制用工协议正文模板</div>',
    templateVariables: [
      { name: 'hourlyRate', description: '小时工资', type: 'currency', required: true },
      { name: 'weeklyHours', description: '每周工时', type: 'number', required: true },
    ],
    description: '适用于兼职/非全日制用工',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL005',
    templateCode: 'CONFIDENTIALITY_AGREEMENT',
    templateName: '保密协议',
    contractType: 'confidentiality',
    version: '1.0',
    status: 'active',
    templateContent: '<div>保密协议正文模板</div>',
    templateVariables: [
      { name: 'confidentialityScope', description: '保密范围', type: 'text', required: true },
      { name: 'confidentialityPeriod', description: '保密期限（月）', type: 'number', required: true },
    ],
    description: '适用于商业秘密保护',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL006',
    templateCode: 'NON_COMPETE_AGREEMENT',
    templateName: '竞业限制协议',
    contractType: 'non_compete',
    version: '1.0',
    status: 'active',
    templateContent: '<div>竞业限制协议正文模板</div>',
    templateVariables: [
      { name: 'nonCompeteCompensation', description: '竞业限制补偿金', type: 'currency', required: true },
      { name: 'nonCompetePeriod', description: '竞业限制期限（月）', type: 'number', required: true },
      { name: 'nonCompeteScope', description: '限制范围', type: 'text', required: true },
    ],
    description: '适用于高管和涉密人员',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL007',
    templateCode: 'DISPATCH_AGREEMENT',
    templateName: '劳务派遣协议',
    contractType: 'dispatch',
    version: '1.0',
    status: 'active',
    templateContent: '<div>劳务派遣协议正文模板</div>',
    templateVariables: [
      { name: 'dispatchCompanyName', description: '派遣公司名称', type: 'string', required: true },
      { name: 'dispatchPeriod', description: '派遣期限（月）', type: 'number', required: true },
    ],
    description: '适用于劳务派遣用工',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
  {
    id: 'TPL008',
    templateCode: 'OUTSOURCING_CONTRACT',
    templateName: '劳务外包合同',
    contractType: 'outsourcing',
    version: '1.0',
    status: 'active',
    templateContent: '<div>劳务外包合同正文模板</div>',
    templateVariables: [
      { name: 'outsourcingCompanyName', description: '外包公司名称', type: 'string', required: true },
      { name: 'outsourcingServiceScope', description: '服务范围', type: 'text', required: true },
    ],
    description: '适用于业务外包',
    createTime: '2026-01-01 09:00:00',
    updateTime: '2026-06-01 09:00:00',
  },
]

/**
 * 映射分页查询参数：前端 page/pageSize → 后端 current/size
 */
function mapQueryParams(params?: ContractTemplateQueryParams): Record<string, unknown> {
  if (!params) return {}
  const { page, pageSize, ...rest } = params as Record<string, unknown>
  const result: Record<string, unknown> = { ...rest }
  if (page !== undefined) result.current = page
  if (pageSize !== undefined) result.size = pageSize
  return result
}

export const contractTemplateApi = {
  /** 获取模板列表（对接后端 GET /v1/hr/contract-template/list） */
  async getList(params?: ContractTemplateQueryParams): Promise<{ records: ContractTemplate[]; total: number }> {
    const res = await get<{ records: ContractTemplate[]; total: number } | ContractTemplate[]>('/v1/hr/contract-template/list', mapQueryParams(params))
    if (Array.isArray(res)) {
      return { records: res, total: res.length }
    }
    return res as { records: ContractTemplate[]; total: number }
  },

  /** 获取启用的模板列表（对接后端 GET /v1/hr/contract-template/active） */
  async getActiveTemplates(contractType?: ContractType): Promise<ContractTemplate[]> {
    return await get<ContractTemplate[]>('/v1/hr/contract-template/active', { contractType })
  },

  /** 获取模板详情（对接后端 GET /v1/hr/contract-template/{id}） */
  async getById(id: string): Promise<ContractTemplate> {
    return await get<ContractTemplate>(`/v1/hr/contract-template/${id}`)
  },

  /** 创建模板（对接后端 POST /v1/hr/contract-template） */
  async create(data: ContractTemplateFormData): Promise<ContractTemplate> {
    return await post<ContractTemplate>('/v1/hr/contract-template', data)
  },

  /** 更新模板（对接后端 PUT /v1/hr/contract-template/{id}） */
  async update(id: string, data: ContractTemplateFormData): Promise<ContractTemplate> {
    return await put<ContractTemplate>(`/v1/hr/contract-template/${id}`, data)
  },

  /** 删除模板（对接后端 DELETE /v1/hr/contract-template/{id}） */
  async delete(id: string): Promise<void> {
    await del(`/v1/hr/contract-template/${id}`)
  },

  /** 预览模板（对接后端 POST /v1/hr/contract-template/{id}/preview） */
  async preview(data: ContractTemplatePreviewData): Promise<string> {
    return await post<string>(`/v1/hr/contract-template/${data.templateId}/preview`, data.variables)
  },

  /** 复制模板（对接后端 POST /v1/hr/contract-template/{id}/duplicate） */
  async duplicate(id: string): Promise<ContractTemplate> {
    return await post<ContractTemplate>(`/v1/hr/contract-template/${id}/duplicate`)
  },

  /** 更新模板状态（对接后端 PUT /v1/hr/contract-template/{id}/status） */
  async updateStatus(id: string, status: TemplateStatus): Promise<void> {
    await put(`/v1/hr/contract-template/${id}/status`, null, { params: { status } })
  },

  /** 获取模板统计（对接后端 GET /v1/hr/contract-template/statistics） */
  async getStatistics(): Promise<Record<string, number>> {
    return await get<Record<string, number>>('/v1/hr/contract-template/statistics')
  },
}
