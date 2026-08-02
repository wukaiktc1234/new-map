/**
 * 追溯码核心 API
 * 对应后端: TraceCodeController (/v1/trace-codes)
 *
 * 改造说明（2026-06-30）：
 * - 移除所有 Mock fallback，后端故障直接抛错（食品安全：禁止假成功）
 * - 改用命名导出 get/post/put（替代 silentGet/silentPost/silentPut）
 * - 状态映射在 API 边界完成（前端字符串 ↔ 后端 Integer）
 * - 路径参数使用 encodeURIComponent 编码，防止特殊字符破坏 URL
 */
import { get, post, put } from '../request'

import type { PageResponse } from '@/types'
import type {
  TraceCodeCreateDTO,
  TraceCodeQueryDTO,
  TraceCodeVO,
  TraceChainNodeVO,
  RecallQueryResultVO,
} from '@/types/traceability'

// ============================================================
// 后端查询参数类型
// ============================================================

/** 追溯码分页查询参数（传给后端） */
interface TraceCodeQueryParams {
  current: number
  size: number
  keyword?: string
  batchNo?: string
  supplierId?: number
  status?: number
  startDate?: string
  endDate?: string
}

/** 追溯码分页响应（后端 IPage） */
interface TraceCodePageBackend {
  records: TraceCodeVO[] | null
  total: number
  current: number
  size: number
  pages?: number
}

/** 追溯码状态：前端字符串 → 后端 Integer 编码 */
const traceCodeStatusToBackend: Record<string, number> = {
  active: 1,    // 正常
  expiring: 2,  // 即将过期
  expired: 3,   // 已过期
  recalled: 4,  // 已召回
  consumed: 5,  // 已消费
}

export const traceCodeApi = {
  /**
   * 生成追溯码
   * POST /v1/trace-codes
   * 写操作：禁止假成功，后端不可用时抛异常（生成假追溯码会污染追溯数据）
   */
  async generate(data: TraceCodeCreateDTO): Promise<TraceCodeVO> {
    return await post<TraceCodeVO>('/v1/trace-codes', data)
  },

  /**
   * 正向追溯查询（扫码查询）
   * GET /v1/trace-codes/{traceCode}
   */
  async queryByTraceCode(traceCode: string): Promise<TraceCodeVO | null> {
    return await get<TraceCodeVO | null>(`/v1/trace-codes/${encodeURIComponent(traceCode)}`)
  },

  /**
   * 分页查询追溯码列表
   * GET /v1/trace-codes/page
   *
   * 参数名映射（前端 → 后端 TraceCodeQueryDTO）：
   * - traceCode → keyword（关键字搜索：追溯码/目标名称）
   * - batchNumber → batchNo
   * - status: string → status: Integer（1正常/2即将过期/3已过期/4已召回/5已消费）
   * - page → current
   * - size → size（一致）
   */
  async queryPage(params: TraceCodeQueryDTO): Promise<PageResponse<TraceCodeVO>> {
    const backendParams: TraceCodeQueryParams = {
      current: params.page || 1,
      size: params.size || 10,
    }
    if (params.traceCode) backendParams.keyword = params.traceCode
    if (params.batchNumber) backendParams.batchNo = params.batchNumber
    if (params.supplierId) backendParams.supplierId = params.supplierId
    if (params.status) {
      const statusCode = traceCodeStatusToBackend[params.status]
      if (statusCode !== undefined) {
        backendParams.status = statusCode
      }
    }
    if (params.startDate) backendParams.startDate = params.startDate
    if (params.endDate) backendParams.endDate = params.endDate

    const res = await get<TraceCodePageBackend | null>('/v1/trace-codes/page', backendParams as unknown as Record<string, unknown>)
    const records = res?.records ?? []
    const total = res?.total ?? 0
    const current = res?.current ?? backendParams.current
    const size = res?.size ?? backendParams.size
    const pages = res?.pages ?? Math.ceil(total / size)
    return { records, total, current, size, pages }
  },

  /**
   * 更新追溯码状态
   * PUT /v1/trace-codes/{id}/status
   * 写操作：禁止假成功，后端不可用时抛异常（状态变更影响食品安全追溯）
   */
  async updateStatus(id: number, status: string): Promise<boolean> {
    await put<void>(`/v1/trace-codes/${encodeURIComponent(id)}/status`, { status })
    return true
  },

  /**
   * 执行召回
   * POST /v1/trace-codes/{id}/recall
   * 写操作：禁止假成功，后端不可用时抛异常（召回影响消费者权益）
   */
  async recall(id: number, recallReason: string, operatorName: string): Promise<boolean> {
    await post<void>(`/v1/trace-codes/${encodeURIComponent(id)}/recall`, { recallReason, operatorName })
    return true
  },

  /**
   * 记录消费者扫码
   * POST /v1/trace-codes/{traceCode}/scan
   * 写操作：禁止假成功，后端不可用时抛异常（扫码记录影响追溯数据）
   */
  async scan(traceCode: string): Promise<boolean> {
    await post<void>(`/v1/trace-codes/${encodeURIComponent(traceCode)}/scan`)
    return true
  },

  /**
   * 反向召回查询（按批次/供应商/物料名）
   * GET /v1/trace-codes/recall-query
   */
  async queryRecallImpact(params: {
    batchNo?: string
    supplierId?: number
    targetName?: string
  }): Promise<RecallQueryResultVO> {
    return await get<RecallQueryResultVO>('/v1/trace-codes/recall-query', params as Record<string, unknown>)
  },

  /**
   * 添加追溯链节点
   * POST /v1/trace-codes/{id}/chain-nodes
   * 写操作：禁止假成功，后端不可用时抛异常（追溯链节点影响追溯完整性）
   */
  async addChainNode(
    id: number,
    data: { nodeType: number; nodeName: string; location: string; detailJson?: string },
  ): Promise<TraceChainNodeVO> {
    return await post<TraceChainNodeVO>(`/v1/trace-codes/${encodeURIComponent(id)}/chain-nodes`, data)
  },

  /**
   * 刷新风险等级
   * POST /v1/trace-codes/{id}/refresh-risk
   * 写操作：禁止假成功，后端不可用时抛异常（风险等级影响召回决策）
   */
  async refreshRiskLevel(id: number): Promise<number> {
    return await post<number>(`/v1/trace-codes/${encodeURIComponent(id)}/refresh-risk`)
  },
}

export default traceCodeApi
