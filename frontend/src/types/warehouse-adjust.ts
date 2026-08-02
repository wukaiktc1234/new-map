/**
 * 库存调整 - 类型定义
 *
 * 调整类型：
 * - gain: 盘盈调整（盘点后库存增加）
 * - loss: 盘亏调整（盘点后库存减少）
 * - temp_loss: 温度损耗（冷链温度异常导致的损耗）
 * - weight_diff: 称重差异（实际称重与系统记录的差异）
 * - other: 其他调整
 *
 * 调整状态流转：
 * pending(待审批) → approved(已审批) → completed(已完成)
 *                  → rejected(已驳回)
 */

/** 调整类型 */
export type AdjustType = 'gain' | 'loss' | 'temp_loss' | 'weight_diff' | 'other'

/** 调整状态 */
export type AdjustStatus = 'pending' | 'approved' | 'completed' | 'rejected'

/** 调整明细 */
export interface AdjustItemInfo {
  /** 明细ID */
  adjustItemId: string
  /** 调整单ID */
  adjustId: string
  /** 物料ID */
  materialId: string
  /** 物料名称 */
  materialName: string
  /** 规格型号 */
  specification: string
  /** 单位 */
  unit: string
  /** 调整前数量 */
  beforeQuantity: number
  /** 调整数量（正数为增加，负数为减少） */
  adjustQuantity: number
  /** 调整后数量 */
  afterQuantity: number
  /** 单价（元） */
  unitCost: string
  /** 调整金额（元） */
  adjustAmount: string
  /** 批次号 */
  batchNo: string
  /** 调整原因 */
  reason: string
}

/** 审批历史步骤 */
export type ApprovalStep = 'submit' | 'approve' | 'reject' | 'execute'

/** 审批历史记录 */
export interface ApprovalHistoryItem {
  /** 步骤类型 */
  step: ApprovalStep
  /** 操作人 */
  userName: string
  /** 操作时间 */
  time: string
  /** 意见/备注 */
  comment: string
}

/** 库存调整单信息 */
export interface InventoryAdjustInfo {
  /** 调整单ID */
  adjustId: string
  /** 调整单号 */
  adjustCode: string
  /** 调整类型 */
  adjustType: AdjustType
  /** 调整类型名称 */
  adjustTypeName: string
  /** 调整仓库ID */
  warehouseId: string
  /** 调整仓库名称 */
  warehouseName: string
  /** 关联盘点单号 */
  referenceCheckCode: string
  /** 调整原因分类 */
  adjustReason: string
  /** 关联单号 */
  referenceNo: string
  /** 调整明细 */
  items: AdjustItemInfo[]
  /** 调整总数量 */
  totalAdjustQuantity: number
  /** 调整总金额（元） */
  totalAdjustAmount: string
  /** 调整状态 */
  status: AdjustStatus
  /** 状态名称 */
  statusName: string
  /** 参与部门 */
  participatingDepts: string[]
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
  /** 审批历史 */
  approvalHistory: ApprovalHistoryItem[]
  /** 备注 */
  remark: string
  /** 创建时间 */
  createTime: string
  /** 更新时间 */
  updateTime: string
}

/** 调整单查询表单 */
export interface AdjustQueryForm {
  /** 调整类型 */
  adjustType?: AdjustType
  /** 调整状态 */
  status?: AdjustStatus
  /** 仓库ID */
  warehouseId?: string
  /** 开始日期 */
  startDate?: string
  /** 结束日期 */
  endDate?: string
  /** 关键词 */
  keyword?: string
  /** 页码 */
  page?: number
  /** 每页条数 */
  size?: number
}

/** 调整单创建表单 */
export interface AdjustCreateForm {
  /** 调整类型 */
  adjustType: AdjustType
  /** 调整仓库ID */
  warehouseId: string
  /** 调整原因分类 */
  adjustReason: string
  /** 关联盘点单号 */
  referenceCheckCode?: string
  /** 关联单号 */
  referenceNo?: string
  /** 参与部门 */
  participatingDepts?: string[]
  /** 调整明细 */
  items: {
    materialId: string
    materialName?: string
    specification?: string
    unit?: string
    beforeQuantity?: number
    batchNo?: string
    unitCost?: string
    adjustQuantity: number
    reason: string
  }[]
  /** 备注 */
  remark?: string
}

/** 调整审批表单 */
export interface AdjustApproveForm {
  /** 是否通过 */
  approved: boolean
  /** 审批意见 */
  opinion?: string
}
