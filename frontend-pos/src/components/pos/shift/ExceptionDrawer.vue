<template>
  <el-drawer
    :model-value="modelValue"
    title="异常单处理"
    size="600px"
    :append-to-body="true"
    data-test="exc-drawer"
    @update:model-value="close"
  >
    <!-- data-test 兼容旧的异常抽屉断言（视觉/对比度用例仍按老结构取值） -->
    <div class="exc-drawer" data-test="shift-anomaly-drawer">
      <el-tabs v-model="store.tab" data-test="exc-tabs" @tab-change="onTabChange">
        <el-tab-pane name="pending">
          <template #label>
            <span data-test="exc-tab-pending">待处理 {{ store.pendingExceptionCount || (fallbackPending || []).length }}</span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="processed">
          <template #label>
            <span data-test="exc-tab-processed">已处理 {{ store.processed.length }}</span>
          </template>
        </el-tab-pane>
      </el-tabs>

      <!-- 操作型错误就地展示（作废/补收/409 冲突），不弹全局 toast -->
      <div v-if="actionError" class="exc-inline-error" role="alert" data-test="exc-action-error">
        <el-icon><WarningFilled /></el-icon>
        <span>{{ actionError }}</span>
        <button type="button" class="exc-inline-error__close" aria-label="关闭" @click="clearActionError">×</button>
      </div>

      <div v-if="store.tab === 'pending'" class="exc-tip sh-anomaly-tip">
        <el-icon><InfoFilled /></el-icon>
        这些订单未计入营业额与应上交现金。补收款或作废后，账目由服务端重新下发。
      </div>

      <div v-loading="store.loadingList" class="exc-list" data-test="exc-list">
        <ExceptionCard
          v-for="item in displayList"
          :key="item.orderId"
          class="exc-card-item sh-order-item is-anomaly"
          :order="item"
          :handled="store.tab === 'processed'"
          :disabled="store.submitting"
          @void="openVoid"
          @resolve="openResolve"
        />
        <div v-if="!store.loadingList && displayList.length === 0" class="exc-empty" data-test="exc-empty">
          {{ store.tab === 'pending' ? '本班次没有待处理异常单' : '本班次还没有处理记录' }}
        </div>
      </div>
    </div>

    <ResolveDialog
      v-model="resolveVisible"
      :order="activeOrder"
      :submitting="store.submitting"
      :create-scan="createScan"
      @submit-cash="onSubmitCash"
      @scan-paid="onScanPaid"
    />

    <VoidDialog
      v-model="voidVisible"
      :order="activeOrder"
      :submitting="store.submitting"
      @submit="onSubmitVoid"
    />
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled, WarningFilled } from '@element-plus/icons-vue'
import ExceptionCard from './ExceptionCard.vue'
import ResolveDialog from './ResolveDialog.vue'
import VoidDialog from './VoidDialog.vue'
import { useShiftHandoverStore } from '@/stores/shiftHandover'
import type { ExceptionOrder, ExceptionTab, ScanPayment } from '@/api/shiftExceptions'
import { resolveErrorMessage } from '@/utils/errorMessage'

const props = defineProps<{
  modelValue: boolean
  /** 后端异常单接口未上线时的降级数据（与顶部徽标同源） */
  fallbackPending?: ExceptionOrder[]
}>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void }>()

const store = useShiftHandoverStore()
const resolveVisible = ref(false)
const voidVisible = ref(false)
const activeOrder = ref<ExceptionOrder | null>(null)

/** 展示列表：优先后端，后端为空时用降级数据（仅待处理 Tab） */
const displayList = computed<ExceptionOrder[]>(() => {
  if (store.currentList.length > 0) return store.currentList
  if (store.tab === 'pending') return props.fallbackPending || []
  return []
})

/** 抽屉打开期间 15s 轮询一次，防多终端冲突 */
const POLL_MS = 15000
let pollTimer: ReturnType<typeof setInterval> | null = null

function startPolling(): void {
  stopPolling()
  pollTimer = setInterval(() => { void store.refreshAll() }, POLL_MS)
}

