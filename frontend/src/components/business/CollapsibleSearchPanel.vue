<script setup lang="ts">
/**
 * 可折叠搜索面板组件
 *
 * 功能：
 * 1. 高频筛选条件始终显示（primary slot）
 * 2. 低频筛选条件可折叠（secondary slot）
 * 3. 展开/收起动画流畅（< 300ms）
 * 4. 显示已选低频条件数量提示
 * 5. 箭头图标随状态旋转
 */
import { ref } from 'vue'

interface Props {
  /** 低频筛选条件的选中数量（用于显示"已选 N 项"提示） */
  selectedSecondaryCount?: number
  /** 默认是否展开（默认 false） */
  defaultExpanded?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  selectedSecondaryCount: 0,
  defaultExpanded: false,
})

const emit = defineEmits<{
  (e: 'toggle', expanded: boolean): void
}>()

const expanded = ref(props.defaultExpanded)

function toggle() {
  expanded.value = !expanded.value
  emit('toggle', expanded.value)
}
</script>

<template>
  <div class="collapsible-search-panel">
    <!-- 高频筛选区域（始终显示） -->
    <div class="primary-filters">
      <slot name="primary" />
    </div>

    <!-- 低频筛选区域（可折叠） -->
    <el-collapse-transition>
      <div v-show="expanded" class="secondary-filters">
        <slot name="secondary" />
      </div>
    </el-collapse-transition>

    <!-- 展开/收起按钮 -->
    <div class="toggle-btn">
      <el-button link type="primary" @click="toggle">
        {{ expanded ? '收起筛选' : '更多筛选' }}
        <el-icon :class="{ 'is-expanded': expanded }">
          <ArrowDown />
        </el-icon>
        <span v-if="selectedSecondaryCount > 0" class="filter-count">
          （已选 {{ selectedSecondaryCount }} 项）
        </span>
      </el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.collapsible-search-panel {
  width: 100%;

  .primary-filters {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-3);
    align-items: center;
  }

  .secondary-filters {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-3);
    align-items: center;
    margin-top: var(--fts-space-3);
    padding-top: var(--fts-space-3);
    border-top: 1px solid var(--fts-border-color-lighter);
  }

  .toggle-btn {
    margin-top: var(--fts-space-3);
    padding-top: var(--fts-space-3);
    border-top: 1px solid var(--fts-border-color-lighter);
    text-align: right;

    .el-button {
      font-size: var(--fts-font-size-sm);

      .el-icon {
        margin-left: 4px;
        transition: transform 0.3s ease;

        &.is-expanded {
          transform: rotate(180deg);
        }
      }

      .filter-count {
        margin-left: 8px;
        color: var(--fts-text-color-secondary);
        font-size: var(--fts-font-size-xs);
      }
    }
  }
}
</style>
