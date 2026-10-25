<script setup lang="ts">
/**
 * 同额歧义处置弹窗（P4-2 / BL-6b，按 lead 统一契约实现）
 *
 * 接口：POST /recon/statements/{statementId}/items/{itemId}/resolve-ambiguity
 * body：{ decision, targetRef, reason } —— 三个字段**都必填**（后端 reason @NotBlank）。
 *
 * 规则（与后端 :544-599 一致）：
 * - decision 必须是**对侧类别**：A↔C、B↔D；前端只给对侧一项可选，当前类别置灰禁用；
 * - targetRef 必须取自「同类别(=decision) + 同金额 + 非本条」的候选：对侧 C/D 取 transactionId，
 *   对侧 A/B 取 flowId；**候选容器内不允许任何自由文本输入**（纯点击选择，无 input[type=text]）；
 * - reason ≥5 字；
 * - 提交后弹二次确认，文案严格为「将 X 类改为 Y 类，涉及金额 N 分，确认？」（N 为分值纯数字）。
 *
 * 逐笔处置：每次只处理一个 item，无批量入口。
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ReconciliationItemVO, ReconciliationStatementVO } from '@/api/finance-recon-statement'
import { formatFen } from '@/types/finance-recon'

interface Props {
  modelValue: boolean
  statement: ReconciliationStatementVO | null
  item: ReconciliationItemVO | null
  submitting?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  submitting: false,
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', payload: { decision: string; targetRef: string; reason: string }): void
}>()

const CATEGORY_LABELS: Record<string, string> = {
  A: '企业已收、银行未收',
  B: '企业已付、银行未付',
  C: '银行已收、企业未收',
  D: '银行已付、企业未付',
}

/** 对侧类别（与后端 switch 完全一致） */
const OPPOSITE: Record<string, string> = { A: 'C', B: 'D', C: 'A', D: 'B' }

/** 处置方式：RESOLVE（改类别，默认）/ EXEMPT（排除，P4-3 新增） */
type AmbiguityMode = 'RESOLVE' | 'EXEMPT'
const mode = ref<AmbiguityMode>('RESOLVE')

const decision = ref('')
const targetRef = ref('')
const reason = ref('')
const confirming = ref(false)

const isExempt = computed<boolean>(() => mode.value === 'EXEMPT')

const currentCategory = computed<string>(() => String(props.item?.category || '').toUpperCase())
const oppositeCategory = computed<string>(() => OPPOSITE[currentCategory.value] || '')
const currentAmount = computed<number | null>(() => props.item?.amount ?? null)

/** decision 下拉：只出现对侧合法值 + 当前类别（禁用） */
const decisionOptions = computed(() => {
  const list: Array<{ value: string; label: string; disabled: boolean }> = []
  if (oppositeCategory.value) {
    list.push({
      value: oppositeCategory.value,
      label: oppositeCategory.value + ' 类（对侧：' + (CATEGORY_LABELS[oppositeCategory.value] || '') + '）',
      disabled: false,
    })
  }
  if (currentCategory.value) {
    list.push({
      value: currentCategory.value,
      label: currentCategory.value + ' 类（当前类别，不可选）',
      disabled: true,
    })
  }
  return list
})

/** 候选：同金额 + 对侧类别 + 非本条 + 有有效引用 */
interface Candidate {
  ref: string
  refLabel: string
  amount: number
  category: string
  ownerText: string
  itemId: number
}

function refOf(item: ReconciliationItemVO): string | null {
  if (oppositeCategory.value === 'C' || oppositeCategory.value === 'D') {
    return item.transactionId != null ? String(item.transactionId) : null
  }
  return item.flowId != null ? String(item.flowId) : null
}

const candidates = computed<Candidate[]>(() => {
  const stmt = props.statement
  const cur = props.item
  if (!stmt || !cur) return []
  const prefix = oppositeCategory.value === 'C' || oppositeCategory.value === 'D' ? 'txn#' : 'flow#'
  const list: Candidate[] = []
  for (const item of stmt.items ?? []) {
    if (item.itemId === cur.itemId) continue
    if (String(item.category || '').toUpperCase() !== oppositeCategory.value) continue
    if (item.amount !== cur.amount) continue
    const ref = refOf(item)
    if (!ref) continue
    list.push({
      ref,
      refLabel: prefix + ref,
      amount: item.amount ?? 0,
      category: String(item.category || '').toUpperCase(),
      ownerText: item.ownerName || (item.ownerUserId != null ? '#' + item.ownerUserId : '-'),
      itemId: item.itemId,
    })
  }
  return list
})

const reasonValid = computed<boolean>(() => reason.value.trim().length >= 5)

