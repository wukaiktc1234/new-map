/**
 * 角色工作台配置定义
 *
 * 将 11 种 UserRole 映射为 6 个角色组（RoleGroup），
 * 每个角色组对应一个独立的工作台视图：
 * - 统计卡片配置（哪些指标 + 数据源 + 颜色 + 图标）
 * - 快捷入口（按角色职责过滤）
 * - 待办事项提示（说明该角色需关注的事项类别）
 * - 预警信息提示（说明该角色需关注的风险类别）
 *
 * 设计原则：
 * - 静态配置 + 动态数据：本文件只定义"展示什么"，"数据值"由 useRoleDashboard 加载
 * - 角色职责驱动：每个角色的卡片/入口都对应其岗位职责
 * - 无假数据：所有数据源都标注 API 来源，未实现的展示 0 + TODO 标注
 *
 * 图标说明：
 * Element Plus 图标在 main.ts 中已全局注册（app.component(key, component)），
 * 因此此处使用字符串名称即可，由 <component :is="icon" /> 解析。
 */
import { UserRole } from '@/stores/permission'

// ============================================================
// 类型定义
// ============================================================

/** 统计卡片数据源标识（对应 dashboardApi 返回字段） */
export type StatDataSource =
  | 'todaySales.todayOrderCount'
  | 'todaySales.todayAmount'
  | 'todaySales.yesterdayGrowth'
  | 'inventory.warningCount'
  | 'inventory.outOfStockCount'
  | 'inventory.overstockCount'
  | 'member.totalMembers'
  | 'member.newMembers'
  | 'member.growthRate'
  | 'finance.monthlyRevenue'
  | 'finance.monthlyExpense'
  | 'finance.monthlyProfit'
  | 'finance.profitMargin'

/** 统计卡片配置 */
export interface StatCardConfig {
  /** 唯一标识 */
  key: string
  /** 卡片标题 */
  label: string
  /** Element Plus 图标名称（字符串，依赖全局注册） */
  icon: string
  /** 颜色类型 */
  colorType: 'primary' | 'success' | 'warning' | 'error' | 'info'
  /** 数据源标识 */
  dataSource: StatDataSource
  /** 显示格式 */
  format?: 'number' | 'currency' | 'percent'
  /** 是否后端 TODO（true 时展示 0 并标注"待实现"） */
  isTodo?: boolean
}

/** 快捷入口配置 */
export interface QuickActionConfig {
  /** 显示名称 */
  label: string
  /** Element Plus 图标名称（字符串，依赖全局注册） */
  icon: string
  /** 路由路径 */
  path: string
  /** CSS 变量颜色 */
  color: string
  /** 所需权限（为空则不校验，任意角色可见） */
  requiredPermission?: string
}

/** 角色组配置 */
export interface RoleProfile {
  /** 角色组标识 */
  key: 'admin_owner' | 'ops' | 'finance' | 'hr' | 'store' | 'employee'
  /** 角色显示名称 */
  displayName: string
  /** 角色描述 */
  description: string
  /** 问候语模板（{name} 会被替换为用户名） */
  greetingTemplate: string
  /** 统计卡片配置列表 */
  statsCards: StatCardConfig[]
  /** 快捷入口列表 */
  quickActions: QuickActionConfig[]
  /** 待办事项说明（用于空状态展示） */
  pendingTasksHint: string
  /** 预警信息说明（用于空状态展示） */
  alertsHint: string
}

// ============================================================
// 颜色变量（避免硬编码颜色，统一使用 CSS 变量）
// ============================================================

const COLOR_PRIMARY = 'var(--fts-primary)'
const COLOR_SUCCESS = 'var(--fts-success)'
const COLOR_WARNING = 'var(--fts-warning)'
const COLOR_ERROR = 'var(--fts-error)'
const COLOR_INFO = 'var(--fts-info)'

// ============================================================
// 6 个角色组配置
// ============================================================

