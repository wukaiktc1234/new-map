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

    /**
     * ③（Lead 2026-10-30 P0 止血）：按 URI 后缀配置阈值 —— 敏感端点必须单独收紧。
     *
     * <p>修前实测：**8 次错误密码全部放行**，因为统一阈值是 100 次/分钟，
     * 而 {@code @RateLimit(5/60s)} 注解所在的切面被 TODO 短路 → 登录实际几乎无限流（可撞库）。</p>
     *
     * <p>规则：按「包含即匹配」判定；表用 LinkedHashMap 保证顺序可读，长的 key 写在前面。</p>
     */
    private static final java.util.LinkedHashMap<String, Integer> PATH_LIMITS = new java.util.LinkedHashMap<>();
    static {
        PATH_LIMITS.put("/auth/login", 5);      // 管理端 /v1/auth/login 与 POS /v1/pos/auth/login 都命中
        PATH_LIMITS.put("/auth/captcha", 10);   // 原注解 10/60s
        PATH_LIMITS.put("/pos/orders", 60);     // POS 开单/支付/退款
    }

    /** 解析该 URI 适用的阈值（未命中则用默认 100） */
    private static int limitFor(String uri) {
        if (uri == null) {
            return MAX_REQUESTS_PER_WINDOW;
        }
        for (java.util.Map.Entry<String, Integer> e : PATH_LIMITS.entrySet()) {
            if (uri.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return MAX_REQUESTS_PER_WINDOW;
    }

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 护栏 1（Lead 2026-10-30）：**限流启用状态必须可观测**。
     *
     * <p>今天的事故是：注解在、切面 Bean 在、方法体第一行 {@code return point.proceed();} 短路，
     * 且**没有任何日志** —— 运行时完全静默，直到实测 8 次登录都不限流才发现。</p>
     */
    @jakarta.annotation.PostConstruct
    public void logEnabled() {
        logger.info("[SECURITY] rate_limit enabled=true mechanism=RateLimitFilter windowMs={} defaultLimit={} pathLimits={}",
                TIME_WINDOW_MS, MAX_REQUESTS_PER_WINDOW, PATH_LIMITS);
        logger.info("[SECURITY] rate_limit note=内存计数，仅单实例有效；多实例部署需换共享存储（tech-debt SEC-1）");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // 获取真实客户端IP，不信任客户端提供的X-Forwarded-For
        String clientIp = getClientIpFromSocket(request);
        String requestUri = request.getRequestURI();
        String rateLimitKey = "rate_limit:" + clientIp + ":" + requestUri;

        int limit = limitFor(requestUri);
        if (!isRateLimited(rateLimitKey, limit)) {
            chain.doFilter(request, response);
        } else {
            logger.warn("限流拦截 - IP: {}, URI: {}", clientIp, requestUri);
            logger.warn("限流拦截 - IP: {}, URI: {}, limit={}", clientIp, requestUri, limit);
            // ★（Lead 2026-10-30）429 必须带标准 **Retry-After** 头：
            //   前端已按「服务端原文 + Retry-After 计算等待时长」实现，只回 body.message 时
            //   「约 N 秒后可重试」永远拿不到真实值。
            RateLimitCounter counter = memoryRateLimit.get(rateLimitKey);
            final long retryAfterSec = (counter == null) ? 1L : counter.secondsUntilReset();
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSec));   // 与 body.retryAfter **同源同值**
            response.setContentType("application/json;charset=UTF-8");
            java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("code", 429);
            payload.put("message", "请求过于频繁，请稍后再试");
            payload.put("timestamp", System.currentTimeMillis());
            payload.put("retryAfter", retryAfterSec);      // ← 同一个变量，禁止再算第二次
            response.getWriter().write(objectMapper.writeValueAsString(payload));
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
    private boolean isRateLimited(String key, int limit) {
        long currentTime = System.currentTimeMillis();
        RateLimitCounter counter = memoryRateLimit.computeIfAbsent(key,
                k -> new RateLimitCounter(currentTime));

        counter.increment();

        // 定期清理过期数据，防止内存泄漏
        if (currentTime - counter.getLastCleanupTime() > TIME_WINDOW_MS) {
            counter.cleanup();
            counter.setLastCleanupTime(currentTime);
        }

        return counter.isLimitExceeded(limit);
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

        /** 距离窗口重置还有多少秒（用于 Retry-After 头） */
        public synchronized long secondsUntilReset() {
            long remain = windowStart + TIME_WINDOW_MS - System.currentTimeMillis();
            return Math.max(1L, (remain + 999) / 1000);
        }

        public synchronized boolean isLimitExceeded(int limit) {
            long currentTime = System.currentTimeMillis();
            // 窗口已过期，不限制
            if (currentTime - windowStart > TIME_WINDOW_MS) {
                return false;
            }
            return requestCount > limit;
        }

        public long getLastCleanupTime() {
            return lastCleanupTime;
        }

        public void setLastCleanupTime(long lastCleanupTime) {
            this.lastCleanupTime = lastCleanupTime;
        }
    }
}
