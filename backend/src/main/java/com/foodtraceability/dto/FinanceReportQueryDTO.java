package com.foodtraceability.dto;

/**
 * 财务报表查询条件DTO
 * 用于封装财务报表的查询条件
 */
public class FinanceReportQueryDTO {
    
    /**
     * 报表名称
     */
    private String reportName;
    
    /**
     * 报表类型
     */
    private String reportType;
    
    /**
     * 报表期间
     */
    private String reportPeriod;
    
    /**
     * 报表状态
     */
    private String status;
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数
     */
    private Integer pageSize = 10;
    
    // getter和setter方法
    public String getReportName() {
        return reportName;
    }
    
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }
    
    public String getReportType() {
        return reportType;
    }
    
    public void setReportType(String reportType) {
        this.reportType = reportType;
    }
    
    public String getReportPeriod() {
        return reportPeriod;
    }
    
    public void setReportPeriod(String reportPeriod) {
        this.reportPeriod = reportPeriod;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
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