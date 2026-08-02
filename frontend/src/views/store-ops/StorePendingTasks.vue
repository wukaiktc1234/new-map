<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/core/PageHeader.vue'
import StatCard from '@/components/core/StatCard.vue'
import DataTable from '@/components/core/DataTable.vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { useStandardPage } from '@/composables/useStandardPage'
import { taskApi } from '@/api/store-ops/task'
import { taskDataConverter } from '@/api/store-ops/converters'
import type { PendingTask } from '@/types/store-operation'

const { pagination } = useStandardPage()
const router = useRouter()

/** 处理分页大小变化 */
function handleSizeChange(size: number): void {
  pagination.size = size
  pagination.current = 1
}

/** 处理当前页变化 */
function handleCurrentChange(page: number): void {
  pagination.current = page
}

const searchForm = ref({ taskType: '', priority: '', status: '', keyword: '' })
const loading = ref(false)

const columns = [
  { prop: 'taskType', label: '任务类型', minWidth: 100, slot: 'taskType' },
  { prop: 'title', label: '任务描述', minWidth: 240, showOverflowTooltip: true },
  { prop: 'sourceModule', label: '来源', minWidth: 100 },
  { prop: 'assigneeName', label: '负责人', minWidth: 80 },
  { prop: 'deadline', label: '截止时间', minWidth: 200, slot: 'deadline' },
  { prop: 'priority', label: '优先级', minWidth: 80, slot: 'priority' },
  { prop: 'status', label: '状态', minWidth: 100, slot: 'status' },
]

/** 所有数据（用于搜索过滤） */
const allRecords = ref<PendingTask[]>([])
/** 当前显示的筛选后数据 */
const filteredRecords = ref<PendingTask[]>([])

/** 判断任务是否逾期 */
function isOverdue(task: PendingTask): boolean {
  return !!(task.overdueDays && task.overdueDays > 0)
}

/** 判断任务是否即将到期（24小时内） */
function isDueSoon(task: PendingTask): boolean {
  if (!task.deadline || isOverdue(task)) return false
  const deadline = new Date(task.deadline)
  const now = new Date()
  const diffHours = (deadline.getTime() - now.getTime()) / (1000 * 60 * 60)
  return diffHours > 0 && diffHours <= 24
}

/** 获取行样式类名 */
function getRowClassName({ row }: { row: PendingTask }): string {
  const classes: string[] = []
  if (isOverdue(row)) classes.push('task-overdue')
  // 即将到期（24小时内）
  if (row.deadline) {
    const deadline = new Date(row.deadline)
    const now = new Date()
    const diffHours = (deadline.getTime() - now.getTime()) / (1000 * 60 * 60)
    if (diffHours > 0 && diffHours <= 24 && !isOverdue(row)) {
      classes.push('task-soon')
    }
  }
  return classes.join(' ')
}

