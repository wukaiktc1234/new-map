package com.foodtrace.employee;

import android.os.Bundle;
import android.webkit.WebView;

import com.getcapacitor.BridgeActivity;

/**
 * 员工门户 - Android 主 Activity
 *
 * v23 变更：恢复标准 Capacitor BridgeActivity 模式
 * 根因已确认为 Service Worker 在 file:// 协议下的异常行为
 * 通过 vite.config.ts 禁用 PWA 插件 + main.ts 防御卸载 解决
 */
public class MainActivity extends BridgeActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 启用 WebView 远程调试（Chrome 访问 chrome://inspect 可查看）
        WebView.setWebContentsDebuggingEnabled(true);

        super.onCreate(savedInstanceState);
    }
}
