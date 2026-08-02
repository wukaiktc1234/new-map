<template>
  <div class="interview-timeline">
    <div v-if="!timeline || timeline.records.length === 0" class="interview-timeline__empty">
      <el-empty description="暂无面试记录" :image-size="60" />
    </div>

    <template v-else>
      <!-- 头部信息 -->
      <div class="interview-timeline__header">
        <h4 class="interview-timeline__title">面试历程</h4>
        <div class="interview-timeline__summary">
          <span class="interview-timeline__progress">
            {{ completedRounds }}/{{ timeline.totalRounds }}轮完成
          </span>
        </div>
      </div>

      <!-- 单轨混合时间线 -->
      <div class="interview-timeline__content">
        <div
          v-for="(record, index) in sortedRecords"
          :key="record.interviewId"
          class="interview-timeline__node"
          :class="[`interview-timeline__node--${record.type}`, { 'is-last': index === sortedRecords.length - 1 }]"
        >
          <!-- 左侧区域：类型标识 + 状态圆点 -->
          <div class="interview-timeline__left-area">
            <div
              class="interview-timeline__type-badge"
              :class="[`interview-timeline__type-badge--${record.type}`]"
            >
              <el-icon class="interview-timeline__type-icon">
                <component :is="getTypeConfig(record.type).icon" />
              </el-icon>
              <span>{{ getTypeConfig(record.type).label }}</span>
            </div>
            <div
              class="interview-timeline__dot"
              :class="[`interview-timeline__dot--${record.result}`]"
              :style="getDotStyle(record)"
            >
              <el-icon class="interview-timeline__dot-icon">
                <component :is="getResultIcon(record.result)" />
              </el-icon>
            </div>
          </div>

          <!-- 连接线 -->
          <div
            v-if="index < sortedRecords.length - 1"
            class="interview-timeline__connector"
          />

          <!-- 内容卡片 -->
          <div
            class="interview-timeline__card"
            @click="toggleExpand(record.interviewId)"
          >
            <div class="interview-timeline__card-header">
              <span class="interview-timeline__round">第{{ record.round }}轮</span>
              <span class="interview-timeline__card-name">{{ record.name }}</span>
              <StatusTag :status="getResultStatus(record.result)" size="small" />
            </div>

            <div class="interview-timeline__card-body">
              <p class="interview-timeline__info">
                {{ formatDateTime(record.scheduledTime) }}
                <span v-if="record.duration">({{ record.duration }}分钟)</span>
              </p>
              <p class="interview-timeline__info">
                面试官：{{ record.interviewerName }}
              </p>
              <p v-if="record.totalScore !== undefined" class="interview-timeline__score">
                评分：<strong>{{ record.totalScore }}分</strong>
              </p>
            </div>

            <el-button
              v-if="record.overallComment"
              link
              type="primary"
              size="small"
              class="interview-timeline__expand-btn"
            >
              {{ expandedIds.has(record.interviewId) ? '收起' : '展开详情' }}
            </el-button>

            <!-- 展开详情 -->
            <div
              v-if="expandedIds.has(record.interviewId) && record.overallComment"
              class="interview-timeline__detail"
            >
              <p class="interview-timeline__comment">{{ record.overallComment }}</p>
              <div v-if="record.strengths?.length" class="interview-timeline__tags">
                <span class="interview-timeline__tag interview-timeline__tag--strength">
                  优势：{{ record.strengths.join('、') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Shop, User } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import {
  InterviewType,
  InterviewResult,
  INTERVIEW_TYPE_CONFIG,
  INTERVIEW_RESULT_CONFIG
} from './interview-config'
import type { InterviewTimeline } from './types'

interface Props {
  timeline: InterviewTimeline | null
}

const props = defineProps<Props>()

const expandedIds = ref<Set<string>>(new Set())

const sortedRecords = computed(() => {
  if (!props.timeline) return []
  return [...props.timeline.records].sort((a, b) => a.round - b.round)
})

const completedRounds = computed(() => {
  if (!props.timeline) return 0
  return props.timeline.records.filter(r => r.result === InterviewResult.PASSED).length
})

function getTypeConfig(type: InterviewType) {
  return INTERVIEW_TYPE_CONFIG[type]
}

function getResultConfig(result: InterviewResult) {
  return INTERVIEW_RESULT_CONFIG[result]
}

function getResultStatus(result: InterviewResult): string {
  return getResultConfig(result).status
}

function getResultIcon(result: InterviewResult) {
  return getResultConfig(result).icon
}

function getDotStyle(record: Record<string, any>) {
  const config = getTypeConfig(record.type)
  const baseStyle: Record<string, string> = {
    borderColor: config.dotColor
  }

  switch (record.result) {
    case InterviewResult.PASSED:
      return {
        ...baseStyle,
        backgroundColor: config.dotColor,
        color: 'var(--fts-text-inverse)'
      }
    case InterviewResult.REJECTED:
      return {
        ...baseStyle,
        backgroundColor: 'var(--fts-error)',
        borderColor: 'var(--fts-error)',
        color: 'var(--fts-text-inverse)'
      }
    default:
      return {
        ...baseStyle,
        backgroundColor: 'var(--fts-bg-card)',
        color: config.dotColor
      }
  }
}

function toggleExpand(id: string) {
  if (expandedIds.value.has(id)) {
    expandedIds.value.delete(id)
  } else {
    expandedIds.value.add(id)
  }
}

function formatDateTime(isoString: string): string {
  if (!isoString) return '-'
  try {
    const date = new Date(isoString)
    return date.toLocaleString('zh-CN', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    })
  } catch {
    return isoString
  }
}
</script>

