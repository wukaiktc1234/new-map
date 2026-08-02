/**
 * 申诉举报API模块
 *
 * 封装处罚申诉与投诉举报的API请求
 * 使用 request 实例发送请求，响应拦截器已自动提取 .data
 */
import request from './request'

// ==================== 类型定义 ====================

/** 申诉类型 */
export type AppealType = 'penalty' | 'complaint'

/** 申诉状态 */
export type AppealStatus = 'pending' | 'processing' | 'resolved' | 'closed' | 'withdrawn'

/** 创建申诉请求参数 */
export interface AppealCreatePayload {
  type: AppealType
  anonymousFlag: boolean
  title: string
  description: string
  targetDecisionId?: string
  expectedResult?: string
}

/** 申诉列表项 */
export interface AppealItem {
  appealId: string
  type: AppealType
  anonymousFlag: boolean
  status: AppealStatus
  title: string
  createTime: string
  attachmentCount: number
}

/** 申诉附件 */
export interface AppealAttachment {
  attachmentId: string
  fileName: string
  fileType: string
  fileSize: number
  fileUrl: string
}

/** 申诉处理日志 */
export interface AppealProcessLog {
  logId: string
  action: string
  operatorName: string
  comment: string
  createTime: string
}

/** 申诉详情 */
export interface AppealDetail extends AppealItem {
  description: string
  targetDecisionId?: string
  expectedResult?: string
  attachments: AppealAttachment[]
  processLogs: AppealProcessLog[]
}

/** 分页查询参数 */
export interface AppealQueryParams {
  status?: AppealStatus
  page: number
  size: number
}

/** 分页响应 */
export interface AppealPageResponse {
  records: AppealItem[]
  total: number
  current: number
  size: number
}

// ==================== API方法 ====================

export const appealApi = {
  /**
   * 创建申诉
   * @param data 申诉表单数据
   * @returns 申诉详情
   */
  create(data: AppealCreatePayload): Promise<AppealDetail> {
    return request.post('/v1/appeals', data)
  },

  /**
   * 获取我的申诉列表
   * @param params 查询参数（支持状态筛选+分页）
   * @returns 分页申诉列表
   */
  getList(params: AppealQueryParams): Promise<AppealPageResponse> {
    return request.get('/v1/appeals', params)
  },

  /**
   * 获取申诉详情
   * @param id 申诉ID
   * @returns 申诉完整详情
   */
  getById(id: string): Promise<AppealDetail> {
    return request.get(`/v1/appeals/${id}`)
  },

  /**
   * 撤回申诉
   * @param id 申诉ID
   */
  withdraw(id: string): Promise<void> {
    return request.put(`/v1/appeals/${id}/withdraw`)
  },
}
