<script setup lang="ts">
/**
 * 公司方签署对话框（电子合同，3步骤）
 *
 * 【流程】
 * Step 1: 信息确认 — 合同摘要 + 公司方信息 + 印章选择（从印章库选择已授权印章）
 * Step 2: 安全验证 — 发送验证码 + 输入6位验证码（5分钟有效）
 * Step 3: 最终确认 — 签署声明 + 勾选同意 + 确认签署
 *
 * 【提交】调用 contractApi.companySign，携带 verifyCode + sealImage + agreed
 * 【安全】记录签署IP、设备信息、SHA-256哈希（由后端生成）
 * 【闭环】签署成功后调用 sealApi.recordUsage 记录印章使用
 */
import { ref, computed, watch, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import SealSelector from '@/components/business/SealSelector.vue'
import { contractApi } from '@/api/hr/contract'
import { sealApi } from '@/api/seal'
import type {
  EmployeeContract,
  CompanySignForm,
} from '@/types/hr/contract'
import type { SealInfo } from '@/types/seal'
import {
  ContractTypeOptions,
  ContractCarrierTagMap,
} from '@/types/hr/contract'

const props = defineProps<{
  /** 对话框可见性 */
  visible: boolean
  /** 合同信息 */
  contract: EmployeeContract | null
}>()

const emit = defineEmits<{
  /** 更新可见性 */
  (e: 'update:visible', val: boolean): void
  /** 签署成功 */
  (e: 'success'): void
}>()

/* ===== 步骤状态 ===== */
const currentStep = ref(0)
const loading = ref(false)
const signLoading = ref(false)

/* ===== Step 1: 印章选择（从印章库选择已授权印章） ===== */
const selectedSealId = ref<string>('')
const selectedSeal = ref<SealInfo | null>(null)

/* ===== Step 2: 验证码 ===== */
const verifyCode = ref<string>('')
const codeExpireTime = ref<string>('')
const countdown = ref(0)
const codeSending = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

/* ===== Step 3: 同意声明 ===== */
const agreed = ref(false)

/* ===== 计算属性 ===== */
const contractTypeLabel = computed(() => {
  if (!props.contract) return ''
  return ContractTypeOptions.find(o => o.value === props.contract!.contractType)?.label || ''
})

const canGoStep2 = computed(() => {
  return !!selectedSealId.value && !!selectedSeal.value
})

/**
 * 验证码仅校验长度（6位），最终校验由后端 companySign 接口完成
 * 安全考虑：不在前端保存验证码明文，避免被 DevTools 绕过
 */
const canGoStep3 = computed(() => {
  return verifyCode.value.length === 6
})

const canSubmit = computed(() => {
  return agreed.value && !!selectedSeal.value && verifyCode.value.length === 6
})

/* ===== 监听 visible 变化，重置表单 ===== */
watch(
  () => props.visible,
  (val) => {
    if (val) {
      resetForm()
    } else {
      clearCountdown()
    }
  }
)

/* ===== 重置表单 ===== */
function resetForm() {
  currentStep.value = 0
  selectedSealId.value = ''
  selectedSeal.value = null
  verifyCode.value = ''
  codeExpireTime.value = ''
  countdown.value = 0
  agreed.value = false
  loading.value = false
  signLoading.value = false
  clearCountdown()
}

/* ===== 印章选择回调 ===== */
function onSealSelect(seal: SealInfo) {
  selectedSeal.value = seal
}

/* ===== Step 1 → Step 2 ===== */
function goToStep2() {
  if (!canGoStep2.value) {
    ElMessage.warning('请先从印章库选择公司印章')
    return
  }
  currentStep.value = 1
}

/* ===== 发送验证码 ===== */
async function handleSendCode() {
  if (!props.contract) return
  if (countdown.value > 0) return
  codeSending.value = true
  try {
    const res = await contractApi.sendVerifyCode(props.contract.id, 'company')
    codeExpireTime.value = res.expireTime
    countdown.value = 60
    startCountdown()
    ElMessage.success('验证码已发送，请查收短信（5分钟内有效）')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '验证码发送失败')
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  clearCountdown()
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearCountdown()
    }
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

/* ===== Step 2 → Step 3 ===== */
function goToStep3() {
  if (!canGoStep3.value) {
    ElMessage.warning('请输入正确的6位验证码')
    return
  }
  currentStep.value = 2
}

/* ===== 获取签署设备信息（IP 由后端从请求中获取，前端不提交） ===== */
function getSignDevice(): string {
  return navigator.userAgent
}

/* ===== 组件卸载时清理定时器，防止内存泄漏 ===== */
onUnmounted(() => {
  clearCountdown()
})

/* ===== 提交签署 ===== */
async function handleSubmit() {
  if (!props.contract || !selectedSeal.value) return
  if (!canSubmit.value) {
    ElMessage.warning('请完成所有步骤并勾选同意声明')
    return
  }
  signLoading.value = true
  try {
    const formData: CompanySignForm = {
      verifyCode: verifyCode.value,
      sealImage: selectedSeal.value.sealImage,
      agreed: agreed.value,
      // 签署 IP 由后端从 HTTP 请求中获取，前端仅提交设备信息
      signDevice: getSignDevice(),
    }
    await contractApi.companySign(props.contract.id, formData)
    // 数据闭环：记录印章使用
    try {
      await sealApi.recordUsage(
        selectedSeal.value.sealId,
        'hr_contract',
        props.contract.id,
        props.contract.contractNo,
        '公司管理员',
      )
    } catch {
      // 印章使用记录失败不影响签署结果，仅记录日志
      console.warn('印章使用记录写入失败')
    }
    ElMessage.success('公司方签署成功')
    emit('success')
    handleClose()
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '签署失败')
  } finally {
    signLoading.value = false
  }
}

