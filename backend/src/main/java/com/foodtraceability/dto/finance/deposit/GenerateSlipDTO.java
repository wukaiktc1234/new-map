package com.foodtraceability.dto.finance.deposit;

import jakarta.validation.constraints.NotNull;

/**
 * SL1 生成缴款单入参（按班次生成，幂等）。
 *
 * <p>storeId 可选：缺省时服务端按「班次时间窗内订单的门店」推导，要求唯一；
 * 班次跨门店/无法唯一归属时直接 400（不猜、不给默认值）。</p>
 */
public class GenerateSlipDTO {

    @NotNull(message = "shiftId 不能为空")
    private Long shiftId;

    /** 可选：显式指定门店（跨门店班次/测试数据场景）；缺省走推导链 */
    private Long storeId;

    public Long getShiftId() { return shiftId; }
    public void setShiftId(Long shiftId) { this.shiftId = shiftId; }

    public Long getStoreId() { return storeId; }
    public void setStoreId(Long storeId) { this.storeId = storeId; }
}
