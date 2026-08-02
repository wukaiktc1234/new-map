package com.foodtraceability.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限范围注解
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresDataScope {
    /**
     * 资源类型，用于确定数据过滤策略
     */
    String resourceType();
    /**
     * 是否启用数据权限，默认启用
     */
    boolean enabled() default true;
}
