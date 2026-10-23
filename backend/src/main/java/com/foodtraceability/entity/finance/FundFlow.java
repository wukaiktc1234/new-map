package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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

    /** 备注（人类可读，例如 POS销售入账：T20261024016；不再包含 [orderId=...] 技术串） */
    private String remark;

    /**
     * 业务订单ID（fund_flows.order_id）
     * <p>POS 销售入账写订单ID，幂等查重与列表搜索都走这一列；
     * 非 POS 流水（付款单等）为 NULL。历史行由
     * scripts/db-fix/2026-10-24-fund-flow-order-id.sql 从旧 remark 回填。</p>
     */
    @TableField("order_id")
    private String orderId;

    /** 创建人ID */
    private Long createUserId;

    /**
     * 流水状态
     * 1-待处理 2-已完成 3-已取消（口径见 docs/alignment/finance-system-state-machine.md 3.7）
     * 历史数据里该列曾是 0（列默认值，无对应状态），已由
     * scripts/db-fix/2026-10-23-fund-flow-status-normalize.sql 归一为 1。
     */
    private Integer status;

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

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
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

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
