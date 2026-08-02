package com.foodtraceability.service.impl;

import com.foodtraceability.entity.DeviceTemplate;
import com.foodtraceability.mapper.DeviceTemplateMapper;
import com.foodtraceability.service.DeviceTemplateService;
import com.foodtraceability.utils.JsonUtils;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeviceTemplateServiceImpl implements DeviceTemplateService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceTemplateServiceImpl.class);
    

    public DeviceTemplateServiceImpl(DeviceTemplateMapper deviceTemplateMapper) {
        this.deviceTemplateMapper = deviceTemplateMapper;
    }

    private final DeviceTemplateMapper deviceTemplateMapper;
    
    @Override
    public List<DeviceTemplate> getDeviceTemplates(Long deviceId, String templateType) {
        if (templateType != null && !templateType.isEmpty()) {
            return deviceTemplateMapper.selectByDeviceIdAndType(deviceId, templateType);
        }
        return deviceTemplateMapper.selectByDeviceId(deviceId);
    }
    
    @Override
    public DeviceTemplate getDeviceTemplate(Long id) {
        return deviceTemplateMapper.selectById(id);
    }
    
    @Override
    public DeviceTemplate getDefaultTemplate(Long deviceId, String templateType) {
        return deviceTemplateMapper.selectDefaultTemplate(deviceId, templateType);
    }
    
    @Override
    public DeviceTemplate saveDeviceTemplate(DeviceTemplate template, jakarta.servlet.http.HttpServletRequest request) {
        if (template.getIsDefault() == null) {
            template.setIsDefault(0);
        }
        
        if (template.getStoreId() == null) {
            template.setStoreId(1L);
        }
        
        template.setCreatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        
        if (template.getId() == null) {
            deviceTemplateMapper.insert(template);
            
            if (template.getIsDefault() == 1) {
                deviceTemplateMapper.updateDefaultTemplate(template.getDeviceId(), template.getTemplateType(), template.getId());
            }
        } else {
            deviceTemplateMapper.updateById(template);
            
            if (template.getIsDefault() == 1) {
                deviceTemplateMapper.updateDefaultTemplate(template.getDeviceId(), template.getTemplateType(), template.getId());
            }
        }
        
        return template;
    }
    
    @Override
    public boolean updateDeviceTemplate(DeviceTemplate template, jakarta.servlet.http.HttpServletRequest request) {
        template.setUpdatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        
        int result = deviceTemplateMapper.updateById(template);
        
        if (result > 0 && template.getIsDefault() == 1) {
            deviceTemplateMapper.updateDefaultTemplate(template.getDeviceId(), template.getTemplateType(), template.getId());
        }
        
        return result > 0;
    }
    
    @Override
    public boolean deleteDeviceTemplate(Long id, jakarta.servlet.http.HttpServletRequest request) {
        return deviceTemplateMapper.deleteById(id) > 0;
    }
    
    @Override
    public boolean setDefaultTemplate(Long deviceId, String templateType, Long id, jakarta.servlet.http.HttpServletRequest request) {
        return deviceTemplateMapper.updateDefaultTemplate(deviceId, templateType, id) > 0;
    }
    
    @Override
    public DeviceTemplate createFromConfig(Long deviceId, String templateType, String templateData, String templateName, jakarta.servlet.http.HttpServletRequest request) {
        DeviceTemplate template = new DeviceTemplate();
        template.setDeviceId(deviceId);
        template.setTemplateType(templateType);
        template.setTemplateName(templateName);
        template.setTemplateData(templateData);
        template.setIsDefault(0);
        template.setStoreId(1L);
        template.setCreatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        
        deviceTemplateMapper.insert(template);
        
        return template;
    }
}