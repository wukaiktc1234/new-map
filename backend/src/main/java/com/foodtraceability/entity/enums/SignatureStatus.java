package com.foodtraceability.entity.enums;

/**
 * 验签状态枚举
 */
public enum SignatureStatus {
    NOT_VERIFIED(0, "未验签"), VERIFIED(1, "验签通过"), FAILED(2, "验签失败");
    private final Integer code;
    private final String description;

    SignatureStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static SignatureStatus fromCode(Integer code) {
        for (SignatureStatus status : values()) {
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
