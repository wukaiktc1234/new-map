package com.foodtraceability.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API请求次数统计切面
 * 基于内存 ConcurrentHashMap 统计每个API的请求次数
 */
@Aspect
@Component
public class ApiRequestCountAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiRequestCountAspect.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 内存统计存储：key -> 请求次数 */
    private final ConcurrentMap<String, AtomicLong> requestCountMap = new ConcurrentHashMap<>();

    /**
     * 构造函数注入（无外部依赖）
     */
    public ApiRequestCountAspect() {
    }

    /**
     * 定义切点，拦截所有控制器方法
     */
    @Pointcut("execution(* com.foodtraceability.controller..*.*(..))")
    public void apiRequestCountPointcut() {
    }

    /**
     * 方法返回后执行，统计请求次数
     */
    @AfterReturning("apiRequestCountPointcut()")
    @SuppressWarnings("null")
    public void countApiRequest(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        try {
            HttpServletRequest request = attributes.getRequest();
            String requestUri = request.getRequestURI();
            String method = request.getMethod();
            String today = LocalDate.now().format(DATE_FORMATTER);

            String key = String.format("api:count:%s:%s:%s", today, method, requestUri);

            requestCountMap.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();

            logger.debug("API请求计数: key={}", key);
        } catch (Exception e) {
            // 统计失败不影响主流程
            logger.debug("API请求计数失败: {}", e.getMessage());
        }
    }

    /**
     * 获取指定key的请求次数（供监控查询使用）
     */
    public long getRequestCount(String key) {
        AtomicLong count = requestCountMap.get(key);
        return count != null ? count.get() : 0;
    }

    /**
     * 获取所有统计快照
     */
    public ConcurrentMap<String, AtomicLong> getAllCounts() {
        return new ConcurrentHashMap<>(requestCountMap);
    }
}
