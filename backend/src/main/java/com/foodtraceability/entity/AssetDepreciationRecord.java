package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.time.LocalDateTime;

/**
 * 资产折旧记录实体类
 * 记录每个资产每月的折旧明细
 */
@TableName("asset_depreciation_records")
public class AssetDepreciationRecord {

    /** 折旧记录ID */
    @TableId(type = IdType.AUTO)
    private Long recordId;

    /** 资产ID */
    private Long assetId;

    /**
     * 折旧期间
     * 格式：YYYY-MM（如2026-04）
     */
    private String period;

    /** 原值（分） */
    private Long originalCost;

    /** 本期折旧额（分） */
    private Long thisPeriodDepreciation;

    /** 累计折旧（分） */
    private Long accumulatedDepreciation;

    /** 折旧后净值（分） */
    private Long netBookValueAfter;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

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

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Long getOriginalCost() {
        return originalCost;
    }

    public void setOriginalCost(Long originalCost) {
        this.originalCost = originalCost;
    }

    public Long getThisPeriodDepreciation() {
        return thisPeriodDepreciation;
    }

    public void setThisPeriodDepreciation(Long thisPeriodDepreciation) {
        this.thisPeriodDepreciation = thisPeriodDepreciation;
    }

    public Long getAccumulatedDepreciation() {
        return accumulatedDepreciation;
    }

    public void setAccumulatedDepreciation(Long accumulatedDepreciation) {
        this.accumulatedDepreciation = accumulatedDepreciation;
    }

    public Long getNetBookValueAfter() {
        return netBookValueAfter;
    }

    public void setNetBookValueAfter(Long netBookValueAfter) {
        this.netBookValueAfter = netBookValueAfter;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
