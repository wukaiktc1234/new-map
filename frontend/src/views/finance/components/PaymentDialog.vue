<script setup lang="ts">
/**
 * 付款登记对话框
 *
 * 功能：
 * - 显示应付账款基本信息（供应商、应付编号、未付金额）
 * - 录入付款信息（付款金额、付款方式、付款账户、付款日期、备注）
 * - 提交后调用 paymentApi.register 触发四账联动：
 *   应付账款 + 银行账户 + 资金流水 + 会计凭证
 *
 * 金额单位说明：
 * - 显示/输入：元（浮点数）
 * - API传输：分（整数）
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { paymentApi, bankAccountApi, yuanToFen } from '@/api/finance'
import { supplierApi } from '@/api/purchase/supplier'
import type {
  FinancePayable,
  FinanceBankAccount,
  PaymentFormData,
  PaymentMethod,
} from '@/types/finance'
import type { SupplierInfo, SettlementMethod } from '@/types/purchase-supplier'

interface Props {
  /** 对话框可见性 */
  visible: boolean
  /** 当前应付账款 */
  payable: FinancePayable | null
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

/** 付款方式选项 */
const paymentMethodOptions: Array<{ label: string; value: PaymentMethod }> = [
  { label: '银行转账', value: 'bank_transfer' },
  { label: '现金', value: 'cash' },
  { label: '支票', value: 'check' },
]

/** 表单数据（金额单位：元） */
const formData = ref({
  payAmount: 0,
  paymentMethod: 'bank_transfer' as PaymentMethod,
  bankAccountId: '',
  paymentDate: new Date().toISOString().slice(0, 10),
  remark: '',
})

/** 银行账户列表 */
const bankAccounts = ref<FinanceBankAccount[]>([])
/** 提交中状态 */
const submitting = ref(false)
/** 账户列表加载中 */
const accountLoading = ref(false)
/** 初始付款方式（根据供应商结算方式预填充） */
const initialPaymentMethod = ref<PaymentMethod>('bank_transfer')

/** 未付金额（元，props.payable.remainAmount 已被 PayableDataConverter.toFrontend 转为元） */
const remainYuan = computed(() => props.payable?.remainAmount ?? 0)

/** 应付金额（元，已由 PayableDataConverter.toFrontend 转为元） */
const amountYuan = computed(() => props.payable?.amount ?? 0)

/** 是否需要选择付款账户（银行转账/支票需要，现金不需要） */
const needBankAccount = computed(() => formData.value.paymentMethod !== 'cash')

