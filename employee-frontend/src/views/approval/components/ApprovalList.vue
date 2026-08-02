<script setup lang="ts">
/**
 * ApprovalList - 审批列表组件
 *
 * 卡片式展示审批记录，支持批量选择（仅审批权限用户）。
 * 每条记录展示标题、状态标签、申请人、时间线和操作按钮。
 */
import { ref, computed } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'
import ActionButtons from './ActionButtons.vue'
import type { ApprovalItem } from '@/types/approval'

interface Props {
  /** 审批列表数据 */
  items: ApprovalItem[]
  /** 当前激活的Tab */
  activeTab: string
  /** 是否有审批权限 */
  canApprove: boolean
  /** 加载状态 */
  loading: boolean
  /** 是否为经理及以上层级（L3+，completed Tab 需显示申请人区分团队成员） */
  isManagerLevel?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  items: () => [],
  activeTab: '',
  canApprove: false,
  loading: false,
})

const emit = defineEmits<{
  (e: 'click-item', id: string): void
  (e: 'batch-action', action: string, ids: string[]): void
  (e: 'item-action', action: string, id: string): void
}>()

// 批量选择
const selectedIds = ref<Set<string>>(new Set())

/** 是否显示批量操作栏 */
const showBatchBar = computed(() => props.canApprove && selectedIds.value.size > 0)

/** 已选数量 */
const selectedCount = computed(() => selectedIds.value.size)

/**
 * 是否显示申请人姓名
 *
 * 数据隔离规则：
 * - "我发起的" tab：隐藏（已隐含是本人）
 * - "已完成" tab + L1/L2：隐藏（数据已过滤为仅自己的）
 * - "已完成" tab + L3+：显示（包含团队成员数据，需区分）
 * - 其他 tab（待我审批等）：显示
 */
const shouldShowApplicant = computed(() => {
  if (props.activeTab === 'initiated') return false
  if (props.activeTab === 'completed' && !props.isManagerLevel) return false
  return true
})

/** 切换单项选中状态 */
function toggleSelect(id: string) {
  if (selectedIds.value.has(id)) {
    selectedIds.value.delete(id)
  } else {
    selectedIds.value.add(id)
  }
  // 触发响应式更新
  selectedIds.value = new Set(selectedIds.value)
}

/** 是否选中 */
function isSelected(id: string): boolean {
  return selectedIds.value.has(id)
}

/** 点击卡片 */
function handleClickItem(id: string) {
  emit('click-item', id)
}

/** 批量操作 */
function handleBatchAction(action: string) {
  emit('batch-action', action, Array.from(selectedIds.value))
}

/** 全部通过 */
function handleApproveAll() {
  handleBatchAction('approve-all')
}

/** 全部驳回 */
function handleRejectAll() {
  handleBatchAction('reject-all')
}
</script>

<template>
  <div class="approval-list">
    <!-- Loading 骨架屏 -->
    <div v-if="loading" class="list-loading">
      <div v-for="i in 4" :key="i" class="skeleton-card">
        <div class="skeleton-line skeleton-line--title" />
        <div class="skeleton-line skeleton_line--summary" />
        <div class="skeleton-actions" />
      </div>
    </div>

    <!-- 空状态 -->
    <EmptyState
      v-else-if="!loading && items.length === 0"
      title="暂无审批记录"
      description="当前条件下没有找到审批数据"
    />

    <!-- 审批列表 -->
    <template v-else>
      <!-- 批量选择区域（仅审批权限） -->
      <div v-if="canApprove" class="batch-hint">
        <span class="batch-hint__text">点击左侧勾选可批量操作</span>
      </div>

      <div class="card-list">
        <div
          v-for="item in items"
          :key="item.id"
          :class="['approval-card', `approval-card--${item.status}`, { 'approval-card--selected': isSelected(item.id) }]"
          @click="handleClickItem(item.id)"
        >
          <!-- 左侧：复选框 + 内容 -->
          <div class="card-body">
            <!-- 批量选择框 -->
            <label
              v-if="canApprove"
              class="checkbox-wrap"
              @click.stop
            >
              <input
                type="checkbox"
                :checked="isSelected(item.id)"
                class="checkbox-input"
                @change="toggleSelect(item.id)"
              />
            </label>

            <!-- 主内容区 -->
            <div class="card-main">
              <!-- 第一行：标题 + 状态 -->
              <div class="card-title-row">
                <div class="title-area">
                  <StatusTag
                    v-if="item.urgency !== 'normal'"
                    :status="item.urgency"
                    :urgency="item.urgency"
                    variant="solid"
                    size="small"
                    dot
                  />
                  <span class="card-title">{{ item.title }}</span>
                </div>
                <StatusTag :status="item.status" size="small" class="card-status" />
              </div>

              <!-- 第二行：摘要 + 时间 -->
              <div class="card-info-row">
                <p v-if="item.summary" class="card-summary">{{ item.summary }}</p>
                <span class="submit-time">{{ item.createdAt?.slice(5, 16)?.replace('T', ' ') || '' }}</span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="card-actions" @click.stop>
              <ActionButtons
                :role="activeTab as 'initiator' | 'approver' | 'viewer'"
                :status="item.status"
                :id="item.id"
                @withdraw="$emit('item-action', 'withdraw', item.id)"
                @urge="$emit('item-action', 'urge', item.id)"
                @approve="$emit('item-action', 'approve', item.id)"
                @reject="$emit('item-action', 'reject', item.id)"
                @transfer="$emit('item-action', 'transfer', item.id)"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 底部批量操作栏 -->
      <BatchActionBar
        :selected-count="selectedCount"
        :visible="showBatchBar"
        @approve-all="handleApproveAll"
        @reject-all="handleRejectAll"
      />
    </template>
  </div>
