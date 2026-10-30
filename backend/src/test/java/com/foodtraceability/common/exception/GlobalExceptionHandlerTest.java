package com.foodtraceability.common.exception;

import com.foodtraceability.common.Result;
import com.foodtraceability.service.ExceptionMonitoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * RT-2 [P1] 回归：未捕获异常的 HTTP 状态必须是 <b>真 500</b>，不能再是「HTTP 200 + body.code=500」。
 *
 * <p>R7 现场：{@code POST /v1/appeals/NOPE/process} → HTTP 200，body {@code {"code":500,...}} ——
 * 按状态码做的告警、网关重试、前端拦截器全部形同虚设。</p>
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(mock(ExceptionMonitoringService.class));

    @Test
    @DisplayName("兜底 Exception → HTTP 500 + body.code 500 + X-Trace-Id")
    void uncaughtExceptionReturnsReal500() {
        ResponseEntity<Result<?>> resp = handler.handleException(new IllegalStateException("boom"));

        assertEquals(500, resp.getStatusCode().value(), "HTTP 状态码必须真是 500");
        assertNotNull(resp.getBody());
        assertEquals(500, resp.getBody().getCode(), "body.code 同步为 500");
        assertNotNull(resp.getHeaders().getFirst("X-Trace-Id"), "必须带 traceId 便于按 ID 反查日志");
        assertTrue(resp.getBody().getMessage().contains("追踪ID："), "文案必须含同一 traceId");
    }

    @Test
    @DisplayName("RuntimeException → HTTP 500（不是 200）")
    void runtimeExceptionReturnsReal500() {
        ResponseEntity<Result<?>> resp = handler.handleRuntimeException(new RuntimeException("npe-like"));
        assertEquals(500, resp.getStatusCode().value());
        assertEquals(500, resp.getBody() == null ? null : resp.getBody().getCode());
    }

    @Test
    @DisplayName("NullPointerException → HTTP 500（不是 200）")
    void npeReturnsReal500() {
        ResponseEntity<Result<?>> resp = handler.handleNullPointerException(new NullPointerException("x"));
        assertEquals(500, resp.getStatusCode().value());
    }

    @Test
    @DisplayName("BusinessException 携带 httpStatus=409 → HTTP 409（唯一键冲突不得漏成 500）")
    void businessConflictReturns409() {
        ResponseEntity<?> resp = handler.handleBusinessException(
                new BusinessException(409, "订单已是【已取消】状态，无需重复作废", 409));
        assertEquals(409, resp.getStatusCode().value());
    }
}
