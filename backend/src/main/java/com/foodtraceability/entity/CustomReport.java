package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.util.Date;

/**
 * 自定义报表实体类
 * 用于存储用户自定义的SQL查询报表配置
 */
@TableName("custom_reports")
public class CustomReport {

    /**
     * 报表ID
     */
    @TableId(type = IdType.AUTO)
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
     * 自定义SQL查询
     */
    private String sqlQuery;

    /**
     * 参数配置JSON
     */
    private String queryParamsConfig;

    /**
     * 图表类型
     * 1表格 2柱状图 3折线图 4饼图 5散点图
     */
    private Integer chartType;

    /**
     * 定时执行cron表达式
     */
    private String scheduleCron;

    /**
     * 最后执行时间
     */
    private Date lastExecutedAt;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

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

    public Date getLastExecutedAt() {
        return lastExecutedAt;
    }

    public void setLastExecutedAt(Date lastExecutedAt) {
        this.lastExecutedAt = lastExecutedAt;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
