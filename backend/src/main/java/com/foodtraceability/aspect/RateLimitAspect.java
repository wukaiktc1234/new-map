package com.foodtraceability.aspect;

import com.foodtraceability.annotation.RateLimit;
import com.foodtraceability.common.Result;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 请求频率限制切面
 * 基于内存计数器实现限流，按注解维度（IP/USER/ALL）+ 接口路径统计
 */
@Aspect
@Component
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);

    /** 内存限流存储：key -> RateLimitEntry */
    private final ConcurrentHashMap<String, RateLimitEntry> localLimitStore = new ConcurrentHashMap<>();

    /**
     * 限流条目，包含当前计数和窗口过期时间
     */
    private static class RateLimitEntry {
        final AtomicInteger count;
        final long expireTime;

        RateLimitEntry(int count, long expireTime) {
            this.count = new AtomicInteger(count);
            this.expireTime = expireTime;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }
    }

    @Around("@annotation(com.foodtraceability.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        // TODO: 开发调试阶段临时关闭限流，前端/后端验收完成后恢复
        return point.proceed();
//        HttpServletRequest request = getRequest();
//        if (request == null) {
//            return point.proceed();
//        }
//
//        MethodSignature signature = (MethodSignature) point.getSignature();
//        Method method = signature.getMethod();
//        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
//
//        String key = generateKey(request, rateLimit);
//
//        return processWithLocalCache(point, rateLimit, key);
    }

    /**
     * 内存限流处理
     */
    private Object processWithLocalCache(ProceedingJoinPoint point, RateLimit rateLimit, String key) throws Throwable {
        long now = System.currentTimeMillis();
        long windowEnd = now + rateLimit.time() * 1000L;

        // 清理过期条目，防止内存泄漏
        localLimitStore.entrySet().removeIf(entry -> entry.getValue().isExpired());

        RateLimitEntry entry = localLimitStore.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired()) {
                return new RateLimitEntry(1, windowEnd);
            }
            return existing;
        });

        int currentCount = entry.count.incrementAndGet();
        if (currentCount > rateLimit.value()) {
            log.warn("请求频率超限: key={}, count={}, limit={}", key, currentCount, rateLimit.value());
            return Result.error(rateLimit.message());
        }

        log.debug("请求频率检查通过: key={}, count={}/{}", key, currentCount, rateLimit.value());
        return point.proceed();
    }

    /**
     * 生成限流键
     */
    private String generateKey(HttpServletRequest request, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder("rate_limit:");

        switch (rateLimit.type()) {
            case IP:
                keyBuilder.append("ip:").append(getClientIp(request));
                break;
            case USER:
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                String userId = auth != null && auth.isAuthenticated() ? auth.getName() : "anonymous";
                keyBuilder.append("user:").append(userId);
                break;
            case ALL:
                keyBuilder.append("all");
                break;
        }

        keyBuilder.append(":").append(request.getRequestURI());

        return keyBuilder.toString();
    }

    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
