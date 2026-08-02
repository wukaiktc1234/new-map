package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;

import java.util.Map;

/**
 * 设备通信测试服务接口
 * 提供设备通信的测试和验证功能
 */
public interface DeviceCommunicationTestService {

    /**
     * 测试设备通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testCommunication(HardwareConfig config);

    /**
     * 测试打印机通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testPrinterCommunication(HardwareConfig config);

    /**
     * 测试扫码枪通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testScannerCommunication(HardwareConfig config);

    /**
     * 测试电子秤通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testScaleCommunication(HardwareConfig config);

    /**
     * 测试摄像头通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testCameraCommunication(HardwareConfig config);

    /**
     * 测试KDS通信
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testKdsCommunication(HardwareConfig config);

    /**
     * 发送测试打印任务
     * @param config 设备配置
     * @return 测试结果
     */
    Map<String, Object> sendTestPrint(HardwareConfig config);

    /**
     * 发送测试扫描任务
     * @param config 设备配置
     * @return 测试结果
     */
    Map<String, Object> sendTestScan(HardwareConfig config);

    /**
     * 发送测试称重任务
     * @param config 设备配置
     * @return 测试结果
     */
    Map<String, Object> sendTestWeigh(HardwareConfig config);

    /**
     * 获取设备通信诊断信息
     * @param config 设备配置
     * @return 诊断信息
     */
    Map<String, Object> getDiagnostics(HardwareConfig config);
}
