<script setup lang="ts">
/**
 * NotFoundPage - 404 页面不存在
 *
 * 兜底路由，捕获所有未定义的路径。
 * APP 打包后用户输入错误 URL 或深度链接失效时展示此页面。
 */
import { useRouter } from 'vue-router'
import { PageContainer } from '@/components/core'

const router = useRouter()

function goHome() {
  router.replace('/home')
}

function goBack() {
  router.back()
}
</script>

<template>
  <PageContainer title="页面不存在">
    <div class="not-found">
      <div class="not-found__icon">
        <svg viewBox="0 0 120 120" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="60" cy="60" r="54" stroke="currentColor" stroke-width="4" opacity="0.1" />
          <text x="60" y="78" text-anchor="middle" font-size="56" font-weight="700" fill="currentColor" opacity="0.25">404</text>
        </svg>
      </div>

      <h2 class="not-found__title">页面走丢了</h2>
      <p class="not-found__desc">您访问的页面不存在或已被移除</p>

      <div class="not-found__actions">
        <button class="not-found__btn not-found__btn--primary" type="button" @click="goHome">
          返回首页
        </button>
        <button class="not-found__btn not-found__btn--secondary" type="button" @click="goBack">
          返回上页
        </button>
      </div>
    </div>
  </PageContainer>
</template>

<style scoped lang="scss">
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 50vh;
  padding: var(--fts-space-8) var(--fts-space-4);
  text-align: center;

  &__icon {
    width: 120px;
    height: 120px;
    color: var(--fts-text-tertiary);
    margin-bottom: var(--fts-space-4);

    svg {
      width: 100%;
      height: 100%;
    }
  }

  &__title {
    font-size: var(--fts-font-size-xl);
    font-weight: 600;
    color: var(--fts-text-primary);
    margin: 0 0 var(--fts-space-2);
  }

  &__desc {
    font-size: var(--fts-font-size-base);
    color: var(--fts-text-tertiary);
    margin: 0 0 var(--fts-space-6);
    max-width: 280px;
    line-height: 1.5;
  }

  &__actions {
    display: flex;
    gap: var(--fts-space-3);
  }

  &__btn {
    padding: var(--fts-space-3) var(--fts-space-6);
    border-radius: var(--fts-radius-md);
    font-size: var(--fts-font-size-base);
    font-weight: 500;
    cursor: pointer;
    border: none;
    transition: all var(--fts-duration-fast) ease;

    &--primary {
      background: var(--fts-primary);
      color: var(--fts-text-on-primary);

      &:hover {
        opacity: 0.9;
      }

      &:active {
        transform: scale(0.97);
      }
    }

    &--secondary {
      background: var(--fts-bg-secondary);
      color: var(--fts-text-secondary);
      border: 1px solid var(--fts-border-secondary);

      &:hover {
        background: var(--fts-bg-hover);
      }

      &:active {
        transform: scale(0.97);
      }
    }
  }
}
</style>
