package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 产品定价历史记录实体
 * 记录所有价格变动，用于审计和分析
 */
@TableName("product_pricing_history")
@Schema(description = "产品定价历史记录实体")
public class ProductPricingHistory {

    /** 定价记录ID */
    @TableId(value = "pricing_id", type = IdType.AUTO)
    @Schema(description = "定价记录ID", example = "1")
    private Long pricingId;

    /** 产品类型: FOOD-菜品, COMBO-套餐 */
    @TableField("product_type")
    @Schema(description = "产品类型: FOOD/COMBO", example = "FOOD")
    private String productType;

    /** 产品名称（冗余存储） */
    @TableField("product_name")
    @Schema(description = "产品名称", example = "红烧肉")
    private String productName;

    /** 产品ID */
    @TableField("product_id")
    @Schema(description = "产品ID", example = "1")
    private Long productId;

    /** 原售价（分） */
    @TableField("old_sale_price")
    @Schema(description = "原售价（分）", example = "3500")
    private Long oldSalePrice;

    /** 新售价（分） */
    @TableField("new_sale_price")
    @Schema(description = "新售价（分）", example = "3800")
    private Long newSalePrice;

    /** 成本价（分，快照） */
    @TableField("cost_price")
    @Schema(description = "成本价（分）", example = "1500")
    private Long costPrice;

    /** 定价策略: MANUAL-手动调整, AUTO-自动计算, BATCH-批量调价 */
    @TableField("pricing_strategy")
    @Schema(description = "定价策略", example = "MANUAL")
    private String pricingStrategy;

    /** 备注 */
    @TableField("remark")
    @Schema(description = "备注")
    private String remark;

    /** 操作人ID */
    @TableField("operator_id")
    @Schema(description = "操作人ID", example = "1")
    private Long operatorId;

    /** 操作人姓名 */
    @TableField("operator_name")
    @Schema(description = "操作人姓名", example = "张三")
    private String operatorName;

    /** 生效日期 */
    @TableField("effective_date")
    @Schema(description = "生效日期")
    private LocalDate effectiveDate;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记（0-正常，1-删除） */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除: 0-未删除, 1-已删除")
    private Integer deleted;

    // Getter和Setter方法
    public Long getPricingId() { return pricingId; }
    public void setPricingId(Long pricingId) { this.pricingId = pricingId; }

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Long getOldSalePrice() { return oldSalePrice; }
    public void setOldSalePrice(Long oldSalePrice) { this.oldSalePrice = oldSalePrice; }

    public Long getNewSalePrice() { return newSalePrice; }
    public void setNewSalePrice(Long newSalePrice) { this.newSalePrice = newSalePrice; }

    public Long getCostPrice() { return costPrice; }
    public void setCostPrice(Long costPrice) { this.costPrice = costPrice; }

    public String getPricingStrategy() { return pricingStrategy; }
    public void setPricingStrategy(String pricingStrategy) { this.pricingStrategy = pricingStrategy; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }

    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }

    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }

    public LocalDate getEffectiveDate() { return effectiveDate; }
    public void setEffectiveDate(LocalDate effectiveDate) { this.effectiveDate = effectiveDate; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    public Integer getDeleted() { return deleted; }
    public void setDeleted(Integer deleted) { this.deleted = deleted; }
}
