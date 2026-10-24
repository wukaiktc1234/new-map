package com.foodtraceability.dto.finance.recon;

import java.util.ArrayList;
import java.util.List;

/** 夹具余额抹平结果 */
public class FixtureReverseVO {
    public Long accountId;
    public Long balanceBefore;
    public Long balanceAfter;
    public Integer versionBefore;
    public Integer versionAfter;
    public Long auditLogId;
    public List<Item> items = new ArrayList<>();

    /** 单条抹平明细 */
    public static class Item {
        public String originalFlowNo;
        public String reverseFlowNo;
        public Long amountFen;
        public Integer reverseDirection;
        public Long balanceAfterFen;
    }
}