<style lang="scss" scoped>
/* var() fallback为防御性编码，确保CSS变量未定义时有默认样式 */
.interview-timeline {
  &__empty {
    padding: var(--fts-space-8);
    text-align: center;
  }

  &__header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: var(--fts-space-4);
    padding-bottom: var(--fts-space-3);
    border-bottom: 1px solid var(--fts-border-primary);
  }

  &__title {
    margin: 0;
    font-size: var(--fts-font-size-base);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-primary);
  }

  &__summary {
    display: flex;
    align-items: center;
    gap: var(--fts-space-3);
  }

  &__progress {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
  }

  // 单轨内容区
  &__content {
    position: relative;
  }

  // 节点容器
  &__node {
    position: relative;
    padding-left: 100px;
    padding-bottom: var(--fts-space-5);

    &.is-last {
      padding-bottom: 0;

      .interview-timeline__connector {
        display: none;
      }
    }

    // 门店类型
    &--store {
      .interview-timeline__type-badge--store {
        background: var(--fts-success-bg, rgba(103, 194, 58, 0.08));
        color: var(--fts-success-dark, #529b2e);
        border-left-color: var(--fts-success);
      }
    }

    // 人事类型
    &--hr {
      .interview-timeline__type-badge--hr {
        background: var(--fts-primary-bg, rgba(64, 158, 255, 0.08));
        color: var(--fts-primary-dark, #337ecc);
        border-left-color: var(--fts-primary);
      }
    }
  }

  // 左侧区域：类型徽章（固定宽度容器）
  &__left-area {
    position: absolute;
    left: 0;
    top: 0;
    width: 85px;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
  }

  // 类型标识徽章
  &__type-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    border-radius: 3px;
    font-size: 11px;
    font-weight: 600;
    white-space: nowrap;
    border-left-width: 3px;
    border-left-style: solid;

    &-icon {
      font-size: 12px;
      flex-shrink: 0;
    }
  }

  // 连接线：垂直细线
  &__connector {
    position: absolute;
    left: 92px;
    top: 24px;
    width: 1px;
    height: calc(100% - 20px);
    background: linear-gradient(
      to bottom,
      var(--fts-border-secondary) 0%,
      var(--fts-border-lighter, #f0f0f0) 100%
    );
    z-index: 0;
  }

  // 圆点节点：在连接线上
  &__dot {
    position: absolute;
    left: 85px;
    top: 10px;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 2px solid;
    background: var(--fts-bg-card);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 2;
    transition: all 0.25s ease;
    box-shadow: 0 0 0 3px var(--fts-bg-card);

    &--passed {
      animation: dot-pulse 2s infinite;
    }

    &-icon {
      width: 8px;
      height: 8px;
      font-size: 8px;
    }
  }

  // 卡片内容
  &__card {
    background: var(--fts-bg-card);
    border-radius: var(--fts-radius-sm);
    padding: var(--fts-space-3) var(--fts-space-4);
    box-shadow: var(--fts-shadow-xs);
    cursor: pointer;
    transition: all 0.25s ease;

    &:hover {
      box-shadow: var(--fts-shadow-md);
      transform: translateY(-1px);
    }
  }

  &__card-header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    margin-bottom: var(--fts-space-2);
  }

  &__round {
    font-size: var(--fts-font-size-xs);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-primary);
    background: var(--fts-primary-bg);
    padding: 2px 6px;
    border-radius: 4px;
  }

  &__card-name {
    font-weight: var(--fts-font-weight-medium);
    color: var(--fts-text-primary);
    flex: 1;
  }

  &__card-body {
    p {
      margin: 0 0 var(--fts-space-1);
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);
      line-height: 1.5;
    }
  }

  &__info {
    display: flex;
    align-items: center;
    gap: var(--fts-space-1);
  }

  &__score {
    strong {
      color: var(--fts-warning);
      font-weight: var(--fts-font-weight-bold);
    }
  }

  &__expand-btn {
    margin-top: var(--fts-space-2);
    padding: 0;
  }

  // 展开详情
  &__detail {
    margin-top: var(--fts-space-3);
    padding-top: var(--fts-space-3);
    border-top: 1px dashed var(--fts-border-secondary);
  }

  &__comment {
    margin: 0 0 var(--fts-space-2);
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-regular);
    line-height: 1.6;
  }

  &__tags {
    display: flex;
    flex-wrap: wrap;
    gap: var(--fts-space-2);
  }

  &__tag {
    font-size: var(--fts-font-size-xs);
    padding: 2px 8px;
    border-radius: 4px;

    &--strength {
      background: var(--fts-success-bg);
      color: var(--fts-success-dark);
    }
  }
}

@keyframes dot-pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 0.9;
  }
  50% {
    transform: scale(1.15);
    opacity: 0.7;
  }
}
</style>
