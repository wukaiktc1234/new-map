package com.foodtraceability.mapper.marketing;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.marketing.BonusBalanceDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

/**
 * 赠送余额明细 Mapper
 */
@Mapper
@Repository
public interface BonusBalanceDetailMapper extends BaseMapper<BonusBalanceDetail> {

    /**
     * 查询会员当前有效赠送余额总和（分）
     * 用于财务流水记录变动后的赠送余额
     *
     * @param memberId 会员ID（字符串形式）
     * @return 有效赠送余额总和，无记录返回 0
     */
    @Select("SELECT COALESCE(SUM(remaining_amount), 0) FROM bonus_balance_details " +
            "WHERE member_id = #{memberId} AND status = 'active' AND deleted = 0")
    Long sumActiveBonusRemaining(@Param("memberId") String memberId);
}
