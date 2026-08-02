<template>
  <div class="scan-page">
    <header class="scan-header">
      <button class="back-btn" @click="$router.back()">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M19 12H5M12 19l-7-7 7-7"></path>
        </svg>
        返回
      </button>
      <h2>扫码核销</h2>
      <div class="header-right">
        <div class="connection-status">
          <span class="status-dot online"></span>
          <span>系统在线</span>
        </div>
      </div>
    </header>

    <main class="scan-main">
      <div class="scan-container">
        <div class="scan-type-tabs">
          <div 
            class="tab-item" 
            :class="{ active: scanType === 'order' }" 
            @click="scanType = 'order'"
          >
            订单核销
          </div>
          <div 
            class="tab-item" 
            :class="{ active: scanType === 'coupon' }" 
            @click="scanType = 'coupon'"
          >
            优惠券核销
          </div>
          <div 
            class="tab-item" 
            :class="{ active: scanType === 'member' }" 
            @click="scanType = 'member'"
          >
            会员码
          </div>
        </div>

        <div class="scan-box">
          <div class="scan-instruction">
            <h3>{{ getScanTitle() }}</h3>
            <p>{{ getScanDescription() }}</p>
          </div>

          <CameraScanner 
            @scan="handleScanResult" 
            @error="handleScanError" 
          />

          <transition name="result-fade">
            <div v-if="scanResult" class="scan-result" :class="scanResult.success ? 'success' : 'error'">
              <div class="result-icon">
                <svg v-if="scanResult.success" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                  <polyline points="22 4 12 14.01 9 11.01"></polyline>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="15" y1="9" x2="9" y2="15"></line>
                  <line x1="9" y1="9" x2="15" y2="15"></line>
                </svg>
              </div>
              <div class="result-content">
                <span class="result-title">{{ scanResult.success ? '核销成功' : '核销失败' }}</span>
                <span class="result-message">{{ scanResult.message }}</span>
                <div v-if="scanResult.orderInfo" class="result-details">
                  <span>订单号: {{ scanResult.orderInfo.orderNumber }}</span>
                  <span>金额: ¥{{ scanResult.orderInfo.amount }}</span>
                </div>
              </div>
            </div>
          </transition>
        </div>

        <div class="tips-section">
          <div class="tip-card">
            <div class="tip-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="16" x2="12" y2="12"></line>
                <line x1="12" y1="8" x2="12.01" y2="8"></line>
              </svg>
            </div>
            <div class="tip-content">
              <h4>操作提示</h4>
              <ul>
                <li v-if="scanType === 'order'">扫描订单二维码完成核销</li>
                <li v-if="scanType === 'order'">核销后订单状态将更新为已完成</li>
                <li v-if="scanType === 'coupon'">扫描优惠券二维码进行核销</li>
                <li v-if="scanType === 'coupon'">核销后优惠券将标记为已使用</li>
                <li v-if="scanType === 'member'">扫描会员码识别会员身份</li>
                <li>支持手机摄像头扫码或手动输入</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import CameraScanner from '@/components/CameraScanner.vue'

interface ScanResult {
  success: boolean
  message: string
  orderInfo?: {
    orderNumber: string
    amount: number
  }
}

const scanResult = ref<ScanResult | null>(null)
const scanType = ref('order')

const getScanTitle = () => {
  const titles: Record<string, string> = {
    order: '请扫描订单二维码',
    coupon: '请扫描优惠券二维码',
    member: '请扫描会员码'
  }
  return titles[scanType.value] || '请扫描二维码'
}

const getScanDescription = () => {
  const descriptions: Record<string, string> = {
    order: '扫描客户订单二维码完成核销',
    coupon: '扫描优惠券二维码进行核销',
    member: '扫描会员码识别会员身份'
  }
  return descriptions[scanType.value] || '将二维码对准扫描区域'
}

