/**
 * 库存调拨类型定义
 * 对齐后端: InventoryTransfer实体
 */

/** 调拨状态（前端语义化） */
export type TransferStatus = 'pending' | 'shipped' | 'received' | 'completed'

/** 审批历史步骤 */
export type TransferApprovalStep = 'submit' | 'approve' | 'reject' | 'execute'

/** 审批历史记录 */
export interface TransferApprovalHistoryItem {
  /** 步骤类型 */
  step: TransferApprovalStep
  /** 操作人 */
  userName: string
  /** 操作时间 */
  time: string
  /** 意见/备注 */
  comment: string
}

/** 调拨单信息（对应后端 InventoryTransfer） */
export interface InventoryTransferInfo {
  transferId: string
  transferCode: string
  fromWarehouseId: string
  fromWarehouseName: string
  toWarehouseId: string
  toWarehouseName: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  quantity: number
  status: TransferStatus
  statusName: string
  /** 调拨原因 */
  transferReason: string
  applyUserId: string
  applyUserName: string
  applyTime: string
  approveUserId: string
  approveUserName: string
  approveTime: string
  executeTime: string
  /** 审批历史 */
  approvalHistory: TransferApprovalHistoryItem[]
  remark: string
  items: InventoryTransferItemInfo[]
  createTime: string
  updateTime: string
}

/** 调拨明细 */
export interface InventoryTransferItemInfo {
  transferItemId: string
  transferId: string
  materialId: string
  materialName: string
  specification: string
  unit: string
  quantity: number
  remark: string
}

/** 调拨单查询参数 */
export interface InventoryTransferQueryForm {
  transferCode?: string
  fromWarehouseId?: string
  toWarehouseId?: string
  status?: TransferStatus
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 调拨单创建表单 */
export interface InventoryTransferCreateForm {
  fromWarehouseId: string
  toWarehouseId: string
  /** 调拨原因 */
  transferReason: string
  remark?: string
  items: {
    materialId: string
    quantity: number
    remark?: string
  }[]
}

/** 调拨审批表单 */
export interface TransferApproveForm {
  approved: boolean
  remark?: string
}
