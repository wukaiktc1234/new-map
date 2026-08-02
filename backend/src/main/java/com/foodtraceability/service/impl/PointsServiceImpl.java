package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.dto.marketing.PointsChangeDTO;
import com.foodtraceability.entity.MemberLevel;
import com.foodtraceability.entity.MemberPointsLog;
import com.foodtraceability.entity.MarketingMember;
import com.foodtraceability.entity.PointsRuleConfig;
import com.foodtraceability.mapper.MemberPointsLogMapper;
import com.foodtraceability.mapper.MarketingMemberMapper;
import com.foodtraceability.mapper.PointsRuleConfigMapper;
import com.foodtraceability.service.MemberLevelService;
import com.foodtraceability.service.MarketingMemberService;
import com.foodtraceability.service.PointsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分规则引擎服务实现
 * 灵活可配置的积分获取/使用/过期管理
 */
@Service
public class PointsServiceImpl extends ServiceImpl<MemberPointsLogMapper, MemberPointsLog> implements PointsService {

    private static final Logger logger = LoggerFactory.getLogger(PointsServiceImpl.class);

    private final MemberPointsLogMapper pointsLogMapper;
    private final MarketingMemberMapper memberMapper;
    private final MarketingMemberService memberService;
    private final MemberLevelService memberLevelService;
    private final PointsRuleConfigMapper ruleConfigMapper;

    public PointsServiceImpl(MemberPointsLogMapper pointsLogMapper,
                             MarketingMemberMapper memberMapper,
                             MarketingMemberService memberService,
                             MemberLevelService memberLevelService,
                             PointsRuleConfigMapper ruleConfigMapper) {
        this.pointsLogMapper = pointsLogMapper;
        this.memberMapper = memberMapper;
        this.memberService = memberService;
        this.memberLevelService = memberLevelService;
        this.ruleConfigMapper = ruleConfigMapper;
    }

    /** 规则缓存（避免频繁查库） */
    private volatile Map<String, String> ruleCache = null;
    private volatile long cacheTime = 0;
    private static final long CACHE_TTL = 60_000; // 缓存1分钟

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnPointsFromConsume(Long memberId, Long consumeAmount, String scene, String orderNo) {
        MarketingMember member = memberService.getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }
        if (member.getStatus() != 1) {
            throw new RuntimeException("会员状态异常，无法获得积分");
        }

        // 1. 获取基础规则配置
        double baseRate = getRuleValueAsDouble("base_points_rate", 1.0);

        // 2. 根据消费场景调整倍率
        double sceneRate = getSceneRate(scene);
        double effectiveRate = baseRate * sceneRate;

        // 3. 获取会员等级加成
        MemberLevel level = null;
        if (member.getMemberLevelId() != null) {
            level = memberLevelService.getById(member.getMemberLevelId());
        }
        double levelMultiplier = (level != null && level.getPointsRate() != null) ?
                level.getPointsRate().doubleValue() : 1.0;

