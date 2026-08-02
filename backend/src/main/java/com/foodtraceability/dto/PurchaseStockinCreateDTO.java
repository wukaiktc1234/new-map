package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 采购入库单创建DTO
 * 用于接收创建入库单时的请求数据
 */
@Schema(description = "采购入库单创建请求")
public class PurchaseStockinCreateDTO {

    /**
     * 关联的采购订单ID
     */
    @NotNull(message = "采购订单ID不能为空")
    @Schema(description = "采购订单ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orderId;

    /**
     * 入库仓库ID
     */
    @NotNull(message = "仓库ID不能为空")
    @Schema(description = "入库仓库ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long warehouseId;

    /**
     * 入库日期
     */
    @NotNull(message = "入库日期不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入库日期", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate stockinDate;

    /**
     * 入库类型（1正常入库 2退货入库 3赠品入库）
     */
    @Min(value = 1, message = "入库类型最小为1")
    @Max(value = 3, message = "入库类型最大为3")
    @Schema(description = "入库类型（1-正常入库, 2-退货入库, 3-赠品入库）", example = "1")
    private Integer stockinType = 1;

    /**
     * 入库明细列表
     */
    @Valid
    @NotEmpty(message = "入库明细不能为空")
    @Size(max = 50, message = "入库明细不能超过50条")
    @Schema(description = "入库明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PurchaseStockinItemDTO> items;

    /**
     * 备注
     */
    @Size(max = 500, message = "备注不能超过500个字符")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getStockinDate() {
        return stockinDate;
    }

    public void setStockinDate(LocalDate stockinDate) {
        this.stockinDate = stockinDate;
    }

    public Integer getStockinType() {
        return stockinType;
    }

    public void setStockinType(Integer stockinType) {
        this.stockinType = stockinType;
    }

    public List<PurchaseStockinItemDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseStockinItemDTO> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    /**
     * 计算总数量
     */
    public BigDecimal calculateTotalQuantity() {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PurchaseStockinItemDTO item : items) {
            if (item.getActualQuantity() != null) {
                total = total.add(item.getActualQuantity());
            }
        }
        return total;
    }

    /**
     * 计算总金额
     */
    public Long calculateTotalAmount() {
        if (items == null || items.isEmpty()) {
            return 0L;
        }
        long total = 0L;
        for (PurchaseStockinItemDTO item : items) {
            total += item.calculateAmount();
        }
        return total;
    }
}
