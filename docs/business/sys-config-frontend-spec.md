# 系统配置管理前端页面 - 验收标准与开发规范

> **文档版本**: v1.0
> **适用模块**: SysConfig 前端配置管理
> **最后更新**: 2026-04-04

---

## 一、文件清单与职责说明

### 1.1 需要新建的文件 (7个)

| 序号 | 文件路径 | 职责说明 | 预估行数 | Element Plus组件 |
|------|----------|----------|----------|------------------|
| 1 | `frontend/src/types/sys-config.ts` | SysConfig全部TypeScript类型定义 | ~120行 | 无 |
| 2 | `frontend/src/api/sys-config.ts` | 对接15个REST API的封装层 | ~280行 | 无 |
| 3 | `frontend/src/stores/sys-config.ts` | Pinia状态管理(缓存/批量操作) | ~200行 | 无 |
| 4 | `frontend/src/views/SystemSettings/SystemConfig/StorageNotification.vue` | 存储与通知设置页(新建) | ~350行 | el-card, el-form, el-input, el-input-number, el-switch, el-select, el-alert |
| 5 | `frontend/src/views/SystemSettings/SystemConfig/ConfigManagement.vue` | 配置管理中心页(新建) | ~550行 | el-table, el-dialog, el-form, el-pagination, el-select, el-input, el-button, el-tag, el-popconfirm |
| 6 | `frontend/src/views/SystemSettings/SystemConfig/ConfigHistory.vue` | 变更历史页(新建) | ~400行 | el-table, el-timeline, el-pagination, el-select, el-date-picker, el-tag |
| 7 | `frontend/src/views/SystemSettings/SystemConfig/CacheManagement.vue` | 缓存管理页(新建) | ~320行 | el-card, el-statistic, el-button, el-popconfirm, el-descriptions, el-alert |

### 1.2 需要重写的文件 (3个)

| 序号 | 文件路径 | 重写原因 | 预估行数 | Element Plus组件 |
|------|----------|----------|----------|------------------|
| 8 | `frontend/src/api/config.ts` | 旧API调用不存在的 `/v1/config/*` 接口，需替换为 `sys-config.ts` 或删除 | 删除 | - |
| 9 | `frontend/src/views/SystemSettings/SystemConfig/BasicSettings.vue` | 从硬编码表单改为动态渲染system+general分组配置项 | ~380行 | el-card, el-form, el-input, el-input-number, el-switch, el-select, el-textarea |
| 10 | `frontend/src/views/SystemSettings/SystemConfig/SecuritySettings.vue` | 从硬编码表单改为动态渲染security分组，增加敏感字段处理 | ~320行 | el-card, el-form, el-input, el-input-number, el-switch, el-alert |

### 1.3 需要修改的文件 (2个)

| 序号 | 文件路径 | 修改内容 |
|------|----------|----------|
| 11 | `frontend/src/router/modules/system.ts` | 在SystemConfig children中新增3条路由(StorageNotification, ConfigManagement, ConfigHistory, CacheManagement) |
| 12 | `frontend/src/views/SystemSettings/Index.vue` | 在侧边栏el-menu中新增"存储与通知"、"配置中心"、"变更历史"、"缓存管理"菜单项 |

### 1.4 文件依赖关系图

```
types/sys-config.ts (类型定义)
       |
       v
api/sys-config.ts (API层) <-- 使用 request.ts 实例
       |
       +---> stores/sys-config.ts (可选状态管理层)
       |
       v
views/SystemSettings/SystemConfig/
       |-- BasicSettings.vue (重写)
       |-- SecuritySettings.vue (重写)
       |-- StorageNotification.vue (新建)
       |-- ConfigManagement.vue (新建)
       |-- ConfigHistory.vue (新建)
       |-- CacheManagement.vue (新建)
```

---

## 二、类型定义规范 (`types/sys-config.ts`)

### 2.1 完整类型定义

