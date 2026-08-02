/**
 * 库位管理类型定义
 * 对齐后端: InventoryLocation实体
 */

/** 库位类型（前端语义化） */
export type LocationType = 'shelf' | 'floor' | 'cold_storage' | 'freezer'

/** 库位状态（前端语义化） */
export type LocationStatus = 'active' | 'inactive'

/** 库位信息（对应后端 InventoryLocation） */
export interface InventoryLocationInfo {
  locationId: string
  warehouseId: string
  warehouseName: string
  locationCode: string
  locationName: string
  locationType: LocationType
  locationTypeName: string
  maxCapacity: number
  currentQuantity: number
  usageRate: number
  status: LocationStatus
  statusName: string
  remark: string
  createTime: string
  updateTime: string
}

/** 库位查询参数 */
export interface LocationQueryForm {
  warehouseId?: string
  locationCode?: string
  locationType?: LocationType
  status?: LocationStatus
  page?: number
  size?: number
}

/** 库位创建表单 */
export interface LocationCreateForm {
  warehouseId: string
  locationCode: string
  locationName: string
  locationType: LocationType
  maxCapacity?: number
  remark?: string
}

/** 库位更新表单 */
export interface LocationUpdateForm {
  locationName?: string
  locationType?: LocationType
  maxCapacity?: number
  status?: LocationStatus
  remark?: string
}
