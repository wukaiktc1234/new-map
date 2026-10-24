package com.foodtraceability.entity.finance.recon;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账 P0 · 差异池（设计 §5.6，表 recon_differences）。
 *
 * <p><b>金额字段单位一律为「分」(BIGINT)</b>。</p>
 */
@TableName("recon_differences")
public class ReconDifference {

    @TableId(value = "difference_id", type = IdType.AUTO)
    private Long differenceId;

    @TableField("run_id")
    private Long runId;

    @TableField("difference_type")
    private String differenceType;

    @TableField("internal_ref")
    private String internalRef;

    @TableField("external_ref")
    private String externalRef;

    @TableField("amount_internal")
    private Long amountInternal;

    @TableField("amount_external")
    private Long amountExternal;

    @TableField("diff_amount")
    private Long diffAmount;

    @TableField("status")
    private String status;

    @TableField("resolution")
    private String resolution;

    @TableField("resolution_remark")
    private String resolutionRemark;

    @TableField("reverse_flow_no")
    private String reverseFlowNo;

    @TableField("assignee_id")
    private Long assigneeId;

    @TableField("assigned_at")
    private LocalDateTime assignedAt;

    @TableField("resolution_by")
    private Long resolutionBy;

    @TableField("resolution_time")
    private LocalDateTime resolutionTime;

    @TableField("reopen_of")
    private Long reopenOf;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getDifferenceId() { return differenceId; }
    public void setDifferenceId(Long differenceId) { this.differenceId = differenceId; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getDifferenceType() { return differenceType; }
    public void setDifferenceType(String differenceType) { this.differenceType = differenceType; }

    public String getInternalRef() { return internalRef; }
    public void setInternalRef(String internalRef) { this.internalRef = internalRef; }

    public String getExternalRef() { return externalRef; }
    public void setExternalRef(String externalRef) { this.externalRef = externalRef; }

    public Long getAmountInternal() { return amountInternal; }
    public void setAmountInternal(Long amountInternal) { this.amountInternal = amountInternal; }

    public Long getAmountExternal() { return amountExternal; }
    public void setAmountExternal(Long amountExternal) { this.amountExternal = amountExternal; }

    public Long getDiffAmount() { return diffAmount; }
    public void setDiffAmount(Long diffAmount) { this.diffAmount = diffAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }

    public String getResolutionRemark() { return resolutionRemark; }
    public void setResolutionRemark(String resolutionRemark) { this.resolutionRemark = resolutionRemark; }

    public String getReverseFlowNo() { return reverseFlowNo; }
    public void setReverseFlowNo(String reverseFlowNo) { this.reverseFlowNo = reverseFlowNo; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public LocalDateTime getAssignedAt() { return assignedAt; }
    public void setAssignedAt(LocalDateTime assignedAt) { this.assignedAt = assignedAt; }

    public Long getResolutionBy() { return resolutionBy; }
    public void setResolutionBy(Long resolutionBy) { this.resolutionBy = resolutionBy; }

    public LocalDateTime getResolutionTime() { return resolutionTime; }
    public void setResolutionTime(LocalDateTime resolutionTime) { this.resolutionTime = resolutionTime; }

    public Long getReopenOf() { return reopenOf; }
    public void setReopenOf(Long reopenOf) { this.reopenOf = reopenOf; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