```typescript
/**
 * 系统配置管理 - TypeScript类型定义
 * @module types/sys-config
 */

// ==================== 枚举定义 ====================

/** 配置值类型枚举 */
export enum ConfigValueType {
  STRING = 'STRING',
  INTEGER = 'INTEGER',
  BOOLEAN = 'BOOLEAN',
  TEXT = 'TEXT',
  DECIMAL = 'DECIMAL',
  JSON = 'JSON'
}

/** 配置状态枚举 */
export enum ConfigStatus {
  ENABLED = 'enabled',
  DISABLED = 'disabled'
}

/** 是否敏感标识 */
export enum SensitiveFlag {
  SENSITIVE = true,
  NOT_SENSITIVE = false
}

/** 变更操作类型 */
export enum ConfigChangeType {
  CREATE = 'CREATE',
  UPDATE = 'UPDATE',
  RESET = 'RESET',
  ENABLE = 'ENABLE',
  DISABLE = 'DISABLE'
}

// ==================== 核心实体类型 ====================

/** 系统配置项实体 */
export interface SysConfigItem {
  /** 配置ID */
  configId: number;
  /** 配置键名 */
  configKey: string;
  /** 配置值(明文，仅管理员可见) */
  configValue: string;
  /** 配置显示值(脱敏后的值) */
  displayValue: string;
  /** 配置名称 */
  configName: string;
  /** 配置分组 */
  configGroup: string;
  /** 值类型 */
  valueType: ConfigValueType;
  /** 是否敏感 */
  isSensitive: boolean;
  /** 状态 */
  status: ConfigStatus;
  /** 默认值 */
  defaultValue: string;
  /** 配置描述 */
  description: string;
  /** 排序号 */
  sortOrder: number;
  /** 创建时间 */
  createTime: string;
  /** 更新时间 */
  updateTime: string;
  /** 更新人 */
  updatedBy: string;
}

/** 分组键值对 (用于按分组获取配置) */
export interface GroupConfigItem {
  configKey: string;
  configValue: string;
  configName: string;
  valueType: ConfigValueType;
  isSensitive: boolean;
  description: string;
  defaultValue: string;
}

/** 分组配置映射 */
export type GroupConfigMap = Record<string, GroupConfigItem>;

// ==================== 请求/响应类型 ====================

/** 分页查询参数 */
export interface SysConfigQueryParams {
  /** 当前页码 */
  current?: number;
  /** 每页大小 */
  size?: number;
  /** 配置键名(模糊搜索) */
  configKey?: string;
  /** 配置名称(模糊搜索) */
  configName?: string;
  /** 配置分组 */
  configGroup?: string;
  /** 状态筛选 */
  status?: ConfigStatus | '';
  /** 值类型筛选 */
  valueType?: ConfigValueType | '';
}

/** 批量修改请求体 */
export interface BatchUpdateRequest {
  items: Array<{
    configKey: string;
    configValue: string;
  }>;
}

/** 单值修改请求体 */
export interface ValueUpdateRequest {
  configValue: string;
}

/** 变更历史记录 */
export interface ConfigHistoryRecord {
  /** 历史记录ID */
  historyId: number;
  /** 配置键名 */
  configKey: string;
  /** 配置名称 */
  configName: string;
  /** 操作类型 */
  changeType: ConfigChangeType;
  /** 旧值(脱敏) */
  oldValue: string;
  /** 新值(脱敏) */
  newValue: string;
  /** 操作人 */
  operator: string;
  /** 操作人ID */
  operatorId: number;
  /** 操作时间 */
  operateTime: string;
  /** 备注信息 */
  remark: string;
  /** IP地址 */
  ipAddress: string;
}

/** 变更历史查询参数 */
export interface HistoryQueryParams {
  current?: number;
  size?: number;
  /** 配置键名筛选 */
  configKey?: string;
  /** 操作类型筛选 */
  changeType?: ConfigChangeType | '';
  /** 操作人筛选 */
  operator?: string;
  /** 开始时间 */
  startTime?: string;
  /** 结束时间 */
  endTime?: string;
}

/** 系统统计信息 */
export interface SysConfigStats {
  /** 总配置数 */
  totalConfigs: number;
  /** 启用数 */
  enabledCount: number;
  /** 禁用数 */
  disabledCount: number;
  /** 敏感配置数 */
  sensitiveCount: number;
  /** 分组列表及数量 */
  groupStats: Array<{
    groupName: string;
    groupCode: string;
    count: number;
  }>;
  /** 今日变更次数 */
  todayChanges: number;
}

/** 分组信息 */
export interface ConfigGroupInfo {
  groupCode: string;
  groupName: string;
  description: string;
  configCount: number;
}

// ==================== 表单类型 ====================

/** 动态配置编辑表单项 */
export interface ConfigFormItem {
  configKey: string;
  configValue: string | number | boolean;
  configName: string;
  valueType: ConfigValueType;
  isSensitive: boolean;
  description: string;
  defaultValue: string;
  /** 表单验证规则 */
  rules?: Array<{
    required?: boolean;
    message: string;
    trigger: string;
    min?: number;
    max?: number;
    pattern?: string;
  }>;
}
```

---

## 三、API层完整实现 (`api/sys-config.ts`)

