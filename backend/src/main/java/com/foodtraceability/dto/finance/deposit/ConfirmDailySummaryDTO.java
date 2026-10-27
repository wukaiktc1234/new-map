package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** NEW-34：存行日汇总人工确认入参。 */
public class ConfirmDailySummaryDTO {

    @NotNull(message = "date 不能为空（YYYY-MM-DD）")
    private LocalDate date;

    /** 可选：指定银行账户（门店有多个基本户时必填，且必须在 candidateAccounts 内） */
    private Long accountId;

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
}
