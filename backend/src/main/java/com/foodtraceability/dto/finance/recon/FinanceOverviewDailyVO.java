package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 资金总览（日）—— 只读聚合视图（P3-4）。
 *
 * <p>金额单位一律**分**。范围由服务端按 {@code data_scope} 强制：all/company → 全部门店；
 * store/department → 仅本人归属门店（无归属 → 403）。</p>
 */
public class FinanceOverviewDailyVO {

    public LocalDate date;
    /** 当前用户的 data_scope（all / company / store / department / self …） */
    public String dataScope;
    /** 店长视角时为其门店；全局视角为 null */
    public Long storeId;

    public List<StoreRow> stores = new ArrayList<>();
    public Channels channels = new Channels();
    public Statements statements = new Statements();

    /** 段1：各店现金户 */
    public static class StoreRow {
        public Long storeId;
        public String storeName;
        public Long cashAccountId;
        public Long balanceFen;
        /** 当日净变动（方向1加、方向2减） */
        public Long netChangeFen;
        /** 当前余额为负 */
        public Boolean negative;
        /**
         * 异常：当日 |净变动| &gt; 该店近 7 日 |净变动| 日均值 × 3。
         * 无历史（近 7 日全为 0）时，仅当当日 |净变动| ≥ 10000 分（100 元）才判异常，避免首日活动噪声。
         */
        public Boolean abnormal;
    }

    /** 段2：渠道（当日 FINALIZED 勾稽批聚合） */
    public static class Channels {
        public Long internalSaleFen = 0L;
        public Long externalReceivedFen = 0L;
        public Long diffFen = 0L;
        /** 这些批下仍处于 OPEN/ASSIGNED/RESOLVING 的差异条数 */
        public Long unexplainedCount = 0L;
        public List<RunRow> runs = new ArrayList<>();
    }

    public static class RunRow {
        public Long runId;
        public Long accountId;
        public Long internalAmount;
        public Long externalAmount;
        public Long differenceCount;
    }

    /** 段3：调节表挂账 */
    public static class Statements {
        /** status NOT IN ('APPROVED') 的张数 */
        public Long openCount = 0L;
        public List<StatusCount> byStatus = new ArrayList<>();
        public Long oldestPendingDays = 0L;
        public Oldest oldest;
    }

    public static class StatusCount {
        public String status;
        public Long count;
    }

    public static class Oldest {
        public Long statementId;
        public String status;
        public Long submittedBy;
        public LocalDateTime submittedAt;
        public Long daysPending;
    }
}
