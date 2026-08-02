package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 客户分析视图对象
 * 用于返回客户分析的详细数据给前端
 */
public class CustomerAnalysisVO {

    /**
     * 报表ID
     */
    private Long reportId;

    /**
     * 报表编号
     */
    private String reportNo;

    /**
     * 统计周期
     */
    private String period;

    /**
     * 会员总数
     */
    private Integer totalMembers;

    /**
     * 新增会员数
     */
    private Integer newMembersCount;

    /**
     * 活跃会员数
     */
    private Integer activeMembersCount;

    /**
     * 流失会员数
     */
    private Integer churnedMembersCount;

    /**
     * 留存率%
     */
    private BigDecimal retentionRate;

    /**
     * 平均消费频次
     */
    private BigDecimal avgFrequency;

    /**
     * 平均客单价（元）
     */
    private String avgTicketSize;

    /**
     * 生命周期分布
     */
    private List<Map<String, Object>> customerLifecycleDist;

    /**
     * RFM分布
     */
    private Map<String, Object> rfmDistribution;

    /**
     * 客户生命周期价值均值（元）
     */
    private String clvAvg;

    /**
     * 生成时间
     */
    private String generateTime;

    // getter和setter方法
    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getReportNo() {
        return reportNo;
    }

    public void setReportNo(String reportNo) {
        this.reportNo = reportNo;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getTotalMembers() {
        return totalMembers;
    }

    public void setTotalMembers(Integer totalMembers) {
        this.totalMembers = totalMembers;
    }

    public Integer getNewMembersCount() {
        return newMembersCount;
    }

    public void setNewMembersCount(Integer newMembersCount) {
        this.newMembersCount = newMembersCount;
    }

    public Integer getActiveMembersCount() {
        return activeMembersCount;
    }

    public void setActiveMembersCount(Integer activeMembersCount) {
        this.activeMembersCount = activeMembersCount;
    }

    public Integer getChurnedMembersCount() {
        return churnedMembersCount;
    }

    public void setChurnedMembersCount(Integer churnedMembersCount) {
        this.churnedMembersCount = churnedMembersCount;
    }

    public BigDecimal getRetentionRate() {
        return retentionRate;
    }

    public void setRetentionRate(BigDecimal retentionRate) {
        this.retentionRate = retentionRate;
    }

    public BigDecimal getAvgFrequency() {
        return avgFrequency;
    }

    public void setAvgFrequency(BigDecimal avgFrequency) {
        this.avgFrequency = avgFrequency;
    }

    public String getAvgTicketSize() {
        return avgTicketSize;
    }

    public void setAvgTicketSize(String avgTicketSize) {
        this.avgTicketSize = avgTicketSize;
    }

    public List<Map<String, Object>> getCustomerLifecycleDist() {
        return customerLifecycleDist;
    }

    public void setCustomerLifecycleDist(List<Map<String, Object>> customerLifecycleDist) {
        this.customerLifecycleDist = customerLifecycleDist;
    }

    public Map<String, Object> getRfmDistribution() {
        return rfmDistribution;
    }

    public void setRfmDistribution(Map<String, Object> rfmDistribution) {
        this.rfmDistribution = rfmDistribution;
    }

    public String getClvAvg() {
        return clvAvg;
    }

    public void setClvAvg(String clvAvg) {
        this.clvAvg = clvAvg;
    }

    public String getGenerateTime() {
        return generateTime;
    }

    public void setGenerateTime(String generateTime) {
        this.generateTime = generateTime;
    }
}
