import { mockDelay, mockLightDelay } from './mock/delays'

// ==================== 类型定义 ====================

/** 任务优先级 */
export type TaskPriority = 'high' | 'medium' | 'low'

/** 任务状态（闭环状态机） */
export type TaskStatus =
  | 'draft'          // 草稿（仅发布端）
  | 'pending'        // 待接收/待处理
  | 'in_progress'    // 进行中
  | 'reviewing'      // 待审核（员工提交后等待审批）
  | 'completed'      // 已完成
  | 'overdue'        // 已逾期
  | 'rejected'       // 已驳回（审核不通过，退回修改）

/** 任务类别 — 决定工作流和参与角色 */
export type TaskCategory =
  | 'daily'          // 日常指派：上级→下属，独立完成
  | 'training'       // 培训考核：HR发布→全员/指定角色→学习→考核→出成绩
  | 'business_trip'  // 出差任务：经理创建→审批→指派→准备→执行→报告→备案
  | 'inventory'      // 盘点任务：店长计划→分配区域→盘点→核对→提交→审核
  | 'assessment'     // 考核任务：绩效/服务质量自评/他评

/** 每种类别对应的工作流阶段 */
export interface WorkflowStage {
  key: string
  label: string
  /** 谁在此阶段操作 */
  actor: 'publisher' | 'assignee' | 'reviewer' | 'collaborator' | 'system'
}

/** 各类别的完整工作流定义 */
export const TASK_WORKFLOWS: Record<TaskCategory, {
  label: string
  icon: string
  description: string
  publisher: string        // 谁可以发布
  receiver: string         // 谁接收
  collaborator?: string    // 协同人（可选）
  filer: string            // 备案者
  stages: WorkflowStage[]
}> = {
  daily: {
    label: '日常指派',
    icon: 'List',
    description: '上级指派的日常工作任务',
    publisher: '直属上级 / 店长',
    receiver: '指定下属',
    filer: '无需备案',
    stages: [
      { key: 'publish',   label: '发布任务',   actor: 'publisher' },
      { key: 'receive',   label: '接收确认',   actor: 'assignee' },
      { key: 'execute',   label: '执行任务',   actor: 'assignee' },
      { key: 'complete',  label: '提交完成',   actor: 'assignee' },
      { key: 'archive',   label: '自动归档',   actor: 'system' },
    ],
  },
  training: {
    label: '培训考核',
    icon: 'TrendCharts',
    description: '培训部发布的培训课程与考核任务',
    publisher: '人力资源部 / 培训负责人',
    receiver: '全体门店员工 或 指定岗位（如新员工/厨师/服务员）',
    collaborator: '培训系统（在线学习平台）',
    filer: '人力资源部（成绩归档）',
    stages: [
      { key: 'create',     label: '创建培训',     actor: 'publisher' },
      { key: 'assign',     label: '批量分配',     actor: 'publisher' },
      { key: 'study',      label: '在线学习',     actor: 'assignee' },
      { key: 'exam',       label: '参加考核',     actor: 'assignee' },
      { key: 'grade',      label: '系统阅卷',     actor: 'system' },
      { key: 'certify',    label: '发放证书',     actor: 'publisher' },
      { key: 'archive',    label: '成绩归档',     actor: 'publisher' },
    ],
  },
  business_trip: {
    label: '出差任务',
    icon: 'Van',
    description: '跨门店/跨区域的出差工作任务',
    publisher: '部门经理 / 区域总监',
    receiver: '指定出差人员',
    collaborator: '行政助理（行程安排）+ 人事（考勤）+ 财务（预支/报销）',
    filer: '财务部 + 人事部',
    stages: [
      { key: 'propose',    label: '发起申请',     actor: 'publisher' },
      { key: 'approve',   label: '上级审批',     actor: 'reviewer' },
      { key: 'assign',    label: '下达任务',     actor: 'publisher' },
      { key: 'prepare',   label: '行前准备',     actor: 'assignee' },
      { key: 'execute',   label: '执行出差',     actor: 'assignee' },
      { key: 'report',    label: '提交报告',     actor: 'assignee' },
      { key: 'reimburse', label: '费用报销',     actor: 'collaborator' },
      { key: 'file',      label: '归档备案',     actor: 'filer' as 'system' },
    ],
  },
  inventory: {
    label: '盘点任务',
    icon: 'Box',
    description: '定期库存盘点与数据核对',
    publisher: '店长 / 库存主管',
    receiver: '门店指定盘点人员（2-3人分组）',
    collaborator: '同组盘点员（交叉核对）',
    filer: '财务部 + 库存管理系统',
    stages: [
      { key: 'plan',      label: '制定计划',     actor: 'publisher' },
      { key: 'assign',    label: '分配区域',     actor: 'publisher' },
      { key: 'count',     label: '实地盘点',     actor: 'assignee' },
      { key: 'verify',    label: '交叉核对',     actor: 'collaborator' },
      { key: 'submit',    label: '提交报表',     actor: 'assignee' },
      { key: 'audit',     label: '主管审核',     actor: 'reviewer' },
      { key: 'sync',      label: '同步系统',     actor: 'system' },
      { key: 'archive',   label: '归档完成',     actor: 'system' },
    ],
  },
  assessment: {
    label: '考核任务',
    icon: 'DataAnalysis',
    description: '周期性绩效考核与服务质量评估',
    publisher: '人事部 / 绩效委员会',
    receiver: '全体员工（按考核周期自动生成）',
    filer: '人事档案系统',
    stages: [
      { key: 'initiate',  label: '启动考核',     actor: 'publisher' },
      { key: 'self',      label: '员工自评',     actor: 'assignee' },
      { key: 'peer',      label: '同事互评',     actor: 'collaborator' },
      { key: 'super',     label: '上级评价',     actor: 'reviewer' },
      { key: 'calibrate', label: '校准打分',     actor: 'publisher' },
      { key: 'feedback',  label: '结果反馈',     actor: 'publisher' },
      { key: 'appeal',    label: '申诉窗口',     actor: 'assignee' },
      { key: 'finalize',  label: '最终归档',     actor: 'system' },
    ],
  },
}

