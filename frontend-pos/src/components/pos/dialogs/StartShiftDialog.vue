<script setup lang="ts">
/**
 * POS 开班对话框（N19-4 · 备用金）
 *
 * 备用金默认值优先级：
 *   ① 门店配置固定备用金 —— **后端尚无该配置来源**（无 store 级 reserve 字段/接口），
 *      这里保留 `loadStoreReserve()` 挂点，当前恒返回 null（已降级）；
 *   ② 上一次该门店/终端班次的 `openingCash`（取 /v1/pos/auth/shift/today 最后一条）；
 *   ③ 都为空 → 默认 0.00（收银员可直接回车开班，也可按实际备用金覆盖）。
 * ⚠️ 不硬编码 1000。
 *
 * 精度：元、最多 2 位小数；超过 2 位小数前端拦截并提示「备用金最多两位小数」，不提交。
 * 提交：POST /v1/pos/auth/shift/start，body 含 openingCash（元 / BigDecimal）。
 * 成功：写入 localStorage 开班标记（utils/shiftSession），刷新页面不再重复弹窗。
 *
 * 本次体验升级（图一）：
 *   - 终端标识脱敏：pos-localhost-1793xxx → 「前台收银机A」，原始串不再出现在界面上；
 *   - 弹窗加宽至 560px，金额输入放大，回车即开班（键盘流不必摸鼠标）；
 *   - 无历史备用金时默认 0，不再是「空着不让提交」；
 *   - 开班成功落盘，交班时由 ShiftHandover 清除。
 */
import { computed, nextTick, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Monitor, User } from '@element-plus/icons-vue'
import { posApi, type PosShiftRecord } from '@/api/posApi'
import { terminalDisplayName, terminalTooltip } from '@/utils/terminal'
import { markShiftStarted } from '@/utils/shiftSession'
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
/** 默认值来源：store-config / last-shift / zero */
const defaultSource = ref<'store-config' | 'last-shift' | 'zero'>('zero')
/** 默认值快照（提示「沿用上一班 X」，不随用户编辑变化） */
const defaultValue = ref('')
const cashInputRef = ref<{ focus: () => void; select?: () => void } | null>(null)

/** 无历史备用金时的兜底值（元） */
const ZERO_DEFAULT = '0.00'
/** 快捷金额（元）：门店常用备用金档位，点一下即可，减少手打错误 */
const QUICK_AMOUNTS = [0, 200, 500, 1000]

/** 终端展示名与脱敏提示（在 setup 阶段解析一次，避免渲染期反复读写 localStorage） */
const terminalName = ref(terminalDisplayName(props.terminalId))
const terminalTip = ref(terminalTooltip(props.terminalId))

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
      defaultValue.value = raw.value
      defaultSource.value = 'store-config'
      return
    }
    const fromLastShift = await loadLastShiftOpeningCash()
    if (fromLastShift != null) {
      raw.value = fromLastShift.toFixed(2)
      defaultValue.value = raw.value
      defaultSource.value = 'last-shift'
      return
    }
    // 无历史备用金：默认 0.00，收银员可直接回车开班（不再卡在「必填为空」）
    raw.value = ZERO_DEFAULT
    defaultValue.value = ZERO_DEFAULT
    defaultSource.value = 'zero'
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

/** 快捷金额：点一下直接覆盖输入（0 元也允许，代表空钱箱） */
function applyQuickAmount(amount: number): void {
  raw.value = amount.toFixed(2)
  errorText.value = ''
}

watch(raw, () => {
  // 输入即校验，但不清空已存在的错误除非已合法
  if (decimalsExceeded.value) errorText.value = '备用金最多两位小数'
  else if (errorText.value === '备用金最多两位小数') errorText.value = ''
})

/** 打开时：刷新终端别名、载入默认值，并把焦点放到金额输入框 */
function handleOpened(): void {
  terminalName.value = terminalDisplayName(props.terminalId)
  terminalTip.value = terminalTooltip(props.terminalId)
  errorText.value = ''
  void loadDefaults().then(() => {
    void nextTick(() => {
      cashInputRef.value?.focus?.()
      cashInputRef.value?.select?.()
    })
  })
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) handleOpened()
  },
  { immediate: true },
)

