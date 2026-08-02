/**
 * 统一类型定义管理文件
 * 用于集中管理项目中所有的类型定义，确保类型一致性和避免重复定义
 */


// finance-report 仅 re-export ReportType / ReportStatus（FinanceReportData 在本文件下方本地定义）
export type { ReportType, ReportStatus } from './finance-report';

// 导入API相关类型（PageResponse 等分页/上传类型实际定义在 ./index 中，故从 ./index 引入）
export type {
  ApiResponse
} from './api';

// 导入通用类型
export type {
  BaseResponse,
  FileInfo,
  NotificationInfo,
  OperationLog,
  LoginLog,
  UserInfo,
  RoleInfo,
  PermissionInfo,
  MenuInfo,
  DeptInfo,
  DictInfo,
  DictItem,
  ConfigInfo,
  TableColumn,
  FormConfig,
  TreeNode,
  CascaderOption,
  SelectOption,
  RouteMeta,
  BreadcrumbItem,
  TagView,
  ThemeConfig,
  LayoutConfig,
  SystemStatus,
  PageResponse,
  PageParams,
  QueryParams,
  SortParams,
  UploadResponse
} from './index';

// 导入枚举类型
export {
  Status,
  DeleteStatus,
  Gender as GenderEnum,
  UserStatus,
  OperationType,
  OrderStatus,
  OrderType,
  Breakpoint
} from './index';

// 导入订单相关类型
export type {
  OrderDish,
  BaseOrder,
  DineInOrder,
  TakeawayOrder,
  OrderStatistics,
  OrderTrend,
  OrderQueryParams
} from './index';

/**

 */
export enum CommonStatus {
  ACTIVE = 'active',
  INACTIVE = 'inactive',
  PENDING = 'pending',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled'
}

/**
 * 通用性别枚举
 */
export enum CommonGender {
  MALE = 'male',
  FEMALE = 'female',
  OTHER = 'other'
}

/**
 * 通用表单字段类型
 */
export interface FormField {
  key: string;
  label: string;
  type: 'input' | 'select' | 'radio' | 'checkbox' | 'date' | 'textarea';
  required?: boolean;
  placeholder?: string;
  options?: Array<{ label: string; value: string | number }>;
  disabled?: boolean;
  readonly?: boolean;
}

/**
 * 通用表单验证规则
 */
export interface FormRule {
  required?: boolean;
  message?: string;
  trigger?: 'blur' | 'change';
  min?: number;
  max?: number;
  pattern?: RegExp;
  validator?: (rule: FormRule, value: unknown, callback: (error?: Error) => void) => void;
  /** 字段类型（用于 async-validator，如 'email'、'number'、'date'） */
  type?: string;
}

/**
 * 通用表单验证规则集合
 */
export interface FormRules {
  [key: string]: FormRule | FormRule[];
}

/**
 * 通用数据转换接口
 */
export interface DataConverter<TForm, TApi> {
  formToApi(formData: TForm): TApi;
  apiToForm(apiData: TApi): TForm;
}

/**

 */
export interface TableColumnConfig {
  prop: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  align?: 'left' | 'center' | 'right';
  sortable?: boolean | 'custom';
  fixed?: boolean | 'left' | 'right';
  showOverflowTooltip?: boolean;
  formatter?: (row: Record<string, unknown>, column: TableColumnConfig, cellValue: unknown, index: number) => string;
  renderHeader?: (h: (tag: string, props: Record<string, unknown>, children?: unknown[]) => unknown, { column, $index }: { column: TableColumnConfig; $index: number }) => unknown;
  children?: TableColumnConfig[];
}

/**
 * 通用搜索配置
 */
export interface SearchConfig {
  label: string;
  prop: string;
  type: 'input' | 'select' | 'date' | 'daterange';
  placeholder?: string;
  options?: Array<{ label: string; value: string | number }>;
  span?: number;
  defaultValue?: string | number;
}

/**
 * 通用操作按钮配置
 */
