package com.foodtraceability.service;

import com.foodtraceability.dto.PosHoldCreateDTO;
import com.foodtraceability.entity.PosHeldOrder;

import java.util.List;

/** POS 挂单服务（Owner 2026-10-30 P0）。 */
public interface PosHeldOrderService {

    /** 创建挂单（幂等：同一幂等键重复调用只产生一行） */
    PosHeldOrder create(PosHoldCreateDTO dto, String idempotencyKey, Long employeeId);

    /** 列出当前员工在当前终端的有效挂单 */
    List<PosHeldOrder> listHeld(Long employeeId, String terminalId);

    /** CAS 作废；返回 false 表示 0 行更新（冲突或已失效） */
    boolean cancel(Long id, Long employeeId, String terminalId);

    /** CAS 结账；返回 false 表示 0 行更新（冲突或已失效） */
    boolean checkout(Long id, Long employeeId, String terminalId);

    /** TTL：把过期的 HOLD 置 EXPIRED，返回影响行数 */
    int expireOverdue();
}
