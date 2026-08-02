package com.foodtraceability.service;

import com.foodtraceability.entity.OrderMaterialRequirement;

import java.util.List;

public interface OrderMaterialRequirementService {

    List<OrderMaterialRequirement> generateRequirementsForOrder(String orderId, String kitchenOrderId, String orderNumber);

    List<OrderMaterialRequirement> generateRequirementsForDish(String dishId, String dishName, Integer quantity, String orderId, String kitchenOrderId, String orderNumber);

    List<OrderMaterialRequirement> generateRequirementsForCombo(String comboId, String comboName, Integer quantity, String orderId, String kitchenOrderId, String orderNumber);

    List<OrderMaterialRequirement> getPendingRequirementsByMaterial(String materialName);

    List<OrderMaterialRequirement> getRequirementsByKitchenOrder(String kitchenOrderId);
}
