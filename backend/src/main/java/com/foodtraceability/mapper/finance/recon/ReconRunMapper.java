package com.foodtraceability.mapper.finance.recon;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.recon.ReconRun;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/**
 * 对账批次 Mapper（recon_runs，设计 §5.4）。
 *
 * <p>P0 仅提供基础 CRUD 能力（结构就位）；勾稽/写账逻辑属 P1/P2，不在 P0 范围。</p>
 */
@Mapper
public interface ReconRunMapper extends BaseMapper<ReconRun> {

    /**
     * 条件更新「抢占」一条 run 用于原地重跑（CAS）。
     *
     * <p>为什么带 expectedStartedAt：僵死 RUNNING 的 expectedStatus 与新状态同为 'RUNNING'，
     * 只用 status 做条件时两个并发请求会**同时命中**（实测会双跑并撞 uk_recon_items_subject 500）。
     * 加上 started_at 精确匹配后，第一个请求改写 started_at，第二个请求条件不再成立 → 恰好一个跑批。</p>
     *
     * @return 1 = 抢占成功；0 = 已被其他请求抢走
     */
    @Update("UPDATE recon_runs SET status = #{status}, started_at = NOW(), finished_at = NULL, update_time = NOW() "
            + "WHERE run_id = #{runId} AND status = #{expectedStatus} "
            + "AND COALESCE(started_at, TIMESTAMP '1970-01-01 00:00:00') = COALESCE(#{expectedStartedAt}, TIMESTAMP '1970-01-01 00:00:00') "
           )
    int claimForRebuild(@Param("runId") Long runId,
                        @Param("status") String status,
                        @Param("expectedStatus") String expectedStatus,
                        @Param("expectedStartedAt") LocalDateTime expectedStartedAt);
}
