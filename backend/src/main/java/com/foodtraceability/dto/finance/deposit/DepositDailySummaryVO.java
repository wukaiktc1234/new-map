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
    /** 门店存在多个基本户且未指定 accountId → true，且 bankInflowFen=null（前端提示「请选择银行账户」） */
    public Boolean bankInflowAccountAmbiguous = Boolean.FALSE;

    /** 当日本次汇总所用账户的 bank_transactions 方向=1 合计（银行进账；歧义时为 null） */
    public Long bankInflowFen = 0L;

    /** NEW-34：最近一次人工确认时间（无确认则 null） */
    public java.time.LocalDateTime confirmedAt;
    /** NEW-34：最近一次确认人 */
    public String confirmedByName;
    /** NEW-34：确认后数据已变化 → true（不自动失效，仅提示） */
    public Boolean confirmedStale = Boolean.FALSE;
    /** NEW-34：本次汇总快照哈希（与确认时比对用） */
    public String snapshotHash;
    /** NEW-34：上次确认时的快照（确认后才非空） */
    public String lastConfirmedSnapshot;

    /** NEW-34 附带：歧义时回给前端的**候选账户**（当日存行单涉及的账户 ∪ 本店启用基本户，去重、按 accountId 升序）。
     *  前端用它替代全局账户列表，避免选中别店账户。 */
    public List<CandidateAccount> candidateAccounts = new ArrayList<>();

    /** 候选账户行 */
    public static class CandidateAccount {
        public Long accountId;
        public String accountName;
        /** 账号（当前 bank_accounts 无账号列 → 恒为 null，保留字段供前端布局） */
        public String accountNo;
    }

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
