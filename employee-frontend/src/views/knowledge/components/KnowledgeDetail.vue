<template>
  <Teleport to="body">
    <Transition name="kb-slide">
      <div
        v-if="article"
        class="kb-detail-overlay"
        @click.self="$emit('close')"
      >
        <div class="kb-detail-panel">
          <!-- 学习模式顶栏（含内嵌迷你进度条） -->
          <div v-if="isLearningMode && currentArticle" class="kb-learning-bar">
            <div class="kb-learning-bar__left">
              <!-- idle 阶段：显示开始测试按钮 -->
              <template v-if="learningPhase === 'idle'">
                <button class="kb-learning-bar__start-btn" @click="$emit('begin-learning')">
                  <el-icon :size="14"><Reading /></el-icon>
                  开始测试
                </button>
              </template>

              <!-- reading 阶段：显示进度 -->
              <template v-else-if="learningPhase === 'reading'">
                <div class="kb-learning-bar__info">
                  <el-icon :size="14" :color="cssPrimary"><Reading /></el-icon>
                  <span class="kb-learning-bar__text">培训测试模式</span>
                </div>
                <div class="kb-learning-bar__progress">
                  <div class="kb-lp__track">
                    <div
                      class="kb-lp__fill"
                      :class="{ 'kb-lp__fill--done': hasMetMinTime }"
                      :style="{ width: `${readProgress}%` }"
                    ></div>
                  </div>
                  <span class="kb-lp__time" :class="{ 'kb-lp__time--done': hasMetMinTime }">
                    {{ hasMetMinTime ? '✓' : `${formattedReadTime}/${formattedRequiredTime}` }}
                  </span>
                </div>
              </template>

              <!-- result 阶段：显示答题状态 -->
              <template v-else-if="learningPhase === 'result'">
                <div class="kb-learning-bar__info">
                  <el-icon :size="14"><Reading /></el-icon>
                  <span class="kb-learning-bar__text">测试完成</span>
                </div>
              </template>

              <!-- reviewing 阶段（复习模式） -->
              <template v-else-if="learningPhase === 'reviewing'">
                <div class="kb-learning-bar__info">
                  <el-icon :size="14" :color="cssWarning"><RefreshRight /></el-icon>
                  <span class="kb-learning-bar__text">复习模式</span>
                </div>
              </template>

              <!-- completed / abandoned 阶段 -->
              <template v-else>
                <div class="kb-learning-bar__info">
                  <el-icon :size="14" :color="learningPhase === 'completed' ? cssSuccess : cssTextTertiary">
                    <component :is="learningPhase === 'completed' ? 'Check' : 'Close'" />
                  </el-icon>
                  <span class="kb-learning-bar__text">{{ learningPhase === 'completed' ? '测试通过' : '已退出测试' }}</span>
                </div>
              </template>
            </div>

            <!-- 操作按钮区 -->
            <template v-if="learningPhase === 'idle'">
              <!-- idle：无操作（已有开始按钮） -->
            </template>
            <template v-else-if="learningPhase === 'reading'">
              <button
                class="kb-learning-bar__abort"
                @click="$emit('abort-confirm')"
                title="退出学习"
              >
                退出
              </button>
            </template>
            <template v-else-if="learningPhase === 'result'">
              <button
                v-if="isPassed"
                class="kb-learning-bar__complete kb-learning-bar__complete--active"
                @click="$emit('complete-learning')"
              >
                <el-icon :size="14"><Check /></el-icon>
                完成学习
              </button>
              <button
                v-else
                class="kb-learning-bar__retry"
                @click="$emit('retry-learning')"
              >
                <el-icon :size="14"><RefreshRight /></el-icon>
                重新学习
              </button>
            </template>
            <template v-else-if="learningPhase === 'abandoned'">
              <button
                class="kb-learning-bar__retry"
                @click="$emit('begin-learning')"
              >
                重新开始
              </button>
            </template>
          </div>

          <!-- 面板头部 -->
          <div class="kb-detail__header">
            <h2 class="kb-detail__title">{{ article.title }}</h2>
            <button class="kb-detail__close" @click="$emit('close')">
              <el-icon :size="18"><Close /></el-icon>
            </button>
          </div>

          <!-- 面板元信息 -->
          <div class="kb-detail__meta">
            <StatusTag :status="categoryStatusMap[article.category]?.status" :label="categoryStatusMap[article.category]?.label" size="small" variant="light" />
            <span>{{ article.author }}</span>
            <span>{{ formatDate(article.updateTime ?? '') }} 更新</span>
            <span><el-icon :size="12"><View /></el-icon> {{ article.viewCount }} 次阅读</span>
            <span v-if="!isLearningMode"><el-icon :size="12"><Reading /></el-icon> {{ estimatedReadTime }}</span>
            <!-- 收藏按钮内联到元信息行（释放底部空间） -->
            <button
              class="kb-favorite-inline"
              :class="{ 'kb-favorite-inline--active': article.isFavorited }"
              @click.stop="$emit('toggle-favorite', article.id)"
              title="收藏"
            >
              <el-icon :size="14">
                <component :is="article.isFavorited ? StarFilled : Star" />
              </el-icon>
              {{ article.isFavorited ? '已收藏' : '收藏' }}
            </button>
          </div>

          <!-- Markdown 内容区 -->
          <div class="kb-detail__content" v-html="renderedContent"></div>

          <!-- 随堂测验弹题 + 结果覆盖层 -->
          <KnowledgeQuiz
            :is-learning-mode="isLearningMode"
            :learning-phase="learningPhase"
            :current-popup-question="currentPopupQuestion"
            :popup-answer="popupAnswer"
            :popup-submitted="popupSubmitted"
            :popup-is-correct="popupIsCorrect"
            :is-current-required="isCurrentRequired"
            :answered-count="answeredCount"
            :total-quiz-count="totalQuizCount"
            :learning-result="learningResult"
            :is-passed="isPassed"
            :fail-reason="failReason"
            :pass-threshold="passThreshold"
            @popup-answer="$emit('popup-answer', $event)"
            @popup-submit-next="$emit('popup-submit-next')"
            @abort-confirm="$emit('abort-confirm')"
            @complete-learning="$emit('complete-learning')"
            @retry-learning="$emit('retry-learning')"
            @return-training="$emit('return-training')"
          />
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { View, Star, StarFilled, Close, Reading, Check, RefreshRight } from '@element-plus/icons-vue'
import { StatusTag } from '@/components/core'
import KnowledgeQuiz from './KnowledgeQuiz.vue'
import type { KnowledgeArticle, KnowledgeCategory, QuizQuestion } from '@/types/knowledge'
import type { LearningPhase, LearningResult } from '@/composables/useKnowledgeReading'

