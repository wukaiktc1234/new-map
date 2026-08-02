package com.foodtraceability.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IP工具类
 */
public class IpUtils {

    private static final Logger logger = LoggerFactory.getLogger(IpUtils.class);

    /**
     * 获取客户端IP地址
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }

    /**
     * 根据IP获取地理位置（简化版，实际项目中可以使用IP库）
     */
    public static String getLocationByIp(String ip) {
        if (ip == null || ip.isEmpty() || "unknown".equals(ip)) {
            return "未知";
        }

        // 本地地址
        if ("127.0.0.1".equals(ip) || ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.")) {
            return "本地网络";
        }

        // 这里可以集成第三方IP库（如：纯真IP库、IP2Region等）
        // 简化处理，返回IP段
        String[] parts = ip.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1] + ".*.*";
        }

        return "未知";
    }

    /**
     * 判断是否为内网IP
     */
    public static boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        return ip.startsWith("127.") ||
               ip.startsWith("192.168.") ||
               ip.startsWith("10.") ||
               (ip.startsWith("172.") &&
                Integer.parseInt(ip.split("\\.")[1]) >= 16 &&
                Integer.parseInt(ip.split("\\.")[1]) <= 31);
    }
}