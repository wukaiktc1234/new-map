/**
 * 库存管理类型定义
 * 对齐后端: Inventory实体
 */

/** 库存状态（前端语义化） */
export type InventoryStatus = 'normal' | 'warning' | 'expired' | 'frozen'

/** 库存类型 */
export type InventoryType = 'raw_material' | 'semi_finished' | 'finished' | 'packaging'

/** 库存信息（对应后端 Inventory 实体） */
export interface InventoryInfo {
  inventoryId: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  warehouseId: string
  locationId: string
  quantity: number
  lockedQuantity: number
  availableQuantity: number
  batchNo: string
  productionDate: string
  expiryDate: string
  unitCost: string
  totalCost: string
  minSafeQty: number
  maxStockQty: number
  status: InventoryStatus
  inventoryType: InventoryType
  createTime: string
  updateTime: string
}

/** 库存查询参数 */
export interface InventoryQueryForm {
  warehouseId?: string
  materialId?: string
  materialName?: string
  batchNo?: string
  status?: InventoryStatus
  lowStockOnly?: boolean
  expiringSoon?: boolean
  page?: number
  size?: number
}

/** 库存增加请求 */
export interface InventoryIncreaseForm {
  materialId: string
  warehouseId: string
  locationId?: string
  quantity: number
  unitCost?: string
  transactionType?: number
  batchNo?: string
  referenceNo?: string
  referenceType?: string
  remark?: string
}

/** 库存锁定请求 */
export interface InventoryLockForm {
  inventoryId: string
  quantity: number
  referenceNo?: string
  referenceType?: string
}

/** 库存扣减请求 */
export interface InventoryDeductForm {
  inventoryId: string
  quantity: number
  referenceNo?: string
  referenceType?: string
  transactionType?: number
}

/** 库存变动类型 */
export type TransactionType = 'purchase_in' | 'sale_out' | 'transfer_out' | 'transfer_in' | 'check_gain' | 'check_loss' | 'damage' | 'return'

/** 库存变动记录（对应后端 InventoryTransaction） */
export interface InventoryTransactionInfo {
  transactionId: string
  transactionType: TransactionType
  inventoryId: string
  materialId: string
  warehouseId: string
  quantityChange: number
  beforeQty: number
  afterQty: number
  unitCost: string
  totalCost: string
  referenceNo: string
  referenceType: string
  createUserId: string
  createTime: string
  remark: string
}
