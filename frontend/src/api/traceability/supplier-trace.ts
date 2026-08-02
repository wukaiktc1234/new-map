/**
 * 供应商追溯 API
 * 对应后端: SupplierTraceController (/v1/supplier-trace)
 *
 * 对接策略：
 * - 所有方法直接调用真实后端 API，无 Mock 降级
 * - 使用 @/api/request 导出的 get 辅助函数（内部基于 request 实例）
 * - GET 请求直接传 params 对象（不嵌套），符合项目 API 规范
 * - 响应数据由拦截器自动提取，前端直接使用返回值（不访问 .data）
 */
import { get } from '@/api/request'
import type {
  SupplierTrace,
  SupplierBatch,
  InspectionRecord,
  RecallRecord,
} from '@/types/traceability'

export const supplierTraceApi = {
  /**
   * 获取供应商追溯汇总信息
   * GET /v1/supplier-trace/{supplierId}
   */
  getSupplierTrace(supplierId: number): Promise<SupplierTrace> {
    return get<SupplierTrace>(`/v1/supplier-trace/${encodeURIComponent(supplierId)}`)
  },

  /**
   * 获取供应商批次列表
   * GET /v1/supplier-trace/{supplierId}/batches
   */
  getBatches(supplierId: number): Promise<SupplierBatch[]> {
    return get<SupplierBatch[]>(`/v1/supplier-trace/${encodeURIComponent(supplierId)}/batches`)
  },

  /**
   * 获取供应商合格率
   * GET /v1/supplier-trace/{supplierId}/quality-rate
   * 注：后端返回字段未固定，使用 Record<string, unknown> 接收
   */
  getQualityRate(supplierId: number): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>(`/v1/supplier-trace/${encodeURIComponent(supplierId)}/quality-rate`)
  },

  /**
   * 获取供应商召回记录
   * GET /v1/supplier-trace/{supplierId}/recalls
   */
  getRecalls(supplierId: number): Promise<RecallRecord[]> {
    return get<RecallRecord[]>(`/v1/supplier-trace/${encodeURIComponent(supplierId)}/recalls`)
  },

  /**
   * 获取供应商检验记录
   * GET /v1/supplier-trace/{supplierId}/inspections
   */
  getInspections(supplierId: number): Promise<InspectionRecord[]> {
    return get<InspectionRecord[]>(`/v1/supplier-trace/${encodeURIComponent(supplierId)}/inspections`)
  },

  /**
   * 获取供应商追溯统计
   * GET /v1/supplier-trace/statistics
   * 注：后端返回字段未固定，使用 Record<string, unknown> 接收
   */
  getStatistics(): Promise<Record<string, unknown>> {
    return get<Record<string, unknown>>('/v1/supplier-trace/statistics')
  },
}

export default supplierTraceApi
