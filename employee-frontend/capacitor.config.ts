import type { CapacitorConfig } from '@capacitor/cli';

/**
 * Capacitor 配置
 *
 * 【关键修复】APP 打包模式不再使用 server.url 从远程服务器加载
 * 根因：cap sync 时 process.env.VITE_APP_MODE 可能未设置，
 * 导致生成的 capacitor.config.json 缺少 server 配置
 *
 * 正确方案：使用 Capacitor 默认的本地文件加载方式（https:// scheme），
 * 配合 androidScheme: 'https' 确保 ES Module 正常执行
 */
const config: CapacitorConfig = {
  appId: 'com.foodtrace.employee',
  appName: '员工门户',
  webDir: 'dist',

  /**
   * 服务器配置：
   * - 开发模式：Vite dev server（实时预览 + HMR）
   * - APP 打包/生产模式：使用本地文件（Capacitor 默认 https:// scheme）
   *
   * 【重要】不再使用 server.url 指向局域网服务器
   * 原因：
   * 1. cap sync 时环境变量可能缺失，导致配置不生效
   * 2. 依赖局域网服务器意味着 APP 无法离线使用
   * 3. Capacitor 6+ 默认使用 https:// scheme，ES Module 可正常执行
   */
  ...(process.env.NODE_ENV === 'development' ? {
    server: {
      url: 'http://localhost:3004',
      cleartext: true,
      androidScheme: 'https',
    },
  } : {}),

  /**
   * Android 配置
   * - androidScheme: 'https' — 使用 https:// 而非 file://，确保 ES Module 正常执行
   * - allowMixedContent: true — 允许 HTTP API 请求（后端在局域网 HTTP 上运行）
   */
  android: {
    androidScheme: 'https',
    allowMixedContent: true,
  },

  /** iOS 配置 */
  ios: {
    contentInset: 'automatic',
    scrollEnabled: false,
  },

  /** 插件配置 */
  plugins: {
    StatusBar: {
      style: 'LIGHT',
      backgroundColor: '#007AFF',
    },
    SplashScreen: {
      launchShowDuration: 1000,
      launchAutoHide: true,
      backgroundColor: '#FFFFFF',
      showSpinner: false,
      spinnerColor: '#007AFF',
    },
    Preferences: {
      group: 'employee_app',
    },
  },
};

export default config;
