<template>
  <div class="action-buttons">
    <!-- 发起人角色 -->
    <template v-if="role === 'initiator'">
      <!-- 待处理：撤回 -->
      <template v-if="status === 'pending'">
        <el-popconfirm title="确定撤回该申请？" @confirm="$emit('withdraw')">
          <template #reference>
            <el-button link type="warning" size="small">撤回</el-button>
          </template>
        </el-popconfirm>
      </template>
      <!-- 审核中：催办 -->
      <template v-if="status === 'reviewing'">
        <el-button link type="primary" size="small" @click="$emit('urge')">催办</el-button>
      </template>
    </template>

    <!-- 审批人角色 -->
    <template v-if="role === 'approver'">
      <!-- 待审批 -->
      <template v-if="status === 'pending'">
        <el-popconfirm title="确定通过该申请？" @confirm="$emit('approve')">
          <template #reference>
            <el-button link type="success" size="small">通过</el-button>
          </template>
        </el-popconfirm>
        <el-popconfirm title="确定驳回该申请？" @confirm="$emit('reject')">
          <template #reference>
            <el-button link type="danger" size="small">驳回</el-button>
          </template>
        </el-popconfirm>
        <el-button link type="primary" size="small" @click="$emit('transfer')">转交</el-button>
      </template>
    </template>

    <!-- 查看者角色：无按钮 -->
  </div>
</template>

<script setup lang="ts">
/**
 * ActionButtons - 审批操作按钮组组件
 *
 * 根据用户角色（发起人/审批人/查看者）和当前审批状态，
 * 动态渲染对应的操作按钮。
 * 所有按钮均使用 el-button link 类型。
 */

interface Props {
  /** 用户角色 */
  role: 'initiator' | 'approver' | 'viewer'
  /** 当前审批状态 */
  status: string
  /** 审批ID（预留扩展） */
  approvalId?: string
}

withDefaults(defineProps<Props>(), {
  approvalId: '',
})

const emit = defineEmits<{
  (e: 'withdraw'): void
  (e: 'urge'): void
  (e: 'approve'): void
  (e: 'reject'): void
  (e: 'transfer'): void
}>()
</script>

<style scoped lang="scss">
.action-buttons {
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  flex-wrap: wrap;

  :deep(.el-button) {
    font-size: var(--fts-font-size-sm);
  }
}
</style>
