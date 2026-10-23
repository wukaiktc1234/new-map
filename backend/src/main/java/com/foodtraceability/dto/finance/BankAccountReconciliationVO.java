package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * 银行账户对账结果VO
 */
public class BankAccountReconciliationVO {

    /** 对账记录ID */
    private Long id;

    /** 银行账户ID */
    private Long accountId;

    /** 账户名称 */
    private String accountName;

    /** 账户余额（分） */
    private Long balance;

    /** 推导余额（分） */
    private Long derivedBalance;

    /** 差额（分）= balance - derivedBalance */
    private Long difference;

    /** 对账状态：MATCH-一致/UNMATCH-不一致/ERROR-对账异常 */
    private String reconciliationStatus;

    /** 最后核对时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReconciledAt;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "BankAccountReconciliationVO{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", accountName='" + accountName + '\'' +
                ", balance=" + balance +
                ", derivedBalance=" + derivedBalance +
                ", difference=" + difference +
                ", reconciliationStatus='" + reconciliationStatus + '\'' +
                ", lastReconciledAt=" + lastReconciledAt +
                ", createdAt=" + createdAt +
                '}';
    }
}