package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;
import java.util.List;

public interface HardwareDeviceService {
    
    Boolean scanInventoryCode(String inventoryCode);
    
    Boolean printTraceabilityLabel(String traceabilityCode, String productName);
    
    Boolean captureMealPhoto(String orderId, String traceabilityCode);
    
    Boolean testConnection(HardwareConfig config);
    
    TestResult testConnectionDetailed(HardwareConfig config);
    
    Boolean getDeviceStatus(String deviceType);

    List<HardwareConfig> getAllDevices(Long storeId);
}