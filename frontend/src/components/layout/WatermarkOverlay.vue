<script setup lang="ts">
/**
 * 页面水印组件
 */
import { computed } from 'vue'

interface Props {
  text?: string
  opacity?: number
}

const props = withDefaults(defineProps<Props>(), {
  text: '管理员',
  opacity: 0.06
})

const watermarkStyle = computed(() => ({
  backgroundImage: `repeating-linear-gradient(
    -45deg,
    transparent,
    transparent 100px,
    rgba(128, 128, 128, ${props.opacity}) 100px,
    rgba(128, 128, 128, ${props.opacity}) 101px
  ), repeating-linear-gradient(
    45deg,
    transparent,
    transparent 150px,
    rgba(128, 128, 128, ${props.opacity}) 150px,
    rgba(128, 128, 128, ${props.opacity}) 151px
  )`,
  backgroundSize: '300px 300px'
}))

const textOverlayStyle = computed(() => {
  const items = []
  for (let i = 0; i < 50; i++) {
    items.push({
      content: props.text,
      top: `${(i % 10) * 15 + 5}%`,
      left: `${Math.floor(i / 10) * 20 + 5}%`,
      transform: `rotate(-30deg)`
    })
  }
  return items
})
</script>

<template>
  <div class="watermark-overlay" :style="watermarkStyle">
    <div
      v-for="(item, index) in textOverlayStyle"
      :key="index"
      class="watermark-text"
      :style="{
        top: item.top,
        left: item.left,
        transform: item.transform,
        opacity: props.opacity + 0.03
      }"
    >
      {{ item.content }}
    </div>
  </div>
</template>

<style scoped lang="scss">
.watermark-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}

.watermark-text {
  position: absolute;
  font-size: 14px;
  color: var(--fts-text-tertiary);
  font-weight: var(--fts-font-weight-medium);
  letter-spacing: 2px;
  white-space: nowrap;
  user-select: none;
}
</style>