async function submit(): Promise<void> {
  if (loading.value) return
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
    // 开班成功即落盘：刷新页面不再重复弹窗，交班时清除
    markShiftStarted({
      terminalId: props.terminalId,
      shiftId: shift?.shiftId,
      employeeId: props.employeeId,
      employeeName: props.employeeName,
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

/** 回车确认：输入框内回车、弹窗任意位置回车都能开班（键盘流不摸鼠标） */
function handleEnter(event: KeyboardEvent): void {
  if (!props.modelValue || loading.value) return
  if (event.isComposing) return
  event.preventDefault()
  void submit()
}

/** 兜底：焦点不在弹窗内部时（例如刚点过快捷金额）依然响应回车 */
function handleWindowKeydown(event: KeyboardEvent): void {
  if (event.key !== 'Enter') return
  if (!props.modelValue) return
  handleEnter(event)
}

watch(
  () => props.modelValue,
  (visible) => {
    if (typeof window === 'undefined') return
    if (visible) window.addEventListener('keydown', handleWindowKeydown)
    else window.removeEventListener('keydown', handleWindowKeydown)
  },
)

onUnmounted(() => {
  if (typeof window !== 'undefined') window.removeEventListener('keydown', handleWindowKeydown)
})

function close(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="开班 · 备用金"
    width="560px"
    class="start-shift-dialog"
    :close-on-click-modal="false"
    :show-close="false"
    :append-to-body="true"
    data-test="start-shift-dialog"
    @keyup.enter="handleEnter"
  >
    <div class="start-shift" v-loading="defaultsLoading" @keyup.enter="handleEnter">
      <!-- 终端 / 员工：终端号脱敏为「前台收银机A」，原始 pos-localhost-1793xxx 不再上屏 -->
      <p class="start-shift__hint" data-test="shift-terminal-line">
        <span class="start-shift__badge" :title="terminalTip" data-test="terminal-display">
          <el-icon><Monitor /></el-icon>
          {{ terminalName }}
        </span>
        <span class="start-shift__operator">
          <el-icon><User /></el-icon>
          {{ employeeName || employeeId || '当前收银员' }}
        </span>
      </p>

      <div class="start-shift__field">
        <label class="start-shift__label" for="opening-cash">备用金（元）</label>
        <el-input
          id="opening-cash"
          ref="cashInputRef"
          v-model="raw"
          class="start-shift__input"
          size="large"
          placeholder="0.00"
          inputmode="decimal"
          data-test="opening-cash-input"
          :data-raw="raw"
          clearable
          @blur="validate"
          @keyup.enter.stop="handleEnter"
        >
          <template #prefix><span class="start-shift__currency">¥</span></template>
        </el-input>
      </div>

      <div class="start-shift__quick" role="group" aria-label="常用备用金">
        <button
          v-for="amount in QUICK_AMOUNTS"
          :key="amount"
          type="button"
          class="start-shift__quick-btn"
          :class="{ 'is-active': parsed === amount }"
          :data-test="`opening-cash-quick-${amount}`"
          @click="applyQuickAmount(amount)"
        >¥{{ amount }}</button>
      </div>

      <p class="start-shift__source" data-test="opening-cash-source">
        <template v-if="defaultSource === 'last-shift'">
          沿用上一班 {{ defaultValue }}（仅为默认值，可修改）
        </template>
        <template v-else-if="defaultSource === 'store-config'">
          沿用门店固定备用金 {{ defaultValue }}（仅为默认值，可修改）
        </template>
        <template v-else>
          无历史备用金，已默认 {{ defaultValue }}，请按钱箱实际金额修改
        </template>
      </p>

      <p v-if="errorText" class="start-shift__error" data-test="opening-cash-error">{{ errorText }}</p>
    </div>

    <template #footer>
      <span class="start-shift__enter-tip">回车键可直接开班</span>
      <el-button class="start-shift__cancel" @click="close">稍后</el-button>
      <el-button
        type="primary"
        class="start-shift__submit"
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
  gap: 12px;
}

.start-shift__hint {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0;
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
}

.start-shift__badge,
.start-shift__operator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: var(--pos-radius-full, 9999px);
  background: var(--pos-bg-tertiary, #f1f5f9);
  color: var(--pos-text-primary, #1e293b);
  font-weight: 600;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.start-shift__badge {
  cursor: help;
  border: 1px solid var(--pos-border-color, #e2e8f0);
}

.start-shift__operator {
  font-weight: 500;
  background: transparent;
  padding: 4px 2px;
  color: var(--pos-text-secondary, #64748b);
}

.start-shift__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.start-shift__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--pos-text-secondary, #64748b);
}

.start-shift__currency {
  font-size: 20px;
  font-weight: 700;
  color: var(--pos-primary, #e3651c);
  padding-right: 4px;
}

/* 金额是收银员唯一要看清的数字：大字号 + 等宽数字，减少误读 */
.start-shift__input :deep(.el-input__inner) {
  height: 56px;
  font-size: 30px;
  font-weight: 700;
  font-family: var(--pos-font-mono, 'Courier New', monospace);
  letter-spacing: 1px;
  color: var(--pos-text-primary, #1e293b);
}

.start-shift__quick {
  display: flex;
  gap: 10px;
}

.start-shift__quick-btn {
  flex: 1;
  min-height: 44px;
  border-radius: var(--pos-radius-sm, 8px);
  border: 1.5px solid var(--pos-border-color, #e2e8f0);
  background: var(--pos-bg-secondary, #f8fafc);
  color: var(--pos-text-primary, #1e293b);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--pos-transition-fast, 150ms ease);
}

.start-shift__quick-btn:hover {
  border-color: var(--pos-primary, #e3651c);
  color: var(--pos-primary, #e3651c);
  background: #fff7ed;
}

.start-shift__quick-btn.is-active {
  border-color: var(--pos-primary, #e3651c);
  background: var(--pos-primary, #e3651c);
  color: #ffffff;
}

.start-shift__source {
  margin: 0;
  font-size: 12px;
  color: var(--pos-text-secondary, #64748b);
}

.start-shift__error {
  margin: 0;
  font-size: 13px;
  font-weight: 600;
  color: var(--pos-danger, #dc2626);
}

.start-shift__enter-tip {
  float: left;
  line-height: 40px;
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}

.start-shift__cancel,
.start-shift__submit {
  min-height: 44px !important;
  min-width: 96px !important;
  font-size: 15px !important;
}
</style>
