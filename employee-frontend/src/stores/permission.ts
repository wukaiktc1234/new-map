import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import {
  HomeFilled, Document, Calendar, Money,
  Tickets, Reading, Bell,
  TrendCharts, User, AlarmClock, Switch,
  Wallet, Van, Box, Grid, ChatDotRound,
  EditPen, List,
} from '@element-plus/icons-vue'
import { useAuthContext } from '@/composables/useAuthContext'

// 从统一身份上下文获取默认用户名（Mock 阶段）
const _auth = useAuthContext()
// 层级化权限体系类型
import {
  UserLevel,
  type LevelInfo,
  type FlowNodeRole,
  type ApprovalChainRule,
  LEVEL_CONFIG_MAP,
  getLevelInfo,
  canApproveLevel as checkCanApproveLevel,
} from '@/types/permission'

// ============================================
// 图标组件映射表（解决TabBar图标不显示问题）
// ============================================
const ICON_MAP: Record<string, any> = {
  HomeFilled,
  Document,
  Calendar,
  Money,
  Tickets,
  Reading,
  Bell,
  TrendCharts,
  User,
  AlarmClock,
  Switch,
  Wallet,
  Van,
  Box,
  Grid,
  ChatDotRound,
  EditPen,
  List,
}

/** 将图标字符串转换为组件引用 */
// eslint-disable-next-line @typescript-eslint/no-explicit-any -- ICON_MAP 返回 Vue 组件引用，无法用具体类型表达
export function getIconComponent(iconName: string): any {
  return ICON_MAP[iconName] || HomeFilled
}

// ============================================
// 角色定义（餐饮行业岗位体系）
// ============================================
export type EmployeeRole =
  | 'store_staff'      // 门店员工（服务员/收银/厨师）
  | 'store_manager'    // 门店店长
  | 'kitchen_staff'    // 后厨人员
  | 'headquarters'     // 总部员工
  | 'warehouse'        // 仓库人员
  | 'hr'               // 人事专员
  | 'finance'          // 财务人员

/** 角色中文显示名映射 */
export const ROLE_LABELS: Record<EmployeeRole, string> = {
  store_staff: '门店员工',
  store_manager: '门店店长',
  kitchen_staff: '后厨人员',
  headquarters: '总部员工',
  warehouse: '仓库人员',
  hr: '人事专员',
  finance: '财务人员',
}

// ============================================
// 模块配置（千人千面的核心数据结构）
// ============================================
export interface ModuleConfig {
  id: string
  label: string
  icon: string
  route: string
  color: string
  roles: EmployeeRole[]        // 空数组 = 所有角色可见
  category: 'main' | 'secondary' | 'tool'
  order: number
  group?: string               // 分组名（用于导航分组）
  badge?: string               // 角标文字（如"新"）
}

// ============================================
// 移动端底部导航类别定义（4个主Tab）
// ============================================

/** 移动端Tab类别标识 */
export type MobileTabCategory = 'home' | 'office' | 'messages' | 'profile'

/** 单个移动端Tab配置 */
export interface MobileTabConfig {
  category: MobileTabCategory
  label: string
  icon: string
  route: string
  fixed: boolean
}

// ============================================
// 办公页面功能分组（动态聚合元数据）
// 分组与 ALL_MODULES 中模块的 group 字段一一对应
// 新增模块时只需在 ALL_MODULES 中填写 group 字段即可自动归组
// ============================================

/** 分组名（中文，对应 ALL_MODULES[].group）→ 分组元数据 */
interface GroupMeta {
  key: string
  title: string
  icon: string
  order: number
}

/** 办公页分组注册表（按排序） */
const GROUP_META: Record<string, GroupMeta> = {
  '日常办公': { key: 'daily_office', title: '日常办公', icon: 'Document', order: 1 },
  '门店运营': { key: 'store_ops',   title: '门店运营', icon: 'Calendar', order: 2 },
  '个人服务': { key: 'personal_svc', title: '个人服务', icon: 'User', order: 3 },
  '消息中心': { key: 'message_center', title: '消息中心', icon: 'Bell', order: 4 },
  '数据分析': { key: 'data_analysis', title: '数据分析', icon: 'TrendCharts', order: 5 },
}

