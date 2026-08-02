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
        <div :id="readerId" class="qr-reader"></div>
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
        placeholder="或手动输入追溯码..."
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
import { ref, onUnmounted, computed } from 'vue'
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode'

const emit = defineEmits<{
  (e: 'scan', code: string): void
  (e: 'error', message: string): void
}>()

const readerId = computed(() => `qr-reader-${Date.now()}`)
const isScanning = ref(false)
const error = ref('')
const manualCode = ref('')
const currentCamera = ref<'user' | 'environment'>('environment')

let html5QrCode: Html5Qrcode | null = null

const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) || 
  (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1)

const isSafari = /^((?!chrome|android).)*safari/i.test(navigator.userAgent)

const startScanning = async () => {
  error.value = ''
  isScanning.value = true
  
  try {
    await new Promise(resolve => setTimeout(resolve, 200))
    
    const actualReaderId = readerId.value
    html5QrCode = new Html5Qrcode(actualReaderId)
    
    const cameras = await Html5Qrcode.getCameras()
    if (cameras && cameras.length === 0) {
      throw new Error('未检测到摄像头设备')
    }
    
    let selectedCamera: any = { facingMode: currentCamera.value }
    
    if (isIOS || isSafari) {
      if (cameras && cameras.length > 0) {
        const backCamera = cameras.find((c: any) => 
          c.label && c.label.toLowerCase().includes('back')
        ) || cameras.find((c: any) => 
          c.label && c.label.toLowerCase().includes('rear')
        ) || cameras.find((c: any) => 
          c.label && c.label.toLowerCase().includes('environment')
        )
        
        if (backCamera) {
          selectedCamera = { deviceId: { exact: backCamera.id } }
        } else if (cameras.length > 0) {
          selectedCamera = { deviceId: { exact: cameras[cameras.length > 1 ? 1 : 0].id } }
        }
      }
    }
    
    const config = {
      fps: 10,
      qrbox: { width: 250, height: 250 },
      aspectRatio: 1.0,
      formatsToSupport: [
        Html5QrcodeSupportedFormats.QR_CODE,
        Html5QrcodeSupportedFormats.EAN_13,
        Html5QrcodeSupportedFormats.EAN_8,
        Html5QrcodeSupportedFormats.CODE_128,
        Html5QrcodeSupportedFormats.CODE_39,
        Html5QrcodeSupportedFormats.UPC_A,
        Html5QrcodeSupportedFormats.UPC_E,
        Html5QrcodeSupportedFormats.ITF,
        Html5QrcodeSupportedFormats.DATA_MATRIX,
      ]
    }
    
    await html5QrCode.start(
      selectedCamera,
      config,
      (decodedText) => {
        emit('scan', decodedText)
        stopScanning()
      },
      (errorMessage) => {
        console.log('扫描警告:', errorMessage)
      }
    )
  } catch (err: any) {
    console.error('启动摄像头失败:', err)
    isScanning.value = false
    
    let errorMsg = '无法启动摄像头'
    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      errorMsg = '摄像头权限被拒绝，请在浏览器设置中允许访问摄像头'
    } else if (err.name === 'NotFoundError') {
      errorMsg = '未检测到摄像头设备'
    } else if (err.name === 'NotReadableError') {
      errorMsg = '摄像头被其他应用占用'
    } else if (err.name === 'OverconstrainedError') {
      errorMsg = '摄像头不支持请求的配置，正在尝试其他摄像头...'
      if (html5QrCode) {
        try {
          const cameras = await Html5Qrcode.getCameras()
          if (cameras && cameras.length > 0) {
            await html5QrCode.start(
              { deviceId: { exact: cameras[0].id } },
              { fps: 10, qrbox: { width: 250, height: 250 } },
              (decodedText) => {
                emit('scan', decodedText)
                stopScanning()
              },
              () => {}
            )
            return
          }
        } catch (retryErr) {
          console.error('重试失败:', retryErr)
        }
      }
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
  background: #0f172a;
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
  background: #1e293b;
}

.placeholder-icon {
  width: 80px;
  height: 80px;
  color: #3b82f6;
  margin-bottom: 16px;
}

.placeholder-icon svg {
  width: 100%;
  height: 100%;
}

.placeholder-text {
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
  margin: 0 0 8px 0;
}

.placeholder-hint {
  font-size: 14px;
  color: #64748b;
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

.qr-reader :deep(#qr-shaded-region) {
  border: none !important;
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
  border-color: #3b82f6;
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
  color: #94a3b8;
  text-align: center;
  margin: 0 0 16px 0;
}

.retry-btn {
  padding: 8px 24px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s;
}

.retry-btn:hover {
  background: #2563eb;
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
  background: #1e293b;
  color: #f1f5f9;
  border: 1px solid #334155;
}

.switch-btn:hover {
  background: #334155;
}

.manual-input {
  display: flex;
  gap: 12px;
}

.manual-input-field {
  flex: 1;
  padding: 12px 16px;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 8px;
  color: #f1f5f9;
  font-size: 15px;
  outline: none;
  transition: border-color 0.3s;
}

.manual-input-field::placeholder {
  color: #64748b;
}

.manual-input-field:focus {
  border-color: #3b82f6;
}

.manual-submit-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #3b82f6 0%, #60a5fa 100%);
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
  box-shadow: 0 4px 12px rgba(59, 130, 246, 0.4);
}
</style>
