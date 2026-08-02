<script setup lang="ts">
/**
 * 采购订单 - 新建/编辑表单对话框
 *
 * 【层级】L7 - 模块层组件
 * 【职责】管理采购订单新建/编辑表单、明细行、物料下拉选择及供应商联动
 *
 * 关键设计：
 * - 供应商与明细主供应商强一致：未开启「允许跨供应商选料」时，切换供应商会强制改回与明细一致（不自动勾选）
 * - 物料下拉：选项富展示（名称[规格]·供应商），选中后仅显示物料名称（简短）
 * - 数量/单价使用纯输入（无加减按钮，省空间）
 */
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

import { purchaseOrderApi } from '@/api/purchase'
import { materialArchiveApi } from '@/api/purchase/archive'
import { supplierConverter } from '@/api/purchase/converters'
import CrossSupplierTipDialog from '@/components/business/CrossSupplierTipDialog.vue'
import type { PurchaseOrderInfo, PurchaseOrderItemInfo } from '@/types/purchase-order'
import type { SupplierInfo } from '@/types/purchase-supplier'
import type { MaterialArchiveInfo } from '@/types/purchase-archive'

interface OrderFormState {
  supplierId: string
  supplierName: string
  expectedDate: string
  remark: string
  items: PurchaseOrderItemInfo[]
}

