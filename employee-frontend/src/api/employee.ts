/**
 * 员工端聚合 API
 * 封装 /v1/employee/* 聚合接口调用
 * 当管理端 B-01（员工端聚合网关）就绪后，此处切换为真实接口
 * 当前阶段使用各模块独立 API 的组合作为 Mock 回退
 *
 * 设计原则：
 * - 一次请求返回多个模块的聚合数据，减少前端请求数
 * - 数据已按权限过滤（self/store），前端无需二次过滤
 * - 与管理端 B-01 接口契约对齐
 */

import request from './request'
import { approvalApi } from './approval'
import { scheduleApi } from './schedule'
import type { ApprovalItem } from '@/types/approval'
import type { ActionableTodo, TodoNotification } from '@/api/converters/todo'
import { todoDataConverter } from '@/api/converters/todo'

// ============================================================
// 类型定义（与后端 /v1/employee/* 契约对齐）
// ============================================================

/** 员工仪表盘聚合数据 */
export interface EmployeeDashboardData {
  /** 待办统计 */
  pendingCount: number
  /** 今日待审批数 */
  todayApprovalCount: number
  /** 今日排班班次 */
  todayShift: {
    shiftType: string
    shiftName: string
    startTime: string
    endTime: string
  } | null
  /** 未读通知数 */
  unreadNoticeCount: number
  /** 未读消息数 */
  unreadMessageCount: number
  /** 本月考勤异常数 */
  attendanceAbnormalCount: number
}

/** 员工菜单配置（由管理端权限模板决定） */
export interface EmployeeMenuConfig {
  /** 可见的功能模块列表 */
  visibleModules: string[]
  /** 各模块的子功能权限 */
  modulePermissions: Record<string, string[]>
}

/** 员工审批配置（审批链路、金额限制等） */
export interface EmployeeApprovalConfig {
  /** 是否可审批 */
  canApprove: boolean
  /** 最大审批金额（分） */
  maxAmount?: number
  /** 可审批的用户层级列表 */
  approvableLevels: number[]
  /** 完整审批链路描述 */
  chain: Array<{ level: number; roleName: string }>
}

/** 员工通知摘要 */
export interface EmployeeNotificationSummary {
  /** 总未读数 */
  totalUnread: number
  /** 公告未读数 */
  noticeUnread: number
  /** 系统消息未读数 */
  systemUnread: number
  /** 最新3条通知预览 */
  recentItems: Array<{
    id: string
    title: string
    type: 'notice' | 'system' | 'approval'
    isRead: boolean
    createdAt: string
  }>
}

/** 员工待办聚合响应 */
export interface EmployeeTodoAggregateResponse {
  /** 需要我操作的待办 */
  actionableTodos: ActionableTodo[]
  /** 进度型待办 */
  progressTodos: Array<{
    id: string
    title: string
    progress: number
    status: string
  }>
  /** 通知类待办（FAB 抽屉用） */
  notifications: TodoNotification[]
  /** 统计摘要 */
  summary: {
    totalPending: number
    urgentCount: number
    overdueCount: number
  }
}

// ============================================================
// API 实现
// ============================================================

