// 全局类型定义

// 导入新增的核心业务类型（Food/Category 等已在下方 ./food 中导出，此处仅补充 Product 体系的额外类型）
export type {
  FoodCategory,
  CategoryFormData,
  PricingRecord,
  PricingFormData,
  PricingQueryForm,
  CategoryQueryForm,
  ProductStatus,
  ProductPageParams,
  ProductPageResponse,
} from './product';
export type {
  OrderQueryForm,
  OrderStatusValue,
  OrderTypeValue,
} from './order';
export type {
  InventoryInfo,
  InventoryQueryForm,
} from './warehouse-inventory';
export type {
  SupplierInfo,
  SupplierFormData,
  SupplierQueryForm,
} from './supplier';
export type {
  PurchaseOrderInfo,
  PurchaseOrderItemInfo,
  PurchaseOrderQueryForm,
} from './purchase-order';
export type {
  KitchenOrderInfo,
  KitchenOrderQueryForm,
  KitchenOrderStatus,
  KitchenOrderPriority,
} from './kitchen-order';
export type {
  MemberInfo,
  MemberCreateForm,
  MemberUpdateForm,
  MemberQueryForm,
} from './member';
export type {
  NotificationConfig,
  NotificationQueryForm,
  NotificationType,
  NotificationReadStatus,
} from './notification-config';
export type {
  DeviceInfo,
  DeviceFormData,
  DeviceQueryForm,
  DeviceType,
  DeviceConnectionType,
  DeviceStatus,
} from './device';

/** 分页响应（对齐后端MyBatis Plus IPage<T>*/
export interface PageResponse<T> {
  /** 数据列表（IPage字段名：records
 */
  records: T[];
  /** 总记录数
 */
  total: number;
  /** 当前页码（IPage字段名：current，同时兼容Element Plus分页组件
 */
  current: number;
  /** 每页条数
 */
  size: number;
  /** 总页
 */
  pages: number;
}

/**
 * 分页请求参数（对齐后端MyBatis Plus IPage
 */
export interface PageRequest {
  /** 当前页码（IPage字段名：current
 */
  current: number;
  /** 每页条数
 */
  size: number;
  sort?: string;
  order?: "asc" | "desc";
}

/**
 * 基础响应
 */
export interface BaseResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
  success: boolean;
  timestamp: number;
}

/**
 * 文件上传响应
 */
export interface UploadResponse {
  url: string;
  name: string;
  size: number;
  type: string;
}

/**
 * 用户信息
 */
