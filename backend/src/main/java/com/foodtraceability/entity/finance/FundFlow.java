package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 资金流水实体类
 * 用于记录银行账户的资金收支流水，关联凭证与业务单据，支持资金监控与对账
 */
@TableName("fund_flows")
public class FundFlow extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 流水ID */
    @TableId(type = IdType.AUTO)
    private Long flowId;

    /** 流水号 */
    private String flowNo;

    /** 账户ID */
    private Long accountId;

    /**
     * 方向
     * 1-收入 2-支出
     */
    private Integer flowDirection;

    /**
     * 分类
     * 1-销售收款 2-采购付款 3-工资发放 4-税费缴纳 5-内部转账 6-其他 7-采购付款退回
     */
    private Integer flowCategory;

    /** 金额（分） */
    private Long amount;

    /** 交易后余额（分） */
    private Long balanceAfter;

    /** 对方名称 */
    private String counterpartyName;

    /** 对方账号 */
    private String counterpartyAccount;

    /** 业务日期 */
    private LocalDate businessDate;

    /** 关联凭证ID */
    private Long voucherId;

    /** 备注 */
    private String remark;

    /** 创建人ID */
    private Long createUserId;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

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

    @Override
    public String toString() {
        return "FundFlow{" +
                "flowId=" + flowId +
                ", flowNo='" + flowNo + '\'' +
                ", accountId=" + accountId +
                ", flowDirection=" + flowDirection +
                ", flowCategory=" + flowCategory +
                ", amount=" + amount +
                ", balanceAfter=" + balanceAfter +
                ", businessDate=" + businessDate +
                '}';
    }
}
