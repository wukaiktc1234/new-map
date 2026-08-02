<script setup lang="ts">
/**
 * AppealListPage - 我的申诉列表页
 *
 * 展示用户提交的所有申诉/举报记录
 * 支持状态筛选：全部/处理中/已完成
 */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Warning, ChatDotSquare } from '@element-plus/icons-vue'
import { PageContainer, StatusTag, EmptyState, FilterTabs, StatCard } from '@/components/core'
import { appealApi, type AppealItem, type AppealStatus } from '@/api/appeal'
import { appealTypeLabelMap, convertAppealStatus } from '@/api/converters/appeal-converters'

const router = useRouter()
const loading = ref(false)
const error = ref<string | null>(null)
const appeals = ref<AppealItem[]>([])
const activeTab = ref<'all' | 'processing' | 'resolved'>('all')

/** 统计数据 */
const appealStats = computed(() => ({
  total: appeals.value.length,
  processing: appeals.value.filter(a => a.status === 'processing').length,
  resolved: appeals.value.filter(a => a.status === 'resolved').length,
}))

/** 筛选选项（FilterTabs 格式） */
const filterOptions = computed(() => [
  { value: 'all', label: '全部' },
  { value: 'processing', label: '处理中' },
  { value: 'resolved', label: '已完成' },
])

/** 筛选后的列表 */
const filteredList = computed(() => {
  if (activeTab.value === 'all') return appeals.value
  return appeals.value.filter(a => a.status === activeTab.value)
})

/** 加载数据 */
async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await appealApi.getList({ page: 1, size: 50 })
    appeals.value = res?.records || []
  } catch (e: unknown) {
    // 响应拦截器已处理错误提示，此处仅记录状态用于UI展示
    appeals.value = []
    error.value = e instanceof Error ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/** 跳转详情 */
function goToDetail(item: AppealItem) {
  router.push(`/appeals/${item.appealId}`)
}

/** 跳转创建 */
function goCreate(type: 'penalty' | 'complaint') {
  router.push(`/appeals/create/${type}`)
}

onMounted(() => {
  fetchData()
})
</script>

<template>
  <PageContainer title="我的申诉" :loading="loading">
    <div class="appeal-list-page">
      <!-- 页面说明 -->
      <p class="page-desc">您的申诉将直接提交至独立受理部门，全程保密处理</p>

      <!-- 统计概览 -->
      <section class="stats-row">
        <StatCard variant="grid" compact :value="appealStats.total" label="全部" />
        <StatCard variant="grid" compact :value="appealStats.processing" label="处理中" />
        <StatCard variant="grid" compact :value="appealStats.resolved" label="已完成" />
      </section>

      <!-- 快捷入口 -->
      <section class="quick-actions">
        <button class="action-card action-card--warning" @click="goCreate('penalty')">
          <span class="action-icon"><el-icon :size="20"><Warning /></el-icon></span>
          <div class="action-body">
            <span class="action-label">发起处罚申诉</span>
            <span class="action-desc">对处罚决定有异议时</span>
          </div>
        </button>
        <button class="action-card action-card--error" @click="goCreate('complaint')">
          <span class="action-icon"><el-icon :size="20"><ChatDotSquare /></el-icon></span>
          <div class="action-body">
            <span class="action-label">提交投诉举报</span>
            <span class="action-desc">发现违规行为时</span>
          </div>
        </button>
      </section>

      <!-- 状态筛选Tab -->
      <FilterTabs v-model="activeTab" :options="filterOptions" variant="pill" />

      <!-- 错误状态 -->
      <div v-if="!loading && error" class="error-state">
        <div class="error-icon">!</div>
        <h3 class="error-title">数据加载失败</h3>
        <p class="error-desc">{{ error }}</p>
        <button class="retry-btn" @click="fetchData">重新加载</button>
      </div>

      <!-- 空状态 -->
      <EmptyState
        v-if="!loading && filteredList.length === 0"
        title="暂无申诉记录"
        description="提交的申诉会在这里显示"
      />

      <!-- 申诉卡片列表 -->
      <section v-else class="list-section">
        <article
          v-for="item in filteredList"
          :key="item.appealId"
          class="appeal-card"
          :class="item.type === 'penalty' ? 'appeal-card--warning' : 'appeal-card--error'"
          @click="goToDetail(item)"
        >
          <!-- 头部：类型 + 状态 -->
          <div class="card-header">
            <span
              class="type-badge"
              :class="item.type === 'penalty' ? 'type-badge--warning' : 'type-badge--error'"
            >
              {{ appealTypeLabelMap[item.type] || item.type }}
            </span>
            <StatusTag
              :status="convertAppealStatus(item.status)"
              size="small"
              variant="light"
            />
            <span v-if="item.anonymousFlag" class="anonymous-tag">匿名</span>
          </div>

          <!-- 标题 -->
          <h3 class="card-title">{{ item.title }}</h3>

          <!-- 底部：时间 + 附件数 -->
          <div class="card-footer">
            <span class="card-time">{{ item.createTime }}</span>
            <span v-if="item.attachmentCount > 0" class="card-attach">
              {{ item.attachmentCount }}个附件
            </span>
          </div>
        </article>
      </section>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.appeal-list-page {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
}

.page-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin: 0;
  line-height: 1.5;
}

