package com.foodtraceability.service;

import com.foodtraceability.common.Result;
import com.foodtraceability.dto.ScanMatchResultDTO;

public interface KitchenScanService {

    ScanMatchResultDTO scanAndMatch(String traceCode, String operatorId, String operatorName);

    ScanMatchResultDTO scanAndMatchWithQuantity(String traceCode, java.math.BigDecimal quantity, String operatorId, String operatorName);

    void lockMaterialsForOrder(String kitchenOrderId);

    void unlockMaterialsForOrder(String kitchenOrderId);
}
