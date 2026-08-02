<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PageContainer, EmptyState } from '@/components/core'
import { reviewApi } from '@/api/review'
import type { ReviewDetail, ReviewScore, ReviewDimension } from '@/types/review'
import { ReviewStatusLabels, ReviewDimensionLabels, GradeLabels, GradeColors } from '@/types/review'

const route = useRoute()
const router = useRouter()
const id = route.params.id as string

const loading = ref(false)
const subLoading = ref(false)
const detail = ref<ReviewDetail | null>(null)

const showAppealDialog = ref(false)
const appealReason = ref('')

const editScores = ref<{ dimension: ReviewDimension; score: number; comment: string }[]>([])

const canSelfReview = computed(() => detail.value?.status === 'self_review')
const canAppeal = computed(() => detail.value?.status === 'completed')
const isAppealing = computed(() => detail.value?.status === 'appealing' || detail.value?.status === 'appeal_resolved')

function weightedTotal(scores: ReviewScore[]): number {
  return scores.reduce((sum, s) => {
    const score = s.managerScore ?? s.selfScore ?? 0
    return sum + (score * s.weight) / 100
  }, 0)
}

async function fetchDetail() {
  loading.value = true
  try {
    const result = await reviewApi.getDetail(id)
    detail.value = result
    if (canSelfReview.value) {
      editScores.value = result.scores.map(s => ({
        dimension: s.dimension,
        score: s.selfScore ?? 0,
        comment: s.comment || '',
      }))
    }
  } catch {
    ElMessage.error('加载考核详情失败')
  } finally {
    loading.value = false
  }
}

async function handleSelfReview() {
  const valid = editScores.value.every(s => s.score >= 0 && s.score <= 100)
  if (!valid) {
    ElMessage.warning('请确保所有评分在0-100之间')
    return
  }

  subLoading.value = true
  try {
    await reviewApi.submitSelfReview(id, {
      scores: editScores.value,
      comment: '已完成自评',
    })
    ElMessage.success('自评提交成功')
    await fetchDetail()
  } catch {
    ElMessage.error('提交失败')
  } finally {
    subLoading.value = false
  }
}

