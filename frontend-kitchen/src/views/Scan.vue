<template>
  <div class="scan-page">
    <header class="scan-header">
      <el-button class="back-btn" @click="$router.back()">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h2>扫码制作</h2>
      <div class="header-right">
        <el-button class="scan-entry-btn" @click="goToScanPage">
          <span class="scan-icon">📷</span>
          <span>摄像头扫码</span>
        </el-button>
        <div class="scan-indicator" :class="{ scanning: isScanning }">
          <span class="scan-icon">📷</span>
          <span class="scan-status">{{ scanStatus }}</span>
        </div>
      </div>
    </header>

    <main class="scan-main">
      <div class="scan-container">
        <div class="scan-type-tabs">
          <el-radio-group v-model="scanType" size="large">
            <el-radio-button label="material">原料追溯码</el-radio-button>
            <el-radio-button label="tray">托盘码</el-radio-button>
            <el-radio-button label="food">食品追溯码</el-radio-button>
          </el-radio-group>
        </div>

        <div class="scan-box" :class="{ scanning: isScanning }">
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
                <el-icon v-if="scanResult.success"><CircleCheckFilled /></el-icon>
                <el-icon v-else><CircleCloseFilled /></el-icon>
              </div>
              <div class="result-content">
                <span class="result-title">{{ scanResult.success ? '扫描成功' : '扫描失败' }}</span>
                <span class="result-message">{{ scanResult.message }}</span>
                <div v-if="scanResult.orderInfo" class="result-details">
                  <span>订单号: {{ scanResult.orderInfo.orderNumber }}</span>
                  <span>状态: {{ scanResult.orderInfo.status }}</span>
                </div>
              </div>
            </div>
          </transition>
        </div>

        <div class="tips-section">
          <div class="tip-card">
            <div class="tip-icon">
              <el-icon><InfoFilled /></el-icon>
            </div>
            <div class="tip-content">
              <h4>操作提示</h4>
              <ul>
                <li><strong>原料追溯码</strong>：扫描后关联订单并开始制作</li>
                <li><strong>托盘码</strong>：扫描后确认出餐（如TRAY001）</li>
                <li><strong>食品追溯码</strong>：扫描后完成订单并通知取餐</li>
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
import {
  ArrowLeft,
  CircleCheckFilled,
  CircleCloseFilled,
  InfoFilled
} from '@element-plus/icons-vue'
import request from '@/api/request'
import CameraScanner from '@/components/CameraScanner.vue'

interface ScanResult {
  success: boolean
  message: string
  orderInfo?: {
    orderNumber: string
    status: string
  }
}

const isScanning = ref(false)
const scanStatus = ref('等待扫码')
const scanResult = ref<ScanResult | null>(null)
const scanType = ref('material')

const getScanTitle = () => {
  const titles: Record<string, string> = {
    material: '请扫描原料追溯码',
    tray: '请扫描托盘码',
    food: '请扫描食品追溯码'
  }
  return titles[scanType.value] || '请扫描二维码'
}

const getScanDescription = () => {
  const descriptions: Record<string, string> = {
    material: '扫描原料追溯码关联订单并开始制作',
    tray: '扫描托盘码确认出餐（如TRAY001）',
    food: '扫描食品追溯码完成订单并通知取餐'
  }
  return descriptions[scanType.value] || '将二维码对准扫描区域'
}

const detectCodeType = (code: string): string => {
  const upperCode = code.toUpperCase()
  if (upperCode.startsWith('TRAY')) {
    return 'tray'
  }
  if (upperCode.startsWith('FTC') || upperCode.startsWith('FOOD')) {
    return 'food'
  }
  if (upperCode.startsWith('MTC') || upperCode.startsWith('MAT')) {
    return 'material'
  }
  return scanType.value
}

