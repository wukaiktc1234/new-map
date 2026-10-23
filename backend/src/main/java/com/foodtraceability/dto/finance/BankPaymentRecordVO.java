package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 银行打款记录展示VO
 */
@Schema(description = "银行打款记录展示对象")
public class BankPaymentRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "记录ID")
    private Long recordId;

    @Schema(description = "关联付款单ID")
    private Long paymentId;

    @Schema(description = "银行流水号")
    private String bankTransactionNo;

    @Schema(description = "银行交易时间")
    private LocalDateTime bankTransactionTime;

    @Schema(description = "凭证类型")
    private String voucherType;

    @Schema(description = "凭证文件URL")
    private String voucherUrl;

    @Schema(description = "状态：0-待回填 1-已回填 2-确认中 3-成功 4-失败 5-已废弃")
    private Integer status;

    @Schema(description = "确认结果：success/fail")
    private String confirmResult;

    @Schema(description = "确认备注")
    private String confirmRemark;

    @Schema(description = "确认人姓名")
    private String confirmUserName;

    @Schema(description = "确认时间")
    private LocalDateTime confirmTime;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

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

    public String getCreateUserName() {
        return createUserName;
    }

    public void setCreateUserName(String createUserName) {
        this.createUserName = createUserName;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
