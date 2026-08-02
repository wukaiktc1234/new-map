<script setup lang="ts">
/**
 * TodoSummaryCard - 三合一待办事项汇总卡片
 *
 * 整合审批、指派任务、考核三类待办，通过Tab切换。
 * 视觉区分：审批(蓝色) / 指派(橙色+截止倒计时) / 考核(紫色)
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowRight, Document, List, TrendCharts, Close, User } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { getApprovalStats, getApprovalsByTab, seedApprovalData } from '@/api/mock'
import { taskApi } from '@/api/task'
import type { ApprovalItem } from '@/types/approval'
import type { AssignedTask } from '@/api/task'

const router = useRouter()

// ==================== Tab 类型 ====================
type TodoTabType = 'approval' | 'task' | 'review'

interface TabConfig {
  key: TodoTabType
  label: string
  icon: typeof Document
  colorVar: string // CSS变量用于边框颜色
}

const TABS: TabConfig[] = [
  { key: 'approval', label: '待审批', icon: Document, colorVar: '--fts-primary' },
  { key: 'task', label: '工作任务', icon: List, colorVar: '--fts-warning' },
  { key: 'review', label: '考核', icon: TrendCharts, colorVar: '--fts-info' },
]

// ==================== 状态 ====================
const loading = ref(false)
const activeTab = ref<TodoTabType>('approval')

// ── 审批数据（原有）──
const pendingApprovals = ref<ApprovalItem[]>([])
const approvalStats = ref({ total: 0, pending: 0, processing: 0, approved: 0, rejected: 0 })

// ── 指派任务数据（新增）──
const assignedTasks = ref<AssignedTask[]>([])
const taskStats = ref({ total: 0, pending: 0, inProgress: 0, overdue: 0 })

// ── 考核数据（占位）──
interface ReviewTodo {
  id: string
  title: string
  status: 'pending' | 'in_progress' | 'completed'
  deadline: string
}
const reviewTodos = ref<ReviewTodo[]>([])

// ── 任务抽屉状态（闭环）──
const taskDrawerVisible = ref(false)

// ==================== 计算属性 ====================

/** 当前激活Tab的配置 */
const activeTabConfig = computed(() =>
  TABS.find(t => t.key === activeTab.value) || TABS[0],
)

/** 待处理总数（用于角标） */
const totalCount = computed(() => {
  switch (activeTab.value) {
    case 'approval': return approvalStats.value.pending + approvalStats.value.processing
    case 'task': return taskStats.value.pending + taskStats.value.inProgress + taskStats.value.overdue
    case 'review': return reviewTodos.value.filter(r => r.status !== 'completed').length
    default: return 0
  }
})

/** 截止时间文本（仅task tab使用） */
function getDeadlineInfo(task: AssignedTask) {
  return taskApi.getDeadlineText(task.deadline)
}

/** 获取各Tab的待处理数量 */
function getTabCount(key: string): number {
  switch (key) {
    case 'approval': return approvalStats.value.pending + approvalStats.value.processing
    case 'task': return taskStats.value.pending + taskStats.value.inProgress + taskStats.value.overdue
    case 'review': return reviewTodos.value.filter(r => r.status !== 'completed').length
    default: return 0
  }
}

// ==================== 数据获取 ====================

async function fetchData() {
  loading.value = true
  try {
    await Promise.all([fetchApprovalData(), fetchTaskData(), fetchReviewData()])
  } finally {
    loading.value = false
  }
}

function fetchApprovalData() {
  seedApprovalData()
  const all = getApprovalsByTab('role', '')
  pendingApprovals.value = all
    .filter(a => a.status === 'pending' || a.status === 'processing')
    .slice(0, 5)

  const s = getApprovalStats()
  const allItems = getApprovalsByTab('role', '')
  approvalStats.value = {
    total: allItems.length,
    pending: allItems.filter(a => a.status === 'pending').length,
    processing: allItems.filter(a => a.status === 'processing').length,
    approved: allItems.filter(a => a.status === 'approved').length,
    rejected: allItems.filter(a => a.status === 'rejected').length,
  }
}

async function fetchTaskData() {
  const [tasks, stats] = await Promise.all([
    taskApi.getAssignedTasks(),
    taskApi.getTaskStats(),
  ])
  assignedTasks.value = tasks.slice(0, 5)
  taskStats.value = stats
}

