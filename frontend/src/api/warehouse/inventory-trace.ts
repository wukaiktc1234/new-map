/**
 * 采购链路追溯 API
 * 对应后端: /v1/purchase/trace
 */
import { get } from '../request'
import type { ProcurementTraceInfo, ProcurementTraceQueryParams } from '@/types/procurement-trace'

export const inventoryTraceApi = {
  /**
   * 根据物料ID查询采购溯源信息
   * @param materialId 物料ID
   */
  async getTraceByMaterialId(materialId: number): Promise<ProcurementTraceInfo[]> {
    const res = await get<ProcurementTraceInfo[] | null>(`/v1/purchase/trace/material/${materialId}`)
    return res ?? []
  },

  /**
   * 根据采购订单编号查询采购溯源信息
   * @param orderNo 采购订单编号
   */
  async getTraceByOrderNo(orderNo: string): Promise<ProcurementTraceInfo | null> {
    const res = await get<ProcurementTraceInfo | null>(`/v1/purchase/trace/order/${orderNo}`)
    return res
  },

  /**
   * 根据采购申请编号查询采购溯源信息
   * @param requestNo 采购申请编号
   */
  async getTraceByRequestNo(requestNo: string): Promise<ProcurementTraceInfo[]> {
    const res = await get<ProcurementTraceInfo[] | null>(`/v1/purchase/trace/request/${requestNo}`)
    return res ?? []
  },

  /**
   * 根据入库单号查询采购溯源信息
   * @param stockinCode 入库单号
   */
  async getTraceByStockinCode(stockinCode: string): Promise<ProcurementTraceInfo | null> {
    const res = await get<ProcurementTraceInfo | null>(`/v1/purchase/trace/stockin/${stockinCode}`)
    return res
  },

  /**
   * 根据批次号查询采购溯源信息
   * @param batchNo 批次号
   */
  async getTraceByBatchNo(batchNo: string): Promise<ProcurementTraceInfo[]> {
    const res = await get<ProcurementTraceInfo[] | null>(`/v1/purchase/trace/batch/${batchNo}`)
    return res ?? []
  },

  /**
   * 综合查询采购溯源信息
   * @param params 查询参数
   */
  async searchTrace(params: ProcurementTraceQueryParams): Promise<ProcurementTraceInfo[]> {
    const query: Record<string, unknown> = {}
    if (params.materialId) query.materialId = params.materialId
    if (params.orderNo) query.orderNo = params.orderNo
    if (params.requestNo) query.requestNo = params.requestNo
    if (params.stockinCode) query.stockinCode = params.stockinCode
    if (params.batchNo) query.batchNo = params.batchNo

    const res = await get<ProcurementTraceInfo[] | null>('/v1/purchase/trace/search', query)
    return res ?? []
  },
}

export default inventoryTraceApi
