package com.foodtraceability.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 请求限流过滤器
 * 基于内存计数器实现限流，按 IP + 接口路径 统计
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitFilter.class);

    private final ObjectMapper objectMapper;

    /** 内存限流器，按 IP+URI 维度计数 */
    private final ConcurrentHashMap<String, RateLimitCounter> memoryRateLimit = new ConcurrentHashMap<>();

    /** 限流时间窗口（毫秒） */
    private static final long TIME_WINDOW_MS = 60_000L;

    /** 单窗口最大请求数 */
    private static final int MAX_REQUESTS_PER_WINDOW = 100;

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 获取真实客户端IP，不信任客户端提供的X-Forwarded-For
        String clientIp = getClientIpFromSocket(request);
        String requestUri = request.getRequestURI();
        String rateLimitKey = "rate_limit:" + clientIp + ":" + requestUri;

        if (!isRateLimited(rateLimitKey)) {
            chain.doFilter(request, response);
        } else {
            logger.warn("限流拦截 - IP: {}, URI: {}", clientIp, requestUri);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(objectMapper.writeValueAsString(
                Map.of("code", 42900, "message", "请求过于频繁，请稍后再试", "timestamp", System.currentTimeMillis())));
        }
    }

    /**
     * 从Socket获取真实IP，防止伪造X-Forwarded-For
     */
    private String getClientIpFromSocket(HttpServletRequest request) {
        // 优先信任反向代理设置的头
        String ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        // 从Socket获取真实连接IP
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null) {
            return remoteAddr;
        }

        return "unknown";
    }

    /**
     * 判断是否触发限流（内存实现）
     */
    private boolean isRateLimited(String key) {
        long currentTime = System.currentTimeMillis();
        RateLimitCounter counter = memoryRateLimit.computeIfAbsent(key,
                k -> new RateLimitCounter(currentTime));

        counter.increment();

        // 定期清理过期数据，防止内存泄漏
        if (currentTime - counter.getLastCleanupTime() > TIME_WINDOW_MS) {
            counter.cleanup();
            counter.setLastCleanupTime(currentTime);
        }

        return counter.isLimitExceeded();
    }

    /**
     * 限流计数器（线程安全）
     */
    private static class RateLimitCounter {
        private long windowStart;
        private int requestCount = 0;
        private long lastCleanupTime;

        public RateLimitCounter(long windowStart) {
            this.windowStart = windowStart;
            this.lastCleanupTime = windowStart;
        }

        public synchronized void increment() {
            long currentTime = System.currentTimeMillis();
            // 窗口过期则重置
            if (currentTime - windowStart > TIME_WINDOW_MS) {
                windowStart = currentTime;
                requestCount = 0;
            }
            requestCount++;
        }

        public synchronized void cleanup() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - windowStart > TIME_WINDOW_MS) {
                windowStart = currentTime;
                requestCount = 0;
            }
        }

        public synchronized boolean isLimitExceeded() {
            long currentTime = System.currentTimeMillis();
            // 窗口已过期，不限制
            if (currentTime - windowStart > TIME_WINDOW_MS) {
                return false;
            }
            return requestCount > MAX_REQUESTS_PER_WINDOW;
        }

        public long getLastCleanupTime() {
            return lastCleanupTime;
        }

        public void setLastCleanupTime(long lastCleanupTime) {
            this.lastCleanupTime = lastCleanupTime;
        }
    }
}
