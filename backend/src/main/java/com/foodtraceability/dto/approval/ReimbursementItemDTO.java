package com.foodtraceability.dto.approval;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 报销项目明细DTO（内嵌DTO）
 * 作为ReimbursementCreateDTO的子元素
 * 描述每一笔报销费用的详细信息
 */
@Schema(description = "报销项目明细DTO")
public class ReimbursementItemDTO {

    /**
     * 费用类别
     * 交通/住宿/餐饮/办公/其他
     */
    @NotBlank(message = "费用类别不能为空")
    @Size(max = 50, message = "费用类别长度不能超过50个字符")
    @Schema(description = "费用类别", example = "交通",
            allowableValues = {"交通", "住宿", "餐饮", "办公", "其他"},
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String category;

    /** 费用说明 */
    @Size(max = 200, message = "费用说明长度不能超过200个字符")
    @Schema(description = "费用说明", example = "高铁二等座往返")
    private String description;

    /**
     * 金额（单位：元）
     * 前端传入元，后端转换为分存储到数据库
     */
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0.01元")
    @Schema(description = "金额（单位：元）", example = "553.00",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal amount;

    /** 票据/发票号码 */
    @Size(max = 50, message = "票据号长度不能超过50个字符")
    @Schema(description = "票据/发票号", example = "INV20260601001")
    private String receiptNo;

    /** 费用发生日期 */
    @Schema(description = "发生日期", example = "2026-06-05")
    private LocalDate occurDate;

    // ==================== Getter & Setter 方法 ====================

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public void setReceiptNo(String receiptNo) {
        this.receiptNo = receiptNo;
    }

    public LocalDate getOccurDate() {
        return occurDate;
    }

    public void setOccurDate(LocalDate occurDate) {
        this.occurDate = occurDate;
    }
}
