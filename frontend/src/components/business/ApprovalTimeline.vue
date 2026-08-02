<script setup lang="ts">
/**
 * ApprovalTimeline - 审批记录时间线组件
 *
 * 功能：
 * 1. 使用 el-timeline 展示审批流程的每一步操作
 * 2. 每个节点显示：操作人、操作类型、审批意见、操作时间
 * 3. 操作类型使用 StatusTag 显示不同颜色
 * 4. 时间线图标根据操作类型不同显示对应图标
 * 5. 支持加载状态
 *
 * 使用方式：
 * <ApprovalTimeline business-id="xxx" />
 * <ApprovalTimeline business-id="xxx" :loading="false" />
 */
import { ref, onMounted, watch, computed } from 'vue'
import {
  CircleCheck,
  CircleClose,
  RefreshLeft,
  Right,
  Clock,
} from '@element-plus/icons-vue'
import { approvalWorkflowApi } from '@/api/approval'
import { approvalWorkflowConverter } from '@/api/approval/converters'
import type { ApprovalAction, ApprovalAuditLogDisplay } from '@/types/approval-workflow'
import StatusTag from '@/components/core/StatusTag.vue'
import EmptyState from '@/components/core/EmptyState.vue'

interface Props {
  businessId: string
  businessType?: string
  loading?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  businessType: '',
  loading: false,
})

/** 审批记录列表 */
const auditLogs = ref<ApprovalAuditLogDisplay[]>([])

/** 内部加载状态 */
const innerLoading = ref(false)

/** 合并后的加载状态 */
const isLoading = computed(() => props.loading || innerLoading.value)

/** 是否为空 */
const isEmpty = computed(() => auditLogs.value.length === 0)

/** 操作类型 → 图标组件映射 */
const actionIconMap: Record<ApprovalAction, typeof CircleCheck> = {
  submit: Clock,
  approve: CircleCheck,
  reject: CircleClose,
  withdraw: RefreshLeft,
  delegate: Right,
  timeout: Clock,
}

/** 获取操作类型对应的图标 */
function getActionIcon(action: ApprovalAction): typeof CircleCheck {
  return actionIconMap[action] || Clock
}

/** 获取操作类型对应的 StatusTag status */
function getActionStatus(action: ApprovalAction): string {
  return approvalWorkflowConverter.toApprovalActionStatusTagStatus(action)
}

/** 获取操作类型标签 */
function getActionLabel(log: ApprovalAuditLogDisplay): string {
  if (log.action === 'delegate' && log.delegateToName) {
    return `委托至 ${log.delegateToName}`
  }
  return log.actionLabel
}

/** 加载审批记录 */
async function fetchAuditLogs() {
  if (!props.businessId) return
  innerLoading.value = true
  try {
    const data = await approvalWorkflowApi.getApprovalLogs(props.businessId)
    auditLogs.value = approvalWorkflowConverter.convertAuditLogsToDisplay(data || [])
  } catch {
    auditLogs.value = []
  } finally {
    innerLoading.value = false
  }
}

/** 获取 CSS 变量值（用于 el-timeline-item 的 color 属性） */
function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}

/** 时间线节点颜色（根据操作类型） */
const timelineColors = computed(() => {
  const success = getCssVar('--fts-success')
  const error = getCssVar('--fts-error')
  const warning = getCssVar('--fts-warning')
  const info = getCssVar('--fts-info')
  return {
    submit: info,
    approve: success,
    reject: error,
    withdraw: warning,
    delegate: info,
    timeout: info,
  } as Record<ApprovalAction, string>
})

/** 获取时间线节点颜色 */
function getTimelineColor(action: ApprovalAction): string {
  return timelineColors.value[action] || getCssVar('--fts-primary')
}

onMounted(() => {
  fetchAuditLogs()
})

/** 监听 businessId 变化重新加载 */
watch(() => props.businessId, () => {
  fetchAuditLogs()
})
</script>

<template>
  <div class="approval-timeline">
    <!-- 加载状态 -->
    <div v-if="isLoading" v-loading="true" class="approval-timeline__loading" />

    <!-- 空状态 -->
    <EmptyState
      v-else-if="isEmpty"
      type="no-data"
      description="暂无审批记录"
    />

    <!-- 时间线列表 -->
    <el-timeline v-else class="approval-timeline__list">
      <el-timeline-item
        v-for="log in auditLogs"
        :key="log.auditLogId"
        :color="getTimelineColor(log.action)"
        :timestamp="log.operateTime"
        placement="top"
      >
        <div class="timeline-item">
          <div class="timeline-item__header">
            <el-icon class="timeline-item__icon">
              <component :is="getActionIcon(log.action)" />
            </el-icon>
            <span class="timeline-item__operator">{{ log.operatorName }}</span>
            <StatusTag
              :status="getActionStatus(log.action)"
              :label="getActionLabel(log)"
              size="small"
            />
          </div>
          <div v-if="log.nodeName" class="timeline-item__node">
            {{ log.nodeName }}
          </div>
          <div v-if="log.comment" class="timeline-item__comment">
            {{ log.comment }}
          </div>
        </div>
      </el-timeline-item>
    </el-timeline>
  </div>
</template>

<style scoped lang="scss">
.approval-timeline {
  width: 100%;

  &__loading {
    min-height: 120px;
  }

  &__list {
    padding-left: var(--fts-space-1);
  }
}

.timeline-item {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);

  &__header {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    flex-wrap: wrap;
  }

  &__icon {
    font-size: 16px;
    flex-shrink: 0;
  }

  &__operator {
    font-size: var(--fts-font-size-base);
    font-weight: 500;
    color: var(--fts-text-primary);
  }

  &__node {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }

  &__comment {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.6;
    padding: var(--fts-space-2) var(--fts-space-3);
    background: var(--fts-bg-secondary);
    border-radius: var(--fts-radius-sm);
    border-left: 3px solid var(--fts-border-primary);
  }
}
</style>
