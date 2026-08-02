package com.foodtraceability.entity.enums;

/**
 * 电子凭证处理状态枚举
 */
public enum VoucherStatus {
    PENDING(0, "待处理"), VERIFIED(1, "已验签"), PROCESSED(2, "已入账"), ARCHIVED(3, "已归档");
    private final Integer code;
    private final String description;

    VoucherStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static VoucherStatus fromCode(Integer code) {
        for (VoucherStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return PENDING;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
