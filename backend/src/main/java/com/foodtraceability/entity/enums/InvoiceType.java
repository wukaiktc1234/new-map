package com.foodtraceability.entity.enums;

/**
 * 发票类型枚举
 */
public enum InvoiceType {
    SPECIAL_VAT("special_vat", "增值税专用发票"), NORMAL_VAT("normal_vat", "增值税普通发票"), ELECTRONIC("electronic", "电子发票"), FULL_ELECTRONIC("full_electronic", "全电发票"), ELECTRONIC_SPECIAL("electronic_special", "电子专用发票"), ELECTRONIC_NORMAL("electronic_normal", "电子普通发票");
    private final String code;
    private final String description;

    InvoiceType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static InvoiceType fromCode(String code) {
        for (InvoiceType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return ELECTRONIC;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
