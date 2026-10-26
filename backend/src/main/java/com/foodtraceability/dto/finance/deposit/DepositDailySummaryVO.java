package com.foodtraceability.dto.finance.deposit;

import java.util.ArrayList;
import java.util.List;

/** 差异3：日汇总（人工汇总模式）—— 当天已存行缴款单 + 绑流水/未绑流水的金额拆分。 */
public class DepositDailySummaryVO {

    /** 当天 status=DEPOSITED 的缴款单 */
    public List<Row> depositedSlips = new ArrayList<>();

    /** Σ cash_counted_fen（计现额） */
    public Long totalCashCountedFen = 0L;
    /** Σ 已绑到账流水（deposit_bank_txn_id 非空）的计现额 */
    public Long matchedFen = 0L;
    /** Σ 未绑到账流水的计现额（人工汇总模式） */
    public Long unmatchedFen = 0L;
    /** 未绑流水的单数 —— **不得静默**，必须 > 0 时被前端提示 */
    public Integer unmatchedCount = 0;
    /** 当日该批账户 bank_transactions 方向=1 合计（银行进账） */
    public Long bankInflowFen = 0L;

    /** 明细行 */
    public static class Row {
        public Long slipId;
        public String slipNo;
        public Long storeId;
        public Long actualAmount;
        public Long reserveFen;
        public Long cashCountedFen;
        public Long depositBankTxnId;
    }
}
