<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="订单查询"
    width="900px"
    class="order-query-dialog"
    :lock-scroll="false"
  >
    <div class="order-query-content">
      <div class="query-filter">
        <el-date-picker
          v-model="queryDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 300px"
          readonly
          :teleported="false"
        />
        <el-input v-model="queryKeyword" placeholder="输入订单号或手机号搜索" style="width: 250px" clearable />
        <el-button type="primary" @click="queryOrders">查询</el-button>
      </div>
      <div class="order-list">
        <el-table :data="orderList" style="width: 100%">
          <el-table-column prop="orderNumber" label="订单号" min-width="140" />
          <el-table-column prop="createTime" label="下单时间" min-width="160" />
          <el-table-column prop="itemCount" label="数量" width="60" align="center" />
          <el-table-column prop="totalAmount" label="金额" width="90">
            <template #default="{ row }">
              ¥{{ row.totalAmount.toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="paymentMethod" label="支付方式" width="90" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="emit('viewDetail', row)">详情</el-button>
              <el-button
                v-if="row.status === 'completed' || row.status === 'paid'"
                type="danger"
                link
                size="small"
                @click="emit('refund', row)"
              >退款</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import request from '@/api/request'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'viewDetail', order: any): void
  (e: 'refund', order: any): void
}>()

const queryDateRange = ref<any[]>([])
const queryKeyword = ref<string>('')
const orderList = ref<any[]>([])
const orderQueryTimer = ref<number | null>(null)

const getStatusType = (status: string) => {
  const map: Record<string, any> = {
    pending: 'warning',
    paid: 'success',
    cancelled: 'danger',
    making: 'primary',
    completed: 'success',
    served: 'success',
    refunded: 'danger'
  }
  return map[status] || 'info'
}

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待制作',
    paid: '已支付',
    cancelled: '已取消',
    making: '制作中',
    completed: '制作完成',
    served: '已出餐',
    refunded: '已退款'
  }
  return map[status] || status
}

const queryOrders = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = queryDateRange.value[0].toISOString().split('T')[0]
      params.endDate = queryDateRange.value[1].toISOString().split('T')[0]
    }

    const result = await request.get('/v1/pos/orders', params) as any

    if (Array.isArray(result)) {
      orderList.value = result
    } else {
      orderList.value = []
    }
  } catch (error) {
    console.error('查询订单失败:', error)
    orderList.value = []
  }
}

const queryOrdersSilent = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = queryDateRange.value[0].toISOString().split('T')[0]
      params.endDate = queryDateRange.value[1].toISOString().split('T')[0]
    }

    const result = await request.get('/v1/pos/orders', params) as any

    if (Array.isArray(result)) {
      const changed = JSON.stringify(orderList.value) !== JSON.stringify(result)
      if (changed) {
        orderList.value = result
      }
    }
  } catch (error) {
    console.error('静默查询订单失败:', error)
  }
}

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    queryOrders()
    if (orderQueryTimer.value) clearInterval(orderQueryTimer.value)
    orderQueryTimer.value = window.setInterval(() => {
      if (props.modelValue) {
        queryOrdersSilent()
      }
    }, 10000)
  } else {
    if (orderQueryTimer.value) {
      clearInterval(orderQueryTimer.value)
      orderQueryTimer.value = null
    }
  }
})

onUnmounted(() => {
  if (orderQueryTimer.value) {
    clearInterval(orderQueryTimer.value)
    orderQueryTimer.value = null
  }
})
</script>

<style scoped>
.order-query-content {
  padding: 8px 0;
}

.query-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;

  :deep(.el-range-separator) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-date-editor) {
    color: var(--pos-text-primary) !important;
  }
}

.order-list {
  max-height: 400px;
  overflow-y: auto;
}

.order-query-dialog {
  :deep(.el-dialog) {
    background-color: var(--pos-bg-secondary) !important;
  }

  :deep(.el-dialog__header) {
    border-bottom: 1px solid var(--pos-border-light);
  }

  :deep(.el-dialog__title) {
    color: var(--pos-text-primary) !important;
    font-weight: 600;
  }

  :deep(.el-dialog__body) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-button--primary) {
    color: #fff;
  }

  :deep(.el-button--text) {
    color: var(--pos-primary) !important;
  }

  :deep(.el-button) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__wrapper) {
    color: var(--pos-text-primary) !important;
    background-color: var(--pos-bg-secondary) !important;
    box-shadow: 0 0 0 1px var(--pos-border-color) inset !important;
  }

  :deep(.el-input__inner) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-input__placeholder) {
    color: var(--pos-text-muted) !important;
  }

  :deep(.el-form-item__label) {
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table th) {
    background-color: var(--pos-bg-secondary);
    color: var(--pos-text-primary) !important;
  }

  :deep(.el-table td) {
    color: var(--pos-text-secondary) !important;
  }
}

.order-list::-webkit-scrollbar {
  width: 6px;
}

.order-list::-webkit-scrollbar-track {
  background: transparent;
}

.order-list::-webkit-scrollbar-thumb {
  background: var(--pos-border-color);
  border-radius: 3px;
}
</style>
