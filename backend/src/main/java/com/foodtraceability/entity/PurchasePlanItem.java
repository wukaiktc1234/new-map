package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购计划明细实体
 * 对应数据库表: purchase_plan_item
 *
 * <p>数量字段 quantity 使用 DECIMAL(12,3) 支持小数（如 1.5 kg）。</p>
 * <p>预估单价 estimated_price 以"分"为单位（Long），前端展示时转换为"元"。</p>
 */
@TableName("purchase_plan_item")
@Schema(description = "采购计划明细")
public class PurchasePlanItem {

    /** 明细ID（主键，自增） */
    @TableId(value = "item_id", type = IdType.AUTO)
    @Schema(description = "明细ID")
    private Long itemId;

    /** 采购计划ID（关联 purchase_plan.plan_id） */
    @TableField("plan_id")
    @Schema(description = "采购计划ID")
    private Long planId;

    /** 物料ID（关联 material_archives，临时物料为空） */
    @TableField("material_id")
    @Schema(description = "物料ID")
    private String materialId;

    /** 物料名称 */
    @TableField("material_name")
    @Schema(description = "物料名称")
    private String materialName;

    /** 规格型号 */
    @TableField("specification")
    @Schema(description = "规格型号")
    private String specification;

    /** 数量（DECIMAL 支持小数） */
    @TableField("quantity")
    @Schema(description = "数量")
    private BigDecimal quantity;

    /** 单位（kg/斤/袋等） */
    @TableField("unit")
    @Schema(description = "单位")
    private String unit;

    /** 预估单价（单位：分） */
    @TableField("estimated_price")
    @Schema(description = "预估单价（分）")
    private Long estimatedPrice;

    /** 是否临时物料：0否 1是 */
    @TableField("is_temp_material")
    @Schema(description = "是否临时物料")
    private Integer isTempMaterial;

    /** 建议供应商ID（关联 suppliers） */
    @TableField("supplier_id")
    @Schema(description = "建议供应商ID")
    private Long supplierId;

    /** 建议供应商名称（冗余） */
    @TableField("supplier_name")
    @Schema(description = "建议供应商名称")
    private String supplierName;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 创建时间 */
    @TableField("create_time")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField("update_time")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除：0未删除 1已删除 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除")
    private Integer deleted;

    // ==================== Getter & Setter ====================

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public String getMaterialId() {
        return materialId;
    }

    public void setMaterialId(String materialId) {
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

    public Integer getIsTempMaterial() {
        return isTempMaterial;
    }

    public void setIsTempMaterial(Integer isTempMaterial) {
        this.isTempMaterial = isTempMaterial;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
