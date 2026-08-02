<template>
  <div class="table-actions" v-if="actions.length > 0">
    <template v-if="actions.length <= 3">
      <el-button
        v-for="(item, index) in actions"
        :key="index"
        :type="getButtonType(item.type)"
        size="small"
        link
        :class="`action-btn--${item.type}`"
        v-permission="item.permission"
        @click="item.action"
      >
        {{ item.label }}
      </el-button>
    </template>
    <el-dropdown v-else trigger="click" @command="(cmd: ActionItem) => cmd.action()">
      <el-button type="primary" size="small" link class="action-btn--primary">
        操作 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
      </el-button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item
            v-for="(item, index) in actions"
            :key="index"
            :command="item"
            :divided="index > 0 && item.type === 'danger'"
          >
            {{ item.label }}
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>
  </div>
</template>

<script setup lang="ts">
import { ArrowDown } from '@element-plus/icons-vue'

export interface ActionItem {
  label: string
  type: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
  action: () => void
  permission?: string
}

defineProps<{
  actions: ActionItem[]
}>()

function getButtonType(type: string): '' | 'default' | 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  return type as ActionItem['type']
}
</script>

<style scoped lang="scss">
.table-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;

  /* 使用更深层次的选择器覆盖Element Plus默认样式 */
  .el-button.is-link {
    font-size: 13px;
    font-weight: 500;
    padding: 4px 8px;
    border-radius: 4px;
    min-width: auto;
    transition: all 0.2s ease;
    background-color: transparent;
    border: none;
    box-shadow: none;

    &:hover {
      text-decoration: underline;
      opacity: 0.8;
    }
  }

  /* 主要操作按钮 - 使用CSS变量支持深色模式 */
  .action-btn--primary {
    color: var(--fts-primary);

    &:hover {
      color: var(--fts-primary-hover);
    }

    html.dark & {
      color: var(--fts-primary-hover);

      &:hover {
        color: var(--fts-primary-bg);
      }
    }
  }

  /* 成功操作按钮 */
  .action-btn--success {
    color: var(--fts-success);

    &:hover {
      color: var(--fts-success-hover);
    }

    html.dark & {
      color: var(--fts-success-hover);

      &:hover {
        color: var(--fts-success-bg);
      }
    }
  }

  /* 警告操作按钮 */
  .action-btn--warning {
    color: var(--fts-warning);

    &:hover {
      color: var(--fts-warning-hover);
    }

    html.dark & {
      color: var(--fts-warning-hover);

      &:hover {
        color: var(--fts-warning-bg);
      }
    }
  }

  /* 危险操作按钮 */
  .action-btn--danger {
    color: var(--fts-error);

    &:hover {
      color: var(--fts-error-hover);
    }

    html.dark & {
      color: var(--fts-error-hover);

      &:hover {
        color: var(--fts-error-bg);
      }
    }
  }

  /* 信息操作按钮 */
  .action-btn--info {
    color: var(--fts-text-secondary);

    &:hover {
      color: var(--fts-text-secondary);
      opacity: 0.8;
    }

    html.dark & {
      color: var(--fts-text-quaternary);

      &:hover {
        color: var(--fts-text-quaternary);
        opacity: 0.8;
      }
    }
  }

  /* 默认操作按钮 */
  .action-btn--default {
    color: var(--fts-text-primary);

    &:hover {
      color: var(--fts-text-primary);
    }

    html.dark & {
      color: var(--fts-text-quaternary);

      &:hover {
        color: var(--fts-text-secondary);
      }
    }
  }
}
</style>
