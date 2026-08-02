package com.foodtraceability.service.impl;

import com.foodtraceability.constant.DeviceConstants;
import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.HardwareOperationLog;
import com.foodtraceability.mapper.HardwareConfigMapper;
import com.foodtraceability.service.DeviceStatusService;
import com.foodtraceability.service.HardwareConfigService;
import com.foodtraceability.service.HardwareConfigVersionService;
import com.foodtraceability.service.HardwareOperationLogService;
import com.foodtraceability.utils.AESUtil;
import com.foodtraceability.utils.JsonUtils;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 硬件配置服务实现类
 * 负责硬件设备配置的CRUD操作和API密钥的加密解密
 */
@Service
public class HardwareConfigServiceImpl implements HardwareConfigService {
    
    private static final Logger log = LoggerFactory.getLogger(HardwareConfigServiceImpl.class);
    

    public HardwareConfigServiceImpl(HardwareConfigMapper hardwareConfigMapper, AESUtil aesUtil, HardwareOperationLogService hardwareOperationLogService, HardwareConfigVersionService hardwareConfigVersionService, DeviceStatusService deviceStatusService) {
        this.hardwareConfigMapper = hardwareConfigMapper;
        this.aesUtil = aesUtil;
        this.hardwareOperationLogService = hardwareOperationLogService;
        this.hardwareConfigVersionService = hardwareConfigVersionService;
        this.deviceStatusService = deviceStatusService;
    }

    private final HardwareConfigMapper hardwareConfigMapper;
    
    private final AESUtil aesUtil;
    
    private final HardwareOperationLogService hardwareOperationLogService;
    
    private final HardwareConfigVersionService hardwareConfigVersionService;
    
    private final DeviceStatusService deviceStatusService;
    
    /**
     * 获取所有设备列表
     * @param storeId 门店ID
     * @return 设备配置列表
     */
    @Override
    public java.util.List<HardwareConfig> getAllDevices(Long storeId) {
        String storeIdStr = storeId != null ? String.valueOf(storeId) : null;
        java.util.List<HardwareConfig> configs = hardwareConfigMapper.selectList(null);
        
        for (HardwareConfig config : configs) {
            if (config.getApiKey() != null) {
                config.setApiKey(aesUtil.decrypt(config.getApiKey()));
            }
        }
        
        return configs;
    }
    
    /**
     * 根据ID获取设备详情
     * @param id 设备ID
     * @return 设备配置对象
     */
    @Override
    public HardwareConfig getDeviceById(Long id) {
        HardwareConfig config = hardwareConfigMapper.selectById(id);
        
        if (config != null && config.getApiKey() != null) {
            config.setApiKey(aesUtil.decrypt(config.getApiKey()));
        }
        
        return config;
    }
    
    /**
     * 获取硬件配置
     * @param storeId 门店ID
     * @param deviceType 设备类型
     * @return 硬件配置对象（如果找不到则返回默认配置）
     */
    @Override
    public HardwareConfig getConfig(Long storeId, String deviceType) {
        HardwareConfig config = hardwareConfigMapper.selectConfig(storeId != null ? String.valueOf(storeId) : null, deviceType);
        
        // 如果找不到配置，返回一个默认配置对象而不是null
        if (config == null) {
            config = new HardwareConfig();
            config.setDeviceType(deviceType);
            config.setStoreId(storeId != null ? storeId : DeviceConstants.DEFAULT_STORE_ID);
            config.setDeviceName("");
            config.setDeviceModel("");
            config.setConnectionType("NETWORK");
            config.setIpAddress("");
            config.setPort(DeviceConstants.DEFAULT_PORT);
            config.setBaudRate(DeviceConstants.DEFAULT_BAUD_RATE);
            config.setApiKey("");
            config.setRemark("");
            config.setStatus(0);
            config.setConfigJson("{}");
        } else {
            // 解密API密钥
            if (config.getApiKey() != null) {
                config.setApiKey(aesUtil.decrypt(config.getApiKey()));
            }
        }
        
        return config;
    }
    
    /**
     * 保存硬件配置
     * @param config 硬件配置对象
     * @return 保存后的硬件配置对象（API密钥已解密）
     */
    @Override
    public HardwareConfig saveConfig(HardwareConfig config, jakarta.servlet.http.HttpServletRequest request) {
        // 获取操作前的数据
        HardwareConfig oldConfig = null;
        String operationType = "CREATE";
        if (config.getId() != null) {
            oldConfig = hardwareConfigMapper.selectById(config.getId());
            operationType = "UPDATE";
        }
        
        // 设置默认状态
        if (config.getStatus() == null) {
            config.setStatus(0); // 默认未连接状态
        }
        
        // 设置默认门店ID（如果没有提供）
        if (config.getStoreId() == null) {
            config.setStoreId(DeviceConstants.DEFAULT_STORE_ID); // 默认门店ID
        }
        
        // 设置默认配置JSON（如果没有提供）
        if (config.getConfigJson() == null) {
            config.setConfigJson("{}");
        }
        
        // 加密API密钥
        if (config.getApiKey() != null) {
            config.setApiKey(aesUtil.encrypt(config.getApiKey()));
        }
        
        if (config.getId() == null) {
            hardwareConfigMapper.insert(config);
        } else {
            hardwareConfigMapper.updateById(config);
        }
        
        // 返回解密后的配置
        if (config.getApiKey() != null) {
            config.setApiKey(aesUtil.decrypt(config.getApiKey()));
        }
        
        // 记录操作日志
        recordHardwareOperationLog(oldConfig, config, operationType, request);
        
        // 创建设备配置版本
        try {
            String versionDesc = "CREATE".equals(operationType) ? "初始创建配置" : "更新配置";
            hardwareConfigVersionService.createVersion(config, versionDesc, request);
        } catch (Exception e) {
            // 记录版本失败不影响主业务流程
            log.error("创建设备配置版本失败，配置ID: {}", config.getId(), e);
        }
        
        // 清除设备状态缓存，确保下次获取时重新检测
        try {
            if (deviceStatusService != null) {
                deviceStatusService.clearDeviceStatusCache(config.getDeviceType());
                log.info("设备配置更新后清除缓存: {}", config.getDeviceType());
            }
        } catch (Exception e) {
            // 清除缓存失败不影响主业务流程
            log.error("清除设备状态缓存失败: {}", config.getDeviceType(), e);
        }
        
        return config;
    }
    
