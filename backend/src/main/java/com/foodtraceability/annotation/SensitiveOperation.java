package com.foodtraceability.annotation;

import java.lang.annotation.*;

/**
 * 敏感操作注解
 * 用于标记需要记录操作日志的敏感操作
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SensitiveOperation {
    
    /**
     * 操作名称
     */
    String name();
    
    /**
     * 操作类型
     */
    String type() default "other";
    
    /**
     * 是否需要记录请求参数
     */
    boolean logParams() default true;
    
    /**
     * 是否需要记录返回结果
     */
    boolean logResult() default false;
}
