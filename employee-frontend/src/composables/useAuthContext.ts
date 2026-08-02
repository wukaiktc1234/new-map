/**
 * useAuthContext - 统一身份上下文
 *
 * 全应用唯一的身份数据源（Single Source of Truth）。
 * 所有需要"当前用户是谁"的地方都必须从这里获取，禁止在业务代码中硬编码用户名。
 *
 * 设计原则：
 * - 当前为 mock 模式，身份硬编码在此处
 * - 对接真实后端后，仅需替换内部实现，所有引用方无需改动
 * - 与 usePermissionStore（权限/层级）配合使用：本模块管"是谁"，permission 管"能做什么"
 */
import { ref, readonly } from 'vue'
import { UserLevel } from '@/types/permission'

// ============================================
// 内部状态（mock 阶段硬编码，生产环境从 token/store 获取）
// ============================================

/** 当前登录用户姓名 */
const _userName = ref('张三')

/** 当前用户层级 */
const _userLevel = ref<UserLevel>(UserLevel.STAFF)

/** 当前用户角色代码 */
const _userRole = ref('store_staff')

/** 当前用户 ID */
const _userId = ref('u001')

// ============================================
// 公开 API（只读）
// ============================================

export function useAuthContext() {
  /** 当前登录用户姓名（只读） */
  const userName = readonly(_userName)

  /** 当前用户层级枚举值（只读） */
  const userLevel = readonly(_userLevel)

  /** 当前用户角色代码（只读） */
  const userRole = readonly(_userRole)

  /** 当前用户 ID（只读） */
  const userId = readonly(_userId)

  /** 是否为经理及以上（L3+，可查看团队数据） */
  const isManager = () => _userLevel.value >= UserLevel.MANAGER

  /** 是否有审批权限（L2+） */
  const canApprove = () => _userLevel.value >= UserLevel.SUPERVISOR

  /**
   * 切换当前用户身份（仅 mock 阶段使用，用于测试多角色场景）
   *
   * @param options - 要切换的身份信息
   */
  function switchIdentity(options: {
    name: string
    level: UserLevel
    role: string
    id?: string
  }) {
    _userName.value = options.name
    _userLevel.value = options.level
    _userRole.value = options.role
    if (options.id) _userId.value = options.id
  }

  return {
    userName,
    userLevel,
    userRole,
    userId,
    isManager,
    canApprove,
    switchIdentity,
  }
}
