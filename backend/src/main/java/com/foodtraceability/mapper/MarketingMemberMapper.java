package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.MarketingMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Map;

/**
 * 营销CRM会员数据访问层
 */
@Mapper
public interface MarketingMemberMapper extends BaseMapper<MarketingMember> {

    /**
     * 根据手机号查询会员
     */
    @Select("SELECT * FROM members WHERE phone = #{phone} AND deleted = 0")
    MarketingMember findByPhone(@Param("phone") String phone);

    /**
     * 根据会员卡号查询会员
     */
    @Select("SELECT * FROM members WHERE member_no = #{memberNo} AND deleted = 0")
    MarketingMember findByMemberNo(@Param("memberNo") String memberNo);

    /**
     * 更新会员积分（增加）
     */
    @Update("UPDATE members SET points = points + #{points}, " +
            "total_points_earned = COALESCE(total_points_earned, 0) + #{points}, " +
            "update_time = NOW() " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int addPoints(@Param("memberId") Long memberId, @Param("points") Integer points);

    /**
     * 更新会员积分（扣减）
     */
    @Update("UPDATE members SET points = points - #{points}, " +
            "total_points_used = COALESCE(total_points_used, 0) + #{points}, " +
            "update_time = NOW() " +
            "WHERE member_id = #{memberId} AND points >= #{points} AND deleted = 0")
    int deductPoints(@Param("memberId") Long memberId, @Param("points") Integer points);

    /**
     * 更新会员余额（增加，如充值入账）
     */
    @Update("UPDATE members SET balance = balance + #{amount}, " +
            "total_recharge = COALESCE(total_recharge, 0) + #{amount}, " +
            "update_time = NOW() " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int addBalance(@Param("memberId") Long memberId, @Param("amount") Long amount);

    /**
     * 更新会员余额（扣减，如消费抵扣）
     */
    @Update("UPDATE members SET balance = balance - #{amount}, " +
            "total_consume = COALESCE(total_consume, 0) + #{amount}, " +
            "order_count = COALESCE(order_count, 0) + 1, " +
            "last_order_time = NOW(), last_visit_time = NOW(), " +
            "update_time = NOW() " +
            "WHERE member_id = #{memberId} AND balance >= #{amount} AND deleted = 0")
    int deductBalanceForConsume(@Param("memberId") Long memberId, @Param("amount") Long amount);

    /**
     * 更新最后到店时间
     */
    @Update("UPDATE members SET last_visit_time = NOW(), update_time = NOW() " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int updateLastVisitTime(@Param("memberId") Long memberId);

    /**
     * 更新会员等级
     */
    @Update("UPDATE members SET member_level_id = #{levelId}, update_time = NOW() " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int updateMemberLevel(@Param("memberId") Long memberId, @Param("levelId") Long levelId);

    /**
     * 更新RFM评分和客户分层
     */
    @Update("UPDATE members SET r_score = #{rScore}, f_score = #{fScore}, m_score = #{mScore}, " +
            "customer_segment = #{segment}, update_time = NOW() " +
            "WHERE member_id = #{memberId} AND deleted = 0")
    int updateRfmScore(@Param("memberId") Long memberId,
                       @Param("rScore") Integer rScore,
                       @Param("fScore") Integer fScore,
                       @Param("mScore") Integer mScore,
                       @Param("segment") String segment);

    /**
     * 查询流失风险会员（N天未消费）
     * 注意：使用 #{days} * INTERVAL '1 DAY' 而非 INTERVAL '#{days} DAY'，
     * 因为 #{} 在字符串字面量内无法被 MyBatis 正确绑定参数
     */
    @Select("SELECT COUNT(*) FROM members " +
            "WHERE status = 1 AND deleted = 0 " +
            "AND (last_visit_time IS NULL OR last_visit_time < NOW() - #{days} * INTERVAL '1 DAY')")
    long countChurnRiskMembers(@Param("days") int days);

    /**
     * 查询活跃会员数（N天内有消费/到店）
     */
    @Select("SELECT COUNT(*) FROM members " +
            "WHERE status = 1 AND deleted = 0 " +
            "AND last_visit_time >= NOW() - #{days} * INTERVAL '1 DAY'")
    long countActiveMembers(@Param("days") int days);

    /**
     * 查询会员统计信息（按等级分组）
     */
    @Select("SELECT ml.level_name, ml.level_code, COUNT(m.member_id) as count " +
            "FROM member_level ml " +
            "LEFT JOIN members m ON m.member_level_id = ml.level_id AND m.deleted = 0 AND m.status = 1 " +
            "WHERE ml.deleted = 0 AND ml.status = 1 " +
            "GROUP BY ml.level_id, ml.level_name, ml.level_code ORDER BY ml.sort_order")
    List<Map<String, Object>> selectLevelDistribution();

    /**
     * 查询RFM分布统计
     */
    @Select("SELECT customer_segment, COUNT(*) as count FROM members " +
            "WHERE status = 1 AND deleted = 0 AND customer_segment IS NOT NULL " +
            "GROUP BY customer_segment ORDER BY count DESC")
    List<Map<String, Object>> selectRfmDistribution();

    /**
     * 按日期范围查询新增会员趋势
     */
    @Select("SELECT DATE(create_time) as date, COUNT(*) as newMembers " +
            "FROM members WHERE deleted = 0 " +
            "AND create_time >= #{startDate} AND create_time < #{endDate} " +
            "GROUP BY DATE(create_time) ORDER BY date")
    List<Map<String, Object>> selectDailyNewMembers(@Param("startDate") String startDate,
                                                     @Param("endDate") String endDate);
}
