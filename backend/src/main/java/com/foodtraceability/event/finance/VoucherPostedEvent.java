package com.foodtraceability.event.finance;

import java.io.Serializable;

/**
 * 凭证过账完成事件（F-022）
 *
 * <p>触发场景：记账凭证过账成功后发布。
 * 不可变事件对象，所有字段使用 final 修饰。
 */
public class VoucherPostedEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 凭证ID */
    private final Long voucherId;
    /** 凭证号 */
    private final String voucherNo;
    /** 合计金额（单位：分） */
    private final Long totalAmount;

    /**
     * 构造方法
     *
     * @param voucherId   凭证ID
     * @param voucherNo   凭证号
     * @param totalAmount 合计金额（单位：分）
     */
    public VoucherPostedEvent(Long voucherId, String voucherNo, Long totalAmount) {
        this.voucherId = voucherId;
        this.voucherNo = voucherNo;
        this.totalAmount = totalAmount;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }
}
