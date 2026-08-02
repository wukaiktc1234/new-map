package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 采购入库单实体类
 * 用于管理采购商品的入库验收信息
 */
@TableName("purchase_stockins")
@Schema(description = "采购入库单实体")
public class PurchaseStockin {

    /**
     * 入库单主键ID（自增）
     */
    @TableId(value = "stockin_id", type = IdType.AUTO)
    @Schema(description = "入库单主键ID", example = "1")
    private Long stockinId;

    /**
     * 入库单编号（对外唯一，如SI20260425001）
     */
    @TableField("stockin_code")
    @Schema(description = "入库单编号", example = "SI20260425001")
    private String stockinCode;

    /**
     * 关联的采购订单编号（非数据库字段，前端展示用）
     */
    @TableField(exist = false)
    @Schema(description = "关联的采购订单编号")
    private String orderNo;

    /**
     * 供应商名称（非数据库字段，前端展示用）
     */
    @TableField(exist = false)
    @Schema(description = "供应商名称")
    private String supplierName;

    /**
     * 关联的采购订单ID
     */
    @TableField("order_id")
    @Schema(description = "采购订单ID", example = "1")
    private Long orderId;

    /**
     * 供应商ID
     */
    @TableField("supplier_id")
    @Schema(description = "供应商ID", example = "1")
    private Long supplierId;

    /**
     * 入库仓库ID
     */
    @TableField("warehouse_id")
    @Schema(description = "入库仓库ID", example = "1")
    private Long warehouseId;

