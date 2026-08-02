<template>
  <div class="quick-actions-grid">
    <button
      v-for="(action, index) in displayActions"
      :key="action.id"
      :class="['action-item', 'card-enhanced', { 'action-item--editing': isEditing, 'action-item--removed': isEditing && isRemoved(action.id) }]"
      type="button"
      :style="{ '--delay': `${index * 50}ms` }"
      @click="handleClick(action)"
      @touchstart.passive="handleTouchStart($event, action)"
      @touchend.passive="handleTouchEnd"
      @touchcancel.passive="handleTouchEnd"
      @mousedown.prevent="handleMouseDown($event, action)"
      @mouseup.prevent="handleMouseUp"
      @mouseleave.prevent="handleMouseUp"
      @contextmenu.prevent
    >
      <!-- 删除按钮（编辑模式） -->
      <span
        v-if="isEditing && !isRemoved(action.id)"
        class="remove-btn"
        @click.stop="handleRemove(action.id)"
      >×</span>

      <div class="action-icon-wrapper">
        <el-icon :size="32" class="action-icon"><component :is="action.icon" /></el-icon>
        <span v-if="action.badge && action.badge > 0" class="action-badge">
          {{ action.badge > 99 ? '99+' : action.badge }}
        </span>
      </div>
      <span class="action-label">{{ action.label }}</span>
    </button>

    <!-- 编辑模式底部：添加区域 -->
    <Transition name="slide-up">
      <div v-if="isEditing" class="add-more-zone" @click.stop @mousedown.stop @touchstart.stop>
        <div class="add-more-hint">点击添加常用功能</div>
        <div class="add-more-grid">
          <button
            v-for="mod in availableModules"
            :key="mod.id"
            class="add-item"
            type="button"
            @click.stop="handleAdd(mod.id)"
          >
            <span class="add-item-icon">
              <el-icon :size="18"><component :is="mod.icon" /></el-icon>
            </span>
            <span class="add-item-label">{{ mod.label }}</span>
          </button>
        </div>

        <!-- 编辑操作栏 -->
        <div class="edit-actions" @click.stop>
          <button class="edit-btn edit-btn--cancel" type="button" @click.stop="handleCancel">
            取消
          </button>
          <button class="edit-btn edit-btn--confirm" type="button" @click.stop="handleConfirm">
            完成（{{ pendingIds.length }}/{{ maxCount }}）
          </button>
        </div>
      </div>
    </Transition>

    <!-- 编辑模式提示文字 -->
    <Transition name="fade">
      <div v-if="isEditing" class="edit-hint">点击 × 移除，下方添加新功能</div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import type { Component } from 'vue'
import { usePermissionStore, getIconComponent } from '@/stores/permission'
import { useQuickActions } from '@/composables/useQuickActions'

export interface QuickAction {
  id: string
  icon: Component
  label: string
  route: string
  badge?: number
}

interface Props {
  actions: QuickAction[]
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'navigate', route: string): void
}>()

const permission = usePermissionStore()
const { selectedIds, maxCount, setIds } = useQuickActions()

const isEditing = ref(false)

let pressTimer: ReturnType<typeof setTimeout> | null = null
let wasShortPress = true

const pendingIds = ref<string[]>([])

const PRESS_DELAY = 500

const activeIds = computed(() =>
  isEditing.value ? pendingIds.value : selectedIds.value
)

const allActionMap = computed(() => {
  const map = new Map<string, QuickAction>()
  for (const a of props.actions) {
    map.set(a.id, a)
  }
  return map
})

/** 基于活跃ID列表 + 权限模块数据 构建显示列表 */
const displayActions = computed(() => {
  return activeIds.value
    .map(id => {
      const existing = allActionMap.value.get(id)
      if (existing) return existing
      const mod = permission.getVisibleNavModules().find(m => m.id === id)
      if (!mod) return null
      return {
        id: mod.id,
        icon: getIconComponent(mod.icon),
        label: mod.label,
        route: mod.route || '/',
      } as QuickAction
    })
    .filter((a): a is QuickAction => a !== null)
})

const availableModules = computed(() => {
  const allModules = permission.getVisibleNavModules()
  const currentIds = new Set(activeIds.value)
  return allModules
    .filter(m =>
      (m.category === 'main' || m.category === 'secondary') &&
      m.id !== 'workspace' &&
      m.id !== 'notices' &&
      !currentIds.has(m.id)
    )
    .map(m => ({
      id: m.id,
      icon: getIconComponent(m.icon),
      label: m.label,
      color: m.color || '#409EFF',
    }))
})