export interface ActionButton {
  label: string;
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info';
  icon?: string;
  disabled?: boolean;
  loading?: boolean;
  permission?: string;
  onClick: (row?: Record<string, unknown>) => void;
}

/**

 */
export interface TreeNodeData {
  id: string | number;
  label: string;
  children?: TreeNodeData[];
  parentId?: string | number;
  disabled?: boolean;
  isLeaf?: boolean;
  [key: string]: unknown;
}

/**
 * 通用级联选择器选项
 */
export interface CascaderData {
  value: string | number;
  label: string;
  children?: CascaderData[];
  disabled?: boolean;
  leaf?: boolean;
  [key: string]: unknown;
}

/**
 * 通用选择器选项
 */
export interface OptionData {
  label: string;
  value: string | number | boolean;
  disabled?: boolean;
  [key: string]: unknown;
}

/**
 * 通用日期范围
 */
export interface DateRange {
  startDate: string;
  endDate: string;
}

/**

 */
export interface PaginationMeta {
  current: number;
  pageSize: number;
  total: number;
  totalPages: number;
  hasNext: boolean;
  hasPrev: boolean;
}

/**
 * 通用API错误响应
 */
export interface ApiError {
  code: number;
  message: string;
  details?: unknown;
  timestamp: number;
}

/**
 * ECharts 相关类型定义
 */

/**
 * ECharts 图表类型
 */
export interface EChartType {
  label: string;
  value: string;
}

/**

 */
export interface EChartDataItem {
  name: string;
  value: number;
  itemStyle?: {
    color?: string;
    [key: string]: unknown;
  };
  children?: EChartDataItem[];
  [key: string]: unknown;
}

/**

 */
export interface EChartOption {
  title?: {
    text?: string;
    subtext?: string;
    left?: string | number;
    top?: string | number;
    [key: string]: unknown;
  };
  tooltip?: {
    trigger?: 'axis' | 'item' | 'none';
    axisPointer?: {
      type?: 'shadow' | 'line' | 'cross';
      [key: string]: unknown;
    };
    formatter?: string | ((params: unknown) => string);
    [key: string]: unknown;
  };
  legend?: {
    orient?: 'vertical' | 'horizontal';
    left?: string | number;
    top?: string | number;
    bottom?: string | number;
    right?: string | number;
    data?: string[];
    textStyle?: {
      color?: string;
      [key: string]: unknown;
    };
    [key: string]: unknown;
  };
  grid?: {
    left?: string | number;
    right?: string | number;
    bottom?: string | number;
    top?: string | number;
    containLabel?: boolean;
    [key: string]: unknown;
  };
  xAxis?: {
    type?: 'category' | 'value' | 'time' | 'log';
    data?: string[];
    name?: string;
    axisLabel?: {
      rotate?: number;
      color?: string;
      formatter?: string | ((value: unknown) => string);
      [key: string]: unknown;
    };
    axisLine?: {
      lineStyle?: {
        color?: string;
        [key: string]: unknown;
      };
      [key: string]: unknown;
    };
    [key: string]: unknown;
  };
  yAxis?: {
    type?: 'category' | 'value' | 'time' | 'log';
    name?: string;
    axisLabel?: {
      formatter?: string | ((value: unknown) => string);
      color?: string;
      [key: string]: unknown;
    };
    axisLine?: {
      lineStyle?: {
        color?: string;
        [key: string]: unknown;
      };
      [key: string]: unknown;
    };
    splitLine?: {
      lineStyle?: {
        color?: string;
        type?: 'solid' | 'dashed' | 'dotted';
        [key: string]: unknown;
      };
      [key: string]: unknown;
    };
    [key: string]: unknown;
  };
  series?: Array<{
    name?: string;
    type?: string;
    data?: unknown[];
    radius?: string | string[];
    center?: string[];
    itemStyle?: {
      color?: string;
      borderRadius?: number;
      borderColor?: string;
      borderWidth?: number;
      [key: string]: unknown;
    };
    label?: {
      show?: boolean;
      position?: string;
      formatter?: string | ((params: unknown) => string);
      color?: string;
      [key: string]: unknown;
    };
    labelLine?: {
      show?: boolean;
      [key: string]: unknown;
    };
    emphasis?: {
      label?: {
        show?: boolean;
        fontSize?: number;
        fontWeight?: string;
        formatter?: string | ((params: unknown) => string);
        [key: string]: unknown;
      };
      itemStyle?: {
        color?: string;
        [key: string]: unknown;
      };
      [key: string]: unknown;
    };
    lineStyle?: {
      width?: number;
      color?: string;
      [key: string]: unknown;
    };
    areaStyle?: {
      color?: string;
      [key: string]: unknown;
    };
    smooth?: boolean;
    [key: string]: unknown;
  }>;
  color?: string[];
  backgroundColor?: string;
  [key: string]: unknown;
}

