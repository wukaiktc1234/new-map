<script setup lang="ts">
/**
 * 收款登记对话框
 *
 * 功能：
 * - 显示应收账款基本信息（客户、应收编号、未收金额）
 * - 录入收款信息（收款金额、收款方式、收款账户、收款日期、备注）
 * - 提交后调用 receiptApi.register 触发四账联动：
 *   应收账款 + 银行账户 + 资金流水 + 会计凭证
 *
 * 金额单位说明：
 * - 显示/输入：元（浮点数）
 * - API传输：分（整数）
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { receiptApi, bankAccountApi, yuanToFen } from '@/api/finance'
import type {
  FinanceReceivable,
  FinanceBankAccount,
  ReceiptFormData,
  ReceiptMethod,
} from '@/types/finance'

interface Props {
  /** 对话框可见性 */
  visible: boolean
  /** 当前应收账款 */
  receivable: FinanceReceivable | null
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'success': []
}>()

/** 对话框可见性双向绑定 */
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
})

/** 收款方式选项 */
const receiptMethodOptions: Array<{ label: string; value: ReceiptMethod }> = [
  { label: '银行转账', value: 'bank_transfer' },
  { label: '现金', value: 'cash' },
  { label: '支票', value: 'check' },
]

/** 表单数据（金额单位：元） */
const formData = ref({
  receiveAmount: 0,
  receiptMethod: 'bank_transfer' as ReceiptMethod,
  bankAccountId: '',
  receiptDate: new Date().toISOString().slice(0, 10),
  remark: '',
})

/** 银行账户列表 */
const bankAccounts = ref<FinanceBankAccount[]>([])
/** 提交中状态 */
const submitting = ref(false)
/** 账户列表加载中 */
const accountLoading = ref(false)

/** 未收金额（元，remainAmount 已被 ReceivableDataConverter.toFrontend 转为元） */
const remainYuan = computed(() => props.receivable?.remainAmount ?? 0)

/** 是否需要选择收款账户（银行转账/支票需要，现金不需要） */
const needBankAccount = computed(() => formData.value.receiptMethod !== 'cash')

/** 表单引用 */
const formRef = ref()

/** 表单校验规则 */
const rules = computed(() => ({
  receiveAmount: [
    { required: true, message: '请输入收款金额', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (err?: Error) => void) => {
        if (!value || value <= 0) {
          callback(new Error('收款金额必须大于0'))
        } else if (value > remainYuan.value) {
          callback(new Error('收款金额不能超过未收金额'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  bankAccountId: [
    {
      validator: (_rule: unknown, value: string, callback: (err?: Error) => void) => {
        if (needBankAccount.value && !value) {
          callback(new Error('请选择收款账户'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  receiptDate: [{ required: true, message: '请选择收款日期', trigger: 'change' }],
}))

/** 账号脱敏：保留后4位，前缀用 **** 替代 */
function maskAccountNo(accountNo: string): string {
  if (!accountNo) return ''
  if (accountNo.length <= 4) return accountNo
  return '****' + accountNo.slice(-4)
}

/** 余额格式化（带千分位，balance 已被 BankAccountDataConverter.toFrontend 转为元） */
function formatBalance(yuan: number | undefined | null): string {
  if (yuan === undefined || yuan === null || isNaN(yuan)) return '0.00'
  return (yuan ?? 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 银行账户选项标签 */
function accountLabel(account: FinanceBankAccount): string {
  return `${account.accountName} - ${maskAccountNo(account.accountNo)} (余额: ¥${formatBalance(account.balance)})`
}

/** 加载启用的银行账户列表 */
async function loadBankAccounts(): Promise<void> {
  accountLoading.value = true
  try {
    const res = await bankAccountApi.getList({ page: 1, size: 100 })
    bankAccounts.value = (res?.records || []).filter(a => a.status === 'active')
  } catch {
    bankAccounts.value = []
  } finally {
    accountLoading.value = false
  }
}

/** 重置表单 */
function resetForm(): void {
  formData.value = {
    receiveAmount: remainYuan.value,
    receiptMethod: 'bank_transfer',
    bankAccountId: '',
    receiptDate: new Date().toISOString().slice(0, 10),
    remark: '',
  }
  formRef.value?.clearValidate?.()
}

/** 提交收款 */
async function handleSubmit(): Promise<void> {
  if (!props.receivable) {
    ElMessage.warning('应收账款信息缺失')
    return
  }

  try {
    await formRef.value?.validate?.()
  } catch {
    return
  }

  submitting.value = true
  try {
    // 构建提交数据（金额元 → 分）
    const submitData: ReceiptFormData = {
      receivableId: props.receivable.id,
      receiptAmount: yuanToFen(formData.value.receiveAmount),
      receiptMethod: formData.value.receiptMethod,
      bankAccountId: formData.value.bankAccountId,
      receiptDate: formData.value.receiptDate,
      remark: formData.value.remark,
    }
    await receiptApi.register(submitData)
    ElMessage.success('收款成功，四账联动已完成')
    dialogVisible.value = false
    emit('success')
  } catch {
    ElMessage.error('收款失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 对话框关闭时重置表单 */
function handleClosed(): void {
  resetForm()
}

// 监听对话框打开：加载数据并初始化表单
watch(
  () => props.visible,
  (visible) => {
    if (visible && props.receivable) {
      loadBankAccounts()
      resetForm()
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="收款登记"
    width="560px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <!-- 只读信息区 -->
      <el-form-item label="客户名称">
        <span class="readonly-text">{{ props.receivable?.customerName || '-' }}</span>
      </el-form-item>
      <el-form-item label="应收编号">
        <span class="readonly-text">{{ props.receivable?.receivableNo || '-' }}</span>
      </el-form-item>
      <el-form-item label="未收金额">
        <span class="readonly-amount">¥{{ remainYuan.toFixed(2) }}</span>
      </el-form-item>

      <!-- 收款金额 -->
      <el-form-item label="收款金额" prop="receiveAmount">
        <el-input-number
          v-model="formData.receiveAmount"
          :precision="2"
          :step="0.01"
          :min="0"
          :max="remainYuan"
          :controls="false"
          style="width: 200px"
          placeholder="请输入收款金额"
        />
        <span class="unit-suffix">元</span>
      </el-form-item>

      <!-- 收款方式 -->
      <el-form-item label="收款方式" prop="receiptMethod">
        <el-select
          v-model="formData.receiptMethod"
          placeholder="请选择收款方式"
          :teleported="false"
          style="width: 200px"
        >
          <el-option
            v-for="opt in receiptMethodOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <!-- 收款账户（银行转账/支票时必选） -->
      <el-form-item
        v-if="needBankAccount"
        label="收款账户"
        prop="bankAccountId"
      >
        <el-select
          v-model="formData.bankAccountId"
          placeholder="请选择收款账户"
          filterable
          :teleported="false"
          :loading="accountLoading"
          style="width: 360px"
        >
          <el-option
            v-for="acc in bankAccounts"
            :key="acc.id"
            :label="accountLabel(acc)"
            :value="acc.id"
          />
        </el-select>
      </el-form-item>

      <!-- 收款日期 -->
      <el-form-item label="收款日期" prop="receiptDate">
        <el-date-picker
          v-model="formData.receiptDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择收款日期"
          :teleported="false"
          style="width: 200px"
        />
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="收款备注（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认收款
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.readonly-text {
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
}

.readonly-amount {
  color: var(--fts-error);
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold, 600);
}

.unit-suffix {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}
</style>
