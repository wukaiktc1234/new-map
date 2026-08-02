<script setup lang="ts">
/**
 * 电子合同 - 作废确认对话框（3步向导）
 *
 * 步骤1：风险提示
 * 步骤2：填写作废原因 + 详细说明
 * 步骤3：输入"确认作废"确认执行
 *
 * 内部状态自管理，父组件仅传入 visible + target，作废完成后 emit('success')
 */
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { electronicContractApi } from '@/api/purchase'
import { electronicContractConverter } from '@/api/purchase/converters'
import type { ElectronicContractInfo } from '@/types/purchase-electronic-contract'

interface Props {
  /** 对话框可见性（v-model） */
  visible: boolean
  /** 作废目标合同 */
  target: ElectronicContractInfo | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  /** 作废成功 */
  success: []
}>()

/** 当前步骤（0/1/2） */
const invalidateStep = ref(0)
/** 作废表单数据 */
const invalidateFormData = reactive({
  reason: '',
  detail: '',
  confirmText: '',
})
/** 作废中加载态 */
const invalidating = ref(false)

/** 监听 visible 变化，打开时初始化状态 */
watch(() => props.visible, (open) => {
  if (open) {
    invalidateStep.value = 0
    Object.assign(invalidateFormData, { reason: '', detail: '', confirmText: '' })
  }
})

/** 关闭对话框 */
function closeDialog() {
  emit('update:visible', false)
}

/** 作废下一步（校验） */
function handleInvalidateNext() {
  if (invalidateStep.value === 1) {
    if (!invalidateFormData.reason) {
      ElMessage.warning('请选择作废原因')
      return
    }
    if (!invalidateFormData.detail?.trim()) {
      ElMessage.warning('请填写详细说明')
      return
    }
  }
  invalidateStep.value++
}

/** 执行作废 */
async function executeInvalidate() {
  if (!props.target) return
  if (invalidateFormData.confirmText !== '确认作废') {
    ElMessage.warning('请输入"确认作废"')
    return
  }
  invalidating.value = true
  try {
    await electronicContractApi.invalidate(props.target.eContractId, {
      reason: `${invalidateFormData.reason}：${invalidateFormData.detail}`,
    })
    ElMessage.success('合同已作废')
    emit('update:visible', false)
    emit('success')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    invalidating.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="作废电子合同"
    width="680px"
    class="fts-dialog--md"
    destroy-on-close
    lock-scroll="false"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-steps :active="invalidateStep" finish-status="success" align-center style="margin-bottom: 24px;">
      <el-step title="风险提示" />
      <el-step title="填写原因" />
      <el-step title="确认作废" />
    </el-steps>

    <!-- 步骤1：风险提示 -->
    <div v-if="invalidateStep === 0" class="invalidate-step-content">
      <el-alert type="error" :closable="false" style="margin-bottom: 16px;">
        <template #title>作废操作不可恢复，请谨慎操作</template>
      </el-alert>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="合同名称">{{ target?.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ target?.eContractNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ target?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ target ? electronicContractConverter.formatYuan(target.totalAmount) : '' }} 元</el-descriptions-item>
      </el-descriptions>
      <el-alert type="warning" :closable="false" style="margin-top: 16px;">
        <template #title>作废后合同将失去法律效力，相关采购订单和结算可能受到影响</template>
      </el-alert>
    </div>

    <!-- 步骤2：填写原因 -->
    <div v-if="invalidateStep === 1" class="invalidate-step-content">
      <el-form label-width="100px">
        <el-form-item label="作废原因" required>
          <el-select v-model="invalidateFormData.reason" placeholder="请选择作废原因" :teleported="false" style="width: 100%;">
            <el-option label="双方协商一致" value="双方协商一致" />
            <el-option label="供应商无法履约" value="供应商无法履约" />
            <el-option label="合同条款变更" value="合同条款变更" />
            <el-option label="业务需求取消" value="业务需求取消" />
            <el-option label="其他原因" value="其他原因" />
          </el-select>
        </el-form-item>
        <el-form-item label="详细说明" required>
          <el-input v-model="invalidateFormData.detail" type="textarea" :rows="3" placeholder="请详细说明作废原因（必填）" />
        </el-form-item>
      </el-form>
    </div>

    <!-- 步骤3：确认作废 -->
    <div v-if="invalidateStep === 2" class="invalidate-step-content">
      <el-alert type="error" :closable="false" style="margin-bottom: 16px;">
        <template #title>请输入"确认作废"以完成操作</template>
      </el-alert>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="合同名称">{{ target?.contractName }}</el-descriptions-item>
        <el-descriptions-item label="作废原因">{{ invalidateFormData.reason }}</el-descriptions-item>
        <el-descriptions-item label="详细说明">{{ invalidateFormData.detail }}</el-descriptions-item>
      </el-descriptions>
      <el-input v-model="invalidateFormData.confirmText" placeholder='请输入"确认作废"' style="margin-top: 16px;" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="invalidateStep > 0" @click="invalidateStep--">上一步</el-button>
        <el-button v-if="invalidateStep < 2" type="primary" @click="handleInvalidateNext">下一步</el-button>
        <el-button v-if="invalidateStep === 2" type="danger" :loading="invalidating" :disabled="invalidateFormData.confirmText !== '确认作废'" @click="executeInvalidate">确认作废</el-button>
        <el-button @click="closeDialog">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.invalidate-step-content {
  min-height: 200px;
  padding: var(--fts-space-2) 0;
}
</style>
