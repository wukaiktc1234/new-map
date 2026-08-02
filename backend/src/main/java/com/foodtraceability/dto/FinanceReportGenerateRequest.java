package com.foodtraceability.dto;

/**
 * 生成报表请求DTO
 */
public class FinanceReportGenerateRequest {
    private String reportType;
    private String reportPeriod;
    private Long generateBy;

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

    public Long getGenerateBy() {
        return generateBy;
    }

    public void setGenerateBy(Long generateBy) {
        this.generateBy = generateBy;
    }
}
