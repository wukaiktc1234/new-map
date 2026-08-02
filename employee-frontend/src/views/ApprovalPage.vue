<script setup lang="ts">
/**
 * ApprovalPage - 审批中心
 *
 * 设计参考：飞书审批中心 / 钉钉审批列表
 *
 * 布局结构（参考飞书审批应用框架）：
 *   ┌─────────────────────────────────────────────────────┐
 *   │ [需要我确认] [我发起的] [已完成]      [+ 新建 ▾]   │  ← 线条式 Tab + 操作按钮
 *   ├─────────────────────────────────────────────────────┤
 *   │              统计面板                                 │
 *   ├─────────────────────────────────────────────────────┤
 *   │  (仅已完成) [筛选: 类型 ▾] [状态 ▾]    [重置]       │  ← 次级筛选
 *   ├─────────────────────────────────────────────────────┤
 *   │              审批卡片列表                             │
 *   └─────────────────────────────────────────────────────┘
 */
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { Plus, Document, AlarmClock, RefreshRight, Van, Wallet, Box, Search, ArrowDown, Close } from '@element-plus/icons-vue'
import { PageContainer } from '@/components/core'
import { FilterTabs } from '@/components/core'
import type { FilterTabOption } from '@/components/core/FilterTabs.vue'
import { ElMessage } from 'element-plus'
import { usePermissionStore } from '@/stores/permission'
import { UserLevel } from '@/types/permission'
import { approvalApi } from '@/api/approval'
import type { ApprovalItem, ApprovalTab, ApprovalType } from '@/types/approval'
import { ApprovalTypeLabels } from '@/types/approval'

// 子组件
import ApprovalList from './approval/components/ApprovalList.vue'
import ApprovalStatsPanel from './approval/components/ApprovalStatsPanel.vue'

const router = useRouter()
const route = useRoute()
const permission = usePermissionStore()

// ===== 状态 =====
const activeTab = ref<ApprovalTab>('initiated')
const statusFilter = ref<string>('')
const typeFilter = ref<ApprovalType | ''>('')
const searchKeyword = ref('')
// ===== 新建操作面板 — 始终挂载 + .is-active class 切换（和测试文件一致）=====
const showActionPanel = ref(false)

function openActionPanel() {
  if (showActionPanel.value) return
  showActionPanel.value = true
}

function closeActionPanel() {
  if (!showActionPanel.value) return
  showActionPanel.value = false
}
const loading = ref(false)
const approvalList = ref<ApprovalItem[]>([])
const statsData = ref<{ totalApplications: number; approvedCount: number; rejectedCount: number; cancelledCount: number; pendingCount: number; passRate: number; leaveDaysUsed: number; leaveDaysTotal: number; reimbursementTotal: number; overtimeHours: number; travelCount: number; swapCount: number; } | null>(null)

// ===== 筛选相关 =====
const showFilters = computed(() => activeTab.value === 'completed')

/** 可用的审批类型选项 */
const typeOptions = computed(() => {
  const options: Array<{ label: string; value: ApprovalType | '' }> = [
    { label: '全部类型', value: '' },
  ]
  Object.entries(ApprovalTypeLabels).forEach(([key, label]) => {
    options.push({ label, value: key as ApprovalType })
  })
  return options
})

/** 已完成状态筛选选项 */
const completedStatusOptions = [
  { label: '全部状态', value: '' },
  { label: '已通过', value: 'approved' },
  { label: '已驳回', value: 'rejected' },
  { label: '已撤回', value: 'cancelled' },
]

/** 新建操作项 — 用于图标网格面板 */
const createActionItems = [
  { icon: Document, label: '请假申请', path: '/approval/create/leave', color: '#4CAF50' },
  { icon: AlarmClock, label: '加班申请', path: '/approval/create/overtime', color: '#FF9800' },
  { icon: RefreshRight, label: '调班申请', path: '/approval/create/swap', color: '#2196F3' },
  { icon: Van, label: '出差申请', path: '/approval/create/travel', color: '#9C27B0' },
  { icon: Wallet, label: '报销申请', path: '/approval/create/reimbursement', color: '#F44336' },
  { icon: Box, label: '物品领用', path: '/approval/create/requisition', color: '#607D8B' },
]

