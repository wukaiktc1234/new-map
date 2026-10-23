package com.foodtraceability.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.foodtraceability.entity.AppDeviceRegistration;

/**
 * APK 设备激活白名单服务
 * 管理端发放一次性激活码，APK 设备凭码绑定后白名单放行
 */
public interface AppDeviceRegistrationService extends IService<AppDeviceRegistration> {

    /**
     * 生成一次性激活码（管理端）
     * @return 生成的白名单登记（含激活码）
     */
    AppDeviceRegistration generateActivationCode();

    /**
     * 设备激活：凭激活码绑定设备，置为 ACTIVE
     * @param activationCode 激活码
     * @param deviceId 设备唯一标识
     * @param deviceName 设备名称
     * @return 激活后的登记
     */
    AppDeviceRegistration activate(String activationCode, String deviceId, String deviceName);

    /**
     * 查询设备当前状态：ACTIVE / PENDING / DISABLED / UNKNOWN（未登记）
     * @param deviceId 设备唯一标识
     */
    String getDeviceStatus(String deviceId);

    /**
     * 设备是否在白名单（ACTIVE 状态）
     * @param deviceId 设备唯一标识
     */
    boolean isWhitelisted(String deviceId);

    /**
     * 分页查询设备白名单
     */
    Page<AppDeviceRegistration> getPage(long current, long size, String keyword);

    /**
     * 禁用设备（踢出白名单）
     */
    void disable(Long registrationId);

    /**
     * 恢复启用设备
     */
    void enable(Long registrationId);

    /**
     * 更新最近访问时间
     */
    void touchLastSeen(String deviceId);
}