    /**
     * 记录硬件设备操作日志
     * @param oldConfig 操作前的配置
     * @param newConfig 操作后的配置
     * @param operationType 操作类型
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     */
    private void recordHardwareOperationLog(HardwareConfig oldConfig, HardwareConfig newConfig, String operationType, jakarta.servlet.http.HttpServletRequest request) {
        try {
            HardwareOperationLog log = new HardwareOperationLog();
            
            // 根据操作类型设置日志内容
            if ("DELETE".equals(operationType)) {
                // 删除操作，使用旧配置信息
                log.setHardwareId(oldConfig.getId());
                log.setDeviceType(oldConfig.getDeviceType());
                log.setDeviceName(oldConfig.getDeviceName());
                log.setStoreId(oldConfig.getStoreId());
                log.setOperationDesc("设备配置删除");
                log.setOldData(JsonUtils.toJson(oldConfig));
                log.setNewData(null);
            } else {
                // 创建或更新操作，使用新配置信息
                log.setHardwareId(newConfig.getId());
                log.setDeviceType(newConfig.getDeviceType());
                log.setDeviceName(newConfig.getDeviceName());
                log.setStoreId(newConfig.getStoreId());
                log.setOperationDesc("设备配置" + ("CREATE".equals(operationType) ? "创建" : "更新"));
                // 序列化旧数据和新数据为JSON
                log.setOldData(oldConfig != null ? JsonUtils.toJson(oldConfig) : null);
                log.setNewData(JsonUtils.toJson(newConfig));
            }
            
            log.setOperationType(operationType);
            
            // 设置操作人信息
            log.setOperatorId(SecurityUtils.getCurrentUserId() != null ? String.valueOf(SecurityUtils.getCurrentUserId()) : "system");
            log.setOperatorName(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "系统");
            
            // 记录日志，包含详细审计信息
            hardwareOperationLogService.recordLog(log, request);
        } catch (Exception e) {
            // 记录日志失败时，避免影响主业务流程
            log.error("记录硬件配置操作日志失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 删除单个硬件配置
     * @param id 配置ID
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 删除是否成功
     */
    @Override
    public boolean deleteConfig(Long id, jakarta.servlet.http.HttpServletRequest request) {
        // 获取要删除的配置
        HardwareConfig config = hardwareConfigMapper.selectById(id);
        if (config != null) {
            boolean result = hardwareConfigMapper.deleteById(id) > 0;
            if (result) {
                // 记录删除日志
                recordHardwareOperationLog(config, null, "DELETE", request);
            }
            return result;
        }
        return false;
    }
    
    /**
     * 批量删除硬件配置
     * @param ids 逗号分隔的配置ID字符串
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 删除是否成功
     */
    @Override
    public boolean batchDeleteConfig(String ids, jakarta.servlet.http.HttpServletRequest request) {
        // 将逗号分隔的ID字符串转换为Long数组
        String[] idArray = ids.split(",");
        java.util.List<Long> idList = new java.util.ArrayList<>();
        for (String idStr : idArray) {
            idList.add(Long.parseLong(idStr.trim()));
        }
        
        // 获取要删除的所有配置
        java.util.List<HardwareConfig> configs = hardwareConfigMapper.selectBatchIds(idList);
        boolean result = hardwareConfigMapper.deleteBatchIds(idList) > 0;
        
        if (result && !configs.isEmpty()) {
            // 批量记录删除日志
            for (HardwareConfig config : configs) {
                recordHardwareOperationLog(config, null, "DELETE", request);
            }
        }
        
        return result;
    }
    
    /**
     * 批量保存硬件配置
     * @param configs 硬件配置列表
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 保存是否成功
     */
    @Override
    public boolean batchSaveConfig(java.util.List<HardwareConfig> configs, jakarta.servlet.http.HttpServletRequest request) {
        if (configs == null || configs.isEmpty()) {
            return true;
        }
        
        boolean allSuccess = true;
        
        for (HardwareConfig config : configs) {
            try {
                // 保存单个配置
                saveConfig(config, request);
            } catch (Exception e) {
                log.error("批量保存硬件配置失败，配置ID: {}", config.getId(), e);
                allSuccess = false;
            }
        }
        
        return allSuccess;
    }
}