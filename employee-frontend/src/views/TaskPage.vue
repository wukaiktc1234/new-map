<script setup lang="ts">
/**
 * TaskPage - 工作任务管理页面
 *
 * 展示上级指派给我的全部任务，支持筛选、进度更新、截止倒计时。
 * 数据来源：taskApi（Mock阶段）
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Clock, Check, Plus } from '@element-plus/icons-vue'
import { PageContainer, StatusTag, EmptyState, FilterTabs } from '@/components/core'
import { taskApi, TASK_WORKFLOWS } from '@/api/task'
import type { AssignedTask, TaskCategory, SubTask } from '@/api/task'

const router = useRouter()

// ==================== 数据 ====================
const loading = ref(false)
const tasks = ref<AssignedTask[]>([])

/** 统一筛选：状态为主维度，类别为辅助筛选 */
const activeFilter = ref<'all' | 'pending' | 'in_progress' | 'overdue' | 'completed'>('all')
/** 任务类别筛选（下拉选择，不再占用独立Tab行） */
const activeCategory = ref<TaskCategory | 'all'>('all')

async function fetchData() {
  loading.value = true
  try {
    tasks.value = await taskApi.getAssignedTasks()
  } finally {
    loading.value = false
  }
}

// ==================== 计算属性 ====================

/** 双重筛选：先按类别，再按状态 */
const filteredTasks = computed(() => {
  let result = tasks.value
  // T-034: 类别筛选
  if (activeCategory.value !== 'all') {
    result = result.filter(t => t.category === activeCategory.value)
  }
  // 状态筛选
  if (activeFilter.value !== 'all') {
    result = result.filter(t => t.status === activeFilter.value)
  }
  return result
})

/** 统计摘要（基于当前类别） */
const currentCategoryTasks = computed(() => {
  if (activeCategory.value === 'all') return tasks.value
  return tasks.value.filter(t => t.category === activeCategory.value)
})

const stats = computed(() => ({
  total: currentCategoryTasks.value.length,
  pending: currentCategoryTasks.value.filter(t => t.status === 'pending').length,
  inProgress: currentCategoryTasks.value.filter(t => t.status === 'in_progress').length,
  overdue: currentCategoryTasks.value.filter(t => t.status === 'overdue').length,
  completed: currentCategoryTasks.value.filter(t => t.status === 'completed').length,
}))

/** 统一状态筛选选项（FilterTabs 格式） */
const filterOptions = computed(() => [
  { value: 'all', label: '全部', count: stats.value.total },
  { value: 'pending', label: '待处理', count: stats.value.pending },
  { value: 'in_progress', label: '进行中', count: stats.value.inProgress },
  { value: 'overdue', label: '已逾期', count: stats.value.overdue },
  { value: 'completed', label: '已完成', count: stats.value.completed },
])

/** 类别下拉选项 */
const categoryOptions = computed(() => [
  { key: 'all' as const, label: '全部类别' },
  { key: 'daily' as TaskCategory, label: '日常指派' },
  { key: 'training' as TaskCategory, label: '培训考核' },
  { key: 'business_trip' as TaskCategory, label: '出差任务' },
  { key: 'inventory' as TaskCategory, label: '盘点任务' },
  { key: 'assessment' as TaskCategory, label: '考核任务' },
])

// ==================== 方法 ====================

function getDeadlineInfo(task: AssignedTask) {
  return taskApi.getDeadlineText(task.deadline)
}

/** T-035: 获取任务的工作流阶段信息 */
function getWorkflowStages(task: AssignedTask) {
  const wf = taskApi.getWorkflow(task.category)
  if (!wf || !wf.stages) return []
  return wf.stages
}

/** T-035: 获取当前阶段在阶段列表中的索引 */
function getCurrentStageIndex(task: AssignedTask): number {
  const stages = getWorkflowStages(task)
  if (!task.currentStage) return 0
  return stages.findIndex(s => s.key === task.currentStage)
}

/** T-036: 切换子任务完成状态 */
async function toggleSubTask(task: AssignedTask, subTask: SubTask) {
  if (subTask.status === 'completed') return // 已完成不可逆

  try {
    // TODO: 对接 taskApi.completeSubTask(subTask.id)
    subTask.status = 'completed'
    // 重新计算父任务进度
    const completed = (task.subTasks?.filter(s => s.status === 'completed').length || 0)
    const total = task.subTasks?.length || 1
    task.progress = Math.round((completed / total) * 100)

    if (task.progress >= 100) {
      task.status = 'completed'
    } else if (subTask.status === 'completed' && task.status === 'pending') {
      task.status = 'in_progress'
    }
  } catch {
    // 回滚状态
    subTask.status = 'pending'
  }
}