/** 点击操作项：直接跳转（不播放收缩动画） */
function handleCreateAction(path: string) {
  closeActionPanel()
  router.push(path)
}

// ===== 计算属性 =====
const canApprove = computed(() => permission.canApproveOthers)
const isManagerLevel = computed(() => permission.userLevel >= UserLevel.MANAGER)
const showPendingTab = computed(() => permission.levelInfo.canApprove)
const showSummaryTab = computed(() => isManagerLevel.value)

/** Tab 选项 */
const viewSwitcherOptions = computed<{ key: string; label: string }[]>(() => {
  const options: Array<{ key: string; label: string }> = []
  if (!showPendingTab.value) {
    options.push({ key: 'initiated', label: '我发起的' })
    options.push({ key: 'completed', label: '已完成' })
    return options
  }
  options.push({ key: 'pending', label: '需要我确认' })
  options.push({ key: 'initiated', label: '我发起的' })
  options.push({ key: 'completed', label: '已完成' })
  if (showSummaryTab.value) {
    options.push({ key: 'summary', label: '部门汇总' })
  }
  return options
})

/** FilterTabs 格式 */
const tabOptions = computed<FilterTabOption[]>(() =>
  viewSwitcherOptions.value.map(opt => ({ value: opt.key, label: opt.label })),
)

// ===== 数据获取 =====
async function fetchApprovalList() {
  try {
    loading.value = true
    const params: Record<string, unknown> = { page: 1, size: 50 }
    if (activeTab.value !== 'all' as string) params.tab = activeTab.value
    const response = await approvalApi.getList(params as Parameters<typeof approvalApi.getList>[0])
    let items = response?.records || []
    if (statusFilter.value) items = items.filter((item: ApprovalItem) => item.status === statusFilter.value)
    if (typeFilter.value) items = items.filter((item: ApprovalItem) => item.type === typeFilter.value)
    approvalList.value = items
  } catch {
    approvalList.value = []
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  try {
    statsData.value = await approvalApi.getStats()
  } catch {
    statsData.value = null
  }
}

// ===== 操作 =====
function handleStatusFilterChange(val: string) { statusFilter.value = val; fetchApprovalList() }
function handleTypeFilterChange(val: ApprovalType | '') { typeFilter.value = val; fetchApprovalList() }
function resetFilters() { statusFilter.value = ''; typeFilter.value = ''; fetchApprovalList() }
function handleTabChange(tab: string) { activeTab.value = tab as ApprovalTab; statusFilter.value = ''; typeFilter.value = ''; fetchApprovalList() }
function handleClickItem(id: string) { router.push(`/approval/detail/${id}`) }
function handleCreateByType(path: string) { router.push(path) }
function handleBatchAction(action: string, ids: string[]) {}
async function handleWithdraw(id: string) {
  try {
    await approvalApi.withdraw(id)
    await fetchApprovalList()
    ElMessage.success('申请已撤回，可以重新编辑后再次提交')
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '撤回失败')
  }
}
function handleItemAction(action: string, id: string) {
  if (action === 'withdraw') handleWithdraw(id)
}

onMounted(() => {
  const queryTab = route.query.tab as string
  if (queryTab && ['initiated', 'pending', 'cc', 'completed', 'summary'].includes(queryTab)) {
    activeTab.value = queryTab as ApprovalTab
  } else if (showPendingTab.value) {
    activeTab.value = 'pending' as ApprovalTab
  } else {
    activeTab.value = 'initiated' as ApprovalTab
  }
  statusFilter.value = (route.query.status as string) || ''
  fetchApprovalList()
  fetchStats()
})
</script>