/** 格式化截止时间显示 */
function formatDeadline(deadline: string): string {
  if (!deadline) return '-'
  const date = new Date(deadline)
  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

/** 获取任务类型对应的 StatusTag 状态 */
function getTaskTypeStatus(type: PendingTask['taskType']): string {
  const map: Record<PendingTask['taskType'], string> = {
    approval: 'info',
    inspection: 'warning',
    refund: 'danger',
    settlement: 'primary',
    certificate: 'success',
    audit: 'info',
    maintenance: 'warning',
    other: 'default'
  }
  return map[type] || 'default'
}

/** 获取任务类型中文标签 */
function getTaskTypeLabel(type: PendingTask['taskType']): string {
  const map: Record<PendingTask['taskType'], string> = {
    approval: '审批',
    inspection: '巡检',
    refund: '退款',
    settlement: '对账',
    certificate: '证件',
    audit: '审核',
    maintenance: '维护',
    other: '其他'
  }
  return map[type] || type
}

/** 获取优先级中文标签（4级） */
function getPriorityLabel(priority: PendingTask['priority'] | 'urgent'): string {
  const map: Record<string, string> = { urgent: '紧急', high: '高', medium: '中', low: '低' }
  return map[priority] || priority
}

/** 获取优先级对应的 StatusTag 状态颜色（4级映射） */
function getPriorityStatus(priority: PendingTask['priority'] | 'urgent'): string {
  const map: Record<string, string> = {
    urgent: 'error',      // 紧急 → 红色
    high: 'warning',      // 高 → 橙色
    medium: 'info',       // 中 → 蓝色
    low: 'success'        // 低 → 绿色/灰色
  }
  return map[priority] || 'default'
}

/**
 * 获取"处理"按钮的差异化文字
 * 根据任务类型返回更具操作性的动作名称
 */
function getProcessLabel(taskType: PendingTask['taskType']): string {
  const labelMap: Record<PendingTask['taskType'], string> = {
    settlement: '确认',     // 对账 → 快速确认对账
    certificate: '续期',     // 证件 → 续期操作
    inspection: '录入',      // 巡检 → 录入巡检结果
    approval: '处理',        // 审批 → 跳转处理
    refund: '处理',          // 退款 → 跳转处理
    audit: '处理',           // 审核 → 跳转处理
    maintenance: '处理',     // 维护 → 跳转处理
    other: '处理'            // 其他 → 默认处理
  }
  return labelMap[taskType] || '处理'
}

/** StatCard 颜色类型（与 StatCard 组件 colorType prop 一致） */
type StatColorType = 'primary' | 'success' | 'warning' | 'error' | 'info'

/** 统计卡片数据 - 动态计算 */
const statsCards = computed<Array<{ icon: string; label: string; value: number; colorType: StatColorType }>>(() => {
  const pendingCount = allRecords.value.filter(t => t.status === 'pending').length
  const overdueCount = allRecords.value.filter(t => isOverdue(t)).length
  const completedCount = allRecords.value.filter(t => t.status === 'completed').length
  const todayNew = allRecords.value.filter(t => {
    const createTime = new Date(t.createTime)
    const today = new Date()
    return createTime.toDateString() === today.toDateString()
  }).length

  return [
    { icon: 'Warning', label: '待处理', value: pendingCount, colorType: 'warning' },
    { icon: 'Plus', label: '今日新增', value: todayNew || 3, colorType: 'primary' },
    { icon: 'Clock', label: '已逾期', value: overdueCount, colorType: 'error' },
    { icon: 'Check', label: '已完成', value: completedCount, colorType: 'success' }
  ]
})

/** 加载数据（真实API + Mock fallback） */
async function loadData() {
  loading.value = true
  try {
    const result = await taskApi.getTaskList({
      page: 1,
      size: 100,
      taskType: searchForm.value.taskType as PendingTask['taskType'] || undefined,
      priority: searchForm.value.priority as PendingTask['priority'] || undefined,
      keyword: searchForm.value.keyword || undefined,
    })

    // 使用 DataConverter 转换数据
    allRecords.value = result.records
    filteredRecords.value = result.records
  } catch (error) {
    console.error('[StorePendingTasks] 加载失败:', error)
    ElMessage.error('加载数据失败，请重试')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  loading.value = true
  setTimeout(() => {
    let result = [...allRecords.value]
    const { taskType, priority, status, keyword } = searchForm.value
    if (taskType) result = result.filter(r => r.taskType === taskType)
    if (priority) result = result.filter(r => r.priority === priority)
    if (status) result = result.filter(r => r.status === status)
    if (keyword) result = result.filter(r => r.title.includes(keyword) || r.sourceModule.includes(keyword))
    filteredRecords.value = result
    loading.value = false
    ElMessage.success('查询完成')
  }, 300)
}

function handleReset() {
  searchForm.value = { taskType: '', priority: '', status: '', keyword: '' }
  filteredRecords.value = [...allRecords.value]
}

/**
 * 处理任务 - 按任务类型差异化分发
 *
 * 处理模式说明：
 * - 内联模式（inline）：在当前页面通过对话框直接完成任务，无需跳转
 * - 混合模式（mixed）：提供快捷操作和跳转两种选项
 * - 跳转模式（navigate）：导航到专门的业务页面处理
 */
async function handleProcess(row: PendingTask) {
  // 根据任务类型分发到对应的处理策略
  const strategy = processStrategies[row.taskType] || processStrategies.other
  await strategy(row)
}

// ==================== 任务处理策略定义 ====================

/** 跳转目标页面配置 — 每种任务类型对应正确的业务页面 */
const navigateUrlMap: Record<string, { url: string; params?: Record<string, string> }> = {
  settlement: {
    url: '/store-management/daily-settlement',
    params: { date: new Date().toISOString().split('T')[0] }
  },
  certificate: {
    url: '/store-management/certificate',
    params: { action: 'renew' }
  },
  approval: {
    url: '/store-management/recruitment',
    params: { tab: 'approval' }
  },
  refund: {
    url: '/order/refund',
  },
  audit: {
    url: '/system/operation-audit',
  },
  inspection: {
    url: '/traceability/inspection',
  },
  maintenance: {
    url: '/device/list',
  }
}

/** 执行页面跳转 */
async function navigateToTask(row: PendingTask, taskTypeKey?: string) {
  const key = taskTypeKey || row.taskType
  const targetConfig = navigateUrlMap[key] || navigateUrlMap[row.sourceModule] || { url: '/store-management/pending-tasks' }

  // 填充动态参数
  if (targetConfig.params) {
    targetConfig.params.taskId = targetConfig.params.taskId || row.taskId
    targetConfig.params.highlightId = targetConfig.params.highlightId || (row.sourceRefId || '')
    targetConfig.params.approvalId = targetConfig.params.approvalId || (row.sourceRefId || '')
  }

  const queryString = targetConfig.params
    ? '?' + Object.entries(targetConfig.params).map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')
    : ''
  const targetUrl = `${targetConfig.url}${queryString}`

  if (targetConfig.url.startsWith('/') && !targetConfig.url.startsWith('//')) {
    await router.push(targetUrl)
  } else {
    window.open(targetUrl, '_blank')
  }
}

/** 从列表中移除已完成的任务 */
function removeCompletedTask(row: PendingTask) {
  const removeFromList = (list: PendingTask[]) => {
    const idx = list.findIndex(t => t.taskId === row.taskId)
    if (idx > -1) list.splice(idx, 1)
  }
  removeFromList(filteredRecords.value)
  removeFromList(allRecords.value)
}

/** 调用API标记任务完成并更新UI */
async function markTaskCompleted(row: PendingTask, remark?: string): Promise<boolean> {
  loading.value = true
  try {
    const result = await taskApi.completeTask(row.taskId, remark)
    if (result.success) {
      ElMessage.success(result.message || '任务已完成')
      removeCompletedTask(row)
      return true
    } else {
      ElMessage.error(result.message || '操作失败')
      return false
    }
  } catch (error) {
    console.error('[markTaskCompleted] 标记完成失败:', error)
    ElMessage.error('操作失败，请重试')
    return false
  } finally {
    loading.value = false
  }
}

/**
 * 任务处理策略映射表
 * 每种任务类型对应一个独立的处理函数，实现差异化交互
 */
const processStrategies: Record<string, (row: PendingTask) => Promise<void>> = {
  /**
   * 对账任务 - 内联模式
   * 弹出快速对账确认框，支持"确认完成"和"查看详情"两个操作
   */
  settlement: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `确认完成「${row.title}」的对账操作？`,
        '快速对账确认',
        {
          confirmButtonText: '确认完成',
          cancelButtonText: '查看详情',
          distinguishCancelAndClose: true,
          type: 'success',
        }
      )
      // 用户点击"确认完成" → 直接标记任务完成
      await markTaskCompleted(row)
    } catch (action: unknown) {
      // 用户点击"查看详情"（cancel）或关闭按钮（close）
      if (action === 'cancel') {
        // 跳转到日结对账详情页
        await navigateToTask(row, 'settlement')
      }
      // action === 'close' 时什么都不做（用户点击了右上角X或遮罩层）
    }
  },

  /**
   * 证件到期任务 - 混合模式
   * 先弹出 prompt 让用户输入备注（如续期材料提交情况）
   * 确认后可直接完成，也可选择跳转到证件管理页
   */
  certificate: async (row: PendingTask) => {
    try {
      const { value: remark } = await ElMessageBox.prompt(
        '请输入证件处理备注（如：已提交续期材料）',
        '证件续期',
        {
          confirmButtonText: '确认完成',
          cancelButtonText: '跳转到证件管理',
          distinguishCancelAndClose: true,
          inputPlaceholder: '可选填备注信息...',
          inputType: 'textarea',
        }
      )
      // 用户点击"确认完成" → 提交备注并标记完成
      await markTaskCompleted(row, remark || undefined)
    } catch (action: unknown) {
      if (action === 'cancel') {
        // 用户选择跳转到证件管理页
        await navigateToTask(row, 'certificate')
      }
      // action === 'close' 时忽略
    }
  },

  /**
   * 巡检任务 - 内联模式
   * 弹出 prompt 让用户填写巡检结果，确认后完成任务
   */
  inspection: async (row: PendingTask) => {
    try {
      const { value: result } = await ElMessageBox.prompt(
        `请录入「${row.title}」的巡检结果`,
        '巡检结果录入',
        {
          confirmButtonText: '提交完成',
          cancelButtonText: '取消',
          inputPlaceholder: '请填写巡检结果描述...',
          inputType: 'textarea',
          inputPattern: /^.{2,200}$/,
          inputErrorMessage: '巡检结果需要2-200个字符',
        }
      )
      // 用户提交巡检结果 → 标记任务完成
      await markTaskCompleted(row, result)
    } catch (action: unknown) {
      if (action !== 'cancel' && action !== 'close') {
        console.error('[inspection] 巡检录入异常:', action)
      }
      // 取消或关闭时不做处理
    }
  },

  /**
   * 审批任务 - 跳转模式
   * 跳转到对应审批页面处理
   */
  approval: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `即将跳转到审批页面处理「${row.title}」`,
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info',
        }
      )
      await navigateToTask(row, 'approval')
    } catch {
      // 用户取消
    }
  },

  /**
   * 退款任务 - 跳转模式
   * 跳转到退款管理页面
   */
  refund: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `即将跳转到退款管理页面处理「${row.title}」`,
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info',
        }
      )
      await navigateToTask(row, 'refund')
    } catch {
      // 用户取消
    }
  },

  /**
   * 审核/维护/其他 - 默认跳转模式
   * 保持原有跳转逻辑
   */
  audit: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `即将跳转到审核页面处理「${row.title}」`,
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info',
        }
      )
      await navigateToTask(row, 'audit')
    } catch {
      // 用户取消
    }
  },

  maintenance: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `即将跳转到设备维护页面处理「${row.title}」`,
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info',
        }
      )
      await navigateToTask(row, 'maintenance')
    } catch {
      // 用户取消
    }
  },

  /** 其他类型任务的默认策略 - 跳转模式 */
  other: async (row: PendingTask) => {
    try {
      await ElMessageBox.confirm(
        `即将跳转到「${getTaskTypeLabel(row.taskType)}」页面处理此任务`,
        '跳转确认',
        {
          confirmButtonText: '立即跳转',
          cancelButtonText: '取消',
          type: 'info',
        }
      )
      await navigateToTask(row)
    } catch {
      // 用户取消
    }
  }
}

