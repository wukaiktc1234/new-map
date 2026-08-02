/**
 * v-permission 权限控制自定义指令
 *
 * 功能说明：
 * 1. 基本用法：v-permission="'product:dish:create'" - 无权限时隐藏元素（display:none）
 * 2. 通配符匹配：v-permission="'product:dish:*'" - 匹配 product:dish 下所有子权限
 * 3. 禁用模式：v-permission.disable="'product:dish:delete'" - 无权限时禁用+置灰
 * 4. 多权限（满足其一即可）：v-permission="['perm1', 'perm2']"
 *
 * 权限数据来源：permission Store (usePermissionStore) — 与路由守卫、菜单过滤使用同一数据源，
 * 确保权限变更后指令能实时更新DOM。
 *
 * 使用示例：
 * ```vue
 * <el-button v-permission="'product:dish:create'">新增</el-button>
 * <el-button v-permission.disable="'product:dish:delete'">删除</el-button>
 * ```
 */

import type { Directive, DirectiveBinding } from 'vue'
import { usePermissionStore } from '@/stores/permission'

/** 标记元素被权限指令隐藏的data属性 */
const PERMISSION_HIDDEN_ATTR = 'data-permission-hidden'
/** 标记元素被权限指令禁用的data属性 */
const PERMISSION_DISABLED_ATTR = 'data-permission-disabled'

/**
 * 从Store获取用户权限列表（与菜单/路由守卫共享同一数据源）
 *
 * @returns 用户权限标识符数组
 */
function getUserPermissions(): string[] {
  try {
    const store = usePermissionStore()
    if (!store.userInfo) {
      return getFallbackPermissions()
    }
    const permissions = store.userInfo.permissions || []
    if (permissions.length === 0) {
      return getFallbackPermissions()
    }
    return permissions
  } catch {
    // Store未初始化时，不信任localStorage（安全策略）
    return getFallbackPermissions()
  }
}

/**
 * 降级权限获取（按环境区分安全策略）
 *
 * - 开发环境（import.meta.env.DEV）：返回 ['*']（超级管理员权限）
 * - 生产环境（import.meta.env.PROD）：返回 []（空数组，无任何权限）
 *
 * 注意：不再从localStorage读取权限，防止权限篡改攻击
 */
function getFallbackPermissions(): string[] {
  if (import.meta.env.DEV) {
    console.warn('[v-permission] 未检测到权限数据，已启用开发模式降级（超级管理员）')
    return ['*']
  }
  console.warn('[v-permission] 未检测到权限数据，生产环境不启用降级，所有受保护元素将隐藏')
  return []
}

/**
 * 检查单个权限是否匹配
 * 支持精确匹配和通配符匹配
 *
 * 通配符规则：
 * 1. "*" - 超级管理员，匹配所有权限
 * 2. "module:*" - 模块级通配符，如 "product:*" 匹配 "product:dish:create"
 * 3. "module:sub:*" - 多级通配符，如 "product:dish:*" 匹配 "product:dish:create"
 *
 * @param userPermissions - 用户拥有的权限列表
 * @param requiredPermission - 需要检查的权限标识符
 * @returns 是否拥有该权限
 */
function matchSinglePermission(
  userPermissions: string[],
  requiredPermission: string
): boolean {
  // 1. 精确匹配：用户权限列表中直接包含该权限
  if (userPermissions.includes(requiredPermission)) {
    return true
  }

  // 2. 超级管理员通配符：拥有 "*" 权限表示拥有所有权限
  if (userPermissions.includes('*')) {
    return true
  }

  // 3. 模块级通配符匹配
  const requiredParts = requiredPermission.split(':')

  for (const userPerm of userPermissions) {
    if (!userPerm.includes('*')) continue

    const userParts = userPerm.split(':')
    let isMatch = true

    for (let i = 0; i < requiredParts.length; i++) {
      const reqPart = requiredParts[i]
      const userPart = userParts[i]

      if (userPart === '*') {
        if (i === userParts.length - 1) {
          break
        }
        continue
      }

      if (i >= userParts.length) {
        isMatch = false
        break
      }

      if (userPart !== reqPart) {
        isMatch = false
        break
      }
    }

    if (isMatch) return true
  }

  // 4. 兼容旧逻辑：requiredPermission 以 ":*" 结尾时进行前缀匹配
  if (requiredPermission.endsWith(':*')) {
    const prefix = requiredPermission.slice(0, -2)
    return userPermissions.some((permission) => permission.startsWith(prefix))
  }

  return false
}

