export enum UserRole {
  OWNER = 'owner',
  ADMIN = 'admin',
  OPS_DIRECTOR = 'ops_director',
  FINANCE_DIRECTOR = 'finance_director',
  HR_DIRECTOR = 'hr_director',
  STORE_MANAGER = 'store_manager',
  TEAM_LEADER = 'team_leader',
  EMPLOYEE = 'employee',
}

export interface UserInfo {
  userId: string | number
  username: string
  fullName?: string
  roles: UserRole[]
  storeName?: string
  departmentName?: string
  avatar?: string
  phone?: string
  /** 用户层级（1-5，对应 UserLevel 枚举） */
  level?: number
  /** 层级名称（如 staff, supervisor, manager 等） */
  levelName?: string
}