/**

 */
export interface FinanceReportDataItem {
  itemName: string;
  amount: number;
  category?: string;
  isTotal?: boolean;
  isSubTotal?: boolean;
  taxType?: string;
  taxAmount?: number;
  budgetItem?: string;
  budgetAmount?: number;
  actualAmount?: number;
  item?: string;
  [key: string]: unknown;
}

/**
 * 财务报表数据
 */
export interface FinanceReportData {
  reportType: string;
  details?: FinanceReportDataItem[];
  [key: string]: unknown;
}

/**
 * 通用文件上传配置
 */
export interface UploadConfig {
  action: string;
  headers?: Record<string, string>;
  data?: Record<string, unknown>;
  name?: string;
  accept?: string;
  multiple?: boolean;
  limit?: number;
  size?: number;
  autoUpload?: boolean;
  showFileList?: boolean;
  drag?: boolean;
}

/**
 * 通用导出配置
 */
export interface ExportConfig {
  filename?: string;
  type?: string;
  fields?: Array<{
    key: string;
    title: string;
    width?: number;
  }>;
  data?: unknown[];
}

/**
 * 通用导入配置
 */
export interface ImportConfig {
  accept?: string;
  multiple?: boolean;
  maxSize?: number;
  beforeUpload?: (file: File) => boolean;
  onSuccess?: (response: unknown, file: File) => void;
  onError?: (error: Error, file: File) => void;
}

/**
 * 通用图表配置
 */
export interface ChartConfig {
  type: 'line' | 'bar' | 'pie' | 'scatter' | 'radar' | 'map';
  title?: string;
  subtitle?: string;
  xAxis?: {
    type: 'category' | 'value' | 'time' | 'log';
    data?: unknown[];
    name?: string;
  };
  yAxis?: {
    type: 'category' | 'value' | 'time' | 'log';
    name?: string;
  };
  series?: Array<{
    name?: string;
    type?: string;
    data?: unknown[];
    color?: string;
  }>;
  legend?: {
    data?: string[];
    position?: 'top' | 'bottom' | 'left' | 'right';
  };
  tooltip?: {
    trigger?: 'axis' | 'item';
    formatter?: string | ((params: unknown) => string);
  };
}

/**
 * 通用主题配置
 */
export interface CommonThemeConfig {
  primaryColor: string;
  successColor: string;
  warningColor: string;
  dangerColor: string;
  infoColor: string;
  backgroundColor: string;
  textColor: string;
  borderColor: string;
  shadowColor: string;
}

/**
 * 通用布局配置
 */
export interface CommonLayoutConfig {
  sidebarCollapsed: boolean;
  sidebarWidth: number;
  headerHeight: number;
  footerHeight: number;
  contentPadding: number;
  showBreadcrumb: boolean;
  showTabs: boolean;
  showFooter: boolean;
  fixedHeader: boolean;
  fixedSidebar: boolean;
}

/**
 * 通用路由配置
 */
export interface RouteConfig {
  path: string;
  name?: string;
  component?: () => Promise<unknown>;
  redirect?: string;
  meta?: {
    title?: string;
    icon?: string;
    hidden?: boolean;
    roles?: string[];
    permissions?: string[];
    keepAlive?: boolean;
    affix?: boolean;
    breadcrumb?: boolean;
    activeMenu?: string;
  };
  children?: RouteConfig[];
}

