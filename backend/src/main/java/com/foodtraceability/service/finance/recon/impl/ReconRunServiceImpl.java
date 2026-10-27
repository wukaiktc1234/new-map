package com.foodtraceability.service.finance.recon.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.dto.finance.recon.ReconItemQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconItemVO;
import com.foodtraceability.dto.finance.recon.ReconMatchDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateDTO;
import com.foodtraceability.dto.finance.recon.ReconRunCreateResultVO;
import com.foodtraceability.dto.finance.recon.ReconRunQueryDTO;
import com.foodtraceability.dto.finance.recon.ReconRunVO;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.entity.finance.recon.ReconItem;
import com.foodtraceability.entity.finance.recon.ReconRun;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import com.foodtraceability.mapper.finance.recon.ReconItemMapper;
import com.foodtraceability.mapper.finance.recon.ReconRunMapper;
import com.foodtraceability.service.finance.recon.ReconDictionary;
import com.foodtraceability.service.finance.recon.ReconRunService;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 对账跑批与勾稽主体服务实现（P1-B：R1 / R2 / R3 / R5 / R6 / R7）。
 *
 * <p><b>红线（P1 范围锁）</b>：本类对 fund_flows / bank_accounts / bank_transactions / order_payment_records
 * <b>只做 SELECT</b>；写入只落在 recon_runs / recon_items / recon_differences。
 * 不写 fund_flows、不改 bank_accounts.balance、不冲正、不生成缴款单与内部转账（P2）。</p>
 *
 * <p><b>幂等与 force</b>：同日同账户已有 run 且未 force → 直接返回既有 run（reused=true，不新建）；
 * force=true → 新建 run 并记 rebuild_of=旧 run id，旧 run 及其 items/differences 一律不改（只读）。
 * 由于 {@code uk_recon_runs_run_key} 对 run_key 唯一，首个 run 使用设计口径
 * {@code RECON-<accountId>-<yyyyMMdd>}，重建 run 使用 {@code RECON-<accountId>-<yyyyMMdd>#R<n>} 避免撞键。</p>
 *
 * <p><b>事务边界</b>：run 行先由 {@link ReconRunTxSupport} 在独立事务落 RUNNING；跑批事务在
 * {@link ReconBatchExecutor#execute(Long)}；失败 → 跑批事务整体回滚 → 独立事务把 run 置 FAILED。</p>
 */
@Service
public class ReconRunServiceImpl implements ReconRunService {

    private static final Logger log = LoggerFactory.getLogger(ReconRunServiceImpl.class);

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter RUN_KEY_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** RUNNING 超过该分钟数视为僵死批次，允许重建（防止上次进程崩溃把当日锁死） */
    private static final long RUNNING_STALE_MINUTES = 10;

    /** 允许发起人工勾稽的当前状态（已自动/人工勾稽的不重复勾稽） */
    private static final Set<String> MATCHABLE_ITEM_STATUS =
            Set.of(ReconDictionary.ITEM_UNMATCHED, ReconDictionary.ITEM_MATCHED);

    private final ReconRunMapper reconRunMapper;
    private final ReconItemMapper reconItemMapper;
    private final BankAccountMapper bankAccountMapper;
    private final ReconRunTxSupport reconRunTxSupport;
    private final ReconBatchExecutor reconBatchExecutor;

    public ReconRunServiceImpl(ReconRunMapper reconRunMapper,
                               ReconItemMapper reconItemMapper,
                               BankAccountMapper bankAccountMapper,
                               ReconRunTxSupport reconRunTxSupport,
                               ReconBatchExecutor reconBatchExecutor) {
        this.reconRunMapper = reconRunMapper;
        this.reconItemMapper = reconItemMapper;
        this.bankAccountMapper = bankAccountMapper;
        this.reconRunTxSupport = reconRunTxSupport;
        this.reconBatchExecutor = reconBatchExecutor;
    }

    // ==================== R1 跑批 ====================

    @Override
    public ReconRunCreateResultVO run(ReconRunCreateDTO dto) {
        if (dto == null || dto.getAccountId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "accountId 不能为空");
        }
        LocalDate businessDate = dto.getBusinessDate() != null ? dto.getBusinessDate() : LocalDate.now(ZONE);
        boolean force = Boolean.TRUE.equals(dto.getForce());

        BankAccount account = bankAccountMapper.selectById(dto.getAccountId());
        if (account == null || Integer.valueOf(1).equals(account.getDeleted())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "银行账户不存在或已删除：accountId=" + dto.getAccountId());
        }

        ReconRun existing = findLatestRun(dto.getAccountId(), businessDate);
        // R1 复用规则（Owner 裁定）：只有「FINALIZED 且 items>0」才复用；
        //   FAILED / 超时的 RUNNING → 重建新 run 并原地跑批（绝不 code=0 却不跑批）；
        //   FINALIZED 但 items=0 → 异常遗留，400 提示清理，不复用。
        boolean rebuild = Boolean.TRUE.equals(force);
        if (existing != null) {
            String status = existing.getStatus();
            if (ReconDictionary.RUN_FINALIZED.equals(status)) {
                long items = countItems(existing.getRunId());
                if (!force) {
                    if (items > 0) {
                        log.info("[METRIC] recon_run_reused accountId={} date={} runId={} items={}",
                                dto.getAccountId(), businessDate, existing.getRunId(), items);
                        return new ReconRunCreateResultVO(true, toVO(existing, account.getAccountName()));
                    }
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "该账户当日已有空的 FINALIZED 对账批次（runId=" + existing.getRunId()
                                    + "，items=0），属历史遗留（旧版允许空窗口 FINALIZE，现已被 ReconBatchExecutor 空窗口守卫杜绝），请先清理后再跑批");
                }
                rebuild = true;
            } else if (ReconDictionary.RUN_FAILED.equals(status)) {
                // 原地重跑：先 CAS 抢占（UPDATE ... WHERE run_id=? AND status='FAILED'）
                if (!reconRunTxSupport.claimForRebuild(existing.getRunId(), ReconDictionary.RUN_FAILED, existing.getStartedAt())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "该批次已被其他请求重建（runId=" + existing.getRunId() + "），请刷新后再试");
                }
                log.info("[METRIC] recon_run_rebuild_failed accountId={} date={} oldRunId={}",
                        dto.getAccountId(), businessDate, existing.getRunId());
                return executeInPlace(existing, account);
            } else {
                boolean stale = existing.getStartedAt() == null
                        || existing.getStartedAt().isBefore(LocalDateTime.now().minusMinutes(RUNNING_STALE_MINUTES));
                if (!stale && !force) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "该账户当日已有进行中的对账批次（runId=" + existing.getRunId()
                                    + "，status=" + status + "），请稍后重试或传 force=true 重建");
                }
                // 僵死 RUNNING：同样 CAS 抢占，只有抢到的那一个请求能跑批
                if (!reconRunTxSupport.claimForRebuild(existing.getRunId(), status, existing.getStartedAt())) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR,
                            "该批次已被其他请求重建（runId=" + existing.getRunId() + "），请刷新后再试");
                }
                log.info("[METRIC] recon_run_rebuild_stale accountId={} date={} oldRunId={} status={} stale={}",
                        dto.getAccountId(), businessDate, existing.getRunId(), status, stale);
                return executeInPlace(existing, account);
            }
        }
        Long rebuildOf = existing == null ? null : existing.getRunId();

        ReconRun run = newRun(dto.getAccountId(), businessDate, account.getStoreId(), rebuildOf, rebuild, existing);
        try {
            reconRunTxSupport.insertRunning(run);
        } catch (DuplicateKeyException e) {
            // 并发下另一请求抢先建了同 key 的 run：按幂等语义返回既有批次
            ReconRun concurrent = findLatestRun(dto.getAccountId(), businessDate);
            if (concurrent == null) {
                throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "对账批次创建冲突且未查到既有批次：runKey=" + run.getRunKey());
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "该批次已被其他请求重建（runId=" + concurrent.getRunId() + "），请刷新后再试");
        }

        try {
            reconBatchExecutor.execute(run.getRunId());
        } catch (RuntimeException e) {
            // 跑批事务已回滚（items/differences 不留半套），这里用独立事务把 run 置 FAILED
            reconRunTxSupport.markFailed(run.getRunId(), e.getMessage());
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "对账跑批失败（runId=" + run.getRunId() + "）：" + e.getMessage());
        }

        ReconRun finalized = reconRunMapper.selectById(run.getRunId());
        if (finalized == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对账批次不存在：runId=" + run.getRunId());
        }
        return new ReconRunCreateResultVO(false, toVO(finalized, account.getAccountName()));
    }

    /** 原地重跑已抢占的 run（不新建行 → 不存在 run_key 撞库与「双跑」窗口） */
    private ReconRunCreateResultVO executeInPlace(ReconRun claimed, BankAccount account) {
        try {
            reconBatchExecutor.execute(claimed.getRunId());
        } catch (RuntimeException e) {
            reconRunTxSupport.markFailed(claimed.getRunId(), e.getMessage());
            if (e instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "对账跑批失败（runId=" + claimed.getRunId() + "）：" + e.getMessage());
        }
        ReconRun finalized = reconRunMapper.selectById(claimed.getRunId());
        return new ReconRunCreateResultVO(false, toVO(finalized, account.getAccountName()));
    }

    private ReconRun newRun(Long accountId, LocalDate businessDate, Long storeId, Long rebuildOf,
                            boolean force, ReconRun existing) {
        ReconRun run = new ReconRun();
        String baseKey = "RECON-" + accountId + "-" + businessDate.format(RUN_KEY_DATE);
        run.setRunKey(force && existing != null ? baseKey + "#R" + (countRuns(accountId, businessDate)) : baseKey);
        run.setAccountId(accountId);
        run.setStoreId(storeId);
        run.setBusinessDate(businessDate);
        run.setStatus(ReconDictionary.RUN_RUNNING);
        run.setInternalCount(0);
        run.setInternalAmount(0L);
        run.setExternalCount(0);
        run.setExternalAmount(0L);
        run.setMatchedCount(0);
        run.setMatchedAmount(0L);
        run.setDifferenceCount(0);
        run.setRebuildOf(rebuildOf);
        run.setStartedAt(LocalDateTime.now());
        run.setOperatorId(SecurityUtils.getCurrentUserId());
        run.setCreateTime(LocalDateTime.now());
        run.setUpdateTime(LocalDateTime.now());
        return run;
    }

    private ReconRun findLatestRun(Long accountId, LocalDate businessDate) {
        List<ReconRun> runs = reconRunMapper.selectList(new LambdaQueryWrapper<ReconRun>()
                .eq(ReconRun::getAccountId, accountId)
                .eq(ReconRun::getBusinessDate, businessDate)
                .orderByDesc(ReconRun::getRunId)
                .last("LIMIT 1"));
        return runs == null || runs.isEmpty() ? null : runs.get(0);
    }

    private long countItems(Long runId) {
        Long count = reconItemMapper.selectCount(new LambdaQueryWrapper<ReconItem>()
                .eq(ReconItem::getRunId, runId));
        return count == null ? 0L : count;
    }

    private int countRuns(Long accountId, LocalDate businessDate) {
        Long count = reconRunMapper.selectCount(new LambdaQueryWrapper<ReconRun>()
                .eq(ReconRun::getAccountId, accountId)
                .eq(ReconRun::getBusinessDate, businessDate));
        return count == null ? 0 : count.intValue();
    }

    // ==================== R2 / R3 ====================

    @Override
    public IPage<ReconRunVO> listRuns(ReconRunQueryDTO query) {
        ReconRunQueryDTO condition = query == null ? new ReconRunQueryDTO() : query;
        LambdaQueryWrapper<ReconRun> wrapper = new LambdaQueryWrapper<>();
        if (condition.getAccountId() != null) {
            wrapper.eq(ReconRun::getAccountId, condition.getAccountId());
        }
        if (condition.getBusinessDate() != null) {
            wrapper.eq(ReconRun::getBusinessDate, condition.getBusinessDate());
        }
        if (condition.getStatus() != null && !condition.getStatus().isBlank()) {
            wrapper.eq(ReconRun::getStatus, condition.getStatus().trim());
        }
        wrapper.orderByDesc(ReconRun::getRunId);

        Page<ReconRun> page = new Page<>(normalizeCurrent(condition.getCurrent()), normalizeSize(condition.getSize()));
        IPage<ReconRun> result = reconRunMapper.selectPage(page, wrapper);

        List<Long> accountIds = new ArrayList<>();
        for (ReconRun run : result.getRecords()) {
            accountIds.add(run.getAccountId());
        }
        Map<Long, String> accountNames = loadAccountNames(accountIds);

        return result.convert(run -> toVO(run, accountNames.get(run.getAccountId())));
    }

    @Override
    public ReconRunVO getRun(Long runId) {
        if (runId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "runId 不能为空");
        }
        ReconRun run = reconRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对账批次不存在：runId=" + runId);
        }
        BankAccount account = bankAccountMapper.selectById(run.getAccountId());
        return toVO(run, account == null ? null : account.getAccountName());
    }

    // ==================== R5 ====================

    @Override
    public IPage<ReconItemVO> listItems(ReconItemQueryDTO query) {
        ReconItemQueryDTO condition = query == null ? new ReconItemQueryDTO() : query;
        LambdaQueryWrapper<ReconItem> wrapper = new LambdaQueryWrapper<>();
        if (condition.getRunId() != null) {
            wrapper.eq(ReconItem::getRunId, condition.getRunId());
        }
        if (condition.getSide() != null && !condition.getSide().isBlank()) {
            wrapper.eq(ReconItem::getSide, condition.getSide().trim());
        }
        if (condition.getSubjectStatus() != null && !condition.getSubjectStatus().isBlank()) {
            wrapper.eq(ReconItem::getSubjectStatus, condition.getSubjectStatus().trim());
        }
        if (condition.getKeyword() != null && !condition.getKeyword().isBlank()) {
            String keyword = condition.getKeyword().trim();
            wrapper.and(w -> w.like(ReconItem::getInternalRef, keyword)
                    .or().like(ReconItem::getExternalRef, keyword)
                    .or().like(ReconItem::getCounterpartyName, keyword));
        }
        wrapper.orderByAsc(ReconItem::getItemId);

        Page<ReconItem> page = new Page<>(normalizeCurrent(condition.getCurrent()), normalizeSize(condition.getSize()));
        return reconItemMapper.selectPage(page, wrapper).convert(this::toVO);
    }

    // ==================== R6 / R7 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconItemVO matchItem(Long itemId, ReconMatchDTO dto) {
        if (itemId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "itemId 不能为空");
        }
        if (dto == null || dto.getExternalRef() == null || dto.getExternalRef().isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "externalRef 不能为空");
        }
        ReconItem internal = reconItemMapper.selectById(itemId);
        if (internal == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "勾稽主体不存在：itemId=" + itemId);
        }
        if (!ReconDictionary.SIDE_INTERNAL.equals(internal.getSide())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "人工勾稽只支持以 INTERNAL 主体发起，当前 side=" + internal.getSide());
        }
        if (!MATCHABLE_ITEM_STATUS.contains(internal.getSubjectStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "主体状态不允许勾稽：itemId=" + itemId + ", status=" + internal.getSubjectStatus());
        }

        List<ReconItem> partners = reconItemMapper.selectList(new LambdaQueryWrapper<ReconItem>()
                .eq(ReconItem::getRunId, internal.getRunId())
                .eq(ReconItem::getSide, ReconDictionary.SIDE_EXTERNAL)
                .eq(ReconItem::getExternalRef, dto.getExternalRef().trim()));
        if (partners == null || partners.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND,
                    "同一批次内不存在该外部流水：runId=" + internal.getRunId() + ", externalRef=" + dto.getExternalRef());
        }
        ReconItem external = partners.get(0);
        if (!MATCHABLE_ITEM_STATUS.contains(external.getSubjectStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "外部主体状态不允许勾稽：itemId=" + external.getItemId() + ", status=" + external.getSubjectStatus());
        }

        String groupKey = dto.getGroupKey() != null && !dto.getGroupKey().isBlank()
                ? dto.getGroupKey().trim()
                : "MG-" + internal.getRunId() + "-" + internal.getItemId() + "-" + System.nanoTime();
        LocalDateTime now = LocalDateTime.now();
        applyMatch(internal, groupKey, now);
        applyMatch(external, groupKey, now);

        ReconItem reloaded = reconItemMapper.selectById(itemId);
        return toVO(reloaded == null ? internal : reloaded);
    }

    private void applyMatch(ReconItem item, String groupKey, LocalDateTime now) {
        item.setSubjectStatus(ReconDictionary.ITEM_MANUAL_MATCHED);
        item.setMatchLevel(ReconDictionary.LEVEL_MANUAL);
        item.setMatchGroup(groupKey);
        item.setMatchedSince(now);
        item.setUpdateTime(now);
        reconItemMapper.updateById(item);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconItemVO unmatchItem(Long itemId) {
        if (itemId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "itemId 不能为空");
        }
        ReconItem item = reconItemMapper.selectById(itemId);
        if (item == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "勾稽主体不存在：itemId=" + itemId);
        }
        if (!ReconDictionary.ITEM_MANUAL_MATCHED.equals(item.getSubjectStatus())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "仅 MANUAL_MATCHED 可解绑，当前 status=" + item.getSubjectStatus());
        }

        List<ReconItem> targets;
        if (item.getMatchGroup() == null || item.getMatchGroup().isBlank()) {
            targets = List.of(item);
        } else {
            targets = reconItemMapper.selectList(new LambdaQueryWrapper<ReconItem>()
                    .eq(ReconItem::getRunId, item.getRunId())
                    .eq(ReconItem::getMatchGroup, item.getMatchGroup()));
            if (targets == null || targets.isEmpty()) {
                targets = List.of(item);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (ReconItem target : targets) {
            // 用 UpdateWrapper 显式 set(...) NULL：MyBatis-Plus 默认 updateById 会忽略 null 字段，
            // 直接 set null + updateById 会把 match_group / matched_since 留在库里（解绑不彻底）。
            reconItemMapper.update(null, new LambdaUpdateWrapper<ReconItem>()
                    .eq(ReconItem::getItemId, target.getItemId())
                    .set(ReconItem::getSubjectStatus, ReconDictionary.ITEM_UNMATCHED)
                    .set(ReconItem::getMatchLevel, null)
                    .set(ReconItem::getMatchGroup, null)
                    .set(ReconItem::getMatchedSince, null)
                    .set(ReconItem::getConfidence, null)
                    .set(ReconItem::getUpdateTime, now));
        }
        ReconItem reloaded = reconItemMapper.selectById(itemId);
        return toVO(reloaded == null ? item : reloaded);
    }

    // ==================== 转换 ====================

    private ReconRunVO toVO(ReconRun run, String accountName) {
        ReconRunVO vo = new ReconRunVO();
        vo.setRunId(run.getRunId());
        vo.setRunKey(run.getRunKey());
        vo.setAccountId(run.getAccountId());
        vo.setAccountName(accountName);
        vo.setStoreId(run.getStoreId());
        vo.setBusinessDate(run.getBusinessDate());
        vo.setStatus(run.getStatus());
        vo.setInternalCount(run.getInternalCount());
        vo.setInternalAmount(run.getInternalAmount());
        vo.setExternalCount(run.getExternalCount());
        vo.setExternalAmount(run.getExternalAmount());
        vo.setMatchedCount(run.getMatchedCount());
        vo.setMatchedAmount(run.getMatchedAmount());
        vo.setMatchRate(matchRate(run.getMatchedAmount(), run.getInternalAmount()));
        vo.setDifferenceCount(run.getDifferenceCount());
        vo.setRebuildOf(run.getRebuildOf());
        vo.setStartedAt(run.getStartedAt());
        vo.setFinishedAt(run.getFinishedAt());
        vo.setOperatorId(run.getOperatorId());
        vo.setCreateTime(run.getCreateTime());
        vo.setUpdateTime(run.getUpdateTime());
        return vo;
    }

    /** 勾稽率 = matched_amount / internal_amount × 100（分母为 0 或无数据时返回 null，不返回默认 0） */
    private BigDecimal matchRate(Long matchedAmount, Long internalAmount) {
        if (internalAmount == null || internalAmount <= 0) {
            return null;
        }
        long matched = matchedAmount == null ? 0L : matchedAmount;
        return BigDecimal.valueOf(matched)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(internalAmount), 2, RoundingMode.HALF_UP);
    }

    private ReconItemVO toVO(ReconItem item) {
        ReconItemVO vo = new ReconItemVO();
        vo.setItemId(item.getItemId());
        vo.setRunId(item.getRunId());
        vo.setSide(item.getSide());
        vo.setInternalRef(item.getInternalRef());
        vo.setExternalRef(item.getExternalRef());
        vo.setPaymentMethods(item.getPaymentMethods());
        vo.setAmount(item.getAmount());
        vo.setTxnTime(item.getTxnTime());
        vo.setCounterpartyName(item.getCounterpartyName());
        vo.setSubjectStatus(item.getSubjectStatus());
        vo.setMatchedSince(item.getMatchedSince());
        vo.setMatchLevel(item.getMatchLevel());
        vo.setMatchGroup(item.getMatchGroup());
        vo.setSuggestedExternalRef(item.getSuggestedExternalRef());
        vo.setConfidence(item.getConfidence());
        vo.setCreateTime(item.getCreateTime());
        vo.setUpdateTime(item.getUpdateTime());
        return vo;
    }

    private Map<Long, String> loadAccountNames(List<Long> accountIds) {
        Map<Long, String> names = new HashMap<>();
        if (accountIds == null || accountIds.isEmpty()) {
            return names;
        }
        List<BankAccount> accounts = bankAccountMapper.selectBatchIds(accountIds);
        if (accounts != null) {
            for (BankAccount account : accounts) {
                names.put(account.getAccountId(), account.getAccountName());
            }
        }
        return names;
    }

    private long normalizeCurrent(Long current) {
        return current == null || current < 1 ? 1L : current;
    }

    private long normalizeSize(Long size) {
        if (size == null || size < 1) {
            return 20L;
        }
        return Math.min(size, 200L);
    }
}
