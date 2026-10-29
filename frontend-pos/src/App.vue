<template>
  <div class="pos-app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: 'Microsoft YaHei', sans-serif;
}

.pos-app {
  height: 100%;
  /* 跟随主题：硬编码 #f5f5f5 会在深色模式下露出一圈浅色底（"白边"根因之一） */
  background: var(--pos-bg-canvas, var(--pos-bg-primary, #f5f5f5));
}

[data-theme='dark'] .pos-app {
  background: #181819;
}
</style>