// ==================== CSS 变量获取（用于动态属性） ====================
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
const cssPrimary = computed(() => getCssVar('--fts-primary'))
const cssTextTertiary = computed(() => getCssVar('--fts-text-tertiary'))
const cssWarning = computed(() => getCssVar('--fts-warning'))
const cssSuccess = computed(() => getCssVar('--fts-success'))

const props = defineProps<{
  /** 当前选中的文章 */
  article: KnowledgeArticle | null
  /** 是否处于学习模式 */
  isLearningMode: boolean
  /** 是否为复习模式 */
  isReviewMode: boolean
  /** 当前学习阶段 */
  learningPhase: LearningPhase
  /** 当前学习文章 */
  currentArticle: KnowledgeArticle | null
  /** 阅读进度百分比 */
  readProgress: number
  /** 是否已满足最小时长 */
  hasMetMinTime: boolean
  /** 格式化的已读时间 */
  formattedReadTime: string
  /** 格式化的要求时间 */
  formattedRequiredTime: string
  /** 是否通过测试 */
  isPassed: boolean
  /** 学习结果 */
  learningResult: LearningResult | null
  /** 未通过原因 */
  failReason: 'required_missed' | 'normal_threshold' | undefined
  /** 渲染后的HTML内容 */
  renderedContent: string
  /** 预估阅读时长 */
  estimatedReadTime: string
  /** 分类状态映射 */
  categoryStatusMap: Record<KnowledgeCategory | string, { status: string; label: string }>
  /** 当前弹出的题目 */
  currentPopupQuestion: QuizQuestion | null
  /** 当前选择的答案ID */
  popupAnswer: string
  /** 是否已提交当前题目 */
  popupSubmitted: boolean
  /** 当前题目是否答对 */
  popupIsCorrect: boolean
  /** 当前题目是否为必会题 */
  isCurrentRequired: boolean
  /** 已答题数 */
  answeredCount: number
  /** 总题数 */
  totalQuizCount: number
  /** 通过阈值 */
  passThreshold: number
}>()