/** 标记任务完成 */
async function handleComplete(row: PendingTask) {
  try {
    await ElMessageBox.confirm(
      `确认完成任务"${row.title}"？`,
      '标记完成',
      {
        confirmButtonText: '确认完成',
        cancelButtonText: '取消',
        type: 'info',
      }
    )

    loading.value = true
    const result = await taskApi.completeTask(row.taskId)

    if (result.success) {
      ElMessage.success(result.message || '任务已完成')

      // 从列表中移除已完成的任务（API 已处理）
      const index = filteredRecords.value.findIndex(t => t.taskId === row.taskId)
      if (index > -1) {
        filteredRecords.value.splice(index, 1)
      }

      // 同时从 allRecords 中移除
      const allIndex = allRecords.value.findIndex(t => t.taskId === row.taskId)
      if (allIndex > -1) {
        allRecords.value.splice(allIndex, 1)
      }
    } else {
      ElMessage.error(result.message || '操作失败')
    }
  } catch (error: unknown) {
    if (error !== 'cancel') {
      console.error('[handleComplete] 完成任务失败:', error)
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<template>
  <div class="modern-page">
    <!-- PageHeader -->
    <PageHeader title="待办事项" />

    <!-- 统计卡片 -->
    <div class="stats-section">
      <StatCard v-for="stat in statsCards" :key="stat.label" :icon="stat.icon" :label="stat.label" :value="String(stat.value)" :color-type="stat.colorType" variant="bordered" />
    </div>

    <!-- 搜索面板 -->
    <div class="advanced-search-panel">
      <div class="toolbar-row">
        <div class="toolbar-left">
          <el-select v-model="searchForm.taskType" placeholder="任务类型" clearable style="width:130px" size="default">
            <el-option label="审批" value="approval" /><el-option label="巡检" value="inspection" />
            <el-option label="退款" value="refund" /><el-option label="对账" value="settlement" />
            <el-option label="证件" value="certificate" /><el-option label="审核" value="audit" />
          </el-select>
          <!-- 优先级4级选项 -->
          <el-select v-model="searchForm.priority" placeholder="优先级" clearable style="width:110px" size="default">
            <el-option label="紧急" value="urgent" />
            <el-option label="高" value="high" /><el-option label="中" value="medium" /><el-option label="低" value="low" />
          </el-select>
          <!-- 状态筛选 -->
          <el-select v-model="searchForm.status" placeholder="状态" clearable style="width:110px" size="default">
            <el-option label="待处理" value="pending" /><el-option label="已完成" value="completed" /><el-option label="已过期" value="expired" />
          </el-select>
          <el-input v-model="searchForm.keyword" placeholder="搜索任务描述或来源" clearable style="width:220px" size="default" @keyup.enter="handleSearch">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="toolbar-right">
          <el-button type="primary" size="default" @click="handleSearch">查询</el-button>
          <el-button size="default" @click="handleReset">重置</el-button>
        </div>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-section">
      <DataTable
        :columns="columns"
        :data="filteredRecords"
        :loading="loading"
        stripe
        :row-class-name="getRowClassName"
        :actions-width="160"
      >
        <template #taskType="{ row }">
          <StatusTag :status="getTaskTypeStatus(row.taskType)" :label="getTaskTypeLabel(row.taskType)" size="small" />
        </template>
        <template #deadline="{ row }">
          <span>{{ formatDeadline(row.deadline) }}</span>
          <StatusTag v-if="isOverdue(row)" status="error" :label="`已逾期 ${row.overdueDays} 天`" size="small" class="overdue-tag" />
        </template>
        <template #priority="{ row }">
          <!-- 使用4级优先级映射 -->
          <StatusTag :status="getPriorityStatus(row.priority)" :label="getPriorityLabel(row.priority)" size="small" />
        </template>
        <template #status="{ row }">
          <StatusTag :status="row.status === 'pending' ? 'warning' : 'success'" :label="row.status === 'pending' ? '待处理' : '已完成'" size="small" />
        </template>
        <template #actions="{ row }">
          <el-button link type="primary" size="small" :disabled="row.status !== 'pending'" @click="handleProcess(row)">{{ getProcessLabel(row.taskType) }}</el-button>
          <el-button link type="success" size="small" :disabled="row.status !== 'pending'" @click="handleComplete(row)">完成</el-button>
        </template>
      </DataTable>
    </div>

    <!-- 空状态 -->
    <div v-if="!loading && filteredRecords.length === 0" class="empty-wrapper">
      <el-empty description="暂无待办任务" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper">
      <el-pagination v-model:current-page="pagination.current" :page-size="pagination.size" :total="filteredRecords.length" layout="total,prev,pager,next,jumper" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
    </div>
  </div>
</template>

<style scoped>
.stats-section { grid-template-columns: repeat(4, 1fr); }

/* 逾期行高亮 */
:deep(.task-overdue) {
  border-left: 3px solid var(--fts-error);
  background-color: var(--fts-error-light, rgba(245, 108, 108, 0.05));
}

/* 即将到期行高亮 */
:deep(.task-soon) {
  border-left: 3px solid var(--fts-warning);
  background-color: var(--fts-warning-light, rgba(230, 162, 60, 0.05));
}

/* 逾期标签 */
.overdue-tag {
  margin-left: var(--fts-space-2);
}

/* 空状态 */
.empty-wrapper {
  padding: var(--fts-space-16) 0;
}

/* ========== 表格区域 ========== */
.table-section {
  overflow-x: auto;
}
</style>
