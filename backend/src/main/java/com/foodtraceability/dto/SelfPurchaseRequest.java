package com.foodtraceability.dto;

import com.foodtraceability.entity.SelfPurchase;
import com.foodtraceability.entity.SelfPurchaseItem;

import java.util.List;

/**
 * 自采记录请求DTO
 */
public class SelfPurchaseRequest {
    private SelfPurchase selfPurchase;
    private List<SelfPurchaseItem> items;

    public SelfPurchase getSelfPurchase() {
        return selfPurchase;
    }

    public void setSelfPurchase(SelfPurchase selfPurchase) {
        this.selfPurchase = selfPurchase;
    }

    public List<SelfPurchaseItem> getItems() {
        return items;
    }

    public void setItems(List<SelfPurchaseItem> items) {
        this.items = items;
    }
}
