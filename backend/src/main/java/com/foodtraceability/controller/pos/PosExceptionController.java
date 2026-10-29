package com.foodtraceability.controller.pos;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.pos.PosExceptionService;
import com.foodtraceability.service.pos.ResolveResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 异常单处理（Owner 2026-10-29 授权落地）。
 *
 * <p><b>HTTP 语义按契约返回真状态码</b>（409/422），不是 200+body.code ——
 * 本接口是「HTTP 语义分批改造」的第一批。</p>
 *
 * <p>响应外层固定含 <b>replayed</b> 布尔（幂等重放 true / 首次 false），Owner 判据。</p>
 */
@RestController
// ⚠ 注意：应用 context-path = /api（application*.yml），所以这里的 mapping **不能再带 /api**。
//   修前写成 "/api/v1/pos/exceptions" → 实际生效路径是 /api/api/v1/pos/exceptions/…，
//   前端 baseURL='/api' 拼出来是 /api/v1/pos/exceptions/… → **框架层 404（响应体为空）**。
@RequestMapping("/v1/pos/exceptions")
@Tag(name = "POS-异常单处理")
public class PosExceptionController {

    private static final Logger log = LoggerFactory.getLogger(PosExceptionController.class);

    private final PosExceptionService service;

    public PosExceptionController(PosExceptionService service) {
        this.service = service;
    }

    @PostMapping("/{orderId}/resolve")
    @PreAuthorize("hasAuthority('pos:order:pay')")
    @Operation(summary = "补收款（异常单）", description = "真 409/422；响应含 replayed")
    public ResponseEntity<Map<String, Object>> resolve(@PathVariable String orderId,
                                                       @RequestBody Map<String, Object> body,
                                                       @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                                       @RequestHeader(value = "Idempotency-Key", required = false) String idemHeader) {
        // ①（Lead 2026-10-29）：**幂等键双读** —— body 优先，回退 Header。
        //   网关/代理去重靠 Header，业务透传靠 body；只支持一个必然有一方撞墙（前端第一次就撞了）。
        String idemKey = asString(body.get("idempotencyKey"));
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = idemHeader;
        }
        if (idemKey == null || idemKey.isBlank()) {
            return respond(false, 400, "idempotencyKey 必填（body.idempotencyKey 或 Header: Idempotency-Key 二选一）", false);
        }
        Long amountFen = asLong(body.get("amountFen"));
        Long cashReceivedFen = asLong(body.get("cashReceivedFen"));
        String payType = asString(body.get("payType"));
        String op = operatorId != null ? operatorId
                : String.valueOf(com.foodtraceability.utils.SecurityUtils.getCurrentUserId());
        try {
            ResolveResult r = service.resolve(orderId, amountFen, payType, cashReceivedFen, idemKey, op);
            switch (r.getStatus()) {
                case OK:
                case REPLAY:
                    return respond(true, 0, r.getMessage(), r.isReplayed());
                case AMOUNT_MISMATCH:
                    return respond(false, 422, r.getMessage(), false);
                case ALREADY_HANDLED:
                    return respond(false, 409, r.getMessage(), false);
                default:
                    return respond(false, 500, r.getMessage(), false);
            }
        } catch (com.foodtraceability.exception.BusinessException be) {
            if (be.getCode() == 40903) {
                return respond(false, 409, be.getMessage(), false);
            }
            return respond(false, 400, be.getMessage(), false);
        } catch (Exception e) {
            log.error("异常单补收款失败: orderId={} err={}", orderId, e.getMessage());
            return respond(false, 500, "补收款失败：" + e.getMessage(), false);
        }
    }

    @PostMapping("/{orderId}/void")
    @PreAuthorize("hasAuthority('pos:order:void')")
    @Operation(summary = "作废异常单", description = "不产生资金流水；已支付单 409 ALREADY_PAID")
    public ResponseEntity<Map<String, Object>> voidOrder(@PathVariable String orderId,
                                                         @RequestBody Map<String, Object> body,
                                                         @RequestHeader(value = "X-Operator-Id", required = false) String operatorId,
                                                         @RequestHeader(value = "Idempotency-Key", required = false) String idemHeader) {
        // ①（Lead 2026-10-29）：幂等键双读，body 优先，回退 Header（与 resolve 保持一致）
        String idemKey = asString(body.get("idempotencyKey"));
        if (idemKey == null || idemKey.isBlank()) {
            idemKey = idemHeader;
        }
        String reason = asString(body.get("reason"));
        if (idemKey == null || idemKey.isBlank()) {
            return respond(false, 400, "idempotencyKey 必填（body.idempotencyKey 或 Header: Idempotency-Key 二选一）", false);
        }
        if (reason == null || reason.isBlank()) {
            return respond(false, 400, "reason 必填", false);
        }
        String op = operatorId != null ? operatorId
                : String.valueOf(com.foodtraceability.utils.SecurityUtils.getCurrentUserId());
        try {
            ResolveResult r = service.voidOrder(orderId, reason, asString(body.get("note")), idemKey, op);
            switch (r.getStatus()) {
                case OK:
                case REPLAY:
                    return respond(true, 0, r.getMessage(), r.isReplayed());
                case ALREADY_PAID:
                    return respond(false, 409, r.getMessage(), false);
                case ALREADY_HANDLED:
                    return respond(false, 409, r.getMessage(), false);
                default:
                    return respond(false, 500, r.getMessage(), false);
            }
        } catch (com.foodtraceability.exception.BusinessException be) {
            return respond(false, be.getCode() == 40903 ? 409 : 400, be.getMessage(), false);
        } catch (Exception e) {
            log.error("异常单作废失败: orderId={} err={}", orderId, e.getMessage());
            return respond(false, 500, "作废失败：" + e.getMessage(), false);
        }
    }

    private ResponseEntity<Map<String, Object>> respond(boolean ok, int code, String message, boolean replayed) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        // ② 结构化字段（Lead 2026-10-29）：40902 时 data 带 pendingExceptionCount，不只塞 message
        Map<String, Object> data = null;
        if (code == 40902 || (message != null && message.contains("pendingExceptionCount="))) {
            java.util.regex.Matcher mt = java.util.regex.Pattern
                    .compile("pendingExceptionCount=(\\d+)").matcher(message == null ? "" : message);
            if (mt.find()) {
                data = new LinkedHashMap<>();
                data.put("pendingExceptionCount", Long.valueOf(mt.group(1)));
            }
        }
        body.put("data", data);
        body.put("replayed", replayed);
        HttpStatus status = ok ? HttpStatus.OK
                : (code == 409 ? HttpStatus.CONFLICT
                : (code == 422 ? HttpStatus.UNPROCESSABLE_ENTITY
                : (code == 400 ? HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR)));
        return ResponseEntity.status(status).body(body);
    }

    private String asString(Object o) { return o == null ? null : String.valueOf(o); }

    private Long asLong(Object o) {
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }
}
