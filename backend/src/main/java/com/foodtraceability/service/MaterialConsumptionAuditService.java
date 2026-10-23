package com.foodtraceability.service;

import com.foodtraceability.common.exception.MaterialDeductionFailureException;

/**
 * KDS 出餐扣料失败审计服务（P0）。
 * <p>
 * 独立 Spring Bean，专门承载"扣料失败审计行"的写入。
 * 实现类方法使用 @Transactional(propagation = REQUIRES_NEW)：
 * 主扣料事务回滚后，FAILED 审计行仍持久化（悬挂主事务 → 新事务提交审计 → 主事务回滚不影响已提交的审计行）。
 * <p>
 * 禁止在 OrderNewServiceImpl 内 self-invoke 本机制（this 调用不经过代理，会静默退化为同事务）——
 * 必须通过本接口注入调用。
 */
public interface MaterialConsumptionAuditService {

    /**
     * 写入一条 material_consumption FAILED 审计行。
     * <p>
     * 该方法保证永不向外抛出异常：审计写入失败只记录 ERROR 日志，
     * 不改变主扣料异常（MaterialDeductionFailureException）的传播与回滚行为。
     *
     * @param failure 扣料失败异常（携带上下文）
     */
    void recordDeductionFailure(MaterialDeductionFailureException failure);
}
