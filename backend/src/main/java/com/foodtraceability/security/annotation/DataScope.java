package com.foodtraceability.security.annotation;

import java.lang.annotation.*;

/**
 * 数据范围权限注解
 * 用于标记需要进行数据范围隔离的方法
 *
 * 8级数据范围：
 * 1. ALL - 全部数据
 * 2. COMPANY - 公司数据
 * 3. STORE - 门店数据
 * 4. DEPARTMENT - 部门数据
 * 5. SELF - 个人数据
 * 6. SELF_AND_SUBORDINATE - 本人及下属数据
 * 7. CUSTOM - 自定义数据范围
 * 8. NONE - 无数据权限
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {

    /**
     * 数据范围类型
     */
    DataScopeType type() default DataScopeType.AUTO;

    /**
     * 数据表别名（用于SQL拼接）
     */
    String tableAlias() default "";

    /**
     * 用户ID字段名
     */
    String userIdColumn() default "create_by";

    /**
     * 部门ID字段名
     */
    String deptIdColumn() default "department_id";

    /**
     * 门店ID字段名
     */
    String storeIdColumn() default "store_id";

    /**
     * 公司ID字段名
     */
    String companyIdColumn() default "company_id";

    /**
     * 数据范围类型枚举
     */
    enum DataScopeType {
        AUTO,           // 自动根据用户角色数据范围设置
        ALL,            // 全部数据
        COMPANY,        // 公司数据
        STORE,          // 门店数据
        DEPARTMENT,     // 部门数据
        SELF,           // 个人数据
        SELF_AND_SUBORDINATE, // 本人及下属
        CUSTOM,         // 自定义
        NONE            // 无权限
    }
}