/* ===== 统计概览 ===== */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--fts-space-2);
}

/* ===== 快捷入口 ===== */
.quick-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-3);
}

.action-card {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3);
  padding: var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-left: 3px solid transparent;
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-sm);
    transform: translateY(-1px);
  }

  &:active { transform: scale(0.98) translateY(0); }

  &--warning {
    border-left-color: var(--fts-warning);
    .action-icon { background: rgba(var(--fts-warning-rgb), 0.1); color: var(--fts-warning); }
  }

  &--error {
    border-left-color: var(--fts-error);
    .action-icon { background: rgba(var(--fts-error-rgb), 0.08); color: var(--fts-error); }
  }
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--fts-radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-body {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.action-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.action-desc {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-quaternary);
}

/* ===== 列表 ===== */
.list-section {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.appeal-card {
  padding: var(--fts-space-4);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-left: 3px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all var(--fts-duration-fast) ease;

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
    transform: translateY(-1px);
  }

  &:active { transform: scale(0.99) translateY(0); }

  &--warning { border-left-color: var(--fts-warning); }
  &--error { border-left-color: var(--fts-error); }
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-2);
}

.type-badge {
  font-size: var(--fts-font-size-2xs);
  font-weight: 600;
  padding: 2px 8px;
  border-radius: var(--fts-radius-full);

  &--warning { background: rgba(var(--fts-warning-rgb), 0.1); color: var(--fts-warning); }
  &--error { background: rgba(var(--fts-error-rgb), 0.08); color: var(--fts-error); }
}

.anonymous-tag {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-info);
  background: rgba(var(--fts-info-rgb), 0.08);
  padding: 1px 6px;
  border-radius: var(--fts-radius-full);
  margin-left: auto;
}

.card-title {
  font-size: var(--fts-font-size-base);
  font-weight: 500;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-2);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--fts-space-2);
  border-top: 1px solid var(--fts-border-secondary);
}

.card-time {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

.card-attach {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-quaternary);
}

/* ===== 错误状态 ===== */
.error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-8) var(--fts-space-4);
  text-align: center;
}

.error-icon {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background-color: rgba(var(--fts-error-rgb), 0.1);
  color: var(--fts-error);
  font-size: 24px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: var(--fts-space-3);
}

.error-title {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-1);
}

.error-desc {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-quaternary);
  margin: 0 0 var(--fts-space-4);
  max-width: 280px;
}

.retry-btn {
  padding: var(--fts-space-2) var(--fts-space-6);
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-on-primary);
  background-color: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: opacity var(--fts-duration-fast) ease;

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.97); }
}
</style>
