package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 采购入库事件DTO
 * 用于采购入库场景的会计事件实现
 * 包含供应商、物料类别、数量单价等特有信息
 * @author example
 * @since 2026-04-04
 */
public class PurchaseInboundEvent implements AccountingEvent {

    /** 事件类型常量：采购入库 */
    public static final String EVENT_TYPE = "PURCHASE_INBOUND";
    
    /** 物料类别枚举：肉类 */
    public static final String MATERIAL_MEAT = "MEAT";
    
    /** 物料类别枚举：蔬菜 */
    public static final String MATERIAL_VEGETABLE = "VEGETABLE";
    
    /** 物料类别枚举：调味品 */
    public static final String MATERIAL_SEASONING = "SEASONING";
    
    /** 物料类别枚举：粮食 */
    public static final String MATERIAL_GRAIN = "GRAIN";
    
    /** 物料类别枚举：饮品 */
    public static final String MATERIAL_DRINK = "DRINK";
    
    /** 物料类别枚举：其他 */
    public static final String MATERIAL_OTHER = "OTHER";

    /**
     * 事件类型
     */
    private String eventType = EVENT_TYPE;

    /**
     * 源业务单据ID（入库单ID）
     */
    private String sourceBusinessId;

    /**
     * 源业务单据编号（入库单号）
     */
    private String sourceBusinessNo;

    /**
     * 事件日期（入库日期）
     */
    private LocalDate eventDate;

    /**
     * 总金额（采购金额）
     */
    private BigDecimal amount;

    /**
     * 供应商ID
     */
    private Long supplierId;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 物料类别
     */
    private String materialCategory;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData = new HashMap<>();

    // 实现AccountingEvent接口方法

    @Override
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    @Override
    public String getSourceBusinessId() {
        return sourceBusinessId;
    }

    public void setSourceBusinessId(String sourceBusinessId) {
        this.sourceBusinessId = sourceBusinessId;
    }

    @Override
    public String getSourceBusinessNo() {
        return sourceBusinessNo;
    }

    public void setSourceBusinessNo(String sourceBusinessNo) {
        this.sourceBusinessNo = sourceBusinessNo;
    }

    @Override
    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    @Override
    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public Map<String, Object> getExtraData() {
        if (extraData == null) {
            extraData = new HashMap<>();
        }
        // 将特有字段添加到扩展数据中
        extraData.put("supplierId", supplierId);
        extraData.put("supplierName", supplierName);
        extraData.put("materialCategory", materialCategory);
        extraData.put("quantity", quantity);
        extraData.put("unitPrice", unitPrice);
        return extraData;
    }

    // getter and setter methods for specific fields
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

    public String getMaterialCategory() {
        return materialCategory;
    }

    public void setMaterialCategory(String materialCategory) {
        this.materialCategory = materialCategory;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    @Override
    public String toString() {
        return "PurchaseInboundEvent{" +
            "eventType='" + eventType + '\'' +
            ", sourceBusinessId='" + sourceBusinessId + '\'' +
            ", sourceBusinessNo='" + sourceBusinessNo + '\'' +
            ", eventDate=" + eventDate +
            ", amount=" + amount +
            ", supplierId=" + supplierId +
            ", supplierName='" + supplierName + '\'' +
            ", materialCategory='" + materialCategory + '\'' +
            ", quantity=" + quantity +
            ", unitPrice=" + unitPrice +
            '}';
    }
}
