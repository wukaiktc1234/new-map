package com.foodtraceability.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.RecruitmentQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 招聘名额 Mapper 接口
 * 对应表: recruitment_quotas
 */
@Mapper
public interface RecruitmentQuotaMapper extends BaseMapper<RecruitmentQuota> {

    /**
     * 原子更新 used_count（避免并发超卖）。
     *
     * <p>SQL 通过 WHERE used_count < headcount 条件保证：
     * 仅当已用数小于名额总数时才执行 +1 更新，避免并发场景下超卖。</p>
     *
     * <p>同时显式更新 update_time，并附加 deleted = 0 条件确保不更新已删除记录。</p>
     *
     * @param quotaId 名额 ID
     * @return 影响行数（0=更新失败/名额已用完, 1=成功）
     */
    @Update("UPDATE recruitment_quotas " +
            "SET used_count = used_count + 1, update_time = CURRENT_TIMESTAMP " +
            "WHERE quota_id = #{quotaId} " +
            "  AND used_count < headcount " +
            "  AND deleted = 0")
    int incrementUsedCountAtomic(@Param("quotaId") Long quotaId);
}