interface Props {
  /** 供应商选项（由父组件加载后传入，避免重复加载） */
  supplierOptions: SupplierInfo[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  /** 提交成功 */
  success: []
}>()

// ================ 状态 ================

const visible = ref(false)
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive<OrderFormState>({
  supplierId: '',
  supplierName: '',
  expectedDate: '',
  remark: '',
  items: [],
})

/** 物料档案选项 */
const materialOptions = ref<MaterialArchiveInfo[]>([])
/** 是否允许跨供应商选料（默认关闭，仅显示当前供应商的物料） */
const allowCrossSupplier = ref(false)
/** 供应商一致性提示对话框 */
const crossTipRef = ref<InstanceType<typeof CrossSupplierTipDialog>>()

// ==================== 计算属性 ====================

/** 当前选中的供应商 */
const selectedSupplier = computed<SupplierInfo | null>(() => {
  return props.supplierOptions.find(s => s.supplierId === formData.supplierId) || null
})

/** 根据当前供应商过滤后的物料选项 */
const filteredMaterialOptions = computed<MaterialArchiveInfo[]>(() => {
  if (!formData.supplierId || allowCrossSupplier.value) {
    return materialOptions.value
  }
  return materialOptions.value.filter(
    m => !m.supplierId || m.supplierId === formData.supplierId,
  )
})

/** 明细合计金额 */
const itemsTotalAmount = computed(() => {
  return formData.items.reduce((sum, item) => sum + (item.amount || 0), 0)
})

// ==================== 表单校验规则 ====================

const formRules: FormRules = {
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  expectedDate: [{ required: true, message: '请选择预计到货日期', trigger: 'change' }],
}

// ==================== 生命周期 ====================

onMounted(() => {
  loadMaterialOptions()
})

// ==================== 数据加载 ====================

/** 加载物料档案选项（仅启用状态的物料） */
async function loadMaterialOptions(): Promise<void> {
  try {
    const res = await materialArchiveApi.getList({ status: 'active', size: 1000 })
    materialOptions.value = res?.records || []
  } catch {
    materialOptions.value = []
  }
}

// ==================== 打开/编辑 ====================

/**
 * 新建订单（可带初始数据，如从申请/计划带入）
 */
function create(initialData?: Partial<OrderFormState>): void {
  isEdit.value = false
  editingId.value = null
  allowCrossSupplier.value = false
  resetForm()
  if (initialData) {
    Object.assign(formData, {
      supplierId: initialData.supplierId || '',
      supplierName: initialData.supplierName || '',
      expectedDate: initialData.expectedDate || '',
      remark: initialData.remark || '',
      items: initialData.items ? [...initialData.items] : [],
    })
  }
  if (formData.items.length === 0) {
    addItem()
  }
  visible.value = true
}

/**
 * 编辑订单
 */
async function edit(id: string): Promise<void> {
  isEdit.value = true
  editingId.value = id
  allowCrossSupplier.value = false
  resetForm()
  try {
    const detail = await purchaseOrderApi.getById(id)
    if (detail) {
      Object.assign(formData, {
        supplierId: detail.supplierId,
        supplierName: detail.supplierName,
        expectedDate: detail.expectedDate,
        remark: detail.remark,
        items: detail.items ? [...detail.items] : [],
      })
      // 供应商自动回填：订单表头未指定供应商（如从采购申请转单，supplierId=null）时，
      // 取明细物料的主供应商（物料档案）自动填入，避免表头空白需手选
      if (!formData.supplierId && formData.items.length > 0) {
        const firstWithMaterial = formData.items.find(item => item.materialId)
        const material = firstWithMaterial
          ? materialOptions.value.find(m => m.materialId === firstWithMaterial.materialId)
          : null
        if (material?.supplierId) {
          const supplier = props.supplierOptions.find(s => s.supplierId === material.supplierId)
          formData.supplierId = material.supplierId
          formData.supplierName = supplier?.supplierName || material.supplierName || ''
        }
      }
      visible.value = true
    }
  } catch {
    ElMessage.error('加载订单详情失败')
  }
}

/** 重置表单 */
function resetForm(): void {
  formData.supplierId = ''
  formData.supplierName = ''
  formData.expectedDate = ''
  formData.remark = ''
  formData.items = []
  formRef.value?.resetFields()
}

// ==================== 供应商变更 ====================

/**
 * 供应商选择变更：未开启跨供应商时强制与明细主供应商一致（不自动勾选）
 */
async function handleSupplierChange(supplierId: string): Promise<void> {
  const supplier = props.supplierOptions.find(s => s.supplierId === supplierId)
  if (supplier) {
    formData.supplierId = supplier.supplierId
    formData.supplierName = supplier.supplierName
  } else {
    formData.supplierId = ''
    formData.supplierName = ''
  }

  // 检测已选明细物料的主供应商与新供应商是否一致
  const mismatched = formData.items.filter(item => {
    if (!item.materialId) return false
    const material = materialOptions.value.find(m => m.materialId === item.materialId)
    return material && material.supplierId && material.supplierId !== supplierId
  })

  if (mismatched.length > 0 && !allowCrossSupplier.value) {
    // 未开启跨供应商：强制改回与明细一致的主供应商（不自动勾选）
    const itemMaterials = formData.items
      .map(item => materialOptions.value.find(m => m.materialId === item.materialId))
      .filter((m): m is MaterialArchiveInfo => !!m && !!m.supplierId)
    const primarySupplierId = itemMaterials[0]?.supplierId ?? supplierId
    const primarySupplier = props.supplierOptions.find(s => s.supplierId === primarySupplierId)
    if (primarySupplier && primarySupplier.supplierId !== supplierId) {
      formData.supplierId = primarySupplier.supplierId
      formData.supplierName = primarySupplier.supplierName
    }
    crossTipRef.value?.open({
      mismatchCount: mismatched.length,
      primarySupplierName: primarySupplier?.supplierName || '-',
      selectedSupplierName: supplier?.supplierName || '所选供应商',
    })
    return
  }

  if (mismatched.length > 0 && allowCrossSupplier.value) {
    ElMessage.warning(
      `当前已选明细中有 ${mismatched.length} 项的主供应商与「${formData.supplierName || '所选供应商'}」不一致，已按跨供应商模式继续`,
    )
  } else {
    // 切换供应商后如果存在当前供应商可供应的物料，则关闭跨供应商开关
    allowCrossSupplier.value = false
  }
}

// ==================== 明细行操作 ====================

/** 添加明细行 */
function addItem(): void {
  formData.items.push({
    id: Date.now(),
    purchaseOrderId: '',
    materialId: '',
    materialName: '',
    specification: '',
    unit: '',
    quantity: 0,
    unitPrice: 0,
    amount: 0,
    receivedQuantity: 0,
    remark: '',
  } as unknown as PurchaseOrderItemInfo)
}

/** 删除明细行 */
function removeItem(index: number): void {
  formData.items.splice(index, 1)
}

/** 计算单项金额 */
function calcItemAmount(index: number): void {
  const item = formData.items[index]
  item.amount = Math.round(item.quantity * item.unitPrice * 100) / 100
}

/** 物料选择变更：自动带出规格、单位、参考价；表头供应商为空时自动带入物料主供应商 */
function handleMaterialChange(index: number, materialId: string): void {
  const item = formData.items[index]
  const material = materialOptions.value.find(m => m.materialId === materialId)
  if (material) {
    item.materialId = material.materialId
    item.materialName = material.materialName
    item.specification = material.spec
    item.unit = material.unit
    item.unitPrice = material.referencePrice
    // 表头供应商未指定时，自动带入该物料的主供应商
    if (!formData.supplierId && material.supplierId) {
      formData.supplierId = material.supplierId
      formData.supplierName = material.supplierName || ''
    }
  } else {
    item.materialId = materialId
    item.materialName = ''
    item.specification = ''
    item.unit = ''
    item.unitPrice = 0
  }
  item.amount = Math.round(item.quantity * item.unitPrice * 100) / 100
}

// ==================== 提交 ====================

async function handleSubmit(): Promise<void> {
  if (!formRef.value) return

  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  // 校验：必须至少添加一条有效明细
  if (!formData.items.some(i => i.materialId)) {
    ElMessage.warning('请至少添加一条有效明细')
    return
  }

  submitLoading.value = true
  try {
    const submitData = {
      ...formData,
    }

    if (isEdit.value && editingId.value) {
      await purchaseOrderApi.update(editingId.value, submitData as Partial<PurchaseOrderInfo>)
      ElMessage.success('保存成功')
    } else {
      const created = await purchaseOrderApi.create(submitData as never)
      ElMessage.success(created.length > 1 ? `已生成 ${created.length} 张采购订单（已按供应商自动拆分）` : '创建成功')
    }
    visible.value = false
    emit('success')
  } catch (error: unknown) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    submitLoading.value = false
  }
}

