package com.foodtraceability.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** {@link UnfinishedFeature} 的容器注解：同一方法可能因多张缺表而重复标注。 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnfinishedFeatures {
    UnfinishedFeature[] value();
}
