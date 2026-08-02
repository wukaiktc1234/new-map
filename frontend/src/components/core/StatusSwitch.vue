<template>
  <div class="status-switch" :class="[`status-switch--${size}`]">
    <div class="status-switch__wrapper">
      <el-switch
        ref="switchRef"
        :model-value="isActive"
        :disabled="isDisabled"
        :loading="isLoading"
        :size="size"
        :active-text="activeText"
        :inactive-text="inactiveText"
        :active-value="computedActiveValue"
        :inactive-value="computedInactiveValue"
        inline-prompt
        style="--el-switch-on-color: var(--el-color-primary)"
        @change="handleChange"
      />
      <!-- loading遮罩层 -->
      <Transition name="status-switch-fade">
        <div v-if="isLoading" class="status-switch__loading">
          <el-icon class="status-switch__loading-icon" :size="size === 'small' ? 12 : 14">
            <Loading />
          </el-icon>
        </div>
      </Transition>
    </div>
    <!-- 状态文字标签 -->
    <span v-if="showLabel" class="status-switch__label" :class="{ 'status-switch__label--active': isActive }">
      {{ currentLabel }}
    </span>
  </div>
</template>

<script setup lang="ts">
/**
 * StatusSwitch 组件
 * 统一的状态滑动切换开关，基于 el-switch 封装，扩展以下能力：
 * - 支持异步状态切换（调用API后确认结果）
 * - 切换前二次确认（可选配置）
 * - loading 状态防重复点击
 * - 失败自动回滚（API 失败时恢复原状态）
 * - 操作日志记录
 *
 * 【使用示例】
 * ```vue
 * <!-- 基础用法：仅展示切换UI，由父组件监听change事件处理逻辑 -->
 * <StatusSwitch v-model="status" />
 *
 * <!-- 带API调用的完整用法 -->
 * <StatusSwitch
 *   v-model="row.foodStatus"
 *   :api-call="(val) => foodApi.updateStatus(row.foodId, val)"
 *   confirm-before-change
 *   name="菜品状态"
 * />
 *
 * <!-- 自定义值映射（后端用数字编码） -->
 * <StatusSwitch
 *   v-model="row.status"
 *   :active-value="1"
 *   :inactive-value="0"
 *   :api-call="(val) => api.updateStatus(row.id, val)"
 * />
 * ```
 */
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'

// ==================== 类型定义 ====================

interface Props {
  /** 当前值（v-model绑定） */
  modelValue: boolean | number | string
  /** 启用时的值，默认根据modelValue类型推断 */
  activeValue?: boolean | number | string
  /** 禁用时的值，默认根据modelValue类型推断 */
  inactiveValue?: boolean | number | string
  /** 是否禁用整个开关 */
  disabled?: boolean
  /** 外部loading状态 */
  loading?: boolean
  /** 切换前是否需要确认弹窗 */
  confirmBeforeChange?: boolean
  /** 确认弹窗文案 */
  confirmText?: string
  /** API调用函数，传入新值，返回Promise */
  apiCall?: (newValue: boolean | number | string) => Promise<void>
  /** 开关尺寸 */
  size?: 'large' | 'default' | 'small'
  /** 开启时文字 */
  activeText?: string
  /** 关闭时文字 */
  inactiveText?: string
  /** 用于日志标识的名称 */
  name?: string
  /** 是否显示右侧状态文字标签 */
  showLabel?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean | number | string): void
  (e: 'change', value: boolean | number | string, oldValue: boolean | number | string): void
  (e: 'success', value: boolean | number | string): void
  (e: 'error', error: Error): void
}

// ==================== Props & Emits ====================

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  loading: false,
  confirmBeforeChange: false,
  confirmText: '确定要更改状态吗？',
  size: 'small',
  activeText: '',
  inactiveText: '',
  name: '',
  showLabel: true,
})

const emit = defineEmits<Emits>()

// ==================== 内部状态 ====================

const switchRef = ref()
const internalLoading = ref(false)
const lastConfirmedValue = ref<boolean | number | string>(props.modelValue)
const hasMounted = ref(false)

onMounted(() => { hasMounted.value = true })

// ==================== 计算属性 ====================

/**
 * 根据modelValue的类型推断默认的active/inactive值
 */
function inferDefaultValues() {
  const val = props.modelValue
  if (typeof val === 'boolean') {
    return { active: true as const, inactive: false as const }
  }
  if (typeof val === 'number') {
    return { active: 1 as const, inactive: 0 as const }
  }
  // string 类型：尝试判断是否为语义化状态
  if (val === 'active' || val === 'inactive' || val === 'soldout') {
    return { active: 'active' as const, inactive: 'inactive' as const }
  }
  return { active: 'active' as const, inactive: 'inactive' as const }
}

/** 计算后的启用值 */
const computedActiveValue = computed(() => {
  if (props.activeValue !== undefined) return props.activeValue
  return inferDefaultValues().active
})