function stopPolling(): void {
  if (pollTimer !== null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

watch(() => props.modelValue, async (visible) => {
  if (visible) {
    await store.refreshAll()
    startPolling()
  } else {
    stopPolling()
  }
})

onUnmounted(() => stopPolling())

function close(): void {
  emit('update:modelValue', false)
}

function onTabChange(name: string | number): void {
  void store.loadExceptions(name as ExceptionTab)
}

/** 抽屉内联错误（操作点提示：作废/补收失败就地显示，不弹全局 toast） */
const actionError = ref('')

function setActionError(message: string): void {
  actionError.value = message
}

function clearActionError(): void {
  actionError.value = ''
}

function openVoid(order: ExceptionOrder): void {
  clearActionError()
  activeOrder.value = order
  voidVisible.value = true
}

function openResolve(order: ExceptionOrder): void {
  clearActionError()
  activeOrder.value = order
  resolveVisible.value = true
}

/** 统一处理 409：已被别人处理 → 提示 + 刷新，绝不本地改数 */
function handleConflict(error: unknown): boolean {
  const status = (error as { status?: number })?.status
  if (status === 409) {
    // 内联提示后端原文（例如「该订单已被其它终端处理，请刷新后重试」），再补一句「已为你刷新列表」
    setActionError(resolveErrorMessage(error, '该订单已被其他终端处理') + '，已为你刷新列表')
    void store.refreshAll()
    return true
  }
  return false
}

async function onSubmitVoid(payload: { reason: string; remark?: string }): Promise<void> {
  if (!activeOrder.value) return
  try {
    const replayed = await store.voidOrder(activeOrder.value.orderId, payload)
    voidVisible.value = false
    ElMessage.success(replayed ? '该单此前已作废（幂等重放），账目已刷新' : '已作废，账目已刷新')
  } catch (error) {
    if (!handleConflict(error)) setActionError('作废失败：' + resolveErrorMessage(error, '请重试'))
  }
}

async function onSubmitCash(payload: { amountFen: number; cashReceivedFen: number }): Promise<void> {
  if (!activeOrder.value) return
  try {
    // 这里收到的已经是**分**，直接透传给 store（store 不再做元→分换算）
    const replayed = await store.resolveCash(activeOrder.value.orderId, payload.amountFen, payload.cashReceivedFen)
    resolveVisible.value = false
    ElMessage.success(replayed ? '该单此前已补收款（幂等重放），账目已刷新' : '补收款成功，账目已刷新')
  } catch (error) {
    if (!handleConflict(error)) setActionError('补收款失败：' + resolveErrorMessage(error, '请重试'))
  }
}

async function createScan(orderId: string): Promise<ScanPayment | null> {
  try {
    return await store.createScanPayment(orderId)
  } catch (error) {
    if (handleConflict(error)) return null
    throw error
  }
}

async function onScanPaid(): Promise<void> {
  // 只有轮询到支付成功才刷新账目（此处之前没有任何乐观更新）
  await store.onScanPaid()
}
</script>

<style scoped>
.exc-drawer {
  display: flex;
  flex-direction: column;
  gap: 10px;
  height: 100%;
}

.exc-inline-error {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--pos-danger-soft, #fbf0f0);
  color: var(--pos-danger, #c94b4b);
  font-size: 13px;
  line-height: 1.45;
}

.exc-inline-error__close {
  margin-left: auto;
  border: 0;
  background: transparent;
  color: inherit;
  font-size: 16px;
  cursor: pointer;
}

.exc-tip {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  padding: 8px 12px;
  border-radius: var(--pos-radius-sm, 8px);
  background: var(--pos-warning-soft, #fbf5e8);
  color: var(--pos-warning-text, #8a6114);
  font-size: 12px;
  line-height: 1.6;
}

.exc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 120px;
}

.exc-empty {
  padding: 32px 0;
  text-align: center;
  font-size: 13px;
  color: var(--pos-text-muted, #94a3b8);
}
</style>