function isRemoved(id: string): boolean {
  return !pendingIds.value.includes(id)
}

function handleTouchStart(event: TouchEvent, _action: QuickAction) {
  if (isEditing.value) return
  wasShortPress = true
  pressTimer = setTimeout(() => {
    wasShortPress = false
    enterEditMode()
  }, PRESS_DELAY)
}

function handleMouseDown(event: MouseEvent, _action: QuickAction) {
  if (isEditing.value) return
  wasShortPress = true
  pressTimer = setTimeout(() => {
    wasShortPress = false
    enterEditMode()
  }, PRESS_DELAY)
}

function handleTouchEnd() {
  clearPressTimer()
}

function handleMouseUp() {
  clearPressTimer()
}

function clearPressTimer() {
  if (pressTimer) {
    clearTimeout(pressTimer)
    pressTimer = null
  }
}

function enterEditMode() {
  isEditing.value = true
  pendingIds.value = [...selectedIds.value]
}

function exitEditMode(discard: boolean) {
  isEditing.value = false
  if (discard) {
    pendingIds.value = []
  }
}

function handleClick(action: QuickAction) {
  if (isEditing.value) return
  if (wasShortPress) {
    emit('navigate', action.route)
  }
}

function handleRemove(id: string) {
  pendingIds.value = pendingIds.value.filter(item => item !== id)
}

function handleAdd(id: string) {
  if (pendingIds.value.length < maxCount && !pendingIds.value.includes(id)) {
    pendingIds.value.push(id)
  }
}

function handleConfirm() {
  setIds(pendingIds.value)
  exitEditMode(false)
}

function handleCancel() {
  exitEditMode(true)
}

function handleClickOutside(event: MouseEvent) {
  if (!isEditing.value) return

  const target = event.target as HTMLElement
  const gridElement = document.querySelector('.quick-actions-grid')
  if (gridElement && !gridElement.contains(target)) {
    handleCancel()
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
  clearPressTimer()
})
</script>

<style scoped lang="scss">
.quick-actions-grid {
  position: relative;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
  gap: var(--fts-space-3, 12px);
  width: 100%;
}

.action-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-2);
  background-color: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  cursor: pointer;
  transition: var(--fts-card-transition);
  overflow: hidden;

  /* 入场动画 */
  animation: fadeInUp var(--fts-duration-normal) var(--fts-easing-smooth) backwards;
  animation-delay: var(--delay, 0ms);

  &:hover:not(.action-item--editing):not(.action-item--removed) {
    border-color: var(--fts-primary-light);

    .action-icon-wrapper {
      background-color: var(--fts-primary-light);
      color: var(--fts-primary);
      transform: scale(1.05);
    }
  }

  &--editing {
    cursor: default;
    border-style: dashed;
    border-color: var(--fts-border-secondary);
    animation: editingPulse 2s ease-in-out infinite;
    
    &:hover {
      transform: none;
      box-shadow: none;
    }
  }

  &--removed {
    opacity: 0.3;
    pointer-events: none;
    filter: grayscale(100%);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes editingPulse {
  0%, 100% {
    border-color: var(--fts-border-secondary);
  }
  50% {
    border-color: var(--fts-primary-light);
  }
}

/* 删除按钮 */
.remove-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  font-size: 14px;
  font-weight: 600;
  line-height: 1;
  color: var(--fts-text-on-primary);
  background-color: var(--fts-error);
  border: none;
  border-radius: var(--fts-radius-full, 9999px);
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;

  &:hover {
    transform: scale(1.1);
    background-color: var(--fts-error-dark, #c45656);
  }

  &:active {
    transform: scale(0.95);
  }
}

.action-icon-wrapper {
  position: relative;
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--fts-radius-lg, 8px);
  background-color: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);
  transition: all var(--fts-duration-fast, 150ms) ease;
}

.action-icon {
  transition: transform var(--fts-duration-fast, 150ms) ease;

  .action-item:hover:not(.action-item--editing):not(.action-item--removed) & {
    transform: scale(1.05);
  }
}

