<script setup lang="ts">
/**
 * 页面底部栏组件
 */
</script>

<template>
  <footer class="page-footer">
    <div class="page-footer__content">
      <span class="page-footer__text">食品溯源系统 © 2024</span>
      <span class="page-footer__divider">|</span>
      <span class="page-footer__text">Version 1.0.0</span>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.page-footer {
  padding: var(--fts-space-4) var(--fts-space-6);
  background: var(--fts-bg-card);
  border-top: 1px solid var(--fts-border-secondary);
  text-align: center;
}

.page-footer__content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--fts-space-3);
}

.page-footer__text {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-text-tertiary);
}

.page-footer__divider {
  font-size: var(--fts-font-size-xs);
  color: var(--fts-border-hover);
}
</style>
