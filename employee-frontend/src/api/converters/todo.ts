/**
 * 待办聚合转换器
 * 负责将各模块的待办事项统一转换为标准 ActionableTodo 格式
 * 遵循规范：禁止在组件中直接做状态映射
 *
 * 设计目标：
 * - 统一各来源待办的数据结构
 * - 根据来源模块自动推断优先级
 * - 提供进度型待办的标准化转换
 */

// ============================================================
// 类型定义
// ============================================================

/** 待办优先级 */
export type TodoPriority = 'urgent' | 'high' | 'medium' | 'low'

/** 待办状态 */
export type TodoStatus = 'pending' | 'in_progress' | 'completed' | 'overdue'

/** 来源模块标识 */
export type TodoSourceModule =
  | 'approval'
  | 'schedule'
  | 'health_cert'
  | 'contract'
  | 'salary'
  | 'inventory'
  | 'purchase'
  | 'finance'
  | 'other'

/** 标准化可操作待办项 */
export interface ActionableTodo {
  /** 唯一标识 */
  id: string
  /** 标题 */
  title: string
  /** 描述 */
  description: string
  /** 来源模块 */
  sourceModule: TodoSourceModule
  /** 优先级 */
  priority: TodoPriority
  /** 截止日期 */
  dueDate: string
  /** 当前状态 */
  status: TodoStatus
  /** 操作链接（路由路径） */
  actionLink: string
  /** 操作按钮文字 */
  actionLabel: string
  /** 图标名称（Element Plus icon） */
  iconName: string
  /** 创建时间 */
  createTime: string
  /** 元数据（用于扩展） */
  meta?: Record<string, unknown>
}

/** 进度型待办项 */
export interface ProgressTodo {
  id: string
  title: string
  sourceModule: TodoSourceModule
  /** 当前进度 0-100 */
  progress: number
  totalSteps: number
  completedSteps: number
  currentStepLabel: string
  status: TodoStatus
  actionLink: string
  createTime: string
}

/** 待办通知（用于FAB展示） */
export interface TodoNotification {
  id: string
  title: string
  message: string
  sourceModule: TodoSourceModule
  priority: TodoPriority
  isRead: boolean
  actionLink: string
  createTime: string
}

// ============================================================
// 模块默认配置
// ============================================================

/** 模块图标映射 */
const MODULE_ICON_MAP: Record<TodoSourceModule, string> = {
  approval: 'Document',
  schedule: 'Calendar',
  health_cert: 'FirstAidKit',
  contract: 'Document',
  salary: 'Money',
  inventory: 'Box',
  purchase: 'ShoppingCart',
  finance: 'TrendCharts',
  other: 'Bell',
}

/** 模块操作标签映射 */
const MODULE_ACTION_LABEL_MAP: Record<TodoSourceModule, string> = {
  approval: '去审批',
  schedule: '查看排班',
  health_cert: '去办理',
  contract: '查看合同',
  salary: '查看薪资',
  inventory: '去盘点',
  purchase: '去处理',
  finance: '查看账目',
  other: '查看详情',
}

// ============================================================
// 导出转换器对象
// ============================================================

