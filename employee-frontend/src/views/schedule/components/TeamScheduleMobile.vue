<template>
  <div class="team-schedule-mobile">
    <!-- 搜索框 -->
    <div class="search-bar">
      <el-input
        :model-value="searchKeyword"
        placeholder="搜索同事"
        clearable
        prefix-icon="Search"
        size="default"
        @update:model-value="(val: string) => $emit('search', val)"
        @clear="$emit('search', '')"
      />
    </div>

    <!-- 加载中骨架屏 -->
    <template v-if="loading">
      <div class="skeleton-list">
        <div v-for="i in 3" :key="i" class="skeleton-row">
          <div class="skeleton-avatar"></div>
          <div class="skeleton-text skeleton-text--short"></div>
          <div class="skeleton-text"></div>
        </div>
      </div>
    </template>

    <!-- 空状态 -->
    <template v-else-if="groupedColleagues.length === 0">
      <EmptyState title="暂无同事排班数据" description="当前条件下没有找到匹配的排班信息" />
    </template>

    <!-- 岗位分组列表 -->
    <template v-else>
      <div
        v-for="group in groupedColleagues"
        :key="group.groupName"
        class="position-group"
      >
        <div class="position-group__title">{{ group.groupName }}</div>
        <ColleagueItem
          v-for="item in group.items"
          :key="item.employeeId"
          :colleague="item"
          @click-colleague="(id: string) => $emit('click-colleague', id)"
        />
      </div>

      <!-- 查看更多按钮 -->
      <div v-if="hasMore" class="load-more">
        <el-button link type="primary" size="small" @click="$emit('load-more')">
          查看更多（共{{ totalCount }}人）
        </el-button>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
/**
 * TeamScheduleMobile - 团队视图移动端组件（Layer 2）
 *
 * 按岗位分组展示同事排班列表，支持搜索和分页加载。
 * 自己始终置顶在第一个分组内。
 */
import { computed } from 'vue'
import EmptyState from '@/components/core/EmptyState.vue'
import ColleagueItem from './ColleagueItem.vue'
import type { TeamColleagueItem } from '@/types/schedule'

interface Props {
  /** 同事列表 */
  colleagues: TeamColleagueItem[]
  /** 加载状态 */
  loading?: boolean
  /** 搜索关键词 */
  searchKeyword?: string
  /** 总人数（分页用） */
  totalCount?: number
  /** 是否还有更多数据 */
  hasMore?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  loading: false,
  searchKeyword: '',
  totalCount: 0,
  hasMore: false,
})

const emit = defineEmits<{
  (e: 'search', keyword: string): void
  (e: 'click-colleague', id: string): void
  (e: 'load-more'): void
}>()

/** 按岗位分组后的同事列表 */
interface PositionGroup {
  groupName: string
  items: TeamColleagueItem[]
}

/** 将同事按 positionGroup 分组，自己置顶 */
const groupedColleagues = computed((): PositionGroup[] => {
  const list = [...props.colleagues]

  // 分离自己和其他人
  const selfList = list.filter((c) => c.isSelf)
  const otherList = list.filter((c) => !c.isSelf)

  // 按 positionGroup 分组
  const groupMap = new Map<string, TeamColleagueItem[]>()

  // 先把自己放入其所属组（确保置顶）
  selfList.forEach((c) => {
    const key = c.positionGroup || '未分组'
    if (!groupMap.has(key)) {
      groupMap.set(key, [])
    }
    groupMap.get(key)!.push(c)
  })

  // 再放入其他人
  otherList.forEach((c) => {
    const key = c.positionGroup || '未分组'
    if (!groupMap.has(key)) {
      groupMap.set(key, [])
    }
    groupMap.get(key)!.push(c)
  })

  return Array.from(groupMap.entries()).map(([groupName, items]) => ({
    groupName,
    items,
  }))
})
</script>

<style scoped lang="scss">
.team-schedule-mobile {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3);
}

.search-bar {
  flex-shrink: 0;
}

/* ===== 骨架屏 ===== */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) 0;
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background-color: var(--fts-bg-white);
  border-radius: var(--fts-radius-md, 8px);
}

.skeleton-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  flex-shrink: 0;
}

.skeleton-text {
  height: 16px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--fts-bg-tertiary) 25%,
    var(--fts-bg-secondary) 50%,
    var(--fts-bg-tertiary) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;

  &--short {
    width: 60px;
    flex-shrink: 0;
  }

  &:not(&--short) {
    flex: 1;
  }
}

@keyframes shimmer {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}

/* ===== 岗位分组 ===== */
.position-group {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  &__title {
    padding: var(--fts-space-1) var(--fts-space-1);
    font-size: var(--fts-font-size-xs);
    font-weight: 600;
    color: var(--fts-text-tertiary);
    letter-spacing: 0.5px;
    text-transform: uppercase;
  }
}

/* ===== 查看更多 ===== */
.load-more {
  display: flex;
  justify-content: center;
  padding: var(--fts-space-2) 0;
}
</style>
