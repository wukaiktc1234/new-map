import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { UserInfo, UserRole } from '@/types/user'
import { usePermissionStore } from '@/stores/permission'
import type { EmployeeRole } from '@/stores/permission'
import { UserLevel } from '@/types/permission'

/**
 * UserRole（API返回）→ EmployeeRole（权限体系）映射表
 * 权限Store使用 EmployeeRole 作为角色/权限的唯一事实来源
 */
const ROLE_MAP: Partial<Record<UserRole, EmployeeRole>> = {
  [UserRole.EMPLOYEE]: 'store_staff',
  [UserRole.TEAM_LEADER]: 'store_staff',
  [UserRole.STORE_MANAGER]: 'store_manager',
  [UserRole.HR_DIRECTOR]: 'hr',
  [UserRole.FINANCE_DIRECTOR]: 'finance',
  [UserRole.ADMIN]: 'headquarters',
  [UserRole.OWNER]: 'headquarters',
  [UserRole.OPS_DIRECTOR]: 'headquarters',
}

/** UserRole → UserLevel 层级映射表 */
const ROLE_LEVEL_MAP: Partial<Record<UserRole, UserLevel>> = {
  [UserRole.EMPLOYEE]: UserLevel.STAFF,
  [UserRole.TEAM_LEADER]: UserLevel.STAFF,
  [UserRole.STORE_MANAGER]: UserLevel.SUPERVISOR,
  [UserRole.HR_DIRECTOR]: UserLevel.MANAGER,
  [UserRole.FINANCE_DIRECTOR]: UserLevel.MANAGER,
  [UserRole.ADMIN]: UserLevel.ADMIN,
  [UserRole.OWNER]: UserLevel.ADMIN,
  [UserRole.OPS_DIRECTOR]: UserLevel.REGION_MANAGER,
}

/** 从 UserRole 数组中提取主角色并转换为 EmployeeRole */
function mapToEmployeeRole(roles: UserRole[]): EmployeeRole {
  for (const r of roles) {
    const mapped = ROLE_MAP[r]
    if (mapped) return mapped
  }
  return 'store_staff'
}

/** 从 UserRole 数组或显式 level 值提取用户层级 */
function mapToUserLevel(roles: UserRole[], explicitLevel?: number): UserLevel {
  // 优先使用显式传入的 level 值
  if (explicitLevel && explicitLevel >= UserLevel.STAFF && explicitLevel <= UserLevel.ADMIN) {
    return explicitLevel as UserLevel
  }
  // 否则从角色推断
  for (const r of roles) {
    const mapped = ROLE_LEVEL_MAP[r]
    if (mapped) return mapped
  }
  return UserLevel.STAFF
}

export const useEmployeeStore = defineStore('employee', () => {
  const permission = usePermissionStore()

  const userInfo = ref<UserInfo>({
    userId: '',
    username: '',
    roles: [],
    storeName: '',
    departmentName: '',
  })

  const isLoggedIn = computed(() => !!localStorage.getItem('token'))

  /** 委托读取权限Store的当前角色（唯一事实来源） */
  const currentRole = computed<EmployeeRole>(() => permission.currentRole)

  /** 基于权限Store角色判断是否为门店人员 */
  const isStoreStaff = computed(() => {
    const storeRoles: EmployeeRole[] = ['store_staff', 'store_manager', 'kitchen_staff']
    return storeRoles.includes(permission.currentRole)
  })

  /**
   * 设置用户信息（登录成功后调用）
   * 内部自动将API返回的角色同步到权限Store，确保单一数据源
   * 同时将层级信息同步到权限Store
   */
  function setUserInfo(info: Partial<UserInfo>) {
    userInfo.value = { ...userInfo.value, ...info }
    const roles = info.roles ?? []
    const employeeRole = mapToEmployeeRole(roles)
    const userLevelValue = mapToUserLevel(roles, info.level)
    permission.initFromApi(employeeRole, {
      name: info.fullName || info.username || '',
      employeeNo: '',
      phone: info.phone || '',
      email: '',
      department: info.departmentName || '',
      position: '',
      storeName: info.storeName || '',
      joinDate: '',
      probationEnd: '',
      workStatus: '',
      avatar: info.avatar || '',
      supervisorName: '',
    }, userLevelValue)
  }

  function logout() {
    localStorage.removeItem('token')
    localStorage.removeItem('refreshToken')
    localStorage.removeItem('tokenExpiresAt')
    userInfo.value = {
      userId: '',
      username: '',
      roles: [],
      storeName: '',
      departmentName: '',
    }
    permission.setRole('store_staff')
  }

  return {
    userInfo,
    isLoggedIn,
    currentRole,
    isStoreStaff,
    setUserInfo,
    logout,
  }
})