async function fetchReviewData() {
  // Mock 考核待办（后续对接 review API）
  const now = new Date()
  reviewTodos.value = [
    {
      id: 'rev-todo-1',
      title: 'Q2 服务质量自评',
      status: 'pending',
      deadline: new Date(now.getTime() + 3 * 86400000).toISOString(),
    },
    {
      id: 'rev-todo-2',
      title: '5月绩效考核确认',
      status: 'in_progress',
      deadline: new Date(now.getTime() + 1 * 86400000).toISOString(),
    },
  ]
}

// ==================== 事件处理 ====================

function handleTabChange(tabKey: TodoTabType) {
  activeTab.value = tabKey
}

function handleApprovalClick(todo: ApprovalItem) {
  router.push(`/approval/detail/${todo.id}`)
}

function handleTaskClick(task: AssignedTask) {
  // 点击单个任务也打开抽屉并高亮
  taskDrawerVisible.value = true
}

function handleReviewClick(todo: ReviewTodo) {
  router.push('/review')
}

function handleViewAll() {
  switch (activeTab.value) {
    case 'approval': return router.push('/approval?tab=role')
    case 'task': return router.push('/tasks')
    case 'review': return router.push('/review')
  }
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <section class="todo-summary-card" :class="`todo-summary-card--${activeTab}`">
    <!-- 头部：标题 + 角标 -->
    <header class="todo-header">
      <h3 class="todo-title">
        <el-icon class="title-icon">
          <component :is="activeTabConfig.icon" />
        </el-icon>
        待办中心
        <span v-if="totalCount > 0" class="count-badge">{{ totalCount }}</span>
      </h3>
    </header>

    <!-- 三合一 Tab 切换 -->
    <nav class="todo-tabs">
      <button
        v-for="tab in TABS"
        :key="tab.key"
        class="todo-tab"
        :class="{ 'todo-tab--active': activeTab === tab.key }"
        :style="{
          '--tab-color': tab.colorVar,
          borderColor: activeTab === tab.key ? tab.colorVar : undefined,
          color: activeTab === tab.key ? tab.colorVar : undefined,
        }"
        @click="handleTabChange(tab.key)"
      >
        {{ tab.label }}
        <!-- 各Tab计数 -->
        <span v-if="getTabCount(tab.key) > 0" class="tab-count">{{ getTabCount(tab.key) }}</span>
      </button>
    </nav>

    <!-- ═══ 审批 Tab 内容 ═══ -->
    <div v-if="activeTab === 'approval'" class="todo-content">
      <div class="todo-stats">
        <div class="stat-item stat-item--clickable" @click="router.push({ path: '/approval', query: { tab: 'role', status: 'processing' } })">
          <StatusTag status="processing" variant="badge" size="small" />
          <span class="stat-value">{{ approvalStats.processing }}</span>
        </div>
        <div class="stat-item stat-item--clickable" @click="router.push({ path: '/approval', query: { tab: 'role', status: 'approved' } })">
          <StatusTag status="approved" variant="badge" size="small" />
          <span class="stat-value">{{ approvalStats.approved }}</span>
        </div>
        <div class="stat-item stat-item--clickable" @click="router.push({ path: '/approval', query: { tab: 'role', status: 'rejected' } })">
          <StatusTag status="rejected" variant="badge" size="small" />
          <span class="stat-value">{{ approvalStats.rejected }}</span>
        </div>
      </div>

      <ul v-if="pendingApprovals.length > 0" class="todo-list">
        <li
          v-for="todo in pendingApprovals"
          :key="todo.id"
          class="todo-item todo-item--approval"
          @click="handleApprovalClick(todo)"
        >
          <div class="todo-info">
            <span class="todo-title-text">{{ todo.title }}</span>
            <StatusTag
              v-if="(todo as any).urgency && (todo as any).urgency !== 'normal'"
              :status="(todo as any).urgency"
              variant="solid"
              size="small"
            />
          </div>
          <span v-if="todo.applicantName" class="todo-applicant">{{ todo.applicantName }}</span>
          <div class="todo-right">
            <StatusTag :status="todo.status" size="small" variant="badge" />
            <span class="todo-time">{{ todo.createdAt.slice(5, 10) }}</span>
          </div>
        </li>
      </ul>
      <div v-else class="todo-empty">暂无待处理审批</div>
    </div>

    <!-- ═══ 指派任务 Tab 内容 ═══ -->
    <div v-else-if="activeTab === 'task'" class="todo-content">
      <div class="todo-stats">
        <div class="stat-item">
          <StatusTag status="pending" variant="badge" size="small" />
          <span class="stat-value">{{ taskStats.pending }}</span>
        </div>
        <div class="stat-item">
          <StatusTag status="processing" variant="badge" size="small" />
          <span class="stat-value">{{ taskStats.inProgress }}</span>
        </div>
        <div class="stat-item">
          <StatusTag status="error" variant="badge" size="small" />
          <span class="stat-value">{{ taskStats.overdue }}</span>
        </div>
      </div>

      <ul v-if="assignedTasks.length > 0" class="todo-list">
        <li v-for="task in assignedTasks" :key="task.id" class="todo-item" @click="handleTaskClick(task)">
          <div class="todo-info">
            <span class="todo-title-text">{{ task.title }}</span>
          </div>
          <div class="todo-right">
            <span
              class="task-deadline"
              :class="{
                'task-deadline--urgent': getDeadlineInfo(task).isUrgent,
                'task-deadline--overdue': getDeadlineInfo(task).isOverdue,
              }"
            >
              {{ getDeadlineInfo(task).text }}
            </span>
          </div>
        </li>
      </ul>
      <div v-else class="todo-empty">暂无工作任务</div>
    </div>

    <!-- ═══ 考核 Tab 内容 ═══ -->
    <div v-else-if="activeTab === 'review'" class="todo-content">
      <ul v-if="reviewTodos.length > 0" class="todo-list">
        <li
          v-for="todo in reviewTodos"
          :key="todo.id"
          class="todo-item todo-item--review"
          @click="handleReviewClick(todo)"
        >
          <div class="todo-info">
            <span class="todo-title-text">{{ todo.title }}</span>
          </div>
          <div class="todo-right">
            <StatusTag :status="todo.status === 'completed' ? 'approved' : todo.status === 'in_progress' ? 'processing' : 'pending'" size="small" variant="badge" />
            <span class="todo-time">{{ todo.deadline.slice(5, 10) }}</span>
          </div>
        </li>
      </ul>
      <div v-else class="todo-empty">暂无考核待办</div>
    </div>

    <!-- 底部：全部待办按钮 -->
    <button class="view-all-btn" @click="handleViewAll">
      查看{{ activeTabConfig.label }}详情
      <el-icon :size="12"><ArrowRight /></el-icon>
    </button>

    <!-- 任务列表抽屉（闭环：不跳转，内联展示全部任务） -->
    <Teleport to="body">
      <Transition name="task-drawer">
        <div v-if="taskDrawerVisible" class="task-drawer-overlay" @click.self="taskDrawerVisible = false">
          <div class="task-drawer-panel">
            <!-- 抽屉头部 -->
            <div class="task-drawer__header">
              <h3 class="task-drawer__title">工作任务</h3>
              <button class="task-drawer__close" @click="taskDrawerVisible = false">
                <el-icon :size="16"><Close /></el-icon>
              </button>
            </div>

            <!-- 统计摘要 -->
            <div class="task-drawer__stats">
              <span class="td-stat td-stat--pending">
                <span class="td-dot"></span> 待处理 {{ taskStats.pending }}
              </span>
              <span class="td-stat td-stat--progress">
                <span class="td-dot"></span> 进行中 {{ taskStats.inProgress }}
              </span>
              <span class="td-stat td-stat--overdue">
                <span class="td-dot"></span> 已逾期 {{ taskStats.overdue }}
              </span>
            </div>

            <!-- 全部任务列表 -->
            <div class="task-drawer__list">
              <div
                v-for="task in assignedTasks"
                :key="task.id"
                class="task-drawer-item"
                :class="{
                  'task-drawer-item--overdue': task.status === 'overdue',
                  'task-drawer-item--done': task.status === 'completed',
                }"
              >
                <div class="td-item-main">
                  <h4 class="td-item-title">{{ task.title }}</h4>
                  <p class="td-item-desc">{{ task.description }}</p>
                  <div class="td-item-meta">
                    <span class="td-assigner">
                      <el-icon :size="12"><User /></el-icon>
                      {{ task.assignerName }}
                    </span>
                    <span
                      class="td-deadline"
                      :class="{
                        'td-deadline--urgent': getDeadlineInfo(task).isUrgent,
                        'td-deadline--overdue': getDeadlineInfo(task).isOverdue,
                      }"
                    >
                      {{ getDeadlineInfo(task).text }}
                    </span>
                  </div>
                </div>

                <!-- 进度条 + 状态 -->
                <div class="td-item-right">
                  <div class="td-progress-bar">
                    <div
                      class="td-progress-fill"
                      :class="{
                        'td-progress-fill--overdue': task.status === 'overdue',
                        'td-progress-fill--done': task.status === 'completed',
                      }"
                      :style="{ width: task.progress + '%' }"
                    ></div>
                  </div>
                  <span class="td-progress-text">{{ task.progress }}%</span>
                  <StatusTag
                    :status="task.status === 'completed' ? 'approved' : task.status === 'overdue' ? 'error' : task.status === 'in_progress' ? 'processing' : 'pending'"
                    size="small"
                    variant="light"
                  />
                </div>
              </div>
            </div>

            <!-- 空状态 -->
            <div v-if="assignedTasks.length === 0" class="task-drawer__empty">
              暂无工作任务
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped lang="scss">
.todo-summary-card {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-4) var(--fts-space-5);
  box-shadow: var(--fts-shadow-sm);
  border: 1px solid var(--fts-border-secondary);
  transition: var(--fts-transition-all);

  &:hover {
    box-shadow: var(--fts-shadow-md);
    border-color: var(--fts-border-primary);
  }
}