/** 角色组 1：超级管理员 / 老板（admin, owner） */
export const ADMIN_OWNER_PROFILE: RoleProfile = {
  key: 'admin_owner',
  displayName: '经营者',
  description: '全权管理企业所有业务域',
  greetingTemplate: '欢迎回来，{name}，以下是企业整体经营概览',
  statsCards: [
    {
      key: 'today-orders',
      label: '今日订单数',
      icon: 'ShoppingCart',
      colorType: 'primary',
      dataSource: 'todaySales.todayOrderCount',
      format: 'number',
    },
    {
      key: 'today-amount',
      label: '今日销售额',
      icon: 'Money',
      colorType: 'success',
      dataSource: 'todaySales.todayAmount',
      format: 'currency',
      isTodo: true,
    },
    {
      key: 'inventory-warning',
      label: '库存预警数',
      icon: 'Warning',
      colorType: 'warning',
      dataSource: 'inventory.warningCount',
      format: 'number',
    },
    {
      key: 'total-members',
      label: '会员总数',
      icon: 'User',
      colorType: 'info',
      dataSource: 'member.totalMembers',
      format: 'number',
    },
  ],
  quickActions: [
    { label: '经营报表', icon: 'TrendCharts', path: '/analytics/dashboard', color: COLOR_PRIMARY },
    { label: '门店管理', icon: 'Shop', path: '/store-ops/archive', color: COLOR_SUCCESS },
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_WARNING, requiredPermission: 'purchase:request:view' },
    { label: '财务总览', icon: 'Wallet', path: '/finance/report', color: COLOR_ERROR },
    { label: '订单查询', icon: 'Search', path: '/order/query', color: COLOR_INFO },
    { label: '员工管理', icon: 'User', path: '/hr/employee', color: COLOR_PRIMARY },
    { label: '权限中心', icon: 'Lock', path: '/system/permission', color: COLOR_INFO },
    { label: '操作审计', icon: 'Document', path: '/system/audit', color: COLOR_WARNING },
  ],
  pendingTasksHint: '待办事项需后端提供统一聚合接口（如 /v1/dashboard/my-pending）',
  alertsHint: '当前展示库存预警，财务/合规预警需后端扩展',
}

/** 角色组 2：运营总监（ops_director） */
export const OPS_PROFILE: RoleProfile = {
  key: 'ops',
  displayName: '运营总监',
  description: '负责门店运营、采购、库存、订单等业务管理',
  greetingTemplate: '欢迎回来，{name}，以下是运营关键指标',
  statsCards: [
    {
      key: 'today-orders',
      label: '今日订单数',
      icon: 'ShoppingCart',
      colorType: 'primary',
      dataSource: 'todaySales.todayOrderCount',
      format: 'number',
    },
    {
      key: 'inventory-warning',
      label: '库存预警数',
      icon: 'Warning',
      colorType: 'warning',
      dataSource: 'inventory.warningCount',
      format: 'number',
    },
    {
      key: 'out-of-stock',
      label: '缺货数',
      icon: 'Box',
      colorType: 'error',
      dataSource: 'inventory.outOfStockCount',
      format: 'number',
      isTodo: true,
    },
    {
      key: 'overstock',
      label: '积压数',
      icon: 'Goods',
      colorType: 'info',
      dataSource: 'inventory.overstockCount',
      format: 'number',
      isTodo: true,
    },
  ],
  quickActions: [
    { label: '门店运营', icon: 'Shop', path: '/store-ops/status', color: COLOR_PRIMARY },
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_SUCCESS, requiredPermission: 'purchase:request:view' },
    { label: '库存管理', icon: 'Van', path: '/warehouse/inventory', color: COLOR_WARNING },
    { label: '运营报表', icon: 'DataAnalysis', path: '/analytics/dashboard', color: COLOR_ERROR },
    { label: '订单查询', icon: 'Search', path: '/order/query', color: COLOR_INFO },
    { label: '设备管理', icon: 'Tools', path: '/device/monitor', color: COLOR_PRIMARY },
    { label: '日结对账', icon: 'Calendar', path: '/store-ops/daily-settlement', color: COLOR_SUCCESS },
    { label: '供应商管理', icon: 'User', path: '/purchase/supplier', color: COLOR_WARNING },
  ],
  pendingTasksHint: '采购审批、库存调整审批需后端提供待办聚合接口',
  alertsHint: '当前展示库存预警，设备异常预警需后端扩展',
}