<template>
  <PageContainer title="审批中心">
    <!-- 新建按钮 — 点击打开图标网格面板 -->
    <template #headerActions>
      <button class="create-btn" :class="{ 'is-panel-open': showActionPanel }" @click.stop="openActionPanel">
        <el-icon><Plus /></el-icon>
        <span>新建</span>
      </button>
    </template>

    <!-- 新建操作面板 — 始终挂载 + CSS class 切换（和测试文件一致） -->
    <Teleport to="body">
      <div
        class="action-panel-overlay"
        :class="{ 'is-active': showActionPanel }"
        @pointerdown.self="closeActionPanel"
      >
        <button class="action-panel__close" @click.stop="closeActionPanel">
          <el-icon :size="18"><Close /></el-icon>
        </button>
        <div class="action-content" v-show="showActionPanel" @pointerdown.stop>
          <div class="action-grid">
            <button
              v-for="item in createActionItems"
              :key="item.path"
              class="action-item"
              @click.stop="handleCreateAction(item.path)"
            >
              <span class="action-item__icon">
                <el-icon :size="32"><Document v-if="item.path.includes('leave')" /><AlarmClock v-else-if="item.path.includes('overtime')" /><RefreshRight v-else-if="item.path.includes('swap')" /><Van v-else-if="item.path.includes('travel')" /><Wallet v-else-if="item.path.includes('reimbursement')" /><Box v-else /></el-icon>
              </span>
              <span class="action-item__label">{{ item.label }}</span>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 搜索栏 -->
    <div class="page-search">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索审批单..."
        :prefix-icon="Search"
        clearable
        size="default"
        @keyup.enter="fetchApprovalList"
        @clear="fetchApprovalList"
      />
    </div>

    <!-- 线条式 Tab（均分宽度） -->
    <nav class="page-tabs" role="tablist">
      <FilterTabs
        :model-value="activeTab"
        :options="tabOptions"
        variant="line"
        size="large"
        @update:model-value="handleTabChange"
      />
    </nav>

    <!-- 统计面板 -->
    <ApprovalStatsPanel v-if="statsData" :stats="statsData" />

    <!-- 已完成Tab筛选器 — 紧凑型筛选条 -->
    <section v-if="showFilters" class="filter-section" aria-label="筛选条件">
      <div class="filter-bar">
        <!-- 类型筛选：当前值显示为 chip，点击下拉选择 -->
        <el-dropdown trigger="click" @command="(v: string) => handleTypeFilterChange(v as ApprovalType | '')">
          <button class="filter-chip">
            <span>{{ typeFilter ? typeOptions.find(o => o.value === typeFilter)?.label : '类型' }}</span>
            <el-icon class="filter-chip__arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="opt in typeOptions"
                :key="opt.value"
                :command="opt.value"
                :class="{ 'is-active': typeFilter === opt.value }"
              >
                {{ opt.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 状态筛选 -->
        <el-dropdown trigger="click" @command="handleStatusFilterChange">
          <button class="filter-chip">
            <span>{{ statusFilter ? completedStatusOptions.find(o => o.value === statusFilter)?.label : '状态' }}</span>
            <el-icon class="filter-chip__arrow"><ArrowDown /></el-icon>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item
                v-for="opt in completedStatusOptions"
                :key="opt.value"
                :command="opt.value"
                :class="{ 'is-active': statusFilter === opt.value }"
              >
                {{ opt.label }}
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>

        <!-- 重置按钮 -->
        <button
          v-if="typeFilter || statusFilter"
          class="filter-reset"
          @click="resetFilters"
        >
          <el-icon><RefreshRight /></el-icon>
          <span>重置</span>
        </button>
      </div>
    </section>

    <!-- 审批列表 -->
    <ApprovalList
      :items="approvalList"
      :active-tab="activeTab"
      :can-approve="canApprove"
      :is-manager-level="isManagerLevel"
      :loading="loading"
      @click-item="handleClickItem"
      @batch-action="handleBatchAction"
      @item-action="handleItemAction"
    />
  </PageContainer>
</template>

<style scoped lang="scss">
/**
 * 页面布局
 *
 * 利用 PageContainer 自带标题栏（显示"审批中心"），
 * 新建按钮通过 #headerActions 插槽放入标题栏右侧。
 * 下方只需：搜索 → Tab → 内容
 */

// 搜索栏
.page-search {
  margin-bottom: var(--fts-space-3);

  :deep(.el-input__wrapper) {
    border-radius: var(--fts-radius-lg);
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);

    &.is-focus {
      box-shadow: 0 1px 3px rgba(var(--fts-primary-rgb), 0.15);
      border-color: var(--fts-primary);
    }
  }
}

// Tab 导航（line 变体均分）
.page-tabs {
  margin-bottom: var(--fts-space-4);

  :deep(.ft.ft--line) {
    width: 100%;
  }
}

// 新建按钮 — 在 PageContainer headerActions 中使用
.create-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-4);
  background: var(--fts-primary);
  color: var(--fts-text-on-primary);
  border: none;
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-semibold);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease, opacity 0.15s ease;
  box-shadow: 0 1px 3px rgba(var(--fts-primary-rgb), 0.2);
  font-family: inherit;
  white-space: nowrap;

  &:hover {
    background: var(--fts-primary-hover);
    box-shadow: 0 2px 8px rgba(var(--fts-primary-rgb), 0.25);
  }

  &:active {
    transform: scale(0.97);
  }

  .el-icon {
    font-size: 14px;
  }

  @media (max-width: 640px) {
    padding: var(--fts-space-2) var(--fts-space-3);
  }

  // 面板打开时隐藏按钮，关闭时平滑恢复
  &.is-panel-open {
    opacity: 0;
    pointer-events: none;
    transform: scale(0.95);
  }

  // 确保按钮显隐有过渡（不受 prefers-reduced-motion 全局规则影响）
  transition: transform 0.2s ease, opacity 0.25s ease, background-color 0.15s ease,
              box-shadow 0.15s ease, border-color 0.15s ease;
}

