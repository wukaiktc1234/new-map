/**
 * 招聘管理API
 * 对应后端（按模块分散至独立 Controller）：
 * - 职位: /v1/recruitment-requirements (RecruitmentRequirementController)
 * - 简历: /v1/resumes (ResumeController)
 * - 面试: /v1/interviews (InterviewController)
 * - 录用: /v1/job-offers (JobOfferController)
 */
import { get, post, put, del } from '../request'
import type {
  RecruitmentPosition,
  Resume,
  InterviewRecord,
  HireRecord,
  PositionCreateDTO,
  PositionUpdateDTO,
  PositionQueryParams,
  ResumeCreateDTO,
  ResumeQueryParams,
  InterviewCreateDTO,
  InterviewQueryParams,
  HireCreateDTO,
  HireQueryParams,
  RecruitmentStatistics,
  HrEvaluationSubmitDTO,
} from '../../types/hr/recruitment'
import { RecruitmentStatus, ResumeStatus } from '../../types/hr/recruitment'

/**
 * 查询参数映射
 * 将前端的分页参数名称映射为后端API所需的名称
 */
function mapQueryParams<T extends { page?: number; pageSize?: number }>(params: T): Record<string, unknown> {
  const { page, pageSize, ...rest } = params
  return {
    ...rest,
    current: page,
    size: pageSize,
  } as Record<string, unknown>
}

/** 后端招聘需求原始结构 */
interface RecruitmentRequirementBackend {
  id: string
  positionName: string
  departmentId: string
  departmentName: string
  requirementNum: number
  status: string
  salaryRange?: string
  educationRequirement?: string
  workLocation?: string
  description?: string
  requirements?: string
  applyDeadline?: string
  createdAt?: string
  createdByName?: string
  applicantCount?: number
  hiredCount?: number
}

/** 后端状态 → 前端招聘状态 */
function mapBackendStatus(status?: string): RecruitmentStatus {
  switch (status) {
    case 'open': return RecruitmentStatus.PUBLISHED
    case 'closed': return RecruitmentStatus.CLOSED
    case 'filled': return RecruitmentStatus.CLOSED
    case 'draft': return RecruitmentStatus.DRAFT
    default: return RecruitmentStatus.DRAFT
  }
}

/** 后端招聘需求 → 前端招聘职位 */
function mapBackendPosition(item: RecruitmentRequirementBackend): RecruitmentPosition {
  return {
    id: item.id,
    positionName: item.positionName,
    departmentId: item.departmentId,
    departmentName: item.departmentName,
    headcount: item.requirementNum ?? 1,
    status: mapBackendStatus(item.status),
    salaryRange: item.salaryRange,
    educationRequirement: (item.educationRequirement as EducationRequirement) || 'none',
    experienceRequirement: item.workLocation,
    jobDescription: item.description || '',
    requirements: item.requirements || '',
    publishDate: item.createdAt,
    deadline: item.applyDeadline?.slice(0, 10) || '',
    applicantCount: item.applicantCount ?? 0,
    hiredCount: item.hiredCount ?? 0,
    creatorName: item.createdByName || '',
    createTime: item.createdAt || '',
    updateTime: item.createdAt || '',
  }
}

/** 前端招聘职位表单 → 后端招聘需求请求体 */
function buildPositionPayload(data: PositionCreateDTO | PositionUpdateDTO, publishNow: boolean): Record<string, unknown> {
  return {
    positionName: data.positionName,
    departmentId: data.departmentId,
    departmentName: data.departmentName,
    requirementNum: data.headcount,
    salaryRange: data.salaryRange,
    educationRequirement: data.educationRequirement,
    workLocation: data.experienceRequirement,
    description: data.jobDescription,
    requirements: data.requirements,
    applyDeadline: data.deadline ? `${data.deadline} 00:00:00` : undefined,
    approvalStatus: publishNow ? 'approved' : 'draft',
  }
}

// ============================================================
// 职位管理 API
// ============================================================

