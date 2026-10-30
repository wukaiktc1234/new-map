import { useHangingOrderStore } from '@/stores/hanging-order'
import { useShiftHandoverStore } from '@/stores/shiftHandover'
import { clearBusinessCaches } from './posCache'

/**
 * 登录成功 / 退出登录 / 切换账号 时的统一重置入口
 *
 * 三件事必须一起做，少一件就会串号：
 *   ① 物理缓存：localStorage 运行态 + sessionStorage 草稿（clearPosRuntimeCache）；
 *   ② Pinia 内存：挂单、交班统计等业务 store 一律 $reset()；
 *   ③ 会话标记：记录"当前是哪个账号在用这台机器"，守卫据此判断是否换人。
 *
 * 注意：偏好类（主题/字号/终端别名）不在此列，属于设备级设置。
 */

const SESSION_USER_KEY = 'pos-session:user'

/** 只重置内存 store（不动任何存储）—— 同一账号重新登录时用 */
export function resetStoresInMemory(): void {
  try {
    useHangingOrderStore().$reset()
  } catch {
    /* no active pinia */
  }
  try {
    useShiftHandoverStore().$reset()
  } catch {
    /* no active pinia */
  }
}

/** 换账号 / 重新登录：只清业务残留 + 重置内存 store（不动凭证） */
export function resetBusinessState(): void {
  // ① 物理缓存（只清业务残留；凭证是登录流程刚写的，清掉就白登录了）
  clearBusinessCaches()

  // ② 内存 store（Pinia 未初始化时静默跳过，避免在模块顶层炸掉）
  try {
    useHangingOrderStore().$reset()
  } catch {
    /* no active pinia */
  }
  try {
    useShiftHandoverStore().$reset()
  } catch {
    /* no active pinia */
  }

  // ③ 会话标记
  try {
    sessionStorage.removeItem(SESSION_USER_KEY)
  } catch {
    /* ignore */
  }
}

/** 退出登录：在其之上再清凭证与终端绑定 */
export function clearAuthSession(): void {
  try {
    localStorage.removeItem('pos-token')
    localStorage.removeItem('pos-user')
    localStorage.removeItem('pos-terminal')
  } catch {
    /* ignore */
  }
  resetBusinessState()
}

/** 记录当前登录账号（登录成功时调用），供路由守卫识别"换人" */
export function markSessionUser(employeeId: string): void {
  try {
    sessionStorage.setItem(SESSION_USER_KEY, String(employeeId || ''))
  } catch {
    /* ignore */
  }
}

export function readSessionUser(): string {
  try {
    return sessionStorage.getItem(SESSION_USER_KEY) || ''
  } catch {
    return ''
  }
}

/**
 * 进入首页前的兜底：发现"当前账号 ≠ 上次会话账号"就重置，
 * 覆盖"直接改 localStorage 里的 user 再刷新"这类非正常切换路径。
 */
export function ensureSessionUser(employeeId: string): boolean {
  const current = String(employeeId || '')
  const previous = readSessionUser()
  if (previous && current && previous !== current) {
    resetBusinessState()
    markSessionUser(current)
    return true
  }
  if (current) markSessionUser(current)
  return false
}
