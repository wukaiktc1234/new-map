package com.foodtraceability.dto.store.operation.vo;

import com.fasterxml.jackson.annotation.JsonView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日结对账视图对象VO
 * 用于返回给前端的日结对账信息
 *
 * <h2>权限控制说明</h2>
 * <ul>
 *   <li><b>基础字段</b>：所有角色可见</li>
 *   <li><b>敏感字段</b>（totalCost/netProfit/grossProfitRate）：
 *       仅总部角色可见，使用@JsonView(Views.Sensitive.class)标记，
 *       前端根据用户角色决定是否展示这些字段</li>
 * </ul>
 *
 * <h2>金额字段说明</h2>
 * 金额字段统一以元为单位显示（String类型），由DataConverter从数据库的分转换而来
 */
@Schema(description = "日结对账视图对象")
public class DailySettlementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ==================== JsonView定义 ====================

    /**
     * JSON视图分类，用于权限控制敏感字段的序列化
     */
    public static class Views {
        /** 基础视图：所有角色可见 */
        public interface Base {}
        /** 敏感视图：仅总部可见 */
        public interface Sensitive extends Base {}
    }

    // ==================== 基础字段（所有角色可见）====================

    /** 结算ID */
    @JsonView(Views.Base.class)
    @Schema(description = "结算ID", example = "1234567890123456789")
    private String settlementId;

    /** 门店ID */
    @JsonView(Views.Base.class)
    @Schema(description = "门店ID", example = "store001")
    private String storeId;

    /** 门店名称（关联查询） */
    @JsonView(Views.Base.class)
    @Schema(description = "门店名称", example = "北京朝阳店")
    private String storeName;

    /** 结算日期 */
    @JsonView(Views.Base.class)
    @Schema(description = "结算日期", example = "2026-05-11")
    private LocalDate settlementDate;

    /**
     * 总营业收入（单位：元）
     * 由DataConverter将数据库中的分转换为元
     */
    @JsonView(Views.Base.class)
    @Schema(description = "总营业收入（元）", example = "15800.50")
    private String totalRevenue;

    /** 订单总数 */
    @JsonView(Views.Base.class)
    @Schema(description = "订单总数", example = "156")
    private Integer orderCount;

    /**
     * 平均客单价（单位：元）
     * 由DataConverter将数据库中的分转换为元
     */
    @JsonView(Views.Base.class)
    @Schema(description = "平均客单价（元）", example = "101.28")
    private String avgOrderValue;

    /** 桌台使用率（百分比） */
    @JsonView(Views.Base.class)
    @Schema(description = "桌台使用率", example = "85.5")
    private BigDecimal tableUsageRate;

    /**
     * 差异金额（单位：元）
     * 系统计算与实际核对差异，由DataConverter转换
     */
    @JsonView(Views.Base.class)
    @Schema(description = "差异金额（元）", example = "-12.00")
    private String differenceAmount;

    /** 结算状态（draft/pending/approved/rejected） */
    @JsonView(Views.Base.class)
    @Schema(description = "结算状态", example = "approved")
    private String status;

    /** 结算状态名称 */
    @JsonView(Views.Base.class)
    @Schema(description = "结算状态名称", example = "已确认")
    private String statusName;

    /** 备注 */
    @JsonView(Views.Base.class)
    @Schema(description = "备注", example = "数据核对无误")
    private String remark;

    /** 创建时间 */
    @JsonView(Views.Base.class)
    @Schema(description = "创建时间", example = "2026-05-12T01:00:00")
    private LocalDateTime createTime;

    // ==================== 敏感字段（仅总部可见）====================

    /**
     * 总成本（单位：元）
     * 敏感字段：仅总部角色可见
     * 由DataConverter将数据库中的分转换为元
     */
    @JsonView(Views.Sensitive.class)
    @Schema(description = "总成本（元，敏感字段）", example = "8500.30")
    private String totalCost;

    /**
     * 净利润（单位：元）
     * 敏感字段：仅总部角色可见
     * 由DataConverter将数据库中的分转换为元
     */
    @JsonView(Views.Sensitive.class)
    @Schema(description = "净利润（元，敏感字段）", example = "7300.20")
    private String netProfit;

    /**
     * 毛利率
     * 敏感字段：仅总部角色可见
     */
    @JsonView(Views.Sensitive.class)
    @Schema(description = "毛利率（敏感字段）", example = "46.20")
    private BigDecimal grossProfitRate;

    // ==================== 扩展字段 ====================

    /** 审核人ID */
    @JsonView(Views.Base.class)
    @Schema(description = "审核人ID", example = "auditor001")
    private String auditorId;

    /** 审核人姓名 */
    @JsonView(Views.Base.class)
    @Schema(description = "审核人姓名", example = "李四")
    private String auditorName;

    /** 审核时间 */
    @JsonView(Views.Base.class)
    @Schema(description = "审核时间", example = "2026-05-12T09:30:00")
    private LocalDateTime auditTime;

    /**
     * 班次明细列表
     * 包含早班、晚班、夜班的详细营业数据
     */
    @JsonView(Views.Base.class)
    @Schema(description = "班次明细列表")
    private List<DailySettlementShiftVO> shifts;

    // ==================== 支付方式与金额统计字段 ====================

    /**
     * 支付方式明细（JSONB解析后的Map）
     * 结构：{"cash": {"amount": xxx, "count": xxx}, "wechat": {...}, "alipay": {...}, "memberBalance": {...}}
     * 金额单位：分（Long类型），由前端DataConverter转换为元显示
     */
    @JsonView(Views.Base.class)
    @Schema(description = "支付方式明细（按支付方式聚合）")
    private Map<String, PaymentMethodDetail> paymentBreakdown;

    /**
     * 优惠总金额（单位：元，用于前端显示）
     * 由DataConverter从数据库的分转换而来
     */
    @JsonView(Views.Base.class)
    @Schema(description = "优惠总金额（元）", example = "580.00")
    private BigDecimal totalDiscountAmountDisplay;

    /**
     * 退款总金额（单位：元，用于前端显示）
     * 由DataConverter从数据库的分转换而来
     */
    @JsonView(Views.Base.class)
    @Schema(description = "退款总金额（元）", example = "120.00")
    private BigDecimal refundAmountDisplay;

    /**
     * 作废订单总金额（单位：元，用于前端显示）
     * 由DataConverter从数据库的分转换而来
     */
    @JsonView(Views.Base.class)
    @Schema(description = "作废订单总金额（元）", example = "50.00")
    private BigDecimal cancelledAmountDisplay;

    // ==================== Getter & Setter ====================

    // 基础字段getter/setter
    public String getSettlementId() { return settlementId; }
    public void setSettlementId(String settlementId) { this.settlementId = settlementId; }

    public String getStoreId() { return storeId; }
    public void setStoreId(String storeId) { this.storeId = storeId; }

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public LocalDate getSettlementDate() { return settlementDate; }
    public void setSettlementDate(LocalDate settlementDate) { this.settlementDate = settlementDate; }

    public String getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(String totalRevenue) { this.totalRevenue = totalRevenue; }

    public Integer getOrderCount() { return orderCount; }
    public void setOrderCount(Integer orderCount) { this.orderCount = orderCount; }

    public String getAvgOrderValue() { return avgOrderValue; }
    public void setAvgOrderValue(String avgOrderValue) { this.avgOrderValue = avgOrderValue; }

    public BigDecimal getTableUsageRate() { return tableUsageRate; }
    public void setTableUsageRate(BigDecimal tableUsageRate) { this.tableUsageRate = tableUsageRate; }

    public String getDifferenceAmount() { return differenceAmount; }
    public void setDifferenceAmount(String differenceAmount) { this.differenceAmount = differenceAmount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // 敏感字段getter/setter
    public String getTotalCost() { return totalCost; }
    public void setTotalCost(String totalCost) { this.totalCost = totalCost; }

    public String getNetProfit() { return netProfit; }
    public void setNetProfit(String netProfit) { this.netProfit = netProfit; }

    public BigDecimal getGrossProfitRate() { return grossProfitRate; }
    public void setGrossProfitRate(BigDecimal grossProfitRate) { this.grossProfitRate = grossProfitRate; }

    // 扩展字段getter/setter
    public String getAuditorId() { return auditorId; }
    public void setAuditorId(String auditorId) { this.auditorId = auditorId; }

    public String getAuditorName() { return auditorName; }
    public void setAuditorName(String auditorName) { this.auditorName = auditorName; }

    public LocalDateTime getAuditTime() { return auditTime; }
    public void setAuditTime(LocalDateTime auditTime) { this.auditTime = auditTime; }

    public List<DailySettlementShiftVO> getShifts() { return shifts; }
    public void setShifts(List<DailySettlementShiftVO> shifts) { this.shifts = shifts; }

    // 支付方式与金额统计字段getter/setter
    public Map<String, PaymentMethodDetail> getPaymentBreakdown() { return paymentBreakdown; }
    public void setPaymentBreakdown(Map<String, PaymentMethodDetail> paymentBreakdown) { this.paymentBreakdown = paymentBreakdown; }

    public BigDecimal getTotalDiscountAmountDisplay() { return totalDiscountAmountDisplay; }
    public void setTotalDiscountAmountDisplay(BigDecimal totalDiscountAmountDisplay) { this.totalDiscountAmountDisplay = totalDiscountAmountDisplay; }

    public BigDecimal getRefundAmountDisplay() { return refundAmountDisplay; }
    public void setRefundAmountDisplay(BigDecimal refundAmountDisplay) { this.refundAmountDisplay = refundAmountDisplay; }

    public BigDecimal getCancelledAmountDisplay() { return cancelledAmountDisplay; }
    public void setCancelledAmountDisplay(BigDecimal cancelledAmountDisplay) { this.cancelledAmountDisplay = cancelledAmountDisplay; }

    // ==================== 支付方式明细内部类 ====================

    /**
     * 支付方式明细内部类
     * 用于表示每种支付方式的金额和笔数统计
     */
    public static class PaymentMethodDetail implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 金额（单位：分） */
        @Schema(description = "金额（分）", example = "1580000")
        private Long amount;

        /** 笔数 */
        @Schema(description = "笔数", example = "86")
        private Integer count;

        public PaymentMethodDetail() {}

        public PaymentMethodDetail(Long amount, Integer count) {
            this.amount = amount;
            this.count = count;
        }

        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }

        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
    }
}