/* ── 头部 ── */
.todo-header {
  margin-bottom: var(--fts-space-3);
}

.todo-title {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  font-size: var(--fts-font-size-md);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0;
}

.title-icon {
  color: var(--fts-primary);
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--fts-error);
  color: var(--fts-text-on-primary);
  font-size: 10px;
  font-weight: 700;
  line-height: 18px;
}

/* ── Tab 导航（钉钉风格：下划线指示器） ── */
.todo-tabs {
  display: flex;
  gap: 0;
  margin-bottom: var(--fts-space-3);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.todo-tab {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-secondary);
  background-color: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s ease, border-color 0.2s ease;

  &:hover {
    color: var(--fts-text-primary);
    background-color: transparent;
  }

  &--active {
    background-color: transparent !important;
    color: var(--tab-color) !important;
    border-color: var(--tab-color) !important;
    font-weight: 600;
  }
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 700;
  /* 浅色模式：深色文字；深色模式：白色文字 */
  background-color: var(--tab-color, var(--fts-error));
  color: #333;

  /* 深色模式下覆盖为白色 */
  :root.dark &,
  [data-theme='dark'] &,
  .dark & {
    color: #fff;
  }
}

/* ── 统计栏 ── */
.todo-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) 0;
  border-top: 1px solid var(--fts-border-secondary);
  border-bottom: 1px solid var(--fts-border-secondary);
  margin-bottom: var(--fts-space-3);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: var(--fts-space-1) var(--fts-space-2);
  border-radius: var(--fts-radius-sm);
  transition: background-color 0.15s ease;

  &--clickable {
    cursor: pointer;

    &:hover {
      background: var(--fts-bg-hover);

      .stat-value { color: var(--fts-primary); }
    }

    &:active { transform: scale(0.97); }
  }
}

