/**
 * 申诉数据转换器
 *
 * 处理前后端数据格式差异：
 * - 状态字段语义化映射
 * - 类型字段中文显示
 * - 操作类型中文显示
 */

/**
 * 申诉状态 → StatusTag 映射
 *
 * 后端状态值 → 前端StatusTag组件status属性
 * resolved(已解决) 映射为 approved(绿色通过态)
 */
export const appealStatusMap: Record<string, string> = {
  pending: 'pending',       // 待处理 → pending 黄色
  processing: 'processing', // 处理中 → processing 蓝色
  resolved: 'approved',     // 已解决 → approved 绿色
  closed: 'info',           // 已关闭 → info 灰色
  withdrawn: 'default',     // 已撤回 → default 默认灰色
} as const

/**
 * 申诉类型中文显示
 */
export const appealTypeLabelMap: Record<string, string> = {
  penalty: '处罚申诉',
  complaint: '投诉举报',
} as const

/**
 * 申诉操作类型中文显示
 */
export const appealActionLabelMap: Record<string, string> = {
  submit: '提交申请',
  accept: '已受理',
  process: '处理中',
  resolve: '已完成',
  close: '已关闭',
  withdraw: '已撤回',
  reject: '已驳回',
} as const

/**
 * 将后端状态转换为StatusTag组件可用的status值
 */
export function convertAppealStatus(backendStatus: string): string {
  return appealStatusMap[backendStatus] || 'default'
}
