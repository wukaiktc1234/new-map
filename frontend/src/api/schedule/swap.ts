/**
 * 换班管理 API
 * 对应后端: /v1/schedule/swap
 */
import { get, post, put } from '@/api/request'
import { swapRequestConverter } from './converters'
import type {
  SwapRequest,
  SwapRequestFormData,
  SwapRequestStatus,
} from '@/types/schedule'
import type { IPage } from '@/types/pagination'

/** 后端返回的换班请求数据（原始格式） */
interface SwapRequestBackend {
  requestId: string
  planId: string
  planName: string
  status: string | number
  initiatorId: string
  initiatorName: string
  initiatorEntry: Record<string, unknown>
  targetEmployeeId: string
  targetEmployeeName: string
  targetEntry: Record<string, unknown>
  reason?: string
  createTime: string
  approverId?: string
  approverName?: string
  approveTime?: string
  approveComment?: string
}

/** 待处理数量响应 */
interface PendingCountResponse {
  count: number
}

/** 分页响应类型 */
type SwapPageResponse = IPage<SwapRequestBackend>

export const swapApi = {
  /**
   * 获取换班请求列表（分页）
   * @param params - 查询参数
   * @returns 分页数据
   */
  async getList(
    params: { page?: number; size?: number; status?: SwapRequestStatus | '' }
  ): Promise<IPage<SwapRequest>> {
    const res = await get<SwapPageResponse>(
      '/v1/schedule/swap',
      params
    )
    if (!res) return { records: [], total: 0, current: 1, size: 10, pages: 0 }
    return {
      ...res,
      records: (res.records || []).map((item: SwapRequestBackend) =>
        swapRequestConverter.toFrontend(item)
      ),
    }
  },

  /**
   * 根据ID获取换班请求详情
   * @param id - 请求ID
   * @returns 请求详情
   */
  async getById(id: string): Promise<SwapRequest> {
    const res = await get<SwapRequestBackend>(
      `/v1/schedule/swap/${id}`
    )
    return swapRequestConverter.toFrontend(res)
  },

  /**
   * 创建换班申请
   * @param data - 表单数据
   * @returns 创建的请求
   */
  async create(data: SwapRequestFormData): Promise<SwapRequest> {
    const res = await post<SwapRequestBackend>(
      '/v1/schedule/swap',
      data
    )
    return swapRequestConverter.toFrontend(res)
  },

  /**
   * 审批通过换班申请
   * @param id - 请求ID
   * @param comment - 审批意见（可选）
   */
  async approve(id: string, comment?: string): Promise<void> {
    await put(`/v1/schedule/swap/${id}/approve`, { comment })
  },

  /**
   * 驳回换班申请
   * @param id - 请求ID
   * @param reason - 驳回原因
   */
  async reject(id: string, reason: string): Promise<void> {
    await put(`/v1/schedule/swap/${id}/reject`, { reason })
  },

  /**
   * 取消换班申请（发起人操作）
   * @param id - 请求ID
   */
  async cancel(id: string): Promise<void> {
    await put(`/v1/schedule/swap/${id}/cancel`)
  },

  /**
   * 获取当前用户的待处理换班请求数量
   * @returns 待处理数量
   */
  async getPendingCount(): Promise<number> {
    const res = await get<PendingCountResponse>(
      '/v1/schedule/swap/pending-count'
    )
    return res?.count ?? 0
  },
}

export default swapApi
