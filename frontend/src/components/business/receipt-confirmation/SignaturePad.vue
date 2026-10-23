<script setup lang="ts">
/**
 * 手写签名板
 * 支持鼠标/触摸绘制，导出 base64 PNG，清空重签
 */
import { ref, onMounted, watch } from 'vue'

const props = withDefaults(
  defineProps<{
    modelValue?: string
    placeholder?: string
    height?: number
    /** 横屏旋转模式：画布被外层容器旋转 90° 后，需将视口触摸坐标逆变换回画布本地坐标 */
    rotate?: boolean
  }>(),
  {
    modelValue: '',
    placeholder: '请在此处签名',
    height: 120,
    rotate: false,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const canvasRef = ref<HTMLCanvasElement>()
let ctx: CanvasRenderingContext2D | null = null
let drawing = false
let lastX = 0
let lastY = 0

function getPos(e: MouseEvent | TouchEvent): { x: number; y: number } {
  const canvas = canvasRef.value
  if (!canvas) return { x: 0, y: 0 }
  const rect = canvas.getBoundingClientRect()
  let px: number
  let py: number
  if (e instanceof MouseEvent) {
    px = e.clientX
    py = e.clientY
  } else {
    const touch = e.touches[0] || e.changedTouches[0]
    px = touch.clientX
    py = touch.clientY
  }
  if (!props.rotate) {
    return { x: px - rect.left, y: py - rect.top }
  }
  // 旋转 90°（顺时针）后的逆变换：
  // 画布未旋转时的 CSS 尺寸（offsetWidth/Height 不受 transform 影响）
  const cw = canvas.offsetWidth
  const ch = canvas.offsetHeight
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  // 视口点 → 相对中心 → 逆时针 90° 映射回画布本地坐标
  // (dx, dy) = (px-cx, py-cy) → 本地 (x, y) = (dx', dy')：旋转90° 顺时针是 (x,y)→(-y,x)，逆推 (dx,dy)→(dy,-dx)
  const dx = px - cx
  const dy = py - cy
  return { x: cw / 2 + dy, y: ch / 2 - dx }
}

function startStroke(e: MouseEvent | TouchEvent): void {
  e.preventDefault()
  const pos = getPos(e)
  drawing = true
  lastX = pos.x
  lastY = pos.y
}

function moveStroke(e: MouseEvent | TouchEvent): void {
  if (!drawing || !ctx) return
  e.preventDefault()
  const pos = getPos(e)
  ctx.beginPath()
  ctx.moveTo(lastX, lastY)
  ctx.lineTo(pos.x, pos.y)
  ctx.stroke()
  lastX = pos.x
  lastY = pos.y
}

function endStroke(): void {
  drawing = false
  emitValue()
}

function emitValue(): void {
  const canvas = canvasRef.value
  if (!canvas) return
  const hasContent = canvas
    .getContext('2d')
    ?.getImageData(0, 0, canvas.width, canvas.height)
    .data.some((v) => v !== 0)
  emit('update:modelValue', hasContent ? canvas.toDataURL('image/png') : '')
}

function clear(): void {
  const canvas = canvasRef.value
  if (!canvas || !ctx) return
  ctx.clearRect(0, 0, canvas.width, canvas.height)
  emitValue()
}

onMounted(() => {
  const canvas = canvasRef.value
  if (!canvas) return
  const dpr = window.devicePixelRatio || 1
  canvas.width = canvas.clientWidth * dpr
  canvas.height = props.height * dpr
  ctx = canvas.getContext('2d')
  if (ctx) {
    ctx.scale(dpr, dpr)
    ctx.lineWidth = 2.5
    ctx.lineCap = 'round'
    ctx.lineJoin = 'round'
    ctx.strokeStyle = '#1f2937'
  }
})

watch(
  () => props.modelValue,
  (val) => {
    if (!val && ctx && canvasRef.value) {
      ctx.clearRect(0, 0, canvasRef.value.width, canvasRef.value.height)
    }
  },
)

defineExpose({ clear })
</script>

<template>
  <div class="signature-pad">
    <canvas
      ref="canvasRef"
      class="signature-pad__canvas"
      :style="{ height: `${height}px` }"
      @mousedown="startStroke"
      @mousemove="moveStroke"
      @mouseup="endStroke"
      @mouseleave="endStroke"
      @touchstart="startStroke"
      @touchmove="moveStroke"
      @touchend="endStroke"
    />
    <div class="signature-pad__footer">
      <span class="signature-pad__placeholder">{{ placeholder }}</span>
      <el-button link type="info" size="small" @click="clear">清空重签</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.signature-pad {
  border: 1px dashed var(--fts-border-primary);
  border-radius: var(--fts-radius-base);
  overflow: hidden;
  background: var(--fts-bg-card);

  &__canvas {
    display: block;
    width: 100%;
    cursor: crosshair;
    touch-action: none;
  }

  &__footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 var(--fts-space-2) var(--fts-space-1);
  }

  &__placeholder {
    font-size: var(--fts-font-size-sm);
    color: var(--fts-text-tertiary);
  }
}
</style>
