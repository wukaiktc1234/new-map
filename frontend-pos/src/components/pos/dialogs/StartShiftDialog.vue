<script setup lang="ts">
/**
 * POS 开班对话框（N19-4 · 备用金）
 *
 * 备用金默认值优先级：
 *   ① 门店配置固定备用金 —— **后端尚无该配置来源**（无 store 级 reserve 字段/接口），
 *      这里保留 `loadStoreReserve()` 挂点，当前恒返回 null（已降级）；
 *   ② 上一次该门店/终端班次的 `openingCash`（取 /v1/pos/auth/shift/today 最后一条）；
 *   ③ 都为空 → 必填（空不允许提交）。
 * ⚠️ 不硬编码 1000。
 *
 * 精度：元、最多 2 位小数；超过 2 位小数前端拦截并提示「备用金最多两位小数」，不提交。
 * 提交：POST /v1/pos/auth/shift/start，body 含 openingCash（元 / BigDecimal）。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { posApi, type PosShiftRecord } from '@/api/posApi'
import logger from '@/utils/logger'

interface Props {
  modelValue: boolean
  terminalId: string
  employeeId: string
  employeeName?: string
  shiftType?: string
}

const props = withDefaults(defineProps<Props>(), {
  employeeName: '',
  shiftType: 'morning',
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', shift: PosShiftRecord): void
}>()

/** 输入原值（字符串，保留用户输入用于精度校验） */
const raw = ref('')
const errorText = ref('')
const loading = ref(false)
const defaultsLoading = ref(false)
/** 默认值来源：store-config / last-shift / none */
const defaultSource = ref<'store-config' | 'last-shift' | 'none'>('none')

/** 门店固定备用金配置（后端暂无来源，恒 null；接口就绪后在此替换） */
async function loadStoreReserve(): Promise<number | null> {
  // TODO(backend): 门店级固定备用金配置缺失（无 store.reserve_fen / 配置接口）。
  // 后端提供后在此请求，例如 GET /v1/stores/{storeId}/shift-reserve → 元。
  return null
}

/** 上一次该终端班次的 openingCash（元） */
async function loadLastShiftOpeningCash(): Promise<number | null> {
  try {
    const shifts = await posApi.getTodayShifts(props.terminalId)
    const list = Array.isArray(shifts) ? shifts : []
    for (let i = list.length - 1; i >= 0; i -= 1) {
      const value = list[i]?.openingCash
      if (typeof value === 'number' && Number.isFinite(value)) return value
    }
    return null
  } catch (error) {
    logger.errorMessage('[StartShiftDialog] 读取上一班备用金失败', error)
    return null
  }
}

async function loadDefaults(): Promise<void> {
  defaultsLoading.value = true
  try {
    const fromConfig = await loadStoreReserve()
    if (fromConfig != null) {
      raw.value = fromConfig.toFixed(2)
      defaultSource.value = 'store-config'
      return
    }
    const fromLastShift = await loadLastShiftOpeningCash()
    if (fromLastShift != null) {
      raw.value = fromLastShift.toFixed(2)
      defaultSource.value = 'last-shift'
      return
    }
    raw.value = ''
    defaultSource.value = 'none'
  } finally {
    defaultsLoading.value = false
  }
}

/** 最多两位小数（元） */
const DECIMAL_OK = /^\d+(\.\d{1,2})?$/

const parsed = computed<number | null>(() => {
  const text = raw.value.trim()
  if (!text) return null
  if (!DECIMAL_OK.test(text)) return null
  const num = Number(text)
  return Number.isFinite(num) ? num : null
})

const decimalsExceeded = computed<boolean>(() => {
  const text = raw.value.trim()
  return /\.\d{3,}$/.test(text)
})

const canSubmit = computed<boolean>(
  () => parsed.value != null && parsed.value >= 0 && !decimalsExceeded.value && !loading.value,
)

/** 实时给出错误提示（供 data-test="opening-cash-error" 断言） */
function validate(): boolean {
  const text = raw.value.trim()
  if (!text) {
    errorText.value = '请填写备用金'
    return false
  }
  if (decimalsExceeded.value) {
    errorText.value = '备用金最多两位小数'
    return false
  }
  if (!DECIMAL_OK.test(text)) {
    errorText.value = '备用金格式不正确（元，最多两位小数）'
    return false
  }
  errorText.value = ''
  return true
}

watch(raw, () => {
  // 输入即校验，但不清空已存在的错误除非已合法
  if (decimalsExceeded.value) errorText.value = '备用金最多两位小数'
  else if (errorText.value === '备用金最多两位小数') errorText.value = ''
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      errorText.value = ''
      void loadDefaults()
    }
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  if (!validate()) {
    ElMessage.warning(errorText.value)
    return
  }
  const openingCash = parsed.value
  if (openingCash == null) return
  loading.value = true
  try {
    const shift = await posApi.startShift({
      terminalId: props.terminalId,
      employeeId: props.employeeId,
      employeeName: props.employeeName,
      shiftType: props.shiftType,
      openingCash,
    })
    ElMessage.success('开班成功，备用金 ' + openingCash.toFixed(2) + ' 元')
    emit('success', shift)
    emit('update:modelValue', false)
  } catch (error) {
    const msg = error && typeof error === 'object' && 'message' in error
      ? String((error as { message: unknown }).message)
      : '开班失败，请稍后重试'
    errorText.value = msg
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

function close(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="开班"
    width="460px"
    :close-on-click-modal="false"
    :show-close="false"
    data-test="start-shift-dialog"
  >
    <div class="start-shift" v-loading="defaultsLoading">
      <p class="start-shift__hint">
        终端：<span class="mono">{{ terminalId }}</span> · 员工：{{ employeeName || employeeId }}
      </p>

      <el-form label-width="110px">
        <el-form-item label="备用金（元）" required>
          <el-input
            v-model="raw"
            placeholder="请输入开班备用金（元，最多两位小数）"
            inputmode="decimal"
            data-test="opening-cash-input"
            :data-raw="raw"
            clearable
            @blur="validate"
          />
        </el-form-item>
      </el-form>

      <p class="start-shift__source">
        默认值来源：{{
          defaultSource === 'store-config' ? '门店固定备用金'
          : defaultSource === 'last-shift' ? '上一班次开班现金'
          : '无默认值（必填）'
        }}
      </p>

      <p v-if="errorText" class="start-shift__error" data-test="opening-cash-error">{{ errorText }}</p>
    </div>

    <template #footer>
      <el-button @click="close">稍后</el-button>
      <el-button
        type="primary"
        data-test="start-shift-submit"
        :disabled="!canSubmit"
        :loading="loading"
        @click="submit"
      >开班</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.start-shift {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.start-shift__hint {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--pos-text-secondary, #909399);
}

.start-shift__source {
  margin: 0;
  font-size: 12px;
  color: var(--pos-text-secondary, #909399);
}

.start-shift__error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--pos-danger, #f56c6c);
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}
</style>
