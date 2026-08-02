/**
 * 系统配置管理模块 - TypeScript 类型定义
 * 对应后端 SysConfig 模块15 REST API
 */

/** 配置值类型枚
 */
export enum ConfigValueType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  LONG = 'LONG',
  DOUBLE = 'DOUBLE',
  BOOLEAN = 'BOOLEAN',
  JSON = 'JSON',
  TEXT = 'TEXT'
}

/** 配置状
 */
export enum ConfigStatus {
  ENABLED = 'active',
  DISABLED = 'inactive'
}

/** 敏感标记
 */
export enum SensitiveFlag {
  NORMAL = 0,
  SENSITIVE = 1
}

/** 变更类型
 */
export enum ConfigChangeType {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  RESET = 'RESET',
  ENABLE = 'ENABLE',
  DISABLE = 'DISABLE'
}

/** 系统配置
 */
export interface SysConfigItem {
  configId: number;
  configKey: string;
  configName: string;
  configGroup: string;
  configCategory: string;
  valueType: ConfigValueType;
  configValue: string;
  defaultValue: string;
  isSensitive: SensitiveFlag;
  isEncrypted: number;
  isEnabled: number;
  isReadonly: number;
  sortOrder: number;
  description: string;
  validationRules: string;
  options: string;
  createUserId: string;
  createUsername: string;
  updateUserId: string;
  updateUsername: string;
  version: number;
  createdAt: string;
  updatedAt: string;
}

/** 分组配置键值对映射
 */
export interface GroupConfigMap {
  [configKey: string]: string;
}

/** 分页查询参数
 */
export interface SysConfigQueryParams {
  current?: number;
  size?: number;
  configKey?: string;
  configName?: string;
  configGroup?: string;
  isEnabled?: number;
  isSensitive?: number;
}

/** 分页响应
 */
export interface PageResponse<T> {
  records: T[];
  total: number;
  current: number;
  size: number;
  pages: number;
}

/** 批量更新请求
 */
export interface BatchUpdateItem {
  configKey: string;
  value: string;
}

/** 批量更新结果
 */
export interface BatchUpdateResult {
  updatedCount: number;
  failures: Array<{ configKey: string
  reason: string }>
}

/** 配置变更历史记录
 */
export interface ConfigHistoryRecord {
  historyId: number;
  configId: number;
  configKey: string;
  oldValue: string;
  newValue: string;
  changeType: ConfigChangeType;
  changeReason: string;
  operatorId: string;
  operatorName: string;
  createdAt: string;
}

/** 历史查询参数
 */
export interface HistoryQueryParams {
  current?: number;
  size?: number;
  configId?: number;
  operatorId?: string;
  changeType?: string;
  startTime?: string;
  endTime?: string;
}

/** 系统计信息
 */
export interface SysConfigStats {
  totalConfigs: number;
  enabledConfigs: number;
  disabledConfigs: number;
  sensitiveConfigs: number;
  readonlyConfigs: number;
  editableConfigs: number;
  groups: string[];
  totalHistoryRecords: number;
}

/** 配置分组信息
 */
export interface ConfigGroupInfo {
  groupName: string;
  count: number;
  description: string;
}