/**
 * 筛选区域 — chip 下拉式筛选
 *
 * 设计原则：
 * - 每个筛选项只占一个 chip（显示当前值），点击展开下拉菜单
 * - 不再展开所有选项为 pill，避免拥挤混乱
 */
.filter-section {
  margin-bottom: var(--fts-space-4);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

// 筛选 chip — 当前值 + 下拉箭头
.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1) var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;
  font-family: inherit;

  &:hover {
    border-color: var(--fts-border-active);
    color: var(--fts-text-primary);
    background: var(--fts-bg-tertiary);
  }

  .filter-chip__arrow {
    font-size: 12px;
    color: var(--fts-text-quaternary);
    transition: transform var(--fts-duration-fast) ease;
  }
}

// 重置按钮
.filter-reset {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-1) var(--fts-space-3);
  background: transparent;
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-full);
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;
  font-family: inherit;

  &:hover {
    color: var(--fts-primary);
    border-color: var(--fts-primary);
    background: rgba(var(--fts-primary-rgb), 0.04);
  }

  .el-icon {
    font-size: 12px;
  }
}
</style>

<!--
  新建操作面板动画 — 非 scoped 样式块
  始终挂载 + .is-active class 切换（和测试文件一致，最可靠方案）
-->
<style lang="scss">
// ===== 遮罩层 — 径向裁剪展开 + 深浅自适应背景 =====
.action-panel-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;

  // 隐藏态：从右上角裁剪为点（不可见不可交互）
  pointer-events: none;
  clip-path: circle(0% at 100% 0%);

  // 浅色模式：白色模糊背景（学钉钉）
  background: rgba(255, 255, 255, 0.85);
  -webkit-backdrop-filter: blur(16px);
  backdrop-filter: blur(16px);

  // 过渡：只过渡 clip-path，最简可靠方案
  transition: clip-path 0.5s cubic-bezier(0.4, 0, 0.2, 1);

  // 展开态：1/4圆扩展到覆盖全屏
  &.is-active {
    pointer-events: auto;
    clip-path: circle(150% at 100% 0%);
  }

  // 深色模式：深色模糊背景
  :root.dark &,
  [data-theme='dark'] &,
  .dark & {
    background: rgba(15, 15, 25, 0.8);
  }
}

