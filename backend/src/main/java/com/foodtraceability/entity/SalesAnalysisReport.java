package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableField;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 销售分析报表实体类
 * 用于存储销售分析报表的快照数据
 */
@TableName("sales_analysis_reports")
public class SalesAnalysisReport {

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
     * 报表类型
     * 1日报 2周报 3月报 4季报 5年报
     */
    private Integer reportType;

    /**
     * 门店ID（支持按门店生成报表，null表示全门店聚合）
     */
    @TableField("store_id")
    private Long storeId;

    /**
     * 统计周期开始日期
     */
    private Date periodStart;

    /**
     * 统计周期结束日期
     */
    private Date periodEnd;

    /**
     * 订单总数
     */
    private Integer totalOrders;

    /**
     * 总销售额（分）
     */
    private Long totalAmount;

    /**
     * 客单价（分）
     */
    private Long avgOrderValue;

    /**
     * 退款单数
     */
    private Integer refundCount;

    /**
     * 退款额（分）
     */
    private Long refundAmount;

    /**
     * 堂食销售额（分）
     */
    private Long dineInAmount;

    /**
     * 外卖销售额（分）
     */
    private Long takeoutAmount;

    /**
     * 自提销售额（分）
     */
    private Long selfPickupAmount;

    /**
     * 高峰时段（0-23）
     */
    private Integer peakHour;

    /**
     * TOP10菜品销量JSON
     */
    private String topFoodsJson;

    /**
     * 支付方式统计JSON
     */
    private String paymentMethodStats;

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

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
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

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public void setTotalOrders(Integer totalOrders) {
        this.totalOrders = totalOrders;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Long getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(Long avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public Integer getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(Integer refundCount) {
        this.refundCount = refundCount;
    }

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Long getDineInAmount() {
        return dineInAmount;
    }

    public void setDineInAmount(Long dineInAmount) {
        this.dineInAmount = dineInAmount;
    }

    public Long getTakeoutAmount() {
        return takeoutAmount;
    }

    public void setTakeoutAmount(Long takeoutAmount) {
        this.takeoutAmount = takeoutAmount;
    }

    public Long getSelfPickupAmount() {
        return selfPickupAmount;
    }

    public void setSelfPickupAmount(Long selfPickupAmount) {
        this.selfPickupAmount = selfPickupAmount;
    }

    public Integer getPeakHour() {
        return peakHour;
    }

    public void setPeakHour(Integer peakHour) {
        this.peakHour = peakHour;
    }

    public String getTopFoodsJson() {
        return topFoodsJson;
    }

    public void setTopFoodsJson(String topFoodsJson) {
        this.topFoodsJson = topFoodsJson;
    }

    public String getPaymentMethodStats() {
        return paymentMethodStats;
    }

    public void setPaymentMethodStats(String paymentMethodStats) {
        this.paymentMethodStats = paymentMethodStats;
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
