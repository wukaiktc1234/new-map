package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日结对账主表实体
 * 对应数据库表 daily_settlements
 * 用于门店每日营业数据的汇总对账，包含收入、成本、利润等核心财务指标
 */
@TableName("daily_settlements")
@Schema(description = "日结对账主表实体")
public class DailySettlement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 结算ID（雪花算法生成） */
    @TableId(type = IdType.ASSIGN_ID)
    @TableField("settlement_id")
    @Schema(description = "结算ID")
    private String settlementId;

    /** 门店ID */
    @TableField("store_id")
    @Schema(description = "门店ID")
    private String storeId;

    /** 结算日期 */
    @TableField("settlement_date")
    @Schema(description = "结算日期")
    private LocalDate settlementDate;

    /** 总营业收入（单位：分） */
    @TableField("total_revenue")
    @Schema(description = "总营业收入（单位：分）")
    private Long totalRevenue;

    /** 总成本（单位：分，敏感字段） */
    @TableField("total_cost")
    @Schema(description = "总成本（单位：分）")
    private Long totalCost;

    /** 净利润（单位：分，敏感字段） */
    @TableField("net_profit")
    @Schema(description = "净利润（单位：分）")
    private Long netProfit;

    /** 毛利率（敏感字段） */
    @TableField("gross_profit_rate")
    @Schema(description = "毛利率")
    private BigDecimal grossProfitRate;

    /** 订单总数 */
    @TableField("order_count")
    @Schema(description = "订单总数")
    private Integer orderCount;

    /** 平均客单价（单位：分） */
    @TableField("avg_order_value")
    @Schema(description = "平均客单价（单位：分）")
    private Long avgOrderValue;

    /** 桌台使用率（百分比） */
    @TableField("table_usage_rate")
    @Schema(description = "桌台使用率")
    private BigDecimal tableUsageRate;

    /** 差异金额（单位：分，系统计算与实际核对差异） */
    @TableField("difference_amount")
    @Schema(description = "差异金额（单位：分）")
    private Long differenceAmount;

    /** 审核人ID */
    @TableField("auditor_id")
    @Schema(description = "审核人ID")
    private String auditorId;

    /** 审核人姓名 */
    @TableField("auditor_name")
    @Schema(description = "审核人姓名")
    private String auditorName;

    /** 结算状态（draft/pending/approved/rejected） */
    @TableField("status")
    @Schema(description = "结算状态")
    private String status;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 支付方式明细（JSONB格式） */
    @TableField("payment_breakdown")
    @Schema(description = "支付方式明细JSONB（cash/wechat/alipay/memberBalance）")
    private String paymentBreakdown;

    /** 优惠总金额（分，从订单DISCOUNT_AMOUNT聚合） */
    @TableField("total_discount_amount")
    @Schema(description = "优惠总金额（分，从订单DISCOUNT_AMOUNT聚合）")
    private Long totalDiscountAmount;

    /** 退款总金额（分） */
    @TableField("refund_amount")
    @Schema(description = "退款总金额（分）")
    private Long refundAmount;

    /** 退款总笔数 */
    @TableField("refund_count")
    @Schema(description = "退款总笔数")
    private Integer refundCount;

    /** 作废订单总金额（分） */
    @TableField("cancelled_amount")
    @Schema(description = "作废订单总金额（分）")
    private Long cancelledAmount;

    /** 作废订单总笔数 */
    @TableField("cancelled_count")
    @Schema(description = "作废订单总笔数")
    private Integer cancelledCount;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号")
    private Long version;

    /** 逻辑删除标记（0=未删除 1=已删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记")
    private Integer deleted;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ==================== Getter & Setter 方法 ====================

    public String getSettlementId() {
        return settlementId;
    }

    public void setSettlementId(String settlementId) {
        this.settlementId = settlementId;
    }

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public LocalDate getSettlementDate() {
        return settlementDate;
    }

    public void setSettlementDate(LocalDate settlementDate) {
        this.settlementDate = settlementDate;
    }

    public Long getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(Long totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public Long getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Long totalCost) {
        this.totalCost = totalCost;
    }

    public Long getNetProfit() {
        return netProfit;
    }

    public void setNetProfit(Long netProfit) {
        this.netProfit = netProfit;
    }

    public BigDecimal getGrossProfitRate() {
        return grossProfitRate;
    }

    public void setGrossProfitRate(BigDecimal grossProfitRate) {
        this.grossProfitRate = grossProfitRate;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public Long getAvgOrderValue() {
        return avgOrderValue;
    }

    public void setAvgOrderValue(Long avgOrderValue) {
        this.avgOrderValue = avgOrderValue;
    }

    public BigDecimal getTableUsageRate() {
        return tableUsageRate;
    }

    public void setTableUsageRate(BigDecimal tableUsageRate) {
        this.tableUsageRate = tableUsageRate;
    }

    public Long getDifferenceAmount() {
        return differenceAmount;
    }

    public void setDifferenceAmount(Long differenceAmount) {
        this.differenceAmount = differenceAmount;
    }

    public String getAuditorId() {
        return auditorId;
    }

    public void setAuditorId(String auditorId) {
        this.auditorId = auditorId;
    }

    public String getAuditorName() {
        return auditorName;
    }

    public void setAuditorName(String auditorName) {
        this.auditorName = auditorName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPaymentBreakdown() {
        return paymentBreakdown;
    }

    public void setPaymentBreakdown(String paymentBreakdown) {
        this.paymentBreakdown = paymentBreakdown;
    }

    public Long getTotalDiscountAmount() {
        return totalDiscountAmount;
    }

    public void setTotalDiscountAmount(Long totalDiscountAmount) {
        this.totalDiscountAmount = totalDiscountAmount;
    }

    public Long getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(Long refundAmount) {
        this.refundAmount = refundAmount;
    }

    public Integer getRefundCount() {
        return refundCount;
    }

    public void setRefundCount(Integer refundCount) {
        this.refundCount = refundCount;
    }

    public Long getCancelledAmount() {
        return cancelledAmount;
    }

    public void setCancelledAmount(Long cancelledAmount) {
        this.cancelledAmount = cancelledAmount;
    }

    public Integer getCancelledCount() {
        return cancelledCount;
    }

    public void setCancelledCount(Integer cancelledCount) {
        this.cancelledCount = cancelledCount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
