package com.foodtraceability.dto.finance.recon;

/** R5 勾稽主体列表查询条件（runId / side / subjectStatus / keyword + 分页）。 */
public class ReconItemQueryDTO {

    private Long runId;
    /** INTERNAL / EXTERNAL */
    private String side;
    /** UNMATCHED / MATCHED / AUTO_MATCHED / MANUAL_MATCHED / EXCEPTION */
    private String subjectStatus;
    /** 模糊匹配 internal_ref / external_ref / counterparty_name */
    private String keyword;
    private Long current = 1L;
    private Long size = 20L;

    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }

    public String getSide() { return side; }
    public void setSide(String side) { this.side = side; }

    public String getSubjectStatus() { return subjectStatus; }
    public void setSubjectStatus(String subjectStatus) { this.subjectStatus = subjectStatus; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }

    public Long getCurrent() { return current; }
    public void setCurrent(Long current) { this.current = current; }

    public Long getSize() { return size; }
    public void setSize(Long size) { this.size = size; }
}
