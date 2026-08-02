/**
 * useEmployeeLifecycle - 员工生命周期管理 Composable（单例模式）
 *
 * 管理员工入职/离职相关的手续状态：
 *   - 入职手续：资料完善、首次打卡、必修培训、设备领取
 *   - 离职手续：工作交接、资产归还、离职证明
 *
 * 数据来源：localStorage（开发阶段）→ 后端 API（生产环境）
 *
 * @last-modified 2026-06-05
 */
import { ref, computed } from 'vue'

/** localStorage 存储键 */
const STORAGE_KEY = 'fts_lifecycle_tasks'

// ============================================
// 类型定义
// ============================================

/** 手续任务状态 */
export type TaskStatus = 'pending' | 'in_progress' | 'completed' | 'skipped'

/** 手续类型 */
export type LifecyclePhase = 'onboarding' | 'offboarding' | 'ongoing'

/** 手续任务定义 */
export interface LifecycleTask {
  id: string
  title: string
  description: string
  phase: LifecyclePhase
  status: TaskStatus
  /** 关联路由（可跳转完成） */
  route?: string
  /** 截止日期 */
  deadline?: string
  /** 是否必须完成 */
  required: boolean
  /** 图标名称（Element Plus icon） */
  icon: string
}

// ============================================
// 默认任务配置
// ============================================

/** 入职阶段任务 */
const ONBOARDING_TASKS: Omit<LifecycleTask, 'status'>[] = [
  {
    id: 'profile_complete',
    title: '完善个人资料',
    description: '填写基本信息以便同事识别',
    phase: 'onboarding',
    required: true,
    icon: 'User',
    route: '/settings/account',
  },
  {
    id: 'first_punch',
    title: '完成首次打卡',
    description: '设置打卡位置并完成签到',
    phase: 'onboarding',
    required: true,
    icon: 'AlarmClock',
    route: '/attendance',
  },
  {
    id: 'required_training',
    title: '完成必修培训',
    description: '通过食品安全、操作规范等课程考核',
    phase: 'onboarding',
    required: true,
    icon: 'Reading',
    route: '/training',
  },
  {
    id: 'policy_confirm',
    title: '确认员工手册',
    description: '阅读并确认了解公司规章制度',
    phase: 'onboarding',
    required: false,
    icon: 'Document',
  },
]

/** 在职期间任务 */
const ONGOING_TASKS: Omit<LifecycleTask, 'status'>[] = [
  {
    id: 'attendance_regular',
    title: '保持正常出勤',
    description: '按时完成每日考勤打卡',
    phase: 'ongoing',
    required: true,
    icon: 'Calendar',
    route: '/attendance',
  },
  {
    id: 'training_update',
    title: '年度培训更新',
    description: '完成年度安全复训课程',
    phase: 'ongoing',
    required: true,
    icon: 'DataLine',
    route: '/training',
  },
]

/** 离职阶段任务（预置，激活时加载） */
const OFFBOARDING_TASKS: Omit<LifecycleTask, 'status'>[] = [
  {
    id: 'handover_work',
    title: '工作交接',
    description: '移交手头工作文件和未完成任务',
    phase: 'offboarding',
    required: true,
    icon: 'Switch',
  },
  {
    id: 'return_assets',
    title: '资产归还',
    description: '归还工服、工牌等公司资产',
    phase: 'offboarding',
    required: true,
    icon: 'Box',
  },
  {
    id: 'exit_interview',
    title: '离职面谈',
    description: '与HR进行离职面谈',
    phase: 'offboarding',
    required: false,
    icon: 'ChatDotRound',
  },
  {
    id: 'exit_certificate',
    title: '获取离职证明',
    description: '下载或领取离职证明文件',
    phase: 'offboarding',
    required: false,
    icon: 'Tickets',
  },
]

// ============================================
// 模块级状态
// ============================================

/** 所有任务列表（含状态） */
const tasks = ref<LifecycleTask[]>([])

/** 当前生命周期阶段 */
const currentPhase = ref<LifecyclePhase>('onboarding')

/** 是否已初始化 */
let _initialized = false

// ============================================
// 计算属性
// ============================================

