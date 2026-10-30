<template>
  <el-dialog
    :model-value="modelValue"
    title="补收款"
    width="480px"
    :append-to-body="true"
    :close-on-click-modal="false"
    data-test="exc-resolve-dialog"
    @update:model-value="close"
  >
    <div class="rd-body">
      <p class="rd-order">
        单号 <b>#{{ order?.orderNumber?.slice(-6) || '—' }}</b> ·
        应收 <b class="is-danger" data-test="exc-resolve-due" :data-due-fen="dueFen">¥{{ fenToYuan(dueFen).toFixed(2) }}</b>
      </p>

      <el-tabs v-model="method" class="rd-tabs" @tab-change="onTabChange">
        <!-- 现金 -->
        <el-tab-pane label="现金" name="cash">
          <el-form label-position="top">
            <el-form-item label="实收现金">
              <el-input
                v-model="receivedInput"
                placeholder="输入顾客实付金额"
                inputmode="decimal"
                data-test="exc-received-input"
              >
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>
            <div class="rd-change" data-test="exc-change">
              应找零 <b>¥{{ change.toFixed(2) }}</b>
            </div>
          </el-form>
        </el-tab-pane>

        <!-- 扫码：后端接口就绪才出现（F-P0-1 断头路） -->
        <el-tab-pane v-if="scanReady" label="扫码" name="scan">
          <div class="rd-scan">
            <div v-if="!scanPay" class="rd-scan__placeholder">
              <el-button
                type="primary"
                :loading="creatingScan"
                data-test="exc-scan-create"
                @click="createScan"
              >
                生成收款码
              </el-button>
              <span class="rd-scan__hint">生成后每 2 秒自动查询一次支付结果</span>
            </div>
            <div v-else class="rd-scan__qr" data-test="exc-scan-qr">
              <div class="rd-qr-box">{{ scanPay.qrContent }}</div>
              <div class="rd-scan__state" data-test="exc-scan-state">
                等待顾客扫码支付 · 剩余 {{ Math.ceil(remainMs / 1000) }}s
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <template #footer>
      <el-button :disabled="submitting" @click="close">取消</el-button>
      <el-button
        v-if="method === 'cash'"
        type="primary"
        :loading="submitting"
        :disabled="!canSubmitCash"
        data-test="exc-resolve-submit"
        @click="submitCash"
      >
        {{ submitting ? '提交中…' : '确认收款' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
// 单位约定见 utils/shiftStatus.ts：后端 amount 是分，输入框是元，提交回分
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fenToYuan, yuanToFen } from '@/utils/shiftStatus'
import { resolveErrorMessage } from '@/utils/errorMessage'
import type { ExceptionOrder, ScanPayment } from '@/api/shiftExceptions'
import { useScanPaymentPolling } from '@/composables/useScanPaymentPolling'
import { POS_FLAGS } from '@/config/posFlags'
import { useDraft } from '@/composables/useDraft'

const props = defineProps<{
  modelValue: boolean
  order: ExceptionOrder | null
  submitting?: boolean
  /** 由父组件注入（内部走 store.createScanPayment），避免子组件直接碰 store */
  createScan?: (orderId: string) => Promise<ScanPayment | null>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'submit-cash', payload: { amountFen: number; cashReceivedFen: number }): void
  (e: 'scan-paid'): void
}>()

/** 后端没就绪的支付方式一律不画入口 */
const scanReady = computed<boolean>(() => POS_FLAGS.SCAN_BACKEND_READY)
/** 草稿：实收金额/当前方式实时落 sessionStorage（R8 P1-4） */
const { draft: resolveDraft, clear: clearResolveDraft } = useDraft<{ method: 'cash' | 'scan'; received: string }>(
  () => 'resolve:' + (props.order?.orderId || 'new'),
  { method: 'cash', received: '' }
)
const method = computed<'cash' | 'scan'>({
  get: (): 'cash' | 'scan' => (scanReady.value ? resolveDraft.value.method : 'cash'),
  set: (v: 'cash' | 'scan') => { resolveDraft.value.method = v }
})
const receivedInput = computed<string>({
  get: () => resolveDraft.value.received,
  set: (v: string) => { resolveDraft.value.received = v }
})
const scanPay = ref<ScanPayment | null>(null)
const creatingScan = ref(false)
const { start: startPolling, stop: stopPolling, remainMs } = useScanPaymentPolling()

