package com.foodtraceability.service;

import com.foodtraceability.entity.HardwareConfig;
import com.foodtraceability.entity.PrintTask;

import java.util.Map;

public interface DevicePrintService {
    
    /**
     * 打印追溯码标签
     * @param traceabilityCode 追溯码
     * @param productName 产品名称
     * @return 打印结果
     */
    Boolean printTraceabilityLabel(String traceabilityCode, String productName);
    
    /**
     * 打印热敏纸
     * @param content 打印内容
     * @param deviceType 设备类型
     * @return 打印结果
     */
    Boolean printThermalPaper(String content, String deviceType);
    
    /**
     * 打印文件
     * @param filePath 文件路径
     * @param deviceType 设备类型
     * @return 打印结果
     */
    Boolean printFile(String filePath, String deviceType);
    
    /**
     * 打印发票
     * @param invoiceData 发票数据
     * @param deviceType 设备类型
     * @return 打印结果
     */
    Boolean printInvoice(Map<String, Object> invoiceData, String deviceType);
    
    /**
     * 生成打印数据
     * @param traceabilityCode 追溯码
     * @param productName 产品名称
     * @param config 设备配置
     * @return 打印数据
     */
    String generatePrintData(String traceabilityCode, String productName, HardwareConfig config);
    
    /**
     * 生成默认打印数据
     * @param traceabilityCode 追溯码
     * @param productName 产品名称
     * @return 默认打印数据
     */
    String generateDefaultPrintData(String traceabilityCode, String productName);
    
    /**
     * 发送打印命令
     * @param config 设备配置
     * @param printData 打印数据
     * @return 发送结果
     */
    Boolean sendPrintCommand(HardwareConfig config, String printData);
    
    /**
     * 生成测试打印数据
     * @param config 设备配置
     * @return 测试打印数据
     */
    byte[] generateTestPrintData(HardwareConfig config);
    
    /**
     * 提交打印任务到队列
     * @param task 打印任务
     * @return 提交结果，包含任务ID
     */
    PrintTask submitPrintTask(PrintTask task);
}