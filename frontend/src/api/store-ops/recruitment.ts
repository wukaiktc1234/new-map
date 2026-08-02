/**
 * 门店招聘管理 API
 *
 * 后端 Controller：StoreManagementRecruitmentController
 * 路径前缀：/v1/store-management/recruitment
 *
 * 注意：
 * 1. 后端仅覆盖"审批流程"端点（提交申请/我的申请/待审批/通过/驳回/详情），
 *    且后端 RecruitmentApprovalVO 为"录用审批记录"模型，与前端 Recruitment（招聘岗位）模型存在语义差异。
 * 2. 前端的"招聘岗位列表/应聘者列表/面试安排"等功能后端缺失，
 *    getRecruitmentList 调用真实后端 /approvals/my-list，做尽力转换，失败时抛错由调用方处理。
 * 3. 原内置 Mock 数据已在 Wave 3（P0-4）中移除，统一走真实 API。
 */
import { get, post, put } from '../request'

// ============================================================
// 后端响应类型定义（与 RecruitmentApprovalVO 字段匹配）
// ============================================================

/** 后端审批历史项（对应 RecruitmentApprovalVO.ApprovalHistoryItem） */
export interface ApprovalHistoryItemBackend {
  historyId?: string
  action?: string
  actionName?: string
  operatorId?: string
  operatorName?: string
  level?: number
  comment?: string
  operateTime?: string
  [key: string]: unknown
}

/** 后端 RecruitmentApprovalVO 响应类型 */
export interface RecruitmentApprovalBackend {
  approvalId?: string
  postId?: string
  postName?: string
  applicantName?: string
  phone?: string
  proposedSalary?: string
  interviewScore?: number
  interviewerId?: string
  interviewerName?: string
  currentLevel?: number
  currentLevelName?: string
  approvalStatus?: string
  approvalStatusName?: string
  rejectionReason?: string
  approvedAt?: string
  finalDecision?: string
  hiredAt?: string
  employeeId?: string
  createTime?: string
  approvalHistory?: ApprovalHistoryItemBackend[]
  [key: string]: unknown
}

/** 后端分页响应（MyBatis-Plus IPage 结构） */
interface BackendPageResponse<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
  [key: string]: unknown
}

// ============================================================
// 前端业务类型定义
// ============================================================

/** 招聘记录（前端岗位模型，与视图中的 Recruitment 一致） */
export interface Recruitment {
  recruitmentId: string
  positionName: string
  department: string
  headcount: number
  hiredCount?: number
  onboardedCount?: number
  salaryMin: number
  salaryMax: number
  salaryRange: string
  publishDate: string
  deadline: string
  applicants: number
  source: 'store' | 'hr'
  urgency: 'normal' | 'urgent' | 'emergency'
  status: 'open' | 'closed' | 'paused' | 'filled'
  description?: string
  approvalStatus: 'pending' | 'store_approved' | 'regional_approved' | 'hr_approved' | 'rejected'
  currentApprover?: 'regional_manager' | 'hr_director'
  isSpecialPosition?: boolean
  exceedQuota?: boolean
}

/** 提交招聘申请参数（映射到后端 RecruitmentApprovalCreateDTO） */
export interface SubmitApplicationParams {
  /** 招聘岗位ID */
  postId: string
  /** 应聘者姓名（岗位发布场景下用店长姓名占位） */
  applicantName: string
  /** 联系电话（需符合手机号格式，失败时走 Mock） */
  phone: string
  /** 建议薪资（元/月） */
  proposedSalary: number
  /** 面试评分（1-100） */
  interviewScore: number
  /** 面试官评语（20-1000字） */
  interviewerComment: string
  /** 面试官ID */
  interviewerId?: string
  /** 岗位编码 */
  positionCode?: string
}

/** 操作结果 */
export interface OperationResult {
  success: boolean
  message: string
  data?: unknown
}

/** 查询参数 */
export interface RecruitmentQueryParams {
  page?: number
  size?: number
}

// ============================================================
// 数据转换工具
// ============================================================

/**
 * 将后端 RecruitmentApprovalVO 转换为前端 Recruitment 类型
 *
 * 注意：后端审批记录为"录用申请"模型，前端为"招聘岗位"模型，语义存在差异。
 * 此处做尽力转换；无法映射的字段保留默认值。
 */
function toFrontendRecruitment(backend: RecruitmentApprovalBackend): Recruitment {
  const salaryNum = Number(backend.proposedSalary) || 0
  return {
    recruitmentId: backend.approvalId ?? backend.postId ?? '',
    positionName: backend.postName ?? '未知岗位',
    department: '后厨部',
    headcount: 1,
    hiredCount: 0,
    onboardedCount: 0,
    salaryMin: salaryNum,
    salaryMax: salaryNum,
    salaryRange: backend.proposedSalary ?? '',
    publishDate: backend.createTime?.split('T')[0] ?? '',
    deadline: '',
    applicants: 0,
    source: 'store',
    urgency: 'normal',
    status: mapBackendApprovalStatusToRecruitmentStatus(backend.approvalStatus),
    description: backend.finalDecision ?? '',
    approvalStatus: mapBackendApprovalStatus(backend.approvalStatus),
    isSpecialPosition: false,
    exceedQuota: false,
  }
}

