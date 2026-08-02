/**
 * 库存出库 - 类型定义
 *
 * 出库类型：
 * - requisition: 领料出库（后厨领用原材料）
 * - sale: 销售出库（成品/半成品出库到门店）
 * - return: 退货出库（退回供应商）
 * - other: 其他出库
 *
 * 出库状态流转：
 * pending(待审批) → approved(已审批) → completed(已出库)
 *                  → rejected(已驳回)
 */

/** 出库类型 */
export type OutboundType = 'requisition' | 'sale' | 'return' | 'other'

/** 出库状态 */
export type OutboundStatus = 'pending' | 'approved' | 'completed' | 'rejected'

/** 出库单明细 */
export interface OutboundItemInfo {
  /** 明细ID */
  outboundItemId: string
  /** 出库单ID */
  outboundId: string
  /** 物料ID */
  materialId: string
  /** 物料名称 */
  materialName: string
  /** 规格型号 */
  specification: string
  /** 单位 */
  unit: string
  /** 申请出库数量 */
  requestQuantity: number
  /** 实际出库数量 */
  actualQuantity: number
  /** 批次号（先进先出） */
  batchNo: string
  /** 单价（元） */
  unitCost: string
  /** 小计金额（元） */
  totalCost: string
  /** 备注 */
  remark: string
}

/** 出库单信息 */
export interface InventoryOutboundInfo {
  /** 出库单ID */
  outboundId: string
  /** 出库单号 */
  outboundCode: string
  /** 出库类型 */
  outboundType: OutboundType
  /** 出库类型名称 */
  outboundTypeName: string
  /** 出库仓库ID */
  warehouseId: string
  /** 出库仓库名称 */
  warehouseName: string
  /** 目标门店/部门ID */
  targetId: string
  /** 目标门店/部门名称 */
  targetName: string
  /** 关联单号（采购订单号/调拨单号等） */
  referenceNo: string
  /** 出库日期 */
  outboundDate: string
  /** 出库明细 */
  items: OutboundItemInfo[]
  /** 总数量 */
  totalQuantity: number
  /** 总金额（元） */
  totalAmount: string
  /** 出库状态 */
  status: OutboundStatus
  /** 状态名称 */
  statusName: string
  /** 申请人ID */
  applyUserId: string
  /** 申请人名称 */
  applyUserName: string
  /** 申请时间 */
  applyTime: string
  /** 审批人ID */
  approveUserId: string
  /** 审批人名称 */
  approveUserName: string
  /** 审批时间 */
  approveTime: string
  /** 完成时间 */
  completeTime: string
  /** 备注 */
  remark: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 出库单查询表单 */
export interface OutboundQueryForm {
  /** 出库类型 */
  outboundType?: OutboundType
  /** 出库状态 */
  status?: OutboundStatus
  /** 仓库ID */
  warehouseId?: string
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 关键词（出库单号/物料名称） */
  keyword?: string
  /** 页码 */
  page?: number
  /** 每页条数 */
  size?: number
}

/** 出库单创建表单 */
export interface OutboundCreateForm {
  /** 出库类型 */
  outboundType: OutboundType
  /** 出库仓库ID */
  warehouseId: string
  /** 目标门店/部门ID */
  targetId?: string
  /** 目标门店/部门名称 */
  targetName?: string
  /** 关联单号 */
  referenceNo?: string
  /** 出库日期 */
  outboundDate: string
  /** 出库明细 */
  items: {
    materialId: string
    requestQuantity: number
    remark?: string
  }[]
  /** 备注 */
  remark?: string
}

/** 出库审批表单 */
export interface OutboundApproveForm {
  /** 是否通过 */
  approved: boolean
  /** 审批意见 */
  opinion?: string
}
