package com.foodtraceability.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API响应时间监控切面
 * 基于内存 ConcurrentHashMap 统计响应时间分布、错误率、平均响应时间
 */
@Aspect
@Component
public class ApiResponseTimeAspect {

    private static final Logger logger = LoggerFactory.getLogger(ApiResponseTimeAspect.class);

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** 响应时间分布统计：key -> 次数 */
    private final ConcurrentMap<String, AtomicLong> timeRangeCountMap = new ConcurrentHashMap<>();

    /** 状态统计（成功/失败）：key -> 次数 */
    private final ConcurrentMap<String, AtomicLong> statusCountMap = new ConcurrentHashMap<>();

    /** 响应时间累计：key -> 累计毫秒 */
    private final ConcurrentMap<String, AtomicLong> totalTimeMap = new ConcurrentHashMap<>();

    /** 请求总数：key -> 次数 */
    private final ConcurrentMap<String, AtomicLong> totalCountMap = new ConcurrentHashMap<>();

    /**
     * 构造函数注入（无外部依赖）
     */
    public ApiResponseTimeAspect() {
    }

    /**
     * 定义切点，拦截所有控制器方法
     */
    @Pointcut("execution(* com.foodtraceability.controller..*.*(..))")
    public void apiResponseTimePointcut() {
    }

    /**
     * 环绕通知，实现API响应时间监控
     */
    @Around("apiResponseTimePointcut()")
    @SuppressWarnings("null")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取请求和响应对象
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            // 如果没有请求上下文，直接执行方法
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();

        // 记录请求开始时间
        Instant startTime = Instant.now();
        String requestUri = request.getRequestURI();
        String method = request.getMethod();
        String today = LocalDate.now().format(DATE_FORMATTER);

        boolean isSuccess = true;
        long responseTime = 0;

        try {
            // 执行原始方法
            Object result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            isSuccess = false;
            throw e;
        } finally {
            // 计算响应时间
            Instant endTime = Instant.now();
            Duration duration = Duration.between(startTime, endTime);
            responseTime = duration.toMillis();

            // 将响应时间添加到响应头
            if (response != null) {
                response.setHeader("X-Response-Time", String.valueOf(responseTime));
            }

            // 记录响应时间日志
            logger.info("API响应时间监控: {} {} 耗时 {}ms，状态: {}", method, requestUri, responseTime, isSuccess ? "成功" : "失败");

            // 如果响应时间超过500ms，记录警告日志
            if (responseTime > 500) {
                logger.warn("API响应时间过长: {} {} 耗时 {}ms", method, requestUri, responseTime);
            }

            // 内存统计
            try {
                // 1. 统计响应时间分布
                String timeRange = getTimeRange(responseTime);
                String timeRangeKey = String.format("api:response-time:%s:%s:%s:%s", today, method, requestUri, timeRange);
                timeRangeCountMap.computeIfAbsent(timeRangeKey, k -> new AtomicLong(0)).incrementAndGet();

                // 2. 统计错误率
                String statusKey = String.format("api:status:%s:%s:%s:%s", today, method, requestUri, isSuccess ? "success" : "error");
                statusCountMap.computeIfAbsent(statusKey, k -> new AtomicLong(0)).incrementAndGet();

                // 3. 统计平均响应时间（累计响应时间）
                String avgTimeKey = String.format("api:avg-time:%s:%s:%s", today, method, requestUri);
                totalTimeMap.computeIfAbsent(avgTimeKey, k -> new AtomicLong(0)).addAndGet(responseTime);

                // 4. 统计请求总数（用于计算平均响应时间）
                String totalCountKey = String.format("api:total-count:%s:%s:%s", today, method, requestUri);
                totalCountMap.computeIfAbsent(totalCountKey, k -> new AtomicLong(0)).incrementAndGet();
            } catch (Exception e) {
                logger.debug("API响应时间统计失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 根据响应时间获取时间范围
     * @param responseTime 响应时间（毫秒）
     * @return 时间范围，如0-100ms、100-500ms等
     */
    private String getTimeRange(long responseTime) {
        if (responseTime < 100) {
            return "0-100ms";
        } else if (responseTime < 500) {
            return "100-500ms";
        } else if (responseTime < 1000) {
            return "500-1000ms";
        } else if (responseTime < 3000) {
            return "1-3s";
        } else if (responseTime < 5000) {
            return "3-5s";
        } else {
            return "5s+";
        }
    }
}
