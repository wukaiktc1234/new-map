/**
 * 库存日志类型定义
 * 对齐后端: InventoryLog 实体
 */

/** 库存操作类型（前端语义化） */
export type InventoryOperationType =
  | 'purchase_in'
  | 'sale_out'
  | 'transfer_out'
  | 'transfer_in'
  | 'check_gain'
  | 'check_loss'
  | 'damage'
  | 'return'

/** 库存日志信息（对应后端 InventoryLog）
 * 注意：后端当前返回 productId，前端统一使用 materialId/materialName 命名；
 * materialName 待后端 InventoryLog 补充物料名称后显示。
 */
export interface InventoryLogInfo {
  id: string
  materialId: string
  materialName: string
  warehouseId: string
  warehouseName: string
  operationType: string
  operationTypeName: string
  beforeStock: number
  afterStock: number
  changeAmount: number
  operatorId: string
  operatorName: string
  remark: string
  createdAt: string
}

/** 库存日志查询参数 */
export interface InventoryLogQueryForm {
  materialId?: string
  warehouseId?: string
  operatorId?: string
  operationType?: string
  startTime?: string
  endTime?: string
  page?: number
  size?: number
}
