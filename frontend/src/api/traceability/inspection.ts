/**
 * 检验记录 API
 * 对应后端: InspectionController (/v1/inspections)
 *
 * 对接策略：
 * - 所有方法直接调用真实后端 API，无 Mock 降级
 * - 使用 @/api/request 导出的 get/post/put/del 辅助函数（内部基于 request 实例）
 * - GET 请求直接传 params 对象（不嵌套），符合项目 API 规范
 * - 响应数据由拦截器自动提取，前端直接使用返回值（不访问 .data）
 */
import { get, post, put, del } from '@/api/request'
import type { PageResponse } from '@/types'
import type {
  InspectionRecord,
  InspectionQuery,
  InspectionFormData,
  InspectionStatistics,
} from '@/types/traceability'

export const inspectionApi = {
  /**
   * 分页查询检验记录列表
   * GET /v1/inspections
   */
  getList(params: InspectionQuery): Promise<PageResponse<InspectionRecord>> {
    return get<PageResponse<InspectionRecord>>('/v1/inspections', params as Record<string, unknown>)
  },

  /**
   * 根据ID查询检验记录详情
   * GET /v1/inspections/{id}
   */
  getById(id: number): Promise<InspectionRecord> {
    return get<InspectionRecord>(`/v1/inspections/${encodeURIComponent(id)}`)
  },

  /**
   * 创建检验记录
   * POST /v1/inspections
   */
  create(data: InspectionFormData): Promise<InspectionRecord> {
    return post<InspectionRecord>('/v1/inspections', data)
  },

  /**
   * 更新检验记录
   * PUT /v1/inspections/{id}
   */
  update(id: number, data: Partial<InspectionFormData>): Promise<InspectionRecord> {
    return put<InspectionRecord>(`/v1/inspections/${encodeURIComponent(id)}`, data)
  },

  /**
   * 删除检验记录
   * DELETE /v1/inspections/{id}
   */
  delete(id: number): Promise<boolean> {
    return del<boolean>(`/v1/inspections/${encodeURIComponent(id)}`)
  },

  /**
   * 按批次号查询检验记录
   * GET /v1/inspections/by-batch/{batchNo}
   */
  getByBatch(batchNo: string): Promise<InspectionRecord[]> {
    return get<InspectionRecord[]>(`/v1/inspections/by-batch/${encodeURIComponent(batchNo)}`)
  },

  /**
   * 按供应商查询检验记录
   * GET /v1/inspections/by-supplier/{supplierId}
   */
  getBySupplier(supplierId: number, params?: InspectionQuery): Promise<PageResponse<InspectionRecord>> {
    return get<PageResponse<InspectionRecord>>(
      `/v1/inspections/by-supplier/${encodeURIComponent(supplierId)}`,
      params as Record<string, unknown> | undefined,
    )
  },

  /**
   * 上传检验报告
   * POST /v1/inspections/{id}/upload-report
   */
  uploadReport(id: number, reportUrl: string): Promise<InspectionRecord> {
    return post<InspectionRecord>(`/v1/inspections/${encodeURIComponent(id)}/upload-report`, { reportUrl })
  },

  /**
   * 获取检验统计
   * GET /v1/inspections/statistics
   */
  getStatistics(params?: InspectionQuery): Promise<InspectionStatistics> {
    return get<InspectionStatistics>('/v1/inspections/statistics', params as Record<string, unknown> | undefined)
  },
}

export default inspectionApi
