/**
 * 仓库管理类型定义
 * 对齐后端: Warehouse实体 + WarehouseVO
 */

/** 仓库类型（前端语义化） */
export type WarehouseType = 'main' | 'cold' | 'freeze' | 'normal'

/** 仓库状态（前端语义化） */
export type WarehouseStatus = 'active' | 'inactive'

/** 仓库信息（对应后端 WarehouseVO） */
export interface WarehouseInfo {
  warehouseId: string
  warehouseCode: string
  warehouseName: string
  warehouseType: WarehouseType
  warehouseTypeName: string
  address: string
  managerId: string
  managerName: string
  phone: string
  capacity: number
  usedCapacity: number
  capacityUsageRate: number
  status: WarehouseStatus
  statusName: string
  remark: string
  createTime: string
  updateTime: string
}

/** 仓库查询参数 */
export interface WarehouseQueryForm {
  warehouseName?: string
  warehouseCode?: string
  warehouseType?: WarehouseType
  status?: WarehouseStatus
  page?: number
  size?: number
}

/** 仓库创建表单 */
export interface WarehouseCreateForm {
  warehouseCode: string
  warehouseName: string
  warehouseType: WarehouseType
  address?: string
  managerId?: string
  phone?: string
  capacity?: number
  remark?: string
}

/** 仓库更新表单 */
export interface WarehouseUpdateForm {
  warehouseName?: string
  warehouseType?: WarehouseType
  address?: string
  managerId?: string
  phone?: string
  capacity?: number
  remark?: string
}
