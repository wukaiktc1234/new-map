# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 【修复】保留 Capacitor WebView JavaScript 接口
# Capacitor 通过 @JavascriptInterface 注解暴露方法给 WebView
# 混淆会导致 JS 调用原生方法时找不到对应的方法名
-keepclassmembers class com.getcapacitor.** {
    @android.webkit.JavascriptInterface <methods>;
}

# 保留 Capacitor 插件类（通过反射加载）
-keep class com.getcapacitor.** { *; }

# 保留行号信息便于调试崩溃日志
-keepattributes SourceFile,LineNumberTable

# 隐藏原始源文件名
-renamesourcefileattribute SourceFile