/** 不在办公页展示的模块ID（Tab级入口/工具类） */
const OFFICE_EXCLUDED_IDS = new Set(['workspace', 'profile', 'notices'])

// ============================================
// 审批类型定义
// ============================================

export interface ApprovalType {
  id: string
  label: string
  icon: string
  color: string
}

/** 全部审批类型清单（macOS系统色彩） */
export const ALL_APPROVAL_TYPES: ApprovalType[] = [
  { id: 'leave', label: '请假', icon: 'Tickets', color: '#007AFF' },
  { id: 'overtime', label: '加班', icon: 'AlarmClock', color: '#FF9500' },
  { id: 'shift_swap', label: '调班', icon: 'Switch', color: '#34C759' },
  { id: 'reimbursement', label: '报销', icon: 'Wallet', color: '#FF3B30' },
  { id: 'business_trip', label: '出差', icon: 'Van', color: '#8E8E93' },
  { id: 'requisition', label: '领用', icon: 'Box', color: '#AF52DE' },
]

// ============================================
// 完整模块注册表（按 category 分组）
// ============================================

const ALL_MODULES: ModuleConfig[] = [
  // ---- 主导航模块（category: main）----
  {
    id: 'workspace',
    label: '首页',
    icon: 'HomeFilled',
    route: '/home',
    color: '#007AFF',
    roles: [],
    category: 'main',
    order: 1,
  },
  {
    id: 'approval',
    label: '审批中心',
    icon: 'CircleCheck',
    route: '/approval',
    color: '#FF9500',
    roles: [],
    category: 'main',
    order: 2,
    badge: '新',
    group: '日常办公',
  },
  {
    id: 'schedule',
    label: '我的排班',
    icon: 'Calendar',
    route: '/schedule',
    color: '#34C759',
    roles: ['store_staff', 'store_manager', 'kitchen_staff'],
    category: 'main',
    order: 3,
    group: '门店运营',
  },
  {
    id: 'salary',
    label: '工资条',
    icon: 'Money',
    route: '/salary',
    color: '#FF3B30',
    roles: [],
    category: 'main',
    order: 4,
    group: '个人服务',
  },

  // ---- 次级导航模块（category: secondary）----
  {
    id: 'leave',
    label: '请假申请',
    icon: 'Tickets',
    route: '/leave',
    color: '#007AFF',
    roles: [],
    category: 'secondary',
    order: 1,
    group: '日常办公',
  },
  {
    id: 'create_approval',
    label: '发起申请',
    icon: 'EditPen',
    route: '/approval?action=create',
    color: '#FF9500',
    roles: [],
    category: 'secondary',
    order: 2,
    group: '日常办公',
  },
  {
    id: 'training',
    label: '培训记录',
    icon: 'Reading',
    route: '/training',
    color: '#AF52DE',
    roles: [],
    category: 'secondary',
    order: 3,
    group: '个人服务',
  },
  {
    id: 'knowledge',
    label: '知识库',
    icon: 'Document',
    route: '/knowledge',
    color: '#5AC8FA',
    roles: [],
    category: 'secondary',
    order: 4,
    group: '个人服务',
  },
  {
    id: 'tasks',
    label: '工作任务',
    icon: 'List',
    route: '/tasks',
    color: '#FF9F0A',
    roles: [],
    category: 'secondary',
    order: 5,
    group: '个人服务',
  },
  {
    id: 'review',
    label: '绩效考核',
    icon: 'TrendCharts',
    route: '/review',
    color: '#34C759',
    roles: [],
    category: 'secondary',
    order: 4,
    group: '个人服务',
  },
  {
    id: 'notices',
    label: '通知消息',
    icon: 'Bell',
    route: '/notices',
    color: '#FF9500',
    roles: [],
    category: 'secondary',
    order: 5,
    group: '消息中心',
  },
  {
    id: 'reports',
    label: '数据报表',
    icon: 'TrendCharts',
    route: '/reports',
    color: '#007AFF',
    // 数据报表：所有员工均可查看自己的考勤/工作数据（个人视角）
    // 管理者额外可见团队/门店维度的报表（由后端权限控制返回的数据范围）
    roles: ['store_staff', 'store_manager', 'headquarters', 'hr', 'finance'],
    category: 'secondary',
    order: 6,
    group: '数据分析',
  },

  // ---- 工具模块（category: tool）----
  {
    id: 'profile',
    label: '个人中心',
    icon: 'User',
    route: '/profile',
    color: '#8E8E93',
    roles: [],
    category: 'tool',
    order: 1,
  },
]

