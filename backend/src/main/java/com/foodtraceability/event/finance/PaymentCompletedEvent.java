package com.foodtraceability.event.finance;

import java.io.Serializable;

/**
 * 付款完成事件（F-022）
 *
 * <p>触发场景：财务付款（供应商付款/员工付款）完成后发布。
 * 不可变事件对象，所有字段使用 final 修饰。
 */
public class PaymentCompletedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 付款单ID */
    private final Long paymentId;
    /** 付款金额（单位：分） */
    private final Long amount;
    /** 收款方类型：supplier/employee */
    private final String payeeType;
    /** 收款方ID */
    private final Long payeeId;

    /**
     * 构造方法
     *
     * @param paymentId 付款单ID
     * @param amount    付款金额（单位：分）
     * @param payeeType 收款方类型：supplier/employee
     * @param payeeId   收款方ID
     */
    public PaymentCompletedEvent(Long paymentId, Long amount, String payeeType, Long payeeId) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.payeeType = payeeType;
        this.payeeId = payeeId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getPayeeType() {
        return payeeType;
    }

    public Long getPayeeId() {
        return payeeId;
    }
}
