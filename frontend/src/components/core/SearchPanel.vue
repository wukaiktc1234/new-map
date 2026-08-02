<script setup lang="ts">
/**
 * 搜索面板组件
 */
import { ref } from 'vue'

interface Props {
  modelValue: Record<string, unknown>
  showAdvanced?: boolean
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: Record<string, unknown>]
  'search': []
  'reset': []
}>()

const isExpanded = ref(false)

function handleSearch() {
  emit('search')
}

function handleReset() {
  emit('reset')
}

function toggleAdvanced() {
  isExpanded.value = !isExpanded.value
}
</script>

<template>
  <div class="search-panel">
    <div class="search-panel__basic">
      <div class="search-panel__inputs">
        <slot name="basic" />
      </div>
      <div class="search-panel__actions">
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon>
          查询
        </el-button>
        <el-button @click="handleReset">
          <el-icon><RefreshRight /></el-icon>
          重置
        </el-button>
        <el-button
          v-if="showAdvanced"
          link
          type="primary"
          @click="toggleAdvanced"
        >
          {{ isExpanded ? '收起' : '高级搜索' }}
          <el-icon>
            <ArrowUp v-if="isExpanded" />
            <ArrowDown v-else />
          </el-icon>
        </el-button>
      </div>
    </div>
    <el-collapse-transition>
      <div v-show="isExpanded && showAdvanced" class="search-panel__advanced">
        <slot name="advanced" />
      </div>
    </el-collapse-transition>
  </div>
</template>

<style scoped lang="scss">
.search-panel {
  background: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg);
  padding: var(--fts-space-5);
  box-shadow: var(--fts-shadow-sm);
  border: 1px solid var(--fts-border-secondary);
}

.search-panel__basic {
  display: flex;
  align-items: center;
  gap: var(--fts-space-4);
  flex-wrap: wrap;
  min-height: 40px;
}

.search-panel__inputs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-3);
  flex: 1;
  min-width: 0;
}

.search-panel__actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-shrink: 0;
}

.search-panel__advanced {
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);
  display: flex;
  flex-wrap: wrap;
  gap: var(--fts-space-3);
}
</style>
