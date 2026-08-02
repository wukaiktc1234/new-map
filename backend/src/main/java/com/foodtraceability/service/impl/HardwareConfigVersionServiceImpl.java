package com.foodtraceability.service.impl;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.HardwareConfigVersion;
import com.foodtraceability.mapper.HardwareConfigMapper;
import com.foodtraceability.mapper.HardwareConfigVersionMapper;
import com.foodtraceability.service.HardwareConfigService;
import com.foodtraceability.service.HardwareConfigVersionService;
import com.foodtraceability.utils.AESUtil;
import com.foodtraceability.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.List;

/**
 * 设备配置版本服务实现类
 * 实现设备配置版本的管理功能，包括版本创建、查询、回滚和删除
 */
@Service
public class HardwareConfigVersionServiceImpl implements HardwareConfigVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(HardwareConfigVersionServiceImpl.class);
    

    public HardwareConfigVersionServiceImpl(HardwareConfigVersionMapper hardwareConfigVersionMapper, HardwareConfigMapper hardwareConfigMapper, AESUtil aesUtil, @Lazy HardwareConfigService hardwareConfigService) {
        this.hardwareConfigVersionMapper = hardwareConfigVersionMapper;
        this.hardwareConfigMapper = hardwareConfigMapper;
        this.aesUtil = aesUtil;
        this.hardwareConfigService = hardwareConfigService;
    }

    private final HardwareConfigVersionMapper hardwareConfigVersionMapper;
    
    private final HardwareConfigMapper hardwareConfigMapper;
    
    private final AESUtil aesUtil;
    
    private final HardwareConfigService hardwareConfigService;
    
    /**
     * 创建设备配置版本
     * @param config 设备配置对象
     * @param versionDesc 版本描述
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 创建的版本对象
     */
    @Override
    public HardwareConfigVersion createVersion(HardwareConfig config, String versionDesc, HttpServletRequest request) {
        // 获取最新版本号
        Integer maxVersion = hardwareConfigVersionMapper.selectMaxVersionByHardwareId(config.getId());
        Integer newVersion = maxVersion != null ? maxVersion + 1 : 1;
        
        // 创建版本对象
        HardwareConfigVersion version = new HardwareConfigVersion();
        version.setHardwareId(config.getId());
        version.setDeviceType(config.getDeviceType());
        version.setDeviceName(config.getDeviceName());
        version.setDeviceModel(config.getDeviceModel());
        version.setConnectionType(config.getConnectionType());
        version.setIpAddress(config.getIpAddress());
        version.setPort(config.getPort());
        version.setBaudRate(config.getBaudRate());
        
        // API密钥已经加密，直接保存
        version.setApiKey(config.getApiKey());
        
        version.setRemark(config.getRemark());
        version.setStatus(config.getStatus());
        version.setConfigJson(config.getConfigJson());
        version.setStoreId(config.getStoreId());
        version.setVersion(newVersion);
        version.setCreatedBy(SecurityUtils.getCurrentUsername() != null ? SecurityUtils.getCurrentUsername() : "system");
        version.setCreatedAt(new Date());
        version.setVersionDesc(versionDesc != null ? versionDesc : "自动创建版本");
        
        // 保存版本
        hardwareConfigVersionMapper.insert(version);
        log.info("创建设备配置版本成功，硬件ID: {}, 版本号: {}", config.getId(), newVersion);
        
        return version;
    }
    
    /**
     * 根据硬件配置ID获取版本列表
     * @param hardwareId 硬件配置ID
     * @return 版本列表
     */
    @Override
    public List<HardwareConfigVersion> getVersionsByHardwareId(Long hardwareId) {
        return hardwareConfigVersionMapper.selectVersionsByHardwareId(hardwareId);
    }
    
    /**
     * 根据版本ID获取版本详情
     * @param versionId 版本ID
     * @return 版本详情
     */
    @Override
    public HardwareConfigVersion getVersionById(Long versionId) {
        return hardwareConfigVersionMapper.selectVersionById(versionId);
    }
    
    /**
     * 回滚到指定版本
     * @param versionId 版本ID
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 回滚后的设备配置对象
     */
    @Override
    public HardwareConfig rollbackToVersion(Long versionId, HttpServletRequest request) {
        // 获取版本详情
        HardwareConfigVersion version = hardwareConfigVersionMapper.selectVersionById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("版本不存在，版本ID: " + versionId);
        }
        
        // 创建新的硬件配置对象
        HardwareConfig config = new HardwareConfig();
        config.setId(version.getHardwareId());
        config.setDeviceType(version.getDeviceType());
        config.setDeviceName(version.getDeviceName());
        config.setDeviceModel(version.getDeviceModel());
        config.setConnectionType(version.getConnectionType());
        config.setIpAddress(version.getIpAddress());
        config.setPort(version.getPort());
        config.setBaudRate(version.getBaudRate());
        config.setApiKey(version.getApiKey()); // 已加密，直接使用
        config.setRemark(version.getRemark());
        config.setStatus(version.getStatus());
        config.setConfigJson(version.getConfigJson());
        config.setStoreId(version.getStoreId());
        
        // 保存配置（会自动处理API密钥的加密）
        HardwareConfig rolledConfig = hardwareConfigService.saveConfig(config, request);
        log.info("设备配置回滚成功，硬件ID: {}, 版本号: {}", version.getHardwareId(), version.getVersion());
        
        return rolledConfig;
    }
    
    /**
     * 删除指定版本
     * @param versionId 版本ID
     * @return 删除是否成功
     */
    @Override
    public boolean deleteVersion(Long versionId) {
        int result = hardwareConfigVersionMapper.deleteById(versionId);
        boolean success = result > 0;
        if (success) {
            log.info("删除设备配置版本成功，版本ID: {}", versionId);
        } else {
            log.warn("删除设备配置版本失败，版本ID: {}", versionId);
        }
        return success;
    }
}