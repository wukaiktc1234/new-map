<script setup lang="ts">
/**
 * FabTodoButton - FAB悬浮按钮
 *
 * 固定在右下角的圆形浮动按钮，点击打开待办抽屉。
 * 带有角标显示待办数量，支持脉冲动画提示。
 */
import { computed } from 'vue'
import { Bell } from '@element-plus/icons-vue'
import { useTodoAggregateStore } from '@/stores/todo-aggregate'

const todoStore = useTodoAggregateStore()

const totalCount = computed(() => todoStore.totalActionableCount)

function handleClick() {
  todoStore.openDrawer()
}
</script>

<template>
  <button
    class="fab-button"
    type="button"
    aria-label="待办中心"
    @click="handleClick"
  >
    <el-icon :size="24" class="fab-icon"><Bell /></el-icon>

    <!-- 角标 -->
    <span
      v-if="totalCount > 0"
      class="fab-badge"
      :class="{ 'fab-badge--pulse': totalCount > 0 }"
    >
      {{ totalCount > 99 ? '99+' : totalCount }}
    </span>
  </button>
</template>

<style scoped lang="scss">
.fab-button {
  position: fixed;
  right: 20px;
  bottom: 24px;
  z-index: 999;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: var(--fts-radius-full, 50%);
  background-color: var(--fts-primary);
  color: var(--fts-text-white);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--fts-shadow-md);

  /* 进入动画 */
  animation: fab-enter 0.4s cubic-bezier(0.34, 1.56, 0.64, 1) both;

  &:hover {
    transform: scale(1.08);
    box-shadow: var(--fts-shadow-card-hover);
  }

  &:active {
    transform: scale(0.95);
  }

  &:focus-visible {
    outline: 3px solid var(--fts-primary-light);
    outline-offset: 2px;
  }
}

@keyframes fab-enter {
  from {
    opacity: 0;
    transform: scale(0) rotate(-15deg);
  }

  to {
    opacity: 1;
    transform: scale(1) rotate(0deg);
  }
}

.fab-icon {
  pointer-events: none;
}

.fab-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  border-radius: 10px;
  background-color: var(--fts-error);
  color: var(--fts-text-white);
  font-size: var(--fts-font-size-xs);
  font-weight: 700;
  line-height: 20px;
  text-align: center;
  border: 2px solid var(--fts-bg-page);
  pointer-events: none;

  &--pulse::after {
    content: '';
    position: absolute;
    top: -2px;
    left: -2px;
    right: -2px;
    bottom: -2px;
    border-radius: 12px;
    background-color: var(--fts-error);
    z-index: -1;
    animation: badge-pulse 2s ease-in-out infinite;
  }
}

@keyframes badge-pulse {
  0%, 100% {
    opacity: 0;
    transform: scale(1);
  }

  50% {
    opacity: 0.4;
    transform: scale(1.25);
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .fab-button {
    animation: none;
  }

  .fab-badge--pulse::after {
    animation: none;
  }
}

/* 移动端：避开底部导航栏(TabBar ~50px + 安全间距) */
@media (max-width: 767px) {
  .fab-button {
    right: 16px;
    bottom: 72px;
    width: 48px;
    height: 48px;
  }

  .fab-icon {
    font-size: var(--fts-font-size-xl);
  }

  .fab-badge {
    min-width: 18px;
    height: 18px;
    font-size: 10px;
    line-height: 18px;
  }
}
</style>
