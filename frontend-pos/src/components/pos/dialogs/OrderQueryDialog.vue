<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="订单查询"
    width="980px"
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
        <el-input v-model="queryKeyword" placeholder="输入订单号/叫号号/手机号搜索" style="width: 250px" clearable />
        <el-button type="primary" @click="queryOrders">查询</el-button>
      </div>
      <div class="order-list">
        <el-table :data="orderList" style="width: 100%">
          <el-table-column prop="callNumber" label="叫号" width="90" align="center">
            <template #default="{ row }">
              <span class="call-number-cell">{{ row.callNumber || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderNumber" label="订单号" min-width="140" />
          <el-table-column prop="createTime" label="下单时间" min-width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.createTime, '-') }}
            </template>
          </el-table-column>
          <!-- 数量：后端 OrderQueryDTO.itemCount（菜品总件数），空值显示 '-' -->
          <el-table-column prop="itemCount" label="数量" width="70" align="right">
            <template #default="{ row }">
              <span class="item-count-cell">{{ row.itemCount ?? '-' }}</span>
            </template>
          </el-table-column>
          <!-- 支付方式：后端已给中文（OrderQueryDTO.paymentMethod），不前端翻译；兼容 paymentMethodName -->
          <el-table-column prop="paymentMethod" label="支付方式" width="100" align="center">
            <template #default="{ row }">
              <span class="payment-method-cell">{{ row.paymentMethod || row.paymentMethodName || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="金额" width="100" align="right">
            <template #default="{ row }">
              ¥{{ (row.totalAmount ?? 0).toFixed(2) }}
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="emit('viewDetail', row)">详情</el-button>
              <el-button
                v-if="canRefund(row)"
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
import { ElMessage } from 'element-plus'
import { posApi } from '@/api/posApi'
import { formatDate, formatDateTime } from '@/utils/format'
import logger from '@/utils/logger'

interface Props {
  modelValue: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'viewDetail', order: any): void
  (e: 'refund', order: any): void
}>()

/**
 * 退款可用判定（放宽口径）：只要不是「未支付 / 已取消 / 已退款」即可发起退款。
 * 真实场景里顾客刚下单或制作中就要退单是常态，不再要求「已完成」。
 */
function canRefund(order: any): boolean {
  if (!order) return false
  const status = String(order.status || '')
  return !['pending', 'unpaid', 'cancelled', 'refunded', 'partial_refunded'].includes(status)
}


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
      params.startDate = formatDate(queryDateRange.value[0])
      params.endDate = formatDate(queryDateRange.value[1])
    }

    // 修复：原写法把查询参数直接当 axios config 传（request.get(url, params)），
    // axios 只认 config.params，导致 keyword/startDate/endDate 一个都没发出 —— 表现为「点了查询没反应」。
    const result = await posApi.getOrders(params)

    if (Array.isArray(result)) {
      orderList.value = result
      if (result.length === 0) {
        ElMessage.info('没有匹配的订单')
      }
    } else {
      orderList.value = []
    }
  } catch (error) {
    logger.errorMessage('查询订单失败:', error)
    orderList.value = []
    ElMessage.error(error instanceof Error ? error.message : '查询订单失败，请重试')
  }
}

const queryOrdersSilent = async () => {
  try {
    const params: any = {}
    if (queryKeyword.value) {
      params.keyword = queryKeyword.value
    }
    if (queryDateRange.value && queryDateRange.value.length === 2) {
      params.startDate = formatDate(queryDateRange.value[0])
      params.endDate = formatDate(queryDateRange.value[1])
    }

    const result = await posApi.getOrders(params)

    if (Array.isArray(result)) {
      const changed = JSON.stringify(orderList.value) !== JSON.stringify(result)
      if (changed) {
        orderList.value = result
      }
    }
  } catch (error) {
    logger.errorMessage('静默查询订单失败:', error)
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

/* 数量/支付方式：等宽数字 + 空值占位保持整齐 */
.item-count-cell {
  font-variant-numeric: tabular-nums;
  color: var(--pos-text-primary);
}

.payment-method-cell {
  white-space: nowrap;
  color: var(--pos-text-primary);
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
