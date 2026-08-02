package com.foodtraceability.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

/**
 * 采购退货单创建/更新DTO
 */
@Schema(description = "采购退货单创建请求")
public class PurchaseReturnCreateDTO {

    @NotNull(message = "原入库单ID不能为空")
    @Schema(description = "关联原入库单ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long stockinId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "退货日期，默认当天")
    private LocalDate returnDate;

    @Schema(description = "退款方式：offset(冲抵) / cash(现金退款)，默认offset")
    private String refundMethod;

    @Schema(description = "备注")
    private String remark;

    @Valid
    @NotEmpty(message = "退货明细不能为空")
    @Size(max = 50, message = "退货明细不能超过50条")
    @Schema(description = "退货明细列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<PurchaseReturnItemCreateDTO> items;

    public Long getStockinId() {
        return stockinId;
    }

    public void setStockinId(Long stockinId) {
        this.stockinId = stockinId;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public String getRefundMethod() {
        return refundMethod;
    }

    public void setRefundMethod(String refundMethod) {
        this.refundMethod = refundMethod;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public List<PurchaseReturnItemCreateDTO> getItems() {
        return items;
    }

    public void setItems(List<PurchaseReturnItemCreateDTO> items) {
        this.items = items;
    }
}
