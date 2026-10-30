/**
 * POS 功能开关（前后端契约就绪情况）
 *
 * 规则：后端没有的接口，前端一律不画入口 —— 宁可少一个 Tab，也不要给用户一条断头路。
 */
export const POS_FLAGS = {
  /**
   * 扫码收款是否可用。
   * R8 审计 F-P0-1：后端尚未提供扫码补收接口，前端曾画出完整 UI，点击必然 405/501。
   * 后端上线并验收通过后，这里改 true 即可恢复入口（UI 已就绪，无需改组件）。
   */
  SCAN_BACKEND_READY: false
} as const

export type PosFlags = typeof POS_FLAGS
