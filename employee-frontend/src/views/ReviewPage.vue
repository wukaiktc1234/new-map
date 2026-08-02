<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { PageContainer, EmptyState, StatusTag, StatCard } from '@/components/core'
import { reviewApi } from '@/api/review'
import type { ReviewItem, ReviewScore } from '@/types/review'
import { GradeLabels, GradeColors, ReviewDimensionLabels } from '@/types/review'

const router = useRouter()
const loading = ref(false)
const reviewList = ref<ReviewItem[]>([])
const pendingList = ref<ReviewItem[]>([])
const completedList = ref<ReviewItem[]>([])

// ========== 统计摘要计算 ==========
/** 总考核次数 */
const totalCount = computed(() => completedList.value.length)
/** 平均分（仅已完成且有分数的记录） */
const avgScore = computed(() => {
  const scored = completedList.value.filter(r => r.totalScore !== null)
  if (scored.length === 0) return '--'
  const sum = scored.reduce((acc, r) => acc + (r.totalScore || 0), 0)
  return (sum / scored.length).toFixed(1)
})
/** 最高分 */
const maxScore = computed(() => {
  if (completedList.value.length === 0) return '--'
  const scores = completedList.value
    .filter(r => r.totalScore !== null)
    .map(r => r.totalScore as number)
  return scores.length > 0 ? Math.max(...scores).toFixed(1) : '--'
})
/** 待处理数量 */
const pendingCount = computed(() => pendingList.value.length)

// ========== 截止日期格式化 ==========
/**
 * 根据截止日期计算显示文本
 * 剩余3天以内显示"剩余N天"，否则显示"截止 MM-DD"
 * 已过期显示"已过期"
 */
function formatDeadline(deadline: string | null): string {
  if (!deadline) return ''
  const now = new Date()
  const end = new Date(deadline)
  const diffMs = end.getTime() - now.getTime()
  const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24))

  if (diffDays < 0) return '已过期'
  if (diffDays <= 3) return `剩余 ${diffDays} 天`
  // 格式化为 MM-DD
  const month = String(end.getMonth() + 1).padStart(2, '0')
  const day = String(end.getDate()).padStart(2, '0')
  return `截止 ${month}-${day}`
}

/** 判断是否紧急（剩余2天以内或已过期） */
function isUrgent(deadline: string | null): boolean {
  if (!deadline) return false
  const now = new Date()
  const end = new Date(deadline)
  const diffDays = Math.ceil((end.getTime() - now.getTime()) / (1000 * 60 * 60 * 24))
  return diffDays <= 2
}

// ========== 已完成考核的维度得分（Mock数据补充） ==========
/** 各已完成考核的维度得分映射（实际应从 detail 接口获取） */
const scoreMap: Record<string, ReviewScore[]> = {
  rv001: [
    { dimension: 'attendance', selfScore: 95, managerScore: 95, maxScore: 100, weight: 20, comment: '' },
    { dimension: 'work_quality', selfScore: 85, managerScore: 82, maxScore: 100, weight: 30, comment: '' },
    { dimension: 'teamwork', selfScore: 90, managerScore: 88, maxScore: 100, weight: 15, comment: '' },
    { dimension: 'customer_service', selfScore: 85, managerScore: 90, maxScore: 100, weight: 15, comment: '' },
  ],
  rv002: [
    { dimension: 'attendance', selfScore: 100, managerScore: 100, maxScore: 100, weight: 20, comment: '' },
    { dimension: 'work_quality', selfScore: 88, managerScore: 90, maxScore: 100, weight: 30, comment: '' },
    { dimension: 'teamwork', selfScore: 90, managerScore: 92, maxScore: 100, weight: 15, comment: '' },
    { dimension: 'customer_service', selfScore: 88, managerScore: 90, maxScore: 100, weight: 15, comment: '' },
  ],
  rv003: [
    { dimension: 'attendance', selfScore: 80, managerScore: 75, maxScore: 100, weight: 20, comment: '' },
    { dimension: 'work_quality', selfScore: 82, managerScore: 78, maxScore: 100, weight: 30, comment: '' },
    { dimension: 'teamwork', selfScore: 85, managerScore: 80, maxScore: 100, weight: 15, comment: '' },
    { dimension: 'customer_service', selfScore: 80, managerScore: 78, maxScore: 100, weight: 15, comment: '' },
  ],
  rv004: [
    { dimension: 'attendance', selfScore: 90, managerScore: 92, maxScore: 100, weight: 20, comment: '' },
    { dimension: 'work_quality', selfScore: 85, managerScore: 84, maxScore: 100, weight: 30, comment: '' },
    { dimension: 'teamwork', selfScore: 88, managerScore: 86, maxScore: 100, weight: 15, comment: '' },
    { dimension: 'customer_service', selfScore: 85, managerScore: 85, maxScore: 100, weight: 15, comment: '' },
  ],
}

/** 获取考核的维度得分列表 */
function getItemScores(id: string): ReviewScore[] {
  return scoreMap[id] || []
}

