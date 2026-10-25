package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 烧号账（BL-5，表 fund_flow_no_burn_ledger）。
 *
 * <p>记录「号段已消耗、但 fund_flows 没有落库」的流水号，用于对齐
 * {@code order_sequence} 与实际流水，满足审计 A25。</p>
 *
 * <p><b>写入约束</b>：只允许由 {@code FlowNoBurnLedgerRecorder#recordBurn} 以
 * {@code REQUIRES_NEW} 独立事务写入 —— 业务事务即将回滚，同事务写账会被一起滚掉。</p>
 */
@TableName("fund_flow_no_burn_ledger")
public class FundFlowNoBurnLedger {

    @TableId(value = "ledger_id", type = IdType.AUTO)
    private Long ledgerId;

    /** 已消耗但未落库的流水号 */
    @TableField("flow_no")
    private String flowNo;

    /** 号段键，如 FLOW20261025 */
    @TableField("seq_key")
    private String seqKey;

    /** 被消耗的序号 */
    @TableField("seq_value")
    private Long seqValue;

    /** 失败原因（duplicate-key / 乐观锁耗尽 / 业务校验失败 …） */
    @TableField("reason")
    private String reason;

    @TableField("created_by")
    private String createdBy;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("archived_flag")
    private Boolean archivedFlag;

    @TableField("archived_time")
    private LocalDateTime archivedTime;

    public Long getLedgerId() { return ledgerId; }
    public void setLedgerId(Long ledgerId) { this.ledgerId = ledgerId; }

    public String getFlowNo() { return flowNo; }
    public void setFlowNo(String flowNo) { this.flowNo = flowNo; }

    public String getSeqKey() { return seqKey; }
    public void setSeqKey(String seqKey) { this.seqKey = seqKey; }

    public Long getSeqValue() { return seqValue; }
    public void setSeqValue(Long seqValue) { this.seqValue = seqValue; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public Boolean getArchivedFlag() { return archivedFlag; }
    public void setArchivedFlag(Boolean archivedFlag) { this.archivedFlag = archivedFlag; }

    public LocalDateTime getArchivedTime() { return archivedTime; }
    public void setArchivedTime(LocalDateTime archivedTime) { this.archivedTime = archivedTime; }
}
