package com.foodtraceability.controller.pos;

import com.foodtraceability.common.exception.BusinessException;
import com.foodtraceability.common.Result;
import com.foodtraceability.dto.PosHoldCreateDTO;
import com.foodtraceability.entity.PosHeldOrder;
import com.foodtraceability.service.PosHeldOrderService;
import com.foodtraceability.utils.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * POS 挂单接口（Owner 2026-10-30 P0）。
 *
 * <p>隔离：employee_id 一律取自 SecurityContext（<b>不读 header</b>），
 * terminal_id 取自请求参数（与 pos_shifts 同一来源，不读 header）。</p>
 *
 * <p>CAS 冲突一律抛 BusinessException(409) → 真 HTTP 409（GlobalExceptionHandler 已映射）。</p>
 */
@RestController
@RequestMapping("/v1/pos/holds")
@Tag(name = "POS 挂单", description = "挂单/取单/作废/结账（后端持久化，TTL 当日 23:59:59）")
public class PosHoldController {

    private final PosHeldOrderService service;

    public PosHoldController(PosHeldOrderService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('pos:order:create')")
    @Operation(summary = "创建挂单", description = "幂等：body.clientHoldId 优先，退化到 Header: Idempotency-Key")
    public Result<Map<String, Object>> create(@RequestBody PosHoldCreateDTO dto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idemHeader) {
        Long uid = SecurityUtils.getCurrentUserId();
        PosHeldOrder row = service.create(dto, idemHeader, uid);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", row.getId());
        data.put("orderNo", row.getOrderNo());
        data.put("expireAt", row.getExpireAt());
        data.put("createTime", row.getCreateTime());
        return Result.success(data, "挂单成功");
    }

    @GetMapping
    @PreAuthorize("hasAuthority('pos:order:query')")
    @Operation(summary = "查询当前终端有效挂单", description = "status=HOLD 且 expire_at > now()，仅本人 + 本终端")
    public Result<Map<String, Object>> list(@RequestParam String terminalId) {
        Long uid = SecurityUtils.getCurrentUserId();
        List<PosHeldOrder> rows = service.listHeld(uid, terminalId);
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (PosHeldOrder r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("orderNo", r.getOrderNo());
            m.put("cartData", r.getCartData());
            m.put("totalAmountFen", r.getTotalAmountFen());
            m.put("createTime", r.getCreateTime());
            m.put("expireAt", r.getExpireAt());
            list.add(m);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("list", list);
        data.put("total", list.size());
        return Result.success(data, "查询成功");
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('pos:order:void')")
    @Operation(summary = "作废挂单", description = "CAS：仅 HOLD + 本人 + 本终端；0 行更新 → 409")
    public Result<Void> cancel(@PathVariable Long id, @RequestParam String terminalId) {
        Long uid = SecurityUtils.getCurrentUserId();
        boolean ok = service.cancel(id, uid, terminalId);
        if (!ok) {
            throw new BusinessException(409, "挂单已被其它终端处理或已失效");
        }
        return Result.success(null, "挂单已作废");
    }

    @PostMapping("/{id}/checkout")
    @PreAuthorize("hasAuthority('pos:order:pay')")
    @Operation(summary = "挂单结账", description = "CAS：仅 HOLD + 本人 + 本终端；0 行更新 → 409")
    public Result<Void> checkout(@PathVariable Long id, @RequestParam String terminalId) {
        Long uid = SecurityUtils.getCurrentUserId();
        boolean ok = service.checkout(id, uid, terminalId);
        if (!ok) {
            throw new BusinessException(409, "挂单已被其它终端处理或已失效");
        }
        return Result.success(null, "挂单已结账");
    }
}