async function handleAppeal() {
  if (!appealReason.value.trim()) return

  subLoading.value = true
  try {
    await reviewApi.submitAppeal(id, appealReason.value.trim())
    showAppealDialog.value = false
    appealReason.value = ''
    ElMessage.success('申诉已提交')
    await fetchDetail()
  } catch {
    ElMessage.error('提交申诉失败')
  } finally {
    subLoading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <PageContainer title="考核详情" :loading="loading">
    <template v-if="detail">
      <!-- 考核头部 -->
      <section class="review-hero" :style="{ borderLeftColor: detail.grade ? GradeColors[detail.grade] || 'var(--fts-primary)' : 'var(--fts-primary)' }">
        <div class="rh-top">
          <div class="rh-period">
            <span class="rh-label">{{ detail.periodLabel }}</span>
            <span class="rh-type">{{ detail.period === 'monthly' ? '月度' : detail.period === 'quarterly' ? '季度' : '年度' }}考核</span>
          </div>
          <span class="rh-status" :class="`rh-status--${detail.status}`">
            {{ ReviewStatusLabels[detail.status] }}
          </span>
        </div>
        <div class="rh-score-row" v-if="detail.totalScore !== null">
          <div class="rh-score-main">
            <span class="rh-total">{{ detail.totalScore.toFixed(1) }}</span>
            <span class="rh-unit">分</span>
          </div>
          <span v-if="detail.grade" class="rh-grade" :style="{ color: GradeColors[detail.grade] || 'var(--fts-text-primary)' }">
            {{ GradeLabels[detail.grade] || detail.grade }}
          </span>
        </div>
      </section>

      <!-- 各维度评分 -->
      <section class="detail-section">
        <h3 class="ds-title">评分详情</h3>
        <div class="scores-table">
          <div class="st-header">
            <span class="sth-dim">考核维度</span>
            <span class="sth-score">自评</span>
            <span class="sth-score">主管评分</span>
            <span class="sth-weight">权重</span>
          </div>
          <div v-for="score in detail.scores" :key="score.dimension" class="st-row">
            <span class="std-dim">{{ ReviewDimensionLabels[score.dimension] }}</span>
            <span class="std-score std-score--self">
              <template v-if="canSelfReview">
                <input
                  v-model.number="editScores.find(s => s.dimension === score.dimension)!.score"
                  type="number"
                  min="0"
                  max="100"
                  class="score-input"
                />
              </template>
              <template v-else>
                {{ score.selfScore !== null ? score.selfScore : '-' }}
              </template>
            </span>
            <span class="std-score std-score--manager">
              {{ score.managerScore !== null ? score.managerScore : '-' }}
            </span>
            <span class="std-weight">{{ score.weight }}%</span>
          </div>
        </div>

        <!-- 自评备注 -->
        <div v-if="canSelfReview" class="self-review-notes">
          <div v-for="score in detail.scores" :key="'note-' + score.dimension" class="sr-note-row">
            <label class="sr-label">{{ ReviewDimensionLabels[score.dimension] }}说明</label>
            <el-input
              v-model="editScores.find(s => s.dimension === score.dimension)!.comment"
              placeholder="请简要说明..."
              :maxlength="100"
              show-word-limit
            />
          </div>
        </div>
      </section>

      <!-- 评语 -->
      <section v-if="!canSelfReview" class="detail-section">
        <h3 class="ds-title">评语</h3>
        <div class="comment-block" v-if="detail.selfComment">
          <span class="cb-label">自评</span>
          <p class="cb-text">{{ detail.selfComment }}</p>
        </div>
        <div class="comment-block" v-if="detail.managerComment">
          <span class="cb-label">主管评语</span>
          <p class="cb-text">{{ detail.managerComment }}</p>
        </div>
        <div class="comment-block" v-if="detail.hrComment">
          <span class="cb-label">HR评语</span>
          <p class="cb-text">{{ detail.hrComment }}</p>
        </div>
      </section>

      <!-- 申诉信息 -->
      <section v-if="isAppealing" class="detail-section">
        <h3 class="ds-title">申诉信息</h3>
        <div class="comment-block">
          <span class="cb-label">申诉原因</span>
          <p class="cb-text">{{ detail.appealReason }}</p>
        </div>
        <div v-if="detail.appealResult" class="comment-block">
          <span class="cb-label">申诉结果</span>
          <p class="cb-text">{{ detail.appealResult }}</p>
        </div>
      </section>

      <!-- 历史记录 -->
      <section class="detail-section">
        <h3 class="ds-title">考核记录</h3>
        <div class="history-list">
          <div v-for="node in detail.history" :key="node.id" class="history-item">
            <div class="hi-dot"></div>
            <div class="hi-content">
              <div class="hi-top">
                <span class="hi-action">{{ node.action }}</span>
                <span class="hi-operator">{{ node.operator }}</span>
              </div>
              <p class="hi-detail">{{ node.detail }}</p>
              <span class="hi-time">{{ node.time }}</span>
            </div>
          </div>
        </div>
      </section>

      <!-- 操作区 -->
      <section v-if="canSelfReview || (canAppeal && !isAppealing)" class="action-section">
        <template v-if="canSelfReview">
          <button class="act-btn act-btn--primary" :disabled="subLoading" @click="handleSelfReview">
            提交自评
          </button>
        </template>
        <template v-if="canAppeal && !isAppealing">
          <button class="act-btn act-btn--outline" :disabled="subLoading" @click="showAppealDialog = true">
            对结果有异议？提交申诉
          </button>
        </template>
      </section>
    </template>

    <!-- 申诉弹窗 -->
    <el-dialog v-model="showAppealDialog" title="考核申诉" width="450px" :close-on-click-modal="false" destroy-on-close>
      <div class="appeal-form">
        <label class="af-label">申诉原因 *</label>
        <el-input
          v-model="appealReason"
          type="textarea"
          placeholder="请详细说明申诉原因，包括对哪些维度的评分有异议..."
          :rows="4"
          maxlength="500"
          show-word-limit
        />
      </div>
      <template #footer>
        <el-button @click="showAppealDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!appealReason.trim()" @click="handleAppeal">提交申诉</el-button>
      </template>
    </el-dialog>
  </PageContainer>
</template>

<style scoped lang="scss">
.review-hero {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-left: 4px solid var(--fts-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.rh-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--fts-space-3);
}

.rh-period {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.rh-label {
  font-size: var(--fts-font-size-lg);
  font-weight: 700;
  color: var(--fts-text-primary);
}

.rh-type {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.rh-status {
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  padding: 3px 12px;
  border-radius: var(--fts-radius-full);

  &--self_review {
    background: rgba(var(--fts-primary-rgb), 0.10);
    color: var(--fts-primary);
  }
  &--completed {
    background: rgba(var(--fts-success-rgb), 0.10);
    color: var(--fts-success);
  }
  &--appealing {
    background: rgba(var(--fts-warning-rgb), 0.10);
    color: var(--fts-warning);
  }
  &--appeal_resolved {
    background: rgba(var(--fts-info-rgb), 0.10);
    color: var(--fts-info);
  }
  &--manager_review, &--hr_review {
    background: rgba(var(--fts-warning-rgb), 0.10);
    color: var(--fts-warning);
  }
}

.rh-score-row {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-3);
}

.rh-score-main {
  display: flex;
  align-items: baseline;
}

.rh-total {
  font-size: 32px;
  font-weight: 800;
  color: var(--fts-text-primary);
  line-height: 1;
}

.rh-unit {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin-left: 4px;
}

.rh-grade {
  font-size: var(--fts-font-size-xl);
  font-weight: 700;
}

.detail-section {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-md);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.ds-title {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin: 0 0 var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-secondary);
}

// 评分表格
.scores-table {
  display: flex;
  flex-direction: column;
}

.st-header {
  display: flex;
  align-items: center;
  padding: var(--fts-space-2) 0;
  border-bottom: 2px solid var(--fts-border-primary);
}

.sth-dim {
  flex: 1;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-text-tertiary);
}

.sth-score {
  width: 60px;
  text-align: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-text-tertiary);
}