// ==================== 展示辅助 ====================

/** 格式化金额 */
function formatAmount(val: number): string {
  return val != null ? val.toFixed(2) : '0.00'
}

/** 获取明细行物料的供应商名称 */
function getItemSupplierName(item: PurchaseOrderItemInfo): string {
  if (!item.materialId) return ''
  const material = materialOptions.value.find(m => m.materialId === item.materialId)
  return material?.supplierName || ''
}

/** 判断明细行是否为跨供应商物料 */
function isCrossSupplierItem(item: PurchaseOrderItemInfo): boolean {
  if (!item.materialId || !formData.supplierId) return false
  const material = materialOptions.value.find(m => m.materialId === item.materialId)
  return !!(material && material.supplierId && material.supplierId !== formData.supplierId)
}

defineExpose({
  create,
  edit,
})
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="isEdit ? '编辑订单' : '新建订单'"
    width="1200px"
    class="fts-dialog--xl"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="供应商" prop="supplierId">
            <el-select
              v-model="formData.supplierId"
              placeholder="请选择供应商"
              filterable
              :teleported="false"
              style="width: 100%"
              @change="handleSupplierChange"
            >
              <el-option
                v-for="s in supplierOptions"
                :key="s.supplierId"
                :label="s.supplierName"
                :value="s.supplierId"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="预计到货日期" prop="expectedDate">
            <el-date-picker
              v-model="formData.expectedDate"
              type="date"
              placeholder="选择日期"
              :teleported="false"
              value-format="YYYY-MM-DD"
              style="width: 100%"
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-row v-if="selectedSupplier" :gutter="20">
        <el-col :span="12">
          <el-form-item label="账期">
            <el-input :model-value="`${selectedSupplier.paymentTerms}天`" disabled />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="结算方式">
            <el-input
              :model-value="supplierConverter.toSettlementMethodLabel(selectedSupplier.settlementMethod)"
              disabled
            />
          </el-form-item>
        </el-col>
      </el-row>

      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>

      <!-- 明细列表 -->
      <div class="form-section">
        <div class="section-title">
          <span>明细列表</span>
          <div class="section-actions">
            <el-checkbox v-model="allowCrossSupplier">
              允许跨供应商选料
            </el-checkbox>
            <el-button type="primary" :icon="Plus" @click="addItem">
              添加明细
            </el-button>
          </div>
        </div>

        <el-table :data="formData.items" border style="width: 100%">
          <el-table-column label="物料名称" min-width="200">
            <template #default="{ row, $index }">
              <el-select
                v-model="row.materialId"
                placeholder="请选择物料"
                filterable
                :teleported="true"
                :popper-options="{ strategy: 'fixed' }"
                style="width: 100%"
                @change="(val: string) => handleMaterialChange($index, val)"
              >
                <el-option
                  v-for="m in filteredMaterialOptions"
                  :key="m.materialId"
                  :label="m.materialName"
                  :value="m.materialId"
                >
                  <div class="mat-option">
                    <span class="mat-option__name">{{ m.materialName }}</span>
                    <span class="mat-option__sub">{{ m.spec ? `[${m.spec}]` : '' }}{{ m.supplierName ? ` · ${m.supplierName}` : '' }}</span>
                  </div>
                </el-option>
                <template #empty>
                  <div class="material-empty-tip">
                    {{ formData.supplierId ? '该供应商暂无供应的物料，可勾选「允许跨供应商选料」后选择' : '暂无可用物料，请先在商品档案维护' }}
                  </div>
                </template>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="供应商" width="130" align="center">
            <template #default="{ row }">
              <span :class="['supplier-tag', { 'supplier-tag--warning': isCrossSupplierItem(row) }]">
                {{ getItemSupplierName(row) || '-' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="规格" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.specification" placeholder="规格" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="单位" width="100" align="center">
            <template #default="{ row }">
              <el-input v-model="row.unit" placeholder="单位" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="数量" width="120" align="center">
            <template #default="{ row, $index }">
              <el-input-number
                v-model="row.quantity"
                :min="0"
                :precision="2"
                :controls="false"
                style="width: 110px"
                @change="() => calcItemAmount($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="单价(元)" width="150" align="center">
            <template #default="{ row, $index }">
              <el-input-number
                v-model="row.unitPrice"
                :min="0"
                :precision="2"
                :controls="false"
                style="width: 130px"
                @change="() => calcItemAmount($index)"
              />
            </template>
          </el-table-column>
          <el-table-column label="金额(元)" width="120" align="right">
            <template #default="{ row }">
              <span class="amount-text">¥{{ formatAmount(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="备注" min-width="140">
            <template #default="{ row }">
              <el-input v-model="row.remark" placeholder="备注" style="width: 100%" />
            </template>
          </el-table-column>
          <el-table-column label="" width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeItem($index)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="!formData.items.length" class="empty-tip">
          <el-text type="info">暂未添加明细，点击上方"添加明细"按钮开始配置</el-text>
        </div>

        <!-- 金额汇总 -->
        <div class="amount-summary">
          <div class="summary-row">
            <span>明细合计：</span>
            <strong class="amount-value">¥{{ formatAmount(itemsTotalAmount) }}</strong>
          </div>
        </div>
      </div>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">
          {{ isEdit ? '保存' : '创建' }}
        </el-button>
      </div>
    </template>
  </el-dialog>

  <!-- 供应商一致性提示 -->
  <CrossSupplierTipDialog ref="crossTipRef" />
</template>

<style scoped lang="scss">
.form-section {
  margin-bottom: var(--fts-space-4);
  padding: var(--fts-space-4);
  border: 1px solid var(--fts-border-primary);
  border-radius: var(--fts-card-radius);
  background: var(--fts-bg-card);

  .section-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: var(--fts-space-3);
    font-weight: var(--fts-font-weight-semibold);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }
}

/* 明细表格：加大行距与字号；内嵌控件撑满单元格（覆盖 el-table .cell 默认内边距） */
:deep(.el-table__cell) {
  padding: 10px 8px;
  font-size: 14px;
}
:deep(.el-table__body-wrapper .cell) {
  padding: 0;
}

/* 跨供应商开关：边框化显眼，与"添加明细"按钮垂直对齐 */
.section-actions {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);

  :deep(.el-checkbox) {
    border: 1px solid var(--fts-border-primary);
    border-radius: var(--fts-radius-base);
    padding: 5px 10px;
    margin-right: 0;
    background: var(--fts-bg-secondary);

    &.is-checked {
      border-color: var(--fts-primary);
    }
  }
}

/* 物料选项：名称 + 供应商/规格 富展示 */
.mat-option {
  display: flex;
  align-items: baseline;
  gap: var(--fts-space-2);

  &__name {
    font-weight: var(--fts-font-weight-medium);
  }

  &__sub {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}

.material-empty-tip {
  padding: var(--fts-space-3);
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  text-align: center;
}

.empty-tip {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: var(--fts-space-6) 0;
}

.amount-summary {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--fts-space-3);

  .summary-row {
    display: flex;
    align-items: center;
    gap: var(--fts-space-2);
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-primary);
  }

  .amount-value {
    font-variant-numeric: tabular-nums;
    color: var(--fts-text-primary);
    font-weight: var(--fts-font-weight-semibold);
  }
}

.supplier-tag {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);

  &--warning {
    color: var(--fts-warning);
  }
}
</style>
