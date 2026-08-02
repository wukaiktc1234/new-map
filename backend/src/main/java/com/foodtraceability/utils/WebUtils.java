package com.foodtraceability.utils;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Web工具类
 * 用于获取客户端IP地址和用户代理信息
 */
public class WebUtils {
    
    /**
     * 获取客户端IP地址
     * @param request HttpServletRequest对象
     * @return 客户端IP地址
     */
    public static String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
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
        
        // 如果是多级代理，取第一个非unknown的IP
        if (ip != null && ip.contains(",")) {
            String[] ips = ip.split(",");
            for (String s : ips) {
                if (!"unknown".equalsIgnoreCase(s.trim())) {
                    ip = s.trim();
                    break;
                }
            }
        }
        
        // 本地地址处理
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }
        
        return ip;
    }
    
    /**
     * 获取客户端用户代理信息
     * @param request HttpServletRequest对象
     * @return 客户端用户代理信息
     */
    public static String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        return request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "unknown";
    }
}