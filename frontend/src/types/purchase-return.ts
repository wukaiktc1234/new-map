/**
 * 采购退货模块类型定义
 * 对应后端：PurchaseReturn / PurchaseReturnItem
 */

/** 退货单状态 */
export type PurchaseReturnStatus = 'pending' | 'approved' | 'rejected' | 'completed'

/** 退款方式 */
export type PurchaseReturnRefundMethod = 'offset' | 'cash'

/** 退货单状态选项 */
export const PurchaseReturnStatusOptions = [
  { label: '待审批', value: 'pending' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已完成', value: 'completed' },
] as const

/** 退款方式选项 */
export const PurchaseReturnRefundMethodOptions = [
  { label: '冲抵货款', value: 'offset' },
  { label: '现金退款', value: 'cash' },
] as const

/** 退货明细 */
export interface PurchaseReturnItem {
  /** 明细ID */
  id: string
  /** 退货单ID */
  returnId: string
  /** 关联原入库明细ID */
  stockinItemId: string
  /** 物料ID */
  materialId: string
  /** 物料名称 */
  materialName: string
  /** 规格 */
  specification: string
  /** 单位 */
  unit: string
  /** 退货数量 */
  quantity: number
  /** 退货单价（元） */
  unitPrice: number
  /** 退货金额（元） */
  totalAmount: number
  /** 退货原因 */
  returnReason: string
  /** 入库批次号 */
  batchNo: string
  /** 创建时间 */
  createTime: string
}

/** 采购退货单 */
export interface PurchaseReturn {
  /** 退货单ID */
  id: string
  /** 退货单号 */
  returnNo: string
  /** 关联原入库单ID */
  stockinId: string
  /** 关联原入库单号 */
  stockinNo: string
  /** 关联采购订单ID */
  orderId: string
  /** 关联采购订单号 */
  orderNo: string
  /** 供应商ID */
  supplierId: string
  /** 供应商名称 */
  supplierName: string
  /** 退货仓库ID */
  warehouseId: string
  /** 退货日期 */
  returnDate: string
  /** 退货总数量 */
  totalQuantity: number
  /** 退货总金额（元） */
  totalAmount: number
  /** 退款方式 */
  refundMethod: PurchaseReturnRefundMethod
  /** 状态 */
  status: PurchaseReturnStatus
  /** 审批备注 */
  approvalRemark: string
  /** 关联红字应付单ID */
  relatedPayableId: string
  /** 备注 */
  remark: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
  /** 退货明细列表 */
  items: PurchaseReturnItem[]
}

/** 采购退货查询参数 */
export interface PurchaseReturnQueryParams {
  page?: number
  size?: number
  returnNo?: string
  supplierId?: string
  stockinNo?: string
  status?: PurchaseReturnStatus | ''
  startDate?: string
  endDate?: string
}

/** 创建退货明细 */
export interface PurchaseReturnCreateItem {
  /** 关联原入库明细ID */
  stockinItemId: string
  /** 物料ID */
  materialId: string
  /** 退货数量 */
  quantity: number
  /** 退货原因 */
  returnReason?: string
  /** 入库批次号 */
  batchNo?: string
}

/** 创建/更新退货单参数 */
export interface PurchaseReturnCreateParams {
  /** 关联原入库单ID */
  stockinId: string
  /** 退货日期 */
  returnDate?: string
  /** 退款方式 */
  refundMethod?: PurchaseReturnRefundMethod
  /** 备注 */
  remark?: string
  /** 退货明细 */
  items: PurchaseReturnCreateItem[]
}

/** 更新退货单参数 */
export interface PurchaseReturnUpdateParams extends PurchaseReturnCreateParams {
  /** 退货单ID */
  returnId: string
}

/** 审批退货单参数 */
export interface PurchaseReturnApproveParams {
  /** 审批结果：approved / rejected */
  status: string
  /** 审批备注 */
  remark?: string
}

/** 采购退货信息别名 */
export type PurchaseReturnInfo = PurchaseReturn