```typescript
/**
 * 系统配置管理 API 封装
 * @module api/sys-config
 * @description 对接后端15个REST API，使用request实例发送请求
 */
import request from './request';
import type {
  SysConfigItem,
  GroupConfigMap,
  GroupConfigItem,
  SysConfigQueryParams,
  BatchUpdateRequest,
  ValueUpdateRequest,
  ConfigHistoryRecord,
  HistoryQueryParams,
  SysConfigStats,
  ConfigGroupInfo,
  IPage
} from '@/types/sys-config';
import type { IPage as IPagination } from '@/types/pagination';

/**
 * 系统配置API对象
 */
export const sysConfigApi = {

  // ==================== 读取类API ====================

  /**
   * 获取单个配置值
   * @param configKey - 配置键名
   * @returns 配置值(敏感字段返回******)
   * @权限 登录用户即可
   */
  getConfigValue(configKey: string): Promise<string> {
    return request.get<string>(`/v1/sys-config/value/${configKey}`);
  },

  /**
   * 按分组获取所有配置键值对
   * @param group - 分组标识(system/security/storage/notification/general等)
   * @returns 分组内所有配置的键值对映射
   * @权限 sys:config:query
   */
  getGroupValues(group: string): Promise<GroupConfigMap> {
    return request.get<GroupConfigMap>(`/v1/sys-config/group/${group}/values`);
  },

  /**
   * 获取配置详情(含脱敏处理)
   * @param configKey - 配置键名
   * @returns 完整配置信息
   * @权限 登录用户即可
   */
  getConfigDetail(configKey: string): Promise<SysConfigItem> {
    return request.get<SysConfigItem>(`/v1/sys-config/${configKey}`);
  },

  /**
   * 分页查询配置列表
   * @param params - 查询参数
   * @returns 分页数据
   * @权限 sys:config:query
   */
  getConfigList(params: SysConfigQueryParams): Promise<IPagination<SysConfigItem>> {
    return request.get<IPagination<SysConfigItem>>('/v1/sys-config', params);
  },

  /**
   * 获取所有分组列表
   * @returns 分组信息数组
   * @权限 sys:config:query
   */
  getGroupList(): Promise<ConfigGroupInfo[]> {
    return request.get<ConfigGroupInfo[]>('/v1/sys-config/groups');
  },

  // ==================== 修改类API ====================

  /**
   * 修改单个配置值
   * @param configKey - 配置键名
   * @param data - 新值
   * @returns void
   * @权限 sys:config:edit
   */
  updateConfigValue(configKey: string, data: ValueUpdateRequest): Promise<void> {
    return request.put<void>(`/v1/sys-config/value/${configKey}`, data);
  },

  /**
   * 批量修改配置值(最多50条)
   * @param data - 批量修改请求体
   * @returns void
   * @权限 sys:config:edit
   */
  batchUpdateConfig(data: BatchUpdateRequest): Promise<void> {
    return request.put<void>('/v1/sys-config/batch', data);
  },

  // ==================== 重置类API ====================

  /**
   * 重置单个配置为默认值
   * @param configKey - 配置键名
   * @returns void
   * @权限 sys:config:edit
   */
  resetConfigToDefault(configKey: string): Promise<void> {
    return request.post<void>(`/v1/sys-config/${configKey}/reset`);
  },

  /**
   * 重置整个分组的配置为默认值
   * @param group - 分组标识
   * @returns void
   * @权限 ROLE_ADMIN
   */
  resetGroupToDefault(group: string): Promise<void> {
    return request.post<void>(`/v1/sys-config/group/${group}/reset`);
  },

  // ==================== 启停类API ====================

  /**
   * 启用配置项
   * @param configKey - 配置键名
   * @returns void
   * @权限 sys:config:edit
   */
  enableConfig(configKey: string): Promise<void> {
    return request.post<void>(`/v1/sys-config/${configKey}/enable`);
  },

  /**
   * 禁用配置项
   * @param configKey - 配置键名
   * @returns void
   * @权限 sys:config:edit
   */
  disableConfig(configKey: string): Promise<void> {
    return request.post<void>(`/v1/sys-config/${configKey}/disable`);
  },

  // ==================== 历史类API ====================

  /**
   * 查询配置变更历史(分页)
   * @param params - 查询参数
   * @returns 分页历史记录
   * @权限 sys:config:query
   */
  getChangeHistory(params: HistoryQueryParams): Promise<IPagination<ConfigHistoryRecord>> {
    return request.get<IPagination<ConfigHistoryRecord>>('/v1/sys-config/history', params);
  },

  // ==================== 缓存类API ====================

  /**
   * 清除单个配置的缓存
   * @param configKey - 配置键名
   * @returns void
   * @权限 ADMIN
   */
  clearConfigCache(configKey: string): Promise<void> {
    return request.delete(`/v1/sys-config/cache/${configKey}`);
  },

  /**
   * 清除整个分组的缓存
   * @param group - 分组标识
   * @returns void
   * @权限 ADMIN
   */
  clearGroupCache(group: string): Promise<void> {
    return request.delete(`/v1/sys-config/cache/group/${group}`);
  },

  /**
   * 清除所有配置缓存
   * @returns void
   * @权限 ADMIN
   */
  clearAllCache(): Promise<void> {
    return request.delete('/v1/sys-config/cache/all');
  },

  // ==================== 统计类API ====================

  /**
   * 获取系统配置统计信息
   * @returns 统计数据
   * @权限 sys:config:query
   */
  getConfigStats(): Promise<SysConfigStats> {
    return request.get<SysConfigStats>('/v1/sys-config/stats');
  }
};
```

---

## 四、页面组件详细规格

### Page 1: 基础设置页 (BasicSettings.vue) -- 重写

**路由**: `/system/config/basic`
**对接分组**: `system` + `general`
**预估行数**: ~380行

#### 4.1.1 页面结构

```
BasicSettings.vue
├── 页面标题区 (可选，由父级Index.vue提供)
├── el-card "系统基本信息"
│   └── el-form (动态渲染 system 分组配置项)
│       ├── system.name → el-input (STRING)
│       ├── system.version → el-input (STRING, disabled只读)
│       └── system.logo_url → 图片上传或 el-input (STRING)
├── el-card "通用设置"
│   └── el-form (动态渲染 general 分组配置项)
│       ├── date_format → el-select (STRING, 预设选项)
│       └── page_size → el-input-number (INTEGER)
└── 操作按钮区
    ├── [重置为默认] 按钮
    └── [保存配置] 按钮 (type="primary")
```

#### 4.1.2 数据流

```typescript
// 生命周期
onMounted() → loadGroupConfigs('system') + loadGroupConfigs('general')
                    ↓
            将API返回的 GroupConfigMap 转换为 ConfigFormItem[]
            按 sortOrder 排序后动态渲染
                    ↓
用户修改 → 收集脏数据 → buildBatchUpdatePayload()
                    ↓
        调用 sysConfigApi.batchUpdateConfig()
```

#### 4.1.3 value_type 控件映射规则

| valueType | 渲染控件 | 特殊处理 |
|-----------|----------|----------|
| STRING | `el-input` | maxlength=255 |
| INTEGER | `el-input-number` :min / :max 根据 description 解析 |
| BOOLEAN | `el-switch` | active-value="true" inactive-value="false" |
| TEXT | `el-textarea` :rows=3 maxlength=1000 |
| DECIMAL | `el-input-number` :precision=2 |

#### 4.1.4 关键交互要求

- [ ] 进入页面自动加载两个分组的配置，显示 `v-loading`
- [ ] 修改任一配置项后，"保存配置"按钮高亮提示有未保存更改
- [ ] 点击"保存"前弹出 `ElMessageBox.confirm` 确认对话框
- [ ] 保存成功后 `ElMessage.success` 提示，失败则 `ElMessage.error`
- [ ] 点击"重置为默认"需二次确认，确认后调用 `resetGroupToDefault` API
- [ ] version字段设置为 `disabled` 只读展示
- [ ] 敏感字段在基础设置页不出现(已归入security/notification分组)

---

### Page 2: 安全设置页 (SecuritySettings.vue) -- 重写

**路由**: `/system/config/security`
**对接分组**: `security`
**预估行数**: ~320行

#### 4.2.1 页面结构

```
SecuritySettings.vue
├── el-card "密码策略"
│   └── el-form (security分组)
│       ├── password.min_length → el-input-number (INTEGER, min=6 max=32)
│       ├── password.max_length → el-input-number (INTEGER, min=8 max=128)
│       ├── login.max_attempts → el-input-number (INTEGER, min=1 max=20)
│       └── login.lock_duration_minutes → el-input-number (INTEGER, min=1 max=1440)
└── 操作按钮区
    ├── [重置安全策略] 按钮
    └── [保存配置] 按钮
```

