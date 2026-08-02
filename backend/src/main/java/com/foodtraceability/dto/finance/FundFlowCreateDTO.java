package com.foodtraceability.dto.finance;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 资金流水创建DTO
 */
@Schema(description = "资金流水创建请求")
public class FundFlowCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "银行账户ID", required = true)
    @NotNull(message = "银行账户ID不能为空")
    private Long accountId;

    @Schema(description = "方向：1-收入 2-支出", required = true)
    @NotNull(message = "收支方向不能为空")
    private Integer flowDirection;

    @Schema(description = "分类：1-销售收款 2-采购付款 3-工资发放 4-税费缴纳 5-内部转账 6-其他 7-采购付款退回", required = true)
    @NotNull(message = "分类不能为空")
    private Integer flowCategory;

    @Schema(description = "金额（单位：分）", required = true)
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于0")
    private Long amount;

    @Schema(description = "对方名称")
    @Size(max = 100, message = "对方名称长度不能超过100位")
    private String counterpartyName;

    @Schema(description = "对方账号")
    @Size(max = 50, message = "对方账号长度不能超过50位")
    private String counterpartyAccount;

    @Schema(description = "业务日期", required = true)
    @NotNull(message = "业务日期不能为空")
    private LocalDate businessDate;

    @Schema(description = "关联凭证ID")
    private Long voucherId;

    @Schema(description = "备注")
    @Size(max = 500, message = "备注长度不能超过500位")
    private String remark;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
}
