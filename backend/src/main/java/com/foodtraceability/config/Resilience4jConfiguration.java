package com.foodtraceability.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j配置类，配置熔断、重试等规则
 */
@Configuration
public class Resilience4jConfiguration {

    /**
     * 配置熔断规则注册表
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        // 通用熔断配置
        CircuitBreakerConfig defaultConfig = CircuitBreakerConfig.custom()
                // 失败率阈值，超过该阈值则熔断
                .failureRateThreshold(50)
                // 滑动窗口类型：COUNT_BASED（基于计数）
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                // 滑动窗口大小
                .slidingWindowSize(10)
                // 最小请求数，只有当请求数超过该值时才会计算失败率
                .minimumNumberOfCalls(5)
                // 等待时间，熔断后经过该时间进入半开状态
                .waitDurationInOpenState(Duration.ofSeconds(30))
                // 半开状态下允许的请求数
                .permittedNumberOfCallsInHalfOpenState(3)
                // 记录的异常类型
                .recordExceptions(Exception.class)
                .build();

        return CircuitBreakerRegistry.of(defaultConfig);
    }

    /**
     * 配置重试规则注册表
     */
    @Bean
    public RetryRegistry retryRegistry() {
        // 通用重试配置
        RetryConfig defaultConfig = RetryConfig.custom()
                // 最大重试次数
                .maxAttempts(3)
                // 重试间隔
                .waitDuration(Duration.ofMillis(500))
                // 重试异常类型
                .retryExceptions(Exception.class)
                .build();

        return RetryRegistry.of(defaultConfig);
    }

    /**
     * 配置时间限制器注册表
     */
    @Bean
    public TimeLimiterRegistry timeLimiterRegistry() {
        // 通用时间限制配置
        TimeLimiterConfig defaultConfig = TimeLimiterConfig.custom()
                // 超时时间
                .timeoutDuration(Duration.ofSeconds(5))
                .build();

        return TimeLimiterRegistry.of(defaultConfig);
    }
}