/** 上级指派任务（扩展版） */
export interface AssignedTask {
  id: string
  /** 任务标题 */
  title: string
  /** 任务描述 */
  description: string
  /** 任务类别 */
  category: TaskCategory
  /** 指派人（上级姓名） */
  assignerName: string
  /** 指派人ID */
  assignerId?: string
  /** 截止日期（ISO 8601） */
  deadline: string
  /** 优先级 */
  priority: TaskPriority
  /** 状态 */
  status: TaskStatus
  /** 完成进度 0-100 */
  progress: number
  /** 创建时间 */
  createdAt: string
  /** 当前工作流阶段 */
  currentStage?: string
  /** 子任务列表（如盘点的各区域、培训的各章节） */
  subTasks?: SubTask[]
  /** 附件数量 */
  attachmentCount?: number
  /** 评论数 */
  commentCount?: number
  /** 关联单据号（如出差申请单号、盘点单号） */
  refNo?: string
}

/** 子任务 */
export interface SubTask {
  id: string
  title: string
  status: 'pending' | 'in_progress' | 'completed'
}

/** 发布任务时的入参 */
export interface CreateTaskPayload {
  title: string
  description: string
  category: TaskCategory
  priority: TaskPriority
  deadline: string
  assigneeIds: string[]       // 接收人ID列表
  assigneeNames: string[]     // 接收人姓名列表（前端展示用）
  /** 培训类专用 */
  trainingConfig?: {
    courseId: string
    examRequired: boolean
    passingScore: number
  }
  /** 出差类专用 */
  tripConfig?: {
    destination: string
    startDate: string
    endDate: string
    purpose: string
    budget?: number
  }
  /** 盘点类专用 */
  inventoryConfig?: {
    areas: string[]           // 盘点区域列表
    expectedItems?: number    // 预计SKU数
  }
}

/** 任务模板（快捷创建） */
export interface TaskTemplate {
  id: string
  category: TaskCategory
  title: string
  description: string
  defaultPriority: TaskPriority
  defaultDeadlineDays: number
}

