package com.foodtraceability.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控切面
 * 自动监控所有Controller方法的执行时间
 */
@Aspect
@Component
public class PerformanceMonitorAspect {
    
    private static final Logger log = LoggerFactory.getLogger(PerformanceMonitorAspect.class);
    
    private static final long SLOW_API_THRESHOLD = 3000;
    
    private final ConcurrentMap<String, ApiStats> apiStatsMap = new ConcurrentHashMap<>();
    
    @Around("@annotation(org.springframework.web.bind.annotation.GetMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.PutMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.DeleteMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.RequestMapping)")
    public Object monitor(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = getMethodName(point);
        
        try {
            Object result = point.proceed();
            
            long executionTime = System.currentTimeMillis() - startTime;
            recordStats(methodName, executionTime, true);
            
            if (executionTime > SLOW_API_THRESHOLD) {
                log.warn("慢接口: {} 执行时间: {}ms", methodName, executionTime);
            } else if (executionTime > 1000) {
                log.info("接口执行: {} 耗时: {}ms", methodName, executionTime);
            } else {
                log.debug("接口执行: {} 耗时: {}ms", methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            recordStats(methodName, executionTime, false);
            throw e;
        }
    }
    
    private String getMethodName(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String className = point.getTarget().getClass().getSimpleName();
        String methodName = signature.getName();
        return className + "." + methodName;
    }
    
    private void recordStats(String methodName, long executionTime, boolean success) {
        ApiStats stats = apiStatsMap.computeIfAbsent(methodName, k -> new ApiStats());
        stats.recordCall(executionTime, success);
    }
    
    public ConcurrentMap<String, ApiStats> getApiStats() {
        return apiStatsMap;
    }
    
    public void resetStats() {
        apiStatsMap.clear();
    }
    
    public static class ApiStats {
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong successCalls = new AtomicLong(0);
        private final AtomicLong failedCalls = new AtomicLong(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private final AtomicLong maxTime = new AtomicLong(0);
        private final AtomicLong minTime = new AtomicLong(Long.MAX_VALUE);
        
        public void recordCall(long executionTime, boolean success) {
            totalCalls.incrementAndGet();
            if (success) {
                successCalls.incrementAndGet();
            } else {
                failedCalls.incrementAndGet();
            }
            totalTime.addAndGet(executionTime);
            
            long currentMax;
            do {
                currentMax = maxTime.get();
                if (executionTime <= currentMax) break;
            } while (!maxTime.compareAndSet(currentMax, executionTime));
            
            long currentMin;
            do {
                currentMin = minTime.get();
                if (executionTime >= currentMin) break;
            } while (!minTime.compareAndSet(currentMin, executionTime));
        }
        
        public long getTotalCalls() {
            return totalCalls.get();
        }
        
        public long getSuccessCalls() {
            return successCalls.get();
        }
        
        public long getFailedCalls() {
            return failedCalls.get();
        }
        
        public double getAverageTime() {
            long calls = totalCalls.get();
            return calls > 0 ? (double) totalTime.get() / calls : 0;
        }
        
        public long getMaxTime() {
            return maxTime.get();
        }
        
        public long getMinTime() {
            long min = minTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }
        
        public double getSuccessRate() {
            long calls = totalCalls.get();
            return calls > 0 ? (double) successCalls.get() / calls * 100 : 0;
        }
    }
}
