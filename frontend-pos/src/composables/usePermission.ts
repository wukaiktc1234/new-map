import { computed, ref } from 'vue'
import request from '@/api/request'

/**
 * POS 前端能力（权限码）判定 —— **权限唯一来源：服务端登录响应**
 *
 * 背景：后端 POS 退款端点用 @PreAuthorize('order:refund') 把守；收银员可以开班但**不能退款**。
 * 安全要求（防前端成为权限绕过入口）：
 *   ① 只信 `POST /v1/pos/auth/login` 返回的 `userInfo.permissions`（服务端 DB 授权结果）
 *   ② **绝不**从 localStorage / sessionStorage 读取权限 —— 本地可篡改，读到旧权限就会渲染出按钮，点了必 403
 *   ③ 取不到权限 = **拒绝**（默认 false），不做角色名启发式、不做内置 admin 特判、不做「运行时授予」
 *   ④ 页面刷新后内存清空 → 由路由守卫引导重新登录（不在本地恢复权限）
 *
 * 说明：`markDenied` 只记录「后端已 403」的能力，属于**收紧**而非授权，且仅存内存。
 */

export const PERM = {
  REFUND: 'order:refund',
  DISCOUNT: 'order:discount',
  SHIFT_END: 'shift:end'
} as const

interface Capabilities {
  permissions: string[]
  roles: string[]
  /** 后端 403 后置灰的能力码（仅内存） */
  denied: string[]
  /** 是否已经从登录响应拿到过能力快照（false = 需要重新登录） */
  loaded: boolean
}

function toArray(value: unknown): string[] {
  if (!value) return []
  if (Array.isArray(value)) {
    return value
      .map((v) => (typeof v === 'string' ? v : (v as { authority?: string })?.authority || ''))
      .map((v) => v.trim())
      .filter(Boolean)
  }
  if (typeof value === 'string') {
    return value.split(/[,;\s]+/).map((v) => v.trim()).filter(Boolean)
  }
  return []
}

/** 内存快照：初始为空 = 未登录/未加载 → 一切判定 false（fail-closed） */
const capabilities = ref<Capabilities>({
  permissions: [],
  roles: [],
  denied: [],
  loaded: false
})

/**
 * 写入能力快照（**只能**由登录响应调用）
 * @param permissions 服务端 userInfo.permissions
 * @param roles 服务端 userInfo.roles（仅展示用途，不参与授权判定）
 */
export function setCapabilities(permissions: unknown, roles: unknown): void {
  capabilities.value = {
    permissions: toArray(permissions),
    roles: toArray(roles).map((r) => r.toLowerCase()),
    denied: [],
    loaded: true
  }
}

/** 清空能力（登出/令牌失效时调用） */
export function clearCapabilities(): void {
  capabilities.value = { permissions: [], roles: [], denied: [], loaded: false }
}

/** 能力是否已从登录响应加载（false → 需要重新登录） */
export function hasCapabilitiesLoaded(): boolean {
  return capabilities.value.loaded
}

/**
 * 静默恢复能力快照（刷新/新标签页用）
 *
 * 仍然不从 localStorage 取权限（本地可篡改），而是拿已有的 token 问服务端要一份。
 * 成功 → 写入内存并返回 true；失败（401/403/网络）→ 返回 false，由调用方决定是否回登录页。
 */
export async function restoreCapabilities(): Promise<boolean> {
  if (capabilities.value.loaded) return true
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('pos-token') : null
  if (!token) return false
  try {
    const me = (await request.get('/v1/pos/auth/me')) as unknown as {
      permissions?: unknown
      roles?: unknown
    } | null
    if (!me) return false
    setCapabilities(me.permissions ?? (me as { perms?: unknown }).perms, me.roles ?? (me as { roleName?: unknown }).roleName)
    return capabilities.value.loaded
  } catch {
    return false
  }
}

/**
 * 兼容旧调用点的空实现：以前它从 localStorage 重新读取权限，
 * 现在权限只在内存里，这里不再读任何存储（保持函数签名以免大范围改调用方）。
 */
export function refreshCapabilities(): void {
  if (!capabilities.value.loaded) {
    // 没有登录响应快照 → 维持「空 = 无权限」，不做任何本地恢复
    capabilities.value = { permissions: [], roles: [], denied: [], loaded: false }
  }
}

function evaluate(state: Capabilities, code: string): boolean {
  if (!code) return false
  if (!state.loaded) return false
  if (state.denied.includes(code)) return false
  if (state.permissions.includes('*')) return true
  return state.permissions.includes(code)
}

export function usePermission() {
  const can = (code: string): boolean => evaluate(capabilities.value, code)

  const canRefund = computed(() => evaluate(capabilities.value, PERM.REFUND))

  /** 后端 403 后记住：立刻消失（快照更新 → 所有用到 canRefund 的组件重渲染）。仅收紧，不授予 */
  const markDenied = (code: string): void => {
    if (!code) return
    capabilities.value = {
      ...capabilities.value,
      denied: Array.from(new Set([...capabilities.value.denied, code]))
    }
  }

  const denyAllKnown = (): boolean => evaluate(capabilities.value, PERM.REFUND)

  return { can, canRefund, markDenied, denyAllKnown, PERM, refreshCapabilities }
}

export default usePermission
