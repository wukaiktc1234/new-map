/**
 * 原料追溯码 API
 * 对应后端: MaterialTraceCodeController (/v1/material-trace-code)
 *
 * 改造说明（2026-06-30）：
 * - 移除所有 Mock fallback，后端故障直接抛错（食品安全：禁止假成功）
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost/silentPut）
 * - 使用 traceabilityDataConverter 做金额转换（分 ↔ 元）、日期格式化、剩余天数计算
 * - 路径参数使用 encodeURIComponent 编码，防止特殊字符破坏 URL
 */
import { get, post, put } from '../request'

import type { PageResponse } from '@/types'
import type {
  MaterialTraceCode,
  MaterialTraceCodeBackend,
  MaterialTraceCodeDisplay,
  MaterialTraceCodeGenerateDTO,
  MaterialTraceCodeQuery,
  MaterialTraceCodeStatus,
} from '@/types/traceability'
import { traceabilityDataConverter } from './converters'

// ============================================================
// 后端响应类型
// ============================================================

/** 后端分页响应（records 为后端原始类型，金额单位为分） */
interface MaterialTraceCodePageBackend {
  records: MaterialTraceCodeBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const materialTraceCodeApi = {
  /**
   * 批量生成原料追溯码
   * POST /v1/material-trace-code/generate
   * 写操作：禁止假成功，后端不可用时抛异常（生成假追溯码会污染追溯数据）
   */
  async generate(data: MaterialTraceCodeGenerateDTO): Promise<MaterialTraceCode[]> {
    const res = await post<MaterialTraceCodeBackend[]>('/v1/material-trace-code/generate', data)
    // 生成接口返回基础类型列表，金额字段保留原值（分）
    return res as unknown as MaterialTraceCode[]
  },

  /**
   * 按追溯码查询
   * GET /v1/material-trace-code/query/{traceCode}
   * 使用 Converter 转换金额（分→元）、日期格式、剩余天数
   */
  async getByTraceCode(traceCode: string): Promise<MaterialTraceCodeDisplay | null> {
    const res = await get<MaterialTraceCodeBackend | null>(
      `/v1/material-trace-code/query/${encodeURIComponent(traceCode)}`,
    )
    return res ? traceabilityDataConverter.toMaterialDisplay(res) : null
  },

  /**
   * 分页查询原料追溯码列表
   * GET /v1/material-trace-code/list
   *
   * 参数名映射（前端 → 后端 list 方法 @RequestParam）：
   * - productId → materialId
   * - status → status（一致）
   * - storageLocation → storageLocation（一致）
   * - batchNumber → batchNumber（一致）
   * - page/size 后端暂不支持分页参数，仅作兼容
   * 使用 Converter 转换列表项金额和日期
   */
  async getList(params: MaterialTraceCodeQuery): Promise<PageResponse<MaterialTraceCodeDisplay>> {
    const backendParams: Record<string, unknown> = {
      page: params.page || 1,
      size: params.size || 10,
    }
    if (params.productId) backendParams.materialId = params.productId
    if (params.status) backendParams.status = params.status
    if (params.storageLocation) backendParams.storageLocation = params.storageLocation
    if (params.batchNumber) backendParams.batchNumber = params.batchNumber

    const res = await get<MaterialTraceCodePageBackend | null>(
      '/v1/material-trace-code/list',
      backendParams,
    )
    const records = traceabilityDataConverter.toMaterialDisplayList(res?.records ?? [])
    const total = res?.total ?? 0
    const current = res?.current ?? params.page ?? 1
    const size = res?.size ?? params.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 按采购入库单查询
   * GET /v1/material-trace-code/stockin/{stockinId}
   * 使用 Converter 转换列表项金额和日期
   */
  async listByStockinId(stockinId: number): Promise<MaterialTraceCodeDisplay[]> {
    const res = await get<MaterialTraceCodeBackend[] | null>(
      `/v1/material-trace-code/stockin/${encodeURIComponent(stockinId)}`,
    )
    return traceabilityDataConverter.toMaterialDisplayList(res ?? [])
  },

  /**
   * 更新原料追溯码状态
   * PUT /v1/material-trace-code/{traceCodeId}/status
   * 写操作：禁止假成功，后端不可用时抛异常（状态变更影响食品安全追溯）
   */
  async updateStatus(
    traceCodeId: string,
    status: MaterialTraceCodeStatus,
    operatorName?: string,
    reason?: string,
  ): Promise<boolean> {
    await put<void>(`/v1/material-trace-code/${encodeURIComponent(traceCodeId)}/status`, {
      status,
      operatorName,
      reason,
    })
    return true
  },

  /**
   * 领用原料追溯码
   * POST /v1/material-trace-code/pick
   * 后端 @RequestParam traceCode/usedById/usedByName/usagePurpose，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（领用变更库存归属）
   */
  async pick(data: {
    traceCode: string
    usedById: number
    usedByName: string
    usagePurpose?: string
  }): Promise<boolean> {
    const params: Record<string, unknown> = {
      traceCode: data.traceCode,
      usedById: data.usedById,
      usedByName: data.usedByName,
    }
    if (data.usagePurpose !== undefined) {
      params.usagePurpose = data.usagePurpose
    }
    await post<void>('/v1/material-trace-code/pick', undefined, { params })
    return true
  },

  /**
   * 退回原料追溯码
   * POST /v1/material-trace-code/return
   * 后端 @RequestParam traceCode/reason，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（退回变更库存状态）
   */
  async return(traceCode: string, reason: string): Promise<boolean> {
    await post<void>(
      '/v1/material-trace-code/return',
      undefined,
      { params: { traceCode, reason } },
    )
    return true
  },

  /**
   * 查询过期原料追溯码
   * GET /v1/material-trace-code/expired
   * 使用 Converter 转换列表项金额和日期
   */
  async getExpired(): Promise<MaterialTraceCodeDisplay[]> {
    const res = await get<MaterialTraceCodeBackend[] | null>('/v1/material-trace-code/expired')
    return traceabilityDataConverter.toMaterialDisplayList(res ?? [])
  },

  /**
   * 查询临期原料追溯码
   * GET /v1/material-trace-code/expiring-soon
   * 使用 Converter 转换列表项金额和日期
   */
  async getExpiringSoon(days = 7): Promise<MaterialTraceCodeDisplay[]> {
    const res = await get<MaterialTraceCodeBackend[] | null>(
      '/v1/material-trace-code/expiring-soon',
      { days },
    )
    return traceabilityDataConverter.toMaterialDisplayList(res ?? [])
  },

  /**
   * 标记过期原料追溯码
   * POST /v1/material-trace-code/mark-expired
   * 写操作：禁止假成功，后端不可用时抛异常（标记过期影响库存可用性）
   */
  async markExpired(): Promise<number> {
    return await post<number>('/v1/material-trace-code/mark-expired')
  },

  /**
   * 生成原料追溯码二维码
   * GET /v1/material-trace-code/qr/{traceCodeId}
   */
  async generateQrCode(traceCodeId: string): Promise<string> {
    return await get<string>(`/v1/material-trace-code/qr/${encodeURIComponent(traceCodeId)}`)
  },

  /**
   * 批量打印原料追溯码
   * POST /v1/material-trace-code/print
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已打印）
   */
  async batchPrint(traceCodeIds: string[], printerId?: number): Promise<boolean> {
    await post<void>('/v1/material-trace-code/print', { traceCodeIds, printerId })
    return true
  },

  /**
   * 获取原料追溯码各状态统计
   * GET /v1/material-trace-code/statistics
   */
  async getStatistics(): Promise<Record<string, number>> {
    return await get<Record<string, number>>('/v1/material-trace-code/statistics')
  },

  /**
   * 扫码确认入库
   * POST /v1/material-trace-code/confirm-stockin
   * 后端 @RequestParam traceCode/operatorName，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（入库变更库存状态）
   */
  async confirmStockin(traceCode: string, operatorName?: string): Promise<boolean> {
    const params: Record<string, unknown> = { traceCode }
    if (operatorName !== undefined) {
      params.operatorName = operatorName
    }
    await post<void>('/v1/material-trace-code/confirm-stockin', undefined, { params })
    return true
  },

  /**
   * 批量扫码确认入库
   * POST /v1/material-trace-code/batch-confirm-stockin
   * 写操作：禁止假成功，后端不可用时抛异常（批量入库变更库存状态）
   */
  async batchConfirmStockin(traceCodes: string[], operatorName?: string): Promise<number> {
    return await post<number>('/v1/material-trace-code/batch-confirm-stockin', {
      traceCodes,
      operatorName,
    })
  },
}

export default materialTraceCodeApi
