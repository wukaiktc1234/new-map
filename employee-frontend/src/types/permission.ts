/**
 * 层级化权限体系类型定义
 * @module types/permission
 *
 * 定义用户层级枚举、层级信息接口和层级配置映射表。
 * 用于员工端审批链路、数据可见性等场景的权限控制。
 */

/** 用户层级枚举（数字编码，与后端一致） */
export enum UserLevel {
  STAFF = 1,            // 普通员工
  SUPERVISOR = 2,       // 主管/领班
  MANAGER = 3,          // 店长
  REGION_MANAGER = 4,   // 区域经理
  ADMIN = 5,            // 超级管理员（总部/品牌方）
}

/** 层级详细信息 */
export interface LevelInfo {
  /** 层级枚举值 */
  level: UserLevel
  /** 层级代码名（如 supervisor） */
  levelName: string
  /** 中文显示名 */
  label: string
  /** 是否拥有审批权限 */
  canApprove: boolean
  /** 可以审批哪些层级的申请（仅比自己低的层级） */
  canApproveLevels: UserLevel[]
  /** 最大审批金额（报销类，单位：分；undefined 表示无限制） */
  maxApprovalAmount?: number
}

/** 审批流程节点角色定义 */
export interface FlowNodeRole {
  /** 节点层级 */
  level: UserLevel
  /** 角色显示名称（如 "L2主管"） */
  roleName: string
  /** 是否为当前用户的审批节点 */
  isCurrentUserNode?: boolean
}

/** 审批链路规则配置 */
export interface ApprovalChainRule {
  /** 审批类型（如 leave, reimbursement 等） */
  type: string
  /** 发起人最低层级要求 */
  minApplicantLevel: UserLevel
  /** 审批节点链路（按顺序排列） */
  chain: FlowNodeRole[]
}

/**
 * 层级配置映射表
 * 每个层级的完整权限和能力定义
 */
export const LEVEL_CONFIG_MAP: Record<UserLevel, LevelInfo> = {
  [UserLevel.STAFF]: {
    level: UserLevel.STAFF,
    levelName: 'staff',
    label: '员工',
    canApprove: false,
    canApproveLevels: [],
  },
  [UserLevel.SUPERVISOR]: {
    level: UserLevel.SUPERVISOR,
    levelName: 'supervisor',
    label: '主管',
    canApprove: true,
    canApproveLevels: [UserLevel.STAFF],
    maxApprovalAmount: 5000, // 500元（分为单位）
  },
  [UserLevel.MANAGER]: {
    level: UserLevel.MANAGER,
    levelName: 'manager',
    label: '店长',
    canApprove: true,
    canApproveLevels: [UserLevel.STAFF, UserLevel.SUPERVISOR],
    maxApprovalAmount: 20000, // 2000元
  },
  [UserLevel.REGION_MANAGER]: {
    level: UserLevel.REGION_MANAGER,
    levelName: 'region_manager',
    label: '区域经理',
    canApprove: true,
    canApproveLevels: [UserLevel.STAFF, UserLevel.SUPERVISOR, UserLevel.MANAGER],
    maxApprovalAmount: 100000, // 10000元
  },
  [UserLevel.ADMIN]: {
    level: UserLevel.ADMIN,
    levelName: 'admin',
    label: '超级管理员',
    canApprove: true,
    canApproveLevels: [UserLevel.STAFF, UserLevel.SUPERVISOR, UserLevel.MANAGER, UserLevel.REGION_MANAGER],
    // 超级管理员无金额限制
  },
}

/**
 * 根据层级值获取层级信息
 * @param level - 层级枚举值
 * @returns 层级信息对象
 */
export function getLevelInfo(level: UserLevel): LevelInfo {
  return LEVEL_CONFIG_MAP[level]
}

/**
 * 根据层级值获取中文显示名
 * @param level - 层级枚举值
 * @returns 中文标签
 */
export function getLevelLabel(level: UserLevel): string {
  return LEVEL_CONFIG_MAP[level]?.label || `L${level}`
}

/**
 * 判断目标层级是否在可审批范围内
 * @param currentLevel - 当前用户层级
 * @param targetLevel - 目标申请人的层级
 * @returns 是否可以审批
 */
export function canApproveLevel(currentLevel: UserLevel, targetLevel: UserLevel): boolean {
  const config = LEVEL_CONFIG_MAP[currentLevel]
  if (!config?.canApprove) return false
  return config.canApproveLevels.includes(targetLevel)
}