.stat-value {
  font-size: var(--fts-font-size-sm);
  font-weight: 700;
  color: var(--fts-text-primary);
  margin-left: auto;
}

/* ── 列表 ── */
.todo-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) var(--fts-space-3);
  border-radius: var(--fts-radius-sm);
  cursor: pointer;
  transition: background-color 0.12s ease;

  &:hover { background: var(--fts-bg-hover); }
  &:active { transform: scale(0.99); }
}

.todo-info {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  min-width: 0;
  flex: 1;
}

.todo-title-text {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.todo-applicant {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  white-space: nowrap;
  flex-shrink: 0;
}

.todo-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
}

.todo-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
  white-space: nowrap;
}

/* ── 任务特有：截止倒计时 ── */
.task-deadline {
  font-size: var(--fts-font-size-2xs, 11px);
  font-weight: 600;
  color: var(--fts-text-quaternary);
  white-space: nowrap;
  padding: 1px 6px;
  border-radius: var(--fts-radius-full, 999px);
  background-color: var(--fts-bg-tertiary);

  &--urgent {
    color: var(--fts-warning);
    background-color: rgba(var(--fts-warning-rgb), 0.1);
  }

  &--overdue {
    color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.1);
  }
}

/* ── 空状态 ── */
.todo-empty {
  text-align: center;
  padding: var(--fts-space-4) 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

/* ── 全部查看按钮 ── */
.view-all-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  width: 100%;
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-2) 0;
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-primary);
  background: transparent;
  border: 1px dashed var(--fts-border-hover);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: rgba(var(--fts-primary-rgb), 0.04);
    border-style: solid;
  }

  &:active { transform: scale(0.98); }
}