export interface UserInfo {
  id: number;
  username: string;
  name: string;
  email: string;
  phone?: string;
  avatar?: string;
  status: string;
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
 * 角色信息
 */
export interface RoleInfo {
  id: number;
  name: string;
  code: string;
  description?: string;
  status: number;
  permissions: PermissionInfo[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 权限信息
 */
export interface PermissionInfo {
  id: number;
  name: string;
  code: string;
  description?: string;
  type: "menu" | "button" | "api";
  parentId?: number;
  path?: string;
  method?: string;
  sort: number;
  status: number;
  createdAt: string;
  updatedAt: string;
}

/**
 * 菜单信息
 */
export interface MenuInfo {
  id: number;
  name: string;
  path: string;
  component?: string;
  redirect?: string;
  meta?: {
    title: string;
    icon?: string;
    hidden?: boolean;
    roles?: string[];
    keepAlive?: boolean;
    affix?: boolean;
  };
  children?: MenuInfo[];
  parentId?: number;
  sort: number;
  status: number;
  type: "menu" | "button";
  permission?: string;
  createdAt: string;
  updatedAt: string;
}

/**
 * 部门信息
 */
export interface DeptInfo {
  id: number;
  name: string;
  code: string;
  parentId?: number;
  sort: number;
  status: number;
  leader?: string;
  phone?: string;
  email?: string;
  children?: DeptInfo[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 字典信息
 */
export interface DictInfo {
  id: number;
  name: string;
  code: string;
  description?: string;
  status: number;
  items: DictItem[];
  createdAt: string;
  updatedAt: string;
}

/**
 * 字典
 */
export interface DictItem {
  id: number;
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
 * 系统配置
 */
export interface ConfigInfo {
  id: number;
  name: string;
  code: string;
  value: string;
  description?: string;
  type: "string" | "number" | "boolean" | "json";
  options?: string[];
  isSystem: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * 操作日志
 */
export interface OperationLog {
  id: number;
  userId: number;
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
 * 登录日志
 */
export interface LoginLog {
  id: number;
  userId: number;
  username: string;
  ip: string;
  userAgent: string;
  loginType: "password" | "sms" | "email" | "social";
  status: number;
  message?: string;
  createdAt: string;
}

/**
 * 文件信息
 */
export interface FileInfo {
  id: number;
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
 * 通知消息
 */
export interface NotificationInfo {
  id: number;
  title: string;
  content: string;
  type: "info" | "success" | "warning" | "error";
  isRead: boolean;
  senderId?: number;
  senderName?: string;
  receiverId: number;
  createdAt: string;
  updatedAt: string;
  route?: string;
}

/**
 * 表格列配
 */
export interface TableColumn {
  prop: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  align?: "left" | "center" | "right";
  sortable?: boolean | "custom";
  fixed?: boolean | "left" | "right";
  showOverflowTooltip?: boolean;
  /* eslint-disable-next-line no-unused-vars
 */
  formatter?: (_row: Record<string, unknown>, _column: TableColumn, _cellValue: unknown, _index: number) => string;
  /* eslint-disable-next-line no-unused-vars
 */
renderHeader?: (_h: (tag: string, props: Record<string, unknown>, children?: unknown[]) =>
unknown, { _column, _$index }: { _column: TableColumn;
_$index: number }) => unknown;
  children?: TableColumn[];
}

/**
 * 表格列配置（泛型版本，用于Composable类型推断）
 */
export interface TableColumnConfig<T = Record<string, unknown>> {
  prop?: string;
  label: string;
  width?: number | string;
  minWidth?: number | string;
  fixed?: "left" | "right" | boolean;
  sortable?: boolean | "custom";
  align?: "left" | "center" | "right";
  showOverflowTooltip?: boolean;
  formatter?: (row: T, column: TableColumnConfig<T>, cellValue: unknown, index: number) => string;
  slot?: string;
}

/**
 * 表单配置
 */
export interface FormConfig {
  label: string;
  prop: string;
  type:
    | "input"
    | "select"
    | "radio"
    | "checkbox"
    | "date"
    | "datetime"
    | "textarea"
    | "switch"
    | "number";
  required?: boolean;
  rules?: Array<{
    required?: boolean;
    message?: string;
    trigger?: string;
    [key: string]: unknown
  }>;
options?: {
label: string;
value: string |
number }[];
  placeholder?: string;
  disabled?: boolean;
  readonly?: boolean;
  span?: number;
  offset?: number;
  push?: number;
  pull?: number;
}

/**
 * 搜索配置
 */
export interface SearchConfig {
  label: string;
  prop: string;
  type:
    | "input"
    | "select"
    | "date"
    | "datetime"
    | "daterange"
    | "datetimerange";
  placeholder?: string;
options?: {
label: string;
value: string |
number }[];
  span?: number;
}

/**
 * 树形结构
 */
export interface TreeNode {
  id: number | string;
  label: string;
  children?: TreeNode[];
  parentId?: number | string;
  disabled?: boolean;
  isLeaf?: boolean;
  [key: string]: unknown;
}

/**
 * 级联选择数据
 */
export interface CascaderOption {
  value: string | number;
  label: string;
  children?: CascaderOption[];
  disabled?: boolean;
  leaf?: boolean;
  [key: string]: unknown;
}

/**
 * 选择器选项
 */
export interface SelectOption {
  label: string;
  value: string | number | boolean;
  disabled?: boolean;
  [key: string]: unknown;
}

/**
 * 路由元信
 */
export interface RouteMeta {
  title?: string;
  icon?: string;
  hidden?: boolean;
  roles?: string[];
  permissions?: string[];
  keepAlive?: boolean;
  affix?: boolean;
  breadcrumb?: boolean;
  activeMenu?: string;
}

/**
 * 面包屑项
 */
export interface BreadcrumbItem {
  title: string;
  path?: string;
  redirect?: string;
  meta?: RouteMeta;
}

/**
 * 标签页项
 */
export interface TagView {
  name: string;
  path: string;
  title: string;
  icon?: string;
  keepAlive?: boolean;
  affix?: boolean;
  query?: Record<string, unknown>;
  params?: Record<string, unknown>;
}

/**
 * 响应式断
 */
/* eslint-disable no-unused-vars
 */
export enum Breakpoint {
  XS = "xs", // < 768px  SM = "sm", // 768px  MD = "md", // 992px  LG = "lg", // 1200px  XL = "xl", // 1920px
}
/* eslint-enable no-unused-vars
 */

/**
 * 主题配置
 */
export interface ThemeConfig {
  primaryColor: string;
  successColor: string;
  warningColor: string;
  dangerColor: string;
  infoColor: string;
  sidebarBg: string;
  headerBg: string;
  mainBg: string;
  textColor: string;
  textColorSecondary: string;
  borderColor: string;
  borderColorLight: string;
}

/**
 * 布局配置
 */
export interface LayoutConfig {
  sidebarCollapsed: boolean;
  sidebarTheme: "light" | "dark";
  headerTheme: "light" | "dark";
  showTagsView: boolean;
  showBreadcrumb: boolean;
  fixedHeader: boolean;
  showLogo: boolean;
  showFooter: boolean;
  uniqueOpened: boolean;
  menuTrigger: "hover" | "click";
}

/**
 * 系统状
 */
export interface SystemStatus {
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

/**
 * API 响应包装
 */
export interface ApiResponse<T = unknown> {
  success: boolean;
  code: number;
  message: string;
  data: T;
  timestamp: number;
}

/**
 * 分页参数
 */
export interface PageParams {
  page: number;
  size: number;
  sort?: string;
  order?: "asc" | "desc";
}

/**
 * 排序参数
 */
export interface SortParams {
  sort: string;
  order: "asc" | "desc";
}

/**
 * 查询参数
 */
export interface QueryParams extends PageParams {
  [key: string]: unknown;
}

/**
 * 状态枚
 */
/* eslint-disable no-unused-vars
 */
export enum Status {
  DISABLED = 0,
  ENABLED = 1,
}

/**
 * 删除状态枚
 */
export enum DeleteStatus {
  NORMAL = 0,
  DELETED = 1,
}

/**
 * 性别枚举
 */
export enum Gender {
  UNKNOWN = 0,
  MALE = 1,
  FEMALE = 2,
}

/**
 * 用户状态枚
 */
export enum UserStatus {
  DISABLED = 0,
  ENABLED = 1,
  LOCKED = 2,
}

/**
 * 操作类型枚举
 */
export enum OperationType {
  CREATE = "CREATE",
  UPDATE = "UPDATE",
  DELETE = "DELETE",
  QUERY = "QUERY",
  EXPORT = "EXPORT",
  IMPORT = "IMPORT",
}

/**
 * 订单状态枚
 */
export enum OrderStatus {
  // 堂食订单状态 PENDING = "pending", // 待处理 PROCESSING = "processing", // 制作中 COMPLETED = "completed", // 已完成 REJECTED = "rejected", // 已拒绝  // 外卖订单状态 NEW = "new", // 待接单 ACCEPTED = "accepted", // 已接收 DELIVERING = "delivering", // 配送中
}

/**
 * 订单类型枚举
 */
export enum OrderType {
  DINE_IN = "dine_in", // 堂食
  TAKEAWAY = "takeaway", // 外卖
}
/* eslint-enable no-unused-vars
 */

/**
 * 订单菜品
 */
export interface OrderDish {
  id: number;
  dishId: number;
  dishName: string;
  price: number;
  quantity: number;
  remark?: string;
}

/**
 * 订单基础信息
 */
export interface BaseOrder {
  id: number;
  orderNo: string;
  type: OrderType;
  status: OrderStatus;
  amount: number;
  dishes: OrderDish[];
  createTime: string;
  updateTime: string;
  remark?: string;
  storeId: string;
  storeName?: string;
}

/**
 * 堂食订单
 */
export interface DineInOrder extends BaseOrder {
  tableNo: string;
  customerCount?: number;
}

/**
 * 外卖订单
 */
export interface TakeawayOrder extends BaseOrder {
  customerName: string;
  phone: string;
  address: string;
  deliveryTime: string;
  deliveryFee: number;
  deliveryPerson?: string;
}

/**
 * 订单统计数据
 */
export interface OrderStatistics {
  todayOrderCount: number;
  todaySales: number;
  todayCustomerCount: number;
  avgOrderAmount: number;
}

/**
 * 订单趋势数据
 */
export interface OrderTrend {
  date: string;
  orderCount: number;
  totalAmount: number;
  avgAmount: number;
  takeawayCount: number;
  dineInCount: number;
}

/**
 * 订单查询参数
 */
export interface OrderQueryParams extends QueryParams {
  orderNo?: string;
  status?: OrderStatus;
  type?: OrderType;
  startTime?: string;
  endTime?: string;
  tableNo?: string;
  customerName?: string;
}