/**
 * ⚠ 单位：ExceptionOrder.amount 是**分**（2200 = ¥22.00）。
 * 这里所有金额计算都在「分」上做，输入框里的「元」只在解析时用 yuanToFen 转一次；
 * 提交给后端的也是分（amountFen / cashReceivedFen），避免旧代码把 2200 分当 2200 元再 ×100 → 422 金额不符。
 */
const dueFen = computed(() => Math.round(props.order?.amount ?? 0))
const receivedFen = computed(() => {
  const raw = receivedInput.value.trim()
  if (!raw) return 0
  const value = Number(raw)
  return Number.isFinite(value) ? yuanToFen(value) : 0
})
/** 找零（元）= (实收 − 应收) / 100，负数按 0 显示，避免「负找零」 */
const change = computed(() => Math.max(0, receivedFen.value - dueFen.value) / 100)
const canSubmitCash = computed(() => receivedFen.value >= dueFen.value && dueFen.value > 0 && !props.submitting)

watch(() => props.modelValue, (visible) => {
  if (!visible) {
    stopPolling()
    scanPay.value = null
    receivedInput.value = ''
    method.value = 'cash'
  }
})

function close(): void {
  if (props.submitting) return
  emit('update:modelValue', false)
}

function onTabChange(): void {
  if (method.value !== 'scan') {
    stopPolling()
    scanPay.value = null
  }
}

async function createScan(): Promise<void> {
  // 兜底：即使被外围强行切到扫码，也只给中文提示，绝不发请求（避免 405/501 英文报错直出）
  if (!scanReady.value) {
    ElMessage.warning('扫码功能暂未开放，请使用现金收款')
    method.value = 'cash'
    return
  }
  if (creatingScan.value || !props.order || !props.createScan) return
  creatingScan.value = true
  try {
    const result = await props.createScan(props.order.orderId)
    if (!result) return
    scanPay.value = result
    // 关键：此时**不做任何账目更新**，只开始轮询
    startPolling(result.payNo, {
      intervalMs: 2000,
      timeoutMs: 120000,
      onPaid: () => {
        ElMessage.success('扫码支付成功')
        emit('scan-paid')
        emit('update:modelValue', false)
      },
      onTimeout: () => {
        ElMessage.warning('收款码已超时，请重新生成或改用现金')
        scanPay.value = null
      }
    })
  } catch (error) {
    ElMessage.error('生成收款码失败：' + resolveErrorMessage(error, '请重试'))
  } finally {
    creatingScan.value = false
  }
}

function submitCash(): void {
  if (!canSubmitCash.value) return
  // 提交给后端的是**分**（amountFen / cashReceivedFen），与 POST .../resolve 契约一致
  emit('submit-cash', { amountFen: dueFen.value, cashReceivedFen: receivedFen.value })
}

/** 父组件提交成功后调用：清掉本单草稿 */
function markSubmitted(): void {
  clearResolveDraft()
}

defineExpose({ method, receivedInput, scanPay, markSubmitted })
</script>

<style scoped>
.rd-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.rd-order {
  margin: 0;
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
}

.rd-order .is-danger {
  color: var(--pos-danger, #c94b4b);
}

.rd-change {
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-bg-tertiary, #f1f5f9);
  font-size: 13px;
  color: var(--pos-text-secondary, #64748b);
}

.rd-change b {
  font-family: var(--pos-font-number, monospace);
  font-size: 18px;
  color: var(--pos-primary, #e3651c);
}

.rd-scan {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
}

.rd-scan__hint {
  font-size: 12px;
  color: var(--pos-text-muted, #94a3b8);
}

.rd-qr-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 180px;
  height: 180px;
  padding: 8px;
  border-radius: var(--pos-radius-sm, 8px);
  background: #fff;
  box-shadow: inset 0 0 0 1px var(--pos-border-color, #e2e8f0);
  font-size: 10px;
  word-break: break-all;
  color: #111;
}

.rd-scan__state {
  font-size: 12px;
  color: var(--pos-warning-text, #8a6114);
}
</style>