// ========== 数据获取 ==========
async function fetchReviews() {
  loading.value = true
  try {
    const response = await reviewApi.getList({ page: 1, size: 20 })
    const all = response.records || []
    pendingList.value = all.filter(r => r.status === 'self_review' || r.status === 'manager_review' || r.status === 'hr_review')
    completedList.value = all.filter(r => r.status === 'completed' || r.status === 'appealing' || r.status === 'appeal_resolved')
    reviewList.value = all
  } catch {
    reviewList.value = []
    pendingList.value = []
    completedList.value = []
  } finally {
    loading.value = false
  }
}

function handleClickItem(id: string) {
  router.push(`/review/detail/${id}`)
}

onMounted(() => {
  fetchReviews()
})
</script>

<template>
  <PageContainer title="绩效考核" :loading="loading">
    <!-- 空状态：无任何考核记录 -->
    <template v-if="reviewList.length === 0 && !loading">
      <EmptyState
        icon="list"
        title="暂无考核记录"
        description="当前没有绩效考核数据"
      />
      <div class="empty-hint">
        <span class="empty-hint__text">还不了解考核制度？</span>
        <a class="empty-hint__link" href="javascript:void(0)">了解考核制度</a>
      </div>
    </template>

    <template v-else>
      <!-- ====== 统计摘要行 ====== -->
      <div class="stats-row">
        <StatCard :value="totalCount" label="总考核数" variant="info" compact />
        <StatCard :value="avgScore" label="平均分" variant="primary" compact />
        <StatCard :value="maxScore" label="最高分" variant="success" compact />
        <StatCard :value="pendingCount" label="待处理" variant="warning" compact />
      </div>

      <!-- ====== 待处理考核 ====== -->
      <section v-if="pendingList.length > 0" class="review-section">
        <h3 class="section-title">待处理考核</h3>
        <div class="card-list">
          <div
            v-for="item in pendingList"
            :key="item.id"
            class="review-card review-card--pending"
            :class="{
              'review-card--self': item.status === 'self_review',
              'review-card--manager': item.status === 'manager_review',
              'review-card--hr': item.status === 'hr_review',
            }"
            @click="handleClickItem(item.id)"
          >
            <div class="card-header">
              <div class="card-period">
                <span class="period-label">{{ item.periodLabel }}</span>
                <span class="period-type">{{ item.period === 'monthly' ? '月度' : item.period === 'quarterly' ? '季度' : '年度' }}考核</span>
              </div>
              <StatusTag :status="item.status" variant="badge" />
            </div>
            <div class="card-body">
              <span class="reviewer">评定人：{{ item.reviewerName }}</span>
              <span class="create-time">{{ item.createdAt.slice(5, 10) }} 发布</span>
            </div>
            <!-- 截止日期 / 倒计时 -->
            <div v-if="item.deadline" class="card-deadline" :class="{ 'card-deadline--urgent': isUrgent(item.deadline) }">
              <span class="deadline-icon">&#9200;</span>
              <span class="deadline-text">{{ formatDeadline(item.deadline) }}</span>
            </div>
            <div class="card-action">
              <span class="action-link">去处理 &rarr;</span>
            </div>
          </div>
        </div>
      </section>

      <!-- ====== 已完成考核 ====== -->
      <section v-if="completedList.length > 0" class="review-section">
        <h3 class="section-title">历史考核</h3>
        <div class="card-list">
          <div
            v-for="item in completedList"
            :key="item.id"
            class="review-card"
            @click="handleClickItem(item.id)"
          >
            <div class="card-header">
              <div class="card-period">
                <span class="period-label">{{ item.periodLabel }}</span>
                <span class="period-type">{{ item.period === 'monthly' ? '月度' : item.period === 'quarterly' ? '季度' : '年度' }}考核</span>
              </div>
              <!-- 等级徽章 + 分数 -->
              <div class="card-grade-row">
                <span
                  v-if="item.grade"
                  class="grade-badge"
                  :style="{
                    color: GradeColors[item.grade] || 'var(--fts-text-primary)',
                    background: gradeBgColor(item.grade),
                    borderColor: GradeColors[item.grade] || 'var(--fts-border-primary)',
                  }"
                >
                  {{ GradeLabels[item.grade] || item.grade }}
                </span>
                <span v-if="item.totalScore !== null" class="card-score">
                  {{ item.totalScore.toFixed(1) }}分
                </span>
              </div>
            </div>

            <!-- 维度评分条 -->
            <div v-if="getItemScores(item.id).length > 0" class="score-bars">
              <div
                v-for="score in getItemScores(item.id)"
                :key="score.dimension"
                class="score-bar-item"
              >
                <span class="score-bar__label">{{ ReviewDimensionLabels[score.dimension] }}</span>
                <div class="score-bar__track">
                  <div
                    class="score-bar__fill"
                    :style="{
                      width: `${((score.managerScore || 0) / score.maxScore) * 100}%`,
                      background: getScoreBarColor(score.managerScore || 0),
                    }"
                  />
                </div>
                <span class="score-bar__value">{{ score.managerScore ?? '--' }}</span>
              </div>
            </div>

            <div class="card-body">
              <span class="reviewer">评定人：{{ item.reviewerName }}</span>
              <span v-if="item.completedAt" class="complete-time">完成于 {{ item.completedAt.slice(5, 10) }}</span>
            </div>
          </div>
        </div>
      </section>
    </template>
  </PageContainer>
