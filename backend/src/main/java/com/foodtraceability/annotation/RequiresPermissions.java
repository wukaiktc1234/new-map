package com.foodtraceability.annotation;

import java.lang.annotation.*;

/**
 * 多权限验证注解
 * 支持AND/OR逻辑
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermissions {
    String[] value();
    Logical logical() default Logical.AND;
}
