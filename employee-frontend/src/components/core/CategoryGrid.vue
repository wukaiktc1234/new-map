<script setup lang="ts">
/**
 * CategoryGrid 分类网格组件（增强版）
 *
 * 设计理念：
 * - 图标区域使用渐变背景 + 微妙光晕
 * - hover时图标放大 + 背景加深 + 轻微上浮
 * - 点击时内凹反馈
 * - 支持深色模式自动适配
 */

export interface GridItem {
  id: string
  label: string
  icon: string
  route?: string
  color?: string
}

export interface GridGroup {
  title?: string
  items: GridItem[]
}

const props = withDefaults(defineProps<{
  groups: GridGroup[]
  columns?: number
}>(), {
  columns: 4,
})

const emit = defineEmits<{
  select: [item: GridItem]
}>()

function handleSelect(item: GridItem) {
  if (item.route) return // 有路由的由外部处理
  emit('select', item)
}
</script>

<template>
  <div class="category-grid">
    <!-- 分组标题 -->
    <div v-for="(group, gIdx) in groups" :key="gIdx" class="grid-group">
      <div v-if="group.title" class="grid-group-title">{{ group.title }}</div>
      <div class="grid-items" :style="{ gridTemplateColumns: `repeat(${columns}, 1fr)` }">
        <button
          v-for="(item, iIdx) in group.items"
          :key="item.id"
          class="grid-item"
          :class="[`grid-item--${iIdx % 5}`]"
          @click="handleSelect(item)"
        >
          <span class="grid-icon-wrap">
            <span class="grid-icon">{{ item.icon }}</span>
          </span>
          <span class="grid-label">{{ item.label }}</span>
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.category-grid {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.grid-group-title {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
  padding: 0 var(--fts-space-2);
  margin-bottom: calc(var(--fts-space-1) * -1);
  font-weight: 500;
  letter-spacing: 0.02em;
}

.grid-items {
  display: grid;
  gap: var(--fts-space-3);
}

.grid-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-4) var(--fts-space-2);
  border-radius: var(--fts-radius-lg);
  background: transparent;
  border: none;
  cursor: pointer;
  
  /* 增强交互：hover上浮+图标缩放 */
  transition: transform var(--fts-duration-normal) var(--fts-easing-spring),
              background-color var(--fts-duration-fast) ease;

  &:hover {
    background-color: var(--fts-bg-hover);
    transform: translateY(-2px);

    .grid-icon-wrap {
      box-shadow: 0 4px 14px rgba(var(--fts-primary-rgb), 0.12);
    }

    .grid-icon {
      transform: scale(1.1);
    }
  }

  &:active {
    transform: scale(0.96) translateY(0);
    
    .grid-icon-wrap {
      box-shadow: inset 0 2px 6px var(--fts-bg-hover);
    }
  }
}

/* 图标容器：渐变背景 + 圆形 */
.grid-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: var(--fts-radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  /* 使用CSS变量控制渐变，适配深色模式 */
  background: linear-gradient(
    145deg,
    var(--fts-bg-tertiary) 0%,
    rgba(var(--fts-primary-rgb), 0.08) 100%
  );
  transition:
    background-color var(--fts-duration-normal) var(--fts-easing-default),
    box-shadow var(--fts-duration-normal) var(--fts-easing-default),
    transform var(--fts-duration-normal) var(--fts-easing-default);
  position: relative;

  /* 微妙的顶部高光（玻璃质感） */
  &::before {
    content: '';
    position: absolute;
    inset: 1px;
    border-radius: inherit;
    background: linear-gradient(
      180deg,
      var(--fts-overlay-light) 0%,
      transparent 50%
    );
    pointer-events: none;
  }
}

.grid-icon {
  font-size: 22px;
  line-height: 1;
  transition: transform var(--fts-duration-fast) var(--fts-easing-spring);
  position: relative;
  z-index: 1;
}

.grid-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  text-align: center;
  line-height: 1.35;
  font-weight: 500;
  transition: color var(--fts-duration-fast) ease;

  .grid-item:hover & {
    color: var(--fts-text-primary);
  }
}
</style>
