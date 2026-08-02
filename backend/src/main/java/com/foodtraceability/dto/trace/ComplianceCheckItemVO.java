package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合规检查项VO
 */
@Schema(description = "合规检查项信息")
public class ComplianceCheckItemVO {

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long itemId;

    /** 关联报告ID */
    @Schema(description = "报告ID")
    private Long reportId;

    /** 检查分类 */
    @Schema(description = "检查分类")
    private Integer checkCategory;

    /** 检查分类名称 */
    @Schema(description = "检查分类名称")
    private String checkCategoryName;

    /** 检查项名称 */
    @Schema(description = "检查项名称")
    private String checkItemName;

    /** 标准要求 */
    @Schema(description = "标准要求")
    private String standardRequirement;

    /** 实际状态 */
    @Schema(description = "实际状态")
    private Integer actualStatus;

    /** 实际状态名称 */
    @Schema(description = "实际状态名称")
    private String actualStatusName;

    /** 证据照片URL */
    @Schema(description = "证据照片URL")
    private String evidencePhotoUrl;

    /** 问题描述 */
    @Schema(description = "问题描述")
    private String issueDescription;

    /** 是否需要整改 */
    @Schema(description = "是否需要整改")
    private Boolean rectificationRequired;

    /** 整改期限 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "整改期限")
    private LocalDate deadline;

    /** 责任人 */
    @Schema(description = "责任人")
    private String responsiblePerson;

    /** 排序 */
    @Schema(description = "排序")
    private Integer sortOrder;

    // Getter和Setter方法

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public Integer getCheckCategory() { return checkCategory; }
    public void setCheckCategory(Integer checkCategory) { this.checkCategory = checkCategory; }
    public String getCheckCategoryName() { return checkCategoryName; }
    public void setCheckCategoryName(String checkCategoryName) { this.checkCategoryName = checkCategoryName; }
    public String getCheckItemName() { return checkItemName; }
    public void setCheckItemName(String checkItemName) { this.checkItemName = checkItemName; }
    public String getStandardRequirement() { return standardRequirement; }
    public void setStandardRequirement(String standardRequirement) { this.standardRequirement = standardRequirement; }
    public Integer getActualStatus() { return actualStatus; }
    public void setActualStatus(Integer actualStatus) { this.actualStatus = actualStatus; }
    public String getActualStatusName() { return actualStatusName; }
    public void setActualStatusName(String actualStatusName) { this.actualStatusName = actualStatusName; }
    public String getEvidencePhotoUrl() { return evidencePhotoUrl; }
    public void setEvidencePhotoUrl(String evidencePhotoUrl) { this.evidencePhotoUrl = evidencePhotoUrl; }
    public String getIssueDescription() { return issueDescription; }
    public void setIssueDescription(String issueDescription) { this.issueDescription = issueDescription; }
    public Boolean getRectificationRequired() { return rectificationRequired; }
    public void setRectificationRequired(Boolean rectificationRequired) { this.rectificationRequired = rectificationRequired; }
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
