/**
 * 库存报损类型定义
 * 对齐后端: InventoryLoss实体
 */

/** 报损类型（前端语义化） */
export type LossType = 'expired' | 'damaged' | 'lost' | 'other'

/** 报损状态（前端语义化） */
export type LossStatus = 'pending' | 'approved' | 'processed'

/** 报损单信息（对应后端 InventoryLoss） */
export interface InventoryLossInfo {
  lossId: string
  lossCode: string
  warehouseId: string
  warehouseName: string
  lossType: LossType
  lossTypeName: string
  status: LossStatus
  statusName: string
  totalAmount: string
  applyUserId: string
  applyUserName: string
  applyTime: string
  approveUserId: string
  approveUserName: string
  approveTime: string
  processTime: string
  remark: string
  items: InventoryLossItemInfo[]
  createTime: string
  updateTime: string
}

/** 报损明细 */
export interface InventoryLossItemInfo {
  lossItemId: string
  lossId: string
  inventoryId: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  quantity: number
  unitCost: string
  totalCost: string
  reason: string
}

/** 报损单查询参数 */
export interface InventoryLossQueryForm {
  lossCode?: string
  warehouseId?: string
  lossType?: LossType
  status?: LossStatus
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 报损单创建表单 */
export interface InventoryLossCreateForm {
  warehouseId: string
  lossType: LossType
  remark?: string
  items: {
    inventoryId: string
    quantity: number
    reason: string
  }[]
}

/** 报损审批表单 */
export interface LossApproveForm {
  approved: boolean
  remark?: string
}