export const positionApi = {
  /** 分页查询招聘职位（GET /v1/recruitment-requirements） */
  async getList(params?: PositionQueryParams): Promise<{ records: RecruitmentPosition[]; total: number }> {
    const mapped = mapQueryParams(params || {} as PositionQueryParams)
    const statusMap: Record<string, string> = { published: 'open', closed: 'closed', cancelled: 'closed', draft: 'draft' }
    if (params?.status) {
      mapped.status = statusMap[params.status] || params.status
    }
    const res = await get<{ records: RecruitmentRequirementBackend[]; total: number }>('/v1/recruitment-requirements', mapped)
    return {
      records: (res?.records ?? []).map(mapBackendPosition),
      total: res?.total ?? 0,
    }
  },

  /** 根据ID获取职位详情（GET /v1/recruitment-requirements/{id}） */
  async getById(id: string): Promise<RecruitmentPosition> {
    const res = await get<RecruitmentRequirementBackend>('/v1/recruitment-requirements/' + id)
    return mapBackendPosition(res)
  },

  /** 创建职位（POST /v1/recruitment-requirements） */
  async create(data: PositionCreateDTO, publishNow = false): Promise<RecruitmentPosition> {
    const res = await post<RecruitmentRequirementBackend>('/v1/recruitment-requirements', buildPositionPayload(data, publishNow))
    return mapBackendPosition(res)
  },

  /** 更新职位（PUT /v1/recruitment-requirements/{id}） */
  async update(id: string, data: PositionUpdateDTO, publishNow = false): Promise<RecruitmentPosition> {
    const res = await put<RecruitmentRequirementBackend>('/v1/recruitment-requirements/' + id, buildPositionPayload(data, publishNow))
    return mapBackendPosition(res)
  },

  /** 发布职位（POST /v1/recruitment-requirements/{id}/approve） */
  async publish(id: string): Promise<void> {
    await post('/v1/recruitment-requirements/' + id + '/approve', undefined, {
      params: { approvalStatus: 'published' },
    })
  },

  /** 关闭职位（POST /v1/recruitment-requirements/{id}/approve） */
  async close(id: string): Promise<void> {
    await post('/v1/recruitment-requirements/' + id + '/approve', undefined, {
      params: { approvalStatus: 'closed' },
    })
  },

  /**
   * 取消职位
   * 注意：后端未实现 cancel 端点，暂用 approve 标记为取消状态。
   * 待后端补全后切换为真实端点。
   */
  async cancel(id: string): Promise<void> {
    await post('/v1/recruitment-requirements/' + id + '/approve', undefined, {
      params: { approvalStatus: 'cancelled' },
    })
  },

  /** 删除职位（DELETE /v1/recruitment-requirements/{id}） */
  async delete(id: string): Promise<void> {
    await del('/v1/recruitment-requirements/' + id)
  },
}

// ============================================================
// 简历管理 API
// ============================================================

/** 后端简历状态 → 前端简历状态 */
function mapBackendResumeStatus(status?: string): ResumeStatus {
  switch (status) {
    case 'pending': return ResumeStatus.SUBMITTED
    case 'reviewing': return ResumeStatus.SCREENING
    case 'interviewed': return ResumeStatus.INTERVIEWING
    case 'offered': return ResumeStatus.OFFERED
    case 'hired': return ResumeStatus.HIRED
    case 'rejected': return ResumeStatus.REJECTED
    default: return ResumeStatus.SUBMITTED
  }
}

/** 后端简历 → 前端简历 */
function mapBackendResume(item: Record<string, unknown>): Resume {
  return {
    id: String(item.id ?? ''),
    positionId: String(item.requirementId ?? ''),
    positionName: String(item.positionName ?? ''),
    applicantName: String(item.candidateName ?? ''),
    phone: String(item.phone ?? ''),
    email: item.email ? String(item.email) : undefined,
    gender: (item.gender as 'male' | 'female') || undefined,
    age: item.age ? Number(item.age) : undefined,
    education: String(item.education ?? ''),
    experience: item.experience ? String(item.experience) : undefined,
    resumeUrl: item.attachmentUrl ? String(item.attachmentUrl) : undefined,
    status: mapBackendResumeStatus(item.status as string),
    submitTime: String(item.createdAt ?? ''),
    remark: item.notes ? String(item.notes) : undefined,
  } as Resume
}

export const resumeApi = {
  /** 分页查询简历（GET /v1/resumes） */
  async getList(params?: ResumeQueryParams): Promise<{ records: Resume[]; total: number }> {
    const query: Record<string, unknown> = { ...mapQueryParams(params || {} as ResumeQueryParams) }
    if (params?.positionId) {
      query.requirementId = params.positionId
      delete query.positionId
    }
    const res = await get<{ records: Record<string, unknown>[]; total: number }>('/v1/resumes', query)
    return {
      records: (res?.records ?? []).map(mapBackendResume),
      total: res?.total ?? 0,
    }
  },

  /** 根据ID获取简历详情（GET /v1/resumes/{id}） */
  async getById(id: string): Promise<Resume> {
    const res = await get<Record<string, unknown>>('/v1/resumes/' + id)
    return mapBackendResume(res)
  },

  /** 创建简历（投递）（POST /v1/resumes） */
  async create(data: ResumeCreateDTO): Promise<Resume> {
    return await post<Resume>('/v1/resumes', data)
  },

  /** 更新简历状态（PUT /v1/resumes/{id}/status?status=xxx） */
  async updateStatus(id: string, status: ResumeStatus): Promise<void> {
    await put('/v1/resumes/' + id + '/status', undefined, {
      params: { status },
    })
  },

  /**
   * 拒绝简历
   * 后端未提供独立的 reject 端点，通过 updateStatus 将状态置为 rejected 实现。
   */
  async reject(id: string, _remark?: string): Promise<void> {
    await put('/v1/resumes/' + id + '/status', undefined, {
      params: { status: ResumeStatus.REJECTED },
    })
  },
}

