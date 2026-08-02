package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 开闭店记录实体类
 * 记录门店每日的开店和闭店操作及检查结果
 */
@TableName("open_close_store_records")
public class OpenCloseStoreRecord {

    /** 记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 门店ID */
    private Long storeId;

    /**
     * 记录类型
     * 1-开店 2-闭店
     */
    private Integer recordType;

    /** 操作人ID */
    private Long operatorId;

    /** 操作时间 */
    private LocalDateTime operateTime;

    /**
     * 开机现金（分）
     * 仅开店时填写
     */
    private BigDecimal openingCashCount;

    /** 库存检查结果 */
    private String inventoryCheckResult;

    /** 设备检查结果 */
    private String equipmentCheckResult;

    /** 安全检查结果 */
    private String safetyCheckResult;

    /**
     * 异常项（JSON格式）
     * 记录检查中发现的问题
     */
    private String abnormalItems;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    private Integer deleted;

    // ==================== Getter & Setter 方法 ====================

    public Long getRecordId() {
        return recordId;
    }

    public void setRecordId(Long recordId) {
        this.recordId = recordId;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Integer getRecordType() {
        return recordType;
    }

    public void setRecordType(Integer recordType) {
        this.recordType = recordType;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public LocalDateTime getOperateTime() {
        return operateTime;
    }

    public void setOperateTime(LocalDateTime operateTime) {
        this.operateTime = operateTime;
    }

    public BigDecimal getOpeningCashCount() {
        return openingCashCount;
    }

    public void setOpeningCashCount(BigDecimal openingCashCount) {
        this.openingCashCount = openingCashCount;
    }

    public String getInventoryCheckResult() {
        return inventoryCheckResult;
    }

    public void setInventoryCheckResult(String inventoryCheckResult) {
        this.inventoryCheckResult = inventoryCheckResult;
    }

    public String getEquipmentCheckResult() {
        return equipmentCheckResult;
    }

    public void setEquipmentCheckResult(String equipmentCheckResult) {
        this.equipmentCheckResult = equipmentCheckResult;
    }

    public String getSafetyCheckResult() {
        return safetyCheckResult;
    }

    public void setSafetyCheckResult(String safetyCheckResult) {
        this.safetyCheckResult = safetyCheckResult;
    }

    public String getAbnormalItems() {
        return abnormalItems;
    }

    public void setAbnormalItems(String abnormalItems) {
        this.abnormalItems = abnormalItems;
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