function handleUpdateProgress(task: AssignedTask) {
  // 后续可弹出进度滑块或详情抽屉
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <PageContainer title="工作任务" :loading="loading">
    <template #headerActions>
      <button class="publish-btn" @click="router.push('/tasks/publish')">
        <el-icon :size="14"><Plus /></el-icon>
        发布任务
      </button>
    </template>

    <!-- 统一筛选栏：状态Tab（唯一筛选行） -->
    <section class="page-section">
      <FilterTabs v-model="activeFilter" :options="filterOptions" variant="pill" size="medium" />
    </section>

    <!-- 任务列表 -->
    <section class="page-section">
      <!-- 空状态 -->
      <EmptyState
        v-if="!loading && filteredTasks.length === 0"
        title="暂无任务"
        description="当前没有指派任务，完成的工作会在这里展示"
      />

      <!-- 任务卡片列表 -->
      <div v-else class="task-list">
        <article
          v-for="task in filteredTasks"
          :key="task.id"
          class="task-card"
          :class="{
            'task-card--overdue': task.status === 'overdue',
            'task-card--done': task.status === 'completed',
          }"
        >
          <!-- 头部：标题 + 优先级 + 状态 -->
          <div class="task-card__header">
            <h3 class="task-card__title">{{ task.title }}</h3>
            <StatusTag
              :status="task.status"
              size="small"
              variant="light"
            />
          </div>

          <!-- 描述 -->
          <p class="task-card__desc">{{ task.description }}</p>

          <!-- T-035: 工作流阶段指示器 -->
          <div v-if="getWorkflowStages(task).length > 0" class="workflow-stages">
            <template v-for="(stage, sIdx) in getWorkflowStages(task)" :key="stage.key">
              <div
                :class="[
                  'wf-stage',
                  {
                    'wf-stage--active': sIdx === getCurrentStageIndex(task),
                    'wf-stage--done': sIdx < getCurrentStageIndex(task),
                    'wf-stage--pending': sIdx > getCurrentStageIndex(task),
                  }
                ]"
              >
                <span class="wf-stage__dot"></span>
                <span class="wf-stage__label">{{ stage.label }}</span>
              </div>
              <div v-if="sIdx < getWorkflowStages(task).length - 1" :class="['wf-connector', { 'wf-connector--done': sIdx < getCurrentStageIndex(task) }]"></div>
            </template>
          </div>

          <!-- 元信息行：指派人 + 截止时间 -->
          <div class="task-card__meta">
            <span class="meta-assigner">
              <el-icon :size="13"><User /></el-icon>
              {{ task.assignerName }} 指派
            </span>
            <span
              class="meta-deadline"
              :class="{
                'meta-deadline--urgent': getDeadlineInfo(task).isUrgent && !getDeadlineInfo(task).isOverdue,
                'meta-deadline--overdue': getDeadlineInfo(task).isOverdue,
              }"
            >
              <el-icon :size="13"><Clock /></el-icon>
              {{ getDeadlineInfo(task).text }}
            </span>
          </div>

          <!-- T-036: 子任务勾选列表 -->
          <div v-if="task.subTasks && task.subTasks.length > 0" class="sub-task-list">
            <div class="sub-task-list__header">
              <span class="sub-task-title">子任务</span>
              <span class="sub-task-count">{{ task.subTasks.filter(s => s.status === 'completed').length }}/{{ task.subTasks.length }}</span>
            </div>
            <label
              v-for="st in task.subTasks"
              :key="st.id"
              :class="['sub-task-item', { 'sub-task-item--done': st.status === 'completed' }]"
            >
              <span
                class="sub-task-checkbox"
                :class="{ 'sub-task-checkbox--checked': st.status === 'completed' }"
                @click.prevent="toggleSubTask(task, st)"
              >
                <el-icon v-if="st.status === 'completed'" :size="11"><Check /></el-icon>
              </span>
              <span :class="['sub-task-name', { 'sub-task-name--done': st.status === 'completed' }]">
                {{ st.title }}
              </span>
            </label>
          </div>

          <!-- 进度条 + 操作 -->
          <div class="task-card__footer">
            <div class="progress-area">
              <div class="progress-track">
                <div
                  class="progress-fill"
                  :class="{
                    'progress-fill--overdue': task.status === 'overdue',
                    'progress-fill--done': task.status === 'completed',
                  }"
                  :style="{ width: task.progress + '%' }"
                ></div>
              </div>
              <span class="progress-pct">{{ task.progress }}%</span>
            </div>
            <button
              v-if="task.status !== 'completed'"
              class="action-btn"
              @click="handleUpdateProgress(task)"
            >
              {{ task.status === 'pending' ? '开始处理' : '更新进度' }}
            </button>
            <span v-else class="done-label">已完成</span>
          </div>
        </article>
      </div>
    </section>
  </PageContainer>
</template>

<style scoped lang="scss">
/* ── 紧凑页面区块（列表页专用，最小化垂直间距） ──
 * 注意：page-body__content 已有 gap 控制，此处不再叠加 margin-bottom */
