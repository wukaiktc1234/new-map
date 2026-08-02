<script setup lang="ts">
/**
 * 数据表格组件 - 现代极简风格
 *
 * 【自动操作列】当父组件传入 #actions 插槽时，自动在表格末尾追加"操作"列，
 *             无需在 columns 数组中手动定义 actions 列。
 */
import { ref, computed, useSlots } from 'vue'
import { vTableDragScroll } from '@/directives/tableDragScroll'

/** 数据表格列定义（对外导出供父组件类型标注使用） */
export interface DataTableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  fixed?: 'left' | 'right'
  slot?: string
  ellipsis?: boolean
  align?: 'left' | 'center' | 'right'
  showOverflowTooltip?: boolean
}

interface Props {
  data: any[]
  columns: DataTableColumn[]
  loading?: boolean
  selectable?: boolean
  stripe?: boolean
  border?: boolean
  hover?: boolean
  actionsWidth?: number | string
  /** 行数据的 Key，用于树形数据的 row-key（如 'categoryId'） */
  rowKey?: string | ((row: any) => string)
  /** 树形数据配置，如 { children: 'children', hasChildren: 'hasChildren' } */
  treeProps?: { children?: string; hasChildren?: string }
  /** 是否默认展开所有树形行 */
  defaultExpandAll?: boolean
  /** 自定义行类名（与 Element Plus el-table row-class-name 一致） */
  rowClassName?: (data: { row: any; rowIndex: number }) => string
}

withDefaults(defineProps<Props>(), {
  loading: false,
  selectable: true,
  stripe: true,
  border: false,
  hover: true,
  actionsWidth: 140,
  rowKey: undefined,
  treeProps: undefined,
  defaultExpandAll: false,
  rowClassName: undefined,
})

const emit = defineEmits<{
  'selection-change': [selection: any[]]
  'row-click': [row: any]
}>()

const tableRef = ref()
const slots = useSlots()
const hasActionsSlot = computed(() => !!slots.actions)

function handleSelectionChange(selection: any[]) {
  emit('selection-change', selection)
}

function handleRowClick(row: any) {
  emit('row-click', row)
}

function handleSortChange() {
  // 排序变化处理，可由父组件监听
}

defineExpose({ tableRef })
</script>

<template>
  <div class="data-table" v-table-drag-scroll>
    <el-table
      ref="tableRef"
      :data="data"
      :stripe="false"
      :border="border"
      :highlight-current-row="false"
      v-loading="loading"
      class="data-table"
      :class="{
        'data-table--striped': stripe,
        'data-table--hover': hover
      }"
      :row-key="rowKey"
      :tree-props="treeProps"
      :default-expand-all="defaultExpandAll"
      :row-class-name="rowClassName"
      @selection-change="handleSelectionChange"
      @sort-change="handleSortChange"
      @row-click="handleRowClick"
      table-layout="fixed"
    >
      <el-table-column
        v-if="selectable"
        type="selection"
        width="48"
        class-name="selection-column"
      />

      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :fixed="col.fixed"
        :show-overflow-tooltip="col.showOverflowTooltip"
        :class-name="col.ellipsis === false ? 'no-ellipsis-cell' : (col.ellipsis === undefined ? '' : 'ellipsis-cell')"
      >
        <template v-if="col.slot" #default="scope">
          <slot :name="col.slot" :row="scope.row" :$index="scope.$index" />
        </template>
      </el-table-column>

      <el-table-column
        v-if="hasActionsSlot"
        label="操作"
        :width="actionsWidth"
        class-name="actions-column"
      >
        <template #default="scope">
          <slot name="actions" :row="scope.row" :$index="scope.$index" />
        </template>
      </el-table-column>

      <slot />
    </el-table>
  </div>
</template>

<style scoped lang="scss">
.data-table {
  background: var(--fts-bg-card);
  padding: 0;
}

