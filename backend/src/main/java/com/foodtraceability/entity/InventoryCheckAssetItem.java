package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 资产盘点明细实体类
 * 记录每个资产在盘点时的实际情况
 */
@TableName("inventory_check_asset_items")
public class InventoryCheckAssetItem {

    /** 明细ID */
    @TableId(type = IdType.AUTO)
    private Long itemId;

    /** 盘点单ID */
    private Long checkId;

    /** 资产ID */
    private Long assetId;

    /** 账面位置（预期位置） */
    private String expectedLocation;

    /** 实际位置（盘点时实际位置） */
    private String actualLocation;

    /** 账面状态 */
    private Integer expectedStatus;

    /** 实际状态 */
    private Integer actualStatus;

    /**
     * 成色评级
     * 1-优 2-良 3-中 4-差 5-报废
     */
    private Integer conditionRating;

    /** 备注 */
    private String remark;

    /** 照片证据URL */
    private String photoEvidence;

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

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public Long getCheckId() {
        return checkId;
    }

    public void setCheckId(Long checkId) {
        this.checkId = checkId;
    }

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getExpectedLocation() {
        return expectedLocation;
    }

    public void setExpectedLocation(String expectedLocation) {
        this.expectedLocation = expectedLocation;
    }

    public String getActualLocation() {
        return actualLocation;
    }

    public void setActualLocation(String actualLocation) {
        this.actualLocation = actualLocation;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Integer getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(Integer actualStatus) {
        this.actualStatus = actualStatus;
    }

    public Integer getConditionRating() {
        return conditionRating;
    }

    public void setConditionRating(Integer conditionRating) {
        this.conditionRating = conditionRating;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getPhotoEvidence() {
        return photoEvidence;
    }

    public void setPhotoEvidence(String photoEvidence) {
        this.photoEvidence = photoEvidence;
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