#### 4.2.2 安全相关特殊处理

- [ ] 所有数值字段必须有合理的 min/max 约束（从 description 中解析或硬编码安全边界）
- [ ] 密码最小长度不能大于最大长度（前端联动校验）
- [ ] 锁定时长建议提供快捷选项：5分钟/15分钟/30分钟/1小时/永久
- [ ] 保存时对每个字段进行范围校验，超出安全范围的给出警告

---

### Page 3: 存储与通知设置页 (StorageNotification.vue) -- 新建

**路由**: `/system/config/storage-notification` (需在路由文件中添加)
**对接分组**: `storage` + `notification`
**预估行数**: ~350行

#### 4.3.1 页面结构

```
StorageNotification.vue
├── el-card "文件存储配置"
│   └── el-form (storage分组)
│       ├── upload.max_file_size_mb → el-input-number (INTEGER, 单位MB)
│       ├── upload.allowed_extensions → el-select (multiple, 可输入自定义)
│       │   预设选项: jpg/png/gif/pdf/doc/xls/txt
│       └── storage.user_quota_mb → el-input-number (INTEGER, 单位MB)
├── el-card "邮件通知配置"
│   ├── el-alert type="warning" (邮件配置涉及敏感信息，请谨慎操作)
│   └── el-form (notification分组)
│       ├── email.enabled → el-switch
│       └── email.smtp_host → el-input (type="password", show-password, 敏感字段)
│           └── 脱敏提示图标 + 说明文字
└── 操作按钮区
    ├── [重置存储配置] 按钮
    ├── [重置通知配置] 按钮
    └── [保存全部配置] 按钮
```

#### 4.3.2 敏感字段特殊UI

对于 `isSensitive === true` 的字段：

```vue
<!-- 敏感字段模板 -->
<el-form-item :label="item.configName">
  <div class="sensitive-field-wrapper">
    <el-input
      v-model="item.configValue"
      :type="showSensitiveValue[item.configKey] ? 'text' : 'password'"
      show-password
      placeholder="******"
    />
    <el-tooltip content="此为敏感信息，修改请谨慎" placement="top">
      <el-icon class="sensitive-icon"><WarningFilled /></el-icon>
    </el-tooltip>
  </div>
  <div class="sensitive-tip">当前值为脱敏显示，点击眼睛图标可查看/编辑</div>
</el-form-item>
```

#### 4.3.3 关键交互

- [ ] `email.smtp_host` 默认以密码形式显示，需要手动切换可见
- [ ] 修改敏感字段值时，保存前弹窗二次确认："您正在修改敏感配置项 xxx，确认继续？"
- [ ] 文件扩展名支持多选+自定义输入（使用 `el-select` 的 `allow-create` `filterable`）
- [ ] 文件大小和存储配额显示单位(MB/GB)，前端做格式化

---

### Page 4: 配置管理中心 (ConfigManagement.vue) -- 新建

**路由**: `/system/config/management`
**功能**: 全量配置列表 CRUD + 高级操作
**预估行数**: ~550行

#### 4.4.1 页面布局

```
ConfigManagement.vue
├── 搜索筛选区 (el-form inline)
│   ├── 关键词搜索 (configKey + configName 联合模糊)
│   ├── 分组筛选 (el-select, 全部分组选项)
│   ├── 状态筛选 (el-select: 全部/启用/禁用)
│   ├── 类型筛选 (el-select: 全部/STRING/INTEGER/BOOLEAN/TEXT)
│   └── [查询] [重置] 按钮
├── 操作栏
│   ├── [新增配置] 按钮 (预留，后端可能不支持前端创建)
│   ├── [批量启用] 按钮 (需选中项)
│   ├── [批量禁用] 按钮 (需选中项)
│   └── [批量重置] 按钮 (需选中项)
├── el-table (全量配置列表)
│   ├── selection列 (checkbox多选)
│   ├── configKey列 (等宽字体, 可复制)
│   ├── configName列
│   ├── configValue列 (敏感显示******, 超长截断tooltip)
│   ├── configGroup列 (el-tag着色)
│   ├── valueType列 (el-tag type区分)
│   ├── status列 (el-switch可切换)
│   ├── isSensitive列 (el-icon警示)
│   └── 操作列
│       ├── [编辑] 图标按钮 → 弹出编辑对话框
│       ├── [重置] 图标按钮 → popconfirm确认
│       └── [查看详情] 图标按钮 → 弹出详情drawer
├── el-pagination (分页)
├── 编辑对话框 (el-dialog)
│   └── el-form (根据valueType动态渲染控件)
└── 详情抽屉 (el-drawer, 可选)
    └── el-descriptions (完整配置信息)
```

#### 4.4.2 表格列定义

```typescript
const tableColumns = [
  { prop: 'configKey', label: '配置键', width: 200, fixed: 'left' },
  { prop: 'configName', label: '配置名称', width: 160 },
  { prop: 'configValue', label: '配置值', minWidth: 180, showOverflowTooltip: true },
  { prop: 'configGroup', label: '分组', width: 120, slot: 'groupTag' },
  { prop: 'valueType', label: '值类型', width: 100, slot: 'typeTag' },
  { prop: 'status', label: '状态', width: 80, slot: 'statusSwitch' },
  { prop: 'isSensitive', label: '敏感', width: 70, slot: 'sensitiveIcon' },
  { prop: 'updateTime', label: '更新时间', width: 170 },
  { prop: 'actions', label: '操作', width: 200, fixed: 'right', slot: 'actions' }
];
```

#### 4.4.3 编辑对话框规格

