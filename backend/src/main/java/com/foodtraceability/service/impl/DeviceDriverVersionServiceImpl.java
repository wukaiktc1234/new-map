package com.foodtraceability.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.foodtraceability.entity.DeviceDriverVersion;
import com.foodtraceability.mapper.DeviceDriverVersionMapper;
import com.foodtraceability.service.DeviceDriverVersionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 设备驱动版本服务实现类
 * 实现设备驱动版本的CRUD操作和版本管理
 */
@Service
public class DeviceDriverVersionServiceImpl extends ServiceImpl<DeviceDriverVersionMapper, DeviceDriverVersion> implements DeviceDriverVersionService {
    
    private static final Logger log = LoggerFactory.getLogger(DeviceDriverVersionServiceImpl.class);
    

    public DeviceDriverVersionServiceImpl(DeviceDriverVersionMapper deviceDriverVersionMapper) {
        this.deviceDriverVersionMapper = deviceDriverVersionMapper;
    }

    private final DeviceDriverVersionMapper deviceDriverVersionMapper;
    
    @Override
    public DeviceDriverVersion getLatestVersion(String deviceType) {
        LambdaQueryWrapper<DeviceDriverVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceDriverVersion::getSupportedDeviceType, deviceType)
                   .eq(DeviceDriverVersion::getStatus, 1)
                   .orderByDesc(DeviceDriverVersion::getReleaseDate)
                   .last("LIMIT 1");
        
        return baseMapper.selectOne(queryWrapper);
    }
    
    @Override
    public List<DeviceDriverVersion> getVersionsByDeviceType(String deviceType) {
        LambdaQueryWrapper<DeviceDriverVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceDriverVersion::getSupportedDeviceType, deviceType)
                   .orderByDesc(DeviceDriverVersion::getReleaseDate);
        
        return baseMapper.selectList(queryWrapper);
    }
    
    @Override
    public DeviceDriverVersion getDefaultVersion(String deviceType) {
        LambdaQueryWrapper<DeviceDriverVersion> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(DeviceDriverVersion::getSupportedDeviceType, deviceType)
                   .eq(DeviceDriverVersion::getIsDefault, 1)
                   .eq(DeviceDriverVersion::getStatus, 1)
                   .last("LIMIT 1");
        
        return baseMapper.selectOne(queryWrapper);
    }
    
    @Override
    @Transactional
    public boolean setDefaultVersion(Long id) {
        // 检查驱动版本是否存在
        DeviceDriverVersion version = baseMapper.selectById(id);
        if (version == null) {
            log.error("驱动版本ID {} 不存在", id);
            return false;
        }
        
        // 先将该设备类型的所有驱动版本的默认标识设为0
        DeviceDriverVersion updateEntity = new DeviceDriverVersion();
        updateEntity.setIsDefault(0);
        LambdaQueryWrapper<DeviceDriverVersion> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(DeviceDriverVersion::getSupportedDeviceType, version.getSupportedDeviceType());
        baseMapper.update(updateEntity, updateWrapper);
        
        // 将指定的驱动版本设为默认
        version.setIsDefault(1);
        int updateCount = baseMapper.updateById(version);
        
        return updateCount > 0;
    }
    
    @Override
    public int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }
        
        // 分割版本号
        String[] v1Parts = version1.split("\\.");
        String[] v2Parts = version2.split("\\.");
        
        int maxLength = Math.max(v1Parts.length, v2Parts.length);
        for (int i = 0; i < maxLength; i++) {
            int v1 = i < v1Parts.length ? parseInt(v1Parts[i]) : 0;
            int v2 = i < v2Parts.length ? parseInt(v2Parts[i]) : 0;
            
            if (v1 > v2) {
                return 1;
            } else if (v1 < v2) {
                return -1;
            }
        }
        
        return 0;
    }
    
    /**
     * 将字符串转换为整数，转换失败返回0
     * @param str 字符串
     * @return 转换后的整数
     */
    private int parseInt(String str) {
        try {
            return Integer.parseInt(str.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    @Override
    public boolean checkCompatibility(String deviceType, String firmwareVersion, String driverVersion) {
        // 这里可以添加更复杂的兼容性检查逻辑
        // 例如：检查驱动版本是否支持设备固件版本
        // 由于是示例，我们简单返回true
        return true;
    }
}