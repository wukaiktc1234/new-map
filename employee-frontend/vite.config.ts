import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

/** 是否为 APP 打包模式（通过 VITE_APP_MODE=1 触发） */
const isAppMode = process.env.VITE_APP_MODE === '1'

export default defineConfig({
  plugins: [
    vue(),
    // 【修复 #1】APP 打包模式完全禁用 PWA/SW
    // 根因：Service Worker 在 Capacitor file:// 协议下拦截请求导致空白页面
    // v12 成功时无 SW，v13+ 注册 SW 后全部空白
    ...(isAppMode ? [] : [VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.svg', 'icons/apple-touch-icon.png'],
      manifest: false,
      workbox: {
        // 新 SW 立即激活，不等待旧 SW 控制的页面关闭
        skipWaiting: true,
        // 新 SW 立即控制所有页面
        clientsClaim: true,
        maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
        globPatterns: ['**/*.{js,css,html,ico,png,svg,woff2}'],
        runtimeCaching: [
          {
            urlPattern: /\/api\/(auth|salary|profile|settings)\/.*$/i,
            handler: 'NetworkOnly',
            options: { cacheName: 'api-sensitive' },
          },
          {
            urlPattern: /\/api\/.*$/i,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'api-cache',
              expiration: { maxEntries: 50, maxAgeSeconds: 60 * 60 },
              networkTimeoutSeconds: 10,
            },
          },
          {
            urlPattern: /.*\.(?:png|jpg|jpeg|svg|gif|webp|ico|woff2?)$/i,
            handler: 'CacheFirst',
            options: {
              cacheName: 'static-images',
              expiration: { maxEntries: 100, maxAgeSeconds: 30 * 24 * 60 * 60 },
            },
          },
        ],
      },
    })]),
  ],
  base: isAppMode ? './' : '/',
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/tokens" as *;`,
      }
    },
  },
  server: {
    port: 3004,
    host: '0.0.0.0',
    open: false,
    hmr: { overlay: true },
    watch: {
      usePolling: true,
      interval: 200,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  },
  /** APP 模式优化：禁用 source map 以减小包体积 */
  build: {
    ...(isAppMode ? {
      sourcemap: false,
      // 【修复】移除 inject: false — 该选项会阻止 Vite 将 script 标签注入到 HTML
      // Capacitor 6+ 使用 https:// scheme 加载本地文件，ES Module 可正常执行
      // 【修复】移除 manualChunks: undefined — 禁用代码分割会导致单个 JS 文件过大
      // 保留默认代码分割策略，有利于首屏加载性能
    } : {}),
  },
})
