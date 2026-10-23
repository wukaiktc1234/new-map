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
  /** 已落库的凭证照片（后端回放历史时透出） */
  evidence?: ReceiptEvidenceForm[]
  /** 逐条质检结论 */
  qualityCheckResult?: number
  /** 验收结论（回放用） */
  accept?: 'receive' | 'reject' | ''
  /** 本次实收数量（回放用） */
  quantity?: number | null
  /** 是否手工匹配产生 */
  manualMatched?: boolean
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
  /** 供应商名称（后端 ReceiptConfirmation 已透出） */
  supplierName?: string
  /** 防伪码（16 位，供打印与查验） */
  verifyCode?: string
  /** 已打印次数 / 最近打印时间（服务端计数） */
  printCount?: number
  lastPrintTime?: string
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

/** 收货凭证照片（质检驳回 / 手工匹配 的现场证据链） */
export interface ReceiptEvidenceForm {
  /** 取证类型：QC_REJECT-质检驳回 / MANUAL_MATCH-手工匹配 */
  evidenceType: 'QC_REJECT' | 'MANUAL_MATCH' | string
  /** 已上传后的图片地址（离线暂存时为空串） */
  photoUrl: string
  /** 离线暂存时的本地 dataURL，恢复网络后重传 */
  localDataUrl?: string
  /** 是否还在等待上传 */
  pendingUpload?: boolean
  /** 拍摄时间（EXIF 优先，缺失时用拍摄动作时刻兜底） */
  photoTakenAt?: string
  /** 设备机型（EXIF 缺失时用 UA 兜底） */
  deviceModel?: string
  /** GPS 坐标（EXIF 优先，缺失时用定位兜底） */
  gps?: string
}

/** 防伪码查验结果（对应后端 ReceiptVerifyVO） */
export interface ReceiptVerifyPrintLog {
  printId?: string | number
  printTime?: string
  operatorName?: string
  sourceIp?: string
}

export interface ReceiptVerifyResult {
  /** 防伪码是否有效 */
  valid: boolean
  message?: string
  confirmationCode?: string | null
  verifyCode?: string | null
  supplierName?: string | null
  confirmTime?: string | null
  /** 确认金额（后端为分，本页直接展示，需要元请用 formatMoney 统一换算） */
  totalAmount?: number | null
  totalQuantity?: number | null
  printCount?: number | null
  lastPrintTime?: string | null
  printLogs: ReceiptVerifyPrintLog[]
}

/** 收货确认签名存证（对应后端 receipt_confirmation_signatures） */
export interface ReceiptSignatureInfo {
  signatureId?: string | number
  confirmationId?: string | number
  /** 签名方：RECEIVER-收货方 / SUPPLIER-供应商 */
  signerType?: 'RECEIVER' | 'SUPPLIER' | string
  signerName?: string
  /** 笔迹图片（base64 或 URL） */
  signatureImage?: string
  signTime?: string
  createTime?: string
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
  /** 确认数量：移动收货台用 quantity 录入，提交时由 converter 兜底映射 */
  confirmedQuantity?: number
  rejectedQuantity?: number
  unitPrice: number
  batchNo?: string
  productionDate?: string
  expiryDate?: string
  remark?: string
  /** 本次实收数量（移动收货台逐条录入，可空表示未填） */
  quantity?: number | null
  /** 验收结论：receive-接收 / reject-拒收；空串表示未选 */
  accept?: 'receive' | 'reject' | ''
  /** 现场凭证照片（拒收必填，手工匹配可附） */
  evidence?: ReceiptEvidenceForm[]
  /** 是否由手工匹配（扫码/名单未命中后人工指定）产生 */
  manualMatched?: boolean
  /** 逐条质检结论：1-合格 2-不合格 3-让步接收 */
  qualityCheckResult?: number
  /** 供应商名称（列表/历史展示用，前端透传） */
  supplierName?: string
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
  /** 条码（扫码收货时随单上送，用于追溯绑定） */
  barcode?: string
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
