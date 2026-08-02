package com.foodtraceability.dto.purchase;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

/**
 * 商品档案更新 DTO
 * 所有字段可选（部分更新）
 */
@Schema(description = "商品档案更新请求")
public class MaterialArchiveUpdateDTO {

    /** 商品名称 */
    @Size(max = 200, message = "商品名称长度不能超过200字")
    @Schema(description = "商品名称", example = "番茄")
    private String materialName;

    /** 分类ID */
    @Schema(description = "分类ID", example = "1")
    private Long categoryId;

    /** 单位 */
    @Size(max = 50, message = "单位长度不能超过50字")
    @Schema(description = "单位", example = "斤")
    private String unit;

    /** 规格型号 */
    @Size(max = 200, message = "规格长度不能超过200字")
    @Schema(description = "规格型号", example = "10kg/箱")
    private String spec;

    /** 参考价（分） */
    @Min(value = 0, message = "参考价不能为负数")
    @Schema(description = "参考价（分）", example = "500")
    private Long referencePrice;
    private String barcode;
    private String origin;
    private String shelfLife;
    private String storageCondition;
    private Long departmentId;

    /** 主供应商ID */
    @Schema(description = "主供应商ID", example = "1")
    private Long supplierId;

    /** 备注 */
    @Size(max = 500, message = "备注长度不能超过500字")
    @Schema(description = "备注")
    private String remark;

    // ===== Getter / Setter =====

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getSpec() { return spec; }
    public void setSpec(String spec) { this.spec = spec; }

    public Long getReferencePrice() { return referencePrice; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getShelfLife() { return shelfLife; }
    public void setShelfLife(String shelfLife) { this.shelfLife = shelfLife; }
    public String getStorageCondition() { return storageCondition; }
    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setStorageCondition(String storageCondition) { this.storageCondition = storageCondition; }
    public void setReferencePrice(Long referencePrice) { this.referencePrice = referencePrice; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