</template>

<script lang="ts">
/**
 * 获取等级徽章的背景色（淡色版本）
 */
function gradeBgColor(grade: string): string {
  const map: Record<string, string> = {
    S: 'rgba(var(--fts-success-rgb), 0.1)',
    A: 'rgba(var(--fts-primary-rgb), 0.1)',
    B: 'rgba(var(--fts-info-rgb), 0.1)',
    C: 'rgba(var(--fts-warning-rgb), 0.1)',
    D: 'rgba(var(--fts-error-rgb), 0.1)',
  }
  return map[grade] || 'var(--fts-bg-secondary)'
}

/**
 * 根据分数返回评分条颜色
 * >=90 绿色, >=70 蓝色, >=60 橙色, <60 红色
 */
function getScoreBarColor(score: number): string {
  if (score >= 90) return 'var(--fts-success)'
  if (score >= 70) return 'var(--fts-primary)'
  if (score >= 60) return 'var(--fts-warning)'
  return 'var(--fts-error)'
}
</script>

<style scoped lang="scss">
/* ================================================================
   统计摘要行 - 4列紧凑排列
   ================================================================ */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-5);

  @media (max-width: 480px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ================================================================
   空状态提示链接
   ================================================================ */
.empty-hint {
  text-align: center;
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);

  &__text {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }

  &__link {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-primary);
    font-weight: 500;
    margin-left: var(--fts-space-1);

    &:hover {
      text-decoration: underline;
    }
  }
}

/* ================================================================
   Section 区域
   ================================================================ */
.review-section {
  margin-bottom: var(--fts-space-6);
}

.section-title {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
}

.card-list {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

/* ================================================================
   考核卡片基础样式
   ================================================================ */
.review-card {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: var(--fts-border-hover);
    box-shadow: var(--fts-shadow-xs);
  }
}

/* ---- 待处理卡片：按状态区分左边框颜色 ---- */
.review-card--pending {
  border-left: 3px solid var(--fts-primary);

  /* 自评状态：主色调蓝色 */
  &--self {
    border-left-color: var(--fts-primary);
  }

  /* 待主管评定：警告色橙色 */
  &--manager {
    border-left-color: var(--fts-warning);
  }

  /* HR审核中：信息色灰色 */
  &--hr {
    border-left-color: var(--fts-info);
  }
}

/* ================================================================
   卡片头部区域
   ================================================================ */
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--fts-space-3);
}

.card-period {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.period-label {
  font-size: var(--fts-font-size-base);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.period-type {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

/* ---- 等级/分数行 ---- */
.card-grade-row {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

/* 等级徽章：圆角标签样式 */
.grade-badge {
  font-size: var(--fts-font-size-xs);
  font-weight: 700;
  padding: 2px 10px;
  border-radius: var(--fts-radius-full);
  border: 1px solid;
  white-space: nowrap;
  letter-spacing: 0.02em;
}

.card-score {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-secondary);
  background: var(--fts-bg-secondary);
  padding: 1px 8px;
  border-radius: var(--fts-radius-sm);
}

/* ================================================================
   维度评分条（已完成考核）
   ================================================================ */
.score-bars {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
  margin-bottom: var(--fts-space-3);
  padding: var(--fts-space-3);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
}

.score-bar-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.score-bar__label {
  font-size: var(--fts-font-size-2xs);
  color: var(--fts-text-tertiary);
  width: 64px;
  flex-shrink: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.score-bar__track {
  flex: 1;
  height: 6px;
  background: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full);
  overflow: hidden;
}

.score-bar__fill {
  height: 100%;
  border-radius: var(--fts-radius-full);
  transition: width 0.3s ease;
  min-width: 2px;
}

.score-bar__value {
  font-size: var(--fts-font-size-2xs);
  font-weight: 600;
  color: var(--fts-text-secondary);
  width: 28px;
  text-align: right;
  flex-shrink: 0;
}

/* ================================================================
   卡片底部信息区
   ================================================================ */
.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.reviewer {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.create-time,
.complete-time {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

/* ================================================================
   截止日期 / 倒计时（待处理卡片）
   ================================================================ */
.card-deadline {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-2);

  &--urgent {
    color: var(--fts-error);
    font-weight: 600;

    .deadline-icon {
      color: var(--fts-error);
    }
  }
}

.deadline-icon {
  font-size: 12px;
}

.deadline-text {
  white-space: nowrap;
}

/* ================================================================
   操作按钮区（待处理卡片）
   ================================================================ */
.card-action {
  margin-top: var(--fts-space-3);
  padding-top: var(--fts-space-3);
  border-top: 1px solid var(--fts-border-secondary);
}

.action-link {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-primary);
  font-weight: 600;
}
</style>
