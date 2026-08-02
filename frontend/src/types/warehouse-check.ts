/**
 * 库存盘点类型定义
 * 对齐后端: InventoryCheck + InventoryCheckItem实体
 */

/** 盘点类型（前端语义化） */
export type CheckType = 'blind' | 'open' | 'cycle'

/** 盘点模式（明盘/盲盘） */
export type CheckMode = 'open' | 'blind'

/** 盘点状态（前端语义化） */
export type CheckStatus = 'pending' | 'checking' | 'approved' | 'completed' | 'cancelled'

/** 部门确认记录 */
export interface DeptConfirmation {
  /** 部门名称 */
  deptName: string
  /** 是否已确认 */
  confirmed: boolean
  /** 确认用户ID */
  confirmUserId: string
  /** 确认用户名称 */
  confirmUserName: string
  /** 确认时间 */
  confirmTime: string
}

/** 盘点人员配置 */
export interface CheckTeamConfig {
  /** 盘点负责人ID */
  leaderId: string
  /** 盘点负责人名称 */
  leaderName: string
  /** 盘点员ID列表 */
  counterIds: string[]
  /** 盘点员名称列表 */
  counterNames: string[]
  /** 财务监督员ID */
  financeSupervisorId: string
  /** 财务监督员名称 */
  financeSupervisorName: string
  /** 仓储管理员ID */
  warehouseManagerId: string
  /** 仓储管理员名称 */
  warehouseManagerName: string
}

/** 盘点单信息（对应后端 InventoryCheck） */
export interface InventoryCheckInfo {
  checkId: string
  checkCode: string
  warehouseId: string
  warehouseName: string
  checkType: CheckType
  checkTypeName: string
  checkStatus: CheckStatus
  checkStatusName: string
  checkDate: string
  completeDate: string
  createUserId: string
  createUserName: string
  approveUserId: string
  approveUserName: string
  approveTime: string
  /** 参与部门 */
  participatingDepts?: string[]
  /** 各部门签字确认 */
  deptConfirmations?: DeptConfirmation[]
  /** 盘点模式 */
  checkMode: CheckMode
  /** 盘点团队 */
  checkTeam: CheckTeamConfig
  /** 是否冻结库存 */
  freezeInventory: boolean
  /** 差异率阈值(%) */
  varianceThreshold: number
  /** 仓储主管审核状态: pending/approved/rejected */
  warehouseApprovalStatus: string
  /** 仓储主管审核人 */
  warehouseApproverName: string
  /** 仓储主管审核时间 */
  warehouseApproveTime: string
  /** 财务监督确认状态: pending/confirmed/rejected */
  financeConfirmStatus: string
  /** 财务监督确认人 */
  financeConfirmerName: string
  /** 财务监督确认时间 */
  financeConfirmTime: string
  remark: string
  items: InventoryCheckItemInfo[]
  createTime: string
  updateTime: string
}

/** 盘点明细（对应后端 InventoryCheckItem） */
export interface InventoryCheckItemInfo {
  checkItemId: string
  checkId: string
  inventoryId: string
  materialName: string
  specification: string
  unit: string
  bookQty: number
  actualQty: number
  diffQty: number
  diffAmount: string
  reason: string
  /** 双人确认 */
  dualConfirmed?: boolean
  /** 拍照留证 */
  hasPhoto?: boolean
  /** 差异说明（超过阈值时必填） */
  varianceExplanation?: string
}

/** 盘点单查询参数 */
export interface InventoryCheckQueryForm {
  warehouseId?: string
  checkStatus?: CheckStatus
  checkCode?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

/** 盘点单创建表单 */
export interface InventoryCheckCreateForm {
  warehouseId: string
  checkType: CheckType
  checkDate: string
  /** 参与部门 */
  participatingDepts?: string[]
  remark?: string
  /** 盘点模式 */
  checkMode?: CheckMode
  /** 盘点团队 */
  checkTeam?: CheckTeamConfig
  /** 是否冻结库存 */
  freezeInventory?: boolean
  /** 差异率阈值(%) */
  varianceThreshold?: number
}

/** 盘点周期（前端语义化） */
export type CheckPlanCycle = 'once' | 'daily' | 'weekly' | 'monthly' | 'quarterly'

/** 盘点计划状态（前端语义化） */
export type CheckPlanStatus = 'active' | 'inactive'

/** 盘点明细提交表单 */
export interface InventoryCheckItemForm {
  checkItemId: string
  actualQty: number
  reason?: string
  dualConfirmed?: boolean
  hasPhoto?: boolean
  varianceExplanation?: string
}

/** 盘点计划信息 */
export interface InventoryCheckPlanInfo {
  planId: string
  planName: string
  warehouseId: string
  warehouseName: string
  checkType: CheckType
  checkMode: CheckMode
  plannedDate: string
  cycle: CheckPlanCycle
  cycleName: string
  status: CheckPlanStatus
  financeSupervisorId: string
  financeSupervisorName: string
  lastExecutedTime: string
  nextExecuteTime: string
  remark: string
  createTime: string
  updateTime: string
}
