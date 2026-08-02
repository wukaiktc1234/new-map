/**
 * 日结对账管理 API
 *
 * 数据来源策略：调用真实后端 API，失败时抛出错误由调用方处理
 * 后端路径前缀：/v1/store-management/settlements
 */
import { get, post, put } from '@/api/request'
import { settlementDataConverter } from '@/api/store-ops/converters'
import type {
  DailySettlement,
  SettlementQueryParams,
  SettlementBackend,
  ConfirmSettlementDTO,
  PageResponse,
  OperationResult,
} from '@/types/store-operation'

/** 后端分页响应结构（MyBatis Plus IPage） */
interface BackendPage<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/** 班次明细后端响应结构 */
interface ShiftBackendVO {
  shiftName: string
  timeRange: string
  revenue: number
  orderCount: number
  cashierName: string
  paymentBreakdown?: Record<string, { amount: number; count: number }>
}

/** 上报异常请求体 */
interface ReportIssueDTO {
  issueType: string
  description: string
}

/**
 * 前端查询参数 → 后端查询参数映射
 * 前端使用 dateRange，后端使用 startDate/endDate
 */
function toBackendQuery(params: SettlementQueryParams): Record<string, unknown> {
  const backend: Record<string, unknown> = {
    current: params.page,
    size: params.size,
  }
  if (params.status) backend.status = params.status
  if (params.storeId) backend.storeId = params.storeId
  if (params.dateRange?.[0]) backend.startDate = params.dateRange[0]
  if (params.dateRange?.[1]) backend.endDate = params.dateRange[1]
  return backend
}

export const settlementApi = {
  /**
   * 分页查询日结对账列表
   */
  async getSettlementList(
    params: SettlementQueryParams,
  ): Promise<PageResponse<DailySettlement>> {
    const res = await get<BackendPage<SettlementBackend>>(
      '/v1/store-management/settlements',
      toBackendQuery(params),
    )
    return {
      records: (res?.records || []).map((r) => settlementDataConverter.toFrontend(r)),
      total: res?.total ?? 0,
      current: res?.current ?? params.page,
      size: res?.size ?? params.size,
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 获取日结对账详情
   */
  async getSettlementDetail(id: string): Promise<DailySettlement | null> {
    const res = await get<SettlementBackend>(
      `/v1/store-management/settlements/${id}`,
    )
    return res ? settlementDataConverter.toFrontend(res) : null
  },

  /**
   * 获取班次明细列表
   */
  async getShiftDetails(id: string): Promise<ShiftBackendVO[]> {
    const res = await get<ShiftBackendVO[]>(
      `/v1/store-management/settlements/${id}/shifts`,
    )
    return res ?? []
  },

  /**
   * 创建日结草稿
   * 失败时返回失败结果
   */
  async createSettlement(data: unknown): Promise<OperationResult> {
    try {
      const res = await post<SettlementBackend>(
        '/v1/store-management/settlements',
        data,
      )
      return {
        success: true,
        message: '创建成功',
        data: res ? settlementDataConverter.toFrontend(res) : undefined,
      }
    } catch {
      return { success: false, message: '创建日结失败，请重试' }
    }
  },

  /**
   * 确认对账
   * 失败时返回失败结果
   */
  async confirmSettlement(dto: ConfirmSettlementDTO): Promise<OperationResult> {
    try {
      await put<void>(
        `/v1/store-management/settlements/${dto.settlementId}/confirm`,
      )
      return { success: true, message: '确认成功' }
    } catch {
      return { success: false, message: '确认对账失败，请重试' }
    }
  },

  /**
   * 上报异常问题
   * 失败时返回失败结果
   */
  async reportIssue(
    id: string,
    data: ReportIssueDTO,
  ): Promise<OperationResult> {
    try {
      await post<void>(
        `/v1/store-management/settlements/${id}/report-issue`,
        data,
      )
      return { success: true, message: '异常申报已提交' }
    } catch {
      return { success: false, message: '异常申报失败，请重试' }
    }
  },

  /**
   * 导出日结对账数据
   * 后端返回 Excel/CSV 文件流，通过 window.open 触发下载
   * 失败时返回 false（前端可回退到本地 CSV 导出）
   */
  async exportSettlements(params: SettlementQueryParams): Promise<boolean> {
    try {
      const query = toBackendQuery(params)
      const queryString = Object.entries(query)
        .filter(([, v]) => v !== undefined && v !== null && v !== '')
        .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
        .join('&')

      // 使用 window.open 触发文件下载（后端直接返回文件流）
      const exportUrl = `/v1/store-management/settlements/export${queryString ? '?' + queryString : ''}`
      window.open(exportUrl, '_blank')
      return true
    } catch {
      return false
    }
  },
}
