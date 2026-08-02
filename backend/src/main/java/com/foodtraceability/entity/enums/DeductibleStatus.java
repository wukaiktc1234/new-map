package com.foodtraceability.entity.enums;

/**
 * 进项税抵扣状态枚举
 */
public enum DeductibleStatus {
    DEDUCTIBLE(1, "可抵扣"), NOT_DEDUCTIBLE(2, "不可抵扣"), PARTIAL_DEDUCTIBLE(3, "部分抵扣");
    private final Integer code;
    private final String description;

    DeductibleStatus(Integer code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DeductibleStatus fromCode(Integer code) {
        for (DeductibleStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return DEDUCTIBLE;
    }

    public Integer getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
