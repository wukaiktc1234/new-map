package com.foodtraceability.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AnomalyAccessLogFilter extends OncePerRequestFilter {

    private static final Logger anomalyLogger = LoggerFactory.getLogger("ANOMALY_ACCESS_LOG");
    private static final Logger logger = LoggerFactory.getLogger(AnomalyAccessLogFilter.class);
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) throws ServletException, IOException {
        
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        
        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, wrappedResponse);
        } finally {
            wrappedResponse.copyBodyToResponse();
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = response.getStatus();
            
            if (isAnomalyStatus(statusCode)) {
                logAnomalyAccess(request, statusCode, duration);
            }
        }
    }

    private boolean isAnomalyStatus(int statusCode) {
        return statusCode == 401 || 
               statusCode == 403 || 
               statusCode == 429 ||
               statusCode >= 500;
    }

    private void logAnomalyAccess(HttpServletRequest request, int statusCode, long durationMs) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", LocalDateTime.now().format(FORMATTER));
            logEntry.put("method", request.getMethod());
            logEntry.put("uri", request.getRequestURI());
            logEntry.put("queryString", request.getQueryString());
            logEntry.put("statusCode", statusCode);
            logEntry.put("clientIp", getClientIp(request));
            logEntry.put("userAgent", request.getHeader("User-Agent"));
            logEntry.put("durationMs", durationMs);
            logEntry.put("referer", request.getHeader("Referer"));
            
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.length() > 20) {
                logEntry.put("authTokenPrefix", authHeader.substring(0, 20) + "...");
            } else {
                logEntry.put("authTokenPresent", authHeader != null && !authHeader.isEmpty());
            }
            
            String jsonLog = objectMapper.writeValueAsString(logEntry);
            
            switch (statusCode) {
                case 401:
                    anomalyLogger.warn("[401-未授权] {}", jsonLog);
                    break;
                case 403:
                    anomalyLogger.warn("[403-禁止访问] {}", jsonLog);
                    break;
                case 429:
                    anomalyLogger.warn("[429-限流] {}", jsonLog);
                    break;
                default:
                    anomalyLogger.error("[{}-服务器错误] {}", statusCode, jsonLog);
            }
        } catch (Exception e) {
            logger.error("记录异常访问日志失败: {}", e.getMessage(), e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 排除静态资源和健康检查
        return path.startsWith("/actuator/") || 
               path.startsWith("/v1/health") ||
               path.endsWith(".css") || 
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".ico");
    }
}
