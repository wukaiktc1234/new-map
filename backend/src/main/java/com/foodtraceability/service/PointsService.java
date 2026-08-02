package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.dto.marketing.PointsChangeDTO;
import com.foodtraceability.entity.MemberPointsLog;
import com.foodtraceability.entity.MarketingMember;

import java.util.List;
import java.util.Map;

/**
 * 积分服务接口
 * 灵活可配置的积分规则引擎，支持多种获取/使用场景
 */
public interface PointsService extends IService<MemberPointsLog> {

    /**
     * 消费获得积分（核心方法）
     * 根据消费金额、会员等级、消费场景计算应得积分
     *
     * @param memberId 会员ID
     * @param consumeAmount 消费金额（分）
     * @param scene 消费场景：dine_in-堂食, takeout-外卖, delivery-配送
     * @param orderNo 关联订单号
     * @return 实际获得的积分数
     */
    int earnPointsFromConsume(Long memberId, Long consumeAmount, String scene, String orderNo);

    /**
     * 签到奖励积分
     * @param memberId 会员ID
     * @return 获得的积分数
     */
    int earnPointsFromCheckin(Long memberId);

    /**
     * 活动赠送积分
     * @param memberId 会员ID
     * @param points 赠送积分数
     * @param activityCode 活动编码
     * @return 变动后的积分余额
     */
    int grantActivityPoints(Long memberId, int points, String activityCode);

    /**
     * 推荐好友奖励积分
     * @param referrerId 推荐人ID
     * @return 获得的积分数
     */
    int earnReferralBonus(Long referrerId);

    /**
     * 使用积分抵扣
     * 检查余额、计算最大可抵扣、执行扣减
     *
     * @param memberId 会员ID
     * @param usePoints 要使用的积分数
     * @param orderAmount 订单金额（分）
     * @param orderNo 关联订单号
     * @return 实际抵扣的金额（分）
     */
    long usePointsForDeduct(Long memberId, int usePoints, long orderAmount, String orderNo);

    /**
     * 管理员手动调整积分
     * @param changeDTO 积分变动请求
     * @return 变动后的积分余额
     */
    int adjustPoints(PointsChangeDTO changeDTO);

    /**
     * 清理过期积分（定时任务调用）
     * 将过期积分清零并记录日志
     *
     * @return 清理的记录数
     */
    int expirePoints();

    /**
     * 查询会员积分明细
     * @param memberId 会员ID
     * @param current 页码
     * @param size 每页大小
     * @return 积分变动记录列表
     */
    List<MemberPointsLog> getPointsLog(Long memberId, int current, int size);

    /**
     * 获取当前积分配置
     * @return 配置Map
     */
    Map<String, String> getPointsConfig();

    /**
     * 更新积分配置
     * @param ruleKey 规则键名
     * @param ruleValue 新值
     */
    void updateRuleConfig(String ruleKey, String ruleValue);

    /**
     * 计算最大可用抵扣积分
     * 根据订单金额和配置的最大抵扣比例计算
     *
     * @param memberId 会员ID
     * @param orderAmount 订单金额（分）
     * @return 最大可使用积分数
     */
    int calculateMaxDeductiblePoints(Long memberId, long orderAmount);
}
