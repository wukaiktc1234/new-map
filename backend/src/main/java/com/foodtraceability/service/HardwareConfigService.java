package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface HardwareConfigService {
    
    List<HardwareConfig> getAllDevices(Long storeId);
    
    HardwareConfig getDeviceById(Long id);
    
    HardwareConfig getConfig(Long storeId, String deviceType);
    
    HardwareConfig saveConfig(HardwareConfig config, HttpServletRequest request);
    
    boolean deleteConfig(Long id, HttpServletRequest request);
    
    boolean batchDeleteConfig(String ids, HttpServletRequest request);
    
    boolean batchSaveConfig(java.util.List<HardwareConfig> configs, HttpServletRequest request);
}