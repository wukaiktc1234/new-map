<template>
  <Teleport to="body">
    <!-- 随堂弹题覆盖层 -->
    <Transition name="kb-popup">
      <div
        v-if="isLearningMode && learningPhase === 'reading' && currentPopupQuestion"
        class="kb-quiz-popup"
        @click.self="$emit('abort-confirm')"
      >
        <div class="kb-quiz-popup__card">
          <!-- 弹题头部（含退出按钮 + 难度标记） -->
          <div class="kb-quiz-popup__header">
            <div class="kb-quiz-popup__header-left">
              <span class="kb-quiz-popup__badge">随堂测验</span>
              <span v-if="isCurrentRequired" class="kb-quiz-popup__required-tag">必会</span>
            </div>
            <div class="kb-quiz-popup__header-right">
              <span class="kb-quiz-popup__progress">{{ answeredCount }}/{{ totalQuizCount }}</span>
              <button class="kb-quiz-popup__exit" @click.stop="$emit('abort-confirm')" title="退出测试">
                <el-icon :size="14"><Close /></el-icon>
              </button>
            </div>
          </div>

          <!-- 题目 -->
          <p class="kb-quiz-popup__question">{{ currentPopupQuestion.question }}</p>

          <!-- 选项（固定序号1/2/3/4，不随打乱变化） -->
          <div class="kb-quiz-popup__options">
            <button
              v-for="(option, optIdx) in currentPopupQuestion.options"
              :key="option.id"
              class="kb-quiz-popup__option"
              :class="{
                'kb-quiz-popup__option--selected': popupAnswer === option.id,
                'kb-quiz-popup__option--correct': popupSubmitted && option.id === currentPopupQuestion.correctAnswerId,
                'kb-quiz-popup__option--wrong': popupSubmitted && popupAnswer === option.id && option.id !== currentPopupQuestion.correctAnswerId,
                'kb-quiz-popup__option--disabled': popupSubmitted,
              }"
              :disabled="popupSubmitted"
              @click="$emit('popup-answer', option.id)"
            >
              <span class="kb-quiz-popup__letter">{{ optIdx + 1 }}</span>
              <span>{{ option.text }}</span>
            </button>
          </div>

          <!-- 反馈 / 操作 -->
          <template v-if="popupSubmitted">
            <p v-if="popupIsCorrect" class="kb-quiz-popup__feedback kb-quiz-popup__feedback--correct">回答正确！</p>
            <p v-else class="kb-quiz-popup__feedback kb-quiz-popup__feedback--wrong">
              正确答案：{{ currentPopupQuestion?.options.find(o => o.id === currentPopupQuestion?.correctAnswerId)?.text }}
            </p>
            <button class="kb-quiz-popup__confirm" @click="$emit('popup-submit-next')">
              继续阅读
            </button>
          </template>
        </div>
      </div>
    </Transition>

    <!-- 学习结果覆盖层（全部答完后显示成绩单） -->
    <Transition name="kb-popup">
      <div
        v-if="isLearningMode && learningPhase === 'result' && learningResult"
        class="kb-quiz-popup"
        @click.self="void 0"
      >
        <div class="kb-quiz-popup__card kb-result-card">
          <!-- 结果头部 -->
          <div class="kb-result-card__header">
            <div class="kb-result-card__icon" :class="{ 'kb-result-card__icon--pass': learningResult.passed, 'kb-result-card__icon--fail': !learningResult.passed }">
              <el-icon :size="36">
                <component :is="learningResult.passed ? 'CircleCheckFilled' : 'CircleCloseFilled'" />
              </el-icon>
            </div>
            <h3 class="kb-result-card__title">
              {{ learningResult.passed ? '恭喜通过！' : (failReason === 'required_missed' ? '必会题未全部答对' : '未达到通过标准') }}
            </h3>
            <p class="kb-result-card__subtitle">
              <template v-if="learningResult.passed">你已完成本课程的学习任务</template>
              <template v-else-if="failReason === 'required_missed'">
                必会题是核心知识点，错一即不通过。请重新学习后再次测试。
              </template>
              <template v-else>
                正确率 {{ learningResult.score }}%，需达到 {{ Math.round(passThreshold * 100) }}% 才能通过
              </template>
            </p>
          </div>

          <!-- 成绩详情 -->
          <div class="kb-result-card__stats">
            <div class="kb-result-stat">
              <span class="kb-result-stat__value" :class="{ 'kb-result-stat__value--good': learningResult.passed, 'kb-result-stat__value--bad': !learningResult.passed }">
                {{ learningResult.correctCount }}/{{ learningResult.totalCount }}
              </span>
              <span class="kb-result-stat__label">答对题目</span>
            </div>
            <div class="kb-result-stat-divider"></div>
            <div class="kb-result-stat">
              <span class="kb-result-stat__value">{{ learningResult.score }}%</span>
              <span class="kb-result-stat__label">正确率</span>
            </div>
            <div class="kb-result-stat-divider"></div>
            <div class="kb-result-stat">
              <span class="kb-result-stat__value" :class="{ 'kb-result-stat__value--good': learningResult.allRequiredPassed, 'kb-result-stat__value--bad': !learningResult.allRequiredPassed }">
                {{ learningResult.requiredCorrect }}/{{ learningResult.requiredTotal }}
              </span>
              <span class="kb-result-stat__label">必会题</span>
            </div>
          </div>

          <!-- 通过线提示（双条件） -->
          <div class="kb-result-card__threshold kb-result-card__threshold--dual">
            <div :class="{ 'kb-threshold-item--fail': !learningResult.allRequiredPassed }">
              <span class="kb-threshold-icon">{{ learningResult.allRequiredPassed ? '✓' : '✗' }}</span>
              必会题：{{ learningResult.allRequiredPassed ? '全部正确' : `有 ${learningResult.requiredTotal - learningResult.requiredCorrect} 题错误` }}
            </div>
            <div :class="{ 'kb-threshold-item--fail': learningResult.allRequiredPassed && learningResult.score < Math.round(passThreshold * 100) }">
              <span class="kb-threshold-icon">{{ learningResult.score >= Math.round(passThreshold * 100) ? '✓' : '✗' }}</span>
              正确率 ≥ {{ Math.round(passThreshold * 100) }}%（当前 {{ learningResult.score }}%）
            </div>
          </div>

          <!-- 答题明细（折叠） -->
          <details v-if="!learningResult.passed" class="kb-result-card__detail-list">
            <summary class="kb-result-card__detail-toggle">查看答题明细</summary>
            <div class="kb-result-detail-items">
              <div
                v-for="(detail, idx) in learningResult.answerDetails"
                :key="detail.questionId"
                class="kb-result-detail-item"
                :class="{ 'kb-result-detail-item--wrong': !detail.correct }"
              >
                <span class="kb-detail-index">{{ idx + 1 }}</span>
                <span v-if="detail.isRequired" class="kb-detail-required-tag">必会</span>
                <span class="kb-detail-text">{{ detail.questionText }}</span>
                <span class="kb-detail-status">{{ detail.correct ? '✓ 正确' : '✗ 错误' }}</span>
              </div>
            </div>
          </details>

          <!-- 操作按钮 -->
          <div class="kb-result-card__actions">
            <button
              v-if="learningResult.passed"
              class="kb-result-card__btn kb-result-card__btn--primary"
              @click="$emit('complete-learning')"
            >
              <el-icon :size="16"><Check /></el-icon>
              完成测试并返回
            </button>
            <button
              v-else
              class="kb-result-card__btn kb-result-card__btn--warning"
              @click="$emit('retry-learning')"
            >
              <el-icon :size="16"><RefreshRight /></el-icon>
              重新测试
            </button>
            <button
              class="kb-result-card__btn kb-result-card__btn--secondary"
              @click="$emit('return-training')"
            >
              稍后再说
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup lang="ts">
import { Close, Check, RefreshRight } from '@element-plus/icons-vue'
import type { QuizQuestion } from '@/types/knowledge'
import type { LearningPhase, LearningResult } from '@/composables/useKnowledgeReading'