/* ===== 关闭对话框 ===== */
function handleClose() {
  clearCountdown()
  emit('update:visible', false)
}

/* ===== 上一步 ===== */
function handlePrev() {
  if (currentStep.value > 0) {
    currentStep.value--
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="公司方电子签署"
    width="960px"
    class="fts-dialog--lg"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <div v-if="contract" class="company-sign-dialog">
      <!-- 步骤条 -->
      <el-steps :active="currentStep" align-center finish-status="success" style="margin-bottom: var(--fts-space-5);">
        <el-step title="信息确认" description="合同摘要与印章" />
        <el-step title="安全验证" description="短信验证码" />
        <el-step title="最终确认" description="签署声明" />
      </el-steps>

      <!-- Step 1: 信息确认 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-descriptions :column="2" border size="small" style="margin-bottom: var(--fts-space-4);">
          <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="员工姓名">{{ contract.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="合同类型">{{ contractTypeLabel }}</el-descriptions-item>
          <el-descriptions-item label="合同载体">
            <StatusTag
              :status="ContractCarrierTagMap.electronic"
              label="电子合同"
              size="small"
            />
          </el-descriptions-item>
          <el-descriptions-item label="开始日期">{{ contract.startDate }}</el-descriptions-item>
          <el-descriptions-item label="结束日期">{{ contract.endDate || '无固定期限' }}</el-descriptions-item>
        </el-descriptions>

        <el-divider content-position="left">公司方印章</el-divider>

        <div class="seal-select-section">
          <SealSelector
            scene="hr_contract"
            v-model="selectedSealId"
            @select="onSealSelect"
          />
        </div>

        <el-alert
          type="info"
          :closable="false"
          style="margin-top: var(--fts-space-4);"
        >
          <template #title>
            <span>印章从公司印章库中选择，仅显示已授权用于人事合同的印章。签署后印章与合同内容SHA-256哈希一并存证，确保签署不可抵赖。</span>
          </template>
        </el-alert>
      </div>

      <!-- Step 2: 安全验证 -->
      <div v-show="currentStep === 1" class="step-content">
        <el-alert
          type="warning"
          :closable="false"
          style="margin-bottom: var(--fts-space-4);"
        >
          <template #title>
            <span>为保障签署安全，需向公司管理员手机发送验证码进行身份核验。验证码5分钟内有效。</span>
          </template>
        </el-alert>

        <el-form label-width="120px">
          <el-form-item label="管理员手机">
            <span style="color: var(--fts-text-primary);">138****8888（公司管理员）</span>
          </el-form-item>
          <el-form-item label="验证码">
            <div class="verify-code-row">
              <el-input
                v-model="verifyCode"
                placeholder="请输入6位验证码"
                maxlength="6"
                style="width: 200px;"
              />
              <el-button
                type="primary"
                :loading="codeSending"
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s 后重发` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item v-if="codeExpireTime" label="过期时间">
            <span style="color: var(--fts-text-secondary);">{{ codeExpireTime }}</span>
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 3: 最终确认 -->
      <div v-show="currentStep === 2" class="step-content">
        <el-alert
          type="warning"
          :closable="false"
          style="margin-bottom: var(--fts-space-4);"
        >
          <template #title>
            <span>请仔细阅读以下签署声明，勾选同意后方可完成签署。</span>
          </template>
        </el-alert>

        <div class="sign-declaration">
          <div class="declaration-title">电子签署声明</div>
          <div class="declaration-content">
            <p>1. 本公司确认合同「{{ contract.contractNo }}」（员工：{{ contract.employeeName }}）内容已经审核，符合公司规章制度和法律法规要求。</p>
            <p>2. 本公司确认所选印章为公司合法有效的电子印章，与实体印章具有同等法律效力。</p>
            <p>3. 本公司确认通过验证码核验身份，签署行为系公司真实意思表示，不可撤销。</p>
            <p>4. 签署后合同内容将通过SHA-256哈希算法生成数字指纹，任何篡改均可被检测。</p>
            <p>5. 签署记录（含IP地址、设备信息、签署时间）将作为司法存证保留。</p>
          </div>
        </div>

        <div class="agree-section">
          <el-checkbox v-model="agreed">
            我已阅读并同意上述签署声明，确认代表公司完成电子签署
          </el-checkbox>
        </div>

        <el-descriptions :column="2" border size="small" style="margin-top: var(--fts-space-4);">
          <el-descriptions-item label="签署方">公司方（甲方）</el-descriptions-item>
          <el-descriptions-item label="签署方式">电子签署</el-descriptions-item>
          <el-descriptions-item label="签署IP">
            <span class="sign-ip-hint">以服务端记录为准</span>
          </el-descriptions-item>
          <el-descriptions-item label="签署设备">{{ getSignDevice() }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button v-if="currentStep > 0" @click="handlePrev">上一步</el-button>
        <el-button v-if="currentStep === 0" type="primary" :disabled="!canGoStep2" @click="goToStep2">下一步</el-button>
        <el-button v-if="currentStep === 1" type="primary" :disabled="!canGoStep3" @click="goToStep3">下一步</el-button>
        <el-button v-if="currentStep === 2" type="primary" :loading="signLoading" :disabled="!canSubmit" @click="handleSubmit">确认签署</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.company-sign-dialog {
  min-height: 400px;
}

.step-content {
  padding: var(--fts-space-2) var(--fts-space-4);
}

.seal-select-section {
  padding: var(--fts-space-3) 0;
}

.sign-ip-hint {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
}

.verify-code-row {
  display: flex;
  gap: var(--fts-space-2);
  align-items: center;
}

.sign-declaration {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-base);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.declaration-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
  padding-bottom: var(--fts-space-2);
  border-bottom: 1px solid var(--fts-border-secondary);
}

.declaration-content {
  p {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: 1.8;
    margin: 0 0 var(--fts-space-2) 0;
  }
}

.agree-section {
  padding: var(--fts-space-3) 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--fts-space-2);
}
</style>
