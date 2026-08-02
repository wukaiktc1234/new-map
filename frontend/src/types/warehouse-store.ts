/**
 * 门店库存类型定义
 * 对齐后端: StoreInventory相关实体
 */

/** 门店信息 */
export interface StoreInfo {
  storeId: string
  storeName: string
  storeCode: string
  address: string
  status: number
}

/** 门店库存信息 */
export interface StoreInventoryInfo {
  inventoryId: string
  storeId: string
  storeName: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  quantity: number
  lockedQuantity: number
  availableQuantity: number
  unitCost: string
  totalCost: string
  lastInTime: string
  lastOutTime: string
  createTime: string
  updateTime: string
}

/** 门店库存汇总 */
export interface StoreInventorySummary {
  storeId: string
  storeName: string
  totalItems: number
  totalQuantity: number
  totalValue: string
  warningCount: number
}

/** 门店库存查询参数 */
export interface StoreInventoryQueryForm {
  storeId?: string
  materialName?: string
  category?: string
  status?: string
  page?: number
  size?: number
}

/** 门店库存调整表单 */
export interface StoreInventoryAdjustForm {
  inventoryId: string
  quantity: number
  type: 'in' | 'out'
  remark?: string
}

/** 门店库存日志 */
export interface StoreInventoryLogInfo {
  logId: string
  inventoryId: string
  storeId: string
  storeName: string
  materialId: string
  materialName: string
  operationType: string
  operationTypeName: string
  quantity: number
  beforeStock: number
  afterStock: number
  operatorId: string
  operatorName: string
  remark: string
  createTime: string
}
