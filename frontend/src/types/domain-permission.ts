/**
 * 域权限矩阵相关类型定义
 *
 * 对接后端 /v1/permission-templates (PermissionTemplateController)
 * 4 种系统模板：centralized-single / standard-chain / large-chain / custom
 */

/**
 * 域访问级别
 * - FULL: 完全访问（可查看、编辑、删除）
 * - READ_ONLY: 只读访问（仅查看）
 * - LIMITED: 受限访问（仅部分资源，如本门店数据）
 * - HIDDEN: 隐藏（菜单和路由均不显示）
 */
export type DomainAccessLevel = 'FULL' | 'READ_ONLY' | 'LIMITED' | 'HIDDEN'

/**
 * 业务域定义
 */
export interface BusinessDomain {
  /** 域代码，如 'workspace' / 'store-ops' */
  domainCode: string
  /** 域中文名，如 '工作台' / '门店运营' */
  domainName: string
  /** Element Plus 图标组件名，如 'Odometer' */
  icon?: string
}

/**
 * 访问级别元数据
 */
export interface DomainAccessLevelMeta {
  /** 中文标签 */
  label: string
  /** 描述说明 */
  description: string
}

/** 访问级别 → 元数据 映射 */
export type DomainAccessLevelMetaMap = Record<DomainAccessLevel, DomainAccessLevelMeta>

/**
 * 角色×域 完整权限矩阵
 * 结构: { [roleCode]: { [domainCode]: DomainAccessLevel } }
 */
export type RoleDomainMatrix = Record<string, Record<string, DomainAccessLevel>>

/**
 * 角色行（用于矩阵表格渲染）
 */
export interface RoleRow {
  /** 角色代码 */
  code: string
  /** 角色显示名 */
  name: string
  /** 可见域列表（非 HIDDEN 的域） */
  domains: string[]
}

/**
 * 菜单预览项
 */
export interface MenuPreviewItem {
  /** 菜单标题 */
  title: string
  /** 图标组件名 */
  icon: string
  /** 路由路径 */
  path: string
  /** 所属域代码 */
  domainCode: string
  /** 是否可见 */
  visible: boolean
  /** 访问级别 */
  accessLevel: DomainAccessLevel
}

/**
 * 角色概览统计（用于摘要栏）
 */
export interface RoleOverviewStat {
  /** 角色代码 */
  code: string
  /** 角色名 */
  name: string
  /** 完全访问域数 */
  full: number
  /** 只读域数 */
  readOnly: number
  /** 受限域数 */
  limited: number
  /** 隐藏域数 */
  hidden: number
}

/**
 * 对比差异项
 */
export interface DomainCompareDiff {
  /** 域代码 */
  domainCode: string
  /** 域名 */
  domainName: string
  /** 当前角色的访问级别 */
  current: DomainAccessLevel
  /** 对比角色的访问级别 */
  compare: DomainAccessLevel
}

/**
 * 自定义模板本地存储格式
 * 保留完整访问级别（FULL/READ_ONLY/LIMITED），避免重载时权限静默升级
 */
export interface CustomTemplateStorage {
  templateCode: 'custom'
  templateName: string
  description: string
  /** 角色→可见域列表（兼容标准模板格式，用于后端同步） */
  domainMatrix: Record<string, string[]>
  /** 角色→域→完整级别（扩展字段，防止 READ_ONLY/LIMITED 丢失） */
  domainMatrixLevels: Record<string, Record<string, string>>
  /** 保存时间 ISO 字符串 */
  savedAt: string
}
