package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 挂单创建入参（Owner 2026-10-30 P0）。
 *
 * <p><b>terminalId 为什么在这里而不是 header</b>：与 pos_shifts 的 terminal_id 同一来源 ——
 * PosStartShiftDTO / PosEndShiftDTO / PosLoginDTO 都是从请求体取的。挂单必须保持一致，
 * 绝不允许从 HTTP header 读取终端标识（P0-D 已因 header 信任出过漏洞）。</p>
 *
 * <p>金额单位：totalAmountFen = <b>分</b>，契约不做元/分自动猜测。</p>
 */
@Schema(description = "挂单创建入参")
public class PosHoldCreateDTO {

    @Schema(description = "购物车数据（任意 JSON 对象）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Map<String, Object> cartData;

    @Schema(description = "合计金额（分）", example = "12800")
    private Long totalAmountFen;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "客户端挂单标识（幂等键，可空则用 Idempotency-Key）")
    private String clientHoldId;

    @Schema(description = "终端ID（与 pos_shifts 同一来源）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String terminalId;

    @Schema(description = "门店ID")
    private Long storeId;

    public Map<String, Object> getCartData() { return cartData; }
    public void setCartData(Map<String, Object> cartData) { this.cartData = cartData; }
    public Long getTotalAmountFen() { return totalAmountFen; }
    public void setTotalAmountFen(Long totalAmountFen) { this.totalAmountFen = totalAmountFen; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getClientHoldId() { return clientHoldId; }
    public void setClientHoldId(String clientHoldId) { this.clientHoldId = clientHoldId; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
