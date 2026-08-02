package com.foodtraceability.dto.marketing;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;

/**
 * 储值统计概览VO
 * 金额字段以元为单位（字符串），与前端契约一致
 * 对应前端 GET /v1/recharge-stats/overview
 */
@Schema(description = "储值统计概览")
public class RechargeStatsOverviewVO {

    @Schema(description = "总储值余额（元）")
    private String totalBalance;

    @Schema(description = "本金余额（元）")
    private String totalPrincipalBalance;

    @Schema(description = "赠送余额（元）")
    private String totalBonusBalance;

    @Schema(description = "本月充值总额（元）")
    private String rechargeThisMonth;

    @Schema(description = "本月赠送总额（元）")
    private String bonusThisMonth;

    @Schema(description = "本月退款总额（元）")
    private String refundThisMonth;

    @Schema(description = "本月充值笔数")
    private Integer rechargeCountThisMonth;

    @Schema(description = "今日充值笔数")
    private Integer rechargeCountToday;

    @Schema(description = "今日充值总额（元）")
    private String rechargeAmountToday;

    @Schema(description = "平均充值金额（元）")
    private String avgRechargeAmount;

    @Schema(description = "充值方案使用分布")
    private List<Map<String, Object>> planUsageDistribution;

    @Schema(description = "近7天充值趋势")
    private List<Map<String, Object>> weeklyTrend;

    @Schema(description = "余额预警（余额低于100元的会员数）")
    private Integer lowBalanceCount;

    @Schema(description = "即将过期赠送金额（元，30天内）")
    private String expiringBonusAmount;

    @Schema(description = "即将过期赠送笔数")
    private Integer expiringBonusCount;

    // ==================== Getter & Setter ====================

    public String getTotalBalance() { return totalBalance; }
    public void setTotalBalance(String totalBalance) { this.totalBalance = totalBalance; }

    public String getTotalPrincipalBalance() { return totalPrincipalBalance; }
    public void setTotalPrincipalBalance(String totalPrincipalBalance) { this.totalPrincipalBalance = totalPrincipalBalance; }

    public String getTotalBonusBalance() { return totalBonusBalance; }
    public void setTotalBonusBalance(String totalBonusBalance) { this.totalBonusBalance = totalBonusBalance; }

    public String getRechargeThisMonth() { return rechargeThisMonth; }
    public void setRechargeThisMonth(String rechargeThisMonth) { this.rechargeThisMonth = rechargeThisMonth; }

    public String getBonusThisMonth() { return bonusThisMonth; }
    public void setBonusThisMonth(String bonusThisMonth) { this.bonusThisMonth = bonusThisMonth; }

    public String getRefundThisMonth() { return refundThisMonth; }
    public void setRefundThisMonth(String refundThisMonth) { this.refundThisMonth = refundThisMonth; }

    public Integer getRechargeCountThisMonth() { return rechargeCountThisMonth; }
    public void setRechargeCountThisMonth(Integer rechargeCountThisMonth) { this.rechargeCountThisMonth = rechargeCountThisMonth; }

    public Integer getRechargeCountToday() { return rechargeCountToday; }
    public void setRechargeCountToday(Integer rechargeCountToday) { this.rechargeCountToday = rechargeCountToday; }

    public String getRechargeAmountToday() { return rechargeAmountToday; }
    public void setRechargeAmountToday(String rechargeAmountToday) { this.rechargeAmountToday = rechargeAmountToday; }

    public String getAvgRechargeAmount() { return avgRechargeAmount; }
    public void setAvgRechargeAmount(String avgRechargeAmount) { this.avgRechargeAmount = avgRechargeAmount; }

    public List<Map<String, Object>> getPlanUsageDistribution() { return planUsageDistribution; }
    public void setPlanUsageDistribution(List<Map<String, Object>> planUsageDistribution) { this.planUsageDistribution = planUsageDistribution; }

    public List<Map<String, Object>> getWeeklyTrend() { return weeklyTrend; }
    public void setWeeklyTrend(List<Map<String, Object>> weeklyTrend) { this.weeklyTrend = weeklyTrend; }

    public Integer getLowBalanceCount() { return lowBalanceCount; }
    public void setLowBalanceCount(Integer lowBalanceCount) { this.lowBalanceCount = lowBalanceCount; }

    public String getExpiringBonusAmount() { return expiringBonusAmount; }
    public void setExpiringBonusAmount(String expiringBonusAmount) { this.expiringBonusAmount = expiringBonusAmount; }

    public Integer getExpiringBonusCount() { return expiringBonusCount; }
    public void setExpiringBonusCount(Integer expiringBonusCount) { this.expiringBonusCount = expiringBonusCount; }
}
