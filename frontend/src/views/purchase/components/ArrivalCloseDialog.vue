<script setup lang="ts">
/**
 * 关闭到货单对话框
 */
import { ref, reactive, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import type { PurchaseArrivalCloseForm } from '@/types/purchase-arrival'

interface Props {
  modelValue: boolean
  arrivalId: string
  arrivalCode: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [data: PurchaseArrivalCloseForm]
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive<PurchaseArrivalCloseForm>({
  closeReason: '',
  remark: '',
})

const formRules: FormRules = {
  closeReason: [{ required: true, message: '请输入关闭原因', trigger: 'blur' }],
}

function resetForm() {
  formData.closeReason = ''
  formData.remark = ''
  formRef.value?.resetFields()
}

function handleClose() {
  emit('update:modelValue', false)
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    emit('submit', { ...formData })
  } finally {
    submitLoading.value = false
  }
}

watch(
  () => props.modelValue,
  (visible) => {
    if (!visible) resetForm()
  },
)
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="关闭到货单"
    width="480px"
    class="fts-dialog--sm"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="emit('update:modelValue', $event)"
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
      <el-form-item label="到货单号">
        <el-input :model-value="arrivalCode" disabled />
      </el-form-item>
      <el-form-item label="关闭原因" prop="closeReason">
        <el-input v-model="formData.closeReason" placeholder="请输入关闭原因" clearable />
      </el-form-item>
      <el-form-item label="备注">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="3"
          placeholder="请输入备注（可选）"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
/* 关闭对话框仅依赖 Element Plus 默认布局，无额外样式 */
</style>
