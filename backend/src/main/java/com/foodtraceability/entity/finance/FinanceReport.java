package com.foodtraceability.entity.finance;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import java.util.Date;

/**
 * 财务报表实体类
 * 用于存储财务报表的基本信息
 */
@TableName("finance_report")
public class FinanceReport {
    
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
     * 报表类型
     * INCOME_EXPENSE: 收支报表
     * PROFIT: 利润报表
     * TAX: 税务报表
     * BALANCE: 资产负债表
     * CASH_FLOW: 现金流量表
     */
    private String reportType;
    
    /**
     * 报表期间
     * 格式：YYYYMMDD-YYYYMMDD
     */
    private String reportPeriod;
    
    /**
     * 报表状态
     * GENERATING: 生成中
     * COMPLETED: 已完成
     * FAILED: 生成失败
     */
    private String status;
    
    /**
     * 生成人ID
     */
    private Long generateBy;
    
    /**
     * 生成时间
     */
    private Date generateTime;
    
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
    
    public Long getGenerateBy() {
        return generateBy;
    }
    
    public void setGenerateBy(Long generateBy) {
        this.generateBy = generateBy;
    }
    
    public Date getGenerateTime() {
        return generateTime;
    }
    
    public void setGenerateTime(Date generateTime) {
        this.generateTime = generateTime;
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
