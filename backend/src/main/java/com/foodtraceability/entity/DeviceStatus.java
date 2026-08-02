package com.foodtraceability.entity;

import java.util.Date;

/**
 * 设备状态详情实体类
 * 用于存储设备的详细状态信息
 */
public class DeviceStatus {
    
    /** 设备类型 */
    private String deviceType;
    
    /** 设备在线状态 */
    private boolean online;
    
    /** 设备响应时间（毫秒） */
    private long responseTime;
    
    /** 设备型号 */
    private String deviceModel;
    
    /** 设备固件版本 */
    private String firmwareVersion;
    
    /** 连接类型：NETWORK（网络）、SERIAL（串口）、BLUETOOTH（蓝牙） */
    private String connectionType;
    
    /** 网络IP地址（仅网络设备） */
    private String ipAddress;
    
    /** 网络端口（仅网络设备） */
    private String port;
    
    /** 最后检测时间 */
    private Date lastCheckTime;
    
    /** 检测结果详情 */
    private String details;
    
    /** 错误信息（如果有） */
    private String errorMessage;
    
    /** 设备名称 */
    private String deviceName;
    
    /** 设备ID */
    private Long deviceId;
    
    /**
     * 获取设备类型
     * @return 设备类型
     */
    public String getDeviceType() {
        return deviceType;
    }
    
    /**
     * 设置设备类型
     * @param deviceType 设备类型
     */
    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }
    
    /**
     * 获取设备在线状态
     * @return 设备在线状态
     */
    public boolean isOnline() {
        return online;
    }
    
    /**
     * 设置设备在线状态
     * @param online 设备在线状态
     */
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    /**
     * 获取设备响应时间
     * @return 设备响应时间（毫秒）
     */
    public long getResponseTime() {
        return responseTime;
    }
    
    /**
     * 设置设备响应时间
     * @param responseTime 设备响应时间（毫秒）
     */
    public void setResponseTime(long responseTime) {
        this.responseTime = responseTime;
    }
    
    /**
     * 获取设备型号
     * @return 设备型号
     */
    public String getDeviceModel() {
        return deviceModel;
    }
    
    /**
     * 设置设备型号
     * @param deviceModel 设备型号
     */
    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }
    
    /**
     * 获取设备固件版本
     * @return 设备固件版本
     */
    public String getFirmwareVersion() {
        return firmwareVersion;
    }
    
    /**
     * 设置设备固件版本
     * @param firmwareVersion 设备固件版本
     */
    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }
    
    /**
     * 获取连接类型
     * @return 连接类型
     */
    public String getConnectionType() {
        return connectionType;
    }
    
    /**
     * 设置连接类型
     * @param connectionType 连接类型
     */
    public void setConnectionType(String connectionType) {
        this.connectionType = connectionType;
    }
    
    /**
     * 获取网络IP地址
     * @return 网络IP地址
     */
    public String getIpAddress() {
        return ipAddress;
    }
    
    /**
     * 设置网络IP地址
     * @param ipAddress 网络IP地址
     */
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    /**
     * 获取网络端口
     * @return 网络端口
     */
    public String getPort() {
        return port;
    }
    
    /**
     * 设置网络端口
     * @param port 网络端口
     */
    public void setPort(String port) {
        this.port = port;
    }
    
    /**
     * 获取最后检测时间
     * @return 最后检测时间
     */
    public Date getLastCheckTime() {
        return lastCheckTime;
    }
    
    /**
     * 设置最后检测时间
     * @param lastCheckTime 最后检测时间
     */
    public void setLastCheckTime(Date lastCheckTime) {
        this.lastCheckTime = lastCheckTime;
    }
    
    /**
     * 获取检测结果详情
     * @return 检测结果详情
     */
    public String getDetails() {
        return details;
    }
    
    /**
     * 设置检测结果详情
     * @param details 检测结果详情
     */
    public void setDetails(String details) {
        this.details = details;
    }
    
    /**
     * 获取错误信息
     * @return 错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 设置错误信息
     * @param errorMessage 错误信息
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    /**
     * 获取设备名称
     * @return 设备名称
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    /**
     * 设置设备名称
     * @param deviceName 设备名称
     */
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    /**
     * 获取设备ID
     * @return 设备ID
     */
    public Long getDeviceId() {
        return deviceId;
    }
    
    /**
     * 设置设备ID
     * @param deviceId 设备ID
     */
    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
}