package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 商品档案实体类
 * 对应数据库表: material_archives
 * 用于维护采购物料/商品的基础信息
 */
@TableName("material_archives")
public class MaterialArchive {

    /** 商品ID（自增主键） */
    @TableId(type = IdType.AUTO)
    @Schema(description = "商品ID")
    private Long materialId;

    /** 商品编码（业务唯一） */
    @TableField("material_code")
    @Schema(description = "商品编码", example = "MAT20260629001")
    private String materialCode;

    /** 商品名称 */
    @TableField("material_name")
    @Schema(description = "商品名称", example = "番茄")
    private String materialName;

    /** 分类ID（关联 material_categories） */
    @TableField("category_id")
    @Schema(description = "分类ID")
    private Long categoryId;

    /** 单位 */
    @TableField("unit")
    @Schema(description = "单位", example = "斤")
    private String unit;

    /** 规格型号 */
    @TableField("spec")
    @Schema(description = "规格型号", example = "10kg/箱")
    private String spec;

    /** 参考价（分） */
    @TableField("reference_price")
    @Schema(description = "参考价（分）", example = "500")
    private Long referencePrice;
    /** 条码 */
    private String barcode;
    /** 产地 */
    private String origin;
    /** 保质期 */
    private String shelfLife;
    /** 存储条件 */
    private String storageCondition;
    /** 使用部门ID（NULL=通用物料，所有部门可见） */
    private Long departmentId;

    /** 主供应商ID（关联 suppliers） */
    @TableField("supplier_id")
    @Schema(description = "主供应商ID")
    private Long supplierId;

    /** 状态：1启用 0停用 */
    @TableField("status")
    @Schema(description = "状态: 1启用 0停用", example = "1")
    private Integer status;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除: 0未删除 1已删除")
    private Integer deleted;

    // ===== Getter / Setter =====

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }

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
    public void setStorageCondition(String storageCondition) { this.storageCondition = storageCondition; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
    public void setReferencePrice(Long referencePrice) { this.referencePrice = referencePrice; }

    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