    /**
     * 入库日期
     */
    @TableField("stockin_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "入库日期")
    private LocalDate stockinDate;

    /**
     * 入库类型（1正常入库 2退货入库 3赠品入库）
     */
    @TableField("stockin_type")
    @Schema(description = "入库类型（1-正常入库, 2-退货入库, 3-赠品入库）", example = "1")
    private Integer stockinType;

    /**
     * 总数量
     */
    @TableField("total_quantity")
    @Schema(description = "总数量", example = "100.000")
    private BigDecimal totalQuantity;

    /**
     * 总金额（单位：分）
     */
    @TableField("total_amount")
    @Schema(description = "总金额（分）", example = "30000")
    private Long totalAmount;

    /**
     * 质检结果（1合格 2不合格 3待检）
     */
    @TableField("quality_check_result")
    @Schema(description = "质检结果（1-合格, 2-不合格, 3-待检）", example = "3")
    private Integer qualityCheckResult;

    /**
     * 质检人ID
     */
    @TableField("quality_check_user_id")
    @Schema(description = "质检人ID", example = "1")
    private Long qualityCheckUserId;

    /**
     * 质检时间
     */
    @TableField("quality_check_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "质检时间")
    private LocalDateTime qualityCheckTime;

    /**
     * 质检备注
     */
    @TableField("quality_remark")
    @Schema(description = "质检备注")
    private String qualityRemark;

    /**
     * 外观检查结果（normal正常 abnormal异常）
     */
    @TableField("appearance_result")
    @Schema(description = "外观检查结果")
    private String appearanceResult;

    /**
     * 气味检查结果（normal正常 abnormal异常）
     */
    @TableField("odor_result")
    @Schema(description = "气味检查结果")
    private String odorResult;

    /**
     * 实测温度（℃）
     */
    @TableField("temperature")
    @Schema(description = "实测温度(℃)")
    private BigDecimal temperature;

    /**
     * 实测湿度（%）
     */
    @TableField("humidity")
    @Schema(description = "实测湿度(%)")
    private BigDecimal humidity;

    /**
     * 抽检数量
     */
    @TableField("sample_quantity")
    @Schema(description = "抽检数量")
    private BigDecimal sampleQuantity;

    /**
     * 抽检比例（%）
     */
    @TableField("sample_rate")
    @Schema(description = "抽检比例(%)")
    private BigDecimal sampleRate;

    /**
     * 不合格类型
     */
    @TableField("unqualified_type")
    @Schema(description = "不合格类型")
    private String unqualifiedType;

    /**
     * 处理意见（return退货 concession让步接收 scrap报废 sort挑选使用）
     */
    @TableField("disposal_opinion")
    @Schema(description = "处理意见")
    private String disposalOpinion;

    /**
     * 随货单据核查结果（complete齐全 incomplete不齐全 none无）
     */
    @TableField("document_check")
    @Schema(description = "随货单据核查结果")
    private String documentCheck;

    /**
     * 状态（0待入库 1已入库 2部分入库 9已作废）
     */
    @TableField("status")
    @Schema(description = "状态（0-待入库, 1-已入库, 2-部分入库, 9-已作废）", example = "0")
    private Integer status;

    /**
     * 作废时间
     */
    @TableField("void_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "作废时间")
    private LocalDateTime voidTime;

    /**
     * 作废备注
     */
    @TableField("void_remark")
    @Schema(description = "作废备注")
    private String voidRemark;

    /**
     * 作废人ID
     */
    @TableField("void_by")
    @Schema(description = "作废人ID", example = "1")
    private Long voidBy;

    /**
     * 创建人ID
     */
    @TableField("create_user_id")
    @Schema(description = "创建人ID", example = "1")
    private Long createUserId;

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
     * 逻辑删除标记（0未删除 1已删除）
     */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "逻辑删除标记", example = "0")
    private Integer deleted;

    /**
     * 入库明细列表（非数据库字段）
     */
    @TableField(exist = false)
    @Schema(description = "入库明细列表")
    private java.util.List<PurchaseStockinItem> items;

    // ==================== Getter & Setter ====================

    public Long getStockinId() {
        return stockinId;
    }

    public void setStockinId(Long stockinId) {
        this.stockinId = stockinId;
    }

    public String getStockinCode() {
        return stockinCode;
    }

    public void setStockinCode(String stockinCode) {
        this.stockinCode = stockinCode;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public LocalDate getStockinDate() {
        return stockinDate;
    }

    public void setStockinDate(LocalDate stockinDate) {
        this.stockinDate = stockinDate;
    }

    public Integer getStockinType() {
        return stockinType;
    }

    public void setStockinType(Integer stockinType) {
        this.stockinType = stockinType;
    }

    public BigDecimal getTotalQuantity() {
        return totalQuantity;
    }

    public void setTotalQuantity(BigDecimal totalQuantity) {
        this.totalQuantity = totalQuantity;
    }

    public Long getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Long totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getQualityCheckResult() {
        return qualityCheckResult;
    }

    public void setQualityCheckResult(Integer qualityCheckResult) {
        this.qualityCheckResult = qualityCheckResult;
    }

    public Long getQualityCheckUserId() {
        return qualityCheckUserId;
    }

    public void setQualityCheckUserId(Long qualityCheckUserId) {
        this.qualityCheckUserId = qualityCheckUserId;
    }

    public LocalDateTime getQualityCheckTime() {
        return qualityCheckTime;
    }

    public void setQualityCheckTime(LocalDateTime qualityCheckTime) {
        this.qualityCheckTime = qualityCheckTime;
    }

    public String getQualityRemark() {
        return qualityRemark;
    }

    public void setQualityRemark(String qualityRemark) {
        this.qualityRemark = qualityRemark;
    }

    public String getAppearanceResult() {
        return appearanceResult;
    }

    public void setAppearanceResult(String appearanceResult) {
        this.appearanceResult = appearanceResult;
    }

    public String getOdorResult() {
        return odorResult;
    }

    public void setOdorResult(String odorResult) {
        this.odorResult = odorResult;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public BigDecimal getHumidity() {
        return humidity;
    }

    public void setHumidity(BigDecimal humidity) {
        this.humidity = humidity;
    }

    public BigDecimal getSampleQuantity() {
        return sampleQuantity;
    }

    public void setSampleQuantity(BigDecimal sampleQuantity) {
        this.sampleQuantity = sampleQuantity;
    }

    public BigDecimal getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(BigDecimal sampleRate) {
        this.sampleRate = sampleRate;
    }

    public String getUnqualifiedType() {
        return unqualifiedType;
    }

    public void setUnqualifiedType(String unqualifiedType) {
        this.unqualifiedType = unqualifiedType;
    }

    public String getDisposalOpinion() {
        return disposalOpinion;
    }

    public void setDisposalOpinion(String disposalOpinion) {
        this.disposalOpinion = disposalOpinion;
    }

    public String getDocumentCheck() {
        return documentCheck;
    }

    public void setDocumentCheck(String documentCheck) {
        this.documentCheck = documentCheck;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public LocalDateTime getVoidTime() {
        return voidTime;
    }

    public void setVoidTime(LocalDateTime voidTime) {
        this.voidTime = voidTime;
    }

    public String getVoidRemark() {
        return voidRemark;
    }

    public void setVoidRemark(String voidRemark) {
        this.voidRemark = voidRemark;
    }

    public Long getVoidBy() {
        return voidBy;
    }

    public void setVoidBy(Long voidBy) {
        this.voidBy = voidBy;
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

    public java.util.List<PurchaseStockinItem> getItems() {
        return items;
    }

    public void setItems(java.util.List<PurchaseStockinItem> items) {
        this.items = items;
    }
}
