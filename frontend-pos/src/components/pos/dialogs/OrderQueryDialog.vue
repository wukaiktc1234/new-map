<template>
  <el-dialog
    :model-value="modelValue"
    @update:model-value="emit('update:modelValue', $event)"
    title="订单查询"
    width="1120px"
    class="order-query-dialog"
    :lock-scroll="false"
    :append-to-body="true"
  >
    <div class="order-query-content">
      <div class="query-filter">
        <!-- readonly 会让输入框完全不可点、日历面板永不弹出（历史 BUG），这里去掉 -->
        <el-date-picker
          v-model="queryDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          class="query-date"
          data-test="order-query-date"
        />
        <el-input
          v-model="queryKeyword"
          placeholder="输入订单号 / 叫号号 / 手机号"
          class="query-keyword"
          clearable
          @keyup.enter="queryOrders"
        />
        <el-button type="primary" class="query-submit" data-test="order-query-submit" @click="queryOrders">
          查询
        </el-button>
      </div>
      <div class="order-list">
        <el-table
          :data="orderList"
          style="width: 100%"
          height="100%"
          data-test="order-query-table"
        >
          <el-table-column prop="callNumber" label="叫号" width="76" align="center">
            <template #default="{ row }">
              <span class="call-number-cell">{{ row.callNumber || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="orderNumber" label="订单号" min-width="150" show-overflow-tooltip />
          <el-table-column prop="createTime" label="下单时间" min-width="150">
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
          <el-table-column prop="paymentMethod" label="支付方式" min-width="110" align="center" show-overflow-tooltip>
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
          <!-- 去掉 fixed="right"：固定列会强制出现横向滚动条并截断中间列 -->
          <el-table-column label="操作" width="120" align="center">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="emit('viewDetail', row)">详情</el-button>
              <!-- 退款需要 order:refund 权限：无权限时按钮不渲染，避免收银员点出 403 -->
              <el-button
                v-if="allowRefund && canRefund(row)"
                type="danger"
                link
                size="small"
                data-test="order-refund-btn"
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
import { getOrderStatus } from '@/utils/orderStatus'
import { usePermission } from '@/composables/usePermission'
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


const { canRefund: allowRefund, refreshCapabilities } = usePermission()

// 打开弹窗时按当前登录信息重算一次权限（模块级快照可能是登录前初始化的）
watch(() => props.modelValue, (visible) => { if (visible) refreshCapabilities() }, { immediate: true })

const queryDateRange = ref<any[]>([])
const queryKeyword = ref<string>('')
const orderList = ref<any[]>([])
const orderQueryTimer = ref<number | null>(null)



// 状态映射统一走 utils/orderStatus（数字规范码 + 文本同义词都认识）
const getStatusType = (status: string) => getOrderStatus(status).tone

const getStatusText = (status: string) => getOrderStatus(status).text

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

/* ====== 顶部筛选行：宽度弹性分配，避免固定 px 把表格挤窄 ====== */
.order-query-content {
  display: flex;
  flex-direction: column;
  gap: var(--pos-spacing-sm, 16px);
}

.query-filter {
  display: flex;
  align-items: center;
  gap: var(--pos-spacing-xs, 8px);
  flex-wrap: wrap;
}

.query-date {
  flex: 0 1 300px;
  min-width: 240px;
}

.query-keyword {
  flex: 1 1 220px;
  min-width: 200px;
}

.query-submit {
  flex: 0 0 auto;
  min-height: 40px;
  padding: 0 var(--pos-spacing-md, 24px);
  border-radius: var(--pos-radius-sm, 8px) !important;
  background: var(--pos-primary, #e3651c) !important;
  border-color: var(--pos-primary, #e3651c) !important;
  color: #ffffff !important;
  font-weight: 600;
}

.query-submit:hover {
  background: var(--pos-primary-dark, #c25515) !important;
  border-color: var(--pos-primary-dark, #c25515) !important;
}

.order-list {
  flex: 1;
  min-height: 0;
}

.order-query-dialog {
  :deep(.el-dialog) {
    background-color: var(--pos-bg-primary) !important;
    border-radius: var(--pos-radius-md, 12px) !important;
  }

  /* 主体布局（固定不滚、表格内滚）统一在全局 dialog.css 的 .order-query-dialog 段 */

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

  :deep(.el-table th.el-table__cell) {
    background-color: var(--pos-bg-tertiary) !important;
    color: var(--pos-text-secondary) !important;
    font-weight: 600 !important;
  }

  :deep(.el-table td.el-table__cell) {
    color: var(--pos-text-primary) !important;
  }

  /* 不用固定列，横向滚动条只在真的放不下时才出现 */
  :deep(.el-table__body-wrapper) {
    overflow-x: auto;
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