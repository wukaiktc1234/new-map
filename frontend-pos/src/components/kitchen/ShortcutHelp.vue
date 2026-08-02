<template>
  <Teleport to="body">
    <Transition name="shortcut-help">
      <div
        v-if="visible"
        class="shortcut-help-overlay"
        @click.self="handleClose"
      >
        <div class="shortcut-help-panel" :class="{ 'is-highlighting': highlightedKey }">
          <!-- 面板头部 -->
          <div class="panel-header">
            <h3 class="panel-title">
              <span class="title-icon">⌨️</span>
              键盘快捷键
            </h3>
            <button class="close-btn" @click="handleClose" aria-label="关闭">
              ✕
            </button>
          </div>

          <!-- 快捷键分类展示 -->
          <div class="shortcuts-container">
            <!-- 订单选择类 -->
            <div class="shortcut-category">
              <h4 class="category-title">订单选择</h4>
              <div class="shortcut-list">
                <div
                  v-for="shortcut in selectionShortcuts"
                  :key="shortcut.key"
                  class="shortcut-item"
                  :class="{ 'is-highlighted': shortcut.key === highlightedKey }"
                >
                  <kbd
                    class="key-badge"
                    :class="{ 'active': shortcut.key === highlightedKey }"
                  >
                    {{ formatKeyLabel(shortcut.key) }}
                  </kbd>
                  <span class="description">{{ shortcut.description }}</span>
                </div>
              </div>
            </div>

            <!-- 操作执行类 -->
            <div class="shortcut-category">
              <h4 class="category-title">操作执行</h4>
              <div class="shortcut-list">
                <div
                  v-for="shortcut in actionShortcuts"
                  :key="shortcut.key"
                  class="shortcut-item"
                  :class="{ 'is-highlighted': shortcut.key === highlightedKey }"
                >
                  <kbd
                    class="key-badge"
                    :class="{ 'active': shortcut.key === highlightedKey }"
                  >
                    {{ formatKeyLabel(shortcut.key) }}
                    <span v-if="shortcut.ctrlKey" class="modifier">Ctrl</span>
                  </kbd>
                  <span class="description">{{ shortcut.description }}</span>
                </div>
              </div>
            </div>

            <!-- 系统功能类 -->
            <div class="shortcut-category">
              <h4 class="category-title">系统功能</h4>
              <div class="shortcut-list">
                <div
                  v-for="shortcut in systemShortcuts"
                  :key="shortcut.key"
                  class="shortcut-item"
                  :class="{ 'is-highlighted': shortcut.key === highlightedKey }"
                >
                  <kbd
                    class="key-badge"
                    :class="{ 'active': shortcut.key === highlightedKey }"
                  >
                    {{ formatKeyLabel(shortcut.key) }}
                  </kbd>
                  <span class="description">{{ shortcut.description }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 底部提示 -->
          <div class="panel-footer">
            <p class="footer-hint">
              💡 按 <kbd class="inline-key">?</kbd> 随时显示/隐藏此面板
            </p>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ShortcutConfig } from '@/composables/kitchen/useKeyboardShortcuts'

/**
 * 组件属性定义
 */
interface Props {
  /** 是否显示帮助面板 */
  visible: boolean
  /** 快捷键列表 */
  shortcuts?: ShortcutConfig[]
  /** 当前高亮的按键 */
  highlightedKey?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  shortcuts: () => [],
  highlightedKey: null
})

/**
 * 组件事件定义
 */
const emit = defineEmits<{
  (e: 'close'): void
}>()

/**
 * 关闭面板
 */
const handleClose = (): void => {
  emit('close')
}

/**
 * 格式化按键标签显示
 * @param key - 按键标识
 */
const formatKeyLabel = (key: string): string => {
  const keyMap: Record<string, string> = {
    ' ': 'Space',
    'Enter': 'Enter',
    'Escape': 'Esc',
    'Tab': 'Tab',
    '?': '?'
  }

  return keyMap[key] || key.toUpperCase()
}

/**
 * 按功能分类的快捷键列表
 */
const selectionShortcuts = computed(() => {
  return props.shortcuts.filter((s) =>
    ['1', '2', '3', '4', '5'].includes(s.key)
  )
})

const actionShortcuts = computed(() => {
  return props.shortcuts.filter((s) =>
    ['Enter', ' ', 'Tab', 'a'].includes(s.key)
  )
})

const systemShortcuts = computed(() => {
  return props.shortcuts.filter((s) =>
    ['Escape', 'F1', 'F5', '?'].includes(s.key)
  )
})
</script>

<style scoped>
/* ========== 覆盖层 ========== */
.shortcut-help-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: var(--pos-z-modal);
  padding: var(--pos-spacing-md);
}

/* ========== 帮助面板 ========== */
.shortcut-help-panel {
  width: 100%;
  max-width: 520px;
  max-height: 80vh;
  background: var(--pos-bg-card);
  border-radius: var(--pos-border-radius-xl);
  box-shadow: var(--pos-shadow-2xl);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  animation: scale-in 0.2s ease-out;
}

@keyframes scale-in {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

/* 高亮动画 */
.shortcut-help-panel.is-highlighting {
  animation: pulse-highlight 0.3s ease-out;
}

@keyframes pulse-highlight {
  0%, 100% {
    box-shadow: var(--pos-shadow-2xl);
  }
  50% {
    box-shadow: 0 0 40px rgba(249, 115, 22, 0.4);
  }
}

/* ========== 面板头部 ========== */
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--pos-spacing-md) var(--pos-spacing-lg);
  background: var(--pos-primary-gradient);
  color: white;
}

