package com.foodtraceability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 自定义报表创建DTO
 * 用于创建或更新自定义报表的请求参数
 */
public class CustomReportCreateDTO {

    /**
     * 报表名称
     */
    @NotBlank(message = "报表名称不能为空")
    @Size(min = 2, max = 100, message = "报表名称长度必须在2-100个字符之间")
    private String reportName;

    /**
     * 报表描述
     */
    @Size(max = 500, message = "报表描述长度不能超过500个字符")
    private String reportDescription;

    /**
     * 自定义SQL查询
     */
    @NotBlank(message = "SQL查询不能为空")
    private String sqlQuery;

    /**
     * 参数配置JSON
     */
    private String queryParamsConfig;

    /**
     * 图表类型
     * 1表格 2柱状图 3折线图 4饼图 5散点图
     */
    @Min(value = 1, message = "图表类型最小值为1")
    @Max(value = 5, message = "图表类型最大值为5")
    private Integer chartType = 1;

    /**
     * 定时执行cron表达式
     */
    private String scheduleCron;

    /**
     * 是否公开
     */
    private Boolean isPublic = false;

    // getter和setter方法
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

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public String getQueryParamsConfig() {
        return queryParamsConfig;
    }

    public void setQueryParamsConfig(String queryParamsConfig) {
        this.queryParamsConfig = queryParamsConfig;
    }

    public Integer getChartType() {
        return chartType;
    }

    public void setChartType(Integer chartType) {
        this.chartType = chartType;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }
}