// ==================== 预置任务模板 ====================

export const TASK_TEMPLATES: TaskTemplate[] = [
  // ── 培训考核 ──
  { id: 'tpl-train-food-safety', category: 'training', title: '食品安全培训', description: '食品安全法规与操作规范培训及考核', defaultPriority: 'high', defaultDeadlineDays: 7 },
  { id: 'tpl-train-new-staff', category: 'training', title: '新员工入职培训', description: '公司制度、企业文化、岗位技能综合培训', defaultPriority: 'high', defaultDeadlineDays: 14 },
  { id: 'tpl-train-fire-safety', category: 'training', title: '消防安全培训', description: '消防器材使用、应急疏散演练培训', defaultPriority: 'medium', defaultDeadlineDays: 3 },
  { id: 'tpl-train-service-qa', category: 'training', title: '服务质量标准培训', description: '客户服务流程、投诉处理技巧培训', defaultPriority: 'medium', defaultDeadlineDays: 5 },
  // ── 出差任务 ──
  { id: 'tpl-trip-store-audit', category: 'business_trip', title: '门店巡查', description: '对目标门店进行运营巡查并提交报告', defaultPriority: 'high', defaultDeadlineDays: 3 },
  { id: 'tpl-trip-supplier-visit', category: 'business_trip', title: '供应商考察', description: '前往供应商处进行实地考察评估', defaultPriority: 'medium', defaultDeadlineDays: 2 },
  { id: 'tpl-trip-conference', category: 'business_trip', title: '行业会议参会', description: '代表公司参加行业交流会议', defaultPriority: 'low', defaultDeadlineDays: 3 },
  { id: 'tpl-trip-new-store', category: 'business_trip', title: '新店开业支持', description: '前往新开门店进行运营支持指导', defaultPriority: 'high', defaultDeadlineDays: 7 },
  // ── 盘点任务 ──
  { id: 'tpl-inv-monthly', category: 'inventory', title: '月度库存盘点', description: '全品类食材月度库存盘点', defaultPriority: 'high', defaultDeadlineDays: 1 },
  { id: 'tpl-inv-food-only', category: 'inventory', title: '生鲜食材盘点', description: '生鲜类食材专项盘点（易腐品重点清查）', defaultPriority: 'high', defaultDeadlineDays: 1 },
  { id: 'tpl-inv-equipment', category: 'inventory', title: '设备资产盘点', description: '厨房设备、餐具等固定资产盘点', defaultPriority: 'low', defaultDeadlineDays: 2 },
  { id: 'tpl-inv-random', category: 'inventory', title: '随机抽盘', description: '不定期随机抽取部分品类进行突击盘点', defaultPriority: 'medium', defaultDeadlineDays: 1 },
  // ── 日常指派 ──
  { id: 'tpl-daily-sop-update', category: 'daily', title: 'SOP手册更新', description: '根据最新规范更新操作手册内容', defaultPriority: 'medium', defaultDeadlineDays: 5 },
  { id: 'tpl-daily-clean-check', category: 'daily', title: '卫生检查整改', description: '针对卫生检查发现的问题进行整改', defaultPriority: 'high', defaultDeadlineDays: 2 },
  { id: 'tpl-daily-report', category: 'daily', title: '周报/月报撰写', description: '撰写工作汇报文档', defaultPriority: 'medium', defaultDeadlineDays: 3 },
  // ── 考核任务 ──
  { id: 'tpl-assess-quarterly', category: 'assessment', title: '季度绩效考核', description: 'Qx季度综合绩效自评与他评', defaultPriority: 'high', defaultDeadlineDays: 5 },
  { id: 'tpl-assess-service', category: 'assessment', title: '服务质量评分', description: '客户满意度与服务质量指标评估', defaultPriority: 'medium', defaultDeadlineDays: 7 },
]

// ==================== Mock 数据 ====================

function getFutureDate(daysFromNow: number): string {
  const d = new Date()
  d.setDate(d.getDate() + daysFromNow)
  return d.toISOString()
}

