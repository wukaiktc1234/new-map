package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 合规报告实体类
 * 食品安全合规检查报告，支持日常自查、月度合规、专项检查、年度审计
 */
@TableName("compliance_reports")
@Schema(description = "合规报告实体")
public class ComplianceReport {

    /** 主键ID */
    @TableId(value = "report_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long reportId;

    /** 报告编号（唯一） */
    @TableField("report_no")
    @Schema(description = "报告编号", example = "CR20260425001")
    private String reportNo;

    /** 报告类型：1日常自查 2月度合规 3专项检查 4年度审计 */
    @TableField("report_type")
    @Schema(description = "报告类型", example = "1")
    private Integer reportType;

    /** 报告期间 */
    @TableField("report_period")
    @Schema(description = "报告期间", example = "2026年4月")
    private String reportPeriod;

    /** 门店ID */
    @TableField("store_id")
    @Schema(description = "门店ID")
    private Long storeId;

    /** 检查范围：1食材溯源 2人员健康 3环境卫生 4设备设施（支持多选位运算） */
    @TableField("scope")
    @Schema(description = "检查范围", example = "15")
    private Integer scope;

    /** 应检查项总数 */
    @TableField("total_items")
    @Schema(description = "应检查项总数", example = "20")
    private Integer totalItems;

    /** 通过项数 */
    @TableField("passed_items")
    @Schema(description = "通过项数", example = "18")
    private Integer passedItems;

    /** 不通过项数 */
    @TableField("failed_items")
    @Schema(description = "不通过项数", example = "1")
    private Integer failedItems;

    /** 待整改项数 */
    @TableField("pending_items")
    @Schema(description = "待整改项数", example = "1")
    private Integer pendingItems;

    /** 得分0-100 */
    @TableField("score")
    @Schema(description = "得分", example = "90")
    private Integer score;

    /** 等级A/B/C/D/E */
    @TableField("grade")
    @Schema(description = "等级", example = "B")
    private String grade;

    /** 检查人ID */
    @TableField("inspector_id")
    @Schema(description = "检查人ID")
    private Long inspectorId;

    /** 检查时间 */
    @TableField("inspect_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "检查时间")
    private LocalDateTime inspectTime;

    /** 结论与建议 */
    @TableField("conclusion")
    @Schema(description = "结论与建议")
    private String conclusion;

    /** 附件列表JSON */
    @TableField("attachments_json")
    @Schema(description = "附件列表")
    private Object attachmentsJson;

    /** 状态：1草稿 2已提交 3已审核 4已归档 */
    @TableField("status")
    @Schema(description = "状态", example = "1")
    private Integer status;

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

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public Integer getReportType() { return reportType; }
    public void setReportType(Integer reportType) { this.reportType = reportType; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public Integer getScope() { return scope; }
    public void setScope(Integer scope) { this.scope = scope; }
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    public Integer getPassedItems() { return passedItems; }
    public void setPassedItems(Integer passedItems) { this.passedItems = passedItems; }
    public Integer getFailedItems() { return failedItems; }
    public void setFailedItems(Integer failedItems) { this.failedItems = failedItems; }
    public Integer getPendingItems() { return pendingItems; }
    public void setPendingItems(Integer pendingItems) { this.pendingItems = pendingItems; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Long getInspectorId() { return inspectorId; }
    public void setInspectorId(Long inspectorId) { this.inspectorId = inspectorId; }
    public LocalDateTime getInspectTime() { return inspectTime; }
    public void setInspectTime(LocalDateTime inspectTime) { this.inspectTime = inspectTime; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public Object getAttachmentsJson() { return attachmentsJson; }
    public void setAttachmentsJson(Object attachmentsJson) { this.attachmentsJson = attachmentsJson; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
