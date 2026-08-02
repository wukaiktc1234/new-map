import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type Plugin } from 'vite'
import vue from '@vitejs/plugin-vue'

/**
 * Vite 插件：禁用 Element Plus 的 useLockscreen（构建模式）
 *
 * Element Plus 弹窗打开时，useLockscreen 会：
 * 1. 给 body 添加 .el-popup-parent--hidden class（CSS: overflow:hidden）
 * 2. 设置 body.style.width = calc(100% - 滚动条宽度px)
 * 导致页面滚动条消失 + 内容左移，造成画面抖动。
 */
function disableElementPlusLockScreen(): Plugin {
  return {
    name: 'disable-element-plus-lockscreen',
    enforce: 'pre',
    transform(code, id) {
      if (id.includes('element-plus') && id.includes('use-lockscreen')) {
        return {
          code: `// [FTS Patch] useLockscreen 已禁用\nexport const useLockscreen = () => {}\n`,
          map: null,
        }
      }
      return null
    },
  }
}

export default defineConfig({
  plugins: [
    disableElementPlusLockScreen(),
    vue(),
  ],
  define: {
    // sockjs-client 等库引用了 Node.js 的 global 变量，浏览器环境不存在。
    // 将 global 映射到 globalThis，使这类库在浏览器中正常运行。
    global: 'globalThis',
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/global/tokens" as *;`,
      }
    },
  },
  /**
   * 依赖预打包优化配置
   *
   * 关键修复：在 dev 模式下，Vite 会将 node_modules 预打包到 .vite/deps/ 目录，
   * plugins 中的 transform 钩子不会对预打包文件执行。
   * Vite 8 使用 Rolldown 进行依赖预打包，需要通过 rolldownOptions.plugin 替换。
   */
  optimizeDeps: {
    rolldownOptions: {
      plugins: [
        {
          name: 'disable-lockscreen-in-deps',
          transform: {
            filter: {
              id: /use-lockscreen/,
            },
            handler(code, id) {
              console.log('[FTS Patch] 替换 useLockscreen:', id)
              return {
                code: '// [FTS Patch] useLockscreen 已禁用\nexport const useLockscreen = () => {}\n',
                map: null,
              }
            },
          },
        },
      ],
    },
  },
  server: {
    port: 3002,
    host: '0.0.0.0',
    open: false,
    hmr: {
      overlay: true,
    },
    watch: {
      usePolling: true,
      interval: 200,
    },
    proxy: {
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true,
        configure: (proxy) => {
          // 后端未启动时静默处理 ECONNREFUSED，避免控制台刷屏
          // 前端 API 层已有 Mock fallback，不影响功能使用
          // 使用 nextTick 确保在 Vite 添加默认 error handler 之后移除它
          process.nextTick(() => {
            proxy.removeAllListeners('error')
            proxy.on('error', (err, _req, res) => {
              if ('code' in err && err.code === 'ECONNREFUSED') {
                // 静默处理：返回 502，不打印错误日志
                if (res && !res.headersSent) {
                  res.writeHead(502, { 'Content-Type': 'application/json' })
                  res.end(JSON.stringify({ code: -1, message: 'Backend service unavailable', data: null }))
                }
                return
              }
              // 其他错误正常输出
              console.error('[proxy]', err.message)
            })
          })
        },
      },
    }
  },
})