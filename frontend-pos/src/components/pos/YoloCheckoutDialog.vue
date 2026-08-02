<template>
  <el-dialog
    v-model="visible"
    title="YOLO 辅助收银"
    width="640px"
    center
    :close-on-click-modal="false"
    :lock-scroll="false"
    class="yolo-dialog"
  >
    <div class="yolo-content">
      <!-- 服务未启用提示 -->
      <el-alert
        v-if="!yoloEnabled"
        type="warning"
        :closable="false"
        show-icon
      >
        <template #title>YOLO 服务未启用</template>
        <template #default>
          <p style="margin: 4px 0 0; font-size: 12px;">
            后端未配置 YOLO 商品识别服务。请联系管理员在 application.yml 中设置
            <code>yolo.product.enabled=true</code>。
          </p>
        </template>
      </el-alert>

      <!-- 摄像头/上传区域 -->
      <div class="capture-section" v-if="yoloEnabled">
        <div class="capture-preview">
          <video
            v-if="cameraActive"
            ref="videoRef"
            autoplay
            playsinline
            class="camera-video"
          ></video>
          <img
            v-else-if="capturedImage"
            :src="capturedImage"
            class="captured-image"
            alt="拍摄预览"
          />
          <div v-else class="capture-placeholder">
            <el-icon :size="48"><Camera /></el-icon>
            <p>点击下方按钮开始拍摄</p>
          </div>
        </div>

        <div class="capture-actions">
          <el-button
            v-if="!cameraActive && !capturedImage"
            type="primary"
            @click="startCamera"
            :loading="startingCamera"
          >
            <el-icon><VideoCamera /></el-icon>
            启动摄像头
          </el-button>
          <el-button
            v-if="cameraActive"
            type="success"
            @click="captureAndRecognize"
            :loading="recognizing"
          >
            <el-icon><Camera /></el-icon>
            拍摄并识别
          </el-button>
          <el-button
            v-if="cameraActive"
            @click="stopCamera"
          >
            关闭摄像头
          </el-button>
          <el-button
            v-if="!cameraActive && capturedImage"
            @click="retake"
          >
            重新拍摄
          </el-button>
          <el-upload
            v-if="!cameraActive"
            :show-file-list="false"
            :before-upload="handleUpload"
            accept="image/*"
          >
            <el-button>
              <el-icon><Upload /></el-icon>
              上传图片
            </el-button>
          </el-upload>
        </div>
      </div>

      <!-- 识别结果区域 -->
      <div v-if="recognitionResult" class="result-section">
        <el-divider content-position="left">
          识别结果（{{ recognitionResult.processingTimeMs }}ms）
        </el-divider>

        <div v-if="recognitionResult.success && recognitionResult.products?.length" class="products-list">
          <div
            v-for="(product, index) in recognitionResult.products"
            :key="index"
            class="product-item"
            :class="{ 'selected': selectedProducts.has(index) }"
            @click="toggleSelect(index)"
          >
            <el-checkbox :model-value="selectedProducts.has(index)" />
            <div class="product-info">
              <div class="product-name">{{ product.foodName }}</div>
              <div class="product-meta">
                <span class="price">¥{{ product.price.toFixed(2) }}</span>
                <span class="qty">x{{ product.quantity }}</span>
                <el-tag size="small" type="success">
                  {{ (product.confidence * 100).toFixed(0) }}%
                </el-tag>
              </div>
            </div>
            <div class="product-subtotal">
              ¥{{ (product.price * product.quantity).toFixed(2) }}
            </div>
          </div>
        </div>

        <el-empty
          v-else-if="recognitionResult.success && !recognitionResult.products?.length"
          description="未识别到任何商品"
        />

        <el-alert
          v-else
          type="error"
          :closable="false"
          show-icon
        >
          {{ recognitionResult.errorMessage || '识别失败' }}
        </el-alert>

        <!-- 未匹配检测框提示 -->
        <div v-if="recognitionResult.unmatchedDetections?.length" class="unmatched-section">
          <el-text type="info" size="small">
            有 {{ recognitionResult.unmatchedDetections.length }} 个检测框未匹配到 SKU，请手动添加
          </el-text>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button
        type="primary"
        :disabled="selectedProducts.size === 0"
        @click="handleConfirm"
      >
        加入购物车（{{ selectedProducts.size }} 项）
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, VideoCamera, Upload } from '@element-plus/icons-vue'
import { posApi, type YoloRecognitionResult, type YoloRecognizedProduct } from '@/api/posApi'

interface CartItem {
  id: string
  name: string
  price: number
  quantity: number
  dishType: string
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'add-to-cart': [items: CartItem[]]
}>()

const visible = ref(props.modelValue)
const yoloEnabled = ref(true)
const cameraActive = ref(false)
const startingCamera = ref(false)
const recognizing = ref(false)
const capturedImage = ref('')
const recognitionResult = ref<YoloRecognitionResult | null>(null)
const selectedProducts = ref<Set<number>>(new Set())
const videoRef = ref<HTMLVideoElement | null>(null)
let stream: MediaStream | null = null

watch(() => props.modelValue, (val) => {
  visible.value = val
  if (val) {
    checkYoloStatus()
  } else {
    stopCamera()
    resetState()
  }
})
watch(visible, (val) => {
  emit('update:modelValue', val)
})

/** 查询 YOLO 服务状态 */
const checkYoloStatus = async () => {
  try {
    const result = await posApi.getYoloStatus() as unknown as { enabled: boolean }
    yoloEnabled.value = result?.enabled ?? false
  } catch {
    yoloEnabled.value = false
  }
}

