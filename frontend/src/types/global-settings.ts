/**
 * 全局设置模块 - 完整类型定义体系
 *
 * 【设计目标】提供统一的设置数据契约，支持：
 * - 设置分类（auth/security/permission/system大类）
 * - 设置分组（一个分类下可有多个组）
 * - 设置项（单个配置项，包含类型、验证、敏感度等信息）
 * - 敏感度分级（public/internal/sensitive/restricted）
 * - 变更追踪（dirty标记、默认值对比）
 *
 * 【使用场景】
 * - Settings Store 的数据结构
 * - useSettings Composable 的输入输出类型
 * - 策略设置页面的表单绑定
 * - 后端API的请求响应类型
 */


/**
 * 设置分类枚举
 * 用于将系统设置按业务领域划分
 */
export type SettingCategory =
  | 'auth'          // 认证相关：密码策略、登录限制、MFA、会话管理
  | 'security'      // 安全相关：IP白名单、审计日志、安全头
  | 'permission'    // 权限相关：数据权限、功能权限
  | 'system'        // 系统参数：通用配置、性能参数
  | 'business'      // 业务参数：行业特定配置
  | 'integration'   // 第三方集成：外部服务对接
  | 'notification'  // 通知参数：邮件、短信、站内信
  | 'ui';           // 界面个性化：主题、布局、显示偏好
/**
 * 设置项值类
 */
export type SettingValueType =
  | 'string'        // 文本字符串
  | 'number'        // 数值（整数或小数）
  | 'boolean'       // 布尔开关
  | 'select'        // 单选下拉（从预定义选项中选择）
  | 'toggle'        // 切换开关（类似boolean但UI不同）
  | 'ip-list'       // IP地址列表
  | 'regex'         // 正则表达式
  | 'json'          // JSON对象或数组
  | 'text-area';    // 多行文本
/**
 * 敏感度级别
 * 决定该设置的可见性和修改权限
 */
export type SensitivityLevel = 'public' | 'internal' | 'sensitive' | 'restricted';


/**
 * 单个设置项
 * 系统中最小的配置单元
 */
export interface SettingItem {
  /** 唯一标识，格式 '{category}.{group}.{item-name}' */
  key: string;

  /** 所属分类 */
  category: SettingCategory;

  /** 所属分组 */
  group: string;

  /** 显示名称 */
  label: string;

  /** 详细说明 */
  description: string;

  /** 值类型 */
  type: SettingValueType;

  /** 默认值 */
  defaultValue: unknown;

  /** 当前值（运行时） */
  currentValue: unknown;

  /**
   * 选项列表（仅select类型使用）
   * 例如：[{ label: '允许', value: 'allow' }, { label: '拒绝', value: 'deny' }]
   */
  options?: Array<{ label: string; value: unknown }>;

  /** 验证规则 */
  validation?: SettingValidation;

  /** 敏感度级别 */
  sensitivity: SensitivityLevel;

  /** 修改后是否需要重启服务才能生效 */
  requiresRestart?: boolean;

  /** 是否启用（控制该设置项是否可用） */
  isEnabled?: boolean;

  /** 排序权重（同组内排序） */
  sortOrder?: number;
}

/**
 * 验证规则
 */
export interface SettingValidation {
  /** 是否必填 */
  required?: boolean;

  /** 最小值（number类型）或最小长度（string类型） */
  min?: number;

  /** 最大值（number类型）或最大长度（string类型） */
  max?: number;

  /** 正则表达式模式 */
  pattern?: string;

  /** 正则匹配失败时的提示信息 */
  patternMessage?: string;

  /** 自定义验证函数（前端使用） */
  validator?: (value: unknown) => boolean | string;
}

/**
 * 设置分组
 * 一个页面可能包含多个组，每组是一个逻辑单元
 */
export interface SettingGroup {
  /** 组唯一标识 */
  id: string;

  /** 组显示名称 */
  label: string;

  /** 图标名称（Element Plus图标） */
  icon: string;

