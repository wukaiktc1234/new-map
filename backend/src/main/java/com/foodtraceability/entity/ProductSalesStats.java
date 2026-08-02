package com.foodtraceability.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProductSalesStats {
    
    private Long id;
    private String productId;
    private String productName;
    private String categoryName;
    private Integer salesVolume;
    private BigDecimal salesAmount;
    private BigDecimal averagePrice;
    private Double growthRate;
    private LocalDateTime statsDate;
    private String timeRange;
    
    public ProductSalesStats() {
    }
    
    public ProductSalesStats(Long id, String productId, String productName, String categoryName,
                            Integer salesVolume, BigDecimal salesAmount, BigDecimal averagePrice,
                            Double growthRate, LocalDateTime statsDate, String timeRange) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.categoryName = categoryName;
        this.salesVolume = salesVolume;
        this.salesAmount = salesAmount;
        this.averagePrice = averagePrice;
        this.growthRate = growthRate;
        this.statsDate = statsDate;
        this.timeRange = timeRange;
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getCategoryName() {
        return categoryName;
    }
    
    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
    
    public BigDecimal getAveragePrice() {
        return averagePrice;
    }
    
    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice;
    }
    
    public Double getGrowthRate() {
        return growthRate;
    }
    
    public void setGrowthRate(Double growthRate) {
        this.growthRate = growthRate;
    }
    
    public LocalDateTime getStatsDate() {
        return statsDate;
    }
    
    public void setStatsDate(LocalDateTime statsDate) {
        this.statsDate = statsDate;
    }
    
    public String getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
}