/**
 * 通用菜单配置
 */
export interface MenuConfig {
  id: string | number;
  name: string;
  path: string;
  icon?: string;
  component?: string;
  redirect?: string;
  hidden?: boolean;
  roles?: string[];
  permissions?: string[];
  keepAlive?: boolean;
  affix?: boolean;
  breadcrumb?: boolean;
  activeMenu?: string;
  children?: MenuConfig[];
  parentId?: string | number;
  sort: number;
  type: 'menu' | 'button';
}

/**
 * 通用权限配置
 */
export interface PermissionConfig {
  id: string | number;
  name: string;
  code: string;
  description?: string;
  type: 'menu' | 'button' | 'api';
  parentId?: string | number;
  path?: string;
  method?: string;
  sort: number;
  status: number;
  children?: PermissionConfig[];
}

/**
 * 通用户配置
 */
export interface UserConfig {
  id: string | number;
  username: string;
  name: string;
  email?: string;
  phone?: string;
  avatar?: string;
  status: number;
  roles: string[];
  permissions: string[];
  createdAt: string;
  updatedAt: string;
  bio?: string;
  position?: string;
  department?: string;
  departmentId?: string;
  storeId?: string;
  storeName?: string;
  employeeId?: string;
  gender?: string;
  birthday?: string;
  hometown?: string;
  joinDate?: string;
}

/**
 * 通用角色配置
 */
export interface RoleConfig {
  id: string | number;
  name: string;
  code: string;
  description?: string;
  status: number;
  permissions: string[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 通用部门配置
 */
export interface DeptConfig {
  id: string | number;
  name: string;
  code: string;
  parentId?: string | number;
  sort: number;
  status: number;
  leader?: string;
  phone?: string;
  email?: string;
  children?: DeptConfig[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 通用字典配置
 */
export interface DictConfig {
  id: string | number;
  name: string;
  code: string;
  description?: string;
  status: number;
  items: DictItemConfig[];
  createdAt: string;
  updatedAt: string;
}

/**

 */
export interface DictItemConfig {
  id: string | number;
  label: string;
  value: string | number;
  sort: number;
  status: number;
  cssClass?: string;
  listClass?: string;
  isDefault?: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * 通用系统配置
 */
export interface SystemConfig {
  id: string | number;
  name: string;
  code: string;
  value: string;
  description?: string;
  type: 'string' | 'number' | 'boolean' | 'json';
  options?: string[];
  isSystem: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * 通用操作日志
 */
export interface OperationLogConfig {
  id: string | number;
  userId: string | number;
  username: string;
  module: string;
  operation: string;
  method: string;
  params: string;
  result: string;
  ip: string;
  userAgent: string;
  duration: number;
  status: number;
  error?: string;
  createdAt: string;
}

/**
 * 通用登录日志
 */
export interface LoginLogConfig {
  id: string | number;
  userId: string | number;
  username: string;
  ip: string;
  userAgent: string;
  loginType: 'password' | 'sms' | 'email' | 'social';
  status: number;
  message?: string;
  createdAt: string;
}

/**
 * 通用文件信息
 */
export interface FileConfig {
  id: string | number;
  name: string;
  originalName: string;
  url: string;
  path: string;
  size: number;
  type: string;
  extension: string;
  mimeType: string;
  md5: string;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 通用通知消息
 */
export interface NotificationConfig {
  id: string | number;
  title: string;
  content: string;
  type: 'info' | 'success' | 'warning' | 'error';
  isRead: boolean;
  senderId?: string | number;
  senderName?: string;
  receiverId: string | number;
  createdAt: string;
  updatedAt: string;
}

/**

 */
export interface SystemStatusConfig {
  cpu: {
    usage: number;
    cores: number;
    model: string;
  };
  memory: {
    total: number;
    used: number;
    free: number;
    usage: number;
  };
  disk: {
    total: number;
    used: number;
    free: number;
    usage: number;
  };
  network: {
    upload: number;
    download: number;
  };
  uptime: number;
  timestamp: number;
}