  /** 组说明 */
  description: string;

  /** 该组包含的所有设置项
 */
  items: SettingItem[];

  /** 是否折叠（UI状态） */
  collapsed?: boolean;
}

/**
 * 设置分类定义
 * 一个大的业务领域，包含多个分组
 */
export interface SettingCategoryDef {
  /** 分类键名 */
  key: SettingCategory;

  /** 分类显示名称 */
  label: string;

  /** 图标名称 */
  icon: string;

  /** 显示顺序 */
  order: number;

  /** 该分类下的所有分组 */
  groups: SettingGroup[];
}


/**
 * 设置变更记录
 * 用于追踪用户修改了哪些设置
 */
export interface SettingChange {
  /** 设置项key */
  key: string;

  /** 修改前的值 */
  oldValue: unknown;

  /** 修改后的值 */
  newValue: unknown;

  /** 修改时间 */
  changedAt: Date;

  /** 是否已保存到后端 */
  isSaved: boolean;
}

/**
 * 批量保存请求
 */
export interface BatchSaveRequest {
  /** 分类 */
  category: SettingCategory;

  /** 需要保存的键值对 */
  changes: Array<{
    key: string;
    value: unknown;
  }>;
}

/**
 * 批量保存结果
 */
export interface BatchSaveResult {
  /** 成功保存的数量 */
  successCount: number;

  /** 失败的数量 */
  failureCount: number;

  /** 失败详情 */
  failures: Array<{
    key: string;
    reason: string;
  }>;
}

/**
 * 设置加载状态
 */
export interface SettingsLoadState {
  /** 是否正在加载 */
  loading: boolean;

  /** 最后加载时间 */
  lastLoadedAt: Date | null;

  /** 加载错误信息 */
  error: string | null;
}


/**
 * 所有支持的设置分类列表
 * 用于遍历和校验
 */
export const SETTING_CATEGORIES: Array<{
  key: SettingCategory;
  label: string;
  icon: string;
}> = [
{ key: 'auth',
label: '认证策略',
icon: 'Lock' },
{ key: 'security',
label: '安全策略',
icon: 'Shield' },
{ key: 'permission',
label: '权限粒度',
icon: 'Key' },
{ key: 'system',
label: '系统参数',
icon: 'Monitor' },
{ key: 'business',
label: '业务参数',
icon: 'Briefcase' },
{ key: 'integration',
label: '第三方集成',
icon: 'Connection' },
{ key: 'notification',
label: '通知参数',
icon: 'Bell' },
{ key: 'ui',
label: '界面个性化',
icon: 'Brush' }
];

/**
 * 敏感度级别配置
 */
export const SENSITIVITY_CONFIG: Record<SensitivityLevel, {
  label: string;
  color: string;        // Element Plus标签颜色
  icon: string;         // 图标
  description: string;
}> = {
  public: {
    label: '公开',
    color: 'success',
    icon: 'View',
    description: '所有用户可以查看',
  },
  internal: {
    label: '内部',
    color: 'info',
    icon: 'OfficeBuilding',
    description: '仅内部员工可以查看',
  },
  sensitive: {
    label: '敏感',
    color: 'warning',
    icon: 'Warning',
    description: '需管理员权限查询',
  },
  restricted: {
    label: '受限',
    color: 'danger',
    icon: 'Lock',
    description: '仅超级管理员可访问',
  }
};


/**
 * 从SettingCategory提取对应的SettingCategoryDef
 */
export type CategoryDefMap = Record<SettingCategory, SettingCategoryDef>;

/**
 * 设置值的联合类型
 */
export type SettingValue = string | number | boolean | string[] | Record<string, unknown>;

/**
 * 设置项的Pick类型（用于表单绑定）
 */
export type SettingFormItem = Pick<SettingItem,
  'key' | 'label' | 'description' | 'type' | 'currentValue'
  | 'defaultValue' | 'options' | 'validation' | 'sensitivity'
  | 'requiresRestart' | 'isEnabled' | 'sortOrder'
>;