/**
 * 检查是否拥有指定权限（核心匹配算法）
 * 支持字符串和数组两种形式
 *
 * @param requiredPermissions - 需要检查的权限（字符串或字符串数组）
 * @returns 是否拥有任一所需权限
 */
export function checkPermission(
  requiredPermissions: string | string[]
): boolean {
  const userPermissions = getUserPermissions()

  // 字符串形式：检查单个权限
  if (typeof requiredPermissions === 'string') {
    return matchSinglePermission(userPermissions, requiredPermissions)
  }

  // 数组形式：满足任一权限即通过（OR逻辑）
  if (Array.isArray(requiredPermissions) && requiredPermissions.length > 0) {
    return requiredPermissions.some((permission) =>
      matchSinglePermission(userPermissions, permission)
    )
  }

  return false
}

/** v-permission指令主体 */
const permissionDirective: Directive<HTMLElement, string | string[]> = {
  /**
   * 指令挂载时执行权限检查
   */
  mounted(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    applyPermissionCheck(el, binding)
  },

  /**
   * 指令更新时重新执行权限检查
   * 用于处理动态权限变更的场景（如切换角色后）
   */
  updated(el: HTMLElement, binding: DirectiveBinding<string | string[]>) {
    applyPermissionCheck(el, binding)
  },
}

/**
 * 应用权限检查逻辑
 * 根据检查结果决定元素的显示/隐藏/禁用状态
 *
 * 使用 display:none 隐藏替代 removeChild 移除，
 * 确保角色切换后元素可以恢复显示
 */
function applyPermissionCheck(
  el: HTMLElement,
  binding: DirectiveBinding<string | string[]>
): void {
  const { value, modifiers } = binding
  const requiredPermission = value

  if (!requiredPermission) {
    console.warn('[v-permission] 未指定权限标识符')
    return
  }

  const permitted = checkPermission(requiredPermission)

  if (permitted) {
    // 有权限：恢复元素状态
    restoreElement(el, modifiers)
  } else {
    // 无权限：隐藏或禁用
    if (modifiers.disable) {
      applyDisableMode(el)
    } else {
      applyHideMode(el)
    }
  }
}

/**
 * 隐藏模式：使用 display:none 隐藏元素
 * 保留DOM节点，角色切换后可恢复
 */
function applyHideMode(el: HTMLElement): void {
  el.style.display = 'none'
  el.setAttribute(PERMISSION_HIDDEN_ATTR, 'true')
  // 清除禁用状态（如果之前是禁用模式）
  if (el.hasAttribute(PERMISSION_DISABLED_ATTR)) {
    el.removeAttribute(PERMISSION_DISABLED_ATTR)
    el.removeAttribute('disabled')
    el.removeAttribute('title')
    el.style.pointerEvents = ''
    el.style.opacity = ''
    el.classList.remove('is-disabled')
  }
}

/**
 * 禁用模式：置灰+禁止交互
 */
function applyDisableMode(el: HTMLElement): void {
  el.setAttribute('disabled', 'true')
  el.setAttribute('title', '您没有权限执行此操作')
  el.style.pointerEvents = 'none'
  el.style.opacity = '0.5'
  el.setAttribute(PERMISSION_DISABLED_ATTR, 'true')

  // 清除隐藏状态（如果之前是隐藏模式）
  if (el.hasAttribute(PERMISSION_HIDDEN_ATTR)) {
    el.removeAttribute(PERMISSION_HIDDEN_ATTR)
    el.style.display = ''
  }

  if (
    el.tagName === 'BUTTON' ||
    el.classList.contains('el-button') ||
    el.classList.contains('el-link')
  ) {
    el.classList.add('is-disabled')
  }
}

/**
 * 恢复元素到正常状态（有权限时调用）
 */
function restoreElement(el: HTMLElement, modifiers: DirectiveBinding['modifiers']): void {
  // 恢复隐藏模式
  if (el.hasAttribute(PERMISSION_HIDDEN_ATTR)) {
    el.removeAttribute(PERMISSION_HIDDEN_ATTR)
    el.style.display = ''
  }

  // 恢复禁用模式
  if (el.hasAttribute(PERMISSION_DISABLED_ATTR)) {
    el.removeAttribute(PERMISSION_DISABLED_ATTR)
    el.removeAttribute('disabled')
    el.removeAttribute('title')
    el.style.pointerEvents = ''
    el.style.opacity = ''
    el.classList.remove('is-disabled')
  }
}

export default permissionDirective