/** 计算后的禁用值 */
const computedInactiveValue = computed(() => {
  if (props.inactiveValue !== undefined) return props.inactiveValue
  return inferDefaultValues().inactive
})

/** 当前是否为启用状态 */
const isActive = computed(() => props.modelValue === computedActiveValue.value)

/** 是否处于加载中（内部或外部） */
const isLoading = computed(() => internalLoading.value || props.loading)

/** 是否被禁用（外部disabled 或 加载中） */
const isDisabled = computed(() => props.disabled || isLoading.value)

/** 当前状态的显示文案 */
const currentLabel = computed(() => {
  if (isActive.value) {
    return props.activeText || '启用'
  }
  return props.inactiveText || '停用'
})

// ==================== 方法 ====================

/**
 * 处理开关切换事件
 * 支持同步和异步两种模式：
 * 1. 无apiCall：直接emit change事件，由父组件处理
 * 2. 有apiCall：内部处理确认、调用、回滚逻辑
 */
async function handleChange(newValue: boolean | number | string): Promise<void> {
  // 防止重复点击
  if (internalLoading.value) return

  // 挂载前的数据绑定变化不触发确认弹窗
  if (!hasMounted.value) return

  const oldValue = props.modelValue

  // 模式一：无API调用，纯UI控制模式
  if (!props.apiCall) {
    emit('update:modelValue', newValue)
    emit('change', newValue, oldValue)
    return
  }

  // 模式二：有API调用，需要二次确认（如果配置了）
  if (props.confirmBeforeChange) {
    try {
      await ElMessageBox.confirm(props.confirmText, '操作确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
    } catch {
      // 用户取消了确认，不需要做任何事（el-switch会自动恢复）
      return
    }
  }

  // 执行异步切换
  internalLoading.value = true
  lastConfirmedValue.value = oldValue

  try {
    // 先更新UI为新的状态
    emit('update:modelValue', newValue)

    // 调用API
    await props.apiCall(newValue)

    // 成功
    emit('success', newValue)
    emit('change', newValue, oldValue)

  } catch (error: unknown) {
    // 失败：回滚到原状态
    const err = error instanceof Error ? error : new Error(String(error))
    emit('update:modelValue', lastConfirmedValue.value)
    emit('error', err)
  } finally {
    internalLoading.value = false
  }
}

// ==================== 监听器 ====================

/** 外部modelValue变化时重置lastConfirmedValue */
watch(
  () => props.modelValue,
  (newVal) => {
    lastConfirmedValue.value = newVal
  }
)
</script>

<style scoped lang="scss">
/* var() fallback为防御性编码，确保CSS变量未定义时有默认样式 */

/* ============================================================
 * StatusSwitch 组件样式
 * 基于BEM命名规范，使用CSS变量适配深色模式
 * ============================================================ */

.status-switch {
  display: inline-flex;
  align-items: center;
  gap: var(--fts-space-2, 6px);
  vertical-align: middle;

  /* 尺寸变体 */
  &--large {
    --switch-scale: 1;
  }

  &--default {
    --switch-scale: 0.9;
  }

  &--small {
    --switch-scale: 0.85;
  }

  /* 开关容器（用于定位loading遮罩） */
  &__wrapper {
    position: relative;
    display: inline-flex;
    align-items: center;
    transform: scale(var(--switch-scale, 1));
    transform-origin: center center;
  }

  /* loading遮罩层 */
  &__loading {
    position: absolute;
    inset: 0;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: color-mix(in srgb, var(--el-bg-color) 80%, transparent);
    border-radius: var(--el-border-radius-base, 4px);
    z-index: 1;
  }

  &__loading-icon {
    color: var(--el-color-primary);
    animation: status-switch-rotate 1s linear infinite;
  }

  /* 状态文字标签 */
  &__label {
    font-size: var(--el-font-size-extra-small, 12px);
    line-height: 1;
    color: var(--el-text-color-secondary, #909399);
    white-space: nowrap;
    user-select: none;
    transition: color 0.25s ease;

    &--active {
      color: var(--el-color-primary, #409eff);
      font-weight: 500;
    }
  }
}

/* loading图标旋转动画 */
@keyframes status-switch-rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* loading遮罩淡入淡出过渡 */
.status-switch-fade-enter-active,
.status-switch-fade-leave-active {
  transition: opacity 0.2s ease;
}

.status-switch-fade-enter-from,
.status-switch-fade-leave-to {
  opacity: 0;
}

/* ============================================================
 * 深色模式适配（通过CSS变量自动适配，无需额外媒体查询）
 * 以下变量在Element Plus深色主题下会自动切换：
 * - --el-bg-color: 背景色
 * - --el-text-color-secondary: 次要文字颜色
 * - --el-color-primary: 主题色
 * - --el-border-radius-base: 圆角
 * ============================================================ */
</style>
