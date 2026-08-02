package com.foodtraceability.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Date;

/**
 * 销售分析查询条件DTO
 * 用于封装销售分析的查询条件
 */
public class SalesAnalysisQueryDTO {

    /**
     * 报表类型
     * 1日报 2周报 3月报 4季报 5年报
     */
    @Min(value = 1, message = "报表类型最小值为1")
    @Max(value = 5, message = "报表类型最大值为5")
    private Integer reportType;

    /**
     * 开始日期
     */
    private Date startDate;

    /**
     * 结束日期
     */
    private Date endDate;

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
    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
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
