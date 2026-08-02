<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="480px" :close-on-click-modal="false" destroy-on-close @close="resetForm">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-form-item label="菜品名称">
        <el-input :model-value="form.name" disabled />
      </el-form-item>

      <el-form-item label="当前售价(元)">
        <el-input-number :model-value="form.currentPrice" disabled style="width: 100%" />
      </el-form-item>

      <el-form-item label="新售价(元)" prop="newPrice">
        <el-input-number
          v-model="form.newPrice"
          :min="0"
          :precision="2"
          :step="1"
          controls-position="right"
          style="width: 100%"
        />
        <div v-if="priceDiff !== null" class="price-diff" :class="{ 'is-increase': priceDiff > 0, 'is-decrease': priceDiff < 0 }">
          {{ priceDiff > 0 ? '+' : '' }}{{ priceDiff.toFixed(2) }} 元 ({{ priceDiff > 0 ? '涨价' : priceDiff < 0 ? '降价' : '不变' }})
        </div>
      </el-form-item>

      <el-form-item label="调价原因">
        <el-input
          v-model="form.remark"
          type="textarea"
          placeholder="请输入调价原因（可选）"
          :rows="2"
          maxlength="200"
          show-word-limit
          resize="none"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定调价</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import type { PricingRecord } from '@/types/product'

const emit = defineEmits<{
  (e: 'submit', data: Record<string, unknown>): void
  (e: 'batch-submit', data: Record<string, unknown>): void
}>()

const visible = ref(false)
const mode = ref<'single' | 'batch'>('single')
const formRef = ref<FormInstance>()
const submitting = ref(false)
const batchRows = ref<PricingRecord[]>([])

const form = ref({
  id: null as number | null,
  name: '',
  currentPrice: 0,
  newPrice: undefined as number | undefined,
  remark: '',
})

const rules: FormRules = {
  newPrice: [{ required: true, message: '请输入新售价', trigger: 'blur' }],
}

const dialogTitle = computed(() => mode.value === 'batch' ? '批量调价' : '调整价格')

/** 价格差异 */
const priceDiff = computed(() => {
  if (form.value.newPrice == null) return null
  return Number((form.value.newPrice - form.value.currentPrice).toFixed(2))
})

function openSingle(row: PricingRecord): void {
  mode.value = 'single'
  batchRows.value = []
  form.value = {
    id: Number(row.foodId),
    name: row.foodName || '',
    currentPrice: Number(row.salePrice) || 0,
    newPrice: undefined,
    remark: '',
  }
  visible.value = true
}

function openBatch(rows: PricingRecord[]): void {
  mode.value = 'batch'
  batchRows.value = rows
  form.value = {
    id: null,
    name: `已选择 ${rows.length} 个菜品`,
    currentPrice: 0,
    newPrice: undefined,
    remark: '',
  }
  visible.value = true
}

function close(): void {
  visible.value = false
}

function resetForm(): void {
  form.value = {
    id: null,
    name: '',
    currentPrice: 0,
    newPrice: undefined,
    remark: '',
  }
  batchRows.value = []
  mode.value = 'single'
  submitting.value = false
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查表单填写是否正确')
    return
  }

  if (mode.value === 'single' && form.value.newPrice === form.value.currentPrice) {
    ElMessage.warning('新售价与当前售价相同，无需调价')
    return
  }

  submitting.value = true
  try {
    if (mode.value === 'batch') {
      emit('batch-submit', { ...form.value, batchIds: batchRows.value.map(r => r.foodId) })
    } else {
      emit('submit', { ...form.value })
    }
  } finally {
    submitting.value = false
  }
}

defineExpose({ openSingle, openBatch, close })
</script>

<style scoped lang="scss">
.price-diff {
  margin-top: 4px;
  font-size: 12px;

  &.is-increase {
    color: var(--fts-error);
  }

  &.is-decrease {
    color: var(--fts-success);
  }
}
</style>
