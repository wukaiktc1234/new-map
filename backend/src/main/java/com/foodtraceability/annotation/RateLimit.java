package com.foodtraceability.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 请求频率限制注解
 * 用于限制API接口的访问频率,防止恶意刷接口
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * 时间窗口内最大请求次数
     */
    int value() default 10;
    
    /**
     * 时间窗口大小(秒)
     */
    int time() default 60;
    
    /**
     * 限流类型
     */
    RateLimitType type() default RateLimitType.IP;

    String message() default "请求过于频繁,请稍后重试";
}
