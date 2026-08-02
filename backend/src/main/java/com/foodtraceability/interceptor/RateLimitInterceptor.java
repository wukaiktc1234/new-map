package com.foodtraceability.interceptor;

import com.foodtraceability.common.Result;
import com.foodtraceability.utils.WebUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * API请求频率限制拦截器
 * 基于内存计数器实现限流，按 IP + 接口路径 统计
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final ObjectMapper objectMapper;

    /** 内存限流存储：key -> Counter */
    private final ConcurrentMap<String, RateCounter> rateLimitMap = new ConcurrentHashMap<>();

    /** 默认每分钟允许的请求数 */
    private static final int DEFAULT_REQUESTS_PER_MINUTE = 100;

    /** 限流的时间窗口（毫秒） */
    private static final long TIME_WINDOW_MS = 60_000L;

    /**
     * 构造函数注入（禁止 @Autowired 字段注入）
     * @param objectMapper JSON对象映射器
     */
    public RateLimitInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取客户端IP地址
        String clientIp = WebUtils.getClientIpAddress(request);
        // 获取请求路径
        String requestPath = request.getRequestURI();
        // 生成限流key：ip:path
        String rateLimitKey = "rate_limit:" + clientIp + ":" + requestPath;

        try {
            long currentTime = System.currentTimeMillis();
            RateCounter counter = rateLimitMap.computeIfAbsent(rateLimitKey,
                    k -> new RateCounter(currentTime));

            long currentCount = counter.incrementAndGet(currentTime);

            // 检查是否超过限流阈值
            if (currentCount > DEFAULT_REQUESTS_PER_MINUTE) {
                log.warn("API请求频率限制触发: IP={}, Path={}, Count={}", clientIp, requestPath, currentCount);

                // 设置响应头
                response.setContentType("application/json;charset=UTF-8");
                response.setStatus(429); // 429 - Too Many Requests

                // 返回限流响应
                Result<?> result = Result.error(429, "请求过于频繁，请稍后再试");
                response.getWriter().write(objectMapper.writeValueAsString(result));

                return false;
            }

            // 定期清理过期条目，防止内存泄漏
            if (rateLimitMap.size() > 10000) {
                rateLimitMap.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTime));
            }
        } catch (Exception e) {
            // 限流逻辑异常时，跳过限流，不影响主流程
            log.error("API请求频率限制执行失败: {}", e.getMessage(), e);
        }

        return true;
    }

    /**
     * 限流计数器（线程安全）
     */
    private static class RateCounter {
        private long windowStart;
        private long count;

        public RateCounter(long windowStart) {
            this.windowStart = windowStart;
            this.count = 0;
        }

        public synchronized long incrementAndGet(long currentTime) {
            // 窗口过期则重置
            if (currentTime - windowStart > TIME_WINDOW_MS) {
                windowStart = currentTime;
                count = 0;
            }
            count++;
            return count;
        }

        public synchronized boolean isExpired(long currentTime) {
            return currentTime - windowStart > TIME_WINDOW_MS;
        }
    }
}
