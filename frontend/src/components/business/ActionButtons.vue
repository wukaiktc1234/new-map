<script setup lang="ts">
/**
 * ActionButtons - 操作列统一组件
 * 
 * 功能：
 * 1. 统一操作按钮样式（link类型）
 * 2. 自动拆分主要操作和"更多"下拉菜单
 * 3. 支持禁用状态、确认提示、权限控制
 * 4. 符合项目规范：使用link按钮、禁止text类型
 */

import { computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowDown, WarningFilled } from '@element-plus/icons-vue'

export interface ActionItem {
  key: string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
  icon?: any
  disabled?: boolean
  disabledReason?: string
  confirmText?: string
  confirmType?: 'warning' | 'info' | 'success' | 'error'
  divider?: boolean
  children?: ActionItem[]
}

const props = withDefaults(defineProps<{
  actions: ActionItem[]
  maxVisible?: number
  size?: 'large' | 'default' | 'small'
}>(), {
  maxVisible: 3,
  size: 'small'
})

const emit = defineEmits<{
  (e: 'action', action: ActionItem): void
}>()

// 拆分主要操作（始终显示）和更多操作（折叠到下拉菜单）
const primaryActions = computed(() => {
  // 只取前 maxVisible 个没有children的操作
  return props.actions
    .filter(action => !action.children)
    .slice(0, props.maxVisible)
})

// 更多操作（超出部分 + 展开有children的操作）
const moreActions = computed(() => {
  const result: ActionItem[] = []
  
  // 1. 获取超出 maxVisible 的无children操作
  const noChildrenActions = props.actions.filter(action => !action.children)
  if (noChildrenActions.length > props.maxVisible) {
    result.push(...noChildrenActions.slice(props.maxVisible))
  }
  
  // 2. 提取所有带children的操作的children（展平）
  props.actions.forEach(action => {
    if (action.children && action.children.length > 0) {
      result.push(...action.children)
    }
  })
  
  return result
})

// 是否显示"更多"下拉菜单
// 智能判断逻辑：
// 1. 如果调用者明确传入了带children的"更多"操作 → 显示
// 2. 如果无children操作超出maxVisible且数量>=1 → 显示（避免丢失操作）
const showMoreDropdown = computed(() => {
  const hasExplicitMoreAction = props.actions.some(action => action.children && action.children.length > 0)
  const hasOverflowActions = moreActions.value.length > 0

  return hasExplicitMoreAction || hasOverflowActions
})

