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

    /** 一条待确认退款（字段名与前端 BoundaryRefundPending 对齐） */
    public static class PendingRefund {
        /** 退款记录ID（提交归属时回传） */
        public Long refundRecordId;
        /** 退款单号 —— **正名**（例 FF202610280064） */
        public String refundNo;
        /** 历史别名，与 refundNo 同值；**下一版本删除** */
        public String flowNo;
        public Long amountFen;
        public LocalDateTime refundTime;
        public String orderId;
        /** 距最近班次边界（start/end）的分钟差；≤ 容差即为边界退款 */
        public Long boundaryDeltaMinutes;
        /** START=贴近开班 ｜ END=贴近交班 */
        public String boundaryKind;
        /** 上一班（可能为 null；后端判定，不用数组下标） */
        public CandidateShift previousShift;
        /** 当前班（本单所属班次） */
        public CandidateShift currentShift;
    }

    /** 一个候选班次 */
    public static class CandidateShift {
        public Long shiftId;
        /** 班次业务编号（与 shiftId 不同） */
        public String shiftNo;
        public String label;
        public LocalDateTime startTime;
        public LocalDateTime endTime;
        /** 是否为本单班次的上一班（后端判定） */
        public Boolean isPrevious;
    }
}
