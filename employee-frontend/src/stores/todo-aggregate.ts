import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ActionableTodo, ProgressTodo, TodoNotification } from '@/types/todo'

/**
 * FAB待办聚合 Pinia Store
 *
 * 从多个业务模块聚合待办事项，供FAB按钮（浮动操作按钮）的抽屉面板展示。
 * 包含5分钟缓存机制，避免频繁请求。
 * 后端API未实现时自动fallback到mock数据。
 */

// ===== 缓存配置 =====
const CACHE_DURATION = 5 * 60 * 1000 // 5分钟

export const useTodoAggregateStore = defineStore('todoAggregate', () => {
  // ===== State =====
  /** 可执行待办列表（需要用户操作的） */
  const actionableTodos = ref<ActionableTodo[]>([])
  /** 进行中待办列表（仅展示进度） */
  const progressTodos = ref<ProgressTodo[]>([])
  /** 待办通知列表（纯提示类） */
  const notifications = ref<TodoNotification[]>([])
  /** FAB抽屉可见性 */
  const drawerVisible = ref(false)
  /** 加载状态 */
  const isLoading = ref(false)
  /** 上次拉取时间戳 */
  const lastFetchTime = ref<number | null>(null)

  // ===== Computed =====

  /** 可执行待办总数（用于FAB角标） */
  const totalActionableCount = computed((): number => {
    return actionableTodos.value.length
  })

  // ===== 内部方法 =====

  /** 判断是否需要刷新数据（超过缓存时间） */
  function shouldRefresh(): boolean {
    if (!lastFetchTime.value) return true
    return Date.now() - lastFetchTime.value > CACHE_DURATION
  }

  /**
   * 生成mock可执行待办数据
   * 当后端API不可用时作为fallback
   */
  function getMockActionableTodos(): ActionableTodo[] {
    return [
      {
        id: 'mock-approval-1',
        sourceModule: 'approval',
        title: '请假审批',
        description: '事假1天，2026-06-02',
        urgency: 'high',
        createTime: new Date(Date.now() - 3600000).toISOString(),
        deadline: '2026-06-02T18:00:00',
        businessId: 'approval-001',
      },
      {
        id: 'mock-training-1',
        sourceModule: 'training',
        title: '食品安全培训完成确认',
        description: '新员工入职培训第3课',
        urgency: 'low',
        createTime: new Date(Date.now() - 86400000).toISOString(),
        businessId: 'training-001',
      },
    ]
  }

  /**
   * 生成mock进行中待办数据
   */
  function getMockProgressTodos(): ProgressTodo[] {
    return [
      {
        id: 'mock-progress-1',
        sourceModule: 'onboarding',
        title: '李四入职流程',
        progress: 65,
        progressLabel: '已完成3/5步骤',
      },
      {
        id: 'mock-progress-2',
        sourceModule: 'contract',
        title: '劳动合同续签',
        progress: 30,
        progressLabel: '等待HR审核',
      },
    ]
  }

  /**
   * 生成mock通知数据
   */
  function getMockNotifications(): TodoNotification[] {
    return [
      {
        id: 'mock-notif-1',
        title: '排班变更提醒',
        content: '您下周三的班次已从早班调整为晚班',
        type: 'info',
        createTime: new Date(Date.now() - 1800000).toISOString(),
        isRead: false,
      },
      {
        id: 'mock-notif-2',
        title: '培训即将开始',
        content: '消防安全培训将于明天14:00在会议室A举行',
        type: 'warning',
        createTime: new Date(Date.now() - 3600000).toISOString(),
        isRead: false,
      },
    ]
  }

  // ===== Actions =====

  /**
   * 从各模块聚合所有待办数据
   * 使用Promise.allSettled确保单个模块失败不影响其他模块
   */
  async function fetchAllTodos(): Promise<void> {
    isLoading.value = true
    try {
      // 尝试调用真实API
      let hasRealData = false

      try {
        // 动态导入避免循环依赖
        // 注意：financeApprovalApi / onboardingApprovalApi 为规划中的模块API，当前阶段使用 mock 回退
        const approvalModule = await import('@/api/approval') as Record<string, unknown>
        const financeApprovalApi = approvalModule.financeApprovalApi as { getPendingApprovals: (_u: string) => Promise<unknown[]> } | undefined
        const onboardingApprovalApi = approvalModule.onboardingApprovalApi as { getPending: () => Promise<unknown[]> } | undefined

        const results = await Promise.allSettled([
          financeApprovalApi?.getPendingApprovals('current-user').catch(() => []) ?? Promise.resolve([]),
          onboardingApprovalApi?.getPending().catch(() => []) ?? Promise.resolve([]),
        ])

        // 如果任一API返回了有效数据，标记为有真实数据
        hasRealData = results.some(
          (r) => r.status === 'fulfilled' && Array.isArray(r.value) && r.value.length > 0
        )

        // 将API结果转换为ActionableTodo格式
        const mapped: ActionableTodo[] = []

        if (results[0].status === 'fulfilled' && Array.isArray(results[0].value)) {
          ;(results[0].value as Array<{ approvalId: string; title: string; status: string; createTime: string }>).forEach(
            (item) => {
              mapped.push({
                id: item.approvalId,
                sourceModule: 'approval',
                title: item.title || '待审批事项',
                urgency: 'medium',
                createTime: item.createTime,
                businessId: item.approvalId,
              })
            }
          )
        }

        if (results[1].status === 'fulfilled' && Array.isArray(results[1].value)) {
          ;(results[1].value as Array<{ approvalId: string; candidateName: string; position: string; createTime: string }>).forEach(
            (item) => {
              mapped.push({
                id: item.approvalId,
                sourceModule: 'approval',
                title: `${item.candidateName}的入职审批`,
                description: `${item.position}`,
                urgency: 'high',
                createTime: item.createTime,
                businessId: item.approvalId,
              })
            }
          )
        }

        actionableTodos.value = mapped.length > 0 ? mapped : getMockActionableTodos()
      } catch (apiError) {
        // API导入或调用完全失败时使用mock数据
        actionableTodos.value = getMockActionableTodos()
      }

      // 进度类和通知类暂无对应API，统一使用mock
      progressTodos.value = getMockProgressTodos()
      notifications.value = getMockNotifications()

      lastFetchTime.value = Date.now()
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 打开FAB抽屉面板
   * 自动判断是否需要刷新数据
   */
  function openDrawer(): void {
    if (shouldRefresh()) {
      fetchAllTodos()
    }
    drawerVisible.value = true
  }

  /**
   * 关闭FAB抽屉面板
   */
  function closeDrawer(): void {
    drawerVisible.value = false
  }

  return {
    // State
    actionableTodos,
    progressTodos,
    notifications,
    drawerVisible,
    isLoading,
    lastFetchTime,
    // Computed
    totalActionableCount,
    // Actions
    fetchAllTodos,
    openDrawer,
    closeDrawer,
    shouldRefresh,
  }
})
