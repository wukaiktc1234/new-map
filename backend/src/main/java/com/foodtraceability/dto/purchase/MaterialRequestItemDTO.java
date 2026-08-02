package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 物资需求提报明细 DTO（用于 CreateDTO/UpdateDTO 内嵌）
 *
 * <p>金额单位：estimatedPrice 以"分"为单位（Long），由前端 DataConverter 转换。</p>
 */
@Schema(description = "物资需求提报明细")
public class MaterialRequestItemDTO {

    /** 物料ID（关联 material_archives，临时物料为空） */
    @Schema(description = "物料ID")
    private Long materialId;

    /** 物料名称 */
    @NotBlank(message = "物料名称不能为空")
    @Size(max = 200, message = "物料名称长度不能超过200")
    @Schema(description = "物料名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String materialName;

    /** 规格型号 */
    @Size(max = 100, message = "规格型号长度不能超过100")
    @Schema(description = "规格型号")
    private String specification;

    /** 数量 */
    @NotNull(message = "数量不能为空")
    @DecimalMin(value = "0.001", message = "数量必须大于0")
    @Digits(integer = 9, fraction = 3, message = "数量格式不正确")
    @Schema(description = "数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantity;

    /** 单位（kg/斤/袋等） */
    @NotBlank(message = "单位不能为空")
    @Size(max = 20, message = "单位长度不能超过20")
    @Schema(description = "单位", requiredMode = Schema.RequiredMode.REQUIRED)
    private String unit;

    /** 预估单价（单位：分） */
    @NotNull(message = "预估单价不能为空")
    @Min(value = 0, message = "预估单价不能为负数")
    @Schema(description = "预估单价（分）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long estimatedPrice;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500")
    @Schema(description = "备注")
    private String remark;

    // ==================== Getter & Setter ====================

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Long getEstimatedPrice() {
        return estimatedPrice;
    }

    public void setEstimatedPrice(Long estimatedPrice) {
        this.estimatedPrice = estimatedPrice;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