export const todoDataConverter = {
  // ----------------------------------------------------------
  // 优先级推断
  // ----------------------------------------------------------

  /**
   * 根据来源模块推断默认优先级
   * 审批类 > 健康证/合同到期 > 薪资/库存 > 其他
   * @param type - 来源模块标识
   * @returns 推断的优先级
   */
  priorityFromType(type: TodoSourceModule): TodoPriority {
    const priorityMap: Partial<Record<TodoSourceModule, TodoPriority>> = {
      approval: 'urgent',
      health_cert: 'high',
      contract: 'high',
      schedule: 'medium',
      salary: 'medium',
      inventory: 'low',
      purchase: 'high',
      finance: 'medium',
    }
    return priorityMap[type] ?? 'low'
  },

  // ----------------------------------------------------------
  // 可操作待办转换
  // ----------------------------------------------------------

  /**
   * 将各模块原始待办转换为标准 ActionableTodo 格式
   * @param raw - 原始待办数据（来自任意模块）
   * @param module - 来源模块标识
   * @returns 标准化的可操作待办
   */
  toActionableTodo(
    raw: Record<string, unknown>,
    module: TodoSourceModule
  ): ActionableTodo {
    // 从原始数据提取截止日期，统一为 YYYY-MM-DD 格式
    let dueDate = (raw.dueDate as string) || ''
    if (!dueDate && raw.deadline) {
      dueDate = String(raw.deadline)
    }

    // 判断是否过期
    let status: TodoStatus = (raw.status as TodoStatus) || 'pending'
    if (status === 'pending' && dueDate) {
      const today = new Date()
      today.setHours(0, 0, 0, 0)
      const due = new Date(dueDate)
      due.setHours(0, 0, 0, 0)
      if (due < today) {
        status = 'overdue'
      }
    }

    return {
      id: (raw.id as string) || '',
      title: (raw.title as string) || (raw.name as string) || '',
      description: (raw.description as string) || (raw.content as string) || '',
      sourceModule: module,
      priority: (raw.priority as TodoPriority) || this.priorityFromType(module),
      dueDate,
      status,
      actionLink: (raw.actionLink as string) || (raw.link as string) || '',
      actionLabel: (raw.actionLabel as string) || MODULE_ACTION_LABEL_MAP[module],
      iconName: (raw.iconName as string) || MODULE_ICON_MAP[module],
      createTime: (raw.createTime as string) || (raw.createdAt as string) || '',
      meta: raw.meta as Record<string, unknown> | undefined,
    }
  },

  /**
   * 批量转换为可操作待办列表
   * @param list - 原始待办数据列表
   * @param module - 来源模块标识
   * @returns 标准化待办数组
   */
  toActionableTodoList(
    list: Record<string, unknown>[],
    module: TodoSourceModule
  ): ActionableTodo[] {
    return list.map(item => this.toActionableTodo(item, module))
  },

  // ----------------------------------------------------------
  // 进度型待办转换
  // ----------------------------------------------------------

  /**
   * 将原始进度数据转换为 ProgressTodo 格式
   * 自动计算百分比进度
   * @param raw - 原始进度数据
   * @returns 进度型待办
   */
  toProgressTodo(raw: Record<string, unknown>): ProgressTodo {
    const total = Number(raw.totalSteps ?? raw.total ?? 1)
    const completed = Number(raw.completedSteps ?? raw.completed ?? 0)
    const progress = total > 0 ? Math.round((completed / total) * 100) : 0

    return {
      id: (raw.id as string) || '',
      title: (raw.title as string) || (raw.name as string) || '',
      sourceModule: (raw.sourceModule as TodoSourceModule) || 'other',
      progress,
      totalSteps: total,
      completedSteps: completed,
      currentStepLabel: (raw.currentStepLabel as string) || (raw.currentStep as string) || '',
      status: (raw.status as TodoStatus) || 'in_progress',
      actionLink: (raw.actionLink as string) || '',
      createTime: (raw.createTime as string) || '',
    }
  },

  /**
   * 批量转换为进度型待办列表
   * @param list - 原始进度数据列表
   * @returns 进度型待办数组
   */
  toProgressTodoList(list: Record<string, unknown>[]): ProgressTodo[] {
    return list.map(item => this.toProgressTodo(item))
  },

  // ----------------------------------------------------------
  // 待办通知转换
  // ----------------------------------------------------------

  /**
   * 将原始通知数据转换为 TodoNotification 格式
   * 用于 FAB 抽屉中的通知展示
   * @param raw - 原始通知数据
   * @returns 待办通知
   */
  toNotification(raw: Record<string, unknown>): TodoNotification {
    const module = (raw.sourceModule as TodoSourceModule) || 'other'

    return {
      id: (raw.id as string) || '',
      title: (raw.title as string) || '',
      message: (raw.message as string) || (raw.content as string) || (raw.description as string) || '',
      sourceModule: module,
      priority: (raw.priority as TodoPriority) || this.priorityFromType(module),
      isRead: Boolean(raw.isRead ?? raw.read ?? false),
      actionLink: (raw.actionLink as string) || (raw.link as string) || '',
      createTime: (raw.createTime as string) || (raw.createdAt as string) || '',
    }
  },

  /**
   * 批量转换为待办通知列表
   * @param list - 原始通知数据列表
   * @returns 待办通知数组
   */
  toNotificationList(list: Record<string, unknown>[]): TodoNotification[] {
    return list.map(item => this.toNotification(item))
  },
}