.page-section {
  margin-bottom: 0;

  &:last-child {
    margin-bottom: 0;
  }
}

/* 覆盖 PageContainer page-body__content 的默认 gap(space-5≈20px)，
 * 列表页改用紧凑间距 */
:deep(.page-body__content) {
  gap: var(--fts-space-2, 8px);
}

/* ── 任务卡片列表 ── */
.task-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.task-card {
  padding: var(--fts-space-3) var(--fts-space-3, 12px);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
  }

  &--overdue {
    border-left: 3px solid var(--fts-error);
  }

  &--done {
    opacity: 0.65;
  }

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-2);
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
    line-height: 1.4;
  }

  &__desc {
    margin: 0 0 var(--fts-space-3);
    font-size: var(--fts-font-size-sm, 13px);
    color: var(--fts-text-secondary);
    line-height: 1.5;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  &__meta {
    display: flex;
    align-items: center;
    gap: var(--fts-space-4, 16px);
    margin-bottom: var(--fts-space-3);
  }
}

.meta-assigner {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
}

.meta-deadline {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--fts-radius-full);
  background-color: var(--fts-bg-tertiary);
  color: var(--fts-text-secondary);

  &--urgent {
    color: var(--fts-warning);
    background-color: rgba(var(--fts-warning-rgb), 0.1);
  }

  &--overdue {
    color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.1);
  }
}

/* ── 进度条 + 操作区 ── */
.task-card__footer {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

.progress-area {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex: 1;
}

.progress-track {
  flex: 1;
  height: 6px;
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full, 999px);
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: var(--fts-radius-full, 999px);
  background-color: var(--fts-primary);
  transition: width 0.4s cubic-bezier(0.22, 1, 0.36, 1);

  &--overdue { background-color: var(--fts-error); }
  &--done     { background-color: var(--fts-success); }
}

.progress-pct {
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 700;
  color: var(--fts-text-secondary);
  min-width: 32px;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.action-btn {
  flex-shrink: 0;
  padding: 6px 14px;
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 600;
  color: var(--fts-text-on-primary);
  background-color: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    background-color: rgba(var(--fts-primary-rgb), 0.85);
  }

  &:active {
    transform: scale(0.96);
  }
}

.done-label {
  flex-shrink: 0;
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 600;
  color: var(--fts-success);
}

/* ── T-035: 工作流阶段指示器 ── */
.workflow-stages {
  display: flex;
  align-items: center;
  gap: 0;
  margin: var(--fts-space-3) 0;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding: var(--fts-space-2) 0;

  &::-webkit-scrollbar { display: none; }
}

.wf-stage {
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
  opacity: 0.4;
  transition: all var(--fts-duration-fast);

  &--done { opacity: 1; }
  &--active { opacity: 1; }
  &--pending { opacity: 0.35; }

  &__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: var(--fts-border-primary);
    flex-shrink: 0;
    transition: all var(--fts-duration-fast);
  }

  &--done &__dot { background-color: var(--fts-success); }
  &--active &__dot {
    background-color: var(--fts-primary);
    box-shadow: 0 0 0 3px rgba(var(--fts-primary-rgb), 0.2);
  }

  &__label {
    font-size: var(--fts-font-size-xs, 12px);
    color: var(--fts-text-secondary);
    font-weight: 500;
  }

  &--active &__label,
  &--done &__label { color: var(--fts-text-primary); }
  &--pending &__label { color: var(--fts-text-tertiary); }
}

.wf-connector {
  width: 20px;
  height: 2px;
  background-color: var(--fts-border-secondary);
  flex-shrink: 0;
  margin: 0 2px;
  transition: background-color var(--fts-duration-fast);

  &--done { background-color: var(--fts-success); }
}

/* ── T-036: 子任务勾选列表 ── */
.sub-task-list {
  margin-bottom: var(--fts-space-3);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-sm);
  overflow: hidden;
}

.sub-task-list__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-1) var(--fts-space-3);
  background-color: var(--fts-bg-tertiary);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.sub-task-title {
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 600;
  color: var(--fts-text-secondary);
}

.sub-task-count {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
}

.sub-task-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast);
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child { border-bottom: none; }
  &:hover { background-color: var(--fts-bg-tertiary); }

  &--done { opacity: 0.65; }
}

.sub-task-checkbox {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: var(--fts-radius-sm);
  border: 1.5px solid var(--fts-border-primary);
  background-color: var(--fts-bg-card);
  flex-shrink: 0;
  transition: all var(--fts-duration-fast);
  color: white;

  &--checked {
    background-color: var(--fts-primary);
    border-color: var(--fts-primary);
  }
}

.sub-task-name {
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-primary);
  line-height: 1.4;

  &--done {
    text-decoration: line-through;
    color: var(--fts-text-tertiary);
  }
}

/* 发布任务按钮（headerActions插槽内） */
.publish-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-2) var(--fts-space-4);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-on-primary);
  background: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.97); }
}
</style>