export const employeeApi = {
  // ----------------------------------------------------------
  // 仪表盘聚合（B-01 核心接口）
  // ----------------------------------------------------------

  /**
   * 获取员工工作台仪表盘数据
   *
   * 后端就绪时替换为:
   *   GET /v1/employee/dashboard
   *
   * 当前回退策略：并行请求多个独立 API 并聚合
   */
  async getDashboard(): Promise<EmployeeDashboardData> {
    try {
      // ★ 后端 B-01 就绪后，改为单次请求：
      // return request.get<EmployeeDashboardData>('/v1/employee/dashboard')

      // --- 当前：Mock 阶段并行聚合 ---
      const [statsResult, scheduleData] = await Promise.allSettled([
        approvalApi.getStats(),
        scheduleApi.getTodaySchedule(),
      ])

      const stats = statsResult.status === 'fulfilled' ? statsResult.value : undefined
      const todaySchedule = scheduleData.status === 'fulfilled' ? scheduleData.value : null

      return {
        pendingCount: stats?.pendingCount ?? 0,
        todayApprovalCount: stats?.pendingCount ?? 0,
        todayShift: todaySchedule
          ? {
              shiftType: todaySchedule.shiftType,
              shiftName: todaySchedule.shiftName,
              startTime: todaySchedule.startTime,
              endTime: todaySchedule.endTime,
            }
          : null,
        unreadNoticeCount: 0,
        unreadMessageCount: 0,
        attendanceAbnormalCount: 0,
      }
    } catch {
      // 降级：返回空数据而非报错
      return {
        pendingCount: 0,
        todayApprovalCount: 0,
        todayShift: null,
        unreadNoticeCount: 0,
        unreadMessageCount: 0,
        attendanceAbnormalCount: 0,
      }
    }
  },

  // ----------------------------------------------------------
  // 菜单配置（B-03 关联接口）
  // ----------------------------------------------------------

  /**
   * 获取员工端可见菜单配置
   *
   * 后端就绪时替换为:
   *   GET /v1/employee/menu
   *
   * 返回值由管理端权限模板（B-07）决定
   */
  async getMenuConfig(): Promise<EmployeeMenuConfig> {
    try {
      // ★ 后端 B-03+B-07 就绪后，改为：
      // return request.get<EmployeeMenuConfig>('/v1/employee/menu')

      // --- 当前：默认全部开放（Mock 阶段） ---
      return {
        visibleModules: [
          'home', 'approval', 'schedule', 'salary',
          'leave', 'messages', 'training', 'office',
          'review', 'reports', 'profile',
        ],
        modulePermissions: {
          approval: ['view', 'create', 'withdraw'],
          schedule: ['view_personal', 'view_team'],
          salary: ['view_own'],
          leave: ['view_calendar', 'create_leave'],
        },
      }
    } catch {
      return {
        visibleModules: ['home', 'approval', 'profile'],
        modulePermissions: {},
      }
    }
  },

  // ----------------------------------------------------------
  // 审批配置（B-02 关联接口）
  // ----------------------------------------------------------

  /**
   * 获取当前用户的审批能力配置
   *
   * 后端就绪时替换为:
   *   GET /v1/employee/approval-config
   *
   * 返回值来自管理端角色层级 + 审批链路配置
   */
  async getApprovalConfig(): Promise<EmployeeApprovalConfig> {
    try {
      // ★ 后端 B-02 就绪后，改为：
      // return request.get<EmployeeApprovalConfig>('/v1/employee/approval-config')

      // --- 当前：从 useAuthContext 派生（Mock 阶段） ---
      const { useAuthContext } = await import('@/composables/useAuthContext')
      const auth = useAuthContext()

      if (!auth.canApprove()) {
        return {
          canApprove: false,
          approvableLevels: [],
          chain: [],
        }
      }

      // 根据 UserLevel 返回对应审批配置
      const level = auth.userLevel.value
      const LEVEL_CONFIGS: Record<number, EmployeeApprovalConfig> = {
        3: { // MANAGER
          canApprove: true,
          maxAmount: 20000,
          approvableLevels: [1, 2],
          chain: [
            { level: 1, roleName: '普通员工' },
            { level: 2, roleName: '主管' },
            { level: 3, roleName: '店长' },
          ],
        },
        4: { // REGION_MANAGER
          canApprove: true,
          maxAmount: 100000,
          approvableLevels: [1, 2, 3],
          chain: [
            { level: 1, roleName: '普通员工' },
            { level: 2, roleName: '主管' },
            { level: 3, roleName: '店长' },
            { level: 4, roleName: '区域经理' },
          ],
        },
        5: { // ADMIN
          canApprove: true,
          approvableLevels: [1, 2, 3, 4],
          chain: [
            { level: 1, roleName: '普通员工' },
            { level: 2, roleName: '主管' },
            { level: 3, roleName: '店长' },
            { level: 4, roleName: '区域经理' },
            { level: 5, roleName: '超级管理员' },
          ],
        },
      }

      return LEVEL_CONFIGS[level] ?? { canApprove: false, approvableLevels: [], chain: [] }
    } catch {
      return { canApprove: false, approvableLevels: [], chain: [] }
    }
  },

  // ----------------------------------------------------------
  // 通知摘要（B-04 关联接口）
  // ----------------------------------------------------------

  /**
   * 获取员工通知摘要
   *
   * 后端就绪时替换为:
   *   GET /v1/employee/notification-summary
   */
  async getNotificationSummary(): Promise<EmployeeNotificationSummary> {
    try {
      // ★ 后端 B-04 就绪后，改为：
      // return request.get<EmployeeNotificationSummary>('/v1/employee/notification-summary')

      return {
        totalUnread: 0,
        noticeUnread: 0,
        systemUnread: 0,
        recentItems: [],
      }
    } catch {
      return { totalUnread: 0, noticeUnread: 0, systemUnread: 0, recentItems: [] }
    }
  },

  // ----------------------------------------------------------
  // 待办聚合（首页 FAB 用）
  // ----------------------------------------------------------

  /**
   * 获取员工待办事项聚合数据
   *
   * 后端就绪时替换为:
   *   GET /v1/employee/todos
   */
  async getTodoAggregate(): Promise<EmployeeTodoAggregateResponse> {
    try {
      // ★ 后端 B-01 就绪后，改为单次请求

      // --- 当前：从审批模块获取 ---
      const listResult = await approvalApi.getList({ tab: 'pending', page: 1, size: 10 })
      const records = listResult.records || []

      const actionableTodos = records.map(item =>
        todoDataConverter.toActionableTodo(
          {
            id: item.id,
            title: `${item.applicantName}的${item.title}`,
            description: item.summary,
            dueDate: '',
            status: item.status === 'pending' ? 'pending' : 'in_progress',
            actionLink: `/approval/${item.id}`,
          },
          'approval'
        )
      )

      return {
        actionableTodos,
        progressTodos: [],
        notifications: [],
        summary: {
          totalPending: listResult.total || 0,
          urgentCount: records.filter(r => r.urgency === 'urgent').length,
          overdueCount: 0,
        },
      }
    } catch {
      return {
        actionableTodos: [],
        progressTodos: [],
        notifications: [],
        summary: { totalPending: 0, urgentCount: 0, overdueCount: 0 },
      }
    }
  },
}
