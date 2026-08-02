<template>
  <div class="camera-scanner">
    <div class="scanner-container">
      <div v-if="!isScanning" class="scanner-placeholder" @click="startScanning">
        <div class="placeholder-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"></path>
            <circle cx="12" cy="13" r="4"></circle>
          </svg>
        </div>
        <p class="placeholder-text">点击启动摄像头扫码</p>
        <p class="placeholder-hint">支持条形码和二维码</p>
      </div>
      
      <div v-show="isScanning" class="scanner-active">
        <div id="qr-reader-pos" class="qr-reader"></div>
        <div class="scanner-overlay">
          <div class="scan-frame">
            <div class="corner tl"></div>
            <div class="corner tr"></div>
            <div class="corner bl"></div>
            <div class="corner br"></div>
          </div>
          <p class="scan-tip">将条码/二维码放入框内自动扫描</p>
        </div>
      </div>
      
      <div v-if="error" class="scanner-error">
        <div class="error-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" y1="8" x2="12" y2="12"></line>
            <line x1="12" y1="16" x2="12.01" y2="16"></line>
          </svg>
        </div>
        <p class="error-text">{{ error }}</p>
        <button class="retry-btn" @click="startScanning">重试</button>
      </div>
    </div>
    
    <div class="scanner-controls" v-if="isScanning">
      <button class="control-btn stop-btn" @click="stopScanning">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="6" y="6" width="12" height="12"></rect>
        </svg>
        停止扫描
      </button>
      <button class="control-btn switch-btn" @click="switchCamera">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"></polyline>
          <polyline points="1 20 1 14 7 14"></polyline>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"></path>
        </svg>
        切换摄像头
      </button>
    </div>
    
    <div class="manual-input">
      <input 
        type="text" 
        v-model="manualCode" 
        placeholder="或手动输入码..."
        @keyup.enter="submitManualCode"
        class="manual-input-field"
      />
      <button class="manual-submit-btn" @click="submitManualCode">
        确认
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onUnmounted } from 'vue'
import { Html5Qrcode } from 'html5-qrcode'

const emit = defineEmits<{
  (e: 'scan', code: string): void
  (e: 'error', message: string): void
}>()

const isScanning = ref(false)
const error = ref('')
const manualCode = ref('')
const currentCamera = ref<'user' | 'environment'>('environment')

let html5QrCode: Html5Qrcode | null = null

const startScanning = async () => {
  error.value = ''
  isScanning.value = true
  
  try {
    await new Promise(resolve => setTimeout(resolve, 100))
    
    html5QrCode = new Html5Qrcode('qr-reader-pos')
    
    const cameras = await Html5Qrcode.getCameras()
    if (cameras && cameras.length === 0) {
      throw new Error('未检测到摄像头设备')
    }
    
    const config = {
      fps: 10,
      qrbox: { width: 250, height: 250 },
      aspectRatio: 1.0
    }
    
    await html5QrCode.start(
      { facingMode: currentCamera.value },
      config,
      (decodedText) => {
        emit('scan', decodedText)
        stopScanning()
      },
      () => {}
    )
  } catch (err: any) {
    console.error('启动摄像头失败:', err)
    isScanning.value = false
    
    let errorMsg = '无法启动摄像头'
    if (err.name === 'NotAllowedError') {
      errorMsg = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
    } else if (err.name === 'NotFoundError') {
      errorMsg = '未检测到摄像头设备'
    } else if (err.name === 'NotReadableError') {
      errorMsg = '摄像头被其他应用占用'
    } else if (err.message) {
      errorMsg = err.message
    }
    
    error.value = errorMsg
    emit('error', errorMsg)
  }
}

const stopScanning = async () => {
  if (html5QrCode && html5QrCode.isScanning) {
    try {
      await html5QrCode.stop()
      html5QrCode.clear()
    } catch (err) {
      console.error('停止扫描失败:', err)
    }
  }
  isScanning.value = false
}

const switchCamera = async () => {
  await stopScanning()
  currentCamera.value = currentCamera.value === 'environment' ? 'user' : 'environment'
  await startScanning()
}

const submitManualCode = () => {
  if (manualCode.value.trim()) {
    emit('scan', manualCode.value.trim())
    manualCode.value = ''
  }
}

onUnmounted(() => {
  stopScanning()
})
</script>

<style scoped>
.camera-scanner {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.scanner-container {
  position: relative;
  width: 100%;
  min-height: 280px;
  background: #1a1a2e;
  border-radius: 16px;
  overflow: hidden;
}

.scanner-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 280px;
  cursor: pointer;
  transition: background 0.3s;
}

.scanner-placeholder:hover {
  background: #16213e;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  color: #e94560;
  margin-bottom: 16px;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 8px 0;
}

.placeholder-hint {
  font-size: 14px;
  color: #888;
  margin: 0;
}

.scanner-active {
  position: relative;
  width: 100%;
  height: 280px;
}

.qr-reader {
  width: 100%;
  height: 100%;
}

.qr-reader :deep(video) {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.scanner-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  pointer-events: none;
}

.scan-frame {
  width: 200px;
  height: 200px;
  position: relative;
}

.corner {
  position: absolute;
  width: 30px;
  height: 30px;
  border-color: #e94560;
  border-style: solid;
  border-width: 0;
}

.corner.tl {
  top: 0;
  left: 0;
  border-top-width: 4px;
  border-left-width: 4px;
  border-top-left-radius: 8px;
}

.corner.tr {
  top: 0;
  right: 0;
  border-top-width: 4px;
  border-right-width: 4px;
  border-top-right-radius: 8px;
}

.corner.bl {
  bottom: 0;
  left: 0;
  border-bottom-width: 4px;
  border-left-width: 4px;
  border-bottom-left-radius: 8px;
}

.corner.br {
  bottom: 0;
  right: 0;
  border-bottom-width: 4px;
  border-right-width: 4px;
  border-bottom-right-radius: 8px;
}

.scan-tip {
  position: absolute;
  bottom: 20px;
  left: 0;
  right: 0;
  text-align: center;
  color: #fff;
  font-size: 14px;
  background: rgba(0, 0, 0, 0.5);
  padding: 8px 16px;
  margin: 0;
}

.scanner-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 280px;
  padding: 20px;
}

.error-icon {
  width: 48px;
  height: 48px;
  color: #ef4444;
  margin-bottom: 16px;
}

.error-icon svg {
  width: 100%;
  height: 100%;
}

.error-text {
  font-size: 14px;
  color: #888;
  text-align: center;
  margin: 0 0 16px 0;
}

.retry-btn {
  padding: 8px 24px;
  background: #e94560;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.retry-btn:hover {
  background: #c73e54;
}

.scanner-controls {
  display: flex;
  gap: 12px;
  justify-content: center;
}

.control-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.control-btn svg {
  width: 18px;
  height: 18px;
}

.stop-btn {
  background: #ef4444;
  color: #fff;
}

.stop-btn:hover {
  background: #dc2626;
}

.switch-btn {
  background: #16213e;
  color: #fff;
  border: 1px solid #2a2a4a;
}

.switch-btn:hover {
  background: #1f3460;
}

.manual-input {
  display: flex;
  gap: 12px;
}

.manual-input-field {
  flex: 1;
  padding: 12px 16px;
  background: #16213e;
  border: 1px solid #2a2a4a;
  border-radius: 8px;
  color: #fff;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;
}

.manual-input-field::placeholder {
  color: #666;
}

.manual-input-field:focus {
  border-color: #e94560;
}

.manual-submit-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #e94560 0%, #ff6b8a 100%);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}

.manual-submit-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(233, 69, 96, 0.4);
}
</style>