/** 启动摄像头 */
const startCamera = async () => {
  startingCamera.value = true
  try {
    stream = await navigator.mediaDevices.getUserMedia({
      video: { facingMode: 'environment', width: 1280, height: 720 }
    })
    await nextTick()
    if (videoRef.value) {
      videoRef.value.srcObject = stream
    }
    cameraActive.value = true
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '摄像头访问失败'
    ElMessage.error(`无法启动摄像头: ${errMsg}`)
  } finally {
    startingCamera.value = false
  }
}

/** 停止摄像头 */
const stopCamera = () => {
  if (stream) {
    stream.getTracks().forEach(track => track.stop())
    stream = null
  }
  cameraActive.value = false
}

/** 拍摄并识别 */
const captureAndRecognize = async () => {
  if (!videoRef.value) return

  recognizing.value = true
  try {
    const canvas = document.createElement('canvas')
    canvas.width = videoRef.value.videoWidth
    canvas.height = videoRef.value.videoHeight
    const ctx = canvas.getContext('2d')
    if (!ctx) {
      ElMessage.error('无法获取画布上下文')
      return
    }
    ctx.drawImage(videoRef.value, 0, 0, canvas.width, canvas.height)
    capturedImage.value = canvas.toDataURL('image/jpeg', 0.8)

    stopCamera()

    const base64Data = capturedImage.value.split(',')[1]
    const result = await posApi.recognizeProducts(base64Data) as unknown as YoloRecognitionResult
    recognitionResult.value = result

    selectedProducts.value = new Set(
      (result.products || []).map((_, idx) => idx)
    )

    if (result.success && result.products?.length) {
      ElMessage.success(`识别到 ${result.products.length} 个商品`)
    } else if (result.success && !result.products?.length) {
      ElMessage.info('未识别到任何商品')
    } else {
      ElMessage.warning(result.errorMessage || '识别失败')
    }
  } catch (error: unknown) {
    const errMsg = error instanceof Error ? error.message : '识别异常'
    ElMessage.error(errMsg)
  } finally {
    recognizing.value = false
  }
}

/** 重新拍摄 */
const retake = () => {
  capturedImage.value = ''
  recognitionResult.value = null
  selectedProducts.value.clear()
  startCamera()
}

/** 处理上传图片 */
const handleUpload = (file: File) => {
  const reader = new FileReader()
  reader.onload = async (e) => {
    capturedImage.value = e.target?.result as string
    recognizing.value = true
    try {
      const base64Data = (e.target?.result as string).split(',')[1]
      const result = await posApi.recognizeProducts(base64Data) as unknown as YoloRecognitionResult
      recognitionResult.value = result
      selectedProducts.value = new Set(
        (result.products || []).map((_, idx) => idx)
      )
      if (result.success && result.products?.length) {
        ElMessage.success(`识别到 ${result.products.length} 个商品`)
      }
    } catch (error: unknown) {
      const errMsg = error instanceof Error ? error.message : '识别异常'
      ElMessage.error(errMsg)
    } finally {
      recognizing.value = false
    }
  }
  reader.readAsDataURL(file)
  return false
}

/** 切换商品选中状态 */
const toggleSelect = (index: number) => {
  if (selectedProducts.value.has(index)) {
    selectedProducts.value.delete(index)
  } else {
    selectedProducts.value.add(index)
  }
  selectedProducts.value = new Set(selectedProducts.value)
}

/** 确认加入购物车 */
const handleConfirm = () => {
  if (!recognitionResult.value?.products) return

  const items: CartItem[] = []
  selectedProducts.value.forEach(idx => {
    const product: YoloRecognizedProduct = recognitionResult.value!.products[idx]
    items.push({
      id: product.foodId,
      name: product.foodName,
      price: product.price,
      quantity: product.quantity,
      dishType: 'single'
    })
  })

  emit('add-to-cart', items)
  ElMessage.success(`已添加 ${items.length} 个商品到购物车`)
  handleClose()
}

/** 关闭弹窗 */
const handleClose = () => {
  stopCamera()
  resetState()
  visible.value = false
}

/** 重置状态 */
const resetState = () => {
  capturedImage.value = ''
  recognitionResult.value = null
  selectedProducts.value.clear()
}

const nextTick = () => new Promise(resolve => setTimeout(resolve, 50))

onBeforeUnmount(() => {
  stopCamera()
})
</script>

<style scoped>
.yolo-dialog :deep(.el-dialog__body) {
  padding: 16px 24px;
}

.yolo-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.capture-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.capture-preview {
  width: 100%;
  height: 320px;
  background: #1a1a1a;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.camera-video,
.captured-image {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}

.capture-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #9ca3af;
}

.capture-placeholder p {
  margin: 0;
  font-size: 14px;
}

.capture-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  justify-content: center;
}

.result-section {
  margin-top: 8px;
}

.products-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.product-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 8px;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s ease;
}

.product-item:hover {
  background: #f3f4f6;
}

.product-item.selected {
  background: #ecfdf5;
  border-color: #10b981;
}

.product-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.product-name {
  font-weight: 600;
  color: #1f2937;
  font-size: 14px;
}

.product-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.product-meta .price {
  color: #ef4444;
  font-weight: 600;
}

.product-subtotal {
  font-weight: 700;
  color: #ef4444;
  font-size: 16px;
}

.unmatched-section {
  margin-top: 12px;
  padding: 8px 12px;
  background: #fef3c7;
  border-radius: 6px;
}
</style>