/** 角色组 3：财务总监（finance_director） */
export const FINANCE_PROFILE: RoleProfile = {
  key: 'finance',
  displayName: '财务总监',
  description: '负责企业财务报表、付款审批、应收应付管理',
  greetingTemplate: '欢迎回来，{name}，以下是财务关键指标',
  statsCards: [
    {
      key: 'monthly-revenue',
      label: '月度营收',
      icon: 'TrendCharts',
      colorType: 'primary',
      dataSource: 'finance.monthlyRevenue',
      format: 'currency',
      isTodo: true,
    },
    {
      key: 'monthly-expense',
      label: '月度支出',
      icon: 'Wallet',
      colorType: 'warning',
      dataSource: 'finance.monthlyExpense',
      format: 'currency',
      isTodo: true,
    },
    {
      key: 'monthly-profit',
      label: '月度利润',
      icon: 'Money',
      colorType: 'success',
      dataSource: 'finance.monthlyProfit',
      format: 'currency',
      isTodo: true,
    },
    {
      key: 'profit-margin',
      label: '利润率',
      icon: 'Histogram',
      colorType: 'info',
      dataSource: 'finance.profitMargin',
      format: 'percent',
      isTodo: true,
    },
  ],
  quickActions: [
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_WARNING, requiredPermission: 'purchase:request:view' },
    { label: '财务报表', icon: 'TrendCharts', path: '/finance/report', color: COLOR_PRIMARY },
    { label: '付款管理', icon: 'Wallet', path: '/finance/payable', color: COLOR_ERROR },
    { label: '应收应付', icon: 'CreditCard', path: '/finance/receivable', color: COLOR_WARNING },
    { label: '税务管理', icon: 'Document', path: '/finance/tax', color: COLOR_INFO },
    { label: '资金流水', icon: 'Money', path: '/finance/fund-flow', color: COLOR_SUCCESS },
    { label: '报销管理', icon: 'Tickets', path: '/finance/reimbursement', color: COLOR_PRIMARY },
    { label: '发票管理', icon: 'Stamp', path: '/finance/invoice', color: COLOR_WARNING },
    { label: '记账凭证', icon: 'Memo', path: '/finance/voucher', color: COLOR_INFO },
  ],
  pendingTasksHint: '付款审批、报销审批需后端提供待办聚合接口',
  alertsHint: '财务预警（逾期/超预算）需后端扩展',
}

/** 角色组 4：HR 总监（hr_director） */
export const HR_PROFILE: RoleProfile = {
  key: 'hr',
  displayName: 'HR 总监',
  description: '负责员工档案、考勤、薪资、招聘、培训管理',
  greetingTemplate: '欢迎回来，{name}，以下是人力资源概览',
  statsCards: [
    {
      key: 'total-members',
      label: '会员总数（员工代理指标）',
      icon: 'User',
      colorType: 'primary',
      dataSource: 'member.totalMembers',
      format: 'number',
    },
    {
      key: 'new-members',
      label: '近30天新增',
      icon: 'UserFilled',
      colorType: 'success',
      dataSource: 'member.newMembers',
      format: 'number',
    },
    {
      key: 'today-orders',
      label: '今日订单数',
      icon: 'ShoppingCart',
      colorType: 'info',
      dataSource: 'todaySales.todayOrderCount',
      format: 'number',
    },
    {
      key: 'member-growth',
      label: '会员增长率',
      icon: 'TrendCharts',
      colorType: 'warning',
      dataSource: 'member.growthRate',
      format: 'percent',
    },
  ],
  quickActions: [
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_WARNING, requiredPermission: 'purchase:request:view' },
    { label: '员工档案', icon: 'User', path: '/hr/employee', color: COLOR_PRIMARY },
    { label: '考勤管理', icon: 'Clock', path: '/hr/attendance', color: COLOR_SUCCESS },
    { label: '薪资管理', icon: 'Wallet', path: '/hr/salary', color: COLOR_WARNING },
    { label: '招聘管理', icon: 'Avatar', path: '/hr/recruitment', color: COLOR_ERROR },
    { label: '健康证', icon: 'Document', path: '/hr/health-certificate', color: COLOR_INFO },
    { label: '培训管理', icon: 'Memo', path: '/hr/training', color: COLOR_PRIMARY },
    { label: '员工合同', icon: 'Files', path: '/hr/contract', color: COLOR_WARNING },
    { label: '知识库', icon: 'Document', path: '/hr/knowledge-base', color: COLOR_INFO },
  ],
  pendingTasksHint: '入职审批、请假审批、薪资审批需后端提供待办聚合接口',
  alertsHint: '健康证到期预警、合同到期预警需后端扩展',
}

/** 角色组 5：店长 / 组长（store_manager, team_leader） */
export const STORE_PROFILE: RoleProfile = {
  key: 'store',
  displayName: '门店管理者',
  description: '负责本门店日常运营、订单、库存、员工管理',
  greetingTemplate: '欢迎回来，{name}，以下是本门店运营概览',
  statsCards: [
    {
      key: 'today-orders',
      label: '今日订单数',
      icon: 'ShoppingCart',
      colorType: 'primary',
      dataSource: 'todaySales.todayOrderCount',
      format: 'number',
    },
    {
      key: 'today-amount',
      label: '今日销售额',
      icon: 'Money',
      colorType: 'success',
      dataSource: 'todaySales.todayAmount',
      format: 'currency',
      isTodo: true,
    },
    {
      key: 'inventory-warning',
      label: '库存预警数',
      icon: 'Warning',
      colorType: 'warning',
      dataSource: 'inventory.warningCount',
      format: 'number',
    },
    {
      key: 'total-members',
      label: '会员总数',
      icon: 'User',
      colorType: 'info',
      dataSource: 'member.totalMembers',
      format: 'number',
    },
  ],
  quickActions: [
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_WARNING, requiredPermission: 'purchase:request:view' },
    { label: '日结对账', icon: 'Calendar', path: '/store-ops/daily-settlement', color: COLOR_WARNING },
    { label: '订单查询', icon: 'Search', path: '/order/query', color: COLOR_PRIMARY },
    { label: '库存盘点', icon: 'Van', path: '/warehouse/inventory', color: COLOR_SUCCESS },
    { label: '考勤管理', icon: 'Clock', path: '/hr/attendance', color: COLOR_INFO },
    { label: '员工管理', icon: 'User', path: '/hr/employee', color: COLOR_PRIMARY },
    { label: '设备管理', icon: 'Tools', path: '/device/monitor', color: COLOR_WARNING },
    { label: '门店证书', icon: 'Document', path: '/store-ops/certificate', color: COLOR_ERROR },
    { label: '排队叫号', icon: 'Bell', path: '/store-ops/queue', color: COLOR_INFO },
  ],
  pendingTasksHint: '门店运营审批（排班/调价/退货）需后端提供待办聚合接口',
  alertsHint: '当前展示库存预警，设备异常预警需后端扩展',
}

