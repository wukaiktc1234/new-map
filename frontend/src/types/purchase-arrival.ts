/**
 * 采购到货单 / 收货确认单相关类型定义
 */

/** 到货单状态 */
export type PurchaseArrivalStatus = 'pending' | 'receiving' | 'partial_received' | 'received' | 'closed'

/** 收货方类型 */
export type ReceiverType = 'STORE' | 'WAREHOUSE'

/** 发货状态 */
export type ShipmentStatus = 'shipped' | 'in_transit' | 'delivered' | ''

/** 运输方式 */
export type TransportMode = 'land' | 'air' | 'sea' | 'express' | 'self' | ''

/** 车辆类型 */
export type VehicleType = 'van' | 'refrigerated' | 'flatbed' | 'minivan' | 'other' | ''

/** 采购到货单明细 */
export interface PurchaseArrivalItem {
  arrivalItemId: string
  arrivalId: string
  orderItemId: string
  materialId: string
  materialName: string
  specification?: string
  unit?: string
  expectedQuantity: number
  receivedQuantity: number
  unitPrice: number
  amount: number
  remark?: string
}

/** 采购到货单 */
export interface PurchaseArrivalInfo {
  arrivalId: string
  arrivalCode: string
  orderId: string
  orderCode?: string
  supplierId?: string
  supplierName?: string
  receiverType: ReceiverType
  storeId?: string
  warehouseId?: string
  shipmentStatus?: ShipmentStatus
  logisticsNo?: string
  logisticsCompany?: string
  transportMode?: TransportMode
  vehiclePlateNo?: string
  vehicleType?: VehicleType
  driverName?: string
  driverPhone?: string
  freightAmount?: number
  estimatedArrivalDate?: string
  actualArrivalDate?: string
  totalQuantity: number
  totalAmount: number
  receivedQuantity: number
  status: PurchaseArrivalStatus
  closeReason?: string
  remark?: string
  createTime: string
  updateTime: string
  items?: PurchaseArrivalItem[]
  overdue?: boolean
  /** 质检结果：0待检 1通过 2失败 */
  qualityCheckResult?: number
  /** 质检备注 */
  qualityCheckRemark?: string
  /** 入库确认时间 */
  confirmTime?: string
}

/** 到货单查询表单 */
export interface PurchaseArrivalQueryForm {
  arrivalCode?: string
  orderCode?: string
  supplierId?: string
  status?: PurchaseArrivalStatus | ''
  receiverType?: ReceiverType | ''
  storeId?: string
  warehouseId?: string
  overdueOnly?: boolean
  estimatedStartDate?: string
  estimatedEndDate?: string
  /** 创建人ID（我的单据筛选） */
  createUserId?: string
}

/** 到货单创建表单 */
export interface PurchaseArrivalFormData {
  orderId: string
  shipmentStatus?: ShipmentStatus
  logisticsNo?: string
  logisticsCompany?: string
  transportMode?: TransportMode
  vehiclePlateNo?: string
  vehicleType?: VehicleType
  driverName?: string
  driverPhone?: string
  freightAmount?: number
  estimatedArrivalDate?: string
  remark?: string
}

/** 到货单更新表单 */
export interface PurchaseArrivalUpdateForm {
  shipmentStatus?: ShipmentStatus
  logisticsNo?: string
  logisticsCompany?: string
  transportMode?: TransportMode
  vehiclePlateNo?: string
  vehicleType?: VehicleType
  driverName?: string
  driverPhone?: string
  freightAmount?: number
  estimatedArrivalDate?: string
  actualArrivalDate?: string
  remark?: string
}

/** 到货单关闭表单 */
export interface PurchaseArrivalCloseForm {
  closeReason: string
  remark?: string
}

/** 收货确认单明细 */
export interface ReceiptConfirmationItem {
  confirmationItemId: string
  confirmationId: string
  arrivalItemId: string
  orderItemId: string
  materialId: string
  materialName: string
  specification?: string
  unit?: string
  confirmedQuantity: number
  rejectedQuantity: number
  unitPrice: number
  amount: number
  batchNo?: string
  productionDate?: string
  expiryDate?: string
  remark?: string
}

/** 收货确认单 */
export interface ReceiptConfirmationInfo {
  confirmationId: string
  confirmationCode: string
  /** 收货来源：arrival-到货确认 / direct-无单直收 */
  receiptSource?: 'arrival' | 'direct'
  arrivalId: string
  orderId: string
  receiverType: ReceiverType
  storeId?: string
  warehouseId?: string
  confirmUserId?: string
  confirmTime: string
  totalQuantity: number
  totalAmount: number
  qualityCheckResult?: number
  qualityRemark?: string
  status: number
  remark?: string
  createTime: string
  updateTime: string
  items?: ReceiptConfirmationItem[]
}

/** 收货确认单查询表单 */
export interface ReceiptConfirmationQueryForm {
  confirmationCode?: string
  arrivalId?: string
  orderId?: string
  receiverType?: ReceiverType | ''
  storeId?: string
  warehouseId?: string
}

/** 收货确认明细表单 */
export interface ReceiptConfirmationItemForm {
  arrivalItemId: string
  materialId: string
  materialName: string
  specification?: string
  unit?: string
  expectedQuantity: number
  receivedQuantity: number
  confirmedQuantity: number
  rejectedQuantity: number
  unitPrice: number
  batchNo?: string
  productionDate?: string
  expiryDate?: string
  remark?: string
}

/** 收货确认单创建表单 */
export interface ReceiptConfirmationFormData {
  arrivalId: string
  receiverType: ReceiverType
  storeId?: string
  warehouseId?: string
  items: ReceiptConfirmationItemForm[]
  qualityCheckResult?: number
  qualityRemark?: string
  remark?: string
}

/** 状态选项 */
export const PurchaseArrivalStatusOptions = [
  { value: 'pending', label: '待收货' },
  { value: 'receiving', label: '收货中' },
  { value: 'partial_received', label: '部分收货' },
  { value: 'received', label: '已收货' },
  { value: 'closed', label: '已关闭' },
]

/** 发货状态选项 */
export const ShipmentStatusOptions = [
  { value: 'shipped', label: '已发货' },
  { value: 'in_transit', label: '运输中' },
  { value: 'delivered', label: '已送达' },
]

/** 运输方式选项 */
export const TransportModeOptions = [
  { value: 'land', label: '陆运' },
  { value: 'air', label: '空运' },
  { value: 'sea', label: '海运' },
  { value: 'express', label: '快递' },
  { value: 'self', label: '自提' },
]

/** 车辆类型选项 */
export const VehicleTypeOptions = [
  { value: 'van', label: '厢式货车' },
  { value: 'refrigerated', label: '冷藏车' },
  { value: 'flatbed', label: '平板车' },
  { value: 'minivan', label: '面包车' },
  { value: 'other', label: '其他' },
]

/** 收货方类型选项 */
export const ReceiverTypeOptions = [
  { value: 'STORE', label: '门店' },
  { value: 'WAREHOUSE', label: '仓库' },
]

/** 质检结果选项 */
export const QualityCheckResultOptions = [
  { value: 1, label: '合格' },
  { value: 2, label: '不合格' },
]
