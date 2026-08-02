package com.foodtraceability.entity.enums;

/**
 * 电子凭证类型枚举
 */
public enum VoucherType {
    INVOICE("invoice", "增值税发票"), FULL_ELECTRONIC_INVOICE("full_electronic", "全电发票"), TRAIN_TICKET("train_ticket", "铁路电子客票"), FLIGHT_TICKET("flight_ticket", "航空运输电子客票行程单"), BANK_RECEIPT("bank_receipt", "银行电子回单"), BANK_STATEMENT("bank_statement", "银行电子对账单"), TAX_RECEIPT("tax_receipt", "财政电子票据"), PAYMENT_VOUCHER("payment_voucher", "国库集中支付电子凭证"), OTHER("other", "其他");
    private final String code;
    private final String description;

    VoucherType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static VoucherType fromCode(String code) {
        for (VoucherType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return OTHER;
    }

    public String getCode() {
        return this.code;
    }

    public String getDescription() {
        return this.description;
    }
}
