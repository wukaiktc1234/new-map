<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="960px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
    >
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="上级分类" v-if="isChildMode">
            <el-input :value="parentCategoryName" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="分类名称" prop="categoryName">
            <el-input
              v-model="form.categoryName"
              placeholder="请输入分类名称"
              maxlength="50"
              show-word-limit
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="排序">
            <el-input-number
              v-model="form.sortOrder"
              :min="0"
              :max="9999"
              controls-position="right"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="状态" prop="status">
            <el-radio-group v-model="form.status">
              <el-radio-button :value="1">启用</el-radio-button>
              <el-radio-button :value="0">停用</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { categoryApi } from '@/api/product/category'
import type { FoodCategory, CategoryFormData } from '@/types/product'

const props = defineProps<{
  categoryTree: FoodCategory[]
}>()

const emit = defineEmits<{
  (e: 'submit', data: CategoryFormData): void
}>()

const visible = ref(false)
const formRef = ref<FormInstance>()
const submitting = ref(false)
const isEditMode = ref(false)
const isChildMode = ref(false)
const parentCategoryName = ref('')

const form = ref<CategoryFormData>({
  categoryName: '',
  status: 1,
  sortOrder: 0,
})

const rules: FormRules = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
}

const dialogTitle = computed(() => {
  if (isEditMode.value) return '编辑分类'
  if (isChildMode.value) return '新增子分类'
  return '新增分类'
})

function openCreate(): void {
  resetForm()
  isEditMode.value = false
  isChildMode.value = false
  visible.value = true
}

function openCreateChild(parent: FoodCategory): void {
  resetForm()
  isEditMode.value = false
  isChildMode.value = true
  parentCategoryName.value = parent.categoryName
  form.value.parentId = parent.categoryId
  visible.value = true
}

function openEdit(row: FoodCategory): void {
  resetForm()
  isEditMode.value = true
  isChildMode.value = false
  form.value = {
    categoryId: row.categoryId,
    categoryName: row.categoryName,
    parentId: row.parentId || undefined,
    // FoodCategory 类型字段为 categoryStatus（'active'/'inactive' 字符串），需转换为数字
    status: row.categoryStatus === 'active' ? 1 : 0,
    sortOrder: row.sortOrder || 0,
  }
  visible.value = true
}

function close(): void {
  visible.value = false
}

function resetForm(): void {
  form.value = {
    categoryName: '',
    status: 1,
    sortOrder: 0,
  }
  isEditMode.value = false
  isChildMode.value = false
  parentCategoryName.value = ''
  submitting.value = false
  formRef.value?.resetFields()
}

function handleClose(): void {
  resetForm()
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查表单填写是否正确')
    return
  }

  // 设置 loading 状态，由父组件在 API 完成后调用 close() 或 setLoading(false) 重置
  submitting.value = true
  emit('submit', { ...form.value })
}

/**
 * 设置 loading 状态（供父组件在 API 失败时调用，重置按钮 loading）
 */
function setLoading(loading: boolean): void {
  submitting.value = loading
}

defineExpose({ openCreate, openCreateChild, openEdit, close, setLoading })
</script>
