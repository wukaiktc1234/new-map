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
  background: #f5f5f5;
}
</style>