// ============================================
// 移动端底部Tab定义（固定4个，与角色无关）
// ============================================

const MOBILE_TABS: MobileTabConfig[] = [
  {
    category: 'home',
    label: '首页',
    icon: 'HomeFilled',
    route: '/home',
    fixed: true,
  },
  {
    category: 'office',
    label: '办公',
    icon: 'Grid',
    route: '/office',
    fixed: true,
  },
  {
    category: 'messages',
    label: '消息',
    icon: 'ChatDotRound',
    route: '/messages',
    fixed: true,
  },
  {
    category: 'profile',
    label: '我的',
    icon: 'User',
    route: '/profile',
    fixed: true,
  },
]

// ============================================
// 审批类型可见性矩阵（千人千面核心）
// 不同角色看到的审批模板不同
// ============================================

/** 各角色可见的审批类型ID集合 */
const APPROVAL_VISIBILITY_MAP: Record<EmployeeRole, string[]> = {
  // 门店员工：请假、加班、调班、报销（无出差/领用）
  store_staff: ['leave', 'overtime', 'shift_swap', 'reimbursement'],
  // 门店店长：全部6种 + 可审批他人提交的申请
  store_manager: ['leave', 'overtime', 'shift_swap', 'reimbursement', 'business_trip', 'requisition'],
  // 后厨人员：与门店员工相同（有排班需求）
  kitchen_staff: ['leave', 'overtime', 'shift_swap', 'reimbursement'],
  // 总部员工：请假、加班、出差、报销（无调班/领用 — 总部不涉及排班和物资领用）
  headquarters: ['leave', 'overtime', 'business_trip', 'reimbursement'],
  // 仓库人员：加班、领用、请假（最少 — 仓库岗位特殊）
  warehouse: ['overtime', 'requisition', 'leave'],
  // 人事专员：全部6种（HR视角，需查看所有类型）
  hr: ['leave', 'overtime', 'shift_swap', 'reimbursement', 'business_trip', 'requisition'],
  // 财务人员：报销审批（财务视角，主要关注费用类）
  finance: ['reimbursement'],
}

// ============================================
// 权限点定义（细粒度权限控制）
// ============================================

/** 权限点 → 拥有该权限的角色映射 */
const PERMISSION_ROLE_MAP: Record<string, EmployeeRole[]> = {
  'approval:review': ['store_manager', 'hr', 'finance'],       // 审批他人申请
  'report:view': ['store_manager', 'headquarters', 'hr', 'finance'],  // 查看报表
  'report:export': ['store_manager', 'headquarters', 'finance'],     // 导出报表
  'schedule:manage': ['store_manager'],                           // 管理排班
  'staff:view': ['store_manager', 'hr'],                          // 查看员工信息
  'staff:manage': ['store_manager', 'hr'],                        // 管理员工
  'finance:view': ['store_manager', 'finance'],                   // 查看财务数据
  'warehouse:manage': ['warehouse', 'store_manager'],             // 管理库存
}

// ============================================
// Mock 用户数据（开发阶段使用）
// ============================================

