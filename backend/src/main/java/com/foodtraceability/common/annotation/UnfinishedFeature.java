package com.foodtraceability.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * P0-unfinished：标注「功能尚未上线」的入口（表结构在开发库与生产库都不存在）。
 *
 * <p>由 UnfinishedFeatureInterceptor 在进入业务前短路，直接返回 code=501 + 文案；
 * 不改 SQL、不补表、不删代码；功能上线后删掉注解即可恢复。</p>
 *
 * <p><b>恢复方式：删掉本注解即恢复原逻辑</b>（门禁只做入口短路，不改 SQL、不补表、不删代码）。</p>
 *
 * <p>登记来源：scripts/qa/unfinished-features-registry.txt；明细 docs/audit/broken-endpoints-prod.md。</p>
 */
@Repeatable(UnfinishedFeatures.class)
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface UnfinishedFeature {

    /** 缺失的表名（与登记册一致，便于 grep 对账）。 */
    String table() default "";

    /** 可选补充说明。 */
    String note() default "";
}
