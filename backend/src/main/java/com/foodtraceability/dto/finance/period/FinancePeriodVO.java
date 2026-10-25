package com.foodtraceability.dto.finance.period;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** 会计期间（结账视图）：列表行与详情共用。金额单位一律**分**。 */
public class FinancePeriodVO {

    public String periodCode;
    public String periodName;
    public LocalDate startDate;
    public LocalDate endDate;
    public Boolean isClosed;
    /**
     * 语义化状态（P4-4 收口）：CLOSED（is_closed=true）／REOPENED（is_closed=false 但 reopen_time 非空）／OPEN。
     *
     * <p>为什么不能只给 is_closed：反结账后 is_closed=false 但 close_time/close_user_id 仍是历史值，
     * 前端只看布尔值会误读成「从未结账」甚至「可重复结账」。</p>
     */
    public String status;
    /** 原始数值状态（0 未结账 / 1 已结账），保留给需要旧字段的调用方 */
    public Integer statusCode;
    /** 是否被反结账过（= reopen_time 非空） */
    public Boolean reopened;
    public LocalDateTime closeTime;
    public Long closeUserId;
    public String closeUser;
    public Boolean trialBalancePassed;
    public LocalDateTime reopenTime;
    public Long reopenUserId;
    public String reopenReason;
    public String remark;

    public Long accountCount;
    public Long openingTotalFen;
    public Long incomeTotalFen;
    public Long expenseTotalFen;
    public Long closingTotalFen;

    /** 结账后又写入 → dirty=true（快照与当前流水不一致） */
    public Boolean dirty;
    /** 逐账户明细（仅 summary 返回） */
    public List<Row> accounts = new ArrayList<>();

    public static class Row {
        public Long accountId;
        public String accountName;
        public Long openingFen;
        public Long incomeFen;
        public Long expenseFen;
        public Long closingFen;
        public Long txnCount;
        public Long lastBalanceAfter;
        public Long currentIncomeFen;
        public Long currentExpenseFen;
        public Long currentClosingFen;
        public Boolean matched;
        /** 该账户本期试算是否通过（closing == 期间内末笔 balance_after） */
        public Boolean trialBalancePassed;
        /** 结账后该账户又发生流水（仅已结账期间有意义） */
        public Boolean dirty;
    }
}
