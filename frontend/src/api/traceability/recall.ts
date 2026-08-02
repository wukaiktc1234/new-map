/**
 * 召回管理 API
 * 对应后端: RecallController (/v1/recalls)
 *
 * 改造说明（2026-06-30）：
 * - 移除所有 Mock fallback，后端故障直接抛错（食品安全：禁止假成功）
 * - 改用命名导出 get/post（替代 silentGet/silentPost）
 */
import { get, post } from '../request'

import type {
  RecallAnalyzeRequest,
  RecallQueryResultVO,
  BatchRecallRequest,
  BatchRecallResult,
  RecallStatisticsVO,
} from '@/types/traceability'

export const recallApi = {
  /**
   * 分析召回影响范围
   * POST /v1/recalls/analyze
   * 查询类：后端不可用时抛异常（移除 Mock 降级，统一错误处理）
   */
  async analyzeRecall(params: RecallAnalyzeRequest): Promise<RecallQueryResultVO> {
    return await post<RecallQueryResultVO>('/v1/recalls/analyze', params)
  },

  /**
   * 批量执行召回
   * POST /v1/recalls/batch-recall
   * 写操作：禁止假成功，后端不可用时抛异常（食品安全关键操作）
   */
  async batchRecall(data: BatchRecallRequest): Promise<BatchRecallResult> {
    return await post<BatchRecallResult>('/v1/recalls/batch-recall', data)
  },

  /**
   * 获取召回统计
   * GET /v1/recalls/statistics
   * 查询类：后端不可用时抛异常（移除 Mock 降级，统一错误处理）
   */
  async getStatistics(): Promise<RecallStatisticsVO> {
    return await get<RecallStatisticsVO>('/v1/recalls/statistics')
  },
}

export default recallApi
