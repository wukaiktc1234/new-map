<script setup lang="ts">
/**
 * 收货确认单创建对话框
 * 从到货单打开，录入本次确认/拒收数量、批次、效期及质检信息
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { receiptConfirmationApi } from '@/api/receipt-confirmation'
import type {
  PurchaseArrivalInfo,
  PurchaseArrivalItem,
  ReceiptConfirmationFormData,
  ReceiptConfirmationItemForm,
  ReceiverType,
} from '@/types/purchase-arrival'
import { QualityCheckResultOptions } from '@/types/purchase-arrival'

interface Props {
  modelValue: boolean
  arrival: PurchaseArrivalInfo | null
  receiverType: ReceiverType
}

const props = withDefaults(defineProps<Props>(), {
  arrival: null,
  receiverType: 'STORE',
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formData = ref<ReceiptConfirmationFormData>({
  arrivalId: '',
  receiverType: props.receiverType,
  items: [],
  qualityCheckResult: undefined,
  qualityRemark: '',
  remark: '',
})

const submitLoading = ref(false)

function buildInitialItem(arrivalItem: PurchaseArrivalItem): ReceiptConfirmationItemForm {
  const expected = arrivalItem.expectedQuantity ?? 0
  const received = arrivalItem.receivedQuantity ?? 0
  return {
    arrivalItemId: arrivalItem.arrivalItemId,
    materialId: arrivalItem.materialId,
    materialName: arrivalItem.materialName,
    specification: arrivalItem.specification,
    unit: arrivalItem.unit,
    expectedQuantity: expected,
    receivedQuantity: received,
    confirmedQuantity: Math.max(0, Number((expected - received).toFixed(2))),
    rejectedQuantity: 0,
    unitPrice: arrivalItem.unitPrice ?? 0,
    batchNo: undefined,
    productionDate: undefined,
    expiryDate: undefined,
    remark: undefined,
  }
}

function resetForm(): void {
  const arrival = props.arrival
  if (!arrival || !arrival.items || arrival.items.length === 0) {
    ElMessage.error('到货单无明细数据，无法进行收货确认')
    emit('update:modelValue', false)
    return
  }
  formData.value = {
    arrivalId: arrival.arrivalId ?? '',
    receiverType: props.receiverType,
    storeId: arrival.storeId,
    warehouseId: arrival.warehouseId,
    items: arrival.items.map(buildInitialItem),
    qualityCheckResult: undefined,
    qualityRemark: '',
    remark: '',
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) resetForm()
  },
)

const totalConfirmed = computed(() => formData.value.items.reduce((sum, item) => sum + (item.confirmedQuantity ?? 0), 0))
const totalRejected = computed(() => formData.value.items.reduce((sum, item) => sum + (item.rejectedQuantity ?? 0), 0))
const totalAmount = computed(() =>
  formData.value.items.reduce((sum, item) => sum + (item.confirmedQuantity ?? 0) * (item.unitPrice ?? 0), 0),
)

function validateItems(): boolean {
  for (const item of formData.value.items) {
    const confirmed = item.confirmedQuantity ?? 0
    const rejected = item.rejectedQuantity ?? 0
    const expected = item.expectedQuantity ?? 0
    if (confirmed < 0 || rejected < 0) {
      ElMessage.warning(`${item.materialName} 数量不能为负数`)
      return false
    }
    if (confirmed + rejected > expected) {
      ElMessage.warning(`${item.materialName} 确认+拒收数量不能超过预计数量`)
      return false
    }
  }
  return true
}

async function handleSubmit(): Promise<void> {
  if (!formData.value.arrivalId) return
  if (!formData.value.items || formData.value.items.length === 0) {
    ElMessage.warning('收货明细不能为空')
    return
  }
  if (!validateItems()) return
  if (!formData.value.qualityCheckResult) {
    ElMessage.warning('请选择质检结果')
    return
  }
  submitLoading.value = true
  try {
    await receiptConfirmationApi.create(formData.value)
    ElMessage.success('收货确认成功')
    emit('success')
    emit('update:modelValue', false)
  } catch {
    ElMessage.error('收货确认失败')
  } finally {
    submitLoading.value = false
  }
}

function handleCancel(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="确认收货"
    width="900px"
    destroy-on-close
    @update:model-value="emit('update:modelValue', $event)"
  >
    <template v-if="arrival">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="到货单号" :span="1">
          {{ arrival.arrivalCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="采购订单号" :span="1">
          {{ arrival.orderCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="供应商" :span="1">
          {{ arrival.supplierName || '-' }}
        </el-descriptions-item>
      </el-descriptions>

      <div class="items-section">
        <div class="items-section__title">到货明细确认</div>
        <el-table :data="formData.items" border size="small" class="items-table">
          <el-table-column label="物料名称/规格/单位" min-width="180">
            <template #default="{ row }">
              <div class="material-cell">
                <span class="material-cell__name">{{ row.materialName }}</span>
                <span v-if="row.specification" class="material-cell__spec">{{ row.specification }}</span>
                <span v-if="row.unit" class="material-cell__unit">单位：{{ row.unit }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="expectedQuantity" label="预计数量" min-width="100" align="right" />
          <el-table-column prop="receivedQuantity" label="已收数量" min-width="100" align="right" />
          <el-table-column label="本次确认" min-width="120" align="right">
            <template #default="{ row }">
              <el-input-number v-model="row.confirmedQuantity" :min="0" :precision="2" :controls="false" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="本次拒收" min-width="120" align="right">
            <template #default="{ row }">
              <el-input-number v-model="row.rejectedQuantity" :min="0" :precision="2" :controls="false" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="批次号" min-width="120">
            <template #default="{ row }">
              <el-input v-model="row.batchNo" placeholder="批次号" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="生产日期" min-width="150">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.productionDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                size="small"
                :teleported="false"
                style="width: 130px"
              />
            </template>
          </el-table-column>
          <el-table-column label="有效期至" min-width="150">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.expiryDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                size="small"
                :teleported="false"
                style="width: 130px"
              />
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" size="small" />
            </template>
          </el-table-column>
        </el-table>

        <div class="items-summary">
          <span>本次确认数量：<b>{{ totalConfirmed }}</b></span>
          <span>本次拒收数量：<b>{{ totalRejected }}</b></span>
          <span>确认金额：<b>{{ totalAmount.toFixed(2) }}</b></span>
        </div>
      </div>

      <el-form :model="formData" label-width="80px" class="confirmation-form">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="质检结果" required>
              <el-select v-model="formData.qualityCheckResult" placeholder="请选择" :teleported="false" clearable>
                <el-option
                  v-for="opt in QualityCheckResultOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="16">
            <el-form-item label="质检备注">
              <el-input v-model="formData.qualityRemark" placeholder="请输入质检备注" maxlength="200" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="2"
            placeholder="请输入备注"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </template>

    <template #footer>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确认收货</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.items-section {
  margin-top: var(--fts-space-5);

  &__title {
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

.material-cell {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &__name {
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-medium);
  }

  &__spec,
  &__unit {
    color: var(--fts-text-secondary);
    font-size: var(--fts-font-size-sm);
  }
}

.items-summary {
  margin-top: var(--fts-space-3);
  display: flex;
  gap: var(--fts-space-6);
  justify-content: flex-end;
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);

  b {
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-semibold);
  }
}

.confirmation-form {
  margin-top: var(--fts-space-5);
}
</style>