defineEmits<{
  /** 关闭详情面板 */
  'close': []
  /** 切换收藏 */
  'toggle-favorite': [id: string]
  /** 开始学习 */
  'begin-learning': []
  /** 退出确认 */
  'abort-confirm': []
  /** 完成学习 */
  'complete-learning': []
  /** 重新学习 */
  'retry-learning': []
  /** 返回培训页 */
  'return-training': []
  /** 选择弹题答案 */
  'popup-answer': [optionId: string]
  /** 提交弹题并进入下一题 */
  'popup-submit-next': []
}>()

/** 格式化日期 */
function formatDate(isoStr: string): string {
  const d = new Date(isoStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}
</script>

<style scoped lang="scss">
/* ── 详情遮罩 ── */
.kb-detail-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background-color: var(--fts-overlay-backdrop);
  display: flex;
  justify-content: flex-end;
}

/* ── 学习模式顶栏（紧凑：进度条内嵌） ── */
.kb-learning-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--fts-space-2, 8px);
  padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
  background: linear-gradient(135deg, rgba(var(--fts-primary-rgb), 0.08), rgba(var(--fts-primary-rgb), 0.03));
  border-bottom: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;
}

.kb-learning-bar__left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-3, 12px);
  flex: 1;
  min-width: 0;
}

.kb-learning-bar__info {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  flex-shrink: 0;
}

.kb-learning-bar__text {
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-primary);
  white-space: nowrap;
}

/* ── 内嵌迷你进度条 ── */
.kb-learning-bar__progress {
  display: flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  flex: 1;
  min-width: 0;
}

.kb-lp__track {
  flex: 1;
  height: 4px;
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-full, 999px);
  overflow: hidden;
  min-width: 40px;
}

.kb-lp__fill {
  height: 100%;
  border-radius: var(--fts-radius-full, 999px);
  background-color: var(--fts-primary);
  transition: width 0.5s ease;

  &--done {
    background-color: var(--fts-success);
  }
}

.kb-lp__time {
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-tertiary);
  white-space: nowrap;
  font-variant-numeric: tabular-nums;

  &--done {
    color: var(--fts-success);
    font-weight: var(--fts-font-weight-semibold, 600);
  }
}

.kb-learning-bar__complete {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  padding: var(--fts-space-1, 4px) var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-on-primary);
  background-color: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), transform var(--fts-duration-fast) var(--fts-easing-default), opacity var(--fts-duration-fast) var(--fts-easing-default);

  &:hover:not(:disabled) {
    opacity: 0.9;
  }

  &:active:not(:disabled) {
    transform: scale(0.96);
  }

  &--disabled {
    opacity: 0.5;
    cursor: not-allowed;
    background-color: var(--fts-text-quaternary);
  }

  &--active {
    background-color: var(--fts-success);
  }
}

