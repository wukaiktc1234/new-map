package com.foodtraceability.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.dto.PosHoldCreateDTO;
import com.foodtraceability.entity.PosHeldOrder;
import com.foodtraceability.mapper.PosHeldOrderMapper;
import com.foodtraceability.service.PosHeldOrderService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** POS 挂单实现（Owner 2026-10-30 P0）。 */
@Service
public class PosHeldOrderServiceImpl implements PosHeldOrderService {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(PosHeldOrderServiceImpl.class);

    private final PosHeldOrderMapper mapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PosHeldOrderServiceImpl(PosHeldOrderMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PosHeldOrder create(PosHoldCreateDTO dto, String idempotencyKey, Long employeeId) {
        if (employeeId == null) {
            throw new BusinessException(401, "未认证，无法确定操作人");
        }
        if (dto == null || dto.getCartData() == null || dto.getCartData().isEmpty()) {
            throw new BusinessException(400, "cartData 必填");
        }
        String terminalId = dto.getTerminalId();
        if (terminalId == null || terminalId.isBlank()) {
            throw new BusinessException(400, "terminalId 必填（与 pos_shifts 同一来源，不从 header 读取）");
        }
        String idem = dto.getClientHoldId();
        if (idem == null || idem.isBlank()) {
            idem = idempotencyKey;
        }
        if (idem == null || idem.isBlank()) {
            throw new BusinessException(400, "幂等键必填（body.clientHoldId 或 Header: Idempotency-Key）");
        }

        String orderNo = "HOLD" + sha256Hex(idem).substring(0, 24);

        PosHeldOrder exist = mapper.selectByOrderNo(orderNo);
        if (exist != null) {
            log.info("[METRIC] hold_create_replay orderNo={} employeeId={} terminalId={}",
                    orderNo, employeeId, terminalId);
            return exist;
        }

        PosHeldOrder row = new PosHeldOrder();
        row.setOrderNo(orderNo);
        row.setEmployeeId(employeeId);
        row.setTerminalId(terminalId);
        row.setStoreId(dto.getStoreId());
        try {
            row.setCartData(objectMapper.writeValueAsString(dto.getCartData()));
        } catch (Exception e) {
            throw new BusinessException(400, "cartData 无法序列化为 JSON：" + e.getMessage());
        }
        row.setTotalAmountFen(dto.getTotalAmountFen() == null ? 0L : dto.getTotalAmountFen());
        if (row.getTotalAmountFen() < 0) {
            throw new BusinessException(400, "totalAmountFen 不能为负（单位：分）");
        }
        row.setStatus("HOLD");
        row.setVersion(0);
        row.setExpireAt(LocalDateTime.of(LocalDate.now(), LocalTime.of(23, 59, 59)));
        row.setDeleted(0);

        try {
            mapper.insertHold(row);
        } catch (DuplicateKeyException dup) {
            PosHeldOrder again = mapper.selectByOrderNo(orderNo);
            if (again == null) {
                throw dup;
            }
            log.info("[METRIC] hold_create_replay_race orderNo={}", orderNo);
            return again;
        }
        log.info("[METRIC] hold_create orderNo={} employeeId={} terminalId={} amountFen={} expireAt={}",
                orderNo, employeeId, terminalId, row.getTotalAmountFen(), row.getExpireAt());
        return mapper.selectByOrderNo(orderNo);
    }

    @Override
    public List<PosHeldOrder> listHeld(Long employeeId, String terminalId) {
        if (employeeId == null) {
            throw new BusinessException(401, "未认证，无法确定操作人");
        }
        if (terminalId == null || terminalId.isBlank()) {
            throw new BusinessException(400, "terminalId 必填");
        }
        return mapper.selectHeld(employeeId, terminalId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long id, Long employeeId, String terminalId) {
        requireScope(employeeId, terminalId);
        return mapper.casCancel(id, employeeId, terminalId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean checkout(Long id, Long employeeId, String terminalId) {
        requireScope(employeeId, terminalId);
        int rows = mapper.casComplete(id, employeeId, terminalId);
        if (rows == 0) {
            log.warn("[METRIC] hold_checkout_conflict id={} employeeId={} terminalId={}",
                    id, employeeId, terminalId);
            return false;
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int expireOverdue() {
        int n = mapper.expireOverdue();
        if (n > 0) {
            log.info("[METRIC] hold_expired rows={}", n);
        }
        return n;
    }

    private void requireScope(Long employeeId, String terminalId) {
        if (employeeId == null) {
            throw new BusinessException(401, "未认证，无法确定操作人");
        }
        if (terminalId == null || terminalId.isBlank()) {
            throw new BusinessException(400, "terminalId 必填");
        }
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
