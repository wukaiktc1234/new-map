package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 收款单展示VO
 * 金额单位：分（前端通过DataConverter转换为元）
 */
@Schema(description = "收款单展示对象")
public class ReceiptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "收款单ID")
    private Long receiptId;

    @Schema(description = "收款单号")
    private String receiptNo;

    @Schema(description = "关联应收账款ID")
    private Long receivableId;

    @Schema(description = "关联应收编号")
    private String receivableNo;

    @Schema(description = "客户ID")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "收款金额（分）")
    private Long receiptAmount;

    @Schema(description = "收款金额显示（元）")
    private String receiptAmountDisplay;

    @Schema(description = "收款方式")
    private String receiptMethod;

    @Schema(description = "收款方式名称")
    private String receiptMethodName;

    @Schema(description = "收款银行账户ID")
    private Long bankAccountId;

    @Schema(description = "收款账户名称")
    private String bankAccountName;

    @Schema(description = "收款日期")
    private LocalDate receiptDate;

    @Schema(description = "关联资金流水ID")
    private Long fundFlowId;

    @Schema(description = "关联流水号")
    private String fundFlowNo;

    @Schema(description = "关联会计凭证ID")
    private Long voucherId;

    @Schema(description = "关联凭证号")
    private String voucherNo;

    @Schema(description = "状态：1-已确认 2-已作废")
    private Integer status;

    @Schema(description = "状态名称")
    private String statusName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @Schema(description = "创建人姓名")
    private String createUserName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getReceiptId() {
        return receiptId;
    }

    public void setReceiptId(Long receiptId) {
        this.receiptId = receiptId;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public Long getReceivableId() {
        return receivableId;
    }

    public void setReceivableId(Long receivableId) {
        this.receivableId = receivableId;
    }

    public String getReceivableNo() {
        return receivableNo;
    }

    public void setReceivableNo(String receivableNo) {
        this.receivableNo = receivableNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getReceiptAmount() {
        return receiptAmount;
    }

    public void setReceiptAmount(Long receiptAmount) {
        this.receiptAmount = receiptAmount;
    }

    public String getReceiptAmountDisplay() {
        return receiptAmountDisplay;
    }

    public void setReceiptAmountDisplay(String receiptAmountDisplay) {
        this.receiptAmountDisplay = receiptAmountDisplay;
    }

    public String getReceiptMethod() {
        return receiptMethod;
    }

    public void setReceiptMethod(String receiptMethod) {
        this.receiptMethod = receiptMethod;
    }

    public String getReceiptMethodName() {
        return receiptMethodName;
    }

    public void setReceiptMethodName(String receiptMethodName) {
        this.receiptMethodName = receiptMethodName;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public String getBankAccountName() {
        return bankAccountName;
    }

    public void setBankAccountName(String bankAccountName) {
        this.bankAccountName = bankAccountName;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public void setReceiptDate(LocalDate receiptDate) {
        this.receiptDate = receiptDate;
    }

    public Long getFundFlowId() {
        return fundFlowId;
    }

    public void setFundFlowId(Long fundFlowId) {
        this.fundFlowId = fundFlowId;
    }

    public String getFundFlowNo() {
        return fundFlowNo;
    }

    public void setFundFlowNo(String fundFlowNo) {
        this.fundFlowNo = fundFlowNo;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public String getVoucherNo() {
        return voucherNo;
    }

    public void setVoucherNo(String voucherNo) {
        this.voucherNo = voucherNo;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusName() {
        return statusName;
    }

    public void setStatusName(String statusName) {
        this.statusName = statusName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
