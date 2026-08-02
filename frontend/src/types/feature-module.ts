/**
 * 功能模块类型定义
 * @module types/feature-module
 *
 * 【设计目标 * 定义可扩展功能框架的核心类型，支持：
 * - 模块注册与发 * - 启用/禁用控制
 * - 依赖关系管理
 * - 路由与权限关 *
 * 【使用场景 * 1. 系统内置模块（core/business * 2. 未来插件扩展模块（extension * 3. 模块管理页面数据结构
 */

/**
 * 模块分类
 * - core: 核心模块（不可禁用，如认证、权限）
 * - business: 业务模块（可按需启用/禁用 * - extension: 扩展模块（通过插件机制动态加载载）
 */
export type ModuleCategory = 'core' | 'business' | 'extension';

/**
 * 模块状态类 * - enabled: 已启 * - disabled: 已禁 * - locked: 已锁定（核心模块不可操作
 */
export type ModuleStatus = 'enabled' | 'disabled' | 'locked';

/**
 * 模块设置项接 * 用于存储模块的用户自定义配置
 */
export interface ModuleSettings {
  /** 设置项键值对
 */
  [key: string]: string | number | boolean | null;
}

/**
 * 功能模块权限定义
 */
export interface ModulePermission {
  /** 权限标识（如 'hr:manage'*/
  code: string;
  /** 权限名称
 */
  name: string;
  /** 权限描述
 */
  description?: string;
}

/**
 * 功能模块路由配置
 * 简化版路由定义，用于模块描
 */
export interface ModuleRouteConfig {
  /** 路由路径
 */
  path: string;
  /** 路由名称
 */
  name: string;
  /** 页面标题
 */
  title: string;
  /** 图标名称
 */
  icon?: string;
  /** 是否为目录（有子路由
 */
  isDirectory?: boolean;
  /** 子路由列
 */
  children?: ModuleRouteConfig[];
}

/**
 * 功能模块完整定义接口
 * 这是整个可扩展功能框架的核心数据结构
 */
export interface FeatureModule {
  /** 唯一标识符（'hr', 'finance', 'trace'*/
  id: string;

  /** 显示名称（如 '人事管理'*/
  name: string;

  /** 图标名称（Element Plus icon 组件名） */
  icon: string;

  /** 功能描述（用于模块管理页面展示） */
  description: string;

  /** 模块分类
 */
  category: ModuleCategory;

  /** 模块版本号（语义化版本，'1.0.0'*/
  version: string;

  /** 依赖的其他模块ID列表（依赖模块必须先启用
 */
  dependencies: string[];

  /** 该模块注册的路由配置
 */
  routes: ModuleRouteConfig[];

  /** 该模块需要的权限列表
 */
  permissions: ModulePermission[];

  /** 模块的可配置项（用户可在模块管理中调整） */
  settings?: ModuleSettings;

  /** 是否为核心锁定模块（不可禁用
 */
  locked?: boolean;

  /** 模块标签（用于分类筛选，'HR', '财务', '运营'*/
  tags?: string[];
}

/**
 * 模块注册表类 * Map 结构支持快速查
 */
export type FeatureRegistry = Map<string, FeatureModule>;

/**
 * 模块状态快 * 用于持久化存储用户的模块启用/禁用配置
 */
export interface ModuleStatusSnapshot {
  /** 模块ID
 */
  moduleId: string;
  /** 当前状
 */
  status: ModuleStatus;
  /** 最后更新时
 */
  updatedAt: string;
}

/**
 * 模块依赖关系图节 * 用于可视化展示和循环依赖检
 */
export interface DependencyNode {
  /** 模块ID
 */
  id: string;
  /** 模块名称
 */
  name: string;
  /** 依赖的模块ID列表
 */
  dependsOn: string[];
  /** 被哪些模块依
 */
  requiredBy: string[];
}

/**
 * 模块操作结果
 */
export interface ModuleOperationResult {
  /** 操作是否成功
 */
  success: boolean;
  /** 操作的模块ID
 */
  moduleId: string;
  /** 操作类型
 */
  operation: 'enable' | 'disable';
  /** 错误信息（失败时
 */
  error?: string;
  /** 受影响的相关模块列表
 */
  affectedModules?: string[];
}
