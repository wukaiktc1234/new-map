package com.foodtraceability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

/**
 * 自定义报表查询条件DTO
 * 用于封装自定义报表的查询条件
 */
public class CustomReportQueryDTO {

    /**
     * 报表名称（模糊查询）
     */
    private String reportName;

    /**
     * 所有者用户ID
     */
    private Long ownerUserId;

    /**
     * 图表类型
     */
    @Min(value = 1, message = "图表类型最小值为1")
    @Max(value = 5, message = "图表类型最大值为5")
    private Integer chartType;

    /**
     * 是否只查看公开报表
     */
    private Boolean isPublicOnly;

    /**
     * 当前页码
     */
    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    @Min(value = 1, message = "每页条数最小值为1")
    @Max(value = 100, message = "每页条数最大值为100")
    private Integer pageSize = 10;

    // getter和setter方法
    public String getReportName() {
        return reportName;
    }

    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public Integer getChartType() {
        return chartType;
    }

    public void setChartType(Integer chartType) {
        this.chartType = chartType;
    }

    public Boolean getIsPublicOnly() {
        return isPublicOnly;
    }

    public void setIsPublicOnly(Boolean isPublicOnly) {
        this.isPublicOnly = isPublicOnly;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
