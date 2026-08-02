<script setup lang="ts">
/**
 * 供应商H5签署页面（移动端布局）
 *
 * 【流程】
 * 1. 通过 token 获取合同信息
 * 2. 实名认证（姓名+身份证+手机号+验证码）
 * 3. 查看合同内容
 * 4. 确认签署 或 拒绝签署
 *
 * 【路由】 /portal/sign?token=xxx
 * 【鉴权】 通过 token 访问，无需登录
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { supplierPortalApi } from '@/api/supplier-portal'
import type { SignLinkInfo, SupplierVerification } from '@/types/supplier-portal'
import { SignLinkStatusLabelMap } from '@/types/supplier-portal'

const route = useRoute()
const router = useRouter()
const token = computed(() => (route.query.token as string) || '')

/* ===== 页面状态 ===== */
const loading = ref(false)
const linkInfo = ref<SignLinkInfo | null>(null)
const errorMsg = ref('')
const currentStep = ref(0) // 0:合同信息 1:实名认证 2:签署确认

/* ===== 实名认证表单 ===== */
const verifyForm = ref({
  realName: '',
  idCard: '',
  phone: '',
  code: '',
})
const codeSending = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

/* ===== 签署/拒绝 ===== */
const signLoading = ref(false)
const rejectDialogVisible = ref(false)
const rejectReason = ref('')

/* ===== 认证结果 ===== */
const verification = ref<SupplierVerification | null>(null)

/* ===== 计算属性 ===== */
const canVerify = computed(() => {
  return (
    verifyForm.value.realName.length >= 2 &&
    /^\d{17}[\dXx]$/.test(verifyForm.value.idCard) &&
    /^1\d{10}$/.test(verifyForm.value.phone) &&
    verifyForm.value.code.length === 6
  )
})

const canSign = computed(() => !!verification.value && verification.value.verifyStatus === 'verified')

/* ===== 初始化：通过 token 加载合同信息 ===== */
async function init() {
  if (!token.value) {
    errorMsg.value = '缺少签署令牌，请检查链接是否完整'
    return
  }
  loading.value = true
  try {
    const info = await supplierPortalApi.getContractByToken(token.value)
    if (!info) {
      errorMsg.value = '签署链接无效或已过期，请联系采购方重新发送'
      return
    }
    if (info.status === 'signed') {
      linkInfo.value = info
      currentStep.value = 3 // 已签署状态
      return
    }
    if (info.status === 'expired') {
      errorMsg.value = '签署链接已过期，请联系采购方重新发送'
      return
    }
    if (info.status === 'rejected') {
      linkInfo.value = info
      currentStep.value = 4 // 已拒绝状态
      return
    }
    linkInfo.value = info
    // 标记为已查看
    await supplierPortalApi.viewContract(token.value)
  } catch (error: unknown) {
    if (error instanceof Error) errorMsg.value = error.message || '加载失败'
  } finally {
    loading.value = false
  }
}

/* ===== 发送验证码 ===== */
async function handleSendCode() {
  if (!verifyForm.value.phone || !/^1\d{10}$/.test(verifyForm.value.phone)) {
    ElMessage.warning('请输入正确的手机号')
    return
  }
  if (!token.value) {
    ElMessage.error('签署令牌缺失，请刷新页面或重新打开链接')
    return
  }
  if (countdown.value > 0) return
  codeSending.value = true
  try {
    // 调用后端真实接口生成验证码（后端会随机生成并存入缓存，未对接短信网关时通过日志输出）
    const result = await supplierPortalApi.sendVerifyCode(token.value, verifyForm.value.phone)
    if (result?.sent) {
      countdown.value = 60
      startCountdown()
      ElMessage.success(`验证码已发送至 ${result.phone}，10 分钟内有效`)
    } else {
      ElMessage.error('验证码发送失败，请稍后重试')
    }
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '验证码发送失败')
    } else {
      ElMessage.error('验证码发送失败')
    }
  } finally {
    codeSending.value = false
  }
}

function startCountdown() {
  clearCountdown()
  countdownTimer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearCountdown()
  }, 1000)
}

function clearCountdown() {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdown.value = 0
}