// ===== 内容区 — 均分页面宽度 =====
.action-content {
  position: absolute;
  top: 56px;
  left: var(--fts-space-5);
  right: var(--fts-space-5);
  padding: var(--fts-space-5) var(--fts-space-3);
  display: flex;
  flex-direction: column;
  align-items: center;
}

// ===== 关闭按钮 — 视口右上角（fixed定位）=====
.action-panel__close {
  position: fixed;
  top: var(--fts-space-4, 16px);
  right: var(--fts-space-4, 16px);
  z-index: 2100;
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px;
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  -webkit-backdrop-filter: blur(4px);
  backdrop-filter: blur(4px);
  transition: all 0.2s ease;

  // 浅色模式：半透明深色按钮
  background: rgba(0, 0, 0, 0.1);
  color: #333;

  &:hover { background: rgba(0, 0, 0, 0.2); transform: rotate(90deg); }
  &:active { transform: rotate(90deg) scale(0.92); }

  // 深色模式：半透明白色按钮
  :root.dark &,
  [data-theme='dark'] &,
  .dark & {
    background: rgba(255, 255, 255, 0.12);
    color: #fff;

    &:hover { background: rgba(255, 255, 255, 0.22); }
  }
}

// ===== 图标网格 — 对齐首页快捷入口（4列均分）=====
.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3, 12px);
  width: 100%;
}

// ===== 操作项 — 无边框，只留图标 =====
.action-item {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3) var(--fts-space-1);
  background: transparent;
  border: none;
  border-radius: var(--fts-radius-lg);
  cursor: pointer; font-family: inherit;
  -webkit-tap-highlight-color: transparent;
  transition: all 0.15s ease;

  &:hover { transform: translateY(-1px); }
  &:active { transform: scale(0.96); }
}

// ===== 图标容器 — 浅色白底黑图 / 深色黑底白图 =====
.action-item__icon {
  display: flex; align-items: center; justify-content: center;
  width: 56px; height: 56px;
  border-radius: var(--fts-radius-lg);

  // 浅色模式：白色背景 + 黑色图标（用户要求）
  color: #333;                        // 黑色图标线
  background-color: #FFFFFF;          // 白色背景
  box-shadow:
    0 2px 8px rgba(0, 0, 0, 0.10),   // 外阴影
    inset 0 1px 0 rgba(255, 255, 255, 1); // 内高光
  transition: all 0.15s ease;

  .action-item:hover & {
    transform: scale(1.05) translateY(-1px);
    box-shadow:
      0 6px 16px rgba(0, 0, 0, 0.14),
      inset 0 1px 0 rgba(255, 255, 255, 1);
  }

  // 深色模式：深灰背景 + 白色图标
  :root.dark &,
  [data-theme='dark'] &,
  .dark & {
    background-color: #1C1C1E;
    color: #F5F5F7;
    box-shadow:
      0 2px 8px rgba(0, 0, 0, 0.3),
      inset 0 1px 0 rgba(255, 255, 255, 0.08);

    .action-item:hover & {
      background-color: #2C2C2E;
      box-shadow:
        0 6px 16px rgba(0, 0, 0, 0.35),
        inset 0 1px 0 rgba(255, 255, 255, 0.12);
    }
  }
}

// ===== 标签文字 — 浅色黑字 / 深色白字 =====
.action-item__label {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  letter-spacing: 0.01em;
  line-height: 1.3;

  // 浅色模式：黑色文字（白色模糊背景上）
  color: #333;

  // 深色模式：白色文字（深色模糊背景上）
  :root.dark &,
  [data-theme='dark'] &,
  .dark & {
    color: rgba(255, 255, 255, 0.9);
  }
}
</style>
