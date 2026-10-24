package com.foodtraceability.dto.finance.recon;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * R1 跑批入参（设计 §2.7 #1）。
 *
 * <p>businessDate 缺省 = 今天（Asia/Shanghai）；force=true 时对同日同账户重建 run，
 * 新 run 记 rebuild_of=旧 run id，旧 run 及其 items 只读不动。</p>
 */
public class ReconRunCreateDTO {

    /** 银行账户ID（必填） */
    @NotNull(message = "accountId 不能为空")
    private Long accountId;

    /** 业务日，缺省今天 */
    private LocalDate businessDate;

    /** 是否强制重算（默认 false） */
    private Boolean force;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public LocalDate getBusinessDate() { return businessDate; }
    public void setBusinessDate(LocalDate businessDate) { this.businessDate = businessDate; }

    public Boolean getForce() { return force; }
    public void setForce(Boolean force) { this.force = force; }
}
