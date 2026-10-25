<script setup lang="ts">
/**
 * 对账工作台 - 核销/处理对话框（P3-1，AC-48）
 *
 * 两条硬约束：
 * 1) `reason` 为空时「提交」按钮 disabled，**且 submit() 内部直接 return（不发请求）**；
 * 2) `resolution` 只允许 WRITE_OFF / EXEMPT / REVERSE（下拉就这三项，提交前再校验一次）。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { RECON_RESOLUTION_LABELS, RECON_RESOLUTIONS } from '@/types/finance-recon'
import type { ReconDifferenceVO, ReconResolution } from '@/types/finance-recon'

interface Props {
  modelValue: boolean
  difference: ReconDifferenceVO | null
  submitting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  submitting: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: { resolution: ReconResolution; remark: string }): void
}>()

const resolution = ref<ReconResolution>('WRITE_OFF')
const reason = ref('')

const resolutionOptions = RECON_RESOLUTIONS.map((value) => ({
  value,
  label: RECON_RESOLUTION_LABELS[value],
}))

/** 原因必填（trim 后非空）——按钮 disabled 的依据 */
const reasonValid = computed<boolean>(() => reason.value.trim().length > 0)
const resolutionValid = computed<boolean>(() => (RECON_RESOLUTIONS as readonly string[]).includes(resolution.value))
const submitDisabled = computed<boolean>(() => !reasonValid.value || !resolutionValid.value || props.submitting)

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      resolution.value = 'WRITE_OFF'
      reason.value = ''
    }
  },
)

function close(): void {
  emit('update:modelValue', false)
}

/**
 * 提交：AC-48 —— 原因为空 / 决议非法 时**直接 return，不发请求**。
 * （按钮 disabled 只是 UI 层，这里再兜一次，防止回车/程序化触发绕过）
 */
function handleSubmit(): void {
  const remark = reason.value.trim()
  if (!remark) {
    ElMessage.warning('请填写处理原因')
    return
  }
  if (!(RECON_RESOLUTIONS as readonly string[]).includes(resolution.value)) {
    ElMessage.warning('处理方式非法')
    return
  }
  emit('submit', { resolution: resolution.value, remark })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="差异处理"
    width="520px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div class="resolve-dialog">
      <p class="resolve-dialog__meta">
        <span>差异 #{{ difference?.differenceId ?? '-' }}</span>
        <span>·</span>
        <span>差额 {{ difference?.diffAmount != null ? (difference.diffAmount / 100).toFixed(2) : '0.00' }} 元</span>
      </p>

      <el-form label-width="88px" class="resolve-dialog__form">
        <el-form-item label="处理方式">
          <el-select v-model="resolution" style="width: 100%" data-test="resolve-resolution">
            <el-option
              v-for="opt in resolutionOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="处理原因" required>
          <el-input
            v-model="reason"
            type="textarea"
            :rows="4"
            maxlength="200"
            show-word-limit
            placeholder="请填写处理原因（必填，作为决议备注写入 resolve.remark）"
            data-test="resolve-reason"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button
        type="primary"
        data-test="resolve-submit"
        :disabled="submitDisabled"
        :loading="submitting"
        @click="handleSubmit"
      >提交</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.resolve-dialog__meta {
  margin: 0 0 12px;
  font-size: 13px;
  color: var(--fts-text-secondary, #909399);
  display: flex;
  gap: 6px;
  font-variant-numeric: tabular-nums;
}

.resolve-dialog__form {
  margin-top: 4px;
}
</style>
