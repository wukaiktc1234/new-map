<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { usePermissionStore, getIconComponent } from '@/stores/permission'
import { PageContainer } from '@/components/core'
import { useStaggerAnimation } from '@/composables/useStaggerAnimation'

const router = useRouter()
const permission = usePermissionStore()

const searchKeyword = ref('')

/** 所有功能分类（来自权限系统） */
const allGroups = computed(() => permission.getOfficeGroups())

const activeGroupKey = ref<string>(allGroups.value[0]?.group.key || '')

// 交错入场动画
const stagger = useStaggerAnimation({ staggerDelay: 50, duration: 400, type: 'fade-up' })

/** 当前选中分类下的功能模块 */
const activeModules = computed(() => {
  const found = allGroups.value.find(g => g.group.key === activeGroupKey.value)
  return found?.modules || []
})

/** 搜索过滤 */
const filteredModules = computed(() => {
  if (!searchKeyword.value.trim()) return activeModules.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return activeModules.value.filter(m => m.label.toLowerCase().includes(kw))
})

function handleModuleClick(route: string) {
  if (route) router.push(route)
}

const colorRgbMap: Record<string, string> = {
  'var(--fts-primary)': 'var(--fts-primary-rgb)',
  'var(--fts-success)': 'var(--fts-success-rgb)',
  'var(--fts-warning)': 'var(--fts-warning-rgb)',
  'var(--fts-error)': 'var(--fts-error-rgb)',
  'var(--fts-info)': 'var(--fts-info-rgb)',
}

function toRgba(color: string, opacity: number): string {
  const rgbVar = colorRgbMap[color]
  if (rgbVar) return `rgba(${rgbVar}, ${opacity})`
  if (color.startsWith('#')) {
    const hex = color.slice(1)
    const r = parseInt(hex.length === 3 ? hex[0] + hex[0] : hex.slice(0, 2), 16)
    const g = parseInt(hex.length === 3 ? hex[1] + hex[1] : hex.slice(2, 4), 16)
    const b = parseInt(hex.length === 3 ? hex[2] + hex[2] : hex.slice(4, 6), 16)
    return `rgba(${r}, ${g}, ${b}, ${opacity})`
  }
  return color
}
</script>

<template>
  <PageContainer title="办公中心">
    <template #headerActions>
      <div class="office-search">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索功能..."
          :prefix-icon="Search"
          clearable
          round
          size="default"
          class="search-input"
        />
      </div>
    </template>

    <div class="office-layout">
      <!-- 左侧分类导航 -->
      <nav class="office-nav">
        <button
          v-for="grp in allGroups"
          :key="grp.group.key"
          :class="['nav-item', { 'nav-item--active': grp.group.key === activeGroupKey }]"
          @click="activeGroupKey = grp.group.key"
        >
          <span class="nav-item__icon">
            <el-icon :size="20"><component :is="getIconComponent(grp.group.icon || 'Grid')" /></el-icon>
          </span>
          <span class="nav-item__label">{{ grp.group.title }}</span>
          <span v-if="grp.modules.length > 0" class="nav-item__count">{{ grp.modules.length }}</span>
        </button>
      </nav>

      <!-- 右侧功能内容区 -->
      <main class="office-content">
        <section class="modules-section">
          <div v-if="filteredModules.length > 0" ref="stagger.containerRef" class="module-grid">
            <button
              v-for="mod in filteredModules"
              :key="mod.id"
              class="module-card card-enhanced"
              @click="handleModuleClick(mod.route)"
            >
              <span class="module-card__icon">
                <el-icon :size="22"><component :is="getIconComponent(mod.icon)" /></el-icon>
              </span>
              <span class="module-card__label">{{ mod.label }}</span>
            </button>
          </div>
          <div v-else class="empty-hint">
            <span>{{ searchKeyword ? '未找到匹配的功能' : '该分类下暂无可用功能' }}</span>
          </div>
        </section>
      </main>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.office-search {
  display: flex;
  align-items: center;
  width: var(--fts-search-width, 220px);
  max-width: 45vw;
  min-width: 140px;

  @media (max-width: 767px) {
    max-width: none;
    width: 100%;
    min-width: 0;
  }
}