/* 开始学习按钮（idle阶段） */
.kb-learning-bar__start-btn {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  padding: var(--fts-space-1, 4px) var(--fts-space-4, 16px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-on-primary);
  background-color: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), transform var(--fts-duration-fast) var(--fts-easing-default), opacity var(--fts-duration-fast) var(--fts-easing-default);

  &:hover { opacity: 0.9; }
  &:active { transform: scale(0.96); }
}

/* 退出学习按钮 */
.kb-learning-bar__abort {
  padding: var(--fts-space-1, 4px) var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-tertiary);
  background: none;
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), transform var(--fts-duration-fast) var(--fts-easing-default), opacity var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    color: var(--fts-error);
    border-color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.05);
  }
}

/* 重试/重新开始按钮 */
.kb-learning-bar__retry {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-1, 4px);
  padding: var(--fts-space-1, 4px) var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-warning-dark);
  background-color: rgba(var(--fts-warning-rgb), 0.12);
  border: 1px solid rgba(var(--fts-warning-rgb), 0.35);
  border-radius: var(--fts-radius-full);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), transform var(--fts-duration-fast) var(--fts-easing-default), opacity var(--fts-duration-fast) var(--fts-easing-default);

  &:hover { opacity: 0.85; }
  &:active { transform: scale(0.96); }
}

/* ── 详情面板 ── */
.kb-detail-panel {
  width: min(520px, 92vw);
  max-width: 100%;
  height: 100%;
  background-color: var(--fts-bg-page);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: var(--fts-shadow-lg);
}

.kb-detail__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-4, 16px) var(--fts-space-4, 16px) 0;
  flex-shrink: 0;
}

.kb-detail__title {
  margin: 0;
  font-size: var(--fts-font-size-lg, 17px);
  font-weight: var(--fts-font-weight-bold, 700);
  color: var(--fts-text-primary);
  line-height: 1.4;
  flex: 1;
}

.kb-detail__close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: none;
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  color: var(--fts-text-tertiary);
  flex-shrink: 0;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), transform var(--fts-duration-fast) var(--fts-easing-default), opacity var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    background-color: var(--fts-bg-tertiary);
    color: var(--fts-text-primary);
  }
}

.kb-detail__meta {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
  padding: 0 var(--fts-space-4, 16px) var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-quaternary);
  border-bottom: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;
  flex-wrap: wrap;
}

