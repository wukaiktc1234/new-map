<script setup lang="ts">
/**
 * 合同验真对话框（公开接口）
 *
 * 【功能】
 * - 输入合同编号 + 哈希前8位
 * - 调用 contractApi.verifyContract 进行验真
 * - 显示验真结果（成功/失败）
 * - 成功时显示合同基本信息（编号、员工、签署时间）
 * - 失败时提示原因
 *
 * 【场景】员工、第三方机构可通过此功能验证合同真伪
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, CircleCloseFilled, Document } from '@element-plus/icons-vue'
import StatusTag from '@/components/core/StatusTag.vue'
import { contractApi } from '@/api/hr/contract'
import type {
  EmployeeContract,
  ContractVerifyForm,
  ContractVerifyResult,
} from '@/types/hr/contract'
import { ContractTypeOptions } from '@/types/hr/contract'

const props = defineProps<{
  /** 对话框可见性 */
  visible: boolean
  /** 合同信息（可选，传入时自动填充合同编号） */
  contract: EmployeeContract | null
}>()

const emit = defineEmits<{
  /** 更新可见性 */
  (e: 'update:visible', val: boolean): void
}>()

/* ===== 表单状态 ===== */
const formData = ref<ContractVerifyForm>({
  contractNo: '',
  hashPrefix: '',
})
const verifying = ref(false)
const verifyResult = ref<ContractVerifyResult | null>(null)

/* ===== 监听 visible 变化，重置表单 ===== */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      resetForm()
      // 如果传入了合同信息，自动填充合同编号
      if (props.contract) {
        formData.value.contractNo = props.contract.contractNo
        // 如果合同有哈希值，自动填充前8位
        if (props.contract.securityVerification?.hashValue) {
          formData.value.hashPrefix = props.contract.securityVerification.hashValue.slice(0, 8)
        }
      }
    }
  }
)

function resetForm() {
  formData.value = {
    contractNo: '',
    hashPrefix: '',
  }
  verifying.value = false
  verifyResult.value = null
}

/* ===== 验真 ===== */
async function handleVerify() {
  if (!formData.value.contractNo) {
    ElMessage.warning('请输入合同编号')
    return
  }
  // 哈希前8位应为16进制字符
  if (!formData.value.hashPrefix || formData.value.hashPrefix.length !== 8) {
    ElMessage.warning('请输入8位哈希前缀')
    return
  }
  if (!/^[0-9a-fA-F]{8}$/.test(formData.value.hashPrefix)) {
    ElMessage.warning('哈希前缀应为16进制字符（0-9, a-f）')
    return
  }

  verifying.value = true
  verifyResult.value = null

  try {
    const result = await contractApi.verifyContract(formData.value)
    verifyResult.value = result
    if (result.verified) {
      ElMessage.success('验真通过，合同真实有效')
    } else {
      ElMessage.warning(`验真失败：${result.failReason || '未知原因'}`)
    }
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '验真请求失败')
  } finally {
    verifying.value = false
  }
}

/* ===== 获取合同类型标签 ===== */
function getContractTypeLabel(type?: string): string {
  if (!type) return '-'
  return ContractTypeOptions.find(o => o.value === type)?.label || type
}

/* ===== 关闭对话框 ===== */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="合同验真"
    width="680px"
    class="fts-dialog--md"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <div class="verify-dialog">
      <!-- 说明 -->
      <el-alert
        type="info"
        :closable="false"
        style="margin-bottom: var(--fts-space-4);"
      >
        <template #title>
          <span>输入合同编号和哈希前8位，验证合同真伪。哈希值可在合同签署完成后的存证记录中获取。</span>
        </template>
      </el-alert>

      <!-- 验真表单 -->
      <el-form label-width="100px">
        <el-form-item label="合同编号" required>
          <el-input
            v-model="formData.contractNo"
            placeholder="请输入合同编号"
            clearable
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item label="哈希前8位" required>
          <el-input
            v-model="formData.hashPrefix"
            placeholder="请输入哈希值前8位（16进制）"
            maxlength="8"
            clearable
            style="width: 100%;"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="verifying"
            @click="handleVerify"
          >
            {{ verifying ? '验真中...' : '开始验真' }}
          </el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 验真结果 -->
      <div v-if="verifyResult" class="verify-result">
        <el-divider content-position="left">验真结果</el-divider>

        <!-- 验真成功 -->
        <div v-if="verifyResult.verified" class="result-success">
          <div class="result-icon">
            <el-icon :size="48" color="var(--fts-success)"><CircleCheckFilled /></el-icon>
          </div>
          <div class="result-text">合同真实有效</div>

          <el-descriptions :column="1" border size="small" style="margin-top: var(--fts-space-4);">
            <el-descriptions-item label="合同编号">{{ verifyResult.contractNo }}</el-descriptions-item>
            <el-descriptions-item label="员工姓名">{{ verifyResult.employeeName }}</el-descriptions-item>
            <el-descriptions-item label="合同类型">{{ getContractTypeLabel(verifyResult.contractType) }}</el-descriptions-item>
            <el-descriptions-item label="签订日期">{{ verifyResult.signDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="公司签署时间">{{ verifyResult.companySignTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="员工签署时间">{{ verifyResult.employeeSignTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="合同状态">
              <StatusTag status="active" label="已签署生效" size="small" />
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 验真失败 -->
        <div v-else class="result-fail">
          <div class="result-icon">
            <el-icon :size="48" color="var(--fts-error)"><CircleCloseFilled /></el-icon>
          </div>
          <div class="result-text">验真失败</div>
          <div class="result-reason">{{ verifyResult.failReason || '未知原因' }}</div>

          <el-alert
            type="warning"
            :closable="false"
            style="margin-top: var(--fts-space-4);"
          >
            <template #title>
              <span>可能原因：1）合同编号输入错误；2）哈希值不匹配（合同可能被篡改）；3）合同尚未完成签署。</span>
            </template>
          </el-alert>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else class="verify-empty">
        <el-icon :size="64" color="var(--fts-text-tertiary)"><Document /></el-icon>
        <div class="empty-text">请输入验真信息后点击"开始验真"</div>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.verify-dialog {
  min-height: 300px;
}

.verify-result {
  margin-top: var(--fts-space-2);
}

.result-success,
.result-fail {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--fts-space-4);
}

.result-icon {
  margin-bottom: var(--fts-space-3);
}

.result-text {
  font-size: var(--fts-font-size-xl);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-2);
}

.result-reason {
  font-size: var(--fts-font-size-lg);
  color: var(--fts-error);
  text-align: center;
}

.verify-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: var(--fts-space-6) 0;
  color: var(--fts-text-tertiary);
}

.empty-text {
  margin-top: var(--fts-space-3);
  font-size: var(--fts-font-size-lg);
}
</style>
