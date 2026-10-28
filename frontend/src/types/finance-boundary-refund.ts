/**

 * 边界退款（boundary refund）待确认归属的数据契约
 *
 * 背景（Owner 拍板）：退款时间距交班（start/end）±5 分钟内的退款标记为 [边界退款]，
 * 生成缴款单时不计入任何班次，落审计 RECON_SHIFT_BOUNDARY_PENDING，
 * 需财务/店长人工指定「归上一班」还是「归这一班」。
 *
 * ⚠️ 契约状态：**提交接口的路径/入参/权限码由 fund-500 提供**，本文件只定义前端类型。
 *    当前页面沿用已上线的 `POST /v1/finance/deposit-slips/{slipId}/confirm-attribution`
 *    （body: { refundRecordId, shiftId, reason }），**不是编造的接口**；
 *    fund-500 给出正式契约后再替换，改的是 api 层一处调用，页面不动。
 */

/** 涉及班次的最小信息 */
export interface BoundaryShiftRef {
  shiftId: number
  /** 展示名，如 "morning #325" */
  label?: string | null
  startTime?: string | null
  endTime?: string | null
}

/** 一条待确认归属的边界退款 */
export interface BoundaryRefundPending {
  refundRecordId: number
  refundNo: string | null
  /** 分 */
  amountFen: number | null
  /** 退款发生时间（yyyy-MM-dd HH:mm:ss） */
  refundTime: string | null
  /** 距交班边界的分钟数（绝对值，前端按 refundTime 与班次边界计算） */
  boundaryDeltaMinutes: number | null
  /** 贴近的是开班(start)还是交班(end)边界 */
  boundaryKind: 'start' | 'end' | 'unknown'
  /** 上一班 */
  previousShift: BoundaryShiftRef | null
  /** 这一班 */
  currentShift: BoundaryShiftRef | null
}

/** 归属选择：归上一班 / 归这一班 */
export type BoundaryRefundDecisionKind = 'previous' | 'current'

/** 提交载荷（字段名与现有 confirm-attribution 对齐，正式契约待 fund-500 确认） */
export interface BoundaryRefundDecision {
  refundRecordId: number
  shiftId: number
  reason: string
  kind: BoundaryRefundDecisionKind
}
