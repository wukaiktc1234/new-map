<template>
  <div class="approval-timeline">
    <el-timeline>
      <el-timeline-item
        v-for="(step, index) in displaySteps"
        :key="index"
        :timestamp="step.time"
        placement="top"
        :type="isLastStep(index) ? 'primary' : undefined"
        :hollow="!isLastStep(index)"
      >
        <div class="timeline-step">
          <div class="step-main">
            <span class="step-actor">{{ step.actor }}</span>
            <span class="step-action">{{ step.action }}</span>
          </div>
          <p v-if="step.comment" class="step-comment">{{ step.comment }}</p>
        </div>
      </el-timeline-item>
    </el-timeline>

    <!-- 展开/折叠按钮 -->
    <div v-if="steps.length > COLLAPSE_THRESHOLD" class="expand-toggle">
      <el-button
        link
        type="primary"
        size="small"
        @click="expanded = !expanded"
      >
        {{ expanded ? '收起' : `展开全部（共${steps.length}步）` }}
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
/**
 * ApprovalTimeline - 审批时间线组件
 *
 * 使用 el-timeline 展示审批流程的各步骤记录。
 * 超过5步时自动折叠，支持展开查看全部。
 * 最后一步使用 primary 色圆点高亮。
 */
import { ref, computed } from 'vue'

/** 时间线步骤数据结构 */
export interface TimelineStep {
  time: string
  actor: string
  action: string
  comment?: string
}

/** 折叠阈值：超过此数量时自动折叠 */
const COLLAPSE_THRESHOLD = 5

interface Props {
  steps: TimelineStep[]
}

const props = defineProps<Props>()

const emit = defineEmits<{}>({})

/** 是否展开全部 */
const expanded = ref(false)

/** 当前展示的步骤列表（受展开/折叠控制） */
const displaySteps = computed<TimelineStep[]>(() => {
  if (expanded.value || props.steps.length <= COLLAPSE_THRESHOLD) {
    return props.steps
  }
  // 折叠状态：显示前4步 + 最后一步
  return [...props.steps.slice(0, COLLAPSE_THRESHOLD - 1), props.steps[props.steps.length - 1]]
})

/** 判断是否为最后一步（用于高亮） */
function isLastStep(index: number): boolean {
  return index === displaySteps.value.length - 1
}
</script>

<style scoped lang="scss">
.approval-timeline {
  padding: var(--fts-space-3) var(--fts-space-4);

  :deep(.el-timeline) {
    padding-left: 0;
  }

  :deep(.el-timeline-item__wrapper) {
    padding-left: var(--fts-space-4);
  }

  :deep(.el-timeline-item__timestamp) {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-placeholder);
  }
}

.timeline-step {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.step-main {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);
  flex-wrap: wrap;
}

.step-actor {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.step-action {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.step-comment {
  margin: var(--fts-space-1) 0 0 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  line-height: 1.6;
  padding: var(--fts-space-1) var(--fts-space-2);
  background-color: var(--fts-bg-tertiary);
  border-radius: var(--fts-radius-sm, 4px);
}

/* ===== 展开/折叠 ===== */
.expand-toggle {
  display: flex;
  justify-content: center;
  padding-top: var(--fts-space-2);
}
</style>
