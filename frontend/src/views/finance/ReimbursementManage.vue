<script setup lang="ts">
/**
 * 报销管理（模块建设中占位页）
 *
 * 背景：工作台财务快捷入口「报销管理」指向 /finance/reimbursement，此前路由表未登记该 path
 *       → 点击后 404 / 白屏。本页用于补齐该入口，保证可访问、不报错。
 *
 * 现状：后端 /v1/finance/invoice-reimbursements 与前端 API（@/api/finance 的 reimbursementApi）
 *       均已就绪，但独立的报销管理页面尚未开发；发票 + 报销申请能力目前由
 *       「发票报销」页（/finance/invoice-reimbursement）提供。
 *
 * 说明：本页只做占位说明，不发起任何业务请求，避免因权限 / 接口异常导致白屏。
 */
import { useRouter } from 'vue-router'
import PageHeader from '@/components/core/PageHeader.vue'
import EmptyState from '@/components/core/EmptyState.vue'

const router = useRouter()

/** 给用户一条可继续操作的路径：跳到已上线的发票报销页 */
function goInvoiceReimbursement(): void {
  router.push('/finance/invoice-reimbursement')
}
</script>

<template>
  <div class="modern-page">
    <PageHeader title="报销管理" description="员工报销申请、审批与付款全流程" />
    <div class="construction">
      <el-alert type="info" :closable="false" show-icon class="construction__alert">
        <template #title>模块建设中</template>
        报销管理页面正在建设中。报销单的申请、审批、付款接口已由后端
        /v1/finance/invoice-reimbursements 提供，页面接入后即可使用。
      </el-alert>
      <EmptyState
        type="no-data"
        title="模块建设中"
        description="报销管理功能即将上线，当前暂无可展示数据。可先前往「发票报销」处理发票与报销申请。"
        action-text="前往发票报销"
        :image-size="140"
        @action="goInvoiceReimbursement"
      />
    </div>
  </div>
</template>

<style scoped lang="scss">
.construction {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-4);
  padding: var(--fts-space-5) var(--fts-space-6) var(--fts-space-8);
  background: var(--fts-bg-card);
  border: 1px solid var(--fts-border-secondary);
  border-top: none;
  border-radius: 0 0 var(--fts-page-radius) var(--fts-page-radius);
  min-height: 60vh;

  &__alert {
    max-width: 100%;
  }
}
</style>
