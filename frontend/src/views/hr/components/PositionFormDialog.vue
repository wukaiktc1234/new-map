<script setup lang="ts">
/**
 * 岗位表单对话框（新增/编辑）
 * 从 HRPosition.vue 中提取，减少页面文件体积
 */
import { ref, reactive, watch, computed, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { positionApi } from '@/api/hr/position'
import type { PositionItem, PositionFormData } from '@/api/hr/position'
import type { DepartmentDTO } from '@/types/hr'

interface Props {
  modelValue: boolean
  isEdit: boolean
  initialData?: Partial<PositionFormData & { positionId?: string }>
  departmentTree: DepartmentDTO[]
}

const props = withDefaults(defineProps<Props>(), {
  initialData: () => ({}),
  departmentTree: () => [],
})

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  success: []
  close: []
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const parentPositionOptions = ref<PositionItem[]>([])

/** 对话框显隐（使用 v-model 绑定，确保 Element Plus popup-manager 正确管理 z-index） */
const visible = computed({
  get: () => props.modelValue,
  set: (val: boolean) => emit('update:modelValue', val),
})

const formData = reactive<PositionFormData & { parentPositionId?: string; minSalary?: number; maxSalary?: number; requirements?: string }>({
  positionCode: '',
  positionName: '',
  departmentId: '',
  maxCount: 1,
  description: '',
  status: 'active',
  parentPositionId: '',
  minSalary: 0,
  maxSalary: 0,
  requirements: '',
})

const formRules: FormRules = {
  positionName: [
    { required: true, message: '请输入岗位名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  positionCode: [
    { required: true, message: '请输入岗位编码', trigger: 'blur' },
  ],
  departmentId: [
    { required: true, message: '请选择所属部门', trigger: 'change' },
  ],
  maxCount: [
    { required: true, message: '请输入编制人数', trigger: 'blur' },
  ],
}

const dialogTitle = computed(() => props.isEdit ? '编辑岗位' : '新增岗位')

function resetForm() {
  Object.assign(formData, {
    positionCode: '',
    positionName: '',
    departmentId: '',
    maxCount: 1,
    description: '',
    status: 'active',
    parentPositionId: '',
    minSalary: 0,
    maxSalary: 0,
    requirements: '',
  })
  parentPositionOptions.value = []
}

function fillForm(data: Partial<PositionFormData & { positionId?: string }>) {
  Object.assign(formData, {
    positionCode: data.positionCode ?? '',
    positionName: data.positionName ?? '',
    departmentId: data.departmentId ?? '',
    maxCount: data.maxCount ?? 1,
    description: data.description ?? '',
    status: data.status ?? 'active',
    parentPositionId: '',
    minSalary: 0,
    maxSalary: 0,
    requirements: '',
  })
}

async function loadParentPositions(departmentId?: string) {
  if (!departmentId) {
    parentPositionOptions.value = []
    return
  }
  try {
    parentPositionOptions.value = await positionApi.getByDepartment(departmentId)
  } catch {
    parentPositionOptions.value = []
  }
}

watch(() => props.modelValue, (visible) => {
  if (visible) {
    if (props.isEdit && props.initialData) {
      fillForm(props.initialData)
      loadParentPositions(props.initialData.departmentId)
    } else {
      resetForm()
    }
    nextTick(() => formRef.value?.clearValidate())
  }
})

watch(() => formData.departmentId, (newVal) => {
  formData.parentPositionId = ''
  loadParentPositions(newVal)
})

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const submitData: PositionFormData = {
      positionCode: formData.positionCode,
      positionName: formData.positionName,
      departmentId: formData.departmentId,
      maxCount: formData.maxCount,
      description: formData.description,
      status: formData.status,
    }

    if (props.isEdit && props.initialData?.positionId) {
      await positionApi.update(props.initialData.positionId, submitData)
      ElMessage.success('更新成功')
    } else {
      await positionApi.create(submitData)
      ElMessage.success('创建成功')
    }
    emit('success')
    emit('update:modelValue', false)
  } catch {
    ElMessage.error(props.isEdit ? '更新失败' : '创建失败')
  } finally {
    submitLoading.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
  emit('close')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="960px"
    class="fts-dialog--lg"
    :close-on-click-modal="false"
    :lock-scroll="false"
    destroy-on-close
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="岗位名称" prop="positionName">
            <el-input v-model="formData.positionName" placeholder="请输入岗位名称" maxlength="50" show-word-limit />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="岗位编码" prop="positionCode">
            <el-input v-model="formData.positionCode" placeholder="请输入岗位编码" />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="所属部门" prop="departmentId">
            <el-select
              v-model="formData.departmentId"
              placeholder="请选择所属部门"
              :teleported="false"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="dept in departmentTree.flatMap(d => [d, ...(d.children || [])])"
                :key="dept.id"
                :label="dept.name"
                :value="dept.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="上级岗位">
            <el-select
              v-model="formData.parentPositionId"
              placeholder="请选择上级岗位"
              :teleported="false"
              filterable
              clearable
              style="width: 100%"
            >
              <el-option
                v-for="pos in parentPositionOptions"
                :key="pos.positionId"
                :label="pos.positionName"
                :value="pos.positionId"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="编制人数" prop="maxCount">
            <el-input-number v-model="formData.maxCount" :min="1" :max="999" controls-position="right" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio value="active">启用</el-radio>
              <el-radio value="inactive">停用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="24">
          <el-form-item label="薪资范围">
            <div class="salary-range">
              <el-input-number v-model="formData.minSalary" :min="0" controls-position="right" style="width: 45%" />
              <span class="range-sep">-</span>
              <el-input-number v-model="formData.maxSalary" :min="0" controls-position="right" style="width: 45%" />
            </div>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="岗位描述">
        <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="请输入岗位描述" maxlength="500" show-word-limit />
      </el-form-item>

      <el-form-item label="任职要求">
        <el-input v-model="formData.requirements" type="textarea" :rows="3" placeholder="请输入任职要求" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.salary-range {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  .range-sep {
    color: var(--fts-text-tertiary);
  }
}
</style>
