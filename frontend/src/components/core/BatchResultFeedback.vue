<script setup lang="ts">
/**
 * 批量操作结果反馈组件（部分成功 N/M，P1-UI-CORE-003）
 *
 * 【定位】core 展示层组件：接收页面批量操作执行结果，渲染「成功 N/M + 失败项列表」。
 * 组件本身不发起任何请求（不新增后端接口、不触碰业务规则）——批量操作由页面基于
 * 既有接口能力执行，执行后把结果传入本组件展示。
 *
 * 【契约（props/emits）】
 * - result: BatchResult | null —— 操作结果；null = 不展示
 *   - total: 选中总数 M
 *   - success: 成功数 N
 *   - failed: 失败项列表（label + reason，可选）
 * - close: 用户关闭反馈条（父组件应清空 result）
 *
 * 【使用示例】
 * const batchResult = ref<BatchResult | null>(null)
 * // 页面基于既有接口执行批量操作后：
 * batchResult.value = { total: 5, success: 3, failed: [{ label: '单号 A001', reason: '库存不足' }] }
 * <BatchResultFeedback :result="batchResult" @close="batchResult = null" />
 */
import { computed } from 'vue'
import { CircleCheck, WarningFilled } from '@element-plus/icons-vue'

/** 失败项（label 为业务标识、reason 为失败原因，均来自既有接口返回/页面映射） */
export interface BatchFailure {
  /** 失败项标识（如单号/名称），可选 */
  label?: string
  /** 失败原因（来自既有接口错误信息），可选 */
  reason?: string
}

/** 批量操作结果：total = 选中总数 M，success = 成功数 N */
export interface BatchResult {
  /** 选中总数 M */
  total: number
  /** 成功数 N */
  success: number
  /** 失败项列表 */
  failed?: BatchFailure[]
}

const props = withDefaults(defineProps<{ result?: BatchResult | null }>(), {
  result: null,
})

const emit = defineEmits<{ close: [] }>()

const failedCount = computed(() => props.result?.failed?.length ?? 0)
const allSuccess = computed(
  () => !!props.result && props.result.success >= props.result.total && failedCount.value === 0,
)
const summary = computed(() => {
  const r = props.result
  if (!r) return ''
  return `成功 ${r.success} / ${r.total}`
})
</script>

<template>
  <div
    v-if="result"
    class="batch-result-feedback"
    :class="
      allSuccess
        ? 'batch-result-feedback--success'
        : 'batch-result-feedback--partial'
    "
  >
    <div class="batch-result-feedback__summary">
      <el-icon class="batch-result-feedback__icon">
        <CircleCheck v-if="allSuccess" />
        <WarningFilled v-else />
      </el-icon>
      <span class="batch-result-feedback__text">
        {{ allSuccess ? '全部操作成功' : '部分操作失败' }}（{{ summary }}）
      </span>
      <el-button
        class="batch-result-feedback__close"
        link
        type="primary"
        @click="emit('close')"
      >
        关闭
      </el-button>
    </div>
    <ul v-if="!allSuccess && failedCount > 0" class="batch-result-feedback__failed">
      <li v-for="(f, i) in result.failed" :key="i" class="batch-result-feedback__failed-item">
        <span class="batch-result-feedback__failed-label">{{ f.label || '未命名项' }}</span>
        <span v-if="f.reason" class="batch-result-feedback__failed-reason">{{ f.reason }}</span>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="scss">
.batch-result-feedback {
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1px solid;
  border-radius: var(--fts-radius-lg);
  margin-bottom: var(--fts-space-3);
}

.batch-result-feedback--success {
  color: var(--fts-status-success-color);
  background: var(--fts-status-success-bg);
  border-color: var(--fts-status-success-border);
}

.batch-result-feedback--partial {
  color: var(--fts-status-warning-color);
  background: var(--fts-status-warning-bg);
  border-color: var(--fts-status-warning-border);
}

.batch-result-feedback__summary {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
}

.batch-result-feedback__text {
  flex: 1;
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
}

.batch-result-feedback__close {
  flex-shrink: 0;
}

.batch-result-feedback__failed {
  margin: var(--fts-space-2) 0 0 0;
  padding-left: var(--fts-space-5);
  list-style: disc;
}

.batch-result-feedback__failed-item {
  font-size: var(--fts-font-size-sm);
  line-height: var(--fts-line-height-normal);
}

.batch-result-feedback__failed-label {
  font-weight: var(--fts-font-weight-medium);
}

.batch-result-feedback__failed-reason {
  color: var(--fts-text-tertiary);
  margin-left: var(--fts-space-2);
}
</style>