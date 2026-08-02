<script setup lang="ts">
/**
 * 合同签署状态对话框（管理端）
 *
 * 【管理端定位】
 * ✅ 查看签署进度和各方签署详情
 * ✅ 发起签署（初始化签署记录，推送到员工端）
 * ✅ 查看签署记录（签署时间、IP等）
 * ✅ 公司方电子签署（电子合同 + 公司未签时，打开 CompanySignDialog）
 * ❌ 不包含员工方签署动作（由员工端完成）
 * ❌ 不包含验证码发送（员工端由员工触发，公司端在公司签署对话框中触发）
 * ❌ 不包含员工拒绝签署（由员工端操作）
 */
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'
import CompanySignDialog from './CompanySignDialog.vue'
import { contractApi } from '@/api/hr/contract'
import type {
  EmployeeContract,
  SignatureStatusDTO,
  SignatoryType,
} from '@/types/hr/contract'
import {
  SignatureStatusOptions,
  SignatureStatusTagMap,
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
  /** 签署状态变更（如初始化签署后） */
  (e: 'success'): void
}>()

/* ===== 状态 ===== */
const loading = ref(false)
const signatureStatus = ref<SignatureStatusDTO | null>(null)
const initLoading = ref(false)

/* ===== 公司方签署对话框状态 ===== */
const companySignDialogVisible = ref(false)

/* ===== 监听 visible 变化，加载签署状态 ===== */
watch(
  () => props.visible,
  async (val) => {
    if (val && props.contract) {
      await loadSignatureStatus()
    }
  }
)

/* ===== 加载签署状态 ===== */
async function loadSignatureStatus() {
  if (!props.contract) return
  loading.value = true
  try {
    signatureStatus.value = await contractApi.getSignatureStatus(props.contract.id)
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '加载签署状态失败')
  } finally {
    loading.value = false
  }
}

/* ===== 发起签署（初始化签署记录，推送到员工端） ===== */
async function handleInitSignatures() {
  if (!props.contract) return
  initLoading.value = true
  try {
    await contractApi.initSignatures(props.contract.id)
    ElMessage.success('签署流程已发起，等待各方在员工端完成签署')
    await loadSignatureStatus()
    emit('success')
  } catch (error: unknown) {
    if (error instanceof Error) ElMessage.error(error.message || '发起签署失败')
  } finally {
    initLoading.value = false
  }
}

/* ===== 刷新签署状态 ===== */
async function handleRefresh() {
  await loadSignatureStatus()
  ElMessage.success('签署状态已刷新')
}

/* ===== 公司方签署（电子合同） ===== */
function handleCompanySign() {
  if (!props.contract) return
  if ((props.contract.carrier || 'electronic') !== 'electronic') {
    ElMessage.warning('仅电子合同支持公司方电子签署')
    return
  }
  companySignDialogVisible.value = true
}

/* ===== 公司签署成功回调 ===== */
async function onCompanySignSuccess() {
  companySignDialogVisible.value = false
  await loadSignatureStatus()
  emit('success')
}

/* ===== 判断公司方是否可签署（电子合同 + 公司未签） ===== */
function canCompanySign(): boolean {
  if (!props.contract) return false
  if ((props.contract.carrier || 'electronic') !== 'electronic') return false
  if (!signatureStatus.value) return false
  const companySigner = signatureStatus.value.signers.find(s => s.signerType === 'company')
  return !companySigner || companySigner.status === 'pending'
}

/* ===== 获取签署人类型标签 ===== */
function getSignerTypeLabel(type: SignatoryType): string {
  return type === 'company' ? '公司方' : '员工方'
}

/* ===== 获取签署状态标签 ===== */
function getStatusLabel(status: string): string {
  return SignatureStatusOptions.find(o => o.value === status)?.label || status
}

