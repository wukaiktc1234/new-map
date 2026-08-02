package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 资产变动记录实体类
 * 记录资产的所有变动历史（购置、折旧、调拨、维修、报废、处置等）
 */
@TableName("asset_flow_records")
public class AssetFlowRecordNew {

    /** 变动记录ID */
    @TableId(type = IdType.AUTO)
    private Long flowId;

    /** 资产ID */
    private Long assetId;

    /**
     * 变动类型
     * 1-购置 2-折旧 3-调拨 4-维修 5-报废 6-处置 7-盘点盈余 8-盘点亏损
     */
    private Integer flowType;

    /** 变动前值（分） */
    private Long beforeValue;

    /** 变动后值（分） */
    private Long afterValue;

    /** 变动金额（分） */
    private Long changeAmount;

    /** 关联单据号 */
    private String referenceNo;

    /** 变动日期 */
    private LocalDate flowDate;

    /** 操作人ID */
    private Long operatorId;

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

    public Long getFlowId() {
        return flowId;
    }

    public void setFlowId(Long flowId) {
        this.flowId = flowId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Integer getFlowType() {
        return flowType;
    }

    public void setFlowType(Integer flowType) {
        this.flowType = flowType;
    }

    public Long getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(Long beforeValue) {
        this.beforeValue = beforeValue;
    }

    public Long getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(Long afterValue) {
        this.afterValue = afterValue;
    }

    public Long getChangeAmount() {
        return changeAmount;
    }

    public void setChangeAmount(Long changeAmount) {
        this.changeAmount = changeAmount;
    }

    public String getReferenceNo() {
        return referenceNo;
    }

    public void setReferenceNo(String referenceNo) {
        this.referenceNo = referenceNo;
    }

    public LocalDate getFlowDate() {
        return flowDate;
    }

    public void setFlowDate(LocalDate flowDate) {
        this.flowDate = flowDate;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
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
