<template>
  <div class="station-print">
    <header class="print-header">
      <div class="header-left">
        <div class="logo">
          <span class="logo-icon">🖨️</span>
          <div class="logo-text">
            <h1>工位打印终端</h1>
            <span class="subtitle">Station Printer Terminal</span>
          </div>
        </div>
      </div>
      <div class="header-right">
        <div class="station-selector">
          <el-select v-model="selectedStation" placeholder="选择工位" size="large" style="width: 200px;">
            <el-option label="蒸箱工位" value="steamer" />
            <el-option label="炸锅工位" value="fryer" />
            <el-option label="炒锅工位" value="wok" />
            <el-option label="凉菜工位" value="cold" />
          </el-select>
        </div>
        <div class="time-display">
          <span class="date">{{ currentDate }}</span>
          <span class="time">{{ currentTime }}</span>
        </div>
      </div>
    </header>

    <main class="print-main">
      <div class="print-container">
        <div class="print-section">
          <h3>待制作菜品</h3>
          <div class="dish-list">
            <div v-for="dish in pendingDishes" :key="dish.id" class="dish-card">
              <div class="dish-info">
                <span class="dish-name">{{ dish.dishName }}</span>
                <span class="dish-quantity">× {{ dish.quantity }}</span>
              </div>
              <div class="dish-meta">
                <span class="order-number">订单: {{ dish.orderNumber }}</span>
                <span class="priority-badge" :class="getPriorityClass(dish.priority)" v-if="dish.priority > 0">
                  {{ getPriorityText(dish.priority) }}
                </span>
              </div>
              <div class="dish-actions">
                <el-button type="primary" size="small" @click="printDish(dish)">
                  🖨️ 打印追溯码
                </el-button>
              </div>
            </div>
            <div v-if="pendingDishes.length === 0" class="empty-state">
              <div class="empty-icon">📋</div>
              <p>暂无待制作菜品</p>
            </div>
          </div>
        </div>

        <div class="print-section">
          <h3>快速打印</h3>
          <div class="quick-print">
            <div class="quick-print-item" v-for="item in quickPrintItems" :key="item.id">
              <el-button type="default" size="large" @click="quickPrint(item)">
                {{ item.icon }} {{ item.name }}
              </el-button>
            </div>
          </div>
        </div>

        <div class="print-section">
          <h3>打印历史</h3>
          <div class="print-history">
            <div v-for="record in printHistory" :key="record.id" class="history-item">
              <span class="history-dish">{{ record.dishName }}</span>
              <span class="history-time">{{ formatTime(record.printTime) }}</span>
              <span class="history-status">已打印</span>
            </div>
            <div v-if="printHistory.length === 0" class="empty-state">
              <div class="empty-icon">📜</div>
              <p>暂无打印记录</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <el-dialog
      v-model="printPreviewVisible"
      title="追溯码预览"
      width="500px">
      <div class="print-preview">
        <div class="preview-label">
          <div class="label-header">
            <h3>食品追溯码</h3>
            <span class="label-station">{{ getStationName(selectedStation) }}</span>
          </div>
          <div class="label-content">
            <div class="label-item">
              <span class="label-key">菜品:</span>
              <span class="label-value">{{ currentPrintDish?.dishName }}</span>
            </div>
            <div class="label-item">
              <span class="label-key">时间:</span>
              <span class="label-value">{{ currentPrintTime }}</span>
            </div>
            <div class="label-qr">
              <div class="qr-placeholder">
                <span class="qr-icon">📱</span>
                <span class="qr-text">追溯码</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="printPreviewVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmPrint">确认打印</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

// 路由实例保留供后续页面跳转使用
const currentTime = ref('')
const currentDate = ref('')
const selectedStation = ref('')
let timeInterval: number | null = null

const pendingDishes = ref<any[]>([
  { id: 1, dishName: '鸡蛋羹', quantity: 3, orderNumber: 'ORD001', priority: 0 },
  { id: 2, dishName: '鸡蛋羹', quantity: 2, orderNumber: 'ORD002', priority: 1 },
  { id: 3, dishName: '蒸蛋羹', quantity: 5, orderNumber: 'ORD003', priority: 0 }
])

const quickPrintItems = ref([
  { id: 1, name: '鸡蛋羹', icon: '🥚' },
  { id: 2, name: '蒸蛋', icon: '🍳' },
  { id: 3, name: '包子', icon: '🥟' },
  { id: 4, name: '馒头', icon: '🍞' }
])

const printHistory = ref<any[]>([])

const printPreviewVisible = ref(false)
const currentPrintDish = ref<any>(null)
const currentPrintTime = ref('')

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  currentDate.value = now.toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })
}