:deep(.el-drawer) {
  background-color: var(--fts-bg-card);
}

/* ── 任务抽屉（闭环） ── */
.task-drawer-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background-color: var(--fts-overlay-backdrop);
  display: flex;
  justify-content: flex-end;
  align-items: flex-end;
}

.task-drawer-panel {
  width: min(460px, 94vw);
  max-height: 80vh;
  background-color: var(--fts-bg-page);
  border-radius: var(--fts-radius-lg, 8px) var(--fts-radius-lg, 8px) 0 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--fts-shadow-lg);
}

.task-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4, 16px) var(--fts-space-4, 16px) var(--fts-space-3, 12px);
  flex-shrink: 0;
}

.task-drawer__title {
  margin: 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-bold, 700);
  color: var(--fts-text-primary);
}

.task-drawer__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: none;
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  color: var(--fts-text-tertiary);

  &:hover { background-color: var(--fts-bg-tertiary); color: var(--fts-text-primary); }
}

/* 统计 */
.task-drawer__stats {
  display: flex;
  gap: var(--fts-space-3, 12px);
  padding: 0 var(--fts-space-4, 16px) var(--fts-space-3, 12px);
  border-bottom: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;
}

.td-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-secondary);

  &--pending { .td-dot { background-color: var(--fts-warning); } }
  &--progress { .td-dot { background-color: var(--fts-primary); } }
  &--overdue { .td-dot { background-color: var(--fts-error); } }
}

.td-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* 列表 */
.task-drawer__list {
  flex: 1;
  overflow-y: auto;
  padding: var(--fts-space-2, 8px) var(--fts-space-4, 16px);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2, 8px);
}

.task-drawer-item {
  display: flex;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3, 12px);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 6px);
  transition: all var(--fts-duration-fast) ease;

  &:hover { border-color: var(--fts-border-primary); }

  &--overdue { border-left: 3px solid var(--fts-error); }
  &--done   { opacity: 0.6; }
}

.td-item-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.td-item-title {
  margin: 0;
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  line-height: 1.4;
}

.td-item-desc {
  margin: 0;
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-tertiary);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.td-item-meta {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
}

.td-assigner {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
}

.td-deadline {
  font-size: var(--fts-font-size-2xs, 11px);
  font-weight: 600;
  padding: 1px 6px;
  border-radius: var(--fts-radius-full, 999px);
  background-color: var(--fts-bg-tertiary);
  color: var(--fts-text-quaternary);

  &--urgent { color: var(--fts-warning); background-color: rgba(var(--fts-warning-rgb), 0.1); }
  &--overdue { color: var(--fts-error); background-color: rgba(var(--fts-error-rgb), 0.1); }
}

/* 右侧进度+状态 */
.td-item-right {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
  flex-shrink: 0;
}

.td-progress-bar {
  width: 50px;
  height: 5px;
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full, 999px);
  overflow: hidden;
}

.td-progress-fill {
  height: 100%;
  border-radius: var(--fts-radius-full, 999px);
  background-color: var(--fts-primary);
  transition: width 0.3s ease;

  &--overdue { background-color: var(--fts-error); }
  &--done     { background-color: var(--fts-success); }
}

.td-progress-text {
  font-size: var(--fts-font-size-2xs, 10px);
  font-weight: 700;
  color: var(--fts-text-secondary);
  font-variant-numeric: tabular-nums;
}

/* 空状态 */
.task-drawer__empty {
  text-align: center;
  padding: var(--fts-space-8, 32px) 0;
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-tertiary);
}

/* 抽屉动画 */
.task-drawer-enter-active,
.task-drawer-leave-active {
  transition: all 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.task-drawer-enter-from,
.task-drawer-leave-to {
  opacity: 0;

  .task-drawer-panel { transform: translateY(100%); }
}

.task-drawer-enter-active .task-drawer-panel,
.task-drawer-leave-active .task-drawer-panel {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}
</style>
