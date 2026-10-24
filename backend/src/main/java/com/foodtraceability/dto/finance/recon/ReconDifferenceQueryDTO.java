package com.foodtraceability.dto.finance.recon;

/** R8 差异池查询条件（status / type / runId / assigneeId + 分页）。 */
public class ReconDifferenceQueryDTO {

    /** OPEN / ASSIGNED / RESOLVING / CLOSED / EXEMPT / PENDING_P2_REVERSE */
    private String status;
    /** AMOUNT_DIFF / TIME_DIFF / ONE_SIDED_INTERNAL / ONE_SIDED_EXTERNAL / DUPLICATE / CHANNEL_FEE / AMBIGUOUS */
    private String type;
    private Long runId;
    private Long assigneeId;
    private Long current = 1L;
    private Long size = 20L;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
