package com.foodtraceability.service.finance.recon.impl;

import com.foodtraceability.entity.finance.recon.ReconRun;
import com.foodtraceability.mapper.finance.recon.ReconRunMapper;
import com.foodtraceability.service.finance.recon.ReconDictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 跑批状态的事务支撑（独立事务 REQUIRES_NEW）。
 *
 * <p>为什么单独成类：run 行必须在跑批事务**之外**先提交（RUNNING），跑批失败回滚后仍能
 * 用独立事务把它改成 FAILED —— 否则「失败回滚」会把 FAILED 标记一起回滚掉。</p>
 */
@Service
public class ReconRunTxSupport {

    private static final Logger log = LoggerFactory.getLogger(ReconRunTxSupport.class);

    private final ReconRunMapper reconRunMapper;

    public ReconRunTxSupport(ReconRunMapper reconRunMapper) {
        this.reconRunMapper = reconRunMapper;
    }

    /** 独立事务插入 RUNNING 状态的 run（run_key 冲突时由调用方转为幂等命中） */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void insertRunning(ReconRun run) {
        reconRunMapper.insert(run);
    }

    /**
     * 独立事务「抢占」一条僵死/失败的 run 用于原地重跑（条件更新，CAS 语义）。
     *
     * <p>为什么必须这么做：两个请求同时判定 RUNNING 超时后都会去 rebuild，若各自 INSERT 新 run，
     * 一个成功、另一个撞 uk_recon_runs_run_key —— 唯一约束只保证「不重复落库」，不保证「只跑一次」。
     * 这里用 UPDATE ... WHERE run_id=? AND status=? 的条件更新抢占：命中 1 行者才继续跑批，
     * 未命中（0 行）说明已被别的请求抢走 → 调用方抛 400「已被其他请求重建」。</p>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public boolean claimForRebuild(Long runId, String expectedStatus, LocalDateTime expectedStartedAt) {
        int affected = reconRunMapper.claimForRebuild(runId, ReconDictionary.RUN_RUNNING, expectedStatus, expectedStartedAt);
        log.info("[METRIC] recon_run_claim runId={} expectedStatus={} expectedStartedAt={} claimed={}",
                runId, expectedStatus, expectedStartedAt, affected == 1);
        return affected == 1;
    }

    /** 独立事务把 run 置 FAILED（跑批事务已回滚，这里必须能独立落库） */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markFailed(Long runId, String reason) {
        ReconRun patch = new ReconRun();
        patch.setRunId(runId);
        patch.setStatus(ReconDictionary.RUN_FAILED);
        patch.setFinishedAt(LocalDateTime.now());
        patch.setUpdateTime(LocalDateTime.now());
        reconRunMapper.updateById(patch);
        log.error("[METRIC] recon_run_failed runId={} reason={}", runId, reason);
    }
}
