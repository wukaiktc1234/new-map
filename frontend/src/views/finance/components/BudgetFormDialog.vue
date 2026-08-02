<script setup lang="ts">
/**
 * 预算新增/编辑对话框
 * - 新增：不传 budgetId
 * - 编辑：传入 budgetId，自动加载详情
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { budgetApi, fenToYuanNumber, yuanToFen } from '@/api/finance'
import type { FinanceBudgetFormData, BudgetType } from '@/types/finance'

const props = defineProps<{
  modelValue: boolean
  budgetId?: string | null
}>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  success: []
}>()

const formRef = ref<FormInstance>()
const submitting = ref(false)

/** 表单数据（金额单位：元） */
const form = ref<FinanceBudgetFormData>({
  budgetName: '',
  budgetType: 'operating',
  year: new Date().getFullYear(),
  month: undefined,
  department: '',
  budgetAmount: 0,
  remark: '',
})

/** 预算类型选项 */
const budgetTypeOptions: Array<{ label: string; value: BudgetType }> = [
  { label: '运营预算', value: 'operating' },
  { label: '采购预算', value: 'procurement' },
  { label: '人力预算', value: 'hr' },
  { label: '营销预算', value: 'marketing' },
  { label: '资本预算', value: 'capital' },
]

/** 年份选项（当前年份 ± 2年） */
const yearOptions = (() => {
  const currentYear = new Date().getFullYear()
  return [currentYear - 1, currentYear, currentYear + 1, currentYear + 2]
})()

/** 月份选项（1-12，0表示年度预算） */
const monthOptions = [
  { label: '年度预算', value: undefined },
  ...Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 })),
]

/** 表单校验规则 */
const rules: FormRules = {
  budgetName: [{ required: true, message: '请输入预算名称', trigger: 'blur' }],
  budgetType: [{ required: true, message: '请选择预算类型', trigger: 'change' }],
  year: [{ required: true, message: '请选择预算年度', trigger: 'change' }],
  budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'blur' }],
}

/** 监听对话框打开 */
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return
    if (props.budgetId) {
      // 编辑模式：加载详情
      try {
        const data = await budgetApi.getById(props.budgetId)
        form.value = {
          budgetName: data.budgetName,
          budgetType: data.budgetType,
          year: data.year,
          month: data.month,
          department: data.department || '',
          budgetAmount: fenToYuanNumber(data.budgetAmount),
          remark: data.remark || '',
        }
      } catch {
        ElMessage.error('加载预算详情失败')
      }
    } else {
      // 新增模式：重置表单
      form.value = {
        budgetName: '',
        budgetType: 'operating',
        year: new Date().getFullYear(),
        month: undefined,
        department: '',
        budgetAmount: 0,
        remark: '',
      }
    }
    formRef.value?.clearValidate()
  }
)

/** 提交表单 */
async function handleSubmit(): Promise<void> {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      // 金额元转分
      const submitData: FinanceBudgetFormData = {
        ...form.value,
        budgetAmount: yuanToFen(form.value.budgetAmount),
      }
      if (props.budgetId) {
        await budgetApi.update(props.budgetId, submitData)
        ElMessage.success('预算更新成功')
      } else {
        await budgetApi.create(submitData)
        ElMessage.success('预算创建成功')
      }
      emit('success')
      emit('update:modelValue', false)
    } catch {
      ElMessage.error(props.budgetId ? '更新失败，请重试' : '创建失败，请重试')
    } finally {
      submitting.value = false
    }
  })
}

/** 关闭对话框 */
function handleClose(): void {
  emit('update:modelValue', false)
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="budgetId ? '编辑预算' : '新增预算'"
    width="600px"
    @update:model-value="handleClose"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:500px">
      <el-form-item label="预算名称" prop="budgetName">
        <el-input v-model="form.budgetName" placeholder="如：2026年度运营预算" maxlength="100" />
      </el-form-item>
      <el-form-item label="预算类型" prop="budgetType">
        <el-select v-model="form.budgetType" placeholder="请选择" :teleported="false" style="width:100%">
          <el-option v-for="opt in budgetTypeOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算年度" prop="year">
        <el-select v-model="form.year" placeholder="请选择" :teleported="false" style="width:100%">
          <el-option v-for="y in yearOptions" :key="y" :label="y + '年'" :value="y" />
        </el-select>
      </el-form-item>
      <el-form-item label="预算月份">
        <el-select v-model="form.month" placeholder="年度预算" :teleported="false" style="width:100%">
          <el-option v-for="opt in monthOptions" :key="opt.label" :label="opt.label" :value="opt.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门">
        <el-input v-model="form.department" placeholder="选填，如：运营部" maxlength="50" />
      </el-form-item>
      <el-form-item label="预算金额" prop="budgetAmount">
        <el-input-number v-model="form.budgetAmount" :min="0" :precision="2" :step="1000" style="width:100%" />
        <span style="margin-left:8px;color:var(--fts-text-secondary);font-size:13px">元</span>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="选填" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
