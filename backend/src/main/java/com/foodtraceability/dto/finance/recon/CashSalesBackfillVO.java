package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.util.List;

/** 现金销售 SALE 补记结果（P2-B） */
public class CashSalesBackfillVO {
    public Boolean dryRun;
    public Long shiftId;
    public LocalDate from;
    public LocalDate to;
    /** 命中口径：已支付 + 有现金支付记录 + 缺 SALE 流水 */
    public Integer candidateCount;
    /** 实际写入条数（dryRun 时为 0） */
    public Integer writtenCount;
    /** 幂等跳过条数（remark 已存在同 shift 的补记） */
    public Integer skippedCount;
    public Long totalAmountFen;
    public List<Candidate> candidates;

    /** 单笔候选 */
    public static class Candidate {
        /** orders.order_id 是 varchar，这里保持字符串，避免数值转换丢精度 */
        public String orderId;
        public String orderNumber;
        public Long storeId;
        public Long cashAmountFen;
        public Long accountId;
        public LocalDate businessDate;
        public String flowNo;
        public String status;
    }
}
