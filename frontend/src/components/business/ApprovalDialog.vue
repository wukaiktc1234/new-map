<script setup lang="ts">
/**
 * ApprovalDialog - 通用审批对话框
 *
 * 功能：
 * 1. 显示当前审批节点信息（节点名称、审批人类型）
 * 2. 审批意见输入
 * 3. 通过/驳回操作
 * 4. 操作后通知父组件
 *
 * 使用方式：
 * <ApprovalDialog
 *   v-model:visible="dialogVisible"
 *   business-id="xxx"
 *   business-type="purchase_order"
 *   current-node-name="一级审批"
 *   @approved="handleApproved"
 *   @rejected="handleRejected"
 * />
 */
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { approvalWorkflowApi } from '@/api/approval'
import { approvalWorkflowConverter } from '@/api/approval/converters'
import { SINGLE_STORE_APPROVAL_FLOWS } from '@/types/approval-workflow'
import type { ApprovalCurrentNode, ApproverType } from '@/types/approval-workflow'

interface Props {
  visible: boolean
  businessId: string
  businessType: string
  currentNodeName?: string
  /** 业务单号（如采购订单号/申请单号），用于审批身份确认，防止审批错单 */
  businessNo?: string
  /** 业务标题/名称（可选展示） */
  businessTitle?: string
  /**
   * 自定义审批通过处理器（推荐传入）
   * 若提供，将替代默认的 mock approvalWorkflowApi.approve 调用，
   * 真正调用后端业务 API 完成状态变更。
   * 若不提供，则回退到 mock 行为（仅用于演示，不会真正更新业务状态）。
   */
  approveHandler?: (comment: string) => Promise<void>
  /**
   * 自定义审批驳回处理器（推荐传入）
   * 同 approveHandler，用于驳回时调用真实后端 API。
   */
  rejectHandler?: (comment: string) => Promise<void>
}

const props = withDefaults(defineProps<Props>(), {
  currentNodeName: '',
  approveHandler: undefined,
  rejectHandler: undefined,
})

const emit = defineEmits<{
  'update:visible': [value: boolean]
  'approved': []
  'rejected': []
}>()

/** 审批意见 */
const comment = ref('')

/** 提交加载状态 */
const submitting = ref(false)

/** 当前审批节点信息（从后端获取） */
const currentNodeInfo = ref<ApprovalCurrentNode | null>(null)

/** 加载当前节点信息 */
const loadingNode = ref(false)

/** 对话框绑定值 */
const dialogVisible = computed({
  get: () => props.visible,
  set: (val: boolean) => emit('update:visible', val),
})

/** 审批人类型标签 */
const approverTypeLabel = computed(() => {
  if (!currentNodeInfo.value) return ''
  return approvalWorkflowConverter.toApproverTypeLabel(currentNodeInfo.value.approverType as ApproverType)
})

/** 显示的节点名称：优先使用 props，其次使用接口返回 */
const displayNodeName = computed(() => {
  return props.currentNodeName || currentNodeInfo.value?.nodeName || '当前节点'
})

/** 业务类型中文标签 */
const businessTypeLabel = computed(() => {
  const map: Record<string, string> = {
    purchase_order: '采购订单审批',
    purchase_request: '采购申请审批',
    reimbursement: '费用报销审批',
    certificate_renewal: '证件续期审批',
    recruitment: '招聘审批',
    leave: '请假审批',
    overtime: '加班审批',
    salary: '薪资审批',
    settlement: '结算审批',
  }
  return map[props.businessType] || props.businessType || '业务审批'
})

/** 单据编号文本：优先展示业务单号（采购单号/申请单号），兜底数字 ID */
const businessIdText = computed(() => {
  if (props.businessNo) return `${businessNoLabel.value}：${props.businessNo}`
  return props.businessId ? `单据编号：${props.businessId}` : ''
})

/** 业务单号前缀标签 */
const businessNoLabel = computed(() => {
  const map: Record<string, string> = {
    purchase_order: '采购单号',
    purchase_request: '申请单号',
    reimbursement: '报销单号',
    certificate_renewal: '证件单号',
    recruitment: '招聘单号',
  }
  return map[props.businessType] || '业务单号'
})

/** 审批意见提示 */
const commentHint = computed(() => '通过可不填；驳回时请填写原因')

/** 监听对话框打开，获取当前节点信息 */
watch(() => props.visible, async (newVal) => {
  if (newVal) {
    comment.value = ''
    currentNodeInfo.value = null
    await fetchCurrentNode()
  }
})

/** 获取当前审批节点信息 */
async function fetchCurrentNode() {
  if (!props.businessId || !props.businessType) return
  loadingNode.value = true
  try {
    const data = await approvalWorkflowApi.getCurrentNode(props.businessId, props.businessType)
    if (data) {
      currentNodeInfo.value = data
    } else {
      // API未返回数据时，从单店模式审批流程配置获取节点
      setCurrentNodeFromFlowConfig()
    }
  } catch {
    // 获取节点信息失败时，从单店模式审批流程配置获取节点
    setCurrentNodeFromFlowConfig()
  } finally {
    loadingNode.value = false
  }
}

/** 从单店模式审批流程配置设置当前审批节点 */
function setCurrentNodeFromFlowConfig() {
  const flowConfig = SINGLE_STORE_APPROVAL_FLOWS[props.businessType]
  if (flowConfig) {
    const approveNode = flowConfig.find(n => n.nodeType === 'approve')
    if (approveNode) {
      currentNodeInfo.value = {
        nodeId: `${props.businessType}-node-approve`,
        nodeName: approveNode.nodeName,
        approverType: 'role',
        approverRole: approveNode.approverRole,
        approverName: approveNode.approverName,
        status: 'pending',
      }
    }
  }
}