:deep(.el-table) {
  --el-table-header-bg-color: var(--fts-bg-card);
  --el-table-row-hover-bg-color: var(--fts-bg-hover);
  --el-table-border-color: transparent;

  &::before {
    display: none;
  }

  .el-table__header-wrapper {
    th {
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-text-secondary);
      font-size: var(--fts-font-size-sm);
      padding: var(--fts-space-3) var(--fts-space-3);
      border-bottom: 1px solid var(--fts-border-primary);

      .cell {
        display: flex;
        align-items: center;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: 0;
      }
    }
  }

  .el-table__row {
    td {
      padding: var(--fts-space-3) var(--fts-space-3);
      color: var(--fts-text-primary);
      font-size: var(--fts-font-size-base);
      border-bottom: 1px solid var(--fts-border-secondary);

      .cell {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        padding: 0;
      }

      &.no-ellipsis-cell {
        .cell {
          white-space: normal;
          overflow: visible;
          text-overflow: clip;
        }
      }

      &.ellipsis-cell {
        .cell {
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }
    }
  }

  // 斑马纹 - 通过CSS类控制，支持深色模式
  &.data-table--striped {
    .el-table__body {
      tr.el-table__row:nth-child(even) {
        td {
          background: var(--fts-bg-secondary);
        }

        &:hover td {
          background: var(--fts-bg-hover);
        }
      }
    }
  }

  // 悬停高亮 - 通过CSS类控制
  &.data-table--hover {
    .el-table__body {
      tr.el-table__row:hover > td {
        background-color: var(--fts-bg-hover) !important;
      }
    }
  }

  // 选择列特殊处理：复选框与内容左对齐
  th.selection-column,
  td.selection-column {
    padding: 0 !important;

    .cell {
      padding: 0 0 0 var(--fts-space-3) !important;
      justify-content: flex-start;
    }
  }

  .el-table__empty-block {
    padding: var(--fts-space-12) 0;
  }

  .caret-wrapper {
    display: none;
  }

  // 操作列样式：按钮水平排列整齐
  .actions-column .cell {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
  }

  .el-checkbox {
      .el-checkbox__inner {
        width: 18px;
        height: 18px;
        border-width: 2px;
        border-color: var(--fts-border-hover);
        border-radius: var(--fts-radius-sm, 4px);
        background-color: var(--fts-bg-card);
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
        box-shadow: none;

        // 勾选标记（√）- 使用Element Plus原生结构
        // 仅调整位置和颜色，不改变核心绘制逻辑
        &::after {
          // Element Plus原生：通过border绘制L形 + rotate(45deg)变成√
          // 我们只调整位置参数，保持原有绘制方式
          position: absolute;
          display: block;
          width: 4.5px;     // 横线宽度
          height: 8px;      // 竖线高度
          left: 6.5px;      // 左偏移（视觉居中）
          top: 5px;         // 上偏移（视觉居中）
          border: 2px solid transparent;  // 初始透明
          border-top: none;
          border-left: none;
          margin: 0;
          // 关键：不使用transform覆盖，让Element Plus自己管理
        }

        &:hover {
          border-color: var(--fts-primary);
          box-shadow: 0 0 0 3px rgba(var(--fts-primary-rgb, 64, 158, 255), 0.12);
        }
      }

      // 选中状态 - 让Element Plus原生逻辑生效
      &.is-checked .el-checkbox__inner {
        background-color: var(--fts-primary);
        border-color: var(--fts-primary);
        box-shadow: 0 2px 8px rgba(var(--fts-primary-rgb, 64, 158, 255), 0.35);
      }

      // 勾选标记显示白色（通过重复类选择器增加特异性，替代!important）
      &.is-checked.is-checked .el-checkbox__inner::after {
        border-color: var(--fts-text-inverse);
      }

      &.is-indeterminate .el-checkbox__inner {
        background-color: var(--fts-primary);
        border-color: var(--fts-primary);
        box-shadow: 0 2px 8px rgba(var(--fts-primary-rgb, 64, 158, 255), 0.35);
      }

      // 不确定状态勾选标记（通过重复类选择器增加特异性，替代!important）
      &.is-indeterminate.is-indeterminate .el-checkbox__inner::after {
        border-color: var(--fts-text-inverse);
      }

      &.is-checked .el-checkbox__inner {
        animation: checkbox-check 0.2s ease-in-out;
      }

      // 聚焦状态（键盘导航）
      &.is-focus .el-checkbox__inner {
        box-shadow: 0 0 0 3px rgba(var(--fts-primary-rgb, 64, 158, 255), 0.25);
      }

      // 禁用状态
      &.is-disabled .el-checkbox__inner {
        background-color: var(--fts-bg-secondary);
        border-color: var(--fts-border-secondary);
        cursor: not-allowed;

        &::after {
          border-color: var(--fts-text-disabled) !important;
        }
      }
    }
}

// 复选框选中动画
@keyframes checkbox-check {
  0% {
    transform: scale(0.8);
    opacity: 0;
  }
  50% {
    transform: scale(1.1);
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