defineProps<{
  /** 是否处于学习模式 */
  isLearningMode: boolean
  /** 当前学习阶段 */
  learningPhase: LearningPhase
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
  /** 学习结果 */
  learningResult: LearningResult | null
  /** 是否通过 */
  isPassed: boolean
  /** 未通过原因 */
  failReason: 'required_missed' | 'normal_threshold' | undefined
  /** 通过阈值 */
  passThreshold: number
}>()

defineEmits<{
  /** 选择答案 */
  'popup-answer': [optionId: string]
  /** 提交并进入下一题 */
  'popup-submit-next': []
  /** 退出确认 */
  'abort-confirm': []
  /** 完成学习 */
  'complete-learning': []
  /** 重新学习 */
  'retry-learning': []
  /** 返回培训页 */
  'return-training': []
}>()
</script>

<style scoped lang="scss">
/* ════════════════════════════════════════════
   随堂测验 — 弹题覆盖层（防开卷作弊）
   替代底部集中测验，题目在阅读过程中逐一弹出
   ════════════════════════════════════════════ */

/* 覆盖层：半透明遮罩 + 模糊背景文章（防透过弹窗抄题） */
.kb-quiz-popup {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--fts-overlay-backdrop);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: var(--fts-space-4, 16px);
}

/* 卡片容器 */
.kb-quiz-popup__card {
  width: 100%;
  max-width: 420px;
  max-height: 80vh;
  overflow-y: auto;
  background-color: var(--fts-bg-card);
  border-radius: var(--fts-radius-lg, 12px);
  box-shadow: var(--fts-shadow-lg);
  padding: var(--fts-space-4, 16px);
  -webkit-overflow-scrolling: touch;
}