.panel-title {
  margin: 0;
  font-size: var(--pos-font-size-lg);
  font-weight: var(--pos-font-weight-bold);
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-sm);
}

.title-icon {
  font-size: 24px;
}

.close-btn {
  width: 36px;
  height: 36px;
  min-width: 36px;
  min-height: 36px;
  border-radius: 50%;
  border: 2px solid rgba(255, 255, 255, 0.3);
  background: transparent;
  color: white;
  font-size: 18px;
  cursor: pointer;
  transition: all var(--pos-transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: rotate(90deg);
}

.close-btn:active {
  transform: scale(0.9) rotate(90deg);
}

/* ========== 快捷键容器 ========== */
.shortcuts-container {
  flex: 1;
  overflow-y: auto;
  padding: var(--pos-spacing-md) var(--pos-spacing-lg);
  scrollbar-width: thin;
  scrollbar-color: var(--pos-border-color) transparent;
}

.shortcuts-container::-webkit-scrollbar {
  width: 6px;
}

.shortcuts-container::-webkit-scrollbar-thumb {
  background: var(--pos-border-color);
  border-radius: 3px;
}

/* ========== 分类区域 ========== */
.shortcut-category {
  margin-bottom: var(--pos-spacing-lg);
}

.shortcut-category:last-child {
  margin-bottom: 0;
}

.category-title {
  margin: 0 0 var(--pos-spacing-sm) 0;
  font-size: var(--pos-font-size-sm);
  font-weight: var(--pos-font-weight-semibold);
  color: var(--pos-primary);
  text-transform: uppercase;
  letter-spacing: 1px;
  padding-bottom: var(--pos-spacing-xs);
  border-bottom: 2px solid var(--pos-primary-light);
}

/* ========== 快捷键列表 ========== */
.shortcut-list {
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-xs);
}

.shortcut-item {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-md);
  padding: var(--pos-spacing-sm) var(--pos-spacing-md);
  border-radius: var(--pos-border-radius-sm);
  transition: all var(--pos-transition-fast);
}

.shortcut-item:hover {
  background: var(--pos-bg-hover);
}

.shortcut-item.is-highlighted {
  background: rgba(249, 115, 22, 0.1);
  transform: translateX(4px);
}

/* ========== 按键徽章 ========== */
.key-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-width: 48px;
  height: 32px;
  padding: 0 var(--pos-spacing-sm);
  background: linear-gradient(180deg, #ffffff 0%, #f5f5f5 100%);
  border: 1px solid #d0d0d0;
  border-bottom-width: 3px;
  border-radius: 6px;
  font-family: 'SF Mono', 'Monaco', 'Consolas', monospace;
  font-size: var(--pos-font-size-sm);
  font-weight: var(--pos-font-weight-semibold);
  color: var(--pos-text-primary);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all var(--pos-transition-fast);
}

.key-badge.active {
  background: var(--pos-primary-gradient);
  border-color: var(--pos-primary-dark);
  color: white;
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.4);
  transform: scale(1.05);
}

.modifier {
  font-size: 10px;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  font-weight: var(--pos-font-weight-normal);
}

.key-badge.active .modifier {
  background: rgba(255, 255, 255, 0.2);
}

/* ========== 描述文本 ========== */
.description {
  flex: 1;
  font-size: var(--pos-font-size-sm);
  color: var(--pos-text-secondary);
  line-height: 1.4;
}

/* ========== 底部提示 ========== */
.panel-footer {
  padding: var(--pos-spacing-md) var(--pos-spacing-lg);
  background: var(--pos-bg-hover);
  border-top: 1px solid var(--pos-border-light);
}

.footer-hint {
  margin: 0;
  font-size: var(--pos-font-size-sm);
  color: var(--pos-text-muted);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--pos-spacing-xs);
}

.inline-key {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  background: white;
  border: 1px solid var(--pos-border-color);
  border-bottom-width: 2px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  font-weight: var(--pos-font-weight-bold);
  color: var(--pos-text-primary);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.08);
}

/* ========== 过渡动画 ========== */
.shortcut-help-enter-active,
.shortcut-help-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.shortcut-help-enter-from,
.shortcut-help-leave-to {
  opacity: 0;
}

.shortcut-help-enter-from .shortcut-help-panel,
.shortcut-help-leave-to .shortcut-help-panel {
  transform: scale(0.9);
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .shortcut-help-overlay {
    padding: var(--pos-spacing-sm);
  }

  .shortcut-help-panel {
    max-height: 90vh;
  }

  .panel-header {
    padding: var(--pos-spacing-sm) var(--pos-spacing-md);
  }

  .panel-title {
    font-size: var(--pos-font-size-md);
  }

  .title-icon {
    font-size: 20px;
  }

  .shortcuts-container {
    padding: var(--pos-spacing-sm) var(--pos-spacing-md);
  }

  .shortcut-item {
    padding: var(--pos-spacing-xs) var(--pos-spacing-sm);
  }

  .key-badge {
    min-width: 44px;
    height: 28px;
    font-size: var(--pos-font-size-xs);
  }

  .description {
    font-size: var(--pos-font-size-xs);
  }
}
</style>
