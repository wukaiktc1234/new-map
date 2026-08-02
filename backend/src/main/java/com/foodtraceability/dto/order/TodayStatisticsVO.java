package com.foodtraceability.dto.order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 今日销售统计VO
 * 用于展示今日销售概况
 */
public class TodayStatisticsVO {

    /** 总订单数 */
    private Long totalOrders;

    /** 已完成订单数 */
    private Long completedOrders;

    /** 已取消订单数 */
    private Long cancelledOrders;

    /** 总销售额（分） */
    private Long totalSalesAmount;

    /** 实收金额（分） */
    private Long actualReceivedAmount;

    /** 退款金额（分） */
    private Long refundAmount;

    /** 订单来源分布 */
    private Map<Integer, Long> orderTypeDistribution;

    /** 支付方式分布 */
    private Map<Integer, Long> paymentMethodDistribution;

    /** 每小时订单数统计 */
    private List<HourlyStats> hourlyStats;

    /** 热销商品TOP10 */
    private List<TopProduct> topProducts;

    /**
     * 每小时统计数据
     */
    public static class HourlyStats {
        private String hour;
        private Long orderCount;
        private Long amount;

        public String getHour() { return hour; }
        public void setHour(String hour) { this.hour = hour; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
        public Long getAmount() { return amount; }
        public void setAmount(Long amount) { this.amount = amount; }
    }

    /**
     * 热销商品
     */
    public static class TopProduct {
        private Long productId;
        private String productName;
        private Long totalQuantity;
        private Long totalAmount;

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public Long getTotalQuantity() { return totalQuantity; }
        public void setTotalQuantity(Long totalQuantity) { this.totalQuantity = totalQuantity; }
        public Long getTotalAmount() { return totalAmount; }
        public void setTotalAmount(Long totalAmount) { this.totalAmount = totalAmount; }
    }

    // Getter和Setter
    public Long getTotalOrders() { return totalOrders; }
    public void setTotalOrders(Long totalOrders) { this.totalOrders = totalOrders; }
    public Long getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Long completedOrders) { this.completedOrders = completedOrders; }
    public Long getCancelledOrders() { return cancelledOrders; }
    public void setCancelledOrders(Long cancelledOrders) { this.cancelledOrders = cancelledOrders; }
    public Long getTotalSalesAmount() { return totalSalesAmount; }
    public void setTotalSalesAmount(Long totalSalesAmount) { this.totalSalesAmount = totalSalesAmount; }
    public Long getActualReceivedAmount() { return actualReceivedAmount; }
    public void setActualReceivedAmount(Long actualReceivedAmount) { this.actualReceivedAmount = actualReceivedAmount; }
    public Long getRefundAmount() { return refundAmount; }
    public void setRefundAmount(Long refundAmount) { this.refundAmount = refundAmount; }
    public Map<Integer, Long> getOrderTypeDistribution() { return orderTypeDistribution; }
    public void setOrderTypeDistribution(Map<Integer, Long> orderTypeDistribution) { this.orderTypeDistribution = orderTypeDistribution; }
    public Map<Integer, Long> getPaymentMethodDistribution() { return paymentMethodDistribution; }
    public void setPaymentMethodDistribution(Map<Integer, Long> paymentMethodDistribution) { this.paymentMethodDistribution = paymentMethodDistribution; }
    public List<HourlyStats> getHourlyStats() { return hourlyStats; }
    public void setHourlyStats(List<HourlyStats> hourlyStats) { this.hourlyStats = hourlyStats; }
    public List<TopProduct> getTopProducts() { return topProducts; }
    public void setTopProducts(List<TopProduct> topProducts) { this.topProducts = topProducts; }
}
