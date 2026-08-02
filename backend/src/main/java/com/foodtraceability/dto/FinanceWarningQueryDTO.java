package com.foodtraceability.dto;

/**
 * 财务风险预警查询条件DTO
 * 用于封装财务风险预警的查询条件
 */
public class FinanceWarningQueryDTO {
    
    /**
     * 预警类型
     */
    private String warningType;
    
    /**
     * 预警级别
     */
    private String warningLevel;
    
    /**
     * 预警状态
     */
    private String status;
    
    /**
     * 预警标题
     */
    private String title;
    
    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    
    /**
     * 每页条数
     */
    private Integer pageSize = 10;
    
    // getter和setter方法
    public String getWarningType() {
        return warningType;
    }
    
    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }
    
    public String getWarningLevel() {
        return warningLevel;
    }
    
    public void setWarningLevel(String warningLevel) {
        this.warningLevel = warningLevel;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
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