/** 按阶段分组的任务 */
const tasksByPhase = computed(() => ({
  onboarding: tasks.value.filter(t => t.phase === 'onboarding'),
  ongoing: tasks.value.filter(t => t.phase === 'ongoing'),
  offboarding: tasks.value.filter(t => t.phase === 'offboarding'),
}))

/** 入职任务完成进度 */
const onboardingProgress = computed(() => {
  const all = tasksByPhase.value.onboarding
  if (all.length === 0) return 0
  const done = all.filter(t => t.status === 'completed').length
  return Math.round((done / all.length) * 100)
})

/** 入职必做任务是否全部完成 */
const isOnboardingComplete = computed(() => {
  return tasksByPhase.value.onboarding
    .filter(t => t.required)
    .every(t => t.status === 'completed')
})

/** 待办数量 */
const pendingCount = computed(() =>
  tasks.value.filter(t => t.status === 'pending' || t.status === 'in_progress').length
)

// ============================================
// 方法
// ============================================

/**
 * 初始化任务列表（从 localStorage 加载或使用默认值）
 */
function initTasks(): void {
  if (_initialized) return

  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) {
      const saved = JSON.parse(raw)
      if (Array.isArray(saved)) {
        tasks.value = saved
        _initialized = true
        // 根据任务状态推断当前阶段
        updatePhase()
        return
      }
    }
  } catch {
    // 解析失败，使用默认值
  }

  // 首次使用：加载默认入职任务
  resetToOnboarding()
  _initialized = true
}

/** 重置为入职阶段 */
function resetToOnboarding(): void {
  tasks.value = [...ONBOARDING_TASKS, ...ONGOING_TASKS].map(t => ({
    ...t,
    status: 'pending' as TaskStatus,
  }))
  currentPhase.value = 'onboarding'
  persist()
}

/** 激活离职流程 */
function activateOffboarding(): void {
  // 添加离职任务（如果不存在）
  const existingIds = new Set(tasks.value.map(t => t.id))
  const newOffboardingTasks = OFFBOARDING_TASKS
    .filter(t => !existingIds.has(t.id))
    .map(t => ({ ...t, status: 'pending' as TaskStatus }))

  tasks.value = [...tasks.value, ...newOffboardingTasks]
  currentPhase.value = 'offboarding'
  persist()
}

/** 更新任务状态 */
function updateTaskStatus(taskId: string, status: TaskStatus): void {
  const task = tasks.value.find(t => t.id === taskId)
  if (task) {
    task.status = status
    persist()
    updatePhase()
  }
}

/** 标记任务完成 */
function completeTask(taskId: string): void {
  updateTaskStatus(taskId, 'completed')
}

/** 跳过非必需任务 */
function skipTask(taskId: string): void {
  const task = tasks.value.find(t => t.id === taskId)
  if (task && !task.required) {
    updateTaskStatus(taskId, 'skipped')
  }
}

/** 根据任务状态推断当前阶段 */
function updatePhase(): void {
  // 如果有离职任务且处于 pending/in_progress，则视为离职中
  const hasActiveOffboarding = tasks.value.some(
    t => t.phase === 'offboarding' && (t.status === 'pending' || t.status === 'in_progress')
  )
  if (hasActiveOffboarding) {
    currentPhase.value = 'offboarding'
    return
  }

  // 如果入职必做任务都完成了，进入在职阶段
  if (isOnboardingComplete.value) {
    currentPhase.value = 'ongoing'
    return
  }

  currentPhase.value = 'onboarding'
}

/** 持久化到 localStorage */
function persist(): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tasks.value))
  } catch {
    // 静默失败
  }
}

/** 清除所有数据（用于测试） */
function clear(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 静默失败
  }
  tasks.value = []
  currentPhase.value = 'onboarding'
  _initialized = false
}

/**
 * 导出 Composable 函数
 */
export function useEmployeeLifecycle() {
  // 自动初始化
  if (!_initialized) {
    initTasks()
  }

  return {
    // 状态
    tasks,
    currentPhase,
    // 计算属性
    tasksByPhase,
    onboardingProgress,
    isOnboardingComplete,
    pendingCount,
    // 方法
    initTasks,
    resetToOnboarding,
    activateOffboarding,
    updateTaskStatus,
    completeTask,
    skipTask,
    clear,
  }
}