const formatTime = (timeStr: string) => {
  if (!timeStr) return ''
  const date = new Date(timeStr)
  return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

const getPriorityText = (priority: number) => {
  const texts: Record<number, string> = {
    0: '',
    1: '加急',
    2: '特急'
  }
  return texts[priority] || ''
}

const getPriorityClass = (priority: number) => {
  const classes: Record<number, string> = {
    0: '',
    1: 'special',
    2: 'urgent'
  }
  return classes[priority] || ''
}

const getStationName = (station: string) => {
  const names: Record<string, string> = {
    steamer: '蒸箱工位',
    fryer: '炸锅工位',
    wok: '炒锅工位',
    cold: '凉菜工位'
  }
  return names[station] || ''
}

const printDish = (dish: any) => {
  currentPrintDish.value = dish
  currentPrintTime.value = new Date().toLocaleString('zh-CN')
  printPreviewVisible.value = true
}

const quickPrint = (item: any) => {
  const dish = {
    id: Date.now(),
    dishName: item.name,
    quantity: 1,
    orderNumber: 'QCK' + Date.now(),
    priority: 0
  }
  printDish(dish)
}

const confirmPrint = () => {
  if (!currentPrintDish.value) return

  const record = {
    id: Date.now(),
    dishName: currentPrintDish.value.dishName,
    orderNumber: currentPrintDish.value.orderNumber,
    printTime: new Date().toISOString()
  }

  printHistory.value.unshift(record)
  if (printHistory.value.length > 20) {
    printHistory.value.pop()
  }

  const index = pendingDishes.value.findIndex(d => d.id === currentPrintDish.value.id)
  if (index > -1) {
    pendingDishes.value.splice(index, 1)
  }

  printPreviewVisible.value = false
  ElMessage.success('追溯码打印成功')
}

onMounted(() => {
  updateTime()
  timeInterval = window.setInterval(updateTime, 1000)
})

onUnmounted(() => {
  if (timeInterval) clearInterval(timeInterval)
})
</script>

<style scoped>
.station-print {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.print-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}

.header-left .logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo-icon {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #8b5cf6 0%, #a855f7 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.logo-text h1 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
}

.logo-text .subtitle {
  font-size: 11px;
  color: #64748b;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 24px;
}

.time-display {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.time-display .date {
  font-size: 12px;
  color: #64748b;
}

.time-display .time {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  font-variant-numeric: tabular-numeric;
}

.print-main {
  flex: 1;
  padding: 20px;
  overflow: hidden;
}

.print-container {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  height: 100%;
}

.print-section {
  background: #1e293b;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #334155;
  display: flex;
  flex-direction: column;
}

.print-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.dish-list, .print-history {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dish-card {
  background: #0f172a;
  border-radius: 12px;
  padding: 16px;
  border: 2px solid #334155;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.dish-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dish-name {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}

.dish-quantity {
  font-size: 16px;
  font-weight: 600;
  color: #60a5fa;
}

.dish-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.order-number {
  font-size: 13px;
  color: #94a3b8;
}

.priority-badge {
  padding: 2px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 600;
}

.priority-badge.urgent {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
  animation: pulse-red 1.5s ease-in-out infinite;
}

.priority-badge.special {
  background: rgba(245, 158, 11, 0.2);
  color: #fbbf24;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}

.dish-actions {
  display: flex;
  justify-content: flex-end;
}

.quick-print {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.quick-print-item .el-button {
  width: 100%;
  height: 80px;
  font-size: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  justify-content: center;
}

.history-item {
  background: #0f172a;
  border-radius: 8px;
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border: 1px solid #334155;
}

.history-dish {
  font-size: 14px;
  font-weight: 500;
  color: #f1f5f9;
}

.history-time {
  font-size: 12px;
  color: #64748b;
}

.history-status {
  font-size: 12px;
  color: #4ade80;
  background: rgba(34, 197, 94, 0.1);
  padding: 2px 8px;
  border-radius: 4px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #64748b;
  flex: 1;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  opacity: 0.5;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.print-preview {
  text-align: center;
}

.preview-label {
  background: #f8fafc;
  border: 2px dashed #94a3b8;
  border-radius: 8px;
  padding: 24px;
  max-width: 300px;
  margin: 0 auto;
}

.label-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e2e8f0;
}

.label-header h3 {
  margin: 0;
  font-size: 16px;
  color: #1e293b;
}

.label-station {
  font-size: 12px;
  color: #64748b;
  background: #e2e8f0;
  padding: 2px 8px;
  border-radius: 4px;
}

.label-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.label-item {
  display: flex;
  justify-content: space-between;
}

.label-key {
  font-size: 14px;
  color: #64748b;
}

.label-value {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.label-qr {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.qr-placeholder {
  width: 120px;
  height: 120px;
  background: #f1f5f9;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 0 auto;
}

.qr-icon {
  font-size: 40px;
}

.qr-text {
  font-size: 12px;
  color: #64748b;
}
</style>
