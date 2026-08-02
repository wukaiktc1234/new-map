package com.foodtraceability.service;

import com.foodtraceability.entity.DeviceTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

public interface DeviceTemplateService {
    
    List<DeviceTemplate> getDeviceTemplates(Long deviceId, String templateType);
    
    DeviceTemplate getDeviceTemplate(Long id);
    
    DeviceTemplate getDefaultTemplate(Long deviceId, String templateType);
    
    DeviceTemplate saveDeviceTemplate(DeviceTemplate template, HttpServletRequest request);
    
    boolean updateDeviceTemplate(DeviceTemplate template, HttpServletRequest request);
    
    boolean deleteDeviceTemplate(Long id, HttpServletRequest request);
    
    boolean setDefaultTemplate(Long deviceId, String templateType, Long id, HttpServletRequest request);
    
    DeviceTemplate createFromConfig(Long deviceId, String templateType, String templateData, String templateName, HttpServletRequest request);
}