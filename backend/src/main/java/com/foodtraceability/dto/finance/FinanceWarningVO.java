package com.foodtraceability.dto.finance;

import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 财务预警VO
 */
@Schema(description = "财务预警信息")
public class FinanceWarningVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键ID")
    private Long id;

    @Schema(description = "预警类型：BUDGET_OVER、AR_OVERDUE、INVENTORY_OVER、CASH_FLOW_RISK、TAX_RISK")
    private String warningType;

    @Schema(description = "预警级别：LOW、MEDIUM、HIGH、URGENT")
    private String warningLevel;

    @Schema(description = "预警标题")
    private String title;

    @Schema(description = "预警内容")
    private String content;

    @Schema(description = "相关业务ID")
    private String businessId;

    @Schema(description = "相关业务类型")
    private String businessType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "预警日期")
    private LocalDateTime warningDate;

    @Schema(description = "状态：UNHANDLED(未处理)、HANDLING(处理中)、RESOLVED(已解决)")
    private String status;

    @Schema(description = "处理人")
    private String handler;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "处理时间")
    private LocalDateTime handleTime;

    @Schema(description = "处理结果")
    private String handleResult;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getWarningType() { return warningType; }
    public void setWarningType(String warningType) { this.warningType = warningType; }
    public String getWarningLevel() { return warningLevel; }
    public void setWarningLevel(String warningLevel) { this.warningLevel = warningLevel; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getBusinessId() { return businessId; }
    public void setBusinessId(String businessId) { this.businessId = businessId; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public LocalDateTime getWarningDate() { return warningDate; }
    public void setWarningDate(LocalDateTime warningDate) { this.warningDate = warningDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public LocalDateTime getHandleTime() { return handleTime; }
    public void setHandleTime(LocalDateTime handleTime) { this.handleTime = handleTime; }
    public String getHandleResult() { return handleResult; }
    public void setHandleResult(String handleResult) { this.handleResult = handleResult; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
}