/** RESOLVE 需要 decision + targetRef + reason；EXEMPT 只需要 reason（≥5 字） */
const canSubmit = computed<boolean>(() => {
  if (props.submitting) return false
  if (isExempt.value) return reasonValid.value
  return Boolean(oppositeCategory.value) && Boolean(targetRef.value) && reasonValid.value
})

/** 原因字段的 label / placeholder（EXEMPT 下改成排除理由口径） */
const reasonLabel = computed<string>(() => (isExempt.value ? '请说明为什么判定此条非歧义' : '处置原因'))
const reasonPlaceholder = computed<string>(() =>
  isExempt.value ? '请说明为什么判定此条非歧义（≥5 字，写入审计记录）' : '请填写处置原因（至少 5 个字）',
)

/** 提交按钮文案 */
const submitText = computed<string>(() => (isExempt.value ? '标记排除' : '确认处置'))

/** 二次确认文案：RESOLVE 严格为「将 X 类改为 Y 类，涉及金额 N 分，确认？」；EXEMPT 为固定文案 */
const confirmText = computed<string>(() => {
  if (isExempt.value) {
    return '确认将此条歧义标记为排除？此操作会写审计记录，且不可撤销'
  }
  const amount = currentAmount.value ?? 0
  return '将 ' + currentCategory.value + ' 类改为 ' + oppositeCategory.value + ' 类，涉及金额 ' + amount + ' 分，确认？'
})

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) {
      confirming.value = false
      return
    }
    // 默认仍是「改类别」（EXEMPT 不是默认）
    mode.value = 'RESOLVE'
    decision.value = oppositeCategory.value || ''
    targetRef.value = candidates.value.length === 1 ? candidates.value[0].ref : ''
    reason.value = ''
    confirming.value = false
  },
  { immediate: true },
)

// 候选异步到达时补默认选中
watch(candidates, (list) => {
  if (!targetRef.value && list.length === 1) targetRef.value = list[0].ref
})

function close(): void {
  confirming.value = false
  emit('update:modelValue', false)
}

function pick(ref: string, category: string): void {
  if (category !== oppositeCategory.value) return
  targetRef.value = ref
}

/** 第一步：校验 + 打开二次确认 */
function openConfirm(): void {
  if (!reasonValid.value) {
    ElMessage.warning(isExempt.value ? '请说明为什么判定此条非歧义（≥5 字）' : '处置原因至少 5 个字')
    return
  }
  if (!isExempt.value) {
    if (!decision.value || !oppositeCategory.value) {
      ElMessage.warning('处置方向必须为对侧类别')
      return
    }
    if (!targetRef.value) {
      ElMessage.warning('请选择对侧同额候选')
      return
    }
  }
  confirming.value = true
}

/**
 * 第二步：确认后真正提交。
 * EXEMPT 的请求体只发 { decision:'EXEMPT', reason }（targetRef 传空串，由上层/API 层剔除该键）。
 */
function doSubmit(): void {
  if (!canSubmit.value) return
  confirming.value = false
  if (isExempt.value) {
    emit('submit', { decision: 'EXEMPT', targetRef: '', reason: reason.value.trim() })
    return
  }
  emit('submit', { decision: oppositeCategory.value, targetRef: targetRef.value, reason: reason.value.trim() })
}
</script>

