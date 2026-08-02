package com.foodtraceability.dto.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 商品档案 VO（视图对象）
 * 返回给前端的完整数据，包含关联表字段（categoryName、supplierName）和状态中文名
 */
@Schema(description = "商品档案视图对象")
public class MaterialArchiveVO {

    /** 商品ID */
    @Schema(description = "商品ID")
    private Long materialId;

    /** 商品编码 */
    @Schema(description = "商品编码")
    private String materialCode;

    /** 商品名称 */
    @Schema(description = "商品名称")
    private String materialName;

    /** 分类ID */
    @Schema(description = "分类ID")
    private Long categoryId;

    /** 分类名称（关联查询） */
    @Schema(description = "分类名称")
    private String categoryName;

    /** 单位 */
    @Schema(description = "单位")
    private String unit;

    /** 规格型号 */
    @Schema(description = "规格型号")
    private String spec;

    /** 参考价（分）—— 前端 Converter 转元展示 */
    @Schema(description = "参考价（分）")
    private Long referencePrice;
    private String barcode;
    private String origin;
    private String shelfLife;
    private String storageCondition;
    private Long departmentId;

    /** 主供应商ID */
    @Schema(description = "主供应商ID")
    private Long supplierId;

    /** 主供应商名称（关联查询） */
    @Schema(description = "主供应商名称")
    private String supplierName;

    /** 状态：1启用 0停用 */
    @Schema(description = "状态: 1启用 0停用")
    private Integer status;

    /** 状态中文名 */
    @Schema(description = "状态中文名")
    private String statusName;

    /** 备注 */
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    // ===== Getter / Setter =====

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

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

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
