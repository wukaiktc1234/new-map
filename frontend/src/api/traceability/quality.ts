/**
 * 质量管理 API
 * 对应后端: QualityController (/v1/quality)
 *
 * 对接策略：
 * - 所有方法直接调用真实后端 API，无 Mock 降级
 * - 使用 @/api/request 导出的 get/post/put/del 辅助函数（内部基于 request 实例）
 * - GET 请求直接传 params 对象（不嵌套），符合项目 API 规范
 * - 响应数据由拦截器自动提取，前端直接使用返回值（不访问 .data）
 *
 * 包含两部分：
 * 1. 质量记录管理（/v1/quality/records）
 * 2. 质量标准管理（/v1/quality/standards）
 */
import { get, post, put, del } from '@/api/request'
import type { PageResponse } from '@/types'
import type {
  QualityRecord,
  QualityStandard,
  QualityRecordFormData,
  QualityStandardFormData,
  QualityQuery,
} from '@/types/traceability'

export const qualityApi = {
  // ============================================================
  // 质量记录管理
  // ============================================================

  /**
   * 分页查询质量记录列表
   * GET /v1/quality/records
   */
  getRecordList(params: QualityQuery): Promise<PageResponse<QualityRecord>> {
    return get<PageResponse<QualityRecord>>('/v1/quality/records', params as Record<string, unknown>)
  },

  /**
   * 根据ID查询质量记录详情
   * GET /v1/quality/records/{id}
   */
  getRecordDetail(id: number): Promise<QualityRecord> {
    return get<QualityRecord>(`/v1/quality/records/${encodeURIComponent(id)}`)
  },

  /**
   * 创建质量记录
   * POST /v1/quality/records
   */
  createRecord(data: QualityRecordFormData): Promise<QualityRecord> {
    return post<QualityRecord>('/v1/quality/records', data)
  },

  /**
   * 删除质量记录
   * DELETE /v1/quality/records/{id}
   */
  deleteRecord(id: number): Promise<boolean> {
    return del<boolean>(`/v1/quality/records/${encodeURIComponent(id)}`)
  },

  /**
   * 查询异常质量记录列表
   * GET /v1/quality/records/abnormal
   */
  getAbnormalList(params: QualityQuery): Promise<PageResponse<QualityRecord>> {
    return get<PageResponse<QualityRecord>>('/v1/quality/records/abnormal', params as Record<string, unknown>)
  },

  /**
   * 处理异常质量记录
   * POST /v1/quality/records/{id}/handle
   * 食品安全关键操作：禁止假成功，后端不可用时抛异常
   */
  handleAbnormal(id: number, handlingResult: string): Promise<boolean> {
    return post<boolean>(`/v1/quality/records/${encodeURIComponent(id)}/handle`, { handlingResult })
  },

  /**
   * 获取质量记录统计
   * GET /v1/quality/records/statistics
   * 注：后端统计字段未固定，使用 Record<string, unknown> 接收，待后端明确后可替换为具体接口
   */
  getStatistics(): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>('/v1/quality/records/statistics')
  },

  // ============================================================
  // 质量标准管理
  // ============================================================

  /**
   * 分页查询质量标准列表
   * GET /v1/quality/standards
   */
  getStandardList(params: QualityQuery): Promise<PageResponse<QualityStandard>> {
    return get<PageResponse<QualityStandard>>('/v1/quality/standards', params as Record<string, unknown>)
  },

  /**
   * 根据ID查询质量标准详情
   * GET /v1/quality/standards/{id}
   */
  getStandardDetail(id: number): Promise<QualityStandard> {
    return get<QualityStandard>(`/v1/quality/standards/${encodeURIComponent(id)}`)
  },

  /**
   * 创建质量标准
   * POST /v1/quality/standards
   */
  createStandard(data: QualityStandardFormData): Promise<QualityStandard> {
    return post<QualityStandard>('/v1/quality/standards', data)
  },

  /**
   * 更新质量标准
   * PUT /v1/quality/standards/{id}
   */
  updateStandard(id: number, data: QualityStandardFormData): Promise<QualityStandard> {
    return put<QualityStandard>(`/v1/quality/standards/${encodeURIComponent(id)}`, data)
  },

  /**
   * 删除质量标准
   * DELETE /v1/quality/standards/{id}
   */
  deleteStandard(id: number): Promise<boolean> {
    return del<boolean>(`/v1/quality/standards/${encodeURIComponent(id)}`)
  },
}

export default qualityApi
