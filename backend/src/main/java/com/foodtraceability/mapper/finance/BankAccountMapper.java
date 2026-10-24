package com.foodtraceability.mapper.finance;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.foodtraceability.entity.finance.BankAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 银行账户Mapper接口
 */
@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {

    /**
     * 原子调整余额（收入传正数、支出传负数）。
     *
     * <p>资金流水记账必须用本方法，不能「先 select 再 updateById」：
     * 后者是读-改-写，多终端并发支付时会丢更新（历史 bug）。
     * 单条 UPDATE 由行锁串行化，调用方随后回读 balance 写入 flow.balance_after。</p>
     *
     * @param accountId 账户ID
     * @param delta     变动额（分），正=入账 负=出账
     * @return 影响行数（0=账户不存在或已删除）
     */
    @Update("UPDATE bank_accounts SET balance = COALESCE(balance, 0) + #{delta}, "
            + "version = COALESCE(version, 0) + 1, update_time = NOW() "
            + "WHERE account_id = #{accountId} AND deleted = 0")
    int addBalance(@Param("accountId") Long accountId, @Param("delta") Long delta);

    /**
     * 带乐观锁的原子调整余额（P2 缴款单专用，红线 4）。
     *
     * <p>与 {@link #addBalance} 的差别：多一个 {@code AND COALESCE(version,0) = #{expectedVersion}}。
     * 调用方先读到 version=expectedVersion，更新成功后 version+1；影响行数 ≠ 1 即并发冲突，
     * 必须重读 version 重试或整体回滚 —— 绝不允许「裸 SET balance = 绝对值」覆盖他人更新。</p>
     *
     * @param accountId       账户ID
     * @param delta           变动额（分），正=入账 负=出账
     * @param expectedVersion 期望的当前版本号（乐观锁）
     * @return 影响行数（1=成功；0=版本冲突/账户不存在/已删除）
     */
    @Update("UPDATE bank_accounts SET balance = COALESCE(balance, 0) + #{delta}, "
            + "version = COALESCE(version, 0) + 1, update_time = NOW() "
            + "WHERE account_id = #{accountId} AND COALESCE(version, 0) = #{expectedVersion} AND deleted = 0")
    int addBalanceOptimistic(@Param("accountId") Long accountId,
                             @Param("delta") Long delta,
                             @Param("expectedVersion") Integer expectedVersion);
}
