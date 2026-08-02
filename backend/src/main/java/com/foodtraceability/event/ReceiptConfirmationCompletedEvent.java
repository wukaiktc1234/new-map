package com.foodtraceability.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收货确认完成事件（到货确认 / 无单直收共用）
 * 触发：原料追溯码生成（SR-7）
 * 监听器：ReceiptConfirmationEventListener（@TransactionalEventListener AFTER_COMMIT + @Async）
 */
public class ReceiptConfirmationCompletedEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long confirmationId;
    private String confirmationCode;
    private String receiptSource;
    private Long supplierId;
    private String supplierName;
    private String storeId;
    private String storeName;
    private List<Item> items;

    public static class Item implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long materialId;
        private String materialName;
        private BigDecimal confirmedQuantity;
        private String unit;
        private String batchNo;
        private LocalDate productionDate;
        private LocalDate expiryDate;

        public Long getMaterialId() {
            return materialId;
        }

        public void setMaterialId(Long materialId) {
            this.materialId = materialId;
        }

        public String getMaterialName() {
            return materialName;
        }

        public void setMaterialName(String materialName) {
            this.materialName = materialName;
        }

        public BigDecimal getConfirmedQuantity() {
            return confirmedQuantity;
        }

        public void setConfirmedQuantity(BigDecimal confirmedQuantity) {
            this.confirmedQuantity = confirmedQuantity;
        }

        public String getUnit() {
            return unit;
        }

        public void setUnit(String unit) {
            this.unit = unit;
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
    }

    public Long getConfirmationId() {
        return confirmationId;
    }

    public void setConfirmationId(Long confirmationId) {
        this.confirmationId = confirmationId;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }

    public String getReceiptSource() {
        return receiptSource;
    }

    public void setReceiptSource(String receiptSource) {
        this.receiptSource = receiptSource;
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

    public String getStoreId() {
        return storeId;
    }

    public void setStoreId(String storeId) {
        this.storeId = storeId;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}
