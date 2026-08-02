<script setup lang="ts">
/**
 * 员工人事变动对话框（调岗/调部门）
 * 从 HREmployee.vue 中提取，避免页面文件持续膨胀
 */
import { ref, reactive, watch, computed, nextTick } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { employeeApi } from '@/api/hr'
import { positionApi } from '@/api/hr/position'
import { useDepartmentOptions } from '@/composables/useDepartmentOptions'
import type { EmployeeTransferData } from '@/types/hr'

interface Employee {
  id: string
  name: string
  departmentId?: string
  department?: string
  positionId?: string
  position?: string
}

interface Props {
  modelValue: boolean
  employee: Employee | null
}

const props = withDefaults(defineProps<Props>(), {
  employee: () => null,
})

const emit = defineEmits<{
  'update:modelValue': [visible: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const positionOptions = ref<{ positionId: string; positionName: string }[]>([])

const { departmentOptions } = useDepartmentOptions(true)

const formData = reactive<EmployeeTransferData>({
  employeeId: '',
  oldDepartmentId: '',
  oldPositionId: '',
  newDepartmentId: '',
  newPositionId: '',
  transferDate: '',
  reason: '',
})

const dialogTitle = computed(() => props.employee ? `人事变动 - ${props.employee.name}` : '人事变动')

const formRules: FormRules = {
  newDepartmentId: [
    { required: true, message: '请选择新部门', trigger: 'change' },
  ],
  newPositionId: [
    { required: true, message: '请选择新岗位', trigger: 'change' },
  ],
  transferDate: [
    { required: true, message: '请选择变动日期', trigger: 'change' },
  ],
}

function resetForm() {
  Object.assign(formData, {
    employeeId: '',
    oldDepartmentId: '',
    oldPositionId: '',
    newDepartmentId: '',
    newPositionId: '',
    transferDate: '',
    reason: '',
  })
  positionOptions.value = []
}

async function loadPositionsByDepartment(departmentId?: string) {
  if (!departmentId) {
    positionOptions.value = []
    return
  }
  try {
    positionOptions.value = await positionApi.getByDepartment(departmentId)
  } catch {
    positionOptions.value = []
  }
}

watch(() => props.modelValue, (visible) => {
  if (visible && props.employee) {
    Object.assign(formData, {
      employeeId: props.employee.id,
      oldDepartmentId: props.employee.departmentId || '',
      oldPositionId: props.employee.positionId || '',
      newDepartmentId: props.employee.departmentId || '',
      newPositionId: props.employee.positionId || '',
      transferDate: new Date().toISOString().split('T')[0],
      reason: '',
    })
    loadPositionsByDepartment(props.employee.departmentId)
    nextTick(() => formRef.value?.clearValidate())
  } else if (!visible) {
    resetForm()
  }
})

watch(() => formData.newDepartmentId, (newVal, oldVal) => {
  if (newVal !== oldVal) {
    formData.newPositionId = ''
    loadPositionsByDepartment(newVal)
  }
})

async function handleSubmit() {
  if (!formRef.value || !props.employee) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (formData.newDepartmentId === formData.oldDepartmentId && formData.newPositionId === formData.oldPositionId) {
    ElMessage.warning('新部门或新岗位至少有一项需要与当前不同')
    return
  }

  submitLoading.value = true
  try {
    await employeeApi.transferEmployee(props.employee.id, formData)
    ElMessage.success('人事变动成功')
    emit('success')
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '人事变动失败')
  } finally {
    submitLoading.value = false
  }
}

function handleClose() {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="680px"
    class="fts-dialog--md"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="当前部门">
            <el-input :model-value="props.employee?.department || '-'" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="当前岗位">
            <el-input :model-value="props.employee?.position || '-'" disabled />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="新部门" prop="newDepartmentId">
            <el-select
              v-model="formData.newDepartmentId"
              placeholder="请选择新部门"
              :teleported="false"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="dept in departmentOptions"
                :key="dept.id"
                :label="dept.label"
                :value="dept.id"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="新岗位" prop="newPositionId">
            <el-select
              v-model="formData.newPositionId"
              placeholder="请选择新岗位"
              :teleported="false"
              filterable
              style="width: 100%"
            >
              <el-option
                v-for="pos in positionOptions"
                :key="pos.positionId"
                :label="pos.positionName"
                :value="pos.positionId"
              />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="变动日期" prop="transferDate">
        <el-date-picker
          v-model="formData.transferDate"
          type="date"
          placeholder="请选择变动日期"
          value-format="YYYY-MM-DD"
          :teleported="false"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="变动原因">
        <el-input
          v-model="formData.reason"
          type="textarea"
          :rows="3"
          placeholder="请输入变动原因"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          确认变动
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>