<template>
  <!-- 主弹窗 -->
  <el-dialog
    :model-value="modelValue"
    title="同额歧义处置"
    width="580px"
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div class="ambiguity" data-test="ambiguity-dialog">
      <!-- 当前歧义条目 -->
      <div
        class="ambiguity__item"
        data-test="ambiguity-item"
        :data-amount-fen="currentAmount ?? ''"
        :data-category="currentCategory"
      >
        <div class="ambiguity__row">
          <span class="k">当前类别</span>
          <span class="v">
            <strong>{{ currentCategory || '-' }} 类</strong>
            <span class="dim">{{ CATEGORY_LABELS[currentCategory] || '' }}</span>
          </span>
        </div>
        <div class="ambiguity__row">
          <span class="k">金额</span>
          <span class="v num">{{ currentAmount ?? '-' }} 分（{{ currentAmount != null ? formatFen(currentAmount) : '-' }} 元）</span>
        </div>
        <div class="ambiguity__row">
          <span class="k">挂账人</span>
          <span class="v">{{ item?.ownerName || (item?.ownerUserId != null ? '#' + item.ownerUserId : '-') }}</span>
        </div>
        <div class="ambiguity__row">
          <span class="k">原因原文</span>
          <span class="v reason">{{ item?.reason || '-' }}</span>
        </div>
      </div>

      <!-- P4-3：处置方式二选一（默认「改类别」，EXEMPT 不是默认） -->
      <el-radio-group v-model="mode" data-test="ambiguity-mode" class="ambiguity__mode">
        <el-radio value="RESOLVE">改类别（对侧同额）</el-radio>
        <el-radio value="EXEMPT">排除（判定为非歧义）</el-radio>
      </el-radio-group>

      <el-form label-width="98px" class="ambiguity__form">
        <el-form-item v-if="!isExempt" label="处置方向">
          <el-select v-model="decision" data-test="ambiguity-decision" style="width: 100%">
            <el-option
              v-for="opt in decisionOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
              :disabled="opt.disabled"
            />
          </el-select>
        </el-form-item>

        <el-form-item v-if="!isExempt" label="对侧同额候选">
          <!-- 候选容器：纯点击选择，内部没有任何自由文本输入（EXEMPT 模式下整块不渲染） -->
          <div class="ambiguity__targets" data-test="ambiguity-target">
            <div
              v-for="c in candidates"
              :key="c.ref"
              class="ambiguity__option"
              :class="{ 'is-active': targetRef === c.ref }"
              data-test="ambiguity-option"
              :data-amount-fen="c.amount"
              :data-category="c.category"
              :data-ref="c.ref"
              role="radio"
              :aria-checked="targetRef === c.ref"
              tabindex="0"
              @click="pick(c.ref, c.category)"
              @keydown.enter.prevent="pick(c.ref, c.category)"
              @keydown.space.prevent="pick(c.ref, c.category)"
            >
              <span class="dot" />
              <span class="mono">{{ c.refLabel }}</span>
              <span class="num">{{ c.amount }} 分</span>
              <span class="dim">{{ c.ownerText }}</span>
            </div>
            <div v-if="candidates.length === 0" class="ambiguity__empty">
              本表内没有同金额的对侧候选，无法处置
            </div>
          </div>
        </el-form-item>

        <el-form-item required>
          <template #label>
            <span data-test="ambiguity-reason-label">{{ reasonLabel }}</span>
          </template>
          <el-input
            v-model="reason"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            :placeholder="reasonPlaceholder"
            data-test="ambiguity-reason"
          />
        </el-form-item>
      </el-form>

      <p class="ambiguity__tip">逐笔处置：每次仅处理当前这一条歧义。</p>
    </div>

    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" data-test="ambiguity-submit" :disabled="!canSubmit" @click="openConfirm">
        {{ submitText }}
      </el-button>
    </template>
  </el-dialog>

  <!-- 二次确认 -->
  <el-dialog
    :model-value="confirming"
    title="确认处置"
    width="440px"
    append-to-body
    :close-on-click-modal="false"
    @update:model-value="(v: boolean) => (confirming = v)"
  >
    <div class="ambiguity__confirm" data-test="ambiguity-confirm">{{ confirmText }}</div>
    <template #footer>
      <el-button @click="confirming = false">取消</el-button>
      <el-button type="warning" data-test="ambiguity-confirm-ok" :loading="submitting" @click="doSubmit">
        确认
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.ambiguity__mode {
  margin-bottom: 12px;
}

.ambiguity__item {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--el-color-warning-light-9, #fdf6ec);
  border: 1px solid var(--el-color-warning-light-5, #f5dab1);
}

.ambiguity__row {
  display: flex;
  gap: 10px;
  font-size: 13px;
  line-height: 20px;
}

.ambiguity__row .k {
  flex: 0 0 70px;
  color: var(--fts-text-secondary, #909399);
}

.ambiguity__row .v {
  flex: 1 1 auto;
  min-width: 0;
  word-break: break-all;
  color: var(--fts-text-primary, #303133);
}

.dim {
  margin-left: 6px;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.num {
  font-variant-numeric: tabular-nums;
}

.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, monospace;
}

.reason {
  color: var(--fts-text-primary, #303133);
}

.ambiguity__targets {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.ambiguity__option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 10px;
  border: 1px solid var(--fts-border-secondary, #dcdfe6);
  border-radius: 8px;
  font-size: 13px;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}

.ambiguity__option:hover {
  border-color: var(--el-color-primary-light-5, #a0cfff);
}

.ambiguity__option:focus-visible {
  outline: 2px solid var(--el-color-primary, #409eff);
  outline-offset: 1px;
}

.ambiguity__option.is-active {
  border-color: var(--el-color-primary, #409eff);
  background: var(--el-color-primary-light-9, #ecf5ff);
}

.ambiguity__option .dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 1px solid var(--el-color-primary-light-5, #a0cfff);
  flex: 0 0 auto;
}

.ambiguity__option.is-active .dot {
  background: var(--el-color-primary, #409eff);
  border-color: var(--el-color-primary, #409eff);
}

.ambiguity__empty {
  padding: 10px 0;
  font-size: 12px;
  color: var(--fts-text-secondary, #909399);
}

.ambiguity__tip {
  margin: 0;
  font-size: 12px;
  color: var(--el-color-warning-dark-2, #b88230);
}

.ambiguity__confirm {
  font-size: 14px;
  line-height: 22px;
  color: var(--fts-text-primary, #303133);
}
</style>
