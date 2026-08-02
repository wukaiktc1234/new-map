package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 盘点明细实体类
 * 用于存储盘点单的明细信息，包括账面数量和实盘数量
 */
@TableName("inventory_check_items")
public class InventoryCheckItem {
    /**
     * 盘点明细ID（主键，自增）
     */
    @TableId(type = IdType.AUTO)
    private Long checkItemId;

    /**
     * 盘点单ID
     */
    @TableField("check_id")
    private Long checkId;

    /**
     * 库存ID
     */
    @TableField("inventory_id")
    private Long inventoryId;

    /**
     * 账面数量
     */
    @TableField("book_qty")
    private BigDecimal bookQty;

    /**
     * 实盘数量
     */
    @TableField("actual_qty")
    private BigDecimal actualQty;

    /**
     * 差异数量（实盘 - 账面）
     */
    @TableField("diff_qty")
    private BigDecimal diffQty;

    /**
     * 差异金额（分）
     */
    @TableField("diff_amount")
    private Long diffAmount;

    /**
     * 差异原因
     */
    @TableField("reason")
    private String reason;

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

    public Long getCheckItemId() {
        return checkItemId;
    }

    public void setCheckItemId(Long checkItemId) {
        this.checkItemId = checkItemId;
    }

    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }

    public Long getInventoryId() {
        return inventoryId;
    }

    public void setInventoryId(Long inventoryId) {
        this.inventoryId = inventoryId;
    }

    public BigDecimal getBookQty() {
        return bookQty;
    }

    public void setBookQty(BigDecimal bookQty) {
        this.bookQty = bookQty;
    }

    public BigDecimal getActualQty() {
        return actualQty;
    }

    public void setActualQty(BigDecimal actualQty) {
        this.actualQty = actualQty;
    }

    public BigDecimal getDiffQty() {
        return diffQty;
    }

    public void setDiffQty(BigDecimal diffQty) {
        this.diffQty = diffQty;
    }

    public Long getDiffAmount() {
        return diffAmount;
    }

    public void setDiffAmount(Long diffAmount) {
        this.diffAmount = diffAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
