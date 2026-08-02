package com.foodtraceability.entity;

import java.math.BigDecimal;

public class TopProductData {
    private String productId;
    private String productName;
    private Integer salesVolume;
    private BigDecimal salesAmount;
    
    public TopProductData() {
    }
    
    public TopProductData(String productId, String productName, Integer salesVolume, BigDecimal salesAmount) {
        this.productId = productId;
        this.productName = productName;
        this.salesVolume = salesVolume;
        this.salesAmount = salesAmount;
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
    
    public Integer getSalesVolume() {
        return salesVolume;
    }
    
    public void setSalesVolume(Integer salesVolume) {
        this.salesVolume = salesVolume;
    }
    
    public BigDecimal getSalesAmount() {
        return salesAmount;
    }
    
    public void setSalesAmount(BigDecimal salesAmount) {
        this.salesAmount = salesAmount;
    }
}
