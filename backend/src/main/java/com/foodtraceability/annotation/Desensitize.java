package com.foodtraceability.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * 用于标记需要进行脱敏处理的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Desensitize {
    
    /**
     * 脱敏类型
     */
    DesensitizeType value() default DesensitizeType.DEFAULT;

    int start() default 0;

    int end() default 0;

    char replacement() default '*';
}