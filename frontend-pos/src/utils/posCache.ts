/**
 * POS 端缓存清理
 *
 * 两档清理，别混用：
 *   ① clearBusinessCaches()：换账号/重新登录用 —— 只清"上一位收银员的业务残留"
 *      （购物车、挂单、草稿、班次标记），**绝不动刚写好的凭证**，否则登录完立刻掉线；
 *   ② clearPosRuntimeCache()：退出登录用 —— 连凭证、终端绑定、客显一起清。
 *
 * 历史坑：早期把挂单当"收银员显式保存的单据"保留 → 换账号后新收银员看到上一个人（甚至几天前）
 * 的挂单，属于串号事故。现在挂单按"本班次运行态"处理，切号即清。
 *
 * 始终保留（设备级偏好，与账号无关）：'pos-preferences' / 'pos-theme-dark' / 'pos-terminal-alias'。
 */

import { clearShiftMark } from './shiftSession'

/** 业务残留（与账号强相关） */
const BUSINESS_CACHE_KEYS = [
  'pos-cart',
  'pos-current-order',
  'heldOrders',
  'heldOrders:backup',
  'pos_customer_display',
]

/**
 * 挂单键前缀（P0-1）：挂单已按 heldOrders:<userId>:<terminalId> 命名空间存储，
 * 换账号时把**所有**命名空间键一起清掉，避免「上一位收银员的挂单」留在机器上。
 */
const HELD_ORDER_KEY_PREFIX = 'heldOrders:'

function removeHeldOrderKeys(): boolean {
  try {
    const toRemove: string[] = []
    for (let i = 0; i < localStorage.length; i += 1) {
      const key = localStorage.key(i)
      if (key && key.startsWith(HELD_ORDER_KEY_PREFIX)) toRemove.push(key)
    }
    toRemove.forEach((key) => localStorage.removeItem(key))
    return true
  } catch {
    return false
  }
}

/** 凭证与终端绑定（只有退出登录才清） */
const AUTH_CACHE_KEYS = ['pos-token', 'pos-user', 'pos-terminal']

/** sessionStorage 里属于"某个人某一次操作"的草稿，切号必须清 */
const SESSION_KEY_PREFIXES = ['pos-draft:', 'pos-session:']

function removeKeys(keys: string[]): boolean {
  let ok = true
  for (const key of keys) {
    try {
      localStorage.removeItem(key)
    } catch {
      ok = false
    }
  }
  return ok
}

function removeSessionDrafts(): boolean {
  try {
    const toRemove: string[] = []
    for (let i = 0; i < sessionStorage.length; i += 1) {
      const key = sessionStorage.key(i)
      if (key && SESSION_KEY_PREFIXES.some((prefix) => key.startsWith(prefix))) toRemove.push(key)
    }
    toRemove.forEach((key) => sessionStorage.removeItem(key))
    return true
  } catch {
    return false
  }
}

/** 换账号 / 重新登录：清业务残留，保留凭证 */
export function clearBusinessCaches(): boolean {
  const ok = removeKeys(BUSINESS_CACHE_KEYS)
  const holds = removeHeldOrderKeys()
  const drafts = removeSessionDrafts()
  clearShiftMark()
  return ok && holds && drafts
}

/** 退出登录：业务残留 + 凭证 + 班次标记全清 */
export function clearPosRuntimeCache(): boolean {
  const ok = removeKeys([...BUSINESS_CACHE_KEYS, ...AUTH_CACHE_KEYS])
  const holds = removeHeldOrderKeys()
  const drafts = removeSessionDrafts()
  clearShiftMark()
  return ok && holds && drafts
}
