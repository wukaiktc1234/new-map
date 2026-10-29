/**
 * POS 端运行时缓存清理
 *
 * 退出登录 / 切换账号时必须清掉的"本班次运行态"：
 *   凭证、终端绑定、开班标记、购物车草稿、客显广播状态。
 *
 * 刻意保留（不属于缓存）：
 *   - 'heldOrders'   挂单是收银员显式保存的单据，清掉等于丢单；
 *   - 'pos-preferences' / 'pos-theme-dark' / 'pos-terminal-alias' 是设备级偏好，
 *     下一班收银员应当沿用同一台机器的显示习惯。
 */

import { clearShiftMark } from './shiftSession'

/** 运行态缓存键（不含挂单与个人偏好） */
const RUNTIME_CACHE_KEYS = [
  'pos-token',
  'pos-user',
  'pos-terminal',
  'pos-cart',
  'pos-current-order',
  'pos_customer_display',
]

/** 清理本班次运行态缓存；返回是否全部清理成功（localStorage 不可用时为 false） */
export function clearPosRuntimeCache(): boolean {
  let ok = true
  for (const key of RUNTIME_CACHE_KEYS) {
    try {
      localStorage.removeItem(key)
    } catch {
      ok = false
    }
  }
  clearShiftMark()
  return ok
}
