package com.foodtraceability.service.pos;

import java.util.Map;

/** 异常单处理（补收款 / 作废）。 */
public interface PosExceptionService {

    ResolveResult resolve(String orderId, Long amountFen, String payType, Long cashReceivedFen,
                          String idempotencyKey, String operatorId);

    ResolveResult voidOrder(String orderId, String reason, String note, String idempotencyKey, String operatorId);

    /** 事务内执行（由 {@code PosExceptionTxBean} 代理调用，避免自调用使 @Transactional 失效） */
    ResolveResult doResolveInTx(String orderId, Long amountFen, String payType, Long cashReceivedFen,
                                String idempotencyKey, String operatorId);

    /** 班次统计快照（最小实现：本班次已收/退款/净额，全部「分」） */
    Map<String, Object> snapshot(Long shiftId);
}
