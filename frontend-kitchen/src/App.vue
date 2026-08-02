<template>
  <div class="kitchen-app">
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

.kitchen-app {
  height: 100%;
  background: #1a1a2e;
  color: #fff;
}
</style>