/* 角标样式 */
.action-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  font-size: 11px;
  font-weight: var(--fts-font-weight-semibold, 600);
  line-height: 18px;
  text-align: center;
  color: var(--fts-text-on-primary);
  background-color: var(--fts-error);
  border-radius: var(--fts-radius-full, 9999px);
  border: 2px solid var(--fts-bg-card);
  pointer-events: none;
}

.action-label {
  font-size: 12px;
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  text-align: center;
  line-height: 1.3;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 编辑模式提示文字 */
.edit-hint {
  grid-column: 1 / -1;
  padding: var(--fts-space-3, 12px) 0;
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
  text-align: center;
  border-top: 1px dashed var(--fts-border-secondary);
  margin-top: var(--fts-space-2, 8px);
}

/* 添加更多区域 */
.add-more-zone {
  grid-column: 1 / -1;
  margin-top: var(--fts-space-3, 12px);
  padding: var(--fts-space-4, 16px);
  background-color: var(--fts-bg-secondary);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg, 8px);
}

.add-more-hint {
  margin-bottom: var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-secondary);
  text-align: center;
}

.add-more-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(80px, 1fr));
  gap: var(--fts-space-3, 12px);
}

.add-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2, 8px);
  padding: var(--fts-space-3, 12px) var(--fts-space-2, 8px);
  background-color: var(--fts-bg-card);
  border: 1px dashed var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  transition: border-color var(--fts-duration-fast) ease, background-color var(--fts-duration-fast) ease;

  &:hover {
    border-color: var(--fts-primary-light);
    border-style: solid;
    background-color: var(--fts-bg-tertiary);
  }

  &:active {
    transform: scale(0.97);
  }
}

.add-item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: var(--fts-radius-md, 6px);
  /* 统一黑白风格：与快捷入口图标对齐 */
  background-color: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);
}

.add-item-label {
  font-size: 11px;
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  text-align: center;
  line-height: 1.2;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 编辑操作栏 */
.edit-actions {
  display: flex;
  gap: var(--fts-space-3);
  justify-content: center;
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-light);
}

.edit-btn {
  flex: 1;
  max-width: 140px;
  padding: var(--fts-space-2) var(--fts-space-5);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  cursor: pointer;
  transition: all var(--fts-duration-fast, 150ms) ease;
  border: 1px solid transparent;
  line-height: 1.5;
}

.edit-btn--cancel {
  background-color: var(--fts-bg-secondary);
  color: var(--fts-text-secondary);
  border-color: var(--fts-border-secondary);

  &:hover {
    background-color: var(--fts-bg-fill);
    color: var(--fts-text-primary);
  }

  &:active {
    transform: scale(0.97);
  }
}

.edit-btn--confirm {
  background-color: var(--fts-primary);
  color: var(--fts-text-on-primary);
  border-color: var(--fts-primary);

  &:hover {
    opacity: 0.9;
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }
}

/* 过渡动画 */
.slide-up-enter-active,
.slide-up-leave-active {
  transition: all var(--fts-duration-normal, 300ms) ease;
}

.slide-up-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.slide-up-leave-to {
  opacity: 0;
  transform: translateY(20px);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--fts-duration-fast, 150ms) ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 响应式适配 - 移动端减小间距 */
@media (max-width: 768px) {
  .quick-actions-grid {
    gap: var(--fts-space-2, 8px);
  }

  .action-item {
    padding: var(--fts-space-3, 12px) var(--fts-space-1, 4px);
  }

  .action-icon-wrapper {
    width: 48px;
    height: 48px;
  }

  .action-icon :deep(.el-icon) {
    font-size: var(--fts-font-size-2xl);
  }

  .action-label {
    font-size: 11px;
  }

  .remove-btn {
    width: 18px;
    height: 18px;
    font-size: 12px;
  }

  .add-more-zone {
    padding: var(--fts-space-3, 12px);
  }

  .add-more-grid {
    grid-template-columns: repeat(auto-fill, minmax(70px, 1fr));
    gap: var(--fts-space-2, 8px);
  }

  .add-item-icon {
    width: 32px;
    height: 32px;
  }

  .add-item-label {
    font-size: 10px;
  }
}

/* 减少动画偏好 */
@media (prefers-reduced-motion: reduce) {
  .action-item,
  .action-icon,
  .action-icon-wrapper {
    animation: none;
    transition: none;
  }

  .action-item {
    animation: none;
  }

  .slide-up-enter-active,
  .slide-up-leave-active,
  .fade-enter-active,
  .fade-leave-active {
    transition: none;
  }
}
</style>