const MOCK_USERS: Record<EmployeeRole, { name: string; department: string; position: string; storeName: string; employeeNo: string; phone: string; email: string; joinDate: string; probationEnd: string; supervisorName: string }> = {
  store_staff: { name: _auth.userName.value, department: '前厅部', position: '服务员', storeName: '朝阳大悦城店', employeeNo: 'EMP2024030101', phone: '138****8888', email: 'zhangsan@example.com', joinDate: '2024-03-01', probationEnd: '2024-08-31', supervisorName: '李四（店长）' },
  store_manager: { name: '李四', department: '门店管理', position: '店长', storeName: '朝阳大悦城店', employeeNo: 'EMP2023011501', phone: '139****6666', email: 'lisi@example.com', joinDate: '2023-01-15', probationEnd: '', supervisorName: '王经理（区域）' },
  kitchen_staff: { name: '王五', department: '厨房部', position: '厨师', storeName: '朝阳大悦城店', employeeNo: 'EMP2024062001', phone: '137****5555', email: 'wangwu@example.com', joinDate: '2024-06-20', probationEnd: '2024-12-19', supervisorName: '李四（店长）' },
  headquarters: { name: '赵六', department: '运营中心', position: '运营专员', storeName: '', employeeNo: 'EMP2023081001', phone: '136****4444', email: 'zhaoliu@example.com', joinDate: '2023-08-10', probationEnd: '', supervisorName: '钱总监' },
  warehouse: { name: '孙七', department: '仓储物流', position: '仓管员', storeName: '中央仓库', employeeNo: 'EPM2024022801', phone: '135****3333', email: 'sunqi@example.com', joinDate: '2024-02-28', probationEnd: '2024-08-27', supervisorName: '周主管' },
  hr: { name: '周八', department: '人力资源', position: 'HR专员', storeName: '', employeeNo: 'EMP2023050501', phone: '134****2222', email: 'zhouba@example.com', joinDate: '2023-05-05', probationEnd: '', supervisorName: '吴经理（HRD）' },
  finance: { name: '吴九', department: '财务部', position: '会计', storeName: '', employeeNo: 'EMP2023041201', phone: '133****1111', email: 'wujiu@example.com', joinDate: '2023-04-12', probationEnd: '', supervisorName: '郑财务总监' },
}

// ============================================
// [M6] 身份同步机制说明（Single Source of Truth）
// ============================================
//
// 本项目中存在三个身份信息存储位置，各有职责：
//
//   1. useAuthContext（composable）    — 轻量级认证上下文
//      存储：userName、userLevel、token 等基础认证信息
//      用途：API 请求拦截器自动附加 token、全局身份快速读取
//      特点：单例模式，应用启动即创建
//
//   2. employeeStore（Pinia Store）     — 员工业务数据
//      存储：userId、username、fullName、departmentName、roles 等
//      用途：员工档案展示、个人中心页面数据源
//      特点：登录成功后由 LoginPage 写入
//
//   3. permissionStore（本文件）        — 权限与角色体系
//      存储：currentRole、userLevel、userInfo（完整档案）、导航/审批可见性
//      用途：权限判断、菜单渲染、千人千面
//      特点：登录成功后由 LoginPage 同步写入
//
// === 身份流转链路（登录成功后）===
//   LoginPage.handleLoginSuccess()
//     → employeeStore.setUserInfo({ userId, username, fullName, roles, ... })
//     → permissionStore.setUserLevel(level)          // 同步层级
//     → permissionStore.initFromApi(role, info, level) // 完整初始化（推荐）
//
// === 单一事实来源（SSOT）===
//   - 认证状态（token/是否登录）：useAuthContext
//   - 员工基础信息（姓名/部门）：employeeStore
//   - 权限角色与层级：permissionStore
//   - 三者通过 LoginPage 登录流程保持同步，避免直接跨 Store 读取对方数据
//
// ============================================

