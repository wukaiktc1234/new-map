package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 合规检查项明细实体类
 * 记录每个合规检查项的具体结果，支持整改跟踪
 */
@TableName("compliance_check_items")
@Schema(description = "合规检查项明细实体")
public class ComplianceCheckItem {

    /** 主键ID */
    @TableId(value = "item_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long itemId;

    /** 关联报告ID */
    @TableField("report_id")
    @Schema(description = "报告ID")
    private Long reportId;

    /** 检查分类：1食材溯源 2人员健康 3环境卫生 4设备设施 */
    @TableField("check_category")
    @Schema(description = "检查分类", example = "1")
    private Integer checkCategory;

    /** 检查项名称 */
    @TableField("check_item_name")
    @Schema(description = "检查项名称", example = "进货查验记录是否完整")
    private String checkItemName;

    /** 标准要求 */
    @TableField("standard_requirement")
    @Schema(description = "标准要求")
    private String standardRequirement;

    /** 实际状态：1符合 2不符合 3不适用 */
    @TableField("actual_status")
    @Schema(description = "实际状态", example = "1")
    private Integer actualStatus;

    /** 证据照片URL */
    @TableField("evidence_photo_url")
    @Schema(description = "证据照片URL")
    private String evidencePhotoUrl;

    /** 问题描述 */
    @TableField("issue_description")
    @Schema(description = "问题描述")
    private String issueDescription;

    /** 是否需要整改 */
    @TableField("rectification_required")
    @Schema(description = "是否需要整改")
    private Boolean rectificationRequired;

    /** 整改期限 */
    @TableField("deadline")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "整改期限")
    private LocalDate deadline;

    /** 责任人 */
    @TableField("responsible_person")
    @Schema(description = "责任人", example = "张三")
    private String responsiblePerson;

    /** 排序 */
    @TableField("sort_order")
    @Schema(description = "排序", example = "1")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    // Getter和Setter方法

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
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
    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }
    public String getResponsiblePerson() { return responsiblePerson; }
    public void setResponsiblePerson(String responsiblePerson) { this.responsiblePerson = responsiblePerson; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