.sth-weight {
  width: 48px;
  text-align: center;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-text-tertiary);
}

.st-row {
  display: flex;
  align-items: center;
  padding: var(--fts-space-3) 0;
  border-bottom: 1px solid var(--fts-border-secondary);

  &:last-child {
    border-bottom: none;
  }
}

.std-dim {
  flex: 1;
  font-size: var(--fts-font-size-sm);
  font-weight: 500;
  color: var(--fts-text-primary);
}

.std-score {
  width: 60px;
  text-align: center;
  font-size: var(--fts-font-size-sm);
  font-weight: 600;

  &--self {
    color: var(--fts-text-secondary);
  }
  &--manager {
    color: var(--fts-primary);
  }
}

.std-weight {
  width: 48px;
  text-align: center;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.score-input {
  width: 48px;
  padding: 4px 6px;
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-radius-sm);
  text-align: center;
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
  background: var(--fts-bg-card);

  &:focus {
    outline: none;
    border-color: var(--fts-primary);
    box-shadow: 0 0 0 2px rgba(var(--fts-primary-rgb), 0.15);
  }
}

// 自评备注
.self-review-notes {
  margin-top: var(--fts-space-4);
  padding-top: var(--fts-space-4);
  border-top: 1px solid var(--fts-border-secondary);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-3);
}

.sr-note-row {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
}

.sr-label {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-weight: 500;
}

// 评语
.comment-block {
  padding: var(--fts-space-3) 0;

  & + & {
    border-top: 1px solid var(--fts-border-secondary);
  }
}

.cb-label {
  display: inline-flex;
  font-size: var(--fts-font-size-xs);
  font-weight: 600;
  color: var(--fts-primary);
  margin-bottom: var(--fts-space-1);
}

.cb-text {
  margin: 0;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  line-height: 1.6;
}

// 历史记录
.history-list {
  display: flex;
  flex-direction: column;
  padding-left: var(--fts-space-2);
}

.history-item {
  display: flex;
  gap: var(--fts-space-3);
  padding: var(--fts-space-2) 0;
  position: relative;

  &::before {
    content: '';
    position: absolute;
    left: 6px;
    top: 24px;
    width: 2px;
    height: calc(100% - 24px);
    background: var(--fts-border-primary);
  }

  &:last-child::before {
    display: none;
  }
}

.hi-dot {
  width: 14px;
  height: 14px;
  min-width: 14px;
  border-radius: 50%;
  background: var(--fts-primary);
  margin-top: 3px;
  z-index: 1;
}

.hi-content {
  flex: 1;
  min-width: 0;
}

.hi-top {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: 2px;
}

.hi-action {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.hi-operator {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.hi-detail {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-secondary);
  line-height: 1.5;
}

.hi-time {
  font-size: 10px;
  color: var(--fts-text-quaternary);
}

// 操作区
.action-section {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
}

.act-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;

  &--primary {
    background: var(--fts-primary);
    color: var(--fts-text-on-primary);
    border-color: var(--fts-primary);
    &:hover { opacity: 0.9; }
    &:active { transform: scale(0.97); }
  }

  &--outline {
    background: transparent;
    color: var(--fts-warning);
    border-color: rgba(var(--fts-warning-rgb), 0.40);
    &:hover { background: rgba(var(--fts-warning-rgb), 0.06); }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.appeal-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.af-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}
</style>