// 处理操作点击
function handleAction(action: ActionItem) {
  if (action.disabled) {
    ElMessage.warning(action.disabledReason || '该操作暂不可用')
    return
  }
  
  if (action.confirmText) {
    ElMessageBox.confirm(action.confirmText, '确认操作', {
      type: action.confirmType || 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then(() => {
      emit('action', action)
    }).catch(() => {
      // 用户取消
    })
  } else {
    emit('action', action)
  }
}

// 获取按钮样式类名
function getButtonClass(action: ActionItem): string[] {
  const classes: string[] = []
  
  if (action.type === 'danger') {
    classes.push('is-danger')
  }
  
  if (action.disabled) {
    classes.push('is-disabled')
  }
  
  return classes
}

/** 处理子菜单项点击 */
function handleSubAction(action: ActionItem) {
  if (action.disabled) {
    ElMessage.warning(action.disabledReason || '该操作暂不可用')
    return
  }
  
  if (action.confirmText) {
    ElMessageBox.confirm(action.confirmText, '确认操作', {
      type: action.confirmType || 'warning',
      confirmButtonText: '确定',
      cancelButtonText: '取消'
    }).then(() => {
      emit('action', action)
    }).catch(() => {
      // 用户取消
    })
  } else {
    emit('action', action)
  }
}
</script>

<template>
  <div class="action-buttons">
    <!-- 主要操作按钮 -->
    <template v-for="action in primaryActions" :key="action.key">
      <!-- 分隔线 -->
      <span v-if="action.divider" class="action-divider"></span>
      
      <el-button
        link
        :type="action.type || 'primary'"
        :size="size"
        :disabled="action.disabled"
        :class="getButtonClass(action)"
        @click.stop="handleAction(action)"
      >
        {{ action.label }}
      </el-button>
    </template>

    <!-- 更多操作下拉菜单 -->
    <el-dropdown
      v-if="showMoreDropdown"
      trigger="click"
      @command="(cmd: ActionItem) => handleSubAction(cmd)"
    >
      <el-button link type="info" :size="size" class="more-button" @click.stop>
        更多
        <el-icon class="more-icon"><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu class="action-dropdown-menu">
          <template v-for="action in moreActions" :key="action.key">
            <!-- 分隔线 -->
            <el-dropdown-item
              v-if="action.divider"
              divided
              disabled
            ></el-dropdown-item>
            
            <!-- 有子菜单的操作（暂未使用，预留扩展） -->
            <el-dropdown-item
              v-else-if="action.children && action.children.length > 0"
              :command="action"
              :disabled="action.disabled"
              divided
            >
              <div class="dropdown-item-content">
                <el-icon v-if="action.icon"><component :is="action.icon" /></el-icon>
                <span>{{ action.label }}</span>
                <el-icon class="submenu-arrow"><ArrowDown /></el-icon>
              </div>
            </el-dropdown-item>
            
            <!-- 普通操作项 -->
            <el-dropdown-item
              v-else
              :command="action"
              :disabled="action.disabled"
              :class="getButtonClass(action)"
            >
              <div class="dropdown-item-content">
                <el-icon v-if="action.icon"><component :is="action.icon" /></el-icon>
                <span>{{ action.label }}</span>
              </div>
            </el-dropdown-item>
          </template>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<style scoped>
/* var() fallback为防御性编码，确保CSS变量未定义时有默认样式 */
.action-buttons {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: nowrap;
}

/* 统一按钮样式 */
.action-buttons :deep(.el-button) {
  padding: 4px 8px;
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: 500;
  transition: all 0.2s ease;

  &:hover {
    opacity: 0.8;
  }

  &.is-danger {
    color: var(--el-color-danger, #F56C6C);

    &:hover {
      color: var(--el-color-danger-light-3, #f89898);
    }
  }

  &.is-disabled {
    cursor: not-allowed;
    opacity: 0.45;
    
    &:hover {
      opacity: 0.45;
    }
  }
}

/* 分隔线 */
.action-divider {
  width: 1px;
  height: 14px;
  background: var(--el-border-color, #dcdfe6);
  margin: 0 4px;
}

/* 更多按钮 */
.more-button {
  .more-icon {
    margin-left: 2px;
    font-size: 10px;
    transition: transform 0.3s ease;
  }

  &:hover .more-icon {
    transform: rotate(180deg);
  }
}

/* 下拉菜单样式 */
.action-dropdown-menu {
  min-width: 140px;
  max-height: 300px;
  overflow-y: auto;

  .el-dropdown-menu__item {
    padding: 8px 16px;
    line-height: 1.4;

    &:hover {
      background: var(--el-fill-color-light, #f5f7fa);
    }

    &.is-danger {
      color: var(--el-color-danger, #F56C6C);
    }

    &.is-disabled {
      color: var(--el-text-color-placeholder, #c0c4cc);
      cursor: not-allowed;
    }
  }
}

/* 下拉菜单项内容 */
.dropdown-item-content {
  display: flex;
  align-items: center;
  gap: 8px;

  .el-icon {
    font-size: 14px;
    flex-shrink: 0;
  }

  span {
    flex: 1;
  }

  .submenu-arrow {
    font-size: 10px;
    color: var(--el-text-color-placeholder, #c0c4cc);
  }
}

/* 子菜单项样式 */
.submenu-items {
  margin-top: 6px;
  padding-top: 6px;
  border-top: 1px solid var(--el-border-color-lighter, #e4e7ed);
  
  .submenu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    cursor: pointer;
    transition: all 0.2s ease;
    border-radius: 4px;
    font-size: 13px;
    color: var(--el-text-color-regular, #606266);
    
    .child-icon {
      font-size: 14px;
      flex-shrink: 0;
      opacity: 0.7;
    }
    
    &:hover {
      background: var(--el-fill-color-light, #f5f7fa);
      color: var(--el-color-primary, #409eff);
      
      .child-icon {
        opacity: 1;
        color: var(--el-color-primary, #409eff);
      }
    }

    &.is-danger:hover {
      background: var(--el-color-danger-light-9, #fef0f0);
      color: var(--el-color-danger, #F56C6C);
      
      .child-icon {
        color: var(--el-color-danger, #F56C6C);
      }
    }

    &.is-warning:hover {
      background: var(--el-color-warning-light-9, #fdf6ec);
      color: var(--el-color-warning, #E6A23C);
      
      .child-icon {
        color: var(--el-color-warning, #E6A23C);
      }
    }

    &.is-success:hover {
      background: var(--el-color-success-light-9, #f0f9eb);
      color: var(--el-color-success, #67C23A);
      
      .child-icon {
        color: var(--el-color-success, #67C23A);
      }
    }

    &.is-disabled {
      cursor: not-allowed;
      opacity: 0.45;
      
      &:hover {
        background: transparent;
        color: var(--el-text-color-placeholder, #c0c4cc);
        
        .child-icon {
          color: var(--el-text-color-placeholder, #c0c4cc);
        }
      }
    }
  }
}

/* 响应式适配 */
@media (max-width: 768px) {
  .action-buttons {
    gap: var(--fts-space-1);
  }

  .action-buttons :deep(.el-button) {
    padding: 4px 6px;
    font-size: 11px;
  }
}
</style>
