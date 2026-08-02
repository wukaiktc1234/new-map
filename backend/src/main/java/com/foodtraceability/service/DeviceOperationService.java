package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.TestResult;

import java.util.Map;

/**
 * 设备操作服务接口
 * 负责执行各种设备的具体操作
 */
public interface DeviceOperationService {

    /**
     * 执行打印机操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executePrinterOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 执行扫码枪操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executeScannerOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 执行摄像头操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executeCameraOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 执行KDS操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executeKdsOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 执行POS机操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executePosOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 执行客显屏操作
     * @param config 设备配置
     * @param operationType 操作类型
     * @param params 操作参数
     * @return 操作结果
     */
    Map<String, Object> executeDisplayOperation(HardwareConfig config, String operationType, Map<String, Object> params);

    /**
     * 测试设备
     * @param config 设备配置
     * @return 测试结果
     */
    TestResult testDevice(HardwareConfig config);

    /**
     * 打印热敏纸
     * @param config 设备配置
     * @param content 打印内容
     * @return 打印结果
     */
    Map<String, Object> printThermalPaper(HardwareConfig config, String content);

    /**
     * 打印追溯码标签
     * @param config 设备配置
     * @param traceabilityCode 追溯码
     * @param productName 产品名称
     * @return 打印结果
     */
    Map<String, Object> printTraceabilityLabel(HardwareConfig config, String traceabilityCode, String productName);

    /**
     * 扫描条形码
     * @param config 设备配置
     * @return 扫描结果
     */
    Map<String, Object> scanBarcode(HardwareConfig config);

    /**
     * 扫描二维码
     * @param config 设备配置
     * @return 扫描结果
     */
    Map<String, Object> scanQRCode(HardwareConfig config);

    /**
     * 拍照
     * @param config 设备配置
     * @return 拍照结果
     */
    Map<String, Object> captureImage(HardwareConfig config);

    /**
     * 发送KDS订单
     * @param config 设备配置
     * @param orderData 订单数据
     * @return 发送结果
     */
    Map<String, Object> sendKdsOrder(HardwareConfig config, Map<String, Object> orderData);

    /**
     * 显示客显信息
     * @param config 设备配置
     * @param displayData 显示数据
     * @return 显示结果
     */
    Map<String, Object> displayCustomerInfo(HardwareConfig config, Map<String, Object> displayData);
}
