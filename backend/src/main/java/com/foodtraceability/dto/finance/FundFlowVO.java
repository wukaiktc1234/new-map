package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资金流水VO
 */
@Schema(description = "资金流水信息")
public class FundFlowVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "流水ID")
    private Long flowId;

    @Schema(description = "流水号")
    private String flowNo;

    @Schema(description = "银行账户ID")
    private Long accountId;

    @Schema(description = "银行账户名称")
    private String accountName;

    @Schema(description = "方向：1-收入 2-支出")
    private Integer flowDirection;

    @Schema(description = "分类：1-销售收款 2-采购付款 3-工资发放 4-税费缴纳 5-内部转账 6-其他")
    private Integer flowCategory;

    @Schema(description = "金额（单位：分）")
    private Long amount;

    @Schema(description = "交易后余额（单位：分）")
    private Long balanceAfter;

    @Schema(description = "对方名称")
    private String counterpartyName;

    @Schema(description = "对方账号")
    private String counterpartyAccount;

    @Schema(description = "业务日期")
    private LocalDate businessDate;

    @Schema(description = "关联凭证ID")
    private Long voucherId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人ID")
    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public String getFlowNo() {
        return flowNo;
    }

    public void setFlowNo(String flowNo) {
        this.flowNo = flowNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public Integer getFlowDirection() {
        return flowDirection;
    }

    public void setFlowDirection(Integer flowDirection) {
        this.flowDirection = flowDirection;
    }

    public Integer getFlowCategory() {
        return flowCategory;
    }

    public void setFlowCategory(Integer flowCategory) {
        this.flowCategory = flowCategory;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getCounterpartyName() {
        return counterpartyName;
    }

    public void setCounterpartyName(String counterpartyName) {
        this.counterpartyName = counterpartyName;
    }

    public String getCounterpartyAccount() {
        return counterpartyAccount;
    }

    public void setCounterpartyAccount(String counterpartyAccount) {
        this.counterpartyAccount = counterpartyAccount;
    }

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
