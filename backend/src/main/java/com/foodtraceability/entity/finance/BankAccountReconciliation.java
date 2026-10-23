package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.foodtraceability.common.BaseEntity;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 银行账户对账结果实体类
 * 记录账户余额与推导余额的对账结果
 */
@TableName("bank_account_reconciliation")
public class BankAccountReconciliation extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 对账记录ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 银行账户ID */
    private Long accountId;

    /** 账户余额（分） */
    private Long balance;

    /** 推导余额（分） */
    private Long derivedBalance;

    /** 差额（分）= balance - derivedBalance */
    private Long difference;

    /**
     * 对账状态
     * MATCH-一致
     * UNMATCH-不一致
     * ERROR-对账异常
     */
    private String reconciliationStatus;

    /** 最后核对时间 */
    private LocalDateTime lastReconciledAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getBalance() {
        return balance;
    }

    public void setBalance(Long balance) {
        this.balance = balance;
    }

    public Long getDerivedBalance() {
        return derivedBalance;
    }

    public void setDerivedBalance(Long derivedBalance) {
        this.derivedBalance = derivedBalance;
    }

    public Long getDifference() {
        return difference;
    }

    public void setDifference(Long difference) {
        this.difference = difference;
    }

    public String getReconciliationStatus() {
        return reconciliationStatus;
    }

    public void setReconciliationStatus(String reconciliationStatus) {
        this.reconciliationStatus = reconciliationStatus;
    }

    public LocalDateTime getLastReconciledAt() {
        return lastReconciledAt;
    }

    public void setLastReconciledAt(LocalDateTime lastReconciledAt) {
        this.lastReconciledAt = lastReconciledAt;
    }

    @Override
    public String toString() {
        return "BankAccountReconciliation{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", balance=" + balance +
                ", derivedBalance=" + derivedBalance +
                ", difference=" + difference +
                ", reconciliationStatus='" + reconciliationStatus + '\'' +
                ", lastReconciledAt=" + lastReconciledAt +
                '}';
    }
}