/** 空值显示为 '-'，用于只读信息区 */
function displayValue(value: string | number | undefined | null): string {
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

/** 表单引用 */
const formRef = ref()

/** 表单校验规则 */
const rules = computed(() => ({
  payAmount: [
    { required: true, message: '请输入付款金额', trigger: 'blur' },
    {
      validator: (_rule: unknown, value: number, callback: (err?: Error) => void) => {
        if (!value || value <= 0) {
          callback(new Error('付款金额必须大于0'))
        } else if (value > remainYuan.value) {
          callback(new Error('付款金额不能超过未付金额'))
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
          callback(new Error('请选择付款账户'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
  paymentDate: [{ required: true, message: '请选择付款日期', trigger: 'change' }],
}))

/** 账号脱敏：保留后4位，前缀用 **** 替代 */
function maskAccountNo(accountNo: string): string {
  if (!accountNo) return ''
  if (accountNo.length <= 4) return accountNo
  return '****' + accountNo.slice(-4)
}

/** 余额格式化（带千分位，account.balance 已被 BankAccountDataConverter 转为元） */
function formatBalance(yuan: number | undefined | null): string {
  if (yuan === undefined || yuan === null || isNaN(yuan)) return '0.00'
  return yuan.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
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

/** 根据供应商结算方式映射为付款方式 */
function mapSettlementToPaymentMethod(
  settlementMethod: SettlementMethod | string | undefined
): PaymentMethod {
  switch (settlementMethod) {
    case 'immediate':
      return 'cash'
    case 'check':
      return 'check'
    case 'monthly':
    case 'prepaid':
    case 'bank_transfer':
    default:
      return 'bank_transfer'
  }
}

/** 加载供应商详情以预填充付款方式 */
async function loadSupplierInfo(): Promise<void> {
  if (!props.payable?.supplierId) {
    initialPaymentMethod.value = 'bank_transfer'
    return
  }
  try {
    const supplier: SupplierInfo | null = await supplierApi.getById(props.payable.supplierId)
    initialPaymentMethod.value = mapSettlementToPaymentMethod(supplier?.settlementMethod)
  } catch {
    initialPaymentMethod.value = 'bank_transfer'
  }
}

/** 重置表单 */
function resetForm(): void {
  formData.value = {
    payAmount: remainYuan.value,
    paymentMethod: initialPaymentMethod.value,
    bankAccountId: '',
    paymentDate: new Date().toISOString().slice(0, 10),
    remark: '',
  }
  formRef.value?.clearValidate?.()
}

/** 提交付款 */
async function handleSubmit(): Promise<void> {
  if (!props.payable) {
    ElMessage.warning('应付账款信息缺失')
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
    const submitData: PaymentFormData = {
      payableId: props.payable.id,
      paymentAmount: yuanToFen(formData.value.payAmount),
      paymentMethod: formData.value.paymentMethod,
      bankAccountId: formData.value.bankAccountId,
      paymentDate: formData.value.paymentDate,
      remark: formData.value.remark,
    }
    await paymentApi.register(submitData)
    ElMessage.success('付款成功，四账联动已完成')
    dialogVisible.value = false
    emit('success')
  } catch {
    ElMessage.error('付款失败，请重试')
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
  async (visible) => {
    if (visible && props.payable) {
      initialPaymentMethod.value = 'bank_transfer'
      await loadSupplierInfo()
      loadBankAccounts()
      resetForm()
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="付款登记"
    width="560px"
    :close-on-click-modal="false"
    @closed="handleClosed"
  >
    <!-- 应付账款只读信息区：集中展示上游应付关键信息，便于付款前核对 -->
    <div class="payable-info-section">
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="应付编号">
          {{ displayValue(props.payable?.payableNo) }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商">
          {{ displayValue(props.payable?.supplierName) }}
        </el-descriptions-item>
        <el-descriptions-item label="采购订单号">
          {{ displayValue(props.payable?.orderNo) }}
        </el-descriptions-item>
        <el-descriptions-item label="入库单号">
          {{ displayValue(props.payable?.stockinNo) }}
        </el-descriptions-item>
        <el-descriptions-item label="发票号">
          {{ displayValue(props.payable?.invoiceNo) }}
        </el-descriptions-item>
        <el-descriptions-item label="到期日">
          {{ displayValue(props.payable?.dueDate) }}
        </el-descriptions-item>
        <el-descriptions-item label="账龄">
          {{ displayValue(props.payable?.aging) }}
        </el-descriptions-item>
        <el-descriptions-item label="应付金额">
          <span class="readonly-amount">¥{{ amountYuan.toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="未付金额">
          <span class="readonly-amount">¥{{ remainYuan.toFixed(2) }}</span>
        </el-descriptions-item>
      </el-descriptions>
    </div>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
    >
      <!-- 付款金额 -->
      <el-form-item label="付款金额" prop="payAmount">
        <el-input-number
          v-model="formData.payAmount"
          :precision="2"
          :step="0.01"
          :min="0"
          :max="remainYuan"
          :controls="false"
          style="width: 200px"
          placeholder="请输入付款金额"
        />
        <span class="unit-suffix">元</span>
      </el-form-item>

      <!-- 付款方式 -->
      <el-form-item label="付款方式" prop="paymentMethod">
        <el-select
          v-model="formData.paymentMethod"
          placeholder="请选择付款方式"
          :teleported="false"
          style="width: 200px"
        >
          <el-option
            v-for="opt in paymentMethodOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <!-- 付款账户（银行转账/支票时必选） -->
      <el-form-item
        v-if="needBankAccount"
        label="付款账户"
        prop="bankAccountId"
      >
        <el-select
          v-model="formData.bankAccountId"
          placeholder="请选择付款账户"
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

      <!-- 付款日期 -->
      <el-form-item label="付款日期" prop="paymentDate">
        <el-date-picker
          v-model="formData.paymentDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择付款日期"
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
          placeholder="付款备注（可选）"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        确认付款
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.payable-info-section {
  margin-bottom: var(--fts-space-4);
}

.readonly-text {
  color: var(--fts-text-primary);
  font-size: var(--fts-font-size-base);
}

.readonly-amount {
  color: var(--fts-error);
  font-size: var(--fts-font-size-base);
  font-weight: var(--fts-font-weight-semibold, 600);
}

.unit-suffix {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}
</style>
