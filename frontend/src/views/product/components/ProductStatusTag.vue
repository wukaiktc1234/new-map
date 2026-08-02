<template>
  <StatusTag :status="statusTagStatus" :label="tagLabel" :size="statusSize" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { productDataConverter } from '@/api/product/converters'
import StatusTag from '@/components/core/StatusTag.vue'

interface Props {
  status: number | string
  size?: 'large' | 'default' | 'small'
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default'
})

/** 产品状态映射到 StatusTag 支持的状态值 */
const statusTagStatus = computed(() => {
  const frontendStatus = productDataConverter.statusToFrontend(props.status)
  const map: Record<string, string> = {
    active: 'active',
    inactive: 'inactive',
    soldout: 'warning',
  }
  return map[frontendStatus] || 'info'
})

const tagLabel = computed(() => {
  const frontendStatus = productDataConverter.statusToFrontend(props.status)
  return productDataConverter.getStatusLabel(frontendStatus)
})

/** ProductStatusTag size 映射到 StatusTag size */
const statusSize = computed<'small' | 'medium' | 'large'>(() => {
  if (props.size === 'large') return 'large'
  if (props.size === 'small') return 'small'
  return 'medium'
})
</script>
