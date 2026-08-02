/**
 * 冲突检测 API
 * 对应后端: /v1/schedule/conflicts
 */
import { get, put } from '@/api/request'
import type { ConflictCheckResult, ConflictDetail } from '@/types/schedule'

export const conflictApi = {
  /**
   * 执行冲突检查
   * @param planId - 计划ID
   * @returns 冲突检测结果
   */
  async checkConflicts(planId: string): Promise<ConflictCheckResult> {
    return get<ConflictCheckResult>(
      `/v1/schedule/conflicts/${planId}/check`
    )
  },

  /**
   * 获取计划的所有冲突列表
   * @param planId - 计划ID
   * @param level - 冲突级别筛选（可选）
   * @returns 冲突详情列表
   */
  async getConflicts(
    planId: string,
    level?: string
  ): Promise<ConflictDetail[]> {
    const params: Record<string, unknown> = { planId }
    if (level) params.level = level
    const res = await get<ConflictDetail[]>(
      '/v1/schedule/conflicts',
      params
    )
    return res || []
  },

  /**
   * 应用自动修复方案
   * @param conflictId - 冲突ID
   * @returns 修复后的冲突详情
   */
  async applyFix(conflictId: string): Promise<ConflictDetail> {
    return put<ConflictDetail>(
      `/v1/schedule/conflicts/fix/${conflictId}`
    )
  },
}

export default conflictApi