function getPastDate(daysAgo: number): string {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  return d.toISOString()
}

/** 模拟已发布的任务数据（覆盖各类别，演示闭环） */
const MOCK_TASKS: AssignedTask[] = [
  // ═══ 培训考核类 ═══
  {
    id: 'task-train-001',
    title: '6月食品安全培训考核',
    description: '参加《食品安全法》第三章线上培训课程（约45分钟），完成后参加20道题考核，满分100分，80分及格',
    category: 'training',
    assignerName: '培训部·陈老师',
    deadline: getFutureDate(2),
    priority: 'high',
    status: 'in_progress',
    progress: 45,
    createdAt: getPastDate(5),
    currentStage: 'study',
    subTasks: [
      { id: 'sub-1', title: '观看培训视频', status: 'completed' },
      { id: 'sub-2', title: '阅读培训材料', status: 'completed' },
      { id: 'sub-3', title: '完成在线考核', status: 'pending' },
    ],
    refNo: 'TRN-202606-0042',
  },
  // ═══ 出差任务类 ═══
  {
    id: 'task-trip-001',
    title: '中山路新店开业支持',
    description: '前往中山路新开门店进行为期3天的运营支持：协助排班梳理、SOP落地指导、开业前演练验收。需提交《开店支持报告》',
    category: 'business_trip',
    assignerName: '区域总监·刘总',
    deadline: getFutureDate(8),
    priority: 'high',
    status: 'pending',
    progress: 0,
    createdAt: getPastDate(2),
    currentStage: 'prepare',
    subTasks: [
      { id: 'sub-t1', title: '确认行程和住宿', status: 'pending' },
      { id: 'sub-t2', title: '准备培训材料', status: 'pending' },
      { id: 'sub-t3', title: '到达门店现场支持', status: 'pending' },
      { id: 'sub-t4', title: '提交支持报告', status: 'pending' },
    ],
    refNo: 'TRIP-202606-0015',
  },
  // ═══ 盘点任务类 ═══
  {
    id: 'task-inv-001',
    title: '6月月度库存盘点',
    description: '月底全品类食材盘点，负责【冷藏区】和【干货区】两个区域。盘点完成后与系统数据核对差异项，填写《盘点差异说明表》',
    category: 'inventory',
    assignerName: '王主管',
    deadline: getFutureDate(-1),
    priority: 'high',
    status: 'overdue',
    progress: 30,
    createdAt: getPastDate(7),
    currentStage: 'count',
    subTasks: [
      { id: 'sub-i1', title: '冷藏区盘点', status: 'in_progress' },
      { id: 'sub-i2', title: '干货区盘点', status: 'pending' },
      { id: 'sub-i3', title: '差异核对与说明', status: 'pending' },
    ],
    refNo: 'INV-202606-0008',
  },
  // ═══ 日常指派类 ═══
  {
    id: 'task-daily-001',
    title: '更新门店SOP操作手册第3章',
    description: '根据最新发布的《食品安全操作规范v2.3》更新操作手册第3章"食材储存"相关内容',
    category: 'daily',
    assignerName: '李经理',
    deadline: getFutureDate(5),
    priority: 'medium',
    status: 'in_progress',
    progress: 60,
    createdAt: getPastDate(10),
    currentStage: 'execute',
  },
  {
    id: 'task-daily-002',
    title: '后厨卫生检查整改',
    description: '针对6月3日卫生检查发现的3个问题项进行整改：①排水沟清洁 ②调料瓶标签更新 ③冰箱温度记录补全',
    category: 'daily',
    assignerName: '张店长',
    deadline: getFutureDate(1),
    priority: 'high',
    status: 'pending',
    progress: 0,
    createdAt: getPastDate(1),
    currentStage: 'execute',
    subTasks: [
      { id: 'sub-d1', title: '排水沟清洁', status: 'pending' },
      { id: 'sub-d2', title: '调料瓶标签更新', status: 'pending' },
      { id: 'sub-d3', title: '冰箱温度记录补全', status: 'pending' },
    ],
  },
  // ═══ 考核任务类 ═══
  {
    id: 'task-assess-001',
    title: 'Q2 服务质量自评',
    description: '完成第二季度服务质量自评问卷，包含服务态度、响应速度、客户反馈处理等维度',
    category: 'assessment',
    assignerName: '人事部',
    deadline: getFutureDate(3),
    priority: 'medium',
    status: 'pending',
    progress: 0,
    createdAt: getPastDate(3),
    currentStage: 'self',
  },
]

