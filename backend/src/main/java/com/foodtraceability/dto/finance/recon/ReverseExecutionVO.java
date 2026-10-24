package com.foodtraceability.dto.finance.recon;

/** REVERSE 执行结果（P2-B） */
public class ReverseExecutionVO {
    public Long differenceId;
    public String originalFlowNo;
    public Integer originalStatus;
    public String reverseFlowNo;
    public Integer reverseDirection;
    public Long reverseAmountFen;
    public Long accountId;
    public Long balanceBeforeFen;
    public Long balanceAfterFen;
    public String differenceStatus;
    public String resolution;
    public Boolean idempotentHit;
}