// ============================================================
// 面试管理 API
// ============================================================

/** 前端面试表单 → 后端面试请求体 */
function mapInterviewPayload(data: InterviewCreateDTO): Record<string, unknown> {
  const roundMap: Record<number, string> = { 1: 'first', 2: 'second', 3: 'final' }
  const typeMap: Record<string, string> = { phone: 'online', video: 'online', onsite: 'offline' }
  return {
    resumeId: data.resumeId,
    interviewRound: roundMap[data.interviewRound] || 'first',
    interviewType: typeMap[data.interviewType] || 'offline',
    interviewDate: data.scheduledTime,
    interviewerName: data.interviewerName,
    status: 'scheduled',
  }
}

export const interviewApi = {
  /** 分页查询面试记录（GET /v1/interviews） */
  async getList(params?: InterviewQueryParams): Promise<{ records: InterviewRecord[]; total: number }> {
    return await get<{ records: InterviewRecord[]; total: number }>('/v1/interviews', mapQueryParams(params || {} as InterviewQueryParams))
  },

  /** 创建面试记录（POST /v1/interviews） */
  async create(data: InterviewCreateDTO): Promise<InterviewRecord> {
    return await post<InterviewRecord>('/v1/interviews', mapInterviewPayload(data))
  },

  /** 更新面试结果（PUT /v1/interviews/{id}/result?result=xxx&feedback=xxx） */
  async updateResult(
    id: string,
    result: InterviewRecord['result'],
    evaluation?: string,
  ): Promise<void> {
    await put('/v1/interviews/' + id + '/result', undefined, {
      params: {
        result,
        ...(evaluation ? { feedback: evaluation } : {}),
      },
    })
  },

  /**
   * 创建人事面谈（POST /v1/interviews/{id}/hr-interview）
   * 门店初面通过后，HR 接手进行人事面谈，登记人事面试官信息并标记状态为 scheduled。
   */
  async createHrInterview(
    id: string,
    hrInterviewerId: string,
    hrInterviewerName: string,
  ): Promise<InterviewRecord> {
    return await post<InterviewRecord>('/v1/interviews/' + id + '/hr-interview', undefined, {
      params: { hrInterviewerId, hrInterviewerName },
    })
  },

  /**
   * 提交人事面评（PUT /v1/interviews/{id}/hr-evaluation）
   * HR 提交人事面评内容、评分、学历核验、背调结果及最终人事面谈状态。
   */
  async submitHrEvaluation(id: string, data: HrEvaluationSubmitDTO): Promise<InterviewRecord> {
    return await put<InterviewRecord>('/v1/interviews/' + id + '/hr-evaluation', data)
  },
}

// ============================================================
// 录用管理 API
// ============================================================

export const hireApi = {
  /** 分页查询录用记录（GET /v1/job-offers） */
  async getList(params?: HireQueryParams): Promise<{ records: HireRecord[]; total: number }> {
    return await get<{ records: HireRecord[]; total: number }>('/v1/job-offers', mapQueryParams(params || {} as HireQueryParams))
  },

  /** 创建录用记录（发Offer）（POST /v1/job-offers） */
  async create(data: HireCreateDTO): Promise<HireRecord> {
    return await post<HireRecord>('/v1/job-offers', data)
  },

  /**
   * 更新录用状态
   * 根据状态值路由到后端对应端点：
   * - 'accepted'  → PUT /v1/job-offers/{id}/accept
   * - 'declined'  → PUT /v1/job-offers/{id}/reject
   * - 'onboarded' → POST /v1/job-offers/{id}/convert-to-onboarding
   */
  async updateStatus(id: string, status: HireRecord['status']): Promise<void> {
    switch (status) {
      case 'accepted':
        await put('/v1/job-offers/' + id + '/accept')
        break
      case 'declined':
        await put('/v1/job-offers/' + id + '/reject')
        break
      case 'onboarded':
        await post('/v1/job-offers/' + id + '/convert-to-onboarding')
        break
      default:
        throw new Error(`不支持的录用状态: ${status}`)
    }
  },
}

// ============================================================
// 招聘统计 API
// ============================================================

export const recruitmentStatsApi = {
  /** 获取招聘统计数据 */
  async getStatistics(): Promise<RecruitmentStatistics> {
    return await get<RecruitmentStatistics>('/v1/recruitment-requirements/statistics')
  },
}

// ============================================================
// 统一导出
// ============================================================

export const recruitmentApi = {
  position: positionApi,
  resume: resumeApi,
  interview: interviewApi,
  hire: hireApi,
  stats: recruitmentStatsApi,
}