/** 角色组 6：普通员工（employee 及其他） */
export const EMPLOYEE_PROFILE: RoleProfile = {
  key: 'employee',
  displayName: '员工',
  description: '个人工作台：考勤、薪资、培训、健康证管理',
  greetingTemplate: '欢迎回来，{name}，以下是与您相关的事项',
  statsCards: [
    {
      key: 'today-orders',
      label: '今日订单数（所在门店）',
      icon: 'ShoppingCart',
      colorType: 'primary',
      dataSource: 'todaySales.todayOrderCount',
      format: 'number',
    },
    {
      key: 'total-members',
      label: '门店会员数',
      icon: 'User',
      colorType: 'info',
      dataSource: 'member.totalMembers',
      format: 'number',
    },
  ],
  quickActions: [
    { label: '采购申请', icon: 'Box', path: '/purchase/request', color: COLOR_WARNING, requiredPermission: 'purchase:request:view' },
    { label: '考勤打卡', icon: 'Clock', path: '/hr/attendance', color: COLOR_PRIMARY },
    { label: '我的薪资', icon: 'Wallet', path: '/hr/salary', color: COLOR_SUCCESS },
    { label: '培训学习', icon: 'Memo', path: '/hr/training', color: COLOR_WARNING },
    { label: '知识库', icon: 'Document', path: '/hr/knowledge-base', color: COLOR_INFO },
    { label: '健康证', icon: 'Document', path: '/hr/health-certificate', color: COLOR_ERROR },
    { label: '个人中心', icon: 'UserFilled', path: '/profile', color: COLOR_PRIMARY },
  ],
  pendingTasksHint: '我的待办（培训任务/审批通知）需后端提供个人待办接口',
  alertsHint: '个人预警（健康证到期/合同到期）需后端扩展',
}

// ============================================================
// 角色 → 角色组映射
// ============================================================

/**
 * 根据用户角色列表，匹配最合适的角色组配置
 * 优先级：admin/owner > ops_director > purchase_manager/department_manager > finance_director > hr_director > store_manager/warehouse_manager > employee
 */
export function resolveRoleProfile(roles: UserRole[]): RoleProfile {
  if (!roles || roles.length === 0) {
    return EMPLOYEE_PROFILE
  }

  // 按优先级匹配
  if (roles.includes(UserRole.ADMIN) || roles.includes(UserRole.OWNER)) {
    return ADMIN_OWNER_PROFILE
  }
  if (roles.includes(UserRole.OPS_DIRECTOR)) {
    return OPS_PROFILE
  }
  if (roles.includes(UserRole.PURCHASE_MANAGER) || roles.includes(UserRole.DEPARTMENT_MANAGER)) {
    return OPS_PROFILE
  }
  if (roles.includes(UserRole.FINANCE_DIRECTOR)) {
    return FINANCE_PROFILE
  }
  if (roles.includes(UserRole.HR_DIRECTOR)) {
    return HR_PROFILE
  }
  if (roles.includes(UserRole.STORE_MANAGER) || roles.includes(UserRole.TEAM_LEADER) || roles.includes(UserRole.WAREHOUSE_MANAGER)) {
    return STORE_PROFILE
  }

  // 默认：普通员工（含 employee/scheduler/region_manager/auditor）
  return EMPLOYEE_PROFILE
}

/** 所有角色组配置（用于调试或后续扩展） */
export const ALL_ROLE_PROFILES: RoleProfile[] = [
  ADMIN_OWNER_PROFILE,
  OPS_PROFILE,
  FINANCE_PROFILE,
  HR_PROFILE,
  STORE_PROFILE,
  EMPLOYEE_PROFILE,
]
