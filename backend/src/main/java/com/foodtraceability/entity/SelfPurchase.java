package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自采记录实体类
 * 用于管理餐饮企业的自采业务
 */
@TableName("self_purchase")
@Schema(description = "自采记录实体")
public class SelfPurchase {
    
    /**
     * 自采记录ID，主键
     */
    @TableId(value = "self_purchase_id", type = IdType.ASSIGN_ID)
    @Schema(description = "自采记录ID", example = "SP20251205001")
    private String selfPurchaseId;
    
    /**
     * 自采编号
     */
    @TableField("purchase_number")
    @Schema(description = "自采编号", example = "SP202512050001")
    private String purchaseNumber;
    
    /**
     * 自采类型（emergency-紧急自采，small-小额自采，special-特殊自采）
     */
    @TableField("purchase_type")
    @Schema(description = "自采类型", example = "emergency")
    private String purchaseType;
    
    /**
     * 自采原因
     */
    @TableField("purchase_reason")
    @Schema(description = "自采原因", example = "临时需要的食材，直接从市场采购")
    private String purchaseReason;
    
    /**
     * 自采总金额
     */
    @TableField("total_amount")
    @Schema(description = "自采总金额", example = "500.00")
    private BigDecimal totalAmount;
    
    /**
     * 自采日期
     */
    @TableField("purchase_date")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "自采日期")
    private LocalDateTime purchaseDate;
    
    /**
     * 采购人
     */
    @TableField("purchaser")
    @Schema(description = "采购人", example = "张三")
    private String purchaser;
    
    /**
     * 采购人电话
     */
    @TableField("purchaser_phone")
    @Schema(description = "采购人电话", example = "13800138000")
    private String purchaserPhone;
    
    /**
     * 自采地点
     */
    @TableField("purchase_location")
    @Schema(description = "自采地点", example = "本地农贸市场")
    private String purchaseLocation;
    
    /**
     * 仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "仓库ID", example = "1")
    private Long warehouseId;
    
    /**
     * 仓库名称
     */
    @TableField("warehouse_name")
    @Schema(description = "仓库名称", example = "主仓库")
    private String warehouseName;
    
    /**
     * 入库状态（pending-待入库，completed-已入库）
     */
    @TableField("status")
    @Schema(description = "入库状态", example = "pending")
    private String status;
    
    /**
     * 报销状态（unreimbursed-未报销，reimbursed-已报销）
     */
    @TableField("reimburse_status")
    @Schema(description = "报销状态", example = "unreimbursed")
    private String reimburseStatus;
    
    /**
     * 备注
     */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;
    
    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    /**
     * 创建人
     */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @Schema(description = "创建人", example = "system")
    private String createBy;
    
    /**
     * 更新人
     */
    @TableField(value = "update_by", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新人", example = "admin")
    private String updateBy;
    
    /**
     * 逻辑删除标记（0-正常，1-删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记", example = "0")
    private Integer deleted;

    // Getter and Setter methods
    public String getSelfPurchaseId() {
        return selfPurchaseId;
    }

    public void setSelfPurchaseId(String selfPurchaseId) {
        this.selfPurchaseId = selfPurchaseId;
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public String getPurchaseType() {
        return purchaseType;
    }

    public void setPurchaseType(String purchaseType) {
        this.purchaseType = purchaseType;
    }

    public String getPurchaseReason() {
        return purchaseReason;
    }

    public void setPurchaseReason(String purchaseReason) {
        this.purchaseReason = purchaseReason;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public LocalDateTime getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDateTime purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getPurchaser() {
        return purchaser;
    }

    public void setPurchaser(String purchaser) {
        this.purchaser = purchaser;
    }

    public String getPurchaserPhone() {
        return purchaserPhone;
    }

    public void setPurchaserPhone(String purchaserPhone) {
        this.purchaserPhone = purchaserPhone;
    }

    public String getPurchaseLocation() {
        return purchaseLocation;
    }

    public void setPurchaseLocation(String purchaseLocation) {
        this.purchaseLocation = purchaseLocation;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReimburseStatus() {
        return reimburseStatus;
    }

    public void setReimburseStatus(String reimburseStatus) {
        this.reimburseStatus = reimburseStatus;
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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