</template>

<style scoped lang="scss">
.approval-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

/* ===== Loading 骨架屏 ===== */
.list-loading {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.skeleton-card {
  background-color: var(--fts-bg-white);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg, 12px);
  padding: var(--fts-space-4) var(--fts-space-5);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.skeleton-line {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 2s ease-in-out infinite;

  &--title {
    width: 50%;
  }

  &_summary {
    width: 70%;
    height: 14px;
  }
}

.skeleton-actions {
  display: flex;
  gap: var(--fts-space-2);

  &::before,
  &::after {
    content: '';
    width: 56px;
    height: 28px;
    border-radius: var(--fts-radius-md, 8px);
    background-color: var(--fts-bg-tertiary);
    animation: pulse 1.5s ease-in-out infinite;
  }
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

/* ===== 批量提示 ===== */
.batch-hint {
  padding: var(--fts-space-2) 0;
}

.batch-hint__text {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
}

/* ===== 审批卡片列表 ===== */
.card-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.approval-card {
  background-color: var(--fts-bg-white);
  border: 1px solid var(--fts-border-secondary);
  border-left: 3px solid transparent;
  border-radius: var(--fts-radius-lg, 12px);
  cursor: pointer;
  transition: all var(--fts-duration-normal) cubic-bezier(0.4, 0, 0.2, 1);

  &--approved,
  &--approved_final { border-left-color: var(--fts-success); }
  &--rejected { border-left-color: var(--fts-error); }
  &--pending,
  &--processing { border-left-color: var(--fts-warning); }
  &--withdrawn,
  &--cancelled { border-left-color: var(--fts-text-quaternary); }

  &:hover {
    border-color: var(--fts-border-hover);
    background: var(--fts-bg-secondary);
    box-shadow: var(--fts-shadow-sm);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0) scale(0.995);
    transition-duration: var(--fts-duration-instant);
  }

  &--selected {
    border-color: var(--fts-primary);
    background-color: var(--fts-primary-lighter, rgba(37, 99, 235, 0.03));
  }
}

.card-body {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-4);
  padding: var(--fts-space-4) var(--fts-space-5);
}

/* 复选框 */
.checkbox-wrap {
  flex-shrink: 0;
  padding-top: 2px;
}

.checkbox-input {
  width: 18px;
  height: 18px;
  accent-color: var(--fts-primary);
  cursor: pointer;
}

.card-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-3);
  min-width: 0;
}

.title-area {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  min-width: 0;
  flex: 1;
}

.card-status {
  flex-shrink: 0;
}

// 第二行：摘要 + 时间

.card-title {
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold);
  letter-spacing: -0.01em;
  color: var(--fts-text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-info-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--fts-space-3);
}

.card-summary {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.submit-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  flex-shrink: 0;
  white-space: nowrap;
}

.card-actions {
  flex-shrink: 0;
}
</style>
