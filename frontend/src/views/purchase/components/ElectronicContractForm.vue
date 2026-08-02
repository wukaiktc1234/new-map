<script setup lang="ts">
/**
 * 电子合同 - 新建/编辑表单对话框
 *
 * 内部管理表单状态和校验，提交时调用 electronicContractApi.create/update
 * 父组件传入 visible + isEdit + editData + 选项数据，完成后 emit('success')
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { electronicContractApi } from '@/api/purchase'
import type { ElectronicContractFormData } from '@/types/purchase-electronic-contract'
import type { ElectronicContractInfo } from '@/types/purchase-electronic-contract'
import type { SupplierInfo } from '@/types/purchase-supplier'

/**
 * 占位数据：原 @/api/purchase/mockData 已清理。
 * 等待后端合同模板 API 落地后替换为真实数据源。
 */
const contractTemplates: Record<string, string> = {}

/** 关联采购合同选项（动态加载） */
interface ContractOption {
  contractId: string
  contractNo: string
  contractName: string
}

interface Props {
  /** 对话框可见性（v-model） */
  visible: boolean
  /** 是否编辑模式 */
  isEdit: boolean
  /** 编辑时的原始数据 */
  editData: ElectronicContractInfo | null
  /** 当前编辑的ID（编辑模式） */
  currentId: string
  /** 供应商选项（动态加载） */
  supplierOptions: SupplierInfo[]
  /** 关联采购合同选项（动态加载） */
  contractOptions: ContractOption[]
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  /** 创建/更新成功 */
  success: []
}>()

/** 表单ref */
const formRef = ref<FormInstance>()
/** 提交中加载态 */
const submitting = ref(false)

/** 表单数据 */
const formData = reactive<ElectronicContractFormData>({
  contractName: '',
  eContractNo: '',
  partyA: '',
  supplierName: '',
  totalAmount: 0,
  signMethod: 'electronic',
  startDate: '',
  expireDate: '',
  relatedContractId: '',
  templateId: '',
  customTemplateName: '',
  contractContent: '',
  remark: '',
})

/** 表单校验规则 */
const formRules = {
  contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
  partyA: [{ required: true, message: '请输入甲方名称', trigger: 'blur' }],
  supplierName: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  totalAmount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  expireDate: [{ required: true, message: '请选择到期日期', trigger: 'change' }],
}

/** 监听 visible 变化，打开时初始化表单 */
watch(() => props.visible, (open) => {
  if (!open) return
  if (props.isEdit && props.editData) {
    // 编辑模式：填充表单
    Object.assign(formData, {
      contractName: props.editData.contractName,
      eContractNo: props.editData.eContractNo,
      partyA: props.editData.partyA || '',
      supplierName: props.editData.supplierName,
      totalAmount: props.editData.totalAmount,
      signMethod: props.editData.signMethod || 'electronic',
      startDate: props.editData.startDate || '',
      expireDate: props.editData.expireDate,
      relatedContractId: props.editData.contractId || '',
      templateId: '',
      customTemplateName: '',
      contractContent: props.editData.contractContent || '',
      remark: props.editData.remark || '',
    })
  } else {
    // 新建模式：重置表单
    Object.assign(formData, {
      contractName: '', eContractNo: '', partyA: '', supplierName: '',
      totalAmount: 0, signMethod: 'electronic', startDate: '', expireDate: '',
      relatedContractId: '', templateId: '', customTemplateName: '',
      contractContent: '', remark: '',
    })
  }
})

/** 选择合同模板 */
function onTemplateChange(templateId: string) {
  // 原 @/api/purchase/mockData 已清理；等待后端合同模板 API 落地
  if (templateId && contractTemplates[templateId]) {
    formData.contractContent = contractTemplates[templateId]
  } else if (templateId === 'tpl-custom') {
    formData.contractContent = ''
  }
}

/** 关闭对话框 */
function closeDialog() {
  emit('update:visible', false)
}

/** 提交表单 */
async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (props.isEdit) {
      await electronicContractApi.update(props.currentId, formData)
      ElMessage.success('更新成功')
    } else {
      await electronicContractApi.create(formData)
      ElMessage.success('创建成功')
    }
    emit('update:visible', false)
    emit('success')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑电子合同' : '新建电子合同'"
    width="1200px"
    class="fts-dialog--xl"
    :close-on-click-modal="false"
    destroy-on-close
    lock-scroll="false"
    append-to-body
    @update:model-value="emit('update:visible', $event)"
  >
    <el-form ref="formRef" :model="formData" :rules="formRules" label-width="110px">
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="合同名称" prop="contractName">
            <el-input v-model="formData.contractName" placeholder="请输入合同名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合同编号">
            <el-input v-model="formData.eContractNo" placeholder="系统自动生成" disabled />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="甲方" prop="partyA">
            <el-input v-model="formData.partyA" placeholder="请输入甲方名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="乙方(供应商)" prop="supplierName">
            <el-select v-model="formData.supplierName" placeholder="请选择供应商" style="width:100%" filterable :teleported="false">
              <el-option v-for="s in supplierOptions" :key="s.supplierId" :label="s.supplierName" :value="s.supplierName" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="合同金额(元)" prop="totalAmount">
            <el-input-number v-model="formData.totalAmount" :min="0" :precision="2" controls-position="right" style="width:100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="签署方式">
            <el-select v-model="formData.signMethod" placeholder="请选择签署方式" style="width:100%" :teleported="false">
              <el-option label="电子签署" value="electronic" />
              <el-option label="混合签署" value="hybrid" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :span="12">
          <el-form-item label="合同开始日期" prop="startDate">
            <el-date-picker v-model="formData.startDate" type="date" placeholder="请选择开始日期" value-format="YYYY-MM-DD" style="width:100%" :teleported="false" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="合同到期日期" prop="expireDate">
            <el-date-picker v-model="formData.expireDate" type="date" placeholder="请选择到期日期" value-format="YYYY-MM-DD" style="width:100%" :teleported="false" />
          </el-form-item>
        </el-col>
      </el-row>
      <!-- 关联采购合同（可选） -->
      <el-form-item label="关联采购合同">
        <el-select v-model="formData.relatedContractId" placeholder="选择关联采购合同（可选）" clearable style="width:100%" :teleported="false">
          <el-option v-for="c in contractOptions" :key="c.contractId" :label="`${c.contractNo} - ${c.contractName}`" :value="c.contractId" />
        </el-select>
      </el-form-item>
      <!-- 合同内容 -->
      <el-divider content-position="left">合同内容</el-divider>
      <el-form-item label="合同模板">
        <el-select v-model="formData.templateId" placeholder="选择合同模板（可选）" clearable style="width:100%" :teleported="false" @change="onTemplateChange">
          <el-option label="标准采购合同模板" value="tpl-standard" />
          <el-option label="食品供应合同模板" value="tpl-food" />
          <el-option label="设备采购合同模板" value="tpl-equipment" />
          <el-option label="自定义模板" value="tpl-custom" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="formData.templateId === 'tpl-custom'" label="模板名称">
        <el-input v-model="formData.customTemplateName" placeholder="请输入自定义模板名称" />
      </el-form-item>
      <el-form-item label="合同正文">
        <el-input v-model="formData.contractContent" type="textarea" :rows="8" placeholder="请输入合同正文内容，或选择合同模板自动填充" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="请输入备注" />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="dialog-footer">
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </div>
    </template>
  </el-dialog>
</template>
