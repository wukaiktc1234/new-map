package com.foodtraceability.dto.finance.deposit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * N19-3b：缴款单「待确认归属」契约（前端不得自造口径，四类计数全部由后端给）。
 */
public class DepositSlipPendingRefundsVO {

    /** 本单班次边界带内、且尚无归属记录的现金退款（待人工确认归属） */
    public List<PendingRefund> pendingRefunds = new ArrayList<>();

    /** 候选班次：上一 / 本 / 下一班次（同门店，缺则少给） */
    public List<CandidateShift> candidateShifts = new ArrayList<>();

    /** 本单班次窗口内 refund_time 为 NULL 的退款笔数（不计入任何班次，待回填） */
    public Integer missingRefundTimeCount = 0;

    /** 一条待确认退款 */
    public static class PendingRefund {
        public Long refundRecordId;
        public String flowNo;
        public Long amountFen;
        public LocalDateTime refundTime;
        public String orderId;
    }

    /** 一个候选班次 */
    public static class CandidateShift {
        public Long shiftId;
        public String label;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
    }
}