/** 后端审批状态映射为前端岗位状态 */
function mapBackendApprovalStatusToRecruitmentStatus(status?: string): Recruitment['status'] {
  switch (status) {
    case 'pending': return 'open'
    case 'approved':
    case 'hired': return 'filled'
    case 'rejected':
    case 'withdrawn': return 'closed'
    default: return 'open'
  }
}

/** 后端审批状态映射为前端审批状态 */
function mapBackendApprovalStatus(status?: string): Recruitment['approvalStatus'] {
  switch (status) {
    case 'pending': return 'pending'
    case 'approved': return 'hr_approved'
    case 'rejected': return 'rejected'
    default: return 'pending'
  }
}

// ============================================================
// 招聘管理 API
// ============================================================

export const recruitmentApi = {
  /**
   * 获取招聘列表
   *
   * 后端端点：GET /v1/store-management/recruitment/approvals/my-list
   *
   * 注意：后端返回的是"我的申请"审批记录，与前端"招聘岗位列表"语义不同。
   *      API 成功时做尽力转换；失败时抛错由调用方处理。
   */
  async getRecruitmentList(params?: RecruitmentQueryParams): Promise<Recruitment[]> {
    const res = await get<BackendPageResponse<RecruitmentApprovalBackend>>(
      '/v1/store-management/recruitment/approvals/my-list',
      { page: params?.page ?? 1, size: params?.size ?? 100 },
    )
    if (!res || !Array.isArray(res.records)) {
      throw new Error('后端响应数据格式不正确')
    }
    return res.records.map(toFrontendRecruitment)
  },

  /**
   * 提交招聘申请
   *
   * 后端端点：POST /v1/store-management/recruitment/approvals
   *
   * 注意：后端此端点为"录用申请"（针对具体应聘者），
   *      前端"岗位发布申请"语义不同，字段做尽力映射。失败时抛错由调用方处理。
   */
  async submitApplication(params: SubmitApplicationParams): Promise<OperationResult> {
    const backendDto = {
      postId: params.postId,
      applicantName: params.applicantName,
      phone: params.phone,
      proposedSalary: String(params.proposedSalary),
      interviewScore: params.interviewScore,
      interviewerComment: params.interviewerComment,
      interviewerId: params.interviewerId ?? '',
      positionCode: params.positionCode ?? '',
    }
    await post<RecruitmentApprovalBackend>(
      '/v1/store-management/recruitment/approvals',
      backendDto,
    )
    return { success: true, message: '申请已提交' }
  },

  /**
   * 获取待我审批的招聘申请列表
   *
   * 后端端点：GET /v1/store-management/recruitment/approvals/pending-me
   * 响应非数组时返回空数组（容错处理）
   */
  async getPendingApprovals(): Promise<RecruitmentApprovalBackend[]> {
    const res = await get<RecruitmentApprovalBackend[]>(
      '/v1/store-management/recruitment/approvals/pending-me',
    )
    if (!Array.isArray(res)) {
      return []
    }
    return res
  },

  /**
   * 审批通过
   *
   * 后端端点：PUT /v1/store-management/recruitment/approvals/{id}/approve
   * 失败时抛错由调用方处理
   */
  async approve(id: string): Promise<OperationResult> {
    await put<void>(`/v1/store-management/recruitment/approvals/${id}/approve`)
    return { success: true, message: '审批通过' }
  },

  /**
   * 审批驳回
   *
   * 后端端点：PUT /v1/store-management/recruitment/approvals/{id}/reject
   * 请求体：{ approvalStatus: 'rejected', rejectionReason: string }
   * 失败时抛错由调用方处理
   */
  async reject(id: string, reason: string): Promise<OperationResult> {
    const backendDto = {
      approvalStatus: 'rejected',
      rejectionReason: reason,
    }
    await put<void>(
      `/v1/store-management/recruitment/approvals/${id}/reject`,
      backendDto,
    )
    return { success: true, message: '已驳回' }
  },

  /**
   * 获取审批详情
   *
   * 后端端点：GET /v1/store-management/recruitment/approvals/{id}
   * 响应为空时返回 null（容错处理）
   */
  async getApprovalDetail(id: string): Promise<RecruitmentApprovalBackend | null> {
    const res = await get<RecruitmentApprovalBackend>(
      `/v1/store-management/recruitment/approvals/${id}`,
    )
    return res ?? null
  },
}
