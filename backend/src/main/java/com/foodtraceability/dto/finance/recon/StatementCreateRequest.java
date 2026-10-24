package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.util.List;

/**
 * S3 生成/重算调节表入参。
 *
 * <p>金额单位一律**分**（BIGINT）；元值请调用方先行 ×100（唯一实现 MoneyYuanToFen）。</p>
 */
public class StatementCreateRequest {

    /** 账户 ID（→ bank_accounts.account_id），必填 */
    private Long accountId;
    /** 期间起（首版按自然月） */
    private LocalDate periodStart;
    /** 期间止 */
    private LocalDate periodEnd;
    /** **对账单余额（分）**，必填，来自银行对账单；缺失即拒绝生成（不默认 0） */
    private Long bankStatementBalanceFen;
    /** 是否重算（默认 true）；false 时若已存在 DRAFT 则直接返回既有表 */
    private Boolean forceRecalc = Boolean.TRUE;
    /** 说明；is_balanced=false 时必填差异原因 */
    private String remark;
    /** 可选：人工指定/覆盖未达账项；为空则全自动识别 */
    private List<StatementItemRequest> items;

    public Long getAccountId() { return accountId; }
    public void setAccountId(Long v) { this.accountId = v; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate v) { this.periodStart = v; }
    public LocalDate getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(LocalDate v) { this.periodEnd = v; }
    public Long getBankStatementBalanceFen() { return bankStatementBalanceFen; }
    public void setBankStatementBalanceFen(Long v) { this.bankStatementBalanceFen = v; }
    public Boolean getForceRecalc() { return forceRecalc; }
    public void setForceRecalc(Boolean v) { this.forceRecalc = v; }
    public String getRemark() { return remark; }
    public void setRemark(String v) { this.remark = v; }
    public List<StatementItemRequest> getItems() { return items; }
    public void setItems(List<StatementItemRequest> v) { this.items = v; }
}
