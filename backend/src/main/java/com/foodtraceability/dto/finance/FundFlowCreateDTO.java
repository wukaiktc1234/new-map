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

    /** 操作人ID（审计留痕）：为空时服务层取当前登录用户，仍取不到则记 0 并 WARN，绝不留 null。 */
    @Schema(description = "操作人用户ID（审计）")
    private Long createUserId;

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
    }

    /** 业务订单ID（POS 销售入账必填，用于幂等与按单号搜索；其它流水可空） */
    @Schema(description = "业务订单ID（可空）")
    @Size(max = 64, message = "订单ID长度不能超过64位")
    private String orderId;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * 流水状态：为空时服务层默认 2（已完成）。
     * 本系统资金流水都由「钱已经动了」的路径创建（POS 销售入账、付款单、人工确认入账），没有草稿态。
     */
    @Schema(description = "流水状态（1=待处理 2=已完成，默认 2）")
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

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
