package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 银行打款记录实体类
 * 记录付款确认过程，支持A/B两种付款模式
 */
@TableName("bank_payment_record")
public class BankPaymentRecord extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 关联付款单ID */
    private Long paymentId;

    /** 银行流水号（转账/支票必填，现金不填） */
    private String bankTransactionNo;

    /** 银行交易时间 */
    private LocalDateTime bankTransactionTime;

    /** 银行响应码 */
    private String bankResponseCode;

    /** 银行响应信息 */
    private String bankResponseMsg;

    /**
     * 凭证类型
     * receipt-银行回单 invoice-发票 other-其他 legacy-历史存量
     */
    private String voucherType;

    /** 凭证文件URL */
    private String voucherUrl;

    /**
     * 状态
     * 0-待回填 1-已回填 2-确认中 3-成功 4-失败 5-已废弃
     */
    private Integer status;

    /** 确认人ID */
    private Long confirmUserId;

    /** 确认人姓名 */
    private String confirmUserName;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /**
     * 确认结果
     * success-成功 fail-失败
     */
    private String confirmResult;

    /** 确认备注 */
    private String confirmRemark;

    /** 创建人ID */
    private Long createUserId;

    /** 创建人姓名 */
    private String createUserName;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getBankTransactionNo() {
        return bankTransactionNo;
    }

    public void setBankTransactionNo(String bankTransactionNo) {
        this.bankTransactionNo = bankTransactionNo;
    }

    public LocalDateTime getBankTransactionTime() {
        return bankTransactionTime;
    }

    public void setBankTransactionTime(LocalDateTime bankTransactionTime) {
        this.bankTransactionTime = bankTransactionTime;
    }

    public String getBankResponseCode() {
        return bankResponseCode;
    }

    public void setBankResponseCode(String bankResponseCode) {
        this.bankResponseCode = bankResponseCode;
    }

    public String getBankResponseMsg() {
        return bankResponseMsg;
    }

    public void setBankResponseMsg(String bankResponseMsg) {
        this.bankResponseMsg = bankResponseMsg;
    }

    public String getVoucherType() {
        return voucherType;
    }

    public void setVoucherType(String voucherType) {
        this.voucherType = voucherType;
    }

    public String getVoucherUrl() {
        return voucherUrl;
    }

    public void setVoucherUrl(String voucherUrl) {
        this.voucherUrl = voucherUrl;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Long getConfirmUserId() {
        return confirmUserId;
    }

    public void setConfirmUserId(Long confirmUserId) {
        this.confirmUserId = confirmUserId;
    }

    public String getConfirmUserName() {
        return confirmUserName;
    }

    public void setConfirmUserName(String confirmUserName) {
        this.confirmUserName = confirmUserName;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }

    public String getConfirmResult() {
        return confirmResult;
    }

    public void setConfirmResult(String confirmResult) {
        this.confirmResult = confirmResult;
    }

    public String getConfirmRemark() {
        return confirmRemark;
    }

    public void setConfirmRemark(String confirmRemark) {
        this.confirmRemark = confirmRemark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    @Override
    public String toString() {
        return "BankPaymentRecord{" +
                "recordId=" + recordId +
                ", paymentId=" + paymentId +
                ", bankTransactionNo='" + bankTransactionNo + '\'' +
                ", status=" + status +
                ", confirmResult='" + confirmResult + '\'' +
                '}';
    }
}