/* ── 详情内容（支持滚动，禁止横向溢出） ── */
.kb-detail__content {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: var(--fts-space-4, 16px);
  font-size: var(--fts-font-size-md);
  line-height: 1.9;
  color: var(--fts-text-secondary);
  max-width: 100%;
  box-sizing: border-box;
  letter-spacing: 0.3px;

  /* Markdown 标题 */
  :deep(h1) {
    font-size: 20px;
    font-weight: var(--fts-font-weight-bold, 700);
    color: var(--fts-text-primary);
    margin: var(--fts-space-4, 16px) 0 var(--fts-space-2, 8px);
    padding-bottom: var(--fts-space-2, 8px);
    border-bottom: 2px solid var(--fts-border-secondary);
    letter-spacing: 0.5px;
  }

  :deep(h2) {
    font-size: 18px;
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
    margin: var(--fts-space-4, 16px) 0 var(--fts-space-2, 8px);
    letter-spacing: 0.5px;
  }

  :deep(h3) {
    font-size: 16px;
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
    margin: var(--fts-space-3, 12px) 0 var(--fts-space-1, 4px);
    letter-spacing: 0.3px;
  }

  /* Markdown 行内格式 */
  :deep(strong) {
    font-weight: var(--fts-font-weight-semibold, 600);
    color: var(--fts-text-primary);
  }

  :deep(code) {
    font-family: ui-monospace, monospace;
    font-size: var(--fts-font-size-xs, 12px);
    padding: 1px 4px;
    background-color: var(--fts-bg-tertiary);
    border-radius: var(--fts-radius-sm, 4px);
  }

  :deep(blockquote) {
    margin: var(--fts-space-2, 8px) 0;
    padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
    border-left: 3px solid var(--fts-primary);
    background-color: rgba(var(--fts-primary-rgb), 0.05);
    color: var(--fts-text-secondary);
    font-style: italic;
  }

  /* Markdown 列表 */
  :deep(ul), :deep(ol) {
    margin: var(--fts-space-2, 8px) 0;
    padding-left: var(--fts-space-5, 20px);
  }

  :deep(li) {
    margin-bottom: var(--fts-space-1, 4px);
  }

  :deep(li > ul), :deep(li > ol) {
    margin-top: var(--fts-space-1, 4px);
    margin-bottom: 0;
  }

  /* Markdown 段落 */
  :deep(p) {
    margin: var(--fts-space-2, 8px) 0;
  }

  /* 分隔线 */
  :deep(hr) {
    border: none;
    border-top: 1px solid var(--fts-border-secondary);
    margin: var(--fts-space-3, 12px) 0;
  }

  /* ── 表格（GFM Markdown 渲染，移动端自适应） ── */
  :deep(table) {
    width: 100%;
    max-width: 100%;
    border-collapse: collapse;
    margin: var(--fts-space-3, 12px) 0;
    font-size: var(--fts-font-size-sm);
    line-height: 1.7;
    table-layout: auto;

    td {
      padding: var(--fts-space-2, 8px) var(--fts-space-2, 8px);
      border: 1px solid var(--fts-border-secondary);
      text-align: left;
      vertical-align: top;
      white-space: normal;
      word-break: break-all;
      overflow-wrap: anywhere;
    }

    th {
      padding: var(--fts-space-2, 8px) var(--fts-space-2, 8px);
      border: 1px solid var(--fts-border-secondary);
      text-align: left;
      vertical-align: top;
      white-space: nowrap;
      background-color: var(--fts-bg-secondary);
      font-weight: var(--fts-font-weight-semibold, 600);
      color: var(--fts-text-primary);
    }

    tbody tr:nth-child(even) td {
      background-color: rgba(var(--fts-bg-tertiary-rgb), 0.4);
    }

    tbody tr:hover td {
      background-color: rgba(var(--fts-primary-rgb), 0.04);
    }
  }
}

/* ── 面板底部操作栏 ── */
.kb-detail__actions {
  display: flex;
  gap: var(--fts-space-2, 8px);
  padding: var(--fts-space-3, 12px) var(--fts-space-4, 16px);
  border-top: 1px solid var(--fts-border-secondary);
  flex-shrink: 0;
}

/* ── 内联收藏按钮（元信息行内，替代底部actions区） ── */
.kb-favorite-inline {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  margin-left: auto;
  padding: 2px var(--fts-space-2, 8px);
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-tertiary);
  background: none;
  border: none;
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;

  &--active {
    color: var(--fts-warning);
  }
}

/* ── 面板滑入动画 ── */
.kb-slide-enter-active,
.kb-slide-leave-active {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1), opacity 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

.kb-slide-enter-from,
.kb-slide-leave-to {
  opacity: 0;

  .kb-detail-panel {
    transform: translateX(100%);
  }
}

.kb-slide-enter-active .kb-detail-panel,
.kb-slide-leave-active .kb-detail-panel {
  transition: transform 0.3s cubic-bezier(0.22, 1, 0.36, 1);
}

/* ── 响应式适配 ── */
@media (max-width: 768px) {
  .kb-detail-panel {
    width: 100vw;
  }
}

/* ── 减少动画偏好 ── */
@media (prefers-reduced-motion: reduce) {
  .kb-favorite-inline,
  .kb-detail__close {
    transition: none;
  }

  .kb-slide-enter-active,
  .kb-slide-leave-active {
    transition: none;
  }
}
</style>
