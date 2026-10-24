package com.foodtraceability.dto.finance.recon;

import java.time.LocalDate;

/** 未达账项明细视图（S5 详情用） */
public class ReconciliationItemVO {
    public Long itemId;
    public String category;
    public String categoryName;
    public Long flowId;
    public Long transactionId;
    public Long amount;
    public LocalDate businessDate;
    public LocalDate periodStart;
    public LocalDate periodEnd;
    public Long ownerUserId;
    public String ownerName;
    public String reason;
    public Boolean clearedFlag;
}
