package com.foodtraceability.dto.trace;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合规报告创建DTO
 */
@Schema(description = "合规报告创建请求")
public class ComplianceReportCreateDTO {

    /** 报告类型：1日常自查 2月度合规 3专项检查 4年度审计 */
    @NotNull(message = "报告类型不能为空")
    @Schema(description = "报告类型", example = "1")
    private Integer reportType;

    /** 报告期间 */
    @Schema(description = "报告期间", example = "2026年4月")
    private String reportPeriod;

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 检查范围（多选位运算） */
    @NotNull(message = "检查范围不能为空")
    @Schema(description = "检查范围", example = "15")
    private Integer scope;

    /** 检查人ID */
    @Schema(description = "检查人ID")
    private Long inspectorId;

    /** 检查项列表 */
    @Schema(description = "检查项列表")
    private List<CheckItemDTO> checkItems;

    /** 结论与建议 */
    @Schema(description = "结论与建议")
    private String conclusion;

    public Integer getReportType() { return reportType; }
    public void setReportType(Integer reportType) { this.reportType = reportType; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getScope() { return scope; }
    public void setScope(Integer scope) { this.scope = scope; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public List<CheckItemDTO> getCheckItems() { return checkItems; }
    public void setCheckItems(List<CheckItemDTO> checkItems) { this.checkItems = checkItems; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }

    /**
     * 检查项内部类
     */
    @Schema(description = "检查项")
    public static class CheckItemDTO {
        @Schema(description = "检查分类")
        private Integer checkCategory;
        @Schema(description = "检查项名称")
        private String checkItemName;
        @Schema(description = "标准要求")
        private String standardRequirement;
        @Schema(description = "实际状态：1符合 2不符合 3不适用")
        private Integer actualStatus;
        @Schema(description = "证据照片URL")
        private String evidencePhotoUrl;
        @Schema(description = "问题描述")
        private String issueDescription;
        @Schema(description = "是否需要整改")
        private Boolean rectificationRequired;
        @Schema(description = "整改期限")
        private String deadline;
        @Schema(description = "责任人")
        private String responsiblePerson;
        @Schema(description = "排序")
        private Integer sortOrder;

        public Integer getCheckCategory() { return checkCategory; }
        public void setCheckCategory(Integer checkCategory) { this.checkCategory = checkCategory; }
        public String getCheckItemName() { return checkItemName; }
        public void setCheckItemName(String checkItemName) { this.checkItemName = checkItemName; }
        public String getStandardRequirement() { return standardRequirement; }
        public void setStandardRequirement(String standardRequirement) { this.standardRequirement = standardRequirement; }
        public Integer getActualStatus() { return actualStatus; }
        public void setActualStatus(Integer actualStatus) { this.actualStatus = actualStatus; }
        public String getEvidencePhotoUrl() { return evidencePhotoUrl; }
        public void setEvidencePhotoUrl(String evidencePhotoUrl) { this.evidencePhotoUrl = evidencePhotoUrl; }
        public String getIssueDescription() { return issueDescription; }
        public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
        public Boolean getRectificationRequired() { return rectificationRequired; }
        public void setRectificationRequired(Boolean rectificationRequired) { this.rectificationRequired = rectificationRequired; }
        public String getDeadline() { return deadline; }
        public void setDeadline(String deadline) { this.deadline = deadline; }
        public String getResponsiblePerson() { return responsiblePerson; }
        public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
        public Integer getSortOrder() { return sortOrder; }
        public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    }
}
