package com.foodtraceability.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 食品追溯码实体类
 * 系统核心表，记录每个可追溯单元的唯一标识和完整链路信息
 * 支持正向追溯（扫码查来源）和反向召回（问题原料追踪）
 */
@TableName("food_trace_codes")
@Schema(description = "食品追溯码实体")
public class TraceCode {

    /** 主键ID */
    @TableId(value = "trace_code_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long traceCodeId;

    /** 追溯码（唯一编码，如TC20260425001或二维码内容） */
    @TableField("trace_code")
    @Schema(description = "追溯码", example = "TC20260425001")
    private String traceCode;

    /** 追溯类型：1菜品追溯码 2原料批次追溯码 3物流追溯码 4检验报告码 */
    @TableField("trace_type")
    @Schema(description = "追溯类型", example = "1")
    private Integer traceType;

    /** 目标类型：1成品菜品 2半成品 3原材料 4包装材料 */
    @TableField("target_type")
    @Schema(description = "目标类型", example = "1")
    private Integer targetType;

    /** 关联的目标ID（food_id或material_id） */
    @TableField("target_id")
    @Schema(description = "目标ID")
    private Long targetId;

    /** 目标名称（冗余存储方便查询） */
    @TableField("target_name")
    @Schema(description = "目标名称", example = "红烧牛肉面")
    private String targetName;

    /** 批次号 */
    @TableField("batch_no")
    @Schema(description = "批次号", example = "BATCH20260420")
    private String batchNo;

    /** 生产/加工日期 */
    @TableField("production_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "生产日期")
    private LocalDate productionDate;

    /** 有效期/保质期至 */
    @TableField("expiry_date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "有效期至")
    private LocalDate expiryDate;

    /** 供应商ID（来自采购系统） */
    @TableField("supplier_id")
    @Schema(description = "供应商ID")
    private Long supplierId;

    /** 供应商名称（冗余存储） */
    @TableField("supplier_name")
    @Schema(description = "供应商名称", example = "绿色农场")
    private String supplierName;

    /** 入库单ID（来自采购系统） */
    @TableField("purchase_stockin_id")
    @Schema(description = "入库单ID")
    private Long purchaseStockinId;

    /** 所在仓库ID（来自仓储系统） */
    @TableField("warehouse_id")
    @Schema(description = "仓库ID")
    private Long warehouseId;

    /** 当前位置（仓库/加工间/餐桌/已售出） */
    @TableField("current_location")
    @Schema(description = "当前位置", example = "中央厨房冷库A区")
    private String currentLocation;

    /** 状态：1正常 2即将过期 3已过期 4已召回 5已消费 */
    @TableField("status")
    @Schema(description = "状态", example = "1")
    private Integer status;

    /** 风险等级：1低风险 2中风险 3高风险 */
    @TableField("risk_level")
    @Schema(description = "风险等级", example = "1")
    private Integer riskLevel;

    /** 二维码图片URL */
    @TableField("qr_code_image_url")
    @Schema(description = "二维码图片URL")
    private String qrCodeImageUrl;

    /** 追溯链数据JSON（关键！存储完整链条信息） */
    @TableField("chain_data")
    @Schema(description = "追溯链数据")
    private String chainData;

    /** 额外信息（如检测报告编号） */
    @TableField("extra_info")
    @Schema(description = "额外信息")
    private String extraInfo;

    /** 创建用户ID */
    @TableField("create_user_id")
    @Schema(description = "创建用户ID")
    private Long createUserId;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /** 逻辑删除标记 */
    @TableLogic
    @TableField("deleted")
    @Schema(description = "删除标记")
    private Integer deleted;

    // ==================== 非数据库映射字段（@TableField(exist = false)） ====================

    /** 仓库名称（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "仓库名称", hidden = true)
    private String warehouseName;

    /** 创建人姓名（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "创建人", hidden = true)
    private String createBy;

    /** 更新人姓名（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "更新人", hidden = true)
    private String updateBy;

    /** 出库时间（业务计算字段，不持久化） */
    @TableField(exist = false)
    @Schema(description = "出库时间", hidden = true)
    private LocalDateTime outboundTime;

    /** 使用者/接收方（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "使用者", hidden = true)
    private String usedBy;

    /** 使用用途（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "使用用途", hidden = true)
    private String usagePurpose;

    /** 入库时间（业务计算字段，不持久化） */
    @TableField(exist = false)
    @Schema(description = "入库时间", hidden = true)
    private LocalDateTime inboundTime;

    /** 库存类型（1:原料 2:半成品 3:成品 4:包装材料，冗余显示） */
    @TableField(exist = false)
    @Schema(description = "库存类型", hidden = true)
    private Integer inventoryType;

    /** 源仓库ID（调拨场景，不持久化） */
    @TableField(exist = false)
    @Schema(description = "源仓库ID", hidden = true)
    private String sourceWarehouseId;

    /** 目标仓库ID（调拨场景，不持久化） */
    @TableField(exist = false)
    @Schema(description = "目标仓库ID", hidden = true)
    private String targetWarehouseId;

    /** 操作员姓名（冗余显示，不持久化） */
    @TableField(exist = false)
    @Schema(description = "操作员姓名", hidden = true)
    private String operatorName;

    public Long getTraceCodeId() {
        return traceCodeId;
    }

    public void setTraceCodeId(Long traceCodeId) {
        this.traceCodeId = traceCodeId;
    }

    public String getTraceCode() {
        return traceCode;
    }

    public void setTraceCode(String traceCode) {
        this.traceCode = traceCode;
    }

    public Integer getTraceType() {
        return traceType;
    }

    public void setTraceType(Integer traceType) {
        this.traceType = traceType;
    }

    public Integer getTargetType() {
        return targetType;
    }

    public void setTargetType(Integer targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public LocalDate getProductionDate() {
        return productionDate;
    }

    public void setProductionDate(LocalDate productionDate) {
        this.productionDate = productionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Long getPurchaseStockinId() {
        return purchaseStockinId;
    }

    public void setPurchaseStockinId(Long purchaseStockinId) {
        this.purchaseStockinId = purchaseStockinId;
    }

    public Long getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(Long warehouseId) {
        this.warehouseId = warehouseId;
    }

    public String getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(String currentLocation) {
        this.currentLocation = currentLocation;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(Integer riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getQrCodeImageUrl() {
        return qrCodeImageUrl;
    }

    public void setQrCodeImageUrl(String qrCodeImageUrl) {
        this.qrCodeImageUrl = qrCodeImageUrl;
    }

    public String getChainData() {
        return chainData;
    }

    public void setChainData(String chainData) {
        this.chainData = chainData;
    }

    public String getExtraInfo() {
        return extraInfo;
    }

    public void setExtraInfo(String extraInfo) {
        this.extraInfo = extraInfo;
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

    public void setCode(String code) {
        this.traceCode = code;
    }

    public String getCode() {
        return traceCode;
    }

    public void setProductId(Long productId) {
        this.targetId = productId;
    }

    public Long getProductId() {
        return targetId;
    }

    public void setSourceType(String sourceType) {
        // sourceType 映射到 traceType（String→Integer转换）
        if (sourceType != null) {
            try {
                this.traceType = Integer.parseInt(sourceType);
            } catch (NumberFormatException e) {
                this.traceType = 1; // 默认值
            }
        }
    }

    public String getSourceType() {
        return traceType != null ? String.valueOf(traceType) : null;
    }

    /**
     * 兼容性方法：产品名称映射到targetName
     */
    public void setProductName(String productName) {
        this.targetName = productName;
    }

    public String getProductName() {
        return targetName;
    }

    /**
     * 兼容性方法：批次号映射到batchNo
     */
    public void setBatchNumber(String batchNumber) {
        this.batchNo = batchNumber;
    }

    public String getBatchNumber() {
        return batchNo;
    }

    /**
     * 兼容性方法：来源ID映射到supplierId（String <-> Long转换）
     */
    public void setSourceId(String sourceId) {
        if (sourceId != null && !sourceId.isEmpty()) {
            try {
                this.supplierId = Long.parseLong(sourceId);
            } catch (NumberFormatException e) {
                this.supplierId = null;
            }
        } else {
            this.supplierId = null;
        }
    }

    public String getSourceId() {
        return supplierId != null ? String.valueOf(supplierId) : null;
    }

    // ==================== Transient 字段 Getter/Setter ====================

    public String getWarehouseName() {
        return warehouseName;
    }

    public void setWarehouseName(String warehouseName) {
        this.warehouseName = warehouseName;
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

    public LocalDateTime getOutboundTime() {
        return outboundTime;
    }

    public void setOutboundTime(LocalDateTime outboundTime) {
        this.outboundTime = outboundTime;
    }

    public String getUsedBy() {
        return usedBy;
    }

    public void setUsedBy(String usedBy) {
        this.usedBy = usedBy;
    }

    public String getUsagePurpose() {
        return usagePurpose;
    }

    public void setUsagePurpose(String usagePurpose) {
        this.usagePurpose = usagePurpose;
    }

    public LocalDateTime getInboundTime() {
        return inboundTime;
    }

    public void setInboundTime(LocalDateTime inboundTime) {
        this.inboundTime = inboundTime;
    }

    public Integer getInventoryType() {
        return inventoryType;
    }

    public void setInventoryType(Integer inventoryType) {
        this.inventoryType = inventoryType;
    }

    public String getSourceWarehouseId() {
        return sourceWarehouseId;
    }

    public void setSourceWarehouseId(String sourceWarehouseId) {
        this.sourceWarehouseId = sourceWarehouseId;
    }

    public String getTargetWarehouseId() {
        return targetWarehouseId;
    }

    public void setTargetWarehouseId(String targetWarehouseId) {
        this.targetWarehouseId = targetWarehouseId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }
}
