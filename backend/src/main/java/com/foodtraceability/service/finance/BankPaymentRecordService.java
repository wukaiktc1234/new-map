package com.foodtraceability.service.finance;

import com.foodtraceability.entity.finance.BankPaymentRecord;

import java.util.List;

/**
 * 银行打款记录Service接口
 */
public interface BankPaymentRecordService {

    /**
     * 创建银行打款记录
     * @param record 银行打款记录
     * @return 创建的记录
     */
    BankPaymentRecord create(BankPaymentRecord record);

    /**
     * 根据付款单ID查询银行记录
     * @param paymentId 付款单ID
     * @return 银行记录列表
     */
    List<BankPaymentRecord> getByPaymentId(Long paymentId);

    /**
     * 根据记录ID查询详情
     * @param recordId 记录ID
     * @return 银行记录
     */
    BankPaymentRecord getById(Long recordId);

    /**
     * 回填银行记录
     * @param recordId 记录ID
     * @param bankTransactionNo 银行流水号
     * @param bankTransactionTime 银行交易时间
     * @param voucherType 凭证类型
     * @param voucherUrl 凭证URL
     * @return 更新后的记录
     */
    BankPaymentRecord fillBankInfo(Long recordId, String bankTransactionNo,
                                   java.time.LocalDateTime bankTransactionTime,
                                   String voucherType, String voucherUrl);

    /**
     * 确认银行记录
     * @param recordId 记录ID
     * @param confirmResult 确认结果（success/fail）
     * @param confirmUserId 确认人ID
     * @param confirmUserName 确认人姓名
     * @param confirmRemark 确认备注
     * @return 更新后的记录
     */
    BankPaymentRecord confirm(Long recordId, String confirmResult,
                              Long confirmUserId, String confirmUserName,
                              String confirmRemark);

    /**
     * 废弃银行记录
     * @param recordId 记录ID
     */
    void abandon(Long recordId);
}