const handleScanResult = async (code: string) => {
  isScanning.value = true
  scanResult.value = null

  try {
    const actualType = detectCodeType(code)
    let result: any
    
    if (actualType === 'tray') {
      result = await request.post(`/v1/tray/scan-serve?trayCode=${encodeURIComponent(code)}`)
      
      if (result.code === 200 || result.success !== false) {
        const data = result.data || result
        scanResult.value = {
          success: true,
          message: `托盘 ${code} 出餐确认成功`,
          orderInfo: data.orderNumber ? {
            orderNumber: data.orderNumber,
            status: data.status
          } : undefined
        }
        ElMessage.success(`托盘 ${code} 出餐确认成功`)
      } else {
        scanResult.value = {
          success: false,
          message: result.message || '托盘出餐确认失败'
        }
        ElMessage.error(result.message || '托盘出餐确认失败')
      }
    } else if (actualType === 'food') {
      result = await request.post(`/v1/kitchen/scan-food-trace-code?traceCode=${encodeURIComponent(code)}`)
      
      if (result.code === 200 || result.success !== false) {
        const data = result.data || result
        scanResult.value = {
          success: true,
          message: data.message || '食品追溯码扫描成功',
          orderInfo: data.kitchenOrder ? {
            orderNumber: data.kitchenOrder.orderNumber,
            status: data.kitchenOrder.status
          } : undefined
        }
        ElMessage.success(data.message || '扫描成功')
      } else {
        scanResult.value = {
          success: false,
          message: result.message || '扫描失败'
        }
        ElMessage.error(result.message || '扫描失败')
      }
    } else {
      result = await request.post(`/v1/kitchen/scan?traceCode=${encodeURIComponent(code)}`)
      
      if (result.code === 200 || result.success !== false) {
        const data = result.data || result
        scanResult.value = {
          success: true,
          message: data.message || '原料扫描成功',
          orderInfo: data.orderNumber ? {
            orderNumber: data.orderNumber,
            status: data.orderStatusChange || ''
          } : undefined
        }
        ElMessage.success(data.message || '扫描成功')
      } else {
        scanResult.value = {
          success: false,
          message: (result as Record<string, unknown>).message as string || '扫描失败'
        }
        ElMessage.error((result as Record<string, unknown>).message as string || '扫描失败')
      }
    }

    setTimeout(() => {
      scanResult.value = null
    }, 3000)
  } catch (error: any) {
    scanResult.value = {
      success: false,
      message: error.message || '扫描失败'
    }
    ElMessage.error(error.message || '扫描失败')
  } finally {
    isScanning.value = false
  }
}

const handleScanError = (message: string) => {
  ElMessage.warning(message)
}

const goToScanPage = () => {
  ElMessage.info('当前页面即为扫码页面')
}
</script>

<style scoped>
.scan-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
}

.scan-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}

.back-btn {
  border: none;
  background: #334155;
  color: #94a3b8;
  font-weight: 500;
}

.back-btn:hover {
  background: #475569;
  color: #f1f5f9;
}

.scan-header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.scan-entry-btn {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  border: none;
  color: #fff;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.3s ease;
}

.scan-entry-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}

.scan-entry-btn .scan-icon {
  font-size: 18px;
}

.scan-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #334155;
  border-radius: 8px;
  font-size: 14px;
  color: #94a3b8;
}

.scan-indicator.scanning {
  background: rgba(59, 130, 246, 0.2);
  color: #60a5fa;
}

.scan-type-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 24px;
}

.scan-type-tabs :deep(.el-radio-button__inner) {
  background: #1e293b;
  border-color: #334155;
  color: #94a3b8;
}

.scan-type-tabs :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
  border-color: #3b82f6;
  color: #fff;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 14px;
  background: rgba(34, 197, 94, 0.1);
  border-radius: 16px;
  font-size: 13px;
  color: #4ade80;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.online {
  background: #22c55e;
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

.scan-box {
  background: #1e293b;
  border-radius: 24px;
  padding: 32px;
  border: 2px solid #334155;
  transition: all 0.3s ease;
}

.scan-box.scanning {
  border-color: #3b82f6;
  box-shadow: 0 0 40px rgba(59, 130, 246, 0.3);
}

.scan-instruction {
  text-align: center;
  margin-bottom: 24px;
}

.scan-instruction h3 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: #f1f5f9;
}

.scan-instruction p {
  margin: 0;
  font-size: 14px;
  color: #94a3b8;
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
  background: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
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
  background: rgba(34, 197, 94, 0.2);
  color: #4ade80;
}

.scan-result.error .result-icon {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
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
  color: #4ade80;
}

.scan-result.error .result-title {
  color: #f87171;
}

.result-message {
  font-size: 13px;
  color: #94a3b8;
}

.result-details {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  font-size: 12px;
  color: #64748b;
}

.tips-section {
  display: flex;
  justify-content: center;
}

.tip-card {
  display: flex;
  gap: 16px;
  background: #1e293b;
  border-radius: 16px;
  padding: 20px 24px;
  border: 1px solid #334155;
  max-width: 480px;
}

.tip-icon {
  width: 44px;
  height: 44px;
  background: rgba(59, 130, 246, 0.1);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #60a5fa;
  flex-shrink: 0;
}

.tip-content h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #f1f5f9;
}

.tip-content ul {
  margin: 0;
  padding-left: 16px;
}

.tip-content li {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.tip-content li:last-child {
  margin-bottom: 0;
}

.tip-content li strong {
  color: #f1f5f9;
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
