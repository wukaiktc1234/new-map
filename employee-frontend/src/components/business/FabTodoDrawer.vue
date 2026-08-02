<script setup lang="ts">
/**
 * FabTodoDrawer - FAB待办抽屉面板
 *
 * 从右侧滑出的待办中心面板，分为三个区域：
 * 1. 需要我操作（可执行待办）
 * 2. 我的申请进展（进度跟踪）
 * 3. 未读通知（消息提醒）
 */
import { computed } from 'vue'
import { Close, Document, Promotion } from '@element-plus/icons-vue'
import { ElDrawer } from 'element-plus'
import { useTodoAggregateStore } from '@/stores/todo-aggregate'
import EmptyState from '@/components/core/EmptyState.vue'
import ActionableTodoItem from './ActionableTodoItem.vue'
import ProgressTodoItem from './ProgressTodoItem.vue'
import NotificationItem from './NotificationItem.vue'

const todoStore = useTodoAggregateStore()

const drawerVisible = computed({
  get: () => todoStore.drawerVisible,
  set: (val: boolean) => {
    if (!val) todoStore.closeDrawer()
  },
})

const isLoading = computed(() => todoStore.isLoading)

/** 各分区数据 */
const actionableTodos = computed(() => todoStore.actionableTodos)
const progressTodos = computed(() => todoStore.progressTodos)
const notifications = computed(() => todoStore.notifications)

function handleClose() {
  todoStore.closeDrawer()
}
</script>

<template>
  <ElDrawer
    :model-value="drawerVisible"
    direction="rtl"
    size="85vw"
    :with-header="false"
    :destroy-on-close="false"
    @update:model-value="(val: boolean) => { if (!val) handleClose() }"
  >
    <div class="todo-drawer">
      <!-- 头部 -->
      <header class="drawer-header">
        <h3 class="drawer-title">待办中心</h3>
        <button class="close-btn" type="button" aria-label="关闭" @click="handleClose">
          <el-icon :size="18"><Close /></el-icon>
        </button>
      </header>

      <!-- Loading 状态 -->
      <div v-if="isLoading" class="drawer-loading">
        <div v-for="i in 5" :key="i" class="skeleton-row">
          <div class="skeleton-dot" />
          <div class="skeleton-text skeleton-text--long" />
          <div class="skeleton-text" />
        </div>
      </div>

      <!-- 内容区 -->
      <template v-else>
        <!-- 分区1：需要我操作 -->
        <section class="drawer-section">
          <div class="section-head">
            <span class="section-icon section-icon--action">🔴</span>
            <span class="section-label">需要我操作</span>
            <span v-if="actionableTodos.length > 0" class="section-count">
              {{ actionableTodos.length }}
            </span>
          </div>

          <EmptyState
            v-if="actionableTodos.length === 0"
            icon="check"
            title="暂无待处理事项"
            description="当前没有需要您操作的待办"
            :compact="true"
          />

          <div v-else class="item-list">
            <ActionableTodoItem
              v-for="item in actionableTodos"
              :key="item.id"
              :item="item"
              @handle="(id: string) => {}"
              @navigate="(url: string) => {}"
            />
          </div>
        </section>

        <!-- 分区2：我的申请进展 -->
        <section class="drawer-section">
          <div class="section-head">
            <span class="section-icon section-icon--progress"><el-icon :size="14"><Document /></el-icon></span>
            <span class="section-label">我的申请进展</span>
            <span v-if="progressTodos.length > 0" class="section-count">
              {{ progressTodos.length }}
            </span>
          </div>

          <EmptyState
            v-if="progressTodos.length === 0"
            icon="document"
            title="暂无进行中的申请"
            description="提交的申请将在此处显示进展"
            :compact="true"
          />

          <div v-else class="item-list">
            <ProgressTodoItem
              v-for="item in progressTodos"
              :key="item.id"
              :item="item"
              @navigate="(url: string) => {}"
            />
          </div>
        </section>

        <!-- 分区3：未读通知 -->
        <section class="drawer-section">
          <div class="section-head">
            <span class="section-icon section-icon--notif"><el-icon :size="14"><Promotion /></el-icon></span>
            <span class="section-label">未读通知</span>
            <span v-if="notifications.filter(n => !n.isRead).length > 0" class="section-count">
              {{ notifications.filter(n => !n.isRead).length }}
            </span>
          </div>

          <EmptyState
            v-if="notifications.length === 0"
            icon="bell"
            title="暂无新通知"
            description="系统通知和提醒将显示在这里"
            :compact="true"
          />

          <div v-else class="item-list">
            <NotificationItem
              v-for="item in notifications"
              :key="item.id"
              :item="item"
              @navigate="(url: string) => {}"
            />
          </div>
        </section>
      </template>
    </div>
  </ElDrawer>
</template>

<style scoped lang="scss">
.todo-drawer {
  display: flex;
  flex-direction: column;
  height: 100%;
  background-color: var(--fts-bg-page);
}

:deep(.el-drawer) {
  background-color: var(--fts-bg-page);

  .el-drawer__body {
    background-color: var(--fts-bg-page);
  }
}

/* ===== 头部 ===== */
.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-4) var(--fts-space-5);
  border-bottom: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;
}

.drawer-title {
  margin: 0;
  font-size: var(--fts-font-size-lg, 18px);
  font-weight: var(--fts-font-weight-bold, 700);
  color: var(--fts-text-primary);
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: var(--fts-radius-md, 8px);
  background-color: var(--fts-bg-tertiary);
  color: var(--fts-text-secondary);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    background-color: var(--fts-bg-secondary);
    color: var(--fts-text-primary);
  }

  &:active {
    transform: scale(0.92);
  }
}

/* ===== Loading ===== */
.drawer-loading {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4) var(--fts-space-5);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) 0;
}

.skeleton-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--fts-bg-tertiary);
  animation: pulse 1.5s ease-in-out infinite;
  flex-shrink: 0;
}

.skeleton-text {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;

  &--long {
    flex: 1;
  }

  &:not(&--long) {
    width: 60px;
    flex-shrink: 0;
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

/* ===== 分区 ===== */
.drawer-section {
  padding: var(--fts-space-4) var(--fts-space-5);
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child {
    border-bottom: none;
  }
}

.section-head {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
}

.section-icon {
  font-size: 14px;
  line-height: 1;
}

.section-label {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-primary);
  flex: 1;
}

.section-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background-color: var(--fts-primary-lighter, rgba(37, 99, 235, 0.1));
  color: var(--fts-primary);
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
}

.item-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}
</style>
