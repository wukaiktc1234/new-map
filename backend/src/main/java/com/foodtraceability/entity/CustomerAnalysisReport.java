package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 客户分析报表实体类
 * 用于存储客户分析报表的快照数据
 */
@TableName("customer_analysis_reports")
public class CustomerAnalysisReport {

    /**
     * 报表ID
     */
    @TableId(type = IdType.AUTO)
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
     * 平均客单价（分）
     */
    private Long avgTicketSize;

    /**
     * 生命周期分布JSON
     */
    private String customerLifecycleDist;

    /**
     * RFM分布JSON
     */
    private String rfmDistribution;

    /**
     * 客户生命周期价值均值（分）
     */
    private Long clvAvg;

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

    public Long getAvgTicketSize() {
        return avgTicketSize;
    }

    public void setAvgTicketSize(Long avgTicketSize) {
        this.avgTicketSize = avgTicketSize;
    }

    public String getCustomerLifecycleDist() {
        return customerLifecycleDist;
    }

    public void setCustomerLifecycleDist(String customerLifecycleDist) {
        this.customerLifecycleDist = customerLifecycleDist;
    }

    public String getRfmDistribution() {
        return rfmDistribution;
    }

    public void setRfmDistribution(String rfmDistribution) {
        this.rfmDistribution = rfmDistribution;
    }

    public Long getClvAvg() {
        return clvAvg;
    }

    public void setClvAvg(Long clvAvg) {
        this.clvAvg = clvAvg;
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
