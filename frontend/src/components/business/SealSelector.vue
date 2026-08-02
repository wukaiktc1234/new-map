<script setup lang="ts">
/**
 * 印章选择器组件
 * 用于 HR 合同、采购合同、电子合同签署时选择已授权印章
 */
import { ref, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled } from '@element-plus/icons-vue'
import { sealApi } from '@/api/seal'
import {
  type SealInfo,
  type SealScene,
  SealTypeLabelMap,
  SealStatusLabelMap,
} from '@/types/seal'
import StatusTag from '@/components/core/StatusTag.vue'

const props = defineProps<{
  /** 使用场景 */
  scene: SealScene
  /** 选中的印章ID */
  modelValue?: string
  /** 是否禁用 */
  disabled?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'select', seal: SealInfo): void
}>()

const loading = ref(false)
const sealList = ref<SealInfo[]>([])

/** 当前选中的印章对象 */
const selectedSeal = computed(() => sealList.value.find(s => s.sealId === props.modelValue) || null)

/** 加载已授权印章 */
async function loadAuthorizedSeals() {
  loading.value = true
  try {
    sealList.value = await sealApi.getAuthorized(props.scene)
  } catch {
    sealList.value = []
  } finally {
    loading.value = false
  }
}

/** 选择印章 */
function handleSelect(seal: SealInfo) {
  if (props.disabled) return
  emit('update:modelValue', seal.sealId)
  emit('select', seal)
}

/** 组件挂载时加载 */
watch(
  () => props.scene,
  () => loadAuthorizedSeals(),
  { immediate: true },
)
</script>

<template>
  <div v-loading="loading" class="seal-selector">
    <div v-if="sealList.length" class="seal-grid">
      <div
        v-for="seal in sealList"
        :key="seal.sealId"
        class="seal-card"
        :class="{ 'seal-card--active': seal.sealId === modelValue, 'seal-card--disabled': disabled }"
        @click="handleSelect(seal)"
      >
        <img :src="seal.sealImage" :alt="seal.sealName" class="seal-card__img" />
        <div class="seal-card__info">
          <div class="seal-card__name">{{ seal.sealName }}</div>
          <div class="seal-card__type">{{ SealTypeLabelMap[seal.sealType] }}</div>
        </div>
        <el-icon v-if="seal.sealId === modelValue" class="seal-card__check"><CircleCheckFilled /></el-icon>
      </div>
    </div>
    <el-empty v-else description="暂无已授权的印章，请联系管理员授权" :image-size="60" />
  </div>
</template>

<style scoped lang="scss">
.seal-selector {
  min-height: 80px;
}

.seal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: var(--fts-space-3);
}

.seal-card {
  position: relative;
  display: flex;
  align-items: center;
  gap: var(--fts-space-2);
  padding: var(--fts-space-3);
  border: 2px solid var(--fts-border-color);
  border-radius: var(--fts-radius-md);
  cursor: pointer;
  transition: all 0.2s ease;

  &:hover:not(.seal-card--disabled) {
    border-color: var(--fts-primary);
    background: var(--fts-primary-light-9, rgba(64, 158, 255, 0.05));
  }

  &--active {
    border-color: var(--fts-primary);
    background: var(--fts-primary-light-9, rgba(64, 158, 255, 0.08));
  }

  &--disabled {
    cursor: not-allowed;
    opacity: 0.6;
  }

  &__img {
    width: 48px;
    height: 48px;
    object-fit: contain;
    flex-shrink: 0;
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__name {
    font-size: var(--fts-font-size-sm);
    font-weight: 500;
    color: var(--fts-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__type {
    font-size: var(--fts-font-size-xs);
    color: var(--fts-text-tertiary);
    margin-top: 2px;
  }

  &__check {
    position: absolute;
    top: var(--fts-space-1);
    right: var(--fts-space-1);
    font-size: 18px;
    color: var(--fts-primary);
  }
}
</style>
