package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 套餐实体类
 * 用于管理餐厅套餐信息，支持有效期、限量等特性
 */
@TableName("dish_combos")
@Schema(description = "套餐实体")
public class DishComboNew {

    /** 套餐ID，主键自增 */
    @TableId(value = "combo_id", type = IdType.AUTO)
    @Schema(description = "套餐ID", example = "1")
    private Long comboId;

    /** 套餐编码，唯一 */
    @TableField("combo_code")
    @Schema(description = "套餐编码", example = "CB001")
    private String comboCode;

    /** 套餐名称 */
    @TableField("combo_name")
    @Schema(description = "套餐名称", example = "超值双人餐")
    private String comboName;

    /** 套餐价格（分） */
    @TableField("combo_price")
    @Schema(description = "套餐价格（分）", example = "8800")
    private Long comboPrice;

    /** 原价（各单品之和，用于显示优惠） */
    @TableField("original_price")
    @Schema(description = "原价（分）", example = "12000")
    private Long originalPrice;

    /** 优惠金额（分） */
    @TableField("discount_amount")
    @Schema(description = "优惠金额（分）", example = "3200")
    private Long discountAmount;

    /** 套餐图片URL */
    @TableField("image_url")
    @Schema(description = "套餐图片URL")
    private String imageUrl;

    /** 套餐描述 */
    @TableField("description")
    @Schema(description = "套餐描述")
    private String description;

    /** 有效期开始日期 */
    @TableField("valid_start_date")
    @Schema(description = "有效期开始日期")
    private LocalDate validStartDate;

    /** 有效期结束日期 */
    @TableField("valid_end_date")
    @Schema(description = "有效期结束日期")
    private LocalDate validEndDate;

    /** 每日限量（NULL表示不限量） */
    @TableField("daily_limit")
    @Schema(description = "每日限量", example = "50")
    private Integer dailyLimit;

    /** 今日已售数量 */
    @TableField("sold_today")
    @Schema(description = "今日已售", example = "10")
    private Integer soldToday;

    /** 状态：1在售 0停售（遵循项目统一状态标准） */
    @TableField("status")
    @Schema(description = "状态: 1在售 0停售", example = "1")
    private Integer status;

    /** 排序权重 */
    @TableField("sort_order")
    @Schema(description = "排序权重", example = "0")
    private Integer sortOrder;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /** 乐观锁版本号 */
    @Version
    @TableField("version")
    @Schema(description = "乐观锁版本号", example = "0")
    private Integer version;

    // Getter方法
    public Long getComboId() { return comboId; }
    public String getComboCode() { return comboCode; }
    public String getComboName() { return comboName; }
    public Long getComboPrice() { return comboPrice; }
    public Long getOriginalPrice() { return originalPrice; }
    public Long getDiscountAmount() { return discountAmount; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public LocalDate getValidStartDate() { return validStartDate; }
    public LocalDate getValidEndDate() { return validEndDate; }
    public Integer getDailyLimit() { return dailyLimit; }
    public Integer getSoldToday() { return soldToday; }
    public Integer getStatus() { return status; }
    public Integer getSortOrder() { return sortOrder; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public Integer getDeleted() { return deleted; }
    public Integer getVersion() { return version; }

    // Setter方法
    public void setComboId(Long comboId) { this.comboId = comboId; }
    public void setComboCode(String comboCode) { this.comboCode = comboCode; }
    public void setComboName(String comboName) { this.comboName = comboName; }
    public void setComboPrice(Long comboPrice) { this.comboPrice = comboPrice; }
    public void setOriginalPrice(Long originalPrice) { this.originalPrice = originalPrice; }
    public void setDiscountAmount(Long discountAmount) { this.discountAmount = discountAmount; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDescription(String description) { this.description = description; }
    public void setValidStartDate(LocalDate validStartDate) { this.validStartDate = validStartDate; }
    public void setValidEndDate(LocalDate validEndDate) { this.validEndDate = validEndDate; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public void setSoldToday(Integer soldToday) { this.soldToday = soldToday; }
    public void setStatus(Integer status) { this.status = status; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
    public void setVersion(Integer version) { this.version = version; }
}