/** 管理端：我发布的任务（模拟数据） */
const MOCK_PUBLISHED_TASKS: AssignedTask[] = [
  {
    id: 'task-pub-001',
    title: '后厨卫生检查整改',
    description: '针对6月3日卫生检查发现的3个问题项进行整改',
    category: 'daily',
    assignerName: '我',
    deadline: getFutureDate(1),
    priority: 'high',
    status: 'pending',
    progress: 0,
    createdAt: getPastDate(1),
    currentStage: 'execute',
  },
  {
    id: 'task-pub-002',
    title: '6月月度库存盘点',
    description: '月底全品类食材盘点，分配给小王（冷藏区+干货区）、小李（常温区+冷冻区）',
    category: 'inventory',
    assignerName: '我',
    deadline: getFutureDate(-1),
    priority: 'high',
    status: 'in_progress',
    progress: 50,
    createdAt: getPastDate(7),
    currentStage: 'count',
  },
]

// ==================== API ====================

/**
 * 任务管理 API（闭环版本）
 *
 * 后端 B-15 就绪后替换为真实接口：
 *
 * 【员工端（接收方）】
 * - GET  /v1/tasks/assigned           我的指派任务列表
 * - GET  /v1/tasks/:id               任务详情
 * - PUT  /v1/tasks/:id/progress      更新任务进度
 * - POST /v1/tasks/:id/complete      提交完成任务
 * - POST /v1/tasks/:id/stage-transition  工作流阶段流转
 *
 * 【管理端（发布方）】
 * - GET  /v1/tasks/published         我发布的任务列表
 * - POST /v1/tasks                   创建并发布任务
 * - PUT  /v1/tasks/:id               修改任务
 * - POST /v1/tasks/:id/cancel        取消任务
 * - GET  /v1/tasks/templates          获取任务模板列表
 * - POST /v1/tasks/:id/review        审核（通过/驳回）
 */