export const usePermissionStore = defineStore('permission', () => {
  // ---------- 状态 ----------
  /** 当前用户角色（默认门店员工，实际从登录API获取） */
  const currentRole = ref<EmployeeRole>('store_staff')

  /** 用户层级（默认员工，从登录信息中获取） */
  const userLevel = ref<UserLevel>(UserLevel.STAFF)

  /**
   * 用户基本信息（员工档案字段）
   * 字段来源：管理端 employees 表 + 关联表（positions/stores/departments）
   * 编辑权限：
   *   - 员工可编辑：name, phone, email（部分）
   *   - 管理端维护：employeeNo, department, position, storeName, joinDate, status 等
   */
  const userInfo = ref({
    // === 基础身份信息 ===
    name: '',           // 姓名（员工可编辑）
    employeeNo: '',     // 工号（管理端维护，只读）
    phone: '',          // 手机号（员工可编辑）
    email: '',          // 邮箱（员工可编辑）

    // === 组织信息 ===
    department: '',     // 部门（管理端维护，只读）
    position: '',       // 职位/岗位（管理端维护，只读）
    storeName: '',      // 所属门店（管理端维护，只读）

    // === 入职信息 ===
    joinDate: '',       // 入职日期（管理端维护，只读）
    probationEnd: '',   // 试用期截止日（管理端维护，只读）
    workStatus: '',     // 在职状态：active/probation/inactive（管理端维护，只读）

    // === 其他 ===
    avatar: '',         // 头像URL
    supervisorName: '', // 直属上级姓名（管理端维护，只读）
  })

  // ---------- 计算属性 ----------

  /** 当前用户层级配置信息（从 LEVEL_CONFIG_MAP 获取） */
  const levelInfo = computed<LevelInfo>(() => getLevelInfo(userLevel.value))

  /** 当前角色对应的审批类型列表 */
  const visibleApprovalTypes = computed<ApprovalType[]>(() => {
    const allowedIds = APPROVAL_VISIBILITY_MAP[currentRole.value] || []
    return ALL_APPROVAL_TYPES.filter(t => allowedIds.includes(t.id))
  })

  /** 当前角色是否可以审批他人申请（基于层级权限体系） */
  const canApproveOthers = computed(() => levelInfo.value.canApprove)

  // ---------- 语义化权限属性 ----------

  /** 是否为一线员工（门店/后厨/仓库） */
  const isFieldStaff = computed(() =>
    ['store_staff', 'kitchen_staff', 'warehouse'].includes(currentRole.value)
  )

  /** 是否为管理人员（店长/人事/财务） */
  const isManager = computed(() =>
    ['store_manager', 'hr', 'finance'].includes(currentRole.value)
  )

  /** 是否为总部人员 */
  const isHeadquarters = computed(() => currentRole.value === 'headquarters')

  /** 是否拥有审批权限 */
  const canApprove = computed(() =>
    ['store_manager', 'hr', 'finance'].includes(currentRole.value)
  )

  /** 是否可查看财务数据 */
  const canViewFinance = computed(() =>
    ['store_manager', 'finance'].includes(currentRole.value)
  )

  /** 是否有门店上下文（非总部人员） */
  const hasStoreContext = computed(() => !isHeadquarters.value)

  // ---------- 核心方法 ----------

  /**
   * 设置当前用户角色
   * 同时更新对应的Mock用户信息
   * @param role - 角色类型
   * @param level - 用户层级（可选，默认根据角色推断）
   */
  function setRole(role: EmployeeRole, level?: UserLevel): void {
    currentRole.value = role
    // 根据角色推断默认层级（如果未显式传入）
    if (level) {
      userLevel.value = level
    } else {
      // 角色到层级的默认映射
      const roleLevelMap: Partial<Record<EmployeeRole, UserLevel>> = {
        store_staff: UserLevel.STAFF,
        kitchen_staff: UserLevel.STAFF,
        warehouse: UserLevel.STAFF,
        store_manager: UserLevel.SUPERVISOR,
        hr: UserLevel.MANAGER,
        finance: UserLevel.MANAGER,
        headquarters: UserLevel.STAFF,
      }
      userLevel.value = roleLevelMap[role] || UserLevel.STAFF
    }
    const mockUser = MOCK_USERS[role]
    if (mockUser) {
      userInfo.value = {
        ...mockUser,
        avatar: '',
        workStatus: '',
      }
    }
  }

  /**
   * 从API初始化用户信息（替换Mock数据）
   * @param role - 服务端返回的角色
   * @param info - 服务端返回的用户信息（完整员工档案字段）
   * @param level - 服务端返回的用户层级（可选）
   */
  function initFromApi(
    role: EmployeeRole,
    info: {
      name: string
      employeeNo: string
      phone: string
      email: string
      department: string
      position: string
      storeName: string
      joinDate: string
      probationEnd: string
      workStatus: string
      avatar: string
      supervisorName: string
    },
    level?: UserLevel
  ): void {
    currentRole.value = role
    userInfo.value = { ...info }
    // 如果后端返回了层级值则使用，否则保持默认
    if (level) {
      userLevel.value = level
    }
  }

  /**
   * 获取当前角色可见的所有导航模块（已按order排序）
   */
  function getVisibleNavModules(): ModuleConfig[] {
    return ALL_MODULES
      .filter(m => isModuleVisible(m.id))
      .sort((a, b) => a.order - b.order)
  }

  /**
   * 获取可见的主导航模块
   */
  function getMainNav(): ModuleConfig[] {
    return getVisibleNavModules().filter(m => m.category === 'main')
  }

  /**
   * 获取可见的次级导航模块
   */
  function getSecondaryNav(): ModuleConfig[] {
    return getVisibleNavModules().filter(m => m.category === 'secondary')
  }

  /**
   * 获取首页可见的应用入口（主导航 + 次级导航中适合展示为卡片入口的模块）
   */
  function getVisibleApps(): ModuleConfig[] {
    return getVisibleNavModules().filter(
      m => m.category === 'main' || (m.category === 'secondary' && !['notices'].includes(m.id))
    )
  }

  /**
   * 获取当前角色可见的审批类型
   */
  function getVisibleApprovalTypes(): ApprovalType[] {
    return visibleApprovalTypes.value
  }

  /**
   * 检查某模块是否对当前角色可见
   * @param moduleId - 模块ID
   */
  function isModuleVisible(moduleId: string): boolean {
    const mod = ALL_MODULES.find(m => m.id === moduleId)
    if (!mod) return false
    // roles 为空数组表示所有角色可见
    if (mod.roles.length === 0) return true
    return mod.roles.includes(currentRole.value)
  }

  /**
   * 检查当前角色是否拥有指定权限点
   * @param permission - 权限标识符（如 'approval:review'）
   */
  function hasPermission(permission: string): boolean {
    const allowedRoles = PERMISSION_ROLE_MAP[permission]
    if (!allowedRoles) return false
    return allowedRoles.includes(currentRole.value)
  }

  /**
   * 获取当前角色的中文显示名
   */
  function getCurrentRoleLabel(): string {
    return ROLE_LABELS[currentRole.value] || currentRole.value
  }

  /**
   * 获取所有可用角色列表（用于角色切换等场景）
   */
  function getAllRoles(): { value: EmployeeRole; label: string }[] {
    return (Object.keys(ROLE_LABELS) as EmployeeRole[]).map(role => ({
      value: role,
      label: ROLE_LABELS[role],
    }))
  }

  // ============================================
  // 移动端导航方法（4Tab分类导航体系）
  // ============================================

  /**
   * 获取移动端底部TabBar配置列表（固定返回4个类别Tab）
   * 返回值格式兼容 EmployeeLayout.mobileTabs 的消费方式
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any -- icon 由 getIconComponent() 返回，为 Vue 组件引用
  function getMobileTabs(): { path: string; icon: any; label: string }[] {
    return MOBILE_TABS.map(tab => ({
      path: tab.route,
      icon: getIconComponent(tab.icon),
      label: tab.label,
    }))
  }

  /**
   * 获取办公页面的功能分组列表（从 ALL_MODULES 动态聚合）
   * 按 group 字段自动归组，无需手动维护 moduleIds
   * 与首页 QuickActionsGrid 共享同一功能池（ALL_MODULES）
   */
  function getOfficeGroups(): { group: GroupMeta; modules: ModuleConfig[] }[] {
    // 筛选可在办公页展示的模块（排除 Tab 入口/工具类）
    const officeModules = ALL_MODULES.filter(
      m => !OFFICE_EXCLUDED_IDS.has(m.id) && isModuleVisible(m.id)
    )

    // 按 group 字段聚合
    const grouped = new Map<string, ModuleConfig[]>()
    for (const mod of officeModules) {
      const groupName = mod.group || '其他'
      if (!grouped.has(groupName)) {
        grouped.set(groupName, [])
      }
      grouped.get(groupName)!.push(mod)
    }

    // 匹配分组元数据，按 order 排序返回
    return Array.from(grouped.entries())
      .map(([groupName, modules]) => ({
        group: GROUP_META[groupName] || {
          key: groupName,
          title: groupName,
          icon: 'Grid',
          order: 99,
        },
        modules: modules.sort((a, b) => a.order - b.order),
      }))
      .sort((a, b) => a.group.order - b.group.order)
      .filter(g => g.modules.length > 0)
  }

  /**
   * 获取办公页面所有可见模块的扁平列表（用于首页快捷入口"添加更多"等场景）
   */
  function getAllOfficeModules(): ModuleConfig[] {
    return ALL_MODULES.filter(
      m => !OFFICE_EXCLUDED_IDS.has(m.id) && isModuleVisible(m.id)
    ).sort((a, b) => a.order - b.order)
  }

  // ============================================
  // 层级化权限体系方法
  // ============================================

  /**
   * 审批链路规则配置表
   * 定义不同审批类型在不同发起人层级下的审批流程
   */
  const approvalChainRules: ApprovalChainRule[] = [
    // 请假类：员工→主管→HR归档；主管→经理→HR归档
    {
      type: 'leave',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
        { level: UserLevel.MANAGER, roleName: 'L3经理' },
      ],
    },
    // 加班类：员工→主管→HR备案
    {
      type: 'overtime',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
        { level: UserLevel.MANAGER, roleName: 'L3经理（HR备案）' },
      ],
    },
    // 报销类：根据金额走不同链路
    {
      type: 'reimbursement',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
        { level: UserLevel.REGION_MANAGER, roleName: 'L4区域经理（财务审核）' },
      ],
    },
    // 出差类：员工→主管→经理
    {
      type: 'travel',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
        { level: UserLevel.MANAGER, roleName: 'L3经理' },
      ],
    },
    // 调班类：仅主管审批即可
    {
      type: 'swap',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
      ],
    },
    // 物品领用类：员工→主管
    {
      type: 'requisition',
      minApplicantLevel: UserLevel.STAFF,
      chain: [
        { level: UserLevel.SUPERVISOR, roleName: 'L2主管' },
      ],
    },
  ]

  /**
   * 检查当前用户是否可以审批指定层级的申请
   * @param targetLevel - 目标申请人的层级
   * @returns 是否可以审批
   */
  function canApproveTargetLevel(targetLevel: UserLevel): boolean {
    return checkCanApproveLevel(userLevel.value, targetLevel)
  }

  /**
   * 根据申请人和审批类型生成审批链路
   * @param applicantLevel - 发起人层级
   * @param type - 审批类型
   * @returns 审批节点列表（过滤掉高于当前用户层级的节点）
   */
  function generateChain(applicantLevel: UserLevel, type: string): FlowNodeRole[] {
    const rule = approvalChainRules.find(r => r.type === type)
    if (!rule) return []

    // 过滤出需要经过的审批节点，并标记哪些是当前用户的节点
    return rule.chain
      .filter(node => node.level > applicantLevel)
      .map(node => ({
        ...node,
        isCurrentUserNode: node.level === userLevel.value,
      }))
  }

  /**
   * 获取当前用户可见的最低审批层级
   * 用于数据过滤：只显示由可审批层级发起的待审批单据
   */
  function getMinApprovableLevel(): UserLevel {
    const config = LEVEL_CONFIG_MAP[userLevel.value]
    if (!config?.canApproveLevels.length) return UserLevel.ADMIN + 1 as UserLevel
    return Math.min(...config.canApproveLevels)
  }

  /**
   * [M6] 设置用户层级（供登录流程调用）
   *
   * 身份流转链路中的关键环节：
   *   LoginPage.handleLoginSuccess() → permissionStore.setUserLevel(level)
   *
   * 注意：本方法仅更新 userLevel，不自动同步 currentRole 和 userInfo。
   * 如需完整身份初始化（含角色+用户信息），请使用 initFromApi() 方法。
   *
   * @param level - 层级枚举值
   */
  function setUserLevel(level: UserLevel): void {
    userLevel.value = level
  }

  // ---------- 初始化（使用默认Mock数据）----------
  setRole('store_staff')

  return {
    currentRole,
    userLevel,
    userInfo,
    levelInfo,
    visibleApprovalTypes,
    canApproveOthers,
    // 语义化权限属性
    isFieldStaff,
    isManager,
    isHeadquarters,
    canApprove,
    canViewFinance,
    hasStoreContext,
    // 方法
    setRole,
    initFromApi,
    getVisibleNavModules,
    getMainNav,
    getSecondaryNav,
    getVisibleApps,
    getVisibleApprovalTypes,
    isModuleVisible,
    hasPermission,
    getCurrentRoleLabel,
    getAllRoles,
    // 层级化权限方法
    approvalChainRules,
    canApproveTargetLevel,
    generateChain,
    getMinApprovableLevel,
    setUserLevel,
    // 移动端导航方法
    getMobileTabs,
    getOfficeGroups,
    getAllOfficeModules,
  }
})