- **触发方式**: 点击表格行的[编辑]按钮
- **对话框标题**: `编辑配置 - ${configKey}`
- **表单内容**:
  - configKey: `el-input` (disabled, 只读)
  - configName: `el-input` (disabled, 只读)
  - configValue: 根据 `valueType` 动态渲染对应控件
  - 若 `isSensitive`: 显示为 password input + 警告提示
- **底部按钮**: [取消] [保存]
- **保存逻辑**: 调用 `updateConfigValue(key, { configValue })`

#### 4.4.4 批量操作规格

| 操作 | API调用 | 前置条件 | 确认方式 |
|------|---------|----------|----------|
| 批量启用 | 循环调用 `enableConfig` | 已选中的禁用项 | `ElMessageBox.confirm` |
| 批量禁用 | 循环调用 `disableConfig` | 已选中的启用项 | `ElMessageBox.confirm` |
| 批量重置 | 循环调用 `resetConfigToDefault` | 已选中任意项 | `ElMessageBox.confirm` (警告级别) |

---

### Page 5: 变更历史页 (ConfigHistory.vue) -- 新建

**路由**: `/system/config/history`
**对接API**: `/v1/sys-config/history`
**预估行数**: ~400行

#### 4.5.1 页面结构

```
ConfigHistory.vue
├── 筛选区 (el-form inline)
│   ├── 配置键名 (el-input, 模糊搜索)
│   ├── 操作类型 (el-select: CREATE/UPDATE/RESET/ENABLE/DISABLE)
│   ├── 操作人 (el-input)
│   ├── 时间范围 (el-date-picker, daterange)
│   └── [查询] [重置] [导出] 按钮
├── 展示切换 (el-radio-group: 表格视图 / 时间线视图)
│
├── [表格视图] el-table
│   ├── configKey列
│   ├── configName列
│   ├── changeType列 (el-tag, 不同颜色)
│   ├── oldValue列 (脱敏, tooltip)
│   ├── newValue列 (脱敏, tooltip)
│   ├── operator列
│   ├── operateTime列
│   └── ipAddress列
│
├── [时间线视图] el-timeline (可选增强)
│   ├── timeline-item 按日期分组
│   │   ├── 时间戳
│   │   ├── 操作人 + 操作类型标签
│   │   ├── 配置键名
│   │   └── 变更详情(old → new)
│
└── el-pagination
```

#### 4.5.2 变更类型标签配色

```typescript
const changeTypeTagMap: Record<ConfigChangeType, { type: string; label: string }> = {
  [ConfigChangeType.CREATE]: { type: 'success', label: '新增' },
  [ConfigChangeType.UPDATE]: { type: 'primary', label: '修改' },
  [ConfigChangeType.RESET]: { type: 'warning', label: '重置' },
  [ConfigChangeType.ENABLE]: { type: 'success', label: '启用' },
  [ConfigChangeType.DISABLE]: { type: 'danger', label: '禁用' }
};
```

#### 4.5.3 关键交互

- [ ] 默认加载最近7天的变更记录
- [ ] 时间线视图按日期降序排列，同日内按时间倒序
- [ ] old/new value超长文本截断，hover tooltip显示完整内容
- [ ] 敏感配置的值始终显示为 `******`
- [ ] 支持按配置键名快速定位（点击配置key跳转到配置管理中心）

---

### Page 6: 缓存管理页 (CacheManagement.vue) -- 新建

**路由**: `/system/config/cache`
**对接API**: `/stats` + 3个cache清除接口
**预估行数**: ~320行

#### 4.6.1 页面结构

```
CacheManagement.vue
├── 统计概览区 (el-row :gutter=20)
│   ├── el-col :span=6 → el-card → el-statistic title="总配置数"
│   ├── el-col :span=6 → el-card → el-statistic title="已启用"
│   ├── el-col :span=6 → el-card → el-statistic title="已禁用"
│   ├── el-col :span=6 → el-card → el-statistic title="敏感配置"
│   └── el-col :span=6 → el-card → el-statistic title="今日变更"
├── 分组统计区 (el-card)
│   └── el-table 或 自定义列表
│       ├── 分组名称 | 配置数 | 操作(清除该组缓存)
│       └── 行内 [清除缓存] 按钮
├── 缓存操作区 (el-card)
│   ├── el-alert type="info" (清除缓存将导致下次访问时重新加载数据)
│   ├── [清除指定Key缓存]
│   │   └── el-input (输入configKey) + [清除] 按钮
│   ├── [清除分组缓存]
│   │   └── el-select (选择分组) + [清除] 按钮
│   └── [清除全部缓存]
│       └── el-button type="danger" (需ADMIN角色)
└── 操作日志区 (可选)
    └── 最近5次缓存清除操作的简要记录
```

#### 4.6.2 权限控制

- **查看统计**: `sys:config:query` 权限
- **单key/分组缓存清除**: `ROLE_ADMIN` 角色
- **全部缓存清除**: `ROLE_ADMIN` 角色 + 二次确认
- 无权限时隐藏对应操作按钮或置灰并提示

#### 4.6.3 缓存清除确认规范

```typescript
// 清除全部缓存的确认
const handleClearAllCache = async () => {
  try {
    await ElMessageBox.confirm(
      '确定要清除所有配置缓存吗？此操作将导致系统临时性能下降。',
      '危险操作确认',
      {
        confirmButtonText: '确定清除',
        cancelButtonText: '取消',
        type: 'warning',
        draggable: true
      }
    );
    await sysConfigApi.clearAllCache();
    ElMessage.success('缓存清除成功');
    await loadStats(); // 刷新统计
  } catch (error: unknown) {
    // 用户取消不做处理
    if (error !== 'cancel' && error instanceof Error) {
      ElMessage.error(error.message || '缓存清除失败');
    }
  }
};
```

---

## 五、路由与导航更新规范

### 5.1 路由配置 (`router/modules/system.ts`)

在现有的 `SystemConfig.children` 数组中追加以下路由：

