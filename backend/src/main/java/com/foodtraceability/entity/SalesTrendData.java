package com.foodtraceability.entity;

import java.math.BigDecimal;
import java.util.List;

public class SalesTrendData {
    private List<String> dates;
    private List<BigDecimal> salesAmount;
    private List<Integer> salesVolume;
    
    public SalesTrendData() {
    }
    
    public SalesTrendData(List<String> dates, List<BigDecimal> salesAmount, List<Integer> salesVolume) {
        this.dates = dates;
        this.salesAmount = salesAmount;
        this.salesVolume = salesVolume;
    }
    
    public List<String> getDates() {
        return dates;
    }
    
    public void setDates(List<String> dates) {
        this.dates = dates;
    }
    
    public List<BigDecimal> getSalesAmount() {
        return salesAmount;
    }
    
    public void setSalesAmount(List<BigDecimal> salesAmount) {
        this.salesAmount = salesAmount;
    }
    
    public List<Integer> getSalesVolume() {
        return salesVolume;
    }
    
    public void setSalesVolume(List<Integer> salesVolume) {
        this.salesVolume = salesVolume;
    }
}