/** 处理通过操作 */
async function handleApprove() {
  await ElMessageBox.confirm(
    '确认通过该审批？',
    '审批确认',
    {
      confirmButtonText: '确认通过',
      cancelButtonText: '取消',
      type: 'success',
    }
  )

  submitting.value = true
  try {
    if (props.approveHandler) {
      // 优先使用父组件传入的真实业务 API 处理器
      await props.approveHandler(comment.value)
    } else {
      // 回退到 mock API（仅用于演示，不会真正更新业务状态）
      await approvalWorkflowApi.approve({
        businessId: props.businessId,
        businessType: props.businessType,
        comment: comment.value,
      })
    }
    emit('approved')
    dialogVisible.value = false
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '审批操作失败')
    }
  } finally {
    submitting.value = false
  }
}

/** 处理驳回操作 */
async function handleReject() {
  if (!comment.value.trim()) {
    ElMessage.warning('驳回时请填写审批意见')
    return
  }

  await ElMessageBox.confirm(
    '确认驳回该审批？驳回后申请人需重新提交。',
    '驳回确认',
    {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )

  submitting.value = true
  try {
    if (props.rejectHandler) {
      // 优先使用父组件传入的真实业务 API 处理器
      await props.rejectHandler(comment.value)
    } else {
      // 回退到 mock API（仅用于演示，不会真正更新业务状态）
      await approvalWorkflowApi.reject({
        businessId: props.businessId,
        businessType: props.businessType,
        comment: comment.value,
      })
    }
    emit('rejected')
    dialogVisible.value = false
  } catch (error: unknown) {
    if (error instanceof Error) {
      ElMessage.error(error.message || '审批操作失败')
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="审批操作"
    width="540px"
    :close-on-click-modal="false"
    class="approval-dialog"
  >
    <!-- 审批头部（仿订单详情 status-header：色带 + 大字 + 元信息） -->
    <div v-loading="loadingNode" class="approval-dialog__header">
      <div class="approval-header__left">
        <span class="approval-header__badge">审</span>
        <div class="approval-header__texts">
          <div class="approval-header__title">{{ displayNodeName }}</div>
          <div class="approval-header__sub">{{ businessTypeLabel }}</div>
        </div>
      </div>
      <div class="approval-header__right">
        <div class="approval-header__no" v-if="businessIdText">{{ businessIdText }}</div>
      </div>
    </div>

    <!-- 审批人信息（与订单详情 descriptions 风格一致） -->
    <el-descriptions :column="2" border class="approval-dialog__meta" v-if="approverTypeLabel || currentNodeInfo?.approverName">
      <el-descriptions-item label="审批人类型">{{ approverTypeLabel || '-' }}</el-descriptions-item>
      <el-descriptions-item label="当前审批人">{{ currentNodeInfo?.approverName || '-' }}</el-descriptions-item>
    </el-descriptions>

    <!-- 审批意见输入 -->
    <div class="approval-dialog__comment">
      <div class="comment__header">
        <label class="comment__label">审批意见</label>
        <span class="comment__hint">{{ commentHint }}</span>
      </div>
      <el-input
        v-model="comment"
        type="textarea"
        placeholder="请输入审批意见"
        :rows="4"
        maxlength="500"
        show-word-limit
      />
    </div>

    <!-- 底部操作：通过（主） / 驳回（危险） -->
    <template #footer>
      <div class="approval-dialog__footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="danger" plain :loading="submitting" @click="handleReject">
          <el-icon style="vertical-align: -2px; margin-right: 4px;"><CircleClose /></el-icon>驳回
        </el-button>
        <el-button type="primary" :loading="submitting" @click="handleApprove">
          <el-icon style="vertical-align: -2px; margin-right: 4px;"><CircleCheck /></el-icon>通过
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped lang="scss">
.approval-dialog {
  /* 审批头部：仿订单详情 status-header（色带 + 大字 + 元信息） */
  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--fts-space-4);
    padding: var(--fts-space-4) var(--fts-space-5);
    background: var(--fts-bg-secondary);
    border: 1px solid var(--fts-border-primary);
    border-left: 4px solid var(--fts-primary);
    border-radius: var(--fts-radius-md);
    margin-bottom: var(--fts-space-4);
  }

  .approval-header {
    &__left {
      display: flex;
      align-items: center;
      gap: var(--fts-space-3);
      min-width: 0;
    }

    &__badge {
      flex-shrink: 0;
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: var(--fts-radius-sm);
      background: var(--fts-primary);
      color: #fff;
      font-size: var(--fts-font-size-lg);
      font-weight: var(--fts-font-weight-bold);
    }

    &__texts {
      display: flex;
      flex-direction: column;
      gap: 2px;
      min-width: 0;
    }

    &__title {
      font-size: var(--fts-font-size-lg);
      font-weight: var(--fts-font-weight-bold);
      color: var(--fts-text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    &__sub {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-secondary);
    }

    &__right {
      flex-shrink: 0;
    }

    &__no {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-tertiary);
      font-variant-numeric: tabular-nums;
    }
  }

  /* 审批人信息（与订单详情 descriptions 风格一致） */
  &__meta {
    margin-bottom: var(--fts-space-4);
  }

  /* 审批意见区 */
  &__comment {
    display: flex;
    flex-direction: column;
    gap: var(--fts-space-2);
  }

  .comment {
    &__header {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    &__label {
      font-size: var(--fts-font-size-base);
      color: var(--fts-text-primary);
      font-weight: var(--fts-font-weight-semibold);
    }

    &__hint {
      font-size: var(--fts-font-size-sm);
      color: var(--fts-text-tertiary);
    }
  }

  &__footer {
    display: flex;
    justify-content: flex-end;
    gap: var(--fts-space-3);
  }
}
</style>
