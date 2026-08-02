package com.foodtraceability.dto.trace;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合规报告VO
 */
@Schema(description = "合规报告信息")
public class ComplianceReportVO {

    /** 主键ID */
    @Schema(description = "主键ID")
    private Long reportId;

    /** 报告编号 */
    @Schema(description = "报告编号")
    private String reportNo;

    /** 报告类型 */
    @Schema(description = "报告类型")
    private Integer reportType;

    /** 报告类型名称 */
    @Schema(description = "报告类型名称")
    private String reportTypeName;

    /** 报告期间 */
    @Schema(description = "报告期间")
    private String reportPeriod;

    /** 门店ID */
    @Schema(description = "门店ID")
    private Long storeId;

    /** 门店名称 */
    @Schema(description = "门店名称")
    private String storeName;

    /** 检查范围 */
    @Schema(description = "检查范围")
    private Integer scope;

    /** 检查范围名称列表 */
    @Schema(description = "检查范围名称列表")
    private List<String> scopeNames;

    /** 应检查项总数 */
    @Schema(description = "应检查项总数")
    private Integer totalItems;

    /** 通过项数 */
    @Schema(description = "通过项数")
    private Integer passedItems;

    /** 不通过项数 */
    @Schema(description = "不通过项数")
    private Integer failedItems;

    /** 待整改项数 */
    @Schema(description = "待整改项数")
    private Integer pendingItems;

    /** 得分 */
    @Schema(description = "得分")
    private Integer score;

    /** 等级 */
    @Schema(description = "等级")
    private String grade;

    /** 检查人ID */
    @Schema(description = "检查人ID")
    private Long inspectorId;

    /** 检查人姓名 */
    @Schema(description = "检查人姓名")
    private String inspectorName;

    /** 检查时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "检查时间")
    private LocalDateTime inspectTime;

    /** 结论与建议 */
    @Schema(description = "结论与建议")
    private String conclusion;

    /** 状态 */
    @Schema(description = "状态")
    private Integer status;

    /** 状态名称 */
    @Schema(description = "状态名称")
    private String statusName;

    /** 检查项明细列表 */
    @Schema(description = "检查项明细列表")
    private List<ComplianceCheckItemVO> checkItems;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // Getter和Setter方法

    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getReportNo() { return reportNo; }
    public void setReportNo(String reportNo) { this.reportNo = reportNo; }
    public Integer getReportType() { return reportType; }
    public void setReportType(Integer reportType) { this.reportType = reportType; }
    public String getReportTypeName() { return reportTypeName; }
    public void setReportTypeName(String reportTypeName) { this.reportTypeName = reportTypeName; }
    public String getReportPeriod() { return reportPeriod; }
    public void setReportPeriod(String reportPeriod) { this.reportPeriod = reportPeriod; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public Integer getScope() { return scope; }
    public void setScope(Integer scope) { this.scope = scope; }
    public List<String> getScopeNames() { return scopeNames; }
    public void setScopeNames(List<String> scopeNames) { this.scopeNames = scopeNames; }
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
    public String getInspectorName() { return inspectorName; }
    public void setInspectorName(String inspectorName) { this.inspectorName = inspectorName; }
    public LocalDateTime getInspectTime() { return inspectTime; }
    public void setInspectTime(LocalDateTime inspectTime) { this.inspectTime = inspectTime; }
    public String getConclusion() { return conclusion; }
    public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public List<ComplianceCheckItemVO> getCheckItems() { return checkItems; }
    public void setCheckItems(List<ComplianceCheckItemVO> checkItems) { this.checkItems = checkItems; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
