<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'

interface RefundDetail {
  id: string
  refundNo: string
  orderNo: string
  storeName: string
  refundAmount: number
  reason: string
  status: string
  applicant: string
  approver: string
  rejectReason: string
  applyTime: string
  approveTime: string
}

const props = defineProps<{
  visible: boolean
  data: RefundDetail | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const drawerRef = ref()
const displayData = computed(() => props.data)

watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => {
      const el = drawerRef.value?.$el
      if (el) {
        const body: HTMLElement | null = el.querySelector('.el-drawer__body')
        if (body) body.scrollTop = 0
      }
    })
  }
})

function handleClose() {
  emit('update:visible', false)
}
</script>

<template>
  <el-drawer
    ref="drawerRef"
    :model-value="visible"
    :size="480"
    direction="ltr"
    :with-header="false"
    :lock-scroll="false"
    :close-on-click-modal="true"
    class="biz-drawer"
    @closed="handleClose"
  >
    <template v-if="displayData">
      <div class="biz-drawer__header">
        <div class="biz-drawer__header-left">
          <span class="biz-drawer__title">退款详情</span>
          <span class="biz-drawer__no">{{ displayData.refundNo }}</span>
        </div>
        <StatusTag :status="displayData.status" size="medium" />
      </div>

      <div class="biz-drawer__body">
        <div class="data-group">
          <h4 class="data-group__title">退款信息</h4>
          <div class="data-grid">
            <div class="data-field data-field--primary">
              <span class="data-field__label">退款金额</span>
              <span class="data-field__value data-field__value--amount">¥{{ displayData.refundAmount }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">门店</span>
              <span class="data-field__value">{{ displayData.storeName }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">关联订单</span>
              <span class="data-field__value">{{ displayData.orderNo }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">退款原因</span>
              <span class="data-field__value">{{ displayData.reason }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">申请人</span>
              <span class="data-field__value">{{ displayData.applicant }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">申请时间</span>
              <span class="data-field__value">{{ displayData.applyTime }}</span>
            </div>
          </div>
        </div>

        <div class="data-group">
          <h4 class="data-group__title">审批信息</h4>
          <div class="data-grid">
            <div class="data-field">
              <span class="data-field__label">审批人</span>
              <span class="data-field__value">{{ displayData.approver || '-' }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">审批时间</span>
              <span class="data-field__value">{{ displayData.approveTime || '-' }}</span>
            </div>
          </div>
          <div v-if="displayData.rejectReason" class="data-reject">
            <span class="data-reject__label">拒绝原因</span>
            <span class="data-reject__value">{{ displayData.rejectReason }}</span>
          </div>
        </div>
      </div>
    </template>

    <template #footer>
      <div class="biz-drawer__footer">
        <el-button @click="handleClose">关闭</el-button>
      </div>
    </template>
  </el-drawer>
</template>

<style scoped lang="scss">
.biz-drawer {
  :deep(.el-drawer__body) {
    padding: 0;
  }

  :deep(.el-drawer) {
    transition-duration: 0.22s;
  }
}

.biz-drawer__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--fts-space-5) var(--fts-space-6);
  border-bottom: 1px solid var(--fts-border-primary);
  background: var(--fts-bg-card);
}

.biz-drawer__header-left {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);
  min-width: 0;
}

.biz-drawer__title {
  font-size: var(--fts-font-size-lg);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-primary);
}

.biz-drawer__no {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  font-variant-numeric: tabular-nums;
}

.biz-drawer__body {
  padding: 0 var(--fts-space-6);
}

.data-group {
  padding: var(--fts-space-5) 0;

  & + & {
    border-top: 1px solid var(--fts-border-secondary);
  }

  &__title {
    margin: 0 0 var(--fts-space-4);
    font-size: var(--fts-font-size-sm);
    font-weight: var(--fts-font-weight-semibold);
    color: var(--fts-text-secondary);
    padding-left: var(--fts-space-3);
    border-left: 3px solid var(--fts-warning);
    line-height: 1;
  }
}

.data-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--fts-space-4) var(--fts-space-5);
}

.data-field {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__value {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-primary);
    word-break: break-all;

    &--amount {
      font-size: var(--fts-font-size-lg);
      font-weight: var(--fts-font-weight-semibold);
      color: var(--fts-warning);
      font-family: 'Courier New', monospace;
    }
  }

  &--primary {
    .data-field__value {
      font-weight: var(--fts-font-weight-medium);
    }
  }
}

.data-reject {
  margin-top: var(--fts-space-3);
  padding: var(--fts-space-3) var(--fts-space-4);
  background: color-mix(in srgb, var(--fts-error) 6%, transparent);
  border-radius: var(--fts-radius-md);
  border: 1px solid color-mix(in srgb, var(--fts-error) 15%, transparent);

  &__label {
    display: block;
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-bottom: var(--fts-space-1);
  }

  &__value {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-error);
    font-weight: var(--fts-font-weight-medium);
  }
}

.biz-drawer__footer {
  display: flex;
  justify-content: flex-end;
  padding: var(--fts-space-4) var(--fts-space-6);
  border-top: 1px solid var(--fts-border-secondary);

  :deep(.el-button) {
    min-width: 88px;
  }
}
</style>