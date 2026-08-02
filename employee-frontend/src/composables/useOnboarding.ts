/**
 * useOnboarding - 新员工入职引导流程 Composable（单例模式）
 *
 * 商用级入职引导状态机，基于行业最佳实践（90天入职计划）：
 *   Day 1: 欢迎页 → 功能导览 → 入门必做
 *   Week 1: 团队介绍 → 培训计划
 *   完成: 准备就绪
 *
 * @last-modified 2026-06-05
 */
import { ref, computed } from 'vue'

/** localStorage 存储键 */
const STORAGE_KEY = 'fts_onboarding_completed'

/** 引导步骤定义 */
export interface OnboardingStep {
  id: string
  title: string
  description: string
  icon: string           // Element Plus 图标名称
  /** 主要操作按钮文字 */
  actionText: string
  /** 次要操作（可选） */
  secondaryAction?: { text: string }
}

/** 引导步骤配置（6步商用级流程） */
const ONBOARDING_STEPS: OnboardingStep[] = [
  {
    id: 'welcome',
    title: '欢迎加入',
    description: '欢迎使用连锁餐饮门店员工端。接下来将为你介绍核心功能和工作流程，帮助你快速融入团队。',
    icon: 'CircleCheck',
    actionText: '开始引导',
    secondaryAction: { text: '跳过' },
  },
  {
    id: 'features',
    title: '核心功能',
    description: '员工端集成了日常工作中最常用的功能模块，覆盖考勤、培训、审批等全场景。',
    icon: 'Grid',
    actionText: '下一步',
    secondaryAction: { text: '跳过' },
  },
  {
    id: 'checklist',
    title: '入门必做',
    description: '建议你优先完成以下几项操作，以便正常使用系统各项功能。',
    icon: 'List',
    actionText: '下一步',
    secondaryAction: { text: '稍后再说' },
  },
  {
    id: 'team',
    title: '团队与组织',
    description: '了解你的直属上级和所在团队信息，方便日常协作和沟通。',
    icon: 'UserFilled',
    actionText: '下一步',
    secondaryAction: { text: '跳过' },
  },
  {
    id: 'training',
    title: '培训计划',
    description: '完成必修课程是转正的必要条件。系统已为你规划了学习路径。',
    icon: 'Reading',
    actionText: '我知道了',
    secondaryAction: { text: '稍后再说' },
  },
  {
    id: 'complete',
    title: '准备就绪',
    description: '你已经了解了基本功能和使用方法。如有问题，随时查看"帮助与反馈"页面或联系你的直属上级。',
    icon: 'Trophy',
    actionText: '开始使用',
  },
]

// ============================================
// 模块级单例状态（所有组件共享同一实例）
// ============================================

/** 当前步骤索引（0-based） */
const currentStepIndex = ref(0)

/** 是否显示引导弹窗 */
const visible = ref(false)

// ============================================
// 计算属性（基于模块级状态）
// ============================================

/** 当前步骤数据 */
const currentStep = computed(() => ONBOARDING_STEPS[currentStepIndex.value])

/** 总步骤数 */
const totalSteps = computed(() => ONBOARDING_STEPS.length)

/** 是否为最后一步 */
const isLastStep = computed(() => currentStepIndex.value >= totalSteps.value - 1)

/** 是否为第一步 */
const isFirstStep = computed(() => currentStepIndex.value === 0)

/** 进度百分比 */
const progress = computed(() =>
  Math.round(((currentStepIndex.value + 1) / totalSteps.value) * 100)
)

/** 所有步骤（供模板遍历进度点） */
const steps = computed(() => ONBOARDING_STEPS)

// ============================================
// 方法
// ============================================

/**
 * 检查是否已完成引导
 * @returns true 表示已完成，无需再次展示
 */
function isCompleted(): boolean {
  try {
    return localStorage.getItem(STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

/**
 * 标记引导已完成
 */
function markCompleted(): void {
  try {
    localStorage.setItem(STORAGE_KEY, '1')
  } catch {
    // localStorage 不可用时静默失败
  }
}

/**
 * 重置引导状态（用于测试或重新展示）
 */
function reset(): void {
  try {
    localStorage.removeItem(STORAGE_KEY)
  } catch {
    // 静默失败
  }
  currentStepIndex.value = 0
  visible.value = false
}

/**
 * 启动引导流程
 * @param force - 是否强制启动（忽略已完成状态）
 */
function start(force = false): void {
  if (!force && isCompleted()) return
  currentStepIndex.value = 0
  visible.value = true
}

/** 关闭引导（不标记完成） */
function close(): void {
  visible.value = false
}

/** 进入下一步 */
function nextStep(): void {
  if (isLastStep.value) {
    complete()
    return
  }
  currentStepIndex.value++
}

/** 回到上一步 */
function prevStep(): void {
  if (currentStepIndex.value > 0) {
    currentStepIndex.value--
  }
}

/** 完成引导 */
function complete(): void {
  markCompleted()
  visible.value = false
}

/** 跳过引导 */
function skip(): void {
  markCompleted()
  visible.value = false
}

/**
 * 导出 Composable 函数（返回共享的单例状态）
 * 所有调用方获取的是同一个响应式状态引用
 */
export function useOnboarding() {
  return {
    // 状态
    currentStepIndex,
    visible,
    // 计算属性
    currentStep,
    totalSteps,
    isLastStep,
    isFirstStep,
    progress,
    steps,
    // 方法
    isCompleted,
    markCompleted,
    reset,
    start,
    close,
    nextStep,
    prevStep,
    complete,
    skip,
  }
}