/* 头部：徽章 + 进度 */
.kb-quiz-popup__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--fts-space-3, 12px);
}

.kb-quiz-popup__badge {
  font-size: var(--fts-font-size-xs, 12px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-primary);
  background-color: rgba(var(--fts-primary-rgb), 0.1);
  padding: 2px var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-sm, 4px);
}

.kb-quiz-popup__progress {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  font-variant-numeric: tabular-nums;
}

/* 题目文字 */
.kb-quiz-popup__question {
  margin: 0 0 var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-medium, 500);
  color: var(--fts-text-primary);
  line-height: 1.7;
}

/* 选项列表 */
.kb-quiz-popup__options {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2, 8px);
  margin-bottom: var(--fts-space-3, 12px);
}

/* 单个选项 */
.kb-quiz-popup__option {
  display: flex;
  align-items: flex-start;
  gap: var(--fts-space-2, 8px);
  width: 100%;
  padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
  font-size: var(--fts-font-size-sm, 13px);
  color: var(--fts-text-secondary);
  background-color: var(--fts-bg-page);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  text-align: left;

  &--selected {
    border-color: var(--fts-primary);
    background-color: rgba(var(--fts-primary-rgb), 0.06);
    color: var(--fts-primary);
  }

  &--correct {
    border-color: var(--fts-success);
    background-color: rgba(var(--fts-success-rgb), 0.08);
    color: var(--fts-success);
  }

  &--wrong {
    border-color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.08);
    color: var(--fts-error);
  }

  &--disabled {
    cursor: default;
  }
}

/* 选项字母标记 */
.kb-quiz-popup__letter {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  font-size: var(--fts-font-size-2xs, 11px);
  font-weight: var(--fts-font-weight-semibold, 600);
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm, 4px);
  color: var(--fts-text-tertiary);
  flex-shrink: 0;

  .kb-quiz-popup__option--selected & { color: var(--fts-text-on-primary); background-color: var(--fts-primary); }
  .kb-quiz-popup__option--correct & { color: var(--fts-text-on-primary); background-color: var(--fts-success); }
  .kb-quiz-popup__option--wrong & { color: var(--fts-text-on-primary); background-color: var(--fts-error); }
}

