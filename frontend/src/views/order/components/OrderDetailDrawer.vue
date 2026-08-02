<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import { ElTimeline, ElTimelineItem } from 'element-plus'
import StatusTag from '@/components/core/StatusTag.vue'

interface OrderDetail {
  id: string
  orderNo: string
  storeName: string
  tableNo: string
  orderType: string
  customerName: string
  dishName: string
  quantity: number
  amount: number
  status: string
  paymentMethod: string
  createdBy: string
  orderTime: string
  completeTime: string
  remarks: string
  timeline: { time: string; operator: string; action: string }[]
}

const props = defineProps<{
  visible: boolean
  data: OrderDetail | null
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

function getCssVar(name: string): string {
  return getComputedStyle(document.documentElement).getPropertyValue(name).trim()
}
const cssPrimary = computed(() => getCssVar('--fts-primary'))
</script>

<template>
  <el-drawer
    ref="drawerRef"
    :model-value="visible"
    :size="520"
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
          <span class="biz-drawer__title">订单详情</span>
          <span class="biz-drawer__no">{{ displayData.orderNo }}</span>
        </div>
        <StatusTag :status="displayData.status" size="medium" />
      </div>

      <div class="biz-drawer__body">
        <div class="data-group">
          <h4 class="data-group__title">基本信息</h4>
          <div class="data-grid">
            <div class="data-field">
              <span class="data-field__label">门店</span>
              <span class="data-field__value">{{ displayData.storeName }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">顾客</span>
              <span class="data-field__value">{{ displayData.customerName }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">菜品</span>
              <span class="data-field__value">{{ displayData.dishName }} ×{{ displayData.quantity }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">桌号</span>
              <span class="data-field__value">{{ displayData.tableNo || '-' }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">就餐类型</span>
              <span class="data-field__value">{{ displayData.orderType || '-' }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">支付方式</span>
              <span class="data-field__value">{{ displayData.paymentMethod }}</span>
            </div>
          </div>
        </div>

        <div class="data-group">
          <h4 class="data-group__title">金额与时间</h4>
          <div class="data-grid">
            <div class="data-field data-field--primary">
              <span class="data-field__label">订单金额</span>
              <span class="data-field__value data-field__value--amount">¥{{ displayData.amount }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">操作员</span>
              <span class="data-field__value">{{ displayData.createdBy }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">下单时间</span>
              <span class="data-field__value">{{ displayData.orderTime }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">完成时间</span>
              <span class="data-field__value">{{ displayData.completeTime || '-' }}</span>
            </div>
          </div>
        </div>

        <div v-if="displayData.remarks" class="data-group">
          <h4 class="data-group__title">备注</h4>
          <p class="data-group__text">{{ displayData.remarks }}</p>
        </div>

        <div class="data-group">
          <h4 class="data-group__title">操作记录</h4>
          <el-timeline v-if="displayData.timeline?.length">
            <el-timeline-item
              v-for="(item, idx) in displayData.timeline"
              :key="idx"
              :timestamp="item.time"
              placement="top"
              :color="cssPrimary"
              size="normal"
            >
              <span class="timeline-operator">{{ item.operator }}</span>
              <span class="timeline-action">{{ item.action }}</span>
            </el-timeline-item>
          </el-timeline>
          <p v-else class="data-group__empty">暂无操作记录</p>
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
    border-left: 3px solid var(--fts-primary);
    line-height: 1;
  }

  &__text {
    margin: 0;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
    line-height: var(--fts-line-height-relaxed);
  }

  &__empty {
    margin: 0;
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
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
      color: var(--fts-primary);
      font-family: 'Courier New', monospace;
    }
  }

  &--primary {
    .data-field__value {
      font-weight: var(--fts-font-weight-medium);
    }
  }
}

.timeline-operator {
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-medium);
  color: var(--fts-text-primary);
  margin-right: var(--fts-space-2);
}

.timeline-action {
  font-size: var(--fts-font-size-sm);
  color: var(--fts-text-secondary);
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