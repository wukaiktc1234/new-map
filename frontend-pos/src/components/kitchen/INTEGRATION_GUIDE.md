/**
 * 后厨工作板集成示例 - Kitchen Home Integration Example
 *
 * 此文件展示如何将🍳-3的批量操作、快捷键、操作反馈系统集成到KitchenHome.vue中
 *
 * 集成组件清单：
 * 1. BatchOperationPanel.vue - 批量操作面板
 * 2. ShortcutHelp.vue - 快捷键帮助面板
 * 3. useKeyboardShortcuts.ts - 快捷键Composable
 * 4. useOperationFeedback.ts - 操作反馈Composable
 * 5. kitchen-touch.css - 触摸优化样式
 */

// ========== 示例代码：KitchenHome.vue 集成模板 ==========
/*
<template>
  <div class="kitchen-home kitchen-touch-optimized">
    !-- 页面头部 --
    <header class="kitchen-header">
      <h1>后厨工作板</h1>
      <button @click="toggleBatchMode" :class="{ active: batchMode }">
        批量操作
      </button>
    </header>

    !-- 订单列表区域 --
    <main class="orders-container">
      <div
        v-for="order in orders"
        :key="order.id"
        class="order-card"
        :class="{ 'is-selected': selectedIds.includes(order.id), 'is-urgent': isUrgent(order) }"
        @click="handleOrderClick(order)"
      >
        !-- 批量选择checkbox（仅在批量模式下显示） --
        <el-checkbox
          v-if="batchMode"
          :model-value="selectedIds.includes(order.id)"
          @change="(val: boolean) => toggleSelection(order.id, val)"
          @click.stop
          class="order-checkbox"
        />

        !-- 订单内容 --
        <div class="order-content">
          <span class="order-name">{{ order.dishName }}</span>
          <span class="order-time">{{ formatTime(order.createTime) }}</span>
        </div>

        !-- 状态标签 --
        <el-tag :type="getStatusType(order.status)">
          {{ getStatusText(order.status) }}
        </el-tag>
      </div>
    </main>

    !-- 批量操作面板（底部浮动） --
    <BatchOperationPanel
      :visible="batchMode && selectedIds.length > 0"
      :selected-ids="selectedIds"
      :pending-count="pendingCount"
      :completed-count="completedCount"
      :total-count="orders.length"
      :loading="batchLoading"
      @close="closeBatchMode"
      @select-all="handleSelectAll"
      @start-cooking="handleStartCooking"
      @mark-complete="handleMarkComplete"
      @reset-selection="resetSelection"
      @quick-complete-all="handleQuickCompleteAll"
      @quick-serve-all="handleQuickServeAll"
    />

    !-- 快捷键帮助面板（模态框） --
    <ShortcutHelp
      :visible="showShortcutHelp"
      :shortcuts="shortcutList"
      :highlighted-key="highlightedKey"
      @close="showShortcutHelp = false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import BatchOperationPanel from '@/components/kitchen/BatchOperationPanel.vue'
import ShortcutHelp from '@/components/kitchen/ShortcutHelp.vue'
import { useKeyboardShortcuts, type ShortcutConfig } from '@/composables/kitchen/useKeyboardShortcuts'
import { useOperationFeedback } from '@/composables/kitchen/useOperationFeedback'

// 导入触摸优化样式
import '@/styles/kitchen-touch.css'

// 接口定义
interface Order {
  id: string
  dishName: string
  status: 'pending' | 'cooking' | 'completed' | 'served'
  createTime: Date
  urgent?: boolean
}

// 响应式状态
const orders = ref<Order[]>([])
const selectedIds = ref<string[]>([])
const batchMode = ref(false)
const batchLoading = ref(false)

// 使用快捷键系统
const {
  isActive: shortcutsActive,
  showHelp: showShortcutHelp,
  highlightedButton: highlightedKey,
  registerShortcuts,
  updateShortcutHandler,
  getShortcutList
} = useKeyboardShortcuts()

// 使用操作反馈系统
const { showSuccess, showError, showWarning, showUrgent } = useOperationFeedback()

// 计算属性
const pendingCount = computed(() =>
  orders.value.filter(o => o.status === 'pending').length
)

const completedCount = computed(() =>
  orders.value.filter(o => o.status === 'completed').length
)

const shortcutList = computed<ShortcutConfig[]>(() => getShortcutList())

// 初始化自定义快捷键
onMounted(() => {
  const customShortcuts: ShortcutConfig[] = [
    // 保留默认的1-5数字键用于选择订单
    ...getKeyboardShortcuts().getDefaultShortcuts().slice(0, 5),

    // 自定义Enter键处理
    {
      key: 'Enter',
      description: '开始制作选中订单',
      handler: () => {
        if (selectedIds.value.length > 0) {
          handleStartCooking(selectedIds.value)
        }
      }
    },

    // 自定义空格键处理
    {
      key: ' ',
      description: '完成选中订单',
      handler: () => {
        if (selectedIds.value.length > 0) {
          handleMarkComplete(selectedIds.value)
        }
      }
    },

    // 其他快捷键...
  ]

  registerShortcuts(customShortcuts)
})

// ========== 批量操作相关方法 ==========

const toggleBatchMode = (): void => {
  batchMode.value = !batchMode.value
  if (!batchMode.value) {
    resetSelection()
  }
  showSuccess(batchMode.value ? '已进入批量模式' : '已退出批量模式')
}

const handleOrderClick = (order: Order): void => {
  if (batchMode.value) {
    toggleSelection(order.id, !selectedIds.value.includes(order.id))
  } else {
    // 单个订单操作逻辑
    handleSingleOrderAction(order)
  }
}

const toggleSelection = (orderId: string, selected: boolean): void => {
  if (selected) {
    if (!selectedIds.value.includes(orderId)) {
      selectedIds.value.push(orderId)
    }
  } else {
    selectedIds.value = selectedIds.value.filter(id => id !== orderId)
  }
}

const handleSelectAll = (selected: boolean): void => {
  if (selected) {
    selectedIds.value = orders.value.map(o => o.id)
  } else {
    resetSelection()
  }
}

const resetSelection = (): void => {
  selectedIds.value = []
}

const closeBatchMode = (): void => {
  batchMode.value = false
  resetSelection()
}

// ========== 批量操作处理函数 ==========

const handleStartCooking = async (ids: string[]): Promise<void> => {
  try {
    batchLoading.value = true
    // 调用🍳-2提供的批量API
    await kitchenApi.batchUpdateStatus(ids, 'cooking')
    showSuccess(`成功开始制作 ${ids.length} 道菜`)

    // 更新本地状态
    updateOrdersStatus(ids, 'cooking')
    resetSelection()
    closeBatchMode()
  } catch (error) {
    showError('批量操作失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

const handleMarkComplete = async (ids: string[]): Promise<void> => {
  try {
    batchLoading.value = true
    await kitchenApi.batchUpdateStatus(ids, 'completed')
    showSuccess(`成功完成 ${ids.length} 道菜`)

    updateOrdersStatus(ids, 'completed')
    resetSelection()
    closeBatchMode()
  } catch (error) {
    showError('标记完成失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

const handleQuickCompleteAll = async (): Promise<void> => {
  try {
    batchLoading.value = true
    const pendingOrderIds = orders.value
      .filter(o => o.status === 'pending')
      .map(o => o.id)

    await kitchenApi.batchUpdateStatus(pendingOrderIds, 'cooking')
    showSuccess(`一键完成 ${pendingOrderIds.length} 个待制作订单`)

    updateOrdersStatus(pendingOrderIds, 'cooking')
  } catch (error) {
    showError('一键完成失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

const handleQuickServeAll = async (): Promise<void> => {
  try {
    batchLoading.value = true
    const completedOrderIds = orders.value
      .filter(o => o.status === 'completed')
      .map(o => o.id)

    await kitchenApi.batchUpdateStatus(completedOrderIds, 'served')
    showSuccess(`一键出餐 ${completedOrderIds.length} 个已完成订单`)

    updateOrdersStatus(completedOrderIds, 'served')
  } catch (error) {
    showError('一键出餐失败，请重试')
  } finally {
    batchLoading.value = false
  }
}

// ========== 辅助方法 ==========

const updateOrdersStatus = (ids: string[], status: Order['status']): void => {
  ids.forEach(id => {
    const order = orders.value.find(o => o.id === id)
    if (order) {
      order.status = status
    }
  })
}

const isUrgent = (order: Order): boolean => {
  const waitTime = Date.now() - order.createTime.getTime()
  return waitTime > 30 * 60 * 1000 || order.urgent
}

const formatTime = (date: Date): string => {
  return new Intl.DateTimeFormat('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  }).format(date)
}
</script>
*/

