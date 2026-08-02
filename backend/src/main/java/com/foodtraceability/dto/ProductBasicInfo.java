package com.foodtraceability.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "产品基本信息DTO")
public class ProductBasicInfo {

    @Schema(description = "产品ID", example = "1234567890")
    private String productId;

    @Schema(description = "产品名称", example = "苹果")
    private String productName;

    @Schema(description = "产品编码", example = "P001")
    private String productCode;

    @Schema(description = "分类ID", example = "1234567890")
    private String categoryId;

    @Schema(description = "分类名称", example = "水果")
    private String categoryName;

    @Schema(description = "计量单位", example = "kg")
    private String unit;

    @Schema(description = "销售价", example = "10.50")
    private BigDecimal price;

    @Schema(description = "成本价", example = "8.00")
    private BigDecimal costPrice;

    @Schema(description = "条形码", example = "6901234567890")
    private String barcode;

    @Schema(description = "规格", example = "500g/袋")
    private String specification;

    @Schema(description = "产地", example = "山东")
    private String origin;

    @Schema(description = "供应商ID", example = "1234567890")
    private String supplierId;

    @Schema(description = "供应商名称", example = "XX供应商")
    private String supplierName;

    @Schema(description = "状态（true: 启用, false: 禁用）", example = "true")
    private Boolean status;

    @Schema(description = "数据版本号", example = "1")
    private Long version;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public ProductBasicInfo() {}

    public ProductBasicInfo(String productId, String productName, String productCode,
                           String categoryId, String categoryName, String unit,
                           BigDecimal price, BigDecimal costPrice, String barcode,
                           String specification, String origin, String supplierId,
                           String supplierName, Boolean status, Long version,
                           LocalDateTime updateTime) {
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.unit = unit;
        this.price = price;
        this.costPrice = costPrice;
        this.barcode = barcode;
        this.specification = specification;
        this.origin = origin;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.status = status;
        this.version = version;
        this.updateTime = updateTime;
    }

    public static ProductBasicInfoBuilder builder() {
        return new ProductBasicInfoBuilder();
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSpecification() {
        return specification;
    }

    public void setSpecification(String specification) {
        this.specification = specification;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(String supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public Boolean getStatus() {
        return status;
    }

    public void setStatus(Boolean status) {
        this.status = status;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public static class ProductBasicInfoBuilder {
        private String productId;
        private String productName;
        private String productCode;
        private String categoryId;
        private String categoryName;
        private String unit;
        private BigDecimal price;
        private BigDecimal costPrice;
        private String barcode;
        private String specification;
        private String origin;
        private String supplierId;
        private String supplierName;
        private Boolean status;
        private Long version;
        private LocalDateTime updateTime;

        public ProductBasicInfoBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public ProductBasicInfoBuilder productName(String productName) {
            this.productName = productName;
            return this;
        }

        public ProductBasicInfoBuilder productCode(String productCode) {
            this.productCode = productCode;
            return this;
        }

        public ProductBasicInfoBuilder categoryId(String categoryId) {
            this.categoryId = categoryId;
            return this;
        }

        public ProductBasicInfoBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }

        public ProductBasicInfoBuilder unit(String unit) {
            this.unit = unit;
            return this;
        }

        public ProductBasicInfoBuilder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ProductBasicInfoBuilder costPrice(BigDecimal costPrice) {
            this.costPrice = costPrice;
            return this;
        }

        public ProductBasicInfoBuilder barcode(String barcode) {
            this.barcode = barcode;
            return this;
        }

        public ProductBasicInfoBuilder specification(String specification) {
            this.specification = specification;
            return this;
        }

        public ProductBasicInfoBuilder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public ProductBasicInfoBuilder supplierId(String supplierId) {
            this.supplierId = supplierId;
            return this;
        }

        public ProductBasicInfoBuilder supplierName(String supplierName) {
            this.supplierName = supplierName;
            return this;
        }

        public ProductBasicInfoBuilder status(Boolean status) {
            this.status = status;
            return this;
        }

        public ProductBasicInfoBuilder version(Long version) {
            this.version = version;
            return this;
        }

        public ProductBasicInfoBuilder updateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
            return this;
        }

        public ProductBasicInfo build() {
            return new ProductBasicInfo(productId, productName, productCode,
                    categoryId, categoryName, unit, price, costPrice, barcode,
                    specification, origin, supplierId, supplierName, status,
                    version, updateTime);
        }
    }
}
