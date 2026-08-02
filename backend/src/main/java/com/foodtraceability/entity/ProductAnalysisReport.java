package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 产品分析报表实体类
 * 用于存储产品分析报表的快照数据
 */
@TableName("product_analysis_reports")
public class ProductAnalysisReport {

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
     * 统计周期开始日期
     */
    private Date periodStart;

    /**
     * 统计周期结束日期
     */
    private Date periodEnd;

    /**
     * 在售菜品数
     */
    private Integer totalFoodsCount;

    /**
     * 套餐数
     */
    private Integer totalCombosCount;

    /**
     * 毛利总额（分）
     */
    private Long grossProfitTotal;

    /**
     * 毛利率%
     */
    private BigDecimal grossProfitRate;

    /**
     * 菜品毛利贡献TOP10 JSON
     */
    private String foodContributionTop10;

    /**
     * 套餐受欢迎度排名JSON
     */
    private String comboPopularity;

    /**
     * 新品表现JSON
     */
    private String newFoodPerformance;

    /**
     * 价格敏感度数据JSON
     */
    private String priceSensitivityData;

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

    public Date getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getTotalFoodsCount() {
        return totalFoodsCount;
    }

    public void setTotalFoodsCount(Integer totalFoodsCount) {
        this.totalFoodsCount = totalFoodsCount;
    }

    public Integer getTotalCombosCount() {
        return totalCombosCount;
    }

    public void setTotalCombosCount(Integer totalCombosCount) {
        this.totalCombosCount = totalCombosCount;
    }

    public Long getGrossProfitTotal() {
        return grossProfitTotal;
    }

    public void setGrossProfitTotal(Long grossProfitTotal) {
        this.grossProfitTotal = grossProfitTotal;
    }

    public BigDecimal getGrossProfitRate() {
        return grossProfitRate;
    }

    public void setGrossProfitRate(BigDecimal grossProfitRate) {
        this.grossProfitRate = grossProfitRate;
    }

    public String getFoodContributionTop10() {
        return foodContributionTop10;
    }

    public void setFoodContributionTop10(String foodContributionTop10) {
        this.foodContributionTop10 = foodContributionTop10;
    }

    public String getComboPopularity() {
        return comboPopularity;
    }

    public void setComboPopularity(String comboPopularity) {
        this.comboPopularity = comboPopularity;
    }

    public String getNewFoodPerformance() {
        return newFoodPerformance;
    }

    public void setNewFoodPerformance(String newFoodPerformance) {
        this.newFoodPerformance = newFoodPerformance;
    }

    public String getPriceSensitivityData() {
        return priceSensitivityData;
    }

    public void setPriceSensitivityData(String priceSensitivityData) {
        this.priceSensitivityData = priceSensitivityData;
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
