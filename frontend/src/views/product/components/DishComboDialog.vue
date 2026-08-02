<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑套餐' : '新增套餐'"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="100px"
    >
      <!-- 基本信息 -->
      <div class="form-section">
        <div class="section-title">基本信息</div>
        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="套餐名称" prop="comboName">
              <el-input
                v-model="form.comboName"
                placeholder="请输入套餐名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="类型" prop="comboType">
              <el-select v-model="form.comboType" :teleported="false" style="width: 100%">
                <el-option label="固定套餐" value="fixed" />
                <el-option label="自选套餐" value="optional" />
                <el-option label="优惠套餐" value="discount" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="售价(元)" prop="price">
              <el-input-number
                v-model="form.price"
                :min="0"
                :precision="2"
                :step="1"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="适用人数">
              <el-input-number
                v-model="form.peopleCount"
                :min="1"
                :max="99"
                controls-position="right"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="描述">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入套餐描述"
            :rows="2"
            maxlength="200"
            show-word-limit
            resize="none"
          />
        </el-form-item>

        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio-button :value="1">启用</el-radio-button>
            <el-radio-button :value="0">停用</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </div>

      <!-- 套餐明细 -->
      <div class="form-section" v-loading="foodOptionsLoading" element-loading-text="正在加载菜品列表...">
        <div class="section-title">
          <span>套餐菜品明细</span>
          <el-button
            type="primary"
            size="small"
            :icon="Plus"
            :disabled="foodOptionsUnavailable"
            @click="addIngredient"
          >
            添加菜品
          </el-button>
        </div>

        <!-- 菜品列表加载失败提示 -->
        <el-alert
          v-if="foodOptionsError"
          type="error"
          :closable="false"
          show-icon
          title="菜品列表加载失败"
          description="无法加载菜品数据，请检查网络连接后重试。保存按钮已禁用。"
          class="options-alert"
        >
          <el-button size="small" type="primary" @click="loadFoodOptions">重新加载</el-button>
        </el-alert>
        <!-- 菜品列表为空提示 -->
        <el-alert
          v-else-if="!foodOptionsLoading && foodOptions.length === 0"
          type="warning"
          :closable="false"
          show-icon
          title="暂无可用菜品"
          description="系统中尚未录入菜品，无法添加套餐明细。请先在菜品管理中创建菜品。保存按钮已禁用。"
          class="options-alert"
        />

        <el-table :data="form.ingredients" size="small" border style="width: 100%" :resizable="false">
          <el-table-column label="菜品名称" min-width="150">
            <template #default="{ row, $index }">
              <el-select
                v-model="row.foodId"
                filterable
                placeholder="选择菜品"
                style="width: 100%"
                :popper-options="{ strategy: 'fixed' }"
                @change="(val: number) => handleFoodSelect(val, $index)"
              >
                <el-option
                  v-for="food in foodOptions"
                  :key="food.value"
                  :label="food.label"
                  :value="food.value"
                />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="数量" width="100" align="center">
            <template #default="{ row }">
              <el-input-number
                v-model="row.quantity"
                :min="0.5"
                :precision="1"
                :step="0.5"
                size="small"
                controls-position="right"
                style="width: 80px"
                @change="() => calcIngredientSubtotal(row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="单价(元)" width="100" align="right">
            <template #default="{ row }">
              <span class="subtotal-text">{{ row.unitPrice != null ? row.unitPrice.toFixed(2) : '0.00' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="小计(元)" width="100" align="right">
            <template #default="{ row }">
              <span class="subtotal-text">{{ row.subtotal != null ? row.subtotal.toFixed(2) : '0.00' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="单位" width="70" align="center">
            <template #default="{ row }">{{ row.unit || '份' }}</template>
          </el-table-column>
          <el-table-column label="必选" width="70" align="center">
            <template #default="{ row }">
              <el-switch v-model="row.isRequired" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="" width="50" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link size="small" @click="removeIngredient($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!form.ingredients.length" class="empty-tip">
          <el-text type="info">暂未添加菜品，点击上方"添加菜品"按钮开始配置</el-text>
        </div>

        <!-- 成本汇总 -->
        <div class="cost-summary">
          <div class="summary-row">
            <span>预估成本合计：</span>
            <strong class="cost-value">{{ totalCost.toFixed(2) }} 元</strong>
          </div>
          <div class="summary-row" v-if="form.price">
            <span>预计毛利率：</span>
            <strong :class="marginRate >= 0 ? 'profit-value' : 'loss-value'">
              {{ marginRate.toFixed(1) }}%
            </strong>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="submitting"
        :disabled="foodOptionsUnavailable"
        @click="handleSubmit"
      >
        {{ isEdit ? '保存' : '创建' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import { foodApi } from '@/api/product/food'
import { comboApi } from '@/api/product/combo'
import type { DishComboFormData, CategoryOption, ComboIngredient } from '@/types/product'

const emit = defineEmits<{
  (e: 'submit', data: DishComboFormData): void
}>()

const visible = ref(false)
const formRef = ref<FormInstance>()
const isEdit = ref(false)
const submitting = ref(false)
const foodOptions = ref<CategoryOption[]>([])
/** 菜品选项加载中状态 */
const foodOptionsLoading = ref(false)
/** 菜品选项加载失败标记（用于在弹窗中显示错误提示和重试按钮） */
const foodOptionsError = ref(false)

/** 菜品选项不可用（加载失败或为空）时禁用保存按钮 */
const foodOptionsUnavailable = computed(() => {
  return !foodOptionsLoading.value && (foodOptionsError.value || foodOptions.value.length === 0)
})

/** 菜品明细项 */
interface IngredientItem {
  foodId: number
  foodName: string
  quantity: number
  unit: string
  costPrice: number
  unitPrice: number
  subtotal: number
  isRequired: boolean
}

const form = reactive({
  id: undefined as number | undefined,
  comboName: '',
  comboType: 'fixed',
  price: undefined as number | undefined,
  peopleCount: 1,
  description: '',
  // 状态使用数字（1启用/0停用），与 DishComboFormData.status 类型一致；后端 ComboUpdateDTO.status 也是 Integer
  status: 1 as number,
  ingredients: [] as IngredientItem[],
})

const rules: FormRules = {
  comboName: [
    { required: true, message: '请输入套餐名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在2到50个字符之间', trigger: 'blur' },
  ],
  comboType: [{ required: true, message: '请选择套餐类型', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
}

/** 计算总成本（优先使用行小计，未计算时用 数量×单价） */
const totalCost = computed(() => {
  return form.ingredients.reduce((sum, item) => {
    const subtotal = item.subtotal != null && item.subtotal > 0 ? item.subtotal : (item.quantity || 0) * (item.unitPrice || item.costPrice || 0)
    return sum + subtotal
  }, 0)
})

/** 计算毛利率 */
const marginRate = computed(() => {
  if (!form.price || form.price <= 0) return 0
  return ((form.price - totalCost.value) / form.price) * 100
})

async function loadFoodOptions(): Promise<void> {
  foodOptionsLoading.value = true
  foodOptionsError.value = false
  try {
    const res = await foodApi.getList({ page: 1, size: 1000 })
    foodOptions.value = (res.records || []).map(food => ({
      value: food.foodId,
      label: food.foodName,
    }))
  } catch {
    foodOptions.value = []
    foodOptionsError.value = true
  } finally {
    foodOptionsLoading.value = false
  }
}

function openCreate(): void {
  resetForm()
  isEdit.value = false
  visible.value = true
  loadFoodOptions()
}

async function openEdit(row: Record<string, unknown>): Promise<void> {
  resetForm()
  isEdit.value = true
  // DishComboRow 字段：comboId, comboCode, comboName, comboPrice(字符串), comboStatus(字符串)
  form.id = (row.comboId as number | undefined) ?? (row.id as number | undefined)
  form.comboName = (row.comboName as string) || ''
  form.comboType = (row.comboType as string) || 'fixed'
  // comboPrice 是字符串（元），转换为 number 给 el-input-number
  const priceStr = (row.comboPrice as string | number | undefined)
  form.price = priceStr != null ? Number(priceStr) : undefined
  form.peopleCount = (row.peopleCount as number) || 1
  form.description = (row.description as string) || ''
  // 表格中字段名为 comboStatus（'active'/'inactive' 字符串），需转换为数字给 form.status
  const statusStr = (row.comboStatus as string | undefined) || (row.status as string | undefined) || 'active'
  form.status = statusStr === 'active' ? 1 : 0

  // 通过 comboApi.getById 加载完整明细（修复：此前不加载导致编辑时明细空白）
  try {
    const detail = await comboApi.getById(form.id as number)
    form.ingredients = (detail.ingredients || []).map((item: ComboIngredient) => ({
      foodId: Number(item.foodId),
      foodName: item.foodName || '',
      quantity: item.quantity || 1,
      unit: item.unit || '份',
      costPrice: Number(item.unitPrice) || 0,
      unitPrice: Number(item.unitPrice) || 0,
      subtotal: Number(item.subtotal) || 0,
      isRequired: item.isRequired !== false,
    }))
  } catch {
    // 加载明细失败时保持空列表，交由用户手动添加
    form.ingredients = []
  }

  visible.value = true
  loadFoodOptions()
}

function close(): void {
  visible.value = false
}

function resetForm(): void {
  form.id = undefined
  form.comboName = ''
  form.comboType = 'fixed'
  form.price = undefined
  form.peopleCount = 1
  form.description = ''
  form.status = 1
  form.ingredients = []
  isEdit.value = false
  submitting.value = false
  formRef.value?.resetFields()
}

function handleClose(): void {
  resetForm()
}

function addIngredient(): void {
  form.ingredients.push({
    foodId: 0,
    foodName: '',
    quantity: 1,
    unit: '份',
    costPrice: 0,
    unitPrice: 0,
    subtotal: 0,
    isRequired: true,
  })
}

function removeIngredient(index: number): void {
  form.ingredients.splice(index, 1)
}

/** 数量变化时重算小计 */
function calcIngredientSubtotal(row: IngredientItem): void {
  row.subtotal = Math.round(Number(row.unitPrice || 0) * Number(row.quantity) * 100) / 100
}

async function handleFoodSelect(foodId: number, index: number): Promise<void> {
  // 查找选中菜品的信息
  try {
    const food = await foodApi.getById(foodId)
    form.ingredients[index] = {
      ...form.ingredients[index],
      foodId,
      foodName: food.foodName,
      unit: '份',
      costPrice: Number(food.costPrice) || 0,
      unitPrice: Number(food.costPrice) || 0,
    }
    calcIngredientSubtotal(form.ingredients[index])
  } catch {
    // 静默处理
  }
}

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  try {
    await formRef.value.validate()
  } catch {
    ElMessage.warning('请检查表单填写是否正确')
    return
  }

  // 校验：套餐必须至少关联一个菜品
  if (!form.ingredients || form.ingredients.length === 0) {
    ElMessage.warning('请至少选择一个菜品')
    return
  }

  submitting.value = true
  try {
    // form.status 已为 number 类型（1/0），与 DishComboFormData.status 类型一致
    const submitData: DishComboFormData = {
      ...form,
      // comboPrice 字段：converter 会将元转分，这里传字符串或数字均可
      comboPrice: form.price != null ? String(form.price) : '0',
    }
    emit('submit', submitData)
  } finally {
    submitting.value = false
  }
}

defineExpose({ openCreate, openEdit, close })
</script>

<style scoped lang="scss">
.form-section {
  margin-bottom: 20px;
}

.section-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 15px;
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px dashed var(--fts-border-secondary);
}

.empty-tip {
  text-align: center;
  padding: 20px 0;
}

// 菜品列表加载失败/为空的提示框
.options-alert {
  margin-bottom: 12px;

  :deep(.el-alert__description) {
    margin-bottom: 8px;
  }
}

.cost-summary {
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--fts-bg-secondary);
  border-radius: var(--el-border-radius-small);
}

.summary-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;

  & + .summary-row {
    margin-top: 6px;
  }
}

.cost-value {
  color: var(--fts-error);
  font-size: 16px;
}

.profit-value {
  color: var(--fts-success);
}

.loss-value {
  color: var(--fts-error);
}
</style>