```typescript
// 在现有3个子路由(basic/security/log)之后追加:
{
  path: '/system/config/storage-notification',
  name: 'StorageNotification',
  component: () => import('@/views/SystemSettings/SystemConfig/StorageNotification.vue'),
  meta: {
    title: '存储与通知',
    icon: 'folder-opened',
    permissions: ['sys:config:query', 'sys:config:edit'],
    menuType: 1,
    category: 'system'
  }
},
{
  path: '/system/config/management',
  name: 'ConfigManagement',
  component: () => import('@/views/SystemSettings/SystemConfig/ConfigManagement.vue'),
  meta: {
    title: '配置中心',
    icon: 'list',
    permissions: ['sys:config:query'],
    menuType: 1,
    category: 'system'
  }
},
{
  path: '/system/config/history',
  name: 'ConfigHistory',
  component: () => import('@/views/SystemSettings/SystemConfig/ConfigHistory.vue'),
  meta: {
    title: '变更历史',
    icon: 'timer',
    permissions: ['sys:config:query'],
    menuType: 1,
    category: 'system'
  }
},
{
  path: '/system/config/cache',
  name: 'CacheManagement',
  component: () => import('@/views/SystemSettings/SystemConfig/CacheManagement.vue'),
  meta: {
    title: '缓存管理',
    icon: 'refresh',
    permissions: ['role:admin'],  // 仅管理员可见
    menuType: 1,
    category: 'system'
  }
}
```

### 5.2 侧边栏菜单 (`views/SystemSettings/Index.vue`)

在现有的 `<el-sub-menu index="config">` 内追加菜单项：

```vue
<!-- 现有菜单项之后追加 -->
<el-menu-item index="/system/config/storage-notification">
  <el-icon><FolderOpened /></el-icon>
  <span>存储与通知</span>
</el-menu-item>
<el-menu-item index="/system/config/management">
  <el-icon><List /></el-icon>
  <span>配置中心</span>
</el-menu-item>
<el-menu-item index="/system/config/history">
  <el-icon><Timer /></el-icon>
  <span>变更历史</span>
</el-menu-item>
<el-menu-item index="/system/config/cache" v-permission="'role:admin'">
  <el-icon><Refresh /></el-icon>
  <span>缓存管理</span>
</el-menu-item>
```

需要额外引入图标：
```typescript
import { FolderOpened, List, Timer, Refresh } from '@element-plus/icons-vue';
```

---

## 六、交互规范

### 6.1 加载状态规范

| 场景 | 加载方式 | 作用域 |
|------|----------|--------|
| 页面初始加载配置 | `v-loading="loading"` | 整个表单区域 |
| 保存提交中 | `:loading="submitting"` | 保存按钮 |
| 表格数据加载 | `v-loading="tableLoading"` | el-table |
| 对话框内操作 | `v-loading="dialogLoading"` | el-dialog内容区 |
| 统计数据刷新 | `v-loading="statsLoading"` | 统计卡片区域 |

### 6.2 保存确认规范

```typescript
/**
 * 通用保存确认流程
 * @param dirtyCount - 修改的配置项数量
 * @param hasSensitive - 是否包含敏感字段修改
 */
const confirmSave = async (dirtyCount: number, hasSensitive: boolean): Promise<boolean> => {
  const message = hasSensitive
    ? `检测到 ${dirtyCount} 项配置变更，其中包含敏感信息。确认保存？`
    : `检测到 ${dirtyCount} 项配置变更，确认保存？`;

  try {
    await ElMessageBox.confirm(message, '保存确认', {
      confirmButtonText: '保存',
      cancelButtonText: '取消',
      type: hasSensitive ? 'warning' : 'info'
    });
    return true;
  } catch {
    return false;
  }
};
```

### 6.3 敏感字段二次确认规范

```typescript
/**
 * 敏感字段编辑确认
 * @param configKey - 配置键名
 * @param configName - 配置名称
 */
const confirmSensitiveEdit = async (configKey: string, configName: string): Promise<boolean> => {
  try {
    await ElMessageBox.confirm(
      `「${configName}」为敏感配置项，修改后将立即生效且可能影响系统运行。\n确认要修改吗？`,
      '敏感操作警告',
      {
        confirmButtonText: '确认修改',
        cancelButtonText: '取消',
        type: 'warning',
        draggable: true
      }
    );
    return true;
  } catch {
    return false;
  }
};
```

### 6.4 消息反馈规范

| 场景 | 消息类型 | 内容模板 | 时长 |
|------|----------|----------|------|
| 保存成功 | success | `{n}项配置已成功保存` | 2000ms |
| 保存失败 | error | `保存失败：{具体错误信息}` | 3000ms |
| 重置成功 | success | `已重置为默认值` | 2000ms |
| 重置失败 | error | `重置失败：{具体错误信息}` | 3000ms |
| 启用成功 | success | `配置「{name}」已启用` | 2000ms |
| 禁用成功 | warning | `配置「{name}」已禁用` | 2000ms |
| 缓存清除成功 | success | `缓存清除完成` | 2000ms |
| 网络异常 | error | `网络连接异常，请检查网络后重试` | 3000ms |
| 无权限 | warning | `抱歉，您没有执行此操作的权限` | 3000ms |

### 6.5 错误处理规范

```typescript
/**
 * 统一错误处理工具函数
 * @param error - 错误对象
 * @param context - 操作上下文描述
 */
function handleApiError(error: unknown, context: string): void {
  if (error === 'cancel') {
    return; // 用户主动取消
  }

  if (typeof error === 'object' && error !== null && 'message' in error) {
    const err = error as { message: string; status?: number };

    switch (err.status) {
      case 401:
        // Token过期已被request拦截器处理，此处仅做兜底
        break;
      case 403:
        ElMessage.warning('抱歉，您没有执行此操作的权限');
        break;
      case 404:
        ElMessage.error(`请求的资源不存在(${context})`);
        break;
      case 422:
        ElMessage.error(err.message || '数据验证失败，请检查输入');
        break;
      default:
        ElMessage.error(`${context}失败：${err.message || '未知错误'}`);
    }
  } else {
    ElMessage.error(`${context}发生未知异常`);
  }
}
```