// ========== 使用说明 ==========

/**
 * 1. 安装依赖
 * 无需额外安装依赖，所有功能使用Vue 3原生API和Element Plus
 *
 * 2. 导入样式文件
 * 在main.ts或App.vue中全局导入：
 *   import '@/styles/kitchen-touch.css'
 *
 * 或在特定页面导入：
 *   import '@/styles/kitchen-touch.css'  // 在<script setup>中
 *
 * 3. 组件注册
 * 在需要使用的页面中导入并注册：
 *   import BatchOperationPanel from '@/components/kitchen/BatchOperationPanel.vue'
 *   import ShortcutHelp from '@/components/kitchen/ShortcutHelp.vue'
 *
 * 4. Composables使用
 *   import { useKeyboardShortcuts } from '@/composables/kitchen/useKeyboardShortcuts.ts'
 *   import { useOperationFeedback } from '@/composables/kitchen/useOperationFeedback.ts'
 *
 *   const keyboard = useKeyboardShortcuts()  // 自动在挂载时注册，卸载时注销
 *   const feedback = useOperationFeedback()  // 提供统一的反馈方法
 *
 * 5. API对接
 * 批量操作需要调用🍳-2提供的后端API：
 *   POST /api/kitchen/orders/batch-update
 *   Body: { orderIds: string[], status: string }
 *
 * 6. 快捷键完整列表
 * 按键     功能                    说明
 * ----    --------------------    ------------------
 * 1-5     选中第N个待制作订单     数字键快速选择
 * Enter   开始制作选中订单       执行批量开始制作
 * Space   完成选中订单           标记为已完成
 * Tab     切换到下一状态栏目     导航不同状态区域
 * Esc     取消选择               清除所有选择
 * F1      刷新订单列表           获取最新数据
 * F5      强制刷新页面           重载整个页面
 * ?       显示/隐藏帮助面板     查看所有快捷键
 * Ctrl+A  全选/取消全选         批量选择所有订单
 *
 * 7. 音频反馈说明
 * 使用Web Audio API生成音效，无需外部音频文件：
 * - 成功：880Hz正弦波，150ms
 * - 警告：440Hz三角波，200ms
 * - 错误：220Hz锯齿波，300ms
 * - 紧急：1000Hz方波，500ms + 特殊震动模式
 *
 * 8. 触摸优化要点
 * - 所有可点击元素最小44x44px
 * - 重要按钮56px高度，更醒目
 * - 按压时有scale(0.95)反馈
 * - 触摸设备自动禁用hover效果
 * - 支持安全区域适配（刘海屏）
 *
 * 9. 性能优化建议
 * - 使用v-show而非v-if控制批量操作面板显示（避免重复创建）
 * - 大列表使用虚拟滚动（如vue-virtual-scroller）
 * - 音频Context复用，避免重复创建
 * - 快捷键监听器在组件卸载时自动清理
 *
 * 10. 可访问性支持
 * - 所有按钮有aria-label属性
 * - 键盘导航支持Tab键
 * - 高对比度颜色方案
 * - 屏幕阅读器友好的语义化标签
 */
