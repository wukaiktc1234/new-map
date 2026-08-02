package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存出库单明细实体类
 * 记录每个物料的申请出库数量、实际出库数量、批次号等
 */
@TableName("inventory_outbound_item")
public class InventoryOutboundItem {

    /**
     * 明细ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long outboundItemId;

    /**
     * 出库单ID
     */
    @TableField("outbound_id")
    private Long outboundId;

    /**
     * 物料ID
     */
    @TableField("material_id")
    private String materialId;

    /**
     * 物料名称（冗余字段）
     */
    @TableField("material_name")
    private String materialName;

    /**
     * 规格型号
     */
    @TableField("specification")
    private String specification;

    /**
     * 单位
     */
    @TableField("unit")
    private String unit;

    /**
     * 申请出库数量
     */
    @TableField("request_quantity")
    private BigDecimal requestQuantity;

    /**
     * 实际出库数量
     */
    @TableField("actual_quantity")
    private BigDecimal actualQuantity;

    /**
     * 批次号（先进先出）
     */
    @TableField("batch_no")
    private String batchNo;

    /**
     * 单价（分）
     */
    @TableField("unit_cost")
    private Long unitCost;

    /**
     * 小计金额（分）
     */
    @TableField("total_cost")
    private Long totalCost;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("update_time")
    private LocalDateTime updateTime;

    public Long getOutboundItemId() {
        return outboundItemId;
    }

    public void setOutboundItemId(Long outboundItemId) {
        this.outboundItemId = outboundItemId;
    }

    public Long getOutboundId() {
        return outboundId;
    }

    public void setOutboundId(Long outboundId) {
        this.outboundId = outboundId;
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

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getRequestQuantity() {
        return requestQuantity;
    }

    public void setRequestQuantity(BigDecimal requestQuantity) {
        this.requestQuantity = requestQuantity;
    }

    public BigDecimal getActualQuantity() {
        return actualQuantity;
    }

    public void setActualQuantity(BigDecimal actualQuantity) {
        this.actualQuantity = actualQuantity;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(Long unitCost) {
        this.unitCost = unitCost;
    }

    public Long getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(Long totalCost) {
        this.totalCost = totalCost;
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
}