export const taskApi = {
  // ==================== 员工端：接收方 ====================

  /**
   * 获取指派给我的任务列表
   */
  async getAssignedTasks(): Promise<AssignedTask[]> {
    await mockLightDelay()
    return MOCK_TASKS.map(t => ({ ...t }))
  },

  /**
   * 获取任务统计摘要（按类别）
   */
  async getTaskStats(): Promise<{
    total: number
    pending: number
    inProgress: number
    completed: number
    overdue: number
  }> {
    await mockDelay(30, 80)
    const tasks = MOCK_TASKS
    return {
      total: tasks.length,
      pending: tasks.filter(t => t.status === 'pending').length,
      inProgress: tasks.filter(t => t.status === 'in_progress').length,
      completed: tasks.filter(t => t.status === 'completed').length,
      overdue: tasks.filter(t => t.status === 'overdue').length,
    }
  },

  /**
   * 按类别获取统计
   */
  async getCategoryStats(): Promise<Record<TaskCategory, number>> {
    await mockDelay(20, 50)
    const stats: Record<string, number> = {}
    for (const cat of Object.keys(TASK_WORKFLOWS) as TaskCategory[]) {
      stats[cat] = MOCK_TASKS.filter(t => t.category === cat).length
    }
    return stats as Record<TaskCategory, number>
  },

  /**
   * 获取单个任务详情
   */
  async getTaskDetail(taskId: string): Promise<AssignedTask | null> {
    await mockLightDelay()
    return MOCK_TASKS.find(t => t.id === taskId) ? { ...MOCK_TASKS.find(t => t.id === taskId)! } : null
  },

  /**
   * 更新任务进度
   */
  async updateProgress(taskId: string, progress: number): Promise<void> {
    await mockDelay(50, 120)
    const task = [...MOCK_TASKS, ...MOCK_PUBLISHED_TASKS].find(t => t.id === taskId)
    if (task) {
      task.progress = Math.min(100, Math.max(0, progress))
      if (task.progress >= 100) {
        task.status = 'completed'
      } else if (task.progress > 0 && task.status === 'pending') {
        task.status = 'in_progress'
      }
    }
  },

  /**
   * 提交任务完成（触发工作流进入审核阶段）
   */
  async submitComplete(taskId: string): Promise<void> {
    await mockDelay(80, 150)
    const task = MOCK_TASKS.find(t => t.id === taskId)
    if (task) {
      task.progress = 100
      const workflow = TASK_WORKFLOWS[task.category]
      if (workflow.stages.length > 4) {
        task.status = 'reviewing'
        const currentIdx = workflow.stages.findIndex(s => s.key === task.currentStage)
        task.currentStage = workflow.stages[Math.min(currentIdx + 1, workflow.stages.length - 1)].key
      } else {
        task.status = 'completed'
      }
    }
  },

  // ==================== 管理端：发布方 ====================

  /**
   * 获取我发布的任务列表
   */
  async getPublishedTasks(): Promise<AssignedTask[]> {
    await mockLightDelay()
    return MOCK_PUBLISHED_TASKS.map(t => ({ ...t }))
  },

  /**
   * 创建并发布任务
   */
  async createTask(payload: CreateTaskPayload): Promise<AssignedTask> {
    await mockDelay(100, 200)
    const newTask: AssignedTask = {
      id: `task-${Date.now()}`,
      title: payload.title,
      description: payload.description,
      category: payload.category,
      assignerName: '我',
      deadline: payload.deadline,
      priority: payload.priority,
      status: 'pending',
      progress: 0,
      createdAt: new Date().toISOString(),
      currentStage: TASK_WORKFLOWS[payload.category].stages[0]?.key || 'execute',
    }
    MOCK_PUBLISHED_TASKS.unshift(newTask)
    return { ...newTask }
  },

  /**
   * 获取任务模板列表
   */
  async getTemplates(category?: TaskCategory): Promise<TaskTemplate[]> {
    await mockLightDelay()
    if (category) {
      return TASK_TEMPLATES.filter(t => t.category === category)
    }
    return [...TASK_TEMPLATES]
  },

  /**
   * 审核任务（管理者操作：通过/驳回）
   */
  async reviewTask(taskId: string, action: 'approve' | 'reject', comment?: string): Promise<void> {
    await mockDelay(80, 150)
    const task = MOCK_TASKS.find(t => t.id === taskId)
    if (task && task.status === 'reviewing') {
      task.status = action === 'approve' ? 'completed' : 'rejected'
    }
  },

  // ==================== 通用工具 ====================

  /**
   * 计算距离截止日期的剩余时间描述
   */
  getDeadlineText(deadline: string): { text: string; isUrgent: boolean; isOverdue: boolean } {
    const now = new Date()
    const end = new Date(deadline)
    const diffMs = end.getTime() - now.getTime()
    const diffHours = Math.ceil(diffMs / (1000 * 60 * 60))
    const diffDays = Math.floor(diffHours / 24)

    if (diffMs < 0) {
      const overdueDays = Math.abs(diffDays)
      return { text: `已逾期 ${overdueDays} 天`, isUrgent: true, isOverdue: true }
    }
    if (diffDays > 0) {
      return { text: `剩余 ${diffDays} 天`, isUrgent: diffDays <= 2, isOverdue: false }
    }
    if (diffHours > 0) {
      return { text: `剩余 ${diffHours} 小时`, isUrgent: true, isOverdue: false }
    }
    return { text: '今日到期', isUrgent: true, isOverdue: false }
  },

  /**
   * 获取某类别的工作流定义
   */
  getWorkflow(category: TaskCategory) {
    return TASK_WORKFLOWS[category]
  },

  /**
   * 获取所有类别的工作流定义
   */
  getAllWorkflows() {
    return TASK_WORKFLOWS
  },
}
