<script setup lang="ts">
/**
 * 电子合同 - 签署确认对话框（3步向导）
 *
 * 步骤1：信息确认 + 印章选择
 * 步骤2：安全验证（验证码）
 * 步骤3：最终确认 + 执行签署
 *
 * 内部状态自管理，父组件仅传入 visible + target，签署完成后 emit('success')
 */
import { ref, reactive, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import SealSelector from '@/components/business/SealSelector.vue'
import { electronicContractApi } from '@/api/purchase'
import { electronicContractConverter } from '@/api/purchase/converters'
import { sealApi } from '@/api/seal'
import type { SealInfo } from '@/types/seal'
import { usePermissionStore } from '@/stores/permission'
import type { ElectronicContractInfo } from '@/types/purchase-electronic-contract'

interface Props {
  /** 对话框可见性（v-model） */
  visible: boolean
  /** 签署目标合同 */
  target: ElectronicContractInfo | null
  /** 对话框标题 */
  title: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:visible': [value: boolean]
  /** 签署成功 */
  success: []
}>()

const permissionStore = usePermissionStore()

/** 当前步骤（0/1/2） */
const signStep = ref(0)
/** 签署表单数据 */
const signFormData = reactive({
  confirmedBy: '',
  remark: '',
  verifyCode: '',
  confirmed: false,
})
/** 验证码倒计时（秒） */
const verifyCodeCountdown = ref(0)
/** 验证码倒计时定时器引用（用于组件卸载或对话框关闭时清理，避免内存泄漏） */
let verifyCodeTimer: ReturnType<typeof setInterval> | null = null
/** 已选印章ID */
const selectedSealId = ref<string>('')
/** 已选印章详情 */
const selectedSeal = ref<SealInfo | null>(null)
/** 签署中加载态 */
const signing = ref(false)

/** 清理验证码倒计时定时器 */
function clearVerifyCodeTimer() {
  if (verifyCodeTimer !== null) {
    clearInterval(verifyCodeTimer)
    verifyCodeTimer = null
  }
  verifyCodeCountdown.value = 0
}

/** 监听 visible 变化，打开时初始化状态，关闭时清理定时器 */
watch(() => props.visible, (open) => {
  if (open && props.target) {
    signStep.value = 0
    Object.assign(signFormData, {
      confirmedBy: permissionStore.userInfo?.fullName || permissionStore.userInfo?.username || '',
      remark: '',
      verifyCode: '',
      confirmed: false,
    })
    selectedSealId.value = ''
    selectedSeal.value = null
  } else if (!open) {
    // 对话框关闭时清理定时器，避免内存泄漏
    clearVerifyCodeTimer()
  }
})

// 组件卸载时清理定时器
onBeforeUnmount(() => {
  clearVerifyCodeTimer()
})

/** 印章选择回调 */
function onSealSelect(seal: SealInfo) {
  selectedSeal.value = seal
}

/** 关闭对话框 */
function closeDialog() {
  emit('update:visible', false)
}

/** 发送验证码（模拟） */
function sendVerifyCode() {
  // 先清理已有的定时器，避免重复发送时产生多个定时器
  clearVerifyCodeTimer()
  verifyCodeCountdown.value = 60
  ElMessage.success('验证码已发送（模拟：123456）')
  verifyCodeTimer = setInterval(() => {
    verifyCodeCountdown.value--
    if (verifyCodeCountdown.value <= 0) {
      clearVerifyCodeTimer()
    }
  }, 1000)
}

/** 执行签署 */
async function executeSign() {
  if (!props.target) return
  if (!signFormData.confirmedBy || !signFormData.verifyCode) {
    ElMessage.warning('请填写签署确认人和验证码')
    return
  }
  if (!selectedSealId.value || !selectedSeal.value) {
    ElMessage.warning('请选择公司印章')
    return
  }
  // 模拟验证码校验
  if (signFormData.verifyCode !== '123456') {
    ElMessage.error('验证码错误')
    return
  }
  signing.value = true
  try {
    if (props.target.status === 'draft') {
      await electronicContractApi.initiateSigning(props.target.eContractId, { remark: signFormData.remark })
    } else {
      await electronicContractApi.confirmSigned(props.target.eContractId, { confirmedBy: signFormData.confirmedBy, remark: signFormData.remark })
    }
    // 数据闭环：记录印章使用
    try {
      await sealApi.recordUsage(
        selectedSeal.value.sealId,
        'electronic_contract',
        props.target.eContractId,
        props.target.eContractNo,
        signFormData.confirmedBy,
      )
    } catch {
      console.warn('印章使用记录写入失败')
    }
    ElMessage.success('签署操作成功')
    emit('update:visible', false)
    emit('success')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    signing.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="title"
    width="960px"
    class="fts-dialog--lg"
    destroy-on-close
    lock-scroll="false"
    :close-on-click-modal="false"
    @update:model-value="emit('update:visible', $event)"
  >
    <el-steps :active="signStep" finish-status="success" align-center style="margin-bottom: 24px;">
      <el-step title="信息确认" />
      <el-step title="安全验证" />
      <el-step title="确认签署" />
    </el-steps>

    <!-- 步骤1：信息确认 -->
    <div v-if="signStep === 0" class="sign-step-content">
      <el-alert type="warning" :closable="false" style="margin-bottom: 16px;">
        <template #title>请仔细核对以下合同信息，确认无误后继续</template>
      </el-alert>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="合同名称">{{ target?.contractName }}</el-descriptions-item>
        <el-descriptions-item label="合同编号">{{ target?.eContractNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ target?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="合同金额">{{ target ? electronicContractConverter.formatYuan(target.totalAmount) : '' }} 元</el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ target?.expireDate }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <StatusTag v-if="target" :status="electronicContractConverter.toStatusTagStatus(target.status)" :label="electronicContractConverter.toStatusLabel(target.status)" size="small" />
        </el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">公司方印章</el-divider>
      <SealSelector
        scene="electronic_contract"
        v-model="selectedSealId"
        @select="onSealSelect"
      />
      <el-alert type="info" :closable="false" style="margin-top: 12px;">
        <template #title>印章从公司印章库中选择，仅显示已授权用于电子合同的印章。签署后印章使用记录将自动归档。</template>
      </el-alert>
    </div>

    <!-- 步骤2：安全验证 -->
    <div v-if="signStep === 1" class="sign-step-content">
      <el-alert type="info" :closable="false" style="margin-bottom: 16px;">
        <template #title>为确保签署安全，请完成以下验证</template>
      </el-alert>
      <el-form label-width="100px">
        <el-form-item label="签署确认人" required>
          <el-input v-model="signFormData.confirmedBy" placeholder="请输入签署确认人姓名" />
        </el-form-item>
        <el-form-item label="签署备注">
          <el-input v-model="signFormData.remark" type="textarea" :rows="2" placeholder="请输入签署备注（选填）" />
        </el-form-item>
        <el-form-item label="验证码" required>
          <div style="display: flex; gap: 8px;">
            <el-input v-model="signFormData.verifyCode" placeholder="请输入验证码" style="flex: 1;" />
            <el-button @click="sendVerifyCode" :disabled="verifyCodeCountdown > 0">
              {{ verifyCodeCountdown > 0 ? `${verifyCodeCountdown}s后重发` : '获取验证码' }}
            </el-button>
          </div>
          <div style="color: var(--fts-text-tertiary); font-size: 12px; margin-top: 4px;">验证码将发送至签署人手机（模拟）</div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 步骤3：最终确认 -->
    <div v-if="signStep === 2" class="sign-step-content">
      <el-alert type="success" :closable="false" style="margin-bottom: 16px;">
        <template #title>请最终确认签署信息</template>
      </el-alert>
      <el-descriptions :column="1" border size="small">
        <el-descriptions-item label="合同名称">{{ target?.contractName }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ target?.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="签署确认人">{{ signFormData.confirmedBy }}</el-descriptions-item>
        <el-descriptions-item label="签署备注">{{ signFormData.remark || '无' }}</el-descriptions-item>
      </el-descriptions>
      <el-checkbox v-model="signFormData.confirmed" style="margin-top: 16px;">
        我已阅读并确认合同内容，同意完成签署
      </el-checkbox>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button v-if="signStep > 0" @click="signStep--">上一步</el-button>
        <el-button v-if="signStep < 2" type="primary" :disabled="signStep === 0 && !selectedSealId" @click="signStep++">下一步</el-button>
        <el-button v-if="signStep === 2" type="primary" :loading="signing" :disabled="!signFormData.confirmed || !signFormData.confirmedBy || !signFormData.verifyCode || !selectedSealId" @click="executeSign">确认签署</el-button>
        <el-button @click="closeDialog">取消</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.sign-step-content {
  min-height: 200px;
  padding: var(--fts-space-2) 0;
}
</style>
