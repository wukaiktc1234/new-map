package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.dto.marketing.MarketingStatisticsVO;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.mapper.MarketingMemberMapper;
import com.foodtraceability.mapper.MemberPointsLogMapper;
import com.foodtraceability.mapper.MarketingPromotionMapper;
import com.foodtraceability.mapper.RechargeRecordBaseMapper;
import com.foodtraceability.service.MarketingAnalysisService;
import com.foodtraceability.service.MarketingMemberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 营销分析服务实现
 * RFM模型、客户分群、流失预警、营销效果分析
 */
@Service
public class MarketingAnalysisServiceImpl implements MarketingAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingAnalysisServiceImpl.class);

    private final MarketingMemberMapper memberMapper;
    private final MarketingMemberService memberService;
    private final MemberPointsLogMapper pointsLogMapper;
    private final RechargeRecordBaseMapper rechargeRecordMapper;
    private final MarketingPromotionMapper promotionMapper;

    public MarketingAnalysisServiceImpl(MarketingMemberMapper memberMapper,
                                        MarketingMemberService memberService,
                                        MemberPointsLogMapper pointsLogMapper,
                                        RechargeRecordBaseMapper rechargeRecordMapper,
                                        MarketingPromotionMapper promotionMapper) {
        this.memberMapper = memberMapper;
        this.memberService = memberService;
        this.pointsLogMapper = pointsLogMapper;
        this.rechargeRecordMapper = rechargeRecordMapper;
        this.promotionMapper = promotionMapper;
    }

    @Override
    public Map<String, Object> calculateRFM() {
        long startTime = System.currentTimeMillis();
        Map<String, Object> result = new HashMap<>();

        // 1. 获取所有正常状态的会员
        LambdaQueryWrapper<MarketingMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMember::getStatus, 1);
        List<MarketingMember> allMembers = memberService.list(wrapper);

        int totalProcessed = 0;
        Map<String, Integer> segmentCount = new HashMap<>();
        segmentCount.put("VIP_VALUE", 0);
        segmentCount.put("VIP_DEVELOP", 0);
        segmentCount.put("VIP_KEEP", 0);
        segmentCount.put("GENERAL", 0);
        segmentCount.put("CHURN_RISK", 0);

        for (MarketingMember member : allMembers) {
            try {
                // 计算R/F/M得分
                RFMScore score = calculateMemberScore(member);

                // 更新数据库
                memberMapper.updateRfmScore(
                        member.getMemberId(),
                        score.rScore,
                        score.fScore,
                        score.mScore,
                        score.segment
                );

                // 统计分层
                String seg = score.segment;
                if (segmentCount.containsKey(seg)) {
                    segmentCount.put(seg, segmentCount.get(seg) + 1);
                }
                totalProcessed++;
            } catch (Exception e) {
                logger.warn("计算RFM失败: memberId={}, error={}", member.getMemberId(), e.getMessage());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        result.put("totalMembers", allMembers.size());
        result.put("processedCount", totalProcessed);
        result.put("distribution", segmentCount);
        result.put("executionTimeMs", executionTime);
        result.put("status", 1);

        logger.info("RFM计算完成: 处理{}个会员, 耗时{}ms", totalProcessed, executionTime);
        return result;
    }

    @Override
    public MarketingStatisticsVO getStatisticsOverview() {
        MarketingStatisticsVO vo = new MarketingStatisticsVO();

        // 会员统计
        vo.setTotalMembers(memberService.countTotal());
        vo.setTodayNewMembers(memberService.countTodayNew());
        long activeCount = memberMapper.countActiveMembers(30);
        vo.setActiveMembers(activeCount);
        long churnCount = memberMapper.countChurnRiskMembers(30);
        vo.setChurnRiskMembers(churnCount);

        // 积分统计（本月）
        String monthStart = LocalDate.now().withDayOfMonth(1).toString();
        String monthEnd = LocalDate.now().plusMonths(1).withDayOfMonth(1).toString();
        vo.setMonthPointsIssued(pointsLogMapper.sumPointsIssued(monthStart, monthEnd));
        vo.setMonthRechargeAmount(rechargeRecordMapper.sumRechargeAmount(monthStart, monthEnd));
        vo.setTodayRechargeAmount(rechargeRecordMapper.sumTodayRecharge());

        // 促销活动统计
        Map<String, Object> promoStats = promotionMapper.selectEffectSummary();
        if (promoStats != null) {
            vo.setActivePromotions(promoStats.get("activityCount") != null ?
                    ((Number) promoStats.get("activityCount")).longValue() : 0L);
        }

        // RFM分布
        vo.setRfmDistribution(getRFMDistribution());

        return vo;
    }

    @Override
    public List<Map<String, Object>> getChurnRiskMembers(int days, int limit) {
        // 查询N天未消费的会员
        LocalDateTime threshold = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<MarketingMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMember::getStatus, 1)
               .and(w -> w.le(MarketingMember::getLastVisitTime, threshold)
                         .or().isNull(MarketingMember::getLastVisitTime))
               .orderByAsc(MarketingMember::getLastVisitTime)
               .last("LIMIT " + limit);

        return memberService.list(wrapper).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", m.getMemberId());
            map.put("phone", m.getPhone());
            map.put("nickname", m.getNickname());
            map.put("lastVisitTime", m.getLastVisitTime());
            map.put("totalConsume", m.getTotalConsume());
            map.put("orderCount", m.getOrderCount());
            map.put("daysSinceLastVisit", m.getLastVisitTime() != null ?
                    ChronoUnit.DAYS.between(m.getLastVisitTime(), LocalDateTime.now()) : 999);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getHighValueMembers(int limit) {
        // 查询高价值客户（VIP_VALUE或消费金额高的）
        LambdaQueryWrapper<MarketingMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMember::getStatus, 1)
               .and(w -> w.eq(MarketingMember::getCustomerSegment, "VIP_VALUE")
                         .or().ge(MarketingMember::getTotalConsume, 100000)) // 累计消费>=1000元
               .orderByDesc(MarketingMember::getTotalConsume)
               .last("LIMIT " + limit);

        return memberService.list(wrapper).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", m.getMemberId());
            map.put("nickname", m.getNickname());
            map.put("phone", desensitizePhone(m.getPhone()));
            map.put("levelName", m.getLevelName());
            map.put("totalConsume", m.getTotalConsume());
            map.put("totalConsumeDisplay", formatFenToYuan(m.getTotalConsume()));
            map.put("orderCount", m.getOrderCount());
            map.put("points", m.getPoints());
            map.put("customerSegment", m.getCustomerSegment());
            map.put("lastOrderTime", m.getLastOrderTime());
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Long> getRFMDistribution() {
        List<Map<String, Object>> raw = memberMapper.selectRfmDistribution();
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> item : raw) {
            String segment = (String) item.get("customer_segment");
            Long count = ((Number) item.get("count")).longValue();
            result.put(segment != null ? segment : "UNKNOWN", count);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getCLVRanking(int limit) {
        // CLV = 客户生命周期价值，简化为累计消费金额排名
        LambdaQueryWrapper<MarketingMember> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MarketingMember::getStatus, 1)
               .isNotNull(MarketingMember::getTotalConsume)
               .orderByDesc(MarketingMember::getTotalConsume)
               .last("LIMIT " + limit);

        return memberService.list(wrapper).stream().map(m -> {
            Map<String, Object> map = new HashMap<>();
            map.put("memberId", m.getMemberId());
            map.put("nickname", m.getNickname());
            map.put("clvValue", m.getTotalConsume()); // CLV近似值
            map.put("clvDisplay", formatFenToYuan(m.getTotalConsume()));
            map.put("orderCount", m.getOrderCount());
            map.put("registerDate", m.getCreateTime() != null ?
                    m.getCreateTime().toLocalDate().toString() : null);
            return map;
        }).collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getTrendData(int days) {
        Map<String, Object> trend = new HashMap<>();

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 新增会员趋势
        List<Map<String, Object>> newMembersTrend = memberMapper.selectDailyNewMembers(
                startDate.toString(), endDate.plusDays(1).toString()
        );
        trend.put("newMembers", newMembersTrend);

        // 充值趋势
        List<Map<String, Object>> rechargeTrend = rechargeRecordMapper.selectDailyRechargeStats(
                startDate.toString(), endDate.plusDays(1).toString()
        );
        trend.put("recharge", rechargeTrend);

        return trend;
    }

    @Override
    public int processChurnWarning(int days) {
        List<Map<String, Object>> atRiskMembers = getChurnRiskMembers(days, 50);
        int processed = 0;

        for (Map<String, Object> member : atRiskMembers) {
            try {
                Long memberId = ((Number) member.get("memberId")).longValue();

                // TODO: 发送流失预警消息/优惠券
                // 可以调用消息服务发送提醒

                // 更新标签（添加"流失风险"标签）
                MarketingMember m = memberService.getById(memberId);
                if (m != null && m.getStatus() == 1) {
                    List<String> tags = m.getTags() != null ? new ArrayList<>(m.getTags()) : new ArrayList<>();
                    if (!tags.contains("CHURN_RISK")) {
                        tags.add("CHURN_RISK");
                        memberService.updateTags(memberId, tags);
                        processed++;
                    }
                }
            } catch (Exception e) {
                logger.error("处理流失预警失败: memberId={}", member.get("memberId"), e);
            }
        }

        logger.info("流失预警处理完成: 处理{}个会员", processed);
        return processed;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算单个会员的RFM得分和客户分层
     */
    private RFMScore calculateMemberScore(MarketingMember member) {
        int rScore = calculateRScore(member.getLastVisitTime(), member.getLastOrderTime());
        int fScore = calculateFScore(member.getOrderCount(), member.getCreateTime());
        int mScore = calculateMScore(member.getTotalConsume());

        String segment = determineSegment(rScore, fScore, mScore);

        return new RFMScore(rScore, fScore, mScore, segment);
    }

    /** R得分：最近消费时间 */
    private int calculateRScore(LocalDateTime lastVisit, LocalDateTime lastOrder) {
        LocalDateTime lastActivity = lastVisit != null ? lastVisit :
                (lastOrder != null ? lastOrder : null);
        if (lastActivity == null) return 1; // 从未消费

        long daysAgo = ChronoUnit.DAYS.between(lastActivity, LocalDateTime.now());
        if (daysAgo <= 7) return 5;      // 7天内
        if (daysAgo <= 30) return 4;     // 8-30天
        if (daysAgo <= 90) return 3;     // 31-90天
        if (daysAgo <= 180) return 2;    // 91-180天
        return 1;                       // 180天以上
    }

    /** F得分：消费频率（月均订单数） */
    private int calculateFScore(Integer orderCount, LocalDateTime registerTime) {
        if (orderCount == null || orderCount == 0) return 1;
        long monthsSinceRegister = ChronoUnit.MONTHS.between(
                registerTime.toLocalDate(), LocalDate.now()
        );
        if (monthsSinceRegister < 1) monthsSinceRegister = 1;

        double monthlyAvg = (double) orderCount / monthsSinceRegister;
        if (monthlyAvg >= 8) return 5;
        if (monthlyAvg >= 5) return 4;
        if (monthlyAvg >= 3) return 3;
        if (monthlyAvg >= 1) return 2;
        return 1;
    }

    /** M得分：消费金额（月均消费） */
    private int calculateMScore(Long totalConsume) {
        if (totalConsume == null || totalConsume == 0) return 1;
        // 假设平均会员生命周期为12个月，月均消费=总消费/12
        long monthlyAvg = totalConsume / 12; // 分转分
        long monthlyAvgYuan = monthlyAvg / 100; // 分转元

        if (monthlyAvgYuan >= 2000) return 5;
        if (monthlyAvgYuan >= 1000) return 4;
        if (monthlyAvgYuan >= 500) return 3;
        if (monthlyAvgYuan >= 200) return 2;
        return 1;
    }

    /** 根据RFM得分确定客户分层 */
    private String determineSegment(int r, int f, int m) {
        boolean highR = r >= 4;
        boolean highF = f >= 4;
        boolean highM = m >= 4;

        if (highR && highF && highM) return "VIP_VALUE";       // 重要价值客户
        if (highR && !highF && highM) return "VIP_DEVELOP";    // 重要发展客户
        if (!highR && highF && highM) return "VIP_KEEP";       // 重要保持客户
        if (!highR && r <= 2) return "CHURN_RISK";             // 流失风险
        return "GENERAL";                                        // 一般客户
    }

    /** 解析标签JSON */
    private List<String> parseTags(String tagsJson) {
        try {
            if (tagsJson == null || tagsJson.isEmpty()) return new ArrayList<>();
            // 简化处理：实际应使用ObjectMapper解析JSON数组
            return Arrays.asList(tagsJson.replace("[", "").replace("]", "")
                    .replace("\"", "").split(","));
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String desensitizePhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }

    /** RFM评分内部类 */
    private static class RFMScore {
        int rScore;
        int fScore;
        int mScore;
        String segment;

        RFMScore(int rScore, int fScore, int mScore, String segment) {
            this.rScore = rScore;
            this.fScore = fScore;
            this.mScore = mScore;
            this.segment = segment;
        }
    }
}
