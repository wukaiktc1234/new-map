package com.foodtraceability.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 健康证报销状态枚举
 * 统一健康证报销状态字段的取值
 */
public enum HealthCertificateExpenseStatusEnum {
    /**
     * 待人事初审
     */
    PENDING_HR_REVIEW("pending_hr_review", "待人事初审"), /**
     * 待财务审核
     */
    PENDING_FINANCE_REVIEW("pending_finance_review", "待财务审核"), /**
     * 已通过
     */
    APPROVED("approved", "已通过"), /**
     * 已拒绝
     */
    REJECTED("rejected", "已拒绝"), /**
     * 已报销
     */
    REIMBURSED("reimbursed", "已报销");
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
    HealthCertificateExpenseStatusEnum(String value, String label) {
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
    public static HealthCertificateExpenseStatusEnum getByValue(String value) {
        for (HealthCertificateExpenseStatusEnum status : HealthCertificateExpenseStatusEnum.values()) {
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