/* ===== 关闭对话框 ===== */
function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    title="合同签署状态"
    width="680px"
    class="fts-dialog--md"
    destroy-on-close
    lock-scroll="false"
    @update:model-value="handleClose"
  >
    <div v-loading="loading">
      <template v-if="signatureStatus">
        <!-- 合同基本信息 -->
        <el-descriptions :column="3" border size="small" style="margin-bottom: var(--fts-space-4);">
          <el-descriptions-item label="合同编号">{{ signatureStatus.contractNo }}</el-descriptions-item>
          <el-descriptions-item label="员工姓名">{{ signatureStatus.employeeName }}</el-descriptions-item>
          <el-descriptions-item label="签署进度">
            <span style="font-weight: 600;">{{ signatureStatus.signedCount }}/{{ signatureStatus.totalSigners }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 签署进度看板 -->
        <div class="progress-board">
          <div class="progress-header">
            <span class="progress-title">签署进度</span>
            <span class="progress-percent">{{ signatureStatus.progressPercent }}%</span>
          </div>
          <el-progress
            :percentage="signatureStatus.progressPercent"
            :stroke-width="20"
            :text-inside="true"
            :status="signatureStatus.progressPercent === 100 ? 'success' : ''"
          />
          <div class="progress-stats">
            <div class="stat-item">
              <span class="stat-label">总签署方</span>
              <span class="stat-value">{{ signatureStatus.totalSigners }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已签署</span>
              <span class="stat-value" style="color: var(--fts-success);">{{ signatureStatus.signedCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">待签署</span>
              <span class="stat-value" style="color: var(--fts-warning);">{{ signatureStatus.pendingCount }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">已拒绝</span>
              <span class="stat-value" style="color: var(--fts-error);">{{ signatureStatus.rejectedCount }}</span>
            </div>
          </div>
        </div>

        <!-- 各方签署详情 -->
        <div class="signers-section">
          <div class="section-title">各方签署详情</div>
          <div v-for="signer in signatureStatus.signers" :key="signer.signerType" class="signer-card">
            <div class="signer-info">
              <div class="signer-header">
                <span class="signer-type">{{ getSignerTypeLabel(signer.signerType) }}</span>
                <StatusTag
                  :status="SignatureStatusTagMap[signer.status]"
                  :label="getStatusLabel(signer.status)"
                  size="small"
                />
              </div>
              <div class="signer-name">{{ signer.signerName }}</div>
              <div v-if="signer.signTime" class="signer-time">
                签署时间：{{ signer.signTime }}
              </div>
              <div v-if="signer.signIp" class="signer-ip">
                签署IP：{{ signer.signIp }}
              </div>
              <div v-if="signer.status === 'pending' && signer.signerType === 'company'" class="signer-hint">
                公司方可在管理端完成电子签署
              </div>
              <div v-if="signer.status === 'pending' && signer.signerType === 'employee'" class="signer-hint">
                等待在员工端完成签署
              </div>
              <div v-if="signer.status === 'rejected' && signer.rejectReason" class="signer-hint" style="color: var(--fts-error);">
                拒绝原因：{{ signer.rejectReason }}
              </div>
            </div>
            <!-- 公司方签署按钮（电子合同 + 公司未签） -->
            <div v-if="signer.signerType === 'company' && signer.status === 'pending' && canCompanySign()" class="signer-action">
              <el-button type="primary" size="small" @click="handleCompanySign">公司签署</el-button>
            </div>
          </div>
        </div>

        <!-- 发起签署按钮（如果没有签署记录） -->
        <div v-if="signatureStatus.totalSigners === 0" class="init-section">
          <el-button type="primary" :loading="initLoading" @click="handleInitSignatures">
            发起签署
          </el-button>
          <div class="init-hint">发起后，签署链接将推送到员工端，由各方在员工端完成签署</div>
        </div>

        <!-- 刷新按钮 -->
        <div v-if="signatureStatus.totalSigners > 0 && signatureStatus.progressPercent < 100" class="refresh-section">
          <el-button @click="handleRefresh">刷新签署状态</el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="暂无签署状态数据" />
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>

    <!-- 公司方签署对话框（嵌套） -->
    <CompanySignDialog
      v-model:visible="companySignDialogVisible"
      :contract="contract"
      @success="onCompanySignSuccess"
    />
  </el-dialog>
</template>

<style scoped lang="scss">
.progress-board {
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-base);
  padding: var(--fts-space-4);
  margin-bottom: var(--fts-space-4);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fts-space-3);
}

.progress-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
}

.progress-percent {
  font-size: var(--fts-font-size-xl);
  font-weight: 700;
  color: var(--fts-primary);
}

.progress-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--fts-space-3);
  margin-top: var(--fts-space-4);
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: var(--fts-font-size-2xl);
  font-weight: 700;
  color: var(--fts-text-primary);
}

.signers-section {
  margin-bottom: var(--fts-space-4);
}

.section-title {
  font-size: var(--fts-font-size-lg);
  font-weight: 600;
  color: var(--fts-text-primary);
  margin-bottom: var(--fts-space-3);
}

.signer-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--fts-space-3) var(--fts-space-4);
  border: 1px solid var(--fts-border-secondary);
  border-radius: var(--fts-radius-base);
  margin-bottom: var(--fts-space-2);
  background: var(--fts-bg-card);
}

.signer-info {
  flex: 1;
}

.signer-action {
  flex-shrink: 0;
  margin-left: var(--fts-space-3);
}

.signer-header {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  margin-bottom: 4px;
}

.signer-type {
  font-weight: 600;
  color: var(--fts-text-primary);
}

.signer-name {
  font-size: var(--fts-font-size-lg);
  color: var(--fts-text-secondary);
}

.signer-time,
.signer-ip {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin-top: 2px;
}

.signer-hint {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin-top: 4px;
}

.init-section {
  text-align: center;
  padding: var(--fts-space-4);
}

.init-hint {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-tertiary);
  margin-top: var(--fts-space-2);
}

.refresh-section {
  text-align: center;
  padding: var(--fts-space-2);
}
</style>
