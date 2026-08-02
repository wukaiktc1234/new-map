<script setup lang="ts">
/**
 * 工具栏组件 - 专业设计
 * 布局：左侧主要操作 | 右侧工具区域
 */
import { ref } from 'vue'

interface SortOption {
  label: string
  value: string
}

interface Props {
  searchPlaceholder?: string
  sortOptions?: SortOption[]
}

withDefaults(defineProps<Props>(), {
  searchPlaceholder: '搜索...',
  sortOptions: () => []
})

const emit = defineEmits<{
  'search': [keyword: string]
  'refresh': []
  'sort': [sortValue: string]
}>()

const keyword = ref('')
const currentSort = ref('')

function handleSearch() {
  emit('search', keyword.value)
}

function handleRefresh() {
  emit('refresh')
}

function handleSortChange(sortValue: string) {
  currentSort.value = sortValue
  emit('sort', sortValue)
}
</script>

<template>
  <div class="toolbar">
    <!-- 左侧：主要操作区 -->
    <div class="toolbar__left">
      <slot name="left" />
    </div>

    <!-- 右侧：工具区 -->
    <div class="toolbar__right">
      <!-- 搜索框 -->
      <div class="toolbar__search">
        <el-input
          v-model="keyword"
          :placeholder="searchPlaceholder"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>

      <!-- 分隔线 -->
      <div class="toolbar__divider" />

      <!-- 排序下拉 -->
      <el-dropdown v-if="sortOptions.length > 0" @command="handleSortChange">
        <el-button text>
          <el-icon><Sort /></el-icon>
          排序
          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="option in sortOptions"
              :key="option.value"
              :command="option.value"
              :class="{ 'is-active': currentSort === option.value }"
            >
              {{ option.label }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>

      <!-- 刷新按钮 -->
      <el-button text @click="handleRefresh">
        <el-icon><Refresh /></el-icon>
      </el-button>

      <!-- 右侧插槽 -->
      <slot name="right" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-bottom: 1px solid var(--fts-border-secondary);
  min-height: 64px;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  border-radius: var(--fts-dynamic-radius, 0) var(--fts-dynamic-radius, 0) 0 0;
}

.toolbar__left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  flex-shrink: 0;
}

.toolbar__right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
  margin-left: auto; // 推到右侧
}

.toolbar__search {
  width: 280px;
  min-width: 200px; // 最小宽度

  :deep(.el-input__inner) {
    height: 36px;
    line-height: 36px;
  }

  :deep(.el-input__prefix) {
    display: flex;
    align-items: center;
  }
}

.toolbar__divider {
  width: 1px;
  height: 24px;
  background: var(--fts-border-secondary);
  margin: 0 var(--fts-space-2);
  flex-shrink: 0;
}

// 响应式适配
@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar__right {
    margin-left: 0;
    justify-content: flex-end;
  }

  .toolbar__search {
    width: 100%;
  }
}
</style>
