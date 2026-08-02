package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 库存分析报表实体类
 * 用于存储库存分析报表的快照数据
 */
@TableName("inventory_analysis_reports")
public class InventoryAnalysisReport {

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
     * 报表日期
     */
    private Date reportDate;

    /**
     * SKU总数
     */
    private Integer totalSkuCount;

    /**
     * 库存总值（分）
     */
    private Long totalInventoryValue;

    /**
     * 周转率%
     */
    private BigDecimal turnoverRate;

    /**
     * 缺货SKU数
     */
    private Integer outOfStockCount;

    /**
     * 积压SKU数
     */
    private Integer overstockCount;

    /**
     * 报损金额（分）
     */
    private Long wasteAmount;

    /**
     * 预警次数
     */
    private Integer warningCount;

    /**
     * 分类库存分析JSON
     */
    private String categoryAnalysisJson;

    /**
     * 周转率排名JSON
     */
    private String turnoverRankingJson;

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

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public Integer getTotalSkuCount() {
        return totalSkuCount;
    }

    public void setTotalSkuCount(Integer totalSkuCount) {
        this.totalSkuCount = totalSkuCount;
    }

    public Long getTotalInventoryValue() {
        return totalInventoryValue;
    }

    public void setTotalInventoryValue(Long totalInventoryValue) {
        this.totalInventoryValue = totalInventoryValue;
    }

    public BigDecimal getTurnoverRate() {
        return turnoverRate;
    }

    public void setTurnoverRate(BigDecimal turnoverRate) {
        this.turnoverRate = turnoverRate;
    }

    public Integer getOutOfStockCount() {
        return outOfStockCount;
    }

    public void setOutOfStockCount(Integer outOfStockCount) {
        this.outOfStockCount = outOfStockCount;
    }

    public Integer getOverstockCount() {
        return overstockCount;
    }

    public void setOverstockCount(Integer overstockCount) {
        this.overstockCount = overstockCount;
    }

    public Long getWasteAmount() {
        return wasteAmount;
    }

    public void setWasteAmount(Long wasteAmount) {
        this.wasteAmount = wasteAmount;
    }

    public Integer getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Integer warningCount) {
        this.warningCount = warningCount;
    }

    public String getCategoryAnalysisJson() {
        return categoryAnalysisJson;
    }

    public void setCategoryAnalysisJson(String categoryAnalysisJson) {
        this.categoryAnalysisJson = categoryAnalysisJson;
    }

    public String getTurnoverRankingJson() {
        return turnoverRankingJson;
    }

    public void setTurnoverRankingJson(String turnoverRankingJson) {
        this.turnoverRankingJson = turnoverRankingJson;
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