/* ===== 提交实名认证 ===== */
async function handleVerify() {
  if (!canVerify.value) {
    ElMessage.warning('请完整填写认证信息')
    return
  }
  loading.value = true
  try {
    const result = await supplierPortalApi.verify(
      token.value,
      verifyForm.value.realName,
      verifyForm.value.idCard,
      verifyForm.value.phone,
      verifyForm.value.code,
    )
    verification.value = result
    currentStep.value = 2
    ElMessage.success('实名认证通过')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '认证失败')
  } finally {
    loading.value = false
  }
}

/* ===== 确认签署 ===== */
async function handleSign() {
  if (!verification.value) return
  signLoading.value = true
  try {
    const result = await supplierPortalApi.signByToken(token.value, verification.value)
    linkInfo.value = result
    currentStep.value = 3
    ElMessage.success('合同签署成功')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '签署失败')
  } finally {
    signLoading.value = false
  }
}

/* ===== 拒绝签署 ===== */
async function handleReject() {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  signLoading.value = true
  try {
    const result = await supplierPortalApi.rejectByToken(token.value, rejectReason.value)
    linkInfo.value = result
    rejectDialogVisible.value = false
    currentStep.value = 4
    ElMessage.success('已拒绝签署')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '操作失败')
  } finally {
    signLoading.value = false
  }
}

onMounted(() => init())
</script>

