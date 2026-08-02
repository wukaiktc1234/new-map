package com.foodtraceability.dto.marketing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 营销统计概览VO
 * 用于仪表盘展示关键指标
 */
public class MarketingStatisticsVO {

    // ========== 会员统计 ==========
    /** 总会员数 */
    private Long totalMembers;

    /** 今日新增会员数 */
    private Long todayNewMembers;

    /** 本月新增会员数 */
    private Long monthNewMembers;

    /** 活跃会员数（30天内有消费） */
    private Long activeMembers;

    /** 流失风险会员数（30天未消费） */
    private Long churnRiskMembers;

    // ========== 积分统计 ==========
    /** 累计发放积分 */
    private Long totalPointsIssued;

    /** 累计消耗积分 */
    private Long totalPointsConsumed;

    /** 当前积分余额总量 */
    private Long currentPointsBalance;

    /** 本月积分发放量 */
    private Long monthPointsIssued;

    // ========== 优惠券统计 ==========
    /** 优惠券模板总数 */
    private Long totalCouponTemplates;

    /** 已发放优惠券数量 */
    private Long totalCouponsIssued;

    /** 已使用优惠券数量 */
    private Long totalCouponsUsed;

    /** 待使用优惠券数量 */
    private Long totalCouponsPending;

    // ========== 充值统计 ==========
    /** 今日充值金额（分） */
    private Long todayRechargeAmount;

    /** 本月充值金额（分） */
    private Long monthRechargeAmount;

    /** 累计充值金额（分） */
    private Long totalRechargeAmount;

    /** 会员总余额（分） */
    private Long totalMemberBalance;

    // ========== 促销活动统计 ==========
    /** 进行中活动数 */
    private Long activePromotions;

    /** 活动总参与人次 */
    private Long totalParticipants;

    /** 活动总销售额（分） */
    private Long promotionSalesAmount;

    /** 活动总优惠额（分） */
    private Long promotionDiscountAmount;

    // ========== RFM分布 ==========
    /** RFM客户分层分布 */
    private Map<String, Long> rfmDistribution;

    // ========== 会员等级分布 ==========
    /** 各等级会员数 */
    private Map<String, Long> levelDistribution;

    // ========== 近7天趋势 ==========
    /** 近7天每日数据 */
    private List<DailyTrendVO> dailyTrends;

    /**
     * 每日趋势数据内部类
     */
    public static class DailyTrendVO {
        private String date;
        private Long newMembers;
        private Long rechargeAmount;
        private Long pointsIssued;
        private Long orderCount;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public Long getNewMembers() { return newMembers; }
        public void setNewMembers(Long newMembers) { this.newMembers = newMembers; }
        public Long getRechargeAmount() { return rechargeAmount; }
        public void setRechargeAmount(Long rechargeAmount) { this.rechargeAmount = rechargeAmount; }
        public Long getPointsIssued() { return pointsIssued; }
        public void setPointsIssued(Long pointsIssued) { this.pointsIssued = pointsIssued; }
        public Long getOrderCount() { return orderCount; }
        public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    }

    // ==================== Getter & Setter ====================
    public Long getTotalMembers() { return totalMembers; }
    public void setTotalMembers(Long totalMembers) { this.totalMembers = totalMembers; }
    public Long getTodayNewMembers() { return todayNewMembers; }
    public void setTodayNewMembers(Long todayNewMembers) { this.todayNewMembers = todayNewMembers; }
    public Long getMonthNewMembers() { return monthNewMembers; }
    public void setMonthNewMembers(Long monthNewMembers) { this.monthNewMembers = monthNewMembers; }
    public Long getActiveMembers() { return activeMembers; }
    public void setActiveMembers(Long activeMembers) { this.activeMembers = activeMembers; }
    public Long getChurnRiskMembers() { return churnRiskMembers; }
    public void setChurnRiskMembers(Long churnRiskMembers) { this.churnRiskMembers = churnRiskMembers; }
    public Long getTotalPointsIssued() { return totalPointsIssued; }
    public void setTotalPointsIssued(Long totalPointsIssued) { this.totalPointsIssued = totalPointsIssued; }
    public Long getTotalPointsConsumed() { return totalPointsConsumed; }
    public void setTotalPointsConsumed(Long totalPointsConsumed) { this.totalPointsConsumed = totalPointsConsumed; }
    public Long getCurrentPointsBalance() { return currentPointsBalance; }
    public void setCurrentPointsBalance(Long currentPointsBalance) { this.currentPointsBalance = currentPointsBalance; }
    public Long getMonthPointsIssued() { return monthPointsIssued; }
    public void setMonthPointsIssued(Long monthPointsIssued) { this.monthPointsIssued = monthPointsIssued; }
    public Long getTotalCouponTemplates() { return totalCouponTemplates; }
    public void setTotalCouponTemplates(Long totalCouponTemplates) { this.totalCouponTemplates = totalCouponTemplates; }
    public Long getTotalCouponsIssued() { return totalCouponsIssued; }
    public void setTotalCouponsIssued(Long totalCouponsIssued) { this.totalCouponsIssued = totalCouponsIssued; }
    public Long getTotalCouponsUsed() { return totalCouponsUsed; }
    public void setTotalCouponsUsed(Long totalCouponsUsed) { this.totalCouponsUsed = totalCouponsUsed; }
    public Long getTotalCouponsPending() { return totalCouponsPending; }
    public void setTotalCouponsPending(Long totalCouponsPending) { this.totalCouponsPending = totalCouponsPending; }
    public Long getTodayRechargeAmount() { return todayRechargeAmount; }
    public void setTodayRechargeAmount(Long todayRechargeAmount) { this.todayRechargeAmount = todayRechargeAmount; }
    public Long getMonthRechargeAmount() { return monthRechargeAmount; }
    public void setMonthRechargeAmount(Long monthRechargeAmount) { this.monthRechargeAmount = monthRechargeAmount; }
    public Long getTotalRechargeAmount() { return totalRechargeAmount; }
    public void setTotalRechargeAmount(Long totalRechargeAmount) { this.totalRechargeAmount = totalRechargeAmount; }
    public Long getTotalMemberBalance() { return totalMemberBalance; }
    public void setTotalMemberBalance(Long totalMemberBalance) { this.totalMemberBalance = totalMemberBalance; }
    public Long getActivePromotions() { return activePromotions; }
    public void setActivePromotions(Long activePromotions) { this.activePromotions = activePromotions; }
    public Long getTotalParticipants() { return totalParticipants; }
    public void setTotalParticipants(Long totalParticipants) { this.totalParticipants = totalParticipants; }
    public Long getPromotionSalesAmount() { return promotionSalesAmount; }
    public void setPromotionSalesAmount(Long promotionSalesAmount) { this.promotionSalesAmount = promotionSalesAmount; }
    public Long getPromotionDiscountAmount() { return promotionDiscountAmount; }
    public void setPromotionDiscountAmount(Long promotionDiscountAmount) { this.promotionDiscountAmount = promotionDiscountAmount; }
    public Map<String, Long> getRfmDistribution() { return rfmDistribution; }
    public void setRfmDistribution(Map<String, Long> rfmDistribution) { this.rfmDistribution = rfmDistribution; }
    public Map<String, Long> getLevelDistribution() { return levelDistribution; }
    public void setLevelDistribution(Map<String, Long> levelDistribution) { this.levelDistribution = levelDistribution; }
    public List<DailyTrendVO> getDailyTrends() { return dailyTrends; }
    public void setDailyTrends(List<DailyTrendVO> dailyTrends) { this.dailyTrends = dailyTrends; }
}
