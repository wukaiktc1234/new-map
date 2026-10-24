package com.foodtraceability.common.finance;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.exception.ConflictException;
import com.foodtraceability.common.exception.ErrorCode;
import com.foodtraceability.entity.finance.BankAccount;
import com.foodtraceability.mapper.finance.BankAccountMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 余额原子更新支持类（P2 缴款单红线 4 的唯一实现）。
 *
 * <p><b>只允许一种写法</b>：
 * UPDATE bank_accounts SET balance = balance + delta, version = version + 1
 * WHERE account_id = ? AND version = expectedVersion AND deleted = 0。</p>
 *
 * <p><b>禁止</b>调用 BankAccountServiceImpl.updateBalance(accountId, newBalance)：
 * 那是「裸 SET 绝对值」，并发下会覆盖其他终端的入账。</p>
 *
 * <p>本类不自己开事务：必须由调用方的 @Transactional 包裹，影响行数 ≠ 1 时抛业务异常，
 * 由外层事务整体回滚（缴款单入账两条腿要么都成、要么都不成）。</p>
 */
@Component
public class AtomicBalanceUpdater {

    private static final Logger log = LoggerFactory.getLogger(AtomicBalanceUpdater.class);

    /** 乐观锁冲突时的默认最大重试次数（每次重读 version 再试） */
    private static final int DEFAULT_MAX_RETRY = 3;

    /**
     * 乐观锁冲突时的最大重试次数（可配置，默认 {@value #DEFAULT_MAX_RETRY}）。
     *
     * <p>BL-3 验收：仅为了能在测试实例上用启动参数
     * {@code --app.finance.balance.max-retry=1} 制造「一次冲突即耗尽」，
     * 生产默认仍是 3，语义不变。</p>
     */
    private final int maxRetry;

    private final BankAccountMapper bankAccountMapper;

    public AtomicBalanceUpdater(BankAccountMapper bankAccountMapper,
                                @org.springframework.beans.factory.annotation.Value("${app.finance.balance.max-retry:3}")
                                int maxRetry) {
        this.bankAccountMapper = bankAccountMapper;
        // 防御：配置成 <=0 时回落到默认值，避免出现「0 次尝试」的死路
        this.maxRetry = maxRetry > 0 ? maxRetry : DEFAULT_MAX_RETRY;
    }

    /**
     * 原子增减余额并返回变更前后快照。
     *
     * @param accountId 账户ID
     * @param delta     变动额（分），正=入账 负=出账
     * @return 变更前/后余额与版本（balance_after 必须取 afterBalance，不得手填）
     */
    public BalanceSnapshot addBalance(Long accountId, long delta) {
        if (accountId == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "账户ID不能为空");
        }
        if (delta == 0L) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "余额变动额不能为 0：accountId=" + accountId);
        }
        for (int attempt = 1; attempt <= maxRetry; attempt++) {
            BankAccount account = bankAccountMapper.selectById(accountId);
            if (account == null || Integer.valueOf(1).equals(account.getDeleted())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "银行账户不存在或已删除：accountId=" + accountId);
            }
            long beforeBalance = account.getBalance() == null ? 0L : account.getBalance();
            int beforeVersion = account.getVersion() == null ? 0 : account.getVersion();
            int rows = bankAccountMapper.addBalanceOptimistic(accountId, delta, beforeVersion);
            if (rows == 1) {
                BankAccount after = bankAccountMapper.selectById(accountId);
                if (after == null) {
                    throw new BusinessException(ErrorCode.NOT_FOUND, "余额更新后账户不可见：accountId=" + accountId);
                }
                long afterBalance = after.getBalance() == null ? 0L : after.getBalance();
                int afterVersion = after.getVersion() == null ? 0 : after.getVersion();
                log.info("[METRIC] balance_atomic_updated accountId={} delta={} before={}/v{} after={}/v{} attempt={}",
                        accountId, delta, beforeBalance, beforeVersion, afterBalance, afterVersion, attempt);
                return new BalanceSnapshot(accountId, beforeBalance, afterBalance, beforeVersion, afterVersion);
            }
            log.warn("[METRIC] balance_version_conflict accountId={} expectedVersion={} attempt={}",
                    accountId, beforeVersion, attempt);
        }
        // BL-3：乐观锁冲突属「业务可识别的冲突」，返回 HTTP 409 + 原文，不再落入 500「系统繁忙」
        throw new ConflictException(
                "余额更新乐观锁冲突，重试 " + maxRetry + " 次仍失败：accountId=" + accountId);
    }

    /** 余额快照（变更前后） */
    public static final class BalanceSnapshot {
        private final Long accountId;
        private final long balanceBefore;
        private final long balanceAfter;
        private final int versionBefore;
        private final int versionAfter;

        public BalanceSnapshot(Long accountId, long balanceBefore, long balanceAfter,
                               int versionBefore, int versionAfter) {
            this.accountId = accountId;
            this.balanceBefore = balanceBefore;
            this.balanceAfter = balanceAfter;
            this.versionBefore = versionBefore;
            this.versionAfter = versionAfter;
        }

        public Long getAccountId() { return accountId; }
        public long getBalanceBefore() { return balanceBefore; }
        public long getBalanceAfter() { return balanceAfter; }
        public int getVersionBefore() { return versionBefore; }
        public int getVersionAfter() { return versionAfter; }
    }
}
