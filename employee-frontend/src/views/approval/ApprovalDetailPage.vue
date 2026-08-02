<script setup lang="ts">
/**
 * ApprovalDetailPage - 审批详情页面
 * 展示审批单完整信息：状态头部、申请内容、审批参考信息、附件、审批流程时间线、评论、操作区
 *
 * 已拆分为7个子组件：
 *   - ApprovalDetailHeader: 状态头部（类型/紧急度/状态标签/申请人/单号）
 *   - ApprovalDetailFields: 申请内容表单字段展示
 *   - ApprovalContextInfo: 审批参考信息（HR风控/假期余额/费用明细等）
 *   - ApprovalAttachments: 附件列表
 *   - ApprovalDetailTimeline: 审批流程时间线
 *   - ApprovalComments: 审批意见列表
 *   - ApprovalDetailActions: 操作按钮 + 驳回弹窗 + 结果提示
 *
 * @last-modified 2026-06-09 (代码拆分重构)
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { PageContainer, StatusTag } from '@/components/core'
// 子组件导入
import ApprovalDetailHeader from './components/ApprovalDetailHeader.vue'
import ApprovalDetailFields from './components/ApprovalDetailFields.vue'
import ApprovalContextInfo from './components/ApprovalContextInfo.vue'
import ApprovalAttachments from './components/ApprovalAttachments.vue'
import ApprovalDetailTimeline from './components/ApprovalDetailTimeline.vue'
import ApprovalComments from './components/ApprovalComments.vue'
import ApprovalDetailActions from './components/ApprovalDetailActions.vue'

import { usePermissionStore } from '@/stores/permission'
import { useAuthContext } from '@/composables/useAuthContext'
import { approvalApi } from '@/api/approval'
import type { ApprovalType, ApprovalStatus, FormField, Comment, Attachment, ContextData, ApprovalDetail } from '@/types/approval'
import { ApprovalTypeLabels } from '@/types/approval'

const route = useRoute()
const router = useRouter()
const permissionStore = usePermissionStore()
const auth = useAuthContext()
const id = route.params.id as string

const loading = ref(false)
const actionLoading = ref(false)

const detail = ref<ApprovalDetail | null>(null)

const canApprove = computed(() => {
  if (!detail.value) return false
  return permissionStore.canApproveOthers &&
    (detail.value.status === 'pending' || detail.value.status === 'processing')
})

const canWithdraw = computed(() => {
  if (!detail.value) return false
  return detail.value.applicantName === auth.userName.value &&
    (detail.value.status === 'pending' || detail.value.status === 'processing')
})

async function fetchDetail() {
  loading.value = true
  try {
    const result = await approvalApi.getDetail(id)
    detail.value = result
  } catch {
    ElMessage.error('加载审批详情失败')
    detail.value = null
  } finally {
    loading.value = false
  }
}

async function handleApprove() {
  actionLoading.value = true
  try {
    await approvalApi.action(id, 'approve', { comment: '同意' })
    ElMessage.success('审批已通过')
    await fetchDetail()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleReject(reason: string) {
  actionLoading.value = true
  try {
    await approvalApi.action(id, 'reject', { comment: reason })
    ElMessage.success('已驳回')
    await fetchDetail()
  } catch {
    ElMessage.error('操作失败')
  } finally {
    actionLoading.value = false
  }
}

async function handleWithdraw() {
  try {
    await ElMessageBox.confirm('确认撤回此申请？', '撤回确认', {
      confirmButtonText: '确认撤回',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await approvalApi.withdraw(id)
    ElMessage.success('申请已撤回')
    await fetchDetail()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<template>
  <PageContainer title="审批详情" :loading="loading">
    <template v-if="detail">
      <!-- 状态头部 -->
      <ApprovalDetailHeader :detail="detail" />

      <!-- 申请内容 -->
      <ApprovalDetailFields :fields="detail.formFields" />

      <!-- 审批参考信息 -->
      <ApprovalContextInfo v-if="detail.contextData" :context-data="detail.contextData" />

      <!-- 附件区域 -->
      <ApprovalAttachments :attachments="detail.attachments || []" />

      <!-- 审批流程时间线 -->
      <ApprovalDetailTimeline :nodes="detail.flowNodes" />

      <!-- 评论区域 -->
      <ApprovalComments :comments="detail.comments || []" />

      <!-- 操作区 + 驳回弹窗 + 结果提示 -->
      <ApprovalDetailActions
        :can-approve="canApprove"
        :can-withdraw="canWithdraw"
        :status="detail.status"
        :action-loading="actionLoading"
        @approve="handleApprove"
        @reject="handleReject"
        @withdraw="handleWithdraw"
      />
    </template>
  </PageContainer>
</template>

<style scoped lang="scss">
/* 主页面仅保留最小化样式（如需要） */
/* 所有子组件的样式已在各自文件中定义 */
</style>