const handleScanResult = async (code: string) => {
  scanResult.value = null

  try {
    let result: any
    
    if (scanType.value === 'order') {
      result = await request.post(`/v1/orders/verify`, { code })
    } else if (scanType.value === 'coupon') {
      result = await request.post(`/v1/coupons/verify`, { code })
    } else {
      result = await request.post(`/v1/members/identify`, { code })
    }
    
    if (result.success !== false) {
      const data = result.data || result
      scanResult.value = {
        success: true,
        message: data.message || '核销成功',
        orderInfo: data.orderNumber ? {
          orderNumber: data.orderNumber,
          amount: data.amount || 0
        } : undefined
      }
      ElMessage.success(data.message || '核销成功')
    } else {
      scanResult.value = {
        success: false,
        message: result.message || '核销失败'
      }
      ElMessage.error(result.message || '核销失败')
    }

    setTimeout(() => {
      scanResult.value = null
    }, 3000)
  } catch (error: any) {
    scanResult.value = {
      success: false,
      message: error.message || '核销失败'
    }
    ElMessage.error(error.message || '核销失败')
  }
}

const handleScanError = (message: string) => {
  ElMessage.warning(message)
}
</script>

<style scoped>
.scan-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
}

.scan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #16213e;
  border-bottom: 1px solid #2a2a4a;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #2a2a4a;
  border: none;
  border-radius: 8px;
  color: #888;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.back-btn svg {
  width: 18px;
  height: 18px;
}

.back-btn:hover {
  background: #3a3a5a;
  color: #fff;
}

.scan-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.header-right {
  display: flex;
  align-items: center;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(16, 185, 129, 0.1);
  border-radius: 16px;
  font-size: 13px;
  color: #10b981;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #10b981;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.9); }
}

.scan-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  overflow-y: auto;
}

.scan-container {
  width: 100%;
  max-width: 560px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.scan-type-tabs {
  display: flex;
  justify-content: center;
  gap: 12px;
  background: #16213e;
  padding: 8px;
  border-radius: 12px;
}

.tab-item {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #888;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-item:hover {
  color: #fff;
}

.tab-item.active {
  background: linear-gradient(135deg, #e94560 0%, #ff6b8a 100%);
  color: #fff;
}

.scan-box {
  background: #16213e;
  border-radius: 24px;
  padding: 32px;
  border: 2px solid #2a2a4a;
}

.scan-instruction {
  text-align: center;
  margin-bottom: 24px;
}

.scan-instruction h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #fff;
}

.scan-instruction p {
  margin: 0;
  font-size: 14px;
  color: #888;
}

.scan-result {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-top: 24px;
  padding: 16px 20px;
  border-radius: 12px;
}

.scan-result.success {
  background: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
}

.scan-result.error {
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
}

.result-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.scan-result.success .result-icon {
  background: rgba(16, 185, 129, 0.2);
  color: #10b981;
}

.scan-result.error .result-icon {
  background: rgba(239, 68, 68, 0.2);
  color: #ef4444;
}

.result-icon svg {
  width: 24px;
  height: 24px;
}

.result-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.result-title {
  font-size: 15px;
  font-weight: 600;
}

.scan-result.success .result-title {
  color: #10b981;
}

.scan-result.error .result-title {
  color: #ef4444;
}

.result-message {
  font-size: 13px;
  color: #888;
}

.result-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(136, 136, 136, 0.2);
  font-size: 12px;
  color: #666;
}

.tips-section {
  display: flex;
  justify-content: center;
}

.tip-card {
  display: flex;
  gap: 16px;
  background: #16213e;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid #2a2a4a;
  max-width: 480px;
}

.tip-icon {
  width: 44px;
  height: 44px;
  background: rgba(233, 69, 96, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #e94560;
  flex-shrink: 0;
}

.tip-icon svg {
  width: 24px;
  height: 24px;
}

.tip-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
}

.tip-content ul {
  margin: 0;
  padding-left: 16px;
}

.tip-content li {
  font-size: 13px;
  color: #888;
  margin-bottom: 4px;
}

.tip-content li:last-child {
  margin-bottom: 0;
}

.result-fade-enter-active,
.result-fade-leave-active {
  transition: all 0.3s ease;
}

.result-fade-enter-from,
.result-fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}
</style>