.search-input {
  --el-input-border-radius: var(--fts-radius-full);
  --el-input-bg-color: var(--fts-bg-tertiary);
  --el-input-text-color: var(--fts-text-primary);
  --el-input-placeholder-color: var(--fts-text-tertiary);
  --el-input-hover-border-color: var(--fts-border-hover);
  --el-input-focus-border-color: var(--fts-primary);

  :global(.el-input__wrapper) {
    box-shadow: none;
    padding: 4px 12px;

    &.is-focus {
      box-shadow: 0 0 0 2px rgba(var(--fts-primary-rgb), 0.15);
    }
  }

  :global(.el-input__prefix) {
    color: var(--fts-text-tertiary);
  }
}

// ========== 左右分栏布局 ==========
.office-layout {
  display: flex;
  gap: var(--fts-space-4);

  @media (max-width: 767px) {
    gap: var(--fts-space-3);
  }
}

// ========== 左侧分类导航 ==========
.office-nav {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  flex-shrink: 0;
  width: 96px;
  padding: var(--fts-space-2);
  background: var(--fts-bg-page);
  border-radius: var(--fts-radius-lg);

  @media (max-width: 767px) {
    width: 80px;
    padding: var(--fts-space-1);
    gap: 2px;
  }
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1);
  padding: var(--fts-space-3) var(--fts-space-2);
  border: none;
  border-radius: var(--fts-radius-md);
  background: transparent;
  cursor: pointer;
  position: relative;
  transition: var(--fts-transition-all);
  -webkit-tap-highlight-color: transparent;
  min-height: 72px;

  &:active:not(&--active) {
    background: var(--fts-bg-secondary);
    transform: scale(0.96);
  }

  &:hover:not(&--active) {
    background: var(--fts-bg-hover);
  }

  &--active {
    background: var(--fts-bg-card);
    box-shadow: var(--fts-shadow-sm);

    .nav-item__label {
      color: var(--fts-primary);
      font-weight: 700;
    }

    &::before {
      content: '';
      position: absolute;
      left: 4px;
      top: 50%;
      transform: translateY(-50%);
      width: 4px;
      height: 20px;
      border-radius: var(--fts-radius-full);
      background: var(--fts-primary);
    }
  }

  &__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--fts-text-secondary);
    transition: var(--fts-transition-transform);

    .nav-item--active & {
      transform: scale(1.1);
      color: var(--fts-primary);
    }
  }

  &__label {
    font-size: var(--fts-font-size-2xs);
    color: var(--fts-text-secondary);
    line-height: 1.2;
    text-align: center;
    font-weight: 500;
  }
}

// ========== 右侧功能内容区 ==========
.office-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

// ========== 功能网格区域 ==========
.modules-section {
  width: 100%;
}

.module-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--fts-space-4);
  width: 100%;

  @media (max-width: 767px) {
    gap: var(--fts-space-3);
  }

  @media (min-width: 768px) {
    grid-template-columns: repeat(3, 1fr);
  }
}

.module-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-5) var(--fts-space-3);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-lg);
  background: var(--fts-bg-card);
  cursor: pointer;
  min-height: 100px;
  transition: var(--fts-card-transition);
  box-shadow: var(--fts-shadow-sm);
  -webkit-tap-highlight-color: transparent;

  @media (max-width: 767px) {
    min-height: 88px;
    padding: var(--fts-space-4) var(--fts-space-3);
  }

  &:hover {
    border-color: var(--fts-primary-light);
    box-shadow: var(--fts-shadow-md);

    .module-card__icon {
      transform: scale(1.1);
    }
  }

  &__icon {
    width: 48px;
    height: 48px;
    border-radius: var(--fts-radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    // 对齐首页快捷入口：统一中性风格，不使用彩色
    color: var(--fts-text-secondary);
    background-color: var(--fts-bg-secondary);
    transition: var(--fts-transition-transform);
  }

  &__label {
    font-size: var(--fts-font-size-sm);
    font-weight: 600;
    color: var(--fts-text-primary);
    text-align: center;
    line-height: 1.3;
  }
}
</style>
