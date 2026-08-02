package com.foodtraceability.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异常监控和告警服务
 * 用于监控系统中的异常情况并进行告警
 */
@Service
public class ExceptionMonitoringService {
    private static final Logger log = LoggerFactory.getLogger(ExceptionMonitoringService.class);

    // 异常统计映射，key为异常类型，value为异常计数
    private final ConcurrentHashMap<String, AtomicInteger> exceptionCounts = new ConcurrentHashMap<>();

    // 异常阈值，超过该阈值则触发告警
    private static final int EXCEPTION_THRESHOLD = 10;

    /**
     * 记录异常
     * @param ex 异常对象
     * @param requestUri 请求URI
     * @param method 请求方法
     */
    public void recordException(Exception ex, String requestUri, String method) {
        String exceptionType = ex.getClass().getName();
        
        // 统计异常次数
        AtomicInteger count = exceptionCounts.computeIfAbsent(exceptionType, k -> new AtomicInteger(0));
        int currentCount = count.incrementAndGet();
        
        // 记录详细日志
        log.error("[系统异常] 异常类型: {}, 异常消息: {}, 请求URI: {}, 请求方法: {}, 异常堆栈: {}", 
                exceptionType, ex.getMessage(), requestUri, method, getStackTrace(ex));
        
        // 检查是否需要触发告警
        if (currentCount >= EXCEPTION_THRESHOLD) {
            triggerAlert(exceptionType, currentCount, requestUri, method);
            // 重置计数，避免重复告警
            count.set(0);
        }
    }

    /**
     * 触发告警
     * @param exceptionType 异常类型
     * @param count 异常次数
     * @param requestUri 请求URI
     * @param method 请求方法
     */
    private void triggerAlert(String exceptionType, int count, String requestUri, String method) {
        try {
            // 这里可以实现具体的告警逻辑，如发送邮件、短信、推送通知等
            // 目前先记录告警日志
            log.error("[异常告警] 异常类型: {}, 异常次数: {}, 请求URI: {}, 请求方法: {}, 告警时间: {}", 
                    exceptionType, count, requestUri, method, LocalDateTime.now());
            
            // 实际项目中可以调用告警服务发送告警
            // alertService.createAlert(...);
        } catch (Exception e) {
            log.error("[告警发送失败] 错误消息: {}", e.getMessage());
        }
    }

    /**
     * 获取异常统计信息
     * @return 异常统计映射
     */
    public ConcurrentHashMap<String, AtomicInteger> getExceptionCounts() {
        return exceptionCounts;
    }

    /**
     * 重置异常统计
     */
    public void resetExceptionCounts() {
        exceptionCounts.clear();
        log.info("[异常统计已重置]");
    }

    /**
     * 获取异常的堆栈信息
     * @param throwable 异常对象
     * @return 异常堆栈信息字符串
     */
    private String getStackTrace(Throwable throwable) {
        if (throwable == null) {
            return "";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
}
