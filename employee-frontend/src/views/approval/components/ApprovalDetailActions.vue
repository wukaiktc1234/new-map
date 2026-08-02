<script setup lang="ts">
/**
 * ApprovalDetailActions - 审批操作区
 * 包含：操作按钮（通过/驳回/撤回）、驳回弹窗、结果提示横幅
 */
import { ElMessageBox } from 'element-plus'
import type { ApprovalStatus } from '@/types/approval'

const props = defineProps<{
  canApprove: boolean
  canWithdraw: boolean
  status: ApprovalStatus
  actionLoading: boolean
}>()

const emit = defineEmits<{
  (e: 'approve'): void
  (e: 'reject', reason: string): void
  (e: 'withdraw'): void
}>()

const showRejectDialog = ref(false)
const rejectComment = ref('')

function handleReject() {
  if (!rejectComment.value.trim()) return
  emit('reject', rejectComment.value.trim())
  showRejectDialog.value = false
  rejectComment.value = ''
}

async function handleWithdraw() {
  try {
    await ElMessageBox.confirm('确认撤回此申请？', '撤回确认', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    emit('withdraw')
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <!-- 操作区 -->
  <section v-if="canApprove || canWithdraw" class="action-section">
    <template v-if="canApprove">
      <button class="act-btn act-btn--approve" :disabled="actionLoading" @click="$emit('approve')">
        通过
      </button>
      <button class="act-btn act-btn--reject" :disabled="actionLoading" @click="showRejectDialog = true">
        驳回
      </button>
    </template>
    <template v-if="canWithdraw && !canApprove">
      <button class="act-btn act-btn--withdraw" @click="handleWithdraw">
        撤回申请
      </button>
    </template>
  </section>

  <!-- 已完成状态提示 -->
  <section v-if="status === 'approved' || status === 'rejected' || status === 'cancelled'" class="result-banner" :class="`result-banner--${status}`">
    <span v-if="status === 'approved'">审批已通过</span>
    <span v-else-if="status === 'rejected'">审批已驳回</span>
    <span v-else>申请已撤回</span>
  </section>

  <!-- 驳回弹窗 -->
  <el-dialog v-model="showRejectDialog" title="驳回审批" width="400px" :close-on-click-modal="false" destroy-on-close>
    <div class="reject-form">
      <label class="rf-label">驳回原因 *</label>
      <el-input
        v-model="rejectComment"
        type="textarea"
        placeholder="请说明驳回原因..."
        :rows="3"
        maxlength="200"
        show-word-limit
      />
    </div>
    <template #footer>
      <el-button @click="showRejectDialog = false">取消</el-button>
      <el-button type="danger" :disabled="!rejectComment.trim()" @click="handleReject">有疑问</el-button>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
// ========== 操作按钮 / 结果提示 ==========
.action-section {
  display: flex;
  gap: var(--fts-space-3);
  margin-bottom: var(--fts-space-4);
}

.act-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s ease;

  &--approve {
    background: var(--fts-success);
    color: var(--fts-text-on-primary);
    border-color: var(--fts-success);
    &:hover { opacity: 0.9; }
    &:active { transform: scale(0.97); }
  }

  &--reject {
    background: transparent;
    color: var(--fts-error);
    border-color: var(--fts-error);
    &:hover { background: rgba(var(--fts-error-rgb), 0.06); }
    &:active { transform: scale(0.97); }
  }

  &--withdraw {
    background: transparent;
    color: var(--fts-warning);
    border-color: rgba(var(--fts-warning-rgb), 0.40);
    &:hover { background: rgba(var(--fts-warning-rgb), 0.06); }
  }

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
}

.result-banner {
  padding: var(--fts-space-3) var(--fts-space-4);
  border-radius: var(--fts-radius-md);
  text-align: center;
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  margin-bottom: var(--fts-space-4);

  &--approved {
    background: rgba(var(--fts-success-rgb), 0.08);
    color: var(--fts-success);
    border: 1px solid rgba(var(--fts-success-rgb), 0.20);
  }

  &--rejected {
    background: rgba(var(--fts-error-rgb), 0.08);
    color: var(--fts-error);
    border: 1px solid rgba(var(--fts-error-rgb), 0.20);
  }

  &--cancelled {
    background: var(--fts-bg-tertiary);
    color: var(--fts-text-tertiary);
    border: 1px solid var(--fts-border-primary);
  }
}

.reject-form {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-2);
}

.rf-label {
  font-size: var(--fts-font-size-sm);
  font-weight: 600;
  color: var(--fts-text-primary);
}
</style>
