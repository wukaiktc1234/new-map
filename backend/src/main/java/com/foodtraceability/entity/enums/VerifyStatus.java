package com.foodtraceability.entity.enums;

/**
 * 验真状态枚举
 */
public enum VerifyStatus {
    NOT_VERIFIED(0, "未验真"), VERIFIED(1, "验真通过"), FAILED(2, "验真失败");
    private final Integer code;
    private final String description;

    VerifyStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static VerifyStatus fromCode(Integer code) {
        for (VerifyStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return NOT_VERIFIED;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
