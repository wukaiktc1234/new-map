package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.HardwareConfigVersion;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 设备配置版本服务接口
 * 用于处理设备配置版本的管理，包括版本创建、查询、回滚等功能
 */
public interface HardwareConfigVersionService {
    
    /**
     * 创建设备配置版本
     * @param config 设备配置对象
     * @param versionDesc 版本描述
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 创建的版本对象
     */
    HardwareConfigVersion createVersion(HardwareConfig config, String versionDesc, HttpServletRequest request);
    
    /**
     * 根据硬件配置ID获取版本列表
     * @param hardwareId 硬件配置ID
     * @return 版本列表
     */
    List<HardwareConfigVersion> getVersionsByHardwareId(Long hardwareId);
    
    /**
     * 根据版本ID获取版本详情
     * @param versionId 版本ID
     * @return 版本详情
     */
    HardwareConfigVersion getVersionById(Long versionId);
    
    /**
     * 回滚到指定版本
     * @param versionId 版本ID
     * @param request HttpServletRequest对象，用于获取客户端IP和用户代理信息
     * @return 回滚后的设备配置对象
     */
    HardwareConfig rollbackToVersion(Long versionId, HttpServletRequest request);
    
    /**
     * 删除指定版本
     * @param versionId 版本ID
     * @return 删除是否成功
     */
    boolean deleteVersion(Long versionId);
}