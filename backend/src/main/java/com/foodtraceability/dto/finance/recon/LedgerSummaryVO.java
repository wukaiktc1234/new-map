package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** S2 月结汇总 */
public class LedgerSummaryVO {
    public Long accountId;
    public String month;
    public LocalDate monthStart;
    public LocalDate monthEnd;
    /** 月初余额（分） */
    public Long openingBalance;
    /** 本月收入（分） */
    public Long incomeAmount;
    /** 本月支出（分） */
    public Long expenseAmount;
    /** 本月净额（分） */
    public Long netAmount;
    /** 月末余额（分）= 本月最后一笔非豁免 balance_after */
    public Long closingBalance;
    /** 月末余额是否等于「次月 1 日期初」（V2） */
    public Boolean v2Matched;
    /** 本月流水笔数（status=2） */
    public Integer flowCount;
}
