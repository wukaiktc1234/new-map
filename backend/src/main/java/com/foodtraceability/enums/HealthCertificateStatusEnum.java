package com.foodtraceability.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 健康证状态枚举
 * 统一健康证状态字段的取值
 */
public enum HealthCertificateStatusEnum {
    /**
     * 有效
     */
    VALID("valid", "有效"), /**
     * 即将过期
     */
    EXPIRING("expiring", "即将过期"), /**
     * 已过期
     */
    EXPIRED("expired", "已过期");
    /**
     * 数据库存储值
     */
    @EnumValue
    private final String value;
    /**
     * 显示名称
     */
    private final String label;

    /**
     * 构造方法
     * @param value 数据库存储值
     * @param label 显示名称
     */
    HealthCertificateStatusEnum(String value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 序列化时返回的值
     * @return 数据库存储值
     */
    @JsonValue
    public String getValue() {
        return this.value;
    }

    /**
     * 根据值获取枚举
     * @param value 数据库存储值
     * @return 枚举
     */
    public static HealthCertificateStatusEnum getByValue(String value) {
        for (HealthCertificateStatusEnum status : HealthCertificateStatusEnum.values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        return null;
    }

    /**
     * 显示名称
     */
    public String getLabel() {
        return this.label;
    }
}
