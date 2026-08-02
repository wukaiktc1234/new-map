/**
 * 食品追溯码 API
 * 对应后端: FoodTraceCodeController (/v1/food-trace-code)
 *
 * 改造说明（2026-06-30）：
 * - 移除所有 Mock fallback，后端故障直接抛错（食品安全：禁止假成功）
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost/silentPut）
 * - 使用 traceabilityDataConverter 做金额转换（分 ↔ 元）和状态映射
 * - 路径参数使用 encodeURIComponent 编码，防止特殊字符破坏 URL
 */
import { get, post, put } from '../request'

import type { PageResponse } from '@/types'
import type {
  FoodTraceCode,
  FoodTraceCodeBackend,
  FoodTraceCodeDisplay,
  FoodTraceCodeGenerateDTO,
  FoodTraceCodeQuery,
} from '@/types/traceability'
import { traceabilityDataConverter } from './converters'

// ============================================================
// 后端响应类型
// ============================================================

/** 后端分页响应（records 为后端原始类型，金额单位为分） */
interface FoodTraceCodePageBackend {
  records: FoodTraceCodeBackend[] | null
  total: number
  current: number
  size: number
  pages?: number
}

export const foodTraceCodeApi = {
  /**
   * 生成食品追溯码
   * POST /v1/food-trace-code/generate
   * 写操作：禁止假成功，后端不可用时抛异常（生成假追溯码会污染追溯数据）
   */
  async generate(data: FoodTraceCodeGenerateDTO): Promise<FoodTraceCode> {
    const res = await post<FoodTraceCodeBackend>('/v1/food-trace-code/generate', data)
    // 金额转换由 Converter 在展示层处理，这里返回基础类型
    return res as unknown as FoodTraceCode
  },

  /**
   * 按追溯码查询
   * GET /v1/food-trace-code/query/{traceCode}
   * 使用 Converter 转换金额（分→元）和状态标签
   */
  async getByTraceCode(traceCode: string): Promise<FoodTraceCodeDisplay | null> {
    const res = await get<FoodTraceCodeBackend | null>(
      `/v1/food-trace-code/query/${encodeURIComponent(traceCode)}`,
    )
    return res ? traceabilityDataConverter.toFoodDisplay(res) : null
  },

  /**
   * 消费者查询追溯信息
   * GET /v1/food-trace-code/trace-info/{traceCode}
   */
  async getTraceInfo(traceCode: string): Promise<unknown> {
    return await get<unknown>(`/v1/food-trace-code/trace-info/${encodeURIComponent(traceCode)}`)
  },

  /**
   * 分页查询食品追溯码列表
   * GET /v1/food-trace-code/list
   * 使用 Converter 转换列表项金额和状态标签
   */
  async getList(params: FoodTraceCodeQuery): Promise<PageResponse<FoodTraceCodeDisplay>> {
    const res = await get<FoodTraceCodePageBackend | null>(
      '/v1/food-trace-code/list',
      params as unknown as Record<string, unknown>,
    )
    const records = traceabilityDataConverter.toFoodDisplayList(res?.records ?? [])
    const total = res?.total ?? 0
    const current = res?.current ?? params.page ?? 1
    const size = res?.size ?? params.size ?? 10
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 按订单查询食品追溯码
   * GET /v1/food-trace-code/order/{orderId}
   * 使用 Converter 转换列表项金额和状态标签
   */
  async listByOrderId(orderId: string): Promise<FoodTraceCodeDisplay[]> {
    const res = await get<FoodTraceCodeBackend[] | null>(
      `/v1/food-trace-code/order/${encodeURIComponent(orderId)}`,
    )
    return traceabilityDataConverter.toFoodDisplayList(res ?? [])
  },

  /**
   * 更新制作状态
   * PUT /v1/food-trace-code/{traceCodeId}/make-status
   * 写操作：禁止假成功，后端不可用时抛异常（制作状态影响出餐追溯）
   */
  async updateMakeStatus(traceCodeId: string, makeStatus: string): Promise<boolean> {
    await put<void>(
      `/v1/food-trace-code/${encodeURIComponent(traceCodeId)}/make-status`,
      { makeStatus },
    )
    return true
  },

  /**
   * 开始制作（关联厨师）
   * POST /v1/food-trace-code/{traceCodeId}/start-make
   * 后端 @RequestParam chefId/chefName，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（厨师关联影响责任追溯）
   */
  async startMake(traceCodeId: string, chefId: number, chefName: string): Promise<boolean> {
    await post<void>(
      `/v1/food-trace-code/${encodeURIComponent(traceCodeId)}/start-make`,
      undefined,
      { params: { chefId, chefName } },
    )
    return true
  },

  /**
   * 完成制作
   * POST /v1/food-trace-code/{traceCodeId}/complete-make
   * 写操作：禁止假成功，后端不可用时抛异常（完成状态影响出餐追溯）
   */
  async completeMake(traceCodeId: string): Promise<boolean> {
    await post<void>(`/v1/food-trace-code/${encodeURIComponent(traceCodeId)}/complete-make`)
    return true
  },

  /**
   * 扫码出餐
   * POST /v1/food-trace-code/serve
   * 后端 @RequestParam traceCode，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（出餐状态影响消费者追溯）
   */
  async serve(traceCode: string): Promise<boolean> {
    await post<void>('/v1/food-trace-code/serve', undefined, { params: { traceCode } })
    return true
  },

  /**
   * 打印标签
   * POST /v1/food-trace-code/{traceCodeId}/print
   * 后端 @RequestParam(required=false) printerId，通过 query 传递
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已打印标签）
   */
  async printLabel(traceCodeId: string, printerId?: number): Promise<boolean> {
    const options = printerId !== undefined ? { params: { printerId } } : undefined
    await post<void>(
      `/v1/food-trace-code/${encodeURIComponent(traceCodeId)}/print`,
      undefined,
      options,
    )
    return true
  },

  /**
   * 批量打印标签
   * POST /v1/food-trace-code/batch-print
   * 写操作：禁止假成功，后端不可用时抛异常（避免误以为已批量打印）
   */
  async batchPrint(traceCodeIds: string[], printerId?: number): Promise<boolean> {
    await post<void>('/v1/food-trace-code/batch-print', { traceCodeIds, printerId })
    return true
  },

  /**
   * 计算订单成本
   * GET /v1/food-trace-code/order-cost/{orderId}
   * 返回值（分）通过 Converter 转为元
   */
  async calculateOrderCost(orderId: string): Promise<string> {
    const costFen = await get<number>(`/v1/food-trace-code/order-cost/${encodeURIComponent(orderId)}`)
    return traceabilityDataConverter.toOrderCostYuan(costFen ?? 0)
  },

  /**
   * 生成食品追溯码二维码
   * GET /v1/food-trace-code/qr/{traceCodeId}
   */
  async generateQrCode(traceCodeId: string): Promise<string> {
    return await get<string>(`/v1/food-trace-code/qr/${encodeURIComponent(traceCodeId)}`)
  },

  /**
   * 获取食品追溯码各状态统计
   * GET /v1/food-trace-code/statistics
   */
  async getStatistics(): Promise<Record<string, number>> {
    return await get<Record<string, number>>('/v1/food-trace-code/statistics')
  },
}

export default foodTraceCodeApi
