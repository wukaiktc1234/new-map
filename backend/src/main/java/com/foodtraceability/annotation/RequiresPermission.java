package com.foodtraceability.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 * 使用三段式权限编码: {模块}:{资源}:{操作}
 * 示例: @RequiresPermission(value = "system:permission:create", action = "create")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {
    /**
     * 权限编码，格式: {模块}:{资源}:{操作}
     */
    String value();

    /**
     * 操作类型，用于审计日志和权限描述
     */
    String action() default "";

    /**
     * 权限描述，用于API文档和日志
     */
    String description() default "";
}
