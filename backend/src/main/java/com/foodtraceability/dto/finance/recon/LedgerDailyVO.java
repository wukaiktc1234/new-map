package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.util.List;

/** S1 日记账（按日） */
public class LedgerDailyVO {
    public Long accountId;
    public LocalDate from;
    public LocalDate to;
    public List<Row> rows;
    /** 区间内所有日 V1 全部通过 */
    public Boolean allV1Matched;

    /** 单日行 */
    public static class Row {
        public LocalDate businessDate;
        /** 期初余额（分） */
        public Long openingBalance;
        /** 收入合计（分） */
        public Long incomeAmount;
        /** 支出合计（分） */
        public Long expenseAmount;
        /** 当日净额（分）= 收入 − 支出 */
        public Long netAmount;
        /** 期末余额（分）= 期初 + 净额 */
        public Long closingBalance;
        /** 当日最后一笔非豁免 balance_after（分） */
        public Long lastBalanceAfter;
        /** V1：期末 == lastBalanceAfter */
        public Boolean v1Matched;
        /** V1 差异（分），0 表示一致 */
        public Long v1Diff;
        /** 笔数（status=2） */
        public Integer flowCount;
        /** 待处理笔数（status=1，不计金额） */
        public Integer pendingCount;
        /** 已取消笔数（status=3，不计金额） */
        public Integer cancelledCount;
    }
}
