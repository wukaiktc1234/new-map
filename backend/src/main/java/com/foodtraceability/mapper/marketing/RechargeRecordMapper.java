package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.RechargeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 充值记录 Mapper
 */
@Mapper
@Repository
public interface RechargeRecordMapper extends BaseMapper<RechargeRecord> {

    /**
     * 储值统计概览（余额、本月、今日统计）
     * @return 统计结果 Map
     */
    Map<String, Object> selectStatsOverview();

    /**
     * 充值方案使用分布
     * @return 方案使用分布列表
     */
    List<Map<String, Object>> selectPlanUsageDistribution();

    /**
     * 近7天充值趋势
     * @param startTime 开始时间
     * @return 趋势列表
     */
    List<Map<String, Object>> selectWeeklyTrend(@Param("startTime") LocalDateTime startTime);

    /**
     * 统计指定会员在时间范围内的充值次数与累计金额
     * @param memberId 会员ID
     * @param startTime 开始时间
     * @return 充值汇总 Map
     */
    Map<String, Object> selectMemberRechargeSummary(@Param("memberId") String memberId,
                                                     @Param("startTime") LocalDateTime startTime);

    /**
     * 即将过期的赠送余额统计（30天内）
     * @param expireThreshold 过期阈值时间
     * @return 含 expiringBonusAmount、expiringBonusCount 的 Map
     */
    Map<String, Object> selectExpiringBonusStats(@Param("expireThreshold") LocalDateTime expireThreshold);

    /**
     * 低余额会员数（余额低于阈值的会员数）
     * @param threshold 余额阈值（分）
     * @return 低余额会员数
     */
    Long selectLowBalanceMemberCount(@Param("threshold") Long threshold);
}