<template>
  <div class="portal-page">
    <!-- 顶部品牌栏 -->
    <header class="portal-header">
      <div class="portal-brand">
        <el-icon :size="24"><Stamp /></el-icon>
        <span>食品溯源 · 电子签署</span>
      </div>
    </header>

    <!-- 加载中 -->
    <div v-if="loading" class="portal-loading">
      <el-icon class="is-loading" :size="32"><Loading /></el-icon>
      <p>正在加载合同信息...</p>
    </div>

    <!-- 错误提示 -->
    <div v-else-if="errorMsg" class="portal-error">
      <el-result icon="error" title="无法签署" :sub-title="errorMsg">
        <template #extra>
          <el-button type="primary" @click="router.push('/')">返回首页</el-button>
        </template>
      </el-result>
    </div>

    <!-- 主内容 -->
    <main v-else-if="linkInfo" class="portal-main">
      <!-- 步骤指示器 -->
      <div class="step-indicator">
        <div class="step-item" :class="{ active: currentStep >= 0, done: currentStep > 0 }">
          <div class="step-num">1</div>
          <div class="step-label">合同信息</div>
        </div>
        <div class="step-line" :class="{ active: currentStep > 0 }"></div>
        <div class="step-item" :class="{ active: currentStep >= 1, done: currentStep > 1 }">
          <div class="step-num">2</div>
          <div class="step-label">实名认证</div>
        </div>
        <div class="step-line" :class="{ active: currentStep > 1 }"></div>
        <div class="step-item" :class="{ active: currentStep >= 2 }">
          <div class="step-num">3</div>
          <div class="step-label">确认签署</div>
        </div>
      </div>

      <!-- Step 0: 合同信息 -->
      <section v-if="currentStep === 0" class="step-content">
        <div class="contract-card">
          <h2 class="contract-title">{{ linkInfo.contractName }}</h2>
          <div class="contract-meta">
            <div class="meta-row"><span class="meta-label">合同编号</span><span class="meta-value">{{ linkInfo.eContractNo }}</span></div>
            <div class="meta-row"><span class="meta-label">采购方</span><span class="meta-value">{{ linkInfo.createBy }}</span></div>
            <div class="meta-row"><span class="meta-label">供应商</span><span class="meta-value">{{ linkInfo.supplierName }}</span></div>
            <div class="meta-row"><span class="meta-label">过期时间</span><span class="meta-value">{{ linkInfo.expireTime }}</span></div>
          </div>
        </div>

        <div class="contract-preview">
          <div class="preview-header">合同正文预览</div>
          <div class="preview-body">
            <p>本合同由{{ linkInfo.createBy }}（以下简称"甲方"）与{{ linkInfo.supplierName }}（以下简称"乙方"）友好协商，就食品食材采购事宜达成如下协议：</p>
            <p><strong>第一条 采购内容</strong></p>
            <p>甲方因经营需要，向乙方采购生鲜食材、调味品等餐饮原材料，具体品类和数量以甲方采购订单为准。</p>
            <p><strong>第二条 质量标准</strong></p>
            <p>乙方提供的食材须符合《食品安全法》及相关国家标准，保证新鲜、无变质、无污染。</p>
            <p><strong>第三条 交付方式</strong></p>
            <p>乙方应按甲方订单要求，于指定时间将食材送达甲方指定地点，运输费用由乙方承担。</p>
            <p><strong>第四条 结算方式</strong></p>
            <p>双方按月结算，乙方于每月5日前提交上月送货清单，甲方核对后于15日前付款。</p>
            <p><strong>第五条 违约责任</strong></p>
            <p>任何一方违约，应向守约方支付违约金，并赔偿因此造成的损失。</p>
          </div>
        </div>

        <div class="action-bar">
          <el-button type="primary" size="large" class="action-btn" @click="currentStep = 1">下一步：实名认证</el-button>
        </div>
      </section>

      <!-- Step 1: 实名认证 -->
      <section v-else-if="currentStep === 1" class="step-content">
        <div class="verify-card">
          <h3 class="card-title">实名认证</h3>
          <p class="card-desc">为保障签署法律效力，请完成实名认证</p>

          <el-form label-position="top" class="verify-form">
            <el-form-item label="真实姓名">
              <el-input v-model="verifyForm.realName" placeholder="请输入真实姓名" size="large" />
            </el-form-item>
            <el-form-item label="身份证号">
              <el-input v-model="verifyForm.idCard" placeholder="请输入18位身份证号" size="large" maxlength="18" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="verifyForm.phone" placeholder="请输入手机号" size="large" maxlength="11" />
            </el-form-item>
            <el-form-item label="验证码">
              <div class="code-row">
                <el-input v-model="verifyForm.code" placeholder="6位验证码" size="large" maxlength="6" />
                <el-button :disabled="countdown > 0" :loading="codeSending" size="large" @click="handleSendCode">
                  {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <div class="action-bar">
          <el-button size="large" class="action-btn" @click="currentStep = 0">上一步</el-button>
          <el-button type="primary" size="large" class="action-btn" :disabled="!canVerify" @click="handleVerify">完成认证</el-button>
        </div>
      </section>

      <!-- Step 2: 确认签署 -->
      <section v-else-if="currentStep === 2" class="step-content">
        <div class="sign-confirm-card">
          <h3 class="card-title">签署确认</h3>

          <div class="verify-result">
            <el-icon :size="40" color="var(--fts-success)"><CircleCheckFilled /></el-icon>
            <div class="result-info">
              <div class="result-name">{{ verification?.realName }}</div>
              <div class="result-desc">{{ verification?.idCardMasked }} · {{ verification?.phone }}</div>
            </div>
          </div>

          <el-divider />

          <div class="sign-notice">
            <h4>签署声明</h4>
            <p>1. 本人确认已仔细阅读并理解上述合同全部内容。</p>
            <p>2. 本人确认所填实名信息真实有效。</p>
            <p>3. 本人对以电子签名方式签署本合同的法律效力予以认可。</p>
            <p>4. 签署后合同即产生法律约束力，双方应严格履行。</p>
          </div>
        </div>

        <div class="action-bar">
          <el-button size="large" class="action-btn" @click="currentStep = 1">上一步</el-button>
          <el-button type="danger" size="large" class="action-btn" @click="rejectDialogVisible = true">拒绝签署</el-button>
          <el-button type="primary" size="large" class="action-btn" :loading="signLoading" @click="handleSign">确认签署</el-button>
        </div>
      </section>

      <!-- Step 3: 签署成功 -->
      <section v-else-if="currentStep === 3" class="step-content">
        <el-result icon="success" title="合同签署成功" :sub-title="`签署时间：${linkInfo.signTime}`">
          <template #extra>
            <div class="sign-success-info">
              <p>合同编号：{{ linkInfo.eContractNo }}</p>
              <p>合同名称：{{ linkInfo.contractName }}</p>
              <p>签署方：{{ verification?.realName }}</p>
              <p class="tip">合同已生成电子存证，具备完整法律效力。如需查阅合同，请联系采购方。</p>
            </div>
          </template>
        </el-result>
      </section>

      <!-- Step 4: 已拒绝 -->
      <section v-else-if="currentStep === 4" class="step-content">
        <el-result icon="warning" title="已拒绝签署" :sub-title="`拒绝时间：${linkInfo.rejectTime}`">
          <template #extra>
            <div class="sign-success-info">
              <p>拒绝原因：{{ linkInfo.rejectReason }}</p>
              <p class="tip">如需重新签署，请联系采购方重新发送签署链接。</p>
            </div>
          </template>
        </el-result>
      </section>
    </main>

    <!-- 拒绝原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="拒绝签署" width="90%" append-to-body>
      <el-input v-model="rejectReason" type="textarea" :rows="4" placeholder="请填写拒绝原因（必填）" />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="signLoading" @click="handleReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.portal-page {
  min-height: 100vh;
  background: var(--fts-bg-page, #f5f7fa);
  display: flex;
  flex-direction: column;
}

.portal-header {
  background: var(--fts-primary, #409eff);
  color: #fff;
  padding: 12px 16px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.portal-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  max-width: 480px;
  margin: 0 auto;
}

.portal-loading,
.portal-error {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 16px;
  text-align: center;

  p {
    margin-top: 16px;
    color: var(--fts-text-secondary, #909399);
  }
}

.portal-main {
  flex: 1;
  max-width: 480px;
  width: 100%;
  margin: 0 auto;
  padding: 16px;
}

/* 步骤指示器 */
.step-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
  padding: 16px 0;
}

.step-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;

  .step-num {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    background: var(--fts-bg-secondary, #e4e7ed);
    color: var(--fts-text-tertiary, #909399);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 14px;
    font-weight: 600;
    transition: all 0.3s;
  }

  .step-label {
    font-size: 12px;
    color: var(--fts-text-tertiary, #909399);
  }

  &.active .step-num {
    background: var(--fts-primary, #409eff);
    color: #fff;
  }

  &.active .step-label {
    color: var(--fts-primary, #409eff);
    font-weight: 500;
  }

  &.done .step-num {
    background: var(--fts-success, #67c23a);
    color: #fff;
  }
}

.step-line {
  width: 40px;
  height: 2px;
  background: var(--fts-bg-secondary, #e4e7ed);
  margin: 0 8px;
  margin-bottom: 20px;
  transition: all 0.3s;

  &.active {
    background: var(--fts-success, #67c23a);
  }
}

.step-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* 合同卡片 */
.contract-card,
.contract-preview,
.verify-card,
.sign-confirm-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 16px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.contract-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 16px 0;
  color: var(--fts-text-primary, #303133);
  text-align: center;
}

.contract-meta {
  .meta-row {
    display: flex;
    justify-content: space-between;
    padding: 8px 0;
    border-bottom: 1px solid var(--fts-border-color, #ebeef5);

    &:last-child { border-bottom: none; }

    .meta-label {
      color: var(--fts-text-tertiary, #909399);
      font-size: 14px;
    }

    .meta-value {
      color: var(--fts-text-primary, #303133);
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.preview-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--fts-text-primary, #303133);
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 2px solid var(--fts-primary, #409eff);
}

.preview-body {
  font-size: 14px;
  line-height: 1.8;
  color: var(--fts-text-secondary, #606266);

  p {
    margin: 8px 0;
  }
}

/* 认证表单 */
.card-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px 0;
  color: var(--fts-text-primary, #303133);
}

.card-desc {
  font-size: 13px;
  color: var(--fts-text-tertiary, #909399);
  margin: 0 0 20px 0;
}

.code-row {
  display: flex;
  gap: 8px;
  width: 100%;

  .el-input { flex: 1; }
}

/* 签署确认 */
.verify-result {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 0;

  .result-info {
    flex: 1;
  }

  .result-name {
    font-size: 16px;
    font-weight: 600;
    color: var(--fts-text-primary, #303133);
  }

  .result-desc {
    font-size: 13px;
    color: var(--fts-text-tertiary, #909399);
    margin-top: 4px;
  }
}

.sign-notice {
  h4 {
    font-size: 14px;
    font-weight: 600;
    margin: 0 0 12px 0;
    color: var(--fts-text-primary, #303133);
  }

  p {
    font-size: 13px;
    line-height: 1.8;
    color: var(--fts-text-secondary, #606266);
    margin: 6px 0;
  }
}

/* 操作栏 */
.action-bar {
  display: flex;
  gap: 12px;
  padding: 16px 0;

  .action-btn {
    flex: 1;
  }
}

.sign-success-info {
  text-align: left;
  font-size: 14px;
  line-height: 2;

  p { margin: 4px 0; }

  .tip {
    color: var(--fts-text-tertiary, #909399);
    font-size: 13px;
    margin-top: 12px;
  }
}
</style>
