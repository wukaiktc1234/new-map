package com.foodtraceability.service.impl.marketing;

import com.foodtraceability.dto.marketing.RechargeStatsOverviewVO;
import com.foodtraceability.mapper.marketing.RechargeRecordMapper;
import com.foodtraceability.service.marketing.RechargeStatsService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 储值统计服务实现
 * 聚合充值、退款、余额等数据，为储值仪表盘提供概览统计
 *
 * 数据来源：recharge_records 表（通过 RechargeRecordMapper 的自定义查询）
 * 兼容 H2/PG：使用 EXTRACT、CAST AS DATE、CURRENT_DATE 等通用语法
 */
@Service
public class RechargeStatsServiceImpl implements RechargeStatsService {

    /** 低余额阈值：100 元（单位：分） */
    private static final Long LOW_BALANCE_THRESHOLD = 10000L;
    /** 即将过期阈值：30 天 */
    private static final int EXPIRING_BONUS_DAYS = 30;

    private final RechargeRecordMapper rechargeRecordMapper;

    public RechargeStatsServiceImpl(RechargeRecordMapper rechargeRecordMapper) {
        this.rechargeRecordMapper = rechargeRecordMapper;
    }

    @Override
    public RechargeStatsOverviewVO getOverview() {
        RechargeStatsOverviewVO vo = new RechargeStatsOverviewVO();

        // 1. 基础统计概览（余额、本月、今日）
        Map<String, Object> stats = rechargeRecordMapper.selectStatsOverview();
        if (stats != null) {
            vo.setTotalBalance(formatFenToYuan(toLong(stats.get("totalBalance"))));
            vo.setTotalPrincipalBalance(formatFenToYuan(toLong(stats.get("totalPrincipalBalance"))));
            vo.setTotalBonusBalance(formatFenToYuan(toLong(stats.get("totalBonusBalance"))));
            vo.setRechargeThisMonth(formatFenToYuan(toLong(stats.get("rechargeThisMonth"))));
            vo.setBonusThisMonth(formatFenToYuan(toLong(stats.get("bonusThisMonth"))));
            vo.setRefundThisMonth(formatFenToYuan(toLong(stats.get("refundThisMonth"))));
            vo.setRechargeCountThisMonth(toInt(stats.get("rechargeCountThisMonth")));
            vo.setRechargeCountToday(toInt(stats.get("rechargeCountToday")));
            vo.setRechargeAmountToday(formatFenToYuan(toLong(stats.get("rechargeAmountToday"))));
            // 平均充值金额 = 本月充值总额 / 本月充值笔数
            int countThisMonth = toInt(stats.get("rechargeCountThisMonth"));
            long rechargeThisMonthFen = toLong(stats.get("rechargeThisMonth"));
            if (countThisMonth > 0) {
                vo.setAvgRechargeAmount(formatFenToYuan(rechargeThisMonthFen / countThisMonth));
            } else {
                vo.setAvgRechargeAmount("0.00");
            }
        }

        // 2. 充值方案使用分布
        List<Map<String, Object>> distribution = rechargeRecordMapper.selectPlanUsageDistribution();
        if (distribution != null) {
            List<Map<String, Object>> formatted = new ArrayList<>();
            for (Map<String, Object> item : distribution) {
                // totalAmount 是分，需要转换为元的字符串
                item.put("totalAmount", formatFenToYuan(toLong(item.get("totalAmount"))));
                formatted.add(item);
            }
            vo.setPlanUsageDistribution(formatted);
        }

        // 3. 近7天充值趋势
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        List<Map<String, Object>> weeklyTrend = rechargeRecordMapper.selectWeeklyTrend(weekAgo);
        if (weeklyTrend != null) {
            List<Map<String, Object>> formatted = new ArrayList<>();
            for (Map<String, Object> item : weeklyTrend) {
                // amount 是分，转换为元；date 格式化为 MM-dd
                Object dateObj = item.get("rechargeDate");
                if (dateObj != null) {
                    String dateStr = dateObj.toString();
                    // 兼容不同数据库返回的日期格式：截取月-日部分
                    if (dateStr.length() >= 10) {
                        item.put("date", dateStr.substring(5, 10));
                    } else {
                        item.put("date", dateStr);
                    }
                    item.remove("rechargeDate");
                }
                item.put("amount", formatFenToYuan(toLong(item.get("amount"))));
                formatted.add(item);
            }
            vo.setWeeklyTrend(formatted);
        }

        // 4. 即将过期赠送统计（30天内）
        LocalDateTime expireThreshold = LocalDateTime.now().plusDays(EXPIRING_BONUS_DAYS);
        Map<String, Object> expiringStats = rechargeRecordMapper.selectExpiringBonusStats(expireThreshold);
        if (expiringStats != null) {
            vo.setExpiringBonusAmount(formatFenToYuan(toLong(expiringStats.get("expiringBonusAmount"))));
            vo.setExpiringBonusCount(toInt(expiringStats.get("expiringBonusCount")));
        }

        // 5. 低余额会员数
        Long lowBalanceCount = rechargeRecordMapper.selectLowBalanceMemberCount(LOW_BALANCE_THRESHOLD);
        vo.setLowBalanceCount(lowBalanceCount != null ? lowBalanceCount.intValue() : 0);

        return vo;
    }

    // ==================== 辅助方法 ====================

    /** 安全转换 Map 值为 long */
    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /** 安全转换 Map 值为 int */
    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 分（Long）转元（字符串，保留两位小数） */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }
}
