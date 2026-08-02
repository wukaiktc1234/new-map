/**
 * 预约管理 API
 * 对应后端: TableReservationNewController (/v1/reservations)
 *
 * 使用标准 get/post 请求，错误由 request 拦截器统一处理。
 */
import { get, post, put } from '@/api/request'
import { orderDataConverter } from './converters'
// 金额转换统一委托给 utils/money
import { yuanToFen } from '@/utils/money'
import type {
  ReservationBackend,
  Reservation,
  ReservationQueryForm,
  ReservationStats,
  PageResponse,
  ReservationStatusValue,
} from '@/types/order'

/** 预约创建表单（金额为元字符串，前端语义） */
export interface ReservationCreateForm {
  customerName: string
  customerPhone: string
  tableId: number
  /** 门店ID */
  storeId?: number
  /** 预约日期（yyyy-MM-dd） */
  reservationDate: string
  /** 预约时间（HH:mm:ss） */
  reservationTime: string
  peopleCount: number
  /** 定金金额（元） */
  depositAmount: string
  /** 备注（可选） */
  remark?: string
}

export const reservationApi = {
  /**
   * 创建预约
   * POST /v1/reservations
   * 转换：前端表单（金额为元字符串）→ 后端 DTO（金额为分整数）
   * 业务场景：电话预约、客户到店预约、第三方平台同步录入
   */
  async create(data: ReservationCreateForm): Promise<Reservation> {
    // 元转分（统一使用 utils/money.yuanToFen）
    const depositFen = data.depositAmount
      ? yuanToFen(data.depositAmount)
      : 0
    const backendDto: Record<string, unknown> = {
      customerName: data.customerName,
      customerPhone: data.customerPhone,
      tableId: data.tableId,
      storeId: data.storeId,
      reservationDate: data.reservationDate,
      reservationTime: data.reservationTime,
      peopleCount: data.peopleCount,
      depositAmount: depositFen,
    }
    if (data.remark) backendDto.remark = data.remark

    const res = await post<ReservationBackend>('/v1/reservations', backendDto)
    return orderDataConverter.reservationToFrontend(res)
  },

  /**
   * 更新预约
   * PUT /v1/reservations/{reservationId}
   * 转换：前端表单（金额为元字符串）→ 后端 DTO（金额为分整数）
   * 后端已有 PUT 接口（TableReservationNewController.update），支持全部字段更新
   */
  async update(reservationId: number, data: ReservationCreateForm): Promise<Reservation> {
    const depositFen = data.depositAmount
      ? Math.round(Number(data.depositAmount) * 100)
      : 0
    const backendDto: Record<string, unknown> = {
      customerName: data.customerName,
      customerPhone: data.customerPhone,
      tableId: data.tableId,
      storeId: data.storeId,
      reservationDate: data.reservationDate,
      reservationTime: data.reservationTime,
      peopleCount: data.peopleCount,
      depositAmount: depositFen,
    }
    if (data.remark) backendDto.remark = data.remark

    const res = await put<ReservationBackend>(`/v1/reservations/${reservationId}`, backendDto)
    return orderDataConverter.reservationToFrontend(res)
  },

  /**
   * 分页查询预约
   * GET /v1/reservations
   * 转换：前端查询参数（语义化状态）→ 后端编码；后端响应（数字状态）→ 前端（语义化字符串）
   */
  async getList(params: ReservationQueryForm): Promise<PageResponse<Reservation>> {
    const backendParams: Record<string, unknown> = {
      page: params.page || 1,
      size: params.size || 10,
    }
    if (params.status) {
      backendParams.status = orderDataConverter.reservationStatusToBackendCode(
        params.status as ReservationStatusValue
      )
    }
    if (params.keyword) backendParams.keyword = params.keyword
    if (params.date) backendParams.date = params.date
    if (params.dateEnd) backendParams.dateEnd = params.dateEnd

    const res = await get<PageResponse<ReservationBackend>>('/v1/reservations', backendParams)
    return {
      records: orderDataConverter.reservationToFrontendList(res?.records),
      total: res?.total ?? 0,
      current: res?.current ?? (params.page || 1),
      size: res?.size ?? (params.size || 10),
      pages: res?.pages ?? 0,
    }
  },

  /**
   * 获取预约详情
   * GET /v1/reservations/{reservationId}
   */
  async getById(reservationId: number): Promise<Reservation> {
    const res = await get<ReservationBackend>(`/v1/reservations/${reservationId}`)
    return orderDataConverter.reservationToFrontend(res)
  },

  /**
   * 确认预约
   * POST /v1/reservations/{reservationId}/confirm
   * 操作人 ID 由后端从 SecurityContext 自动提取（前端不再传递 operatorId，避免伪造）
   */
  async confirm(reservationId: number): Promise<void> {
    await post(`/v1/reservations/${reservationId}/confirm`)
  },

  /**
   * 标记到店
   * POST /v1/reservations/{reservationId}/arrive
   * 操作人 ID 由后端从 SecurityContext 自动提取
   */
  async arrive(reservationId: number): Promise<void> {
    await post(`/v1/reservations/${reservationId}/arrive`)
  },

  /**
   * 取消预约
   * POST /v1/reservations/{reservationId}/cancel
   * 后端 @RequestParam(required=false, defaultValue="") String reason（可选，通过 query 传递）
   * 操作人 ID 由后端从 SecurityContext 自动提取
   */
  async cancel(reservationId: number, reason?: string): Promise<void> {
    const options = reason ? { params: { reason } } : undefined
    await post(`/v1/reservations/${reservationId}/cancel`, undefined, options)
  },

  /**
   * 预约统计
   * GET /v1/reservations/stats
   */
  async getStats(): Promise<ReservationStats> {
    const res = await get<ReservationStats>('/v1/reservations/stats')
    return {
      todayReservations: res?.todayReservations ?? 0,
      pendingConfirm: res?.pendingConfirm ?? 0,
      arrived: res?.arrived ?? 0,
      cancelRate: res?.cancelRate ?? 0,
      confirmed: res?.confirmed ?? 0,
      cancelled: res?.cancelled ?? 0,
    }
  },
}

export default reservationApi
