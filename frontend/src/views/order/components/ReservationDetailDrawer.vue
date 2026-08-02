<script setup lang="ts">
import { computed, ref, watch, nextTick } from 'vue'
import StatusTag from '@/components/core/StatusTag.vue'

interface ReservationDetail {
  id: string
  resNo: string
  storeName: string
  customerName: string
  phone: string
  resTime: string
  guestCount: number
  tableNo: string
  status: string
  createdBy: string
  confirmedBy: string
  notes: string
}

const props = defineProps<{
  visible: boolean
  data: ReservationDetail | null
}>()

const emit = defineEmits<{
  'update:visible': [value: boolean]
}>()

const drawerRef = ref()
const displayData = computed(() => props.data)

/** 预约状态 → StatusTag 组件 status 属性映射 */
const statusTagType = computed(() => {
  const map: Record<string, string> = {
    pending: 'warning',
    confirmed: 'primary',
    arrived: 'success',
    cancelled: 'info',
    no_show: 'error',
  }
  return map[displayData.value?.status || ''] || 'info'
})

/** 预约状态 → 中文标签映射 */
const statusLabel = computed(() => {
  const map: Record<string, string> = {
    pending: '待确认',
    confirmed: '已确认',
    arrived: '已到店',
    cancelled: '已取消',
    no_show: '未到店',
  }
  return map[displayData.value?.status || ''] || displayData.value?.status || ''
})

watch(() => props.visible, (val) => {
  if (val) {
    nextTick(() => {
      // drawerRef.$el 可能是非元素节点（如注释节点），需检查是否为 HTMLElement
      const el = drawerRef.value?.$el
      if (el instanceof HTMLElement) {
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
          <span class="biz-drawer__title">预约详情</span>
          <span class="biz-drawer__no">{{ displayData.resNo }}</span>
        </div>
        <StatusTag :status="statusTagType" :label="statusLabel" size="medium" />
      </div>

      <div class="biz-drawer__body">
        <div class="data-group">
          <h4 class="data-group__title">预约信息</h4>
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
              <span class="data-field__label">联系电话</span>
              <span class="data-field__value">{{ displayData.phone }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">预约时间</span>
              <span class="data-field__value data-field__value--primary">{{ displayData.resTime }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">人数</span>
              <span class="data-field__value">{{ displayData.guestCount }} 人</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">桌号</span>
              <span class="data-field__value">{{ displayData.tableNo || '-' }}</span>
            </div>
          </div>
        </div>

        <div class="data-group">
          <h4 class="data-group__title">操作信息</h4>
          <div class="data-grid">
            <div class="data-field">
              <span class="data-field__label">创建人</span>
              <span class="data-field__value">{{ displayData.createdBy }}</span>
            </div>
            <div class="data-field">
              <span class="data-field__label">确认人</span>
              <span class="data-field__value">{{ displayData.confirmedBy || '-' }}</span>
            </div>
          </div>
          <div v-if="displayData.notes" class="data-notes">
            <span class="data-notes__label">备注</span>
            <span class="data-notes__value">{{ displayData.notes }}</span>
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
    border-left: 3px solid var(--fts-info);
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

    &--primary {
      font-weight: var(--fts-font-weight-medium);
    }
  }
}

.data-notes {
  margin-top: var(--fts-space-3);
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  &__label {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
  }

  &__value {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-secondary);
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