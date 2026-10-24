package com.foodtraceability.dto.finance.recon;

import java.time.LocalDateTime;

/** 差异池视图（§5.6）。金额单位「分」，diff_amount = amount_external − amount_internal。 */
public class ReconDifferenceVO {

    private Long differenceId;
    private Long runId;
    private String differenceType;
    private String internalRef;
    private String externalRef;
    private Long amountInternal;
    private Long amountExternal;
    private Long diffAmount;
    private String status;
    private String resolution;
    private String resolutionRemark;
    private String reverseFlowNo;
    private Long assigneeId;
    private LocalDateTime assignedAt;
    private Long resolutionBy;
    private LocalDateTime resolutionTime;
    private Long reopenOf;
    private LocalDateTime createTime;
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
