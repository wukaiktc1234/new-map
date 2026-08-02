<script setup lang="ts">
/**
 * 凭证录入/编辑对话框
 *
 * 功能：
 * - 凭证头部信息录入（日期、类型、附件张数、摘要）
 * - 多行分录明细（科目、摘要、借方、贷方）
 * - 实时借贷平衡校验
 * - 保存草稿（创建/更新凭证）
 *
 * 金额单位说明：
 * - 前端输入/显示：元（浮点数）
 * - API传输：分（整数）
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { voucherApi, subjectApi, fenToYuanNumber, yuanToFen } from '@/api/finance'
import type { FinanceSubject, VoucherType, VoucherFormData } from '@/types/finance'

interface Props {
  /** 对话框可见性（v-model） */
  modelValue: boolean
  /** 凭证ID（编辑模式传入，新增模式传空） */
  voucherId?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  voucherId: null,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

/** 对话框可见性双向绑定 */
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

/** 凭证类型选项 */
const voucherTypeOptions: Array<{ label: string; value: VoucherType }> = [
  { label: '收款凭证', value: 'receipt' },
  { label: '付款凭证', value: 'payment' },
  { label: '转账凭证', value: 'transfer' },
  { label: '通用凭证', value: 'general' },
]

/** 分录行结构（金额单位：元） */
interface VoucherEntryRow {
  subjectId: string
  subjectCode: string
  subjectName: string
  debitAmount: number
  creditAmount: number
  summary: string
}

/** 表单数据 */
const formData = ref({
  voucherType: 'general' as VoucherType,
  voucherDate: new Date().toISOString().slice(0, 10),
  summary: '',
  attachmentCount: 1,
  entries: [] as VoucherEntryRow[],
  remark: '',
})

/** 叶子科目列表 */
const subjectOptions = ref<FinanceSubject[]>([])
/** 提交中状态 */
const submitting = ref(false)
/** 加载科目列表中 */
const subjectLoading = ref(false)

/** 借方合计（元） */
const debitTotal = computed(() =>
  formData.value.entries.reduce((sum, e) => sum + (Number(e.debitAmount) || 0), 0)
)

/** 贷方合计（元） */
const creditTotal = computed(() =>
  formData.value.entries.reduce((sum, e) => sum + (Number(e.creditAmount) || 0), 0)
)

/** 借贷差额（元） */
const balanceDiff = computed(() => debitTotal.value - creditTotal.value)

/** 借贷是否平衡 */
const isBalanced = computed(() => {
  // 借贷合计必须相等且都大于0
  return debitTotal.value > 0 && creditTotal.value > 0 && Math.abs(balanceDiff.value) < 0.005
})

/** 是否编辑模式 */
const isEditMode = computed(() => !!props.voucherId)

/** 对话框标题 */
const dialogTitle = computed(() => isEditMode.value ? '编辑凭证' : '新增凭证')

