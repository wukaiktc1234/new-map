<template>
  <el-dialog v-model="visible" title="成本管理" width="500px">
    <el-form :model="form" label-width="80px">
      <el-form-item label="菜品名称">
        <el-input :model-value="form.foodName" disabled />
      </el-form-item>
      <el-form-item label="成本价(元)">
        <el-input-number v-model="form.costPrice" :min="0" :precision="2" style="width: 100%" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="submitting">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const emit = defineEmits<{
  (e: 'submit', data: { id: string | number; costPrice: number }): void
}>()

const visible = ref(false)
const submitting = ref(false)
const form = ref({
  id: '' as string | number,
  foodName: '',
  costPrice: undefined as number | undefined,
})

function open(row: Record<string, unknown>): void {
  form.value = {
    id: (row.id as string | number) || '',
    foodName: row.foodName as string || '',
    costPrice: row.costPrice as number || undefined,
  }
  visible.value = true
}

async function handleSave(): Promise<void> {
  if (form.value.costPrice === undefined || form.value.costPrice === null) {
    ElMessage.warning('请输入成本价')
    return
  }
  submitting.value = true
  try {
    emit('submit', { id: form.value.id, costPrice: form.value.costPrice })
    visible.value = false
  } finally {
    submitting.value = false
  }
}

defineExpose({ open })
</script>
