package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.foodtraceability.entity.ExceptionOps;
import com.foodtraceability.entity.OrderNew;
import com.foodtraceability.mapper.ExceptionOpsMapper;
import com.foodtraceability.mapper.OrderNewMapper;
import com.foodtraceability.service.pos.PosExceptionService;
import com.foodtraceability.service.pos.ResolveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 异常单处理（补收款 RESOLVE）。
 *
 * <p>幂等前置在**事务外**做（读 exception_ops）；真正的写操作委托给 {@link PosExceptionTxBean}
 * （跨 Bean 调用，避免 this.xxx() 使 @Transactional 失效）。</p>
 */
@Service
public class PosExceptionServiceImpl implements PosExceptionService {

    private static final Logger log = LoggerFactory.getLogger(PosExceptionServiceImpl.class);

    private final ExceptionOpsMapper exceptionOpsMapper;
    private final OrderNewMapper orderNewMapper;
    private final PosExceptionTxBean txBean;

    public PosExceptionServiceImpl(ExceptionOpsMapper exceptionOpsMapper,
                                   OrderNewMapper orderNewMapper,
                                   PosExceptionTxBean txBean) {
        this.exceptionOpsMapper = exceptionOpsMapper;
        this.orderNewMapper = orderNewMapper;
        this.txBean = txBean;
    }

    @Override
    public ResolveResult resolve(String orderId, Long amountFen, String payType, Long cashReceivedFen,
                                 String idempotencyKey, String operatorId) {
        // 1) 幂等前置：同 key 已成功 → 直接重放，不做任何写
        ExceptionOps existed = exceptionOpsMapper.selectByKey(idempotencyKey);
        if (existed != null) {
            if (!orderId.equals(existed.getOrderId())) {
                return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "幂等键已被其它订单占用");
            }
            log.info("[EXC-OPS] 幂等重放 orderId={} key={}", orderId, idempotencyKey);
            return ResolveResult.replay("幂等重放：本次未执行任何写操作");
        }
        // 2) 真正写操作：委托到跨 Bean 的事务执行体
        return txBean.execute(orderId, amountFen, payType, cashReceivedFen, idempotencyKey, operatorId);
    }

    @Override
    public ResolveResult voidOrder(String orderId, String reason, String note, String idempotencyKey, String operatorId) {
        ExceptionOps existed = exceptionOpsMapper.selectByKey(idempotencyKey);
        if (existed != null) {
            if (!orderId.equals(existed.getOrderId())) {
                return ResolveResult.fail(ResolveResult.Status.ALREADY_HANDLED, "幂等键已被其它订单占用");
            }
            log.info("[EXC-OPS] 幂等重放(VOID) orderId={} key={}", orderId, idempotencyKey);
            return ResolveResult.replay("幂等重放：本次未执行任何写操作");
        }
        return txBean.executeVoid(orderId, reason, note, idempotencyKey, operatorId);
    }

    @Override
    public ResolveResult doResolveInTx(String orderId, Long amountFen, String payType, Long cashReceivedFen,
                                       String idempotencyKey, String operatorId) {
        return txBean.execute(orderId, amountFen, payType, cashReceivedFen, idempotencyKey, operatorId);
    }

    @Override
    public Map<String, Object> snapshot(Long shiftId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("shiftId", shiftId);
        if (shiftId != null) {
            m.put("pendingExceptionCount", exceptionOpsMapper.countPendingExceptionOrders(shiftId));
        }
        return m;
    }
}