        // 4. 计算最终积分（消费金额分转元，乘以倍率）
        // consumeAmount是分，需要转为元计算积分：元 * 倍率 = 积分
        BigDecimal amountYuan = BigDecimal.valueOf(consumeAmount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        int earnedPoints = amountYuan.multiply(
                BigDecimal.valueOf(effectiveRate).multiply(BigDecimal.valueOf(levelMultiplier))
        ).setScale(0, RoundingMode.HALF_UP).intValue();

        // 最少给1积分（如果消费金额大于0）
        if (earnedPoints <= 0 && consumeAmount > 0) {
            earnedPoints = 1;
        }

        // 5. 执行积分入账
        return doAddPoints(memberId, earnedPoints, 1, orderNo, 1,
                "消费获得积分: " + scene + ", 消费金额=" + formatFenToYuan(consumeAmount) + "元");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnPointsFromCheckin(Long memberId) {
        int checkinPoints = getRuleValueAsInt("checkin_points", 5);
        int maxDaily = getRuleValueAsInt("daily_checkin_max", 1);

        // TODO: 可以添加每日签到次数检查逻辑

        return doAddPoints(memberId, checkinPoints, 3, "CK" + System.currentTimeMillis(), 3,
                "签到奖励积分");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int grantActivityPoints(Long memberId, int points, String activityCode) {
        return doAddPoints(memberId, points, 4, activityCode, 2,
                "活动赠送积分: " + activityCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnReferralBonus(Long referrerId) {
        int bonusPoints = getRuleValueAsInt("referral_bonus", 200);
        return doAddPoints(referrerId, bonusPoints, 8, "REF" + System.currentTimeMillis(), 6,
                "推荐好友奖励积分");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long usePointsForDeduct(Long memberId, int usePoints, long orderAmount, String orderNo) {
        MarketingMember member = memberService.getById(memberId);
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }
        if (member.getStatus() != 1) {
            throw new RuntimeException("会员状态异常");
        }

        // 1. 检查积分余额是否足够
        if (member.getPoints() < usePoints) {
            throw new RuntimeException("积分余额不足，当前余额: " + member.getPoints());
        }

        // 2. 计算最大可抵扣比例和金额
        int maxDeductPercent = getRuleValueAsInt("points_deduct_max_percent", 30);
        long maxDeductByPercent = orderAmount * maxDeductPercent / 100;

        // 3. 获取汇率：多少积分=1元（分）
        int exchangeRate = getRuleValueAsInt("points_exchange_rate", 100);
        long maxDeductByPoints = (long) usePoints * 100 / exchangeRate; // 积分能抵扣的最大金额（分）

        // 4. 取最小值作为实际抵扣金额
        long actualDeduct = Math.min(maxDeductByPercent, maxDeductByPoints);
        int actualPointsUsed = (int) (actualDeduct * exchangeRate / 100); // 实际消耗的积分

        // 5. 执行积分扣减
        doDeductPoints(memberId, actualPointsUsed, orderNo, 1,
                "订单抵扣使用积分, 抵扣金额=" + formatFenToYuan(actualDeduct) + "元");

        logger.info("积分抵扣成功: memberId={}, usePoints={}, deductAmount={}分",
                memberId, actualPointsUsed, actualDeduct);

        return actualDeduct;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int adjustPoints(PointsChangeDTO changeDTO) {
        MarketingMember member = memberService.getById(changeDTO.getMemberId());
        if (member == null) {
            throw new RuntimeException("会员不存在");
        }

        int changePoints = changeDTO.getChangePoints();
        if (changePoints > 0) {
            return doAddPoints(changeDTO.getMemberId(), changePoints,
                    changeDTO.getChangeType(),
                    changeDTO.getReferenceNo() != null ? changeDTO.getReferenceNo() : "ADJ" + System.currentTimeMillis(),
                    changeDTO.getReferenceType() != null ? changeDTO.getReferenceType() : 4,
                    changeDTO.getRemark() != null ? changeDTO.getRemark() : "管理员调整积分",
                    changeDTO.getOperatorId(),
                    changeDTO.getOperatorName());
        } else if (changePoints < 0) {
            return doDeductPoints(changeDTO.getMemberId(), Math.abs(changePoints),
                    changeDTO.getReferenceNo() != null ? changeDTO.getReferenceNo() : "ADJ" + System.currentTimeMillis(),
                    changeDTO.getReferenceType() != null ? changeDTO.getReferenceType() : 4,
                    changeDTO.getRemark() != null ? changeDTO.getRemark() : "管理员扣减积分",
                    changeDTO.getOperatorId(),
                    changeDTO.getOperatorName());
        } else {
            return member.getPoints();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expirePoints() {
        // 查询即将过期的积分记录（默认提前30天预警后执行清理）
        int validMonths = getRuleValueAsInt("points_valid_months", 12);
        LocalDateTime expireTarget = LocalDateTime.now().plusMonths(validMonths);

        List<MemberPointsLog> expiringLogs = pointsLogMapper.selectExpiringPoints(
                expireTarget.toString().replace("T", " ")
        );

        int expiredCount = 0;
        for (MemberPointsLog log : expiringLogs) {
            try {
                // 执行过期清零（只扣减正数积分）
                if (log.getChangePoints() > 0) {
                    memberMapper.deductPoints(log.getMemberId(), log.getChangePoints());

                    // 记录过期日志
                    MemberPointsLog expireLog = new MemberPointsLog();
                    expireLog.setMemberId(log.getMemberId());
                    expireLog.setChangeType(6); // 过期清零
                    expireLog.setChangePoints(-log.getChangePoints()); // 负数表示减少
                    expireLog.setBalanceAfter(Math.max(0, log.getBalanceAfter() - log.getChangePoints()));
                    expireLog.setReferenceNo("EXP" + log.getLogId());
                    expireLog.setReferenceType(0);
                    expireLog.setRemark("积分过期自动清零, 原始logId=" + log.getLogId());
                    save(expireLog);
                    expiredCount++;
                }
            } catch (Exception e) {
                logger.error("处理过期积分失败: logId={}, error={}", log.getLogId(), e.getMessage());
            }
        }

        logger.info("积分过期清理完成: 处理{}条记录", expiredCount);
        return expiredCount;
    }

    @Override
    public List<MemberPointsLog> getPointsLog(Long memberId, int current, int size) {
        int offset = (current - 1) * size;
        return pointsLogMapper.selectByMemberId(memberId, offset, size);
    }

    @Override
    public Map<String, String> getPointsConfig() {
        refreshCacheIfNeeded();
        return new HashMap<>(ruleCache);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRuleConfig(String ruleKey, String ruleValue) {
        PointsRuleConfig config = ruleConfigMapper.selectByKey(ruleKey);
        if (config == null) {
            throw new RuntimeException("规则不存在: " + ruleKey);
        }
        config.setRuleValue(ruleValue);
        ruleConfigMapper.updateById(config);
        clearCache(); // 清除缓存使下次查询重新加载
    }

    @Override
    public int calculateMaxDeductiblePoints(Long memberId, long orderAmount) {
        MarketingMember member = memberService.getById(memberId);
        if (member == null || member.getPoints() == null) {
            return 0;
        }

        // 最大抵扣比例
        int maxDeductPercent = getRuleValueAsInt("points_deduct_max_percent", 30);
        long maxDeductAmount = orderAmount * maxDeductPercent / 100;

        // 积分汇率
        int exchangeRate = getRuleValueAsInt("points_exchange_rate", 100);

        // 按金额算最多能用多少积分
        int maxByAmount = (int) (maxDeductAmount * exchangeRate / 100);

        // 取会员可用积分和按金额限制的较小值
        return Math.min(member.getPoints(), maxByAmount);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 执行积分增加操作
     */
    private int doAddPoints(Long memberId, int points, int changeType, String referenceNo,
                            int referenceType, String remark) {
        return doAddPoints(memberId, points, changeType, referenceNo, referenceType, remark, null, null);
    }

    /**
     * 执行积分增加操作（带操作人信息）
     */
    private int doAddPoints(Long memberId, int points, int changeType, String referenceNo,
                            int referenceType, String remark, Long operatorId, String operatorName) {
        // 1. 更新会员积分
        int affected = memberMapper.addPoints(memberId, points);
        if (affected == 0) {
            throw new RuntimeException("更新会员积分失败");
        }

        // 2. 获取更新后的余额
        MarketingMember updated = memberService.getById(memberId);
        int balanceAfter = updated != null ? updated.getPoints() : 0;

        // 3. 计算过期时间
        int validMonths = getRuleValueAsInt("points_valid_months", 12);
        LocalDateTime expireTime = LocalDateTime.now().plusMonths(validMonths);

        // 4. 记录变动日志
        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(memberId);
        log.setChangeType(changeType);
        log.setChangePoints(points);
        log.setBalanceAfter(balanceAfter);
        log.setReferenceNo(referenceNo);
        log.setReferenceType(referenceType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        log.setExpireTime(expireTime);
        save(log);

        // 5. 检查并触发等级升级
        memberService.checkAndUpgradeLevel(memberId);

        return balanceAfter;
    }

    /**
     * 执行积分扣减操作
     */
    private int doDeductPoints(Long memberId, int points, String referenceNo,
                               int referenceType, String remark) {
        return doDeductPoints(memberId, points, referenceNo, referenceType, remark, null, null);
    }

    /**
     * 执行积分扣减操作（带操作人信息）
     */
    private int doDeductPoints(Long memberId, int points, String referenceNo,
                               int referenceType, String remark, Long operatorId, String operatorName) {
        // 1. 更新会员积分
        int affected = memberMapper.deductPoints(memberId, points);
        if (affected == 0) {
            throw new RuntimeException("积分扣减失败，可能余额不足或会员不存在");
        }

        // 2. 获取更新后的余额
        MarketingMember updated = memberService.getById(memberId);
        int balanceAfter = updated != null ? updated.getPoints() : 0;

        // 3. 记录变动日志
        MemberPointsLog log = new MemberPointsLog();
        log.setMemberId(memberId);
        log.setChangeType(2); // 使用抵扣
        log.setChangePoints(-points); // 负数表示减少
        log.setBalanceAfter(balanceAfter);
        log.setReferenceNo(referenceNo);
        log.setReferenceType(referenceType);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setRemark(remark);
        save(log);

        return balanceAfter;
    }

    /**
     * 获取场景倍率
     */
    private double getSceneRate(String scene) {
        switch (scene) {
            case "dine_in":
                return getRuleValueAsDouble("dine_in_rate", 1.0);
            case "takeout":
                return getRuleValueAsDouble("takeout_rate", 0.8);
            case "delivery":
                return getRuleValueAsDouble("takeout_rate", 0.8); // 配送同外卖
            default:
                return 1.0;
        }
    }

    /**
     * 获取规则值（double类型）
     */
    private double getRuleValueAsDouble(String ruleKey, double defaultValue) {
        String value = getCachedRule(ruleKey);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 获取规则值（int类型）
     */
    private int getRuleValueAsInt(String ruleKey, int defaultValue) {
        String value = getCachedRule(ruleKey);
        if (value == null || value.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * 从缓存获取规则值
     */
    private String getCachedRule(String ruleKey) {
        refreshCacheIfNeeded();
        return ruleCache != null ? ruleCache.get(ruleKey) : null;
    }

    /**
     * 刷新规则缓存
     */
    private void refreshCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (ruleCache == null || (now - cacheTime) > CACHE_TTL) {
            synchronized (this) {
                if (ruleCache == null || (System.currentTimeMillis() - cacheTime) > CACHE_TTL) {
                    List<Map<String, String>> rules = ruleConfigMapper.selectAllEnabledRules();
                    Map<String, String> newCache = new HashMap<>();
                    for (Map<String, String> rule : rules) {
                        if (rule.containsKey("rule_key") && rule.containsKey("rule_value")) {
                            newCache.put(rule.get("rule_key"), rule.get("rule_value"));
                        }
                    }
                    this.ruleCache = newCache;
                    this.cacheTime = now;
                }
            }
        }
    }

    /** 清除缓存 */
    private void clearCache() {
        synchronized (this) {
            this.ruleCache = null;
            this.cacheTime = 0;
        }
    }

    /** 分转元格式化 */
    private String formatFenToYuan(Long fen) {
        if (fen == null) return "0.00";
        return String.format("%.2f", fen / 100.0);
    }
}
