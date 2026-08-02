<script setup lang="ts">
/**
 * FormField - 表单字段包装器组件（视觉增强版）
 *
 * 统一管理字段标签、必填标记、帮助提示和错误信息。
 * 支持实时字段级验证反馈。
 *
 * 视觉特性：
 * - Focus 状态：标签颜色过渡 + Focus Ring 装饰
 * - Error 状态：红色边框闪烁 + 滑入动画
 */
import { ref } from 'vue'

interface Props {
  /** 字段标签文字 */
  label: string
  /** 是否为必填字段（显示红色星号） */
  required?: boolean
  /** 字段级错误信息（非空时显示红色错误提示） */
  error?: string
  /** 帮助提示文字（显示在标签下方） */
  hint?: string
}

withDefaults(defineProps<Props>(), {
  required: false,
  error: undefined,
  hint: undefined,
})

const isFocused = ref(false)
</script>

<template>
  <div
    class="form-field"
    :class="{ 'form-field--error': error, 'form-field--focused': isFocused }"
    @focusin="isFocused = true"
    @focusout="isFocused = false"
  >
    <!-- 标签行 -->
    <label class="form-field__label">
      {{ label }}
      <span v-if="required" class="form-field__required" aria-label="必填">*</span>
    </label>

    <!-- 帮助提示 -->
    <p v-if="hint" class="form-field__hint">{{ hint }}</p>

    <!-- 字段控件插槽 -->
    <div class="form-field__control">
      <slot />
      <!-- Focus Ring 装饰 -->
      <div v-if="isFocused" class="form-field__focus-ring" />
    </div>

    <!-- 错误提示 -->
    <Transition name="error-slide">
      <p v-if="error" class="form-field__error" role="alert">{{ error }}</p>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
/* ================================================================
   FormField - 表单字段包装器（视觉增强版）
   特性：Focus Ring + Label 过渡 + Error 滑入动画
   ================================================================ */

.form-field {
  display: flex;
  flex-direction: column;
  gap: var(--fts-space-1);

  /* Focus 状态：标签颜色过渡 */
  &--focused {
    .form-field__label {
      color: var(--fts-primary);
    }
  }

  /* 错误状态：控件区域添加错误边框感 */
  &--error {
    .form-field__control {
      position: relative;

      &::after {
        content: '';
        position: absolute;
        left: 0;
        right: 0;
        bottom: 0;
        height: 2px;
        background: var(--fts-error);
        border-radius: 0 0 var(--fts-radius-xs) var(--fts-radius-xs);
        pointer-events: none;
        animation: errorBorderFlash 0.6s ease;
      }
    }
  }
}

@keyframes errorBorderFlash {
  0% { opacity: 0; transform: scaleX(0); }
  50% { opacity: 1; transform: scaleX(1.05); }
  100% { opacity: 1; transform: scaleX(1); }
}

// ----- 标签（支持颜色过渡） -----
.form-field__label {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: var(--fts-font-size-sm);
  font-weight: var(--fts-font-weight-semibold);
  color: var(--fts-text-tertiary);
  line-height: var(--fts-line-height-normal);
  margin: 0;
  transition: color var(--fts-duration-fast) var(--fts-easing-default);

  .form-field--error & {
    color: var(--fts-error);
  }
}

.form-field__required {
  color: var(--fts-error);
  font-weight: var(--fts-font-weight-bold);
  font-size: var(--fts-font-size-sm);
  line-height: 1;
}

// ----- 帮助提示 -----
.form-field__hint {
  margin: 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
  line-height: var(--fts-line-height-normal);
}

// ----- 控件区域 + Focus Ring -----
.form-field__control {
  position: relative;
}

.form-field__focus-ring {
  position: absolute;
  inset: -3px;
  border-radius: calc(var(--fts-radius-md) + 2px);
  pointer-events: none;
  animation: focusRingIn 0.25s ease-out forwards;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    border: 2px solid var(--fts-primary);
    opacity: 0.4;
  }

  &::after {
    content: '';
    position: absolute;
    inset: -2px;
    border-radius: inherit;
    box-shadow:
      0 0 0 2px rgba(var(--fts-primary-rgb), 0.1),
      0 0 12px rgba(var(--fts-primary-rgb), 0.15);
  }
}

@keyframes focusRingIn {
  from {
    opacity: 0;
    transform: scale(0.96);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

// ----- 错误提示（滑入动画） -----
.form-field__error {
  margin: calc(var(--fts-space-1) * -0.5) 0 0;
  padding: 0;
  font-size: var(--fts-font-size-xs);
  color: var(--fts-error);
  line-height: var(--fts-line-height-normal);
}

// Error 滑入过渡
.error-slide-enter-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}
.error-slide-leave-active {
  transition: all 0.2s cubic-bezier(0.55, 0, 1, 1);
}
.error-slide-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}
.error-slide-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
