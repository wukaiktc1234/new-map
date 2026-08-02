/**
 * Vitest 测试框架配置
 *
 * 继承 vite.config.ts 的别名和插件配置
 * 使用 jsdom 模拟浏览器 DOM 环境（Vue 组件测试必需）
 *
 * @last-modified 2026-06-04
 */
import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  // SCSS 全局变量注入（与 vite.config.ts 保持一致）
  css: {
    preprocessorOptions: {
      scss: {
        additionalData: `@use "@/styles/tokens" as *;`,
      },
    },
  },
  test: {
    // jsdom 环境：模拟浏览器 DOM，支持 Vue 组件挂载
    environment: 'jsdom',

    // 全局 Setup 文件路径
    setupFiles: ['./src/test/setup.ts'],

    // 测试文件匹配规则
    include: ['src/**/*.{test,spec}.{ts,tsx}'],

    // 排除文件
    exclude: ['node_modules', 'dist', '.output'],

    // 覆盖率配置
    coverage: {
      provider: 'v8',
      reporter: ['text', 'text-summary', 'json', 'html'],
      // 核心业务代码覆盖率阈值
      thresholds: {
        statements: 60,
        branches: 50,
        functions: 60,
        lines: 60,
      },
      // 排除非业务代码
      exclude: [
        'src/test/**',
        '**/*.d.ts',
        '**/types/**',
        '**/mock/**',
        '**/router/**',
        'src/main.ts',
        'src/App.vue',
      ],
    },

    // 全局 API 可用性
    globals: true,

    // 测试超时时间（毫秒）
    testTimeout: 10000,

    // Hook 超时时间
    hookTimeout: 10000,

    // 并行执行测试
    threads: true,

    // 隔离环境（每个测试文件独立环境）
    isolate: true,

    // 报告器配置
    reporter: ['verbose', 'json'],
    outputFile: {
      json: './coverage/test-results.json',
    },
  },
})