---

## 七、验收标准清单

### F-01 ~ F-10 功能性验收

| 编号 | 验收项 | 验收标准 | 优先级 |
|------|--------|----------|--------|
| F-01 | API对接完整性 | 15个后端API全部正确对接，无遗漏、无多余调用 | P0 |
| F-02 | 基础设置页加载 | 进入 `/system/config/basic` 正确加载 system + general 分组的所有配置项，控件类型匹配value_type | P0 |
| F-03 | 安全设置页加载 | 进入 `/system/config/security` 正确加载 security 分组，数值字段有合理约束 | P0 |
| F-04 | 存储通知页加载 | 进入 `/system/config/storage-notification` 正确加载 storage + notification 分组，敏感字段有特殊UI | P0 |
| F-05 | 配置中心列表 | 配置中心页正确展示全量配置列表，支持搜索/筛选/排序/分页 | P0 |
| F-06 | 配置编辑功能 | 点击编辑按钮弹出对话框，修改后正确调用PUT接口保存 | P0 |
| F-07 | 批量操作功能 | 批量启用/禁用/重置功能正常，有确认步骤 | P1 |
| F-08 | 启停切换 | 表格行内switch切换能即时调用enable/disable API | P0 |
| F-09 | 变更历史查询 | 变更历史页正确展示记录，筛选功能正常 | P1 |
| F-10 | 缓存管理功能 | 统计数据正确展示，缓存清除操作正常且有确认 | P1 |

### F-11 ~ F-20 用户体验验收

| 编号 | 验收项 | 验收标准 | 优先级 |
|------|--------|----------|--------|
| F-11 | 加载状态反馈 | 所有异步操作期间均有 `v-loading` 骨架屏/加载动画 | P0 |
| F-12 | 保存确认机制 | 保存前弹出确认对话框，显示变更项数量 | P0 |
| F-13 | 敏感字段保护 | 敏感字段默认脱敏显示，编辑时有二次确认 | P0 |
| F-14 | 操作结果反馈 | 所有操作成功/失败均有ElMessage提示，文案清晰 | P0 |
| F-15 | 表单重置功能 | 重置按钮可恢复到上次保存的状态（非清空） | P1 |
| F-16 | 未保存提示 | 页面有未保存修改时离开应提示确认（可选增强） | P2 |
| F-17 | 响应式适配 | 页面在 1280px/1440px/1920px 宽度下均正常显示 | P1 |
| F-18 | 键盘可操作性 | 表单支持Tab键切换，对话框支持Esc关闭 | P1 |
| F-19 | 空状态处理 | 无数据时显示el-empty空状态占位 | P1 |
| F-20 | 页面过渡动画 | 路由切换有fade过渡效果（已有基础设施） | P2 |

### F-21 ~ F-30 安全性验收

| 编号 | 验收项 | 验收标准 | 优先级 |
|------|--------|----------|--------|
| F-21 | 权限控制 | 各页面/按钮根据meta.permissions正确显隐 | P0 |
| F-22 | 敏感数据脱敏 | 前端不存储/打印敏感值明文，日志中不含password等字段 | P0 |
| F-23 | 输入校验 | 所有表单字段有前端校验规则，防止XSS注入 | P0 |
| F-24 | 危险操作确认 | 缓存清除、批量重置等破坏性操作需二次确认 | P0 |
| F-25 | Admin操作隔离 | 缓存管理页仅ADMIN角色可见和使用 | P0 |
| F-26 | 请求鉴权 | 所有API调用通过request实例自动携带Token | P0 |
| F-27 | 错误信息屏蔽 | 前端错误提示不暴露堆栈/内部细节 | P1 |
| F-28 | 按钮防重复点击 | 提交中按钮disabled防止重复提交 | P1 |
| F-29 | 配置值长度限制 | STRING类型限制255字符，TEXT限制1000字符 | P1 |
| F-30 | 数值范围约束 | INTEGER/DECIMAL类型在前端限制合理范围 | P1 |

### F-31 ~ F-38 性能验收

| 编号 | 验收项 | 验收标准 | 优先级 |
|------|--------|----------|--------|
| F-31 | 首屏加载时间 | 设置页面首次加载 < 800ms (局域网环境) | P1 |
| F-32 | 配置中心分页 | 每页20条，翻页响应 < 300ms | P1 |
| F-33 | 列表搜索延迟 | 搜索输入防抖300ms后再发起请求 | P1 |
| F-34 | 大列表渲染 | 配置中心表格使用虚拟滚动或分页（必须分页） | P1 |
| F-35 | 路由懒加载 | 所有页面组件使用 `() => import()` 异步加载 | P0 |
| F-36 | 无内存泄漏 | 组件卸载时清除定时器/事件监听 | P1 |
| F-37 | API请求去重 | 快速连续点击保存只发一次请求 | P1 |
| F-38 | 打包体积影响 | 新增代码打包后体积增量 < 50KB (gzipped) | P2 |

### F-39 ~ F-50 代码质量验收

