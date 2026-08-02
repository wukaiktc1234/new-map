package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;

public interface DeviceConnectionService {
    
    /**
     * 测试设备连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testConnection(HardwareConfig config);
    
    /**
     * 测试设备连接（详细结果）
     * @param config 设备配置
     * @return 详细的连接测试结果
     */
    TestResult testConnectionDetailed(HardwareConfig config);
    
    /**
     * 测试扫码枪连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testScannerConnection(HardwareConfig config);
    
    /**
     * 测试打印机连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testPrinterConnection(HardwareConfig config);
    
    /**
     * 测试WSD打印机连接
     * @param ip IP地址
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testWsdPrinterConnection(String ip, HardwareConfig config);
    
    /**
     * 测试Socket打印机连接
     * @param ip IP地址
     * @param port 端口号
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testSocketPrinterConnection(String ip, int port, HardwareConfig config);
    
    /**
     * 测试打印队列连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testPrintQueueConnection(HardwareConfig config);
    
    /**
     * 测试摄像头连接
     * @param config 设备配置
     * @return 连接测试结果
     */
    Boolean testCameraConnection(HardwareConfig config);
    
    /**
     * 使用WSD协议发送打印命令
     * @param ip IP地址
     * @param printData 打印数据
     * @return 发送结果
     */
    Boolean sendWsdPrintCommand(String ip, String printData);
    
    /**
     * 使用Socket连接发送打印命令
     * @param ip IP地址
     * @param port 端口号
     * @param printData 打印数据
     * @return 发送结果
     */
    Boolean sendSocketPrintCommand(String ip, int port, String printData);
}