/** 格式化金额显示（带千分位） */
function formatDisplayAmount(val: number): string {
  return (val || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 创建空分录行 */
function createEmptyEntry(): VoucherEntryRow {
  return {
    subjectId: '',
    subjectCode: '',
    subjectName: '',
    debitAmount: 0,
    creditAmount: 0,
    summary: '',
  }
}

/** 添加分录行 */
function handleAddEntry(): void {
  formData.value.entries.push(createEmptyEntry())
}

/** 删除分录行 */
function handleRemoveEntry(index: number): void {
  if (formData.value.entries.length <= 1) {
    ElMessage.warning('至少保留一行分录')
    return
  }
  formData.value.entries.splice(index, 1)
}

/** 科目选择变化时同步编码和名称 */
function handleSubjectChange(row: VoucherEntryRow): void {
  const subject = subjectOptions.value.find(s => s.id === row.subjectId)
  if (subject) {
    row.subjectCode = subject.subjectCode
    row.subjectName = subject.subjectName
  } else {
    row.subjectCode = ''
    row.subjectName = ''
  }
}

/** 借方金额输入时清空贷方 */
function handleDebitInput(row: VoucherEntryRow): void {
  if (row.debitAmount > 0) {
    row.creditAmount = 0
  }
}

/** 贷方金额输入时清空借方 */
function handleCreditInput(row: VoucherEntryRow): void {
  if (row.creditAmount > 0) {
    row.debitAmount = 0
  }
}

/** 加载叶子科目列表 */
async function loadSubjects(): Promise<void> {
  subjectLoading.value = true
  try {
    const list = await subjectApi.getLeafSubjects()
    subjectOptions.value = list || []
  } catch {
    subjectOptions.value = []
  } finally {
    subjectLoading.value = false
  }
}

/** 加载凭证详情（编辑模式） */
async function loadVoucherDetail(id: string): Promise<void> {
  try {
    const detail = await voucherApi.getById(id)
    if (detail) {
      formData.value.voucherType = detail.voucherType
      formData.value.voucherDate = detail.voucherDate
      formData.value.summary = detail.summary
      formData.value.attachmentCount = detail.attachmentCount ?? 1
      formData.value.remark = detail.remark ?? ''
      formData.value.entries = (detail.entries || []).map(e => ({
        subjectId: e.subjectId,
        subjectCode: e.subjectCode,
        subjectName: e.subjectName,
        // 分 → 元
        debitAmount: fenToYuanNumber(e.debitAmount),
        creditAmount: fenToYuanNumber(e.creditAmount),
        summary: e.summary,
      }))
    }
  } catch {
    ElMessage.error('加载凭证详情失败')
  }
}

/** 重置表单 */
function resetForm(): void {
  formData.value = {
    voucherType: 'general',
    voucherDate: new Date().toISOString().slice(0, 10),
    summary: '',
    attachmentCount: 1,
    entries: [createEmptyEntry(), createEmptyEntry()],
    remark: '',
  }
}

/** 保存草稿 */
async function handleSave(): Promise<void> {
  // 校验：至少有一行有效分录（科目不为空）
  const validEntries = formData.value.entries.filter(e => e.subjectId)
  if (validEntries.length < 2) {
    ElMessage.warning('请至少填写两行有效分录（借贷各一行）')
    return
  }

  // 校验借贷平衡
  if (!isBalanced.value) {
    ElMessage.warning('借贷不平衡，请检查金额')
    return
  }

  // 校验每行分录：借方或贷方必须有值且不能同时有值
  for (const entry of validEntries) {
    if (entry.debitAmount > 0 && entry.creditAmount > 0) {
      ElMessage.warning('每行分录借方和贷方不能同时有金额')
      return
    }
    if (entry.debitAmount <= 0 && entry.creditAmount <= 0) {
      ElMessage.warning('每行分录借方或贷方必须有金额')
      return
    }
  }

  submitting.value = true
  try {
    // 构建表单数据（金额元 → 分）
    const submitData: VoucherFormData = {
      voucherType: formData.value.voucherType,
      voucherDate: formData.value.voucherDate,
      summary: formData.value.summary,
      attachmentCount: formData.value.attachmentCount,
      entries: validEntries.map(e => ({
        subjectId: e.subjectId,
        subjectCode: e.subjectCode,
        subjectName: e.subjectName,
        debitAmount: yuanToFen(e.debitAmount),
        creditAmount: yuanToFen(e.creditAmount),
        summary: e.summary,
      })),
      remark: formData.value.remark,
    }

    if (isEditMode.value && props.voucherId) {
      submitData.id = props.voucherId
      await voucherApi.update(props.voucherId, submitData)
      ElMessage.success('凭证更新成功')
    } else {
      await voucherApi.create(submitData)
      ElMessage.success('凭证创建成功')
    }

    dialogVisible.value = false
    emit('success')
  } catch {
    ElMessage.error(isEditMode.value ? '凭证更新失败，请重试' : '凭证创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 对话框关闭时重置表单 */
function handleClosed(): void {
  resetForm()
}

// 监听对话框打开，加载数据
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      loadSubjects()
      if (props.voucherId) {
        loadVoucherDetail(props.voucherId)
      } else {
        resetForm()
      }
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="900px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <!-- 头部信息 -->
    <div class="voucher-form-header">
      <el-form label-width="90px" label-position="right">
        <div class="header-grid">
          <el-form-item label="凭证日期">
            <el-date-picker
              v-model="formData.voucherDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              :teleported="false"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="凭证类型">
            <el-select
              v-model="formData.voucherType"
              placeholder="选择类型"
              :teleported="false"
              style="width: 100%"
            >
              <el-option
                v-for="opt in voucherTypeOptions"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="附件张数">
            <el-input-number
              v-model="formData.attachmentCount"
              :min="0"
              :max="999"
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="摘要" class="header-summary">
            <el-input
              v-model="formData.summary"
              placeholder="请输入凭证摘要"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </div>
      </el-form>
    </div>

    <!-- 分录明细表格 -->
    <div class="voucher-entries-section">
      <div class="entries-header">
        <span class="entries-title">分录明细</span>
        <el-button type="primary" link size="small" @click="handleAddEntry">+ 添加分录行</el-button>
      </div>
      <el-table :data="formData.entries" border style="width: 100%" size="small">
        <el-table-column label="科目" min-width="220">
          <template #default="{ row }">
            <el-select
              v-model="row.subjectId"
              placeholder="选择科目"
              filterable
              :teleported="false"
              :loading="subjectLoading"
              style="width: 100%"
              @change="handleSubjectChange(row)"
            >
              <el-option
                v-for="s in subjectOptions"
                :key="s.id"
                :label="`${s.subjectCode} ${s.subjectName}`"
                :value="s.id"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="摘要" min-width="160">
          <template #default="{ row }">
            <el-input v-model="row.summary" placeholder="分录摘要" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="借方金额(元)" min-width="140" align="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.debitAmount"
              :precision="2"
              :step="0.01"
              :min="0"
              :controls="false"
              size="small"
              style="width: 100%"
              @input="handleDebitInput(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="贷方金额(元)" min-width="140" align="right">
          <template #default="{ row }">
            <el-input-number
              v-model="row.creditAmount"
              :precision="2"
              :step="0.01"
              :min="0"
              :controls="false"
              size="small"
              style="width: 100%"
              @input="handleCreditInput(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="handleRemoveEntry($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 合计行 -->
      <div class="entries-total">
        <div class="total-item">
          <span class="total-label">借方合计：</span>
          <span class="total-value">¥{{ formatDisplayAmount(debitTotal) }}</span>
        </div>
        <div class="total-item">
          <span class="total-label">贷方合计：</span>
          <span class="total-value">¥{{ formatDisplayAmount(creditTotal) }}</span>
        </div>
        <div class="total-item" :class="{ 'total-unbalanced': !isBalanced }">
          <span class="total-label">差额：</span>
          <span class="total-value">¥{{ formatDisplayAmount(balanceDiff) }}</span>
        </div>
      </div>

      <!-- 借贷不平衡提示 -->
      <div v-if="!isBalanced && (debitTotal > 0 || creditTotal > 0)" class="balance-warning">
        借贷不平衡，请调整金额使借方合计等于贷方合计
      </div>
    </div>

    <!-- 备注 -->
    <el-form label-width="90px" label-position="right" style="margin-top: 16px;">
      <el-form-item label="备注">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="凭证备注（可选）"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" :disabled="!isBalanced" @click="handleSave">
        保存草稿
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.voucher-form-header {
  margin-bottom: var(--fts-space-4);

  .header-grid {
    display: grid;
    grid-template-columns: 1fr 1fr 1fr;
    gap: 0 var(--fts-space-4);

    .header-summary {
      grid-column: 1 / -1;
    }
  }
}

.voucher-entries-section {
  .entries-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);

    .entries-title {
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-text-primary);
      font-size: var(--fts-font-size-base);
    }
  }
}

.entries-total {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--fts-space-6);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: var(--fts-bg-secondary);
  border-radius: var(--fts-radius-sm);
  margin-top: var(--fts-space-3);

  .total-item {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);

    .total-label {
      color: var(--fts-text-secondary);
      font-size: var(--fts-font-size-sm);
    }

    .total-value {
      color: var(--fts-text-primary);
      font-weight: var(--fts-font-weight-semibold);
      font-size: var(--fts-font-size-base);
    }

    &.total-unbalanced .total-value {
      color: var(--fts-error);
    }
  }
}

.balance-warning {
  margin-top: var(--fts-space-2);
  padding: var(--fts-space-2) var(--fts-space-3);
  background: var(--fts-error-bg, rgba(245, 108, 108, 0.1));
  color: var(--fts-error);
  font-size: var(--fts-font-size-sm);
  border-radius: var(--fts-radius-sm);
  text-align: center;
}
</style>