| 编号 | 验收项 | 验收标准 | 优先级 |
|------|--------|----------|--------|
| F-39 | TypeScript严格模式 | 无any类型，所有变量/参数/返回值有明确类型 | P0 |
| F-40 | 文件行数限制 | 单文件不超过规定行数(Vue<=1000, TS<=800, SCSS<=600) | P0 |
| F-41 | 组件命名规范 | PascalCase命名，与文件名一致 | P0 |
| F-42 | CSS变量使用 | 使用项目统一的CSS变量/SCSS变量，避免硬编码色值 | P1 |
| F-43 | Scoped样式 | 所有组件样式使用scoped，避免全局污染 | P0 |
| F-44 | 中文注释 | 公共接口/复杂逻辑有中文注释 | P1 |
| F-45 | 无console.log | 生产代码无console.log/console.debug残留 | P0 |
| F-46 | API调用规范 | 使用request实例，禁止直接axios/fetch | P0 |
| F-47 | 响应数据处理 | 不再访问 `.data.data`，直接使用返回值 | P0 |
| F-48 | 代码分割合理性 | 配置中心拆分子组件(表格/对话框/搜索栏)若超过600行 | P1 |
| F-49 | 导入顺序规范 | 外部库→内部组件→工具函数→类型 | P2 |
| F-50 | 构建通过 | `npm run build` 零错误零警告 | P0 |

---

## 八、开发注意事项

### 8.1 旧代码处理

1. **`api/config.ts` 文件**: 开发完成后删除此文件（或保留但确保不被引用）
2. **旧的 BasicSettings/SecuritySettings/LogSettings**: 完全重写，不保留旧逻辑
3. **LogSettings.vue**: 本次不在重写范围内（后端无audit分组），保持原样或后续迭代处理

### 8.2 种子数据参考

开发阶段可用以下测试数据验证UI渲染：

```typescript
// system 分组预期配置项
const systemGroupSeeds = [
  { key: 'system.name', name: '系统名称', type: 'STRING', value: '食品溯源系统', sensitive: false },
  { key: 'system.version', name: '系统版本', type: 'STRING', value: '1.0.0', sensitive: false },
  { key: 'system.logo_url', name: '系统Logo地址', type: 'STRING', value: '', sensitive: false }
];

// security 分组预期配置项
const securityGroupSeeds = [
  { key: 'password.min_length', name: '最小密码长度', type: 'INTEGER', value: '8', sensitive: false },
  { key: 'password.max_length', name: '最大密码长度', type: 'INTEGER', value: '32', sensitive: false },
  { key: 'login.max_attempts', name: '最大登录尝试次数', type: 'INTEGER', value: '5', sensitive: false },
  { key: 'login.lock_duration_minutes', name: '账户锁定时长(分钟)', type: 'INTEGER', value: '30', sensitive: false }
];

// storage 分组预期配置项
const storageGroupSeeds = [
  { key: 'upload.max_file_size_mb', name: '最大文件上传大小(MB)', type: 'INTEGER', value: '10', sensitive: false },
  { key: 'upload.allowed_extensions', name: '允许上传的文件扩展名', type: 'STRING', value: 'jpg,png,gif,pdf,doc,xls,txt', sensitive: false },
  { key: 'storage.user_quota_mb', name: '用户存储配额(MB)', type: 'INTEGER', value: '500', sensitive: false }
];

// notification 分组预期配置项
const notificationGroupSeeds = [
  { key: 'email.enabled', name: '启用邮件通知', type: 'BOOLEAN', value: 'false', sensitive: false },
  { key: 'email.smtp_host', name: 'SMTP服务器地址', type: 'STRING', value: 'smtp.example.com', sensitive: true }
];

// general 分组预期配置项
const generalGroupSeeds = [
  { key: 'date_format', name: '日期格式', type: 'STRING', value: 'YYYY-MM-DD', sensitive: false },
  { key: 'page_size', name: '默认分页大小', type: 'INTEGER', value: '20', sensitive: false }
];
```

### 8.3 与现有模式的兼容性

- 保持与 [RoleManagement.vue](file:///p:/my-new-project/frontend/src/views/SystemSettings/PermissionCenter/RoleManagement.vue) 相同的组件拆分风格
- 保持与 [Index.vue](file:///p:/my-new-project/frontend/src/views/SystemSettings/Index.vue) 侧边栏一致的菜单样式
- 复用 [variables.scss](file:///p:/my-new-project/frontend/src/styles/variables.scss) 中的设计令牌
- 遵循 [request.ts](file:///p:/my-new-project/frontend/src/api/request.ts) 的请求/响应拦截器约定

---

## 九、附录

### A. Element Plus 组件使用汇总

| 组件 | 使用页面 | 用途 |
|------|----------|------|
| el-card | 全部6个页面 | 分组容器 |
| el-form | Pages 1-4 | 配置编辑表单 |
| el-input | Pages 1-4 | 字符串/文本输入 |
| el-input-number | Pages 1-3 | 数值输入 |
| el-switch | Pages 1-4 | 布尔值开关/状态切换 |
| el-select | Pages 1,3,4,5 | 选项选择/筛选 |
| el-textarea | Page 1 | 多行文本 |
| el-table | Pages 4,5,6 | 列表展示 |
| el-pagination | Pages 4,5 | 分页控件 |
| el-dialog | Page 4 | 编辑对话框 |
| el-popconfirm | Pages 4,6 | 危险操作确认 |
| el-message | 全局 | 操作反馈 |
| el-message-box | 全局 | 确认对话框 |
| el-alert | Pages 3,6 | 信息/警告提示 |
| el-tag | Pages 4,5 | 状态/类型标签 |
| el-statistic | Page 6 | 统计数字展示 |
| el-timeline | Page 5 (可选) | 时间线视图 |
| el-tooltip | Pages 3,4 | 悬浮提示 |
| el-drawer | Page 4 (可选) | 详情抽屉 |
| el-descriptions | Page 4,6 | 结构化信息展示 |
| el-radio-group | Page 5 | 视图切换 |

### B. 测试用例建议

建议覆盖以下核心场景：
1. 正常加载各分组配置并正确渲染对应控件
2. 修改配置值后保存成功的完整流程
3. 修改敏感字段的二次确认流程
4. 配置中心的搜索/筛选/分页
5. 行内启停切换的即时生效
6. 批量操作的确认和执行
7. 变更历史的筛选和时间线展示
8. 缓存清除的危险操作确认
9. 无权限时的UI降级处理
10. 网络异常时的友好错误提示
