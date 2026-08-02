<script setup lang="ts">
/**
 * 表格操作栏组件
 */
import { ref } from 'vue'

interface Props {
  loading?: boolean
  columns?: { prop: string; label: string; visible?: boolean }[]
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  columns: () => []
})

const emit = defineEmits<{
  'search': []
  'refresh': []
  'fullscreen': []
  'column-change': [columns: string[]]
}>()

const isFullscreen = ref(false)

function handleFullscreen() {
  isFullscreen.value = !isFullscreen.value
  emit('fullscreen')
}
</script>

<template>
  <div class="table-action-bar">
    <div class="table-action-bar__left">
      <slot />
    </div>
    <div class="table-action-bar__right">
      <el-tooltip content="搜索" placement="top">
        <el-button circle size="small" @click="emit('search')">
          <el-icon><Search /></el-icon>
        </el-button>
      </el-tooltip>
      
      <el-tooltip content="刷新" placement="top">
        <el-button circle size="small" :loading="loading" @click="emit('refresh')">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </el-tooltip>
      
      <el-tooltip content="全屏" placement="top">
        <el-button circle size="small" @click="handleFullscreen">
          <el-icon>
            <FullScreen v-if="!isFullscreen" />
            <Close v-else />
          </el-icon>
        </el-button>
      </el-tooltip>
      
      <el-dropdown v-if="columns.length > 0" trigger="click">
        <el-button circle size="small">
          <el-icon><Setting /></el-icon>
        </el-button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item
              v-for="col in columns"
              :key="col.prop"
            >
              <el-checkbox :model-value="col.visible !== false">
                {{ col.label }}
              </el-checkbox>
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped lang="scss">
.table-action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-3) 0;
}

.table-action-bar__left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
}

.table-action-bar__right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}
</style>
