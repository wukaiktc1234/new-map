<script setup lang="ts">
/**
 * 成本记录新增/编辑对话框
 *
 * 功能：
 * - 新增成本记录（costId 为空）
 * - 编辑成本记录（costId 不为空时加载详情）
 * - 金额单位：前端元（浮点数）↔ API分（整数）
 *
 * 数据来源：/v1/costs
 */
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { costApi, fenToYuanNumber, yuanToFen } from '@/api/finance'
import type { FinanceCostFormData, FinanceCostType } from '@/types/finance'

interface Props {
  /** 对话框可见性（v-model） */
  modelValue: boolean
  /** 成本记录ID（编辑模式传入，新增模式传空） */
  costId?: string | null
}

const props = withDefaults(defineProps<Props>(), {
  costId: null,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'success': []
}>()

/** 对话框可见性双向绑定 */
const dialogVisible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val),
})

/** 成本类型选项 */
const costTypeOptions: Array<{ label: string; value: FinanceCostType }> = [
  { label: '食材成本', value: 'material' },
  { label: '人工成本', value: 'labor' },
  { label: '租金成本', value: 'rent' },
  { label: '能耗成本', value: 'energy' },
  { label: '营销成本', value: 'marketing' },
  { label: '其他', value: 'other' },
]

/** 表单数据（金额单位：元） */
const formData = ref<FinanceCostFormData>({
  costDate: new Date().toISOString().slice(0, 10),
  costType: 'material',
  categoryName: '',
  amount: 0,
  budgetAmount: undefined,
  description: '',
  department: '',
  remark: '',
})

/** 表单引用 */
const formRef = ref<FormInstance>()
/** 提交中状态 */
const submitting = ref(false)
/** 详情加载中 */
const loading = ref(false)

/** 是否编辑模式 */
const isEditMode = computed(() => !!props.costId)

/** 对话框标题 */
const dialogTitle = computed(() => isEditMode.value ? '编辑成本' : '新增成本')

/** 表单校验规则 */
const rules = computed<FormRules>(() => ({
  costDate: [{ required: true, message: '请选择成本日期', trigger: 'change' }],
  costType: [{ required: true, message: '请选择成本类型', trigger: 'change' }],
  categoryName: [{ required: true, message: '请输入成本类别名称', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入实际金额', trigger: 'blur' }],
  description: [{ required: true, message: '请输入说明', trigger: 'blur' }],
}))

/** 重置表单为默认值 */
function resetForm(): void {
  formData.value = {
    costDate: new Date().toISOString().slice(0, 10),
    costType: 'material',
    categoryName: '',
    amount: 0,
    budgetAmount: undefined,
    description: '',
    department: '',
    remark: '',
  }
  formRef.value?.clearValidate?.()
}

/** 加载成本记录详情（编辑模式） */
async function loadCostDetail(id: string): Promise<void> {
  loading.value = true
  try {
    const detail = await costApi.getById(id)
    if (detail) {
      formData.value = {
        costDate: detail.costDate,
        costType: detail.costType,
        categoryName: detail.categoryName,
        // 分 → 元
        amount: fenToYuanNumber(detail.amount),
        budgetAmount: detail.budgetAmount !== undefined ? fenToYuanNumber(detail.budgetAmount) : undefined,
        description: detail.description,
        department: detail.department ?? '',
        remark: detail.remark ?? '',
      }
    }
  } catch {
    ElMessage.error('加载成本记录失败')
  } finally {
    loading.value = false
  }
}

/** 提交表单 */
async function handleSubmit(): Promise<void> {
  try {
    await formRef.value?.validate?.()
  } catch {
    return
  }

  submitting.value = true
  try {
    // 构建提交数据（金额元 → 分）
    const submitData: FinanceCostFormData = {
      costDate: formData.value.costDate,
      costType: formData.value.costType,
      categoryName: formData.value.categoryName,
      amount: yuanToFen(formData.value.amount),
      budgetAmount: formData.value.budgetAmount !== undefined && formData.value.budgetAmount !== null
        ? yuanToFen(formData.value.budgetAmount)
        : undefined,
      description: formData.value.description,
      department: formData.value.department || undefined,
      remark: formData.value.remark || undefined,
    }

    if (isEditMode.value && props.costId) {
      await costApi.update(props.costId, submitData)
      ElMessage.success('成本记录更新成功')
    } else {
      await costApi.create(submitData)
      ElMessage.success('成本记录创建成功')
    }

    dialogVisible.value = false
    emit('success')
  } catch {
    ElMessage.error(isEditMode.value ? '成本记录更新失败，请重试' : '成本记录创建失败，请重试')
  } finally {
    submitting.value = false
  }
}

/** 对话框关闭时重置表单 */
function handleClosed(): void {
  resetForm()
}

// 监听对话框打开，加载数据
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      if (props.costId) {
        loadCostDetail(props.costId)
      } else {
        resetForm()
      }
    }
  }
)
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    :title="dialogTitle"
    width="600px"
    :close-on-click-modal="false"
    v-loading="loading"
    @closed="handleClosed"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="right"
      class="cost-form"
    >
      <!-- 成本日期 -->
      <el-form-item label="成本日期" prop="costDate">
        <el-date-picker
          v-model="formData.costDate"
          type="date"
          value-format="YYYY-MM-DD"
          placeholder="选择成本日期"
          :teleported="false"
          style="width: 100%"
        />
      </el-form-item>

      <!-- 成本类型 -->
      <el-form-item label="成本类型" prop="costType">
        <el-select
          v-model="formData.costType"
          placeholder="请选择成本类型"
          :teleported="false"
          style="width: 100%"
        >
          <el-option
            v-for="opt in costTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <!-- 成本类别名称 -->
      <el-form-item label="成本类别" prop="categoryName">
        <el-input
          v-model="formData.categoryName"
          placeholder="如：蔬菜采购、员工工资"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <!-- 实际金额 -->
      <el-form-item label="实际金额" prop="amount">
        <el-input-number
          v-model="formData.amount"
          :min="0"
          :precision="2"
          :step="100"
          :controls="true"
          style="width: 200px"
          placeholder="请输入实际金额"
        />
        <span class="unit-suffix">元</span>
      </el-form-item>

      <!-- 预算金额 -->
      <el-form-item label="预算金额" prop="budgetAmount">
        <el-input-number
          v-model="formData.budgetAmount"
          :min="0"
          :precision="2"
          :step="100"
          :controls="true"
          style="width: 200px"
          placeholder="选填"
        />
        <span class="unit-suffix">元</span>
      </el-form-item>

      <!-- 部门 -->
      <el-form-item label="部门" prop="department">
        <el-input
          v-model="formData.department"
          placeholder="选填"
          maxlength="50"
        />
      </el-form-item>

      <!-- 说明 -->
      <el-form-item label="说明" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="请输入成本说明"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>

      <!-- 备注 -->
      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="formData.remark"
          type="textarea"
          :rows="2"
          placeholder="选填"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        {{ isEditMode ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.cost-form {
  width: 500px;
}

.unit-suffix {
  margin-left: var(--fts-space-2);
  color: var(--fts-text-secondary);
  font-size: var(--fts-font-size-sm);
}
</style>