/* 答题反馈文字 */
.kb-quiz-popup__feedback {
  margin: 0 0 var(--fts-space-2, 8px);
  font-size: var(--fts-font-size-sm, 13px);
  font-weight: var(--fts-font-weight-medium, 500);
  text-align: center;
  padding: var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-md, 6px);

  &--correct { color: var(--fts-success); background-color: rgba(var(--fts-success-rgb), 0.08); }
  &--wrong   { color: var(--fts-error);   background-color: rgba(var(--fts-error-rgb), 0.08); }
}

/* 确认/继续按钮 */
.kb-quiz-popup__confirm {
  width: 100%;
  padding: var(--fts-space-2, 8px) 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-on-primary);
  background-color: var(--fts-primary);
  border: none;
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;

  &:hover { opacity: 0.9; }
  &:active { opacity: 0.8; }
}

/* 弹窗头部右侧（进度 + 退出） */
.kb-quiz-popup__header-right {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
}

.kb-quiz-popup__exit {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: none;
  border-radius: var(--fts-radius-sm, 4px);
  color: var(--fts-text-quaternary);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), border-color var(--fts-duration-fast) var(--fts-easing-default);

  &:hover {
    color: var(--fts-error);
    background-color: rgba(var(--fts-error-rgb), 0.1);
  }
}

/* 弹窗头部左侧（测验徽章 + 必会标记） */
.kb-quiz-popup__header-left {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
}

/* 必会题标记（弹窗头部 + 答题明细） */
.kb-quiz-popup__required-tag,
.kb-detail-required-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  font-size: 10px;
  font-weight: var(--fts-font-weight-bold, 700);
  line-height: 1.4;
  color: var(--fts-text-on-primary);
  background-color: var(--fts-error);
  border-radius: var(--fts-radius-full);
  letter-spacing: 0.5px;
}

/* ════════════════════════════════════════════
   学习结果覆盖层（成绩单）
   全部答完后显示，展示通过/未通过 + 成绩详情
   ════════════════════════════════════════════ */

.kb-result-card {
  max-width: 380px;
  text-align: center;

  &__header {
    margin-bottom: var(--fts-space-4, 16px);
  }

  &__icon {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 72px;
    height: 72px;
    border-radius: 50%;
    margin-bottom: var(--fts-space-3, 12px);

    &--pass {
      color: var(--fts-success);
      background-color: rgba(var(--fts-success-rgb), 0.1);
    }

    &--fail {
      color: var(--fts-error);
      background-color: rgba(var(--fts-error-rgb), 0.1);
    }
  }

  &__title {
    margin: 0 0 var(--fts-space-1, 4px);
    font-size: var(--fts-font-size-lg, 17px);
    font-weight: var(--fts-font-weight-bold, 700);
    color: var(--fts-text-primary);
  }

  &__subtitle {
    margin: 0;
    font-size: var(--fts-font-size-sm, 13px);
    color: var(--fts-text-tertiary);
    line-height: 1.5;
  }
}

/* 成绩统计行 */
.kb-result-card__stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3, 12px);
  padding: var(--fts-space-3, 12px) 0;
  margin-bottom: var(--fts-space-3, 12px);
}

.kb-result-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.kb-result-stat__value {
  font-size: var(--fts-font-size-lg, 17px);
  font-weight: var(--fts-font-weight-bold, 700);
  color: var(--fts-text-primary);

  &--good { color: var(--fts-success); }
  &--bad { color: var(--fts-error); }
}

.kb-result-stat__label {
  font-size: var(--fts-font-size-2xs, 11px);
  color: var(--fts-text-quaternary);
}

.kb-result-stat-divider {
  width: 1px;
  height: 32px;
  background-color: var(--fts-border-secondary);
}

