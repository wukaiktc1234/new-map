/**
 * 采购链路追溯相关类型定义
 */

/** 采购溯源链路节点 */
export interface ProcurementTraceNode {
  nodeType: string
  nodeCode: string
  nodeId: string
  title: string
  status: string
  statusText: string
  operateTime?: string
  operatorName?: string
  remark?: string
}

/** 采购溯源物料明细 */
export interface ProcurementTraceMaterialItem {
  materialId: number
  materialName: string
  materialCode?: string
  specification?: string
  quantity: number
  unit: string
  unitPrice?: number
  amount?: number
  batchNo?: string
  productionDate?: string
  expiryDate?: string
}

/** 采购溯源资产信息 */
export interface ProcurementTraceAsset {
  assetId: number
  assetCode: string
  assetName: string
  specification?: string
  status: string
  statusText: string
  purchaseDate?: string
  originalValue?: number
}

/** 采购溯源信息 */
export interface ProcurementTraceInfo {
  traceBatchNo?: string
  supplierId?: number
  supplierName?: string
  requestId?: string
  requestNo?: string
  requestTitle?: string
  /** 申请人名称（统一命名） */
  createByName?: string
  requestTime?: string
  orderId?: number
  orderNo?: string
  orderAmount?: number
  orderDate?: string
  orderStatus?: string
  orderStatusText?: string
  stockinCodes: string[]
  stockinTime?: string
  totalStockinQuantity?: number
  qualityCheckResult?: number
  qualityCheckResultText?: string
  warehouseId?: number
  warehouseName?: string
  materialItems: ProcurementTraceMaterialItem[]
  assets: ProcurementTraceAsset[]
  traceNodes: ProcurementTraceNode[]
}

/** 采购溯源查询参数 */
export interface ProcurementTraceQueryParams {
  materialId?: number
  orderNo?: string
  requestNo?: string
  stockinCode?: string
  batchNo?: string
}
