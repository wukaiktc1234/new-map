package com.foodtraceability.dto;

import java.util.List;
import java.util.Map;

/**
 * 自定义报表视图对象
 * 用于返回自定义报表的详细信息给前端
 */
public class CustomReportVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表名称
     */
    private String reportName;

    /**
     * 报表描述
     */
    private String reportDescription;

    /**
     * 所有者用户ID
     */
    private Long ownerUserId;

    /**
     * 所有者用户名（关联查询）
     */
    private String ownerUserName;

    /**
     * SQL查询语句（脱敏处理）
     */
    private String sqlQueryDisplay;

    /**
     * 参数配置JSON
     */
    private List<Map<String, Object>> queryParamsConfig;

    /**
     * 图表类型
     */
    private Integer chartType;

    /**
     * 图表类型名称
     */
    private String chartTypeName;

    /**
     * 定时执行cron表达式
     */
    private String scheduleCron;

    /**
     * 最后执行时间
     */
    private String lastExecutedAt;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 创建时间
     */
    private String createTime;

    // getter和setter方法
    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public String getReportDescription() {
        return reportDescription;
    }

    public void setReportDescription(String reportDescription) {
        this.reportDescription = reportDescription;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUserName() {
        return ownerUserName;
    }

    public void setOwnerUserName(String ownerUserName) {
        this.ownerUserName = ownerUserName;
    }

    public String getSqlQueryDisplay() {
        return sqlQueryDisplay;
    }

    public void setSqlQueryDisplay(String sqlQueryDisplay) {
        this.sqlQueryDisplay = sqlQueryDisplay;
    }

    public List<Map<String, Object>> getQueryParamsConfig() {
        return queryParamsConfig;
    }

    public void setQueryParamsConfig(List<Map<String, Object>> queryParamsConfig) {
        this.queryParamsConfig = queryParamsConfig;
    }

    public Integer getChartType() {
        return chartType;
    }

    public void setChartType(Integer chartType) {
        this.chartType = chartType;
    }

    public String getChartTypeName() {
        return chartTypeName;
    }

    public void setChartTypeName(String chartTypeName) {
        this.chartTypeName = chartTypeName;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public String getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(String lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }
}
