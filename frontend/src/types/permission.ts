/**
 * 权限相关类型定义
 */

import type { UserRole } from '@/stores/permission'

/**
 * 数据权限范围类型
 */
export type DataScopeType =
  | 'all'          // 全部数据
  | 'company'      // 本公司
  | 'region'       // 本区域
  | 'store'        // 本门店
  | 'department'   // 本部门
  | 'stores'       // 指定门店
  | 'departments'  // 指定部门
  | 'self';        // 仅本人

/**
 * 用户状态类型
 */
export type UserStatus = 'active' | 'inactive' | 'locked';

/**
 * 用户信息
 */
export interface User {
  id: string;
  username: string;
  email: string;
  phone: string;
  fullName: string;
  avatar: string;
  storeId: string;
  storeName: string;
  departmentId: string;
  departmentName: string;
  status: UserStatus;
  isLocked: boolean;
  lockTime: string | null;
  passwordErrorCount: number;
  lastLoginTime: string | null;
  roles: Role[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 用户创建DTO
 */
export interface UserCreateDTO {
  username: string;
  password: string;
  email: string;
  phone: string;
  fullName: string;
  storeId?: string;
  departmentId?: string;
  roleIds: string[];
}

/**
 * 用户更新DTO
 */
export interface UserUpdateDTO {
  email?: string;
  phone?: string;
  fullName?: string;
  storeId?: string;
  departmentId?: string;
  status?: UserStatus;
}

/**
 * 用户查询参数
 */
export interface UserQueryParams {
  page: number;
  size: number;
  keyword?: string;
  status?: string;
  storeId?: string;
  departmentId?: string;
  roleId?: string;
}

/**
 * 角色信息
 */
export interface Role {
  id: number;
  name: string;
  code: string;
  description: string;
  dataScope: DataScopeType;
  isSystem: boolean;
  status: number;
  permissionCount: number;
  userCount: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 角色创建DTO
 */
export interface RoleCreateDTO {
  name: string;
  code: string;
  description: string;
  dataScope: DataScopeType;
  permissionIds: number[];
}

/**
 * 角色更新DTO
 */
export interface RoleUpdateDTO {
  name?: string;
  description?: string;
  dataScope?: DataScopeType;
  status?: number;
}

/**
 * 角色查询参数
 */
export interface RoleQueryParams {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
}

/**
 * 权限信息
 */
export interface Permission {
  id: number;
  resource: string;
  action: string;
  name: string;
  description: string;
  parentId: number | null;
  sortOrder: number;
  status: number;
  children?: Permission[];
}

/**
 * 权限树节
 */
export interface PermissionTreeNode {
  id: number;
  label: string;
  permissionName: string;
  permissionCode: string;
  permissionType?: number;
  module?: string;
  resource: string;
  action: string;
  checked?: boolean;
  children?: PermissionTreeNode[];
}

/**
 * 数据权限配置
 */
export interface DataScopeConfig {
  scope: DataScopeType;
  storeIds?: string[];
  departmentIds?: string[];
}

/**
 * 分页响应
 */
export interface PageResponse<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/**
 * 权限完整性检查结
 */
export interface PermissionIntegrityResult {
  missing: string[];
  unused: string[];
  conflicts: string[];
}

/**
 * 权限同步结果
 */
export interface PermissionSyncResult {
  added: number;
  updated: number;
  deleted: number;
  unchanged: number;
  addedPermissions: string[];
  updatedPermissions: string[];
  deletedPermissions: string[];
}

/**
 * 权限模板DTO
 */
export interface PermissionTemplateDTO {
  id: number;
  name: string;
  code: string;
  description: string;
  enterpriseType: string;
  scaleRange: string;
  roleConfig: {
    roles: RoleTemplateItem[];
  };
}

/**
 * 角色模板
 */
export interface RoleTemplateItem {
  name: string;
  code: string;
  dataScope: DataScopeType;
  permissions: string[];
}

/**
 * 角色自定义DTO
 */
export interface RoleCustomDTO {
  name: string;
  code: string;
  description?: string;
  dataScope: DataScopeType;
  permissions: string[];
  storeIds?: string[];
  departmentIds?: string[];
}

/**
 * 企业信息DTO
 */
export interface EnterpriseInfoDTO {
  name: string;
  type: string;
  scale: string;
}

/**
 * 组织架构DTO
 */
export interface OrganizationDTO {
  stores: StoreDTO[];
  departments: DepartmentDTO[];
}

/**
 * 门店DTO
 */
export interface StoreDTO {
  name: string;
  code: string;
  isHeadquarters?: boolean;
  region?: string;
  address?: string;
}

/**
 * 部门DTO
 */
export interface DepartmentDTO {
  name: string;
  code: string;
  parentId?: string;
  storeId?: string;
}

/**
 * 初始化状态DTO
 */
export interface InitStatusDTO {
  isInitialized: boolean;
  currentStep: string;
  completedSteps: string[];
  enterpriseInfo?: EnterpriseInfoDTO;
  organization?: OrganizationDTO;
  roleConfig?: {
    useTemplate: boolean;
    templateId: number | null;
    customRoles: RoleCustomDTO[];
  };
}

/**
 * 门店信息
 */
export interface Store {
  id: string;
  name: string;
  code: string;
  region: string;
  address: string;
  status: string;
}

/**
 * 部门信息
 */
export interface Department {
  id: string;
  name: string;
  code: string;
  parentId: string | null;
  storeId: string;
  status: string;
}

/**
 * 系统初始化状
 */
export interface SystemInitStatus {
  step: string;
  isCompleted: boolean;
  completedAt: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 企业信息
 */
export interface EnterpriseInfo {
  enterpriseName: string;
  enterpriseType: string;
  scale: string;
  logoUrl?: string;
  contactEmail?: string;
  contactPhone?: string;
  address?: string;
}

/**
 * 组织架构信息
 */
export interface OrganizationInfo {
  stores: StoreInfo[];
  departments: DepartmentInfo[];
}

/**
 * 门店信息（初始化用）
 */
export interface StoreInfo {
  storeCode: string;
  storeName: string;
  region?: string;
  address?: string;
  contactPerson?: string;
  contactPhone?: string;
  isHeadquarters?: boolean;
}

/**
 * 部门信息（初始化用）
 */
export interface DepartmentInfo {
  deptCode: string;
  deptName: string;
  parentId?: number;
  level?: number;
  sortOrder?: number;
  description?: string;
}

/**
 * 权限模板
 */
export interface PermissionTemplate {
  id: number;
  templateName: string;
  templateCode: string;
  description: string;
  enterpriseType: string;
  scaleRange: string;
  roleConfig: RoleConfig;
  isSystem: boolean;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 角色配置
 */
export interface RoleConfig {
  roles: RoleConfigItem[];
}

/**
 * 角色配置
 */
export interface RoleConfigItem {
  name: string;
  code: string;
  dataScope: DataScopeType;
  permissions: string[];
  storeIds?: string[];
  departmentIds?: string[];
}

/**
 * 数据范围标签映射
 */
export const DATA_SCOPE_LABELS: Record<DataScopeType, string> = {
  all: '全部数据',
  company: '本公司数',
  region: '本区域数',
  store: '本门店数',
  department: '本部门数',
  stores: '指定门店',
  departments: '指定部门',
  self: '仅本人数据'
};

/**
 * 用户状态标签映
 */
export const USER_STATUS_LABELS: Record<UserStatus, string> = {
  active: '正常',
  inactive: '停用',
  locked: '锁定'
};

// ========== 菜单配置类型 ==========

/**
 * 门店规模档位
 * 与权限中心模板（PermissionTemplate）的 templateCode 一一对应
 * 用于控制子菜单在不同规模企业的可见性
 * - mini: 单人小店（1-2人，夫妻店/单人作坊）
 * - standard: 集中式单店（3-20人，单店小型餐饮）
 * - chain-standard: 标准连锁（3-10店，每店15-50人）
 * - chain-enterprise: 大型连锁（5+店，每店50-200人）
 */
export type ScaleLevel = 'mini' | 'standard' | 'chain-standard' | 'chain-enterprise'

/** 子菜单项 */
export interface SubMenuItem {
  /** 显示名称 */
  title: string
  /** Element Plus 图标组件名（如 'Odometer', 'Goods'） */
  icon: string
  /** 路由路径 */
  path: string
  /** 权限码（可选，细粒度控制，如 'product:food:create'） */
  permission?: string
  /** 是否隐藏（用户级覆盖用） */
  hidden?: boolean
  /**
   * 规模档位标记（可选）
   * - 未设置 → 全规模可见
   * - 有值 → 仅在列出的规模档位下可见
   * 与权限中心模板联动，渲染层根据 currentTemplate 推导当前规模自动过滤
   */
  scaleLevel?: ScaleLevel[]
}

/** 一级菜单组配置 */
export interface MenuGroupConfig {
  /** 唯一标识，如 'workspace', 'product', 'order' */
  id: string
  /** 显示名称，如 '产品中心' */
  title: string
  /** Element Plus 图标组件名 */
  icon: string
  /** 默认路由（点击一级菜单时跳转，可选） */
  path?: string
  /**
   * 可见角色列表。
   * - 未设置 或 空数组 → **所有角色可见**
   * - 有值 → 仅拥有任一角色的用户可见
   * - 超级管理员(OWNER/ADMIN)始终忽略此字段，看到全部
   */
  visibleRoles?: UserRole[]
  /** 排序权重（数字越小越靠前） */
  order: number
  /** 子菜单列表 */
  children: SubMenuItem[]
  /**
   * 业务分类标签（用于权限中心的分组展示和模板配置）
   * - operation: 运营类（运营中心、门店管理）
   * - management: 管理类（产品、订单、采购、仓储、会员）
   * - finance: 财务类（财务中心、资产管理）
   * - hr: 人事类（人事管理）
   * - system: 系统类（系统管理、设备管理）
   * - traceability: 溯源类（食品追溯）
   */
  category?: 'operation' | 'management' | 'finance' | 'hr' | 'system' | 'traceability'
  /** 是否默认折叠子菜单 */
  collapsed?: boolean
  /** 角标文字（如 'NEW'） */
  badge?: string
  /** 是否隐藏（运行时域矩阵覆盖用，防御性检查） */
  hidden?: boolean
}

/** 菜单覆盖规则（角色级或用户级） */
export interface MenuOverride {
  /** 要操作的菜单组ID */
  groupId: string
  /** 操作类型 */
  action: 'hide' | 'show' | 'replace' | 'reorder'
  /** replace时的替换内容（部分字段覆盖） */
  replacement?: Partial<MenuGroupConfig>
  /** reorder时的新排序值 */
  newOrder?: number
  /** 目标用户ID（仅用户级覆盖需要，为空则表示全局/角色级覆盖） */
  targetUserId?: string
}

// ========== 用户权限覆盖类型 ==========

/** 覆盖类型：ADD-添加权限 / REMOVE-移除权限 */
export type OverrideType = 'ADD' | 'REMOVE'

/** 覆盖状态：ACTIVE-生效中 / PENDING-待审批 / EXPIRED-已过期 / REVOKED-已撤销 / REJECTED-已拒绝 */
export type OverrideStatus = 'ACTIVE' | 'PENDING' | 'EXPIRED' | 'REVOKED' | 'REJECTED'

/** 预设菜单模板配置 */
export interface MenuTemplateConfig {
  templateId: string       // 如 'large-chain', 'single-store', 'hq-only'
  templateName: string     // '大型连锁模式' / '标准连锁模式' / '集中式单店模式' / '自定义'
  description: string      // 模板描述
  /** 此模板的菜单覆盖规则列表 */
  overrides: MenuOverride[]
}
