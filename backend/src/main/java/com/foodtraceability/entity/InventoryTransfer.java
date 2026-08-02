package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 调拨单实体类
 * 用于管理仓库之间的物料调拨操作
 */
@TableName("inventory_transfers")
public class InventoryTransfer {
    /**
     * 调拨单ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long transferId;

    /**
     * 调拨单号（唯一）
     */
    @TableField("transfer_code")
    private String transferCode;

    /**
     * 源仓库ID
     */
    @TableField("from_warehouse_id")
    private Long fromWarehouseId;

    /**
     * 目标仓库ID
     */
    @TableField("to_warehouse_id")
    private Long toWarehouseId;

    /**
     * 调拨状态（0:草稿 1:待审批 2:已审批 3:已完成 4:已取消/已拒绝）
     * 对应枚举 {@link com.foodtraceability.entity.enums.InventoryTransferStatus}
     */
    @TableField("transfer_status")
    private Integer transferStatus;

    /**
     * 调拨日期
     */
    @TableField("transfer_date")
    private LocalDate transferDate;

    /**
     * 接收日期
     */
    @TableField("receive_date")
    private LocalDate receiveDate;

    /**
     * 总数量
     */
    @TableField("total_quantity")
    private BigDecimal totalQuantity;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    private Long createUserId;

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

    /**
     * 逻辑删除标记（0:未删除 1:已删除）
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    // ==================== 兼容性字段（Service层使用）====================

    /** 产品ID（冗余） */
    @TableField("product_id")
    private Long productId;

    /** 产品名称（冗余） */
    @TableField("product_name")
    private String productName;

    /** 源仓库名称（冗余） */
    @TableField("from_warehouse_name")
    private String fromWarehouseName;

    /** 目标仓库名称（冗余） */
    @TableField("to_warehouse_name")
    private String toWarehouseName;

    /** 调拨数量（兼容性） */
    @TableField("transfer_quantity")
    private BigDecimal transferQuantity;

    public Long getTransferId() {
        return transferId;
    }

    public void setTransferId(Long transferId) {
        this.transferId = transferId;
    }

    public String getTransferCode() {
        return transferCode;
    }

    public void setTransferCode(String transferCode) {
        this.transferCode = transferCode;
    }

    public Long getFromWarehouseId() {
        return fromWarehouseId;
    }

    public void setFromWarehouseId(Long fromWarehouseId) {
        this.fromWarehouseId = fromWarehouseId;
    }

    public Long getToWarehouseId() {
        return toWarehouseId;
    }

    public void setToWarehouseId(Long toWarehouseId) {
        this.toWarehouseId = toWarehouseId;
    }

    public Integer getTransferStatus() {
        return transferStatus;
    }

    public void setTransferStatus(Integer transferStatus) {
        this.transferStatus = transferStatus;
    }

    public LocalDate getTransferDate() {
        return transferDate;
    }

    public void setTransferDate(LocalDate transferDate) {
        this.transferDate = transferDate;
    }

    public LocalDate getReceiveDate() {
        return receiveDate;
    }

    public void setReceiveDate(LocalDate receiveDate) {
        this.receiveDate = receiveDate;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Long getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(Long createUserId) {
        this.createUserId = createUserId;
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

    // ==================== 兼容性方法 ====================

    public Long getId() {
        return transferId;
    }

    public void setId(Long id) {
        this.transferId = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getFromWarehouseName() {
        return fromWarehouseName;
    }

    public void setFromWarehouseName(String fromWarehouseName) {
        this.fromWarehouseName = fromWarehouseName;
    }

    public String getToWarehouseName() {
        return toWarehouseName;
    }

    public void setToWarehouseName(String toWarehouseName) {
        this.toWarehouseName = toWarehouseName;
    }

    public BigDecimal getTransferQuantity() {
        return transferQuantity != null ? transferQuantity : totalQuantity;
    }

    public void setTransferQuantity(BigDecimal transferQuantity) {
        this.transferQuantity = transferQuantity;
        this.totalQuantity = transferQuantity;
    }

    public int getStatus() {
        return transferStatus != null ? transferStatus : 0;
    }

    public void setStatus(int status) {
        this.transferStatus = status;
    }

    public java.util.Date getCreatedAt() {
        return createTime != null ? java.sql.Timestamp.valueOf(createTime) : null;
    }

    public void setCreatedAt(java.util.Date createdAt) {
        this.createTime = createdAt != null ? createdAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
    }

    public java.util.Date getUpdatedAt() {
        return updateTime != null ? java.sql.Timestamp.valueOf(updateTime) : null;
    }

    public void setUpdatedAt(java.util.Date updatedAt) {
        this.updateTime = updatedAt != null ? updatedAt.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime() : null;
    }
}
