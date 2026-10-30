<template>
  <el-dialog
    :model-value="modelValue"
    title="作废订单"
    width="460px"
    :append-to-body="true"
    :close-on-click-modal="false"
    data-test="exc-void-dialog"
    @update:model-value="close"
  >
    <div class="vd-body">
      <p class="vd-order">
        单号 <b>#{{ order?.orderNumber?.slice(-6) || '—' }}</b> ·
        金额 <b class="is-danger">¥{{ fenToYuan(order?.amount || 0).toFixed(2) }}</b>
      </p>

      <el-form label-position="top">
        <el-form-item label="作废原因" required>
          <el-select
            v-model="reason"
            placeholder="请选择作废原因"
            style="width: 100%"
            data-test="exc-void-reason"
          >
            <el-option v-for="r in REASONS" :key="r.value" :label="r.label" :value="r.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="needRemark" label="备注（必填）" required>
          <el-input
            v-model="remark"
            type="textarea"
            :rows="3"
            maxlength="100"
            show-word-limit
            placeholder="请写明作废原因，便于事后追查"
            data-test="exc-void-remark"
          />
        </el-form-item>
      </el-form>

      <p class="vd-warn" data-test="exc-void-warn">
        <el-icon><WarningFilled /></el-icon>
        作废后不可撤回：该单将永久不计入营业额，操作会记录操作人与时间。
      </p>
    </div>

    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button
        type="danger"
        :loading="submitting"
        :disabled="!canSubmit"
        data-test="exc-void-submit"
        @click="submit"
      >
        {{ submitting ? '作废中…' : '确认作废' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, watch } from 'vue'
import { ElMessageBox } from 'element-plus'
import { WarningFilled } from '@element-plus/icons-vue'
import type { ExceptionOrder } from '@/api/shiftExceptions'
import { fenToYuan } from '@/utils/shiftStatus'
import { useDraft } from '@/composables/useDraft'

const props = defineProps<{
  modelValue: boolean
  order: ExceptionOrder | null
  submitting?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'submit', payload: { reason: string; remark?: string }): void
}>()

/** 作废原因是枚举，后端按 code 落库；「其他」必须补备注 */
const REASONS = [
  { value: 'customer_cancel', label: '顾客取消' },
  { value: 'duplicate', label: '重复开单' },
  { value: 'wrong_item', label: '点错菜品' },
  { value: 'payment_failed', label: '支付失败未成交' },
  { value: 'closed_shop', label: '门店提前打烊' },
  { value: 'other', label: '其他（需备注）' }
]

/** 草稿：原因/备注实时落 sessionStorage，手抖刷新不白干（R8 P1-4） */
const { draft, clear: clearDraft } = useDraft(
  () => 'void:' + (props.order?.orderId || 'new'),
  { reason: '', remark: '' }
)
const reason = computed<string>({
  get: () => draft.value.reason,
  set: (v: string) => { draft.value.reason = v }
})
const remark = computed<string>({
  get: () => draft.value.remark,
  set: (v: string) => { draft.value.remark = v }
})
const needRemark = computed(() => reason.value === 'other')
const canSubmit = computed(() => {
  if (!reason.value) return false
  if (needRemark.value && remark.value.trim().length === 0) return false
  return true
})

watch(() => props.modelValue, (visible) => {
  // 打开不再清空：草稿优先（上次没提交完的原因/备注要能继续填）
  if (!visible && props.submitting) clearDraft()
})

function close(): void {
  emit('update:modelValue', false)
}

async function submit(): Promise<void> {
  if (!canSubmit.value || props.submitting) return
  const label = REASONS.find((r) => r.value === reason.value)?.label || reason.value
  try {
    // 二次确认：作废不可撤回，必须让人再点一次
    await ElMessageBox.confirm(
      '确认作废该订单？原因：' + label + (needRemark.value ? '（' + remark.value.trim() + '）' : '') + '。作废后不可撤回。',
      '二次确认',
      { type: 'warning', confirmButtonText: '确认作废', cancelButtonText: '再想想' }
    )
  } catch {
    return
  }
  emit('submit', {
    reason: reason.value,
    remark: needRemark.value ? remark.value.trim() : undefined
  })
}

/** 父组件提交成功后调用：清掉本单草稿 */
function markSubmitted(): void {
  clearDraft()
}

defineExpose({ reason, remark, markSubmitted })


</script>

<style scoped>
.vd-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vd-order {
  margin: 0;
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
}

.vd-order .is-danger {
  color: var(--pos-danger, #c94b4b);
}

.vd-warn {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 4px 0 0;
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  font-size: 12px;
}
</style>