/* 通过线提示 */
.kb-result-card__threshold {
  font-size: var(--fts-font-size-xs, 12px);
  color: var(--fts-text-tertiary);
  padding: var(--fts-space-2, 8px);
  background-color: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-md, 6px);
  margin-bottom: var(--fts-space-4, 16px);

  /* 双条件布局（必会题 + 正确率） */
  &--dual {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-1, 4px);
    padding: var(--fts-space-2, 8px) var(--fts-space-3, 12px);
  }
}

.kb-threshold-item--fail {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-medium, 500);
}

.kb-threshold-icon {
  display: inline-block;
  width: 14px;
  text-align: center;
  margin-right: 2px;
  font-size: var(--fts-font-size-xs, 12px);
}

/* 答题明细（折叠列表） */
.kb-result-card__detail-list {
  margin-bottom: var(--fts-space-4, 16px);

  summary {
    cursor: pointer;
    font-size: var(--fts-font-size-xs, 12px);
    color: var(--fts-primary);
    padding: var(--fts-space-2, 8px) 0;
    border-bottom: 1px solid var(--fts-border-secondary);

    &:hover { opacity: 0.8; }
  }

  &[open] summary {
    border-bottom-color: transparent;
  }
}

.kb-result-card__detail-toggle {
  font-weight: var(--fts-font-weight-medium, 500);
}

.kb-result-detail-items {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1, 4px);
  max-height: 180px;
  overflow-y: auto;
  padding: var(--fts-space-2, 8px) 0;
}

.kb-result-detail-item {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2, 8px);
  padding: var(--fts-space-1, 4px) var(--fts-space-2, 8px);
  border-radius: var(--fts-radius-sm, 4px);
  font-size: var(--fts-font-size-xs, 12px);

  &--wrong {
    background-color: rgba(var(--fts-error-rgb), 0.06);
  }
}

.kb-detail-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 10px;
  font-weight: var(--fts-font-weight-semibold, 600);
  color: var(--fts-text-quaternary);
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm, 4px);
  flex-shrink: 0;
}

.kb-detail-text {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--fts-text-secondary);
}

.kb-detail-status {
  flex-shrink: 0;
  font-size: var(--fts-font-size-2xs, 11px);
  font-weight: var(--fts-font-weight-medium, 500);

  .kb-result-detail-item--wrong & {
    color: var(--fts-error);
  }
}

/* 结果页操作按钮区 */
.kb-result-card__actions {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2, 8px);
}

.kb-result-card__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-1, 4px);
  width: 100%;
  padding: var(--fts-space-2, 8px) 0;
  font-size: var(--fts-font-size-base, 14px);
  font-weight: var(--fts-font-weight-semibold, 600);
  border: none;
  border-radius: var(--fts-radius-md, 6px);
  cursor: pointer;
  transition: background-color var(--fts-duration-fast) var(--fts-easing-default), color var(--fts-duration-fast) var(--fts-easing-default), border-color var(--fts-duration-fast) var(--fts-easing-default);

  &--primary {
    color: var(--fts-text-on-primary);
    background-color: var(--fts-success);
    &:hover { opacity: 0.9; }
    &:active { transform: scale(0.98); }
  }

  &--warning {
    color: var(--fts-warning-dark);
    background-color: rgba(var(--fts-warning-rgb), 0.15);
    &:hover { opacity: 0.85; }
    &:active { transform: scale(0.98); }
  }

  &--secondary {
    color: var(--fts-text-tertiary);
    background-color: var(--fts-bg-tertiary);
    &:hover { color: var(--fts-text-secondary); }
    &:active { transform: scale(0.98); }
  }
}

/* ── 弹题进入/退出动画 ── */
.kb-popup-enter-active {
  transition: transform 0.25s ease-out, opacity 0.25s ease-out;
}
.kb-popup-leave-active {
  transition: transform 0.2s ease-in, opacity 0.2s ease-in;
}
.kb-popup-enter-from,
.kb-popup-leave-to {
  opacity: 0;

  .kb-quiz-popup__card {
    transform: scale(0.92) translateY(10px);
  }
}
</style>
