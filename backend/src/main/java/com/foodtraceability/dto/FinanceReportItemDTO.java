package com.foodtraceability.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class FinanceReportItemDTO {

    private Long id;

    private Long reportId;

    private String itemName;

    private String itemType;

    private BigDecimal amount;

    private BigDecimal percentage;

    private String period;

    private Integer sort;

    private List<FinanceReportItemDTO> children;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public List<FinanceReportItemDTO> getChildren() {
        return children;
    }

    public void setChildren(List<FinanceReportItemDTO> children) {
        this.children = children;
    }

    public void addChild(FinanceReportItemDTO child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
